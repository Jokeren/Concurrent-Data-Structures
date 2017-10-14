package com.jokeren.concurrent.structures.linkedlist;

/**
 * Created by robinho364 on 2015/9/22.
 */
public interface Linkedlist<T> {
    boolean insert(int key, T value);

    boolean delete(int key);

    boolean search(int key);

    int size();
}
