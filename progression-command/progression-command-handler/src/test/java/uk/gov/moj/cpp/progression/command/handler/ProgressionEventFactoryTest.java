package uk.gov.moj.cpp.progression.command.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.handler.matchers.DefendantEventMatcher;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.test.utils.DefendantBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ProgressionEventFactoryTest {

    private static final String PROGRESSION_ID = UUID.randomUUID().toString();
    private static final String CASE_ID = UUID.randomUUID().toString();
    @Mock
    JsonEnvelope envelope;
    @Mock
    JsonObject jsonObj;

    @InjectMocks
    ProgressionEventFactory progressionEventFactory;



    @Before
    public void SetUp() {
        when(envelope.payloadAsJsonObject()).thenReturn(jsonObj);
        when(jsonObj.getString(Mockito.eq("caseProgressionId"))).thenReturn(PROGRESSION_ID);
        when(jsonObj.getString(Mockito.eq("caseId"))).thenReturn(CASE_ID);
        when(jsonObj.getString(Mockito.eq("isKeyEvidence"))).thenReturn("true");
        when(jsonObj.getString(Mockito.eq("planDate"))).thenReturn(LocalDate.now().toString());
        when(jsonObj.getString(Mockito.eq("sendingCommittalDate")))
                        .thenReturn(LocalDate.now().toString());
        when(jsonObj.getString(Mockito.eq("sentenceHearingDate")))
                        .thenReturn(LocalDate.now().toString());
        when(jsonObj.getJsonArray(Mockito.eq("defendants")))
                        .thenReturn(Json.createArrayBuilder().add(Json.createObjectBuilder()
                                        .add("id", UUID.randomUUID().toString()).build()).build());
    }

    @Test
    public void testCreateCaseAddedToCrownCourt() {
        final Object obj = progressionEventFactory.createCaseAddedToCrownCourt(envelope);
        assertThat(obj, instanceOf(CaseAddedToCrownCourt.class));
    }


    @Test
    public void testCreateSendingCommittalHearingInformationAdded() {
        final Object obj = progressionEventFactory
                        .createSendingCommittalHearingInformationAdded(envelope);
        assertThat(obj, instanceOf(SendingCommittalHearingInformationAdded.class));
    }

    @Test
    public void testCreateDirectionIssued() {
        final Object obj = progressionEventFactory.createDirectionIssued(envelope);
        assertThat(obj, instanceOf(DirectionIssued.class));
    }

    @Test
    public void testCreateSentenceHearingDateAdded() {
        final Object obj = progressionEventFactory.createSentenceHearingDateAdded(envelope);
        assertThat(obj, instanceOf(SentenceHearingDateAdded.class));
    }

    @Test
    public void testCreateCaseToBeAssignedUpdated() {
        final Object obj = progressionEventFactory.createCaseToBeAssignedUpdated(envelope);
        assertThat(obj, instanceOf(CaseToBeAssignedUpdated.class));
    }

    @Test
    public void testCreateCaseAssignedForReviewUpdated() {
        final Object obj = progressionEventFactory.createCaseAssignedForReviewUpdated(envelope);
        assertThat(obj, instanceOf(CaseAssignedForReviewUpdated.class));
    }

    @Test
    public void testCreateCaseReadyForSentenceHearing() {
        final CaseReadyForSentenceHearing obj =
                        (CaseReadyForSentenceHearing) progressionEventFactory
                                        .createCaseReadyForSentenceHearing(envelope);

        assertThat(PROGRESSION_ID, equalTo(obj.getCaseProgressionId().toString()));
        assertThat(CaseStatusEnum.READY_FOR_SENTENCING_HEARING, equalTo(obj.getStatus()));
        assertThat(LocalDateTime.now().toLocalDate(),
                        equalTo(obj.getReadyForSentenceHearingDate().toLocalDate()));

    }

    @Test
    public void shouldAddDefendantEvent() {
        // given
        final DefendantCommand defendant = DefendantBuilder.defaultDefendant();

        // when
        final DefendantAdditionalInformationAdded defendantEvent =
                        (DefendantAdditionalInformationAdded) progressionEventFactory
                                        .addDefendantEvent(defendant);

        // then
        assertThat(defendantEvent, sameAs(defendant));
    }

    private Matcher<DefendantAdditionalInformationAdded> sameAs(
                    final DefendantCommand defendantCommand) {
        return new DefendantEventMatcher(defendantCommand);
    }



    private UUID randomUUID() {
        return UUID.randomUUID();
    }

    private JsonEnvelope createJsonCommand() {
        final JsonObject metadataAsJsonObject = Json.createObjectBuilder().add(ID, PROGRESSION_ID)
                        .add(NAME, "SomeName").build();

        final JsonObject payloadAsJsonObject = Json.createObjectBuilder().build();

        return DefaultJsonEnvelope.envelopeFrom(
                        JsonObjectMetadata.metadataFrom(metadataAsJsonObject), payloadAsJsonObject);

    }
}
