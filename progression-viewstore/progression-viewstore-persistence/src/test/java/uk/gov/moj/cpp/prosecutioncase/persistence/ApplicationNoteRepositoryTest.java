package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ApplicationNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ApplicationNoteRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class ApplicationNoteRepositoryTest {

    private static final UUID APPLICATION_ID = randomUUID();
    @Inject
    private ApplicationNoteRepository applicationNoteRepository;


    @Test
    public void shouldSaveAndReadApplicationNoteAndOrderByCreatedDateDesc() {
        final ApplicationNoteEntity firstNote = new ApplicationNoteEntity(randomUUID(), APPLICATION_ID,
                "Sample note 1", FALSE, "Bob", "Marley", now());
        final ApplicationNoteEntity secondNote = new ApplicationNoteEntity(randomUUID(), APPLICATION_ID,
                "Sample note 2", FALSE, "Bob", "Marley", now().plusMinutes(1));

        applicationNoteRepository.save(firstNote);
        applicationNoteRepository.save(secondNote);

        final List<ApplicationNoteEntity> allNotes = applicationNoteRepository.findByApplicationIdOrderByCreatedDateTimeDesc(APPLICATION_ID);
        assertThat(allNotes.size(), equalTo(2));
        verifyApplicationNote(allNotes.get(0), secondNote);
        verifyApplicationNote(allNotes.get(1), firstNote);

        ApplicationNoteEntity secondNoteUpdated = allNotes.get(0);
        secondNoteUpdated.setPinned(TRUE);
        applicationNoteRepository.save(secondNoteUpdated);
        final List<ApplicationNoteEntity> allNotesAfterUpdate = applicationNoteRepository.findByApplicationIdOrderByCreatedDateTimeDesc(APPLICATION_ID);
        assertThat(allNotesAfterUpdate.size(), equalTo(2));
        verifyApplicationNote(allNotesAfterUpdate.get(0), secondNoteUpdated);
        verifyApplicationNote(allNotesAfterUpdate.get(1), firstNote);
    }

    private void verifyApplicationNote(final ApplicationNoteEntity actual, final ApplicationNoteEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getApplicationId(), equalTo(APPLICATION_ID));
        assertThat(actual.getNote(), equalTo(expected.getNote()));
        assertThat(actual.getFirstName(), equalTo(expected.getFirstName()));
        assertThat(actual.getLastName(), equalTo(expected.getLastName()));
        assertThat(actual.getCreatedDateTime(), equalTo(expected.getCreatedDateTime()));
    }
}