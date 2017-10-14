package com.jokeren.concurrent.structures.quadtree;

import com.jokeren.concurrent.structures.miscellaneous.ThreadMisc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by robin on 2015/12/3.
 */
public class ThreadLoopCase extends Thread implements ThreadMisc {
    final private int threadId;
    final KeySet[] keys;
    final private int insertRatio;
    final private int removeRatio;
    final private int containRatio;
    final private int moveRatio;
    final private int nonUniform;
    final private int range;
    final CyclicBarrier gate;
    final private Quadtree quadtree;
    final private double directions[][] = new double[8][2];

    private long containResponseTime = 0;
    private long insertResponseTime = 0;
    private long removeResponseTime = 0;
    private long moveResponseTime = 0;
    private int containOperationCount = 0;
    private int insertOperationCount = 0;
    private int removeOperationCount = 0;
    private int moveOperationCount = 0;


    public ThreadLoopCase(int threadId, KeySet[] keys, int insertRatio, int removeRatio, int containRatio,
                          int moveRatio, int nonUniform, int range, CyclicBarrier gate, Quadtree quadtree) {
        this.threadId = threadId;
        this.keys = keys;
        this.insertRatio = insertRatio;
        this.removeRatio = removeRatio;
        this.containRatio = containRatio;
        this.moveRatio = moveRatio;
        this.nonUniform = nonUniform;
        this.range = range;
        this.gate = gate;
        this.quadtree = quadtree;
    }

    public int getContainOperationCount() {
        return this.containOperationCount;
    }

    public long getContainResponseTime() {
        return containResponseTime;
    }

    public long getInsertResponseTime() {
        return insertResponseTime;
    }

    public long getRemoveResponseTime() {
        return removeResponseTime;
    }

    public long getMoveResponseTime() {
        return moveResponseTime;
    }

    public int getInsertOperationCount() {
        return insertOperationCount;
    }

    public int getRemoveOperationCount() {
        return removeOperationCount;
    }

    public int getMoveOperationCount() {
        return moveOperationCount;
    }

    @Override
    public void run() {
        long start = 0, end = 0;
        KeySet keySet = null;
        if (nonUniform == 1) {
            keySet = keys[ThreadLocalRandom.current().nextInt(range * range)];
        } else {
            keySet = keys[ThreadLocalRandom.current().nextInt(range)];
        }
        //FIXME: could be deleted, init threadlocal variables
        quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
        quadtree.remove(keySet.getKeyX(), keySet.getKeyY());
        int perThread = range / (gate.getParties() - 1);
        int startIdx = threadId * perThread;
        int endIdx = (threadId + 1) * perThread + 1;
        if (threadId == gate.getParties() - 1) {
            endIdx = keys.length;
        }
        try {
            gate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        for (int i = startIdx; i < endIdx; ++i) {
            keySet = keys[i];
            start = System.nanoTime();
            if (nonUniform == 3) {
                quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                end = System.nanoTime();
                insertResponseTime += end - start;
                insertOperationCount += 1;
            } else if (nonUniform == 4) {
                quadtree.remove(keySet.getKeyX(), keySet.getKeyY());
                end = System.nanoTime();
                removeResponseTime += end - start;
                removeOperationCount += 1;
            } else if (nonUniform == 5) {
                quadtree.contains(keySet.getKeyX(), keySet.getKeyY());
                end = System.nanoTime();
                containResponseTime += end - start;
                containOperationCount += 1;
            }
        }
    }
}
