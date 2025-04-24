package uk.gov.moj.cpp.progression.query.api;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.core.courts.AssignedUser;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.ApplicationHearingQueryView;
import uk.gov.moj.cpp.progression.query.ApplicationNotesQueryView;
import uk.gov.moj.cpp.progression.query.ApplicationQueryView;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@ServiceComponent(Component.QUERY_API)
@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ApplicationQueryApi {

    public static final String ASSIGNED_USER_FIELD_NAME = "assignedUser";
    public static final String LINKED_CASES = "linkedCases";
    private static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";
    private static final String GROUPS = "groups";
    public static final String GROUP_NAME = "groupName";

    @Inject
    private Requester requester;

    @Inject
    private ApplicationQueryView applicationQueryView;

    @Inject
    private ApplicationNotesQueryView applicationNotesQueryView;

    @Inject
    private ApplicationHearingQueryView applicationHearingQueryView;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    private DefenceQueryService defenceQueryService;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private UsersGroupQueryService usersGroupQueryService;

    @Handles("progression.query.application")
    public JsonEnvelope getApplication(final JsonEnvelope query) {
        final JsonEnvelope appQueryResponse = applicationQueryView.getApplication(query);
        JsonEnvelope response = appQueryResponse;
        final JsonObject jsonObject = appQueryResponse.payloadAsJsonObject();
        if (jsonObject.containsKey(ASSIGNED_USER_FIELD_NAME)) {
            final UUID id = fromString(jsonObject.getJsonObject(ASSIGNED_USER_FIELD_NAME).getString("userId"));
            final UserGroupsUserDetails userGroupsUserDetails = userDetailsLoader.getUserById(requester, query, id);
            final AssignedUser assignedUser = AssignedUser.assignedUser()
                    .withLastName(userGroupsUserDetails.getLastName())
                    .withFirstName(userGroupsUserDetails.getFirstName())
                    .withUserId(id)
                    .build();
            final JsonObject userJson = objectToJsonObjectConverter.convert(assignedUser);

            final JsonObjectBuilder newPayload = createObjectBuilder();
            appQueryResponse.payloadAsJsonObject().keySet().forEach(
                    keyName -> {
                        if (ASSIGNED_USER_FIELD_NAME.equals(keyName)) {
                            newPayload.add(keyName, userJson);
                        } else {
                            newPayload.add(keyName, appQueryResponse.payloadAsJsonObject().get(keyName));
                        }
                    }
            );

            response = enveloper.withMetadataFrom(appQueryResponse, "progression.query.application")
                    .apply(newPayload.build());
        }

        return response;
    }

    @Handles("progression.query.application-only")
    public JsonEnvelope getApplicationOnly(final JsonEnvelope query) {
        return applicationQueryView.getApplicationOnly(query);
    }

    @Handles("progression.query.application.summary")
    public JsonEnvelope getApplicationSummary(final JsonEnvelope query) {
        return applicationQueryView.getApplicationSummary(query);
    }

    @Handles("progression.query.application.aaag")
    public JsonEnvelope getCourtApplicationForApplicationAtAGlance(final JsonEnvelope query) {
        return applicationQueryView.getCourtApplicationForApplicationAtAGlance(query);
    }

    @Handles("progression.query.application.aaag-for-defence")
    @SuppressWarnings("squid:S3655")
    public JsonEnvelope getCourtApplicationForApplicationAtAGlanceForDefence(final JsonEnvelope query) {
        final Metadata metadata = metadataFrom(query.metadata())
                .withName("progression.query.application.aaag").build();
        final UUID userId = metadata.userId().isPresent() ? fromString(metadata.userId().get()) : null;
        final boolean isNonCPSUserWithValidProsecutingAuthority =  usersGroupQueryService.getUserGroups(metadata, userId).getJsonArray(GROUPS).getValuesAs(JsonObject.class).stream()
                .anyMatch(userGroup -> NON_CPS_PROSECUTORS.equals(userGroup.getString(GROUP_NAME)));


        final JsonEnvelope applicationAaagEnvelope = envelopeFrom(metadata, query.payload());
        final JsonEnvelope jsonEnvelope = applicationQueryView.getCourtApplicationForApplicationAtAGlance(applicationAaagEnvelope);
        if (!jsonEnvelope.payloadAsJsonObject().containsKey(LINKED_CASES)) {
            throw new ForbiddenRequestException("Cannot view application details, no linked cases found for the application");
        } else {
            final JsonArray linkedCases = jsonEnvelope.payloadAsJsonObject().getJsonArray(LINKED_CASES);
            if (!isNonCPSUserWithValidProsecutingAuthority && nonNull(linkedCases)) {
                final Optional<JsonValue> linkedCase = linkedCases.stream()
                        .filter(lc -> defenceQueryService.isUserProsecutingOrDefendingCase(query, ((JsonObject) lc).getString("prosecutionCaseId")))
                        .findFirst();
                if (!linkedCase.isPresent()) {
                    throw new ForbiddenRequestException("Cannot view application details, user is not prosecuting or defending the case");
                }
            }
        }
        return jsonEnvelope;
    }

    @Handles("progression.query.application-notes")
    public JsonEnvelope getApplicationNotes(final JsonEnvelope query) {
        return applicationNotesQueryView.getApplicationNotes(query);
    }

    @Handles("progression.query.applicationhearings")
    public JsonEnvelope getApplicationHearings(final JsonEnvelope query) {
        return applicationQueryView.getApplicationHearings(query);
    }

    @Handles("progression.query.court-proceedings-for-application")
    public JsonEnvelope getCourtProceedingsForApplication(final JsonEnvelope query) {
        return applicationQueryView.getCourtProceedingsForApplication(query);
    }

    @Handles("progression.query.case.status-for-application")
    public JsonEnvelope getCaseStatusForApplication(final JsonEnvelope query) {
        return applicationQueryView.getCaseStatusForApplication(query);
    }

    @Handles("progression.query.application-hearing-case-details")
    public JsonEnvelope getApplicationHearingCaseDetails(final JsonEnvelope query) {
        return applicationHearingQueryView.getApplicationHearingCaseDetails(query);

    }

}
