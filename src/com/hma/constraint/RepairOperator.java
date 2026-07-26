package com.hma.constraint;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;
import java.util.*;

public class RepairOperator {
    
    // Thực hiện sửa chữa tất cả các ràng buộc kỹ thuật của phương án
    public static void repairAll(HMASolution sol, HMAConfig cfg) {
        // 1. Đảm bảo ràng buộc giới hạn chuyến đi (7) - mỗi chuyến đi tối đa 1 công trường
        repairTripLimit(sol, cfg);
        
        // 2. Sửa chữa nhu cầu (6) - thêm/bớt chuyến để bằng chính xác Ceil(Di/Q)
        repairDemand(sol, cfg);
        
        // 3. Đảm bảo ràng buộc thời gian tuần tự (8) và điều kiện không âm (11)
        repairSequenceAndDepartureTimes(sol, cfg);
    }
    
    // Phương trình (7): sum_i xikm <= 1. Đảm bảo mỗi chuyến m của xe k đi tối đa 1 công trường
    public static void repairTripLimit(HMASolution sol, HMAConfig cfg) {
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int activeCount = 0;
                int bestSite = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        activeCount++;
                        bestSite = i;
                    }
                }
                if (activeCount > 1) {
                    // Giữ lại công trường đầu tiên/tốt nhất, hủy các công trường còn lại
                    for (int i = 0; i < cfg.N; i++) {
                        if (i != bestSite) {
                            sol.xikm[i][k][m] = 0;
                        }
                    }
                }
            }
        }
    }
    
    // Phương trình (6): sum_k sum_m xikm = Ceil(Di / Q). Đảm bảo đáp ứng chính xác nhu cầu
    public static void repairDemand(HMASolution sol, HMAConfig cfg) {
        for (int i = 0; i < cfg.N; i++) {
            int requiredTrips = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            
            // Đếm số chuyến thực tế đang giao đến công trường i
            int actualTrips = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        actualTrips++;
                    }
                }
            }
            
            // Trường hợp 1: Thừa chuyến -> giảm bớt chuyến
            if (actualTrips > requiredTrips) {
                int surplus = actualTrips - requiredTrips;
                outerLoop:
                for (int k = cfg.T - 1; k >= 0; k--) {
                    for (int m = cfg.Mk - 1; m >= 0; m--) {
                        if (sol.xikm[i][k][m] == 1) {
                            sol.xikm[i][k][m] = 0;
                            surplus--;
                            if (surplus == 0) break outerLoop;
                        }
                    }
                }
            }
            // Trường hợp 2: Thiếu chuyến -> bổ sung chuyến
            else if (actualTrips < requiredTrips) {
                int deficit = requiredTrips - actualTrips;
                
                // Thử gán chuyến cho các xe đang hoạt động còn slot trống
                for (int k = 0; k < cfg.T; k++) {
                    if (sol.zk[k] == 1) {
                        for (int m = 0; m < cfg.Mk; m++) {
                            boolean isIdle = true;
                            for (int j = 0; j < cfg.N; j++) {
                                if (sol.xikm[j][k][m] == 1) {
                                    isIdle = false;
                                    break;
                                }
                            }
                            if (isIdle) {
                                sol.xikm[i][k][m] = 1;
                                deficit--;
                                if (deficit == 0) return;
                            }
                        }
                    }
                }
                
                // Nếu vẫn thiếu, huy động thêm xe mới chưa sử dụng
                for (int k = 0; k < cfg.T; k++) {
                    if (sol.zk[k] == 0) {
                        sol.zk[k] = 1; // Huy động xe
                        for (int m = 0; m < cfg.Mk; m++) {
                            sol.xikm[i][k][m] = 1;
                            deficit--;
                            if (deficit == 0) return;
                        }
                    }
                }
            }
        }
        
        // Tắt các xe không chạy chuyến nào
        for (int k = 0; k < cfg.T; k++) {
            boolean hasTrip = false;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        hasTrip = true;
                        break;
                    }
                }
                if (hasTrip) break;
            }
            if (!hasTrip) {
                sol.zk[k] = 0;
            }
        }
    }
    
    // Phương trình (8): txp_{km+1} >= txp_{km} + sum_i (2 * doi / v * 60 + dtdo) * xikm
    // Đảm bảo tính tuần tự thời gian chuyến xe và điều kiện không âm (11)
    public static void repairSequenceAndDepartureTimes(HMASolution sol, HMAConfig cfg) {
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            
            // 1. Sắp xếp các chuyến xe đang hoạt động theo thứ tự thời gian xuất phát ban đầu
            List<Integer> activeTripIndices = new ArrayList<>();
            for (int m = 0; m < cfg.Mk; m++) {
                boolean isActive = false;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        isActive = true;
                        break;
                    }
                }
                if (isActive) {
                    activeTripIndices.add(m);
                }
            }
            
            final double[] times = sol.txp_km[k];
            activeTripIndices.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Double.compare(times[o1], times[o2]);
                }
            });
            
            // 2. Điều chỉnh thời gian xuất phát đảm bảo tính tuần tự
            double currentTime = 0.0;
            for (int idx = 0; idx < activeTripIndices.size(); idx++) {
                int m = activeTripIndices.get(idx);
                
                // Đảm bảo txp_km >= 0
                if (sol.txp_km[k][m] < currentTime) {
                    sol.txp_km[k][m] = currentTime;
                }
                
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                
                if (site != -1) {
                    // Thời gian thực hiện chuyến khứ hồi = (2 * doi / v) * 60 + dtdo
                    double tripDuration = (2.0 * cfg.doi[site] / cfg.v) * 60.0 + cfg.dtdo;
                    currentTime = sol.txp_km[k][m] + tripDuration;
                    
                    // Nếu thời gian quay về vượt quá ca làm việc T_ca, hủy chuyến này và các chuyến sau
                    if (currentTime > cfg.T_ca) {
                        for (int remIdx = idx; remIdx < activeTripIndices.size(); remIdx++) {
                            int remM = activeTripIndices.get(remIdx);
                            for (int i = 0; i < cfg.N; i++) {
                                sol.xikm[i][k][remM] = 0;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
