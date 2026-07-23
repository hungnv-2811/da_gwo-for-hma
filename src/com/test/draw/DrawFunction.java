package com.test.draw;

public class DrawFunction {
    static double GWO_res[] = new double[23];
    static double DA_res[] = new double[23];
    static double DA_GWO_res[] = new double[23];
    static int N = 30;
    static int maxIter = 30;

    public static void main(String[] args) throws Exception {
        f_test_draw f = new f_test_draw();
        for (int i=0; i<23; i++){
            String fname = "f"+(i+1);
            f.getFunctionDetail(fname);
            GWO_Draw result_GWO = new GWO_Draw(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test_draw.maxiter, f_test_draw.N,i);
            GWO_res[i] = result_GWO.getRes();

            DA_Draw result_DA = new DA_Draw(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test_draw.maxiter, f_test_draw.N,i);
            result_DA.solution();
            DA_res[i] = result_DA.getRes();

            DA_GWO_Draw result_DA_GWO = new DA_GWO_Draw(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test_draw.maxiter, f_test_draw.N,i);
            result_DA_GWO.solution();
            DA_res[i] = result_DA_GWO.getRes();
        }
    }
}
