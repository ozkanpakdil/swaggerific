package com.mascix.swaggerific.algorithms;

public class Test1 {
    public static int beautifulBinaryString(String b) {
        int r=0;
        while(b.contains("010")){
            b=b.replaceFirst("010", "111");
            r++;
        }
        return r;
    }

    public static void main(String[] args) {
        System.out.println(beautifulBinaryString("10110101101010001111011100100001010001111010110000111100110110111110011011000111100010011100111"));
    }
}
