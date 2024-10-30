package uk.gov.moj.cpp.progression.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.core.courts.CotrPdfContent;
import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.Defence;
import uk.gov.justice.core.courts.DefendantCotrServed;
import uk.gov.justice.core.courts.Fields;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RequestCotrTask;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.cpp.progression.event.CotrArchived;
import uk.gov.justice.cpp.progression.event.CotrCreated;
import uk.gov.justice.cpp.progression.event.Defendants;
import uk.gov.justice.cpp.progression.event.ProsecutionCotrServed;
import uk.gov.justice.progression.courts.AddFurtherInfoDefenceCotrCommand;
import uk.gov.justice.progression.courts.AddFurtherInfoProsecutionCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.progression.courts.CotrNotes;
import uk.gov.justice.progression.courts.ReviewNoteType;
import uk.gov.justice.progression.courts.ReviewNotes;
import uk.gov.justice.progression.courts.UpdateReviewNotes;
import uk.gov.justice.progression.event.DefendantAddedToCotr;
import uk.gov.justice.progression.event.DefendantRemovedFromCotr;
import uk.gov.justice.progression.event.FurtherInfoForDefenceCotrAdded;
import uk.gov.justice.progression.event.FurtherInfoForProsecutionCotrAdded;
import uk.gov.justice.progression.event.ReviewNotesUpdated;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;
import uk.gov.moj.cpp.progression.json.schemas.event.CotrTaskRequested;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CotrAggregateTest {

    @InjectMocks
    private CotrAggregate cotrAggregate;

    @AfterEach
    public void teardown() {
        try {
            // ensure aggregate is serializable
            SerializationUtils.serialize(cotrAggregate);
        } catch (SerializationException e) {
            fail("Aggregate should be serializable");
        }
    }

    @Test
    public void testShouldCotrCreatedForDefendantsOfHearing() {
        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID caseId = randomUUID();
        final List<UUID> defendantIds = Arrays.asList(randomUUID(), randomUUID());

        Stream<Object> events = cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(caseId)
                .withDefendantIds(defendantIds)
                .build());
        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(1, lEvents.size());
        Object event = lEvents.get(0);
        assertEquals(event.getClass(), CotrCreated.class);
        final CotrCreated cotrCreated = (CotrCreated) event;
        assertThat(cotrCreated.getCotrId(), is(cotrId));
        assertThat(cotrCreated.getHearingId(), is(hearingId));
        assertThat(cotrCreated.getCaseId(), is(caseId));
        assertThat(cotrCreated.getDefendants(), hasSize(equalTo(2)));
    }

    @Test
    public void testShouldServeProsecutionCotr() {
        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        Stream<Object> events = cotrAggregate.serveProsecutionCotr(ServeProsecutionCotr.serveProsecutionCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withIsTheTimeEstimateCorrect(PolarQuestion.polarQuestion().withAnswer("N").build())
                .build());
        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(2, lEvents.size());

        Object event = lEvents.get(0);
        assertEquals(event.getClass(), CotrTaskRequested.class);

        event = lEvents.get(1);
        assertEquals(event.getClass(), ProsecutionCotrServed.class);
        final ProsecutionCotrServed serveProsecutionCotrCreated = (ProsecutionCotrServed) event;
        assertThat(serveProsecutionCotrCreated.getCotrId(), is(cotrId));
        assertThat(serveProsecutionCotrCreated.getHearingId(), is(hearingId));
    }

    @Test
    public void testShouldRequestCotrTask() {
        final UUID cotrId = randomUUID();
        final UUID hearingId = randomUUID();
        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        RequestCotrTask requestCotrTask = RequestCotrTask.requestCotrTask()
                .withTaskName("taskName")
                .withRoles("someRoles")
                .withNumberOfDays(2)
                .withCotrId(cotrId)
                .withComments("comments").build();
        Stream<Object> events = cotrAggregate.requestCotrTask(requestCotrTask);
        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(1, lEvents.size());
        Object event = lEvents.get(0);
        assertEquals(event.getClass(), CotrTaskRequested.class);
        final CotrTaskRequested cotrTaskRequested = (CotrTaskRequested) event;
        assertThat(cotrTaskRequested.getCotrId(), is(cotrId));
    }

    @Test
    public void testShouldArchiveCotr() {
        final UUID cotrId = randomUUID();
        Stream<Object> events = cotrAggregate.archiveCotr(cotrId);
        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(1, lEvents.size());
        Object event = lEvents.get(0);
        assertEquals(event.getClass(), CotrArchived.class);
        final CotrArchived cotrArchived = (CotrArchived) event;
        assertThat(cotrArchived.getCotrId(), is(cotrId));
    }


    @Test
    public void shouldNotReturnCotrArchived() {
        ReflectionUtil.setField(this.cotrAggregate, "archived", true);
        final List<Object> eventStream = cotrAggregate.archiveCotr(randomUUID()).collect(toList());
        assertThat(eventStream.size(), CoreMatchers.is(0));
    }

    @Test
    public void testShouldServeDefendantCotr() {
        final UUID defendenId = randomUUID();
        final UUID cotrId = randomUUID();
        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(randomUUID())
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        Stream<Object> events = cotrAggregate.serveDefendantCotr(ServeDefendantCotr.serveDefendantCotr()
                .withCotrId(cotrId)
                .withIsWelshForm(Boolean.TRUE)
                .withCorrectEstimate(Boolean.FALSE)
                .withDefendantId(defendenId).withDefendantFormData("data")
                .build());
        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(3, lEvents.size());
        assertEquals(lEvents.get(0).getClass(), CotrTaskRequested.class);
        assertEquals("Welsh Translation request Task", ((CotrTaskRequested) lEvents.get(0)).getTaskName());
        assertEquals(lEvents.get(1).getClass(), CotrTaskRequested.class);
        assertEquals("Review Listing Task", ((CotrTaskRequested) lEvents.get(1)).getTaskName());
        Object event = lEvents.get(2);
        assertEquals(event.getClass(), DefendantCotrServed.class);
        final DefendantCotrServed serveDefendentCotr = (DefendantCotrServed) event;
        assertThat(serveDefendentCotr.getCotrId(), is(cotrId));
        assertThat(serveDefendentCotr.getDefendantId(), is(defendenId));
        assertThat(serveDefendentCotr.getDefendantFormData(), is("data"));
    }

    @Test
    public void shouldCreateDefendantAddedToCotrEvent() {

        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        cotrAggregate.apply(CotrCreated.cotrCreated()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withCaseId(randomUUID())
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendants(Arrays.asList(
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(1)
                                .build(),
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(2)
                                .build()))
                .build());

        final Stream<Object> events = cotrAggregate.changeDefendantsCotr(ChangeDefendantsCotr.changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withAddedDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build());

        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(2, lEvents.size());

        Object event = lEvents.get(0);
        assertEquals(event.getClass(), DefendantAddedToCotr.class);
        DefendantAddedToCotr defendantAddedToCotr = (DefendantAddedToCotr) event;
        assertThat(defendantAddedToCotr.getCotrId(), is(cotrId));
        assertThat(defendantAddedToCotr.getHearingId(), is(hearingId));
        assertThat(defendantAddedToCotr.getDefendantId(), is(defendantId1));
        assertThat(defendantAddedToCotr.getDefendantNumber(), is(3));

        event = lEvents.get(1);
        assertEquals(event.getClass(), DefendantAddedToCotr.class);
        defendantAddedToCotr = (DefendantAddedToCotr) event;
        assertThat(defendantAddedToCotr.getCotrId(), is(cotrId));
        assertThat(defendantAddedToCotr.getHearingId(), is(hearingId));
        assertThat(defendantAddedToCotr.getDefendantId(), is(defendantId2));
        assertThat(defendantAddedToCotr.getDefendantNumber(), is(4));

    }

    @Test
    public void shouldCreateDefendantRemovedFromCotrEvent() {

        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final Stream<Object> events = cotrAggregate.changeDefendantsCotr(ChangeDefendantsCotr.changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withRemovedDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build());

        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(2, lEvents.size());

        Object event = lEvents.get(0);
        assertEquals(event.getClass(), DefendantRemovedFromCotr.class);
        DefendantRemovedFromCotr defendantRemovedFromCotr = (DefendantRemovedFromCotr) event;
        assertThat(defendantRemovedFromCotr.getCotrId(), is(cotrId));
        assertThat(defendantRemovedFromCotr.getHearingId(), is(hearingId));
        assertThat(defendantRemovedFromCotr.getDefendantId(), is(defendantId1));

        event = lEvents.get(1);
        assertEquals(event.getClass(), DefendantRemovedFromCotr.class);
        defendantRemovedFromCotr = (DefendantRemovedFromCotr) event;
        assertThat(defendantRemovedFromCotr.getCotrId(), is(cotrId));
        assertThat(defendantRemovedFromCotr.getHearingId(), is(hearingId));
        assertThat(defendantRemovedFromCotr.getDefendantId(), is(defendantId2));

    }

    @Test
    public void shouldCreateBothDefendantAddedToCotrEventAndDefendantRemovedFromCotrEvent() {

        final UUID hearingId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        cotrAggregate.apply(CotrCreated.cotrCreated()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withCaseId(hearingId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendants(Arrays.asList(
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(1)
                                .build(),
                        Defendants.defendants()
                                .withId(randomUUID())
                                .withDnumber(2)
                                .build()))
                .build());

        final Stream<Object> events = cotrAggregate.changeDefendantsCotr(ChangeDefendantsCotr.changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(hearingId)
                .withAddedDefendantIds(Arrays.asList(defendantId1))
                .withRemovedDefendantIds(Arrays.asList(defendantId2))
                .build());

        List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(2, lEvents.size());

        Object event = lEvents.get(0);
        assertEquals(event.getClass(), DefendantAddedToCotr.class);
        DefendantAddedToCotr defendantAddedToCotr = (DefendantAddedToCotr) event;
        assertThat(defendantAddedToCotr.getCotrId(), is(cotrId));
        assertThat(defendantAddedToCotr.getHearingId(), is(hearingId));
        assertThat(defendantAddedToCotr.getDefendantId(), is(defendantId1));
        assertThat(defendantAddedToCotr.getDefendantNumber(), is(3));

        event = lEvents.get(1);
        assertEquals(event.getClass(), DefendantRemovedFromCotr.class);
        DefendantRemovedFromCotr defendantRemovedFromCotr = (DefendantRemovedFromCotr) event;
        assertThat(defendantRemovedFromCotr.getCotrId(), is(cotrId));
        assertThat(defendantRemovedFromCotr.getHearingId(), is(hearingId));
        assertThat(defendantRemovedFromCotr.getDefendantId(), is(defendantId2));

    }

    @Test
    public void shouldCreateFurtherInfoForProsecutionCotrAdded() {

        final UUID cotrId = randomUUID();
        final String message = "Further Info For Prosecution Cotr";
        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(randomUUID())
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        final Stream<Object> events = cotrAggregate.addFurtherInfoForProsecutionCotr(AddFurtherInfoProsecutionCotr.addFurtherInfoProsecutionCotr()
                .withCotrId(cotrId)
                .withMessage(message)
                .build());

        final List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(2, lEvents.size());
        Object event = lEvents.get(1);
        assertEquals(event.getClass(), FurtherInfoForProsecutionCotrAdded.class);
        final FurtherInfoForProsecutionCotrAdded furtherInfoForProsecutionCotrAdded = (FurtherInfoForProsecutionCotrAdded) event;
        assertThat(furtherInfoForProsecutionCotrAdded.getCotrId(), is(cotrId));
        assertThat(furtherInfoForProsecutionCotrAdded.getMessage(), is(message));

    }

    @Test
    public void shouldCreateFurtherInfoForDefenceCotrAdded() {

        final UUID cotrId = randomUUID();
        final UUID defendantId = randomUUID();
        final String message = "Further Info For Defence Cotr";
        final UUID userId = randomUUID();
        final String userName = "James Turner";

        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(randomUUID())
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        cotrAggregate.serveDefendantCotr(ServeDefendantCotr.serveDefendantCotr()
                .withCotrId(cotrId)
                .withIsWelshForm(Boolean.TRUE)
                .withCorrectEstimate(Boolean.FALSE)
                .withDefendantId(defendantId).withDefendantFormData("data")
                .withPdfContent(CotrPdfContent.cotrPdfContent()
                        .withDefence(Arrays.asList(Defence.defence().withTitle("title")
                                .withFields(Arrays.asList(Fields.fields().withLabel("Label").build())).build())).build())
                .build());

        final Stream<Object> events = cotrAggregate.addFurtherInfoForDefenceCotr(AddFurtherInfoDefenceCotrCommand.addFurtherInfoDefenceCotrCommand()
                .withCotrId(cotrId)
                .withDefendantId(defendantId)
                .withMessage(message)
                .withIsWelshForm(Boolean.TRUE)
                .withIsCertificationReady(Boolean.TRUE)
                .withAddedBy(userId)
                .withAddedByName(userName)
                .build());

        final List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(3, lEvents.size());

        Object event = lEvents.get(0);
        assertEquals(event.getClass(), FurtherInfoForDefenceCotrAdded.class);
        final FurtherInfoForDefenceCotrAdded furtherInfoForDefenceCotrAdded = (FurtherInfoForDefenceCotrAdded) event;
        assertThat(furtherInfoForDefenceCotrAdded.getCotrId(), is(cotrId));
        assertThat(furtherInfoForDefenceCotrAdded.getDefendantId(), is(defendantId));
        assertThat(furtherInfoForDefenceCotrAdded.getMessage(), is(message));
        assertThat(furtherInfoForDefenceCotrAdded.getIsCertificationReady(), is(Boolean.TRUE));
        assertThat(furtherInfoForDefenceCotrAdded.getAddedBy(), is(userId));
        assertThat(furtherInfoForDefenceCotrAdded.getAddedByName(), is(userName));
        assertEquals(lEvents.get(1).getClass(), CotrTaskRequested.class);
        assertEquals("Review Defence CoTR Task", ((CotrTaskRequested) lEvents.get(1)).getTaskName());
        assertEquals(lEvents.get(2).getClass(), CotrTaskRequested.class);
        assertEquals("Welsh Translation request Task", ((CotrTaskRequested) lEvents.get(2)).getTaskName());

    }

    @Test
    public void shouldCreateReviewNotesUpdatedEvent() {

        final UUID cotrId = randomUUID();
        cotrAggregate.createCotr(CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(randomUUID())
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withCaseId(randomUUID())
                .withDefendantIds(Arrays.asList(randomUUID()))
                .build());
        final Stream<Object> events = cotrAggregate.updateReviewNotes(UpdateReviewNotes.updateReviewNotes()
                .withCotrId(cotrId)
                .withCotrNotes(Arrays.asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(Arrays.asList(
                                ReviewNotes.reviewNotes()
                                        .withId(randomUUID())
                                        .withComment("comments1")
                                        .build(),
                                ReviewNotes.reviewNotes()
                                        .withId(randomUUID())
                                        .withComment("comments2")
                                        .build()))
                        .build()))
                .build());

        final List<Object> lEvents = events.collect(Collectors.toList());
        assertEquals(1, lEvents.size());
        Object event = lEvents.get(0);
        assertEquals(event.getClass(), ReviewNotesUpdated.class);
        final ReviewNotesUpdated reviewNotesUpdated = (ReviewNotesUpdated) event;
        assertThat(reviewNotesUpdated.getCotrId(), is(cotrId));
        assertThat(reviewNotesUpdated.getCotrNotes().get(0).getReviewNoteType(), is(uk.gov.justice.progression.event.ReviewNoteType.CASE_PROGRESSION));
        assertThat(reviewNotesUpdated.getCotrNotes().get(0).getReviewNotes().size(), is(2));

    }
}