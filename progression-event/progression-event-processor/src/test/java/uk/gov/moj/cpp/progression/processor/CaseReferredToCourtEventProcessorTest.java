package uk.gov.moj.cpp.progression.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.helper.TestHelper.buildJsonEnvelope;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.MessageService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ReferredCourtDocumentTransformer;
import uk.gov.moj.cpp.progression.transformer.ReferredProsecutionCaseTransformer;
import uk.gov.moj.cpp.progression.transformer.SendCaseForListingTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseReferredToCourtEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @InjectMocks
    private CasesReferredToCourtProcessor eventProcessor;
    @Mock
    private ReferredProsecutionCaseTransformer referredProsecutionCaseTransformer;
    @Mock
    private ReferredCourtDocumentTransformer referredCourtDocumentTransformer;
    @Mock
    private SendCaseForListingTransformer sendCaseForListingTransformer;

    @Mock
    private ProsecutionCase prosecutionCase;
    @Mock
    private MessageService messageService ;
    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObject courtReferralJson;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Sender sender;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ListingService listingService;

    @Mock
    private ProgressionService progressionService;

    @Rule
    public ExpectedException expectedException =ExpectedException.none();


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        // Setup
        PodamFactory factory = new PodamFactoryImpl();
        SjpCourtReferral sjpCourtReferral = factory.manufacturePojoWithFullData(SjpCourtReferral.class);
        SendCaseForListing sendCaseForListing= SendCaseForListing.sendCaseForListing().build();
        ProsecutionCase prosecutionCase=ProsecutionCase.prosecutionCase().build();
        CourtDocument courtDocument=CourtDocument.courtDocument().build();

//        final JsonEnvelope jsonEnvelope = buildJsonEnvelope();
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("searchResults", Json.createArrayBuilder().build
                        ()).build()));
        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any
                (JsonEnvelope.class))).thenReturn(prosecutionCase);
        when(referredCourtDocumentTransformer.transform(any(ReferredCourtDocument.class), any
                (JsonEnvelope.class))).thenReturn(courtDocument);
        when(sendCaseForListingTransformer.transform(any(), any(), any(), any(), any())).thenReturn
                (sendCaseForListing);


        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command" +
                ".create-prosecution-case")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.create-hearing-defendant-request")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);


        //When
        this.eventProcessor.process(jsonEnvelope);
        verify(listingService).sendCaseForListing(jsonEnvelope,sendCaseForListing);
        verify(progressionService).createProsecutionCases(any(), any());
        verify(progressionService).createCourtDocument(any(), any());
    }

    @Test
    public void shouldHandleExceptionsOnMissingRequiredData() throws Exception {
        // Setup
        PodamFactory factory = new PodamFactoryImpl();
        SjpCourtReferral sjpCourtReferral = factory.manufacturePojoWithFullData(SjpCourtReferral.class);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("searchResults", Json.createArrayBuilder().build
                        ()).build()));

        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any
                (JsonEnvelope.class))).thenThrow(new MissingRequiredFieldException("value"));

        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class),any(JsonObject.class),any(String.class));
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(sendCaseForListingTransformer);
    }

    @Test
    public void shouldHandleExceptionsOnRefData() throws Exception {
        // Setup
        PodamFactory factory = new PodamFactoryImpl();
        SjpCourtReferral sjpCourtReferral = factory.manufacturePojoWithFullData(SjpCourtReferral.class);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("searchResults", Json.createArrayBuilder().build
                        ()).build()));

        when(referredProsecutionCaseTransformer.transform(any(ReferredProsecutionCase.class), any
                (JsonEnvelope.class))).thenThrow(new ReferenceDataNotFoundException("Key","value"));


        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class),any(JsonObject.class),any(String.class));
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(sendCaseForListingTransformer);
    }

    @Test
    public void shouldHandleExceptionsOnSearch() throws Exception {
        // Setup
        PodamFactory factory = new PodamFactoryImpl();
        SjpCourtReferral sjpCourtReferral = factory.manufacturePojoWithFullData(SjpCourtReferral.class);
        //Given
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, SjpCourtReferral.class))
                .thenReturn(sjpCourtReferral);

        when(progressionService.searchCaseDetailByReference(any(), any())).thenReturn(Optional.of
                (Json.createObjectBuilder().add("searchResults", Json.createArrayBuilder().add("some value").build
                        ()).build()));

        //When
        this.eventProcessor.process(jsonEnvelope);

        verify(messageService).sendMessage(any(JsonEnvelope.class),any(JsonObject.class),any(String.class));
        verifyNoMoreInteractions(referredProsecutionCaseTransformer);
        verifyNoMoreInteractions(referredCourtDocumentTransformer);
        verifyNoMoreInteractions(sendCaseForListingTransformer);
    }
}