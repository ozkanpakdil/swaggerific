package com.mascix.swaggerific.algorithms;

import java.util.*;

public class Another {
    private static int MaxSetSize(List<Integer> riceBags)
    {
        int result = -1, square = 0, counter = 0;

        for(int i = 0; i < riceBags.size(); i++)
        {
            if(!riceBags.contains(riceBags.get(i) / 2))
            {
                counter = 0;
                square = riceBags.get(i);
                while(riceBags.contains(square))
                {
                    square *= square;
                    counter++;
                }
            }

            if(counter > 1)
                result = Math.max(result, counter);
        }

        return result;
    }
    public static int maxSetSize2(List<Integer> riceBags) {
        int []bags=riceBags.stream().mapToInt(i->i).toArray();
        int res= perfectRiceBags(bags);
        return res < 2 ? -1 : res;
    }
    private static Set<Integer> elements = new HashSet<>();
    private static Map<Integer, Integer> cache = new HashMap<>();

    public static int perfectRiceBags(int[] arr) {
        for (int a : arr) {
            elements.add(a);
        }

        int max = Integer.MIN_VALUE;

        for (int i = 0; i < arr.length; i++) {
            int riceBags = dfs(arr[i]);

            if (max < riceBags) {
                max = riceBags;
            }
        }

        return max;
    }

    private static int dfs(int a) {
        if (cache.containsKey(a)) {
            return cache.get(a);
        }

        if (elements.contains(a)) {
            int result = 1 + dfs(a * a);
            cache.put(a, result);
            return result;
        }

        return 0;
    }

    public static void main(String[] args) {
        List<Integer> riceBags1 = List.of(3, 4, 6, 8, 9, 10, 12, 14, 16, 18, 20, 22, 24, 26, 81, 6561);
        System.out.println(MaxSetSize(riceBags1)); // Output: 3

        List<Integer> riceBags2 = List.of(4, 7, 8, 9);
        System.out.println(MaxSetSize(riceBags2)); // Output: -1

        List<Integer> riceBags3 = List.of(7, 4, 8, 9);
        System.out.println(MaxSetSize(riceBags3)); // Output: -1
    }
}
