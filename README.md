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

Co the test bang trinh duyet hoac Postman:

- http://localhost:8080/api/products
- http://localhost:8080/api/orders

## 6) Build file jar

```bash
mvn clean package
java -jar target/TMDT_CK-0.0.1-SNAPSHOT.jar
```

## 7) Loi thuong gap

- Loi `JAVA_HOME`:
  - Kiem tra bien moi truong Java da tro dung JDK.
- Loi port `8080` dang duoc su dung:
  - Doi port trong file `src/main/resources/application.properties` (vi du `server.port=8081`).
- Maven khong tai duoc dependency:
  - Kiem tra ket noi internet/proxy va chay lai `mvn -U clean package`.

## 8) Dinh huong mo rong tiep

- Chuyen du lieu in-memory sang MySQL/PostgreSQL
- Mapping day du theo `E-commerce db.pdf`
- Them dang nhap/phan quyen (buyer, artisan, admin)
- Them gio hang, thanh toan, ton kho, custom request workflow

## 9) Quy uoc UI de tai su dung

Toan bo mau sac va kich thuoc giao dien da duoc dinh nghia trong `src/main/resources/static/styles.css` bang CSS variables (`:root`).

### Design tokens (hang so)
- Mau: `--color-*` (vi du `--color-action-primary`, `--color-bg-surface`)
- Chu: `--font-*`
- Khoang cach: `--space-*`
- Bo goc / bong: `--radius-*`, `--shadow-*`
- Kich thuoc component: `--nav-height`, `--card-image-height`, `--sidebar-width`

### Component classes dung lai
- Nut: `.btn`, `.btn.ghost`
- The san pham: `.card`, `.card-body`, `.ph`
- Khung noi dung: `.panel`
- Dieu huong: `.site-header`, `.topbar`, `.main-nav`, `.utility-nav`
- Form: `input`, `textarea`

Khi them man hinh moi, uu tien dung lai token + class co san thay vi hardcode mau/kich thuoc.
