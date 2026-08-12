# OWLEXA — Hệ thống quản lý trung tâm tiếng Anh VSTEP

## Roadmap 100 ngày đầu tiên

**Solo Developer · Java Spring Boot · React TypeScript · Android Kotlin**

3–4 giờ/ngày · ~350 giờ tổng · Mục tiêu: Deploy thật + Tốt nghiệp

2025 – 2026

---

## 1. Tổng quan dự án

| Mục | Chi tiết |
| --- | --- |
| **Tên hệ thống** | Owlexa |
| **Loại sản phẩm** | SaaS — Hệ thống quản lý trung tâm tiếng Anh (tập trung VSTEP) |
| **Mô hình kiến trúc** | Multi-tenant — 1 backend, mỗi trung tâm có subdomain riêng |
| **Ví dụ tên miền** | vinhphuc-english.owlexa.vn │ trungtam-abc.owlexa.vn |
| **Thời gian phát triển** | 100 ngày đầu (trong lộ trình ~1,5 năm đến tốt nghiệp) |
| **Nhân sự** | Solo Developer — 3 đến 4 giờ/ngày |
| **Mục tiêu kép** | Đồ án tốt nghiệp + Sản phẩm thực tế triển khai cho trung tâm thật |

### Công nghệ sử dụng

| Lớp | Công nghệ |
| --- | --- |
| **Backend** | Java Spring Boot 3 · Spring Security · JPA/Hibernate · MySQL |
| **Frontend** | React TypeScript · Vite · Tailwind CSS · React Router v6 · Axios |
| **Mobile** | Android Kotlin · Retrofit · MVVM Architecture |
| **AI** | Google Gemini API — tự động chấm bài Essay/Letter VSTEP |
| **Thanh toán** | Sepay Webhook — xác thực chuyển khoản ngân hàng online |
| **Lưu trữ media** | Cloudinary — ảnh đại diện, tài liệu học tập, video |
| **Bảo mật** | JWT · RBAC (5 roles: ADMIN, OWNER, TEACHER, STUDENT, CASHIER) · Flat Role Design |
| **Server** | VPS Ubuntu 22.04 · Nginx (reverse proxy + wildcard SSL) · Vercel (FE) |

---

## 2. Chiến lược GitHub

### Cấu trúc repositories

| Repository | Mô tả |
| --- | --- |
| **owlexa-backend** | Java Spring Boot — toàn bộ REST API, business logic, database |
| **owlexa-frontend** | React TypeScript — Web admin, teacher dashboard, student portal |
| **owlexa-android** | Kotlin Android — Student mobile app |

### Quy tắc nhánh (Branch Strategy)

Áp dụng cho cả 3 repositories:

| Nhánh | Mục đích | Quy tắc |
| --- | --- | --- |
| **main** | Production (bảo vệ) | Chỉ merge khi release — không push trực tiếp |
| **develop** | Integration (tích hợp) | Luôn chạy được — merge feature xong vào đây |
| **feature/tên-feature** | Phát triển tính năng | Tạo từ develop, merge về develop khi xong |
| **hotfix/tên-lỗi** | Sửa lỗi khẩn cấp | Tạo từ main, merge về cả main và develop |

**Ví dụ tên nhánh feature:**

- `feature/jwt-auth` — xây dựng hệ thống xác thực
- `feature/class-management` — quản lý lớp học
- `feature/sepay-webhook` — tích hợp thanh toán
- `feature/gemini-essay-grading` — AI chấm bài tự luận
- `feature/student-portal` — cổng thông tin học sinh

---

## 3. Tổng hợp 5 Phase — 100 ngày

| Phase | Tên | Thời gian |
| --- | --- | --- |
| **Phase 1** | Nền tảng & Setup | Ngày 1–15 |
| **Phase 2** | Backend — Nghiệp vụ cốt lõi | Ngày 16–50 |
| **Phase 3** | Frontend React TypeScript | Ngày 51–75 |
| **Phase 4** | AI Essay + Thi thử | Ngày 76–90 |
| **Phase 5** | Android + VPS Deploy | Ngày 91–100 |

**Nguyên tắc phát triển:**

- Luôn hoàn thiện Backend API trước → test bằng Postman → sau đó mới làm Frontend
- Mỗi tính năng xong thì commit và push lên develop ngay — không để code chỉ ở máy local
- Ưu tiên core flow chạy được end-to-end, không cần hoàn thiện 100% giao diện ngay
- Viết README cho từng repo để giáo viên hướng dẫn có thể theo dõi tiến độ

