package com.test;

import org.apache.commons.math3.special.Gamma;

import java.io.IOException;

public class DA {
    double [] lb;
    double [] ub;
    double [] r;
    double [] Delta_max;
    double Food_fitness;
    double [] Food_pos;
    double Enemy_fitness;
    double [] Enemy_pos;
    double [][] X;
    double [] Fitness;
    double [][] DeltaX;
    int dim;
    int SearchAgents_no;
    int Max_iteration;
    double inf = 10E+50;
    double Best_score;
    double [] Best_pos;
    f_xj fobj;
    public DA(f_xj fobj, double [] lb, double [] ub, int Max_iteration, int SearchAgents_no) {
        this.fobj = fobj;
        dim = ub.length;
        this.SearchAgents_no = SearchAgents_no;
        this.Max_iteration = Max_iteration;
        this.ub = ub;
        this.lb = lb;
        r = new double[dim];
        Delta_max = new double[dim];
        Food_fitness = inf;
        Food_pos = new double[dim];
        Enemy_fitness = -inf;
        Enemy_pos = new double[dim];
        X = new double[SearchAgents_no][dim];
        Fitness = new double[SearchAgents_no];
        DeltaX = new double[SearchAgents_no][dim];
        Best_score = 0;
        Best_pos = new double[dim];
    }

    void init(){
        //init Delta_max
        for (int i=0; i<dim; i++) {
            Delta_max[i] = (ub[i]-lb[i])/10;
        }

        //init X
        for (int i=0; i<SearchAgents_no; i++){
            for (int j=0; j<dim; j++){
                X[i][j] = lb[j] + (ub[j] - lb[j]) * nextRand();
            }
        }

        //init DeltaX
        for (int i=0; i<SearchAgents_no; i++){
            for (int j=0; j<dim; j++){
                DeltaX[i][j] = lb[j] + (ub[j] - lb[j]) * nextRand();
            }
        }
    }

    void solution() throws IOException {
        init();

        for (int iter=1; iter<=Max_iteration; iter++){
            for (int i=0; i<dim; i++) {
                r[i] = (ub[i]-lb[i])/4+((ub[i]-lb[i])*((double) iter/Max_iteration)*2);
            }
            double w = 0.9- (double) iter*((0.9-0.4)/Max_iteration);
            double my_c = 0.1- (double) iter*((0.1-0)/((double) Max_iteration/2));
            if (my_c<0) my_c = 0;

            double s= 2*nextRand()*my_c; // Seperation weight
            double a= 2*nextRand()*my_c; // Alignment weight
            double c= 2*nextRand()*my_c; // Cohesion weight
            double f= 2*nextRand();      // Food attraction weight
            double e=my_c;               // Enemy distraction weight

            for (int i=0; i<SearchAgents_no; i++){  //Calculate all the objective values first
                Fitness[i] = fobj.func(X[i]);
                if (Fitness[i] < Food_fitness){
                    Food_fitness = Fitness[i];
                    for (int j=0; j<dim; j++){
                        Food_pos[j] = X[i][j];
                    }
                }

                if (Fitness[i] > Enemy_fitness){
                    if (lt(X[i], ub) && gt(X[i], lb)) {
                        Enemy_fitness = Fitness[i];
                        for (int j=0; j<dim; j++){
                            Enemy_pos[j] = X[i][j];
                        }
                    }
                }
            }

            for (int i=0; i<SearchAgents_no; i++){
                int index=-1;
                int neighbours_no=0;
                double [][] Neighbours_DeltaX = new double[SearchAgents_no][dim];
                double [][] Neighbours_X = new double[SearchAgents_no][dim];
                //find the neighbouring solutions
                for (int j=0; j<SearchAgents_no; j++){
                    double [] Dist2Enemy = distance(X[i], X[j]);
                    double zero[] = new double[dim];
                    if (lte(Dist2Enemy, r) && ne(Dist2Enemy,zero)){
                        index = index+1;
                        neighbours_no = neighbours_no + 1;
                        for (int k=0; k<dim; k++){
                            Neighbours_DeltaX[index][k] = DeltaX[j][k];
                            Neighbours_X[index][k] = X[j][k];
                        }
                    }
                }

                //Seperation%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                //Eq. (3.1)
                double S[] = new double[dim];
                if (neighbours_no>1) {
                    for (int k=0; k<neighbours_no; k++){
                        for (int j=0; j<dim; j++) {
                            S[j] = S[j] + (Neighbours_X[k][j]-X[i][j]);
                        }
                    }
                    for (int j=0; j<dim; j++) {
                        S[j] = -S[j];
                    }
                }

                // Căn chỉnh (Alignment) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                // Phương trình (3.2)

                double [] A = new double[dim];

                if (neighbours_no > 1){
                    for (int j=0; j<dim; j++){
                        double sum = 0;
                        for (int k=0; k<neighbours_no; k++){
                            sum = sum + Neighbours_DeltaX[k][j];
                        }
                        A[j] = sum/neighbours_no;
                    }
                } else {
                    A = DeltaX[i];
                }

                // Tụ hội (Cohesion) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                // Phương trình (3.3)
                double C_temp[] = new double[dim];
                double [] C = new double[dim];
                if (neighbours_no > 1){
                    for (int j=0; j<dim; j++){
                        double sum = 0;
                        for (int k=0; k<neighbours_no; k++){
                            sum = sum + Neighbours_X[k][j];
                        }
                        C_temp[j] = sum/neighbours_no;
                    }
                } else {
                    for (int j=0; j<dim; j++){
                        C_temp[j] = X[i][j];
                    }
                }
                for (int j=0; j<dim; j++){
                    C[j]=C_temp[j]-X[i][j];
                }

                // Hướng về thức ăn (Attraction to food) %%%%%%%%%%%%%%%%%
                // Phương trình (3.4)
                double [] F = new double[dim];
                double [] Dist2Food = distance(X[i], Food_pos);
                if (lte(Dist2Food,r)){
                    for (int j=0; j<dim; j++){
                        F[j] = Food_pos[j]-X[i][j];
                    }
                }

                // Tránh xa kẻ thù (Distraction from enemy) %%%%%%%%%%%%%%%
                // Phương trình (3.5)
                double [] Enemy = new double[dim];
                double [] Dist2Enemy = distance(X[i], Enemy_pos);
                if (lte(Dist2Enemy,r)){
                    for (int j=0; j<dim; j++){
                        Enemy[j] = Enemy_pos[j]+X[i][j];
                    }
                }

                for (int j=0; j<dim; j++){
                    if (X[i][j]>ub[j]){
                        X[i][j] = lb[j];
                        DeltaX[i][j] = nextRand();
                    }
                    if (X[i][j]<lb[j]){
                        X[i][j] = ub[j];
                        DeltaX[i][j] = nextRand();
                    }
                }

                if (any_gt(Dist2Food, r)){
                    if (neighbours_no > 1){
                        for (int j=0; j<dim; j++){
                            DeltaX[i][j] = w*DeltaX[i][j] + nextRand()*A[j] + nextRand()*C[j] + nextRand()*S[j];
                            if (DeltaX[i][j] > Delta_max[j]){
                                DeltaX[i][j] = Delta_max[j];
                            }
                            if (DeltaX[i][j] < -Delta_max[j]){
                                DeltaX[i][j] = -Delta_max[j];
                            }
                            X[i][j] = X[i][j] + DeltaX[i][j];
                        }
                    } else {
                        // Chuyến bay Lévy (Lévy flight) khi không có hàng xóm - Phương trình (3.8)
                        double [] levy = Levy(dim);
                        for (int j=0; j<dim; j++){
                            X[i][j] = X[i][j] +  levy[j]*X[i][j];
                            DeltaX[i][j] = 0;
                        }
                    }
                } else {
                    for (int j=0; j<dim; j++){
                        DeltaX[i][j] = a*A[j] + c*C[j] + s*S[j] + f*F[j] + e*Enemy[j] + w*DeltaX[i][j];
                        if (DeltaX[i][j] > Delta_max[j]){
                            DeltaX[i][j] = Delta_max[j];
                        }
                        if (DeltaX[i][j] < -Delta_max[j]){
                            DeltaX[i][j] = -Delta_max[j];
                        }
                        X[i][j] = X[i][j] + DeltaX[i][j];
                    }
                }

                for (int j=0; j<dim; j++){
                    if (X[i][j] > ub[j]){
                        X[i][j] = ub[j];
                    }
                    if (X[i][j] < lb[j]){
                        X[i][j] = lb[j];
                    }
                }

            }
            Best_score=Food_fitness;
            Best_pos=Food_pos;
//            System.out.println("Iter: "+iter);
//            System.out.println("Best_score: "+Best_score);
        }
//
//        System.out.println("Best_score: "+Best_score);
//        System.out.println("Best_pos:");
//        for (int j=0; j<dim; j++){
//            System.out.print("x["+j+"]= "+Best_pos[j]+"\t");
//        }
//        System.out.println();
    }

