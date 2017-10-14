package com.jokeren.concurrent.structures.quadtree;

/**
 * Created by robin on 2015/11/6.
 */
public interface Quadtree<V> {
    public boolean insert(double keyX, double keyY, V value);

    public boolean remove(double keyX, double keyY);

    public boolean contains(double keyX, double keyY);

    public boolean move(double oldKeyX, double oldKeyY, double newKeyX, double newKeyY);

    //not thread safe
    public int size();
}
