package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnCourtAddress;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnDefendant;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnDefendantAddress;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnHearingCourtDetails;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnOffences;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnReferralReason;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.assertOnSummonsData;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateCourtCentreJson;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateCourtCentreJsonInWelsh;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateDocumentTypeAccess;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateLjaDetails;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateLjaDetailsWithWelsh;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateProsecutionCaseJson;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateReferralReasonsJson;
import static uk.gov.moj.cpp.progression.processor.helper.SummonDataPreparedEventProcessorTestHelper.generateSummonsData;

import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsRequired;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SummonsDataPreparedEventProcessorTest {

    public static final String NAME_HARRY_JACK_KANE = "Harry Jack Kane";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID REFERRAL_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.fromString("89b10041-b44d-43c8-9b1e-d1b9fee15c93");
    public static final UUID SUMMONS_DOCUMENT_TYPE_ID = UUID.fromString("460f7ec0-c002-11e8-a355-529269fb1459");
    private static final String ADDRESS = "address";
    public static final String OFFENCES = "offences";
    public static final String ADDRESSEE = "addressee";
    public static final String HEARING_COURT_DETAILS = "hearingCourtDetails";
    public static final String COURT_ADDRESS = "courtAddress";
    public static final String USER_ID = UUID.randomUUID().toString();


    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Requester requester;

    @Mock
    private SummonsDataPrepared summonsDataPrepared;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @InjectMocks
    private SummonsDataPreparedEventProcessor summonsDataPreparedEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptorForCourtDocument;

    @Captor
    private ArgumentCaptor<Sender> senderArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> templateArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> caseIdArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> applicationIdArgumentCaptor;

    @Test
    public void shouldGenerateSummonsPayloadForFirstHearing() {

        //Given
        prepareDataAndConditionsForDocumentGeneration(generateCourtCentreJson(),
                SummonsRequired.FIRST_HEARING,
                false);

        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //When
        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                templateArgumentCaptor.capture(),
                senderArgumentCaptor.capture(),
                caseIdArgumentCaptor.capture(),
                applicationIdArgumentCaptor.capture());


        //Then
        verifyCourtDocument(1);
        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertOnCaseIdAndEnvelope();

        assertOnSummonsData(summonsDataJson,
                SummonsRequired.FIRST_HEARING,
                false);

        final JsonObject defendantJson = assertOnDefendant(summonsDataJson);

        final JsonObject defendantAddressJson = defendantJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(defendantAddressJson);

        final JsonArray offencesJson = summonsDataJson.getJsonArray(OFFENCES);
        assertOnOffences(offencesJson, false);

        final JsonObject addresseeJson = summonsDataJson.getJsonObject(ADDRESSEE);
        assertThat(addresseeJson, notNullValue());
        assertThat(addresseeJson.getString("name"), is(NAME_HARRY_JACK_KANE));

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(addresseeAddressJson);


        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject(HEARING_COURT_DETAILS);
        assertOnHearingCourtDetails(hearingCourtDetails);

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject(COURT_ADDRESS);
        assertOnCourtAddress(courtAddress, false);
    }

    @Test
    public void shouldGenerateSummonsPayloadForSjpReferral() {

        //Given
        prepareDataAndConditionsForDocumentGeneration(generateCourtCentreJson(),
                SummonsRequired.SJP_REFERRAL,
                false);

        //When
        summonsDataPreparedEventProcessor.requestSummons(envelope);

        //Then
        verifyCourtDocument(1);
        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                templateArgumentCaptor.capture(),
                senderArgumentCaptor.capture(),
                caseIdArgumentCaptor.capture(),
                applicationIdArgumentCaptor.capture());

        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertOnCaseIdAndEnvelope();

        assertOnSummonsData(summonsDataJson,
                SummonsRequired.SJP_REFERRAL,
                false);

        final JsonObject defendantJson = assertOnDefendant(summonsDataJson);

        final JsonObject defendantAddressJson = defendantJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(defendantAddressJson);

        final JsonArray offencesJson = summonsDataJson.getJsonArray(OFFENCES);
        assertOnOffences(offencesJson, false);

        final JsonObject addresseeJson = summonsDataJson.getJsonObject(ADDRESSEE);
        assertThat(addresseeJson, notNullValue());
        assertThat(addresseeJson.getString("name"), is(NAME_HARRY_JACK_KANE));

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(addresseeAddressJson);

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject(HEARING_COURT_DETAILS);
        assertOnHearingCourtDetails(hearingCourtDetails);

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject(COURT_ADDRESS);
        assertOnCourtAddress(courtAddress, false);

        final JsonObject referralReason = summonsDataJson.getJsonObject("referralContent");
        assertOnReferralReason(referralReason, false);
    }

    @Test
    public void shouldGenerateSummonsPayloadForSjpReferralForWelsh() {

        //Given
        prepareDataAndConditionsForDocumentGeneration(generateCourtCentreJsonInWelsh(),
                SummonsRequired.SJP_REFERRAL,
                true);

        //When
        summonsDataPreparedEventProcessor.requestSummons(envelope);

        verify(documentGeneratorService).generateDocument(envelopeArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                templateArgumentCaptor.capture(),
                senderArgumentCaptor.capture(),
                caseIdArgumentCaptor.capture(),
                applicationIdArgumentCaptor.capture());
        verifyCourtDocument(1);

        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertOnCaseIdAndEnvelope();
        assertOnSummonsData(summonsDataJson,
                SummonsRequired.SJP_REFERRAL,
                true);

        final JsonObject defendantJson = assertOnDefendant(summonsDataJson);

        final JsonObject defendantAddressJson = defendantJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(defendantAddressJson);

        final JsonArray offencesJson = summonsDataJson.getJsonArray(OFFENCES);
        assertOnOffences(offencesJson, true);

        final JsonObject addresseeJson = summonsDataJson.getJsonObject(ADDRESSEE);
        assertThat(addresseeJson, notNullValue());
        assertThat(addresseeJson.getString("name"), is(NAME_HARRY_JACK_KANE));

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(addresseeAddressJson);

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject(HEARING_COURT_DETAILS);
        assertOnHearingCourtDetails(hearingCourtDetails);

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject(COURT_ADDRESS);
        assertOnCourtAddress(courtAddress, true);

        final JsonObject referralReason = summonsDataJson.getJsonObject("referralContent");
        assertOnReferralReason(referralReason, true);
    }

    @Test
    public void shouldGenerateSummonsPayloadForYouth() {
        //Given
        prepareDataAndConditionsForDocumentGeneration(generateCourtCentreJson(),
                SummonsRequired.YOUTH,
                false);

        summonsDataPreparedEventProcessor.requestSummons(envelope);
        verify(documentGeneratorService,
                times(2)).generateDocument(envelopeArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                templateArgumentCaptor.capture(),
                senderArgumentCaptor.capture(),
                caseIdArgumentCaptor.capture(),
                applicationIdArgumentCaptor.capture());

        //Then
        verifyCourtDocument(2);
        assertOnCaseIdAndEnvelope();
        final JsonObject summonsDataJson = jsonObjectArgumentCaptor.getValue();
        assertOnSummonsData(summonsDataJson, SummonsRequired.YOUTH, false);

        assertOnSummonsData(summonsDataJson,
                SummonsRequired.YOUTH,
                false);

        final JsonObject defendantJson = assertOnDefendant(summonsDataJson);
        final JsonObject defendantAddressJson = defendantJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(defendantAddressJson);

        final JsonArray offencesJson = summonsDataJson.getJsonArray(OFFENCES);
        assertOnOffences(offencesJson, false);

        final JsonObject addresseeJson = summonsDataJson.getJsonObject(ADDRESSEE);
        assertThat(addresseeJson, notNullValue());

        final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(addresseeAddressJson);

        final JsonObject youthJson = summonsDataJson.getJsonObject("youthContent");
        assertThat(youthJson, notNullValue());

        final JsonObject guardianAddressJson = youthJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(guardianAddressJson);

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject(HEARING_COURT_DETAILS);
        assertOnHearingCourtDetails(hearingCourtDetails);

        final JsonObject courtAddress = hearingCourtDetails.getJsonObject(COURT_ADDRESS);
        assertOnCourtAddress(courtAddress, false);
    }

    private void prepareDataAndConditionsForDocumentGeneration(final JsonObject jsonObject,
                                                               SummonsRequired summonsRequired,
                                                               final boolean welshRequiredFlag) {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SummonsDataPrepared.class)).thenReturn(summonsDataPrepared);
        when(envelope.metadata()).thenReturn(metadataWithRandomUUID("progression.event.summons-data-prepared").withUserId(USER_ID).build());

        final SummonsData summonsData = generateSummonsData(summonsRequired, CASE_ID, DEFENDANT_ID, COURT_CENTRE_ID, REFERRAL_ID);
        when(summonsDataPrepared.getSummonsData()).thenReturn(summonsData);

        when(progressionService.getProsecutionCaseDetailById(envelope, CASE_ID.toString())).thenReturn(Optional.of(generateProsecutionCaseJson(CASE_ID.toString(), DEFENDANT_ID.toString())));
        when(referenceDataService.getReferralReasons(envelope, requester)).thenReturn(Optional.of(generateReferralReasonsJson(REFERRAL_ID.toString())));
        when(referenceDataService.getOrganisationUnitById(COURT_CENTRE_ID, envelope, requester)).thenReturn(Optional.of(jsonObject));
        when(referenceDataService.getEnforcementAreaByLjaCode(envelope, "1810", requester)).thenReturn(generateLjaDetails());
        when(referenceDataService.getLocalJusticeArea(envelope, "2577", requester)).thenReturn(Optional.of(generateLjaDetailsWithWelsh(welshRequiredFlag)));

        when(referenceDataService.getDocumentTypeAccessData(SUMMONS_DOCUMENT_TYPE_ID, envelope, requester)).thenReturn(Optional.of(generateDocumentTypeAccess(SUMMONS_DOCUMENT_TYPE_ID)));
    }


    private void assertOnCaseIdAndEnvelope() {
        assertThat(caseIdArgumentCaptor.getValue(), is(CASE_ID));
        assertThat(envelopeArgumentCaptor.getValue(), is(envelope));
    }

    private void verifyCourtDocument(int times) {
        verify(this.sender, times(times)).send(this.envelopeArgumentCaptorForCourtDocument.capture());
        final List<JsonEnvelope> jsonEnvelope = this.envelopeArgumentCaptorForCourtDocument.getAllValues();
        IntStream.range(0, times).forEach( index -> {

            MatcherAssert.assertThat(jsonEnvelope.get(index), jsonEnvelope(
                    metadata().withName("progression.command.create-court-document"),
                    payloadIsJson(allOf(
                            withJsonPath("$.courtDocument.mimeType", Matchers.is("application/pdf")),
                            withJsonPath("$.courtDocument.documentTypeDescription", Matchers.is("Charges"))
                    ))));
                }
        );

    }

}
