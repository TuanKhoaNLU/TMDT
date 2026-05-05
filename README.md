# TMDT_CK

- Backend: Spring Boot (REST API)
- Frontend: HTML + CSS + JavaScrip

## 1) Yeu cau moi truong

- Java 17 tro len
- Maven 3.9 tro len

Kiem tra nhanh:

```bash
java -version
mvn -version
```

## 2) Chay project

Tu thu muc goc project:

```bash
cd D:\TMDT\TMDT_CK
mvn spring-boot:run
```

Neu chay thanh cong, man hinh se hien thi Spring Boot start o port `8080`.

## 3) Truy cap ung dung

Mo trinh duyet:

- Trang chinh: http://localhost:8080/

## 4) Danh sach trang giao dien

- `/` - Card Discovery
- `/login.html` - Login
- `/customize.html` - Customize Order
- `/checkout.html` - Checkout Information
- `/order-history.html` - Order History
- `/order-confirmation.html` - Order Confirmation
- `/profile-settings.html` - Profile Settings
- `/progress-check.html` - Progress Check
- `/wishlist.html` - Wishlist

## 5) API co ban (de test)

- `GET /api/products`
- `GET /api/orders`

Co the test Postman:

- http://localhost:8080/api/products
- http://localhost:8080/api/orders

## 6) Build file jar

```bash
mvn clean package
java -jar target/TMDT_CK-0.0.1-SNAPSHOT.jar
```

