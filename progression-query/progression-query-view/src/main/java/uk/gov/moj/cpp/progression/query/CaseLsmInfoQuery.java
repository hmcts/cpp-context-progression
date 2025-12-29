package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.query.utils.CaseLsmInfoConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseLinkSplitMergeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseLinkSplitMergeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class CaseLsmInfoQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseLsmInfoQuery.class);
    public static final String PARAM_CASE_ID = "caseId";

    private static final String CASE_ID = "caseId";
    private static final String CASE_URN = "caseUrn";
    private static final String RELATED_URN = "relatedUrn";
    private static final String LINK_GROUP_ID = "linkGroupId";
    private static final String MATCHED_MASTER_DEFENDANT_ID = "matchedMasterDefendantId";
    private static final String DEFENDANTS = "defendants";
    private static final String MATCHED_DEFENDANT_CASES = "matchedDefendantCases";
    private static final String LINKED_CASES = "linkedCases";
    private static final String MERGED_CASES = "mergedCases";
    private static final String SPLIT_CASES = "splitCases";

    @Inject
    private CaseLsmInfoConverter caseLsmInfoConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private CaseLinkSplitMergeRepository caseLinkSplitMergeRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.query.case-lsm-info")
    public JsonEnvelope getCaseLsmInfo(final JsonEnvelope envelope) {
        final UUID caseId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), PARAM_CASE_ID)
                .orElseThrow(() -> new IllegalArgumentException("caseId parameter cannot be empty!"));

        final JsonObjectBuilder responseBuilder = JsonObjects.createObjectBuilder();

        //MATCHED DEFENDANTS
        try {
            final List<UUID> masterDefendantIds = retrieveMasterDefendantIdList(caseId);
            final List<MatchDefendantCaseHearingEntity> matchedCases = matchDefendantCaseHearingRepository.findByMasterDefendantId(masterDefendantIds);
            final HashSet<MatchDefendantCaseHearingEntity> uniqueMatchedCases = new HashSet<>(matchedCases);

            if (!uniqueMatchedCases.isEmpty()) {
                //There will be 2 records for case one with hearing id and other null, the logic below removes the record with null hearing id,
                //as the query does't need to return record with null hearing id.
                final List<MatchDefendantCaseHearingEntity> nullHearingIdDefendantCaseHearing = uniqueMatchedCases.stream()
                        .filter(matchDefendantCaseHearingEntity -> Objects.isNull(matchDefendantCaseHearingEntity.getHearingId())).collect(Collectors.toList());
                final List<UUID> defendantCaseHearingEntityToBeRemoved = nullHearingIdDefendantCaseHearing.stream().filter(e ->
                        uniqueMatchedCases.stream().filter(matchDefendantCaseHearingEntity -> matchDefendantCaseHearingEntity.getDefendantId().equals(e.getDefendantId())
                                && matchDefendantCaseHearingEntity.getMasterDefendantId().equals(e.getMasterDefendantId())
                                && matchDefendantCaseHearingEntity.getProsecutionCaseId().equals(e.getProsecutionCaseId())).count() > 1
                ).map(e -> e.getId()).collect(Collectors.toList());
                defendantCaseHearingEntityToBeRemoved.stream().forEach(e ->
                        uniqueMatchedCases.removeIf(matchDefendantCaseHearingEntity -> matchDefendantCaseHearingEntity.getId().equals(e))
                );
                final JsonArrayBuilder matchedCasesArrayBuilder = JsonObjects.createArrayBuilder();
                uniqueMatchedCases.stream().forEach(e -> matchedCasesArrayBuilder.add(buildMatchedDefendantCase(e.getProsecutionCase(), e.getMasterDefendantId(), Optional.ofNullable(e.getHearing()))));
                responseBuilder.add(MATCHED_DEFENDANT_CASES, matchedCasesArrayBuilder);
            }
        } catch (final NoResultException e) {
            LOGGER.warn("# No case found yet for caseId '{}'", caseId, e);
        }

        //RELATED CASES
        final Map<LinkType, List<CaseLinkSplitMergeEntity>> linkSplitMergeCases = caseLinkSplitMergeRepository.findByCaseId(caseId).stream()
                .collect(Collectors.groupingBy(CaseLinkSplitMergeEntity::getType));

        linkSplitMergeCases.entrySet().stream().forEach(entry -> buildRelatedCases(responseBuilder, entry.getKey(), entry.getValue()));

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                responseBuilder.build());
    }

    private void buildRelatedCases(final JsonObjectBuilder responseBuilder, final LinkType linkType, final List<CaseLinkSplitMergeEntity> linkedCases) {
        final JsonArrayBuilder linkedCasesArrayBuilder = JsonObjects.createArrayBuilder();
        linkedCases.stream().forEach(e -> linkedCasesArrayBuilder.add(buildRelatedCase(e.getLinkedCase(), e.getType(), Optional.ofNullable(e.getReference()), e.getLinkGroupId(), getHearingByCaseId(e.getLinkedCaseId()))));
        responseBuilder.add(getLinkTitle(linkType), linkedCasesArrayBuilder);
    }

    private String getLinkTitle(final LinkType linkType) {
        final String title;
        switch (linkType) {
            case LINK:
                title = LINKED_CASES;
                break;
            case MERGE:
                title = MERGED_CASES;
                break;
            case SPLIT:
                title = SPLIT_CASES;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown link type (%s)", linkType));
        }
        return title;
    }

    private Optional<HearingEntity> getHearingByCaseId(final UUID caseId) {
        return caseDefendantHearingRepository.findByCaseId(caseId).stream()
                .map(CaseDefendantHearingEntity::getHearing)
                .filter(h -> h.getListingStatus() == HearingListingStatus.HEARING_INITIALISED)
                .findFirst();
    }

    private JsonObjectBuilder buildMatchedDefendantCase(final ProsecutionCaseEntity prosecutionCaseEntity, final UUID matchedMasterDefendantId, final Optional<HearingEntity> hearingEntity) {
        final ProsecutionCase prosecutionCase = convertToProsecutionCase(prosecutionCaseEntity);
        final Hearing hearing = convertToHearing(hearingEntity);

        final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder()
                .add(CASE_ID, prosecutionCase.getId().toString())
                .add(CASE_URN, extractCaseUrn(prosecutionCase))
                .add(MATCHED_MASTER_DEFENDANT_ID, matchedMasterDefendantId.toString())
                .add(DEFENDANTS, caseLsmInfoConverter.convertMatchedCaseDefendants(prosecutionCase.getDefendants(), hearing, matchedMasterDefendantId));

        if (nonNull(prosecutionCase.getRelatedUrn())) {
            jsonObjectBuilder.add(RELATED_URN, prosecutionCase.getRelatedUrn());
        }

        return jsonObjectBuilder;
    }

    private JsonObjectBuilder buildRelatedCase(final ProsecutionCaseEntity prosecutionCaseEntity, final LinkType linkType, final Optional<String> linkReference, final UUID linkGroupId, final Optional<HearingEntity> hearingEntity) {
        final ProsecutionCase prosecutionCase = convertToProsecutionCase(prosecutionCaseEntity);
        final Hearing hearing = convertToHearing(hearingEntity);

        final String caseUrn;
        if (linkReference.isPresent() && (linkType == LinkType.MERGE || linkType == LinkType.SPLIT)) {
            caseUrn = linkReference.get();
        } else {
            caseUrn = extractCaseUrn(prosecutionCase);
        }

        return JsonObjects.createObjectBuilder()
                .add(CASE_ID, prosecutionCase.getId().toString())
                .add(CASE_URN, caseUrn)
                .add(LINK_GROUP_ID, linkGroupId.toString())
                .add(DEFENDANTS, caseLsmInfoConverter.convertRelatedCaseDefendants(prosecutionCase.getDefendants(), hearing));
    }

    private String extractCaseUrn(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseIdentifier pci = prosecutionCase.getProsecutionCaseIdentifier();
        if (pci.getCaseURN() != null) {
            return pci.getCaseURN();
        } else {
            return pci.getProsecutionAuthorityReference();
        }
    }

    private Hearing convertToHearing(final Optional<HearingEntity> hearingEntity) {
        if (!hearingEntity.isPresent()) {
            return null;
        }

        final JsonObject hearingPayload = stringToJsonObjectConverter.convert(hearingEntity.get().getPayload());
        return jsonObjectToObjectConverter.convert(hearingPayload, Hearing.class);
    }

    private ProsecutionCase convertToProsecutionCase(final ProsecutionCaseEntity prosecutionCaseEntity) {
        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        return jsonObjectToObjectConverter.convert(prosecutionCasePayload, ProsecutionCase.class);
    }

    private List<UUID> retrieveMasterDefendantIdList(final UUID caseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCasePayload, ProsecutionCase.class);
        return prosecutionCase.getDefendants().stream()
                .map(Defendant::getMasterDefendantId)
                .collect(Collectors.toList());
    }

}
