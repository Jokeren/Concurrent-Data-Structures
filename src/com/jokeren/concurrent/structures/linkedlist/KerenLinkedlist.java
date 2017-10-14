package com.jokeren.concurrent.structures.linkedlist;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * a lock-free linked-list adapted from Tim-harris's algorithm-"A pragmatic implementation of non-blocking linked lists".
 * Both insert and delete operations are lock-free. The search operation is wait-free, as researches suggest that help
 * mechanisms should be selfish.
 * The insert operation encounters no help, and the delete operation helps itself to clean up nodes which are logically marked.
 * Created by robinho364 on 2015/9/22.
 */
public class KerenLinkedlist<T> implements Linkedlist<T> {

    private static class Node {
        final int key;
        final Object value;
        volatile NextPointer nextPointer;

        final static AtomicReferenceFieldUpdater<Node, NextPointer> nextUpdater =
                AtomicReferenceFieldUpdater.newUpdater(Node.class, NextPointer.class, "nextPointer");

        boolean casNext(Node expectNode, Node updateNode, boolean expectMark, boolean updateMark) {
            NextPointer pointer = this.nextPointer;
            if (pointer.next == expectNode && pointer.mark == expectMark) {
                return nextUpdater.compareAndSet(this, pointer, new NextPointer(updateNode, updateMark));
            }
            return false;
        }

        class NextPointer {
            final Node next;
            final boolean mark;

            public NextPointer(Node next, boolean mark) {
                this.next = next;
                this.mark = mark;
            }
        }

        public Node(int key, Object value, Node next) {
            this.key = key;
            this.value = value;
            this.nextPointer = new NextPointer(next, false);
        }
    }

    private final Node head, tail;

    public KerenLinkedlist() {
        this.tail = new Node(Integer.MAX_VALUE, null, null);
        this.head = new Node(Integer.MIN_VALUE, null, this.tail);
    }

    /**
     * clean target node in the list
     * @param node
     */
    private void helpClean(Node node) {
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
                Node succ = curr.nextPointer.next;
                if (helpDelete(prev, curr, succ)) {
                    return ;
                }
            } else {
                return ;
            }
        }
    }

    /**
     * prev -> succ cas operation
     * @param prev
     * @param next
     * @param succ
     * @return true if cas successes
     */
    private boolean helpDelete(Node prev, Node next, Node succ) {
        return prev.casNext(next, succ, false, false);
    }

    /**
     * insert without any help, see-"Why Non-Blocking Operations Should Be Selfish"
     * @param key
     * @param value
     * @return true if successfully inserted
     */
    @Override
    public boolean insert(int key, T value) {
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

            Node newNode = new Node(key, value, curr);

            if (prev.casNext(curr, newNode, false, false)) {
                return true;
            }
        }
    }

    /**
     * delete existing nodes in the list
     * @param key
     * @return true if physically deleted
     */
    @Override
    public boolean delete(int key) {
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

            if (mark) {
                helpClean(curr);
                continue;
            }

            if (curr.casNext(succ, succ, false, true)) {//success mark
                if (!prev.casNext(curr, succ, false, false)) {//prev is marked, or nodes are inserted
                    helpClean(curr);
                }
                return true;
            }
        }
    }

    @Override
    public boolean search(int key) {
        return false;
    }

    /**
     * traverse from head to tail
     * @return node size
     */
    @Override
    public int size() {
        int count = 0;
        Node prev = this.head.nextPointer.next;
        while (prev != this.tail) {
            System.out.println("key : " + prev.key);
            prev = prev.nextPointer.next;
            ++count;
        }
        return count;
    }
}
