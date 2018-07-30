package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import uk.gov.moj.cpp.progression.domain.event.defendant.BaseDefendantOffence;
import uk.gov.moj.cpp.progression.domain.event.defendant.BaseDefendantOffences;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChanged;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceIndicatedPlea;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class DefendantOffenceHelperTest {

    @Test
    public void testDeletedOffences() {
        final UUID offenceIdOne = UUID.randomUUID();
        final UUID offenceIdTwo = UUID.randomUUID();
        final UUID offenceIdThree = UUID.randomUUID();
        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        commandOffences.add(new OffenceForDefendant(offenceIdOne, null, null, null, null, null, 1,
                        null, null, null, null));
        commandOffences.add(new OffenceForDefendant(offenceIdTwo, null, null, null, null, null, 2,
                        null, null, null, null));
        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, null, null, null, null, null, 1,
                        null, null, null, null));
        previousOffences.add(new OffenceForDefendant(offenceIdTwo, null, null, null, null, null, 2,
                        null, null, null, null));
        previousOffences.add(new OffenceForDefendant(offenceIdThree, null, null, null, null, null,
                        2, null, null, null, null));
        final DefendantOffencesChanged event = DefendantOffenceHelper.getDefendantOffencesChanged(
                        UUID.randomUUID(), UUID.randomUUID(), commandOffences, previousOffences)
                        .get();
        assertThat(event.getDeletedOffences().size(), is(1));
        assertThat(event.getDeletedOffences().get(0).getOffences().get(0).getId(), is(offenceIdThree));
        assertNull(event.getAddedOffences());
        assertNull(event.getUpdatedOffences());
    }

    @Test
    public void testAddedOffences() {
        final UUID offenceIdOne = UUID.randomUUID();
        final UUID offenceIdTwo = UUID.randomUUID();
        final UUID offenceIdThree = UUID.randomUUID();
        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        commandOffences.add(new OffenceForDefendant(offenceIdOne, null, null, null, null, null, 1,
                        null, null, null, null));
        commandOffences.add(new OffenceForDefendant(offenceIdTwo, null, null, null, null, null, 2,
                        null, null, null, null));
        commandOffences.add(new OffenceForDefendant(offenceIdThree, null, null, null, null, null, 3,
                        null, null, null, null));
        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, null, null, null, null, null, 1,
                        null, null, null, null));
        previousOffences.add(new OffenceForDefendant(offenceIdThree, null, null, null, null, null,
                        3, null, null, null, null));
        final DefendantOffencesChanged event = DefendantOffenceHelper.getDefendantOffencesChanged(
                        UUID.randomUUID(), UUID.randomUUID(), commandOffences, previousOffences)
                        .get();
        assertThat(event.getAddedOffences().size(), is(1));
        assertThat(event.getAddedOffences().get(0).getOffences().get(0).getId(), is(offenceIdTwo));
        assertNull(event.getDeletedOffences());
        assertNull(event.getUpdatedOffences());
    }

    @Test
    public void testUpdates() {
        final UUID offenceIdOne = UUID.randomUUID();
        final UUID offenceIdTwo = UUID.randomUUID();
        final UUID offenceIdThree = UUID.randomUUID();
        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        final LocalDate updatedStartDate = LocalDate.of(2018, 1, 1);
        final LocalDate updatedEndDate = LocalDate.of(2020, 1, 1);
        final LocalDate updatedConvicationDate = LocalDate.of(2018, 2, 2);
        commandOffences.add(new OffenceForDefendant(offenceIdOne, "OFF11", "S22", "Robbery",
                        updatedStartDate, updatedEndDate, 1, 1, null, null,
                        updatedConvicationDate));
        commandOffences.add(new OffenceForDefendant(offenceIdTwo, "O2222", "S22", "Robbery",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 2, 3, null, null,
                        LocalDate.of(2018, 1, 1)));

        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "S11", "Theft",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null, null,
                        LocalDate.of(2018, 1, 1)));
        previousOffences.add(new OffenceForDefendant(offenceIdTwo, "O2222", "S22", "Robbery",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 2, 3, null, null,
                        LocalDate.of(2018, 1, 1)));
        previousOffences.add(new OffenceForDefendant(offenceIdThree, null, null, null, null, null,
                        2, 3, null, null, LocalDate.of(2018, 1, 1)));
        final DefendantOffencesChanged event = DefendantOffenceHelper.getDefendantOffencesChanged(
                        UUID.randomUUID(), UUID.randomUUID(), commandOffences, previousOffences)
                        .get();

        assertThat(event.getDeletedOffences().size(), is(1));
        assertThat(event.getDeletedOffences().get(0).getOffences().get(0).getId(), is(offenceIdThree));

        assertNull(event.getAddedOffences());

        assertThat(event.getUpdatedOffences().size(), is(1));
        final BaseDefendantOffence updatedOffence = event.getUpdatedOffences().get(0).getOffences().get(0);
        assertThat(updatedOffence.getId(), is(offenceIdOne));
        assertThat(updatedOffence.getOffenceCode(), is("OFF11"));
        assertThat(updatedOffence.getWording(), is("Robbery"));
        assertThat(updatedOffence.getStartDate(), is(updatedStartDate));
        assertThat(updatedOffence.getEndDate(), is(updatedEndDate));
        assertThat(updatedOffence.getCount(), is(1));
        assertThat(updatedOffence.getConvictionDate(), is(updatedConvicationDate));
    }


    @Test
    public void testUpdatesComparableFieldsUpdatedOnOneOffence() {
        final UUID offenceIdOne = UUID.randomUUID();
        final UUID offenceIdTwo = UUID.randomUUID();
        final UUID offenceIdThree = UUID.randomUUID();
        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        final LocalDate updatedStartDate = LocalDate.of(2018, 1, 1);
        final LocalDate updatedEndDate = LocalDate.of(2020, 1, 1);
        final LocalDate updatedConvicationDate = LocalDate.of(2018, 2, 2);
        commandOffences.add(new OffenceForDefendant(offenceIdOne, "OFF11", "SECTION-UPDATED",
                        "Robbery", updatedStartDate, updatedEndDate, 1, 1, null, null,
                        updatedConvicationDate));
        commandOffences.add(new OffenceForDefendant(offenceIdTwo, "O2222", "SECTION-UPDATED",
                        "Robbery", LocalDate.now(), LocalDate.of(2019, 1, 1), 2, 3, null,
                        new OffenceIndicatedPlea(UUID.randomUUID(), "value", "allocationDecision"),
                        LocalDate.of(2018, 1, 1)));

        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "S11", "Theft",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null, null,
                        LocalDate.of(2018, 1, 1)));
        previousOffences.add(new OffenceForDefendant(offenceIdTwo, "O2222", "S22", "Robbery",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 2, 3, null, null,
                        LocalDate.of(2018, 1, 1)));
        previousOffences.add(new OffenceForDefendant(offenceIdThree, null, null, null, null, null,
                        2, 3, null, null, LocalDate.of(2018, 1, 1)));
        final DefendantOffencesChanged event = DefendantOffenceHelper.getDefendantOffencesChanged(
                        UUID.randomUUID(), UUID.randomUUID(), commandOffences, previousOffences)
                        .get();

        assertThat(event.getDeletedOffences().size(), is(1));
        assertThat(event.getDeletedOffences().get(0).getOffences().get(0).getId(), is(offenceIdThree));

        assertNull(event.getAddedOffences());
        assertThat(event.getUpdatedOffences().size(), is(1));

        assertThat(event.getUpdatedOffences().size(), is(1));
        final BaseDefendantOffence updatedOffence = event.getUpdatedOffences().get(0).getOffences().get(0);
        assertThat(updatedOffence.getId(), is(offenceIdOne));
        assertThat(updatedOffence.getOffenceCode(), is("OFF11"));
        assertThat(updatedOffence.getWording(), is("Robbery"));
        assertThat(updatedOffence.getStartDate(), is(updatedStartDate));
        assertThat(updatedOffence.getEndDate(), is(updatedEndDate));
        assertThat(updatedOffence.getCount(), is(1));
        assertThat(updatedOffence.getConvictionDate(), is(updatedConvicationDate));
    }

    @Test
    public void testUpdatesNoUpdatesToComparableFieldsOnlyAdd() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceIdOne = UUID.randomUUID();
        final UUID offenceIdTwo = UUID.randomUUID();
        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        commandOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "SECTION-UPDATED",
                        "Theft", LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null,
                        new OffenceIndicatedPlea(UUID.randomUUID(), "value", "allocationDecision"),
                        LocalDate.of(2018, 1, 1)));
        commandOffences.add(new OffenceForDefendant(offenceIdTwo, "OFF2", "S22", "Robbery",
                        LocalDate.of(2018, 1, 1), LocalDate.of(2019, 1, 1), 2, 3, null, null,
                        LocalDate.of(2018, 1, 1)));

        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "S11", "Theft",
                        LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null, null,
                        LocalDate.of(2018, 1, 1)));

        final DefendantOffencesChanged event = DefendantOffenceHelper.getDefendantOffencesChanged(
                        caseId, defendantId, commandOffences, previousOffences).get();

        assertThat(event.getAddedOffences().size(), is(1));
        final BaseDefendantOffences offencesAdded = event.getAddedOffences().get(0);
        assertThat(offencesAdded.getCaseId(), is(caseId));
        assertThat(offencesAdded.getDefendantId(), is(defendantId));
        assertThat(offencesAdded.getOffences().size(), is(1));

        final BaseDefendantOffence updatedOffence = offencesAdded.getOffences().get(0);
        assertThat(updatedOffence.getId(), is(offenceIdTwo));
        assertThat(updatedOffence.getOffenceCode(), is("OFF2"));
        assertThat(updatedOffence.getWording(), is("Robbery"));
        assertThat(updatedOffence.getStartDate(), is(LocalDate.of(2018, 1, 1)));
        assertThat(updatedOffence.getEndDate(), is(LocalDate.of(2019, 1, 1)));
        assertThat(updatedOffence.getCount(), is(3));
        assertThat(updatedOffence.getConvictionDate(), is(LocalDate.of(2018, 1, 1)));
        assertNull(event.getUpdatedOffences());
    }

    @Test
    public void testNoAddUpdateDelete() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceIdOne = UUID.randomUUID();

        final List<OffenceForDefendant> commandOffences = new ArrayList<>();
        commandOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "SECTION-UPDATED",
                        "Theft", LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null,
                        new OffenceIndicatedPlea(UUID.randomUUID(), "value", "allocationDecision"),
                        LocalDate.of(2018, 1, 1)));

        final List<OffenceForDefendant> previousOffences = new ArrayList<>();
        previousOffences.add(new OffenceForDefendant(offenceIdOne, "OFF1", "SECTION-UPDATED",
                        "Theft", LocalDate.now(), LocalDate.of(2019, 1, 1), 1, 3, null,
                        new OffenceIndicatedPlea(UUID.randomUUID(), "value", "allocationDecision"),
                        LocalDate.of(2018, 1, 1)));

        final Optional<DefendantOffencesChanged> event =
                        DefendantOffenceHelper.getDefendantOffencesChanged(caseId, defendantId,
                                        commandOffences, previousOffences);
        assertFalse(event.isPresent());

    }
    

}
