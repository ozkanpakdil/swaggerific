package io.github.ozkanpakdil.swaggerific.algorithms;

import java.util.*;

class Process {
    int index;
    int priority;

    public Process(int index, int priority) {
        this.index = index;
        this.priority = priority;
    }
}

public class Another3 {

    public static List<Integer> getPrioritiesAfterExecution(List<Integer> priority) {
        List<Integer> result = new ArrayList<>(priority);

        while (true) {
            int maxPriority = Collections.max(result);
            if (Collections.frequency(result, maxPriority) < 2 || maxPriority == 0) {
                break; // No more processes to execute
            }

            int process1Index = result.indexOf(maxPriority);
            result.set(process1Index, 0);

            int process2Index = result.lastIndexOf(maxPriority);

            result.set(process1Index, 0);

            int newPriority = maxPriority / 2;
            result.set(process2Index, newPriority);
        }

        result.removeIf(p -> p == 0);
        return result;
    }

    public static void main(String[] args) {
        List<Integer> priorities = Arrays.asList(6,

                2,

                1,

                5,

                10,

                10,

                1);
        List<Integer> finalPriorities = getPrioritiesAfterExecution(priorities);
        System.out.println(finalPriorities); // Output: [3, 6, 0]
    }
}