---

### Phase 1 — Nền tảng & Setup | Ngày 1–15 │ ~45–60 giờ

#### Ngày 1–3: Khởi động dự án

- Tạo 3 repository GitHub: `owlexa-backend`, `owlexa-frontend`, `owlexa-android`
- Tạo nhánh `develop` từ `main` trong từng repo
- Viết README tổng quan: mô tả hệ thống, tech stack, hướng dẫn chạy local
- Cài đặt môi trường dev: JDK 17+, Node.js 20+, Android Studio, VS Code
- Cài extension hỗ trợ: Spring Boot Tools, ESLint, Prettier, Kotlin

#### Ngày 4–6: Thiết kế database (ERD) — ★ Quan trọng nhất

- Thiết kế toàn bộ bảng dữ liệu trên dbdiagram.io hoặc draw.io
- Các bảng chính: `centers`, `users`, `roles`, `teacher_center` (many-to-many), `class_enrollments`, `classes`, `schedules`, `attendances`, `fee_records`, `payments`, `documents`, `essays`, `essay_criterias`, `mock_tests`, `questions`, `test_results`, `notifications`
- Thêm cột `center_id` vào các bảng nghiệp vụ — nền tảng kiến trúc multi-tenant. Ngoại lệ: bảng `users` không bắt buộc `center_id` (STUDENT tự do và ADMIN không thuộc trung tâm nào)
- Thiết kế index phù hợp: `center_id + entity_id` là composite index chính
- Export ERD thành file ảnh, lưu vào thư mục `docs/` trong `owlexa-backend`

#### Ngày 7–15: Backend core — Authentication & RBAC

- Khởi tạo Spring Boot project (Spring Initializr): Spring Web, Spring Security, Spring Data JPA, MySQL Driver, Lombok, Validation
- Cấu trúc package: `controller / service / repository / entity / dto / config / security / exception`
- Tạo các Entity cơ bản: `Center`, `User`, `Role`, `Permission` với JPA annotations
- Implement JWT: tạo token khi login, validate token ở mỗi request, refresh token
- RBAC flat 5 role — mỗi user có đúng 1 role, phân quyền kiểm tra trong code:
  - **ADMIN**: 1 tài khoản duy nhất, tạo thẳng trong DB, toàn quyền hệ thống, không thể tự đăng ký
  - **OWNER**: tự đăng ký tự do bằng phone, sau khi đăng nhập mới tạo được center (1 owner → nhiều center)
  - **TEACHER**: do OWNER đăng ký cho từng center, không tự đăng ký, có thể thuộc nhiều center, lương là trường riêng chỉ OWNER mới set/get
  - **STUDENT**: tự đăng ký bằng phone (không bắt buộc thuộc center) hoặc được center đăng ký hàng loạt
  - **CASHIER**: do OWNER đăng ký cho từng center, quản lý thu học phí trực tiếp
- Middleware `TenantFilter`: đọc subdomain từ HTTP header `X-Tenant-ID` → inject `center_id` vào ThreadLocal context
- API Auth — các luồng đăng ký/đăng nhập:
  - `POST /auth/register/owner` — OWNER tự đăng ký (phone + password)
  - `POST /auth/register/student` — STUDENT tự đăng ký (phone + password, center_id null)
  - `POST /auth/login` — đăng nhập tất cả role, trả về JWT + role
  - `POST /auth/refresh-token` — làm mới access token
  - `POST /centers/{centerId}/teachers` — OWNER tạo 1 Teacher cho center
  - `POST /centers/{centerId}/teachers/bulk` — OWNER tạo nhiều Teacher cùng lúc, trả về danh sách phone + password
  - `POST /centers/{centerId}/students/bulk` — OWNER/center đăng ký hàng loạt Student, trả về phone + password tương ứng
  - `POST /centers/{centerId}/cashiers` — OWNER tạo Cashier cho center
- Test toàn bộ với Postman Collection ★ Test kỹ

**Tech:** Spring Boot | Spring Security | JWT | MySQL | GitHub | Postman

---

### Phase 2 — Backend — Nghiệp vụ cốt lõi | Ngày 16–50 │ ~105–140 giờ

#### Ngày 16–23: Quản lý người dùng

