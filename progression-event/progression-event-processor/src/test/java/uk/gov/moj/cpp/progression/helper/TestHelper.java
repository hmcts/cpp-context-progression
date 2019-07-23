package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.Json;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;

public class TestHelper {

    private TestHelper(){

    }

    public static JsonEnvelope buildJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("name").build(),
                Json.createObjectBuilder().build());
    }

    public static ReferredCourtDocument buildCourtDocument(UUID documentTypeId) {
        return ReferredCourtDocument
                .referredCourtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(randomUUID())
                                .build()).build())
//                .withDocumentTypeDescription(randomUUID().toString().substring(0, 10))
                .withDocumentTypeId(documentTypeId)
//                .withIsRemoved(false)
                .withMaterials
                        (Arrays.asList(Material.material().withId(randomUUID())
                                .withUserGroups(Arrays.asList("Listing Officers", "Legal")).build()))
                .withMimeType("application/pdf")
                .withName("SampleReferredCourtDocument")
                .build();
    }


}
