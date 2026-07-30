# NguyenXiuTai v1.3.0-AI

Plugin Tài Xỉu cho Minecraft với **Local AI** tích hợp.

## 🤖 Tính năng AI

### Nhóm 1: Điều chỉnh tỷ lệ
- Tỷ lệ Tài/Xỉu động dựa trên lịch sử
- House-edge động theo quỹ server
- Max/Min bet động theo hành vi player

### Nhóm 2: Phân tích dữ liệu
- Phát hiện streak (chuỗi thắng/thua)
- Dự đoán xu hướng phiên sau
- Phân tích hành vi từng player

### Nhóm 3: Smart Bot
- 5 bot có tính cách: Aggressive, Conservative, Balanced, Follower, Contrarian
- Bot phản ứng cầu (đọc streak để đặt)
- Bot điều tiết quỹ

### Nhóm 4: Quản lý kinh tế
- Cảnh báo vỡ quỹ
- Auto inject quỹ khi cần
- Smart bonus cho người chơi đen
- Giới hạn lãi/lỗ ngày

## 📦 Build

```bash
mvn clean package
```

## ⚙️ Cài đặt

1. Copy `target/NguyenXiuTai-1.3.0-AI.jar` vào `plugins/`
2. Copy `src/main/resources/ai-config.yml` vào `plugins/NguyenXiuTai/`
3. Restart server

## 🎮 Lệnh

- `/tai <số tiền>` - Đặt Tài
- `/xiu <số tiền>` - Đặt Xỉu
- `/taixiu` - Mở giao diện
- `/cau` - Xem cầu
- `/taixiu ai status` - Xem trạng thái AI
- `/taixiu ai streak` - Xem cầu hiện tại
- `/taixiu ai predict` - Dự đoán phiên sau
- `/taixiu ai economy` - Xem kinh tế
- `/taixiu ai bots` - Xem danh sách bot
- `/taixiu ai reload` - Reload config AI

## 📝 Config AI

Xem file `ai-config.yml` để tùy chỉnh tất cả tính năng AI.

## 📄 License

MIT
