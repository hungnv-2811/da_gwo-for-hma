package com.test.draw;

import com.test.f_xj;

public class f_test_draw {
    public static String F_name = "f23";
    public static int maxiter = 100;
    public static int N = 6;
    private f_xj f = null;
    private double lb;
    private double ub;
    private int dim;
    public void getFunctionDetail(String f_name) throws Exception {
        switch (f_name){
            case "f1":
                f = new f1();
                lb=-100;
                ub=100;
                dim=10;
                break;
            case "f2":
                f = new f2();
                lb=-10;
                ub=10;
                dim=10;
                break;
            case "f3":
                f = new f3();
                lb=-100;
                ub=100;
                dim=10;
                break;
            case "f4":
                f = new f4();
                lb=-100;
                ub=100;
                dim=10;
                break;
            case "f5":
                f = new f5();
                lb=-30;
                ub=30;
                dim=10;
                break;
            case "f6":
                f = new f6();
                lb=-100;
                ub=100;
                dim=10;
                break;
            case "f7":
                f = new f7();
                lb=-1.28;
                ub=1.28;
                dim=10;
                break;
            case "f8":
                f = new f8();
                lb=-500;
                ub=500;
                dim=10;
                break;
            case "f9":
                f = new f9();
                lb=-5.12;
                ub=5.12;
                dim=10;
                break;
            case "f10":
                f = new f10();
                lb=-32;
                ub=32;
                dim=10;
                break;
            case "f11":
                f = new f11();
                lb=-600;
                ub=600;
                dim=10;
                break;
            case "f12":
                f = new f12();
                lb=-50;
                ub=50;
                dim=10;
                break;
            case "f13":
                f = new f13();
                lb=-50;
                ub=50;
                dim=10;
                break;
            case "f14":
                f = new f14();
                lb=-65.536;
                ub=65.536;
                dim=2;
                break;
            case "f15":
                f = new f15();
                lb=-5;
                ub=5;
                dim=4;
                break;
            case "f16":
                f = new f16();
                lb=-5;
                ub=5;
                dim=2;
                break;
            case "f17":
                f = new f17();
                lb=-5;
                ub=5;
                dim=2;
                break;
            case "f18":
                f = new f18();
                lb=-2;
                ub=2;
                dim=2;
                break;
            case "f19":
                f = new f19();
                lb=0;
                ub=1;
                dim=3;
                break;
            case "f20":
                f = new f20();
                lb=0;
                ub=1;
                dim=6;
                break;
            case "f21":
                f = new f21();
                lb=0;
                ub=10;
                dim=4;
                break;
            case "f22":
                f = new f22();
                lb=0;
                ub=10;
                dim=4;
                break;
            case "f23":
                f = new f23();
                lb=0;
                ub=10;
                dim=4;
                break;
            default:
                throw new Exception("Not support function has name: "+f_name);
        }
    }

    public double [] getLowerBound(){
        double lb [] = new double[dim];
        for (int i=0; i<dim; i++){
            lb[i] = this.lb;
        }
        return lb;
    }

    public double [] getUpperBound(){
        double ub [] = new double[dim];
        for (int i=0; i<dim; i++){
            ub[i] = this.ub;
        }
        return ub;
    }

    public f_xj getF(){
        return f;
    }

    class f1 extends f_xj {
        public double func(double x[]) {
            int n = x.length-1;
            double f = 0;
            for (int i=0; i<n; i++){
                f = f+x[i]*x[i];
            }
            return f;
        }
    }

    class f2 extends f_xj {
        public double func(double x[]) {
            int n = x.length-1;
            double f = 0;
            double sum = 0;
            double prod = 1.0;
            for (int i=0; i<n; i++){
                sum = sum+ Math.abs(x[i]);
                prod = prod*Math.abs(x[i]);
            }
            f = f+sum+prod;
            return f;
        }
    }

    class f3 extends f_xj {
        public double func(double x[]) {
            int n = x.length-1;
            double f = 0;
            for (int i=0; i<n; i++){
                double sum = 0;
                for (int j=0; j<=i; j++){
                    sum = sum+ x[j];
                }
                f = f+sum*sum;
            }
            return f;
        }
    }

