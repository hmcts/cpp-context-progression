package uk.gov.moj.cpp.progression.service;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQueryService.class);
    public static final String CASE_ID = "caseId";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase-v2";
    private static final String PROGRESSION_QUERY_ALL_CASE_HEARINGS = "progression.query.case.allhearings";
    private static final String ALL_CASE_HEARINGS = "allCaseHearings";
    private static final String HEARINGS = "hearings";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    private static final String HEARING_ID = "id";

    @Inject
    private Enveloper enveloper;


    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;


    public Optional<JsonObject> getProsecutionCase(final JsonEnvelope envelope, final String caseId) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} , Get prosecution case detail request {}", caseId, requestParameter);

        final JsonEnvelope prosecutioncase = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_PROSECUTION_CASES)
                .apply(requestParameter));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutioncase.toObfuscatedDebugString());
        }

        if (!prosecutioncase.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(prosecutioncase.payloadAsJsonObject());
        }
        return result;
    }

    public List<UUID> getAllHearingIdsForCase(final JsonEnvelope envelope, final UUID caseId){
        final Optional<JsonObject> allHearingsAtGlance = this.getAllCaseHearings(envelope, caseId);
        if(allHearingsAtGlance.isPresent()
                && nonNull(allHearingsAtGlance.get().getJsonObject(ALL_CASE_HEARINGS))
                && nonNull(allHearingsAtGlance.get().getJsonObject(ALL_CASE_HEARINGS).getJsonArray(HEARINGS))){
            return allHearingsAtGlance.get()
                    .getJsonObject(ALL_CASE_HEARINGS)
                    .getJsonArray(HEARINGS)
                    .stream()
                    .map(p -> (JsonObject)p)
                    .filter(hearing->!hearing.getString(HEARING_LISTING_STATUS).equalsIgnoreCase(HearingListingStatus.HEARING_RESULTED.toString()))
                    .map(hearing->UUID.fromString(hearing.getString(HEARING_ID)))
                    .collect(Collectors.toList());
        }
        return asList();
    }

    private Optional<JsonObject> getAllCaseHearings(final JsonEnvelope envelope, final UUID caseId) {
        Optional<JsonObject> result = Optional.empty();
        if(nonNull(caseId)){
            final JsonObject requestParameter = createObjectBuilder()
                    .add(CASE_ID, caseId.toString())
                    .build();
            final JsonEnvelope hearingsAtGlance =  requester.request(envelop(requestParameter)
                    .withName(PROGRESSION_QUERY_ALL_CASE_HEARINGS).withMetadataFrom(envelope));
            if (!hearingsAtGlance.payloadAsJsonObject().isEmpty()) {
                result = Optional.of(hearingsAtGlance.payloadAsJsonObject());
            }
        }
        return result;
    }


}
