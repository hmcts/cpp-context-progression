package uk.gov.moj.cpp.progression.pilot;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import org.hibernate.Version;

@WebListener
public class PilotChecks implements ServletContextListener {
    @EJB
    private RelatedReferencePilotRepository repository;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        final String provider = Version.getVersionString();
        require(Runtime.version().feature() == 25, "Java 25 required");
        require("7.3.2.Final".equals(provider), "Unexpected server Hibernate version " + provider);
        checkRepository();
        event.getServletContext().log("PROGRESSION_JAVA25_PILOT_PASS java="
                + Runtime.version() + " hibernate=" + provider
                + " checks=zero,one,many,case-isolation,assigned-id-update,flushed-rollback");
    }

    private void checkRepository() {
        final UUID caseId = UUID.randomUUID();
        final UUID otherCaseId = UUID.randomUUID();
        final RelatedReference first = reference(caseId, "first");
        final RelatedReference second = reference(caseId, "second");

        assertReferences(caseId, Map.of());
        repository.save(first);
        assertReferences(caseId, Map.of(first.getId(), "first"));
        final RelatedReference update = reference(caseId, "updated");
        update.setId(first.getId());
        repository.save(update);
        assertReferences(caseId, Map.of(first.getId(), "updated"));
        repository.save(second);
        repository.save(reference(otherCaseId, "other case"));
        assertReferences(caseId, Map.of(first.getId(), "updated", second.getId(), "second"));
        repository.saveAndRollBack(reference(caseId, "rolled back"));
        assertReferences(caseId, Map.of(first.getId(), "updated", second.getId(), "second"));
    }

    private void assertReferences(final UUID caseId, final Map<UUID, String> expected) {
        final List<RelatedReference> rows = repository.findByProsecutionCaseId(caseId);
        require(rows.stream().allMatch(row -> caseId.equals(row.getProsecutionCaseId())),
                "Query returned a reference for another case");
        final Map<UUID, String> actual = rows.stream()
                .collect(Collectors.toMap(RelatedReference::getId, RelatedReference::getReference));
        require(expected.equals(actual), "Unexpected references for the synthetic case");
    }

    private static RelatedReference reference(final UUID caseId, final String text) {
        final RelatedReference reference = new RelatedReference();
        reference.setId(UUID.randomUUID());
        reference.setProsecutionCaseId(caseId);
        reference.setReference(text);
        return reference;
    }

    private static void require(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
