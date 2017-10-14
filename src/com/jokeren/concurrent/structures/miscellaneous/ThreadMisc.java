package com.jokeren.concurrent.structures.miscellaneous;

/**
 * Created by robin on 2015/12/3.
 */
public interface ThreadMisc {
    public int getContainOperationCount();
    public long getContainResponseTime();

    public long getInsertResponseTime();
    public long getRemoveResponseTime();

    public long getMoveResponseTime();

    public int getInsertOperationCount();

    public int getRemoveOperationCount();

    public int getMoveOperationCount();
}
