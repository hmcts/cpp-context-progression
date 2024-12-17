package uk.gov.moj.cpp.progression.service;

import static java.time.ZonedDateTime.now;
import static java.util.Locale.UK;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.Prosecutor.prosecutor;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.events.RepresentationType.REPRESENTATION_ORDER;
import static uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder;
import static uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress.defenceOrganisationAddressBuilder;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.getPayloadAsJsonObject;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.CourtListPublished;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.PublishCourtListPayload;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("JUnitMalformedDeclaration")
@ExtendWith(MockitoExtension.class)
@Disabled
public class PublishCourtListPayloadBuilderServiceTest {

    static Stream<Arguments>  fixedDateEventAndPayloadLocation() {
        return Stream.of(
                Arguments.of("publish-court-list/fixed-date/event/draft-court-list-published-single-hearing.json",
                        "publish-court-list/fixed-date/payload/expected-draft-publish-court-list-single-hearing-defence.json",
                        "publish-court-list/fixed-date/payload/expected-draft-publish-court-list-single-hearing-defence-advocate.json"
                ),
                Arguments.of("publish-court-list/fixed-date/event/draft-court-list-published-single-hearing-no-courtroom.json",
                        "publish-court-list/fixed-date/payload/expected-draft-publish-court-list-single-hearing-no-courtroom-defence.json",
                        "publish-court-list/fixed-date/payload/expected-draft-publish-court-list-single-hearing-no-courtroom-defence-advocate.json"
                ),
                Arguments.of("publish-court-list/fixed-date/event/final-court-list-published-single-hearing.json",
                        "publish-court-list/fixed-date/payload/expected-final-publish-court-list-single-hearing-defence.json",
                        "publish-court-list/fixed-date/payload/expected-final-publish-court-list-single-hearing-defence-advocate.json"
                ),
                Arguments.of("publish-court-list/fixed-date/event/final-court-list-published-single-hearing-no-courtroom.json",
                        "publish-court-list/fixed-date/payload/expected-final-publish-court-list-single-hearing-no-courtroom-defence.json",
                        "publish-court-list/fixed-date/payload/expected-final-publish-court-list-single-hearing-no-courtroom-defence-advocate.json"
                )
                );
    }

    @ParameterizedTest
    @MethodSource("amendmentType")
    public static Object[][] selfRepresentedDefendantEventLocation() {
        return new Object[][]{
                {"publish-court-list/fixed-date/event/draft-court-list-published-defendant-self-represented.json"},
                {"publish-court-list/fixed-date/event/final-court-list-published-defendant-self-represented.json"},
                {"publish-court-list/week-commencing/event/warn-court-list-published-defendant-self-represented.json"},
                {"publish-court-list/week-commencing/event/firm-court-list-published-defendant-self-represented.json"}
        };
    }

    static Stream<Arguments> weekCommencingEventAndPayloadLocation() {
        return Stream.of(
                Arguments.of("publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-fixed-date.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-defence.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-prosecutor.json"),
                Arguments.of("publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-fixed-date-no-courtroom.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-no-courtroom-defence.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-no-courtroom-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-fixed-date-no-courtroom-prosecutor.json"),
                Arguments.of("publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-week-commencing.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-defence.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-prosecutor.json"),
                Arguments.of("publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-week-commencing-no-courtroom.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-no-courtroom-defence.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-no-courtroom-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-warn-publish-court-list-single-hearing-week-commencing-no-courtroom-prosecutor.json"),
                Arguments.of("publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-fixed-date.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-defence.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-prosecutor.json"),
                Arguments.of("publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-fixed-date-no-courtroom.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-no-courtroom-defence.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-no-courtroom-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-fixed-date-no-courtroom-prosecutor.json"),
            Arguments.of("publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-week-commencing.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-defence.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-prosecutor.json"),
        Arguments.of("publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-week-commencing-no-courtroom.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-no-courtroom-defence.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-no-courtroom-defence-advocate.json",
                        "publish-court-list/week-commencing/payload/expected-firm-publish-court-list-single-hearing-week-commencing-no-courtroom-prosecutor.json")
                );
    }

    @ParameterizedTest
    @MethodSource("amendmentType")
    public static Object[][] weekCommencingEventLocationForCPSProsecutor() {
        return new Object[][]{
                {"publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-week-commencing.json"},
                {"publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-week-commencing.json"}
        };
    }

