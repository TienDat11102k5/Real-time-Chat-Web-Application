# Đồ Án: Ứng Dụng Chat Realtime Đa Hội Thoại (React + Spring Boot + SQLite)

Hệ thống ứng dụng web chat thời gian thực (Realtime Chat Web Application) được phát triển và nâng cấp toàn diện từ mô hình mạng Python TCP Socket truyền thống lên kiến trúc Web hiện đại: **React 18 (Vite) + Java Spring Boot 3 (REST API & WebSocket) + SQLite Database**.

---

## 1. Kiến trúc tổng quan hệ thống

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER (React + Vite)                      │
│   - Port: 5173                                                          │
│   - Multi-Conversation UI: Phòng chung + Nhiều tab chat riêng song song │
│   - Quản lý trạng thái xem (UI View) độc lập với kết nối mạng           │
└──────────────────┬─────────────────────────────────┬────────────────────┘
                   │ HTTP REST API (Port 8080)       │ WebSocket (/ws)
                   │ (Auth, History, Online, Admin)  │ (JSON Message Protocol)
                   ▼                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    BACKEND LAYER (Java Spring Boot 3)                   │
│   - Port: 8080                                                          │
│   - REST Controllers: AuthController, MessageController, AdminController│
│   - WebSocket Handler: ChatWebSocketHandler & HandshakeInterceptor      │
│   - ConnectionManager: Quản lý tối đa 5 clients, phiên chat riêng       │
│   - Security & Validation: JWT Token, SHA-256, Audit Logging            │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ JDBC SQLite
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DATABASE LAYER (SQLite)                         │
│   - File: chat_server.db                                                │
│   - Bảng: users, messages, audit_logs                                   │
│   - Giữ nguyên 100% schema và tính tương thích dữ liệu gốc             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Các tính năng nổi bật của đồ án

### 2.1. Kiến trúc Đa hội thoại (Multi-Conversation Architecture)
* **Tách biệt 3 tầng trạng thái**:
  * **Connection State**: Kết nối WebSocket thời gian thực giữa Client và Server.
  * **Room / Conversation State**: Quyền thành viên hội thoại. Mọi user online **luôn thuộc phòng chung** và nhận đầy đủ tin nhắn phòng chung, dù đang mở tab nào trên giao diện.
  * **UI View State**: Màn hình hội thoại mà người dùng đang chọn xem trên thanh bên (Sidebar).
* **Danh sách cuộc trò chuyện linh hoạt**:
  * Chuyển đổi mượt mà giữa **Phòng chung** và **Các tab Chat riêng** chỉ bằng một cú click.
  * **Huy hiệu tin nhắn chưa đọc (Unread Badge)**: Tự động đếm số lượng tin nhắn mới khi có tin gửi đến cuộc trò chuyện mà người dùng đang không mở xem, và tự động xóa về 0 khi chọn vào tab đó.
* **Khả năng tự phục hồi khi F5 / Reload trang (Reload Resilience)**:
  * Trạng thái các tab chat riêng và tab đang mở được đồng bộ lưu trữ an toàn trong `localStorage` và máy chủ backend.
  * Khi người dùng F5 hoặc tải lại trang: **Không bị mất tab chat riêng**, **không bị kẹt session**, lịch sử tin nhắn tự động nạp lại và người dùng tiếp tục trò chuyện bình thường.

### 2.2. Nhắn tin riêng trực tiếp (Instant Direct Private Chat)
* **Không cần chờ phê duyệt**: Muốn chat với bất kỳ ai đang trực tuyến, chỉ cần nhấn **"Nhắn tin"** (hoặc **"Mở chat"**), hệ thống sẽ lập tức tạo phiên và mở tab trò chuyện riêng.
* **Tự động kích hoạt phiên**: Khi người A gửi tin nhắn riêng cho người B, hệ thống tự động thiết lập phiên trò chuyện cho cả 2 bên. Phía người B sẽ tự động xuất hiện tab chat riêng trong Sidebar kèm thông báo tin nhắn chưa đọc.
* **Bảo mật tuyệt đối**: Tin nhắn riêng chỉ gửi đến đúng 2 người trong cuộc trò chuyện, người dùng khác ở phòng chung hoàn toàn không thấy nội dung này.
* **Nút điều hướng tiện lợi**:
  * **Về phòng chung**: Quay lại xem phòng chung mà vẫn duy trì phiên chat riêng.
  * **Kết thúc chat riêng**: Đóng cuộc trò chuyện riêng, đồng bộ trạng thái kết thúc tới cả 2 client và dọn dẹp sạch tài nguyên.

