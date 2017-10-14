package com.jokeren.concurrent.structures.pattree;


import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class PatriciaTree<V>{

    /**
     * Note that we ignore the sign bit
     * We assume keys 000...0 and 111...1 cannot be inserted or deleted from the trie
     * Since we perform bitwise operations on keys we cannot use generic type
     */


    final long one=1;								//create 000...01
    final long allOnes = (one <<(63) >> (63) )>>>1;  //key of dummy leaf node 111...1
    final long allZeros = 0;							//key of dummy leaf node 000...0
    final long rootKey = one << 62;					//key of root node 100...0

	/*
	 * Assume no null long value is inserted to tree
	 */

    /**
     * Node class defines node objects of the tree.
     * Node is either leaf or internal.
     *
     */

    protected final static class Node<V>{
        final Long key;
        final V value;
        final long mask;
        volatile Node<V> left;
        volatile Node<V> right;
        volatile Info<V> info;

        Node(final Long key, final V value, final long mask, final Node<V> left, final Node<V> right) {
            this.key = key;
            this.value = value;
            this.mask = mask;
            this.left = left;
            this.right = right;
            this.info = null;
        }


        // to create a new copy of node
        Node(final Node<V> node) {
            this(node.key, node.value, node.mask, node.left, node.right);
        }

        // to create a leaf node
        Node(final Long key, final V value) {
            this(key, value, 0, null, null);
        }

        // to create an internal node
        Node(final Long key, final long mask, final Node<V> left, final Node<V> right) {
            this(key, null, mask, left, right);
        }
    }

    /**
     * Info class defines info objects of nodes.
     * Info is either flag or unflag.
     *
     */
    protected static abstract class Info<V>{}

    protected final static class Flag<V> extends Info<V>{
        Node<V> flagNode1, flagNode2, flagNode3, flagNode4, unflagNode1, unflagNode2;
        Info<V> oldInfo1, oldInfo2, oldInfo3, oldInfo4;
        Node<V> pNode1, oldChild1, newChild1, pNode2, oldChild2, newChild2, movLeaf;												// to determine that Info is flag or unflag
        volatile boolean flagDone;

        // to create flag Info
        Flag(final Node<V> flagNode1, final Node<V> flagNode2, final Node<V> flagNode3, final Node<V> flagNode4,
             final Node<V> unflagNode1, final Node<V> unflagNode2,
             final Info<V> oldInfo1, final Info<V> oldInfo2, final Info<V> oldInfo3, final Info<V> oldInfo4,
             final Node<V> pNode1, final Node<V> oldChild1, final Node<V> newChild1,
             final Node<V> pNode2, final Node<V> oldChild2, final Node<V> newChild2 , final Node<V> movLeaf) {
            this.flagNode1 = flagNode1;
            this.flagNode2 = flagNode2;
            this.flagNode3 = flagNode3;
            this.flagNode4 = flagNode4;
            this.unflagNode1 = unflagNode1;
            this.unflagNode2 = unflagNode2;
            this.oldInfo1 = oldInfo1;
            this.oldInfo2 = oldInfo2;
            this.oldInfo3 = oldInfo3;
            this.oldInfo4 = oldInfo4;
            this.pNode1 = pNode1;
            this.oldChild1 = oldChild1;
            this.newChild1 = newChild1;
            this.pNode2 = pNode2;
            this.oldChild2 = oldChild2;
            this.newChild2 = newChild2;
            this.movLeaf = movLeaf;
            this.flagDone = false;
        }
    }

    // to create unflag Info
    protected final static class Unflag<V> extends Info<V>{
        Unflag(){}
    }

    /**
     * Initialization of Patricia Trie
     * The constructor sets the root node and the upper bound of keys.
     *
     */

    final Node<V> root;



    public PatriciaTree(){

    	/*
    	 * to avoid special case root is initialized to an internal node whose key is upper bound of all keys and
    	 * whose children are two dummy leaf nodes.
    	 * create 2 dummy nodes, both contain key null
    	 */
        Long kRootKey = new Long(rootKey);
        Long leftKey = new Long(allZeros);
        Long rightKey = new Long(allOnes);

        root = new Node<V>(kRootKey, 0, new Node<V>(leftKey,null), new Node<V>(rightKey,null));
    }

    /*
     * defining CAS on child fields and info field
     */

    private static final AtomicReferenceFieldUpdater<Node, Node> leftUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "left");
    private static final AtomicReferenceFieldUpdater<Node, Node> rightUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "right");
    private static final AtomicReferenceFieldUpdater<Node, Info> infoUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Info.class, "info");

    /**
     * The find operation tries to find a key in the tree.
     *
     */

    private int countLeaf(Node parent) {
        int c = 0;
        if (parent.left == null && parent.right == null) {
            c = 1;
        }

        if (parent.left != null) {
            c += countLeaf(parent.left);
        }

        if (parent.right != null) {
            c += countLeaf(parent.right);
        }

        return c;
    }

    public int size() {
        return countLeaf(root);
    }

    public final boolean find(final Long key) {

        Node<V> node;
        boolean moved = false;

        	/*
        	 * search begins
        	 */
        node = root;

        while((node.left != null) && (((node.key ^ key) & node.mask) == 0))
            node = (node.key > key) ? node.left : node.right;		// check if bit bitNum of value is 0 or 1


        //check if leaf node is logically in the trie
        if((node.left==null)&&(node.info!=null)&&(node.info.getClass() == Flag.class)){
            Flag<V> nodeInfo = (Flag<V>) node.info;
            moved = ((nodeInfo.pNode1.left != nodeInfo.oldChild1)&&(nodeInfo.pNode1.right != nodeInfo.oldChild1));
        }


           	/*
        	 * search ends
        	 */

        return((node.left==null)&&(key == node.key)&&(moved==false));
    }

    /**
     * The insert operation tries to add a key to the tree.
     *
     */

    public final Long insert(final Long key, final V value) {

        // search variables
        Node<V> p, node;
        Info<V> pInfo;
        boolean moved;

        while(true){

        	/*
        	 * search begins
        	 */
            p = null;
            pInfo = null;
            node = root;

            while((node.left != null) && (((node.key ^ key) & node.mask) == 0)){
                p = node;
                node = (p.key > key) ? node.left : node.right;		// check if bit bitNum of value is 0 or 1
            }

            pInfo = p.info;                             // read pinfo once instead of every iteration
            if (node != p.left && node != p.right) continue;

            //check if node is logically removed by move
            moved = false;
            if((node.left == null)&&(node.info != null) && (node.info.getClass() == Flag.class)){ //if leaf is flagged
                Flag<V> I = (Flag<V>) node.info;
                moved = ((I.pNode1.left != I.oldChild1) && (I.pNode1.right != I.oldChild1)); //if logically removed
            }
            /*
        	 * search ends
        	 */

            // if tree already contains key
            if((node.left == null) && (node.key.longValue() == key) && (moved == false)) return null;

            Info<V> nodeInfo = node.info;

            // if any internal is flagged help
            if((pInfo!=null)&&(pInfo.getClass() == Flag.class))	help((Flag<V>)pInfo);
            else if((node.left!=null) && (nodeInfo!=null) && (nodeInfo.getClass() == Flag.class)) help((Flag<V>)nodeInfo);


                //otherwise create a new flag Info
            else{ //node.key != key

                long newKey = 0;
                long newMask = 0;

                long temp = node.key ^ key;
                byte i = 0;

                if (temp==0) {//children are not prefixes of each other
                    System.out.println("children are not prefixes of each other inside insert");
                    continue;
                }
                while(temp != 0){					//find the first different bit of node.key and value
                    temp >>>=  1;
                    i++;
                }

                newKey = key >>> i << i; 			//set leftmost (i-1) bits of newKey
                long mask = one << (i-1);
                newKey = newKey | mask; 			// set bit i of newKey to one
                newMask = allOnes >>> i << i;

                // create new Internal node whose children are a new copy of node and a new node
                Node<V> newInternal;
                Long kNewKey = new Long(newKey);
                newInternal =(newKey > key) ? new Node<V>(kNewKey, newMask, new Node<V>(key,value), new Node<V>(node))
                        : new Node<V>(kNewKey, newMask, new Node<V>(node), new Node<V>(key,value));


                // create new flag Info
                Flag<V> insInfo;
                if (node.left != null){ // if node is internal order node and p to get falgged
                    if (p.key < node.key)
                        insInfo = new Flag<V>(p, node, null, null, p, null,  pInfo, nodeInfo, null, null,
                                p, node, newInternal, null, null, null, null);
                    else
                        insInfo = new Flag<V>(node, p, null, null, p, null,  nodeInfo, pInfo, null, null,
                                p, node, newInternal, null, null, null, null);
                }
                else  // node is leaf
                    insInfo = new Flag<V>(p, null, null, null, p, null, pInfo, null,null, null,
                            p, node, newInternal, null, null, null, null);

                // call help routine to complete insert
                if(help(insInfo)) return key;
            }																// end of else
        }																	// end of while loop
    }																		// end of insert operation


    /**
     * The delete operation tries to remove a key from the tree.
     *
     */

    public final Long delete(final Long key, final V value) {

        // search variables
        Node<V> gp, p, node;
        Info<V> gpInfo, pInfo;
        boolean moved;

        while(true){

        	/*
        	 * search begins
        	 */
            gpInfo = null;
            gp = null;
            pInfo = null;
            p = null;
            node = root;
            while((node.left != null) && (((node.key ^ key) & node.mask) == 0)){
                gp = p;
                p = node;
                node = (p.key > key) ? node.left : node.right;		// check if bit bitNum of value is 0 or 1
            }

            if (gp != null) {
                gpInfo = gp.info;                               // - read gpinfo once instead of every iteration
                if (p != gp.left && p != gp.right) continue;    //   then confirm the child link to p is valid
                pInfo = p.info;                                 //   (just as if we'd read gp's info field before the reference to p)
                if (node != p.left && node != p.right) continue;      // - do the same for pinfo and l
            }

            moved=false;
            if((node.left == null)&&(node.info != null) && (node.info.getClass() == Flag.class)){ //if leaf is flagged
                Flag<V> I = (Flag<V>) node.info;
                moved = ((I.pNode1.left != I.oldChild1) && (I.pNode1.right != I.oldChild1)); //if logically removed
            }
        	/*
        	 * search ends
        	 */

            // if tree does not contain key
            if((node.left != null) || (node.key.longValue() != key) || (moved == true)) return null;

            // if any internal is flagged help
            if((pInfo!=null)&&(pInfo.getClass() == Flag.class))	help((Flag<V>)pInfo);
            else if((gpInfo!=null)&&(gpInfo.getClass() == Flag.class)) help((Flag<V>)gpInfo);

                //otherwise create a new flag Info
            else{

                // create new flag Info
                Flag<V> delInfo;
                Node<V> nodeSibling = (p.key > node.key) ? p.right : p.left;

                if(gp!=null){ //order gp and p to get flagged
                    if (gp.key < p.key)
                        delInfo = new Flag<V>(gp, p, null, null, gp, null, gpInfo, pInfo, null, null,
                                gp, p, nodeSibling, null, null, null, null);
                    else
                        delInfo = new Flag<V>(p, gp, null, null, gp, null, pInfo, gpInfo, null, null,
                                gp, p, nodeSibling, null, null, null, null);

                    if(help(delInfo)) return key;

                }
            }
        }																								// end of while loop																	// end of insert operation
    }


    /**
     * The delete operation tries to add a key to the tree and remove a key from the tree.
     *
     */

    public final Long move(final Long key_d, final V value_d, final Long key_i, final V value_i) {

        // search variables
        Node<V> gp_d, p_d, node_d, p_i, node_i;
        Info<V> gpInfo_d, pInfo_d, pInfo_i;
        boolean moved_d, moved_i;

        while(true){

        	/*
        	 * search begins
        	 */
            //first search for key_d
            gpInfo_d = null;
            gp_d = null;
            pInfo_d = null;
            p_d = null;
            node_d = root;
            while((node_d.left != null) && (((node_d.key ^ key_d) & node_d.mask) == 0)){
                gp_d = p_d;
                p_d = node_d;
                node_d = (p_d.key > key_d) ? node_d.left : node_d.right;
            }

            if (gp_d != null) {
                gpInfo_d = gp_d.info;                               // - read gpinfo once instead of every iteration
                if (p_d != gp_d.left && p_d != gp_d.right) continue;    //   then confirm the child link to p is valid
                pInfo_d = p_d.info;                                 //   (just as if we'd read gp's info field before the reference to p)
                if (node_d != p_d.left && node_d != p_d.right) continue;      // - do the same for pinfo and l
            }

            //check if node_d is logically in the trie
            moved_d = false;
            if((node_d.left == null)&&(node_d.info != null) && (node_d.info.getClass() == Flag.class)){ //if leaf is flagged
                Flag<V> I = (Flag<V>) node_d.info;
                moved_d = ((I.pNode1.left != I.oldChild1) && (I.pNode1.right != I.oldChild1)); //if logically removed
            }
        	/*
        	 * search ends
        	 */

            // if tree does not contain key_d
            if((node_d.left != null) || (node_d.key.longValue() != key_d) || (moved_d == true)) return null;


        	/*
        	 * search begins
        	 */
            //second search for key_i
            p_i = null;																					//initially root has two dummy children
            pInfo_i = null;
            node_i = root;

            while((node_i.left != null) && (((node_i.key ^ key_i) & node_i.mask) == 0)){
                p_i = node_i;
                node_i = (p_i.key > key_i) ? node_i.left : node_i.right;		// check if bit bitNum of value is 0 or 1
            }

            pInfo_i = p_i.info;                             // read pinfo once instead of every iteration
            if (node_i != p_i.left && node_i != p_i.right) continue;

            moved_i = false;
            if((node_i.left == null)&&(node_i.info != null) && (node_i.info.getClass() == Flag.class)){ //if leaf is flagged
                Flag<V> I = (Flag<V>) node_i.info;
                moved_i = ((I.pNode1.left != I.oldChild1) && (I.pNode1.right != I.oldChild1)); //if logically removed
            }
        	/*
        	 * search ends
        	 */

            // if tree already contains key
            if((node_i.left == null) && (node_i.key.longValue() == key_i) && (moved_i == false)) return null;

            Info<V> nodeInfo_i = node_i.info;

            //check if any internal node is flagged
            if((pInfo_d!=null)&&(pInfo_d.getClass() == Flag.class))	help((Flag<V>)pInfo_d);
            else if((gpInfo_d!=null)&&(gpInfo_d.getClass() == Flag.class)) help((Flag<V>)gpInfo_d);
            else if((pInfo_i!=null)&&(pInfo_i.getClass() == Flag.class)) help((Flag<V>)pInfo_i);
            else if((node_i.left!=null) && (nodeInfo_i!=null) && (nodeInfo_i.getClass() == Flag.class)) help((Flag<V>)nodeInfo_i);

                //otherwise create a new flag Info
            else{
                Flag<V> movInfo = null;
                Node<V> nodeSibling_d = (p_d.key > node_d.key) ? p_d.right : p_d.left;

                //special case 1
                if(node_i == node_d)
                    movInfo = new Flag<V>(p_d, null, null, null, p_d, null, pInfo_d, null, null, null,
                            p_d, node_d, new Node<V>(key_i,value_i), null, null, null, null);

                    //special case 2 and 3
                else if(((node_i == p_d) && (p_i == gp_d)) ||
                        ((gp_d != null) && (node_i != node_d) && (p_i == p_d))){

                    long newKey = 0;
                    long newMask = 0;

                    long temp = nodeSibling_d.key ^ key_i;
                    byte i = 0;

                    if (temp==0) {//children are not prefixes of each other
                        System.out.println("children are not prefixes of each other inside move");
                        continue;
                    }

                    while(temp != 0){					//find the first different bit of node.key and value
                        temp >>>=  1;
                        i++;
                    }

                    newKey = key_i >>> i << i; 			//set leftmost (i-1) bits of newKey
                    long mask = one << (i-1);
                    newKey = newKey | mask; 			// set bit i of newKey to one
                    newMask = allOnes >>> i << i;


                    // create new Internal node whose children are a new leaf and nodeSibling
                    Node<V> newInternal;
                    Long kNewKey = new Long(newKey);
                    newInternal =(newKey > key_i) ? new Node<V>(kNewKey, newMask, new Node<V>(key_i, value_i), nodeSibling_d)
                            : new Node<V>(kNewKey, newMask, nodeSibling_d, new Node<V>(key_i, value_i));

                    if(gp_d.key < p_d.key) //order gp_d and p_d to get flagged
                        movInfo = new Flag<V>(gp_d, p_d, null, null, gp_d, null, gpInfo_d, pInfo_d, null, null,
                                gp_d, p_d, newInternal, null, null, null, null);
                    else
                        movInfo = new Flag<V>(p_d, gp_d, null, null, gp_d, null, pInfo_d, gpInfo_d, null, null,
                                gp_d, p_d, newInternal, null, null, null, null);

                }//end of special case 2 and 3

                //special case 4
                else if(node_i == gp_d){

                    Node<V> pSibling_d = (gp_d.key > p_d.key) ? gp_d.right : gp_d.left;

                    long newKey = 0;
                    long newMask = 0;

                    long temp = nodeSibling_d.key ^ pSibling_d.key;
                    byte i = 0;

                    if (temp==0) {//children are not prefixes of each other
                        System.out.println("children are not prefixes of each other inside move");
                        continue;
                    }

                    while(temp != 0){					//find the first different bit of node.key and value
                        temp >>>=  1;
                        i++;
                    }

                    newKey = nodeSibling_d.key >>> i << i; 			//set leftmost (i-1) bits of newKey
                    long mask = one << (i-1);
                    newKey = newKey | mask; 			// set bit i of newKey to one
                    newMask = allOnes >>> i << i;


                    // create new Internal node whose children are nodeSibling and pSibling
                    Node<V> newChild;
                    Long kNewKey = new Long(newKey);
                    newChild =(newKey > pSibling_d.key) ? new Node<V>(kNewKey, newMask, pSibling_d, nodeSibling_d)
                            : new Node<V>(kNewKey, newMask, nodeSibling_d, pSibling_d);

                    newKey = 0;
                    newMask = 0;

                    temp = key_i ^ newChild.key;
                    i = 0;

                    if (temp==0) {//children are not prefixes of each other
                        System.out.println("children are not prefixes of each other inside move");
                        continue;
                    }

                    while(temp != 0){					//find the first different bit of node.key and value
                        temp >>>=  1;
                        i++;
                    }

                    newKey = key_i >>> i << i; 			//set leftmost (i-1) bits of newKey
                    mask = one << (i-1);
                    newKey = newKey | mask; 			// set bit i of newKey to one
                    newMask = allOnes >>> i << i;


                    // create new Internal node whose children are a new copy of node and a new node
                    Node<V> newInternal;
                    kNewKey = new Long(newKey);
                    newInternal =(newKey > key_i) ? new Node<V>(kNewKey, newMask, new Node<V>(key_i, value_i), newChild)
                            : new Node<V>(kNewKey, newMask, newChild, new Node<V>(key_i, value_i));

                    if(gp_d.key < p_d.key){ //order gp_d, p_d and p_i to get flagged
                        if(p_i.key < gp_d.key)
                            movInfo = new Flag<V>(p_i, gp_d, p_d, null, p_i, null, pInfo_i, gpInfo_d, pInfo_d, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                        else if (p_d.key < p_i.key)
                            movInfo = new Flag<V>(gp_d, p_d, p_i, null, p_i, null, gpInfo_d, pInfo_d, pInfo_i, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                        else
                            movInfo = new Flag<V>(gp_d, p_i, p_d, null, p_i, null, gpInfo_d, pInfo_i, pInfo_d, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                    }
                    else{
                        if(p_i.key < p_d.key)
                            movInfo = new Flag<V>(p_i, p_d, gp_d, null, p_i, null, pInfo_i, pInfo_d, gpInfo_d, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                        else if (gp_d.key < p_i.key)
                            movInfo = new Flag<V>(p_d, gp_d, p_i, null, p_i, null, pInfo_d, gpInfo_d, pInfo_i, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                        else
                            movInfo = new Flag<V>(p_d, p_i, gp_d, null, p_i, null, pInfo_d, pInfo_i, gpInfo_d, null,
                                    p_i, node_i, newInternal, null, null, null, null);
                    }
                }//end of special case 4

                else if((gp_d != null) && (node_i != node_d) && (node_i != p_d) && (node_i != gp_d) && (p_i != p_d )){ //general cases
                    long newKey = 0;
                    long newMask = 0;

                    long temp = node_i.key ^ key_i;
                    byte i = 0;

                    if (temp==0) {//children are not prefixes of each other
                        System.out.println("children are not prefixes of each other inside move");
                        continue;
                    }

                    while(temp != 0){					//find the first different bit of node.key and value
                        temp >>>=  1;
                        i++;
                    }

                    newKey = key_i >>> i << i; 			//set leftmost (i-1) bits of newKey
                    long mask = one << (i-1);
                    newKey = newKey | mask; 			// set bit i of newKey to one
                    newMask = allOnes >>> i << i;


                    // create new Internal node whose children are a new copy of node and a new node
                    Node<V> newInternal;
                    Long kNewKey = new Long(newKey);
                    newInternal =(newKey > key_i) ? new Node<V>(kNewKey, newMask, new Node<V>(key_i, value_i), new Node<V>(node_i))
                            : new Node<V>(kNewKey, newMask, new Node<V>(node_i), new Node<V>(key_i, value_i));


                    //general case 1
                    if((node_i.left!=null)&&(gp_d!=p_i)){ //node_i is internal

                        if(gp_d.key < p_d.key){ //order gp_d, p_d, p_i and node_i to get flagged
                            if(p_i.key < gp_d.key){
                                if(node_i.key < p_i.key)
                                    movInfo = new Flag<V>(node_i, p_i, gp_d, p_d, gp_d, p_i, nodeInfo_i, pInfo_i, gpInfo_d, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(p_i, node_i, gp_d, p_d, gp_d, p_i, pInfo_i, nodeInfo_i, gpInfo_d, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_d.key)
                                    movInfo = new Flag<V>(p_i, gp_d, node_i, p_d, gp_d, p_i, pInfo_i, gpInfo_d, nodeInfo_i, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(p_i, gp_d, p_d, node_i, gp_d, p_i, pInfo_i, gpInfo_d, pInfo_d, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }
                            else if (p_d.key < p_i.key){
                                if(node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(node_i, gp_d, p_d, p_i, gp_d, p_i, nodeInfo_i, gpInfo_d, pInfo_d, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_d.key)
                                    movInfo = new Flag<V>(gp_d, node_i, p_d, p_i, gp_d, p_i, gpInfo_d, nodeInfo_i, pInfo_d, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_i.key)
                                    movInfo = new Flag<V>(gp_d, p_d, node_i, p_i, gp_d, p_i, gpInfo_d, pInfo_d, nodeInfo_i, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(gp_d, p_d, p_i, node_i, gp_d, p_i, gpInfo_d, pInfo_d, pInfo_i, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }
                            else{
                                if(node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(node_i, gp_d, p_i, p_d, gp_d, p_i, nodeInfo_i, gpInfo_d, pInfo_i, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_i.key)
                                    movInfo = new Flag<V>(gp_d, node_i, p_i, p_d, gp_d, p_i, gpInfo_d, nodeInfo_i, pInfo_i, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_d.key)
                                    movInfo = new Flag<V>(gp_d, p_i, node_i, p_d, gp_d, p_i, gpInfo_d, pInfo_i, nodeInfo_i, pInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(gp_d, p_i, p_d, node_i, gp_d, p_i, gpInfo_d, pInfo_i, pInfo_d, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }

                        }// gp_d<p_d
                        else{     //p_d<gp_d
                            if(p_i.key < p_d.key){
                                if(node_i.key < p_i.key)
                                    movInfo = new Flag<V>(node_i, p_i, p_d, gp_d, gp_d, p_i, nodeInfo_i, pInfo_i, pInfo_d, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_d.key)
                                    movInfo = new Flag<V>(p_i, node_i, p_d, gp_d, gp_d, p_i, pInfo_i, nodeInfo_i, pInfo_d, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(p_i, p_d, node_i, gp_d, gp_d, p_i, pInfo_i, pInfo_d, nodeInfo_i, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(p_i, p_d, gp_d, node_i, gp_d, p_i, pInfo_i, pInfo_d, gpInfo_d, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }

                            else if (gp_d.key < p_i.key){
                                if(node_i.key < p_d.key)
                                    movInfo = new Flag<V>(node_i, p_d, gp_d, p_i, gp_d, p_i, nodeInfo_i, pInfo_d, gpInfo_d, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(p_d, node_i, gp_d, p_i, gp_d, p_i, pInfo_d, nodeInfo_i, gpInfo_d, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_i.key)
                                    movInfo = new Flag<V>(p_d, gp_d, node_i, p_i, gp_d, p_i, pInfo_d, gpInfo_d, nodeInfo_i, pInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(p_d, gp_d, p_i, node_i, gp_d, p_i, pInfo_d, gpInfo_d, pInfo_i, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }

                            else{
                                if(node_i.key < p_d.key)
                                    movInfo = new Flag<V>(node_i, p_d, p_i, gp_d, gp_d, p_i, nodeInfo_i, pInfo_d, pInfo_i, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < p_i.key)
                                    movInfo = new Flag<V>(p_d, node_i, p_i, gp_d, gp_d, p_i, pInfo_d, nodeInfo_i, pInfo_i, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else if (node_i.key < gp_d.key)
                                    movInfo = new Flag<V>(p_d, p_i, node_i, gp_d, gp_d, p_i, pInfo_d, pInfo_i, nodeInfo_i, gpInfo_d,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                                else
                                    movInfo = new Flag<V>(p_d, p_i, gp_d, node_i, gp_d, p_i, pInfo_d, pInfo_i, gpInfo_d, nodeInfo_i,
                                            p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            }
                        }//p_d<gp_d
                    }//end of general case 1

                    //general case 1b
                    if((node_i.left!=null)&&(gp_d==p_i)){ //order gp_d, p_d, node_i

                        if(gp_d.key < p_d.key){
                            if(node_i.key < gp_d.key)
                                movInfo = new Flag<V>(node_i, gp_d, p_d, null, gp_d, null, nodeInfo_i, gpInfo_d, pInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else if (p_d.key < node_i.key)
                                movInfo = new Flag<V>(gp_d, p_d, node_i, null, gp_d, null, gpInfo_d, pInfo_d, nodeInfo_i, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else
                                movInfo = new Flag<V>(gp_d, node_i, p_d, null, gp_d, null, gpInfo_d, nodeInfo_i, pInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                        }
                        else{
                            if(node_i.key < p_d.key)
                                movInfo = new Flag<V>(node_i, p_d, gp_d, null, gp_d, null, nodeInfo_i, pInfo_d, gpInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else if (gp_d.key < node_i.key)
                                movInfo = new Flag<V>(p_d, gp_d, node_i, null, gp_d, null, pInfo_d, gpInfo_d, nodeInfo_i, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else
                                movInfo = new Flag<V>(p_d, node_i, gp_d, null, gp_d, null, pInfo_d, nodeInfo_i, gpInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                        }
                    }//end of general case 1b

                    //general case 2
                    if((node_i.left==null)&&(gp_d!=p_i)){	//node_i is a leaf node

                        if(gp_d.key < p_d.key){ // order gp_d, p_d and p_i to get flagged
                            if(p_i.key < gp_d.key)
                                movInfo = new Flag<V>(p_i, gp_d, p_d, null, gp_d, p_i, pInfo_i, gpInfo_d, pInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else if (p_d.key < p_i.key)
                                movInfo = new Flag<V>(gp_d, p_d, p_i, null, gp_d, p_i, gpInfo_d, pInfo_d, pInfo_i, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else
                                movInfo = new Flag<V>(gp_d, p_i, p_d, null, gp_d, p_i, gpInfo_d, pInfo_i, pInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                        }
                        else{
                            if(p_i.key < p_d.key)
                                movInfo = new Flag<V>(p_i, p_d, gp_d, null, gp_d, p_i, pInfo_i, pInfo_d, gpInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else if (gp_d.key < p_i.key)
                                movInfo = new Flag<V>(p_d, gp_d, p_i, null, gp_d, p_i, pInfo_d, gpInfo_d, pInfo_i, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                            else
                                movInfo = new Flag<V>(p_d, p_i, gp_d, null, gp_d, p_i, pInfo_d, pInfo_i, gpInfo_d, null,
                                        p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                        }
                    }//end of general case 2

                    //general case 2b
                    if((node_i.left==null)&&(gp_d==p_i)){ //order gp_d and p_d to get flagged

                        if(gp_d.key < p_d.key)
                            movInfo = new Flag<V>(gp_d, p_d, null, null, gp_d, null, gpInfo_d, pInfo_d, null, null,
                                    p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                        else
                            movInfo = new Flag<V>(p_d, gp_d, null, null, gp_d, null, pInfo_d, gpInfo_d, null, null,
                                    p_i, node_i, newInternal, gp_d, p_d, nodeSibling_d, node_d);
                    }//end of general case 2b
                }

                if((movInfo!=null)&&help(movInfo)) return key_d;
            }//end of creating Flag object



        }//end of while loop
    }//end of move operation

    /**
     * The help routine is called to complete an update operation.
     *
     */

    private boolean help(Flag<V> info){

        boolean doChildCAS = true;

        // flagging internal nodes
        infoUpdater.compareAndSet(info.flagNode1, info.oldInfo1, info);
        doChildCAS = (info.flagNode1.info == info);

        if((doChildCAS) && (info.flagNode2!=null)){
            infoUpdater.compareAndSet(info.flagNode2, info.oldInfo2, info);
            doChildCAS = (info.flagNode2.info == info);
        }

        if((doChildCAS) && (info.flagNode3!=null)){
            infoUpdater.compareAndSet(info.flagNode3, info.oldInfo3, info);
            doChildCAS = (info.flagNode3.info == info);
        }

        if((doChildCAS) && (info.flagNode4!=null)){
            infoUpdater.compareAndSet(info.flagNode4, info.oldInfo4, info);
            doChildCAS = (info.flagNode4.info == info);
        }

        //if successfully flag all internals
        if(doChildCAS){
            info.flagDone = true;
            if(info.movLeaf != null) info.movLeaf.info = info; //flag the leaf node in Move operations
            ((info.pNode1.left == info.oldChild1) ? leftUpdater : rightUpdater).compareAndSet(info.pNode1, info.oldChild1, info.newChild1);
            if(info.pNode2 != null)
                ((info.pNode2.left == info.oldChild2) ? leftUpdater : rightUpdater).compareAndSet(info.pNode2, info.oldChild2, info.newChild2);
        }

        // unflag internals
        if(info.flagDone){
            infoUpdater.compareAndSet(info.unflagNode1, info, new Unflag<V>());
            if(info.unflagNode2 != null) infoUpdater.compareAndSet(info.unflagNode2, info, new Unflag<V>());
            return true;
        }
        else{ //backtracking
            if(info.flagNode4 != null) infoUpdater.compareAndSet(info.flagNode4, info, new Unflag<V>());
            if(info.flagNode3 != null) infoUpdater.compareAndSet(info.flagNode3, info, new Unflag<V>());
            if(info.flagNode2 != null) infoUpdater.compareAndSet(info.flagNode2, info, new Unflag<V>());
            infoUpdater.compareAndSet(info.flagNode1, info, new Unflag());
            return false;
        }
    }

}