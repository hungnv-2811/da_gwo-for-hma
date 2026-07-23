package com.test;

public class DA_test {
    public static void main(String args[]) throws Exception {
        f_test f = new f_test();
        f.getFunctionDetail(f_test.F_name);
        DA result = new DA(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test.maxiter, f_test.N);
        long startTime = System.currentTimeMillis();
        result.solution();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println((totalTime / 1000.0) + " sec");
    }
}