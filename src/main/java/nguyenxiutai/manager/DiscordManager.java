/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package nguyenxiutai.manager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nguyenxiutai.manager.MessageManager;
import nguyenxiutai.model.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordManager {
    private static final Gson GSON = new Gson();
    private boolean enabled = false;
    private String url = "";
    private final ThreadLocal<DecimalFormat> fmt = ThreadLocal.withInitial(() -> new DecimalFormat("#,###"));
    private String cur = "\u0111";
    private MessageManager msg;
    private final Logger logger;
    private final JavaPlugin plugin;

    public DiscordManager(Logger logger, JavaPlugin plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    public void setMessageManager(MessageManager msg) {
        this.msg = msg;
    }

    public void configure(boolean e, String u, String c) {
        this.enabled = e;
        this.url = u;
        this.cur = c;
    }

    public void sendSessionResult(GameSession.SessionResult r) {
        if (!this.enabled || this.url == null || this.url.isEmpty()) {
            return;
        }
        if (r.getTaiTotal() == 0L && r.getXiuTotal() == 0L) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                JsonObject payload = new JsonObject();
                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                int[] d = r.getDice();
                String diceStr = d[0] + "+" + d[1] + "+" + d[2] + "=" + r.getDiceTotal();
                String result = r.isTai() ? "T\u00e0i" : "X\u1ec9u";
                String title = this.msg != null ? this.msg.getDiscordEmbedTitle().replace("{session}", String.valueOf(r.getSessionId())) : "\ud83c\udfb2 Phi\u00ean #" + r.getSessionId();
                embed.addProperty("title", title);
                DecimalFormat df = this.fmt.get();
                StringBuilder desc = new StringBuilder();
                desc.append("**").append(this.msg != null ? this.msg.getDiscordFieldDice() : "X\u00fac x\u1eafc").append(":** ").append(diceStr).append("\n");
                desc.append("**").append(this.msg != null ? this.msg.getDiscordFieldKetQua() : "K\u1ebft qu\u1ea3").append(":** ").append(result).append("\n");
                desc.append("**").append(this.msg != null ? this.msg.getDiscordFieldTongCuoc() : "T\u1ed5ng c\u01b0\u1ee3c").append(":** ").append(df.format(r.getTaiTotal())).append(this.cur).append(" (T\u00e0i) | ").append(df.format(r.getXiuTotal())).append(this.cur).append(" (X\u1ec9u)\n");
                if (r.isJackpot()) {
                    desc.append("\n**").append(this.msg != null ? this.msg.getDiscordFieldJackpot() : "\ud83c\udf89 N\u1ed5 h\u0169").append(":** ").append(r.getJackpotPlayer());
                }
                embed.addProperty("description", desc.toString());
                int color = r.isTai() ? (this.msg != null ? this.msg.getDiscordColorTai() : 65280) : (this.msg != null ? this.msg.getDiscordColorXiu() : 0xFF0000);
                embed.addProperty("color", color);
                embed.addProperty("timestamp", Instant.now().toString());
                JsonObject footer = new JsonObject();
                footer.addProperty("text", this.msg != null ? this.msg.getDiscordFooter() : "NguyenXiuTai v1.2.0");
                embed.add("footer", footer);
                embeds.add(embed);
                payload.add("embeds", embeds);
                String json = GSON.toJson(payload);
                HttpURLConnection conn = (HttpURLConnection)new URL(this.url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                try (OutputStream os = conn.getOutputStream();){
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code != 204 && code != 200) {
                    this.logger.warning("[Discord] Webhook HTTP " + code);
                }
                conn.disconnect();
            }
            catch (Exception e) {
                this.logger.warning("[Discord] Webhook error: " + e.getMessage());
            }
        });
    }
}

