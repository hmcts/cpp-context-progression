package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.HearingApplicationLinkCreated.hearingApplicationLinkCreated;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingApplicationLinkCreatedProcessorTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private Sender sender;

    @InjectMocks
    private HearingApplicationLinkCreatedProcessor hearingApplicationLinkCreatedProcessor;

    @Captor
    private ArgumentCaptor<Envelope> argumentCaptor;

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID COURTROOM_ID = UUID.randomUUID();

    @Test
    public void shouldCallCommand(){
        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre().withCode("A01").build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtRoomId(COURTROOM_ID).build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-application-link-created"),
                payload);

        hearingApplicationLinkCreatedProcessor.process(envelope);

        verify(sender).send(argumentCaptor.capture());
        JsonObject cmdPayload = (JsonObject) argumentCaptor.getValue().payload();
        assertThat(argumentCaptor.getValue().metadata().name(), is ("progression.command.update-hearing-for-allocation-fields"));
        assertThat(cmdPayload.getString("id"), is (HEARING_ID.toString()));
        assertThat(cmdPayload.getJsonObject("courtCentre").getString("code"), is ("A01"));
        assertThat(cmdPayload.getString("hearingLanguage"), is("ENGLISH"));
        assertThat(cmdPayload.getJsonArray("hearingDays").getJsonObject(0).getString("courtRoomId"), is (COURTROOM_ID.toString()));
        assertThat(cmdPayload.getJsonObject("courtApplication").getString("id"), is (APPLICATION_ID.toString()));
    }

    @Test
    public void shouldCallCommandWithoutHearingLanguage(){
        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre().withCode("A01").build())
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtRoomId(COURTROOM_ID).build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-application-link-created"),
                payload);

        hearingApplicationLinkCreatedProcessor.process(envelope);

        verify(sender).send(argumentCaptor.capture());
        JsonObject cmdPayload = (JsonObject) argumentCaptor.getValue().payload();
        assertThat(argumentCaptor.getValue().metadata().name(), is ("progression.command.update-hearing-for-allocation-fields"));
        assertThat(cmdPayload.getString("id"), is (HEARING_ID.toString()));
        assertThat(cmdPayload.getJsonObject("courtCentre").getString("code"), is ("A01"));
        assertThat(cmdPayload.getString("hearingLanguage", null), is(nullValue()));
        assertThat(cmdPayload.getJsonArray("hearingDays").getJsonObject(0).getString("courtRoomId"), is (COURTROOM_ID.toString()));
    }

    @Test
    public void shouldCallCommandWithoutApplication(){
        final Hearing hearing = Hearing.hearing().withId(HEARING_ID)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A01").build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtRoomId(COURTROOM_ID).build()))
                .build();

        HearingApplicationLinkCreated hearingApplicationLinkCreated
                = hearingApplicationLinkCreated()
                .withApplicationId(APPLICATION_ID)
                .withHearing(hearing)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingApplicationLinkCreated);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.hearing-application-link-created"),
                payload);

        hearingApplicationLinkCreatedProcessor.process(envelope);

        verify(sender).send(argumentCaptor.capture());
        JsonObject cmdPayload = (JsonObject) argumentCaptor.getValue().payload();
        assertThat(argumentCaptor.getValue().metadata().name(), is ("progression.command.update-hearing-for-allocation-fields"));
        assertThat(cmdPayload.getString("id"), is (HEARING_ID.toString()));
        assertThat(cmdPayload.getJsonObject("courtCentre").getString("code"), is ("A01"));
        assertThat(cmdPayload.getString("hearingLanguage"), is("ENGLISH"));
        assertThat(cmdPayload.getJsonArray("hearingDays").getJsonObject(0).getString("courtRoomId"), is (COURTROOM_ID.toString()));
        assertThat(cmdPayload.getJsonObject("courtApplication"), is (nullValue()));
    }
}
