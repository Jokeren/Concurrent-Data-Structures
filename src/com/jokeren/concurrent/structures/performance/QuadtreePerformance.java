package com.jokeren.concurrent.structures.performance;

import com.jokeren.concurrent.structures.miscellaneous.QuadtreeMisc;
import com.jokeren.concurrent.structures.miscellaneous.ThreadMisc;
import com.jokeren.concurrent.structures.quadtree.*;
import com.jokeren.concurrent.utils.Performance;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by robin on 2015/11/17.
 */
public class QuadtreePerformance implements Performance {
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
    Logger logger = Logger.getLogger("QuadtreePerformance");

    private void generateSquare(KeySet[] keySets) {
        for (int i = 0; i < range; ++i) {
            for (int j = 0; j < range; ++j) {
                keySets[i * range + j] = new KeySet(i, j);
            }
        }
    }

    private void generateNonUniformKeySets(KeySet[] keySets) {
        Random random = new Random();
        for (int i = 0; i < keySets.length / 100; ++i) {
            double p = random.nextDouble() * Integer.MAX_VALUE + 1.0f;
            int j = 0;
            for (int pX = 0; pX < 10; ++pX) {
                for (int pY = 0; pY < 10; ++pY) {
                    keySets[i * 100 + j] = new KeySet(p + 0.0001f * pX, p + 0.0001f * pY);
                    ++j;
                }
            }
        }
    }

    private void generateUniformKeySets(KeySet[] keySets) {
        Random random = new Random();
        for (int i = 0; i < keySets.length; ++i) {
            keySets[i] = new KeySet(random.nextDouble() * Integer.MAX_VALUE, random.nextDouble() * Integer.MAX_VALUE);
        }
    }

    private Quadtree getQuadtree() {
        Quadtree quadtree = null;
        Double maxH = (double)Integer.MAX_VALUE + 1, maxW = (double)Integer.MAX_VALUE + 1;
        if (nonUniform == 1) {
            maxH = maxW = (double)range;
        }
        switch (choose) {
            case "qbp"://QuadBasicPure
                quadtree = new QuadBasicPure<Object>(maxH, maxW);
                break;
            case "qop":
                quadtree = new QuadOneParentPure<Object>(maxH, maxW);
                break;
            case "qsp":
                quadtree = new QuadStackPure<Object>(maxH, maxW);
                break;
            case "qslp":
                quadtree = new QuadStackLCAPure<Object>(maxH, maxW);
                break;
            case "qfp":
                quadtree = new QuadFlagPure<Object>(maxH, maxW);
                break;
            case "qfdp":
                quadtree = new QuadFlagDecouplePure<>(maxH, maxW);
                break;
            case "qcp":
                quadtree = new QuadStackLCAPure<>(maxH, maxW);
                break;
            default:
                break;
        }

        return quadtree;
    }

    public static void main(String args[]) {
        QuadtreePerformance quadtreePerformance = new QuadtreePerformance();
        //parse
        choose = args[0];
		range = Integer.parseInt(args[1]);
        nThread = Integer.parseInt(args[2]);
        miscelleuous = Integer.parseInt(args[3]);
        nonUniform = Integer.parseInt(args[4]);
		insert = Integer.parseInt(args[5]);
		remove = Integer.parseInt(args[6]);
		contain = Integer.parseInt(args[7]);
        move = Integer.parseInt(args[8]);

        if (nonUniform == 3 || nonUniform == 4 || nonUniform == 5) {
            //12 cases, 7 for warmup, 5 for test
            for (int i = 0; i < 12; ++i) {
                quadtreePerformance.run();
            }
        } else {
            //8 cases, 3 for warmup, 5 for test
            for (int i = 0; i < 8; ++i) {
                quadtreePerformance.run();
            }
        }
    }

