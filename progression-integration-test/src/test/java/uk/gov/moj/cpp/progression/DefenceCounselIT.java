package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefenceCounsel;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.removeDefenceCounsel;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateDefenceCounsel;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefenceCounselIT extends AbstractIT {

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String DOCUMENT_TEXT = STRING.next();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED = "public.hearing.defence-counsel-added";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED = "public.hearing.defence-counsel-updated";
    private static final String PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED = "public.hearing.defence-counsel-removed";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";


    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;

    @BeforeEach
    public void setUp() {
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
    }

    @Test
    public void shouldHandleDefenceCounselWhenRaisedPublicEvent() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonObject hearingAddDefenceCounselJson = getDefenceCounselPublicEventPayload(hearingId, "JOHN");
        final JsonEnvelope publicEventAddedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, userId), hearingAddDefenceCounselJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_DEFENCE_COUNSEL_ADDED, publicEventAddedEnvelope);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.defenceCounsels[0].id", Matchers.is("fab947a3-c50c-4dbb-accf-b2758b1d2d6d")),
                withJsonPath("$.hearing.defenceCounsels[0].firstName", Matchers.is("JOHN")),
                withJsonPath("$.hearing.defenceCounsels[0].status", Matchers.is("QC")),
                withJsonPath("$.hearing.defenceCounsels[0].title", Matchers.is("Mr")),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[0]", Matchers.is("2018-07-17")),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[1]", Matchers.is("2018-07-18"))
        );

        //UPDATE
        final JsonObject hearingUpdateDefenceCounselJson = getDefenceCounselPublicEventPayload(hearingId, "DOE");
        final JsonEnvelope publicEventUpdatedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED, userId), hearingUpdateDefenceCounselJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_DEFENCE_COUNSEL_UPDATED, publicEventUpdatedEnvelope);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.defenceCounsels[0].id", Matchers.is("fab947a3-c50c-4dbb-accf-b2758b1d2d6d")),
                withJsonPath("$.hearing.defenceCounsels[0].firstName", Matchers.is("DOE")),
                withJsonPath("$.hearing.defenceCounsels[0].status", Matchers.is("QC")),
                withJsonPath("$.hearing.defenceCounsels[0].title", Matchers.is("Mr")),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[0]", Matchers.is("2018-07-17")),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[1]", Matchers.is("2018-07-18"))
        );

        //REMOVE
        final JsonObject hearingRemovedDefenceCounselJson = getDefenceCounselRemovedPublicEventPayload(hearingId);
        final JsonEnvelope publicEventRemovedEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED, userId), hearingRemovedDefenceCounselJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_DEFENCE_COUNSEL_REMOVED, publicEventRemovedEnvelope);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withoutJsonPath("$.hearing.defenceCounsels")
        );

    }

    @Test
    public void shouldAddAndUpdateAndRemoveDefenseCounsel() throws Exception {

        final String defenceCounselId = randomUUID().toString();
        final List<String> defendantList = Stream.of(randomUUID().toString(), randomUUID().toString()).collect(Collectors.toList());
        final List<String> attendanceDaysList = Stream.of(FUTURE_LOCAL_DATE.next().toString(), FUTURE_LOCAL_DATE.next().toString()).collect(Collectors.toList());
        final List<String> updatedDefendantList = Stream.of(randomUUID().toString(), randomUUID().toString()).collect(Collectors.toList());
        final List<String> updatedAttendanceDaysList = Stream.of(FUTURE_LOCAL_DATE.next().toString(), FUTURE_LOCAL_DATE.next().toString()).collect(Collectors.toList());


        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        addDefenceCounsel(hearingId, defenceCounselId, defendantList, attendanceDaysList, "progression.add-hearing-defence-counsel.json");

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.defenceCounsels[0].id", is(defenceCounselId)),
                withJsonPath("$.hearing.defenceCounsels[0].firstName", is("Eric")),
                withJsonPath("$.hearing.defenceCounsels[0].lastName", is("Ormsby")),
                withJsonPath("$.hearing.defenceCounsels[0].title", is("Mr")),
                withJsonPath("$.hearing.defenceCounsels[0].defendants[0]", is(defendantList.get(0))),
                withJsonPath("$.hearing.defenceCounsels[0].defendants[1]", is(defendantList.get(1))),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[0]", is(attendanceDaysList.get(0))),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[1]", is(attendanceDaysList.get(1)))
        );

        updateDefenceCounsel(hearingId, defenceCounselId, updatedDefendantList, updatedAttendanceDaysList, "progression.add-hearing-defence-counsel.json");
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.defenceCounsels[0].id", is(defenceCounselId)),
                withJsonPath("$.hearing.defenceCounsels[0].firstName", is("Eric")),
                withJsonPath("$.hearing.defenceCounsels[0].lastName", is("Ormsby")),
                withJsonPath("$.hearing.defenceCounsels[0].title", is("Mr")),
                withJsonPath("$.hearing.defenceCounsels[0].defendants[0]", is(updatedDefendantList.get(0))),
                withJsonPath("$.hearing.defenceCounsels[0].defendants[1]", is(updatedDefendantList.get(1))),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[0]", is(updatedAttendanceDaysList.get(0))),
                withJsonPath("$.hearing.defenceCounsels[0].attendanceDays[1]", is(updatedAttendanceDaysList.get(1)))
        );

        removeDefenceCounsel(hearingId, defenceCounselId, defendantList, attendanceDaysList, "progression.remove-hearing-defence-counsel.json");
        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withoutJsonPath("$.hearing.defenceCounsels.*")
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    private JsonObject getDefenceCounselPublicEventPayload(final String hearingId, final String firstName) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.defence-counsel-added-or-updated.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("FIRST_NAME", firstName);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getDefenceCounselRemovedPublicEventPayload(final String hearingId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.counsel-removed.json")
                .replaceAll("HEARING_ID", hearingId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }
}
