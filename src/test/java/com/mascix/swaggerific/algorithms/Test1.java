package com.mascix.swaggerific.algorithms;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class Test1 {
    public static int beautifulBinaryString(String b) {
        int r=0;
        while(b.contains("010")){
            b=b.replaceFirst("010", "111");
            r++;
        }
        return r;
    }

    public static void main(String[] args) {
        System.out.println(beautifulBinaryString("10110101101010001111011100100001010001111010110000111100110110111110011011000111100010011100111"));
    }

    @Test
    public void whenFilterEmployees_thenGetFilteredStream() {
        Arrays.asList("come on!", "I", "love", "lisp", "because", "is", "cool")
                .stream()
                .filter(s -> s.startsWith("c"))
                .map(String::toUpperCase)
                .sorted()
                .forEach(System.out::println);
    }
}