### 2.3. Bảng chọn Biểu tượng cảm xúc (Quick Icon / Emoji Picker)
* Tích hợp nút `😊` ngay tại thanh soạn thảo tin nhắn.
* Phân chia thành các nhóm icon trực quan:
  * **Phản ứng nhanh**: 👍, 👎, ❤️, 😂, 🎉, 🔥,...
  * **Cảm xúc biểu cảm**: 😀, 😍, 😎, 🤔, 😢, 😡,...
  * **Biểu tượng trạng thái**: ✅, ❌, ⭐, 💡, 📌, 🚀,...
* Hỗ trợ 2 chế độ tương tác:
  * **Gửi nhanh (Quick Send)**: Nhấn vào biểu tượng để gửi tin nhắn ngay lập tức.
  * **Chèn vào ô soạn thảo (Insert)**: Chèn icon vào đúng vị trí con trỏ văn bản để viết tiếp nội dung dài.

### 2.4. Đăng ký, Đăng nhập & Bảo mật người dùng
* Đăng ký tài khoản với chính sách mật khẩu và định danh an toàn:
  * Username: 3–20 ký tự, không chứa ký tự đặc biệt, chống trùng lặp, cấm các từ khóa hệ thống (`ADMIN`, `SERVER`, `SYSTEM`, `ROOT`).
  * Mật khẩu: Tối thiểu 6 ký tự, mã hóa SHA-256 (tương thích 100% với cơ sở dữ liệu cũ).
* Cấp mã định danh JWT Token sau khi đăng nhập để xác thực cả HTTP REST API và WebSocket Handshake.
* Hỗ trợ chức năng **Đổi mật khẩu** trực tiếp trên thanh Sidebar.

### 2.5. Trung tâm Quản trị Hệ thống (Admin Panel)
* Đường dẫn truy cập riêng biệt và bảo mật: `http://localhost:5173/#/admin` (hoặc thông qua nút bảo mật trên giao diện).
* Tài khoản quản trị mặc định: `admin` / `admin123456` (có thể tùy biến qua biến môi trường hoặc `application.properties`).
* Giao diện Glassmorphism / Dark Theme hiện đại với đầy đủ chức năng quản lý:
  * **Dashboard**: Biểu đồ và thẻ thống kê realtime về tổng clients online, người trong phòng chung, cặp chat riêng, số lượng tin nhắn và thời gian máy chủ hoạt động (Uptime).
  * **Quản lý Online & Kick cưỡng chế**: Xem danh sách người dùng đang online, địa chỉ IP, thời điểm kết nối, tính năng **Kick** ngắt kết nối WebSocket ngay lập tức kèm lý do vi phạm.
  * **Quản lý Tài khoản (Accounts)**: Tìm kiếm theo tên người dùng, phân trang, gán vai trò (`user`, `moderator`, `admin`), khóa/mở khóa (`lock`/`unlock`), đặt lại mật khẩu và xóa mềm (`soft-delete`).
  * **Giám sát Phòng chat**: Theo dõi danh sách phòng chung và các cặp chat riêng đang hoạt động.
  * **Nhật ký Quản trị (Audit Logs)**: Ghi nhận vết toàn bộ hành động can thiệp của Admin (kick, đổi quyền, khóa tài khoản,...) kèm bộ lọc theo loại sự kiện.

### 2.6. Kiểm soát tài nguyên & Giới hạn an toàn (System Limits)
* **Giới hạn kết nối đồng thời**: Tối đa 5 clients đồng thời (kết nối thứ 6 tự động bị chặn ngay từ bước bắt tay WebSocket Handshake).
* **Độ dài tin nhắn**: Giới hạn tối đa 500 ký tự mỗi tin nhắn, có bộ đếm ký tự thời gian thực ở ô soạn thảo.
* **Lọc ký tự điều khiển**: Tự động loại bỏ các ký tự điều khiển độc hại (`\x00`, `\x01`, `\x02`).
* **Cơ chế Reconnect**: Tự động kết nối lại khi mạng gián đoạn; tự động ngừng kết nối lại nếu client bị quản trị viên Kick.

