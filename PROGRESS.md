# Progress

Status values: NOT STARTED / IN PROGRESS / BLOCKED / DONE

| Task | Description | Status | Notes |
|---|---|---|---|
| 00 | Project setup | DONE | Tạo entry point Swing và cửa sổ tối giản; đã hướng dẫn build/run trong README. |
| 01 | Window and game state | DONE | Thêm GameState, menu Play/Exit và chuyển màn hình placeholder bằng CardLayout. |
| 02 | Board and rendering | DONE | Thêm board rỗng 10x20 và GamePanel vẽ lưới, thống kê tĩnh; menu Play mở màn hình game. |
| 03 | Tetromino shapes | DONE | Thêm 7 loại khối, model tọa độ/vòng xoay và kiểm tra model Java độc lập. |
| 04 | Movement and collision | DONE | Thêm kiểm tra biên/va chạm, di chuyển trái/phải/soft drop bằng Key Bindings và gravity Swing Timer; di chuyển xuống bị chặn trả về false, chưa khóa khối theo phạm vi task. |
| 05 | Rotation and wall kick | DONE | Thêm xoay thuận/ngược có kiểm tra vị trí và wall kick ngang 0,-1,+1,-2,+2; O giữ nguyên; đã nối phím Up/X/Z và kiểm tra model. |
| 06 | Lock piece and line clear | DONE | Thêm khóa khối, xóa/dồn nhiều hàng và phát hiện spawn thất bại để dừng timer, chuyển GAME_OVER; compile và các test model đều đạt. |
| 07 | Score, level, speed | DONE | ScoreManager tính điểm theo số hàng và level, tăng level mỗi 5 hàng, gravity cơ bản 420ms giảm dần theo level và giới hạn 70ms; UI/timer cập nhật theo chỉ số. Compile và các test model/service đều đạt. |
| 08 | Next piece and controls | DONE | Thêm current/next piece không sinh trùng, preview màu, Space hard drop và +2 điểm mỗi ô; pause/restart được nối cùng luồng trạng thái tại Task 09. Compile và các test model/UI đều đạt. |
| 09 | Menu, pause, game over | DONE | Hoàn thiện menu/pause/game over; có nút Pause ngay trong lúc chơi và Resume tiếp tục ván hiện tại; P/Escape chuyển pause/resume, R restart; reset sạch board/score/timer. Có nút "Tua x2" bật/tắt rơi gấp đôi và reset về tốc độ thường. Compile và các test lifecycle đều đạt. |
| 10 | High score persistence | DONE | HighScoreManager lưu tại %USERPROFILE%\\.blockfall\\high-score.txt, chỉ ghi kỷ lục mới và có fallback/log cho file thiếu, lỗi định dạng/I/O; best score hiển thị ở menu/game. Compile và test persistence đều đạt. |
| 11 | Polish and testing | DONE | Chuẩn hóa nhãn BlockFall/nút arcade, co font số liệu dài; checklist test gameplay, lifecycle, game over, high score và menu đều đạt; tiến trình ứng dụng khởi động bình thường. |
| 12 | Build JAR and README | DONE | README, manifest và build.ps1 hoàn chỉnh; BlockFall.jar có Main-Class: Main, build và chạy thử java -jar đều đạt. Bộ test tự động được xóa theo yêu cầu. |
| 13 | Hold piece and ghost piece | DONE | Thêm phím C để giữ/đổi khối một lần mỗi lượt, hiển thị Hold preview và ghost tại vị trí hạ cánh; reset xóa Hold. Bổ sung kiểm thử đổi/giới hạn/reset Hold và kiểm tra vị trí ghost. |
| 14 | Seven-bag piece generation | DONE | Thay sinh khối độc lập bằng 7-bag xáo trộn; mỗi nhóm bảy khối có đủ bảy loại, bag được làm mới khi restart. Bổ sung test cho nhiều nhóm và reset. |
| 15 | Play records | DONE | Thêm kỉ lục Điểm cao nhất và Level cao nhất; lưu độc lập tại thư mục .blockfall và cập nhật khi game over. |
| 16 | Line explosion effect | DONE | Thêm hiệu ứng nổ ngắn, flash, particle vàng/xanh và vòng sáng khi ăn dòng; hiệu ứng reset/pause/resume được quản lý trong GamePanel. |
| 17 | Vietnamese interface | DONE | Chuyển toàn bộ nhãn giao diện game sang tiếng Việt: menu, nút điều khiển, tạm dừng, kết thúc, điểm, cấp độ và kỉ lục. |
| 18 | Sound effects | DONE | Sinh âm hiệu ứng trực tiếp bằng Java cho di chuyển, xoay, rơi, khóa, giữ, ăn dòng, hard drop và game over; có nút bật/tắt âm thanh trong màn chơi. |
| 19 | Random level palettes | DONE | Mỗi lần level tăng tạo bảng màu ngẫu nhiên mới cho khối, ghost và preview; restart tạo palette khởi đầu ngẫu nhiên. |
| 20 | Player leaderboard | DONE | Thêm nhập tên trước khi chơi, ID ổn định theo tên (phân biệt hoa/thường, chuẩn hóa khoảng trắng), lưu điểm cao nhất từng người tại `.blockfall/leaderboard.txt`; menu dùng bảng top 10 thay cho hai ô kỷ lục chung, có thẻ nhập tên và bảng xếp hạng gọn ở góc phải. Kiểm thử persistence/UI đạt. |
| 21 | Combo clear effect | DONE | Mỗi lượt khóa có xóa dòng hiển thị banner từ COMBO x1; các lượt liên tiếp tăng xN, khóa không xóa dòng thì reset. Hiệu ứng bật lên/mờ dần, timer hỗ trợ pause/resume/restart; không đổi công thức điểm. |
| 22 | SQL Server persistence and game history | DONE | Chuyển hồ sơ/người chơi và bảng xếp hạng sang SQL Server; nhập dữ liệu file cũ một lần, ghi điểm/cấp/dòng/thời điểm khi game over và thêm màn hình xem 100 ván gần nhất. Đã build JAR và smoke test migration/UI/driver. Chưa chạy end-to-end với SQL Server vì chưa có app login trong môi trường; hướng dẫn setup nằm trong README. |

## Current working task
All tasks 00–22 complete.

## Known issues
Bộ test tự động (`test/` và `test.ps1`) đã được xóa theo yêu cầu; build/run vẫn dùng `build.ps1`. Môi trường hiện tại dùng JDK 26 với `--release 21`; chưa kiểm chứng bằng compiler JDK 21 thực tế. Chưa chạy end-to-end với SQL Server: cần tạo database/login theo README và cấu hình `BLOCKFALL_DB_URL`, `BLOCKFALL_DB_USER`, `BLOCKFALL_DB_PASSWORD`; không sửa các SQL Server instance đang có trong phiên làm việc này.
