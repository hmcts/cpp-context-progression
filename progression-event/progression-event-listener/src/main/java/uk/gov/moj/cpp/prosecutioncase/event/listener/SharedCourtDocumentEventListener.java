package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtDocumentShared;
import uk.gov.justice.core.courts.CourtDocumentSharedV2;
import uk.gov.justice.core.courts.SharedCourtDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;

import java.util.UUID;

import javax.inject.Inject;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class SharedCourtDocumentEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @Handles("progression.event.court-document-shared")
    public void processCourtDocumentShared(final JsonEnvelope event) {
        final CourtDocumentShared courtDocumentSharedEvent = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtDocumentShared.class);
        final SharedCourtDocument sharedCourtDocument = courtDocumentSharedEvent.getSharedCourtDocument();

        if(nonNull(sharedCourtDocument.getCaseIds())) {

            for (final UUID caseId : sharedCourtDocument.getCaseIds()) {
                final SharedCourtDocumentEntity sharedCourtDocumentEntity = new SharedCourtDocumentEntity(randomUUID(),
                        sharedCourtDocument.getCourtDocumentId(),
                        sharedCourtDocument.getHearingId(),
                        sharedCourtDocument.getUserGroupId(),
                        sharedCourtDocument.getUserId(),
                        caseId,
                        null,
                        sharedCourtDocument.getDefendantId(),
                        sharedCourtDocument.getSeqNum());

                sharedCourtDocumentRepository.save(sharedCourtDocumentEntity);
            }
        }
    }

    @Handles("progression.event.court-document-shared-v2")
    public void processCourtDocumentSharedV2(final JsonEnvelope event) {
        final CourtDocumentSharedV2 courtDocumentSharedEventV2 = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtDocumentSharedV2.class);
        final SharedCourtDocument sharedCourtDocument = courtDocumentSharedEventV2.getSharedCourtDocument();

        if(nonNull(sharedCourtDocument.getCaseIds())) {

            for (final UUID caseId : sharedCourtDocument.getCaseIds()) {
                final SharedCourtDocumentEntity sharedCourtDocumentEntity = new SharedCourtDocumentEntity(randomUUID(),
                        sharedCourtDocument.getCourtDocumentId(),
                        sharedCourtDocument.getHearingId(),
                        sharedCourtDocument.getUserGroupId(),
                        sharedCourtDocument.getUserId(),
                        caseId,
                        null,
                        sharedCourtDocument.getDefendantId(),
                        sharedCourtDocument.getSeqNum());

                sharedCourtDocumentRepository.save(sharedCourtDocumentEntity);
            }
        }
        if(nonNull(sharedCourtDocument.getApplicationId())) {
            final SharedCourtDocumentEntity sharedCourtDocumentEntity = new SharedCourtDocumentEntity(randomUUID(),
                    sharedCourtDocument.getCourtDocumentId(),
                    sharedCourtDocument.getHearingId(),
                    sharedCourtDocument.getUserGroupId(),
                    sharedCourtDocument.getUserId(),
                    null,
                    sharedCourtDocument.getApplicationId(),
                    sharedCourtDocument.getDefendantId(),
                    sharedCourtDocument.getSeqNum());

            sharedCourtDocumentRepository.save(sharedCourtDocumentEntity);
        }
    }


}
