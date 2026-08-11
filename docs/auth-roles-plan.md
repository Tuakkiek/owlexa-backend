# Auth & Roles Plan

## 1. Kết luận ngắn gọn

Hướng thay đổi này là hợp lý, nhưng có một điểm cần chốt rõ ngay từ đầu:

- Có thể giữ `flat roles` ở mức `User.role` để hệ thống dễ hiểu ở phase 1.
- Không nên dùng `role` một mình để biểu diễn toàn bộ nghiệp vụ.
- Các quan hệ theo trung tâm phải tách riêng bằng bảng membership/association.

Lý do:

- `OWNER` có thể sở hữu nhiều trung tâm.
- `TEACHER` có thể thuộc nhiều trung tâm.
- `CASHIER` cũng nên thiết kế để có thể thuộc nhiều trung tâm.
- `STUDENT` có thể đăng ký tự do trước, sau đó mới tham gia trung tâm và lớp.
- `salary` của `TEACHER` là dữ liệu theo từng trung tâm, không phải dữ liệu toàn cục của user.

Kết luận thực tế:

- Giữ `1 user = 1 global role duy nhất`.
- Nhưng phân quyền thực tế phải dựa trên `global role + center membership + class enrollment`.

## 2. Quyết định nghiệp vụ đã chốt

### Global roles

Mỗi user chỉ có đúng 1 role cố định:

- `ADMIN`
- `OWNER`
- `TEACHER`
- `STUDENT`
- `CASHIER`

Không cho đổi role tự động trong các flow nghiệp vụ thông thường.

Nếu `phoneNumber` đã tồn tại nhưng đang là role khác với role cần tạo:

- Trả lỗi.
- Không tự convert role.

### Định danh đăng nhập

- Phase 1 dùng `phoneNumber` làm định danh chính.
- `phoneNumber` phải unique toàn hệ thống.
- Đăng ký bằng email để sau mở rộng, chưa cần là flow chính.

### OWNER

- `OWNER` được tự đăng ký.
- Sau khi đăng nhập, `OWNER` mới được tạo center.
- Một `OWNER` có thể tạo và quản lý nhiều center.

### ADMIN

- `ADMIN` không có API đăng ký.
- Phase local: tạo một account bình thường trước, sau đó update role bằng SQL.
- Về sau khi deploy có thể chuyển sang seed/migration riêng.

### TEACHER

- `TEACHER` không được tự đăng ký.
- Chỉ `OWNER` mới được tạo `TEACHER`.
- Hỗ trợ tạo từng người hoặc tạo hàng loạt.
- `TEACHER` có thể thuộc nhiều center.
- `salary` được lưu theo từng center.
- `salary` có thể `null`.
- Chỉ `OWNER` của center đó mới được set/get `salary`.

### STUDENT

- `STUDENT` được tự đăng ký bằng `phoneNumber`.
- Ngoài ra `OWNER` cũng có thể tạo hàng loạt student bằng danh sách số điện thoại.
- Khi tạo hàng loạt, response nên trả về danh sách `phoneNumber` và `temporaryPassword`.
- `STUDENT` không bắt buộc phải thuộc center ngay sau khi đăng ký.
- Sau này student tự do có thể thi thử, làm bài tập chung của hệ thống, nhưng phần này chưa ưu tiên triển khai ngay.

### CASHIER

- `CASHIER` không được tự đăng ký.
- `OWNER` tạo `CASHIER`.
- Phase 1 chỉ cần UI/API tạo từng người là đủ.
- Nhưng data model nên cho phép `CASHIER` thuộc nhiều center để khỏi phải sửa schema sau này.

## 3. Luồng đăng ký và tham gia lớp

## 3.1 Student tự đăng ký

- Student tự đăng ký bằng `phoneNumber`.
- Hệ thống tạo account với `role = STUDENT`.
- Student có thể tồn tại mà chưa thuộc center nào.

## 3.2 Owner tạo student hàng loạt

- Input là danh sách student cần tạo.
- Mỗi record tối thiểu có `phoneNumber`, có thể kèm `fullName`.
- Hệ thống sinh `temporaryPassword` cho từng account mới.
- Response trả về danh sách:
  - `phoneNumber`
  - `temporaryPassword`
  - trạng thái tạo mới hay đã tồn tại

