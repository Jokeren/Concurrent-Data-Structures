package com.jokeren.concurrent.structures.test;


/**
 * Created by robin on 2015/11/15.
 */
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.jokeren.concurrent.structures.bst.*;

public class BSTFunctionTest implements FunctionTest{
    Map<Integer, Integer> tree;

    private void generate(int[][] cases, int nThread, int num, int range) {
        Random random = new Random();
        for (int k = 0; k < nThread; k++) {
            for (int i = 0; i < num; i++) {
                int key = random.nextInt(range);
                cases[k][i] = key;
            }
        }
    }

    @Override
    public void addTest() {
        // TODO Auto-generated method stub
        final int ntestCase = 3000;
        final int nThread = 4;
        final int perThread = ntestCase / nThread;
        Thread[] thread = new Thread[nThread];
        tree = new FlagEdgeMarkedBST<Integer, Integer>();

        for (int i = 0; i < nThread; i++) {
            final int idx = i * perThread;
            thread[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    for (int j = 0; j < perThread; j++) {
                        int key = idx + j + 1;
                        tree.put(key, key);
                    }
                }
            });
        }

        long start = System.nanoTime();
        for (int i = 0; i < nThread; i++) {
            thread[i].start();
        }

        for (int i = 0; i < nThread; i++) {
            try {
                thread[i].join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long end = System.nanoTime();
        System.out.println(" time" + (end - start));
        for (int i = 1; i < ntestCase + 1; i++) {
            int key = i;
            if (!tree.containsKey(key)) {
                System.out.println("error!!!");
            }
        }
    }

    @Override
    public void largeCaseTest() {
        // TODO Auto-generated method stub
        final int nTestCase = 100000;
        final int range = 1000;
        final int nThread = 4;
        final int[][] cases = new int[nThread][nTestCase];
        Thread[] thread = new Thread[nThread];
        tree = new FlagEdgeMarkedBST<Integer, Integer>();
        generate(cases, nThread, nTestCase, range);
        final AtomicInteger nCount = new AtomicInteger(0);

        for (int i = 0; i < nThread; i++) {
            final int idx = i;
            thread[i] = new Thread(new Runnable() {
                Random random = new Random();

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    int operation = 0;
                    for (int j = 0; j < nTestCase; j++) {
                        int key = cases[idx][j];
                        operation = random.nextInt(100);
                        if (operation < 50) {
                            if (tree.remove(key) != null)
                                nCount.decrementAndGet();
                        } else if (operation < 100) {
                            if (tree.put(key, key) != null)
                                nCount.incrementAndGet();
                        } else {
                            tree.containsKey(key);
                        }
                    }
                }
            });
        }

        long start = System.nanoTime();
        for (int i = 0; i < nThread; i++) {
            thread[i].start();
        }

        for (int i = 0; i < nThread; i++) {
            try {
                thread[i].join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long end = System.nanoTime();
        int size = tree.size();
        System.out.println("time: " + (end - start));
        System.out.println("nCount size: " + nCount.get());
        System.out.println("Real size: " + size);
    }

    @Override
    public void removeTest() {
        // TODO Auto-generated method stub
        final int ntestCase = 300;
        final int nThread = 10;
        final int perThread = ntestCase / nThread;
        Thread[] thread = new Thread[nThread];
        tree = new FlagEdgeMarkedBST<Integer, Integer>();

        for (int i = 0; i < ntestCase; i++) {
            int key = i + 1;
            tree.put(key, key);
        }

        final AtomicInteger nCount = new AtomicInteger(0);
        for (int i = 0; i < nThread; i++) {
            final int idx = i * perThread;
            thread[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    for (int j = 0; j < perThread; j++) {
                        int key = idx + j + 1;
                        if (tree.remove(key) != null) {
                            nCount.incrementAndGet();
                        }
                    }
                }
            });
        }

        long start = System.nanoTime();
        for (int i = 0; i < nThread; i++) {
            thread[i].run();
        }

        for (int i = 0; i < nThread; i++) {
            try {
                thread[i].join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        long end = System.nanoTime();
        System.out.println(nCount.get() + " time" + (end - start));

        for (int i = 0; i < ntestCase; i++) {
            int key = i + 1;
            if (tree.containsKey(key)) {
                System.out.println("error!!! " + (i + 1));
            }
        }
    }

    @Override
    public void sequentialTest() {
        // TODO Auto-generated method stub
        tree = new FlagNodeBST<Integer, Integer>();

        for (int i = 1; i < 128; i++) {
            int key = i;
            System.out.println(tree.put(key, key));
        }

        for (int i = 1; i < 128; i++) {
            int key = i;
            if (!tree.containsKey(key)) {
                System.out.println("error!!! key " + i + ". Not exist!");
            }
        }

        for (int i = 1; i < 128; i++) {
            int key = i;
            if (tree.remove(key) == null) {
                System.out.println("error!!! remove key " + i + ".");
            }
        }

        for (int i = 1; i < 128; i++) {
            int key = i;
            if (tree.containsKey(key)) {
                System.out.println("error!!! key " + i + ". exist!");
            }
        }
    }

    @Override
    public void smallCaseTest() {
        // TODO Auto-generated method stub

    }

    @Override
    public void testReport(int succCount, int actualSize) {

    }
}