    class f4 extends f_xj {
        public double func(double x[]) {
            double max = Math.abs(x[0]);
            for (int i=1; i<x.length-1; i++){
                if (Math.abs(x[i])>max){
                    max = Math.abs(x[i]);
                }
            }
            return max;
        }
    }

    class f5 extends f_xj {
        public double func(double x[]) {
            int n=x.length-1;
            double ff=0.0;
            for(int i=0;i<n-1;i++){
                ff+=(100.0*(x[i+1]-x[i]*x[i])*(x[i+1]-x[i]*x[i])+(1.0-x[i])*(1.0-x[i]));
            }
            return ff;
        }
    }

    class f6 extends f_xj {
        public double func(double x[]) {
            int n=x.length-1;
            double ff=0.0;
            for(int i=0;i<n;i++){
                ff+=Math.abs(x[i]+0.5)*Math.abs(x[i]+0.5);
            }
            return ff;
        }
    }

    class f7 extends f_xj {
        public double func(double x[]) {
            int n=x.length-1;
            double ff=0.0;
            for(int i=0;i<n;i++){
                ff+= (i+1)*x[i]*x[i]*x[i]*x[i];
            }
            ff+=+Math.random();
            return ff;
        }
    }

    class f8 extends f_xj {
        public double func(double x[])
        {
            int n=x.length-1;
            double f=0.0;
            for(int i=0;i<n;i++)
            {
                f+=(x[i]*Math.sin(Math.sqrt(Math.abs(x[i]))));
            }
            return -f;
        }
    }

    class f9 extends f_xj {
        public double func(double x[]) {
            double ff=0;
            int n=x.length-1;
            for(int i=0;i<n;i++) {
                ff+=10.0+x[i]*x[i]-10*Math.cos(2.0*Math.PI*x[i]);
            }
            return ff;
        }
    }

    class f10 extends f_xj {
        public double func(double x[]) {
            double ff=0;
            int n=x.length-1;
            double sum1 = 0;
            double sum2 = 0;
            for(int i=0;i<n;i++) {
                sum1+= x[i]*x[i];
                sum2+= Math.cos(2*Math.PI*x[i]);
            }
            ff+=-20*Math.exp(-0.2*Math.sqrt((1.0/n)*sum1))-Math.exp((1.0/n)*sum2)+20+Math.E;
            return ff;
        }
    }

    class f11 extends f_xj {
        public double func(double x[]) {
            double s=0.0;
            double fact=1.0;
            int m=x.length-1;
            for(int i=0;i<m;i++) {
                s+=x[i]*x[i];
            }
            for(int i=0;i<m;i++) {
                fact*=Math.cos(x[i]/Math.sqrt(i+1));
            }
            return (s/4000.0)+1.0+(-fact);
        }
    }

    static class UFun {
        public static double func(double x, double a, double k, double m) {
            if (x>a){
                return k*Math.pow(x-a, m);
            } else if (x<-a){
                return k*Math.pow(-x-a, m);
            }
            else {
                return 0;
            }
        }
    }

    class f12 extends f_xj {
        public double func(double x[]) {
            int n = x.length-1;
            double sum1 = 0;
            double sum2 = 0;
            for (int i=0; i<n-1; i++){
                sum1+= Math.pow((x[i]+1)/4, 2)* (1.0+ 10.0*Math.pow(Math.sin(Math.PI*(1.0+(x[i+1]+1)/4.0)),2));
            }
            for (int i=0; i<n; i++){
                sum2+= UFun.func(x[i],10.0,100.0,4.0);
            }
            double f = (Math.PI/n)* (10.0*Math.pow(Math.sin(Math.PI*(1.0+(x[0]+1)/4)), 2)+sum1+Math.pow((x[n-1]+1)/4,2)) + sum2;
            return f;
        }
    }

    class f13 extends f_xj {
        public double func(double x[]) {
            int n = x.length-1;
            double sum1 = 0;
            double sum2 = 0;
            for (int i=0; i<n; i++){
                sum1+= (x[i]-1)*(x[i]-1)*(1+ Math.pow(Math.sin(3*Math.PI*x[i]+1), 2));
                sum2+= UFun.func(x[i],5,100,4);
            }
            double f = 0.1*(Math.pow(Math.sin(3*Math.PI*x[0]), 2) + sum1 + (x[n-1]-1)*(x[n-1]-1)*(1+ Math.pow(Math.sin(2*Math.PI*x[n-1]),2))) + sum2;
            return f;
        }
    }

