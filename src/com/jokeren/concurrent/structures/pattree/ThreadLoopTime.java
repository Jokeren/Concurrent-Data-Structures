package com.jokeren.concurrent.structures.pattree;

import com.jokeren.concurrent.structures.miscellaneous.ThreadMisc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by robin on 2015/12/2.
 */
public class ThreadLoopTime extends Thread implements ThreadMisc {
    final private int threadId;
    final Long[] keys;
    final private int insertRatio;
    final private int removeRatio;
    final private int containRatio;
    final private int moveRatio;
    final private int nonUniform;
    final private int range;
    final CyclicBarrier gate;
    final private PatriciaTree patriciaTree;
    final private double directions[][] = new double[8][2];

    private long containResponseTime = 0;
    private long insertResponseTime = 0;
    private long removeResponseTime = 0;
    private long moveResponseTime = 0;
    private int containOperationCount = 0;
    private int insertOperationCount = 0;
    private int removeOperationCount = 0;
    private int moveOperationCount = 0;
    volatile boolean stopFlag = false;

    public ThreadLoopTime(int threadId, Long[] keys, int insertRatio, int removeRatio, int containRatio,
                          int moveRatio, int nonUniform, int range, CyclicBarrier gate, PatriciaTree patriciaTree) {
        this.threadId = threadId;
        this.keys = keys;
        this.insertRatio = insertRatio;
        this.removeRatio = removeRatio;
        this.containRatio = containRatio;
        this.moveRatio = moveRatio;
        this.nonUniform = nonUniform;
        this.range = range;
        this.gate = gate;
        this.patriciaTree = patriciaTree;
        //init direction
        directions[0][0] = -0.0001f;
        directions[0][1] = -0.0001f;
        directions[1][0] = 0.0001f;
        directions[1][1] = -0.0001f;
        directions[2][0] = -0.0001f;
        directions[2][1] = 0.0001f;
        directions[3][0] = 0.0001f;
        directions[3][1] = 0.0f;
        directions[4][0] = 0.0f;
        directions[4][1] = 0.0001f;
        directions[5][0] = 0.0001f;
        directions[5][1] = 0.0001f;
        directions[6][0] = 0.0001f;
        directions[6][1] = 0.0f;
        directions[7][0] = 0.0f;
        directions[7][1] = 0.0001f;
    }

    public void stopThread() {
        stopFlag = true;
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
        Long keySet = null;
        int operation;
        int point;
        int pX, pY;
        if (nonUniform == 1) {
            keySet = keys[ThreadLocalRandom.current().nextInt(range * range)];
        } else {
            keySet = keys[ThreadLocalRandom.current().nextInt(range)];
        }
        //init threadlocal variables
        patriciaTree.insert(keySet, new Object());
        patriciaTree.delete(keySet, new Object());
        try {
            gate.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        while (stopFlag == false) {
            if (nonUniform == 1) {
                keySet = keys[ThreadLocalRandom.current().nextInt(range * range)];
            } else {
                keySet = keys[ThreadLocalRandom.current().nextInt(range)];
            }

        start = System.nanoTime();
        operation = ThreadLocalRandom.current().nextInt(100);
        if (operation < insertRatio) {
            patriciaTree.insert(keySet, new Object());
            end = System.nanoTime();
            insertResponseTime += end - start;
            insertOperationCount += 1;
        } else if (operation < (insertRatio + removeRatio)) {
            patriciaTree.delete(keySet, new Object());
            end = System.nanoTime();
            removeResponseTime += end - start;
            removeOperationCount += 1;
        } else if (operation < (insertRatio + removeRatio + containRatio)) {
            patriciaTree.find(keySet);
            end = System.nanoTime();
            containResponseTime += end - start;
            containOperationCount += 1;
        } else {
            Long moveKeySet = keys[ThreadLocalRandom.current().nextInt(range)];
            if (moveKeySet != keySet) {
                patriciaTree.move(keySet, new Object(), moveKeySet, new Object());
                moveOperationCount += 1;
            }
        }
        }
    }
}
