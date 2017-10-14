package com.jokeren.concurrent.structures.quadtree;

import com.jokeren.concurrent.structures.miscellaneous.QuadtreeMisc;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/24.
 */
public class QuadFlagPure<V> implements Quadtree<V>, QuadtreeMisc{
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
    final private Empty empty = new Empty<>();

    final static Logger logger = Logger.getLogger("QuadFlagPure");

    final private Internal root;

    public QuadFlagPure(double w, double h) {
        root = new Internal<V>(0.0f, 0.0f, w, h);
        split();
    }

    public QuadFlagPure() {
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
            internal.nw = empty;
        }

        if (ne) {
            internal.ne = node;
        } else {
            internal.ne = empty;
        }

        if (sw) {
            internal.sw = node;
        } else {
            internal.sw = empty;
        }

        if (se) {
            internal.se = node;
        } else {
            internal.se = empty;
        }

        return internal;
    }

    private Node<V> getQuadrant(Internal parent, double keyX, double keyY, int [] direction) {
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

    private static class Node<V> {
    }

    private final static class Leaf<V> extends Node<V> {
        final double keyX, keyY;//double for simplicity
        final V value;
        //different from patricia, do not need flag

        public Leaf(double keyX, double keyY, V value) {
            this.keyX = keyX;
            this.keyY = keyY;
            this.value = value;
        }
    }

    private final static class Empty<V> extends Node<V> {

    }

    private static class Operation {

    }

    private final static class Mark extends Operation {
        Internal gp, p;
        Operation pOp;
        Node oldChild;
        int prevDirection;

        public Mark(Internal gp, Internal p, Operation pOp, Node oldChild, int prevDirection) {
            this.gp = gp;
            this.p = p;
            this.pOp = pOp;
            this.oldChild = oldChild;
            this.prevDirection = prevDirection;
        }
    }

    private final static class Delete extends Operation {
        Mark mark;

        public Delete(Mark mark) {
            this.mark = mark;
        }
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
        Operation iOldOp;
        volatile boolean iFlag = false;

        public Move(Internal iParent, Internal dParent, Node oldIChild, Node oldDChild, Node newIChild,
                    int prevIDirection, int prevDDirection, Operation iOldOp) {
            this.iParent = iParent;
            this.dParent = dParent;
            this.oldIChild = oldIChild;
            this.oldDChild = oldDChild;
            this.newIChild = newIChild;
            this.prevIDirection = prevIDirection;
            this.prevDDirection = prevDDirection;
            this.iOldOp = iOldOp;
        }
    }

    private final static class Clean extends Operation {

    }

    private void help(Operation op) {
        if (op.getClass() == Substitute.class) {//Replace
            helpSubstitute((Substitute) op);
        }
        else if (op.getClass() == Mark.class) {//Mark
            helpRemove((Mark) op);
        }
        else if (op.getClass() == Delete.class){//Delete
            helpDelete(((Delete)op).mark);
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
        helpFlag(op.iParent, op.iOldOp, op);

        if (op == op.iParent.op) {
            op.iFlag = true;
            if (op.oldDChild == op.oldIChild){
                boolean replace = helpReplace(op.dParent, op.oldDChild, op.newIChild, op.prevDDirection);
            } else {
                //delete node
                boolean delete = helpReplace(op.dParent, op.oldDChild, new Empty<V>(), op.prevDDirection);
                //insert node
                boolean insert = helpReplace(op.iParent, op.oldIChild, op.newIChild, op.prevIDirection);
            }
        }

        if (op.iFlag) {
            //unflag
            helpFlag(op.iParent, op, new Clean());
            if (op.iParent != op.dParent)
                helpFlag(op.dParent, op, new Clean());
            return true;
        } else {
            helpFlag(op.dParent, op, new Clean());
            return false;
        }
    }

    private boolean helpRemove(Mark gpOp) {
        Operation newPop = new Delete(gpOp);
        boolean result = helpFlag(gpOp.p, gpOp.pOp, newPop);
        Operation pOp = gpOp.p.op;

        if (result || (pOp.getClass() == Delete.class && ((Delete)pOp).mark == gpOp)) {
            helpDelete(gpOp);
            return true;
        } else {
            help(pOp);
            helpFlag(gpOp.gp, gpOp, new Clean());
            return false;
        }
    }

    private void helpDelete(Mark gpOp) {
        Internal p = gpOp.p, gp = gpOp.gp;
        helpReplace(p, gpOp.oldChild, new Empty<>(), gpOp.prevDirection);
        Node candidate = null;
        if (gp != root && p.ne.getClass() == Empty.class && p.nw.getClass() == Empty.class
                && p.sw.getClass() == Empty.class && p.se.getClass() == Empty.class) {
            candidate = new Empty<>();
        } else {
            Internal newP = new Internal(p.x, p.y, p.w, p.h);
            newP.nw = p.nw;
            newP.ne = p.ne;
            newP.sw = p.sw;
            newP.se = p.se;
            candidate = newP;
        }
        if (gp.nw == p) {
            helpReplace(gp, p, candidate, 0);
        } else if (gp.ne == p) {
            helpReplace(gp, p, candidate, 1);
        } else if (gp.sw == p) {
            helpReplace(gp, p, candidate, 2);
        } else if (gp.se == p) {
            helpReplace(gp, p, candidate, 3);
        }
        helpFlag(gp, gpOp, new Clean());
    }

    private boolean helpReplace(Internal parent, Node oldChild, Node newChild, int prevDirection) {
        boolean value = false;
        switch (prevDirection) {
            case 0:
                value = nwUpdater.compareAndSet(parent, oldChild, newChild);
                break;
            case 1:
                value = neUpdater.compareAndSet(parent, oldChild, newChild);
                break;
            case 2:
                value = swUpdater.compareAndSet(parent, oldChild, newChild);
                break;
            case 3:
                value = seUpdater.compareAndSet(parent, oldChild, newChild);
                break;
            default:
                break;
        }
        return value;
    }

    private void recursiveCompress(Internal p, int prevDirection, Trace visited, Record record) {
        Operation pOp = null;
        Internal gp = null;
        while (true) {
            pOp = p.op;
            if (pOp.getClass() == Clean.class) {
                record.node = visited.peekNode();
                record.prevDirection = visited.peekDirection();
                visited.pop();
                gp = (Internal) record.node;
                if (gp == root) {//if root, not compress
                    return;
                }

                Operation newOp = new Compress(gp, p, prevDirection);
                prevDirection = record.prevDirection;
                if (!helpCheck(p)) {
                    return;
                }
                if (!helpFlag(p, pOp, newOp)) {
                    help(p.op);
                    return;
                }
                helpCompress((Compress) newOp);
                p = gp;
            } else {//do not help, as the same operation could be done in recursive help
                help(pOp);
                return;
            }
        }
    }

    private Node<V> createNode(Leaf child, double x, double y, double w, double h,
                               double keyX, double keyY, V value, int[] direction) {
        w /= 2.0f;
        h /= 2.0f;
        switch (direction[0]) {
            case 0: break;
            case 1: x = x + w; break;
            case 2: y = y + h; break;
            case 3: x = x + w; y = y + h; break;
            default: break;
        }
        Internal internal = split(child, x, y, w, h);
        Internal result = internal;
        Internal prevNode = null;
        Node candidate = getQuadrant(internal, keyX, keyY, direction);

        while (candidate.getClass() == Leaf.class) {
            w /= 2.0f;
            h /= 2.0f;
            prevNode = internal;
            switch (direction[0]) {
                case 0: internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.nw = internal; break;
                case 1: x = x + w; internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.ne = internal; break;
                case 2: y = y + h; internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.sw = internal; break;
                case 3: x = x + w;  y = y + h; internal = split((Leaf) candidate, x, y, w, h);
                    prevNode.se = internal; break;
                default: break;
            }
            candidate = getQuadrant(internal, keyX, keyY, direction);
        }

        Leaf leaf = new Leaf<V>(keyX, keyY, value);
        switch (direction[0]) {
            case 0: internal.nw = leaf; break;
            case 1: internal.ne = leaf; break;
            case 2: internal.sw = leaf; break;
            case 3: internal.se = leaf; break;
            default: break;
        }

        return result;
    }

    @Override
    public boolean insert(double keyX, double keyY, V value) {
        int [] direction = new int[1];
        while (true) {
            //route to leaf or empty node
            Node p = null, l = root;
            Operation pOp = null;

            while (l.getClass() == Internal.class) {
                pOp = ((Internal)l).op;
                p = l;
                l = getQuadrant((Internal) p, keyX, keyY, direction);
            }

            Internal parent = (Internal) p;
            Leaf child = null;
            if (l.getClass() == Leaf.class) {
                child = new Leaf(((Leaf)l).keyX, ((Leaf)l).keyY, ((Leaf)l).value);
                if (child.keyX == keyX && child.keyY == keyY) {//if exist, return false
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
        }
    }

    @Override
    public boolean remove(double keyX, double keyY) {
        int [] direction = new int [1];

        while (true) {
            Node l = root;
            Internal p = null, gp = null;
            Leaf child = null;
            Operation pOp = null, gpOp = null;
            //route to leaf or empty node
            while (l.getClass() == Internal.class) {
                gp = p;
                gpOp = pOp;
                p = (Internal) l;
                pOp = p.op;
                l = getQuadrant(p, keyX, keyY, direction);
            }

            child = null;
            if (l.getClass() == Leaf.class) {
                child = (Leaf) l;
                if (!(child.keyX == keyX && child.keyY == keyY)) {//if not exist, return false
                    return false;
                }
            } else {//if empty node
                return false;
            }

            int prevDirection = direction[0];
            if (gpOp.getClass() == Clean.class) {
                if (pOp.getClass() == Clean.class) {
                    Operation newgpOp = new Mark(gp, p, pOp, l, prevDirection);

                    if (helpFlag(gp, gpOp, newgpOp)) {
                        if (helpRemove((Mark) newgpOp)) {
                            return true;
                        }
                    } else {
                        Operation op = gp.op;
                        help(op);
                    }
                } else {
                    help(pOp);
                }
            } else {
                help(gpOp);
            }
        }
    }

    @Override
    public boolean contains(double keyX, double keyY) {
        Node l = root;
        int [] direction = new int[1];
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

        @Override
    public boolean move(double oldKeyX, double oldKeyY, double newKeyX, double newKeyY) {
        Trace dVisited = dTrace.get();
        Record dRecord = new Record();
        while (true) {
            //locate the delete node
            Node l = root;
            Operation dPop = null;
            dVisited.clear();
            int[] direction = new int[1];

            while (l.getClass() == Internal.class) {
                dVisited.push((Internal) l, direction[0]);
                dPop = ((Internal) l).op;
                l = getQuadrant((Internal) l, oldKeyX, oldKeyY, direction);
            }
            int prevDDirection = direction[0];

            Leaf dChild = null;
            if (l.getClass() == Leaf.class) {
                dChild = (Leaf) l;
                if (dChild.keyX != oldKeyX || dChild.keyY != oldKeyY) {//if not exist, return false
                    return false;
                }
            } else {//if empty node
                return false;
            }

            //locate the insert node
            l = root;
            Operation iPop = null;
            Internal iParent = null;

            while (l.getClass() == Internal.class) {
                iParent = (Internal) l;
                iPop = ((Internal) iParent).op;
                l = getQuadrant((Internal) l, newKeyX, newKeyY, direction);
            }
            int prevIDirection = direction[0];

            Node iChild = l;
            if (l.getClass() == Leaf.class) {
                if (((Leaf) iChild).keyX == newKeyX && ((Leaf) iChild).keyY == newKeyY) {//if exist, return false
                    return false;
                }
            }

            boolean iFail = false;
            boolean dFail = false;
            Internal dParent = null;
            //flag
            dRecord.node = dVisited.peekNode();
            dRecord.prevDirection = dVisited.peekDirection();
            dVisited.pop();
            dParent = (Internal) dRecord.node;
            Node newNode = null;
            if (dPop.getClass() != Clean.class) {
                help(dPop);
            } else {
                if (iPop.getClass() != Clean.class) {
                    help(iPop);
                } else {
                    if (iChild.getClass() == Empty.class || iChild == dChild) {
                        newNode = new Leaf<V>(newKeyX, newKeyY, (V) dChild.value);
                    } else {
                        //TODO:optimize, if dFail, newNode needn't to be create again
                        direction[0] = prevIDirection;
                        newNode = createNode((Leaf) iChild, iParent.x, iParent.y, iParent.w, iParent.h,
                                newKeyX, newKeyY, (V) dChild.value, direction);
                    }

                    Operation move = new Move(iParent, dParent, iChild, dChild, newNode,
                            prevIDirection, prevDDirection, iPop);

                    if (dParent != iParent) {
                        if (helpFlag(dParent, dPop, move)) {
                            if (helpMove((Move) move)) {
                                //TODO:structure adjustment?
                                recursiveCompress(dParent, dRecord.prevDirection, dVisited, dRecord);
                                return true;
                            }
                        } else {
                            help(dParent.op);
                        }
                    } else {//special, common parent
                        if (dPop != iPop) {
                            continue;
                        } else {
                            if (helpMove((Move) move)) {
                                return true;
                            }
                        }
                    }
                }
            }
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
            nw = countMaxDepth((Internal) parent.nw,  depth + 1);
        } else {
            nw = depth + 1;
        }
        if (parent.se.getClass() == Internal.class) {
            se = countMaxDepth((Internal) parent.se,  depth + 1);
        } else {
            se = depth + 1;
        }
        if (parent.sw.getClass() == Internal.class) {
            sw = countMaxDepth((Internal) parent.sw,  depth + 1);
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
            if (parent.ne.getClass() == Leaf.class) {
                c += 1;
            }
        } else {
            c += countAllLeaves((Internal) parent.ne);
        }
        if (parent.nw.getClass() != Internal.class) {
            if (parent.nw.getClass() == Leaf.class) {
                c += 1;
            }
        } else {
            c += countAllLeaves((Internal) parent.nw);
        }
        if (parent.se.getClass() != Internal.class) {
            if (parent.se.getClass() == Leaf.class) {
                c += 1;
            }
        } else {
            c += countAllLeaves((Internal) parent.se);
        }
        if (parent.sw.getClass() != Internal.class) {
            if (parent.sw.getClass() == Leaf.class) {
                c += 1;
            }
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
            nw = countAllDepth((Internal) parent.nw,  depth + 1);
        } else {
            nw = depth + 1;
        }

        if (parent.se.getClass() == Internal.class) {
            se = countAllDepth((Internal) parent.se,  depth + 1);
        } else {
            se = depth + 1;
        }

        if (parent.sw.getClass() == Internal.class) {
            sw = countAllDepth((Internal) parent.sw,  depth + 1);
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

    private int countUseless(Internal parent) {
        boolean useless = true;
        int count = 0;

        if (parent.nw.getClass() == Leaf.class) {
            useless = false;
        } else if (parent.nw.getClass() == Internal.class) {
            count += countUseless((Internal) parent.nw);
        }

        if (parent.ne.getClass() == Leaf.class) {
            useless = false;
        } else if (parent.ne.getClass() == Internal.class) {
            count += countUseless((Internal) parent.ne);
        }

        if (parent.sw.getClass() == Leaf.class) {
            useless = false;
        } else if (parent.sw.getClass() == Internal.class) {
            count += countUseless((Internal) parent.sw);
        }

        if (parent.se.getClass() == Leaf.class) {
            useless = false;
        } else if (parent.se.getClass() == Internal.class) {
            count += countUseless((Internal) parent.se);
        }

        if (useless) {
            count += 1;
        }

        return count;
    }

    @Override
    public int uselessInternal() {
        return countUseless(root);
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