    public double getRes(){
        return Best_score;
    }

    double [] Levy(int d){
        double beta = 3.0/2.0;
        //Eq. (3.10)
        double sigma = Math.pow(Gamma.gamma(1.0+beta)*Math.sin(Math.PI*beta/2.0)/(Gamma.gamma((1.0+beta)/2.0)*beta*Math.pow(2.0, (beta-1.0)/2.0)), 1.0/beta);
        double [] u = new double[d];
        double [] v = new double[d];
        double [] step = new double[d];
        for (int i=0; i<d; i++){
            u[i] = nextRand()*sigma;
            v[i] = nextRand();
            step[i] =  0.01*u[i]/(Math.pow(Math.abs(v[i]), 1.0/beta));
        }
        return step;
    }

    boolean gt(double x[], double y[]){   //greater than
        for (int i=0; i<x.length; i++){
            if (x[i]<=y[i]) return false;
        }
        return true;
    }

    boolean lt(double x[], double y[]){   //less than
        for (int i=0; i<x.length; i++){
            if (x[i]>=y[i]) return false;
        }
        return true;
    }

    boolean gte(double x[], double y[]){   //less than equal
        for (int i=0; i<x.length; i++){
            if (x[i]<y[i]) return false;
        }
        return true;
    }

    boolean lte(double x[], double y[]){   //less than equal
        for (int i=0; i<x.length; i++){
            if (x[i]>y[i]) return false;
        }
        return true;
    }

    boolean ne(double x[], double y[]){   //not equal
        for (int i=0; i<x.length; i++){
            if (x[i]==y[i]) return false;
        }
        return true;
    }

    boolean equal(double x[], double y[]){   //equal
        for (int i=0; i<x.length; i++){
            if (x[i]!=y[i]) return false;
        }
        return true;
    }

    boolean any_gt(double x[], double y[]){   //any greater than
        for (int i=0; i<x.length; i++){
            if (x[i]>y[i]) return true;
        }
        return false;
    }

    double [] distance(double a[], double b[]){
        double d[] = new double[a.length];
        for (int i=0; i<a.length; i++){
            d[i] = Math.sqrt((a[i]-b[i])*(a[i]-b[i]));
        }
        return d;
    }

    double nextRand(){
        return Math.random();
    }
}
