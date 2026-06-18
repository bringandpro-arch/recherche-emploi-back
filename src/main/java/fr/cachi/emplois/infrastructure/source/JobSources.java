package fr.cachi.emplois.infrastructure.source;

import fr.cachi.emplois.domain.port.JobSource;

import java.util.List;

/** Registre des connecteurs de sources. Active uniquement ceux correctement configurés. */
public final class JobSources {

    private JobSources() {
    }

    /** Toutes les sources connues (activées ou non). */
    public static List<JobSource> all() {
        return List.of(
                new FranceTravailJobSource(),
                new AdzunaJobSource(),
                new RemotiveJobSource(),
                new TheMuseJobSource(),
                new RemoteOkJobSource(),
                new RssJobSource(),
                // APIs publiques remote/tech sans clé
                new ArbeitnowJobSource(),
                new JobicyJobSource(),
                new HimalayasJobSource(),
                // Agrégateurs ATS, opt-in via liste de boards/entreprises
                new GreenhouseJobSource(),
                new LeverJobSource(),
                new AshbyJobSource(),
                new SmartRecruitersJobSource(),
                new RecruiteeJobSource(),
                // Agrégateurs FR, clé optionnelle
                new JoobleJobSource(),
                new CareerjetJobSource());
    }

    /** Uniquement les sources activées (clés présentes / non désactivées). */
    public static List<JobSource> enabled() {
        return all().stream().filter(JobSource::enabled).toList();
    }
}