- **API Teacher**: OWNER tạo từng người hoặc hàng loạt (bulk), sửa/xóa, xem hồ sơ, trạng thái công tác. Trường `salary` chỉ OWNER được set/get, mặc định null khi tạo. Teacher có thể thuộc nhiều center (many-to-many qua `teacher_center`)
- **API Student**: 2 loại student — (1) tự đăng ký bằng phone, `center_id` null; (2) được center đăng ký hàng loạt, trả về phone+password. Nếu student đã có tài khoản muốn vào center thì OWNER thêm thẳng vào lớp. API cập nhật thông tin, lịch sử học, lịch sử học phí
- **API Cashier**: OWNER đăng ký từng Cashier cho center (đăng ký đơn, không cần hàng loạt), sửa/xóa, phân công center. Cashier thuộc đúng 1 center
- Upload và lưu ảnh đại diện lên Cloudinary, lưu URL vào database
- API tìm kiếm và lọc: tìm học sinh theo tên/lớp/trạng thái học phí

#### Ngày 24–32: Quản lý lớp học & lịch dạy

- API CRUD Class: tạo lớp, đặt tên, cấp độ VSTEP, sĩ số tối đa, học phí/tháng
- API CRUD Schedule: phân ca học (sáng/chiều/tối), phòng học, giáo viên phụ trách
- API xem lịch theo nhiều góc độ: lịch của 1 giáo viên / lịch của 1 lớp / lịch của 1 học sinh
- API điểm danh: teacher ghi nhận từng học sinh có mặt/vắng/phép trong từng buổi
- API báo cáo chuyên cần: tỉ lệ đi học theo tháng, danh sách vắng nhiều

#### Ngày 33–42: Học phí & Thanh toán

- Bảng `fee_records`: ghi nhận học phí tháng, hạn đóng, số tiền, trạng thái
- API tự động tạo hóa đơn học phí đầu mỗi tháng cho toàn bộ học sinh đang học
- Tích hợp Sepay webhook: nhận event thanh toán → xác thực chữ ký → cập nhật trạng thái ★ Quan trọng
- API thu học phí: 2 luồng — (1) CASHIER ghi nhận tiền mặt trực tiếp tại quầy; (2) STUDENT đóng online qua Sepay QR trên hệ thống. Cả 2 luồng đều cập nhật trạng thái `fee_record`
- API lịch sử thanh toán: xem theo học sinh / theo lớp / theo tháng / theo trung tâm
- API cảnh báo: danh sách học sinh chưa đóng học phí sắp đến hạn

#### Ngày 43–50: Tài liệu, thông báo & thống kê

- API upload tài liệu học (PDF, video) lên Cloudinary, phân quyền xem theo lớp
- API CRUD Notification: thông báo nghỉ học, thay đổi lịch, nhắc học phí
- API dashboard Owner: tổng doanh thu tháng, số học sinh mới, tỉ lệ đóng học phí
- API báo cáo kết quả học tập: điểm thi thử, tỉ lệ chuyên cần theo lớp

**Tech:** Spring Boot | Cloudinary | Sepay Webhook | MySQL | Postman

---

### Phase 3 — Frontend React TypeScript | Ngày 51–75 │ ~75–100 giờ

#### Ngày 51–55: Setup & Layout nền

- Khởi tạo dự án: `npm create vite@latest owlexa-frontend -- --template react-ts`
- Cài dependencies: Tailwind CSS, React Router v6, Axios, React Query (hoặc Zustand cho state)
- Cấu hình Axios instance: base URL, JWT interceptor, auto refresh token khi 401
- Layout chính: Sidebar thay đổi theo role (OWNER / TEACHER / STUDENT / CASHIER). ADMIN dùng giao diện riêng để xem thống kê hệ thống
- Trang Login / Register / Quên mật khẩu — gọi API Auth đã có

#### Ngày 56–63: Dashboard chủ trung tâm (OWNER)

- Trang tổng quan: thẻ thống kê doanh thu tháng, số học sinh, số lớp đang chạy
- Quản lý giáo viên: bảng danh sách, form thêm/sửa, upload ảnh, phân công lớp
- Quản lý học sinh: tìm kiếm, lọc theo lớp, xem hồ sơ, xem lịch sử học phí
- Quản lý lớp học: tạo lớp, xem danh sách học sinh, xem lịch học
- Báo cáo học phí: bảng ai đóng rồi / ai chưa, nút xuất danh sách

#### Ngày 64–69: Dashboard giáo viên (TEACHER)

- Lịch dạy cá nhân: hiển thị theo tuần và tháng, màu sắc theo lớp
- Điểm danh: chọn ngày/buổi → click từng học sinh có mặt / vắng / phép
- Danh sách học sinh trong lớp: thông tin cơ bản, tỉ lệ chuyên cần
- Cài đặt tiêu chí chấm Essay: form nhập rubric chi tiết để AI dùng chấm bài ★ AI Feature