    public void run() {
        Quadtree quadtree = getQuadtree();
        final Random random = new Random();
        KeySet[] keySets = null;
        //to ensure start at the same time
        final CyclicBarrier gate = new CyclicBarrier(nThread + 1);
        final Thread[] threads = new Thread[nThread];
        int time = 1;
        int initialCount = 0;

        switch (nonUniform) {
            case 0://uniform
                keySets = new KeySet[range + 1];
                generateUniformKeySets(keySets);
                for (int i = 0; i < keySets.length / 2; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            case 1://uniform square
                keySets = new KeySet[range * range + 1];
                generateSquare(keySets);
                for (int i = 0; i < keySets.length / 2; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            case 2:
                keySets = new KeySet[range + 1];
                generateNonUniformKeySets(keySets);
                for (int i = 0; i < keySets.length / 2; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            case 3://insert mode
                keySets = new KeySet[range + 1];
                generateUniformKeySets(keySets);
                break;
            case 4://remove mode
                keySets = new KeySet[range + 1];
                generateUniformKeySets(keySets);
                for (int i = 0; i < keySets.length; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            case 5://lookup mode
                keySets = new KeySet[range + 1];
                generateUniformKeySets(keySets);
                for (int i = 0; i < keySets.length; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            case 6://uniform
                time = 5;
                keySets = new KeySet[range + 1];
                generateUniformKeySets(keySets);
                for (int i = 0; i < keySets.length / 2; ++i) {
                    KeySet keySet = keySets[i];
                    quadtree.insert(keySet.getKeyX(), keySet.getKeyY(), new Object());
                }
                break;
            default:
                break;
        }

        if (miscelleuous == 1) {
            QuadtreeMisc misc = (QuadtreeMisc) quadtree;
            misc.resetMisc();
        }

        long start = 0, end = 0;
        for (int i = 0; i < nThread; ++i) {
            final int threadId = i;
            if (nonUniform == 3 || nonUniform == 4 || nonUniform == 5) {
                threads[i] = new ThreadLoopCase(threadId, keySets, insert, remove, contain, move,
                        nonUniform, range, gate, quadtree);
            } else {
                threads[i] = new ThreadLoopTime(threadId, keySets, insert, remove, contain, move,
                        nonUniform, range, gate, quadtree);
            }
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
        start = System.nanoTime();

        if (nonUniform == 3 || nonUniform == 4 || nonUniform == 5) {
            for (int i = 0; i < nThread; ++i) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            end = System.nanoTime();
        } else {
            end = System.nanoTime();
            while (end - start < 1e9 * time) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                end = System.nanoTime();
            }

            //to interrupt after 3s
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
        }

        long totalSuccCount = 0;
        long totalInsertCount = 0;
        long totalRemoveCount = 0;
        long totalContainCount = 0;
        long totalMoveCount = 0;
        double averageInsertRespondingTime = 0.0f;
        double averageRemoveRespondingTime = 0.0f;
        double averageContainRespondingTime = 0.0f;
        double averageMoveResponseTime = 0.0f;

        for (int i = 0; i < nThread; ++i) {
            ThreadMisc threadMisc = (ThreadMisc) threads[i];
            averageInsertRespondingTime += threadMisc.getInsertResponseTime();
            averageRemoveRespondingTime += threadMisc.getRemoveResponseTime();
            averageContainRespondingTime += threadMisc.getContainResponseTime();
            averageMoveResponseTime += threadMisc.getMoveResponseTime();
            totalInsertCount += threadMisc.getInsertOperationCount();
            totalRemoveCount += threadMisc.getRemoveOperationCount();
            totalContainCount += threadMisc.getContainOperationCount();
            totalMoveCount += threadMisc.getMoveOperationCount();
        }

        totalSuccCount = totalContainCount + totalInsertCount + totalRemoveCount + totalMoveCount;
        if (totalInsertCount != 0)
            averageInsertRespondingTime /= totalInsertCount;

        if (totalRemoveCount != 0)
            averageRemoveRespondingTime /= totalRemoveCount;

        if (totalContainCount != 0)
            averageContainRespondingTime /= totalContainCount;

        if (totalMoveCount != 0)
            averageMoveResponseTime /= totalMoveCount;

        double duration = end - start;
        logger.info(choose + " throughput :" + totalSuccCount / (duration / 1000000000L));

        if (miscelleuous == 1) {
            logger.info(choose + " insert rt :" + averageInsertRespondingTime);
            logger.info(choose + " remove rt :" + averageRemoveRespondingTime);
            logger.info(choose + " contain rt :" + averageContainRespondingTime);
            logger.info(choose + " move rt :" + averageMoveResponseTime);
            logger.info(choose + " insert op :" + totalInsertCount);
            logger.info(choose + " remove op :" + totalRemoveCount);
            logger.info(choose + " contain op :" + totalContainCount);
            logger.info(choose + " move op :" + totalMoveCount);
            if (choose.equals("qb")) {
                logger.info(choose + " qb insert cas success count :" + totalInsertCount);
                logger.info(choose + " qb remove cas success count :" + totalRemoveCount);
            } else {
                logger.info(choose + " insert cas success count :" + (totalInsertCount * 3));
                logger.info(choose + " remove cas success count :" + (totalRemoveCount * 3));
            }

            logger.info(choose + " size :" + quadtree.size());
            QuadtreeMisc misc = (QuadtreeMisc) quadtree;
            logger.info(choose + " all nodes :" + misc.allNodes());
            logger.info(choose + " max depth :" + misc.maxDepth());
            logger.info(choose + " avg depth :" + misc.averageDepth());
            logger.info(choose + " useless :" + misc.uselessInternal());
            logger.info(choose + " insertPath :" + misc.insertSuccessPath());
            logger.info(choose + " containPath :" + misc.containSuccessPath());
            logger.info(choose + " pendingPath :" + misc.pendingSuccessPath());
            logger.info(choose + " removePath :" + misc.removeSuccessPath());
            logger.info(choose + " compressPath :" + misc.compressSuccessPath());
            logger.info(choose + " newNodeCount :" + misc.newNodeCreate());
            logger.info(choose + " casFailures :" + misc.casFailures());
            logger.info(choose + " casTimeCount :" + misc.casTime());
        }
    }
}
