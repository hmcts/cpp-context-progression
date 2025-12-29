package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.api.service.RecordSheetService;
import uk.gov.moj.cpp.progression.query.api.service.CourtOrderService;
import uk.gov.moj.cpp.progression.query.api.service.OrganisationService;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.query.api.helper.ProgressionQueryHelper.addProperty;

@ServiceComponent(Component.QUERY_API)
public class ProsecutionCaseQueryApi {
    private static final String ORGANISATION_NAME = "name";
    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_2 = "addressLine2";
    private static final String ADDRESS_LINE_3 = "addressLine3";
    private static final String ADDRESS_LINE_4 = "addressLine4";
    private static final String ADDRESS_POSTCODE = "addressPostcode";
    private static final String DEFENDANTS = "defendants";
    private static final String COURT_ORDERS = "courtOrders";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final String ID = "id";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ORGANISATION_ADDRESS = "organisationAddress";
    private static final String CASE_ID = "caseId";
    private static final String REPRESENTATION = "representation";
    public static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";
    public static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";
    public static final String PROGRESSION_QUERY_PROSECUTION_CASE = "progression.query.prosecutioncase";
    public static final String RECORD_SHEET = "RecordSheet";
    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;
    @Inject
    private Requester requester;

    @Inject
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Inject
    private OrganisationService organisationService;

    @Inject
    private RecordSheetService recordSheetService;
    @Inject
    private CourtOrderService courtOrderService;

    @Inject
    private UsersGroupQueryService usersGroupQueryService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.query.prosecutioncase-details")
    public JsonEnvelope getCaseProsecutionCaseDetails(final JsonEnvelope query) {
        return prosecutionCaseQuery.getProsecutionCaseDetails(query);
    }

    @Handles("progression.query.mastercase-details")
    public JsonEnvelope getProsecutionMasterCaseDetails(final JsonEnvelope query) {
        return prosecutionCaseQuery.getProsecutionMasterCaseDetails(query);
    }

    @Handles("progression.query.group-member-cases")
    public JsonEnvelope geGroupMemberCases(final JsonEnvelope query) {
        return prosecutionCaseQuery.getGroupMemberCases(query);
    }

    @SuppressWarnings("squid:S3655")
    @Handles("progression.query.prosecutioncase")
    public JsonEnvelope getCaseProsecutionCase(final JsonEnvelope query) {

        final JsonEnvelope appQueryResponse = prosecutionCaseQuery.getProsecutionCase(query);
        final JsonObject queryViewPayload = appQueryResponse.payloadAsJsonObject();
        final JsonObject prosecutionCase = appQueryResponse.payloadAsJsonObject().getJsonObject("prosecutionCase");

        if (nonNull(prosecutionCase)) {
            final JsonArray defendants = prosecutionCase.getJsonArray(DEFENDANTS);
            final JsonArrayBuilder activeCourtOrdersArrayBuilder = JsonObjects.createArrayBuilder();

            final Set<UUID> uniqueMasterDefendantIds = defendants.stream()
                    .map(defendant -> UUID.fromString(((JsonObject) defendant).getString(MASTER_DEFENDANT_ID)))
                    .collect(Collectors.toSet());

            uniqueMasterDefendantIds.forEach(masterDefendantId -> {
                final JsonObject courtOrders = courtOrderService.getCourtOrdersByDefendant(query, masterDefendantId, requester);
                if (nonNull(courtOrders) && courtOrders.containsKey(COURT_ORDERS)) {
                    final JsonArray activeCourtOrders = courtOrders.getJsonArray(COURT_ORDERS);
                    if (!activeCourtOrders.isEmpty()) {
                        final JsonObjectBuilder objectBuilder = JsonObjects.createObjectBuilder()
                                .add(MASTER_DEFENDANT_ID, masterDefendantId.toString())
                                .add(COURT_ORDERS, activeCourtOrders);
                        activeCourtOrdersArrayBuilder.add(objectBuilder.build());
                    }
                }
            });

            final JsonArray activeCourtOrders = activeCourtOrdersArrayBuilder.build();
            if (!activeCourtOrders.isEmpty()) {
                return envelopeFrom(query.metadata(), addProperty(queryViewPayload, "activeCourtOrders", activeCourtOrders));
            }
        }
        return appQueryResponse;
    }

