package com.jokeren.concurrent.structures.test;

import com.jokeren.concurrent.structures.kary.FunctionTest;
import com.jokeren.concurrent.structures.kary.LockFree4ST;
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
public class KaryFunctionTest implements FunctionTest {
    final LockFree4ST<Long, Object> kary = new LockFree4ST<>();
    Logger logger = Logger.getLogger("KaryFunctionTest");

    public static void main(String args[]) {
        KaryFunctionTest karyFunctionTest = new KaryFunctionTest();
        try {
            Method method = karyFunctionTest.getClass().getMethod("mixedTest");
            method.invoke(karyFunctionTest);
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
                        if (kary.putIfAbsent(keySet, new Object()) == null) {
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

        testReport(succCount.get(), kary.size());
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
            if (kary.putIfAbsent(keySet, new Object()) == null) {
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
                        if (kary.remove(keySet) != null) {
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

        testReport(succCount.get(), kary.size());
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
            if (kary.putIfAbsent(keySet, new Object()) == null) {
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
                            if (kary.remove(keySet) != null) {
                                succCount.decrementAndGet();
                            }
                        } else if (operation < 60){
                            if (kary.putIfAbsent(keySet, new Object()) == null) {
                                succCount.incrementAndGet();
                            }
                        } else {
                            kary.containsKey(keySet);
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

        testReport(succCount.get(), kary.size());
    }

    @Override
    public void testReport(int succCount, int actualSize) {
        logger.info("kary succ size: " + succCount);
        logger.info("kary actual size: " + actualSize);
    }
}
