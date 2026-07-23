package com.test.draw;

import com.test.ExcelUtils;
import com.test.f_xj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class GWO_Draw
{
    double r1;
    double r2;
    int N;
    int D;
    int maxiter;
    double alfa[];
    double beta[];
    double delta[];
    double Lower[];
    double Upper[];
    f_xj ff;
    double XX[][];
    double X1;
    double X2;
    double X3;
    double fitness[];
    double iterdep[];
    double a;
    double A1;
    double C1;
    double A2;
    double C2;
    double A3;
    double C3;
    double[][] arrRandomBestVal;
    double[][] Result;

    //for draw
    double F_min[]; //F min tai moi lan lap
    double F_avg[]; //F trung binh cua cac search agents sau moi lan lap
    double X_1[];   //gia tri x1 cua search agent dau tien sau moi lan lap
    double X_2[];   //gia tri x2 cua search agent dau tien sau moi lan lap
    static int Search_agent_1_pos = 0;
    static int orderOfF;

    public GWO_Draw(f_xj iff, double iLower[], double iUpper[], int imaxiter, int iN, int orderOfF)
    {
        maxiter = imaxiter;
        ff = iff;
        Lower = iLower;
        Upper = iUpper;
        N = iN;
        D = Upper.length;
        XX = new double[N][D+1];
        alfa = new double[D];
        beta = new double[D];
        delta = new double[D];
        iterdep = new double[maxiter];

        arrRandomBestVal = new double[maxiter][D];

        F_min = new double[maxiter];
        F_avg = new double[maxiter];
        X_1 = new double[maxiter];
        X_2 = new double[maxiter];
        GWO_Draw.orderOfF = orderOfF;
    }

    double[][] sort_and_index(double[][] XXX) throws IOException {

        double[] yval = new double[N];

        for(int i = 0; i < N; i++) {
            yval[i] = ff.func(XXX[i]);
        }

        ArrayList<Double> nfit = new ArrayList<Double>();

        for(int i = 0; i < N; i++) {
            nfit.add(yval[i]);
        }

        ArrayList<Double> nstore = new ArrayList<Double>(nfit);
        Collections.sort(nfit);

        int[] indexes = new int[nfit.size()];

        for(int i = 0; i < nfit.size(); i++) {
            indexes[i] = nstore.indexOf(nfit.get(i));
        }

        double[][] B = new double[N][D+1];

        for(int i = 0; i < N; i++) {
            for(int j = 0; j < D+1; j++) {
                B[i][j] = XXX[indexes[i]][j];
            }
            if (B[i][D]==1){
                System.out.println("Search_agent_1_pos: "+i);
            }
        }

        return B;
    }

    void init() throws IOException {
        for(int i = 0; i < N; i++) {
            for(int j = 0; j < D; j++) {
                XX[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
            }
        }

        XX[0][D] = 1;

        XX=sort_and_index(XX);

        for(int i = 0; i < D; i++) {
            alfa[i] = XX[0][i];
        }

        for(int i = 0; i < D; i++) {
            beta[i] = XX[1][i];
        }

        for(int i = 0; i < D; i++) {
            delta[i] = XX[2][i];
        }
    }

    double[][] simplebounds(double s[][]) {
        for(int i = 0; i < N; i++) {
            for(int j = 0; j < D; j++) {
                if(s[i][j] < Lower[j]) {
                    s[i][j] = Lower[j] + ((Upper[j] - Lower[j]) * Math.random());
                }

                if(s[i][j] > Upper[j]) {
                    s[i][j] = Lower[j] + ((Upper[j] - Lower[j]) * Math.random());
                }
            }
        }
        return s;
    }

    double[][] solution() throws IOException {
        init();
        int iter;
        for (iter=1; iter<=maxiter; iter++) {
            a = 2.0 -((double)iter * (2.0 / (double)maxiter));

            for(int i = 0; i < N; i++) {
                for(int j = 0; j < D; j++) {
                    r1 = Math.random();
                    r2 = Math.random();
                    A1 = 2.0 * a * r1 - a;
                    C1 = 2.0 * r2;
                    X1 = alfa[j] - A1 * (Math.abs(C1 * alfa[j] - XX[i][j]));
                    if (X1<Lower[j] || X1>Upper[j]) X1 = Lower[j] + ((Upper[j] - Lower[j]) * Math.random());

                    r1 = Math.random();
                    r2 = Math.random();
                    A2 = 2.0 * a * r1 - a;
                    C2 = 2.0*r2;
                    X2 = beta[j] - A2 * (Math.abs(C2 * beta[j] - XX[i][j]));
                    if (X2<Lower[j] || X2>Upper[j]) X2 = Lower[j] + ((Upper[j] - Lower[j]) * Math.random());

                    r1 = Math.random();
                    r2 = Math.random();
                    A3 = 2.0 * a * r1 - a;
                    C3 = 2.0 * r2;
                    X3 = delta[j] - A3 * (Math.abs(C3 * delta[j] - XX[i][j]));
                    if (X3<Lower[j] || X3>Upper[j]) X3 = Lower[j] + ((Upper[j] - Lower[j]) * Math.random());
                    XX[i][j] = (X1 + X2 + X3) / 3.0;
                }
            }
            XX = simplebounds(XX);
            XX = sort_and_index(XX);

            for(int i = 0; i < D; i++) {
                XX[N-1][i] = XX[0][i];
            }


            for(int i = 0; i < D; i++) {
                alfa[i] = XX[0][i];
            }

            for(int i = 0; i < D; i++) {
                beta[i] = XX[1][i];
            }

            for(int i = 0; i < D; i++) {
                delta[i] = XX[2][i];
            }

            F_min[iter-1] = ff.func(XX[0]);
            double sumF = 0.0;
            for (int i=0; i<N; i++){
                sumF+= ff.func(XX[i]);
            }
            F_avg[iter-1] = sumF/N;
            X_1[iter-1] = XX[Search_agent_1_pos][0];
            X_2[iter-1] = XX[Search_agent_1_pos][1];

            arrRandomBestVal[iter-1] = XX[0];
//            System.out.println("Iter: "+iter);
//            System.out.println(F_min[iter]);
            //iter++;


        }

        double[][] out = new double[2][D];

        for(int i = 0; i < D; i++){
            out[1][i] = alfa[i];
        }

        out[0][0] = ff.func(alfa);
        return out;
    }

    public void execute() throws IOException {
        Result = solution();
    }

    void toStringnew() throws IOException {
        double[][] in=solution();
        System.out.println("Optimized value = "+in[0][0]);
        for(int i=0;i<D;i++) {
            System.out.print("x["+i+"] = "+in[1][i]+"\t");
        }
        System.out.println();
    }

    public double getRes() throws IOException {
        double[][] in=solution();
        ExcelUtils.fillForDrawFunctionToExcel(F_min, F_avg, X_1, X_2, 5, 95, 186, 277, orderOfF, maxiter);
        return in[0][0];
    }

    public void toStringNew(String sMessage) throws IOException {
        System.out.println(sMessage + Result[0][0]);

        for(int i = 0; i < D;i++) {
            System.out.println("x["+i+"] = "+ Result[1][i]);
        }

        System.out.println("----------------------------------------");
    }

    public double[] getBestArray()
    {
        return Result[1];
    }

    public double[][] getArrayRandomResult(){
        return arrRandomBestVal;
    }
}
