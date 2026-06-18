package fr.cachi.emplois.infrastructure.source;

import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.domain.model.SearchCriteria;
import fr.cachi.emplois.domain.port.JobSource;
import fr.cachi.emplois.infrastructure.source.http.HttpJson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Connecteur RSS générique (F16) : agrège des flux RSS 2.0 paramétrés via {@code RSS_FEEDS}
 * (URLs séparées par des virgules — ex. Free-Work, flux divers). Désactivé si aucun flux configuré.
 *
 * <p>Le parsing XML est protégé contre les entités externes (XXE) : DOCTYPE interdit, entités
 * externes désactivées.</p>
 */
public class RssJobSource implements JobSource {

    public static final String CODE = "rss";

    private final List<String> feeds;

    public RssJobSource() {
        this(parseFeeds(System.getenv("RSS_FEEDS")));
    }

    public RssJobSource(List<String> feeds) {
        this.feeds = feeds == null ? List.of() : List.copyOf(feeds);
    }

    static List<String> parseFeeds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean enabled() {
        return !feeds.isEmpty();
    }

    @Override
    public List<RawJob> fetch(SearchCriteria criteria) {
        List<RawJob> all = new ArrayList<>();
        for (String feed : feeds) {
            try {
                String xml = HttpJson.getText(feed, null);
                all.addAll(parse(xml));
            } catch (Exception e) {
                System.out.println("[rss] échec du flux " + feed + " : " + e.getMessage());
            }
        }
        return all;
    }

    /** Parsing testable hors-ligne d'un flux RSS 2.0. */
    public static List<RawJob> parse(String xml) {
        List<RawJob> jobs = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return jobs;
        }
        try {
            Document doc = secureBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = tag(item, "title");
                String link = tag(item, "link");
                if (title == null && link == null) {
                    continue;
                }
                String guid = tag(item, "guid");
                List<String> categories = tags(item, "category");
                jobs.add(new RawJob(
                        CODE,
                        guid != null ? guid : link,
                        title,
                        null,
                        null,
                        link,
                        tag(item, "description"),
                        parseRfc1123(tag(item, "pubDate")),
                        null,
                        null,
                        null,
                        categories));
            }
        } catch (Exception e) {
            System.out.println("[rss] flux non interprétable : " + e.getMessage());
        }
        return jobs;
    }

    private static DocumentBuilder secureBuilder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder();
    }

    /** Texte direct du premier sous-élément de tag donné (enfant direct de l'item). */
    private static String tag(Element item, String name) {
        NodeList nl = item.getElementsByTagName(name);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == item) {
                String text = n.getTextContent();
                return text == null || text.isBlank() ? null : text.trim();
            }
        }
        return null;
    }

    private static List<String> tags(Element item, String name) {
        List<String> values = new ArrayList<>();
        NodeList nl = item.getElementsByTagName(name);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() == item) {
                String text = n.getTextContent();
                if (text != null && !text.isBlank()) {
                    values.add(text.trim());
                }
            }
        }
        return values;
    }

    /** Date RSS au format RFC-1123 (ex. "Tue, 10 Jun 2026 09:00:00 +0000"), null si non interprétable. */
    static Instant parseRfc1123(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
