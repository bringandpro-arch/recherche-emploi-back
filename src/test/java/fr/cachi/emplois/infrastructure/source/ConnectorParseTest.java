package fr.cachi.emplois.infrastructure.source;

import com.fasterxml.jackson.databind.JsonNode;
import fr.cachi.emplois.domain.model.RawJob;
import fr.cachi.emplois.infrastructure.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de parsing hors-ligne des connecteurs (aucun appel réseau). */
class ConnectorParseTest {

    @Test
    void remotive_parse_les_offres() throws Exception {
        String fixture = """
            {"jobs":[
              {"id":123,"title":"DevOps Engineer","company_name":"Acme",
               "candidate_required_location":"Europe","url":"https://x/1",
               "description":"Terraform AWS","publication_date":"2026-06-10 09:00:00",
               "job_type":"full_time","salary":"60k-80k","tags":["aws","terraform"]}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = RemotiveJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("remotive", j.source());
        assertEquals("123", j.externalId());
        assertEquals("DevOps Engineer", j.title());
        assertEquals("Acme", j.company());
        assertEquals("remote", j.remoteRaw());
        assertTrue(j.tags().contains("terraform"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void adzuna_parse_les_offres_et_le_salaire() throws Exception {
        String fixture = """
            {"results":[
              {"id":"a1","title":"Architecte Cloud","company":{"display_name":"BigCorp"},
               "location":{"display_name":"Lyon, Rhône"},"redirect_url":"https://x/2",
               "description":"AWS GCP","created":"2026-06-09T08:00:00Z",
               "contract_type":"permanent","salary_min":55000,"salary_max":70000}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = AdzunaJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("adzuna", j.source());
        assertEquals("Architecte Cloud", j.title());
        assertEquals("BigCorp", j.company());
        assertEquals("Lyon, Rhône", j.locationRaw());
        assertEquals("55000 - 70000", j.salaryRaw());
        assertNotNull(j.publishedAt());
    }

    @Test
    void france_travail_parse_les_offres() throws Exception {
        String fixture = """
            {"resultats":[
              {"id":"FT-1","intitule":"Ingénieur DevOps","entreprise":{"nom":"Société X"},
               "lieuTravail":{"libelle":"69 - Lyon"},"description":"Kubernetes Observabilité",
               "dateCreation":"2026-06-08T07:00:00.000+02:00","typeContrat":"CDI",
               "typeContratLibelle":"Contrat à durée indéterminée","salaire":{"libelle":"50k-60k"},
               "origineOffre":{"urlOrigine":"https://candidat.francetravail.fr/offres/FT-1"}}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = FranceTravailJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("france-travail", j.source());
        assertEquals("Ingénieur DevOps", j.title());
        assertEquals("Société X", j.company());
        assertEquals("69 - Lyon", j.locationRaw());
        assertEquals("Contrat à durée indéterminée", j.contractRaw());
        assertTrue(j.url().contains("francetravail"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void the_muse_parse_les_offres() throws Exception {
        String fixture = """
            {"results":[
              {"id":42,"name":"Senior DevOps Engineer","company":{"name":"MuseCorp"},
               "locations":[{"name":"Flexible / Remote"}],
               "refs":{"landing_page":"https://x/muse/42"},
               "contents":"Kubernetes AWS","publication_date":"2026-06-11T10:00:00Z",
               "categories":[{"name":"Software Engineering"}]}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = TheMuseJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("the-muse", j.source());
        assertEquals("42", j.externalId());
        assertEquals("Senior DevOps Engineer", j.title());
        assertEquals("MuseCorp", j.company());
        assertEquals("Flexible / Remote", j.locationRaw());
        assertEquals("remote", j.remoteRaw());
        assertTrue(j.url().contains("muse/42"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void remoteok_parse_et_ignore_la_mention_legale() throws Exception {
        String fixture = """
            [
              {"legal":"RemoteOK API legal notice"},
              {"id":"ro-1","position":"Platform Engineer","company":"RemoteCorp",
               "location":"Worldwide","url":"https://x/ro/1","description":"Terraform AWS",
               "date":"2026-06-12T08:00:00Z","tags":["aws","terraform"],
               "salary_min":90000,"salary_max":120000}
            ]""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = RemoteOkJobSource.parse(root);

        assertEquals(1, jobs.size(), "la mention légale doit être ignorée");
        RawJob j = jobs.get(0);
        assertEquals("remoteok", j.source());
        assertEquals("Platform Engineer", j.title());
        assertEquals("RemoteCorp", j.company());
        assertEquals("remote", j.remoteRaw());
        assertEquals("90000 - 120000", j.salaryRaw());
        assertTrue(j.tags().contains("terraform"));
    }

    @Test
    void rss_parse_les_items() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <title>Free-Work</title>
              <item>
                <title>Mission Architecte Cloud</title>
                <link>https://x/rss/1</link>
                <guid>rss-1</guid>
                <description>Régie AWS Terraform</description>
                <pubDate>Wed, 10 Jun 2026 09:00:00 +0000</pubDate>
                <category>cloud</category>
                <category>aws</category>
              </item>
            </channel></rss>""";
        List<RawJob> jobs = RssJobSource.parse(xml);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("rss", j.source());
        assertEquals("rss-1", j.externalId());
        assertEquals("Mission Architecte Cloud", j.title());
        assertEquals("https://x/rss/1", j.url());
        assertTrue(j.tags().contains("aws"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void rss_feeds_parse_la_configuration() {
        assertEquals(2, RssJobSource.parseFeeds("https://a/feed.xml, https://b/feed.xml").size());
        assertTrue(RssJobSource.parseFeeds("").isEmpty());
        assertTrue(RssJobSource.parseFeeds(null).isEmpty());
    }

    @Test
    void reponse_vide_donne_liste_vide() throws Exception {
        JsonNode empty = Json.mapper().readTree("{}");
        assertTrue(RemotiveJobSource.parse(empty).isEmpty());
        assertTrue(AdzunaJobSource.parse(empty).isEmpty());
        assertTrue(FranceTravailJobSource.parse(empty).isEmpty());
        assertTrue(TheMuseJobSource.parse(empty).isEmpty());
        assertTrue(RemoteOkJobSource.parse(Json.mapper().readTree("[]")).isEmpty());
        assertTrue(RssJobSource.parse("").isEmpty());
    }
}
