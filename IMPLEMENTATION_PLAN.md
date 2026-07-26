# Kế Hoạch Phân Tích & Thiết Kế Hệ Thống Tối Ưu Hóa Vận Chuyển HMA (DA-GWO Hybrid)

> [!CAUTION]
> ### Sự khác biệt quan trọng so với bản phân tích trước
> | Điểm | Bản trước (dự đoán) | Tài liệu thực tế |
> |---|---|---|
> | **Mô hình nhiệt độ** | Newton's exponential cooling: `T(t) = T_env + (T_init-T_env)·e^(-kt)` | **Tuyến tính**: `T_ikm = To - 0.5 × (doi / (v×60))` |
> | **Hàm phạt** | Quadratic penalty: `λ × violation²` | **Step function nhị phân**: `F(T) = 0 nếu T≥120°C, Q×α nếu T<120°C` |
> | **Đội xe** | Heterogeneous (mỗi xe capacity khác nhau) | **Homogeneous**: tất cả xe cùng tải trọng Q |
> | **Chi phí vận hành** | `fuel_rate × distance × fuel_price` | `2 × doi × coi × xikm` (đơn giản hơn) |
> | **Chi phí cố định** | `FC[i]` mỗi xe khác nhau | `f × zk` — đơn giá `f` chung cho mọi xe |
> | **Ràng buộc nhu cầu** | `delivered ≥ demand` (bất đẳng thức) | `Σ xikm = ⌈Di/Q⌉` (đẳng thức chính xác) |

---

## Hệ thống Tham số và Biến số (từ tài liệu)

### Tham số đầu vào

| Ký hiệu | Ý nghĩa | Ví dụ |
|---|---|---|
| `N` | Số công trường cần phục vụ | 3 |
| `T` | Số phương tiện trong đội xe | 5 |
| `Mk` | Số chuyến tối đa của xe k trong 1 ca | 6 |
| `f` | Đơn giá chi phí cố định (VNĐ/xe) | 2,000,000 |
| `coi` | Đơn giá vận hành (VNĐ/km) | 15,000 |
| `doi` | Khoảng cách 1 chiều từ trạm trộn đến công trường i (km) | 25 |
| `Q` | Tải trọng định mức (tấn/chuyến) | 12.5 |
| `Di` | Nhu cầu HMA tại công trường i (tấn) | 150 |
| `To` | Nhiệt độ HMA khi rời trạm trộn (°C) | 160 |
| `α` | Hệ số phạt khi nhiệt độ < 120°C (VNĐ/tấn) | 500,000 |
| `v` | Vận tốc trung bình (km/h) | 40 |
| `Δtdo` | Thời gian đổ vật liệu tại hiện trường (phút) | 30 |

### Biến quyết định

| Ký hiệu | Loại | Ý nghĩa |
|---|---|---|
| `zk` | Nhị phân {0,1} | Xe k có được huy động không? |
| `xikm` | Nhị phân {0,1} | Chuyến m của xe k có đến công trường i không? |
| `txp_km` | Liên tục ≥ 0 | Thời điểm chuyến m của xe k rời trạm trộn (phút) |

---

## BƯỚC 1: Phân tích thuật toán DA_GWO

### 1.1. Search Agent là gì?

Trong code, **Search Agent** = 1 cá thể trong quần thể = 1 ứng viên lời giải.

| Thuộc tính | Vị trí trong code | Ý nghĩa hiện tại | **Ý nghĩa trong HMA** |
|---|---|---|---|
| Tổng agents | `SearchAgents_no` | 40 con sói/chuồn chuồn | 40 **phương án vận chuyển** |
| Vị trí agent i | `X[i][j]` | Vector ℝ^dim | Vector mã hóa `zk`, `xikm`, `txp_km` |
| Fitness agent i | `Fitness[i]` | f(x) benchmark | **TC** = C_fixed + C_oper + C_penalty |

### 1.2. Position Vector đang biểu diễn cái gì?

```
X[i] = [z₁..zT | x₁₁₁..xNTM | txp₁₁..txpTM]  (mã hóa phương án vận chuyển HMA)
```

---

## BƯỚC 2: Cấu trúc lớp và mô hình toán HMA

Hàm mục tiêu:
$$\min TC = C_{fixed} + C_{operational} + C_{penalty}$$

- $C_{fixed} = \sum_{k=1}^{T} f \cdot z_k$
- $C_{operational} = \sum_{k=1}^{T} \sum_{i=1}^{N} \sum_{m=1}^{M_k} 2 \cdot d_{oi} \cdot c_{oi} \cdot x_{ikm}$
- $C_{penalty} = \sum_{k=1}^{T} \sum_{i=1}^{N} \sum_{m=1}^{M_k} F(T_{ikm}) \cdot x_{ikm}$ với $F(T_{ikm}) = 0$ nếu $T_{ikm} \ge 120^\circ\text{C}$, ngược lại $Q \cdot \alpha$.

Mô hình suy giảm nhiệt độ:
$$T_{ikm} = T_o - 0.5 \times \left(\frac{d_{oi}}{v}\right) \times 60$$

---

## BƯỚC 3: Thiết kế Encoding & Repair Operator

### Continuous Vector Encoding:
Vector $X \in [0, 1]^{dim}$ với $dim = T + N \cdot T \cdot M_k + T \cdot M_k$.
- **Phần $z_k$**: Ánh xạ $z_k = 1$ nếu $X[k] \ge 0.5$, ngược lại $0$.
- **Phần $x_{ikm}$**: Với mỗi chuyến $m$ của xe $k$, chọn công trường $i = \arg\max_i X[idx]$.
- **Phần $t_{xp}^{km}$**: Ánh xạ $t_{xp}^{km} = X[idx] \cdot T_{ca}$.

### Repair Operator:
- `repairTripLimit()`: Đảm bảo phương trình (7).
- `repairDemand()`: Đảm bảo phương trình (6) (thêm/bớt chuyến để đúng $\lceil D_i/Q \rceil$).
- `repairSequenceAndDepartureTimes()`: Đảm bảo phương trình (8) và ca làm việc $T_{ca}$.