---

## 3. Công nghệ sử dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Frontend** | React 18, Vite, Lucide React, Vanilla CSS | Xây dựng Single Page Application (SPA), giao diện người dùng thời gian thực |
| **Backend** | Java 21, Spring Boot 3.3.5 | Cung cấp RESTful API và quản lý kết nối WebSocket |
| **Giao thức mạng** | WebSocket (JSR-356 / Spring WebSocket), HTTP/REST | Giao tiếp 2 chiều độ trễ thấp thời gian thực và quản lý tài nguyên |
| **Bảo mật** | JWT (JSON Web Token), SHA-256 Password Hashing | Xác thực phiên đăng nhập, phân quyền người dùng và bảo vệ dữ liệu |
| **Cơ sở dữ liệu** | SQLite JDBC Driver, SQLite 3 (`chat_server.db`) | Lưu trữ người dùng, lịch sử tin nhắn và nhật ký audit log |
| **Kiểm thử** | JUnit 5, Spring Boot Test, TestRestTemplate | Kiểm thử tự động API và kiểm thử kịch bản WebSocket đa người dùng |

---

## 4. Yêu cầu môi trường (Prerequisites)

* **Java Development Kit (JDK)**: Phiên bản 21 trở lên.
* **Maven**: 3.9+ (Dự án đã tích hợp sẵn Maven Wrapper `mvnw.cmd` trong thư mục `backend/`).
* **Node.js**: Phiên bản v18, v20 hoặc v24 LTS.
* **npm**: Phiên bản 9.x trở lên.

---

## 5. Hướng dẫn cài đặt và khởi chạy dự án

### Bước 1: Khởi chạy Backend (Spring Boot)
Mở cửa sổ Terminal tại thư mục gốc của dự án:

```powershell
# Di chuyển vào thư mục backend
cd backend

# Chạy backend qua Maven Wrapper
.\mvnw.cmd spring-boot:run
```

* Backend sẽ khởi động tại địa chỉ: `http://localhost:8080`
* Kiểm tra trạng thái máy chủ (Healthcheck): `http://localhost:8080/api/health`

### Bước 2: Khởi chạy Frontend (React + Vite)
Mở một cửa sổ Terminal thứ hai tại thư mục gốc của dự án:

```powershell
# Di chuyển vào thư mục frontend
cd frontend

# Cài đặt thư viện phụ thuộc (chỉ cần chạy lần đầu)
npm install

# Khởi chạy server phát triển
npm run dev
```

* Frontend sẽ khởi chạy tại địa chỉ: `http://localhost:5173`

---

## 6. Kịch bản kiểm thử trực quan (Realtime Demo)

Dưới đây là kịch bản kiểm tra đầy đủ tính năng bằng 2 hoặc 3 cửa sổ trình duyệt:

1. **Chuẩn bị 2 tài khoản**:
   * Mở trình duyệt Chrome truy cập `http://localhost:5173`, đăng ký tài khoản `alice` (mật khẩu: `123456`) và đăng nhập.
   * Mở trình duyệt thứ 2 (hoặc tab ẩn danh Incognito) truy cập `http://localhost:5173`, đăng ký tài khoản `bob` (mật khẩu: `123456`) và đăng nhập.

2. **Kiểm tra Chat phòng chung**:
   * Cả 2 trình duyệt lập tức thấy nhau trong danh sách người dùng online ở thanh bên trái.
   * `alice` gửi tin nhắn phòng chung -> `bob` nhận được ngay lập tức theo thời gian thực.
   * Thử bấm nút `😊` để gửi các biểu tượng icon cảm xúc nhanh.

