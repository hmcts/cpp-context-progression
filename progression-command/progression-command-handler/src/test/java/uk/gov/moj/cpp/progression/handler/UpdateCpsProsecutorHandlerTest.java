package uk.gov.moj.cpp.progression.handler;


import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UpdateCpsProsecutorDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCpsProsecutorHandlerTest {

    private static final String CONTACT = "abx.xqz.com";
    private static final String MAJOR_CREDITOR_CODE = "OUCode";
    private static final String AUTH_OU_CODE = "B01EF01";
    private static final String PROSECUTOR_CODE = "BLOO1";

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
    @Mock
    private Sender sender;

    @InjectMocks
    private UpdateCpsProsecutorHandler updateCpsProsecutorHandler;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private Stream<Object> events;

    @Mock
    private RefDataService referenceDataService;

    private final UUID caseId = randomUUID();

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    @Captor
    private ArgumentCaptor<ProsecutionCaseIdentifier> cpsProsecutorCaptor;

    @Captor
    private ArgumentCaptor<String> cpsOldProsecutorCaptor;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        this.caseAggregate.createProsecutionCase(ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode("SL00Q")
                        .withProsecutionAuthorityName("TFL")
                        .build())
                .withDefendants(Collections.emptyList())
                .build());

        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldHandleUpdateCpsProsecutor() throws Exception {

        UUID prosecutionAuthorityId = randomUUID();
        Address address = Address.address().withAddress1("adres1").build();
        final UpdateCpsProsecutorDetails cpsProsecutorDetails = UpdateCpsProsecutorDetails.updateCpsProsecutorDetails()
                .withOldCpsProsecutor(PROSECUTOR_CODE)
                .withProsecutionCaseId(caseId)
                .withProsecutionAuthorityCode("TFL")
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityName("Name")
                .withContact(CONTACT)
                .withMajorCreditorCode(MAJOR_CREDITOR_CODE)
                .withProsecutionAuthorityOUCode(AUTH_OU_CODE)
                .withAddress(address)
                .build();

        ProsecutionCaseIdentifier prosecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode("TFL")
                .withProsecutionAuthorityId(prosecutionAuthorityId)
                .withProsecutionAuthorityName("Name")
                .withProsecutionAuthorityOUCode(AUTH_OU_CODE)
                .withAddress(address)
                .withContact(ContactNumber.contactNumber().withPrimaryEmail(CONTACT).build())
                .withMajorCreditorCode(MAJOR_CREDITOR_CODE)
                .build();

        when(caseAggregate.updateCaseProsecutorDetails(any(), any())).thenReturn(events);

        final Envelope<UpdateCpsProsecutorDetails> envelope = Envelope.envelopeFrom(
                metadataFor("progression.command.update-cps-prosecutor-details", randomUUID()), cpsProsecutorDetails);

        updateCpsProsecutorHandler.handleUpdateCpsProsecutor(envelope);

        verify(caseAggregate, times(1)).updateCaseProsecutorDetails(cpsProsecutorCaptor.capture(), cpsOldProsecutorCaptor.capture());

        assertThat(cpsOldProsecutorCaptor.getValue(), is(PROSECUTOR_CODE));
        assertThat(objectToJsonObjectConverter.convert(cpsProsecutorCaptor.getValue()), is(objectToJsonObjectConverter.convert(prosecutionCaseIdentifier)));

    }

    @Test
    public void shouldUpdateProsecutionAuthorityWhenCpsOrganisationIsValid() throws Exception {
        final JsonObject prosecutorFromReferenceData = handlerTestHelper.convertFromFile("json/cps_prosecutor_from_reference_data.json", JsonObject.class);
        final ProsecutionCaseIdentifier expectedProsecutor = handlerTestHelper.convertFromFile("json/cps_prosecutor.json", ProsecutionCaseIdentifier.class);
        when(caseAggregate.getProsecutionCase()).thenReturn(ProsecutionCase.prosecutionCase().withCpsOrganisation("GAFTL00").withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build()).build());
        when(referenceDataService.getCPSProsecutorByOuCode(any(), eq("GAFTL00"), any())).thenReturn(of(prosecutorFromReferenceData));
        when(caseAggregate.updateCaseProsecutorDetails(any(ProsecutionCaseIdentifier.class))).thenReturn(events);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.command.update-case-for-cps-prosecutor"),
                createObjectBuilder().add("caseId", caseId.toString()).build());

        updateCpsProsecutorHandler.handleUpdateCpsProsecutorFromReferenceData(jsonEnvelope);

        verify(referenceDataService, times(1)).getCPSProsecutorByOuCode(any(), eq("GAFTL00"), any());
        verify(caseAggregate, times(1)).updateCaseProsecutorDetails(cpsProsecutorCaptor.capture());
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = cpsProsecutorCaptor.getValue();

        assertThat(objectToJsonObjectConverter.convert(prosecutionCaseIdentifier), is(objectToJsonObjectConverter.convert(expectedProsecutor)));
    }

    @Test
    public void shouldUpdateWithNullProsecutorCaseIdentifierWhenCpsOrganisationIsInNull() throws Exception {
        when(caseAggregate.getProsecutionCase()).thenReturn(ProsecutionCase.prosecutionCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build()).build());
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.command.update-case-for-cps-prosecutor"),
                createObjectBuilder().add("caseId", caseId.toString()).build());

        updateCpsProsecutorHandler.handleUpdateCpsProsecutorFromReferenceData(jsonEnvelope);

        verify(referenceDataService, times(0)).getCPSProsecutorByOuCode(any(), eq("GAFTL00"), any());
        verify(caseAggregate, times(1)).updateCaseProsecutorDetails(cpsProsecutorCaptor.capture());
        assertNull(cpsProsecutorCaptor.getValue());
    }

    @Test
    public void shouldUpdateProsecutionAuthorityWhenCpsOrganisationIsInNullAndCpsOrganisationIdIsNotNull() throws Exception {
        final UUID cpsOrgId = randomUUID();
        when(caseAggregate.getProsecutionCase()).thenReturn(ProsecutionCase.prosecutionCase().withCpsOrganisationId(cpsOrgId).withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build()).build());
        final JsonObject prosecutorFromReferenceData = handlerTestHelper.convertFromFile("json/cps_prosecutor_from_reference_data.json", JsonObject.class);
        when(referenceDataService.getProsecutor(any(), eq(cpsOrgId), any())).thenReturn(of(prosecutorFromReferenceData));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.command.update-case-for-cps-prosecutor"),
                createObjectBuilder().add("caseId", caseId.toString()).build());
        when(caseAggregate.updateCaseProsecutorDetails(any(ProsecutionCaseIdentifier.class))).thenReturn(events);

        updateCpsProsecutorHandler.handleUpdateCpsProsecutorFromReferenceData(jsonEnvelope);

        verify(referenceDataService, times(1)).getProsecutor(any(), eq(cpsOrgId), any());
        verify(caseAggregate, times(1)).updateCaseProsecutorDetails(cpsProsecutorCaptor.capture());
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = cpsProsecutorCaptor.getValue();
        final ProsecutionCaseIdentifier expectedProsecutor = handlerTestHelper.convertFromFile("json/cps_prosecutor.json", ProsecutionCaseIdentifier.class);
        assertThat(objectToJsonObjectConverter.convert(prosecutionCaseIdentifier), is(objectToJsonObjectConverter.convert(expectedProsecutor)));
    }

    @Test
    public void shouldUpdateWithCpsProsecutorFlagAsFalseWhenCpsOrganisationIsInValid() throws Exception {
        final JsonObject prosecutorFromReferenceData = handlerTestHelper.convertFromFile("json/cps_prosecutor_from_reference_data_invalid_cps.json", JsonObject.class);
        when(caseAggregate.getProsecutionCase()).thenReturn(ProsecutionCase.prosecutionCase().withCpsOrganisation("GAFTL00").withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build()).build());
        when(referenceDataService.getCPSProsecutorByOuCode(any(), eq("GAFTL00"), any())).thenReturn(of(prosecutorFromReferenceData));
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.command.update-case-for-cps-prosecutor"),
                createObjectBuilder().add("caseId", caseId.toString()).build());
        when(caseAggregate.updateCaseProsecutorDetails(any(ProsecutionCaseIdentifier.class))).thenReturn(events);
        updateCpsProsecutorHandler.handleUpdateCpsProsecutorFromReferenceData(jsonEnvelope);

        verify(referenceDataService, times(1)).getCPSProsecutorByOuCode(any(), eq("GAFTL00"), any());
        verify(caseAggregate, times(1)).updateCaseProsecutorDetails(cpsProsecutorCaptor.capture());
    }
}
