package com.jokeren.concurrent.structures.linkedlist;

/**
 * Implements harris' original lock-free linked-list
 * Created by robinho364 on 2015/9/22.
 */
public class HarrisLinkedlist<T> implements Linkedlist<T> {
    @Override
    public boolean insert(int key, T value) {
        return false;
    }

    @Override
    public boolean delete(int key) {
        return false;
    }

    @Override
    public boolean search(int key) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }
}
