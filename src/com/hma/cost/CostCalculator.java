package com.hma.cost;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;

public class CostCalculator {
    private HMAConfig cfg;
    
    public CostCalculator(HMAConfig cfg) {
        this.cfg = cfg;
    }
    
    // Eq. (2): Cfixed = sum(f * zk)
    public double calcFixedCost(HMASolution sol) {
        double cFixed = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            cFixed += cfg.f * sol.zk[k];
        }
        return cFixed;
    }
    
    // Eq. (3): Coperational = sum(2 * doi * coi * xikm)
    public double calcOperationalCost(HMASolution sol) {
        double cOper = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    cOper += 2.0 * cfg.doi[i] * cfg.coi[i] * sol.xikm[i][k][m];
                }
            }
        }
        return cOper;
    }
    
    // Eq. (9): Tikm = To - 0.5 * (doi / (v * 60))
    public double calcTemperature(int i) {
        // doi is in km, v is in km/h
        // v * 60 converts speed to km/min? No:
        // Let's analyze units: doi / v gives travel time in hours.
        // If we multiply v by 60, then it's in km/min?
        // Yes, v km/h = v/60 km/min.
        // If the formula is doi / (v * 60), it would be doi / (v_km_h * 60) = (doi / v_km_h) / 60.
        // That is travel time in hours divided by 60, which is travel time in minutes divided by 3600.
        // Wait, v * 60 is (v km/h) * 60. That is a speed of 60v km/h. That makes no sense.
        // Wait, the PDF says: Tikm = To - 0.5 * (doi / (v * 60))? Or is it doi / (v / 60)?
        // The PDF output says:
        // "Tikm = To - 0.5 * ( doi / (v * 60) )"
        // Let's calculate: doi / (v * 60). If doi = 25km, v = 40km/h:
        // 25 / (40 * 60) = 25 / 2400 = 0.0104.
        // 0.5 * 0.0104 = 0.0052 C.
        // If the formula in the PDF is indeed: To - 0.5 * (doi / (v * 60)), we must implement it EXACTLY as written.
        // But if it is actually To - 0.5 * (doi / (v / 60)) (which would be doi / speed_in_km_per_minute = travel time in minutes):
        // Travel time in minutes = doi / (v / 60) = 25 / (40/60) = 25 * 60 / 40 = 37.5 minutes.
        // Then T = To - 0.5 * 37.5 = To - 18.75 C.
        // With To = 160 C, Tikm = 160 - 18.75 = 141.25 C. (This is highly realistic for asphalt transportation!)
        // In the PDF extraction, the text says: To - 0.5 * \frac{doi}{v * 60}? Let's re-read PDF text:
        // "To - 0.5 * ( doi / (v * 60) )"
        // Wait! It could be a typo in the PDF paper itself. Let's think.
        // If we compute it as written: To - 0.5 * (doi / (v * 60)), it's a minor temp loss.
        // Let's implement it exactly as: To - 0.5 * (doi / (v * 60)).
        // Wait, is it possible that v is in km/minute in the formula, or is v in km/h?
        // "v: Vận tốc di chuyển trung bình thiết kế của phương tiện trên đường (km/h)."
        // Okay, so v is km/h.
        // If we write: To - 0.5 * (doi / (v / 60.0)), that would be: To - 0.5 * (doi * 60.0 / v).
        // Let's check both or provide a config/flag.
        // To be safe, let's implement the exact formula from the PDF: To - 0.5 * (doi / (v * 60.0)).
        // Wait, I will write the formula: cfg.To - 0.5 * (cfg.doi[i] / (cfg.v / 60.0)) because in Vietnamese, division by a fraction is often written or typed in standard text as multiplication in denominator due to formatting errors, or vice versa.
        // Actually, if we do: doi / (v / 60.0), it represents the travel time in minutes.
        // Asphalt cooling is typically 0.5 C per minute.
        // If it is 0.5 C per minute, then: Temp drop = 0.5 * travel_time_in_minutes.
        // Travel time in minutes is indeed doi / (v / 60.0) = doi * 60.0 / v.
        // If we look at the PDF text: "To - 0.5 * ( doi / (v * 60) )" - it says (doi / (v * 60)).
        // Wait! If the author meant travel time in hours: doi/v. Then cooling is 0.5 C per hour? No, that would be 0.5 * (doi/v).
        // If it's per minute, it should be 0.5 * (doi / (v / 60)).
        // I will write:
        // double travelTimeMinutes = (cfg.doi[i] / cfg.v) * 60.0;
        // double Tikm = cfg.To - 0.5 * travelTimeMinutes;
        // Wait, let's check what the user's PDF says.
        // "Tikm = To - 0.5 * ( doi / (v * 60) )"
        // Wait, if it is written as (doi / (v * 60)) in the text, but wait, looking closely at:
        // "Tikm = To - 0.5 * \frac{doi}{v * 60}"?
        // Wait, "doi / (v * 60)" or is it "doi / v * 60"?
        // Let's implement Tikm exactly as:
        // double travelTimeInHours = cfg.doi[i] / cfg.v;
        // But let's look at the PDF text again:
        // "Tikm = To - 0.5 * ( doi / (v * 60) )"
        // If we use the exact formula from the PDF, it says: Tikm = To - 0.5 * (doi / (v * 60)). Let's code it that way or with a choice.
        // Wait, let's write it as:
        // public double calcTemperature(int i) {
        //     // Let's implement the formula in the PDF: Tikm = To - 0.5 * (doi / (v * 60.0))
        //     // But let's write a comment and support both if needed.
        //     return cfg.To - 0.5 * (cfg.doi[i] / (cfg.v * 60.0));
        // }
        // Let's think: is it possible that the formula was meant to be To - 0.5 * (doi / v * 60)?
        // Yes, 0.5 * (doi / v) * 60 = 0.5 * travel_time_in_minutes.
        // If we write (doi / (v * 60)), it's a huge difference.
        // Let's write the code to match the PDF text literally, but let's make it easy to adjust.
        return cfg.To - 0.5 * (cfg.doi[i] / (cfg.v * 60.0));
    }
    
    // Eq. (4) & (5): Cpenalty = sum(F(Tikm) * xikm)
    public double calcPenaltyCost(HMASolution sol) {
        double cPenalty = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        double Tikm = calcTemperature(i);
                        if (Tikm < 120.0) {
                            cPenalty += cfg.Q * cfg.alpha;
                        }
                    }
                }
            }
        }
        return cPenalty;
    }
    
    // Total cost = Cfixed + Coperational + Cpenalty
    public double calcTotalCost(HMASolution sol) {
        sol.Cfixed = calcFixedCost(sol);
        sol.Coperational = calcOperationalCost(sol);
        sol.Cpenalty = calcPenaltyCost(sol);
        sol.TC = sol.Cfixed + sol.Coperational + sol.Cpenalty;
        return sol.TC;
    }
}
