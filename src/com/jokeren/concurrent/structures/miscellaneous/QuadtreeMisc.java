package com.jokeren.concurrent.structures.miscellaneous;

/**
 * Created by robin on 2015/11/17.
 */
public interface QuadtreeMisc {
    public int allNodes();

    public int maxDepth();

    public int averageDepth();

    public int uselessInternal();

    public int insertSuccessPath();

    public int pendingSuccessPath();

    public int containSuccessPath();

    public int removeSuccessPath();

    public int compressSuccessPath();

    public int newNodeCreate();

    public void resetMisc();

    public int casFailures();

    public long casTime();

    //cas failures
    //op allocations
}
