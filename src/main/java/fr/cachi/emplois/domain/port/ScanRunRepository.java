package fr.cachi.emplois.domain.port;

import fr.cachi.emplois.domain.model.ScanRun;

/** Port de persistance des exécutions de scan (observabilité). */
public interface ScanRunRepository {
    void save(ScanRun run);
}