3. **Kiểm tra Chat riêng trực tiếp (Không cần chờ đồng ý)**:
   * Tại màn hình của `alice`, tìm `bob` trong danh sách online và bấm nút **"Nhắn tin"**.
   * Màn hình của `alice` lập tức mở tab **"Chat riêng với bob"**.
   * `alice` gửi một tin nhắn riêng: *"Chào Bob, đây là tin nhắn bí mật!"*.
   * Tại màn hình của `bob`:
     * Ngay lập tức xuất hiện tab chat riêng với `alice` trên thanh bên, kèm huy hiệu số `1` (tin chưa đọc).
     * `bob` bấm vào tab `alice` -> Xem được tin nhắn riêng và trả lời lại.
   * Nếu mở thêm tab thứ 3 với tài khoản `charlie` ở phòng chung: `charlie` hoàn toàn không nhìn thấy bất kỳ tin nhắn nào giữa `alice` và `bob`.

4. **Kiểm tra Khả năng phục hồi khi Reload (F5)**:
   * Khi đang ở tab chat riêng, người dùng `alice` bấm phím **F5** để tải lại trang.
   * Kết quả: Tab chat riêng với `bob` vẫn giữ nguyên vẹn, lịch sử trò chuyện được tải lại đầy đủ, `bob` không bị ngắt quãng và cả 2 tiếp tục nhắn tin bình thường.

5. **Kiểm tra Điều hướng & Kết thúc chat riêng**:
   * Bấm nút **"Về phòng chung"**: Giao diện chuyển về xem phòng chung, phiên chat riêng với đối phương vẫn được giữ trong danh sách.
   * Bấm nút **"Kết thúc chat riêng"**: Phiên trò chuyện riêng kết thúc, cả 2 bên được thông báo và tab chat riêng đóng lại an toàn.

6. **Kiểm tra Quản trị viên (Admin Panel)**:
   * Truy cập `http://localhost:5173/#/admin` và đăng nhập tài khoản: `admin` / `admin123456`.
   * Thử nghiệm tính năng **Kick**: Chọn user `bob` và bấm Kick -> Tab trình duyệt của `bob` ngay lập tức bị ngắt kết nối và hiển thị hộp thoại thông báo vi phạm.
   * Thử nghiệm tính năng **Khóa tài khoản (Lock)** hoặc **Đổi quyền (Role)** trong mục Quản lý tài khoản.

---

## 7. Hệ thống Kiểm thử Tự động (Automated Testing)

Dự án được tích hợp bộ kiểm thử tự động toàn diện gồm **13 test cases** sử dụng JUnit 5 và Spring Boot Test:

```powershell
cd backend
.\mvnw.cmd test
```

### Danh mục các bài test chính:
* `MultiConversationArchitectureTest`:
  * `testMultiConversationArchitectureScenario`: Kiểm tra kiến trúc đa hội thoại, đảm bảo user chat riêng vẫn luôn nhận tin phòng chung, tính toán đúng số lượng phòng và người dùng.
  * `testPageReloadPreservesPrivateSession`: Kiểm tra tính toàn vẹn của phiên chat riêng khi 1 client ngắt kết nối và reload (F5) kết nối lại.
* `WebSocketPublicChatTest`:
  * Kiểm tra gửi/nhận tin nhắn phòng chung thời gian thực.
  * Kiểm tra chặn tin nhắn vượt quá 500 ký tự.
  * Kiểm tra từ chối kết nối thứ 6 khi đạt giới hạn 5 clients.
* `WebSocketPrivateChatTest`:
  * Kiểm tra chu trình gửi và lưu trữ tin nhắn riêng tư vào SQLite.
  * Kiểm tra tính biệt lập của dữ liệu (người ngoài không đọc được tin riêng).
* `AdminControllerTest` & `AuthControllerTest`:
  * Kiểm tra xác thực đăng nhập, cấp token, đăng ký tài khoản và phân quyền Admin REST APIs.

---

## 8. Cấu trúc thư mục dự án

