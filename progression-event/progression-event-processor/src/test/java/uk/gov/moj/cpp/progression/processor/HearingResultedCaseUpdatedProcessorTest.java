package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CustodialEstablishmentUpdateHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UpdateDefendantService;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class HearingResultedCaseUpdatedProcessorTest {

    @InjectMocks
    private HearingResultedCaseUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CustodialEstablishmentUpdateHelper custodialEstablishmentUpdateHelper;

    @Mock
    private UpdateDefendantService updateDefendantService;

    @Mock
    private RefDataService referenceDataService;
    @Mock
    private Requester requester;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private HearingResultHelper resultHelper;

    @Mock
    private ProgressionService progressionService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEventMessage() throws Exception {
        //When
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(mock(HearingResultedCaseUpdated.class));
        when(resultHelper.doProsecutionCasesContainNextHearingResults(any())).thenReturn(false);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.hearing-resulted-case-updated")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        this.eventProcessor.process(envelope);

        //Then
        verify(sender).send(finalEnvelope);
        verify(jsonObjectToObjectConverter).convert(any(),any());
        verify(resultHelper).doProsecutionCasesContainNextHearingResults(any());
        verify(progressionService,times(0)).updateCivilFees(any(),any());
        verify(envelope).payloadAsJsonObject();
        verify(enveloper).withMetadataFrom(envelope,"public.progression.hearing-resulted-case-updated");
    }

    @Test
    public void shouldHandleProsecutionCaseOffencesUpdatedEventMessageWhenHearingIsAdjourned() throws Exception {
        //When
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(mock(HearingResultedCaseUpdated.class));
        when(resultHelper.doProsecutionCasesContainNextHearingResults(any())).thenReturn(true);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.hearing-resulted-case-updated")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        this.eventProcessor.process(envelope);

        //Then
        verify(sender).send(finalEnvelope);
        verify(jsonObjectToObjectConverter).convert(any(),any());
        verify(resultHelper).doProsecutionCasesContainNextHearingResults(any());
        verify(progressionService).updateCivilFees(any(),any());
        verify(envelope).payloadAsJsonObject();
        verify(enveloper).withMetadataFrom(envelope,"public.progression.hearing-resulted-case-updated");
    }

    @Test
    public void shouldIssueCommandsToUpdateCustodialEstablishmentWhenDefendantHearingResultedInPrisonCustody() {
        final UUID caseId = randomUUID();
        final Defendant defendant1 = defendant().withId(randomUUID()).build();
        final CustodialEstablishment custodialEstablishment1 = custodialEstablishment().withId(randomUUID()).withCustody("HMP Birmingham").build();

        final HearingResultedCaseUpdated hearingResultedCaseUpdated = HearingResultedCaseUpdated.hearingResultedCaseUpdated()
                .withProsecutionCase(prosecutionCase().withId(caseId).withDefendants(singletonList(defendant1)).build())
                .build();

        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(hearingResultedCaseUpdated);
        when(resultHelper.doProsecutionCasesContainNextHearingResults(any())).thenReturn(false);
        when(custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant1, emptyList())).thenReturn(of(custodialEstablishment1));

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.hearing-resulted-case-updated"),
                objectToJsonObjectConverter.convert(hearingResultedCaseUpdated));

        this.eventProcessor.process(event);

        verify(updateDefendantService).updateDefendantCustodialEstablishment(event.metadata(), defendant1, custodialEstablishment1);
    }
}