    class f14 extends f_xj {
        public double func(double x[]) {
            double aS[][]={{-32, -16, 0, 16, 32, -32, -16, 0, 16, 32, -32, -16, 0, 16, 32, -32, -16, 0, 16, 32, -32,-16, 0, 16, 32},
                        {-32, -32, -32, -32, -32, -16, -16, -16, -16, -16, 0, 0, 0, 0, 0, 16, 16, 16, 16, 16, 32, 32, 32, 32, 32}};
            double temp = 0;
            for (int j=0; j<25; j++){
                double sum = 0;
                for (int i=0; i<2; i++){
                    sum+= Math.pow((x[i]-aS[i][j]),6);
                }
                temp+= 1/(j+sum);

            }
            double f = Math.pow(1.0/500+temp,-1);
            return f;
        }
    }

    class f15 extends f_xj {
        public double func(double x[]) {
            double aK[]={.1957, .1947, .1735, .16, .0844, .0627, .0456, .0342, .0323, .0235, .0246};
            double bK[]={.25, .5, 1, 2, 4, 6, 8, 10, 12, 14, 16};
            for (int i=0; i<bK.length; i++){
                bK[i] = 1.0/bK[i];
            }
            double temp = 0;
            for (int i=0; i<11; i++){
                temp+= Math.pow(aK[i]-(x[0]*(bK[i]*bK[i]+bK[i]*x[1]))/(bK[i]*bK[i]+bK[i]*x[2]+x[3]),2);
            }
            double f = temp;
            return f;
        }
    }

    class f16 extends f_xj {
        public double func(double x[]) {
            return 4*x[0]*x[0] - 2.1*Math.pow(x[0],4) + (1.0/3)*Math.pow(x[0], 6)+ x[0]*x[1] - 4*x[1]*x[1]+4*Math.pow(x[1],4);
        }
    }

    class f17 extends f_xj {
        public double func(double x[]) {
            double first=0.0;
            first=((x[1]-(5.1*x[0]*x[0]/(4.0*Math.PI*Math.PI))+(5.0*x[0]/Math.PI)-6.0)*(x[1]-(5.1*x[0]*x[0]/(4.0*Math.PI*Math.PI))+(5.0*x[0]/Math.PI)-6.0))
                    +(10.0*(1.0-(1.0/(8.0*Math.PI)))*Math.cos(x[0]))+10.0;
            return first;
        }
    }

    class f18 extends f_xj {
        public double func(double x[]) {
            double first=0.0;double second=0.0;
            first=(1.0+(x[0]+x[1]+1.0)*(x[0]+x[1]+1.0)*(19.0-14.0*x[0]+3.0*x[0]*x[0]-14.0*x[1]+6.0*x[0]*x[1]+3.0*x[1]*x[1]));
            second=30.0+(2.0*x[0]-3.0*x[1])*(2.0*x[0]-3.0*x[1])*(18.0-32.0*x[0]+12.0*x[0]*x[0]+48.0*x[1]-36.0*x[0]*x[1]+27*x[1]*x[1]);
            return first*second;
        }
    }

    class f19 extends f_xj {
        public double func(double x[]) {
            double a[][]={{3, 10, 30}, {.1, 10, 35}, {3, 10, 30},{.1, 10, 35}};
            double c[]={1, 1.2, 3, 3.2};
            double p[][]={{.3689, .117, .2673}, {.4699, .4387, .747}, {.1091, .8732, .5547}, {.03815, .5743, .8828}};
            double f = 0;
            for (int i=0; i<4; i++){
                double temp = 0;
                for (int j=0; j<3;j++){
                    temp+= a[i][j]*(x[j]-p[i][j])*(x[j]-p[i][j]);
                }
                temp = -temp;
                f+= c[i]*Math.exp(temp);
            }
            f = -f;
            return f;
        }
    }

