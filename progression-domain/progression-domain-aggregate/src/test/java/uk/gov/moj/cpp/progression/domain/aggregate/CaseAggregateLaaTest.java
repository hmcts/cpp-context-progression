package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.JudicialResultCategory.ANCILLARY;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged;
import static uk.gov.justice.core.courts.LaaReference.laaReference;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseCreated.prosecutionCaseCreated;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAggregateLaaTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final String URN = "URN";
    private CaseAggregate caseAggregate = new CaseAggregate();

    @Test
    public void testResendLaaOutcomeConcluded() throws IOException {
        final UUID caseId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged1 = laaDefendantProceedingConcludedChanged()
                .withValuesFrom(convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class, hearingId1.toString()))
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId1).build();

        final LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged2 = laaDefendantProceedingConcludedChanged()
                .withValuesFrom(convertFromFile("json/progression.event.laa-defendant-proceeding-concluded-changed.json", LaaDefendantProceedingConcludedChanged.class, hearingId2.toString()))
                .withProsecutionCaseId(caseId)
                .withHearingId(hearingId2).build();

        final List<Object> events = caseAggregate.resendLaaOutcomeConcluded(asList(laaDefendantProceedingConcludedChanged1, laaDefendantProceedingConcludedChanged2)).collect(toList());

        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent1 = (LaaDefendantProceedingConcludedResent) events.get(0);
        assertThat(laaDefendantProceedingConcludedResent1.getLaaDefendantProceedingConcludedChanged().getProsecutionCaseId(), is(caseId));
        assertThat(laaDefendantProceedingConcludedResent1.getLaaDefendantProceedingConcludedChanged().getHearingId(), is(hearingId1));

        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent2 = (LaaDefendantProceedingConcludedResent) events.get(1);
        assertThat(laaDefendantProceedingConcludedResent2.getLaaDefendantProceedingConcludedChanged().getProsecutionCaseId(), is(caseId));
        assertThat(laaDefendantProceedingConcludedResent2.getLaaDefendantProceedingConcludedChanged().getHearingId(), is(hearingId2));

    }

    @Test
    public void shouldSendLaaDefendantProceedingConcludedChangedWhenHearingIsResulted() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withLaaApplnReference(laaReference().withApplicationReference("tests").build())
                                .build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();

        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .withResultText("Result text")
                        .withLabel("Label")
                        .withJudicialResultId(randomUUID())
                        .withLifeDuration(Boolean.TRUE)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<Object> eventStream = this.caseAggregate.updateCase(prosecutionCase, asList(defendantJudicialResult), courtCentre, hearingId, "Trial", MAGISTRATES, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));

        final Object laaDefendantProceedingConcludedChangedEvent = eventStream.get(0);
        assertThat(laaDefendantProceedingConcludedChangedEvent.getClass(), is(equalTo(LaaDefendantProceedingConcludedChanged.class)));
        assertThat(((LaaDefendantProceedingConcludedChanged) laaDefendantProceedingConcludedChangedEvent).getHearingId(), is(hearingId));

        assertThat(eventStream.get(1).getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
    }

    @Test
    public void shouldSendLaaConcludedEventWithPrevResultedOffencesWhenCurrentHearingIsNotResulted() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final Offence offence1 = offence()
                .withId(offenceId1)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off1").build())
                .build();
        final Offence offence2 = offence()
                .withId(offenceId2)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off2").build())
                .build();
        final ProsecutionCase initialProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(asList(
                                offence1,
                                offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated()
                .withProsecutionCase(initialProsecutionCase)
                .build());

        // 2. Offence1 resulted with Judicial results
        ReflectionUtil.setField(offence1, "proceedingsConcluded", true);
        ReflectionUtil.setField(offence1, "judicialResults", singletonList(JudicialResult.judicialResult().withCategory(JudicialResultCategory.FINAL).build()));
        this.caseAggregate.apply(laaDefendantProceedingConcludedChanged()
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence1))
                        .build()))
                .build());

        // 3. Result Offence2
        final ProsecutionCase updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        List<DefendantJudicialResult> defendantJudicialResults = emptyList();

        final List<Object> eventStream = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, randomUUID(), "Trial", MAGISTRATES, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object laaDefendantProceedingConcludedChangedEvent = eventStream.get(0);
        assertThat(laaDefendantProceedingConcludedChangedEvent.getClass(), is(equalTo(LaaDefendantProceedingConcludedChanged.class)));
        assertThat(((LaaDefendantProceedingConcludedChanged) laaDefendantProceedingConcludedChangedEvent).getDefendants().get(0).getOffences(), contains(offence1));
    }

    @Test
    public void shouldSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsNotResultedAndThereAreNoPrevResultedOffences() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final Offence offence1 = offence()
                .withId(offenceId1)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off1").build())
                .build();
        final Offence offence2 = offence()
                .withId(offenceId2)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off2").build())
                .build();
        final ProsecutionCase initialProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(asList(
                                offence1,
                                offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated()
                .withProsecutionCase(initialProsecutionCase)
                .build());

        // 2. Offence1 resulted without Judicial results
        this.caseAggregate.apply(laaDefendantProceedingConcludedChanged()
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence1))
                        .build()))
                .build());

        // 3. Result Offence2 without Judicial results
        final ProsecutionCase updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        List<DefendantJudicialResult> defendantJudicialResults = emptyList();

        final List<Object> eventStream = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, randomUUID(), "Trial", MAGISTRATES, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object laaDefendantProceedingConcludedChangedEvent = eventStream.get(0);
        assertThat(laaDefendantProceedingConcludedChangedEvent.getClass(), is(equalTo(LaaDefendantProceedingConcludedChanged.class)));
        assertThat(((LaaDefendantProceedingConcludedChanged) laaDefendantProceedingConcludedChangedEvent).getDefendants().get(0).getOffences(), contains(offence2));
    }

    @Test
    public void shouldSendLaaConcludedEventWithCurrentOffencesWhenCurrentHearingIsResulted() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final Offence offence1 = offence()
                .withId(offenceId1)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off1").build())
                .build();
        final Offence offence2 = offence()
                .withId(offenceId2)
                .withProceedingsConcluded(false)
                .withLaaApplnReference(laaReference().withApplicationReference("off2").build())
                .build();
        final ProsecutionCase initialProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(asList(
                                offence1,
                                offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated()
                .withProsecutionCase(initialProsecutionCase)
                .build());

        // 2. Offence1 resulted without Judicial results
        this.caseAggregate.apply(laaDefendantProceedingConcludedChanged()
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(false)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence1))
                        .build()))
                .build());

        // 3. Result Offence2 with Judicial results
        final ProsecutionCase updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withProceedingsConcluded(true)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence2))
                        .build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("urn").build())
                .build();
        List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOffenceId(offenceId2)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withResultText("Result text")
                        .withLabel("Label")
                        .withJudicialResultId(randomUUID())
                        .withLifeDuration(Boolean.TRUE)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build());


        final List<Object> eventStream = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, randomUUID(), "Trial", MAGISTRATES, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object laaDefendantProceedingConcludedChangedEvent = eventStream.get(0);
        assertThat(laaDefendantProceedingConcludedChangedEvent.getClass(), is(equalTo(LaaDefendantProceedingConcludedChanged.class)));
        assertThat(((LaaDefendantProceedingConcludedChanged) laaDefendantProceedingConcludedChangedEvent).getDefendants().get(0).getOffences(),
                hasItem(allOf(
                        hasProperty("id", is(offenceId2)),
                        hasProperty("proceedingsConcluded", is(true)))));
    }

    @Test
    public void shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshareWhenResultIsInDefendantLevel(){
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String hearingType = "Trial";
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).withName("Court Name").withCode("code")
                .withRoomId(randomUUID()).withRoomName("roomName").build();
        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test").build())
                                .build()))
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();


        this.caseAggregate.apply(prosecutionCaseCreated);

        Defendant updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(true)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withProceedingsConcluded(true)
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test").build())
                                .build()))
                .build();

        ProsecutionCase updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        List<Object> eventList =  this.caseAggregate.updateCase(updatedProsecutionCase, emptyList(), courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList() ).collect(toList());
        LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged)eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(false));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(false));

        this.caseAggregate.apply(eventList);

        updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(false)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withProceedingsConcluded(false)
                                .withLaaApplnReference(laaReference().withApplicationReference("test1").build())
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test2").build())
                                .build()))
                .build();

        updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(offenceId1)
                        .withJudicialResultId(randomUUID())
                        .withCategory(JudicialResultCategory.ANCILLARY)
                        .build()).build());

        eventList =  this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList() ).collect(toList());

        laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged)eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(false));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(false));

        this.caseAggregate.apply(eventList);

        updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(false)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withProceedingsConcluded(false)
                                .withLaaApplnReference(laaReference().withApplicationReference("test1").build())
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test2").build())
                                .build()))
                .build();

        updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(offenceId1)
                        .withJudicialResultId(randomUUID())
                        .withCategory(FINAL)
                        .build()).build());

        eventList =  this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList() ).collect(toList());

        laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged)eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(true));




    }

    @Test
    public void shouldUpdateProceedingConcludedWithLAAWhenCaseIsUpdatedWithReshareWhenResultIsInDefendantLevelAndOffenceLevel() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String hearingType = "Trial";
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).withName("Court Name").withCode("code")
                .withRoomId(randomUUID()).withRoomName("roomName").build();
        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test").build())
                                .build()))
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();


        this.caseAggregate.apply(prosecutionCaseCreated);

        Defendant updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(true)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(ANCILLARY).build()))
                                .withProceedingsConcluded(true)
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test").build())
                                .build()))
                .build();

        ProsecutionCase updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        List<Object> eventList = this.caseAggregate.updateCase(updatedProsecutionCase, emptyList(), courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());
        LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged) eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(false));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(false));

        this.caseAggregate.apply(eventList);

        updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(false)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withProceedingsConcluded(false)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(ANCILLARY).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test1").build())
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test2").build())
                                .build()))
                .build();

        updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        List<DefendantJudicialResult> defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(offenceId1)
                        .withJudicialResultId(randomUUID())
                        .withCategory(JudicialResultCategory.ANCILLARY)
                        .build()).build());

        eventList = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged) eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(false));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(false));

        this.caseAggregate.apply(eventList);

        updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(false)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(ANCILLARY).build()))
                                .withProceedingsConcluded(false)
                                .withLaaApplnReference(laaReference().withApplicationReference("test1").build())
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test2").build())
                                .build()))
                .build();

        updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(offenceId1)
                        .withJudicialResultId(randomUUID())
                        .withCategory(FINAL)
                        .build()).build());

        eventList = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged) eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(false));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(false));

        this.caseAggregate.apply(eventList);

        updatedDefendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(false)
                .withOffences(asList(offence()
                                .withId(offenceId1).withListingNumber(1)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult().withCategory(FINAL).build()))
                                .withProceedingsConcluded(true)
                                .withLaaApplnReference(laaReference().withApplicationReference("test1").build())
                                .build(),
                        offence()
                                .withId(offenceId2).withListingNumber(2)
                                .withProceedingsConcluded(true)
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withCategory(FINAL).build()))
                                .withLaaApplnReference(laaReference().withApplicationReference("test2").build())
                                .build()))
                .build();

        updatedProsecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(updatedDefendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        defendantJudicialResults = singletonList(DefendantJudicialResult.defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withOrderedDate(LocalDate.now())
                        .withOffenceId(offenceId1)
                        .withJudicialResultId(randomUUID())
                        .withCategory(FINAL)
                        .build()).build());

        eventList = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        laaDefendantProceedingConcludedChanged = (LaaDefendantProceedingConcludedChanged) eventList.get(0);

        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(0).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getOffences().get(1).getProceedingsConcluded(), is(true));
        assertThat(laaDefendantProceedingConcludedChanged.getDefendants().get(0).getProceedingsConcluded(), is(true));


    }


    public <T> T convertFromFile(final String url, final Class<T> clazz, String hearingId) throws IOException {
        final String content = readFileToString(new File(this.getClass().getClassLoader().getResource(url).getFile())).replace("HEARING_ID", hearingId);
        return OBJECT_MAPPER.readValue(content, clazz);
    }
}