Nếu số điện thoại đã tồn tại:

- Nếu role hiện tại là `STUDENT`: không tạo account mới, có thể trả trạng thái `ALREADY_EXISTS`.
- Nếu role hiện tại khác `STUDENT`: trả lỗi nghiệp vụ.

## 3.3 Owner tạo teacher hàng loạt

- Flow tương tự student bulk create.
- Nếu account đã tồn tại và role là `TEACHER` thì chỉ cần add vào center nếu chưa có membership.
- Nếu account đã tồn tại nhưng role khác `TEACHER` thì trả lỗi.

## 3.4 Student chọn khóa/lớp và thanh toán

Nghiệp vụ cuối cùng đang hợp lý nhất nếu hiểu theo cách sau:

- Trong một center có nhiều `Course` hoặc `Program`.
- Mỗi `Course` có nhiều `Class`.
- Mỗi `Class` có giáo viên riêng, lịch học riêng, số lượng chỗ riêng.
- Student xem danh sách khóa/lớp đang mở hoặc sắp mở.
- Student thấy được:
  - thông tin khóa
  - lớp cụ thể
  - giáo viên phụ trách
  - mô tả giáo viên
  - link mạng xã hội của giáo viên
  - trạng thái còn chỗ / đủ học viên / sắp khai giảng

Flow đề xuất:

1. Student chọn một `Class` cụ thể nằm trong một `Course`.
2. Student thanh toán online cho lớp đó.
3. Khi thanh toán thành công, hệ thống tự động:
   - tạo membership với center nếu student chưa thuộc center đó
   - tạo enrollment vào class
   - gắn lịch học tương ứng cho student

Điểm quan trọng:

- Dù màn hình có thể bắt đầu từ `Course`, giao dịch thanh toán cuối cùng nên gắn với `Class` cụ thể.
- Vì sau khi thanh toán xong bạn muốn add ngay student vào lớp với đúng giáo viên đó.

## 3.5 Thanh toán sai

Nếu student thanh toán nhầm:

- Không cho tự hoàn tác online ở phase 1.
- Student phải gặp trực tiếp `CASHIER`.
- `CASHIER` xử lý theo nghiệp vụ nội bộ.

Vì vậy nên có các trạng thái giao dịch tối thiểu:

- `PENDING`
- `PAID`
- `CANCELLED`
- `REFUND_REQUESTED`
- `REFUNDED_MANUAL`

## 4. Mô hình dữ liệu đề xuất

## 4.1 User

`User` là danh tính toàn cục của hệ thống.

Gợi ý field chính:

- `id`
- `phone_number` unique
- `email` nullable
- `full_name`
- `password`
- `role`
- `status`
- `created_at`

Lưu ý:

- `role` là role toàn cục.
- Không lưu `salary`, `centerId`, `classId` trực tiếp ở bảng `users`.

## 4.2 Center

- `id`
- `name`
- `subdomain`
- `owner_user_id`
- `status`
- `created_at`

Một `OWNER` có thể sở hữu nhiều center.

## 4.3 CenterMembership

Nên có bảng membership riêng cho quan hệ user với center.

Ví dụ:

- `id`
- `user_id`
- `center_id`
- `joined_by_user_id`
- `status`
- `joined_at`

Ý nghĩa:

- Cho biết user có thuộc center hay không.
- Dùng chung cho `OWNER`, `TEACHER`, `STUDENT`, `CASHIER`.
- Là nền để check quyền theo trung tâm.

Lưu ý:

- Với `flat roles`, bảng membership không cần lưu thêm `role` ở phase 1.
- Role lấy từ `users.role`.
- Nếu sau này cần role theo center thì mới mở rộng tiếp.

## 4.4 TeacherCenterProfile

Vì `salary` là dữ liệu riêng theo center, nên không nên nhét vào `users`.

Đề xuất bảng:

- `id`
- `teacher_user_id`
- `center_id`
- `salary` nullable
- `currency`
- `created_at`
- `updated_at`

Unique:

- unique (`teacher_user_id`, `center_id`)

## 4.5 Course

