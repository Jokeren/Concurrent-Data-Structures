package com.jokeren.concurrent.structures.test;

import com.jokeren.concurrent.structures.quadtree.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/7.
 */
public class QuadtreeFunctionTest implements FunctionTest{
    final Quadtree<Object> quadtree = new QuadStackLCAPure<>();
    Logger logger = Logger.getLogger("QuadStackLCAPure");

    public static void main(String args[]) {
        for (int i = 0; i < 2000; ++i) {
            QuadtreeFunctionTest functionTest = new QuadtreeFunctionTest();
            try {
                Method method = functionTest.getClass().getMethod("moveTest");
                method.invoke(functionTest);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateKeySets(KeySet[] keySets) {
        Random random = new Random();
        for (int i = 0; i < keySets.length; ++i) {
            keySets[i] = new KeySet(Math.abs(random.nextInt()), Math.abs(random.nextInt()));
        }
    }


    @Override
    public void insertTest() {
        final int threadCount = 1;
        final int iteration = 18;
        final int range = 20000;
        final KeySet[] keySets = new KeySet[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 1; i < iteration; ++i) {
//                        KeySet keySet = keySets[random.nextInt(range)];
                        if (quadtree.insert(i, i, new Object())) {
                            succCount.incrementAndGet();
                        }
                    }
                    logger.info("thread finish");
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

        for (int i = 1; i < iteration; ++i) {
            if (!quadtree.contains(i, i)) {
                logger.info("here");
            }
        }
        testReport(succCount.get(), quadtree.size());
    }

    @Override
    public void removeTest() {
        final int threadCount = 1;
        final Random random = new Random();
        final int iteration = 1000;
        final int range = 100;
        final KeySet[] keySets = new KeySet[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        //set up initial nodes
        for (int i = 0; i < iteration / 2; ++i) {
            KeySet keySet = keySets[random.nextInt(range)];
            if (quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object())) {
                succCount.incrementAndGet();
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        KeySet keySet = keySets[random.nextInt(range)];
                        if (quadtree.remove(keySet.getKeyX(), keySet.getKeyY())) {
                            succCount.decrementAndGet();
                        }
                    }
                    logger.info("thread finish");
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

        testReport(succCount.get(), quadtree.size());
    }

    @Override
    public void moveTest() {
        final int threadCount = 1;
        final Random random = new Random();
        final int iteration = 40000;
        final int range = 4000;
        final KeySet[] keySets = new KeySet[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        //set up initial nodes
        for (int i = 0; i < range / 2; ++i) {
            KeySet keySet = keySets[random.nextInt(range)];

            if (quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object())) {
                succCount.incrementAndGet();
//                logger.info(keySet.keyX + " " + keySet.keyY);
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        KeySet oldKeySet = keySets[random.nextInt(range)];
                        KeySet newKeySet = keySets[random.nextInt(range)];
                        if (newKeySet.getKeyX() != oldKeySet.getKeyX()
                                || newKeySet.getKeyY() != oldKeySet.getKeyY()) {
                           if (quadtree.move(oldKeySet.getKeyX(), oldKeySet.getKeyY(),
                                   newKeySet.getKeyX(), newKeySet.getKeyY())) {
                               //
                           }
                        }
                    }
                    logger.info("thread finish");
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

        testReport(succCount.get(), quadtree.size());
    }

    @Override
    public void mixedTest() {
        final int threadCount = 4;
        final Random random = new Random();
        final int iteration = 10000;
        final int range = 100;
        final KeySet[] keySets = new KeySet[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);
        long start = System.nanoTime();

        //set up initial nodes
        for (int i = 0; i < range / 2; ++i) {
            KeySet keySet = keySets[random.nextInt(range)];
            if (quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object())) {
                succCount.incrementAndGet();
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    int operation;
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        KeySet keySet = keySets[random.nextInt(range)];
                        operation = random.nextInt(100);
                        if (operation < 20) {
                            if (quadtree.remove(keySet.getKeyX(), keySet.getKeyY())) {
                                succCount.decrementAndGet();
                            }
                        } else if (operation < 40){
                            if (quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object())) {
                                succCount.incrementAndGet();
                            }
                        } else {
                            KeySet moveKeySet = keySets[random.nextInt(range)];
                            if (moveKeySet.getKeyX() != keySet.getKeyX() || moveKeySet.getKeyY() != keySet.getKeyY()) {
                                quadtree.move(keySet.getKeyX(), keySet.getKeyY(), moveKeySet.getKeyX(), moveKeySet.getKeyY());
                            }
                        }
                    }
                    //logger.info("thread finish");
                    latch.countDown();
                }
            });
        }

        executorService.shutdown();

        long end = System.nanoTime();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testReport(quadtree.size(), succCount.get());
        long duration = end - start;
        logger.info("Running time : " + duration);
    }

    @Override
    public void testReport(int succCount, int actualSize) {
        logger.info("count size : " + succCount);
        logger.info("actual size : " + actualSize);
        assert(succCount == actualSize);
    }
}
