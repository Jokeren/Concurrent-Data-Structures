package com.jokeren.concurrent.structures.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Created by robinho364 on 2015/9/18.
 */
public class LockfreeGraph<T> implements Graph<T> {
    private static class Node {
        final int key;
        final Object value;
        volatile NextPointer nextPointer;

        class NextPointer {
            final Node next;
            final boolean mark;

            public NextPointer(Node next, boolean mark) {
                this.next = next;
                this.mark = mark;
            }
        }

        static AtomicReferenceFieldUpdater<Node, NextPointer> nextUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, NextPointer.class, "nextPointer");

        public Node(int key, Object value, Node next) {
            this.key = key;
            this.value = value;
            this.nextPointer = new NextPointer(next, false);
        }

        boolean casNext(Node expectNode, Node updateNode, boolean expectMark, boolean updateMark) {
            NextPointer pointer = this.nextPointer;
            if (pointer.next == expectNode && pointer.mark == expectMark) {
                return nextUpdater.compareAndSet(this, pointer, new NextPointer(updateNode, updateMark));
            }
            return false;
        }
    }

    private static final class EdgeNode extends Node {
        final BaseNode baseLink;

        public EdgeNode(int key, Object value, Node next, BaseNode baseLink) {
            super(key, value, next);
            this.baseLink = baseLink;
        }
    }

    private static final class BaseNode extends Node {
        final EdgeNode edgeHead, edgeTail;

        public BaseNode(int key, Object value, Node next) {
            super(key, value, next);
            edgeTail = new EdgeNode(Integer.MAX_VALUE, null, null, null);
            edgeHead = new EdgeNode(Integer.MIN_VALUE, null, edgeTail, null);
        }

        final AtomicInteger refCount = new AtomicInteger(0);

        int getRefCount() {
            return refCount.get();
        }

        boolean casRefCount(int expect, int update) {
            return refCount.compareAndSet(expect, update);
        }

        boolean incrementCount() {
            int count = this.getRefCount();
            while (count != -1) {
                if (this.casRefCount(count, count + 1)) {
                    return true;
                }
                count = this.getRefCount();
            }
            return false;
        }

        void decrementCount() {
            this.refCount.decrementAndGet();
        }
    }

    private final BaseNode head, tail;

    public LockfreeGraph() {
        this(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public LockfreeGraph(int minKey, int maxKey) {
        this.tail = new BaseNode(maxKey, null, null);
        this.head = new BaseNode(minKey, null, this.tail);
    }

    @Override
    public boolean searchNode(int key) {
        Node curr = this.head.nextPointer.next;
        while (curr.key < key) {
            curr = curr.nextPointer.next;
        }

        return curr.key == key;
    }

    @Override
    public boolean searchEdge(int from, int to) {
        return false;
    }

    private static void helpCleanEdge(EdgeNode node, EdgeNode base) {
        int key = node.key;
        while (true) {
            Node prev = base;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;

            while (curr.key < key) {
                nextPointer = curr.nextPointer;
                prev = curr;
                curr = nextPointer.next;
            }

            if (curr == node) {
                nextPointer = curr.nextPointer;
                Node succ = nextPointer.next;
                if (helpDelete(prev, curr, succ)) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static void helpCleanBase(BaseNode node, BaseNode head) {
        int key = node.key;
        while (true) {
            Node prev = head;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;

            while (curr.key < key) {
                nextPointer = curr.nextPointer;
                prev = curr;
                curr = nextPointer.next;
            }

            if (curr == node) {
                nextPointer = curr.nextPointer;
                Node succ = nextPointer.next;
                boolean mark = nextPointer.mark;
                if (!mark) {
                    helpMark(curr, succ);
                }
                if (curr.nextPointer.mark && helpDelete(prev, curr, succ)) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static boolean helpInsert(Node prev, Node curr, Node newNode) {
        return prev.casNext(curr, newNode, false, false);
    }

    private static boolean helpDelete(Node prev, Node curr, Node succ) {
        return prev.casNext(curr, succ, false, false);
    }

    private static boolean helpMark(Node curr, Node succ) {
        return curr.casNext(succ, succ, false, true);
    }

    /**
     * lock-free node insert
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public boolean insertNode(int key, T value) {
        while (true) {
            Node prev = head;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;

            while (curr.key < key) {
                nextPointer = curr.nextPointer;
                prev = curr;
                curr = nextPointer.next;
            }

            if (key == curr.key) {
                return false;
            }

            BaseNode newNode = new BaseNode(key, value, curr);

            if (prev.casNext(curr, newNode, false, false)) {
                return true;
            }
        }
    }


    /**
     * lock-free node delete
     *
     * @param key
     * @return
     */
    @Override
    public boolean deleteNode(int key) {
        while (true) {
            Node prev = head;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;

            while (curr.key < key) {
                nextPointer = curr.nextPointer;
                prev = curr;
                curr = nextPointer.next;
            }

            if (curr.key != key) {
                return false;
            }

            Node.NextPointer currPointer = curr.nextPointer;
            Node succ = currPointer.next;
            boolean mark = currPointer.mark;
            int count = ((BaseNode) curr).getRefCount();

            //logical delete, helpMark or helpClean it
            if (count == -1) {
                if (!mark) {
                    helpMark(curr, succ);
                }

                if (curr.nextPointer.mark) helpDelete(prev, curr, succ);
                continue;
            }

            if (count == 0) {
                if (((BaseNode) curr).casRefCount(0, -1)) {
                    if (helpMark(curr, succ) || ((BaseNode) curr).nextPointer.mark) {
                        if (helpDelete(prev, curr, succ)) {
                            return true;
                        }
                    }
                    helpCleanBase(((BaseNode) curr), head);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean insertEdge(int from, int to) {
        while (true) {
            //first find toNode
            Node prev = head;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;
            BaseNode toNode = null, fromNode = null;

            while (curr.key < to) {
                nextPointer = curr.nextPointer;
                curr = nextPointer.next;
            }

            if (curr.key != to) {
                return false;
            }

            toNode = (BaseNode) curr;
            if (!toNode.incrementCount()) {
                helpCleanBase(toNode, head);
                return false;
            }

            //find fromNode
            prev = head;
            nextPointer = prev.nextPointer;
            curr = nextPointer.next;
            while (curr.key < from) {
                nextPointer = curr.nextPointer;
                curr = nextPointer.next;
            }

            if (curr.key != from) {
                toNode.decrementCount();
                return false;
            }

            fromNode = (BaseNode) curr;
            if (!fromNode.incrementCount()) {
                toNode.decrementCount();
                helpCleanBase(fromNode, head);
                return false;
            }

//            System.out.println("keyFrom1 : " + fromNode.key + ", " + "keyTo1 : " + toNode.key);
            while (true) {
                prev = fromNode.edgeHead;
                nextPointer = prev.nextPointer;
                curr = nextPointer.next;

                while (curr.key < to) {
                    nextPointer = curr.nextPointer;
                    prev = curr;
                    curr = nextPointer.next;
                }

                if (curr.key == to) {
                    fromNode.decrementCount();
                    toNode.decrementCount();
                    return false;
                }

                EdgeNode newNode = new EdgeNode(to, null, curr, toNode);
                if (helpInsert(prev, curr, newNode)) {
                    return true;
                }
            }
        }
    }

    @Override
    public boolean deleteEdge(int from, int to) {
        while (true) {
            Node prev = head;
            Node.NextPointer nextPointer = prev.nextPointer;
            Node curr = nextPointer.next;
            BaseNode toNode = null, fromNode = null;

            while (curr.key < from) {
                nextPointer = curr.nextPointer;
                curr = nextPointer.next;
            }

            if (curr.key != from) {
                return false;
            }

            fromNode = (BaseNode) curr;
            while (true) {
                prev = fromNode.edgeHead;
                nextPointer = prev.nextPointer;
                curr = nextPointer.next;

                while (curr.key < to) {
                    nextPointer = curr.nextPointer;
                    prev = curr;
                    curr = nextPointer.next;
                }

                if (curr.key != to) {
                    return false;
                }

                nextPointer = curr.nextPointer;
                Node succ = nextPointer.next;
                boolean mark = nextPointer.mark;

                if (mark) {
                    helpCleanEdge((EdgeNode) curr, fromNode.edgeHead);
                    continue;
                }

                if (helpMark(curr, succ)) {
                    if (!helpDelete(prev, curr, succ)) {
                        helpCleanEdge((EdgeNode) curr, fromNode.edgeHead);
                    }
                    toNode = ((EdgeNode) curr).baseLink;
                    toNode.decrementCount();
                    fromNode.decrementCount();
                    return true;
                }
            }
        }
    }

    /**
     * not thread safe
     *
     * @return
     */
    @Override
    public long nodeSize() {
        long count = 0;
        Node curr = this.head.nextPointer.next;
        while (curr != this.tail) {
            //System.out.println("key : " + curr.key);
            curr = curr.nextPointer.next;
            ++count;
        }
        return count;
    }

    /**
     * not thread safe
     *
     * @return
     */
    @Override
    public long edgeSize() {
        long count = 0;
        Node curr = this.head.nextPointer.next;
        while (curr != this.tail) {
            Node currEdge = ((BaseNode)curr).edgeHead.nextPointer.next;
            while (currEdge != ((BaseNode)curr).edgeTail) {
                currEdge = currEdge.nextPointer.next;
                ++count;
            }
            curr = curr.nextPointer.next;
        }
        return count;
    }

    @Override
    public void clear() {
        this.head.casNext(this.head.nextPointer.next, this.tail, false, false);
    }

}
