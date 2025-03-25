package io.github.ozkanpakdil.swaggerific.algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

public class MaxBalancedString {
    /*
    Given a string s consisting of parentheses, you need to find the maximum score possible in a balanced substring of s. The score of a substring is calculated by choosing two indices i and j (0 <= i < j < len(s)) such that s[i] is an opening parenthesis "(" and s[j] is a closing parenthesis ")". The score of the substring is defined as j - i, i.e., the difference between the indices.

    Write a function/method that takes a string s as input and returns the maximum score that can be obtained from a balanced substring of s.

    Example:

    Input: (())

    Output: 4

    Explanation: There are two possible balanced substrings: "( ( ) )" 3-0 + 2-1 = 4 or 2-0 + 3-1 = 4
    * */
    public static void main(String[] args) {
        String s = "(())";

        System.out.println(isBalanced(s));

        System.out.println(solution(s.toCharArray()));
    }

    private static long solution(char[] s) {
        long r = 0;
        for (int left = 0, right = s.length - 1; left < right; ++left, --right) {
            for (; left < right && s[left] == ')'; ++left)
                ;
            for (; left < right && s[right] == '('; --right)
                ;
            if (left < right) {
                r += right - left;
            }
        }
        return r;
    }

    public static String isBalanced(String s) {
        Stack stack = new Stack();
        for (char c : s.toCharArray()) {
            if (isOpenParenthesis(c)) {
                stack.push(c);
            } else {
                if (stack.empty() || !isMatch((Character) stack.pop(), c)) {
                    return "NO";
                }
            }
        }
        return (stack.isEmpty()) ? "YES" : "NO";
    }

    static boolean isMatch(char pop, char c) {
        switch (c) {
            case '}':
                return pop == '{';
            case ')':
                return pop == '(';
            case ']':
                return pop == '[';
        }
        return false;
    }

    static boolean isOpenParenthesis(char c) {
        if (c == '(' || c == '{' || c == '[')
            return true;
        return false;
    }


    class Solution {
        int mod = (int)1e9 + 7;
        public int numFactoredBinaryTrees(int[] arr) {

            Arrays.sort(arr);
            int ans = 0;
            HashMap<Integer, Long> map = new HashMap<>(arr.length * 4) ;

            for (int x : arr) {

                long ways = 1;
                int max = (int)Math.sqrt(x);
                for (int j = 0, left = arr[0]; left <= max; left = arr[++j]) {
                    if (x % left != 0) continue;
                    int right = x / left;
                    if (map.containsKey(right))
                        ways = (ways + map.get(left) * map.get(right) * (left == right ? 1 : 2)) % mod;
                }

                map.put(x, ways);
                ans = (int)((ans + ways) % mod);
            }
            return ans;
        }
    }

}
