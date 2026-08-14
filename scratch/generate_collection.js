const fs = require('fs');
const crypto = require('crypto');

function createItem(name, method, urlPath, description = "", body = null, auth = null, event = null, headers = null) {
    let item = {
        name: name,
        request: {
            method: method,
            header: headers || [],
            url: {
                raw: "{{baseUrl}}" + urlPath,
                host: ["{{baseUrl}}"],
                path: urlPath.replace(/^\//, '').split('/').filter(p => p)
            }
        },
        response: []
    };
    if (description) {
        item.request.description = description;
    }
    if (body) {
        item.request.body = {
            mode: "raw",
            raw: JSON.stringify(body, null, 2),
            options: {
                raw: { language: "json" }
            }
        };
    }
    if (auth) {
        item.request.auth = auth;
    }
    if (event) {
        item.event = event;
    }
    return item;
}

function folder(name, items, description = "") {
    let f = { name: name, item: items };
    if (description) {
        f.description = description;
    }
    return f;
}

const authLoginEvent = [
    {
        listen: "test",
        script: {
            exec: [
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
            type: "text/javascript"
        }
    }
];

const deviceHeaders = [
    { key: "X-Device-Type", value: "WEB", description: "WEB | MOBILE | DESKTOP | TABLET" },
    { key: "X-Device-Info", value: "Postman", description: "Optional" }
];

const items = [
    folder("1. Authentication", [
        createItem("Login", "POST", "/auth/login", "Đăng nhập", { phoneNumber: "0987654321", password: "Password123", deviceType: "WEB" }, { type: "noauth" }, authLoginEvent, deviceHeaders),
        createItem("Register Owner", "POST", "/auth/register/owner", "Đăng ký Owner", { phoneNumber: "0911223344", email: "owner@example.com", password: "Password123", fullName: "Nguyen Van Owner", centerName: "Trung tam Tuyen", subdomain: "tuyen" }, { type: "noauth" }, authLoginEvent),
        createItem("Refresh Token", "POST", "/auth/refresh-token", "Làm mới token", { type: "noauth" }, authLoginEvent),
        createItem("Logout", "POST", "/auth/logout", "Đăng xuất"),
        createItem("Get Sessions", "GET", "/auth/sessions", "Danh sách thiết bị"),
        createItem("Revoke Session", "DELETE", "/auth/sessions/{{sessionId}}", "Xóa 1 phiên"),
        createItem("Revoke All Sessions", "DELETE", "/auth/sessions", "Xóa toàn bộ phiên"),
        createItem("Get My Account", "GET", "/account", "Thông tin tài khoản"),
        createItem("Update My Account", "PUT", "/account", "Cập nhật tài khoản", { fullName: "New Name", email: "new@example.com", avatarUrl: "" }),
        createItem("Change Password", "PATCH", "/account/password", "Đổi mật khẩu", { oldPassword: "Password123", newPassword: "NewPassword123" }),
        createItem("Get Public Center", "GET", "/public/centers/me", "Thông tin Center từ subdomain", null, { type: "noauth" }),
    ], "API xác thực và tài khoản"),
    
    folder("2. Owner - Core", [
        folder("Centers", [
            createItem("Get All Centers", "GET", "/owner/centers"),
            createItem("Create Center", "POST", "/owner/centers", null, { name: "Center mới", subdomain: "center-moi" }),
            createItem("Get Center", "GET", "/owner/centers/1"),
            createItem("Update Center", "PUT", "/owner/centers/1", null, { name: "Center Update", subdomain: "center-update" }),
            createItem("Delete Center", "DELETE", "/owner/centers/1"),
        ]),
        folder("Permissions", [
            createItem("List Permissions", "GET", "/owner/permissions"),
            createItem("User Permissions", "GET", "/owner/users/1/permissions"),
            createItem("Bulk Update Overrides", "PUT", "/owner/users/1/permissions", null, { overrides: [{ permissionCode: "CLASS_VIEW", type: "GRANT" }] }),
            createItem("Single Override", "PATCH", "/owner/users/1/permissions/CLASS_VIEW", null, { type: "DENY" }),
            createItem("Remove Overrides", "DELETE", "/owner/users/1/permissions"),
        ]),
        folder("Teachers", [
            createItem("Get Teachers", "GET", "/owner/teachers"),
            createItem("Create Teacher", "POST", "/owner/teachers", null, { phoneNumber: "0922334455", fullName: "Teacher A", password: "Password123" }),
            createItem("Bulk Create Teachers", "POST", "/owner/teachers/bulk", null, { teachers: [{ phoneNumber: "0922334466", fullName: "Teacher B", password: "Password123" }] }),
            createItem("Update Teacher", "PUT", "/owner/teachers/1", null, { fullName: "Teacher Update", email: "teacher@example.com" }),
            createItem("Delete Teacher", "DELETE", "/owner/teachers/1"),
            createItem("Get Salary", "GET", "/owner/teachers/1/salary"),
            createItem("Update Salary", "PUT", "/owner/teachers/1/salary", null, { salaryType: "MONTHLY", baseSalary: 10000000, hourlyRate: 0 }),
            createItem("Clear Salary", "DELETE", "/owner/teachers/1/salary"),
        ]),
        folder("Students", [
            createItem("Get Students", "GET", "/owner/students"),
            createItem("Create Student", "POST", "/owner/students", null, { phoneNumber: "0933445566", fullName: "Student A", password: "Password123" }),
            createItem("Bulk Create Students", "POST", "/owner/students/bulk", null, { students: [{ phoneNumber: "0933445577", fullName: "Student B", password: "Password123" }] }),
            createItem("Update Student", "PUT", "/owner/students/1", null, { fullName: "Student Update", email: "student@example.com" }),
            createItem("Delete Student", "DELETE", "/owner/students/1"),
        ]),
        folder("Rooms", [
            createItem("Get Rooms", "GET", "/owner/rooms"),
            createItem("Create Room", "POST", "/owner/rooms", null, { name: "Phòng 101", capacity: 30, isActive: true }),
            createItem("Get Room", "GET", "/owner/rooms/1"),
            createItem("Update Room", "PUT", "/owner/rooms/1", null, { name: "Phòng 101 Update", capacity: 35, isActive: true }),
            createItem("Delete Room", "DELETE", "/owner/rooms/1"),
            createItem("Schedule Summary", "GET", "/owner/rooms/1/schedule-summary"),
            createItem("Validate Delete", "GET", "/owner/rooms/1/delete-validation"),
        ]),
        folder("Courses", [
            createItem("Get Courses", "GET", "/owner/courses"),
            createItem("Get All Courses", "GET", "/owner/courses/all"),
            createItem("Create Course", "POST", "/owner/courses", null, { code: "IELTS1", name: "IELTS 6.0", description: "Khóa IELTS", totalSessions: 24, feeAmount: 5000000, isActive: true }),
            createItem("Get Course", "GET", "/owner/courses/1"),
            createItem("Update Course", "PUT", "/owner/courses/1", null, { code: "IELTS1", name: "IELTS 6.0 Update", totalSessions: 24, feeAmount: 5500000, isActive: true }),
            createItem("Delete Course", "DELETE", "/owner/courses/1"),
            createItem("Course Stats", "GET", "/owner/courses/1/statistics"),
            createItem("Course Classes", "GET", "/owner/courses/1/classes"),
            createItem("Validate Delete", "GET", "/owner/courses/1/delete-validation"),
        ]),
    ]),

    folder("3. Owner - Operations", [
        folder("Classes", [
            createItem("Get Classes", "GET", "/owner/classes"),
            createItem("Classes With Students", "GET", "/owner/classes/with-students"),
            createItem("Create Class", "POST", "/owner/classes", null, { courseId: 1, code: "IELTS-01", name: "Lớp IELTS 01", teacherUserId: 2, startDate: "2026-08-01", endDate: "2026-12-01", tuitionFee: 5000000, capacity: 20 }),
            createItem("Get Class", "GET", "/owner/classes/1"),
            createItem("Update Class", "PUT", "/owner/classes/1", null, { courseId: 1, code: "IELTS-01", name: "Lớp IELTS 01", teacherUserId: 2, tuitionFee: 5000000, capacity: 20 }),
            createItem("Update Status", "PATCH", "/owner/classes/1/status", null, "OPENED"),
            createItem("Delete Class", "DELETE", "/owner/classes/1"),
        ]),
        folder("Enrollments", [
            createItem("Get Enrollments", "GET", "/owner/classes/1/enrollments"),
            createItem("Create Enrollment", "POST", "/owner/classes/1/enrollments", null, { studentUserId: 3, discountAmount: 0, status: "APPROVED" }),
            createItem("Approve", "PATCH", "/owner/classes/1/enrollments/3/approve"),
            createItem("Reject", "PATCH", "/owner/classes/1/enrollments/3/reject"),
            createItem("Drop", "PATCH", "/owner/classes/1/enrollments/3/drop"),
            createItem("Suspend", "PATCH", "/owner/classes/1/enrollments/3/suspend"),
            createItem("Reactivate", "PATCH", "/owner/classes/1/enrollments/3/reactivate"),
            createItem("Drop with Reason", "POST", "/owner/classes/1/enrollments/3/drop-with-reason", null, { reason: "Nghỉ học", refundAmount: 0 }),
            createItem("Get Dropped", "GET", "/owner/classes/1/enrollments/dropped"),
        ]),
        folder("Time Slots", [
            createItem("Get Time Slots", "GET", "/owner/time-slots"),
            createItem("Get Active Time Slots", "GET", "/owner/time-slots/active"),
            createItem("Create Time Slot", "POST", "/owner/time-slots", null, { name: "Ca 1", startTime: "08:00", endTime: "10:00", isActive: true }),
            createItem("Update Time Slot", "PUT", "/owner/time-slots/1", null, { name: "Ca 1 Update", startTime: "08:00", endTime: "10:00", isActive: true }),
            createItem("Delete Time Slot", "DELETE", "/owner/time-slots/1"),
            createItem("Quick Setup", "POST", "/owner/time-slots/quick-setup", null, { type: "MORNING" }),
        ]),
        folder("Schedules", [
            createItem("Get My Schedules", "GET", "/owner/schedules/me"),
            createItem("Get Class Schedules", "GET", "/owner/classes/1/schedules"),
            createItem("Get Teacher Schedules", "GET", "/owner/classes/1/schedules/teacher/2"),
            createItem("Create Schedule Rule", "POST", "/owner/classes/1/schedule-rules", null, { dayOfWeek: "MONDAY", timeSlotId: 1, roomId: 1, teacherUserId: 2, startDate: "2026-08-01", endDate: "2026-12-01" }),
            createItem("Get Schedule Rules", "GET", "/owner/classes/1/schedule-rules"),
            createItem("Generate Events", "POST", "/owner/classes/1/schedule-rules/1/generate"),
            createItem("Create Event", "POST", "/owner/classes/1/schedule-events", null, { date: "2026-08-15", timeSlotId: 1, roomId: 1, teacherUserId: 2, scheduleRuleId: 1, type: "REGULAR" }),
            createItem("Get Events", "GET", "/owner/classes/1/schedule-events"),
            createItem("Update Event", "PUT", "/owner/classes/1/schedule-events/1", null, { date: "2026-08-16", timeSlotId: 1, roomId: 1, teacherUserId: 2, type: "MAKEUP" }),
            createItem("Cancel Event", "PATCH", "/owner/classes/1/schedule-events/1/cancel"),
        ]),
        folder("Attendance", [
            createItem("Get Attendance by Schedule", "GET", "/owner/attendance/schedules/1?date=2026-08-15"),
            createItem("Get Attendance by Class", "GET", "/owner/attendance/classes/1?date=2026-08-15"),
            createItem("Get Attendance Range", "GET", "/owner/attendance/classes/1/range?startDate=2026-08-01&endDate=2026-08-31"),
            createItem("Get Stats", "GET", "/owner/attendance/classes/1/stats?startDate=2026-08-01&endDate=2026-08-31"),
            createItem("Mark Teacher Attendance", "POST", "/owner/teacher-attendance", null, { date: "2026-08-15", records: [{ teacherUserId: 2, status: "PRESENT", note: "" }] }),
            createItem("Get Teacher Attendance", "GET", "/owner/teacher-attendance"),
            createItem("Get Teacher Attendance Details", "GET", "/owner/teacher-attendance/1"),
            createItem("Update Teacher Attendance", "PUT", "/owner/teacher-attendance/1?status=ABSENT&note=Sick"),
            createItem("Delete Teacher Attendance", "DELETE", "/owner/teacher-attendance/1"),
        ]),
        folder("Documents", [
            createItem("Get Documents", "GET", "/owner/classes/1/documents"),
            createItem("Upload Document", "POST", "/owner/classes/1/documents", null, { fileId: 1, fileName: "Document 1", fileSize: 1024, type: "MATERIAL" }),
            createItem("Delete Document", "DELETE", "/owner/classes/1/documents/1"),
        ]),
    ]),
    
    folder("4. Finance (Owner/Cashier)", [
        folder("Cashiers", [
            createItem("Get Cashiers", "GET", "/owner/cashiers"),
            createItem("Create Cashier", "POST", "/owner/cashiers", null, { phoneNumber: "0944556677", fullName: "Cashier A", password: "Password123" }),
            createItem("Update Cashier", "PUT", "/owner/cashiers/1", null, { fullName: "Cashier Update", email: "cashier@example.com" }),
            createItem("Delete Cashier", "DELETE", "/owner/cashiers/1"),
        ]),
        folder("Fee Records", [
            createItem("Get My Fees", "GET", "/fee-records/me"),
            createItem("Overdue Fees", "GET", "/owner/fee-records/overdue"),
            createItem("Pending Fees", "GET", "/owner/fee-records/pending"),
            createItem("Class Fees", "GET", "/owner/classes/1/fee-records"),
            createItem("Update Due Date", "PUT", "/owner/classes/1/fee-records/due-date", null, { dueDate: "2026-09-01" }),
        ]),
        folder("Installments", [
            createItem("Create Installments", "POST", "/owner/fee-record/1/installments", null, { installments: [{ amount: 2500000, dueDate: "2026-08-15" }, { amount: 2500000, dueDate: "2026-09-15" }] }),
            createItem("Get Installments", "GET", "/owner/fee-record/1/installments"),
            createItem("Update Installment", "PUT", "/owner/installments/1", null, { amount: 2000000, dueDate: "2026-08-20" }),
            createItem("Delete Installment", "DELETE", "/owner/installments/1"),
        ]),
        folder("Payments", [
            createItem("Collect Cash", "POST", "/cashier/fee-record/1/payments/cash", null, { amount: 5000000, note: "Tiền mặt" }),
            createItem("Bank Transfer Pending", "POST", "/cashier/fee-record/1/payments/bank-transfer", null, { amount: 5000000, note: "Chuyển khoản" }),
            createItem("Get QR", "GET", "/cashier/payments/1/qr"),
            createItem("Get Payments by Fee Record", "GET", "/owner/fee-record/1/payments"),
            createItem("Get All Payments", "GET", "/owner/payments"),
            createItem("Get Receipt", "GET", "/owner/payments/1/receipt"),
            createItem("Void Payment", "POST", "/owner/payments/1/void?reason=Sai_sot"),
            createItem("Refund Payment", "POST", "/owner/payments/1/refund?amount=5000000&reason=Huy_hoc"),
            createItem("Financial Timeline", "GET", "/owner/students/3/timeline"),
        ]),
        folder("Refunds", [
            createItem("Request Refund", "POST", "/owner/refunds", null, { feeRecordId: 1, requestedAmount: 2000000, reason: "Chuyển chỗ ở", bankAccountInfo: "VCB 123456" }),
            createItem("Decide Refund", "PATCH", "/owner/refunds/1/decision", null, { status: "APPROVED", approvedAmount: 2000000, approverNote: "Duyệt" }),
            createItem("Payout Refund", "PATCH", "/owner/refunds/1/payout", null, { actualRefundedAmount: 2000000, paymentMethod: "BANK_TRANSFER", referenceCode: "TXN123", payoutNote: "Đã ck" }),
            createItem("Get Refunds", "GET", "/owner/refunds"),
        ]),
    ]),
    
    folder("5. Academic (Teacher)", [
        folder("Classes & Schedules", [
            createItem("My Classes", "GET", "/teacher/classes/me"),
            createItem("My Classes with Students", "GET", "/teacher/classes/with-students"),
            createItem("My Schedules", "GET", "/teacher/schedules/me"),
            createItem("Mark Attendance", "POST", "/teacher/attendance/schedules/1", null, { date: "2026-08-15", records: [{ studentUserId: 3, status: "PRESENT", note: "" }] }),
            createItem("Get Attendance", "GET", "/teacher/attendance/schedules/1?date=2026-08-15"),
            createItem("Get Documents", "GET", "/teacher/classes/1/documents"),
            createItem("Upload Document", "POST", "/teacher/classes/1/documents", null, { fileId: 1, fileName: "Tài liệu", fileSize: 1024, type: "MATERIAL" }),
            createItem("Delete Document", "DELETE", "/teacher/classes/1/documents/1"),
        ]),
        folder("Question Bank", [
            createItem("Get Collections", "GET", "/teacher/question-collections"),
            createItem("Create Collection", "POST", "/teacher/question-collections", null, { name: "Collection IELTS", description: "Tập câu hỏi IELTS" }),
            createItem("Get Collection", "GET", "/teacher/question-collections/1"),
            createItem("Update Collection", "PUT", "/teacher/question-collections/1", null, { name: "Collection IELTS 2", description: "" }),
            createItem("Delete Collection", "DELETE", "/teacher/question-collections/1"),
            createItem("Get Questions", "GET", "/teacher/questions"),
            createItem("Create Question", "POST", "/teacher/questions", null, { collectionId: 1, type: "MULTIPLE_CHOICE", difficulty: "EASY", content: "1+1=?", answers: [{ content: "2", isCorrect: true }, { content: "3", isCorrect: false }] }),
            createItem("Get Question", "GET", "/teacher/questions/1"),
            createItem("Update Question", "PUT", "/teacher/questions/1", null, { collectionId: 1, type: "MULTIPLE_CHOICE", difficulty: "EASY", content: "2+2=?", answers: [{ content: "4", isCorrect: true }] }),
            createItem("Delete Question", "DELETE", "/teacher/questions/1"),
            createItem("Bulk Delete", "POST", "/teacher/questions/bulk-delete", null, { questionIds: [1, 2, 3] }),
            createItem("Get Section Codes", "GET", "/teacher/questions/section-codes?collectionId=1"),
        ]),
        folder("Assessments & Grading", [
            createItem("Get Criteria", "GET", "/teacher/grading-criteria"),
            createItem("Create Criteria", "POST", "/teacher/grading-criteria", null, { name: "Tiêu chí IELTS Writing", maxScore: 9.0, rubricData: "{}" }),
            createItem("Get Criteria Details", "GET", "/teacher/grading-criteria/1"),
            createItem("Update Criteria", "PUT", "/teacher/grading-criteria/1", null, { name: "IELTS Writing", maxScore: 9.0, rubricData: "{}" }),
            createItem("Delete Criteria", "DELETE", "/teacher/grading-criteria/1"),
            createItem("Get Assessments", "GET", "/teacher/assessments"),
            createItem("Create Assessment", "POST", "/teacher/assessments", null, { title: "Đề thi IELTS", description: "Đề thi thử", durationMinutes: 60, totalScore: 9.0 }),
            createItem("Get Assessment", "GET", "/teacher/assessments/1"),
            createItem("Update Assessment", "PUT", "/teacher/assessments/1", null, { title: "Đề thi IELTS 2", durationMinutes: 90, totalScore: 9.0 }),
            createItem("Publish Assessment", "POST", "/teacher/assessments/1/publish"),
            createItem("Archive Assessment", "POST", "/teacher/assessments/1/archive"),
            createItem("Delete Assessment", "DELETE", "/teacher/assessments/1"),
        ]),
        folder("Assignments & Reviews", [
            createItem("Get Assignments", "GET", "/teacher/assignments"),
            createItem("Create Assignment", "POST", "/teacher/assignments", null, { title: "BTVN 1", classId: 1, assessmentId: 1, startTime: "2026-08-01T00:00:00Z", endTime: "2026-08-05T00:00:00Z" }),
            createItem("Get Assignment", "GET", "/teacher/assignments/1"),
            createItem("Update Assignment", "PUT", "/teacher/assignments/1", null, { title: "BTVN 1 Update", classId: 1, assessmentId: 1 }),
            createItem("Publish Assignment", "POST", "/teacher/assignments/1/publish"),
            createItem("Close Assignment", "POST", "/teacher/assignments/1/close"),
            createItem("Archive Assignment", "POST", "/teacher/assignments/1/archive"),
            createItem("Restore Assignment", "POST", "/teacher/assignments/1/restore"),
            createItem("Delete Assignment", "DELETE", "/teacher/assignments/1"),
            createItem("Get Submissions", "GET", "/teacher/assignments/1/submissions"),
            createItem("Get Attempt Details", "GET", "/teacher/submission-attempts/1"),
            createItem("Create/Get Review", "POST", "/teacher/submission-attempts/1/review"),
            createItem("Get Review", "GET", "/teacher/submission-attempts/1/review"),
            createItem("Update Review", "PUT", "/teacher/reviews/1", null, { score: 8.0, feedback: "Good job", aiSupported: false }),
            createItem("Finalize Review", "POST", "/teacher/reviews/1/finalize"),
            createItem("Release Review", "POST", "/teacher/reviews/1/release"),
            createItem("Assignment Reviews", "GET", "/teacher/assignments/1/reviews"),
            createItem("Start AI Grading", "POST", "/teacher/submission-attempts/1/ai-grading"),
            createItem("Retry AI Grading", "POST", "/teacher/ai-grading-jobs/1/retry"),
            createItem("Get AI Job", "GET", "/teacher/ai-grading-jobs/1"),
            createItem("List AI Jobs", "GET", "/teacher/submission-attempts/1/ai-grading-jobs"),
            createItem("Get AI Result", "GET", "/teacher/submission-attempts/1/ai-grading-results"),
        ]),
    ]),

    folder("6. Student Hub", [
        createItem("My Attendance", "GET", "/student/attendance"),
        createItem("My Documents", "GET", "/student/documents"),
        createItem("My Schedules", "GET", "/student/schedules/me"),
        createItem("My Assignments", "GET", "/student/assignments"),
        createItem("Start Attempt", "POST", "/student/assignments/1/attempts/start", null, {}),
        createItem("Get Attempts", "GET", "/student/assignments/1/attempts"),
        createItem("Get Attempt Details", "GET", "/student/submission-attempts/1"),
        createItem("Save Answers", "PUT", "/student/submission-attempts/1/answers", null, { answers: [] }),
        createItem("Save Audio Progress", "PUT", "/student/submission-attempts/1/audio-progress", null, { questionId: 1, playedCount: 1 }),
        createItem("Submit Attempt", "POST", "/student/submission-attempts/1/submit"),
        createItem("Get Review Result", "GET", "/student/submission-attempts/1/result"),
        createItem("Create QR Payment", "POST", "/student/fee-record/1/payments/qr"),
        createItem("Get Pending Payment", "GET", "/student/fee-record/1/payments/pending"),
        createItem("Cancel Payment", "POST", "/student/payments/1/cancel"),
        createItem("Get Payment QR", "GET", "/student/payments/1/qr"),
        createItem("My Payments", "GET", "/student/payments/me"),
    ]),

    folder("7. System & Admin", [
        createItem("Dashboard Owner Stats", "GET", "/owner/dashboard/stats"),
        createItem("Dashboard Cashier Stats", "GET", "/cashier/dashboard/stats"),
        createItem("Dashboard Revenue", "GET", "/owner/dashboard/revenue"),
        createItem("Audit Logs (Owner)", "GET", "/owner/audit-logs"),
        createItem("Admin Stats", "GET", "/admin/stats"),
        createItem("Admin Users", "GET", "/admin/users"),
        createItem("Admin User Details", "GET", "/admin/users/1"),
        createItem("Admin Update User Status", "PATCH", "/admin/users/1/status", null, { active: false, reason: "Vi phạm" }),
        createItem("Admin Centers", "GET", "/admin/centers"),
        createItem("Admin Center Details", "GET", "/admin/centers/1"),
        createItem("Admin Update Center Status", "PATCH", "/admin/centers/1/status", null, { active: false, reason: "Hết hạn" }),
        createItem("Admin Audit Logs", "GET", "/admin/audit-logs"),
    ]),

    folder("8. File Upload", [
        createItem("Upload File", "POST", "/api/files/upload", "Sử dụng form-data, key: file"),
        createItem("Get File Details", "GET", "/api/files/1"),
        createItem("Delete File", "DELETE", "/api/files/1"),
    ])
];

const collection = {
    info: {
        _postman_id: crypto.randomUUID(),
        name: "Owlexa API Collection",
        description: "Postman Collection hoàn chỉnh cho Owlexa Backend API.\n\n### Tính năng:\n1. **Authentication**: Tự động lưu `accessToken`, `refreshToken`, và `sessionId` khi login thành công.\n2. **Tự động gắn Headers**: Collection tự động đính kèm `X-Tenant-ID`, Bearer token.\n3. **Cấu trúc rõ ràng**: Phân chia theo Owner, Finance, Academic, Student, Admin.",
        schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    item: items,
    auth: {
        type: "bearer",
        bearer: [
            {
                key: "token",
                value: "{{accessToken}}",
                type: "string"
            }
        ]
    },
    event: [
        {
            listen: "prerequest",
            script: {
                type: "text/javascript",
                exec: [
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
    variable: [
        { key: "baseUrl", value: "http://localhost:8081", type: "string" },
        { key: "accessToken", value: "", type: "string" },
        { key: "refreshToken", value: "", type: "string" },
        { key: "sessionId", value: "", type: "string" },
        { key: "tenantId", value: "1", type: "string" }
    ]
};

fs.writeFileSync('c:/Users/ADMIN/Owlexa/owlexa-backend/owlexa-api-collection.json', JSON.stringify(collection, null, 2), 'utf-8');
console.log("Collection generated successfully!");
