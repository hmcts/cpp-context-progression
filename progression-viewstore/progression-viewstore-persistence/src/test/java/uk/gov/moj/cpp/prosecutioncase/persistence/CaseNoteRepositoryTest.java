package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseNoteRepositoryTest {

    private static final UUID CASE_ID = randomUUID();
    @Inject
    private CaseNoteRepository caseNoteRepository;


    @Test
    public void shouldSaveAndReadACaseNoteAndOrderByCreatedDateDesc() {

        CaseNoteEntity firstNote = new CaseNoteEntity(randomUUID(), CASE_ID, "A Note", "Bob", "Marley", now(), false);
        CaseNoteEntity secondNote = new CaseNoteEntity();
        secondNote.setId(randomUUID());
        secondNote.setCaseId(CASE_ID);
        secondNote.setNote("A Second Note");
        secondNote.setFirstName("Bob");
        secondNote.setLastName("Marley");
        secondNote.setCreatedDateTime(now().plusMinutes(1));
        secondNote.setPinned(false);

        caseNoteRepository.save(firstNote);
        caseNoteRepository.save(secondNote);

        final List<CaseNoteEntity> allCaseNotes = caseNoteRepository.findByCaseIdOrderByCreatedDateTimeDesc(CASE_ID);
        assertThat(allCaseNotes.size(), equalTo(2));
        verifyCaseNote(allCaseNotes.get(0), secondNote);
        verifyCaseNote(allCaseNotes.get(1), firstNote);

    }

    private void verifyCaseNote(final CaseNoteEntity actual, final CaseNoteEntity expected) {
        assertThat(actual.getCaseId(), equalTo(CASE_ID));
        assertThat(actual.getNote(), equalTo(expected.getNote()));
        assertThat(actual.getFirstName(), equalTo(expected.getFirstName()));
        assertThat(actual.getLastName(), equalTo(expected.getLastName()));
        assertThat(actual.getCreatedDateTime(), equalTo(expected.getCreatedDateTime()));
    }
}