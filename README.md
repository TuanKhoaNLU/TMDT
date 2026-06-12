# TMDT Handmade Marketplace

Spring Boot + React/Vite marketplace demo cho san pham handmade. Project co buyer, seller, admin, gio hang theo user, checkout nhieu shop, tinh phi GHN, thanh toan VNPay, quan ly order theo tung shop package.

## Tech Stack

- Backend: Spring Boot 3.3, Java 17, JDBC, MySQL, Redis cache
- Frontend: React 18, Vite, React Query, Axios
- Database: MySQL database `e_commerce`
- Shipping: GHN API cho tinh/thanh, quan/huyen, phuong/xa va phi van chuyen
- Payment: VNPay sandbox hoac mock local

## Requirements

- Java 17+
- Node.js 18+
- MySQL 8+ hoac XAMPP MySQL
- Redis 6+ khuyen nghi, app van co memory fallback cho cart neu Redis khong san sang
- Git

Kiem tra nhanh:

```bash
java -version
node -v
npm -v
```

Windows co the dung Maven wrapper:

```powershell
.\mvnw.cmd -version
```

macOS/Linux:

```bash
./mvnw -version
```

## Setup Local

1. Clone repo va vao thu muc project:

```bash
git clone <repo-url>
cd TMDT
```

2. Tao file cau hinh local:

```bash
cp .env.example .env
```

Tren PowerShell:

```powershell
Copy-Item .env.example .env
```

3. Sua `.env` theo may local:

```properties
MYSQL_URL=jdbc:mysql://localhost:3306/e_commerce?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USER=root
MYSQL_PASSWORD=
REDIS_HOST=localhost
REDIS_PORT=6379
```

Neu dung XAMPP, bat MySQL trong XAMPP Control Panel. User mac dinh thuong la `root`, password de trong.

4. Cau hinh GHN:

```properties
GHN_TOKEN=...
GHN_SHOP_ID=...
GHN_BASE_URL=https://online-gateway.ghn.vn/shiip/public-api
GHN_FROM_DISTRICT_ID=1442
GHN_FROM_WARD_CODE=20101
```

GHN dang duoc goi that cho tinh/thanh, quan/huyen, phuong/xa va phi ship. Neu thieu `GHN_TOKEN` hoac `GHN_SHOP_ID`, man checkout/tinh phi ship se tra 503.

5. Cau hinh VNPay:

```properties
VNPAY_MOCK_ENABLED=false
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment-result
```

Muon test nhanh khong qua VNPay sandbox thi co the dat:

```properties
VNPAY_MOCK_ENABLED=true
```

Khong nen bat mock khi demo luong thanh toan that.

## Run Project

Chay backend tu thu muc goc.

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Backend chay o:

```text
http://localhost:8080
```

Chay frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend chay o:

```text
http://localhost:5173
```

Vite da proxy `/api` sang backend `http://localhost:8080`, nen nen mo web bang `localhost:5173`.

## Database

Khi backend start, `DatabaseInitializer` tu tao/cap nhat schema can thiet neu thieu:

- `Users`, `Accounts`, `Shops`, `Categories`, `Products`, `ProductImages`, `Storage`
- `Orders`, `shop_orders`, `OrderItems`
- `payment_transactions`, `Payments`
- `shipments`, `shop_shipping_profiles`

App cung seed data demo gom buyer, seller, admin, shop va san pham.

Neu can reset data local:

```sql
DROP DATABASE e_commerce;
CREATE DATABASE e_commerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau do start backend lai.

## Test Accounts

| Role | Username | Password | Ghi chu |
| --- | --- | --- | --- |
| Buyer | `buyer` | `buyer` | Mua hang, checkout, xem/huy don |
| Seller | `seller` | `seller` | Quan ly shop orders cua shop minh |
| Admin | `admin` | `admin` | Xem va quan ly tat ca orders |

Trang test nhanh:

- Buyer: `http://localhost:5173/products`, `/cart`, `/checkout`, `/orders`
- Seller: `http://localhost:5173/seller/orders`
- Admin: `http://localhost:5173/admin/orders`

## Main Flows

Buyer:

1. Login bang `buyer/buyer`.
2. Vao `/products`, them san pham vao gio.
3. Vao `/cart`, kiem tra gio hang group theo shop.
4. Checkout tai `/checkout`, dia chi lay tu GHN, phi ship tinh bang GHN.
5. Chon COD hoac VNPay.
6. Xem lich su don tai `/orders`.

Order nhieu shop:

- Mot checkout co the tao 1 order tong va nhieu `shop_orders`.
- Huy toan bo don chi nen dung khi muon huy ca order tong.
- Neu don co nhieu shop, vao chi tiet order de huy tung goi shop bang nut `Huy goi nay`.
- Package da `PACKING`, `SHIPPING`, `COMPLETED` khong cho buyer tu huy.

Seller:

1. Login bang `seller/seller`.
2. Vao `/seller/orders`.
3. Chuyen trang thai theo flow: `NEW -> CONFIRMED -> PACKING -> SHIPPING -> COMPLETED`.
4. Seller chi thay shop do tai khoan minh quan ly.

Admin:

1. Login bang `admin/admin`.
2. Vao `/admin/orders`.
3. Xem order tong, shop order, payment, shipment.

## Important API

Auth:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`

Products/cart:

- `GET /api/v1/products`
- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{itemKey}`
- `DELETE /api/v1/cart/items/{itemKey}`
- `DELETE /api/v1/cart`

Checkout/payment/shipping:

- `GET /api/v1/checkout/summary`
- `POST /api/v1/orders/checkout`
- `POST /api/v1/payments/vnpay/create`
- `GET /api/v1/payments/vnpay-return`
- `GET /api/v1/shipping/provinces`
- `GET /api/v1/shipping/districts?provinceId=...`
- `GET /api/v1/shipping/wards?districtId=...`
- `POST /api/v1/shipping/fee`

Orders:

- `GET /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `PUT /api/v1/orders/{orderId}/cancel`
- `PUT /api/v1/orders/{orderId}/shop-orders/{shopOrderId}/cancel`
- `GET /api/v1/seller/orders`
- `PUT /api/v1/seller/orders/{shopOrderId}/status`
- `POST /api/v1/seller/orders/{shopOrderId}/shipments/ghn`
- `GET /api/v1/admin/orders`
- `PUT /api/v1/admin/orders/{orderId}/cancel`

## Build

Backend:

```bash
./mvnw clean package
java -jar target/TMDT_CK-0.0.1-SNAPSHOT.jar
```

Windows:

```powershell
.\mvnw.cmd clean package
java -jar target\TMDT_CK-0.0.1-SNAPSHOT.jar
```

Frontend:

```bash
cd frontend
npm run build
npm run preview
```

## Before Pushing To GitHub

Khong commit `.env` vi co token GHN/VNPay/MySQL. Repo da ignore `.env`, chi commit `.env.example`.

Nen chay truoc:

```bash
git status --short
./mvnw -q -DskipTests compile
cd frontend && npm run build
```

Windows:

```powershell
git status --short
.\mvnw.cmd -q -DskipTests compile
cd frontend
npm run build
```
