package io.github.ozkanpakdil.swaggerific.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RotateArray {
    public static void main(String[] args) throws IOException {
        int[] arr = new int[]{1, 3, 5, 7, 9};
        int K = 3;
        turnTheArrayToRight(arr, K);
        System.out.println("-----------");
        turnTheArrayToLeft(arr, K);

        System.out.println("-----------");
        // no need to reinvent the wheel.
        List l = new ArrayList<>();
        Arrays.stream(arr).forEach(i -> l.add(i));
        Collections.rotate(l, K); // rotate right
        System.out.println(Arrays.toString(l.toArray()));
        Collections.rotate(l, -1 * K); // rotate left
        System.out.println(Arrays.toString(l.toArray()));
    }

    private static int[] turnTheArrayToRight(int[] nums, int k) {
        for (int j = 0; j < k; j++) {
            for (int i = nums.length - 1; i > 0; i--) {
                int temp = nums[i - 1];
                nums[i - 1] = nums[i];
                nums[i] = temp;
            }
            System.out.println(Arrays.toString(nums));
        }
        return nums;
    }

    private static int[] turnTheArrayToLeft(int[] nums, int k) {
        for (int j = 0; j < k; j++) {
            for (int i = 0; i < nums.length - 1; i++) {
                int temp = nums[i];
                nums[i] = nums[i + 1];
                nums[i + 1] = temp;
            }
            System.out.println(Arrays.toString(nums));
        }
        return nums;
    }
}
