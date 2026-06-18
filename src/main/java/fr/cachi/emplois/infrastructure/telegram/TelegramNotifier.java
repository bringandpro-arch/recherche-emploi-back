package fr.cachi.emplois.infrastructure.telegram;

import fr.cachi.emplois.domain.model.Offer;
import fr.cachi.emplois.domain.model.Profile;
import fr.cachi.emplois.domain.model.ScoredOffer;
import fr.cachi.emplois.domain.port.Notifier;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;

import java.util.Map;

/**
 * Notifier Telegram (F9). Envoie un message par offre pertinente (lien + résumé + score).
 * Token via {@code TELEGRAM_BOT_TOKEN} (BotFather), destinataire via le profil ou {@code TELEGRAM_CHAT_ID}.
 */
public class TelegramNotifier implements Notifier {

    private final String botToken;
    private final String defaultChatId;

    public TelegramNotifier() {
        this(System.getenv("TELEGRAM_BOT_TOKEN"), System.getenv("TELEGRAM_CHAT_ID"));
    }

    public TelegramNotifier(String botToken, String defaultChatId) {
        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
    }

    @Override
    public boolean enabled() {
        return botToken != null && !botToken.isBlank();
    }

    @Override
    public boolean notify(Profile profile, ScoredOffer scored) {
        String chatId = profile != null && profile.telegramChatId() != null && !profile.telegramChatId().isBlank()
                ? profile.telegramChatId() : defaultChatId;
        if (!enabled() || chatId == null || chatId.isBlank()) {
            return false;
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            HttpJson.postForm(url, Map.of(
                    "chat_id", chatId,
                    "text", message(scored),
                    "parse_mode", "HTML",
                    "disable_web_page_preview", "false"), null);
            return true;
        } catch (Exception e) {
            System.out.println("[telegram] échec d'envoi : " + e.getMessage());
            return false;
        }
    }

    /** Met en forme le message (lien + résumé + score). HTML simple. */
    static String message(ScoredOffer s) {
        Offer o = s.offer();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escape(o.title())).append("</b>\n");
        if (o.company() != null) {
            sb.append("🏢 ").append(escape(o.company())).append("\n");
        }
        String loc = o.city() != null ? o.city() : o.locationRaw();
        if (loc != null) {
            sb.append("📍 ").append(escape(loc));
            if (o.remotePercent() != null) {
                sb.append(" · 🏠 ").append(o.remotePercent()).append("% remote");
            }
            sb.append("\n");
        }
        sb.append("📄 ").append(o.contractType());
        String comp = compensation(o);
        if (comp != null) {
            sb.append(" · 💶 ").append(comp);
        }
        sb.append("\n");
        sb.append("🎯 Score ").append(s.result().score()).append("/100 (")
                .append(s.result().confidenceLabel()).append(", indicatif)\n");
        if (s.result().freelanceConvertible()) {
            sb.append("🔄 Potentiellement ouvrable en freelance\n");
        }
        if (!s.result().reasons().isEmpty()) {
            sb.append("• ").append(escape(String.join(" · ",
                    s.result().reasons().stream().limit(3).toList()))).append("\n");
        }
        if (o.url() != null) {
            sb.append("🔗 ").append(o.url());
        }
        return sb.toString();
    }

    private static String compensation(Offer o) {
        if (o.tjmMin() != null || o.tjmMax() != null) {
            return "TJM " + n(o.tjmMin()) + "-" + n(o.tjmMax()) + "€";
        }
        if (o.salaryMin() != null || o.salaryMax() != null) {
            return n(o.salaryMin()) + "-" + n(o.salaryMax()) + "€/an";
        }
        return null;
    }

    private static String n(Integer i) {
        return i == null ? "?" : i.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
