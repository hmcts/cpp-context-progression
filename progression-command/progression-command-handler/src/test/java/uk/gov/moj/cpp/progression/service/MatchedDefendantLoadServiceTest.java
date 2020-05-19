package uk.gov.moj.cpp.progression.service;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.MatchedDefendantsResult;
import uk.gov.justice.core.courts.PartialMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.additionalproperties.AdditionalPropertiesModule;
import uk.gov.justice.services.common.converter.jackson.jsr353.InclusionAwareJSR353Module;
import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.helper.MatchedDefendantHelper;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class MatchedDefendantLoadServiceTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SAMPLE_PNC_ID = "2099/1234567L";
    private static final String SAMPLE_CRO_NUMBER = "123456/20L";
    private static final String SAMPLE_POSTCODE = "HA1 1QF";
    private static final String SAMPLE_ADDRESS_LINE1 = "addressLine1";
    private static final String SAMPLE_ADDRESS_LINE3 = "addressLine3";
    private static final String SAMPLE_ADDRESS_LINE4 = "addressLine4";
    private static final String SAMPLE_ADDRESS_LINE5 = "addressLine5";
    private static final String SAMPLE_LAST_NAME = "SMITT";
    private static final String SAMPLE_FIRST_NAME = "Teagan";
    private static final String SAMPLE_MIDDLE_NAME = "M.";
    private static final String SAMPLE_CASE_URN = "99AB21233";
    private static final String SAMPLE_PROSECUTION_AUTHORITY_REFERENCE = "PAR1234567";
    private static final LocalDate SAMPLE_DATE_OF_BIRTH = LocalDate.of(1987, 12, 5);
    private static final ZonedDateTime SAMPLE_COURT_PROCEEDINGS_INITIATED = ZonedDateTime.now();

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final boolean DEFAULT_PROCEEDINGS_CONCLUDED = false;
    private static final boolean DEFAULT_CROWN_OR_MAGISTRATES = true;
    private static final String PAGE_SIZE = "pageSize";
    private static final String START_FROM = "startFrom";
    private static final String PNC_ID = "pncId";
    private static final String CRO_NUMBER = "croNumber";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String ADDRESS_LINE = "addressLine1";
    private static final String PROCEEDINGS_CONCLUDED = "proceedingsConcluded";
    private static final String CROWN_OR_MAGISTRATES = "crownOrMagistrates";


    @Mock
    private Requester requester;

    @Mock
    private Envelope<?> envelope;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Envelope emptyCpSearchResponse;

    @Mock
    private Envelope cpSearchResponse;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantPartialMatchCreated.class,
            DefendantMatched.class, MasterDefendantIdUpdated.class, PartialMatchedDefendantSearchResultStored.class);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Spy
    private MatchedDefendantHelper matchedDefendantHelper;

    @InjectMocks
    private MatchedDefendantLoadService matchedDefendantLoadService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> unifiedSearchQueryParamCapture;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> persistEventCapture;

    private CaseAggregate caseAggregate;

    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule(PROPERTIES))
                .registerModule(new JavaTimeModule())
                .registerModule(new InclusionAwareJSR353Module())
                .registerModule(new AdditionalPropertiesModule());
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter);
        setField(this.matchedDefendantHelper, "listToJsonArrayConverter", this.listToJsonArrayConverter);

        caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(UUID.randomUUID())
                .withName("unifiedsearch.query.defendant.cases");
        when(envelope.metadata()).thenReturn(metadataBuilder.build());

        when(emptyCpSearchResponse.payload())
                .thenReturn(MatchedDefendantsResult.matchedDefendantsResult()
                        .withTotalResults(0)
                        .withCases(new ArrayList<>())
                        .build());
    }

    @Test
    public void shouldNotCallCpSearch_whenDefendantHasNotPersonDetails() throws EventStreamException {
        //given
        final Defendant defendant = Defendant.defendant().build();
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        //then
        verify(requester, never()).request(any());
        verify(eventStream, never()).append(persistEventCapture.capture());
    }

    @Test
    public void shouldHandleException_whenCpSearchThrowsException() throws EventStreamException {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);

        when(requester.requestAsAdmin(any(), any())).thenThrow(new AccessControlViolationException(""));

        try {
            matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);
            assertThat("AccessControlViolationException was expected", false);
        } catch (AccessControlViolationException e) {
            verify(requester).requestAsAdmin(any(), any());
        }
    }

    @Test
    public void shouldCallCpSearch() throws EventStreamException {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(3)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));

        final JsonObject firstCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(0).payload();
        assertThat(firstCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(firstCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(firstCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(firstCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(firstCallUnifiedSearchQueryParam.containsKey(PNC_ID), is(true));
        assertThat(firstCallUnifiedSearchQueryParam.containsKey(LAST_NAME), is(true));
        assertThat(firstCallUnifiedSearchQueryParam.entrySet().size(), is(6));

        final JsonObject secondCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(1).payload();
        assertThat(secondCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(secondCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(secondCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(secondCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(secondCallUnifiedSearchQueryParam.containsKey(CRO_NUMBER), is(true));
        assertThat(secondCallUnifiedSearchQueryParam.containsKey(LAST_NAME), is(true));
        assertThat(secondCallUnifiedSearchQueryParam.entrySet().size(), is(6));

        final JsonObject thirdCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(2).payload();
        assertThat(thirdCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(thirdCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(thirdCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(thirdCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(thirdCallUnifiedSearchQueryParam.containsKey(FIRST_NAME), is(true));
        assertThat(thirdCallUnifiedSearchQueryParam.containsKey(DATE_OF_BIRTH), is(true));
        assertThat(thirdCallUnifiedSearchQueryParam.containsKey(ADDRESS_LINE), is(true));
        assertThat(thirdCallUnifiedSearchQueryParam.containsKey(LAST_NAME), is(true));
        assertThat(thirdCallUnifiedSearchQueryParam.entrySet().size(), is(8));
    }

    @Test
    public void shouldNotCallCpSearchTwice_whenFirstCallReturnRecords() throws EventStreamException {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class))).thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
    }

    @Test
    public void shouldNotCallCpSearchThirdTimes_whenSecondCallReturnRecords() throws EventStreamException {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), any()))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(2)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
    }


    @Test
    public void shouldCallMultipleTimesCpSearchForExact_whenExactSearchGreaterThanZero() throws EventStreamException {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE * 3 - 1), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class))).thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(3)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));

        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(0).payload().getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(0).payload().getInt(START_FROM), is(0));

        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(1).payload().getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(1).payload().getInt(START_FROM), is(1));

        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(2).payload().getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(2).payload().getInt(START_FROM), is(2));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(2).payload().getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(2).payload().getString(PNC_ID), is(SAMPLE_PNC_ID));
    }


    @Test
    public void shouldCallPartialSearch_whenExactSearchReturnEmpty() throws Throwable {
        final DefendantPartialMatchCreated defendantPartialMatchCreated = DefendantPartialMatchCreated.defendantPartialMatchCreated().build();
        caseAggregate.apply(DefendantPartialMatchCreated.defendantPartialMatchCreated().build());
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.event.defendant-partial-match-created")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<DefendantPartialMatchCreated> envelope = envelopeFrom(metadata, defendantPartialMatchCreated);

        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(8)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
        verify(eventStream, times(1)).append(persistEventCapture.capture());

        final JsonEnvelope persistEnvelope = persistEventCapture.getAllValues().get(0).findFirst().orElseThrow(RuntimeException::new);
        assertThat(persistEnvelope.metadata().name(), is("progression.event.partial-matched-defendant-search-result-stored"));
        assertThat(persistEnvelope.payloadAsJsonObject().getJsonArray("cases").size(), is(2));

        final JsonObject firstCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(3).payload();
        assertThat(firstCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(firstCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(firstCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(firstCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(firstCallUnifiedSearchQueryParam.getString(PNC_ID), is(SAMPLE_PNC_ID));
        assertThat(firstCallUnifiedSearchQueryParam.entrySet().size(), is(5));

        final JsonObject secondCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(4).payload();
        assertThat(secondCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(secondCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(secondCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(secondCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(secondCallUnifiedSearchQueryParam.getString(CRO_NUMBER), is(SAMPLE_CRO_NUMBER));
        assertThat(secondCallUnifiedSearchQueryParam.entrySet().size(), is(5));

        final JsonObject thirdCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(5).payload();
        assertThat(thirdCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(thirdCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(thirdCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(thirdCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(thirdCallUnifiedSearchQueryParam.getString(LAST_NAME), is(SAMPLE_LAST_NAME));
        assertThat(thirdCallUnifiedSearchQueryParam.getString(ADDRESS_LINE), is(SAMPLE_ADDRESS_LINE1));
        assertThat(thirdCallUnifiedSearchQueryParam.getString(DATE_OF_BIRTH), is(FORMATTER.format(SAMPLE_DATE_OF_BIRTH)));
        assertThat(thirdCallUnifiedSearchQueryParam.entrySet().size(), is(7));

        final JsonObject fourthCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(6).payload();
        assertThat(fourthCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(fourthCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(fourthCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(fourthCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(fourthCallUnifiedSearchQueryParam.getString(LAST_NAME), is(SAMPLE_LAST_NAME));
        assertThat(fourthCallUnifiedSearchQueryParam.getString(DATE_OF_BIRTH), is(FORMATTER.format(SAMPLE_DATE_OF_BIRTH)));
        assertThat(fourthCallUnifiedSearchQueryParam.entrySet().size(), is(6));

        final JsonObject fifthCallUnifiedSearchQueryParam = unifiedSearchQueryParamCapture.getAllValues().get(7).payload();
        assertThat(fifthCallUnifiedSearchQueryParam.getInt(PAGE_SIZE), is(DEFAULT_PAGE_SIZE));
        assertThat(fifthCallUnifiedSearchQueryParam.getInt(START_FROM), is(0));
        assertThat(fifthCallUnifiedSearchQueryParam.getBoolean(PROCEEDINGS_CONCLUDED), is(DEFAULT_PROCEEDINGS_CONCLUDED));
        assertThat(fifthCallUnifiedSearchQueryParam.getBoolean(CROWN_OR_MAGISTRATES), is(DEFAULT_CROWN_OR_MAGISTRATES));
        assertThat(fifthCallUnifiedSearchQueryParam.getString(ADDRESS_LINE), is(SAMPLE_ADDRESS_LINE1));
        assertThat(fifthCallUnifiedSearchQueryParam.getString(DATE_OF_BIRTH), is(FORMATTER.format(SAMPLE_DATE_OF_BIRTH)));
        assertThat(fourthCallUnifiedSearchQueryParam.entrySet().size(), is(6));
    }

    @Test
    public void shouldNotSecondCallPartialSearch_whenExactSearchReturnEmptyAndFirstPartialCallReturnsRecord() throws Throwable {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);
        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(4)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
        verify(eventStream, times(1)).append(persistEventCapture.capture());
    }

    @Test
    public void shouldNotThirdCallPartialSearch_whenExactSearchReturnEmptyAndSecondPartialCallReturnsRecord() throws Throwable {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(5)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
        verify(eventStream, times(1)).append(persistEventCapture.capture());
    }

    @Test
    public void shouldNotFourthCallPartialSearch_whenExactSearchReturnEmptyAndThirdPartialCallReturnsRecord() throws Throwable {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(6)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
        verify(eventStream, times(1)).append(persistEventCapture.capture());
    }

    @Test
    public void shouldNotFifthCallPartialSearch_whenExactSearchReturnEmptyAndFourthPartialCallReturnsRecord() throws Throwable {
        final Defendant defendant = getSampleDefendant("");
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(SAMPLE_CASE_URN, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        verify(requester, times(7)).requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class));
        verify(eventStream, times(1)).append(persistEventCapture.capture());
    }

    @Test
    public void shouldCallMultipleTimesPartialSearch_whenExactSearchReturnEmptyAndTotalCountGreaterThanPageSize() throws EventStreamException {
        //given
        final DefendantPartialMatchCreated defendantPartialMatchCreated = DefendantPartialMatchCreated.defendantPartialMatchCreated().build();
        caseAggregate.apply(DefendantPartialMatchCreated.defendantPartialMatchCreated().build());
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.event.defendant-partial-match-created")
                .withId(UUID.randomUUID())
                .build();
        final Envelope<DefendantPartialMatchCreated> envelope = envelopeFrom(metadata, defendantPartialMatchCreated);

        final Defendant defendant = getSampleDefendant(SAMPLE_MIDDLE_NAME);
        final ProsecutionCase prosecutionCase = getSampleProsecutionCase(null, SAMPLE_PROSECUTION_AUTHORITY_REFERENCE, defendant);
        final MatchedDefendantsResult result = jsonObjectConverter.convert(getUnifiedSearchResult(DEFAULT_PAGE_SIZE + 1), MatchedDefendantsResult.class);

        when(cpSearchResponse.payload()).thenReturn(result);

        when(requester.requestAsAdmin(unifiedSearchQueryParamCapture.capture(), eq(MatchedDefendantsResult.class)))
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(emptyCpSearchResponse)
                .thenReturn(cpSearchResponse);

        //when
        matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(envelope, prosecutionCase);

        //then
        verify(requester, times(5)).requestAsAdmin(any(), eq(MatchedDefendantsResult.class));
        verify(eventStream).append(persistEventCapture.capture());

        final JsonEnvelope persistEnvelope = persistEventCapture.getAllValues().get(0).findFirst().orElseThrow(RuntimeException::new);
        assertThat(persistEnvelope.metadata().name(), is("progression.event.partial-matched-defendant-search-result-stored"));
        assertThat(persistEnvelope.payloadAsJsonObject().getJsonArray("cases").size(), is(4));

        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(3).payload().getInt(START_FROM), is(0));
        assertThat(unifiedSearchQueryParamCapture.getAllValues().get(4).payload().getInt(START_FROM), is(1));
    }

    private ProsecutionCase getSampleProsecutionCase(final String caseUrn, final String prosecutionAuthorityReference, Defendant defendant) {
        return ProsecutionCase.prosecutionCase()
                .withId(UUID.randomUUID())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityReference(prosecutionAuthorityReference)
                        .build())
                .withDefendants(Arrays.asList(defendant))
                .build();
    }

    private Defendant getSampleDefendant(String middleName) {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withMasterDefendantId(UUID.randomUUID())
                .withPncId(SAMPLE_PNC_ID)
                .withCroNumber(SAMPLE_CRO_NUMBER)
                .withCourtProceedingsInitiated(SAMPLE_COURT_PROCEEDINGS_INITIATED)
                .withProceedingsConcluded(true)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName(SAMPLE_LAST_NAME)
                                .withFirstName(SAMPLE_FIRST_NAME)
                                .withMiddleName(middleName)
                                .withDateOfBirth(SAMPLE_DATE_OF_BIRTH)
                                .withAddress(Address.address()
                                        .withPostcode(SAMPLE_POSTCODE)
                                        .withAddress1(SAMPLE_ADDRESS_LINE1)
                                        .withAddress3(SAMPLE_ADDRESS_LINE3)
                                        .withAddress4(SAMPLE_ADDRESS_LINE4)
                                        .withAddress5(SAMPLE_ADDRESS_LINE5)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private JsonObject getUnifiedSearchResult(final Integer totalCount) {
        return stringToJsonObjectConverter.convert("{\n" +
                "  \"totalResults\": " + totalCount + ",\n" +
                "  \"cases\": [\n" +
                "    {\n" +
                "      \"caseReference\": \"20GB12345666\",\n" +
                "      \"prosecutionCaseId\": \"31af405e-7b60-4dd8-a244-c24c2d3fa595\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-02T10:00:00.000Z\",\n" +
                "          \"defendantId\": \"9e4932f7-97b2-3010-b942-ddd2624e4dd8\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"firstName\": \"Teagan\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"SMITT\",\n" +
                "          \"dateOfBirth\": \"2019-04-21\",\n" +
                "          \"pncId\": \"2099/1234567L\",\n" +
                "          \"croNumber\": \"\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"15, somewhere street\",\n" +
                "            \"address2\": \"15th Lane\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"caseReference\": \"45GB12345777\",\n" +
                "      \"prosecutionCaseId\": \"7e967376-eacf-4fca-9b30-21b0c5aad427\",\n" +
                "      \"defendants\": [\n" +
                "        {\n" +
                "          \"defendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"masterDefendantId\": \"0a5372c5-b60f-4d95-8390-8c6462e2d7af\",\n" +
                "          \"courtProceedingsInitiated\": \"2020-03-03T10:00:00.000Z\",\n" +
                "          \"firstName\": \"Teagan\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"SITH\",\n" +
                "          \"dateOfBirth\": \"\",\n" +
                "          \"pncId\": \"2098/1234568L\",\n" +
                "          \"croNumber\": \"123456/20L\",\n" +
                "          \"address\": {\n" +
                "            \"address1\": \"19, ABC street\",\n" +
                "            \"address2\": \"19th Lane\",\n" +
                "            \"address3\": \"\",\n" +
                "            \"address4\": \"\",\n" +
                "            \"address5\": \"London\",\n" +
                "            \"postcode\": \"HA1 1QF\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}");
    }
}
