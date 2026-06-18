package fr.cachi.emplois.infrastructure.source.http;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.infrastructure.json.Json;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/** Petit client HTTP JSON partagé (java.net.http) pour les connecteurs de sources. */
public final class HttpJson {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpJson() {
    }

    /** GET JSON avec en-têtes optionnels. */
    public static JsonNode get(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET();
        if (headers != null) {
            headers.forEach(b::header);
        }
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " sur " + url);
        }
        return Json.mapper().readTree(resp.body());
    }

    /** GET d'un corps texte brut (ex. flux RSS/XML) avec en-têtes optionnels. */
    public static String getText(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET();
        if (headers != null) {
            headers.forEach(b::header);
        }
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " sur " + url);
        }
        return resp.body();
    }

    /** POST application/x-www-form-urlencoded, renvoie le corps texte (ex. réponse OAuth). */
    public static String postForm(String url, Map<String, String> form, Map<String, String> headers)
            throws Exception {
        String body = form.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (headers != null) {
            headers.forEach(b::header);
        }
        HttpResponse<String> resp = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + resp.statusCode() + " sur " + url);
        }
        return resp.body();
    }

    public static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
