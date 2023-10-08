package com.mascix.swaggerific.algorithms;

import java.util.Arrays;
import java.util.HashMap;

public class LongestSubstring {
    public static void main(String[] args) {
        String in = "ABCDEFGABEF";
        char c;
        int lon = 0, longest = 0;
        int start = 0, last = 0;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            lon = 0;
            start = i;
            while (i + 1 < in.length() && c != in.charAt(++i)) {
                lon++;
                if (longest < lon) {
                    longest = lon;
                    last = i;
                }
            }
            System.out.println(lon);
        }
        System.out.println(start + " end:" + last);
        System.out.println(findLongestSubstring(in));


        int[] myArray = {
                240, 17, 912, 1274, 2049, 1102, 990, 1883, 536, 274, 2822, 1063, 2763, 1420, 1008, 306, 1167, 417, 2259, 1898,
                1799, 1919, 643, 507, 2841, 1380, 122, 2037, 1379, 2300, 2842, 597, 1681, 1572, 37, 546, 1918, 1871, 1016, 2097,
                2086, 1275, 1733, 412, 434, 292, 358, 2512, 474, 896, 1803, 543, 2013, 2264, 2596, 2394, 678, 1027, 542, 182,
                1117, 1315, 765, 2191, 1185, 2047, 884, 985, 2477, 2706, 2155, 905, 2526, 2892, 1473, 1649, 722, 841, 2392,
                2245, 101, 52, 1826, 2312, 1176, 1682, 500, 1015, 824, 1652, 1707, 1371, 1716, 1204, 1903, 1401, 930, 1368,
                1902, 1212, 2095, 2743, 891, 661, 2729, 1507, 1847, 1268, 532, 506, 501, 2756, 2075, 1005, 1280, 2004, 2617,
                1026, 2127, 920, 1995, 2064, 278, 483, 1283, 1370, 2079, 1677, 544, 724, 2637, 1891, 1152, 73, 68, 911, 992,
                1630, 2130, 1475, 2453, 2429, 1479, 1861, 1079, 2280, 263, 2590, 372, 1211, 2328, 254, 1765, 2627, 367, 1541,
                329, 1197, 825, 1256, 22, 782, 1933, 792, 491, 1613, 405, 2832, 1141, 269, 2271, 1780, 940, 756, 647, 419,
                2682, 589, 1581, 2891, 1174, 2957, 2750, 565, 2950, 2716, 1402, 277, 1075, 872, 2740, 193, 1458, 1459, 636,
                1937, 2639, 1436, 909, 197, 2261, 238, 611, 1669, 1725, 1448, 2746, 482, 2154, 1732, 2255, 1590, 1292, 800,
                1703, 2033, 1591, 1855, 2249, 1217, 1046, 1650, 1997, 493, 1489, 2108, 610, 2110, 209, 2864, 1635, 1583, 2397,
                653, 1290, 778, 440, 860, 476, 815
        };
        System.out.println(numTeams(myArray));

        System.out.println(longestSubstrDistinctChars("geeksforgeeks"));
    }

    static int longestSubstrDistinctChars(String S){
        // code here
        int n = S.length();

        int res = 0; // result

        // last index of all characters is initialized as -1
        int[] lastIndex = new int[256];
        Arrays.fill(lastIndex, -1);

        // Initialize start of current window
        int i = 0;

        // Move end of current window
        for (int j = 0; j < n; j++) {

            // Find the last index of str[j] Update i (starting index of current window)
            // as maximum of current value of i and last index plus 1
            i = Math.max(i, lastIndex[S.charAt(j)] + 1);

            // Update result if we get a larger window
            res = Math.max(res, j - i + 1);

            // Update last index of j.
            lastIndex[S.charAt(j)] = j;
        }
        return res;
    }

    public static int numTeams(int[] rating) {
        HashMap<String, int[]> l = new HashMap();
        for (int i = 0; i < rating.length; i++)
            for (int j = i; j < rating.length; j++)
                for (int k = j; k < rating.length; k++) {
                    int[] r = new int[]{rating[i], rating[j], rating[k]};
                    if (l.get(Arrays.toString(r)) == null && 0 <= i && i < j && j < k)
                        if ((rating[i] < rating[j] && rating[j] < rating[k]) ||
                                (rating[i] > rating[j] && rating[j] > rating[k])
                        ) {
                            l.put(Arrays.toString(r), r);
                        }
                }
        return l.size();
    }

    public static String findLongestSubstring(String str) {
        int i;
        int n = str.length();

        // Starting point of current substring.
        int st = 0;

        // length of  current substring.
        int currlen = 0;

        // maximum length substring without repeating characters.
        int maxlen = 0;

        // starting index of maximum length substring.
        int start = 0;

        // Hash Map to store last occurrence of each already visited character.
        HashMap<Character, Integer> pos = new HashMap<>();

        // Last occurrence of first  character is index 0;
        pos.put(str.charAt(0), 0);

        for (i = 1; i < n; i++) {
            // If this character is not present in hash,  then this is first occurrence of this  character, store this in hash.
            if (!pos.containsKey(str.charAt(i))) {
                pos.put(str.charAt(i), i);
            } else {
                // If this character is present in hash then this character has previous occurrence,
                // check if that occurrence is before or after starting point of current substring.
                if (pos.get(str.charAt(i)) >= st) {
                    // find length of current substring and update maxlen and start accordingly.
                    currlen = i - st;
                    if (maxlen < currlen) {
                        maxlen = currlen;
                        start = st;
                    }

                    // Next substring will start after the last occurrence
                    // of current character to avoid its repetition.
                    st = pos.get(str.charAt(i)) + 1;
                }

                // Update last occurrence of current character.
                pos.replace(str.charAt(i), i);
            }
        }

        // Compare length of last substring with maxlen and
        // update maxlen and start accordingly.
        if (maxlen < i - st) {
            maxlen = i - st;
            start = st;
        }

        // The required longest substring without
        // repeating characters is from str[start] to str[start+maxlen-1].
        return str.substring(start,
                start +
                        maxlen);
    }
}
