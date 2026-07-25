package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.SolutionPrinter;
import com.hma.constraint.RepairOperator;
import com.hma.cost.CostCalculator;

/**
 * HMAOptimizer_GWO - HMA Transportation Optimization using Grey Wolf Optimizer (GWO)
 *
 * GWO (Grey Wolf Optimizer) mimics the leadership hierarchy and hunting mechanism of grey wolves.
 * The pack is divided into 4 types:
 *   - Alpha (α): Best solution — leads the hunt
 *   - Beta  (β): Second best solution — assists alpha
 *   - Delta (δ): Third best solution — guides the rest
 *   - Omega (ω): Remaining wolves — follow α, β, δ
 *
 * Position update formula:
 *   X1 = Xα - A1 * |C1 * Xα - X|
 *   X2 = Xβ - A2 * |C2 * Xβ - X|
 *   X3 = Xδ - A3 * |C3 * Xδ - X|
 *   X_new = (X1 + X2 + X3) / 3
 *
 * Where:
 *   a decreases linearly from 2 to 0 over iterations (controls exploration vs exploitation)
 *   A = 2*a*r1 - a,  C = 2*r2   (r1, r2 are random in [0,1])
 */
public class HMAOptimizer_GWO {

    public static void main(String[] args) throws Exception {
        // ---------------------------------------------------------------
        // 1. PROBLEM SETUP
        // ---------------------------------------------------------------
        HMAConfig cfg = SampleData.getSampleConfig();

        System.out.println("===============================================================");
        System.out.println("    HMA TRANSPORTATION OPTIMIZATION WITH GWO (GREY WOLF)       ");
        System.out.println("===============================================================");
        System.out.println("Problem Scale:");
        System.out.println("  - Construction Sites (N) = " + cfg.N);
        System.out.println("  - Vehicles (T)           = " + cfg.T);
        System.out.println("  - Max Trips per Vehicle  = " + cfg.Mk);
        System.out.println("  - Search Space Dimension = " + cfg.dim);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Algorithm: GWO - Grey Wolf Optimizer");
        System.out.println("  Strategy: Alpha, Beta, Delta wolves guide Omega wolves");
        System.out.println("  Exploration: a linearly decreases from 2 -> 0 over iterations");
        System.out.println("---------------------------------------------------------------");

        // Define bounds [0.0, 1.0] for continuous mapping of discrete decisions
        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }

        // Initialize objective function (HMA fitness)
        f_xj fobj = new HMAFitness(cfg);

        // ---------------------------------------------------------------
        // 2. ALGORITHM PARAMETERS
        // ---------------------------------------------------------------
        int maxIter  = 300;   // Maximum iterations
        int popSize  = 40;    // Population size (number of grey wolves)

        System.out.println("Parameters:");
        System.out.println("  - Population Size  = " + popSize + " wolves");
        System.out.println("  - Max Iterations   = " + maxIter);
        System.out.println("===============================================================");

        // ---------------------------------------------------------------
        // 3. RUN GWO
        // ---------------------------------------------------------------
        System.out.println("\nRunning GWO optimization...");
        System.out.println("(Each dot = 50 iterations completed)");

        long startTime = System.currentTimeMillis();

        // GWO solver with internal convergence tracking
        GWO_HMA solver = new GWO_HMA(fobj, lb, ub, maxIter, popSize);
        solver.solution();

        long endTime = System.currentTimeMillis();

        // ---------------------------------------------------------------
        // 4. CONVERGENCE REPORT
        // ---------------------------------------------------------------
        System.out.println("\n\nOptimization completed in " + ((endTime - startTime) / 1000.0) + " seconds.");
        System.out.println("\nConvergence Milestones (Best Cost per Iteration):");
        System.out.printf("  - Iteration   1 : %,.0f VND%n", solver.convergenceHistory[1]);
        System.out.printf("  - Iteration  50 : %,.0f VND%n", solver.convergenceHistory[50]);
        System.out.printf("  - Iteration 100 : %,.0f VND%n", solver.convergenceHistory[100]);
        System.out.printf("  - Iteration 150 : %,.0f VND%n", solver.convergenceHistory[150]);
        System.out.printf("  - Iteration 200 : %,.0f VND%n", solver.convergenceHistory[200]);
        System.out.printf("  - Iteration 250 : %,.0f VND%n", solver.convergenceHistory[250]);
        System.out.printf("  - Iteration 300 : %,.0f VND%n", solver.convergenceHistory[300]);

        // Improvement percentage
        double initCost  = solver.convergenceHistory[1];
        double finalCost = solver.convergenceHistory[maxIter];
        double improvement = (initCost > 0) ? (initCost - finalCost) / initCost * 100.0 : 0;
        System.out.printf("%nImprovement from Iter 1 to %d: %.2f%%%n", maxIter, improvement);

        // ---------------------------------------------------------------
        // 5. DECODE AND PRINT SOLUTION
        // ---------------------------------------------------------------
        double[] bestPos = solver.Best_pos;
        HMASolution sol  = HMASolution.decode(bestPos, cfg);
        RepairOperator.repairAll(sol, cfg);

        // Print full solution schedule
        SolutionPrinter.printSolution(sol, cfg);

