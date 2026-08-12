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

API dành cho Admin:

- `POST /auth/login`
- `POST /auth/logout`
- `GET /admin/stats` (yêu cầu Bearer token có role `ADMIN`)
- `GET /admin/users` và `GET /admin/users/{id}`
- `PATCH /admin/users/{id}/status`
- `GET /admin/centers` và `GET /admin/centers/{id}`
- `PATCH /admin/centers/{id}/status`
- `GET /admin/audit-logs`

Tài khoản bị khóa không thể đăng nhập và token đã cấp cũng mất hiệu lực. Tài
khoản role `ADMIN` được bảo vệ, không thể chuyển sang trạng thái khóa. Mọi yêu
cầu đổi trạng thái phải có `reason` từ 3 đến 500 ký tự và thay đổi thực tế sẽ
được lưu vào bảng `admin_audit_logs` trong cùng transaction.
