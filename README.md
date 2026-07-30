# NguyenXiuTai v1.3.0-AI

Plugin Tài Xỉu cho Minecraft với **Local AI Engine** tích hợp — phân tích dữ liệu, điều chỉnh tỷ lệ động, chống lạm dụng, quản lý kinh tế server.

> **Không cần ML model, không gọi API bên ngoài.** Toàn bộ AI chạy trong JVM, phân tích real-time dựa trên lịch sử game.

---

## 📋 Mục lục

- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc AI](#-kiến-trúc-ai)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt](#-cài-đặt)
- [Lệnh](#-lệnh)
- [Config](#-config)
- [Permissions](#-permissions)
- [PlaceholderAPI](#-placeholderapi)
- [Build từ source](#-build-từ-source)
- [Cấu trúc project](#-cấu-trúc-project)
- [FAQ](#-faq)
- [License](#-license)

---

## 🎯 Tính năng chính

### 🎲 Game Tài Xỉu
- Đặt cược Tài/Xỉu bằng GUI hoặc chat
- Hệ thống cược linh hoạt: `/tai 10k`, `/xiu 5M`, `/taixiu` mở GUI
- Jackpot ngẫu nhiên
- Boss bar hiển thị phiên + thời gian
- Lịch sử cá nhân + bảng xếp hạng
- Daily bonus với streak multiplier
- Discord webhook (tùy chọn)

### 🤖 AI Engine (8 modules)

| Module | File | Chức năng |
|--------|------|-----------|
| **AIEngine** | `AIEngine.java` | Bộ não trung tâm — dynamic ratio, house edge, prediction |
| **AIDataManager** | `AIDataManager.java` | Bộ nhớ AI — history, player data, daily stats |
| **EconomyTracker** | `EconomyTracker.java` | Theo dõi kinh tế toàn server (M2 growth) |
| **AbuseDetector** | `AbuseDetector.java` | Chống lạm dụng, anti-farming, Sybil detection |
| **SmartBot** | `SmartBot.java` | Hệ thống bot 5 tính cách |
| **HeatMapGUI** | `HeatMapGUI.java` | Giao diện phân tích cầu trực quan |
| **AICommand** | `AICommand.java` | Quản lý lệnh AI |
| **AIConfig** | `AIConfig.java` | Cấu hình 6 nhóm AI |

---

## 🧠 Kiến trúc AI

### 1. Dynamic Tai Ratio (Mean-Reversion)
```
Lịch sử 50 phiên: 70% Tài
→ AIEngine tính deviation: 70% - 50% = +20%
→ Giảm xác suất Tài: 50% - (20% × 0.5) = 40%
→ Kết quả: Tài xuất hiện ít hơn, kéo tỷ lệ về 50/50
```

### 2. Dynamic House Edge
```
Quỹ thấp (< 200k) → House edge = 12% (bảo vệ quỹ)
Quỹ cao  (> 5M)   → House edge = 2%  (cho player thắng nhiều hơn)
Quỹ trung bình     → Nội suy tuyến tính giữa 2-12%
+ M2 adjustment từ EconomyTracker (±5%)
```

### 3. Economy Tracker (M2)
```
Mỗi phiên: đọc tổng balance online players qua Vault
Tính M2 growth rate = tốc độ tăng cung tiền/ngày
M2 > 15%/ngày  → INFLATION → tăng house edge
M2 < -10%/ngày → DEFLATION → giảm house edge
Predictive injection: linear regression dự báo khi nào quỹ sắp cạn
```

### 4. Abuse Detection
```
Phát hiện:
- Bonus ratio > 40% sessions → suspicious
- Account < 1 giờ + claim 2+ bonus → suspicious
- 3+ lần cược tối thiểu liên tiếp + tín hiệu khác → suspicious
- 3+ accounts cùng IP (hashed SHA-256) → suspicious
→ Flag cho admin review (không auto-block mặc định)
```

### 5. Smart Bot
```
5 tính cách:
- AGGRESSIVE:   Cược lớn, hay cược ngược cầu
- CONSERVATIVE: Cược nhỏ, theo cầu an toàn
- BALANCED:     Cược trung bình, random
- FOLLOWER:     Luôn theo streak
- CONTRARIAN:   Luôn ngược streak

Bot bets tách biệt khỏi economy thật (decorative only)
```

### 6. Dice Roll
```
AIEngine quyết định kết quả TRƯỚC dựa trên xác suất đã điều chỉnh
→ Xúc xắc quay cho khớp kết quả đã định (visual only)
→ Đây là "xác suất có kiểm soát", không phải RNG thuần
```

---

## 💻 Yêu cầu hệ thống

| Thành phần | Phiên bản |
|------------|-----------|
| Minecraft | 1.20.x (Spigot/Paper) |
| Java | 17+ |
| Vault | Bắt buộc |
| Economy plugin | EssentialsX, CMI, hoặc tương đương |
| PlaceholderAPI | Tùy chọn |

---

## 📦 Cài đặt

1. Download `NguyenXiuTai-1.3.0-AI.jar`
2. Copy vào thư mục `plugins/`
3. Đảm bảo đã cài **Vault** + **economy plugin** (EssentialsX...)
4. Restart server
5. Plugin tự tạo `plugins/NguyenXiuTai/` với:
   - `config.yml` — cấu hình cơ bản
   - `ai-config.yml` — cấu hình AI
   - `discord.yml` — Discord webhook
   - `Message.yml` — tin nhắn tùy chỉnh

### Files data (tự tạo khi chạy):
- `ai-data.yml` — lịch sử game, player data
- `economy-tracker.yml` — M2 snapshots
- `abuse-detection.yml` — abuse profiles
- `stats.yml` — thống kê player
- `daily-bonus.yml` — daily bonus records

---

## 🎮 Lệnh

### Người chơi
| Lệnh | Mô tả |
|-------|-------|
| `/tai <số tiền>` | Đặt cược Tài |
| `/xiu <số tiền>` | Đặt cược Xỉu |
| `/taixiu` | Mở giao diện chính |
| `/cau` | Xem cầu (lịch sử + thống kê) |

### AI (Public — mọi người xem được)
| Lệnh | Mô tả |
|-------|-------|
| `/taixiu ai status` | Trạng thái AI (bật/tắt, ratio, house edge) |
| `/taixiu ai streak` | Cầu hiện tại + lịch sử 20 phiên |
| `/taixiu ai predict` | Dự đoán phiên sau |

### AI (Admin — cần quyền `nguyenxiutai.admin`)
| Lệnh | Mô tả |
|-------|-------|
| `/taixiu ai economy` | Kinh tế hôm nay (Tài/Xỉu count, profit) |
| `/taixiu ai bots` | Danh sách bot |
| `/taixiu ai tracker` | M2 growth, inflation/deflation |
| `/taixiu ai abuse` | Anti-farming report (flagged players) |
| `/taixiu ai abuse <name>` | Chi tiết 1 player |
| `/taixiu ai reload` | Reload config AI |
| `/taixiu reload` | Reload toàn plugin |

### Format tiền
```
10k    = 10,000
1.5M   = 1,500,000
10,5k  = 10,500
2B     = 2,000,000,000
```

---

## ⚙️ Config

### config.yml — Cấu hình cơ bản
```yaml
thoi-gian-dat-cuoc: 45      # Giây đặt cược
thoi-gian-quay: 5            # Giây quay xúc xắc
cuoc-toi-thieu: 1000         # Bet tối thiểu
cuoc-toi-da: 10000000        # Bet tối đa (base)
so-tien-ban-dau-hu: 500000   # Quỹ ban đầu
ti-le-no-hu: 100             # Tỷ lệ nổ hũ (1/N)
so-tien-no-hu: 5000000       # Tiền nổ hũ
tien-te: "đ"                 # Ký hiệu tiền tệ
can-bang-payout: true        # Payout cân bằng theo pot
house-edge: 5                # House edge mặc định (%)
auto-save-interval: 300      # Auto-save mỗi N giây
daily-bonus-amount: 10000    # Daily bonus
daily-bonus-streak-days: 7   # Streak multiplier days
daily-bonus-streak-multiplier: 2.0
```

### ai-config.yml — Cấu hình AI (6 nhóm)

#### Nhóm 1: Điều chỉnh tỷ lệ
```yaml
ratio:
  dynamic-enabled: true
  window-size: 50          # Số phiên phân tích
  max-adjustment: 0.10     # Điều chỉnh tối đa 10%

house-edge:
  dynamic-enabled: true
  min: 0.02                # 2% khi quỹ cao
  max: 0.12                # 12% khi quỹ thấp
  quy-threshold-low: 200000
  quy-threshold-high: 5000000

bet-limit:
  dynamic-enabled: true
  floor: 500000            # Max bet tối thiểu
  ceiling: 15000000        # Max bet tối đa
  win-streak-threshold: 5  # Giảm max bet khi thắng streak
```

#### Nhóm 2: Phân tích
```yaml
analytics:
  streak-detection: true
  streak-alert-threshold: 8
  player-analysis: true
  player-min-sessions: 10
  prediction: true
  prediction-lookback: 20
```

#### Nhóm 3: Smart Bot
```yaml
bot:
  enabled: true
  count: 5
  min-bet: 5000
  max-bet: 500000
  streak-reaction: 0.6     # Bot cược ngược cầu 60%
```

#### Nhóm 4: Economy Protection
```yaml
economy:
  protection-enabled: true
  daily-profit-limit: 2000000
  daily-loss-limit: -3000000
  player-daily-loss-limit: -500000
  auto-inject-threshold: 100000
  auto-inject-amount: 200000
  smart-bonus-enabled: true
  loss-streak-for-bonus: 5
  bonus-amount: 50000
```

#### Nhóm 5: Economy Tracker (M2)
```yaml
economy-tracker:
  inflation-threshold: 0.15    # 15%/ngày = inflation alert
  deflation-threshold: -0.10   # -10%/ngày = deflation alert
  min-sessions: 10
  edge-smoothing: 0.3
```

#### Nhóm 6: Anti-Farming
```yaml
abuse-detection:
  bonus-ratio-threshold: 0.4   # >40% sessions nhận bonus = flag
  min-sessions: 5
  new-account-ms: 3600000      # < 1 giờ = suspicious
  min-bet-farming-count: 3
  auto-block: false            # true = auto block, false = flag only
  ip-cluster-threshold: 3
```

---

## 🔐 Permissions

| Permission | Default | Mô tả |
|------------|---------|-------|
| `nguyenxiutai.admin` | op | Admin: reload, economy, bots, abuse, tracker |
| `nguyenxiutai.ai.view` | true | Xem AI status, streak, predict |

---

## 📊 PlaceholderAPI

Nếu cài PlaceholderAPI, sử dụng:

| Placeholder | Mô tả |
|-------------|-------|
| `%nxt_session%` | ID phiên hiện tại |
| `%nxt_tai_total%` | Tổng cược Tài |
| `%nxt_xiu_total%` | Tổng cược Xỉu |
| `%nxt_tai_total_formatted%` | Tổng cược Tài (format) |
| `%nxt_xiu_total_formatted%` | Tổng cược Xỉu (format) |
| `%nxt_tai_count%` | Số người cược Tài |
| `%nxt_xiu_count%` | Số người cược Xỉu |
| `%nxt_total_bet%` | Tổng cược |
| `%nxt_hu%` | Quỹ hũ |
| `%nxt_hu_formatted%` | Quỹ hũ (format) |
| `%nxt_time_left%` | Thời gian còn lại |
| `%nxt_phase%` | Giai đoạn: BETTING/ROLLING/FINISHED |
| `%nxt_dice%` | Xúc xắc (Unicode) |
| `%nxt_dice_total%` | Tổng xúc xắc |
| `%nxt_result%` | Kết quả: Tài/Xỉu |
| `%nxt_streak%` | Streak hiện tại |
| `%nxt_streak_side%` | Bên streak: Tài/Xỉu |
| `%nxt_tai_percent%` | % Tài trong lịch sử |
| `%nxt_xiu_percent%` | % Xỉu trong lịch sử |
| `%nxt_player_bet%` | Cược của player |
| `%nxt_player_side%` | Bên player đang cược |
| `%nxt_player_won%` | Player thắng ván trước? |
| `%nxt_player_total_wins%` | Tổng thắng |
| `%nxt_player_total_losses%` | Tổng thua |
| `%nxt_player_winrate%` | Tỷ lệ thắng (%) |
| `%nxt_min_bet%` | Bet tối thiểu |
| `%nxt_max_bet%` | Bet tối đa |
| `%nxt_currency%` | Ký hiệu tiền tệ |
| `%nxt_history_1%` | Kết quả phiên gần nhất |
| `%nxt_history_1_color%` | Kết quả có màu |

---

## 🔨 Build từ source

```bash
# Yêu cầu: Java 17 + Maven 3.9+
git clone https://github.com/bb99kra/NguyenXiuTai-AI.git
cd NguyenXiuTai-AI
mvn clean package -DskipTests

# JAR output: target/NguyenXiuTai-1.3.0-AI.jar
```

---

## 📁 Cấu trúc project

```
src/main/java/nguyenxiutai/
├── NguyenXiuTaiPlugin.java      # Main plugin (entry point)
├── ai/
│   ├── AIEngine.java            # Bộ não AI (ratio, house edge, prediction)
│   ├── AIConfig.java            # Cấu hình 6 nhóm
│   ├── AIDataManager.java       # Bộ nhớ (history, player data, daily stats)
│   ├── EconomyTracker.java      # M2 tracker (inflation/deflation)
│   ├── AbuseDetector.java       # Anti-farming, Sybil detection
│   ├── SmartBot.java            # 5 tính cách bot
│   ├── HeatMapGUI.java          # GUI phân tích cầu
│   └── AICommand.java           # Lệnh AI
├── command/
│   ├── TaiXiuCommand.java       # Router /taixiu
│   ├── TaiCommand.java          # /tai
│   ├── XiuCommand.java          # /xiu
│   └── CauCommand.java          # /cau
├── gui/
│   ├── MainGUI.java             # GUI chính
│   ├── BetGUI.java              # GUI chọn tiền
│   ├── CauGUI.java              # GUI cầu
│   ├── Leaderboard.java         # Bảng xếp hạng
│   └── PersonalHistory.java     # Lịch sử cá nhân
├── manager/
│   ├── GameManager.java         # Quản lý game (session, bet, payout)
│   ├── EconomyManager.java      # Vault economy wrapper
│   ├── StatsManager.java        # Thống kê player
│   ├── BossBarManager.java      # Boss bar
│   ├── MessageManager.java      # Tin nhắn đa ngôn ngữ
│   ├── DailyBonusManager.java   # Daily bonus + streak
│   └── DiscordManager.java      # Discord webhook
├── model/
│   ├── GameSession.java         # Phiên game (bets, results)
│   └── PlayerStats.java         # Stats player
└── hook/
    └── NguyenXiuTaiExpansion.java  # PlaceholderAPI expansion
```

---

## ❓ FAQ

### AI có phải ML không?
Không. Đây là **rule-based heuristic engine** — phân tích thống kê, mean-reversion, threshold. Không có model, training, hay inference.

### Xúc xắc có ngẫu nhiên không?
Kết quả được AI quyết định TRƯỚC dựa trên xác suất đã điều chỉnh, xúc xắc chỉ quay cho khớp. Đây là "xác suất có kiểm soát".

### Bot có ảnh hưởng economy không?
Không. Bot bets tách biệt (decorative only), không withdraw/deposit tiền thật.

### Tại sao house edge thay đổi?
AI điều chỉnh 2-12% dựa trên quỹ plugin + M2 growth toàn server. Quỹ thấp → tăng để bảo vệ, quỹ cao → giảm cho player thắng.

### Anti-farming hoạt động thế nào?
Theo dõi bonus-to-session ratio, min-bet farming, account age, IP cluster. Flag cho admin review, không auto-block mặc định.

### Server crash thì data có mất không?
Không. Dữ liệu lưu vào YAML files, auto-save mỗi 5 phút. Restart không mất data.

---

## 📄 License

MIT