        // ---------------------------------------------------------------
        // 6. COST BREAKDOWN
        // ---------------------------------------------------------------
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        System.out.println("\nCost Breakdown (GWO Result):");
        System.out.printf("  Total Cost (TC)      : %,.0f VND%n", sol.TC);
        System.out.printf("  Fixed Cost           : %,.0f VND%n", sol.Cfixed);
        System.out.printf("  Operational Cost     : %,.0f VND%n", sol.Coperational);
        System.out.printf("  Temperature Penalty  : %,.0f VND%n", sol.Cpenalty);

        // ---------------------------------------------------------------
        // 7. EXPORT TO EXCEL
        // ---------------------------------------------------------------
        com.hma.utils.ExcelExporter.exportSolutionToExcel(sol, cfg, solver.convergenceHistory);

        System.out.println("===============================================================");
        System.out.println("                   GWO OPTIMIZATION DONE                      ");
        System.out.println("===============================================================");
    }
}

// -----------------------------------------------------------------------
// Inner helper: GWO_HMA extends the base GWO logic with convergence
// history tracking (for HMA-specific use, similar to DA_GWO style)
// -----------------------------------------------------------------------
class GWO_HMA {
    // GWO core fields
    double r1, r2;
    int N, D, maxiter;
    double[] alfa, beta, delta;
    double[] Lower, Upper;
    f_xj ff;
    double[][] XX;
    double X1, X2, X3;
    double[] fitness;
    double a;
    double A1, C1, A2, C2, A3, C3;

    // Result fields (public for external access)
    public double   Best_score;
    public double[] Best_pos;
    public double[] convergenceHistory;

    public GWO_HMA(f_xj fobj, double[] lb, double[] ub, int maxIter, int popSize) {
        this.ff      = fobj;
        this.Lower   = lb;
        this.Upper   = ub;
        this.maxiter = maxIter;
        this.N       = popSize;
        this.D       = ub.length;

        XX    = new double[N][D];
        alfa  = new double[D];
        beta  = new double[D];
        delta = new double[D];

        convergenceHistory = new double[maxIter + 1];
        Best_pos           = new double[D];
        Best_score         = Double.MAX_VALUE;
    }

    // Initialization: random positions, find alpha/beta/delta
    void init() throws Exception {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < D; j++) {
                XX[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
            }
        }
        XX = sort_and_index(XX);
        for (int j = 0; j < D; j++) alfa[j]  = XX[0][j];
        for (int j = 0; j < D; j++) beta[j]  = XX[1][j];
        for (int j = 0; j < D; j++) delta[j] = XX[2][j];
    }

    // Main optimization loop
    public void solution() throws Exception {
        init();

        for (int iter = 1; iter <= maxiter; iter++) {
            // a linearly decreases from 2 to 0 (controls balance between exploration & exploitation)
            a = 2.0 - (double) iter * (2.0 / (double) maxiter);

            // Update positions of all wolves (Omega wolves follow α, β, δ)
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < D; j++) {
                    // Interaction with Alpha
                    r1 = Math.random(); r2 = Math.random();
                    A1 = 2.0 * a * r1 - a;
                    C1 = 2.0 * r2;
                    X1 = alfa[j] - A1 * Math.abs(C1 * alfa[j] - XX[i][j]);
                    if (X1 < Lower[j] || X1 > Upper[j]) X1 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // Interaction with Beta
                    r1 = Math.random(); r2 = Math.random();
                    A2 = 2.0 * a * r1 - a;
                    C2 = 2.0 * r2;
                    X2 = beta[j] - A2 * Math.abs(C2 * beta[j] - XX[i][j]);
                    if (X2 < Lower[j] || X2 > Upper[j]) X2 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // Interaction with Delta
                    r1 = Math.random(); r2 = Math.random();
                    A3 = 2.0 * a * r1 - a;
                    C3 = 2.0 * r2;
                    X3 = delta[j] - A3 * Math.abs(C3 * delta[j] - XX[i][j]);
                    if (X3 < Lower[j] || X3 > Upper[j]) X3 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // New position = average of all three influences
                    XX[i][j] = (X1 + X2 + X3) / 3.0;
                }
            }

            // Apply bounds + sort to find new leaders
            XX = simplebounds(XX);
            XX = sort_and_index(XX);

            // Update Alpha, Beta, Delta roles
            for (int j = 0; j < D; j++) alfa[j]  = XX[0][j];
            for (int j = 0; j < D; j++) beta[j]  = XX[1][j];
            for (int j = 0; j < D; j++) delta[j] = XX[2][j];

            // Record best cost this iteration
            Best_score             = ff.func(XX[0]);
            Best_pos               = XX[0].clone();
            convergenceHistory[iter] = Best_score;

            // Progress indicator
            if (iter % 50 == 0) System.out.print(".");
        }
    }

    // Sort population by fitness (ascending = best first)
    double[][] sort_and_index(double[][] pop) throws Exception {
        int n = pop.length;
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) vals[i] = ff.func(pop[i]);

        // Bubble sort (stable, simple for small N)
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if (vals[j] < vals[i]) {
                    double tmp = vals[i]; vals[i] = vals[j]; vals[j] = tmp;
                    int   ti  = idx[i];  idx[i]  = idx[j];  idx[j]  = ti;
                }
            }
        }
        double[][] sorted = new double[n][D];
        for (int i = 0; i < n; i++) sorted[i] = pop[idx[i]].clone();
        return sorted;
    }

    // Clamp positions back to [lb, ub]
    double[][] simplebounds(double[][] s) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < D; j++) {
                if (s[i][j] < Lower[j]) s[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
                if (s[i][j] > Upper[j]) s[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
            }
        }
        return s;
    }
}
