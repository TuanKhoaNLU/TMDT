# TMDT Handmade Marketplace

Marketplace sản phẩm handmade xây dựng bằng Spring Boot và React/Vite. Hệ thống hỗ trợ khách hàng, người bán, quản trị viên, giỏ hàng đa shop, đơn custom, vận chuyển GHN và thanh toán COD/VNPay.

## Chức năng chính

### Khách hàng

- Đăng ký, xác thực OTP, đăng nhập, đăng xuất và đặt lại mật khẩu.
- Tìm kiếm, lọc giá, sắp xếp và xem chi tiết sản phẩm/shop.
- Quản lý giỏ hàng đa shop, voucher, điểm thưởng và checkout.
- Thanh toán COD hoặc VNPay, theo dõi và hủy đơn hợp lệ.
- Đánh giá sản phẩm sau khi đơn hoàn tất; mỗi sản phẩm chỉ được đánh giá một lần.
- Đăng yêu cầu sản phẩm custom, xem proposal/bản thảo và chấp nhận báo giá.
- Theo dõi đơn custom và gửi yêu cầu chỉnh sửa.
- Gửi yêu cầu hoàn trả/hoàn tiền.

### Người bán

- Đăng ký seller và chờ quản trị viên phê duyệt.
- Chỉnh sửa hồ sơ gian hàng: tên, logo, ảnh bìa, mô tả, vật liệu và kinh nghiệm.
- Tạo/sửa sản phẩm, cập nhật tồn kho và ngưỡng cảnh báo.
- Nhận thông báo yêu cầu custom, gửi báo giá và bản thảo.
- Xử lý revision, đơn thường, vận đơn và hoàn trả.
- Xem giao dịch, doanh thu, phí nền tảng và thực nhận.

### Quản trị viên

- Phê duyệt seller và sản phẩm.
- Quản lý đơn hàng, báo cáo vi phạm và đánh giá.
- Cấu hình phí nền tảng; phí mới được áp dụng cho các checkout tiếp theo.
- Theo dõi dashboard và độ tin cậy thanh toán.

## Công nghệ

- Backend: Java 17+, Spring Boot 3.3, JDBC, MySQL, Redis.
- Frontend: React 18, Vite, React Query, Axios.
- Vận chuyển: GHN API.
- Thanh toán: VNPay sandbox hoặc mock local.

## Yêu cầu môi trường

- Java 17 trở lên.
- Node.js 18 trở lên và npm.
- MySQL 8 trở lên hoặc MySQL trong XAMPP.
- Redis 6 trở lên là tùy chọn; giỏ hàng có memory fallback khi Redis không hoạt động.
- Git.

Kiểm tra phiên bản:

```powershell
java -version
node -v
npm -v
git --version
```

## Cấu hình backend

Tạo file cấu hình local từ file mẫu:

```powershell
cd backend
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cd backend
cp .env.example .env
```

Cấu hình MySQL tối thiểu trong `backend/.env`:

```properties
MYSQL_URL=jdbc:mysql://localhost:3306/e_commerce?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USER=root
MYSQL_PASSWORD=

REDIS_HOST=localhost
REDIS_PORT=6379
```

Nếu dùng XAMPP, bật MySQL trước khi chạy backend. Tài khoản mặc định thường là `root` và mật khẩu trống.

### GHN

Điền thông tin sau để tải tỉnh/huyện/xã và tính phí vận chuyển thật:

```properties
GHN_TOKEN=your_token
GHN_SHOP_ID=your_shop_id
GHN_FROM_DISTRICT_ID=1442
GHN_FROM_WARD_CODE=20101
```

Thiếu token/shop ID có thể khiến các API địa chỉ hoặc phí giao hàng trả lỗi `503`.

### VNPay

Test nhanh bằng mock local:

```properties
VNPAY_MOCK_ENABLED=true
VNPAY_RETURN_URL=http://localhost:5173/payment-result
```

Dùng VNPay sandbox thật:

```properties
VNPAY_MOCK_ENABLED=false
VNPAY_TMN_CODE=your_tmn_code
VNPAY_HASH_SECRET=your_hash_secret
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment-result
```

Không commit `backend/.env`. File này đã nằm trong `.gitignore`; chỉ commit `.env.example`.

## Chạy dự án

### 1. Chạy backend

Mở terminal thứ nhất:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

Lần khởi động đầu, `DatabaseInitializer` tự tạo/cập nhật schema và seed dữ liệu demo trong database `e_commerce`.

### 2. Chạy frontend

Mở terminal thứ hai:

```powershell
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`. Vite proxy các request `/api` sang `http://localhost:8080`.

## Tài khoản demo

| Vai trò | Tên đăng nhập | Mật khẩu |
| --- | --- | --- |
| Khách hàng | `buyer` | `buyer` |
| Người bán shop đầu tiên | `seller` | `seller` |
| Người bán khác | `seller2` đến `seller8` | `seller` |
| Quản trị viên | `admin` | `admin` |

