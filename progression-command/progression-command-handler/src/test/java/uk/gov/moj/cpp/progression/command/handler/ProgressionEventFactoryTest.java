package uk.gov.moj.cpp.progression.command.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.ID;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.NAME;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
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
import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.handler.matchers.DefendantEventMatcher;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;

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
        when(jsonObj.getString(Mockito.eq("version"))).thenReturn("1");
        when(jsonObj.getString(Mockito.eq("isKeyEvidence"))).thenReturn("true");
        when(jsonObj.getString(Mockito.eq("indicateStatementId")))
                        .thenReturn(UUID.randomUUID().toString());
        when(jsonObj.getString(Mockito.eq("planDate"))).thenReturn(LocalDate.now().toString());
        when(jsonObj.getString(Mockito.eq("sendingCommittalDate")))
                        .thenReturn(LocalDate.now().toString());
        when(jsonObj.getString(Mockito.eq("sentenceHearingDate")))
                        .thenReturn(LocalDate.now().toString());
        when(jsonObj.getJsonArray(Mockito.eq("defendants")))
                        .thenReturn(Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                        .add("id", UUID.randomUUID().toString())
                                                        .build())
                                        .build());
    }

    @Test
    public void testCreateCaseSentToCrownCourt() {
        Object obj = progressionEventFactory.createCaseSentToCrownCourt(envelope);
        assertThat(obj, instanceOf(CaseSentToCrownCourt.class));
    }

    @Test
    public void testCreateCaseAddedToCrownCourt() {
        Object obj = progressionEventFactory.createCaseAddedToCrownCourt(envelope);
        assertThat(obj, instanceOf(CaseAddedToCrownCourt.class));
    }

    @Test
    public void testCreateDefenceIssuesAdded() {
        Object obj = progressionEventFactory.createDefenceIssuesAdded(envelope);
        assertThat(obj, instanceOf(DefenceIssuesAdded.class));
    }

    @Test
    public void testCreateSfrIssuesAdded() {
        Object obj = progressionEventFactory.createSfrIssuesAdded(envelope);
        assertThat(obj, instanceOf(SfrIssuesAdded.class));
    }

    @Test
    public void testCreateSendingCommittalHearingInformationAdded() {
        Object obj = progressionEventFactory
                        .createSendingCommittalHearingInformationAdded(envelope);
        assertThat(obj, instanceOf(SendingCommittalHearingInformationAdded.class));
    }

    @Test
    public void testCreateDefenceTrialEstimateAdded() {
        Object obj = progressionEventFactory.createDefenceTrialEstimateAdded(envelope);
        assertThat(obj, instanceOf(DefenceTrialEstimateAdded.class));
    }

    @Test
    public void testCreateProsecutionTrialEstimateAdded() {
        Object obj = progressionEventFactory.createProsecutionTrialEstimateAdded(envelope);
        assertThat(obj, instanceOf(ProsecutionTrialEstimateAdded.class));
    }

    @Test
    public void testCreateDirectionIssued() {
        Object obj = progressionEventFactory.createDirectionIssued(envelope);
        assertThat(obj, instanceOf(DirectionIssued.class));
    }

    @Test
    public void testCreatePreSentenceReportOrdered() {
        Object obj = progressionEventFactory.createPreSentenceReportOrdered(envelope);
        assertThat(obj, instanceOf(PreSentenceReportOrdered.class));
    }

    @Test
    public void testCreateIndicateEvidenceServed() {
        Object obj = progressionEventFactory.createIndicateEvidenceServed(envelope);
        assertThat(obj, instanceOf(IndicateEvidenceServed.class));
    }

    @Test
    public void testCreateAllStatementsIdentified() {
        Object obj = progressionEventFactory.createAllStatementsIdentified(envelope);
        assertThat(obj, instanceOf(AllStatementsIdentified.class));
    }

    @Test
    public void testCreateAllStatementsServed() {
        Object obj = progressionEventFactory.createAllStatementsServed(envelope);
        assertThat(obj, instanceOf(AllStatementsServed.class));
    }

    @Test
    public void testCreatePTPHearingVacated() {
        Object obj = progressionEventFactory.createPTPHearingVacated(envelope);
        assertThat(obj, instanceOf(PTPHearingVacated.class));
    }

    @Test
    public void testCreateSentenceHearingDateAdded() {
        Object obj = progressionEventFactory.createSentenceHearingDateAdded(envelope);
        assertThat(obj, instanceOf(SentenceHearingDateAdded.class));
    }

    @Test
    public void testCreateCaseToBeAssignedUpdated() {
        Object obj = progressionEventFactory.createCaseToBeAssignedUpdated(envelope);
        assertThat(obj, instanceOf(CaseToBeAssignedUpdated.class));
    }

    @Test
    public void testCreateCaseAssignedForReviewUpdated() {
        Object obj = progressionEventFactory.createCaseAssignedForReviewUpdated(envelope);
        assertThat(obj, instanceOf(CaseAssignedForReviewUpdated.class));
    }

    @Test
    public void testCreateCaseReadyForSentenceHearing() {
        CaseReadyForSentenceHearing obj = (CaseReadyForSentenceHearing) progressionEventFactory
                        .createCaseReadyForSentenceHearing(envelope);

        assertThat(PROGRESSION_ID, equalTo(obj.getCaseProgressionId().toString()));
        assertThat(CaseStatusEnum.READY_FOR_SENTENCING_HEARING, equalTo(obj.getStatus()));
        assertThat(LocalDateTime.now().toLocalDate(),
                        equalTo(obj.getReadyForSentenceHearingDate().toLocalDate()));

    }

    @Test
    public void shouldAddDefendantEvent() {
        // given
        UUID defendantId = randomUUID();
        UUID defendantProgressionId = randomUUID();
        MedicalDocumentationCommand medicalDocumentation = new MedicalDocumentationCommand();
        medicalDocumentation.setDetails(randomString());

        DefenceCommand defence = new DefenceCommand();
        defence.setMedicalDocumentation(medicalDocumentation);

        AncillaryOrdersCommand ancillaryOrders = new AncillaryOrdersCommand();
        ancillaryOrders.setDetails(randomString());

        AdditionalInformationCommand additionalInformation = new AdditionalInformationCommand();
        additionalInformation.setDefence(defence);

        PreSentenceReportCommand preSentenceReport = new PreSentenceReportCommand();
        preSentenceReport.setDrugAssessment(randomBoolean());
        preSentenceReport.setProvideGuidance(randomString());

        ProbationCommand probation = new ProbationCommand();
        probation.setDangerousnessAssessment(randomBoolean());
        probation.setPreSentenceReport(preSentenceReport);

        additionalInformation.setProbation(probation);

        DefendantCommand defendant = new DefendantCommand();
        defendant.setDefendantId(defendantId);
        defendant.setDefendantProgressionId(defendantProgressionId);
        defendant.setAdditionalInformation(additionalInformation);

        // when
        DefendantEvent defendantEvent =
                        (DefendantEvent) progressionEventFactory.addDefendantEvent(defendant);

        // then
        assertThat(defendantEvent, sameAs(defendant));
    }

    private Matcher<DefendantEvent> sameAs(final DefendantCommand defendantCommand) {
        return new DefendantEventMatcher(defendantCommand);
    }

    private String randomString() {
        return RandomStringUtils.randomAlphanumeric(5);
    }

    private LocalDate randomDate() {
        return LocalDate.now();
    }

    private Boolean randomBoolean() {
        return new Random().nextBoolean();
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
