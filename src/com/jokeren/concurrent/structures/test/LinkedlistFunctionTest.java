package com.jokeren.concurrent.structures.test;

import com.jokeren.concurrent.structures.linkedlist.*;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robinho364 on 2015/9/22.
 */
public class LinkedlistFunctionTest implements FunctionTest {
    final Linkedlist<Object> linkedlist = new KerenLinkedlist<>();

    public static void main(String args[]) {
        LinkedlistFunctionTest linkedlistFunctionTest = new LinkedlistFunctionTest();
        try {
            Method method = linkedlistFunctionTest.getClass().getMethod("insertNodeTest");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void testReport(int succCount, int actualSize) {
        System.out.println("Actual node size : " + actualSize);
        System.out.println("Count node size : " + succCount);
    }

    @Override
    public void insertNodeTest() {
        final int threadCount = 4;
        final Random random = new Random();
        final int upperBound = 30;
        final int iteration = 100;
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        Integer key = Math.abs(random.nextInt()) % upperBound;
                        if (linkedlist.insert(key, key)) {
                            succCount.incrementAndGet();
                        }
                    }
                    System.out.println("thread finish");
                    latch.countDown();
                }
            });
        }

        executorService.shutdown();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testReport(succCount.get(), linkedlist.size());
    }

    @Override
    public void deleteNodeTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int upperBound = 1000;
        final int iteration = 100;
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //insert nodes
        for (int i = 0; i < upperBound; ++i) {
            Integer key = i;
            linkedlist.insert(key, key);
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        Integer key = Math.abs(random.nextInt()) % upperBound;
//                        System.out.println("delete : " + key);
                        if (linkedlist.delete(key)) {
                            succCount.incrementAndGet();
                        }
                    }
                    System.out.println("thread finish");
                    latch.countDown();
                }
            });
        }

        executorService.shutdown();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testReport(upperBound - succCount.get(), linkedlist.size());
    }

    @Override
    public void mixNodeTest() {
        final int threadCount = 4;
        final Random random = new Random();
        final int upperBound = 100;
        final int iteration = 200000;
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        long startTime = System.nanoTime();
        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        int ratio = random.nextInt() % 100;
                        Integer key = Math.abs(random.nextInt()) % upperBound;
//                        synchronized (graphMap) {
                        if (ratio < 50) {
                            if (linkedlist.insert(key, key)) {
                                succCount.incrementAndGet();
                            }
                        } else {
                            if (linkedlist.delete(key)) {
                                succCount.decrementAndGet();
                            }
                        }
//                        }
                    }
                    latch.countDown();
                    System.out.println("thread finish");
                }
            });
        }

        executorService.shutdown();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        System.out.println("Running Time : " + (endTime - startTime));
        testReport(succCount.get(), linkedlist.size());
    }
}