OTP demo cho đăng ký/xác thực là `123456`.

## Kiểm tra tự động và build

### Backend

Windows:

```powershell
cd backend
.\mvnw.cmd test
```

macOS/Linux:

```bash
cd backend
./mvnw test
```

Hiện repository chưa có test class trong `src/test`, nên lệnh trên xác nhận backend compile thành công và sẽ tự chạy test khi test được bổ sung.

Đóng gói file JAR:

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target\TMDT_CK-0.0.1-SNAPSHOT.jar
```

### Frontend

```powershell
cd frontend
npm run build
npm run preview
```

`npm run build` kiểm tra JSX/import và tạo bản production trong `frontend/dist`.

## Checklist kiểm thử thủ công

### Luồng khách hàng

1. Đăng nhập `buyer/buyer` tại `/login`.
2. Tìm kiếm/lọc sản phẩm tại `/`, mở chi tiết và thêm vào giỏ.
3. Cập nhật số lượng hoặc xóa sản phẩm tại `/cart`.
4. Checkout tại `/checkout`, chọn COD hoặc VNPay mock.
5. Xem danh sách/chi tiết đơn tại `/orders`.
6. Chỉ đánh giá sản phẩm sau khi kiện hàng chuyển `COMPLETED`; thử gửi lần hai để kiểm tra chống trùng.
7. Vào `/custom-requests`, đăng yêu cầu custom và chờ seller gửi proposal.
8. Chấp nhận proposal, mở `/custom-orders` và gửi revision khi đơn đang chế tác.
9. Kiểm tra `/notifications` có thông báo báo giá/revision.
10. Test quên mật khẩu tại `/forgot-password` bằng OTP demo.

### Luồng người bán

1. Đăng nhập `seller/seller`, mở `/seller`.
2. Chỉnh hồ sơ tại `/seller/profile`, sau đó mở trang shop công khai để kiểm tra.
3. Tạo/sửa listing và cập nhật nhanh tồn kho tại `/seller/products`.
4. Mở `/custom-requests`, chọn yêu cầu và gửi giá, thời gian, URL bản thảo.
5. Xử lý đơn và revision tại `/seller/custom-orders` hoặc `/custom-orders`.
6. Xử lý đơn thường tại `/seller/orders` theo luồng `NEW -> CONFIRMED -> PACKING -> SHIPPING -> COMPLETED`.
7. Kiểm tra doanh thu/phí sàn tại `/seller/transactions`.

### Luồng quản trị viên

1. Đăng nhập `admin/admin`, mở `/admin`.
2. Duyệt seller trong `/admin/manage`.
3. Duyệt sản phẩm trong `/admin/catalog`.
4. Cập nhật phí nền tảng trong `/admin/manage`.
5. Tạo checkout mới bằng buyer và xác nhận `commissionAmount`/`payoutAmount` thay đổi theo mức phí mới.
6. Kiểm tra đơn toàn hệ thống tại `/admin/orders`.

## Route giao diện quan trọng

| Route | Chức năng |
| --- | --- |
| `/` | Danh sách, tìm kiếm, lọc sản phẩm |
| `/cart`, `/checkout` | Giỏ hàng và đặt hàng |
| `/orders` | Đơn mua |
| `/custom-requests` | Yêu cầu custom, proposal và bản thảo |
| `/custom-orders` | Theo dõi đơn custom/revision |
| `/seller/profile` | Hồ sơ gian hàng |
| `/seller/products` | Listing và tồn kho |
| `/seller/orders` | Đơn hàng của shop |
| `/seller/transactions` | Giao dịch, phí sàn, thực nhận |
| `/admin/manage` | Người dùng, báo cáo, phí nền tảng |

## Xử lý lỗi thường gặp

### Backend không kết nối MySQL

Lỗi thường gặp: `Communications link failure` hoặc `Connection refused`.

- Kiểm tra MySQL/XAMPP đã bật.
- Kiểm tra port MySQL, thường là `3306`.
- Kiểm tra `MYSQL_USER`, `MYSQL_PASSWORD` trong `backend/.env`.
- Kiểm tra không có service khác chiếm port.

Reset database local khi cần dữ liệu sạch:

```sql
DROP DATABASE IF EXISTS e_commerce;
CREATE DATABASE e_commerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó chạy lại backend để tự tạo schema và seed dữ liệu.

### Frontend không gọi được API

- Đảm bảo backend đang chạy port `8080`.
- Mở frontend bằng `http://localhost:5173`, không mở trực tiếp file HTML.
- Kiểm tra terminal backend để xem lỗi database hoặc GHN.

## Trước khi push Git

```powershell
git status --short
cd backend
.\mvnw.cmd test
cd ..\frontend
npm run build
```

Chỉ push khi cả backend và frontend build thành công.
