# owlexa-backend

Java Spring Boot — REST API, business logic, database.

## Chạy local

Backend mặc định chạy tại `http://localhost:8081` và kết nối MySQL database
`owlexa_db`.

```powershell
.\mvnw.cmd spring-boot:run
```

Các giá trị kết nối có thể được ghi đè bằng `DB_URL`, `DB_USERNAME` và
`DB_PASSWORD`.

## Tài khoản Admin được seed

Khi ứng dụng khởi động, hệ thống chỉ tạo Admin nếu database chưa có tài khoản
với role `ADMIN`. Seeder không đổi mật khẩu của Admin đã tồn tại.

- Số điện thoại local: `0900000000`
- Mật khẩu local: `password123`
- Trang frontend: `http://localhost:5173/admin/dashboard`

Trước khi triển khai, bắt buộc đặt `ADMIN_PHONE_NUMBER`, `ADMIN_PASSWORD` và
`JWT_SECRET` bằng biến môi trường. Có thể tắt seeder với
`SEED_ADMIN_ENABLED=false`.

API đầu tiên dành cho Admin:

- `POST /auth/login`
- `POST /auth/logout`
- `GET /admin/stats` (yêu cầu Bearer token có role `ADMIN`)
