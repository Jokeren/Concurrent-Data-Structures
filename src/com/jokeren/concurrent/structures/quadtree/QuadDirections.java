package com.jokeren.concurrent.structures.quadtree;

/**
 * Created by robin on 2015/12/2.
 */
public class QuadDirections {
    final private int size;//default size
    final private int[] containDirection;
    final private int[] insertDirection;
    final private int[] removeDirection;

    public QuadDirections() {
        this(16);
    }

    public QuadDirections(int size) {
        this.size = size;
        this.containDirection = new int[size];
        this.insertDirection = new int[size];
        this.removeDirection = new int[size];
    }

    public int getContainDirection(int threadId) {
        return containDirection[threadId];
    }

    public int getInsertDirection(int threadId) {
        return insertDirection[threadId];
    }

    public int getRemoveDirection(int threadId) {
        return removeDirection[threadId];
    }

    public void setContainDirection(int threadId, int direction) {
        containDirection[threadId] = direction;
    }

    public void setInsertDirection(int threadId, int direction) {
        insertDirection[threadId] = direction;
    }

    public void setRemoveDirection(int threadId, int direction) {
        removeDirection[threadId] = direction;
    }
}
