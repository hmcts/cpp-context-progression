package uk.gov.moj.cpp.progression.query.api;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.isPermitted;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_API)
public class CourtDocumentQueryApi {

    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";
    public static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME = "progression.query.courtdocuments.with.pagination";
    public static final String COURT_DOCUMENTS_SEARCH_DEFENCE = "progression.query.courtdocuments.for.defence";
    public static final String COURT_DOCUMENT_PROSECUTION_NOTIFICATION_STATUS = "progression.query.prosecution.notification-status";
    public static final String COURT_DOCUMENT_APPLICATION_NOTIFICATION_STATUS = "progression.query.application.notification-status";
    private static final String HEARING_ID = "hearingId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";

    @Inject
    private Requester requester;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private HearingDetailsLoader hearingDetailsLoader;

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles(COURT_DOCUMENT_SEARCH_NAME)
    public JsonEnvelope getCourtDocument(final JsonEnvelope query) {

        return requester.request(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_NAME)
    public JsonEnvelope searchCourtDocuments(final JsonEnvelope query) {
        final Optional<UserGroupsDetails> magsGroup = getMagistratesGroup(query);
        final Optional<HearingDetails> hearingDetails = getHearingDetails(query);

        if (magsGroup.isPresent() && hearingDetails.isPresent() && isSharedCourtDocumentsQuery(query, hearingDetails.get())) {
            return requestSharedCourtDocuments(query, magsGroup.get().getGroupId().toString());
        }

        return requester.request(query);
    }

    @Handles(COURT_DOCUMENTS_SEARCH_WITH_PAGINATION_NAME)
    public JsonEnvelope searchCourtDocumentsWithPagination(final JsonEnvelope query) {
        return requester.request(query);
    }


    @Handles(COURT_DOCUMENTS_SEARCH_DEFENCE)
    public JsonEnvelope searchCourtDocumentsForDefence(final JsonEnvelope query) {
        if (!(query.payloadAsJsonObject().containsKey(CASE_ID) && query.payloadAsJsonObject().containsKey(DEFENDANT_ID))) {
            throw new BadRequestException(String.format("%s no search parameter specified ", COURT_DOCUMENTS_SEARCH_DEFENCE));
        }
        if(!isPermitted(query, userDetailsLoader, requester, query.payloadAsJsonObject().getString(DEFENDANT_ID))) {
            throw new ForbiddenRequestException("User has neither associated or granted permission to view");
        }

        final Metadata metadata = metadataFrom(query.metadata())
                .withName(COURT_DOCUMENTS_SEARCH_NAME)
                .build();

        return requester.request(envelopeFrom(metadata, query.payloadAsJsonObject()));
    }



    @Handles(COURT_DOCUMENT_PROSECUTION_NOTIFICATION_STATUS)
    public JsonEnvelope getCaseNotificationStatus(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles(COURT_DOCUMENT_APPLICATION_NOTIFICATION_STATUS)
    public JsonEnvelope getApplicationNotificationStatus(final JsonEnvelope query) {
        return requester.request(query);
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
                .collect(Collectors.toList());

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
        return requester.request(envelopeFrom(metadata, withGroupId));
    }

}
