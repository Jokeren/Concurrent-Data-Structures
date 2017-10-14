package com.jokeren.concurrent.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robin on 2015/12/2.
 */
public class ThreadId {
    private static final AtomicInteger startId = new AtomicInteger(0);

    public static int getThreadId() {
        return startId.getAndIncrement();
    }
}
