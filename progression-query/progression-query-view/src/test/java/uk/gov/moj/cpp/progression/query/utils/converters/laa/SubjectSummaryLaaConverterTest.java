package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.progression.query.laa.CourtCentre;
import uk.gov.justice.progression.query.laa.SubjectSummary;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectSummaryLaaConverterTest {

    @InjectMocks
    private SubjectSummaryLaaConverter subjectSummaryLaaConverter;

    @Mock
    private DefendantASNConverter defendantASNConverter;

    @Mock
    private DateOfNextHearingConverter dateOfNextHearingConverter;

    @Mock
    private OffenceSummaryConverter offenceSummaryConverter;

    @Mock
    private RepresentationOrderConverter representationOrderConverter;

    @Test
    void shouldConvertSubjectSummaryWhenDefendantIsPerson() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withProceedingsConcluded(false)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person()
                                                .withDateOfBirth(LocalDate.now())
                                                .withFirstName("forename")
                                                .withMiddleName("middleName")
                                                .withLastName("surname")
                                                .withNationalInsuranceNumber("NIN")
                                                .build())
                                        .withArrestSummonsNumber("ASN")
                                        .build())
                                .withMasterDefendantId(randomUUID())
                                .build())
                        .withId(randomUUID())
                        .build())
                .build();


        final SubjectSummary result = subjectSummaryLaaConverter.convert(courtApplication, null);

        assertThat(result.getDefendantDOB(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getDateOfBirth().toString()));
        assertThat(result.getDefendantFirstName(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName()));
        assertThat(result.getDefendantMiddleName(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getMiddleName()));
        assertThat(result.getDefendantLastName(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getLastName()));
        assertThat(result.getDefendantNINO(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getNationalInsuranceNumber()));
        assertThat(result.getMasterDefendantId(), is(courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()));
        assertThat(result.getProceedingsConcluded(), is(courtApplication.getProceedingsConcluded()));
        assertThat(result.getSubjectId(), is(courtApplication.getSubject().getId()));
    }

    @Test
    void shouldConvertSubjectSummaryWhenDefendantIsCorporate() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withProceedingsConcluded(false)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                        .withOrganisation(Organisation.organisation()
                                                .withName("orgName")
                                                .build())
                                        .build())
                                .withMasterDefendantId(randomUUID())
                                .build())
                        .withId(randomUUID())
                        .build())
                .build();


        final SubjectSummary result = subjectSummaryLaaConverter.convert(courtApplication, null);

        assertThat(result.getDefendantDOB(), nullValue());
        assertThat(result.getDefendantFirstName(), is(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant().getOrganisation().getName()));
        assertThat(result.getDefendantMiddleName(), nullValue());
        assertThat(result.getDefendantLastName(), nullValue());
        assertThat(result.getDefendantNINO(), nullValue());
        assertThat(result.getMasterDefendantId(), is(courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()));
        assertThat(result.getProceedingsConcluded(), is(courtApplication.getProceedingsConcluded()));
        assertThat(result.getSubjectId(), is(courtApplication.getSubject().getId()));
    }

    @Test
    void shouldConvertSubjectSummaryWhenMasterDefendantIsNull() {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withProceedingsConcluded(false)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withMasterDefendant(null)
                        .build())
                .build();


        final SubjectSummary result = subjectSummaryLaaConverter.convert(courtApplication, null);

        assertThat(result.getDefendantDOB(), nullValue());
        assertThat(result.getDefendantFirstName(), nullValue());
        assertThat(result.getDefendantMiddleName(), nullValue());
        assertThat(result.getDefendantLastName(), nullValue());
        assertThat(result.getDefendantNINO(), nullValue());
        assertThat(result.getMasterDefendantId(), nullValue());
        assertThat(result.getProceedingsConcluded(), is(courtApplication.getProceedingsConcluded()));
        assertThat(result.getSubjectId(), is(courtApplication.getSubject().getId()));
    }

}