#### Ngày 70–75: Cổng thông tin học sinh (STUDENT)

- Xem lịch học cá nhân theo tuần, hiển thị phòng học và giáo viên
- Đóng học phí online: xem hóa đơn còn nợ, nhấn thanh toán → Sepay QR
- Thư viện tài liệu: xem và tải tài liệu được phân quyền theo lớp học
- Nộp bài Essay: nhập/paste bài viết, chọn rubric của giáo viên, nhấn Chấm bài → xem kết quả AI

**Tech:** React TypeScript | Vite | Tailwind CSS | React Router | Axios | React Query

---

### Phase 4 — AI Chấm Essay + Hệ thống thi thử | Ngày 76–90 │ ~45–60 giờ

#### Ngày 76–82: Tích hợp AI chấm bài tự luận (Tính năng đặc trưng) ★ Core Feature

- Giáo viên soạn rubric chấm chi tiết: cấu trúc bài, từ vựng, ngữ pháp, tính mạch lạc, độ dài, format
- Backend: tích hợp Google Gemini API — gửi (bài essay + rubric chi tiết) → nhận kết quả chấm
- Thiết kế prompt engineering kỹ lưỡng: yêu cầu Gemini chấm theo từng tiêu chí, cho điểm và nhận xét cụ thể
- API `POST /essays/submit` → trigger async grading → lưu kết quả vào database
- Frontend: giao diện nộp bài đơn giản, loading spinner khi AI đang chấm
- Trang kết quả: điểm tổng, điểm từng tiêu chí dạng thanh progress, nhận xét của AI theo đoạn
- Giáo viên xem lại tất cả bài đã chấm của học sinh trong lớp, có thể thêm nhận xét thủ công

#### Ngày 83–90: Hệ thống thi thử VSTEP

- Bộ đề thi: CRUD tạo đề cho trung tâm — câu hỏi MCQ, listening, reading comprehension
- Phân loại câu hỏi theo kỹ năng (Listening / Reading / Writing) và cấp độ (B1 / B2)
- Giao diện thi: timer đếm ngược, lưu nháp tự động, submit khi hết giờ hoặc chủ động nộp
- Chấm tự động phần trắc nghiệm (MCQ), hiển thị điểm và đáp án đúng ngay sau khi nộp
- Lịch sử thi: học sinh xem lại tất cả các lần thi, biểu đồ tiến bộ theo thời gian

**Tech:** Google Gemini API | Spring Boot | React TypeScript | MySQL

---

### Phase 5 — Android + VPS Deploy | Ngày 91–100 │ ~30–40 giờ

#### Ngày 91–95: Android Kotlin — Student App

- Setup project: MVVM architecture, ViewBinding, Retrofit2, Coroutines
- Màn hình đăng nhập: nhập email/password → gọi API login → lưu JWT vào SharedPreferences
- Màn hình chính: lịch học tuần này, thông báo mới nhất từ trung tâm
- Màn hình kết quả: xem điểm thi thử, xem kết quả essay đã chấm
- Retrofit sử dụng lại toàn bộ API đã có từ Phase 2 — không cần viết thêm backend

#### Ngày 96–100: Deploy VPS & Hoàn thiện ★ VPS Setup

- Thuê VPS: DigitalOcean hoặc Vultr — $6/tháng, Ubuntu 22.04 LTS, 1GB RAM
- Cài MySQL on Linux, tạo database production, import schema từ dev
- Build Spring Boot thành file JAR, copy lên VPS, chạy bằng systemd service
- Cài Nginx: cấu hình reverse proxy port 8080 → 443, wildcard SSL certificate cho `*.owlexa.vn` ★ Quan trọng
- Cấu hình Nginx đọc subdomain → pass header `X-Tenant-ID` cho Spring Boot
- Deploy Frontend lên Vercel: push `owlexa-frontend` → Vercel tự build và deploy miễn phí
- Test end-to-end toàn bộ luồng: đăng ký trung tâm → tạo lớp → học sinh đóng học phí → AI chấm essay
- Kết nối trung tâm tiếng Anh quen biết để dùng thử, thu thập feedback đầu tiên

**Tech:** Android Kotlin | Retrofit | Ubuntu Server | Nginx | SSL/HTTPS | Vercel

---

## 4. Danh sách tính năng đầy đủ

