package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class ApplicantEmailAddressUtilTest {

    private static final String EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();

    private ApplicantEmailAddressUtil applicantEmailAddressUtil = new ApplicantEmailAddressUtil();

    public static Stream<Arguments> applicantType() {
        return Stream.of(
                // party type
                Arguments.of(PartyType.MASTER_DEFENDANT_PERSON),
                Arguments.of(PartyType.MASTER_DEFENDANT_LEGAL_ENTITY),
                Arguments.of(PartyType.INDIVIDUAL),
                Arguments.of(PartyType.ORGANISATION),
                Arguments.of(PartyType.PROSECUTION_AUTHORITY)
        );
    }

    @ParameterizedTest
    @MethodSource("applicantType")
    public void getApplicantEmailAddress_EmailAddressPresent(final PartyType partyType) {
        final CourtApplication courtApplication = buildCourtApplication(partyType, true);
        final Optional<String> applicantEmailAddress = applicantEmailAddressUtil.getApplicantEmailAddress(courtApplication);
        assertThat(applicantEmailAddress.isPresent(), is(true));
        assertThat(applicantEmailAddress.get(), is(EMAIL_ADDRESS));
    }

    @ParameterizedTest
    @MethodSource("applicantType")
    public void getApplicantEmailAddress_EmailAddressNotPresent(final PartyType partyType) {
        final CourtApplication courtApplication = buildCourtApplication(partyType, false);
        final Optional<String> applicantEmailAddress = applicantEmailAddressUtil.getApplicantEmailAddress(courtApplication);
        assertThat(applicantEmailAddress.isPresent(), is(false));
    }

    private CourtApplication buildCourtApplication(final PartyType partyType, final boolean hasEmailAddress) {
        return courtApplication()
                .withType(courtApplicationType().withSummonsTemplateType(SummonsTemplateType.FIRST_HEARING).build())
                .withId(randomUUID())
                .withApplicationReference(randomAlphabetic(20))
                .withApplicant(getParty(partyType, hasEmailAddress))
                .build();
    }

    private CourtApplicationParty getParty(final PartyType partyType, final boolean hasEmailAddress) {
        final CourtApplicationParty.Builder courtApplicationPartyBuilder = courtApplicationParty();
        final ContactNumber contact = contactNumber().withPrimaryEmail(hasEmailAddress ? EMAIL_ADDRESS : null).build();
        switch (partyType) {
            case MASTER_DEFENDANT_PERSON:
                final Person defendantPerson = person()
                        .withContact(contact)
                        .build();
                courtApplicationPartyBuilder
                        .withMasterDefendant(masterDefendant()
                                .withPersonDefendant(personDefendant().withPersonDetails(defendantPerson).build())
                                .build());
                break;

            case MASTER_DEFENDANT_LEGAL_ENTITY:
                courtApplicationPartyBuilder
                        .withMasterDefendant(
                                masterDefendant()
                                        .withLegalEntityDefendant(legalEntityDefendant()
                                                .withOrganisation(organisation().withContact(contact).build()).build()).build()
                        ).build();
                break;
            case INDIVIDUAL:
                final Person individualPerson = person().withContact(contact).build();
                courtApplicationPartyBuilder.withPersonDetails(individualPerson);
                break;
            case ORGANISATION:
                courtApplicationPartyBuilder.withOrganisation(organisation().withContact(contact).build()).build();
                break;
            case PROSECUTION_AUTHORITY:
                courtApplicationPartyBuilder.withProsecutingAuthority(prosecutingAuthority().withContact(contact).build()).build();
                break;
        }

        return courtApplicationPartyBuilder.build();
    }
}