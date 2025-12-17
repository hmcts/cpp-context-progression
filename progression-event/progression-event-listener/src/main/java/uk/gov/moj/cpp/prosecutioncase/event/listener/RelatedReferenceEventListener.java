package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.RelatedReferenceAdded;
import uk.gov.moj.cpp.progression.events.RelatedReferenceDeleted;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.RelatedReference;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.RelatedReferenceRepository;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class RelatedReferenceEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedReferenceEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private RelatedReferenceRepository relatedReferenceRepository;

    @Handles("progression.event.related-reference-added")
    public void handleRelatedReferenceAdded(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.related-reference-added {} ", event.toObfuscatedDebugString());
        }
        final RelatedReferenceAdded relatedReferenceAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), RelatedReferenceAdded.class);

        final UUID caseId = relatedReferenceAdded.getProsecutionCaseId();
        final String relatedReference = relatedReferenceAdded.getRelatedReference();
        final RelatedReference relatedReferenceEntity = new RelatedReference();

        relatedReferenceEntity.setReference(relatedReference);
        relatedReferenceEntity.setProsecutionCaseId(caseId);
        relatedReferenceEntity.setId(relatedReferenceAdded.getRelatedReferenceId());

        relatedReferenceRepository.save(relatedReferenceEntity);

    }

    @Handles("progression.event.related-reference-deleted")
    public void handleRelatedReferenceDeleted(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.related-reference-deleted {} ", event.toObfuscatedDebugString());
        }

        final RelatedReferenceDeleted relatedReferenceDeleted = jsonObjectConverter.convert(event.payloadAsJsonObject(), RelatedReferenceDeleted.class);
        final RelatedReference relatedReferenceEntity = relatedReferenceRepository.findBy(relatedReferenceDeleted.getRelatedReferenceId());

        relatedReferenceRepository.remove(relatedReferenceEntity);
    }

}
