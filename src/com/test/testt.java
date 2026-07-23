package com.test;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import java.io.IOException;

public class testt {
    static double GWO_res[][] = new double[23][30];
    static double DA_res[][] = new double[23][30];
    static double DA_GWO_res[][] = new double[23][30];
    static double pvalue_DAGWO_DA[] = new double[23];
    static double pvalue_DAGWO_GWO[] = new double[23];
    static double pvalue_GWO_DA[] = new double[23];
    static int times = 30; //so lan chay
    public static void main(String[] args) throws Exception {

        GWO(times);
        DA(times);
        DA_GWO(times);
        calPvalue();
    }

    public static void GWO(int times) throws Exception {
        f_test f = new f_test();
        double avg[] = new double[23];
        double std[] = new double[23];
        for (int i=0; i<23; i++){
            String fname = "f"+(i+1);
            f.getFunctionDetail(fname);
            double f_optimize[] = new double[times];
            double sum = 0;
            for (int j=0; j<times; j++){
                GWO result = new GWO(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test.maxiter, f_test.N);
                f_optimize[j] = result.getRes();
                GWO_res[i][j] =  f_optimize[j];
                sum+=f_optimize[j];
            }

            avg[i] = sum/times;

            for (int j=0; j<times; j++){
                std[i]+= (f_optimize[j]-avg[i])*(f_optimize[j]-avg[i]);
            }
            std[i]/=(times-1);
            std[i] = Math.sqrt(std[i]);
            System.out.println("Fname: "+fname);
            System.out.println("Avg = "+avg[i]);
            System.out.println("Std = "+std[i]);
        }
        ExcelUtils.fillAvgAndStdToExcel(f_test.N, f_test.maxiter, times, avg,std,0);
        ExcelUtils.fillBestScoreToExcel(GWO_res, 23, times, 0);
    }

    public static void DA(int times) throws Exception {
        f_test f = new f_test();
        double avg[] = new double[23];
        double std[] = new double[23];
        for (int i=0; i<23; i++){
            String fname = "f"+(i+1);
            f.getFunctionDetail(fname);
            double f_optimize[] = new double[times];
            double sum = 0;
            for (int j=0; j<times; j++){
                DA result = new DA(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test.maxiter, f_test.N);
                result.solution();
                f_optimize[j] = result.getRes();
                DA_res[i][j] =  f_optimize[j];
                sum+=f_optimize[j];
            }

            avg[i] = sum/times;

            for (int j=0; j<times; j++){
                std[i]+= (f_optimize[j]-avg[i])*(f_optimize[j]-avg[i]);
            }
            std[i]/=(times-1);
            std[i] = Math.sqrt(std[i]);
            System.out.println("Fname: "+fname);
            System.out.println("Avg = "+avg[i]);
            System.out.println("Std = "+std[i]);
        }
        ExcelUtils.fillAvgAndStdToExcel(f_test.N, f_test.maxiter, times, avg,std, 2);
        ExcelUtils.fillBestScoreToExcel(DA_res, 23, times, 29);
    }

    public static void DA_GWO(int times) throws Exception {
        f_test f = new f_test();
        double avg[] = new double[23];
        double std[] = new double[23];
        for (int i=0; i<23; i++){
            String fname = "f"+(i+1);
            f.getFunctionDetail(fname);
            double f_optimize[] = new double[times];
            double sum = 0;
            for (int j=0; j<times; j++){
                DA_GWO result = new DA_GWO(f.getF(), f.getLowerBound(), f.getUpperBound(), f_test.maxiter, f_test.N);
                result.solution();
                f_optimize[j] = result.getRes();
                DA_GWO_res[i][j] =  f_optimize[j];
                sum+=f_optimize[j];
            }

            avg[i] = sum/times;

            for (int j=0; j<times; j++){
                std[i]+= (f_optimize[j]-avg[i])*(f_optimize[j]-avg[i]);
            }
            std[i]/=(times-1);
            std[i] = Math.sqrt(std[i]);
            System.out.println("Fname: "+fname);
            System.out.println("Avg = "+avg[i]);
            System.out.println("Std = "+std[i]);
        }
        ExcelUtils.fillAvgAndStdToExcel(f_test.N, f_test.maxiter, times, avg,std, 4);
        ExcelUtils.fillBestScoreToExcel(DA_GWO_res, 23, times, 58);
    }

    static void calPvalue() throws IOException {
        MannWhitneyUTest mannWhitneyUTest = new MannWhitneyUTest();
        double x[] = new double[times];
        double y[] = new double[times];
        for (int i=0; i<23; i++){
            //p value for DA_GWO and GWO
            for (int j=0; j<times; j++){
                x[j] = DA_GWO_res[i][j];
                y[j] = GWO_res[i][j];
            }
            pvalue_DAGWO_GWO[i] = mannWhitneyUTest.mannWhitneyUTest(x,y);
            //p value for DA_GWO and DA
            for (int j=0; j<times; j++){
                x[j] = DA_GWO_res[i][j];
                y[j] = DA_res[i][j];
            }
            pvalue_DAGWO_DA[i] = mannWhitneyUTest.mannWhitneyUTest(x,y);
            //p value for GWO and DA
            for (int j=0; j<times; j++){
                x[j] = GWO_res[i][j];
                y[j] = DA_res[i][j];
            }
            pvalue_GWO_DA[i] = mannWhitneyUTest.mannWhitneyUTest(x,y);
        }
        ExcelUtils.fillPValueToExcel(pvalue_DAGWO_DA, pvalue_DAGWO_GWO, pvalue_GWO_DA, 23);
    }
}
