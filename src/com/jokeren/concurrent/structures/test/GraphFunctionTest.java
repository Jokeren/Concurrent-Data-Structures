package com.jokeren.concurrent.structures.test;

import com.jokeren.concurrent.structures.graph.Graph;
import com.jokeren.concurrent.structures.graph.LockGraph;
import com.jokeren.concurrent.structures.graph.FunctionTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by robinho364 on 2015/9/21.
 */
public class GraphFunctionTest implements FunctionTest{
    final Graph<Object> graphMap = new LockGraph<>();

    public static void main(String args[]) {
        GraphFunctionTest function = new GraphFunctionTest();
        for (int i = 0; i < 10; ++i) {
            try {
                Method method = function.getClass().getMethod("mixNodeTest");
                method.invoke(function);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            function.graphMap.clear();
        }
    }

    @Override
    public void testReport(int succCount, int actualSize) {
        System.out.println("Count size : " + succCount);
        System.out.println("Actual size : " + actualSize);
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
                        Integer key = random.nextInt(upperBound);
                        if (graphMap.insertNode(key, key)) {
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

    }

    @Override
    public void deleteNodeTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int upperBound = 10000;
        final int iteration = 100;
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //insert nodes
        for (int i = 0; i < upperBound; ++i) {
            Integer key = i;
            graphMap.insertNode(key, key);
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        Integer key = random.nextInt(upperBound);
//                        System.out.println("delete : " + key);
                        if (graphMap.deleteNode(key)) {
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

    }

    @Override
    public void mixNodeTest() {
        final int threadCount = 4;
        final int upperBound = 8192;
        final int iteration = 10000;
        final AtomicInteger succCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    for (int i = 0; i < iteration; ++i) {
                        int ratio = random.nextInt(100);
                        Integer key = random.nextInt(upperBound);
//                        synchronized (graphMap) {
                        if (ratio < 20) {
                            if (graphMap.insertNode(key, key)) {
                                succCount.incrementAndGet();
                            }
                        } else if (ratio < 30) {
                            if (graphMap.deleteNode(key)) {
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

        long endTime = System.currentTimeMillis();
        System.out.println("Running Time : " + (endTime - startTime));

    }

    @Override
    public void insertEdgeTest() {
        final int threadCount = 10;
        final Random random = new Random();
        final int upperBound = 50;
        final int iteration = 1000;
        final AtomicInteger succNodeCount = new AtomicInteger(0);
        final AtomicInteger succEdgeCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //initialize nodes
        for (int i = 0; i < upperBound / 2; ++i) {
            graphMap.insertNode(i, i);
            succNodeCount.incrementAndGet();
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        Integer keyFrom = random.nextInt(upperBound);
                        Integer keyTo = random.nextInt(upperBound);
                        if (keyFrom != keyTo && graphMap.insertEdge(keyFrom, keyTo)) {
//                            System.out.println("keyFrom : " + keyFrom + ", keyTo : " + keyTo);
                            succEdgeCount.incrementAndGet();
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
    }

    @Override
    public void deleteEdgeTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int upperBound = 50;
        final int iteration = 1000;
        final AtomicInteger succEdgeCount = new AtomicInteger(0);
        final AtomicInteger succNodeCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //initialize nodes
        for (int i = 0; i < upperBound / 2; ++i) {
            graphMap.insertNode(i, i);
            succNodeCount.incrementAndGet();
        }

        //initialize edges
        for (int i = 0; i < upperBound / 2; ++i) {
            for (int j = 0; j < upperBound / 2; ++j) {
                graphMap.insertEdge(i, j);
                succEdgeCount.incrementAndGet();
            }
        }

        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        Integer keyFrom = random.nextInt(upperBound);
                        Integer keyTo = random.nextInt(upperBound);
                        if (keyFrom != keyTo && graphMap.deleteEdge(keyFrom, keyTo)) {
                            succEdgeCount.decrementAndGet();
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
    }

    @Override
    public void mixEdgeTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int upperBound = 100;
        final int iteration = 1000;
        final AtomicInteger succEdgeCount = new AtomicInteger(0);
        final AtomicInteger succNodeCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //initialize nodes
        for (int i = 0; i < upperBound / 2; ++i) {
            graphMap.insertNode(i, i);
            succNodeCount.incrementAndGet();
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        int ratio = random.nextInt(100);
                        Integer keyFrom = random.nextInt(upperBound);
                        Integer keyTo = random.nextInt(upperBound);
//                        synchronized (graphMap) {
                        if (ratio < 50) {
                            if (keyFrom != keyTo && graphMap.insertEdge(keyFrom, keyTo)) {
                                succEdgeCount.incrementAndGet();
                            }
                        } else {
                            if (keyFrom != keyTo && graphMap.deleteEdge(keyFrom, keyTo)) {
                                succEdgeCount.decrementAndGet();
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
    }

    @Override
    public void mixAllTest() {
        final int threadCount = 16;
        final Random random = new Random();
        final int upperBound = 2000;
        final int iteration = 10000;
        final AtomicInteger succNodeCount = new AtomicInteger(0);
        final AtomicInteger succEdgeCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //initialize nodes
//        for (int i = 0; i < upperBound / 2; ++i) {
//            graphMap.insertNode(i, i);
//            succNodeCount.incrementAndGet();
//        }

        long startTime = System.nanoTime();
        for (int i = 0; i < threadCount; ++i) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < iteration; ++i) {
                        int ratio = random.nextInt(100);
                        Integer keyFrom = random.nextInt(upperBound);
                        Integer keyTo = random.nextInt(upperBound);
//                        synchronized (graphMap) {
                        if (ratio < 10) {
//                            System.out.println("insertEdge");
                            if (keyFrom != keyTo && graphMap.insertEdge(keyFrom, keyTo)) {
                                succEdgeCount.incrementAndGet();
                            }
                        } else if (ratio < 20) {
//                            System.out.println("deleteEdge");
                            if (keyFrom != keyTo && graphMap.deleteEdge(keyFrom, keyTo)) {
                                succEdgeCount.decrementAndGet();
                            }
                        } else if (ratio < 30) {
//                            System.out.println("insertNode");
                            if (graphMap.insertNode(keyFrom, keyFrom)) {
                                succNodeCount.incrementAndGet();
                            }
                        } else {
//                            System.out.println("deleteNode");
                            if (graphMap.deleteNode(keyFrom)) {
                                succNodeCount.decrementAndGet();
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
    }
}
