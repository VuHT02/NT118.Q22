<p align="center">
  <a href="https://www.uit.edu.vn/" title="Trường Đại học Công nghệ Thông tin" style="border: none;">
    <img src="https://i.imgur.com/WmMnSRt.png" alt="Trường Đại học Công nghệ Thông tin | University of Information Technology">
  </a>
</p>

<h1 align="center"><b>PHÁT TRIỂN ỨNG DỤNG TRÊN THIẾT BỊ DI ĐỘNG - NT118</b></h1>

## GIỚI THIỆU MÔN HỌC

-    **Tên môn học:** Phát triển ứng dụng trên thiết bị di động
-    **Mã môn học:** NT118
-    **Mã lớp:** NT118.Q22
-    **Năm học:** HK2 (2025 - 2026)
-    **Giảng viên:** Ths.Trần Hồng Nghi

## GIỚI THIỆU ĐỒ ÁN

-    **Đề tài:** CityMove - Hệ thống Vé điện tử & Dẫn đường Giao thông công cộng
-    **Mô tả:** Ứng dụng hỗ trợ người dân sử dụng phương tiện công cộng (Metro, Bus) thuận tiện hơn thông qua mô hình MaaS (Mobility as a Service). Giải quyết bài toán xếp hàng mua vé thủ công bằng vé điện tử (QR Code), tích hợp tìm kiếm lộ trình đa phương thức và theo dõi vị trí tàu/xe theo thời gian thực.

## CHỨC NĂNG CHÍNH

### 1. Dành cho Hành khách (Passenger)
- **Quản lý tài khoản:** Đăng ký/Đăng nhập, liên kết ví điện tử.
- **Tìm kiếm lộ trình (AI Routing):**
    - Tìm đường từ điểm A đến điểm B.
    - Gợi ý lộ trình kết hợp (Ví dụ: Đi bộ -> Xe buýt -> Metro).
    - So sánh lộ trình: Rẻ nhất, Nhanh nhất, Ít đi bộ nhất.
- **Đặt vé & Thanh toán:**
    - Tính toán giá vé tự động theo khoảng cách/chặng.
    - Hỗ trợ vé lượt, vé ngày, vé tháng và vé liên vận (Combo).
    - Thanh toán qua ví nội bộ hoặc cổng thanh toán online.
- **Vé điện tử (E-Ticket):**
    - Xuất mã QR động (thay đổi theo thời gian để bảo mật) để qua cổng soát vé.
    - Chế độ Offline (xem vé khi không có mạng).
- **Theo dõi thời gian thực (Real-time):**
    - Xem vị trí xe Bus/Tàu trên bản đồ.
    - Dự kiến thời gian đến (ETA).
- **Thông báo thông minh:** Cảnh báo khi sắp đến trạm xuống, thông báo sự cố/tắc đường.
- **Lịch sử di chuyển:** Xem lại các chuyến đi và chi tiêu.

### 2. Dành cho Kiểm soát viên/Tài xế (Driver/Inspector)
- **Soát vé:** Quét mã QR của hành khách để kiểm tra tính hợp lệ (Check-in/Check-out).
- **Quản lý chuyến:** Báo cáo số lượng khách trên xe/tàu.

### 3. Chức năng Quản trị (Admin System)
- Quản lý danh sách Ga tàu, Trạm xe buýt.
- Quản lý Tuyến đường và Lịch trình chạy.
- Thống kê doanh thu và lưu lượng hành khách.

## CÔNG NGHỆ SỬ DỤNG

-    **Backend:** [ASP.NET](https://dotnet.microsoft.com/en-us/apps/aspnet)
-    **AI Service:** [FastAPI](https://fastapi.tiangolo.com/) - Gợi ý lộ trình thông minh & Dự đoán thời gian đến.
-    **Frontend:** [Kotlin](https://kotlinlang.org/) (Android Native), [Google Maps SDK/Mapbox](https://developers.google.com/maps), [ZXing](https://github.com/zxing/zxing) (QR Code).
-    **Database:** [PostgreSQL](https://www.postgresql.org/) (Sử dụng PostGIS cho dữ liệu bản đồ).
-    **Realtime:** [SignalR](https://dotnet.microsoft.com/en-us/apps/aspnet/signalr) (Cập nhật vị trí xe).

## THÀNH VIÊN NHÓM

| STT | MSSV     | Họ và Tên            | GitHub                              | Email                   |
| :-- | :------- | :------------------- | :---------------------------------- | :---------------------- |
| 1   | 23521816 | Thái Văn Vũ          | https://github.com/VuHT02        | 23521816@gm.uit.edu.vn  |
| 2   | 23520090 | Phạm Bá Bằng         | https://github.com/Bang3107       | 23520090@gm.uit.edu.vn  |
| 3   | 23520535 | Huỳnh Trần Anh Thư   | https://github.com/Thuhuynhtran05  | 23520535@gm.uit.edu.vn  |

## GIAO DIỆN
