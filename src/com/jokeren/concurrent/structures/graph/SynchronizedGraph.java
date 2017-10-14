package com.jokeren.concurrent.structures.graph;

/**
 * Created by robinho364 on 2015/9/28.
 */
public class SynchronizedGraph<T> implements Graph<T> {
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
        return false;
    }

    @Override
    public boolean deleteNode(int key) {
        return false;
    }

    @Override
    public boolean insertEdge(int from, int to) {
        return false;
    }

    @Override
    public boolean deleteEdge(int from, int to) {
        return false;
    }

    @Override
    public long nodeSize() {
        return 0;
    }

    @Override
    public long edgeSize() {
        return 0;
    }

    @Override
    public void clear() {

    }
}
