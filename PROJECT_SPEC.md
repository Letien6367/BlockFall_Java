# Project Specification – Tetris Desktop

## Mục tiêu
Tạo game xếp gạch Tetris chạy trực tiếp trên Windows Desktop bằng Java Swing/AWT, có thể đóng gói thành JAR.

## Công nghệ
- JDK 21
- Java Swing, AWT, Graphics2D
- Swing Timer
- JDBC với Microsoft SQL Server JDBC Driver
- SQL Server Express/LocalDB cài trên cùng máy; không cần dịch vụ online
- VS Code + Extension Pack for Java
- Không database từ xa, không multiplayer/online, không JavaFX.

## Kích thước và luật chơi
- Cửa sổ đề xuất: 900 x 650 px.
- Board: 10 cột x 20 hàng hiển thị; có thể có ẩn hàng spawn nếu kiến trúc cần.
- 7 Tetromino: I, O, T, S, Z, J, L.
- Khối được sinh theo 7-bag: mỗi nhóm bảy khối có đủ bảy loại, thứ tự được xáo trộn.
- Người chơi di chuyển trái/phải, soft drop, rotate, hard drop.
- Có thể giữ/đổi khối một lần cho mỗi khối đang chơi.
- Hiển thị ghost piece tại vị trí khối sẽ hạ cánh.
- Khối dừng khi không thể đi xuống.
- Hàng đầy được xóa; các hàng phía trên rơi xuống.
- Game over khi khối mới không thể spawn.
- Hiển thị Score, Best Score, Level, Lines, Next Piece.
- Pause, Resume, Restart, Main Menu.
- Tên người chơi, bảng xếp hạng và lịch sử ván lưu trong SQL Server database `BlockFall`.
- Mỗi ván kết thúc lưu điểm, cấp độ, số dòng đã xóa và thời điểm.
- Dữ liệu bảng xếp hạng và kỷ lục cũ được nhập một lần từ file local, không xóa file gốc.

## Điều khiển mặc định
- Left / A: sang trái
- Right / D: sang phải
- Down / S: soft drop
- Up / X: xoay theo chiều kim đồng hồ
- Z: xoay ngược chiều kim đồng hồ (nếu triển khai)
- Space: hard drop
- C: hold/swap piece (một lần cho mỗi khối)
- P / Escape: pause hoặc resume
- R: restart khi phù hợp

## Quy tắc tính điểm đề xuất
- 1 hàng: 100 x level
- 2 hàng: 300 x level
- 3 hàng: 500 x level
- 4 hàng: 800 x level
- Soft drop: +1 mỗi ô rơi thủ công (tùy chọn)
- Hard drop: +2 mỗi ô rơi (tùy chọn)
- Level tăng mỗi 5 hàng đã xóa.
- Tốc độ rơi tăng theo level, có giới hạn tối thiểu để game vẫn chơi được.

## Giao diện
- Phong cách dark arcade, nền navy/dark.
- Khối gạch màu dễ phân biệt.
- Khu vực board nổi bật ở giữa/trái.
- Panel bên phải hiển thị Next, Hold, Score, Best, Level, Lines.
- Menu, Pause, Game Over có nút thực sự hoạt động.
- Không bắt buộc dùng ảnh ngoài; ưu tiên vẽ bằng Graphics2D để dễ đóng gói.

## Ngoài phạm vi phiên bản đầu
- Multiplayer/online.
- Đăng nhập/tài khoản.
- Database từ xa/cloud.
- Hệ thống huy hiệu thành tích mở khóa.
- Shop, vật phẩm phức tạp.
- Nhạc nền bắt buộc.
- Tùy chỉnh skin nâng cao.

## Tiêu chuẩn hoàn thành
- Chạy ổn định trên Windows có Java 21.
- Không lỗi compile.
- Không treo giao diện.
- Điều khiển chính xác, không xuyên board.
- Xóa hàng đúng.
- Pause không làm game tiếp tục chạy ngầm.
- Restart reset đúng toàn bộ trạng thái ván chơi.
- Thành tích và tên người chơi tồn tại sau khi tắt/mở game.
- Lịch sử ván có thể xem trong giao diện; lỗi kết nối database được báo rõ.
- Dữ liệu text cũ chỉ được nhập một lần và file nguồn vẫn được giữ nguyên.
- Có README hướng dẫn chạy và build.
