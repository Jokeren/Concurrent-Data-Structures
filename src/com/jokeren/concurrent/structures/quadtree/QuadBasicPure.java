package com.jokeren.concurrent.structures.quadtree;

import com.jokeren.concurrent.structures.miscellaneous.QuadtreeMisc;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/6.
 */
public class QuadBasicPure<V> implements Quadtree<V>, QuadtreeMisc {
    final private static AtomicReferenceFieldUpdater<Internal, Node> nwUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "nw");
    final private static AtomicReferenceFieldUpdater<Internal, Node> neUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "ne");
    final private static AtomicReferenceFieldUpdater<Internal, Node> swUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "sw");
    final private static AtomicReferenceFieldUpdater<Internal, Node> seUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Internal.class, Node.class, "se");
    final Logger logger = Logger.getLogger("QuadBasicPure");
    final private Empty empty = new Empty<>();
    final private Internal root;

    public QuadBasicPure(double w, double h) {
        root = new Internal<V>(0.0f, 0.0f, w, h);
        split();
    }

    public QuadBasicPure() {
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

    private Node<V> createNode(Leaf child, double x, double y, double w, double h,
                               double keyX, double keyY, V value, int[] direction) {
        w /= 2.0;
        h /= 2.0;
        switch (direction[0]) {
            case 0:
                break;
            case 1:
                x = x + w;
                break;
            case 2:
                y = y + h;
                break;
            case 3:
                x = x + w;
                y = y + h;
                break;
            default:
                break;
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

    private static class Node<V> {
    }

    private final static class Internal<V> extends Node<V> {
        final double x, y, w, h;
        volatile Node<V> nw, ne, sw, se;

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

        public Leaf(double keyX, double keyY, V value) {
            this.keyX = keyX;
            this.keyY = keyY;
            this.value = value;
        }
    }

    private final static class Empty<V> extends Node<V> {

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

    @Override
    public boolean insert(double keyX, double keyY, V value) {
        int [] direction = new int[1];
        while (true) {
            //route to leaf or empty node
            Node p = null, l = root;
            while (l.getClass() == Internal.class) {
                p = l;
                l = getQuadrant((Internal) p, keyX, keyY, direction);
            }

            Internal parent = (Internal) p;
            Leaf child = null;
            if (l.getClass() == Leaf.class) {
                //TODO:optimize, not create new child
                child = new Leaf(((Leaf)l).keyX, ((Leaf)l).keyY, ((Leaf)l).value);
                if (child.keyX == keyX && child.keyY == keyY) {//if exist, return false
                    return false;
                }
            }

            int prevDirection = direction[0];
            Node newNode = null;
            if (child == null) {//terminal node is empty, therefore create a leaf node
                newNode = new Leaf<V>(keyX, keyY, value);
            } else {//terminal node is leaf, therefore split it
                newNode = createNode(child, parent.x, parent.y, parent.w, parent.h, keyX, keyY, value, direction);
            }
            if (helpReplace(parent, l, newNode, prevDirection)) {//insert new node
                return true;
            }
        }
    }

    @Override
    public boolean remove(double keyX, double keyY) {
        int [] direction = new int[1];
        Node newNode = new Empty<V>();
        while (true) {
            Node p = null, l = root;

            //route to leaf or empty node
            while (l.getClass() == Internal.class) {
                p = l;
                l = getQuadrant((Internal) p, keyX, keyY, direction);
            }

            Internal parent = (Internal) p;
            Leaf child = null;
            if (l.getClass() == Leaf.class) {
                child = (Leaf) l;
                if (child.keyX != keyX || child.keyY != keyY) {//if not exist, return false
                    return false;
                }
            } else {//if empty node
                return false;
            }

            if (helpReplace(parent, child, newNode, direction[0])) {
                return true;
            }
        }
    }

    @Override
    public boolean contains(double keyX, double keyY) {
        Node l = root;
        int [] direction = new int [1];
        while (l.getClass() == Internal.class) {
            l = getQuadrant((Internal) l, keyX, keyY, direction);
//            ++containPath;
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
        throw new UnsupportedOperationException();
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

    private void storeDepth(Internal parent, int [] depth, int curDepth) {
        int ne = 0;
        int nw = 0;
        int se = 0;
        int sw = 0;
        if (parent.ne.getClass() == Internal.class) {
            storeDepth((Internal) parent.ne, depth, curDepth + 1);
        } else {
            ne = curDepth + 1;
            ++depth[ne];
        }

        if (parent.nw.getClass() == Internal.class) {
            storeDepth((Internal) parent.nw, depth, curDepth + 1);
        } else {
            nw = curDepth + 1;
            ++depth[nw];
        }

        if (parent.se.getClass() == Internal.class) {
            storeDepth((Internal) parent.se, depth, curDepth + 1);
        } else {
            se = curDepth + 1;
            ++depth[se];
        }

        if (parent.sw.getClass() == Internal.class) {
            storeDepth((Internal) parent.sw, depth, curDepth + 1);
        } else {
            sw = curDepth + 1;
            ++depth[sw];
        }

        return;
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
