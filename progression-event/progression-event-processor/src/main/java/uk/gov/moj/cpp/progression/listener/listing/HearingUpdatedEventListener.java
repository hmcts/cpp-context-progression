package uk.gov.moj.cpp.progression.listener.listing;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_COURT_CENTRE_ID;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_COURT_ROOM_ID;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_HEARING_DAYS;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_ID;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_JUDGE_FIRST_NAME;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_JUDGE_ID;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_JUDGE_LAST_NAME;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_JUDGE_TITLE;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.FIELD_HEARING_TYPE;
import static uk.gov.moj.cpp.progression.listener.listing.HearingBuilder.JUDGE_ID;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(EVENT_PROCESSOR)
public class HearingUpdatedEventListener {
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PUBLIC_EVENT_HEARING_UPDATED = "public.hearing-updated";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles(PUBLIC_EVENT_HEARING_UPDATED)
    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope event) {
        this.sender.send(this.enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_EVENTS_HEARING_DETAIL_CHANGED).apply(enrichHearing(event)));
    }

    private JsonObject enrichHearing(final JsonEnvelope event) {
        final JsonObject hearingJsonObject = event.payloadAsJsonObject().getJsonObject(FIELD_HEARING);
        final String hearingId = hearingJsonObject.getString(FIELD_HEARING_ID);
        final String courtRoomId = hearingJsonObject.getString(FIELD_HEARING_COURT_ROOM_ID);
        final String judgeId = hearingJsonObject.getString(JUDGE_ID);

        return new HearingBuilder().with(hearingBuilder -> {
            hearingBuilder.id = hearingId;
            hearingBuilder.type = hearingJsonObject.getString(FIELD_HEARING_TYPE);
            hearingBuilder.courtRoomId = courtRoomId;
            referenceDataService.getCourtCentreByIdAsText(hearingJsonObject.getString(FIELD_HEARING_COURT_CENTRE_ID), event).ifPresent(
                    jsonObject -> hearingBuilder.courtRoomName = enrichCourtRoomName(courtRoomId, jsonObject)
            );
            referenceDataService.getJudgeByIdAsText(judgeId, event).ifPresent(
                    jsonObject -> hearingBuilder.judge = enrichJudge(judgeId, jsonObject)
            );
            hearingBuilder.hearingDays = hearingJsonObject.getJsonArray(FIELD_HEARING_HEARING_DAYS);
        }).createHearing();
    }


    private String enrichCourtRoomName(final String courtRoomId, final JsonObject courtCentreJson) {
        return courtCentreJson.getJsonArray("courtRooms").getValuesAs(JsonObject.class).stream()
                .filter(cr -> courtRoomId.equals(cr.getString("id")))
                .map(cr -> cr.getString("name")).findFirst().orElseGet(() -> "");
    }

    private JsonObject enrichJudge(final String judgeId, final JsonObject judgeJson) {
        return Json.createObjectBuilder()
                .add(FIELD_HEARING_JUDGE_ID, judgeId)
                .add(FIELD_HEARING_JUDGE_TITLE, judgeJson.getString(FIELD_HEARING_JUDGE_TITLE))
                .add(FIELD_HEARING_JUDGE_FIRST_NAME, judgeJson.getString(FIELD_HEARING_JUDGE_FIRST_NAME))
                .add(FIELD_HEARING_JUDGE_LAST_NAME, judgeJson.getString(FIELD_HEARING_JUDGE_LAST_NAME))
                .build();
    }
}


class HearingBuilder {
    static final String JUDGE_ID = "judgeId";
    static final String FIELD_HEARING = "hearing";
    static final String FIELD_HEARING_ID = "id";
    static final String FIELD_HEARING_TYPE = "type";
    static final String FIELD_HEARING_COURT_CENTRE_ID = "courtCentreId";
    static final String FIELD_HEARING_COURT_ROOM_ID = "courtRoomId";
    static final String FIELD_HEARING_COURT_ROOM_NAME = "courtRoomName";
    static final String FIELD_HEARING_JUDGE = "judge";
    static final String FIELD_HEARING_JUDGE_ID = "id";
    static final String FIELD_HEARING_JUDGE_TITLE = "title";
    static final String FIELD_HEARING_JUDGE_FIRST_NAME = "firstName";
    static final String FIELD_HEARING_JUDGE_LAST_NAME = "lastName";
    static final String FIELD_HEARING_HEARING_DAYS = "hearingDays";


    String id;
    String type;
    String courtRoomId;
    String courtRoomName;
    JsonObject judge;
    JsonArray hearingDays;

    public HearingBuilder with(
            Consumer<HearingBuilder> hearingBuilderFunction) {
        hearingBuilderFunction.accept(this);
        return this;
    }

    public JsonObject createHearing() {
        return Json.createObjectBuilder()
                .add(FIELD_HEARING,
                        Json.createObjectBuilder()
                                .add(FIELD_HEARING_ID, this.id)
                                .add(FIELD_HEARING_TYPE, this.type)
                                .add(FIELD_HEARING_COURT_ROOM_ID, this.courtRoomId)
                                .add(FIELD_HEARING_COURT_ROOM_NAME, this.courtRoomName)
                                .add(FIELD_HEARING_JUDGE, judge)
                                .add(FIELD_HEARING_HEARING_DAYS, this.hearingDays)
                                .build()).build();
    }
}