Đây là cấp khóa học/chương trình.

Ví dụ:

- `VSTEP cho người mới bắt đầu`
- `TOEIC nền tảng`
- `TOEIC 800+`

Field gợi ý:

- `id`
- `center_id`
- `name`
- `description`
- `status`

## 4.6 Class

Đây là lớp học cụ thể để student đăng ký.

Field gợi ý:

- `id`
- `center_id`
- `course_id`
- `teacher_user_id`
- `name`
- `start_date`
- `end_date`
- `capacity`
- `enrolled_count`
- `status`

`status` có thể gồm:

- `DRAFT`
- `OPEN_FOR_ENROLLMENT`
- `FULL`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

## 4.7 TeacherPublicProfile

Vì student cần xem thông tin giáo viên trước khi đăng ký, nên nên tách dữ liệu public ra rõ ràng.

Gợi ý:

- `user_id`
- `bio`
- `facebook_url`
- `tiktok_url`
- `youtube_url`
- `linkedin_url`
- `avatar_url`

Đây là profile public, khác với dữ liệu nhạy cảm như `salary`.

## 4.8 Payment

Thanh toán nên gắn với class mà student đã chọn.

Field gợi ý:

- `id`
- `student_user_id`
- `center_id`
- `course_id`
- `class_id`
- `amount`
- `payment_method`
- `provider`
- `provider_transaction_id`
- `status`
- `paid_at`
- `created_at`

`payment_method`:

- `ONLINE_SEPAY`
- `CASH`

## 4.9 ClassEnrollment

Sau khi thanh toán thành công thì tạo enrollment.

Field gợi ý:

- `id`
- `student_user_id`
- `class_id`
- `payment_id`
- `status`
- `enrolled_at`

Unique:

- unique (`student_user_id`, `class_id`)

## 5. Nguyên tắc phân quyền

## 5.1 Mức quyền

Nên tách quyền theo 3 lớp:

1. `Global role`
2. `Center membership`
3. `Resource ownership / class relation`

Không nên viết logic kiểu:

- thấy `role = OWNER` là cho làm mọi thứ ở mọi center

Mà phải là:

- user có role `OWNER`
- và user có quyền trên center đó

## 5.2 Quyền tối thiểu theo role

### ADMIN

- Toàn quyền hệ thống
- Xem thống kê toàn hệ thống
- Quản lý dữ liệu cấp hệ thống

### OWNER

- Tạo center
- Quản lý center mình sở hữu
- Tạo teacher
- Tạo cashier
- Tạo student hàng loạt
- Set/get lương teacher trong center của mình
- Mở course, mở class
- Gán teacher vào class

### TEACHER

- Xem class mình dạy
- Xem student trong class mình dạy
- Không được tự set/get `salary` nếu bạn muốn bảo mật tuyệt đối theo owner

### STUDENT

- Tự đăng ký
- Xem course/class mở cho đăng ký
- Xem thông tin giáo viên công khai
- Thanh toán online
- Xem lịch học của mình sau khi enroll

### CASHIER

- Xem và xử lý thanh toán tại center
- Hỗ trợ nghiệp vụ khi student đóng nhầm
- Không nên có quyền quản lý teacher/class ngoài phạm vi tài chính

## 6. Đề xuất API ở phase 1

## 6.1 Auth

- `POST /auth/register/student`
- `POST /auth/register/owner`
- `POST /auth/login`

Không nên giữ một endpoint register chung kiểu truyền `roleName`, vì:

- Dễ hở nghiệp vụ
- Dễ bị tạo nhầm role
- Khó khóa các flow đặc biệt như `TEACHER`, `CASHIER`, `ADMIN`

## 6.2 Owner management

- `POST /owner/centers`
- `GET /owner/centers`
- `POST /owner/teachers`
- `POST /owner/teachers/bulk`
- `POST /owner/students/bulk`
- `POST /owner/cashiers`
- `PUT /owner/teachers/{teacherId}/salary`

## 6.3 Student enrollment

- `GET /centers/{centerId}/courses`
- `GET /centers/{centerId}/classes/open`
- `GET /classes/{classId}`
- `POST /student/payments`
- `POST /payments/sepay/webhook`

