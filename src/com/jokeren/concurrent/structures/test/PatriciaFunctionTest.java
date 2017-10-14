package com.jokeren.concurrent.structures.test;

import com.jokeren.concurrent.structures.pattree.FunctionTest;
import com.jokeren.concurrent.structures.pattree.PatriciaTree;
import com.jokeren.concurrent.utils.PointTransform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/18.
 */
public class PatriciaFunctionTest implements FunctionTest{
    final PatriciaTree<Object> patriciaTree = new PatriciaTree<>();
    Logger logger = Logger.getLogger("PatriciaFunctionTest");

    public static void main(String args[]) {
        PatriciaFunctionTest patriciaFunctionTest = new PatriciaFunctionTest();
        try {
            Method method = patriciaFunctionTest.getClass().getMethod("mixedTest");
            method.invoke(patriciaFunctionTest);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private long longAbs(long num) {
        return num < 0 ? -num : num;
    }
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

    @Override
    public void insertTest() {
        final int threadCount = 4;
        final int iteration = 100;
        final int range = 1000;
        final Long[] keySets = new Long[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        Long keySet = keySets[random.nextInt(range)];
                        if (patriciaTree.insert(keySet, new Object()) != null) {
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

        testReport(succCount.get(), patriciaTree.size() - 2);
    }

    @Override
    public void removeTest() {
        final int threadCount = 4;
        final int iteration = 100;
        final int range = 1000;
        final Long[] keySets = new Long[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        Random random = new Random();
        //set up initial nodes
        for (int i = 0; i < range / 2; ++i) {
            Long keySet = keySets[random.nextInt(range)];
            if (patriciaTree.insert(keySet, new Object()) != null) {
                succCount.incrementAndGet();
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        Long keySet = keySets[random.nextInt(range)];
                        if (patriciaTree.delete(keySet, new Object()) != null) {
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

        testReport(succCount.get(), patriciaTree.size() - 2);
    }

    @Override
    public void moveTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int iteration = 40000;
        final int range = 4000;
        final Long[] keySets = new Long[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        //set up initial nodes
        for (int i = 0; i < range / 2; ++i) {
            Long keySet = keySets[random.nextInt(range)];
            if (patriciaTree.insert(keySet, new Object()) != null) {
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
                        Long oldKeySet = keySets[random.nextInt(range)];
                        Long newKeySet = keySets[random.nextInt(range)];
                        if (oldKeySet != newKeySet) {
                            patriciaTree.move(oldKeySet, new Object(), newKeySet, new Object());
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

        testReport(succCount.get(), patriciaTree.size() - 2);
    }

    @Override
    public void mixedTest() {
        final int threadCount = 16;
        final int iteration = 100;
        final int range = 1000;
        final Long[] keySets = new Long[range];
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        generateKeySets(keySets);

        Random random = new Random();
        //set up initial nodes
        for (int i = 0; i < range / 2; ++i) {
            Long keySet = keySets[random.nextInt(range)];
            if (patriciaTree.insert(keySet, new Object()) != null) {
                succCount.incrementAndGet();
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    int operation;
                    for (int i = 0; i < iteration; ++i) {
                        Long keySet = keySets[random.nextInt(range)];
                        operation = random.nextInt(100);
                        if (operation < 20) {
                            if (patriciaTree.delete(keySet, new Object()) != null) {
                                succCount.decrementAndGet();
                            }
                        } else if (operation < 60){
                            if (patriciaTree.insert(keySet, new Object()) != null) {
                                succCount.incrementAndGet();
                            }
                        } else if (operation < 80) {
                            Long moveKeySet = keySets[random.nextInt(range)];
                            if (moveKeySet != keySet) {
                                patriciaTree.move(keySet, new Object(), moveKeySet, new Object());
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

        testReport(succCount.get(), patriciaTree.size() - 2);
    }

    @Override
    public void testReport(int succCount, int actualSize) {
        logger.info("patricia succ size: " + succCount);
        logger.info("patricia actual size: " + actualSize);
    }
}
