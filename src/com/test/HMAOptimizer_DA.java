package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.SolutionPrinter;
import com.hma.constraint.RepairOperator;
import com.hma.cost.CostCalculator;
import org.apache.commons.math3.special.Gamma;

/**
 * HMAOptimizer_DA - HMA Transportation Optimization using Dragonfly Algorithm (DA)
 *
 * DA (Dragonfly Algorithm) simulates the static and dynamic swarming behaviors of dragonflies.
 * Each dragonfly updates its step vector (ΔX) based on 5 swarm behaviors:
 *
 *   1. Separation (S)   - Avoid collision with neighbors            [Eq. 3.1]
 *   2. Alignment  (A)   - Match velocity with neighbors             [Eq. 3.2]
 *   3. Cohesion   (C)   - Move toward the center of the swarm      [Eq. 3.3]
 *   4. Food       (F)   - Attraction to best solution (food)        [Eq. 3.4]
 *   5. Enemy      (E)   - Repulsion from worst solution (enemy)     [Eq. 3.5]
 *
 * When no neighbors exist, Lévy flight is used for random walk:     [Eq. 3.8]
 *   ΔX_new = w*ΔX + rand*A + rand*C + rand*S
 *   X_new  = X + ΔX
 *
 * Neighborhood radius r expands progressively from 25% to 75% of search space.
 * Inertia weight w decreases from 0.9 to 0.4 (encourages exploitation late on).
 */
public class HMAOptimizer_DA {

    public static void main(String[] args) throws Exception {
        // ---------------------------------------------------------------
        // 1. PROBLEM SETUP
        // ---------------------------------------------------------------
        HMAConfig cfg = SampleData.getSampleConfig();

        System.out.println("===============================================================");
        System.out.println("   HMA TRANSPORTATION OPTIMIZATION WITH DA (DRAGONFLY ALG.)   ");
        System.out.println("===============================================================");
        System.out.println("Problem Scale:");
        System.out.println("  - Construction Sites (N) = " + cfg.N);
        System.out.println("  - Vehicles (T)           = " + cfg.T);
        System.out.println("  - Max Trips per Vehicle  = " + cfg.Mk);
        System.out.println("  - Search Space Dimension = " + cfg.dim);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Algorithm: DA - Dragonfly Algorithm");
        System.out.println("  Strategy: 5 swarm behaviors + Levy flight for lone dragonflies");
        System.out.println("  Food Source  : Best solution found so far (minimization target)");
        System.out.println("  Enemy Source : Worst solution within bounds (to flee from)");
        System.out.println("  Neighborhood : Radius expands from 25% to 75% of search space");
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
        int maxIter  = 300;  // Maximum iterations
        int popSize  = 40;   // Population size (number of dragonflies)

        System.out.println("Parameters:");
        System.out.println("  - Population Size  = " + popSize + " dragonflies");
        System.out.println("  - Max Iterations   = " + maxIter);
        System.out.println("  - Inertia w        : 0.9 -> 0.4 (linearly decreasing)");
        System.out.println("  - my_c             : 0.1 -> 0   (behavior weight control)");
        System.out.println("===============================================================");

        // ---------------------------------------------------------------
        // 3. RUN DA
        // ---------------------------------------------------------------
        System.out.println("\nRunning DA optimization...");
        System.out.println("(Each dot = 50 iterations completed)");

        long startTime = System.currentTimeMillis();

        DA_HMA solver = new DA_HMA(fobj, lb, ub, maxIter, popSize);
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
        double initCost   = solver.convergenceHistory[1];
        double finalCost  = solver.convergenceHistory[maxIter];
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
        System.out.println("\nCost Breakdown (DA Result):");
        System.out.printf("  Total Cost (TC)      : %,.0f VND%n", sol.TC);
        System.out.printf("  Fixed Cost           : %,.0f VND%n", sol.Cfixed);
        System.out.printf("  Operational Cost     : %,.0f VND%n", sol.Coperational);
        System.out.printf("  Temperature Penalty  : %,.0f VND%n", sol.Cpenalty);

        // ---------------------------------------------------------------
        // 7. EXPORT TO EXCEL
        // ---------------------------------------------------------------
        com.hma.utils.ExcelExporter.exportSolutionToExcel(sol, cfg, solver.convergenceHistory);

        System.out.println("===============================================================");
        System.out.println("                    DA OPTIMIZATION DONE                      ");
        System.out.println("===============================================================");
    }
}

// -----------------------------------------------------------------------
// Inner helper: DA_HMA — full DA implementation with convergence tracking
// for HMA-specific optimization (mirrors DA.java but with public history)
// -----------------------------------------------------------------------
class DA_HMA {
    // DA state fields
    double[]   lb, ub;
    double[]   r;              // Neighborhood radius per dimension
    double[]   Delta_max;      // Max allowed step size
    double     Food_fitness;   // Best fitness found (food source)
    double[]   Food_pos;       // Position of food source
    double     Enemy_fitness;  // Worst fitness found (enemy)
    double[]   Enemy_pos;      // Position of enemy
    double[][] X;              // Dragonfly positions
    double[]   Fitness;        // Fitness values
    double[][] DeltaX;         // Step vectors (velocity equivalent)
    int        dim;
    int        SearchAgents_no;
    int        Max_iteration;
    double     inf = 10E+50;

