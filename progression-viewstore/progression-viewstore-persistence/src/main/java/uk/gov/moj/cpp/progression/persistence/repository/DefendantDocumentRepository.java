package uk.gov.moj.cpp.progression.persistence.repository;

import uk.gov.moj.cpp.progression.persistence.entity.DefendantDocument;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
/**
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@SuppressWarnings("squid:S1133")
@Repository
public interface DefendantDocumentRepository extends EntityRepository<DefendantDocument, UUID> {

    @Query(value = "SELECT dd FROM DefendantDocument dd WHERE dd.caseId = :caseId " +
            "AND dd.defendantId = :defendantId ORDER BY dd.lastModified DESC", max = 1)
    DefendantDocument findLatestDefendantDocument(
            @QueryParam("caseId") final UUID caseId,
            @QueryParam("defendantId") final UUID defendantId);

}
