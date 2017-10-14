package com.jokeren.concurrent.structures.graph;

/**
 * Created by robinho364 on 2015/9/19.
 */
public interface Graph<T> {
    boolean searchNode(int key);

    boolean searchEdge(int from, int to);

    boolean insertNode(int key, T value);

    boolean deleteNode(int key);

    boolean insertEdge(int from, int to);

    boolean deleteEdge(int from, int to);

    long nodeSize();

    long edgeSize();

    void clear();
}