### Dành cho chủ trung tâm (OWNER)

- Quản lý giáo viên: đăng ký từng người hoặc hàng loạt, hồ sơ, lương (chỉ OWNER xem/sửa), phân công lớp, trạng thái công tác
- Quản lý học sinh: đăng ký hàng loạt (trả phone+password), xếp lớp, hồ sơ, lịch sử học. Student đã có tài khoản tự đăng ký được thêm thẳng vào lớp
- Quản lý lớp học: tạo lớp, cấp độ VSTEP, sĩ số, học phí/tháng
- Quản lý thời khóa biểu: phòng học, ca học, phân công giáo viên
- Quản lý học phí: tạo hóa đơn, theo dõi đóng tiền, xem nợ học phí
- Thanh toán online: xác thực qua Sepay — học sinh chuyển khoản tự động ghi nhận
- Thanh toán trực tiếp: thu ngân ghi nhận tiền mặt tại quầy
- Thư viện tài liệu: upload PDF, video, phân quyền theo lớp
- Bộ đề thi thử VSTEP: tạo và quản lý ngân hàng câu hỏi
- Báo cáo thống kê: doanh thu, học sinh mới, tỉ lệ đóng học phí, chuyên cần
- Quản lý nhân viên thu ngân: OWNER đăng ký Cashier cho từng center, phân quyền CASHIER
- Thông báo toàn trung tâm: nghỉ lễ, thay đổi lịch học, thông báo quan trọng

### Dành cho giáo viên (TEACHER)

- Xem lịch dạy cá nhân theo tuần/tháng
- Điểm danh học sinh từng buổi học
- Xem danh sách học sinh trong lớp, hồ sơ tóm tắt, tỉ lệ chuyên cần
- Soạn rubric chấm essay chi tiết — AI sẽ dùng tiêu chí này để chấm
- Xem và bình luận thêm kết quả essay mà AI đã chấm của học sinh
- Upload tài liệu học cho lớp phụ trách
- Xem kết quả thi thử của học sinh, thống kê điểm theo lớp

### Dành cho học sinh (STUDENT) — Ưu tiên cao nhất

- Xem lịch học cá nhân, thông tin phòng học và giáo viên
- Đóng học phí online qua Sepay — quét QR, chuyển khoản, tự động xác nhận
- Xem trạng thái học phí: đã đóng, còn nợ, sắp đến hạn
- Nộp bài Essay/Letter → AI Gemini chấm tự động theo rubric của giáo viên ★ Tính năng đặc trưng
- Xem kết quả chấm essay chi tiết: điểm từng tiêu chí, nhận xét cụ thể
- Thi thử VSTEP: chọn đề, làm bài có timer, xem kết quả và đáp án ngay
- Biểu đồ tiến bộ: so sánh điểm thi qua các lần, xu hướng cải thiện
- Thư viện tài liệu học tập: tải PDF, xem video bài giảng
- Xem lịch sử điểm danh cá nhân, tỉ lệ chuyên cần

---

## 5. ERD — Database Schema

