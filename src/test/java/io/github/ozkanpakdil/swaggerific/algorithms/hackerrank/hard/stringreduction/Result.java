package io.github.ozkanpakdil.swaggerific.algorithms.hackerrank.hard.stringreduction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.IntStream;

class Result {

    /*
     * Complete the 'stringReduction' function below.
     *
     * The function is expected to return an INTEGER.
     * The function accepts STRING s as parameter.
     */

    public static int stringReduction(String s) {
        char[] l = s.toCharArray();
        String r = s;
        while (!r.matches("(.)\\1*")) {
            r = r.replaceFirst("ab", "c");
            r = r.replaceFirst("ba", "c");
            r = r.replaceFirst("ca", "b");
            r = r.replaceFirst("ac", "b");
            r = r.replaceFirst("bc", "a");
            r = r.replaceFirst("cb", "a");

        }
        return r.length();
    }

}

class Solution {
    public static void main(String[] args) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        int t = Integer.parseInt(bufferedReader.readLine().trim());

        IntStream.range(0, t).forEach(tItr -> {
            try {
                String s = bufferedReader.readLine();

                int result = Result.stringReduction(s);
                System.out.println(result);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        bufferedReader.close();
    }
}
