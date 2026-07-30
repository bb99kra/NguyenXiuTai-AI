package nguyenxiutai.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Smart Bot System - AI-controlled players
 */
public class SmartBot {

    private final AIConfig config;
    private final List<BotPlayer> bots = new ArrayList<>();
    private final Random random = ThreadLocalRandom.current();

    // Vietnamese-style bot names
    private static final String[] BOT_NAMES = {
        "DatPro2k", "TaiXiuKing", "NguoiChoi123", "CaoThuVN", "MayMan99",
        "CuocThuTai", "XiuMaster", "TaiDo", "NguChoi", "HayNhat",
        "ProGamerVN", "TaiXiuPro", "DoiBan", "CaThu", "NguoiMoi",
        "TaiDepTrai", "XiuXinhGai", "CuocNho", "ChoiVui", "ThuNghiem"
    };

    // Bot personality types
    public enum Personality {
        AGGRESSIVE,  // Cược lớn, hay cược ngược streak
        CONSERVATIVE, // Cược nhỏ, an toàn
        BALANCED,    // Cược trung bình, random
        FOLLOWER,    // Theo dõi streak, cược theo đám đông
        CONTRARIAN   // Luôn cược ngược lại
    }

    public SmartBot(AIConfig config) {
        this.config = config;
    }

    /**
     * Initialize bots
     */
    public void initBots() {
        bots.clear();
        int count = config.getBotCount();
        String[] names = shuffleArray(BOT_NAMES);

        for (int i = 0; i < count && i < names.length; i++) {
            Personality[] personalities = Personality.values();
            Personality p = personalities[i % personalities.length];
            bots.add(new BotPlayer(
                UUID.randomUUID(),
                names[i],
                p,
                config.getBotMinBet(),
                config.getBotMaxBet()
            ));
        }
    }

    /**
     * Get bot bets for current session
     * Returns list of (botUUID, isTai, amount)
     */
    public List<BotBet> generateBotBets(AIEngine engine) {
        List<BotBet> bets = new ArrayList<>();
        AIEngine.StreakInfo streak = engine.getStreakInfo();

        for (BotPlayer bot : bots) {
            if (random.nextDouble() > 0.7) continue; // 30% chance bot skips this session

            boolean isTai = decideBotSide(bot, streak);
            long amount = decideBotAmount(bot, engine);

            if (amount > 0) {
                bets.add(new BotBet(bot.uuid, bot.name, isTai, amount));
            }
        }
        return bets;
    }

    /**
     * Decide which side bot bets on
     */
    private boolean decideBotSide(BotPlayer bot, AIEngine.StreakInfo streak) {
        double streakReaction = config.getBotReactionToStreak();

        switch (bot.personality) {
            case AGGRESSIVE:
                // Bets against streak
                if (streak.count >= 3 && random.nextDouble() < streakReaction) {
                    return !streak.side; // Opposite of streak
                }
                return random.nextBoolean();

            case CONSERVATIVE:
                // Bets with streak (safer)
                if (streak.count >= 3 && random.nextDouble() < 0.4) {
                    return streak.side; // Follow streak
                }
                return random.nextBoolean();

            case FOLLOWER:
                // Always follows the streak
                if (streak.count >= 2) {
                    return streak.side;
                }
                return random.nextBoolean();

            case CONTRARIAN:
                // Always opposite of streak
                if (streak.count >= 2) {
                    return !streak.side;
                }
                return random.nextBoolean();

            case BALANCED:
            default:
                return random.nextBoolean();
        }
    }

    /**
     * Decide how much bot bets
     */
    private long decideBotAmount(BotPlayer bot, AIEngine engine) {
        long min = bot.minBet;
        long max = bot.maxBet;

        switch (bot.personality) {
            case AGGRESSIVE:
                // Bets 60-100% of max
                return min + (long)(random.nextDouble() * 0.4 * (max - min) + 0.6 * (max - min));

            case CONSERVATIVE:
                // Bets 10-30% of max
                return min + (long)(random.nextDouble() * 0.2 * (max - min));

            case FOLLOWER:
            case CONTRARIAN:
                // Bets 20-60% of max
                return min + (long)(random.nextDouble() * 0.4 * (max - min) + 0.2 * (max - min));

            case BALANCED:
            default:
                // Bets 30-70% of max
                return min + (long)(random.nextDouble() * 0.4 * (max - min) + 0.3 * (max - min));
        }
    }

    /**
     * Get bot count
     */
    public int getBotCount() { return bots.size(); }

    /**
     * Get all bot names
     */
    public List<String> getBotNames() {
        List<String> names = new ArrayList<>();
        for (BotPlayer bot : bots) {
            names.add(bot.name);
        }
        return names;
    }

    /**
     * Check if UUID belongs to a bot
     */
    public boolean isBot(UUID uuid) {
        for (BotPlayer bot : bots) {
            if (bot.uuid.equals(uuid)) return true;
        }
        return false;
    }

    /**
     * Get bot name by UUID
     */
    public String getBotName(UUID uuid) {
        for (BotPlayer bot : bots) {
            if (bot.uuid.equals(uuid)) return bot.name;
        }
        return null;
    }

    private String[] shuffleArray(String[] arr) {
        String[] copy = arr.clone();
        for (int i = copy.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String temp = copy[i];
            copy[i] = copy[j];
            copy[j] = temp;
        }
        return copy;
    }

    // === Inner Classes ===

    public static class BotPlayer {
        public final UUID uuid;
        public final String name;
        public final Personality personality;
        public final long minBet;
        public final long maxBet;

        public BotPlayer(UUID uuid, String name, Personality personality, long minBet, long maxBet) {
            this.uuid = uuid;
            this.name = name;
            this.personality = personality;
            this.minBet = minBet;
            this.maxBet = maxBet;
        }
    }

    public static class BotBet {
        public final UUID uuid;
        public final String name;
        public final boolean isTai;
        public final long amount;

        public BotBet(UUID uuid, String name, boolean isTai, long amount) {
            this.uuid = uuid;
            this.name = name;
            this.isTai = isTai;
            this.amount = amount;
        }
    }
}