```
// =====================
// CORE SYSTEM
// =====================

users [icon: user, color: blue] {
  id long pk
  phone_number string unique
  password_hash string
  full_name string
  avatar_url string
  role string  // ADMIN, OWNER, TEACHER, STUDENT, CASHIER
  is_active boolean
  created_at datetime
  updated_at datetime
}

centers [icon: home, color: purple] {
  id long pk
  owner_user_id long  // FK → users.id
  name string
  subdomain string unique
  address string
  is_active boolean
  created_at datetime
}

memberships [icon: link, color: gray] {
  id long pk
  user_id long        // FK → users.id
  center_id long      // FK → centers.id
  salary decimal      // nullable — chỉ OWNER set/get, dùng cho TEACHER
  joined_at datetime
  joined_by_user_id long  // FK → users.id (ai thêm vào)
  unique(user_id, center_id)
}

// =====================
// CLASS MANAGEMENT
// =====================

classes [icon: book, color: blue] {
  id long pk
  center_id long          // FK → centers.id
  created_by_user_id long // FK → users.id (OWNER)
  name string
  vstep_level string      // B1, B2, C1
  max_students int
  monthly_fee decimal
  is_active boolean
  created_at datetime
}

class_teachers [icon: link, color: orange] {
  class_id long pk          // FK → classes.id
  teacher_user_id long pk   // FK → users.id
  assigned_at datetime
  assigned_by_user_id long  // FK → users.id (OWNER)
}

enrollments [icon: link, color: green] {
  class_id long pk          // FK → classes.id
  student_user_id long pk   // FK → users.id
  status string             // ACTIVE, DROPPED
  enrolled_at datetime
  enrolled_by_user_id long  // FK → users.id (OWNER hoặc null nếu tự enroll)
}

schedules [icon: calendar, color: blue] {
  id long pk
  class_id long   // FK → classes.id
  day_of_week int // 0 = CN, 1 = T2, ..., 6 = T7
  start_time string
  end_time string
  room string
}

attendances [icon: check-square, color: yellow] {
  id long pk
  schedule_id long        // FK → schedules.id
  student_user_id long    // FK → users.id
  session_date date
  status string           // PRESENT, ABSENT, EXCUSED
  noted_by_user_id long   // FK → users.id (TEACHER)
  created_at datetime
}

// =====================
// PAYMENT
// =====================

fee_records [icon: file-text, color: orange] {
  id long pk
  student_user_id long  // FK → users.id
  center_id long        // FK → centers.id
  class_id long         // FK → classes.id
  amount decimal
  month string          // "2026-05"
  due_date date
  status string         // UNPAID, PARTIAL, PAID
  created_at datetime
}

payments [icon: credit-card, color: green] {
  id long pk
  fee_record_id long          // FK → fee_records.id
  collected_by_user_id long   // FK → users.id (CASHIER hoặc null nếu Sepay)
  amount decimal
  method string               // CASH, SEPAY
  sepay_ref string            // nullable — mã giao dịch Sepay
  created_at datetime
}

// =====================
// CONTENT & LEARNING
// =====================

documents [icon: file, color: teal] {
  id long pk
  class_id long             // FK → classes.id
  uploaded_by_user_id long  // FK → users.id (TEACHER/OWNER)
  title string
  file_url string           // Cloudinary URL
  file_type string          // PDF, VIDEO
  created_at datetime
}

essay_criterias [icon: list, color: purple] {
  id long pk
  class_id long           // FK → classes.id
  created_by_user_id long // FK → users.id (TEACHER)
  title string
  description text
  max_score decimal
  created_at datetime
}

essays [icon: edit, color: pink] {
  id long pk
  student_user_id long  // FK → users.id
  criteria_id long      // FK → essay_criterias.id
  content text
  ai_score decimal      // nullable — điền sau khi AI chấm
  ai_feedback text      // nullable
  teacher_note text     // nullable — giáo viên có thể thêm nhận xét thủ công
  status string         // PENDING, GRADED
  submitted_at datetime
  graded_at datetime    // nullable
}

// =====================
// MOCK TEST
// =====================

mock_tests [icon: clipboard, color: red] {
  id long pk
  center_id long          // FK → centers.id
  created_by_user_id long // FK → users.id (OWNER/TEACHER)
  title string
  vstep_level string      // B1, B2, C1
  duration_minutes int
  is_active boolean
  created_at datetime
}

questions [icon: help-circle, color: orange] {
  id long pk
  mock_test_id long   // FK → mock_tests.id
  skill string        // LISTENING, READING, WRITING
  question_text text
  option_a string
  option_b string
  option_c string
  option_d string
  correct_answer string
  order_index int
}

test_results [icon: bar-chart, color: green] {
  id long pk
  mock_test_id long     // FK → mock_tests.id
  student_user_id long  // FK → users.id
  score decimal
  correct_count int
  total_questions int
  started_at datetime
  submitted_at datetime // nullable — null nếu đang thi
}

test_answers [icon: check-square, color: gray] {
  id long pk
  test_result_id long  // FK → test_results.id
  question_id long     // FK → questions.id
  selected_answer string
  is_correct boolean
}

// =====================
// NOTIFICATIONS
// =====================

notifications [icon: bell, color: yellow] {
  id long pk
  center_id long          // FK → centers.id (nullable — null = thông báo hệ thống từ ADMIN)
  created_by_user_id long // FK → users.id
  title string
  body text
  target_role string      // ALL, STUDENT, TEACHER, hoặc null nếu gửi cho 1 user cụ thể
  created_at datetime
}

notification_reads [icon: eye, color: gray] {
  id long pk
  notification_id long  // FK → notifications.id
  user_id long          // FK → users.id
  read_at datetime
}

// =====================
// RELATIONSHIPS
// =====================

// Core
centers.owner_user_id > users.id
memberships.user_id > users.id
memberships.center_id > centers.id
memberships.joined_by_user_id > users.id

// Class
classes.center_id > centers.id
classes.created_by_user_id > users.id

// Teacher assignment
class_teachers.class_id > classes.id
class_teachers.teacher_user_id > users.id
class_teachers.assigned_by_user_id > users.id

// Student enrollment
enrollments.class_id > classes.id
enrollments.student_user_id > users.id
enrollments.enrolled_by_user_id > users.id

// Schedule & Attendance
schedules.class_id > classes.id
attendances.schedule_id > schedules.id
attendances.student_user_id > users.id
attendances.noted_by_user_id > users.id

// Payment
fee_records.student_user_id > users.id
fee_records.center_id > centers.id
fee_records.class_id > classes.id
payments.fee_record_id > fee_records.id
payments.collected_by_user_id > users.id

// Content
documents.class_id > classes.id
documents.uploaded_by_user_id > users.id
essay_criterias.class_id > classes.id
essay_criterias.created_by_user_id > users.id
essays.student_user_id > users.id
essays.criteria_id > essay_criterias.id

// Mock Test
mock_tests.center_id > centers.id
mock_tests.created_by_user_id > users.id
questions.mock_test_id > mock_tests.id
test_results.mock_test_id > mock_tests.id
test_results.student_user_id > users.id
test_answers.test_result_id > test_results.id
test_answers.question_id > questions.id

// Notifications
notifications.center_id > centers.id
notifications.created_by_user_id > users.id
notification_reads.notification_id > notifications.id
notification_reads.user_id > users.id
```

