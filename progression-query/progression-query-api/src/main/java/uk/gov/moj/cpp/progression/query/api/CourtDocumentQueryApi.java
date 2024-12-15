package uk.gov.moj.cpp.progression.query.api;


import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;
import static uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery.APPEALS_LODGED;
import static uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery.APPEALS_LODGED_INFO;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.progression.query.ApplicationQueryView;
import uk.gov.moj.cpp.progression.query.CourtDocumentQueryView;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.SharedCourtDocumentsQueryView;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentQueryApi {

    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENTS_SEARCH_NAME_ALL = "progression.query.courtdocuments-all";
    public static final String COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME = "progression.query.courtdocuments.with.pagination";
    public static final String COURT_DOCUMENTS_SEARCH_DEFENCE = "progression.query.courtdocuments.for.defence";
    public static final String COURT_DOCUMENTS_SEARCH_PROSECUTION = "progression.query.courtdocuments.for.prosecution";
    public static final String COURT_DOCUMENT_PROSECUTION_NOTIFICATION_STATUS = "progression.query.prosecution.notification-status";
    public static final String COURT_DOCUMENT_APPLICATION_NOTIFICATION_STATUS = "progression.query.application.notification-status";
    private static final String HEARING_ID = "hearingId";
    static final String CASE_ID = "caseId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String APPLICATION_ID = "applicationId";
    public static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";
    public static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";

    @Inject
    private Requester requester;

    @Inject
    private CourtDocumentQueryView courtDocumentQueryView;

    @Inject
    private ApplicationQueryView applicationQueryView;

    @Inject
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private DefenceQueryService defenceQueryService;

    @Inject
    private HearingDetailsLoader hearingDetailsLoader;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private UsersGroupQueryService usersGroupQueryService;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ProsecutionCaseQuery prosecutionCaseQuery;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentQueryApi.class);

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope query) {
        return courtDocumentQueryView.getCourtDocument(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchCourtDocuments(final JsonEnvelope query) {
        final Optional<UserGroupsDetails> magsGroup = getMagistratesGroup(query);
        final Optional<HearingDetails> hearingDetails = getHearingDetails(query);

        if (magsGroup.isPresent() && hearingDetails.isPresent() && isSharedCourtDocumentsQuery(query, hearingDetails.get())) {
            return requestSharedCourtDocuments(query, magsGroup.get().getGroupId().toString());
        }

        return courtDocumentQueryView.searchCourtDocuments(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME_ALL)
    public JsonEnvelope searchCourtDocumentsAll(final JsonEnvelope query) {
        return courtDocumentQueryView.searchCourtDocumentsAll(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME)
    public JsonEnvelope searchCourtDocumentsWithPagination(final JsonEnvelope query) {
        return courtDocumentQueryView.searchCourtDocumentsWithPagination(query);
    }


    @Handles(COURT_DOCUMENTS_SEARCH_DEFENCE)
    public JsonEnvelope searchCourtDocumentsForDefence(final JsonEnvelope query) {
        if (!query.payloadAsJsonObject().containsKey(CASE_ID) && !query.payloadAsJsonObject().containsKey(APPLICATION_ID)) {
            throw new BadRequestException(String.format("%s no search parameter specified ", COURT_DOCUMENTS_SEARCH_DEFENCE));
        }

        final Metadata metadata = metadataFrom(query.metadata())
                .withName(COURT_DOCUMENTS_SEARCH_NAME)
                .build();

        if (query.payloadAsJsonObject().containsKey(CASE_ID)) {
            final List<UUID> defendantList = defenceQueryService.getDefendantList(query, query.payloadAsJsonObject().getString(CASE_ID));
            final List<CourtDocumentIndex> finalDocumentList = new ArrayList<>();
            defendantList.forEach(defendantId -> {
                final Courtdocuments courtdocuments = getCourtDocument(query, metadata, defendantId);
                if (nonNull(courtdocuments) && isNotEmpty(courtdocuments.getDocumentIndices())) {
                    final List<CourtDocumentIndex> filteredList = getFilteredList(courtdocuments.getDocumentIndices(), finalDocumentList);
                    if (isNotEmpty(filteredList)) {
                        finalDocumentList.addAll(filteredList);
                    }

                }
            });

            removeDefenceOnlyDocumentsIfAppealLodged(query, finalDocumentList);

            final JsonObject resultJson = objectToJsonObjectConverter.convert(Courtdocuments.courtdocuments().withDocumentIndices(finalDocumentList).build());
            return envelopeFrom(query.metadata(), resultJson);
        } else { // for applicationId
            return courtDocumentQueryView.searchCourtDocuments(envelopeFrom(metadata, query.payloadAsJsonObject()));
        }

    }

    private void removeDefenceOnlyDocumentsIfAppealLodged(final JsonEnvelope query, final List<CourtDocumentIndex> finalDocumentList) {
        final JsonEnvelope caagEnvelope = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(query);
        final JsonObject caagObject = caagEnvelope.payloadAsJsonObject();
        if (nonNull(caagObject) && caagObject.containsKey(APPEALS_LODGED_INFO)
                && caagObject.getJsonObject(APPEALS_LODGED_INFO).containsKey(APPEALS_LODGED)
                && caagObject.getJsonObject(APPEALS_LODGED_INFO).getBoolean(APPEALS_LODGED)) {

            final List<DocumentTypeAccessReferenceData> defenceOnlyDTA = referenceDataService.getDocumentsTypeAccess()
                    .stream().filter(dta -> nonNull(dta.getDefenceOnly()) && TRUE.equals(dta.getDefenceOnly())).toList();

            finalDocumentList.removeIf(doc -> defenceOnlyDTA.stream()
                    .anyMatch(dta -> isNotEmpty(dta.getDocumentCategory()) && isNotEmpty(doc.getCategory()) && dta.getDocumentCategory().equals(doc.getCategory())));
        }
    }

    public List<CourtDocumentIndex> getFilteredList(final List<CourtDocumentIndex> fetchedDocumentIndices, final List<CourtDocumentIndex> existingDocumentList) {
        return fetchedDocumentIndices.stream()
                .filter(newCourtDocumentIndex ->
                        !("case level".equalsIgnoreCase(newCourtDocumentIndex.getCategory()) &&
                                existingDocumentList.stream().anyMatch(existingDocumentIndex -> existingDocumentIndex.getDocument().getCourtDocumentId().equals(newCourtDocumentIndex.getDocument().getCourtDocumentId())))
                ).collect(toList());
    }

    private Courtdocuments getCourtDocument(final JsonEnvelope query, final Metadata metadata, final UUID defendantId) {
        final JsonEnvelope responseEnvelope = courtDocumentQueryView.searchCourtDocuments(envelopeFrom(metadata, getEnrichedQueryPayload(query, defendantId)));
        return jsonObjectToObjectConverter.convert(responseEnvelope.payloadAsJsonObject(), Courtdocuments.class);
    }

    private JsonObject getEnrichedQueryPayload(final JsonEnvelope query, final UUID defendantId) {
        final JsonObjectBuilder enrichedQueryDocumentBuilder = uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder(query.payloadAsJsonObject());
        enrichedQueryDocumentBuilder.add(DEFENDANT_ID, defendantId.toString());
        return enrichedQueryDocumentBuilder.build();
    }


    @SuppressWarnings("squid:S3655")
    @Handles(COURT_DOCUMENTS_SEARCH_PROSECUTION)
    public JsonEnvelope searchCourtDocumentsForProsecution(final JsonEnvelope query) {

        boolean isProsecutingCase = true;

        if (!query.payloadAsJsonObject().containsKey(CASE_ID) && !query.payloadAsJsonObject().containsKey(APPLICATION_ID)) {
            throw new BadRequestException(String.format("%s no search parameter specified ", COURT_DOCUMENTS_SEARCH_PROSECUTION));
        }
        final UUID userId = query.metadata().userId().isPresent() ? fromString(query.metadata().userId().get()) : null;

        final JsonEnvelope appQueryResponse = prosecutionCaseQuery.getProsecutionCase(query);
        final JsonObject prosecutionCase = appQueryResponse.payloadAsJsonObject().getJsonObject("prosecutionCase");

        final ProsecutionCase prosecutionCaseObj = jsonObjectToObjectConverter.convert(prosecutionCase, ProsecutionCase.class);
        final String shortName = nonNull(prosecutionCaseObj.getProsecutor()) && nonNull(prosecutionCaseObj.getProsecutor().getProsecutorCode()) ? prosecutionCaseObj.getProsecutor().getProsecutorCode() : prosecutionCaseObj.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        final Optional<String> orgMatch = usersGroupQueryService.validateNonCPSUserOrg(query.metadata(), userId, NON_CPS_PROSECUTORS, shortName);

        if (orgMatch.isPresent()) {
            if (ORGANISATION_MIS_MATCH.equals(orgMatch.get())) {
                throw new ForbiddenRequestException("Forbidden!! Non CPS Prosecutor user cannot view court documents if it is not belongs to the same Prosecuting Authority of the user logged in");
            }
        } else {
            isProsecutingCase = defenceQueryService.isUserProsecutingCase(query, query.payloadAsJsonObject().getString(CASE_ID));
        }

        if (!query.payloadAsJsonObject().containsKey(APPLICATION_ID) && !isProsecutingCase) {
            throw new ForbiddenRequestException("Forbidden!! Cannot view court documents, user not prosecuting the case");
        }

        final Metadata metadata = metadataFrom(query.metadata())
                .withName(COURT_DOCUMENTS_SEARCH_NAME)
                .build();

        return courtDocumentQueryView.searchCourtDocuments(envelopeFrom(metadata, getUpdatedQueryPayload(query.payloadAsJsonObject(), isProsecutingCase)));

    }


    @Handles(COURT_DOCUMENT_PROSECUTION_NOTIFICATION_STATUS)
    public JsonEnvelope getCaseNotificationStatus(final JsonEnvelope query) {
        return courtDocumentQueryView.getCaseNotifications(query);
    }

    @Handles(COURT_DOCUMENT_APPLICATION_NOTIFICATION_STATUS)
    public JsonEnvelope getApplicationNotificationStatus(final JsonEnvelope query) {
        return applicationQueryView.getApplicationNotifications(query);
    }

    private Optional<UserGroupsDetails> getMagistratesGroup(final JsonEnvelope query) {
        final Optional<String> userId = query.metadata().userId();
        if (userId.isPresent()) {
            final List<UserGroupsDetails> groups = userDetailsLoader.getGroupsUserBelongsTo(requester, fromString(userId.get()));
            return groups.stream().filter(group -> "Magistrates".equals(group.getGroupName())).findAny();
        }
        return empty();
    }

    private Optional<HearingDetails> getHearingDetails(final JsonEnvelope query) {
        if (query.payloadAsJsonObject().containsKey(HEARING_ID)) {
            return ofNullable(hearingDetailsLoader.getHearingDetails(requester, fromString(query.payloadAsJsonObject().getString(HEARING_ID))));
        }
        return empty();
    }

    private boolean isSharedCourtDocumentsQuery(final JsonEnvelope query, final HearingDetails hearingDetails) {
        final Optional<String> userId = query.metadata().userId();
        return userId.isPresent() && isTrialHearing(query, hearingDetails) && isMagUserInHearing(userId.get(), hearingDetails);
    }

    private boolean isMagUserInHearing(final String userId, final HearingDetails hearingDetails) {
        return hearingDetails.getUserIds().contains(fromString(userId));
    }

    private boolean isTrialHearing(final JsonEnvelope query, final HearingDetails hearingDetails) {
        final Map<UUID, ReferenceDataService.ReferenceHearingDetails> hearingTypes = referenceDataService.getHearingTypes(query);

        if (!hearingTypes.containsKey(hearingDetails.getHearingTypeId())) {
            return false;
        }
        final ReferenceDataService.ReferenceHearingDetails referenceHearingDetails = hearingTypes.get(hearingDetails.getHearingTypeId());

        final List<ReferenceDataService.ReferenceHearingDetails> hearingsOfTypeTrial = hearingTypes.values().stream()
                .filter(ReferenceDataService.ReferenceHearingDetails::getTrialTypeFlag)
                .collect(toList());

        return hearingsOfTypeTrial.stream().anyMatch(type -> type.getHearingTypeCode().equals(referenceHearingDetails.getHearingTypeCode()));
    }

    private JsonEnvelope requestSharedCourtDocuments(final JsonEnvelope query, final String magsGroupId) {
        final JsonObject withGroupId = createObjectBuilder()
                .add(HEARING_ID, query.payloadAsJsonObject().getString(HEARING_ID))
                .add("userGroupId", magsGroupId)
                .add(CASE_ID, query.payloadAsJsonObject().getString(CASE_ID))
                .add(DEFENDANT_ID, query.payloadAsJsonObject().getString(DEFENDANT_ID))
                .build();
        final Metadata metadata = metadataFrom(query.metadata())
                .withName("progression.query.shared-court-documents")
                .build();
        return sharedCourtDocumentsQueryView.getSharedCourtDocuments(envelopeFrom(metadata, withGroupId));
    }

    private JsonObject getUpdatedQueryPayload(JsonObject payload, final boolean isProsecutingCase) {

        if (payload.containsKey(APPLICATION_ID)) {
            payload = removeProperty(payload, CASE_ID);
        }
        payload = addProperty(payload, "prosecutingCase", isProsecutingCase);

        LOGGER.info("payload is {}", payload);
        return payload;
    }

}
