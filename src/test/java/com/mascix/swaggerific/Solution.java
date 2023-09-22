package com.mascix.swaggerific;

import java.io.IOException;
import java.util.Arrays;

public class Solution {
    public static void main(String[] args) throws IOException {
        int[] arr = new int[]{1, 3, 5, 7, 9};
        int K = 2;
        System.out.println(Arrays.toString(turnTheArray(arr, K)));





    }

    private static int[] turnTheArray(int[] nums, int k) {
        for (int j=0; j<k; j++)
        {
            for (int i=nums.length-1; 0<i; i--)
            {
                // shifting towards the right
                int temp = nums[i - 1];
                nums[i-1]=nums[i];
                nums[i]=temp;
            }
        }
        return nums;
    }
}