---

## 🦉 OWLEXA — HƯỚNG DẪN MỞ ĐẦU PHIÊN LÀM VIỆC

Bạn là AI engineer pair cho dự án Owlexa.
Toàn bộ roadmap nằm trong file @owlexa_roadmap.md, phía trên phần hướng dẫn này.

---
### BƯỚC 1 — ĐỌC CODEBASE

Trước khi nói bất cứ điều gì, hãy đọc:
- Cấu trúc thư mục owlexa-backend (src/main/java/...)
- Các entity đã có
- Các controller, service, repository đã có
- Các tính năng đã được implement và tính năng nào còn trống

Đừng dựa vào roadmap để đoán tiến độ — hãy đọc code thật.

### BƯỚC 2 — BÁO CÁO TIẾN ĐỘ

Sau khi đọc xong, trình bày ngắn gọn:
- Đã làm xong: liệt kê các tính năng có code thật
- Chưa làm: liệt kê những gì còn thiếu theo roadmap
- Đề xuất: tính năng nên làm tiếp theo và lý do phụ thuộc

### BƯỚC 3 — BẮT ĐẦU TÍNH NĂNG TIẾP THEO

Triển khai tính năng theo đúng thứ tự lớp:
Entity → Repository → Request DTO → Response DTO → Service → Controller

---
## CÁCH VIẾT CODE TRONG MỖI BƯỚC

### Viết code hoàn chỉnh, không viết placeholder
Mỗi file phải chạy được thật sự, không có TODO hay ellipsis.
Không viết "// ... rest of the code" hay để trống logic.

### Giải thích WHY tại mỗi quyết định thiết kế
Không giải thích HOW — code đã nói HOW rồi.
Chỉ giải thích những chỗ không hiển nhiên, ví dụ:
  - Tại sao dùng @Builder.Default
  - Tại sao không delete cứng mà dùng status DROPPED
  - Tại sao batch tốt hơn từng request riêng lẻ
  - Tại sao check tenant trước khi check business rule

### Thứ tự xử lý trong service
Với mọi method trong service, luôn theo thứ tự:
1. Lấy current user và centerId
2. Kiểm tra quyền (role + membership)
3. Load entity chính, throw nếu không tìm thấy
4. Kiểm tra tenant (entity có thuộc center này không)
5. Kiểm tra business rule (class đầy, student đã enroll rồi...)
6. Thực hiện thao tác chính
7. Save và return response

Không bao giờ đảo thứ tự này.

### Kết thúc mỗi bước
Luôn kết thúc bằng:
- Danh sách API sau bước này (method + path + body mẫu)
- Những câu hỏi edge case nên tự hỏi
- "Bước tiếp theo nên làm là X vì lý do Y"

---
## QUY TẮC BẮT BUỘC

### Bỏ qua test
Không viết unit test, integration test hay Postman test script
trừ khi tôi yêu cầu rõ ràng trong tin nhắn.