Sau khi webhook thanh toán thành công:

- tạo `CenterMembership` nếu chưa có
- tạo `ClassEnrollment`
- cập nhật chỗ còn lại của lớp

## 6.4 Cashier

- `GET /cashier/payments`
- `POST /cashier/payments/{paymentId}/refund-request`
- `POST /cashier/payments/{paymentId}/refund-confirm`

Tên endpoint có thể thay đổi, nhưng tách role-based flow như vậy sẽ dễ hiểu hơn.

## 7. Điểm cần sửa trong code hiện tại

Repo hiện tại đang có một số điểm chưa khớp với hướng mới:

- `Role` mới có `OWNER`, `TEACHER`, `STUDENT`.
- Chưa có `ADMIN`, `CASHIER`.
- `RegisterRequest` đang bắt `email` là bắt buộc.
- `RegisterRequest` đang cho truyền `roleName` trực tiếp.
- `/auth/register` hiện đang là register chung.
- `/admin/**` hiện tại lại đang được dùng cho `OWNER`, tên route này dễ gây nhầm.

Khuyến nghị:

- Giữ `/admin/**` cho `ADMIN` thật sự.
- Tạo namespace riêng như `/owner/**`, `/teacher/**`, `/student/**`, `/cashier/**`.

Đây là chỗ mình nói thẳng:

- Nếu tiếp tục dùng `/admin/**` cho owner, sau này thêm `ADMIN` thật sẽ rất rối cả code lẫn tài liệu.
- Nên đổi từ sớm khi codebase còn nhỏ.

## 8. Lộ trình triển khai đề xuất

## Phase 1: Chốt nền auth + role + membership

- Thêm `ADMIN`, `CASHIER` vào enum role
- Tách register endpoint theo role hợp lệ
- Student chỉ đăng ký bằng `phoneNumber`
- Owner tự đăng ký được
- Teacher/Cashier không tự đăng ký
- Thêm membership theo center

## Phase 2: Owner vận hành nhân sự

- Tạo center
- Tạo teacher từng người
- Tạo teacher hàng loạt
- Tạo student hàng loạt
- Tạo cashier
- Set/get salary theo center

## Phase 3: Course/Class/Teacher public info

- Tạo course
- Tạo class
- Gán teacher vào class
- Public teacher profile
- Student xem class và teacher trước khi đăng ký

## Phase 4: Thanh toán và auto enrollment

- Tích hợp Sepay
- Student thanh toán cho class đã chọn
- Webhook xác nhận thanh toán
- Auto add vào center nếu chưa có membership
- Auto add vào class
- Sinh lịch học

## Phase 5: Nghiệp vụ hoàn tiền/correction

- Cashier xử lý case thanh toán nhầm
- Lưu audit log cho thay đổi thủ công

## Phase 6: Tối ưu vận hành sau này

- Thuật toán tự chọn phòng
- Thuật toán tự chọn giờ
- Xếp lớp thông minh
- Chính sách chờ đủ học viên để mở lớp

## 9. SQL local để đổi một account thành ADMIN

Flow local đơn giản:

1. Tạo account bằng flow bình thường
2. Update role trong DB

SQL mẫu:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE phone_number = '0987654321';
```

Kiểm tra lại:

```sql
SELECT id, phone_number, full_name, role
FROM users
WHERE phone_number = '0987654321';
```

Ghi chú:

- Với Spring Boot mặc định, field `phoneNumber` thường map thành cột `phone_number`.
- Nếu local DB của bạn đang dùng tên cột khác thì chỉnh lại câu SQL cho đúng.

## 10. Quyết định kiến trúc cuối cùng

Đề xuất cuối cùng để vừa đơn giản vừa không cồng kềnh:

- `User.role` vẫn là `flat role`
- `CenterMembership` quản lý quan hệ với center
- `TeacherCenterProfile` giữ dữ liệu riêng theo center như lương
- `Course -> Class -> Payment -> Enrollment` là trục chính cho student

Đây là mức thiết kế đủ rõ để code ngay, nhưng vẫn còn chỗ mở rộng tốt về sau mà chưa cần đem RBAC phức tạp vào quá sớm.
