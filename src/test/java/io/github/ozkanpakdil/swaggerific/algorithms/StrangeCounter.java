package io.github.ozkanpakdil.swaggerific.algorithms;

import java.util.HashMap;

public class StrangeCounter {
    public static long strangeCounter(long t) {
        HashMap<Integer,Integer> m=new HashMap<>();
        m.put(1, 3);
        m.put(2, 2);
        m.put(3, 1);
        m.put(4, 6);
        m.put(5, 5);
        m.put(6, 4);
        m.put(7, 3);
        m.put(8, 2);
        m.put(9, 1);
        m.put(10, 12);
        m.put(11, 11);
        m.put(12, 10);
        m.put(13, 9);
        m.put(14, 8);
        m.put(15, 7);
        m.put(16, 6);
        m.put(17, 5);
        m.put(18, 4);
        m.put(19, 3);
        m.put(20, 2);
        m.put(21, 1);
        int s= (int) (t%21);
        return m.get(s);
    }

    public static void main(String[] args) {
        System.out.println(strangeCounter(1000));
    }
}
