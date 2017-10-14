package com.jokeren.concurrent.structures.graph;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by robinho364 on 2015/9/28.
 */
public class LockGraph<T> implements Graph<T> {
    private static class Node {
        final int key;
        final Object value;
        volatile Node next;

        public Node(int key, Object value) {
            this(key, value, null);
        }

        public Node(int key, Object value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private final static class BaseNode extends Node {
        final EdgeNode edgeHead, edgeTail;
        final Lock lock = new ReentrantLock();

        final AtomicInteger refCount = new AtomicInteger(0);

        public BaseNode(int key, Object value) {
            this(key, value, null);
        }

        public BaseNode(int key, Object value, Node next) {
            super(key, value, next);
            edgeTail = new EdgeNode(Integer.MAX_VALUE, null, null, null);
            edgeHead = new EdgeNode(Integer.MIN_VALUE, null, edgeTail, null);
        }
    }

    private final static class EdgeNode extends Node {
        final BaseNode baseLink;

        public EdgeNode(int key, Object value, BaseNode baseLink) {
            this(key, value, null, baseLink);
        }

        public EdgeNode(int key, Object value, Node next, BaseNode baseLink) {
            super(key, value, next);
            this.baseLink = baseLink;
        }
    }

    private final BaseNode head, tail;

    public LockGraph() {
        this(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public LockGraph(int minKey, int maxKey) {
        this.tail = new BaseNode(maxKey, null, null);
        this.head = new BaseNode(minKey, null, this.tail);
    }

    @Override
    public boolean searchNode(int key) {
        return false;
    }

    @Override
    public boolean searchEdge(int from, int to) {
        return false;
    }

    @Override
    public boolean insertNode(int key, T value) {
        while (true) {
            Node prev = head;
            Node curr = head.next;

            while (curr.key < key) {
                prev = curr;
                curr = curr.next;
            }

            if (key == curr.key) {
                return false;
            }

            BaseNode basePrev = (BaseNode) prev;
            if (basePrev.lock.tryLock()) {
                if (prev.next == curr) {
                    curr = basePrev.next;
                    BaseNode newNode = new BaseNode(key, value, curr);
                    prev.next = newNode;
                    basePrev.lock.unlock();
                    return true;
                } else {
                    basePrev.lock.unlock();
                }
            }
        }
    }

    @Override
    public boolean deleteNode(int key) {
        while (true) {
            Node prev = head;
            Node curr = prev.next;

            while (curr.key < key) {
                prev = curr;
                curr = curr.next;
            }

            if (curr.key != key) {
                return false;
            }

            BaseNode basePrev = (BaseNode) prev;
            BaseNode baseCurr = (BaseNode) curr;

            if (basePrev.lock.tryLock()) {
                if (baseCurr.lock.tryLock()) {
                    if (baseCurr.refCount.get() != 0) {
                        basePrev.lock.unlock();
                        baseCurr.lock.unlock();
                        return false;
                    }
                    if (basePrev.next == baseCurr) {
                        prev.next = baseCurr.next;
                        basePrev.lock.unlock();
                        return true;
                    } else {
                        basePrev.lock.unlock();
                        baseCurr.lock.unlock();
                    }
                } else {
                    basePrev.lock.unlock();
                }
            }
        }
    }

    @Override
    public boolean insertEdge(int from, int to) {
        while (true) {
            Node prev = head;
            Node curr = prev.next;
            BaseNode toNode = null, fromNode = null;

            while (curr.key < to) {
                curr = curr.next;
            }

            if (curr.key != to) {
                return false;
            }

            toNode = (BaseNode) curr;
            if (!toNode.lock.tryLock()) {
                continue;
            }

            prev = head;
            curr = prev.next;
            while (curr.key < from) {
                curr = curr.next;
            }

            if (curr.key != from) {
                toNode.lock.unlock();
                return false;
            }

            fromNode = (BaseNode) curr;
            if (!fromNode.lock.tryLock()) {
                toNode.lock.unlock();
                continue;
            }

            while (true) {
                prev = fromNode.edgeHead;
                curr = prev.next;

                while (curr.key < to) {
                    prev = curr;
                    curr = prev.next;
                }

                if (curr.key == to) {
                    return false;
                }

                EdgeNode newNode = new EdgeNode(to, null, curr, toNode);
                prev.next = newNode;
                toNode.refCount.incrementAndGet();
                fromNode.lock.unlock();
                toNode.lock.unlock();
                return true;
            }
        }
    }

    @Override
    public boolean deleteEdge(int from, int to) {
        while (true) {
            Node prev = head;
            Node curr = prev.next;
            BaseNode toNode = null, fromNode = null;

            while (curr.key < from) {
                curr = curr.next;
            }

            if (curr.key != from) {
                return false;
            }

            fromNode = (BaseNode) curr;
            while (true) {
                prev = fromNode.edgeHead;
                curr = prev.next;

                while (curr.key < to) {
                    prev = curr;
                    curr = curr.next;
                }

                if (curr.key != to) {
                    return false;
                }

                Node succ = curr.next;
                prev.next = succ;
                fromNode.lock.unlock();
                toNode.lock.unlock();
            }
        }
    }

    @Override
    public long nodeSize() {
        long count = 0;
        Node curr = head.next;
        while (curr != tail) {
            ++count;
            curr = curr.next;
        }

        return count;
    }

    @Override
    public long edgeSize() {
        return 0;
    }

    @Override
    public void clear() {
        this.head.next = this.tail;
    }
}
