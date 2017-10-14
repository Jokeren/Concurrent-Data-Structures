package com.jokeren.concurrent.structures.quadtree;

import com.jokeren.concurrent.structures.miscellaneous.QuadtreeMisc;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/15.
 */
public class QuadStackLCAPure<V> implements Quadtree<V>, QuadtreeMisc {
    final private static AtomicReferenceFieldUpdater<Internal, Node> nwUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "nw");
    final private static AtomicReferenceFieldUpdater<Internal, Node> neUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "ne");
    final private static AtomicReferenceFieldUpdater<Internal, Node> swUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "sw");
    final private static AtomicReferenceFieldUpdater<Internal, Node> seUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "se");
    final private static AtomicReferenceFieldUpdater<Internal, Operation> opUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Operation.class, "op");

    final private static ThreadLocal<Trace> iTrace = new ThreadLocal<Trace>() {
        @Override
        protected Trace initialValue() {
            return new Trace();
        }
    };
    final private static ThreadLocal<Trace> dTrace = new ThreadLocal<Trace>() {
        @Override
        protected Trace initialValue() {
            return new Trace();
        }
    };
    final private static ThreadLocal<Trace> cTrace = new ThreadLocal<Trace>() {
        @Override
        protected Trace initialValue() {
            return new Trace();
        }
    };
    final private Empty<V> empty = new Empty<>();

    final static Logger logger = Logger.getLogger("QuadZeroStackLazy");

    final private Internal root;

    public QuadStackLCAPure(double w, double h) {
        root = new Internal<V>(0.0f, 0.0f, w, h);
        split();
    }

    public QuadStackLCAPure() {

        this(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private void split() {
        root.nw = new Internal<V>(root.x, root.y, root.w / 2, root.h / 2);
        root.ne = new Internal<V>(root.x + root.w / 2, root.y, root.w / 2, root.h / 2);
        root.sw = new Internal<V>(root.x, root.y + root.h / 2, root.w / 2, root.h / 2);
        root.se = new Internal<V>(root.x + root.w / 2, root.y + root.h / 2, root.w / 2, root.h / 2);
        Internal nw = (Internal) root.nw, ne = (Internal) root.ne, sw = (Internal) root.sw, se = (Internal) root.se;
        nw.nw = new Empty<>();
        nw.ne = new Empty<>();
        nw.sw = new Empty<>();
        nw.se = new Empty<>();
        ne.nw = new Empty<>();
        ne.ne = new Empty<>();
        ne.sw = new Empty<>();
        ne.se = new Empty<>();
        sw.nw = new Empty<>();
        sw.ne = new Empty<>();
        sw.sw = new Empty<>();
        sw.se = new Empty<>();
        se.nw = new Empty<>();
        se.ne = new Empty<>();
        se.sw = new Empty<>();
        se.se = new Empty<>();
    }

    private Internal split(Leaf node, double x, double y, double w, double h) {
        boolean nw = false, ne = false, sw = false, se = false;
        if (node.keyX < x + w / 2) {
            if (node.keyY < y + h / 2) {
                nw = true;
            } else {
                sw = true;
            }
        } else {
            if (node.keyY < y + h / 2) {
                ne = true;
            } else {
                se = true;
            }
        }

        Internal internal = new Internal<V>(x, y, w, h);
        if (nw) {
            internal.nw = node;
        } else {
            internal.nw = new Empty<>();
        }

        if (ne) {
            internal.ne = node;
        } else {
            internal.ne = new Empty<>();
        }

        if (sw) {
            internal.sw = node;
        } else {
            internal.sw = new Empty<>();
        }

        if (se) {
            internal.se = node;
        } else {
            internal.se = new Empty<>();
        }

        return internal;
    }

    private Node<V> getQuadrant(Internal parent, double keyX, double keyY, int[] direction) {
        if (keyX < parent.x + parent.w / 2) {
            if (keyY < parent.y + parent.h / 2) {
                direction[0] = 0;
                return parent.nw;
            } else {
                direction[0] = 2;
                return parent.sw;
            }
        } else {
            if (keyY < parent.y + parent.h / 2) {
                direction[0] = 1;
                return parent.ne;
            } else {
                direction[0] = 3;
                return parent.se;
            }
        }
    }

    private void getQuadrantDirection(Internal parent, double keyX, double keyY, int[] direction) {
        if (keyX < parent.x + parent.w / 2) {
            if (keyY < parent.y + parent.h / 2) {
                direction[0] = 0;
            } else {
                direction[0] = 2;
            }
        } else {
            if (keyY < parent.y + parent.h / 2) {
                direction[0] = 1;
            } else {
                direction[0] = 3;
            }
        }
    }

    private static class Record {
        Node node;
        int prevDirection;

        public Record() {
            this(null, 0);
        }

        public Record(Node node, int prevDirection) {
            this.node = node;
            this.prevDirection = prevDirection;
        }
    }

    /**
     * an optimized stack for storing and retrieve record efficiently
     * almost zero-copy, since the length is bounded by the tree height
     * 2^2048 for double type
     */
    private static class Trace {
        int curIdx;
        Record[] records;

        public Trace() {
            this(128);
        }

        public Trace(int size) {
            curIdx = 0;
            records = new Record[size];
            for (int i = 0; i < records.length; ++i) {
                records[i] = new Record();//dummy node, 1-copy
            }
        }

        void resize(int newSize) {
            if (newSize < curIdx) {
                return;
            }
            logger.info("newSize : " + newSize);
            records = Arrays.copyOf(records, newSize);
            for (int i = curIdx; i < records.length; ++i) {
                records[i] = new Record();
            }
        }

        void push(Node node, int prevDirection) {
            if (curIdx == records.length) {//2x growth
                resize(curIdx * 2);
            }

            records[curIdx].node = node;
            records[curIdx].prevDirection = prevDirection;
            ++curIdx;
        }

        Node peekNode() {
            if (curIdx == 0) {
                throw new ArrayIndexOutOfBoundsException();
            }

            return records[curIdx - 1].node;
        }

        int peekDirection() {
            if (curIdx == 0) {
                throw new ArrayIndexOutOfBoundsException();
            }

            return records[curIdx - 1].prevDirection;
        }

        void pop() {
            if (curIdx == 0) {
                throw new ArrayIndexOutOfBoundsException();
            }

            --curIdx;
        }

        void clear() {
            curIdx = 0;
        }

        boolean empty() {
            return curIdx == 0 ? true : false;
        }

        int getCurIdx() {
            return curIdx;
        }

        void setCurIdx(int curIdx) {
            this.curIdx = curIdx;
        }

        Node getNode(int index) {
            return records[index].node;
        }
    }

    private static class Node<V> {

    }

    private final static class Internal<V> extends Node<V> {
        final double x, y, w, h;
        volatile Node nw, ne, sw, se;
        volatile Operation op = new Clean();

        public Internal(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    private final static class Leaf<V> extends Node<V> {
        final double keyX, keyY;//double for simplicity
        final V value;
        volatile Move move;
        //different from patricia, do not need flag

        public Leaf(double keyX, double keyY, V value) {
            this.keyX = keyX;
            this.keyY = keyY;
            this.value = value;
        }

        public void setMove(Move move) {
            this.move = move;
        }
    }

    private final static class Empty<V> extends Node<V> {

    }

    private static class Operation {

    }

    private final static class Substitute extends Operation {
        Internal parent;
        Node oldChild, newNode;
        int prevDirection;

        public Substitute(Internal parent, Node oldChild, Node newNode, int prevDirection) {
            this.parent = parent;
            this.oldChild = oldChild;
            this.newNode = newNode;
            this.prevDirection = prevDirection;
        }
    }

    private final static class Compress extends Operation {
        Internal parent;
        Node oldChild;
        int prevDirection;

        public Compress(Internal parent, Node oldChild, int prevDirection) {
            this.parent = parent;
            this.oldChild = oldChild;
            this.prevDirection = prevDirection;
        }
    }

    private final static class Move extends Operation {
        Internal iParent, dParent;
        Node oldIChild, oldDChild, newIChild;
        int prevIDirection, prevDDirection;
        Operation iOldOp, dOldOp;
        volatile boolean iFlag = false;
        int order = 1;

        public Move(Internal iParent, Internal dParent, Node oldIChild, Node oldDChild, Node newIChild,
                    int prevIDirection, int prevDDirection, Operation iOldOp, Operation dOldOp) {
            this.iParent = iParent;
            this.dParent = dParent;
            this.oldIChild = oldIChild;
            this.oldDChild = oldDChild;
            this.newIChild = newIChild;
            this.prevIDirection = prevIDirection;
            this.prevDDirection = prevDDirection;
            this.iOldOp = iOldOp;
            this.dOldOp = dOldOp;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    private final static class Clean extends Operation {

    }

    private void help(Operation op) {
        if (op.getClass() == Substitute.class) {//Replace
            helpSubstitute((Substitute) op);

        }
        else if (op.getClass() == Compress.class) {//Compress
            helpCompress((Compress) op);
        }
        else if (op.getClass() == Move.class) {//Move
            helpMove((Move) op);
        }//Clean
    }

    private boolean helpCheck(Internal node) {
        return node.nw.getClass() == Empty.class && node.ne.getClass() == Empty.class &&
                node.sw.getClass() == Empty.class && node.se.getClass() == Empty.class;
    }

    private boolean helpFlag(Internal node, Operation oldOp, Operation newOp) {
        return opUpdater.compareAndSet(node, oldOp, newOp);
    }

    private void helpSubstitute(Substitute op) {
        helpReplace(op.parent, op.oldChild, op.newNode, op.prevDirection);
        helpFlag(op.parent, op, new Clean());
    }

    private boolean helpCompress(Compress op) {
        return helpReplace(op.parent, op.oldChild, new Empty<V>(), op.prevDirection);
    }

    private boolean helpMove(Move op) {
        boolean doCAS = false;
        if (op.order == 1) {
            helpFlag(op.iParent, op.iOldOp, op);
            if (op.iParent.op == op) doCAS = true;
//            else help(op.iParent.op);
        } else {
            doCAS = helpFlag(op.dParent, op.dOldOp, op);
            if (op.dParent.op == op) doCAS = true;
//            else help(op.dParent.op);
        }

        if (doCAS) {
            op.iFlag = true;
            Leaf dChild = (Leaf) op.oldDChild;
            dChild.setMove(op);
            if (op.oldDChild == op.oldIChild) {
                helpReplace(op.dParent, op.oldDChild, op.newIChild, op.prevDDirection);
            } else {
                //insert node
                helpReplace(op.iParent, op.oldIChild, op.newIChild, op.prevIDirection);
                //delete node
                helpReplace(op.dParent, op.oldDChild, new Empty<V>(), op.prevDDirection);
            }
        }

        if (op.order == 0) {
            if (op.iFlag) helpFlag(op.dParent, op, new Clean());
            if (op.dParent != op.iParent) helpFlag(op.iParent, op, new Clean());
        } else {
            if (op.iFlag) helpFlag(op.iParent, op, new Clean());
            if (op.dParent != op.iParent) helpFlag(op.dParent, op, new Clean());
        }
        return op.iFlag;
    }

    private boolean helpReplace(Internal parent, Node oldChild, Node newChild, int prevDirection) {
//        boolean value = false;
        if (parent.nw == oldChild) {
            return nwUpdater.compareAndSet(parent, oldChild, newChild);

        } else if (parent.ne == oldChild) {
            return neUpdater.compareAndSet(parent, oldChild, newChild);

        } else if (parent.sw == oldChild) {
            return swUpdater.compareAndSet(parent, oldChild, newChild);

        } else if (parent.se == oldChild) {
            return seUpdater.compareAndSet(parent, oldChild, newChild);

        }
        return false;
    }

    private void recursiveCompress(Internal p, int prevDirection, Trace visited, Record record) {
        Operation pOp = null;
        Internal gp = null;
        while (true) {
            pOp = p.op;
            if (pOp.getClass() == Clean.class) {
                record.node = visited.peekNode();
                visited.pop();
                gp = (Internal) record.node;

                if (gp == root) {//if root, not compress
                    return;
                }

                Operation newOp = new Compress(gp, p, 0);
                if (!helpCheck(p)) {
                    return;
                }
                if (!helpFlag(p, pOp, newOp)) {
                    return;
                }
                helpCompress((Compress) newOp);
                p = gp;
            } else {//do not help, as the same operation could be done in recursive help
                return;
            }
        }
    }

    private Node<V> createNode(Leaf child, double x, double y, double w, double h,
                               double keyX, double keyY, V value, int[] direction) {
        w /= 2.0f;
        h /= 2.0f;
        if (keyX < x + w) {
            if (keyY < y + h) {
            } else {
                y = y + h;
            }
        } else {
            if (keyY < y + h) {
                x = x + w;
            } else {
                x = x + w;
                y = y + h;
            }
        }

        Internal internal = split(child, x, y, w, h);
        Internal result = internal;
        Internal prevNode = null;
        Node candidate = getQuadrant(internal, keyX, keyY, direction);

//        logger.info("ka here1");
        while (candidate.getClass() == Leaf.class) {
            w /= 2.0f;
            h /= 2.0f;
            prevNode = internal;
            switch (direction[0]) {
                case 0:
                    internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.nw = internal;
                    break;
                case 1:
                    x = x + w;
                    internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.ne = internal;
                    break;
                case 2:
                    y = y + h;
                    internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.sw = internal;
                    break;
                case 3:
                    x = x + w;
                    y = y + h;
                    internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.se = internal;
                    break;
                default:
                    break;

            }
            candidate = getQuadrant(internal, keyX, keyY, direction);
        }
//        logger.info("ka here2");

        Leaf leaf = new Leaf<V>(keyX, keyY, value);
        switch (direction[0]) {
            case 0:
                internal.nw = leaf;
                break;
            case 1:
                internal.ne = leaf;
                break;
            case 2:
                internal.sw = leaf;
                break;
            case 3:
                internal.se = leaf;
                break;
            default:
                break;
        }

        return result;
    }

    private static boolean hasChild(Internal parent, Node oldChild) {
        if (parent.nw == oldChild || parent.ne == oldChild || parent.sw == oldChild || parent.se == oldChild) {
            return true;
        }
        return  false;
    }

    @Override
    public boolean insert(double keyX, double keyY, V value) {
        Node l = root;
        Internal parent = null;
        Leaf child = null;
        Operation pOp = null;
        Trace visited = iTrace.get();
        Record record = new Record();
        visited.clear();
        int [] direction = new int[1];

        while (l.getClass() == Internal.class) {
            //TODO:it can be optimized, as the insert operation doesn't need direction records
            //FIXME:some errors, the root still maintains a direction, but it doesn't affect the correctness
            visited.push((Internal) l, direction[0]);
            pOp = ((Internal)l).op;
            l = getQuadrant((Internal) l, keyX, keyY, direction);
        }

        while (true) {
            parent = (Internal) visited.peekNode();
            record.prevDirection = visited.peekDirection();
            visited.pop();
            child = null;
            if (l.getClass() == Leaf.class) {
                child = new Leaf(((Leaf)l).keyX, ((Leaf)l).keyY, ((Leaf)l).value);
                Leaf liChild = (Leaf)child;
                boolean inTree = liChild.keyX == keyX && liChild.keyY == keyY;
                boolean logicalRemove = liChild.move != null && !hasChild(liChild.move.iParent, liChild.move.oldIChild);
                if (inTree && !logicalRemove) {//if exist, return false
                    return false;
                }
            }

            int prevDirection = direction[0];
            if (pOp.getClass() == Clean.class) {
                Node newNode = null;
                if (child == null) {//terminal node is empty, therefore create a leaf node
                    newNode = new Leaf<V>(keyX, keyY, value);
                } else {//terminal node is leaf, therefore split it
                    newNode = createNode(child, parent.x, parent.y, parent.w, parent.h, keyX, keyY, value, direction);
                }

                Operation newOp = new Substitute(parent, l, newNode, prevDirection);

                if (helpFlag(parent, pOp, newOp)) {
                    helpSubstitute((Substitute) newOp);
                    return true;
                } else {
                    pOp = parent.op;
                }
            }

            help(pOp);
            if (pOp.getClass() != Compress.class) {
                l = parent;
                direction[0] = record.prevDirection;
            } else {
                while (!visited.empty()) {
                    l = visited.peekNode();
                    direction[0] = visited.peekDirection();
                    visited.pop();
                    pOp = ((Internal)l).op;
                    if (pOp.getClass() == Compress.class) {//if not compress, it can move down
                        help(pOp);
                    } else {
                        break;
                    }
                }
            }

            while (l.getClass() == Internal.class) {
                visited.push(l, direction[0]);
                pOp = ((Internal)l).op;
                l = getQuadrant((Internal) l, keyX, keyY, direction);
            }
        }
    }

    @Override
    public boolean remove(double keyX, double keyY) {
        Node l = root;
        Internal parent = null;
        Leaf child = null;
        Operation pOp = null;
        Trace visited = dTrace.get();
        visited.clear();
        Record record = new Record();

        int[] direction = new int[1];

        //route to leaf or empty node
        while (l.getClass() == Internal.class) {
            visited.push((Internal) l, direction[0]);
            pOp = ((Internal) l).op;
            l = getQuadrant((Internal) l, keyX, keyY, direction);
        }

        while (true) {
            record.node = visited.peekNode();
            record.prevDirection = visited.peekDirection();
            parent = (Internal) record.node;
            visited.pop();
            if (l.getClass() == Leaf.class) {
                child = (Leaf) l;
                boolean inTree = child.keyX == keyX && child.keyY == keyY;
                boolean logicalRemove = child.move != null && !hasChild(child.move.iParent, child.move.oldIChild);
                if (!inTree || (inTree && logicalRemove)) {//if not exist, return false
                    return false;
                }
            } else {//if empty node
                return false;
            }

            int prevDirection = direction[0];
            if (pOp.getClass() == Clean.class) {
                Operation newOp = new Substitute(parent, l, new Empty<V>(), prevDirection);

                if (helpFlag(parent, pOp, newOp)) {
                    helpSubstitute((Substitute) newOp);
                    recursiveCompress(parent, record.prevDirection, visited, record);
                    return true;
                } else {
                    pOp = parent.op;
                }
            }

            //TODO:optimize, reduce number of statements
            help(pOp);
            if (pOp.getClass() != Compress.class) {
                l = parent;
                direction[0] = record.prevDirection;
            } else {
                while (!visited.empty()) {
                    l = visited.peekNode();
                    direction[0] = visited.peekDirection();
                    visited.pop();
                    pOp = ((Internal)l).op;
                    if (pOp.getClass() == Compress.class) {//if not compress, it can move down
                        help(pOp);
                    } else {
                        break;
                    }
                }
            }
            while (l.getClass() == Internal.class) {
                visited.push(l, direction[0]);
                pOp = ((Internal) l).op;
                l = getQuadrant((Internal) l, keyX, keyY, direction);
            }
        }
    }

    @Override
    public boolean contains(double keyX, double keyY) {
        Node l = root;
        int[] direction = new int[1];
        while (l.getClass() == Internal.class) {
            l = getQuadrant((Internal) l, keyX, keyY, direction);
        }

        if (l.getClass() == Leaf.class) {
            Leaf leaf = (Leaf) l;
            if (leaf.keyX == keyX && leaf.keyY == keyY) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean move(double oldKeyX, double oldKeyY, double newKeyX, double newKeyY) {
        //locate the delete node
        Node dl = root, il = null;
        Internal iParent, dParent;
        int lca = 0;
        Operation dPop = null;
        Operation iPop = null;
        Trace dVisited = dTrace.get();
        dVisited.clear();
        Trace iVisited = iTrace.get();
        iVisited.clear();
        int[] iDirection = new int[1];
        int[] dDirection = new int[1];

        while (dl.getClass() == Internal.class) {
            dVisited.push(dl, 0);
            dPop = ((Internal) dl).op;
            getQuadrantDirection((Internal) dl, newKeyX, newKeyY, iDirection);//Optimization
            dl = getQuadrant((Internal) dl, oldKeyX, oldKeyY, dDirection);
            if (dDirection[0] != iDirection[0]) {
                break;
            }
        }
        lca = dVisited.getCurIdx();
        iPop = dPop;

        //locate the remove node
        while (dl.getClass() == Internal.class) {
            dVisited.push(dl, 0);
            dPop = ((Internal) dl).op;
            dl = getQuadrant((Internal) dl, oldKeyX, oldKeyY, dDirection);
        }

        Leaf dChild = null;
        if (dl.getClass() == Leaf.class) {
            dChild = (Leaf) dl;
            boolean inTree = dChild.keyX == newKeyX && dChild.keyY == newKeyY;
            boolean logicalRemove = dChild.move != null && !hasChild(dChild.move.iParent, dChild.move.oldIChild);
            if (!inTree || (inTree && logicalRemove)) {//if not exist, return false
                return false;
            }
        } else {//if empty node
            return false;
        }
        dParent = (Internal) dVisited.peekNode();
        dVisited.pop();

        //il must be equal to the lca node
        il = dVisited.getNode(lca - 1);
        while (il.getClass() == Internal.class) {
            iVisited.push(il, 0);
            iPop = ((Internal) il).op;
            il = getQuadrant((Internal) il, newKeyX, newKeyY, iDirection);
        }
        iParent = (Internal) iVisited.peekNode();
        iVisited.pop();

        Leaf newIChild = null;
        Node oldIChild = il;
        if (il.getClass() == Leaf.class) {
            newIChild = new Leaf(((Leaf)il).keyX, ((Leaf)il).keyY, ((Leaf)il).value);
            Leaf liChild = (Leaf) oldIChild;
            boolean inTree = liChild.keyX == newKeyX && liChild.keyY == newKeyY;
            boolean logicalRemove = liChild.move != null && !hasChild(liChild.move.iParent, liChild.move.oldIChild);
            if (inTree && !logicalRemove) {//if exist, return false
                return false;
            }
        }

        Node newNode = null;
        boolean iFail = false;
        boolean dFail = false;
        boolean cFail = false;

        while (true) {
            if (dPop.getClass() != Clean.class) {
                dFail = true;
            }
            if (iPop.getClass() != Clean.class) {
                iFail = true;
            }
            if (iPop != dPop && iParent == dParent) {
                cFail = true;
            }
            if (!(iFail || dFail || cFail)) {
                if (oldIChild.getClass() == Empty.class || oldIChild == dChild) {
                    newNode = new Leaf<V>(newKeyX, newKeyY, (V) dChild.value);
                } else {
                    newNode = createNode((Leaf) newIChild, iParent.x, iParent.y, iParent.w, iParent.h,
                            newKeyX, newKeyY, (V) dChild.value, iDirection);
                }

                Operation move = new Move(iParent, dParent, oldIChild, dChild, newNode,
                        0, 0, iPop, dPop);

                if (dParent != iParent) {
                    int order = 0;
                    if (iParent.x > dParent.x) {
                        order = 0;
                    } else {
                        if (iParent.x == dParent.x) {
                            if (iParent.y > dParent.y) {
                                order = 0;
                            } else {
                                if (iParent.y == dParent.y) {
                                    if (iParent.h >= dParent.h) {
                                        order = 0;
                                    } else {
                                        order = 1;
                                    }
                                } else {
                                    order = 1;
                                }
                            }
                        } else {
                            order = 1;
                        }
                    }

                    ((Move)move).setOrder(order);
                    boolean condi = false;
                    if (order == 0) {
                        condi = helpFlag(iParent, iPop, move);
                    }  else {
                        condi = helpFlag(dParent, dPop, move);
                    }
                    if (condi) {
                        if (helpMove((Move) move)) {
                            recursiveCompress(dParent, 0, dVisited, new Record());
                            return true;
                        } else {
//                            dPop = dParent.op;
                            dFail = true;
                            iFail = true;
                        }
                    } else {
//                        cFail = true;
                        if (order == 1) {
                            dFail = true;
                            dPop = dParent.op;
                        } else {
                            iFail = true;
                            iPop = iParent.op;
                        }
                    }
                } else {//special, common parent
//                    return true;
                    if (helpMove((Move) move)) {
                        return true;
                    } else {
                        iPop = dPop = iParent.op;
                        cFail = true;
                    }
                }
            }

            if (dFail) {
                help(dPop);
                if (dPop.getClass() != Compress.class) {
                    dl = dParent;
                } else {
                    while (!dVisited.empty()) {
                        if (dVisited.getCurIdx() < lca) {
                            cFail = true;
                            break;
                        }
                        dl = dVisited.peekNode();
                        dVisited.pop();
                        dPop = ((Internal) dl).op;
                        if (dPop.getClass() == Compress.class) {
                            help(dPop);
                        } else {
                            break;
                        }
                    }
                }

                if (!cFail) {
                    while (dl.getClass() == Internal.class) {
                        dVisited.push(dl, 0);
                        dPop = ((Internal) dl).op;
                        dl = getQuadrant((Internal) dl, oldKeyX, oldKeyY, dDirection);
                    }

                    if (dl.getClass() == Leaf.class) {
                        dChild = (Leaf) dl;
                        boolean inTree = dChild.keyX == newKeyX && dChild.keyY == newKeyY;
                        boolean logicalRemove = dChild.move != null && !hasChild(dChild.move.iParent, dChild.move.oldIChild);
                        if (!inTree || (inTree && logicalRemove)) {//if not exist, return false
                            return false;
                        }
                    } else {//if empty node
                        return false;
                    }
                    dParent = ((Internal)dVisited.peekNode());
                    dVisited.pop();
                }
            }

            if (!cFail && iFail) {
                newNode = null;
                help(iPop);
                if (iPop.getClass() != Compress.class) {
                    il = iParent;
                } else {
                    while (!iVisited.empty()) {
                        il = iVisited.peekNode();
                        iVisited.pop();
                        iPop = ((Internal) il).op;
                        if (iPop.getClass() == Compress.class) {
                            help(iPop);
                        } else {
                            break;
                        }
                    }
                }

                if (iPop.getClass() == Compress.class) {
                    cFail = true;
                }

                if (!cFail) {
                    while (il.getClass() == Internal.class) {
                        iVisited.push(il, 0);
                        iPop = ((Internal) il).op;
                        il = getQuadrant((Internal) il, newKeyX, newKeyY, iDirection);
                    }
                    newIChild = null;
                    oldIChild = il;
                    if (il.getClass() == Leaf.class) {
                        newIChild = new Leaf(((Leaf)il).keyX, ((Leaf)il).keyY, ((Leaf)il).value);
                        Leaf liChild = (Leaf) oldIChild;
                        boolean inTree = liChild.keyX == newKeyX && liChild.keyY == newKeyY;
                        boolean logicalRemove = liChild.move != null && !hasChild(liChild.move.iParent, liChild.move.oldIChild);
                        if (inTree && !logicalRemove) {//if exist, return false
                            return false;
                        }
                    }
                    iParent = (Internal)iVisited.peekNode();
                    iVisited.pop();
                }
            }

            if (cFail) {
                help(iPop);
                help(dPop);
                dVisited.setCurIdx(lca);
                iVisited.clear();

                while (!dVisited.empty()) {//first time must be not empty
                    dl = dVisited.peekNode();
                    dVisited.pop();
                    dPop = ((Internal)dl).op;
                    if (dPop.getClass() == Compress.class) {
                        help(dPop);
                    } else {
                        break;
                    }
                }


                while (dl.getClass() == Internal.class) {
                    dVisited.push(dl, 0);
                    dPop = ((Internal) dl).op;
                    getQuadrantDirection((Internal) dl, newKeyX, newKeyY, iDirection);//Optimization
                    dl = getQuadrant((Internal) dl, oldKeyX, oldKeyY, dDirection);
                    if (dDirection[0] != iDirection[0]) {
                        break;
                    }
                }
                lca = dVisited.getCurIdx();
                iPop = dPop;

                //locate the remove node
                while (dl.getClass() == Internal.class) {
                    dVisited.push(dl, 0);
                    dPop = ((Internal) dl).op;
                    dl = getQuadrant((Internal) dl, oldKeyX, oldKeyY, dDirection);
                }

                dChild = null;
                if (dl.getClass() == Leaf.class) {
                    dChild = (Leaf) dl;
                    boolean inTree = dChild.keyX == newKeyX && dChild.keyY == newKeyY;
                    boolean logicalRemove = dChild.move != null && !hasChild(dChild.move.iParent, dChild.move.oldIChild);
                    if (!inTree || (inTree && logicalRemove)) {//if not exist, return false
                        return false;
                    }
                } else {//if empty node
                    return false;
                }
                dParent = (Internal) dVisited.peekNode();
                dVisited.pop();

                //il must be equal to the lca node
                il = dVisited.getNode(lca - 1);
                while (il.getClass() == Internal.class) {
                    iVisited.push(il, 0);
                    iPop = ((Internal) il).op;
                    il = getQuadrant((Internal) il, newKeyX, newKeyY, iDirection);
                }
                iParent = (Internal) iVisited.peekNode();
                iVisited.pop();

                newIChild = null;
                oldIChild = il;

                if (il.getClass() == Leaf.class) {
                    newIChild = new Leaf(((Leaf)il).keyX, ((Leaf)il).keyY, ((Leaf)il).value);
                    Leaf liChild = (Leaf) oldIChild;
                    boolean inTree = liChild.keyX == newKeyX && liChild.keyY == newKeyY;
                    boolean logicalRemove = liChild.move != null && !hasChild(liChild.move.iParent, liChild.move.oldIChild);
                    if (inTree && !logicalRemove) {//if exist, return false
                        return false;
                    }
                }

                newNode = null;
            }

            iFail = false;
            dFail = false;
            cFail = false;
        }
    }

    private int countAllNodes(Internal parent) {
        int c = 0;
        if (parent.ne.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNodes((Internal) parent.ne);
        }
        if (parent.nw.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNodes((Internal) parent.nw);
        }
        if (parent.se.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNodes((Internal) parent.se);
        }
        if (parent.sw.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNodes((Internal) parent.sw);
        }
        c += 1;

        return c;
    }

    @Override
    public int allNodes() {
        return countAllNodes(root);
    }

    private int countMaxDepth(Internal parent, int depth) {
        int ne = 0;
        int nw = 0;
        int se = 0;
        int sw = 0;
        if (parent.ne.getClass() == Internal.class) {
            ne = countMaxDepth((Internal) parent.ne, depth + 1);
        } else {
            ne = depth + 1;
        }
        if (parent.nw.getClass() == Internal.class) {
            nw = countMaxDepth((Internal) parent.nw, depth + 1);
        } else {
            nw = depth + 1;
        }
        if (parent.se.getClass() == Internal.class) {
            se = countMaxDepth((Internal) parent.se, depth + 1);
        } else {
            se = depth + 1;
        }
        if (parent.sw.getClass() == Internal.class) {
            sw = countMaxDepth((Internal) parent.sw, depth + 1);
        } else {
            sw = depth + 1;
        }

        return Math.max(ne, Math.max(nw, Math.max(sw, se)));
    }

    @Override
    public int maxDepth() {
        return countMaxDepth(root, 1);
    }

    private int countAllNonInternal(Internal parent) {
        int c = 0;
        if (parent.ne.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNonInternal((Internal) parent.ne);
        }
        if (parent.nw.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNonInternal((Internal) parent.nw);
        }
        if (parent.se.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNonInternal((Internal) parent.se);
        }
        if (parent.sw.getClass() != Internal.class) {
            c += 1;
        } else {
            c += countAllNonInternal((Internal) parent.sw);
        }

        return c;
    }

    private int countAllLeaves(Internal parent) {
        int c = 0;
        if (parent.ne.getClass() != Internal.class) {
            if (parent.ne.getClass() == Leaf.class)
                c += 1;
        } else {
            c += countAllLeaves((Internal) parent.ne);
        }
        if (parent.nw.getClass() != Internal.class) {
            if (parent.nw.getClass() == Leaf.class)
                c += 1;
        } else {
            c += countAllLeaves((Internal) parent.nw);
        }
        if (parent.se.getClass() != Internal.class) {
            if (parent.se.getClass() == Leaf.class)
                c += 1;
        } else {
            c += countAllLeaves((Internal) parent.se);
        }
        if (parent.sw.getClass() != Internal.class) {
            if (parent.sw.getClass() == Leaf.class)
                c += 1;
        } else {
            c += countAllLeaves((Internal) parent.sw);
        }

        return c;
    }

    private int countAllDepth(Internal parent, int depth) {
        int ne = 0;
        int nw = 0;
        int se = 0;
        int sw = 0;
        if (parent.ne.getClass() == Internal.class) {
            ne = countAllDepth((Internal) parent.ne, depth + 1);
        } else {
            ne = depth + 1;
        }

        if (parent.nw.getClass() == Internal.class) {
            nw = countAllDepth((Internal) parent.nw, depth + 1);
        } else {
            nw = depth + 1;
        }

        if (parent.se.getClass() == Internal.class) {
            se = countAllDepth((Internal) parent.se, depth + 1);
        } else {
            se = depth + 1;
        }

        if (parent.sw.getClass() == Internal.class) {
            sw = countAllDepth((Internal) parent.sw, depth + 1);
        } else {
            sw = depth + 1;
        }

        return ne + nw + se + sw;
    }

    @Override
    public int averageDepth() {
        int nonInternal = countAllNonInternal(root);
        int depth = countAllDepth(root, 1);
        logger.info("nonInternal : " + nonInternal);
        logger.info("depth : " + depth);
        return depth / nonInternal;
    }

    @Override
    public int uselessInternal() {
        return 0;
    }

    @Override
    public int insertSuccessPath() {
        return 0;
    }

    @Override
    public int pendingSuccessPath() {
        return 0;
    }

    @Override
    public int containSuccessPath() {
        return 0;
    }

    @Override
    public int removeSuccessPath() {
        return 0;
    }

    @Override
    public int compressSuccessPath() {
        return 0;
    }

    @Override
    public int newNodeCreate() {
        return 0;
    }

    @Override
    public void resetMisc() {

    }

    @Override
    public int casFailures() {
        return 0;
    }

    @Override
    public long casTime() {
        return 0;
    }


    @Override
    public int size() {
        return countAllLeaves(root);
    }
}
