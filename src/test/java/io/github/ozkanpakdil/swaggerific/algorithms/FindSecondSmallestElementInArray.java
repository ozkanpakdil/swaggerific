package io.github.ozkanpakdil.swaggerific.algorithms;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

public class FindSecondSmallestElementInArray {

    @ParameterizedTest
    @MethodSource
    void findSecondSmallestJunit(int[] nums) {
        System.out.println(Arrays.toString(nums));
        System.out.println("%d".formatted(findSecondSmallest(nums)));
    }

    public static Stream<int[]> findSecondSmallestJunit() {
        return Stream.of(
                new int[]{13, 14, 65, 456, 31, 83},
                new int[]{4, 8, 9, 2, 1, 1},
                new int[]{1, 3, 3, 1, 6, 5, 4, 5, 6},
                new int[]{2, 3, 1, 6, 5, 4, 5, 6}
        );
    }

    public static int findSecondSmallest(int[] arr) {
        int firstSmallest = Integer.MAX_VALUE;
        int secondSmallest = Integer.MAX_VALUE;

        for (int num : arr) {
            if (num < firstSmallest) {
                secondSmallest = firstSmallest;
                firstSmallest = num;
            } else if (num < secondSmallest) {
                secondSmallest = num;
            }
        }

        return secondSmallest;
    }

    public static void main(String[] args) {
        int[] arr = {4, 8, 9, 2, 4, 2};
        int result = findSecondSmallest(arr);
        System.out.println(result); // Output: 1
        Arrays.sort(arr);
        System.out.println(arr[1]); // Output: 1
    }
}