### Không tự sửa file trong codebase
Không tạo file, không sửa file trực tiếp trong project của tôi
trừ khi tôi nói rõ "bạn được phép sửa file X" trong tin nhắn đó.
Chỉ đưa code để tôi tự copy vào đúng file.

### Đi đúng thứ tự phụ thuộc nghiệp vụ
Không nhảy cóc tính năng. Thứ tự chuẩn:
  Enrollment → Schedule → Attendance → FeeRecord → Payment → Sepay Webhook
  → Essay/Grading → MockTest → Notification → Dashboard
Tính năng sau phụ thuộc vào tính năng trước.
Nếu tính năng trước chưa hoàn chỉnh, chỉ ra điểm thiếu trước khi đi tiếp.

### Khi tôi gõ "ok" hoặc "qua bước tiếp theo"
Không hỏi lại, không xác nhận.
Đi ngay sang tính năng tiếp theo theo đúng thứ tự phụ thuộc.

---
## THÔNG TIN NGỮ CẢNH

- Solo developer, 3-4 giờ/ngày
- Mục tiêu kép: đồ án tốt nghiệp + sản phẩm thật
- Tech stack: Java Spring Boot 3, Spring Security, JPA/Hibernate, MySQL
- Multi-tenant: mỗi request mang X-Tenant-ID header → inject centerId vào TenantFilter
- RBAC 5 role: ADMIN, OWNER, TEACHER, STUDENT, CASHIER
- Sau mỗi session, tôi commit lên GitHub develop branch

Bây giờ bắt đầu từ Bước 1.
---


# Chế độ hướng dẫn học tập (Learning Mode)

Tôi muốn học và hiểu cách làm, không muốn bạn trở thành người lập trình thay tôi.

Khi phân tích code, thiết kế tính năng hoặc đề xuất cải tiến, hãy tuân thủ các quy tắc sau:

## Nguyên tắc quan trọng

* Không tự động sửa code trực tiếp.
* Không refactor toàn bộ project.
* Không tạo pull request hoặc đưa ra hàng loạt thay đổi lớn một lúc.
* Không trả về phiên bản hoàn chỉnh của nhiều file để tôi copy-paste.
* Không thực hiện thay tôi toàn bộ công việc.

Mục tiêu là giúp tôi hiểu tư duy thiết kế và tự viết được code.

---

## Cách trả lời mong muốn

Khi phát hiện vấn đề:

1. Giải thích:

   * Vấn đề nằm ở đâu.
   * Vì sao nó là vấn đề.
   * Hậu quả có thể xảy ra.

2. Hướng dẫn cách sửa:

   * Nên sửa ở lớp nào (Controller, Service, Repository, Entity, Config,...).
   * Nên thay đổi ý tưởng gì.
   * Luồng xử lý sau khi sửa sẽ như thế nào.

3. Chỉ cung cấp:

   * Đoạn code nhỏ minh họa nếu cần.
   * Pseudocode.
   * Sơ đồ luồng xử lý.
   * Ví dụ tối thiểu.

4. Sau đó để tôi tự code.

---

## Nếu tôi muốn tự luyện tập

Hãy trả lời theo cấu trúc:

### Bài toán

Mô tả ngắn gọn vấn đề.

### Gợi ý 1

Gợi ý ở mức kiến trúc.

### Gợi ý 2

Gợi ý ở mức class hoặc method.

### Gợi ý 3

Gợi ý ở mức logic.

### Đáp án tham khảo (ẩn)

Chỉ cung cấp khi tôi yêu cầu rõ:
"Cho tôi xem code mẫu"
hoặc
"Cho tôi đáp án".

---

## Khi review code

Ưu tiên:

* Giải thích WHY hơn HOW.
* Giải thích tư duy của senior developer.
* Chỉ ra trade-off giữa các phương án.
* Đưa ra các câu hỏi để tôi tự suy nghĩ trước.

Ví dụ:

Thay vì:

"Đây là code hoàn chỉnh, copy vào PaymentService."

Hãy:

"Bạn cần kiểm tra quyền trước khi tạo Payment. Theo bạn nên đặt logic này ở Controller hay Service? Hãy thử suy nghĩ về lý do trước khi xem gợi ý."

---

## Mức độ hỗ trợ mặc định

Mặc định chỉ hỗ trợ tối đa 30% lời giải.

70% còn lại để tôi tự thực hiện.

Chỉ khi tôi nói rõ:

"Cho tôi code hoàn chỉnh"

thì mới cung cấp full code.


*Owlexa © 2025 — Tài liệu nội bộ*