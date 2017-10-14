package com.jokeren.concurrent.structures.performance;

import com.jokeren.concurrent.structures.kary.LockFree4ST;
import com.jokeren.concurrent.structures.kary.ThreadLoopTime;
import com.jokeren.concurrent.structures.miscellaneous.ThreadMisc;
import com.jokeren.concurrent.utils.Performance;
import com.jokeren.concurrent.utils.PointTransform;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/17.
 */
public class KaryPerformance implements Performance{
    //choose which quadtree
    private static String choose;
    //key range
    private static int range;
    //how many threads?
    private static int nThread;
    //enable miscelleuous? 0 or 1
    private static int miscelleuous;
    //enable non-uniform? 0 or 1
    private static int nonUniform;
    //ratios, total 100
    //insert, positive integer
    private static int insert;
    //remove, positive integer
    private static int remove;
    //contain, positive integer
    private static int contain;
    //move, positive integer
    private static int move;
    //logger
    Logger logger = Logger.getLogger("KaryPerformance");
    private int intAbs(int num) {
        return num < 0 ? -num : num;
    }


    private void generateKeySets(Long[] keySets) {
        Random random = new Random();
        for (int i = 0; i < keySets.length; ++i) {
            Integer a = intAbs(random.nextInt());
            Integer b = intAbs(random.nextInt());

            keySets[i] = PointTransform.getLong(a, b);
        }
    }

    private void generateSquare(Long[] keySets) {
        for (int i = 0; i < range; ++i) {
            for (int j = 0; j < range; ++j) {
                keySets[i * range + j] = Long.valueOf(i * range + j);
            }
        }
    }

    public static void main(String args[]) {
        KaryPerformance karyPerformance = new KaryPerformance();
        //parse
        range = Integer.parseInt(args[0]);
        nThread = Integer.parseInt(args[1]);
        miscelleuous = Integer.parseInt(args[2]);
        nonUniform = Integer.parseInt(args[3]);
        insert = Integer.parseInt(args[4]);
        remove = Integer.parseInt(args[5]);
        contain = Integer.parseInt(args[6]);

        //8 cases, 3 for warmup, 5 for test
        for (int i = 0; i < 8; ++i) {
            karyPerformance.run();
        }
    }

    public void run() {
        LockFree4ST<Long, Object> kary = new LockFree4ST<>();

        final Random random = new Random();
        Long[] keySets = null;
        //to ensure start at the same time
        final CyclicBarrier gate = new CyclicBarrier(nThread + 1);
        final Thread[] threads = new Thread[nThread];
        int initialCount = 0;

        ExecutorService executorService = Executors.newFixedThreadPool(nThread);

        if (nonUniform == 0) {
            keySets = new Long[range + 1];
            generateKeySets(keySets);

            //init half range, set up initial nodes
            while (initialCount < range / 2){
                Long keySet = keySets[random.nextInt(range)];
                if (kary.putIfAbsent(keySet, new Object()) == null) {
                    ++initialCount;
                }
            }
        } else {
            keySets = new Long[range * range + 1];
            generateSquare(keySets);

            //init half range, set up initial nodes
            while (initialCount < range * range / 2) {
                Long keySet = keySets[random.nextInt(range * range)];
                if (kary.putIfAbsent(keySet, new Object()) == null) {
                    ++initialCount;
                }
            }
        }

        for (int i = 0; i < nThread; ++i) {
            final int threadId = i;
            final Long[] keys = keySets;
            threads[i] = new ThreadLoopTime(threadId, keySets, insert, remove, contain, move,
                    nonUniform, range, gate, kary);
        }

        for (int i = 0; i < nThread; ++i) {
            threads[i].start();
        }

        try {
            gate.await();
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (BrokenBarrierException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        long start = System.nanoTime();
        long end = System.nanoTime();
        while (end - start < 1e9 * 1) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            end = System.nanoTime();
        }
        //to interrupt after 5s
        for (int i = 0; i < nThread; ++i) {
            ((ThreadLoopTime)threads[i]).stopThread();
        }
        for (int i = 0; i < nThread; ++i) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long totalSuccCount = 0;
        for (int i = 0; i < nThread; ++i) {
            totalSuccCount += ((ThreadMisc) threads[i]).getContainOperationCount();
            totalSuccCount += ((ThreadMisc) threads[i]).getInsertOperationCount();
            totalSuccCount += ((ThreadMisc) threads[i]).getRemoveOperationCount();
        }


        double duration = end - start;
        logger.info(choose + " throughput :" + totalSuccCount / (duration / 1000000000));

        if (miscelleuous == 1) {
            throw new NotImplementedException();
        }
    }
}