```text
UngDungChat/
├── backend/                               # Máy chủ Spring Boot Backend (Java 21)
│   ├── pom.xml                            # Cấu hình Maven và dependencies
│   ├── mvnw.cmd                           # Maven Wrapper script (Windows)
│   └── src/
│       ├── main/
│       │   ├── java/com/chat/
│       │   │   ├── ChatApplication.java  # Lớp khởi chạy Spring Boot
│       │   │   ├── config/               # Cấu hình Database, WebSocket, CORS, Interceptor
│       │   │   ├── controller/           # REST Controllers (Auth, Admin, Message, User, Server)
│       │   │   ├── dto/                  # DTOs truyền nhận dữ liệu
│       │   │   ├── model/                # Thực thể dữ liệu (User, ChatMessage, AuditLog,...)
│       │   │   ├── repository/           # Thao tác JDBC SQLite (User, Message, AuditLog)
│       │   │   ├── security/             # JWT Token Service, Admin Security Interceptor
│       │   │   ├── service/              # Logic nghiệp vụ (AuthService, ValidationService)
│       │   │   └── websocket/            # Xử lý kết nối WebSocket, ConnectionManager
│       │   └── resources/
│       │       └── application.properties# Cấu hình cổng 8080, đường dẫn SQLite, tài khoản Admin
│       └── test/java/com/chat/           # Bộ kiểm thử tự động toàn diện (13 test cases)
├── frontend/                              # Ứng dụng Web Client React (Vite)
│   ├── index.html                         # Trang chủ HTML
│   ├── package.json                       # Cấu hình npm và các thư viện giao diện
│   ├── vite.config.js                     # Cấu hình Vite dev server và cổng 5173
│   └── src/
│       ├── main.jsx                       # Điểm khởi tạo React DOM
│       ├── App.jsx                        # Điều phối trạng thái đăng nhập, Chat và Admin
│       ├── api/
│       │   ├── http.js                    # Client gửi yêu cầu REST API
│       │   └── socket.js                  # Client quản lý kết nối WebSocket thời gian thực
│       ├── admin/                         # Phân hệ Quản trị hệ thống (Admin Panel)
│       │   ├── adminApi.js                # API client dành riêng cho Admin
│       │   ├── AdminLayout.jsx            # Khung điều hướng giao diện Admin
│       │   ├── AdminDashboard.jsx         # Bảng thống kê hệ thống realtime
│       │   ├── AdminUsers.jsx             # Quản lý người dùng online và nút Kick
│       │   ├── AdminAccounts.jsx          # Quản lý tài khoản (Khóa/Mở khóa, Phân quyền, Reset pass)
│       │   ├── AdminRooms.jsx             # Giám sát phòng chung và các cặp chat riêng
│       │   └── AdminLogs.jsx              # Nhật ký kiểm toán (Audit Logs)
│       ├── components/                    # Các thành phần giao diện người dùng
│       │   ├── ChatLayout.jsx             # Bố cục màn hình chat chính
│       │   ├── Sidebar.jsx                # Thanh bên chứa profile, hội thoại và danh sách online
│       │   ├── ConversationList.jsx       # Danh sách các cuộc trò chuyện & huy hiệu unread
│       │   ├── OnlineUsers.jsx            # Danh sách người dùng online & nút nhắn tin trực tiếp
│       │   ├── MessageList.jsx            # Danh sách hiển thị bong bóng tin nhắn
│       │   ├── MessageInput.jsx           # Ô soạn thảo tin nhắn, đếm ký tự & Quick Icon
│       │   ├── QuickIconPicker.jsx        # Bảng chọn biểu tượng cảm xúc phân loại
│       │   ├── LoginForm.jsx              # Biểu mẫu đăng nhập người dùng
│       │   ├── RegisterForm.jsx           # Biểu mẫu đăng ký tài khoản
│       │   ├── ChangePasswordModal.jsx    # Hộp thoại đổi mật khẩu
│       │   └── Toast.jsx                  # Thông báo nổi (Toast Notification)
│       └── styles/
│           └── main.css                   # Toàn bộ CSS hệ thống (Dark Theme, Glassmorphism)
├── src/                                   # Mã nguồn Python Socket nguyên bản (tham chiếu đồ án)
│   ├── Client.py                          # Client Python TCP Socket cũ
│   └── Server.py                          # Server Python TCP Socket cũ
├── chat_server.db                         # Cơ sở dữ liệu SQLite dùng chung
└── README.md                              # Tài liệu thuyết minh và hướng dẫn sử dụng đồ án
```

---

## 9. Tác giả & Đồ án môn học
* **Đề tài**: Xây dựng Ứng dụng Trò chuyện Thời gian thực Đa Hội thoại (Realtime Multi-Conversation Chat Application).
* **Nền tảng**: Web Application (Client-Server Architecture).