package com.mascix.swaggerific.algorithms;

import java.util.ArrayList;
import java.util.List;

public class Another2 {
    public static List<Integer> getPrioritiesAfterExecution(List<Integer> p) {
        ArrayList<Integer> priority=new ArrayList(p);
        int n = priority.size();
        List<Integer> result = new ArrayList<>();

        while (true) {
            int maxPriority = getMaxPriority(priority);
            int countMaxPriority = countPriority(priority, maxPriority);

            if (countMaxPriority < 2 || maxPriority == 0) {
                break;
            }

            int process1 = -1;
            int process2 = -1;

            for (int i = 0; i < n; i++) {
                if (priority.get(i) == maxPriority) {
                    if (process1 == -1) {
                        process1 = i;
                    } else if (process2 == -1) {
                        process2 = i;
                        break;
                    }
                }
            }

            result.add(maxPriority);
            priority.set(process1, 0);

            priority.set(process2, maxPriority / 2);
        }

        for (int i = 0; i < n; i++) {
            if (priority.get(i) > 0) {
                result.add(priority.get(i));
            }
        }
        System.out.println(result);

        return result;
    }

    private static int getMaxPriority(List<Integer> priority) {
        int max = Integer.MIN_VALUE;
        for (int p : priority) {
            max = Math.max(max, p);
        }
        return max;
    }

    private static int countPriority(List<Integer> priority, int p) {
        int count = 0;
        for (int value : priority) {
            if (value == p) {
                count++;
            }
        }
        return count;
    }

    public static void main(String[] args) {
        List<Integer> riceBags1 = List.of(4, 4, 4, 2, 1);
        System.out.println(getPrioritiesAfterExecution(riceBags1)); // Output: 3

    }
}