    class f20 extends f_xj {
        public double func(double x[]) {
            double a[][]={{10, 3, 17, 3.5, 1.7, 8}, {.05, 10, 17, .1, 8, 14}, {3, 3.5, 1.7, 10, 17, 8}, {17, 8, .05, 10, .1, 14}};
            double c[]={1, 1.2, 3, 3.2};
            double p[][]={{.1312, .1696, .5569, .0124, .8283, .5886}, {.2329, .4135, .8307, .3736, .1004, .9991},
                    {.2348, .1415, .3522, .2883, .3047, .6650}, {.4047, .8828, .8732, .5743, .1091, .0381}};
            double f = 0;
            for (int i=0; i<4; i++){
                double temp = 0;
                for (int j=0; j<6;j++){
                    temp+= a[i][j]*(x[j]-p[i][j])*(x[j]-p[i][j]);
                }
                temp = -temp;
                f+= c[i]*Math.exp(temp);
            }
            f = -f;
            return f;
        }
    }

    class f21 extends f_xj {
        public double func(double x[]) {
            double a[][]={{4, 4, 4, 4}, {1, 1, 1, 1}, {8, 8, 8, 8}, {6, 6, 6, 6}, {3, 7, 3, 7}, {2, 9, 2, 9}, {5, 5, 3, 3}, {8, 1, 8, 1}, {6, 2, 6, 2}, {7, 3.6, 7, 3.6}};
            double c[]={.1, .2, .2, .4, .4, .6, .3, .7, .5, .5};
            double f = 0;
            for (int i=0; i<5; i++){
                double matrix1[] = new double[4];
                double matrix2[] = new double[4];
                double maxtrix_prod = 0;
                for (int j=0; j<4; j++) {
                    matrix1[j] = x[j]-a[i][j];
                    matrix2[j] = x[j]-a[i][j];
                    maxtrix_prod+= matrix1[j]*matrix2[j];
                }
                double temp = Math.pow(maxtrix_prod+c[i], -1);
                f+= temp;
            }
            f = -f;
            return f;
        }
    }

    class f22 extends f_xj {
        public double func(double x[]) {
            double a[][]={{4, 4, 4, 4}, {1, 1, 1, 1}, {8, 8, 8, 8}, {6, 6, 6, 6}, {3, 7, 3, 7}, {2, 9, 2, 9}, {5, 5, 3, 3}, {8, 1, 8, 1}, {6, 2, 6, 2}, {7, 3.6, 7, 3.6}};
            double c[]={.1, .2, .2, .4, .4, .6, .3, .7, .5, .5};
            double f = 0;
            for (int i=0; i<7; i++){
                double matrix1[] = new double[4];
                double matrix2[] = new double[4];
                double maxtrix_prod = 0;
                for (int j=0; j<4; j++) {
                    matrix1[j] = x[j]-a[i][j];
                    matrix2[j] = x[j]-a[i][j];
                    maxtrix_prod+= matrix1[j]*matrix2[j];
                }
                double temp = Math.pow(maxtrix_prod+c[i], -1);
                f+= temp;
            }
            f = -f;
            return f;
        }
    }

    class f23 extends f_xj {
        public double func(double x[]) {
            double a[][]={{4, 4, 4, 4}, {1, 1, 1, 1}, {8, 8, 8, 8}, {6, 6, 6, 6}, {3, 7, 3, 7}, {2, 9, 2, 9}, {5, 5, 3, 3}, {8, 1, 8, 1}, {6, 2, 6, 2}, {7, 3.6, 7, 3.6}};
            double c[]={.1, .2, .2, .4, .4, .6, .3, .7, .5, .5};
            double f = 0;
            for (int i=0; i<10; i++){
                double matrix1[] = new double[4];
                double matrix2[] = new double[4];
                double maxtrix_prod = 0;
                for (int j=0; j<4; j++) {
                    matrix1[j] = x[j]-a[i][j];
                    matrix2[j] = x[j]-a[i][j];
                    maxtrix_prod+= matrix1[j]*matrix2[j];
                }
                double temp = Math.pow(maxtrix_prod+c[i], -1);
                f+= temp;
            }
            f = -f;
            return f;
        }
    }

}
