package com.test;

public class GWO_test {
    public static void main(String args[]) throws Exception {
        f_test f = new f_test();
        f.getFunctionDetail("f1");
        GWO result = new GWO(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test.maxiter, f_test.N);
        long startTime = System.currentTimeMillis();
        result.toStringnew();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println((totalTime / 1000.0) + " sec");
    }

}