    // Result fields (public for external access)
    public double   Best_score;
    public double[] Best_pos;
    public double[] convergenceHistory;

    f_xj fobj;

    public DA_HMA(f_xj fobj, double[] lb, double[] ub, int maxIter, int popSize) {
        this.fobj              = fobj;
        this.lb                = lb;
        this.ub                = ub;
        this.Max_iteration     = maxIter;
        this.SearchAgents_no   = popSize;
        this.dim               = ub.length;

        r            = new double[dim];
        Delta_max    = new double[dim];
        Food_pos     = new double[dim];
        Enemy_pos    = new double[dim];
        X            = new double[popSize][dim];
        Fitness      = new double[popSize];
        DeltaX       = new double[popSize][dim];
        Food_fitness  = inf;
        Enemy_fitness = -inf;

        convergenceHistory = new double[maxIter + 1];
        Best_pos           = new double[dim];
        Best_score         = Double.MAX_VALUE;
    }

    // Initialization
    void init() {
        // Delta_max = (ub - lb) / 10
        for (int i = 0; i < dim; i++) {
            Delta_max[i] = (ub[i] - lb[i]) / 10.0;
        }
        // Random initial positions
        for (int i = 0; i < SearchAgents_no; i++) {
            for (int j = 0; j < dim; j++) {
                X[i][j]      = lb[j] + (ub[j] - lb[j]) * Math.random();
                DeltaX[i][j] = lb[j] + (ub[j] - lb[j]) * Math.random();
            }
        }
    }