    private static final UUID COURT_CENTRE_ID = fromString("07e45c88-9e5d-3e44-b664-d5345bb13be2");
    private static final UUID COURT_ROOM_1_ID = fromString("731816c1-5ee4-373a-9bda-840e13a5bcb0");
    private static final UUID COURT_ROOM_2_ID = fromString("22ef1c48-90e7-46c9-997d-a9526220978b");
    private static final UUID COURT_ROOM_3_ID = fromString("9ff22a17-bace-4aa9-be55-b358a6a15ac7");
    private static final UUID DEFENDANT_ID_1 = fromString("808f65d0-dc76-42b5-8510-ea45c038b8c0"); //CASE_ID : 3,2
    private static final UUID DEFENDANT_ID_2 = fromString("c4fbed8f-790d-4480-86c8-bb1b8a49cf78"); //CASE_ID : 1,3
    private static final UUID DEFENDANT_ID_3 = fromString("dcd8800b-640e-48ae-9447-8fb481b73c2e"); //CASE_ID : 1,6
    private static final UUID DEFENDANT_ID_4 = fromString("f5d1836d-4402-49be-ae26-784b357d2bf0"); //CASE_ID : 3
    private static final UUID DEFENDANT_ID_5 = fromString("07b8b98c-093c-4019-90ae-29a6ce3f0add"); //CASE_ID : 4
    private static final UUID DEFENDANT_ID_6 = fromString("ba0b94c8-ebdb-4708-aed8-83954be9694f"); //CASE_ID : 5
    private static final UUID SELF_REPRESENTING_DEFENDANT_ID_1 = fromString("a5bae35f-178d-4fd9-96c8-8554d881a509");
    private static final UUID SELF_REPRESENTING_DEFENDANT_ID_2 = fromString("3e0c4bba-8dc4-4dba-ae4f-105f0eea49f5");
    private static final String LJA_CODE = "MERS";
    private static final String LJA_NAME = "Merseyside LJA";
    private static final String COURT_CENTRE_NAME = "Croydon Crown Court";
    private static final UUID CASE_ID_1 = fromString("6c510b76-5ea0-4471-b0e2-a99fca32a623"); //urn:91GD6403223  def 2,3
    private static final UUID CASE_ID_2 = fromString("ad6b2424-9c50-4fb9-8150-e709b3e3043d"); //urn:CTZH5HVZIF def 1
    private static final UUID CASE_ID_3 = fromString("e312c8f6-9fb3-4ab9-acec-d4cd192b1758"); //urn:92GD4165920  def 1,2,4
    private static final UUID CASE_ID_4 = fromString("c5547049-86ef-4888-9a7d-94f604b83a6c"); //urn:CVJVHKJGJC  def 5
    private static final UUID CASE_ID_5 = fromString("a740ef9e-8a1a-49f3-a89a-ee904eb54122"); //urn:13SS0123695  def 6
    private static final UUID CASE_ID_6 = fromString("1ce92d9a-4e20-4ce3-8917-d2da046eae5d"); //urn:90GD9173LAA  def 3
    private static final UUID PROSECUTOR_ID_1 = randomUUID();
    private static final UUID PROSECUTOR_ID_2 = randomUUID();
    private final Faker faker = new Faker(UK);

    @Mock
    private RefDataService referenceDataService;
    @Mock
    private ProgressionService progressionService;
    @Mock
    private DefenceService defenceService;
    @Mock
    private CorrespondenceService correspondenceService;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Mock
    private Requester requester;

    @InjectMocks
    private PublishCourtListPayloadBuilderService underTest;

    @BeforeEach
    public void setup() {
        initMocks(this);
    }

