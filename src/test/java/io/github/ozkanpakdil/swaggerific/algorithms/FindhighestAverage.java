package io.github.ozkanpakdil.swaggerific.algorithms;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class FindhighestAverage {

    @Test
    void findSecondSmallestJunit() {
        //  Given a 2-D String array of student-marks find the student with the highest average and output his average score. If the average is in decimals, floor it down to the nearest integer.

        String[][] scores = new String[][]{{"Bob", "87"},
                {"Mike", "35"},
                {"Bob", "52"},
                {"Jason", "35"},
                {"Mike", "55"},
                {"Jessica", "99"}};
        HashMap<String, Integer> m = new HashMap();
        HashMap<String, Integer> f = new HashMap();
        for (String[] s : scores) {
            m.put(s[0], m.getOrDefault(s[0], 0) + Integer.valueOf(s[1]));
            f.put(s[0], f.getOrDefault(s[0], 0) + 1);
        }
        int highest = 0;
        for (String k : m.keySet()) {
            int avg = m.get(k) / f.get(k);
            if (avg > highest)
                highest = avg;
        }

        System.out.println(m);
        System.out.println(f);
        System.out.println(highest);
    }

    @Test
    void anotherQ() {
        // Q1 -- given a String like aaabbbcccaa need to return output like a3b3c3a2.
        String input = "aaabbbcccazzdddxx";
        char[] arr = input.toCharArray();

        for (int i = 0; i < arr.length; i++) {
            char c = arr[i];
            int charCounter = 1;
            System.out.print(c);
            while (i + 1 < arr.length && c == arr[i + 1]) {
                charCounter++;
                i++;
            }
            System.out.print(charCounter);
        }

        System.out.println("\n" + transformString(input));
    }

    public static String transformString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        int count = 1;
        char currentChar = input.charAt(0);

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == currentChar) {
                count++;
            } else {
                result.append(currentChar).append(count);
                count = 1;
                currentChar = c;
            }
        }

        result.append(currentChar).append(count);
        return result.toString();
    }

    @Test
    void matrixQ() {
        // need to find max amount of gold in a given matrix, only allowed take steps up and right. Source point will be bottom-left
        int[][] grid = {
                {0, 1, 4, 2, 3},
                {1, 3, 3, 2, 2},
                {4, 7, 6, 1, 5},
                {2, 5, 3, 9, 2}
        };
        int maxGoldAmount = maxGold(grid);
        System.out.println("Maximum amount of gold: " + maxGoldAmount); // Output: 28

    }

    public static int maxGold(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int rows = grid.length;
        int cols = grid[0].length;

        // Create a DP table to store the maximum gold collected at each cell
        int[][] dp = new int[rows][cols];

        // Initialize the DP table with the values from the last row of the grid
        for (int i = 0; i < cols; i++) {
            dp[rows - 1][i] = grid[rows - 1][i];
        }

        // Fill the DP table by iterating from bottom to top and from right to left
        for (int i = rows - 2; i >= 0; i--) {
            for (int j = cols - 1; j >= 0; j--) {
                int right = (j + 1 < cols) ? dp[i][j + 1] : 0;
                int up = dp[i + 1][j];
                dp[i][j] = grid[i][j] + Math.max(right, up);
            }
        }

        // The maximum gold is stored in the top-left corner of the DP table
        return dp[0][0];
    }
}
