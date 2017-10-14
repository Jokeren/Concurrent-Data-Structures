package com.jokeren.concurrent.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

/**
 * Created by robin on 2015/11/17.
 */
public class PointTransform {
    private static String interleave(String a, String b) {
        if (a.length() != b.length()) {
            //complement by 0
            int maxLength = Math.max(a.length(), b.length());
            int t = 0;
            String c;
            if (a.length() < maxLength) {
                t = maxLength - a.length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < t; ++i) {
                    sb.append(0);
                }
                a = sb.append(a).toString();
            } else {
                t = maxLength - b.length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < t; ++i) {
                    sb.append(0);
                }
                b = sb.append(b).toString();
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length(); ++i) {
            sb.append(a.charAt(i));
            sb.append(b.charAt(i));
        }

        return sb.toString();
    }

    public static String getString(Object x, Object y) {
        Method method = null;
        try {
            method= x.getClass().getDeclaredMethod("toBinaryString", Integer.TYPE);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        String a = null, b = null;
        try {
            a = (String) method.invoke(x, x);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        try {
            b = (String) method.invoke(y, y);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return interleave(a, b);
    }

    public static long getLong(Object x, Object y) {
        String s = getString(x, y);
        return new BigInteger(s, 2).longValue();
    }

    public static void main(String args[]) {
        Integer x = 100;
        Integer y = 10;
        String s = getString(x, y);
        System.out.println(s);
        Long z = getLong(x, y);
        System.out.println(z);
    }
}
