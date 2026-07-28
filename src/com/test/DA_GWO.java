package com.test;

import org.apache.commons.math3.special.Gamma;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class DA_GWO {
    double [] lb;
    double [] ub;
    double [] r;
    double [] Delta_max;
    double Food_fitness;
    double [] Food_pos;
    double Enemy_fitness;
    double [] Enemy_pos;
    double [][] X;
    double [][] X_GWO;
    double [][] X_DA;
    double [] Fitness;
    double [][] DeltaX;
    int dim;
    int SearchAgents_no;
    int Max_iteration;
    double inf = 10E+50;
    double Best_score;
    double [] Best_pos;
    public double [] convergenceHistory;
    double gwoRatio; // Ty le ca the GWO trong quan the (0.0 - 1.0)
    f_xj fobj;

    double r1;
    double r2;
    double alfa[];
    double beta[];
    double delta[];
    double A1;
    double C1;
    double A2;
    double C2;
    double A3;
    double C3;
    double a;
    double X1;
    double X2;
    double X3;


    /** Constructor mac dinh: gwoRatio = 0.2 (20% GWO, 80% DA) */
    public DA_GWO(f_xj fobj, double [] lb, double [] ub, int Max_iteration, int SearchAgents_no) {
        this(fobj, lb, ub, Max_iteration, SearchAgents_no, 0.2);
    }

    /**
     * Constructor mo rong: cho phep dieu chinh ty le GWO dong.
     * @param gwoRatio ty le ca the GWO trong quan the (vi du: 0.2 = 20% GWO, 80% DA)
     */
    public DA_GWO(f_xj fobj, double [] lb, double [] ub, int Max_iteration, int SearchAgents_no, double gwoRatio) {
        //khoi tao chung
        this.fobj = fobj;   //ham muc tieu
        dim = ub.length;    //so chieu khong gian (so luong bien)
        this.SearchAgents_no = SearchAgents_no; //so luong ca the (chuon chuon, soi xam)
        this.Max_iteration = Max_iteration;     //so vong lap toi da
        this.ub = ub;                           //bien tren
        this.lb = lb;                           //bien duoi
        this.gwoRatio = Math.max(0.05, Math.min(0.95, gwoRatio)); // kep vao [0.05, 0.95]
        X = new double[SearchAgents_no][dim];   //vi tri cua ca the
        X_GWO = new double[SearchAgents_no][dim];   //vi tri cua ca the soi xam
        X_DA = new double[SearchAgents_no][dim];    //vi tri cua ca the chuon chuon
        Best_score = 0;                         //gia tri toi uu
        Best_pos = new double[dim];             //vi tri ca the toi uu
        convergenceHistory = new double[Max_iteration + 1];

        //khoi tao cho GWO
        alfa = new double[dim];     //vi tri alpha
        beta = new double[dim];     //vi tri bete
        delta = new double[dim];    //vi tri delta

        //khoi tao cho DA
        r = new double[dim];            //ban kinh anh huong
        Delta_max = new double[dim];    //buoc nhay delta max
        Fitness = new double[SearchAgents_no];  //bien luu gia tri ham muc tieu
        Food_fitness = inf;         //gia tri ham muc tieu tai vi tri con moi, cung la gia tri toi uu hien tai
        Food_pos = new double[dim]; //vi tri cua con moi
        Enemy_fitness = -inf;       //gia tri ham muc tieu tai vi tri ke thu
        Enemy_pos = new double[dim];//vi tri cua ke thu
        DeltaX = new double[SearchAgents_no][dim];  // buoc nhay delta X
    }

    void init() throws IOException {
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

        X=sort_and_index(X, SearchAgents_no);

        for(int i = 0; i < dim; i++) {
            alfa[i] = X[0][i];
        }

        for(int i = 0; i < dim; i++) {
            beta[i] = X[1][i];
        }

        for(int i = 0; i < dim; i++) {
            delta[i] = X[2][i];
        }
    }

    void solution() throws IOException {
        init();

        // ============================================================
        // TY LE LAI DONG (Dynamic Ratio): dieu chinh bang tham so gwoRatio
        // gwoRatio=0.2 => 20% GWO - 80% DA (mac dinh)
        // gwoRatio=0.3 => 30% GWO - 70% DA
        // gwoRatio=0.5 => 50% GWO - 50% DA
        // ============================================================
        final int N_GWO = (int) Math.max(1, Math.round(SearchAgents_no * gwoRatio));
        final int N_DA  = SearchAgents_no - N_GWO;

        for (int iter=1; iter<=Max_iteration; iter++){
            //tach quan the thanh GWO va DA (N_GWO ca the dau, N_DA ca the sau)
            for (int i=0; i<N_GWO; i++){
                for (int j=0; j<dim; j++){
                    X_GWO[i][j] = X[i][j];
                }
            }
            for (int i=N_GWO; i<SearchAgents_no; i++){
                for (int j=0; j<dim; j++){
                    X_DA[i-N_GWO][j] = X[i][j];
                }
            }

//            System.out.println("X value: "+iter);
//            for (int i=0; i<SearchAgents_no; i++){
//                for (int j=0; j<dim; j++){
//                    System.out.print(X[i][j]+"\t");
//                }
//                System.out.println();
//            }

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

            //GWO
            a = 2.0 -((double)iter * (2.0 / (double) Max_iteration));
            for(int i = 0; i < N_GWO; i++) {
                for(int j = 0; j < dim; j++)
                {
                    r1 = nextRand();
                    r2 = nextRand();
                    A1 = 2.0 * a * r1 - a;
                    C1 = 2.0 * r2;
                    X1 = alfa[j] - A1 * (Math.abs(C1 * alfa[j] - X_GWO[i][j]));
                    if (X1<lb[j] || X1>ub[j]) X1 = lb[j] + ((ub[j] - lb[j]) * nextRand());

                    r1 = nextRand();
                    r2 = nextRand();
                    A2 = 2.0 * a * r1 - a;
                    C2 = 2.0*r2;
                    X2 = beta[j] - A2 * (Math.abs(C2 * beta[j] - X_GWO[i][j]));
                    if (X2<lb[j] || X2>ub[j]) X2 = lb[j] + ((ub[j] - lb[j]) * nextRand());

                    r1 = nextRand();
                    r2 = nextRand();
                    A3 = 2.0 * a * r1 - a;
                    C3 = 2.0 * r2;
                    X3 = delta[j] - A3 * (Math.abs(C3 * delta[j] - X_GWO[i][j]));
                    if (X3<lb[j] || X3>ub[j]) X3 = lb[j] + ((ub[j] - lb[j]) * nextRand());
                    X_GWO[i][j] = (X1 + X2 + X3) / 3.0;
                }
            }

            //DA
            for (int i=0; i<dim; i++) {
                r[i] = (ub[i]-lb[i])/4+((ub[i]-lb[i])*((double) iter/Max_iteration)*2);
            }
            double w = 0.9- (double) iter*((0.9-0.4)/Max_iteration);
            double my_c = 0.1- (double) iter*((0.1-0)/((double) Max_iteration/2));
            if (my_c<0) my_c = 0;

            double s= 2*nextRand()*my_c; // Trọng số Phân tách (Separation)
            double alignment= 2*nextRand()*my_c; // Trọng số Căn chỉnh (Alignment)
            double c= 2*nextRand()*my_c; // Trọng số Tụ hội (Cohesion)
            double f= 2*nextRand();      // Trọng số Thu hút thức ăn (Food)
            double e=my_c;               // Trọng số Xua đuổi kẻ thù (Enemy)

//            for (int i=0; i<N_DA; i++){  //Calculate all the objective values first
//                Fitness[i] = fobj.func(X_DA[i]);
//                if (Fitness[i] < Food_fitness){
//                    Food_fitness = Fitness[i];
//                    for (int j=0; j<dim; j++){
//                        Food_pos[j] = X_DA[i][j];
//                    }
//                }
//
//                if (Fitness[i] > Enemy_fitness){
//                    if (lt(X_DA[i], ub) && gt(X_DA[i], lb)) {
//                        Enemy_fitness = Fitness[i];
//                        for (int j=0; j<dim; j++){
//                            Enemy_pos[j] = X_DA[i][j];
//                        }
//                    }
//                }
//            }

            for (int i=0; i<N_DA; i++){
                int index=-1;
                int neighbours_no=0;
                double [][] Neighbours_DeltaX = new double[N_DA][dim];
                double [][] Neighbours_X = new double[N_DA][dim];
                // Tìm các phương án lân cận trong bán kính r
                for (int j=0; j<N_DA; j++){
                    double [] Dist2Enemy = distance(X_DA[i], X_DA[j]);
                    double zero[] = new double[dim];
                    if (lte(Dist2Enemy, r) && ne(Dist2Enemy,zero)){
                        index = index+1;
                        neighbours_no = neighbours_no + 1;
                        for (int k=0; k<dim; k++){
                            Neighbours_DeltaX[index][k] = DeltaX[j][k];
                            Neighbours_X[index][k] = X_DA[j][k];
                        }
                    }
                }

                // Phân tách (Separation) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
                // Phương trình (3.1)
                double S[] = new double[dim];
                if (neighbours_no>1) {
                    for (int k=0; k<neighbours_no; k++){
                        for (int j=0; j<dim; j++) {
                            S[j] = S[j] + (Neighbours_X[k][j]-X_DA[i][j]);
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
                        C_temp[j] = X_DA[i][j];
                    }
                }
                for (int j=0; j<dim; j++){
                    C[j]=C_temp[j]-X_DA[i][j];
                }

                // Hướng về thức ăn (Attraction to food) %%%%%%%%%%%%%%%%%
                // Phương trình (3.4)
                double [] F = new double[dim];
                double [] Dist2Food = distance(X_DA[i], Food_pos);
                if (lte(Dist2Food,r)){
                    for (int j=0; j<dim; j++){
                        F[j] = Food_pos[j]-X_DA[i][j];
                    }
                }

                // Tránh xa kẻ thù (Distraction from enemy) %%%%%%%%%%%%%%%
                // Phương trình (3.5)
                double [] Enemy = new double[dim];
                double [] Dist2Enemy = distance(X_DA[i], Enemy_pos);
                if (lte(Dist2Enemy,r)){
                    for (int j=0; j<dim; j++){
                        Enemy[j] = Enemy_pos[j]+X_DA[i][j];
                    }
                }

                for (int j=0; j<dim; j++){
                    if (X_DA[i][j]>ub[j]){
                        X_DA[i][j] = lb[j];
                        DeltaX[i][j] = nextRand();
                    }
                    if (X_DA[i][j]<lb[j]){
                        X_DA[i][j] = ub[j];
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
                            X_DA[i][j] = X_DA[i][j] + DeltaX[i][j];
                        }
                    } else {
                        // Chuyến bay Lévy (Lévy flight) khi không có hàng xóm - Phương trình (3.8)
                        double [] levy = Levy(dim);
                        for (int j=0; j<dim; j++){
                            X_DA[i][j] = X_DA[i][j] +  levy[j]*X_DA[i][j];
                            DeltaX[i][j] = 0;
                        }
                    }
                } else {
                    for (int j=0; j<dim; j++){
                        DeltaX[i][j] = alignment*A[j] + c*C[j] + s*S[j] + f*F[j] + e*Enemy[j] + w*DeltaX[i][j];
                        if (DeltaX[i][j] > Delta_max[j]){
                            DeltaX[i][j] = Delta_max[j];
                        }
                        if (DeltaX[i][j] < -Delta_max[j]){
                            DeltaX[i][j] = -Delta_max[j];
                        }
                        X_DA[i][j] = X_DA[i][j] + DeltaX[i][j];
                    }
                }

                for (int j=0; j<dim; j++){
                    if (X_DA[i][j] > ub[j]){
                        X_DA[i][j] = ub[j];
                    }
                    if (X_DA[i][j] < lb[j]){
                        X_DA[i][j] = lb[j];
                    }
                }

            }

            //tong hop 2 quan the GWO va DA
            for (int i=0; i<N_GWO; i++){
                for (int j=0; j<dim; j++){
                    X[i][j]=X_GWO[i][j];
                }
            }
            for (int i=N_GWO; i<SearchAgents_no; i++){
                for (int j=0; j<dim; j++){
                    X[i][j]=X_DA[i-N_GWO][j];
                }
            }

            X = simplebounds(X, SearchAgents_no);
            X = sort_and_index(X, SearchAgents_no);
//            TODO: check
//            for(int i = 0; i < dim; i++) {
//                X[SearchAgents_no-1][i] = X[0][i];
//            }

            for(int i = 0; i < dim; i++) {
                alfa[i] = X[0][i];
            }

            for(int i = 0; i < dim; i++) {
                beta[i] = X[1][i];
            }

            for(int i = 0; i < dim; i++) {
                delta[i] = X[2][i];
            }

            Best_score=fobj.func(X[0]);
            Best_pos=X[0];
            convergenceHistory[iter] = Best_score;
//            System.out.println("Iter: "+iter);
//            System.out.println("Best_score: "+Best_score);
        }

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

    double[][] sort_and_index(double[][] XXX, int N) throws IOException {
        double[] yval = new double[N];

        for(int i = 0; i < N; i++) {
            yval[i] = fobj.func(XXX[i]);
        }

        ArrayList<Double> nfit = new ArrayList<Double>();

        for(int i = 0; i < N; i++) {
            nfit.add(yval[i]);
        }

        ArrayList<Double> nstore = new ArrayList<Double>(nfit);
        Collections.sort(nfit);

        double[] ret = new double[nfit.size()];
        Iterator<Double> iterator = nfit.iterator();

        int ii = 0;

        while(iterator.hasNext()) {
            ret[ii] = iterator.next().doubleValue();
            ii++;
        }

        int[] indexes = new int[nfit.size()];

        for(int n = 0; n < nfit.size(); n++) {
            indexes[n] = nstore.indexOf(nfit.get(n));
        }

        double[][] B = new double[N][dim];

        for(int i = 0; i < N; i++) {
            for(int j = 0; j < dim; j++) {
                B[i][j] = XXX[indexes[i]][j];
            }
        }

        return B ;
    }

    double[][] simplebounds(double s[][], int N) {
        for(int i = 0; i < N; i++) {
            for(int j = 0; j < dim; j++) {
                if(s[i][j] < lb[j]) {
                    s[i][j] = lb[j] + ((ub[j] - lb[j]) * nextRand());
                }

                if(s[i][j] > ub[j]) {
                    s[i][j] = lb[j] + ((ub[j] - lb[j]) * nextRand());
                }
            }
        }
        return s;
    }

    double nextRand(){
        return Math.random();
    }
}