    // Main DA optimization loop
    public void solution() throws Exception {
        init();

        for (int iter = 1; iter <= Max_iteration; iter++) {

            // Update neighborhood radius r (expands from 25% to 75% of range)
            for (int i = 0; i < dim; i++) {
                r[i] = (ub[i] - lb[i]) / 4.0 + ((ub[i] - lb[i]) * ((double) iter / Max_iteration) * 2.0);
            }

            // Inertia weight w: 0.9 -> 0.4
            double w     = 0.9 - (double) iter * ((0.9 - 0.4) / Max_iteration);
            // Behavior weight control my_c: 0.1 -> 0 (first half), then stays 0
            double my_c  = 0.1 - (double) iter * ((0.1 - 0.0) / ((double) Max_iteration / 2.0));
            if (my_c < 0) my_c = 0;

            // Randomized weights for each behavior
            double s         = 2 * Math.random() * my_c;  // Separation weight
            double alignment = 2 * Math.random() * my_c;  // Alignment weight
            double c         = 2 * Math.random() * my_c;  // Cohesion weight
            double f         = 2 * Math.random();          // Food attraction weight
            double e         = my_c;                       // Enemy distraction weight

            // ---- Evaluate all dragonflies; update food and enemy ----
            for (int i = 0; i < SearchAgents_no; i++) {
                Fitness[i] = fobj.func(X[i]);

                // Update food source (best solution)
                if (Fitness[i] < Food_fitness) {
                    Food_fitness = Fitness[i];
                    System.arraycopy(X[i], 0, Food_pos, 0, dim);
                }

                // Update enemy source (worst solution within bounds)
                if (Fitness[i] > Enemy_fitness) {
                    if (lt(X[i], ub) && gt(X[i], lb)) {
                        Enemy_fitness = Fitness[i];
                        System.arraycopy(X[i], 0, Enemy_pos, 0, dim);
                    }
                }
            }

            // ---- Update each dragonfly's step and position ----
            for (int i = 0; i < SearchAgents_no; i++) {
                int neighbours_no = 0;
                double[][] Neighbours_DeltaX = new double[SearchAgents_no][dim];
                double[][] Neighbours_X      = new double[SearchAgents_no][dim];
                int index = -1;

                // Find neighbors within radius r
                double[] zero = new double[dim];
                for (int j = 0; j < SearchAgents_no; j++) {
                    double[] dist = distance(X[i], X[j]);
                    if (lte(dist, r) && ne(dist, zero)) {
                        index++;
                        neighbours_no++;
                        Neighbours_DeltaX[index] = DeltaX[j].clone();
                        Neighbours_X[index]      = X[j].clone();
                    }
                }

                // -- Separation (Eq. 3.1): avoid crowding --
                double[] S = new double[dim];
                if (neighbours_no > 1) {
                    for (int k = 0; k < neighbours_no; k++) {
                        for (int j = 0; j < dim; j++) {
                            S[j] += (Neighbours_X[k][j] - X[i][j]);
                        }
                    }
                    for (int j = 0; j < dim; j++) S[j] = -S[j];
                }

                // -- Alignment (Eq. 3.2): match neighbor velocities --
                double[] A = new double[dim];
                if (neighbours_no > 1) {
                    for (int j = 0; j < dim; j++) {
                        double sum = 0;
                        for (int k = 0; k < neighbours_no; k++) sum += Neighbours_DeltaX[k][j];
                        A[j] = sum / neighbours_no;
                    }
                } else {
                    A = DeltaX[i].clone();
                }

                // -- Cohesion (Eq. 3.3): move toward neighbor center --
                double[] C_temp = new double[dim];
                double[] C      = new double[dim];
                if (neighbours_no > 1) {
                    for (int j = 0; j < dim; j++) {
                        double sum = 0;
                        for (int k = 0; k < neighbours_no; k++) sum += Neighbours_X[k][j];
                        C_temp[j] = sum / neighbours_no;
                    }
                } else {
                    C_temp = X[i].clone();
                }
                for (int j = 0; j < dim; j++) C[j] = C_temp[j] - X[i][j];

                // -- Food Attraction (Eq. 3.4): move toward food --
                double[] F           = new double[dim];
                double[] Dist2Food   = distance(X[i], Food_pos);
                if (lte(Dist2Food, r)) {
                    for (int j = 0; j < dim; j++) F[j] = Food_pos[j] - X[i][j];
                }

                // -- Enemy Distraction (Eq. 3.5): flee from enemy --
                double[] Enemy       = new double[dim];
                double[] Dist2Enemy  = distance(X[i], Enemy_pos);
                if (lte(Dist2Enemy, r)) {
                    for (int j = 0; j < dim; j++) Enemy[j] = Enemy_pos[j] + X[i][j];
                }

                // Boundary wrap-around
                for (int j = 0; j < dim; j++) {
                    if (X[i][j] > ub[j]) { X[i][j] = lb[j]; DeltaX[i][j] = Math.random(); }
                    if (X[i][j] < lb[j]) { X[i][j] = ub[j]; DeltaX[i][j] = Math.random(); }
                }

                // -- Update step ΔX and position X --
                if (any_gt(Dist2Food, r)) {
                    // Not near food: either swarm update or Lévy flight
                    if (neighbours_no > 1) {
                        // Swarm behavior update
                        for (int j = 0; j < dim; j++) {
                            DeltaX[i][j] = w * DeltaX[i][j]
                                         + Math.random() * A[j]
                                         + Math.random() * C[j]
                                         + Math.random() * S[j];
                            DeltaX[i][j] = Math.max(-Delta_max[j], Math.min(Delta_max[j], DeltaX[i][j]));
                            X[i][j]      = X[i][j] + DeltaX[i][j];
                        }
                    } else {
                        // Lévy flight (Eq. 3.8) — explore when isolated
                        double[] levy = Levy(dim);
                        for (int j = 0; j < dim; j++) {
                            X[i][j]      = X[i][j] + levy[j] * X[i][j];
                            DeltaX[i][j] = 0;
                        }
                    }
                } else {
                    // Near food: full behavior-weighted update
                    for (int j = 0; j < dim; j++) {
                        DeltaX[i][j] = alignment * A[j] + c * C[j] + s * S[j]
                                     + f * F[j] + e * Enemy[j]
                                     + w * DeltaX[i][j];
                        DeltaX[i][j] = Math.max(-Delta_max[j], Math.min(Delta_max[j], DeltaX[i][j]));
                        X[i][j]      = X[i][j] + DeltaX[i][j];
                    }
                }

                // Clamp to bounds
                for (int j = 0; j < dim; j++) {
                    X[i][j] = Math.max(lb[j], Math.min(ub[j], X[i][j]));
                }
            }

            // Record the best (food) as this iteration's result
            Best_score               = Food_fitness;
            Best_pos                 = Food_pos.clone();
            convergenceHistory[iter] = Best_score;

            // Progress indicator
            if (iter % 50 == 0) System.out.print(".");
        }
    }

    // ---- Lévy Flight random step (Eq. 3.10) ----
    double[] Levy(int d) {
        double beta  = 3.0 / 2.0;
        double sigma = Math.pow(
            Gamma.gamma(1.0 + beta) * Math.sin(Math.PI * beta / 2.0)
            / (Gamma.gamma((1.0 + beta) / 2.0) * beta * Math.pow(2.0, (beta - 1.0) / 2.0)),
            1.0 / beta
        );
        double[] step = new double[d];
        for (int i = 0; i < d; i++) {
            double u = Math.random() * sigma;
            double v = Math.random();
            step[i]  = 0.01 * u / Math.pow(Math.abs(v), 1.0 / beta);
        }
        return step;
    }

    // ---- Utility comparison helpers ----
    boolean gt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] <= y[i]) return false;
        return true;
    }
    boolean lt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] >= y[i]) return false;
        return true;
    }
    boolean lte(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] > y[i]) return false;
        return true;
    }
    boolean ne(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] == y[i]) return false;
        return true;
    }
    boolean any_gt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] > y[i]) return true;
        return false;
    }
    double[] distance(double[] a, double[] b) {
        double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) d[i] = Math.sqrt((a[i] - b[i]) * (a[i] - b[i]));
        return d;
    }
}
