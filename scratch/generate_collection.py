import json
import uuid

def create_item(name, method, url_path, description="", body=None, auth=None, event=None, headers=None):
    item = {
        "name": name,
        "request": {
            "method": method,
            "header": headers or [],
            "url": {
                "raw": "{{baseUrl}}" + url_path,
                "host": ["{{baseUrl}}"],
                "path": [p for p in url_path.strip("/").split("/") if p]
            }
        },
        "response": []
    }
    if description:
        item["request"]["description"] = description
    if body:
        item["request"]["body"] = {
            "mode": "raw",
            "raw": json.dumps(body, indent=2, ensure_ascii=False),
            "options": {
                "raw": {
                    "language": "json"
                }
            }
        }
    if auth:
        item["request"]["auth"] = auth
    if event:
        item["event"] = event
    return item

def folder(name, items, description=""):
    f = {
        "name": name,
        "item": items
    }
    if description:
        f["description"] = description
    return f

# Predefined variables
auth_login_event = [
    {
        "listen": "test",
        "script": {
            "exec": [
                "var jsonData = pm.response.json();",
                "",
                "pm.test(\"Status 200\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "if (jsonData && jsonData.accessToken) {",
                "    pm.collectionVariables.set(\"accessToken\", jsonData.accessToken);",
                "    console.log(\"[Auth] Saved accessToken\");",
                "}",
                "",
                "if (jsonData && jsonData.refreshToken) {",
                "    pm.collectionVariables.set(\"refreshToken\", jsonData.refreshToken);",
                "    console.log(\"[Auth] Saved refreshToken\");",
                "}",
                "",
                "var sessionId = null;",
                "if (jsonData && jsonData.session) {",
                "    sessionId = jsonData.session.sessionId || jsonData.session.id;",
                "} else if (jsonData && jsonData.sessionId) {",
                "    sessionId = jsonData.sessionId;",
                "}",
                "if (sessionId) {",
                "    pm.collectionVariables.set(\"sessionId\", String(sessionId));",
                "    console.log(\"[Auth] Saved sessionId: \" + sessionId);",
                "}"
            ],
            "type": "text/javascript"
        }
    }
]

device_headers = [
    {"key": "X-Device-Type", "value": "WEB", "description": "WEB | MOBILE | DESKTOP | TABLET"},
    {"key": "X-Device-Info", "value": "Postman", "description": "Optional"}
]

# --- Build Collection ---

items = [
    folder("1. Authentication", [
        create_item("Login", "POST", "/auth/login", "Đăng nhập", {"phoneNumber": "0987654321", "password": "Password123", "deviceType": "WEB"}, auth={"type": "noauth"}, event=auth_login_event, headers=device_headers),
        create_item("Register Owner", "POST", "/auth/register/owner", "Đăng ký Owner", {"phoneNumber": "0911223344", "email": "owner@example.com", "password": "Password123", "fullName": "Nguyen Van Owner", "centerName": "Trung tam Tuyen", "subdomain": "tuyen"}, auth={"type": "noauth"}, event=auth_login_event),
        create_item("Refresh Token", "POST", "/auth/refresh-token", "Làm mới token (Yêu cầu Cookie refreshToken)", auth={"type": "noauth"}, event=auth_login_event),
        create_item("Logout", "POST", "/auth/logout", "Đăng xuất"),
        create_item("Get Sessions", "GET", "/auth/sessions", "Danh sách thiết bị"),
        create_item("Revoke Session", "DELETE", "/auth/sessions/{{sessionId}}", "Xóa 1 phiên"),
        create_item("Revoke All Sessions", "DELETE", "/auth/sessions", "Xóa toàn bộ phiên"),
        create_item("Get My Account", "GET", "/account", "Thông tin tài khoản"),
        create_item("Update My Account", "PUT", "/account", "Cập nhật tài khoản", {"fullName": "New Name", "email": "new@example.com", "avatarUrl": ""}),
        create_item("Change Password", "PATCH", "/account/password", "Đổi mật khẩu", {"oldPassword": "Password123", "newPassword": "NewPassword123"}),
        create_item("Get Public Center", "GET", "/public/centers/me", "Thông tin Center từ subdomain", auth={"type": "noauth"}),
    ], "API xác thực và tài khoản"),
    
    folder("2. Owner - Core", [
        folder("Centers", [
            create_item("Get All Centers", "GET", "/owner/centers"),
            create_item("Create Center", "POST", "/owner/centers", body={"name": "Center mới", "subdomain": "center-moi"}),
            create_item("Get Center", "GET", "/owner/centers/1"),
            create_item("Update Center", "PUT", "/owner/centers/1", body={"name": "Center Update", "subdomain": "center-update"}),
            create_item("Delete Center", "DELETE", "/owner/centers/1"),
        ]),
        folder("Permissions", [
            create_item("List Permissions", "GET", "/owner/permissions"),
            create_item("User Permissions", "GET", "/owner/users/1/permissions"),
            create_item("Bulk Update Overrides", "PUT", "/owner/users/1/permissions", body={"overrides": [{"permissionCode": "CLASS_VIEW", "type": "GRANT"}]}),
            create_item("Single Override", "PATCH", "/owner/users/1/permissions/CLASS_VIEW", body={"type": "DENY"}),
            create_item("Remove Overrides", "DELETE", "/owner/users/1/permissions"),
        ]),
        folder("Teachers", [
            create_item("Get Teachers", "GET", "/owner/teachers"),
            create_item("Create Teacher", "POST", "/owner/teachers", body={"phoneNumber": "0922334455", "fullName": "Teacher A", "password": "Password123"}),
            create_item("Bulk Create Teachers", "POST", "/owner/teachers/bulk", body={"teachers": [{"phoneNumber": "0922334466", "fullName": "Teacher B", "password": "Password123"}]}),
            create_item("Update Teacher", "PUT", "/owner/teachers/1", body={"fullName": "Teacher Update", "email": "teacher@example.com"}),
            create_item("Delete Teacher", "DELETE", "/owner/teachers/1"),
            create_item("Get Salary", "GET", "/owner/teachers/1/salary"),
            create_item("Update Salary", "PUT", "/owner/teachers/1/salary", body={"salaryType": "MONTHLY", "baseSalary": 10000000, "hourlyRate": 0}),
            create_item("Clear Salary", "DELETE", "/owner/teachers/1/salary"),
        ]),
        folder("Students", [
            create_item("Get Students", "GET", "/owner/students"),
            create_item("Create Student", "POST", "/owner/students", body={"phoneNumber": "0933445566", "fullName": "Student A", "password": "Password123"}),
            create_item("Bulk Create Students", "POST", "/owner/students/bulk", body={"students": [{"phoneNumber": "0933445577", "fullName": "Student B", "password": "Password123"}]}),
            create_item("Update Student", "PUT", "/owner/students/1", body={"fullName": "Student Update", "email": "student@example.com"}),
            create_item("Delete Student", "DELETE", "/owner/students/1"),
        ]),
        folder("Rooms", [
            create_item("Get Rooms", "GET", "/owner/rooms"),
            create_item("Create Room", "POST", "/owner/rooms", body={"name": "Phòng 101", "capacity": 30, "isActive": True}),
            create_item("Get Room", "GET", "/owner/rooms/1"),
            create_item("Update Room", "PUT", "/owner/rooms/1", body={"name": "Phòng 101 Update", "capacity": 35, "isActive": True}),
            create_item("Delete Room", "DELETE", "/owner/rooms/1"),
            create_item("Schedule Summary", "GET", "/owner/rooms/1/schedule-summary"),
            create_item("Validate Delete", "GET", "/owner/rooms/1/delete-validation"),
        ]),
        folder("Courses", [
            create_item("Get Courses", "GET", "/owner/courses"),
            create_item("Get All Courses (Incl Inactive)", "GET", "/owner/courses/all"),
            create_item("Create Course", "POST", "/owner/courses", body={"code": "IELTS1", "name": "IELTS 6.0", "description": "Khóa IELTS", "totalSessions": 24, "feeAmount": 5000000, "isActive": True}),
            create_item("Get Course", "GET", "/owner/courses/1"),
            create_item("Update Course", "PUT", "/owner/courses/1", body={"code": "IELTS1", "name": "IELTS 6.0 Update", "totalSessions": 24, "feeAmount": 5500000, "isActive": True}),
            create_item("Delete Course", "DELETE", "/owner/courses/1"),
            create_item("Course Stats", "GET", "/owner/courses/1/statistics"),
            create_item("Course Classes", "GET", "/owner/courses/1/classes"),
            create_item("Validate Delete", "GET", "/owner/courses/1/delete-validation"),
        ]),
    ]),

    folder("3. Owner - Operations", [
        folder("Classes", [
            create_item("Get Classes", "GET", "/owner/classes"),
            create_item("Classes With Students", "GET", "/owner/classes/with-students"),
            create_item("Create Class", "POST", "/owner/classes", body={"courseId": 1, "code": "IELTS-01", "name": "Lớp IELTS 01", "teacherUserId": 2, "startDate": "2026-08-01", "endDate": "2026-12-01", "tuitionFee": 5000000, "capacity": 20}),
            create_item("Get Class", "GET", "/owner/classes/1"),
            create_item("Update Class", "PUT", "/owner/classes/1", body={"courseId": 1, "code": "IELTS-01", "name": "Lớp IELTS 01", "teacherUserId": 2, "tuitionFee": 5000000, "capacity": 20}),
            create_item("Update Status", "PATCH", "/owner/classes/1/status", body="OPENED"),
            create_item("Delete Class", "DELETE", "/owner/classes/1"),
        ]),
        folder("Enrollments", [
            create_item("Get Enrollments", "GET", "/owner/classes/1/enrollments"),
            create_item("Create Enrollment", "POST", "/owner/classes/1/enrollments", body={"studentUserId": 3, "discountAmount": 0, "status": "APPROVED"}),
            create_item("Approve", "PATCH", "/owner/classes/1/enrollments/3/approve"),
            create_item("Reject", "PATCH", "/owner/classes/1/enrollments/3/reject"),
            create_item("Drop", "PATCH", "/owner/classes/1/enrollments/3/drop"),
            create_item("Suspend", "PATCH", "/owner/classes/1/enrollments/3/suspend"),
            create_item("Reactivate", "PATCH", "/owner/classes/1/enrollments/3/reactivate"),
            create_item("Drop with Reason", "POST", "/owner/classes/1/enrollments/3/drop-with-reason", body={"reason": "Nghỉ học", "refundAmount": 0}),
            create_item("Get Dropped", "GET", "/owner/classes/1/enrollments/dropped"),
        ]),
        folder("Time Slots", [
            create_item("Get Time Slots", "GET", "/owner/time-slots"),
            create_item("Get Active Time Slots", "GET", "/owner/time-slots/active"),
            create_item("Create Time Slot", "POST", "/owner/time-slots", body={"name": "Ca 1", "startTime": "08:00", "endTime": "10:00", "isActive": True}),
            create_item("Update Time Slot", "PUT", "/owner/time-slots/1", body={"name": "Ca 1 Update", "startTime": "08:00", "endTime": "10:00", "isActive": True}),
            create_item("Delete Time Slot", "DELETE", "/owner/time-slots/1"),
            create_item("Quick Setup", "POST", "/owner/time-slots/quick-setup", body={"type": "MORNING"}),
        ]),
        folder("Schedules", [
            create_item("Get My Schedules", "GET", "/owner/schedules/me"),
            create_item("Get Class Schedules", "GET", "/owner/classes/1/schedules"),
            create_item("Get Teacher Schedules", "GET", "/owner/classes/1/schedules/teacher/2"),
            create_item("Create Schedule Rule", "POST", "/owner/classes/1/schedule-rules", body={"dayOfWeek": "MONDAY", "timeSlotId": 1, "roomId": 1, "teacherUserId": 2, "startDate": "2026-08-01", "endDate": "2026-12-01"}),
            create_item("Get Schedule Rules", "GET", "/owner/classes/1/schedule-rules"),
            create_item("Generate Events", "POST", "/owner/classes/1/schedule-rules/1/generate"),
            create_item("Create Event", "POST", "/owner/classes/1/schedule-events", body={"date": "2026-08-15", "timeSlotId": 1, "roomId": 1, "teacherUserId": 2, "scheduleRuleId": 1, "type": "REGULAR"}),
            create_item("Get Events", "GET", "/owner/classes/1/schedule-events"),
            create_item("Update Event", "PUT", "/owner/classes/1/schedule-events/1", body={"date": "2026-08-16", "timeSlotId": 1, "roomId": 1, "teacherUserId": 2, "type": "MAKEUP"}),
            create_item("Cancel Event", "PATCH", "/owner/classes/1/schedule-events/1/cancel"),
        ]),
        folder("Attendance", [
            create_item("Get Attendance by Schedule", "GET", "/owner/attendance/schedules/1?date=2026-08-15"),
            create_item("Get Attendance by Class", "GET", "/owner/attendance/classes/1?date=2026-08-15"),
            create_item("Get Attendance Range", "GET", "/owner/attendance/classes/1/range?startDate=2026-08-01&endDate=2026-08-31"),
            create_item("Get Stats", "GET", "/owner/attendance/classes/1/stats?startDate=2026-08-01&endDate=2026-08-31"),
            create_item("Mark Teacher Attendance", "POST", "/owner/teacher-attendance", body={"date": "2026-08-15", "records": [{"teacherUserId": 2, "status": "PRESENT", "note": ""}]}),
            create_item("Get Teacher Attendance", "GET", "/owner/teacher-attendance"),
            create_item("Get Teacher Attendance Details", "GET", "/owner/teacher-attendance/1"),
            create_item("Update Teacher Attendance", "PUT", "/owner/teacher-attendance/1?status=ABSENT&note=Sick"),
            create_item("Delete Teacher Attendance", "DELETE", "/owner/teacher-attendance/1"),
        ]),
        folder("Documents", [
            create_item("Get Documents", "GET", "/owner/classes/1/documents"),
            create_item("Upload Document", "POST", "/owner/classes/1/documents", body={"fileId": 1, "fileName": "Document 1", "fileSize": 1024, "type": "MATERIAL"}),
            create_item("Delete Document", "DELETE", "/owner/classes/1/documents/1"),
        ]),
    ]),
    
    folder("4. Finance (Owner/Cashier)", [
        folder("Cashiers", [
            create_item("Get Cashiers", "GET", "/owner/cashiers"),
            create_item("Create Cashier", "POST", "/owner/cashiers", body={"phoneNumber": "0944556677", "fullName": "Cashier A", "password": "Password123"}),
            create_item("Update Cashier", "PUT", "/owner/cashiers/1", body={"fullName": "Cashier Update", "email": "cashier@example.com"}),
            create_item("Delete Cashier", "DELETE", "/owner/cashiers/1"),
        ]),
        folder("Fee Records", [
            create_item("Get My Fees", "GET", "/fee-records/me"),
            create_item("Overdue Fees", "GET", "/owner/fee-records/overdue"),
            create_item("Pending Fees", "GET", "/owner/fee-records/pending"),
            create_item("Class Fees", "GET", "/owner/classes/1/fee-records"),
            create_item("Update Due Date", "PUT", "/owner/classes/1/fee-records/due-date", body={"dueDate": "2026-09-01"}),
        ]),
        folder("Installments", [
            create_item("Create Installments", "POST", "/owner/fee-record/1/installments", body={"installments": [{"amount": 2500000, "dueDate": "2026-08-15"}, {"amount": 2500000, "dueDate": "2026-09-15"}]}),
            create_item("Get Installments", "GET", "/owner/fee-record/1/installments"),
            create_item("Update Installment", "PUT", "/owner/installments/1", body={"amount": 2000000, "dueDate": "2026-08-20"}),
            create_item("Delete Installment", "DELETE", "/owner/installments/1"),
        ]),
        folder("Payments", [
            create_item("Collect Cash", "POST", "/cashier/fee-record/1/payments/cash", body={"amount": 5000000, "note": "Tiền mặt"}),
            create_item("Bank Transfer Pending", "POST", "/cashier/fee-record/1/payments/bank-transfer", body={"amount": 5000000, "note": "Chuyển khoản"}),
            create_item("Get QR", "GET", "/cashier/payments/1/qr"),
            create_item("Get Payments by Fee Record", "GET", "/owner/fee-record/1/payments"),
            create_item("Get All Payments", "GET", "/owner/payments"),
            create_item("Get Receipt", "GET", "/owner/payments/1/receipt"),
            create_item("Void Payment", "POST", "/owner/payments/1/void?reason=Sai_sot"),
            create_item("Refund Payment", "POST", "/owner/payments/1/refund?amount=5000000&reason=Huy_hoc"),
            create_item("Financial Timeline", "GET", "/owner/students/3/timeline"),
        ]),
        folder("Refunds", [
            create_item("Request Refund", "POST", "/owner/refunds", body={"feeRecordId": 1, "requestedAmount": 2000000, "reason": "Chuyển chỗ ở", "bankAccountInfo": "VCB 123456"}),
            create_item("Decide Refund", "PATCH", "/owner/refunds/1/decision", body={"status": "APPROVED", "approvedAmount": 2000000, "approverNote": "Duyệt"}),
            create_item("Payout Refund", "PATCH", "/owner/refunds/1/payout", body={"actualRefundedAmount": 2000000, "paymentMethod": "BANK_TRANSFER", "referenceCode": "TXN123", "payoutNote": "Đã ck"}),
            create_item("Get Refunds", "GET", "/owner/refunds"),
        ]),
    ]),
    
    folder("5. Academic (Teacher)", [
        folder("Classes & Schedules", [
            create_item("My Classes", "GET", "/teacher/classes/me"),
            create_item("My Classes with Students", "GET", "/teacher/classes/with-students"),
            create_item("My Schedules", "GET", "/teacher/schedules/me"),
            create_item("Mark Attendance", "POST", "/teacher/attendance/schedules/1", body={"date": "2026-08-15", "records": [{"studentUserId": 3, "status": "PRESENT", "note": ""}]}),
            create_item("Get Attendance", "GET", "/teacher/attendance/schedules/1?date=2026-08-15"),
            create_item("Get Documents", "GET", "/teacher/classes/1/documents"),
            create_item("Upload Document", "POST", "/teacher/classes/1/documents", body={"fileId": 1, "fileName": "Tài liệu", "fileSize": 1024, "type": "MATERIAL"}),
            create_item("Delete Document", "DELETE", "/teacher/classes/1/documents/1"),
        ]),
        folder("Question Bank", [
            create_item("Get Collections", "GET", "/teacher/question-collections"),
            create_item("Create Collection", "POST", "/teacher/question-collections", body={"name": "Collection IELTS", "description": "Tập câu hỏi IELTS"}),
            create_item("Get Collection", "GET", "/teacher/question-collections/1"),
            create_item("Update Collection", "PUT", "/teacher/question-collections/1", body={"name": "Collection IELTS 2", "description": ""}),
            create_item("Delete Collection", "DELETE", "/teacher/question-collections/1"),
            create_item("Get Questions", "GET", "/teacher/questions"),
            create_item("Create Question", "POST", "/teacher/questions", body={"collectionId": 1, "type": "MULTIPLE_CHOICE", "difficulty": "EASY", "content": "1+1=?", "answers": [{"content": "2", "isCorrect": True}, {"content": "3", "isCorrect": False}]}),
            create_item("Get Question", "GET", "/teacher/questions/1"),
            create_item("Update Question", "PUT", "/teacher/questions/1", body={"collectionId": 1, "type": "MULTIPLE_CHOICE", "difficulty": "EASY", "content": "2+2=?", "answers": [{"content": "4", "isCorrect": True}]}),
            create_item("Delete Question", "DELETE", "/teacher/questions/1"),
            create_item("Bulk Delete", "POST", "/teacher/questions/bulk-delete", body={"questionIds": [1, 2, 3]}),
            create_item("Get Section Codes", "GET", "/teacher/questions/section-codes?collectionId=1"),
        ]),
        folder("Assessments & Grading", [
            create_item("Get Criteria", "GET", "/teacher/grading-criteria"),
            create_item("Create Criteria", "POST", "/teacher/grading-criteria", body={"name": "Tiêu chí IELTS Writing", "maxScore": 9.0, "rubricData": "{}"}),
            create_item("Get Criteria Details", "GET", "/teacher/grading-criteria/1"),
            create_item("Update Criteria", "PUT", "/teacher/grading-criteria/1", body={"name": "IELTS Writing", "maxScore": 9.0, "rubricData": "{}"}),
            create_item("Delete Criteria", "DELETE", "/teacher/grading-criteria/1"),
            create_item("Get Assessments", "GET", "/teacher/assessments"),
            create_item("Create Assessment", "POST", "/teacher/assessments", body={"title": "Đề thi IELTS", "description": "Đề thi thử", "durationMinutes": 60, "totalScore": 9.0}),
            create_item("Get Assessment", "GET", "/teacher/assessments/1"),
            create_item("Update Assessment", "PUT", "/teacher/assessments/1", body={"title": "Đề thi IELTS 2", "durationMinutes": 90, "totalScore": 9.0}),
            create_item("Publish Assessment", "POST", "/teacher/assessments/1/publish"),
            create_item("Archive Assessment", "POST", "/teacher/assessments/1/archive"),
            create_item("Delete Assessment", "DELETE", "/teacher/assessments/1"),
        ]),
        folder("Assignments & Reviews", [
            create_item("Get Assignments", "GET", "/teacher/assignments"),
            create_item("Create Assignment", "POST", "/teacher/assignments", body={"title": "BTVN 1", "classId": 1, "assessmentId": 1, "startTime": "2026-08-01T00:00:00Z", "endTime": "2026-08-05T00:00:00Z"}),
            create_item("Get Assignment", "GET", "/teacher/assignments/1"),
            create_item("Update Assignment", "PUT", "/teacher/assignments/1", body={"title": "BTVN 1 Update", "classId": 1, "assessmentId": 1}),
            create_item("Publish Assignment", "POST", "/teacher/assignments/1/publish"),
            create_item("Close Assignment", "POST", "/teacher/assignments/1/close"),
            create_item("Archive Assignment", "POST", "/teacher/assignments/1/archive"),
            create_item("Restore Assignment", "POST", "/teacher/assignments/1/restore"),
            create_item("Delete Assignment", "DELETE", "/teacher/assignments/1"),
            create_item("Get Submissions", "GET", "/teacher/assignments/1/submissions"),
            create_item("Get Attempt Details", "GET", "/teacher/submission-attempts/1"),
            create_item("Create/Get Review", "POST", "/teacher/submission-attempts/1/review"),
            create_item("Get Review", "GET", "/teacher/submission-attempts/1/review"),
            create_item("Update Review", "PUT", "/teacher/reviews/1", body={"score": 8.0, "feedback": "Good job", "aiSupported": False}),
            create_item("Finalize Review", "POST", "/teacher/reviews/1/finalize"),
            create_item("Release Review", "POST", "/teacher/reviews/1/release"),
            create_item("Assignment Reviews", "GET", "/teacher/assignments/1/reviews"),
            create_item("Start AI Grading", "POST", "/teacher/submission-attempts/1/ai-grading"),
            create_item("Retry AI Grading", "POST", "/teacher/ai-grading-jobs/1/retry"),
            create_item("Get AI Job", "GET", "/teacher/ai-grading-jobs/1"),
            create_item("List AI Jobs", "GET", "/teacher/submission-attempts/1/ai-grading-jobs"),
            create_item("Get AI Result", "GET", "/teacher/submission-attempts/1/ai-grading-results"),
        ]),
    ]),

    folder("6. Student Hub", [
        create_item("My Attendance", "GET", "/student/attendance"),
        create_item("My Documents", "GET", "/student/documents"),
        create_item("My Schedules", "GET", "/student/schedules/me"),
        create_item("My Assignments", "GET", "/student/assignments"),
        create_item("Start Attempt", "POST", "/student/assignments/1/attempts/start", body={}),
        create_item("Get Attempts", "GET", "/student/assignments/1/attempts"),
        create_item("Get Attempt Details", "GET", "/student/submission-attempts/1"),
        create_item("Save Answers", "PUT", "/student/submission-attempts/1/answers", body={"answers": []}),
        create_item("Save Audio Progress", "PUT", "/student/submission-attempts/1/audio-progress", body={"questionId": 1, "playedCount": 1}),
        create_item("Submit Attempt", "POST", "/student/submission-attempts/1/submit"),
        create_item("Get Review Result", "GET", "/student/submission-attempts/1/result"),
        create_item("Create QR Payment", "POST", "/student/fee-record/1/payments/qr"),
        create_item("Get Pending Payment", "GET", "/student/fee-record/1/payments/pending"),
        create_item("Cancel Payment", "POST", "/student/payments/1/cancel"),
        create_item("Get Payment QR", "GET", "/student/payments/1/qr"),
        create_item("My Payments", "GET", "/student/payments/me"),
    ]),

    folder("7. System & Admin", [
        create_item("Dashboard Owner Stats", "GET", "/owner/dashboard/stats"),
        create_item("Dashboard Cashier Stats", "GET", "/cashier/dashboard/stats"),
        create_item("Dashboard Revenue", "GET", "/owner/dashboard/revenue"),
        create_item("Audit Logs (Owner)", "GET", "/owner/audit-logs"),
        create_item("Admin Stats", "GET", "/admin/stats"),
        create_item("Admin Users", "GET", "/admin/users"),
        create_item("Admin User Details", "GET", "/admin/users/1"),
        create_item("Admin Update User Status", "PATCH", "/admin/users/1/status", body={"active": False, "reason": "Vi phạm"}),
        create_item("Admin Centers", "GET", "/admin/centers"),
        create_item("Admin Center Details", "GET", "/admin/centers/1"),
        create_item("Admin Update Center Status", "PATCH", "/admin/centers/1/status", body={"active": False, "reason": "Hết hạn"}),
        create_item("Admin Audit Logs", "GET", "/admin/audit-logs"),
    ]),

    folder("8. File Upload", [
        create_item("Upload File", "POST", "/api/files/upload", "Sử dụng form-data, key: file"),
        create_item("Get File Details", "GET", "/api/files/1"),
        create_item("Delete File", "DELETE", "/api/files/1"),
    ]),
]

collection = {
    "info": {
        "_postman_id": str(uuid.uuid4()),
        "name": "Owlexa API Collection (Generated)",
        "description": "Postman Collection hoàn chỉnh cho Owlexa Backend API.\n\n### Tính năng:\n1. **Authentication**: Tự động lưu `accessToken`, `refreshToken`, và `sessionId` khi login thành công.\n2. **Tự động gắn Headers**: Collection tự động đính kèm `X-Tenant-ID`, Bearer token.\n3. **Cấu trúc rõ ràng**: Phân chia theo Owner, Finance, Academic, Student, Admin.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": items,
    "auth": {
        "type": "bearer",
        "bearer": [
            {
                "key": "token",
                "value": "{{accessToken}}",
                "type": "string"
            }
        ]
    },
    "event": [
        {
            "listen": "prerequest",
            "script": {
                "type": "text/javascript",
                "exec": [
                    "// Set Content-Type",
                    "pm.request.headers.add({",
                    "    key: 'Content-Type',",
                    "    value: 'application/json'",
                    "});",
                    "",
                    "// Set Tenant ID if available",
                    "var tenantId = pm.collectionVariables.get('tenantId');",
                    "if (tenantId) {",
                    "    pm.request.headers.add({",
                    "        key: 'X-Tenant-ID',",
                    "        value: tenantId",
                    "    });",
                    "}"
                ]
            }
        }
    ],
    "variable": [
        {
            "key": "baseUrl",
            "value": "http://localhost:8080/api",
            "type": "string"
        },
        {
            "key": "accessToken",
            "value": "",
            "type": "string"
        },
        {
            "key": "refreshToken",
            "value": "",
            "type": "string"
        },
        {
            "key": "sessionId",
            "value": "",
            "type": "string"
        },
        {
            "key": "tenantId",
            "value": "1",
            "type": "string"
        }
    ]
}

with open('c:/Users/ADMIN/Owlexa/owlexa-backend/owlexa-api-collection.json', 'w', encoding='utf-8') as f:
    json.dump(collection, f, indent=2, ensure_ascii=False)
print("Collection generated successfully!")
