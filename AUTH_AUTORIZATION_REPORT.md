# BÁO CÁO CHI TIẾT
## HỆ THỐNG AUTHENTICATION VÀ AUTHORIZATION
### Dự án Owlexa-Backend

---

## MỤC LỤC

1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [Authentication System](#2-authentication-system)
3. [Authorization System](#3-authorization-system)
4. [Token Management](#4-token-management)
5. [Session Management](#5-session-management)
6. [Multi-Tenant Support](#6-multi-tenant-support)
7. [Security Features](#7-security-features)
8. [API Endpoints](#8-api-endpoints)
9. [Database Schema](#9-database-schema)
10. [Configuration](#10-configuration)
11. [Testing](#11-testing)
12. [Security Best Practices](#12-security-best-practices)
13. [Recommendations](#13-recommendations)

---

## 1. Tổng Quan Kiến Trúc

### 1.1 Công Nghệ Sử Dụng

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.x |
| Security | Spring Security | - |
| Token | JWT (jjwt) | - |
| Database | MySQL | - |
| ORM | Spring Data JPA / Hibernate | - |
| Password Encoding | BCrypt | - |

### 1.2 Cấu Trúc Thư Mục

```
owlexa-backend/
├── src/main/java/com/owlexa/owlexabackend/
│   ├── common/
│   │   ├── security/          # Core authentication
│   │   ├── filter/            # HTTP filters (tenant)
│   │   ├── exception/         # Exception handling
│   │   └── util/              # Utilities
│   ├── modules/
│   │   ├── auth/             # Authentication module
│   │   │   ├── controller/   # REST endpoints
│   │   │   ├── service/      # Business logic
│   │   │   └── dto/          # Data transfer objects
│   │   └── user/             # User management
│   │       ├── entity/       # JPA entities
│   │       ├── repository/   # Data access
│   │       └── service/      # Authorization service
```

---

## 2. Authentication System

### 2.1 Luồng Xác Thực (Authentication Flow)

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Client    │────▶│  AuthController │───▶│   AuthService   │
└─────────────┘     └──────────────┘     └────────┬────────┘
                                                   │
                    ┌──────────────┐     ┌────────▼────────┐
                    │   JwtUtil    │◀────│ UserRepository │
                    └──────────────┘     └─────────────────┘
```

### 2.2 JWT Token Structure

#### Access Token Claims
```json
{
  "sub": "0901234567",           // Số điện thoại (username)
  "role": "STUDENT",              // Role của user
  "tokenType": "access",          // Loại token
  "sessionId": "uuid-session-id", // Session ID
  "iat": 1719312000,             // Issued at
  "exp": 1719312900              // Expiration (15 phút)
}
```

#### Refresh Token Claims
```json
{
  "sub": "0901234567",
  "tokenType": "refresh",
  "sessionId": "uuid-session-id",
  "iat": 1719312000,
  "exp": 1719916800              // 7 ngày
}
```

### 2.3 Password Security

#### Mã hóa Password
- **Algorithm**: BCrypt
- **Strength**: Default (10 rounds)
- **Storage**: Hash không bao giờ lưu plaintext

```java
// Trong SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### Migration từ Legacy Password
- Hệ thống hỗ trợ cả plaintext (legacy) và BCrypt
- Khi user đăng nhập với plaintext, tự động upgrade lên BCrypt

```java
// Trong AuthService.java - login()
if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
    String encoded = passwordEncoder.encode(rawPassword);
    userRepository.updatePasswordById(user.getId(), encoded);
}
```

---

## 3. Authorization System

### 3.1 Role-Based Access Control (RBAC)

#### Roles Enum
```java
public enum Role {
    OWNER,    // Chủ trung tâm
    TEACHER,  // Giáo viên
    STUDENT,  // Học sinh
    CASHIER,  // Thu ngân
    ADMIN     // Quản trị hệ thống
}
```

#### Route-Based Authorization

| Route Pattern | Required Role |
|--------------|---------------|
| `/admin/**` | ADMIN |
| `/owner/**` | OWNER |
| `/teacher/**` | TEACHER |
| `/student/**` | STUDENT |
| `/cashier/**` | CASHIER |

```java
// Trong SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
    .requestMatchers("/owner/**").hasAnyAuthority("OWNER")
    .requestMatchers("/teacher/**").hasAnyAuthority("TEACHER")
    .requestMatchers("/student/**").hasAnyAuthority("STUDENT")
    .requestMatchers("/cashier/**").hasAnyAuthority("CASHIER")
    .anyRequest().authenticated()
)
```

### 3.2 Permission-Based Authorization

#### Permission Entity
```java
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String code;  // Ví dụ: "CENTER_CREATE", "VIEW_STUDENT"
    
    @Column(length = 255)
    private String description;
}
```

#### Default Permissions by Role

```java
private List<String> getDefaultPermissionCode(Role role) {
    return switch (role) {
        case OWNER   -> List.of("CENTER_CREATE", "VIEW_STUDENT", "EDIT_FEE");
        case CASHIER -> List.of("EDIT_FEE");
        case TEACHER -> List.of("VIEW_STUDENT");
        case STUDENT -> List.of("VIEW_STUDENT");
        case ADMIN   -> List.of("VIEW_SALARY");
    };
}
```

#### AuthorizationService
```java
@Service
public class AuthorizationService {
    
    // Kiểm tra role
    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser.getRole() == role;
    }
    
    // Kiểm tra permission cụ thể
    public boolean hasPermission(String permissionCode) {
        return userPermissionRepository.existsByUserIdAndPermissionCode(
            currentUser.getId(),
            permissionCode.toUpperCase()
        );
    }
    
    // Kiểm tra ownership
    public boolean isOwnerOfCenter(Long centerId) {
        return centerRepository.findById(centerId)
            .map(center -> center.getOwner().getId().equals(currentUser.getId()))
            .orElse(false);
    }
}
```

---

## 4. Token Management

### 4.1 Token Generation

```java
// Trong JwtUtil.java

public String generateAccessToken(String subject, String role, String sessionId) {
    return Jwts.builder()
            .setSubject(subject)
            .claim("role", role)
            .claim("tokenType", "access")
            .claim("sessionId", sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
}

public String generateRefreshToken(String subject, String sessionId) {
    return Jwts.builder()
            .setSubject(subject)
            .claim("tokenType", "refresh")
            .claim("sessionId", sessionId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
}
```

### 4.2 Token Expiration

| Token Type | Expiration | Config Key |
|------------|------------|------------|
| Access Token | 15 phút | `jwt.access-token-expiration-ms=900000` |
| Refresh Token | 7 ngày | `jwt.refresh-token-expiration-ms=604800000` |

### 4.3 Refresh Token Rotation

**Chiến lược**: Mỗi lần refresh token được sử dụng, một token mới được tạo và token cũ bị vô hiệu hóa.

```java
// Trong AuthService.java - refreshToken()
String newRefreshToken = jwtUtil.generateRefreshToken(phoneNumber, sessionId);
session.setRefreshTokenHash(jwtUtil.hashToken(newRefreshToken));
sessionRepository.save(session);
```

### 4.4 Token Reuse Detection

**Mục đích**: Phát hiện khi refresh token bị đánh cắp và sử dụng lại.

```java
// Trong AuthService.java
String incomingHash = jwtUtil.hashToken(token);

if (!incomingHash.equals(session.getRefreshTokenHash())) {
    log.warn("Refresh token reuse detected for userId={} sessionId={}", 
             session.getUser().getId(), sessionId);
    sessionRepository.deactivateAllByUserId(session.getUser().getId());
    throw new BadRequestException(
        "Security alert: token reuse detected. All sessions have been revoked.");
}
```

**Scenario**:
1. Attacker đánh cắp refresh token
2. Victim sử dụng refresh token → được token mới
3. Attacker dùng token cũ → **REUSE DETECTED**
4. Hệ thống revoke tất cả sessions của user

---

## 5. Session Management

### 5.1 Session Entity

```java
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    private String id;  // UUID
    
    @ManyToOne
    private User user;
    
    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;
    
    private String deviceName;
    private DeviceType deviceType;
    private String ipAddress;
    
    @Lob
    private String userAgent;
    
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiredAt;
}
```

### 5.2 Device Types

```java
public enum DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    UNKNOWN
}
```

### 5.3 Session Operations

| Operation | Description |
|-----------|-------------|
| Create | Tạo session mới khi login/register |
| Validate | Kiểm tra session còn active trong JwtFilter |
| Refresh | Cập nhật lastUsedAt khi refresh token |
| Revoke Single | Vô hiệu hóa 1 session cụ thể |
| Revoke All | Vô hiệu hóa tất cả sessions |
| Expire | Tự động hết hạn sau 7 ngày |

---

## 6. Multi-Tenant Support

### 6.1 Tenant Resolution

**Header**: `X-Tenant-ID`
- Numeric ID (ví dụ: `123`)
- Subdomain (ví dụ: `hanoi.owlexa.vn`)

### 6.2 TenantFilter

```java
public class TenantFilter extends OncePerRequestFilter {
    
    private static final ThreadLocal<Long> currentCenterId = new ThreadLocal<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String tenantId = request.getHeader("X-Tenant-ID");
        
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                // Thử parse là numeric ID
                currentCenterId.set(Long.parseLong(tenantId));
            } catch (NumberFormatException e) {
                // Thử tìm theo subdomain
                Optional<Center> center = centerRepository.findBySubdomain(tenantId);
                center.ifPresent(c -> currentCenterId.set(c.getId()));
            }
        } else {
            // Resolve từ authenticated user
            resolveTenantFromAuthenticatedUser();
        }
    }
    
    public static Long getCurrentCenterId() {
        return currentCenterId.get();
    }
}
```

### 6.3 Tenant Resolution Priority

1. `X-Tenant-ID` header (numeric ID)
2. `X-Tenant-ID` header (subdomain → lookup DB)
3. User's single membership (nếu chỉ có 1 center)

---

## 7. Security Features

### 7.1 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.getWriter().write("{\"status\":403,\"message\":\"Forbidden\"}");
                })
            )
            .build();
    }
}
```

### 7.2 CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:5173"));
    configuration.setAllowedOriginPatterns(List.of("https://*.owlexa.vn"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-ID"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);
    return configuration;
}
```

### 7.3 Cookie Security

```java
// Trong CookieUtil.java
public void setRefreshTokenCookie(HttpServletResponse response, String token) {
    ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
            .httpOnly(true)          // Không đọc bằng JavaScript
            .secure(cookieSecure)    // HTTPS only
            .sameSite("Lax")         // CSRF protection
            .path("/auth")
            .maxAge(refreshTokenExpirationMs / 1000)
            .build();
    response.addHeader("Set-Cookie", cookie.toString());
}
```

### 7.4 JWT Filter Flow

```java
// Trong JwtFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    String header = request.getHeader("Authorization");
    
    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        
        // Bỏ qua refresh tokens
        if (jwtUtil.isRefreshToken(token)) {
            chain.doFilter(request, response);
            return;
        }
        
        String phoneNumber = jwtUtil.extractSubject(token);
        String sessionId   = jwtUtil.extractSessionId(token);
        
        // Kiểm tra session còn active
        if (sessionRepository.existsByIdAndActiveTrue(sessionId)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setAttribute("currentSessionId", sessionId);
        }
    }
}
```

---

## 8. API Endpoints

### 8.1 Authentication Endpoints

| Method | Endpoint | Auth Required | Description |
|--------|----------|---------------|-------------|
| POST | `/auth/login` | No | Đăng nhập |
| POST | `/auth/register/student` | No | Đăng ký học sinh |
| POST | `/auth/register/owner` | No | Đăng ký chủ trung tâm |
| POST | `/auth/refresh-token` | No | Làm mới token |
| POST | `/auth/logout` | Yes | Đăng xuất |
| GET | `/auth/sessions` | Yes | Lấy danh sách sessions |
| DELETE | `/auth/sessions/{sessionId}` | Yes | Xóa 1 session |
| DELETE | `/auth/sessions` | Yes | Xóa tất cả sessions |

### 8.2 Request/Response Examples

#### Login Request
```json
POST /auth/login
{
  "phoneNumber": "0901234567",
  "password": "123456",
  "deviceName": "iPhone 15",
  "deviceType": "MOBILE"
}
```

#### Login Response
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "phoneNumber": "0901234567",
  "email": "student@example.com",
  "fullName": "Nguyễn Văn A",
  "roleName": "STUDENT",
  "centerName": null
}
```

#### Refresh Token
```http
POST /auth/refresh-token
Cookie: refreshToken=eyJhbGciOiJIUzI1NiJ9...
```

---

## 9. Database Schema

### 9.1 ER Diagram

```
┌─────────────────┐       ┌─────────────────────┐
│      users      │       │    permissions      │
├─────────────────┤       ├─────────────────────┤
│ id (PK)         │       │ id (PK)             │
│ phone_number    │       │ code (UNIQUE)       │
│ email           │       │ description          │
│ full_name       │       └─────────────────────┘
│ password        │              │
│ role            │              │
└────────┬────────┘              │
         │                       │
         │ 1:N                   │ N:M
         │                       │
┌────────▼────────┐       ┌──────▼────────┐
│ user_permission │       │ user_sessions │
├─────────────────┤       ├───────────────┤
│ id (PK)         │       │ id (PK) [UUID]│
│ user_id (FK)    │       │ user_id (FK)  │
│ permission_id   │       │ refresh_hash  │
│ granted_at      │       │ device_name   │
└─────────────────┘       │ device_type   │
         │                │ ip_address    │
         │                │ user_agent    │
         │                │ is_active     │
         │                │ created_at    │
         │                │ last_used_at  │
┌────────▼────────┐       │ expired_at    │
│   membership    │       └───────────────┘
├─────────────────┤
│ id (PK)         │
│ user_id (FK)    │       ┌─────────────────┐
│ center_id (FK)  │       │    centers     │
│ joined_at       │       ├─────────────────┤
└────────┬────────┘       │ id (PK)        │
         │                │ owner_user_id  │
         │ 1:N            │ name           │
         │                │ subdomain      │
┌────────▼────────┐       │ created_at    │
│    centers      │       └─────────────────┘
└─────────────────┘
```

### 9.2 Table Definitions

#### users
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role ENUM('OWNER', 'TEACHER', 'STUDENT', 'CASHIER', 'ADMIN') NOT NULL
);
```

#### user_sessions
```sql
CREATE TABLE user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(88) NOT NULL,
    device_name VARCHAR(120),
    device_type ENUM('DESKTOP', 'MOBILE', 'TABLET', 'UNKNOWN'),
    ip_address VARCHAR(50),
    user_agent TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    last_used_at DATETIME,
    expired_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_id_active ON user_sessions(id, is_active);
```

---

## 10. Configuration

### 10.1 Application Properties

```properties
# Server
server.port=${SERVER_PORT:8081}

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/owlexa_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=123456

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=owlexa-super-secret-key-minimum-256-bits-long-change-in-production-env
jwt.access-token-expiration-ms=900000
jwt.refresh-token-expiration-ms=604800000

# Cookie
cookie.secure=false
```

### 10.2 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8081 | Server port |
| `JWT_SECRET` | (hardcoded) | JWT signing key |
| `COOKIE_SECURE` | false | HTTPS only for cookies |

---

## 11. Testing

### 11.1 Unit Tests Coverage

Test file: `src/test/java/com/owlexa/owlexabackend/auth/service/AuthServiceTest.java`

| Test Case | Description |
|-----------|-------------|
| `registerStudent_shouldCreateStudentAccount` | Đăng ký student thành công |
| `registerStudent_whenPhoneNumberExists` | Đăng ký với SĐT trùng |
| `registerOwner_shouldCreateOwnerAccount` | Đăng ký owner thành công |
| `login_whenPasswordIsInvalid` | Login với mật khẩu sai |
| `login_whenPasswordValid` | Login thành công |
| `login_withLegacyPlaintextPassword` | Upgrade plaintext → BCrypt |
| `refreshToken_whenRefreshTokenIsValid` | Refresh token hợp lệ |
| `refreshToken_whenTokenIsEmpty` | Refresh token rỗng |
| `refreshToken_whenTokenIsMalformed` | Refresh token malformed |
| `refreshToken_whenTokenIsReuseDetected` | Phát hiện token reuse |
| `refreshToken_whenSessionIsRevoked` | Session đã bị revoke |
| `refreshToken_whenSessionIsExpired` | Session hết hạn |
| `logout_shouldDeactivateSession` | Logout thành công |
| `revokeSession_shouldDeactivateSpecificSession` | Xóa session cụ thể |
| `revokeAllSessions_shouldDeactivateAllUserSessions` | Xóa tất cả sessions |
| `grantPermissionToUser_shouldAddPermission` | Gán permission |

### 11.2 Test Scenarios

```java
@Test
void refreshToken_whenTokenIsReuseDetected_shouldDeactivateAllSessionsAndThrowException() {
    // Setup: Attacker dùng token cũ sau khi victim đã refresh
    String stolenToken = "old-stolen-token";
    UserSession session = new UserSession();
    session.setRefreshTokenHash("new-hash-from-victim"); // Khác với hash của stolen token
    
    when(jwtUtil.hashToken(stolenToken)).thenReturn("incoming-hash");
    
    // Verify: Tất cả sessions bị revoke
    assertThatThrownBy(() -> authService.refreshToken(stolenToken))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Security alert: token reuse detected");
    
    verify(sessionRepository).deactivateAllByUserId(userId);
}
```

---

## 12. Security Best Practices

### 12.1 Đã Áp Dụng

| Feature | Description |
|---------|-------------|
| BCrypt Password | Mật khẩu được hash với cost factor 10 |
| JWT HS256 | Token được sign với HMAC-SHA256 |
| Access/Refresh Token | Phân tách quyền truy cập |
| Token Rotation | Refresh token được rotate sau mỗi lần sử dụng |
| Reuse Detection | Phát hiện token bị đánh cắp |
| HttpOnly Cookie | Refresh token không thể đọc bằng JS |
| SameSite Cookie | Bảo vệ against CSRF |
| Session Tracking | Theo dõi device/IP/user-agent |
| Role-based Access | Phân quyền theo route |
| Permission System | Kiểm tra permission chi tiết |
| Multi-tenant | Cô lập dữ liệu theo center |

### 12.2 Cần Cải Thiện

| Issue | Recommendation |
|-------|----------------|
| JWT Secret hardcoded | Sử dụng environment variable hoặc secrets manager |
| Cookie secure=false dev | Set true trong production |
| No rate limiting | Thêm rate limiting cho login endpoint |
| No MFA | Nên thêm 2FA cho admin/owner |
| No password policy | Thêm validation về password strength |
| No audit logging | Thêm audit log cho auth events |

---

## 13. Recommendations

### 13.1 Short-term Improvements

1. **Environment-based JWT Secret**
```properties
jwt.secret=${JWT_SECRET}
```

2. **Rate Limiting**
```java
@Bean
public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
    // Implement sliding window rate limiting
}
```

3. **Audit Logging**
```java
log.info("AUTH_LOGIN_SUCCESS userId={} ip={} device={}", userId, ip, device);
log.warn("AUTH_LOGIN_FAILED phoneNumber={} reason=INVALID_PASSWORD", phone);
log.warn("AUTH_TOKEN_REUSE_DETECTED userId={} sessionId={}", userId, sessionId);
```

### 13.2 Long-term Improvements

1. **OAuth 2.0 / OpenID Connect** cho SSO
2. **Hardware Key (WebAuthn)** cho MFA
3. **Password Policy Enforcement** (min length, complexity)
4. **Device Fingerprinting** để phát hiện đăng nhập bất thường
5. **Session Timeout** cho inactive users
6. **IP Allowlisting** cho admin accounts

### 13.3 Production Checklist

- [ ] Set `jwt.secret` từ environment variable
- [ ] Enable `cookie.secure=true`
- [ ] Configure proper CORS origins
- [ ] Enable HTTPS/TLS
- [ ] Set up monitoring/alerting
- [ ] Configure backup cho database
- [ ] Review and rotate JWT secret periodically

---

## Phụ Lục

### A. Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        SecurityConfig                           │
├─────────────────────────────────────────────────────────────────┤
│ + filterChain(HttpSecurity): SecurityFilterChain               │
│ + corsConfigurationSource(): CorsConfigurationSource            │
│ + passwordEncoder(): PasswordEncoder                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────┐    ┌───────────────┐    ┌─────────────────────┐
│    JwtFilter    │───▶│  JwtUtil      │◀───│CustomUserDetailsSvc │
├─────────────────┤    ├───────────────┤    ├─────────────────────┤
│ + doFilterInt() │    │ + generate()  │    │ + loadUserByUsername│
│ - extractToken()│    │ + extract()   │    └─────────────────────┘
│ - validateSess()│    │ + isRefresh() │
└─────────────────┘    │ + hashToken() │
         │             └───────────────┘
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AuthService                              │
├─────────────────────────────────────────────────────────────────┤
│ + login(LoginRequest): LoginResult                              │
│ + refreshToken(String): RefreshResult                           │
│ + logout(String): void                                          │
│ + registerStudent(RegisterRequest): LoginResult                 │
│ + registerOwner(RegisterRequest): LoginResult                  │
│ + revokeSession(String, String): void                           │
│ + revokeAllSessions(String): void                               │
│ + grantPermissionToUser(Long, String): void                     │
└─────────────────────────────────────────────────────────────────┘
```

### B. Sequence Diagram - Login Flow

```
┌──────┐    ┌─────────────┐    ┌────────────┐    ┌─────────────┐    ┌─────────┐
│Client│    │AuthController│   │AuthService │    │ UserRepository│   │JwtUtil  │
└──┬───┘    └──────┬──────┘    └─────┬──────┘    └──────┬───────┘    └────┬────┘
   │ POST /auth    │               │                    │                 │
   │ login         │               │                    │                 │
   │──────────────▶│               │                    │                 │
   │               │ login()       │                    │                 │
   │               │──────────────▶│                    │                 │
   │               │               │ findByPhone()      │                 │
   │               │               │─────────────────────────────────────▶│
   │               │               │◀─────────────────────────────────────│
   │               │               │ verifyPassword()                     │
   │               │               │──────────────────────────────────────▶│ genToken()
   │               │               │◀──────────────────────────────────────│
   │               │               │ saveSession()                        │
   │               │               │───────────────────────────────────────▶│
   │               │ LoginResult    │                    │                 │
   │ AuthResponse  │◀──────────────│                    │                 │
   │◀──────────────│               │                    │                 │
   │ Set-Cookie    │               │                    │                 │
   │──────────────▶│               │                    │                 │
```

### C. Exception Handling

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `BadRequestException` | 400 | Invalid request data |
| `DuplicateResourceException` | 409 | Resource already exists |
| `ResourceNotFoundException` | 404 | Resource not found |
| `AccessDeniedException` | 403 | Insufficient permissions |
| `AuthenticationException` | 401 | Not authenticated |

---

## Kết Luận

Hệ thống Authentication và Authorization của Owlexa-Backend được thiết kế với các best practices về security:

1. **JWT-based authentication** với access/refresh token pattern
2. **Token rotation** kết hợp **reuse detection** để bảo vệ against token stealing
3. **Session management** chi tiết với device/IP tracking
4. **Multi-tenant architecture** với tenant isolation
5. **Role-based** và **Permission-based authorization**
6. **Secure cookie handling** với HttpOnly và SameSite attributes

Hệ thống cung cấp nền tảng security vững chắc cho ứng dụng, đồng thời có thể mở rộng thêm với MFA, audit logging, và advanced threat detection.

---

*Document generated: 2026-06-30*
*Author: Owlexa Backend Team*
