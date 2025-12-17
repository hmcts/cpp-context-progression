package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.events.LinkCases;
import uk.gov.moj.cpp.progression.events.MergeCases;
import uk.gov.moj.cpp.progression.events.SplitCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class LinkSplitMergeCasesEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkSplitMergeCasesEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @Inject
    private SearchProsecutionCaseRepository searchCaseRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Handles("progression.event.link-cases")
    public void linkCases(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.link-cases {} ", event.toObfuscatedDebugString());
        }
        final LinkCases linkCases = jsonObjectConverter.convert(event.payloadAsJsonObject(), LinkCases.class);

        final UUID leadCaseId = linkCases.getProsecutionCaseId();
        final UUID linkGroupId = UUID.randomUUID();

        linkCases.getCaseUrns().forEach(caseUrnToLink -> {

            // get case ID from caseUrn
            final UUID caseIdToLink = UUID.fromString(searchCaseRepository.findByCaseUrn(caseUrnToLink.toUpperCase()).get(0).getCaseId());

            // need to insert 2 records, one for originating case, one for implicit case
            saveLSMEntity(leadCaseId, linkGroupId, caseIdToLink, LinkType.LINK);
            saveLSMEntity(caseIdToLink, linkGroupId, leadCaseId, LinkType.LINK);

        });

    }

    @Handles("progression.event.split-cases")
    public void splitCases(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.split-cases {} ", event.toObfuscatedDebugString());
        }

        final SplitCases splitCases = jsonObjectConverter.convert(event.payloadAsJsonObject(), SplitCases.class);

        final UUID caseId = splitCases.getProsecutionCaseId();
        final UUID splitGroupId = UUID.randomUUID();
        String caseReferences = String.join(",", splitCases.getCaseUrns());

        final List<CaseLinkSplitMergeEntity> splitEntity = caseLinkSplitMergeRepository.findByCaseIdAndLinkedCaseIdAndType(caseId, caseId, LinkType.SPLIT);
        if (!splitEntity.isEmpty()) {
            // if there is previous record, append reference
            caseReferences = caseReferences.concat(",").concat(splitEntity.get(0).getReference());
            updateSplitEntity(splitEntity.get(0), caseReferences);
        } else {
            // if splitting for the first time, save all references comma separated in a single entity
            saveLSMEntity(caseId, splitGroupId, caseId, LinkType.SPLIT, caseReferences);
        }


    }

    @Handles("progression.event.merge-cases")
    public void mergeCases(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.merge-cases {} ", event.toObfuscatedDebugString());
        }

        final MergeCases mergeCases = jsonObjectConverter.convert(event.payloadAsJsonObject(), MergeCases.class);
        final UUID leadCaseId = mergeCases.getProsecutionCaseId();
        final UUID mergeGroupId = UUID.randomUUID();
        final String leadCaseUrn = searchCaseRepository.findByCaseId(leadCaseId.toString()).get(0).getReference();

        mergeCases.getCaseUrns().forEach(caseUrnToMerge -> {
                    // get case ID from caseUrn
            final UUID caseIdToMerge = UUID.fromString(searchCaseRepository.findByCaseUrn(caseUrnToMerge.toUpperCase()).get(0).getCaseId());

                    // insert the originating merges
                    saveLSMEntity(leadCaseId, mergeGroupId, caseIdToMerge, LinkType.MERGE, caseUrnToMerge + "/M");
                    saveLSMEntity(caseIdToMerge, mergeGroupId, leadCaseId, LinkType.MERGE, leadCaseUrn);

                    // check for previous merged cases against caseUrnToMerge/M (excluding itself)
                    final List<CaseLinkSplitMergeEntity> previousMerges = caseLinkSplitMergeRepository.findPreviousMergesByReference(leadCaseId, caseUrnToMerge + "/M");
                    previousMerges.forEach(previouslyMergedEntity -> {
                        saveLSMEntity(previouslyMergedEntity.getCaseId(), mergeGroupId, leadCaseId, LinkType.MERGE, leadCaseUrn);
                        saveLSMEntity(leadCaseId, mergeGroupId, previouslyMergedEntity.getCaseId(), LinkType.MERGE, searchCaseRepository.findByCaseId(previouslyMergedEntity.getCaseId().toString()).stream().findFirst().get().getReference());
                    });

                }

        );
    }

    private void saveLSMEntity(final UUID case1, final UUID linkGroupId, final UUID case2, final LinkType linkType) {
        saveLSMEntity(case1, linkGroupId, case2, linkType, "");
    }

    private void saveLSMEntity(final UUID case1, final UUID linkGroupId, final UUID case2, final LinkType linkType, final String reference) {
        final CaseLinkSplitMergeEntity entity = new CaseLinkSplitMergeEntity();
        // first save for the originating case
        entity.setId(UUID.randomUUID());
        entity.setCaseId(case1);
        entity.setLinkedCaseId(case2);
        entity.setType(linkType);
        entity.setLinkGroupId(linkGroupId);
        if (!reference.isEmpty()) {
            entity.setReference(reference);
        }
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(case1);
        entity.setLinkedCase(prosecutionCaseEntity);

        caseLinkSplitMergeRepository.save(entity);
    }

    private void updateSplitEntity(final CaseLinkSplitMergeEntity entity, final String reference) {
        entity.setReference(reference);
        caseLinkSplitMergeRepository.save(entity);
    }
}
