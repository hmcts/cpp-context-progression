package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.UnlinkedCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;

import javax.inject.Inject;

import java.util.List;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class UnlinkCasesEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkCasesEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @Handles("progression.event.cases-unlinked")
    public void casesUnlinked(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.cases-unlinked {} ", event.toObfuscatedDebugString());
        }

        final CasesUnlinked casesUnlinked = jsonObjectConverter.convert(event.payloadAsJsonObject(), CasesUnlinked.class);
        final UUID leadCaseId = casesUnlinked.getProsecutionCaseId();

        casesUnlinked.getUnlinkedCases().forEach(
                unlinkedCases -> {
                    final List<CaseLinkSplitMergeEntity> caseLinkSplitMergeEntityList = caseLinkSplitMergeRepository.findByLinkGroupId(unlinkedCases.getLinkGroupId());
                    caseLinkSplitMergeEntityList.stream()
                            .filter(entity -> isMatch(entity, unlinkedCases, leadCaseId))
                            .forEach(entity -> caseLinkSplitMergeRepository.remove(entity));
                }
        );
    }

    private boolean isMatch(CaseLinkSplitMergeEntity linkedEntity, UnlinkedCases unlinkedCase, UUID leadCaseId) {
        if (linkedEntity.getType() != LinkType.LINK){
            return false;
        }

        if (linkedEntity.getCaseId().equals(leadCaseId) && linkedEntity.getLinkedCaseId().equals(unlinkedCase.getCaseId())){
            return true;
        }

        return linkedEntity.getCaseId().equals(unlinkedCase.getCaseId()) && linkedEntity.getLinkedCaseId().equals(leadCaseId);
    }
}
