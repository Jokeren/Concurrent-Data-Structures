package com.jokeren.concurrent.structures.quadtree;

/**
 * Created by robin on 2015/11/8.
 */
public class KeySet {
    double keyX;
    double keyY;

    public KeySet(double keyX, double keyY) {
        this.keyX = keyX;
        this.keyY = keyY;
    }

    public double getKeyX() {
        return keyX;
    }

    public double getKeyY() {
        return keyY;
    }
}