    @ParameterizedTest
    @MethodSource("fixedDateEventAndPayloadLocation")
    public void shouldPrepareDocumentPayloadForDefenceOrganisationOnlyWhenFixedDateCourtListIsPublishedForSingleHearing(final String courtListPublishedEventLocation,
                                                                                                                        final String expectedPayloadLocationForDefence,
                                                                                                                        final String expectedPayloadLocationForDefenceAdvocate) throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final JsonObject courtCentreWithCourtRooms = prepareCourtCentreWithCourtRooms();
        final AssociatedDefenceOrganisation defenceOrganisation = prepareDefenceOrganisation1();
        final JsonObject correspondenceContacts = prepareCorrespondenceCaseContacts();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();
        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(courtCentreWithCourtRooms));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        when(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), any())).thenReturn(defenceOrganisation);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_1))).willReturn(correspondenceContacts);

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject(courtListPublishedEventLocation), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(prosecutionPayloadBuilderByName.isEmpty(), is(true));
        assertThat(defenceOrganisationPayloadBuilderByName.size(), is(1));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation.getEmail()));

        assertThat(defenceAdvocatePayloadBuilderByName.size(), is(1));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("defenceAdvocate@organisation.com"));

        final PublishCourtListPayload actualPublishCourtListPayload = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation.getEmail()).build();
        assertEquals(getPayload(expectedPayloadLocationForDefence), objectToJsonObjectConverter.convert(actualPublishCourtListPayload).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload = defenceAdvocatePayloadBuilderByName.get("defenceAdvocate@organisation.com").build();
        assertEquals(getPayload(expectedPayloadLocationForDefenceAdvocate), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload).toString(), STRICT_ORDER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish-court-list/fixed-date/event/draft-court-list-published-defendant-self-represented.json",
            "publish-court-list/fixed-date/event/final-court-list-published-defendant-self-represented.json"
    })
    public void shouldNotPrepareDocumentPayloadForDefenceOrganisationWhenDefendantIsSelfRepresented(final String courtListPublishedEventLocation) throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();
        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(prepareCourtCentreWithCourtRooms()));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        when(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), any())).thenReturn(null);

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject(courtListPublishedEventLocation), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(defenceOrganisationPayloadBuilderByName.isEmpty(), is(true));
        assertThat(defenceAdvocatePayloadBuilderByName.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish-court-list/week-commencing/event/warn-court-list-published-defendant-self-represented.json",
            "publish-court-list/week-commencing/event/firm-court-list-published-defendant-self-represented.json"
    })
    public void shouldNotPrepareDocumentPayloadForDefenceOrganisationWhenDefendantIsSelfRepresented1(final String courtListPublishedEventLocation) throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();
        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(prepareCourtCentreWithCourtRooms()));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        when(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), any())).thenReturn(null);
        when(referenceDataService.getProsecutor(envelope, PROSECUTOR_ID_1, requester)).thenReturn(Optional.of(prepareProsecutorDetails1()));
        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject(courtListPublishedEventLocation), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(defenceOrganisationPayloadBuilderByName.isEmpty(), is(true));
        assertThat(defenceAdvocatePayloadBuilderByName.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish-court-list/week-commencing/event/warn-court-list-published-single-hearing-week-commencing.json",
            "publish-court-list/week-commencing/event/firm-court-list-published-single-hearing-week-commencing.json"
    })
    public void shouldNotPrepareDocumentPayloadForProsecutorWhenProsecutorIsCPS(final String courtListPublishedEventLocation) throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();
        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(prepareCourtCentreWithCourtRooms()));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_1))).willReturn(null);
        given(referenceDataService.getProsecutor(envelope, PROSECUTOR_ID_1, requester)).willReturn(Optional.of(prepareCPSProsecutorDetails()));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_1.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject(courtListPublishedEventLocation), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(prosecutionPayloadBuilderByName.isEmpty(), is(true));
    }

    @ParameterizedTest
    @MethodSource("weekCommencingEventAndPayloadLocation")
    public void shouldPrepareDocumentPayloadForDefenceOrganisationAndProsecutorWhenWeekCommencingCourtListIsPublishedForSingleHearing(final String courtListPublishedEventLocation,
                                                                                                                                      final String expectedPayloadLocationForDefence,
                                                                                                                                      final String expectedPayloadLocationForDefenceAdvocate,
                                                                                                                                      final String expectedPayloadLocationForProsecutor) throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final JsonObject courtCentreWithCourtRooms = prepareCourtCentreWithCourtRooms();
        final AssociatedDefenceOrganisation defenceOrganisation = prepareDefenceOrganisation1();
        final JsonObject prosecutor = prepareProsecutorDetails1();
        final JsonObject correspondenceContacts = prepareCorrespondenceCaseContacts_WithoutEmail();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutorPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();

        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(courtCentreWithCourtRooms));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        when(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), any())).thenReturn(defenceOrganisation);
        given(referenceDataService.getProsecutor(envelope, PROSECUTOR_ID_1, requester)).willReturn(Optional.of(prosecutor));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_1.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_1))).willReturn(correspondenceContacts);

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject(courtListPublishedEventLocation), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutorPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(prosecutorPayloadBuilderByName.size(), is(1));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation.getEmail()));
        assertThat(defenceOrganisationPayloadBuilderByName.size(), is(1));
        assertThat(prosecutorPayloadBuilderByName, hasKey(prosecutor.getString("contactEmailAddress")));

        assertThat(defenceAdvocatePayloadBuilderByName.size(), is(1));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("contactPersonName"));

        final PublishCourtListPayload actualDefenceOrganisationPublishCourtListPayload = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation.getEmail()).build();
        assertEquals(getPayload(expectedPayloadLocationForDefence), objectToJsonObjectConverter.convert(actualDefenceOrganisationPublishCourtListPayload).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload = defenceAdvocatePayloadBuilderByName.get("contactPersonName").build();
        assertEquals(getPayload(expectedPayloadLocationForDefenceAdvocate), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualProsecutorPublishCourtListPayload = prosecutorPayloadBuilderByName.get(prosecutor.getString("contactEmailAddress")).build();
        assertEquals(getPayload(expectedPayloadLocationForProsecutor), objectToJsonObjectConverter.convert(actualProsecutorPublishCourtListPayload).toString(), STRICT_ORDER);
    }

    @Test
    public void shouldPrepareDocumentPayloadForDefenceOrganisationOnlyWhenFixedDateCourtListIsPublishedForMultipleHearings() throws Exception {
        final JsonEnvelope envelope = prepareEnvelope();
        final JsonObject courtCentreWithCourtRooms = prepareCourtCentreWithCourtRooms();
        final JsonObject correspondenceContacts1 = prepareCorrespondenceCaseContacts_WithoutEmail();
        final JsonObject correspondenceContacts2 = prepareCorrespondenceCaseContacts();
        final AssociatedDefenceOrganisation defenceOrganisation1 = prepareDefenceOrganisation1();
        final AssociatedDefenceOrganisation defenceOrganisation2 = prepareDefenceOrganisation2();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutionPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();

        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(courtCentreWithCourtRooms));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_1))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_2))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_3))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_4))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_5))).willReturn(correspondenceContacts2);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_6))).willReturn(correspondenceContacts2);

        prepareDefenceOrganisationForDefendants(defenceOrganisation1, defenceOrganisation2);

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject("publish-court-list/fixed-date/event/draft-court-list-published-multiple-hearing.json"), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutionPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(prosecutionPayloadBuilderByName.isEmpty(), is(true));
        assertThat(defenceOrganisationPayloadBuilderByName.size(), is(2));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation1.getEmail()));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation2.getEmail()));

        assertThat(defenceAdvocatePayloadBuilderByName.size(), is(2));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("contactPersonName"));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("defenceAdvocate@organisation.com"));

        final PublishCourtListPayload actualPublishCourtListPayload1 = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation1.getEmail()).build();
        assertEquals(getPayload("publish-court-list/fixed-date/payload/expected-draft-publish-court-list-multiple-hearing-defence1.json"), objectToJsonObjectConverter.convert(actualPublishCourtListPayload1).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload1 = defenceAdvocatePayloadBuilderByName.get("contactPersonName").build();
        assertEquals(getPayload("publish-court-list/fixed-date/payload/expected-draft-publish-court-list-multiple-hearing-defence1-advocate.json"), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload1).toString(), STRICT_ORDER);


        final PublishCourtListPayload actualPublishCourtListPayload2 = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation2.getEmail()).build();
        assertEquals(getPayload("publish-court-list/fixed-date/payload/expected-draft-publish-court-list-multiple-hearing-defence2.json"), objectToJsonObjectConverter.convert(actualPublishCourtListPayload2).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload2 = defenceAdvocatePayloadBuilderByName.get("defenceAdvocate@organisation.com").build();
        assertEquals(getPayload("publish-court-list/fixed-date/payload/expected-draft-publish-court-list-multiple-hearing-defence2-advocate.json"), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload2).toString(), STRICT_ORDER);

    }

    @Test
    public void shouldPrepareDocumentPayloadForDefenceOrganisationAndProsecutorWhenWeekCommencingCourtListIsPublishedForMultipleHearings() throws Exception {
        System.out.println("---------------------------------");
        System.out.println(Locale.getDefault());
        final JsonEnvelope envelope = prepareEnvelope();
        final JsonObject courtCentreWithCourtRooms = prepareCourtCentreWithCourtRooms();
        final AssociatedDefenceOrganisation defenceOrganisation1 = prepareDefenceOrganisation1();
        final AssociatedDefenceOrganisation defenceOrganisation2 = prepareDefenceOrganisation2();
        final JsonObject prosecutor1 = prepareProsecutorDetails1();
        final JsonObject prosecutor2 = prepareProsecutorDetails2();
        final JsonObject correspondenceContacts1 = prepareCorrespondenceCaseContacts_WithoutEmail();
        final JsonObject correspondenceContacts2 = prepareCorrespondenceCaseContacts();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceOrganisationPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> prosecutorPayloadBuilderByName = new HashMap<>();
        final Map<String, PublishCourtListPayload.PublishCourtListPayloadBuilder> defenceAdvocatePayloadBuilderByName = new HashMap<>();

        given(referenceDataService.getCourtCentreWithCourtRoomsById(eq(COURT_CENTRE_ID), any(JsonEnvelope.class), any(Requester.class))).willReturn(Optional.of(courtCentreWithCourtRooms));
        given(referenceDataService.getEnforcementAreaByLjaCode(any(JsonEnvelope.class), eq(LJA_CODE), any(Requester.class))).willReturn(prepareEnforcementAreaJson());
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_1))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_2))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_3))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_4))).willReturn(correspondenceContacts1);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_5))).willReturn(correspondenceContacts2);
        given(correspondenceService.getCaseContacts(any(JsonEnvelope.class), eq(CASE_ID_6))).willReturn(correspondenceContacts2);

        prepareDefenceOrganisationForDefendants(defenceOrganisation1, defenceOrganisation2);
        prepareProsecutorForCases(envelope, prosecutor1, prosecutor2);

        final CourtListPublished courtListPublishedEvent = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject("publish-court-list/week-commencing/event/firm-court-list-published-multiple-hearing.json"), CourtListPublished.class);
        underTest.buildPayloadForInterestedParties(envelope, courtListPublishedEvent, defenceOrganisationPayloadBuilderByName, prosecutorPayloadBuilderByName, defenceAdvocatePayloadBuilderByName);

        assertThat(prosecutorPayloadBuilderByName.size(), is(2));
        assertThat(defenceOrganisationPayloadBuilderByName.size(), is(2));
        assertThat(prosecutorPayloadBuilderByName, hasKey(prosecutor1.getString("contactEmailAddress")));
        assertThat(prosecutorPayloadBuilderByName, hasKey(prosecutor2.getString("contactEmailAddress")));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation1.getEmail()));
        assertThat(defenceOrganisationPayloadBuilderByName, hasKey(defenceOrganisation2.getEmail()));

        assertThat(defenceAdvocatePayloadBuilderByName.size(), is(2));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("contactPersonName"));
        assertThat(defenceAdvocatePayloadBuilderByName, hasKey("defenceAdvocate@organisation.com"));

        final PublishCourtListPayload actualDefenceOrganisationPublishCourtListPayload1 = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation1.getEmail()).build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-defence1.json"), objectToJsonObjectConverter.convert(actualDefenceOrganisationPublishCourtListPayload1).toString(), STRICT_ORDER);
        final PublishCourtListPayload actualDefenceOrganisationPublishCourtListPayload2 = defenceOrganisationPayloadBuilderByName.get(defenceOrganisation2.getEmail()).build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-defence2.json"), objectToJsonObjectConverter.convert(actualDefenceOrganisationPublishCourtListPayload2).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload1 = defenceAdvocatePayloadBuilderByName.get("contactPersonName").build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-defence1-advocate.json"), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload1).toString(), STRICT_ORDER);
        final PublishCourtListPayload actualDefenceAdvocatePublishCourtListPayload2 = defenceAdvocatePayloadBuilderByName.get("defenceAdvocate@organisation.com").build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-defence2-advocate.json"), objectToJsonObjectConverter.convert(actualDefenceAdvocatePublishCourtListPayload2).toString(), STRICT_ORDER);

        final PublishCourtListPayload actualProsecutorPublishCourtListPayload1 = prosecutorPayloadBuilderByName.get(prosecutor1.getString("contactEmailAddress")).build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-prosecutor1.json"), objectToJsonObjectConverter.convert(actualProsecutorPublishCourtListPayload1).toString(), STRICT_ORDER);
        final PublishCourtListPayload actualProsecutorPublishCourtListPayload2 = prosecutorPayloadBuilderByName.get(prosecutor2.getString("contactEmailAddress")).build();
        assertEquals(getPayload("publish-court-list/week-commencing/payload/expected-firm-publish-court-list-multiple-hearing-prosecutor2.json"), objectToJsonObjectConverter.convert(actualProsecutorPublishCourtListPayload2).toString(), STRICT_ORDER);
    }

    private JsonObject prepareCourtCentreWithCourtRooms() {
        return createObjectBuilder()
                .add("id", COURT_CENTRE_ID.toString())
                .add("lja", LJA_CODE)
                .add("oucodeL3Name", COURT_CENTRE_NAME)
                .add("oucodeL3WelshName", "Llys Y Goron Croydon")
                .add("isWelsh", false)
                .add("address1", "The Law Courts")
                .add("address2", "Altyre Road")
                .add("address3", "Croydon")
                .add("postcode", "CR9 5AB")
                .add("courtrooms", createArrayBuilder()
                        .add(createObjectBuilder().add("id", COURT_ROOM_1_ID.toString()).add("courtroomName", "Courtroom 01"))
                        .add(createObjectBuilder().add("id", COURT_ROOM_2_ID.toString()).add("courtroomName", "Courtroom 02"))
                        .add(createObjectBuilder().add("id", COURT_ROOM_3_ID.toString()).add("courtroomName", "Courtroom 03"))
                )
                .build();
    }

    private JsonObject prepareEnforcementAreaJson() {
        return createObjectBuilder()
                .add("enforcingCourtCode", "828")
                .add("localJusticeArea", createObjectBuilder()
                        .add("nationalCourtCode", LJA_CODE)
                        .add("name", LJA_NAME)
                )
                .build();
    }

    private JsonObject prepareProsecutionCaseJson(final UUID prosecutorId) {
        final ProsecutionCase prosecutionCase;
        if (faker.bool().bool()) {
            prosecutionCase = prosecutionCase()
                    .withProsecutor(prosecutor()
                            .withProsecutorId(prosecutorId)
                            .build())
                    .build();
        } else {
            prosecutionCase = prosecutionCase()
                    .withProsecutionCaseIdentifier(
                            prosecutionCaseIdentifier()
                                    .withProsecutionAuthorityId(prosecutorId)
                                    .build())
                    .build();
        }
        return createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build();
    }

    private JsonObject prepareProsecutorDetails1() {
        return createObjectBuilder()
                .add("fullName", "Transport For London")
                .add("contactEmailAddress", "test_tfl.enforcement@tfl.cjsm.net")
                .add("address", createObjectBuilder()
                        .add("address1", "6th Floor Windsor House")
                        .add("address2", "42-50 Victoria Street")
                        .add("address3", "London")
                        .add("postcode", "SW1H 0TL"))
                .build();
    }

    private JsonObject prepareProsecutorDetails2() {
        return createObjectBuilder()
                .add("fullName", "DVLA")
                .add("contactEmailAddress", "test_dvla.enforcement@dvla.cjsm.net")
                .add("address", createObjectBuilder()
                        .add("address1", "1th Floor Windsor House")
                        .add("address2", "42-50 Victoria Street")
                        .add("address3", "London")
                        .add("postcode", "SW1H 0TL"))
                .build();
    }

    private JsonObject prepareCPSProsecutorDetails() {
        return createObjectBuilder()
                .add("fullName", "Hampshire Police")
                .add("contactEmailAddress", "cjd.phoenix@hampshire.pnn.police.uk")
                .add("address", createObjectBuilder()
                        .add("address1", "Operational HQ")
                        .add("address2", "Mottisfont Court")
                        .add("address3", "Winchester")
                        .add("postcode", "SO23 8ZD"))
                .add("cpsFlag", true)
                .build();
    }

    private AssociatedDefenceOrganisation prepareDefenceOrganisation1() {
        return associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withOrganisationName("Robert Stone LLP")
                .withStatus("Active Barrister/Solicitor of record")
                .withRepresentationType(REPRESENTATION_ORDER)
                .withStartDate(now().minusMonths(1))
                .withPhoneNumber(faker.phoneNumber().phoneNumber())
                .withEmail("organisation@organisation.com")
                .withAddress(defenceOrganisationAddressBuilder()
                        .withAddress1("349")
                        .withAddress2("Sipes Square")
                        .withAddress3("Lake Gertrudeview")
                        .withAddress4("Mauritius")
                        .withAddressPostcode("Z7W 9NL")
                        .build())
                .build();
    }

    private JsonObject prepareCorrespondenceCaseContacts() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("caseContext", "HMCTS")
                .add("caseId", CASE_ID_1.toString())
                .add("contactId", randomUUID().toString())
                .add("contactPersonName", "contactPersonName")
                .add("primaryEmail", "defenceAdvocate@organisation.com")
                .add("contactType", createObjectBuilder().add("contactTypeMapping", "Defence advocate").add("id", randomUUID().toString()).build())
                .add("isAdhocContact", true)
                .add("address", createObjectBuilder().add("address1", "address1").add("address2", "address2").add("postcode", "postcode").build())
                .add("defendantIds", createArrayBuilder().add(DEFENDANT_ID_1.toString()).add(DEFENDANT_ID_2.toString()).add(DEFENDANT_ID_3.toString()).build());

        return createObjectBuilder().add("contacts", createArrayBuilder()
                .add(jsonObjectBuilder.build()))
                .build();

    }

    private JsonObject prepareCorrespondenceCaseContacts_WithoutEmail() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("caseContext", "HMCTS")
                .add("caseId", CASE_ID_1.toString())
                .add("contactId", randomUUID().toString())
                .add("contactPersonName", "contactPersonName")
                .add("contactType", createObjectBuilder().add("contactTypeMapping", "Defence advocate").add("id", randomUUID().toString()).build())
                .add("isAdhocContact", true)
                .add("address", createObjectBuilder().add("address1", "address1").add("address2", "address2").add("postcode", "postcode").build())
                .add("defendantIds", createArrayBuilder().add(DEFENDANT_ID_1.toString()).add(DEFENDANT_ID_2.toString()).add(DEFENDANT_ID_3.toString()).build());

        return createObjectBuilder().add("contacts", createArrayBuilder()
                .add(jsonObjectBuilder.build()))
                .build();

    }

    private AssociatedDefenceOrganisation prepareDefenceOrganisation2() {
        return associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withOrganisationName("Premier Solicitor LLP")
                .withStatus("Active Barrister/Solicitor of record")
                .withRepresentationType(REPRESENTATION_ORDER)
                .withStartDate(now().minusMonths(1))
                .withPhoneNumber(faker.phoneNumber().phoneNumber())
                .withEmail(faker.internet().safeEmailAddress())
                .withAddress(defenceOrganisationAddressBuilder()
                        .withAddress1("45 Church Road")
                        .withAddress4("Liverpool")
                        .withAddressPostcode("L3 1SK")
                        .build())
                .build();
    }

    private void prepareProsecutorForCases(final JsonEnvelope envelope, final JsonObject prosecutor1, final JsonObject prosecutor2) {
        given(referenceDataService.getProsecutor(envelope, PROSECUTOR_ID_1, requester)).willReturn(Optional.of(prosecutor1));
        given(referenceDataService.getProsecutor(envelope, PROSECUTOR_ID_2, requester)).willReturn(Optional.of(prosecutor2));

        when(progressionService.getProsecutionCase(envelope, CASE_ID_1.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_2.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_2)));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_3.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_4.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_2)));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_5.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_1)));
        when(progressionService.getProsecutionCase(envelope, CASE_ID_6.toString())).thenReturn(Optional.of(prepareProsecutionCaseJson(PROSECUTOR_ID_2)));
    }

    private void prepareDefenceOrganisationForDefendants(final AssociatedDefenceOrganisation defenceOrganisation1, final AssociatedDefenceOrganisation defenceOrganisation2) {
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_1))).willReturn(defenceOrganisation1);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_2))).willReturn(defenceOrganisation2);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_3))).willReturn(defenceOrganisation1);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_4))).willReturn(defenceOrganisation1);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_5))).willReturn(defenceOrganisation2);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(DEFENDANT_ID_6))).willReturn(defenceOrganisation1);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(SELF_REPRESENTING_DEFENDANT_ID_1))).willReturn(null);
        given(defenceService.getDefenceOrganisationByDefendantId(any(JsonEnvelope.class), eq(SELF_REPRESENTING_DEFENDANT_ID_2))).willReturn(null);
    }

    private JsonEnvelope prepareEnvelope() {
        return envelopeFrom(
                metadataBuilder().withId(randomUUID()).withName("progression.event.court-list-published").build(),
                createObjectBuilder().build());
    }
}
