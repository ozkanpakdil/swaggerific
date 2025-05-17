package io.github.ozkanpakdil.swaggerific.algorithms;

import java.util.List;

public class StreamTest {
    public static void main(String[] args) {
        int[] factor = new int[] { 2 };
        var numbers = List.of(1, 2, 3);

        var stream = numbers.stream()
                .map(number -> number * factor[0]);
        factor[0] = 0;
        stream.forEach(System.out::print);
    }
}
