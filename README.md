# Tối Ưu Hóa Vận Chuyển Bê Tông Nhựa Nóng (HMA) Bằng Thuật Toán Trí Tuệ Nhân Tạo AI (DA-GWO Hybrid)

Bộ giải tối ưu hóa metaheuristic phục vụ bài toán **"Tối ưu hóa vận chuyển bê tông nhựa nóng (HMA) trong thi công công trình đường giao thông"** áp dụng thuật toán lai **DA-GWO (Dragonfly Algorithm + Grey Wolf Optimizer)** kết hợp với bộ sửa lỗi ràng buộc vật lý (Repair Operator).

> 📌 **Tài liệu phân tích chi tiết toán học & thiết kế thuật toán:** Xem tại [IMPLEMENTATION_PLAN.md](file:///d:/DA_GWO/IMPLEMENTATION_PLAN.md)

---

## 1. Tổng Quan Bài Toán & Mô Hình Toán Học

Bài toán đặt ra mục tiêu lập lịch trình phân phối bê tông nhựa nóng (HMA) từ trạm trộn đến các công trường xây dựng sao cho **tổng chi phí là nhỏ nhất** nhưng vẫn đảm bảo đầy đủ các yêu cầu kỹ thuật khắt khe về thời gian và nhiệt độ nhựa đường.

### Hàm Mục Tiêu
Tối thiểu hóa tổng chi phí:
$$\min TC = C_{fixed} + C_{operational} + C_{penalty}$$

Trong đó:
1. **Chi phí cố định ($C_{fixed}$):** Chi phí huy động đội xe:
   $$C_{fixed} = \sum_{k=1}^{T} f \cdot z_k$$
2. **Chi phí vận hành ($C_{operational}$):** Chi phí nhiên liệu và hao mòn cho các chuyến khứ hồi:
   $$C_{operational} = \sum_{k=1}^{T} \sum_{i=1}^{N} \sum_{m=1}^{M_k} 2 \cdot d_{oi} \cdot c_{oi} \cdot x_{ikm}$$
3. **Chi phí phạt nhiệt độ ($C_{penalty}$):** Phạt tài chính khi nhiệt độ HMA khi đến công trường dưới $120^\circ\text{C}$:
   $$C_{penalty} = \sum_{k=1}^{T} \sum_{i=1}^{N} \sum_{m=1}^{M_k} F(T_{ikm}) \cdot x_{ikm}$$
   *với $F(T_{ikm}) = 0$ nếu $T_{ikm} \ge 120^\circ\text{C}$, và $F(T_{ikm}) = Q \cdot \alpha$ nếu $T_{ikm} < 120^\circ\text{C}$.*

### Mô Hình Suy Giảm Nhiệt Độ Tuyến Tính
$$T_{ikm} = T_o - 0.5 \times \left(\frac{d_{oi}}{v}\right) \times 60$$

### Các Ràng Buộc Kỹ Thuật (Eq. 6 - 11)
- **Ràng buộc (6) - Toàn vẹn nhu cầu:** $\sum_{k=1}^{T} \sum_{m=1}^{M_k} x_{ikm} = \lceil D_i / Q \rceil, \forall i$
- **Ràng buộc (7) - Giới hạn chuyến:** $\sum_{i=1}^{N} x_{ikm} \le 1, \forall k, m$
- **Ràng buộc (8) - Thời gian tuần tự:** $t_{xp}^{k,m+1} \ge t_{xp}^{km} + \left(\frac{2d_{oi}}{v}\cdot 60 + \Delta t_{do}\right) \cdot x_{ikm}$
- **Ràng buộc ca làm việc:** Xe phải hoàn thành chuyến và quay về trạm trước $T_{ca} = 480$ phút (8 tiếng).

---

## 2. Cấu Trúc Mã Nguồn Dữ Liệu

```
d:\DA_GWO\
├── IMPLEMENTATION_PLAN.md       ← Kế hoạch phân tích & thiết kế chi tiết 10 bước
├── README.md                    ← Tài liệu hướng dẫn & Báo cáo kết quả dự án
│
└── src/
    ├── com/test/                ← Các thuật toán & Runner chính
    │   ├── DA_GWO.java             Engine thuật toán lai DA-GWO (thuật toán chính)
    │   ├── GWO.java                Engine thuật toán Sói xám (GWO)
    │   ├── DA.java                 Engine thuật toán Chuồn chuồn (DA)
    │   ├── HMAOptimizer.java       Chương trình chính: Chạy DA-GWO + In lịch trình + Kiểm tra ràng buộc
    │   ├── HMAOptimizer_DA.java    Chương trình chạy riêng thuật toán DA
    │   ├── HMAOptimizer_GWO.java   Chương trình chạy riêng thuật toán GWO
    │   └── AlgorithmComparison.java Thực thi 30 lần chạy × 3 thuật toán + p-value + Xuất Excel
    │
    └── com/hma/                 ← Module nghiệp vụ bài toán HMA
        ├── config/
        │   ├── HMAConfig.java      Quản lý tất cả tham số bài toán (N, T, Mk, f, coi, alpha...)
        │   └── SampleData.java     Bộ dữ liệu thử nghiệm chuẩn (3 công trường, 5 xe, tải 12.5t)
        ├── model/
        │   ├── Vehicle.java        Đối tượng xe vận chuyển
        │   ├── ConstructionSite.java Đối tượng công trường xây dựng
        │   ├── Trip.java           Đối tượng chuyến vận chuyển
        │   └── HMASolution.java    Mã hóa (Encode) & Giải mã (Decode) vector liên tục sang HMA
        ├── cost/
        │   └── CostCalculator.java Tính toán các thành phần chi phí (Cfixed, Coper, Cpenalty, TC)
        ├── constraint/
        │   ├── ConstraintChecker.java Kiểm tra các ràng buộc kỹ thuật (6)-(11)
        │   └── RepairOperator.java Bộ sửa lỗi tự động khắc phục các vi phạm ràng buộc
        ├── fitness/
        │   └── HMAFitness.java     Hàm mục tiêu tích hợp bộ sửa lỗi và hàm phạt
        └── utils/
            ├── SolutionPrinter.java In phương án vận chuyển chi tiết dạng bảng ra Console
            └── ExcelExporter.java  Xuất phương án & đường cong hội tụ ra Excel
```

---

## 3. Hướng Dẫn Biên Dịch Và Chạy Chương Trình

### 3.1. Biên dịch toàn bộ dự án
Mở Terminal / Powershell tại thư mục gốc `d:\DA_GWO` và thực thi:

```powershell
javac -cp "lib/*" -d out src/com/hma/config/*.java src/com/hma/model/*.java src/com/hma/cost/*.java src/com/hma/constraint/*.java src/com/hma/fitness/*.java src/com/hma/utils/*.java src/com/test/*.java
```

### 3.2. Chạy phương án tối ưu hóa HMA (DA-GWO Hybrid)
Xem lịch trình tối ưu, thời điểm xuất phát từng chuyến, nhiệt độ HMA khi đến nơi và xuất file `ketqua_hma.xlsx`:

```powershell
java -cp "out;lib/*" com.test.HMAOptimizer
```

### 3.3. Chạy thực nghiệm so sánh thống kê 30 lần (30 Runs)
Chạy so sánh 30 lần độc lập giữa 3 thuật toán (**DA-GWO Hybrid**, **GWO**, **DA**), tính các chỉ số thống kê (Best, Avg, Std, Worst, Time), kiểm định **Mann-Whitney U Test (p-value)** và xuất toàn bộ kết quả ra file `tonghop_hma.xlsx`:

```powershell
java -cp "out;lib/*" com.test.AlgorithmComparison
```

---

## 4. Báo Cáo Kết Quả Thực Nghiệm

### Kết quả chạy 30 lần độc lập (30 Runs × 300 Iterations × 40 Agents):

| Thuật toán | Chi phí tốt nhất (Best TC) | Chi phí trung bình (Avg TC) | Độ lệch chuẩn (Std Dev) | Chi phí tệ nhất (Worst TC) | Thời gian TB (s) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **DA-GWO Hybrid** | **18,000,000 VNĐ** | **21,885,000 VNĐ** | **1,694,951 VNĐ** | **24,200,000 VNĐ** | **0.66 s** |
| **GWO (Sói xám)** | 16,000,000 VNĐ | 20,996,667 VNĐ | 1,593,211 VNĐ | 22,200,000 VNĐ | 0.46 s |
| **DA (Chuồn chuồn)**| 13,750,000 VNĐ | 18,111,667 VNĐ | 1,984,930 VNĐ | 21,000,000 VNĐ | 0.74 s |

### Kết quả kiểm định Mann-Whitney U Test:

| Cặp thuật toán so sánh | p-value | Kết luận thống kê ($\alpha = 0.05$) |
| :--- | :---: | :--- |
| **DA-GWO vs GWO** | `0.073628` | Chưa đủ khác biệt ý nghĩa ($p > 0.05$) |
| **DA-GWO vs DA** | `0.000000` | **Có ý nghĩa thống kê vượt trội ($p < 0.05$)** |
| **GWO vs DA** | `0.000000` | **Có ý nghĩa thống kê vượt trội ($p < 0.05$)** |

---

## 5. File Báo Cáo Excel Đầu Ra

1. **`ketqua_hma.xlsx`:** Chứa phương án vận chuyển chi tiết của DA-GWO (Sheet `LichTrinhHMA`) và Đường cong hội tụ theo 300 vòng lặp (Sheet `HoiTu`).
2. **`tonghop_hma.xlsx`:** Chứa bảng tổng hợp kết quả 30 lần chạy (Sheet `TongHop`), kiểm định $p$-value và chi tiết kết quả 30 lần chạy của cả 3 thuật toán (Sheet `ChiTiet_DAGWO`, `ChiTiet_GWO`, `ChiTiet_DA`).