    @Handles("progression.query.prosecutioncase-v2")
    public JsonEnvelope getCaseProsecutionCaseV2(final JsonEnvelope query) {
        return prosecutionCaseQuery.getProsecutionCase(query);
    }

    @Handles("progression.query.prosecutioncase.caag")
    public JsonEnvelope getProsecutionCaseForCaseAtAGlance(final JsonEnvelope query) {
        final JsonEnvelope appQueryResponse = prosecutionCaseQuery.getProsecutionCaseForCaseAtAGlance(query);
        final JsonObject payload = appQueryResponse.payloadAsJsonObject();
        final JsonArray defendants = payload.getJsonArray(DEFENDANTS);
        final JsonArrayBuilder caagDefendantsBuilder = JsonObjects.createArrayBuilder();
        if (nonNull(defendants)) {
            final JsonObject associatedCaseDefendants = organisationService.getAssociatedCaseDefendantsWithOrganisationAddress(query, payload.getString(CASE_ID), requester);
            final JsonArray associatedDefendants = associatedCaseDefendants.getJsonArray(DEFENDANTS);
            defendants.stream().map(x -> (JsonObject) x)
                    .forEach(caagDefendant -> {
                        final Optional<JsonObject> matchingCaseDefendant = associatedDefendants.stream().map(x -> (JsonObject) x)
                                .filter(cd -> caagDefendant.getString(ID).equals(cd.getString(DEFENDANT_ID))).findFirst();

                        if (matchingCaseDefendant.isPresent() && nonNull(matchingCaseDefendant.get().getJsonObject(ORGANISATION_ADDRESS))) {
                            final JsonObject representation = createOrganisation(matchingCaseDefendant.get());
                            caagDefendantsBuilder.add(addProperty(caagDefendant, REPRESENTATION, representation));
                        } else {
                            caagDefendantsBuilder.add(caagDefendant);
                        }
                    });
            final JsonObject resultPayload = addProperty(payload, DEFENDANTS, caagDefendantsBuilder.build());
            return envelopeFrom(query.metadata(), resultPayload);
        }
        return appQueryResponse;
    }

    @Handles("progression.query.case")
    public JsonEnvelope getProsecutionCase(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCase(query);
    }

    @Handles("progression.query.casehearings")
    public JsonEnvelope getCaseHearings(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCaseHearings(query);
    }

    @Handles("progression.query.case-hearings-for-court-extract")
    public JsonEnvelope getCaseHearingsForCourtExtract(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCaseHearingsForCourtExtract(query);
    }

    @Handles("progression.query.case-defendant-hearings")
    public JsonEnvelope getCaseDefendantHearings(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCaseDefendantHearings(query);
    }

    @Handles("progression.query.usergroups-by-material-id")
    public JsonEnvelope searchForUserGroupsByMaterialId(final JsonEnvelope query) {
        return this.prosecutionCaseQuery.searchByMaterialId(query);
    }


    @Handles("progression.query.search-cases")
    public JsonEnvelope searchCaseProsecutionCase(final JsonEnvelope query) {
        return prosecutionCaseQuery.searchCase(query);
    }

    @Handles("progression.query.prosecutionauthorityid-by-case-id")
    public JsonEnvelope searchProsecutionAuthorityIdByCaseId(final JsonEnvelope query) {
        return prosecutionCaseQuery.searchProsecutionAuthorityId(query);
    }

    @Handles("progression.query.prosecutorid-prosecutionauthorityid-by-case-id")
    public JsonEnvelope searchProsecutorIdProsecutionAuthorityIdByCaseId(final JsonEnvelope query) {
        return prosecutionCaseQuery.searchProsecutorIdProsecutionAuthorityId(query);
    }

    @Handles("progression.query.search-cases-by-caseurn")
    public JsonEnvelope searchCaseByUrn(final JsonEnvelope query) {
        return prosecutionCaseQuery.searchCaseByCaseUrn(query);
    }

    @Handles("progression.query.case-exist-by-caseurn")
    public JsonEnvelope searchCaseExistsByCaseUrn(final JsonEnvelope query) {
        return prosecutionCaseQuery.caseExistsByCaseUrn(query);
    }


