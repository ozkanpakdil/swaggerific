package com.mascix.swaggerific.algorithms;

public class MaxSumPathInMatrix {
    public static int maxSumPath(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;

        // Create a DP table to store the maximum sum at each cell
        int[][] dp = new int[rows][cols];

        // Initialize the DP table with the values from the bottom-left corner
        dp[rows - 1][0] = matrix[rows - 1][0];

        // Fill the DP table by iterating from bottom to top and from left to right
        for (int i = rows - 2; i >= 0; i--) {
            dp[i][0] = dp[i + 1][0] + matrix[i][0];
        }
        for (int j = 1; j < cols; j++) {
            dp[rows - 1][j] = dp[rows - 1][j - 1] + matrix[rows - 1][j];
        }

        for (int i = rows - 2; i >= 0; i--) {
            for (int j = 1; j < cols; j++) {
                dp[i][j] = matrix[i][j] + Math.max(dp[i + 1][j], dp[i][j - 1]);
            }
        }

        // The maximum sum path is the value at the top-right corner of the DP table
        return dp[0][cols - 1];
    }

    public static void main(String[] args) {
        int[][] matrix = {
                {0, 0, 1, 0, 3},
                {0, 1, 2, 1, 0},
                {2, 0, 5, 0, 0}
        };

        int maxSum = maxSumPath(matrix);
        System.out.println("Maximum sum path: " + maxSum); // Output: 8
    }
}
