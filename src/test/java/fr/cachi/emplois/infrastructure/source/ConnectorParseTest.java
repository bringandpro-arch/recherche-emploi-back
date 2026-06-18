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
    void arbeitnow_parse_les_offres() throws Exception {
        String fixture = """
            {"data":[
              {"slug":"an-1","title":"Cloud Engineer","company_name":"AcmeEU",
               "location":"Berlin","url":"https://x/an/1","description":"AWS Kubernetes",
               "remote":true,"job_types":["full_time"],"tags":["aws"],"created_at":1718000000}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = ArbeitnowJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("arbeitnow", j.source());
        assertEquals("an-1", j.externalId());
        assertEquals("Cloud Engineer", j.title());
        assertEquals("AcmeEU", j.company());
        assertEquals("remote", j.remoteRaw());
        assertEquals("full_time", j.contractRaw());
        assertTrue(j.tags().contains("aws"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void jobicy_parse_les_offres() throws Exception {
        String fixture = """
            {"jobs":[
              {"id":77,"jobTitle":"DevOps Engineer","companyName":"JobiCorp","jobGeo":"Anywhere",
               "url":"https://x/jb/77","jobDescription":"Terraform AWS",
               "pubDate":"2026-06-10 09:00:00","jobType":["full-time"],
               "jobIndustry":["DevOps"],"annualSalaryMin":60000,"annualSalaryMax":90000}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = JobicyJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("jobicy", j.source());
        assertEquals("77", j.externalId());
        assertEquals("DevOps Engineer", j.title());
        assertEquals("JobiCorp", j.company());
        assertEquals("remote", j.remoteRaw());
        assertEquals("full-time", j.contractRaw());
        assertEquals("60000 - 90000", j.salaryRaw());
        assertNotNull(j.publishedAt());
    }

    @Test
    void himalayas_parse_les_offres() throws Exception {
        String fixture = """
            {"jobs":[
              {"guid":"hi-1","title":"SRE","companyName":"HimCorp",
               "locationRestrictions":["Europe"],"applicationLink":"https://x/hi/1",
               "description":"Observabilité","pubDate":1718000000,"employmentType":"Full Time",
               "categories":["sre"],"minSalary":70000,"maxSalary":100000}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = HimalayasJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("himalayas", j.source());
        assertEquals("hi-1", j.externalId());
        assertEquals("SRE", j.title());
        assertEquals("HimCorp", j.company());
        assertEquals("Europe", j.locationRaw());
        assertEquals("remote", j.remoteRaw());
        assertEquals("70000 - 100000", j.salaryRaw());
        assertNotNull(j.publishedAt());
    }

    @Test
    void greenhouse_parse_les_offres() throws Exception {
        String fixture = """
            {"jobs":[
              {"id":900,"title":"Platform Engineer","absolute_url":"https://x/gh/900",
               "location":{"name":"Paris, France"},"content":"Kubernetes",
               "updated_at":"2026-06-09T08:00:00.000Z"}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = GreenhouseJobSource.parse(root, "acme");

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("greenhouse", j.source());
        assertEquals("900", j.externalId());
        assertEquals("Platform Engineer", j.title());
        assertEquals("acme", j.company());
        assertEquals("Paris, France", j.locationRaw());
        assertNotNull(j.publishedAt());
    }

    @Test
    void lever_parse_les_offres() throws Exception {
        String fixture = """
            [
              {"id":"lv-1","text":"Backend Engineer","hostedUrl":"https://x/lv/1",
               "categories":{"location":"Remote - Europe","team":"Platform","commitment":"Full-time"},
               "createdAt":1718000000000,"descriptionPlain":"Go Kubernetes"}
            ]""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = LeverJobSource.parse(root, "acme");

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("lever", j.source());
        assertEquals("lv-1", j.externalId());
        assertEquals("Backend Engineer", j.title());
        assertEquals("acme", j.company());
        assertEquals("Remote - Europe", j.locationRaw());
        assertEquals("Full-time", j.contractRaw());
        assertEquals("remote", j.remoteRaw());
        assertTrue(j.tags().contains("Platform"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void ashby_parse_les_offres() throws Exception {
        String fixture = """
            {"jobs":[
              {"id":"ah-1","title":"Staff Engineer","jobUrl":"https://x/ah/1","location":"Remote",
               "isRemote":true,"descriptionPlain":"AWS","publishedAt":"2026-06-11T10:00:00Z",
               "employmentType":"FullTime","departmentName":"Engineering"}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = AshbyJobSource.parse(root, "acme");

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("ashby", j.source());
        assertEquals("ah-1", j.externalId());
        assertEquals("Staff Engineer", j.title());
        assertEquals("acme", j.company());
        assertEquals("remote", j.remoteRaw());
        assertEquals("FullTime", j.contractRaw());
        assertTrue(j.tags().contains("Engineering"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void smartrecruiters_parse_les_offres() throws Exception {
        String fixture = """
            {"content":[
              {"id":"sr-1","name":"Cloud Architect",
               "location":{"city":"Lyon","region":"ARA","country":"fr","remote":false},
               "releasedDate":"2026-06-12T08:00:00.000Z"}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = SmartRecruitersJobSource.parse(root, "acme");

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("smartrecruiters", j.source());
        assertEquals("sr-1", j.externalId());
        assertEquals("Cloud Architect", j.title());
        assertEquals("acme", j.company());
        assertEquals("Lyon, ARA, fr", j.locationRaw());
        assertTrue(j.url().contains("acme/sr-1"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void recruitee_parse_les_offres() throws Exception {
        String fixture = """
            {"offers":[
              {"id":555,"title":"DevOps","careers_url":"https://x/rc/555","location":"Paris",
               "city":"Paris","description":"Terraform","published_at":"2026-06-13T09:00:00.000Z",
               "employment_type_code":"fulltime","department":"Infra","remote":true}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = RecruiteeJobSource.parse(root, "acme");

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("recruitee", j.source());
        assertEquals("555", j.externalId());
        assertEquals("DevOps", j.title());
        assertEquals("acme", j.company());
        assertEquals("Paris", j.locationRaw());
        assertEquals("remote", j.remoteRaw());
        assertTrue(j.tags().contains("Infra"));
        assertNotNull(j.publishedAt());
    }

    @Test
    void jooble_parse_les_offres() throws Exception {
        String fixture = """
            {"totalCount":1,"jobs":[
              {"id":4242,"title":"Architecte Cloud","company":"JoobCorp","location":"Lyon",
               "snippet":"AWS GCP","salary":"60k","type":"CDI","link":"https://x/jo/4242",
               "updated":"2026-06-14T07:00:00.0000000"}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = JoobleJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("jooble", j.source());
        assertEquals("4242", j.externalId());
        assertEquals("Architecte Cloud", j.title());
        assertEquals("JoobCorp", j.company());
        assertEquals("Lyon", j.locationRaw());
        assertEquals("CDI", j.contractRaw());
        assertEquals("60k", j.salaryRaw());
    }

    @Test
    void careerjet_parse_les_offres() throws Exception {
        String fixture = """
            {"hits":1,"jobs":[
              {"title":"Ingénieur DevOps","company":"CareerCorp","locations":"Lyon",
               "salary":"50k-60k","description":"Kubernetes","url":"https://x/cj/1",
               "date":"2026-06-15"}
            ]}""";
        JsonNode root = Json.mapper().readTree(fixture);
        List<RawJob> jobs = CareerjetJobSource.parse(root);

        assertEquals(1, jobs.size());
        RawJob j = jobs.get(0);
        assertEquals("careerjet", j.source());
        assertEquals("Ingénieur DevOps", j.title());
        assertEquals("CareerCorp", j.company());
        assertEquals("Lyon", j.locationRaw());
        assertEquals("50k-60k", j.salaryRaw());
        assertTrue(j.url().contains("cj/1"));
    }

    @Test
    void boards_parse_la_configuration() {
        assertEquals(2, Boards.parse("acme, globex").size());
        assertTrue(Boards.parse("").isEmpty());
        assertTrue(Boards.parse(null).isEmpty());
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
        assertTrue(ArbeitnowJobSource.parse(empty).isEmpty());
        assertTrue(JobicyJobSource.parse(empty).isEmpty());
        assertTrue(HimalayasJobSource.parse(empty).isEmpty());
        assertTrue(GreenhouseJobSource.parse(empty, "acme").isEmpty());
        assertTrue(LeverJobSource.parse(Json.mapper().readTree("[]"), "acme").isEmpty());
        assertTrue(AshbyJobSource.parse(empty, "acme").isEmpty());
        assertTrue(SmartRecruitersJobSource.parse(empty, "acme").isEmpty());
        assertTrue(RecruiteeJobSource.parse(empty, "acme").isEmpty());
        assertTrue(JoobleJobSource.parse(empty).isEmpty());
        assertTrue(CareerjetJobSource.parse(empty).isEmpty());
    }
}