    /**
     * Handler returns document details and not document content. This is consequence of non
     * framework endpoint which uses standard framework interceptors. Handler is invoked at the end
     * of programmatically invoked interceptor chain, see DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.
     */
    @Handles("progression.query.court-extract")
    public JsonEnvelope getCourtExtract(final JsonEnvelope query) {

        return query;
    }

    @Handles("progression.query.record-sheet")
    public JsonEnvelope getRecordSheet(final JsonEnvelope query) {
        final UUID userId = getUserId();
        JsonEnvelope documentQuery = getDocumentQuery(query, userId);
        return recordSheetService.getTrialRecordSheetPayload(query, getCaseProsecutionCase(documentQuery), userId );
    }

    @Handles("progression.query.record-sheet-for-application")
    public JsonEnvelope getRecordSheetForApplication(final JsonEnvelope query) {
        final UUID userId = getUserId();
        final JsonEnvelope documentQuery = getDocumentQuery(query, userId);
        return recordSheetService.getTrialRecordSheetPayloadForApplication(query, getCaseProsecutionCase(documentQuery), userId);
    }


    private JsonEnvelope getDocumentQuery(final JsonEnvelope query, final UUID userId) {
        final JsonObject payloadAsJsonObject = query.payloadAsJsonObject();
        String caseId = payloadAsJsonObject.containsKey(CASE_ID) ? payloadAsJsonObject.getString(CASE_ID) : null;

        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROGRESSION_QUERY_PROSECUTION_CASE)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add(CASE_ID, caseId)
                        .add("template", RECORD_SHEET)
                        .build()
        );
    }

    private UUID getUserId() {
        final Optional<UUID> systemUserOptional = serviceContextSystemUserProvider.getContextSystemUserId();
        return systemUserOptional.isPresent() ? systemUserOptional.get() : null;
    }


    @Handles("progression.query.eject-case")
    public JsonEnvelope ejectCase(final JsonEnvelope query) {

        return query;
    }

    @Handles("progression.query.case.hearingtypes")
    public JsonEnvelope getCaseHearingTypes(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCaseHearingTypes(query);
    }

    @Handles("progression.query.cotr-trial-hearings")
    public JsonEnvelope getTrialHearings(final JsonEnvelope query){
        return prosecutionCaseQuery.getTrialHearings(query);
    }

    @Handles("progression.query.cotr-details")
    public JsonEnvelope getCotrDetails(final JsonEnvelope query){
        return prosecutionCaseQuery.getCotrDetails(query);
    }

    @Handles("progression.query.cotr-form")
    public JsonEnvelope getCotrForm(final JsonEnvelope query){
        return prosecutionCaseQuery.getCotrForm(query);
    }

    @Handles("progression.query.case.allhearingtypes")
    public JsonEnvelope getCaseAllHearingTypes(final JsonEnvelope query) {
        return prosecutionCaseQuery.getCaseAllHearingTypes(query);
    }

    private JsonObject createOrganisation(final JsonObject completeOrganisationDetails) {

        final JsonObject address = completeOrganisationDetails.getJsonObject(ORGANISATION_ADDRESS);
        return JsonObjects.createObjectBuilder().add(ORGANISATION_NAME, completeOrganisationDetails.getString("organisationName"))
                .add("address", JsonObjects.createObjectBuilder()
                        .add(ADDRESS_LINE_1, address.getString("address1"))
                        .add(ADDRESS_LINE_2, address.getString("address2"))
                        .add(ADDRESS_LINE_3, address.getString("address3"))
                        .add(ADDRESS_LINE_4, address.getString("address4"))
                        .add(ADDRESS_POSTCODE, address.getString(ADDRESS_POSTCODE))
                ).build();
    }

    @Handles("progression.query.cotr.details.prosecutioncase")
    public JsonEnvelope getCotrDetailsProsecutionCase(final JsonEnvelope query){
        return prosecutionCaseQuery.getCotrDetailsByCaseId(query);
    }

    @Handles("progression.query.case.allhearings")
    public JsonEnvelope getAllCaseHearings(final JsonEnvelope query){
        return prosecutionCaseQuery.getAllCaseHearings(query);
    }

    @Handles("progression.query.active-applications-on-case")
    public JsonEnvelope getActiveApplicationsOnCase(final JsonEnvelope envelope) {
        return prosecutionCaseQuery.getActiveApplicationsOnCase(envelope);
    }

}