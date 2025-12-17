package uk.gov.moj.cpp.progression.handler.courts.document;

import static uk.gov.justice.core.courts.Material.material;

import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;

public class MaterialEnricher {

    @Inject
    private DocumentUserGroupFinder documentUserGroupFinder;

    @Inject
    private UtcClock clock;

    public Material enrichMaterial(final Material material, final DocumentTypeAccess documentTypeAccess) {

        final List<String> readUserGroups = documentUserGroupFinder.getReadUserGroupsFrom(documentTypeAccess);
        final ZonedDateTime uploadDateTime = getUploadDateTime(material);

        return material()
                .withId(material.getId())
                .withGenerationStatus(material.getGenerationStatus())
                .withName(material.getName())
                .withUserGroups(readUserGroups)
                .withUploadDateTime(uploadDateTime)
                .withReceivedDateTime(material.getReceivedDateTime())
                .withPrintedDateTime(material.getPrintedDateTime()).build();
    }

    private ZonedDateTime getUploadDateTime(final Material material) {
        final ZonedDateTime uploadDateTime = material.getUploadDateTime();

        if (uploadDateTime != null) {
            return uploadDateTime;
        }

        return clock.now();
    }
}
