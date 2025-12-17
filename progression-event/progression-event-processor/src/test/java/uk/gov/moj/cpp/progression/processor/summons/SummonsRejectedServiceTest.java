package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getFullName;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SummonsRejectedServiceTest {

    private static final ArrayList<String> REASONS = newArrayList(randomAlphabetic(20), randomAlphabetic(20));

    public static Stream<Arguments> applicationTypesNotInterested() {
        return Stream.of(
                // summons template type
                Arguments.of(SummonsTemplateType.PARENT_GENERIC_CASE),
                Arguments.of(SummonsTemplateType.NOT_APPLICABLE)
        );
    }

    public static Stream<Arguments> applicationSummons() {
        return Stream.of(
                // summons template type, party type
                Arguments.of(SummonsTemplateType.GENERIC_APPLICATION, PartyType.MASTER_DEFENDANT_PERSON),
                Arguments.of(SummonsTemplateType.GENERIC_APPLICATION, PartyType.MASTER_DEFENDANT_LEGAL_ENTITY),
                Arguments.of(SummonsTemplateType.BREACH, PartyType.INDIVIDUAL),
                Arguments.of(SummonsTemplateType.BREACH, PartyType.ORGANISATION),
                Arguments.of(SummonsTemplateType.BREACH, PartyType.PROSECUTION_AUTHORITY)
                );
    }

    public static Stream<Arguments> firstHearingSummons() {
        return Stream.of(
                // summons template type, personal defendant
                Arguments.of(SummonsTemplateType.FIRST_HEARING, PartyType.MASTER_DEFENDANT_PERSON),
                Arguments.of(SummonsTemplateType.FIRST_HEARING, PartyType.MASTER_DEFENDANT_LEGAL_ENTITY)
                );
    }

    @Mock
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private EmailChannel emailChannel;

    @InjectMocks
    private SummonsRejectedService summonsRejectedService;

    private UUID applicationId;

    private static final String APPLICANT_EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();

    @BeforeEach
    public void setup() {
        initMocks(this);
    }

    @MethodSource("applicationTypesNotInterested")
    @ParameterizedTest
    public void doNotSendSummonsRejectionNotificationForOtherTemplateTypes(final SummonsTemplateType summonsTemplateType) {
        final CourtApplication courtApplication = buildCourtApplication(summonsTemplateType, PartyType.MASTER_DEFENDANT_PERSON);

        summonsRejectedService.sendSummonsRejectionNotification(jsonEnvelope, courtApplication, getRejectionOutcome());

        verifyNoInteractions(summonsNotificationEmailPayloadService, notificationService);
    }

    @MethodSource("applicationSummons")
    @ParameterizedTest
    public void sendSummonsRejectionNotificationForApplications(final SummonsTemplateType summonsTemplateType, final PartyType partyType) {
        final CourtApplication courtApplication = buildCourtApplication(summonsTemplateType, partyType);
        final List<String> partyDetails = getPartyDetails(singletonList(courtApplication.getSubject()), partyType);

        when(summonsNotificationEmailPayloadService.getEmailChannelForSummonsRejected(eq(APPLICANT_EMAIL_ADDRESS), eq(courtApplication.getApplicationReference()), eq(partyDetails), eq(REASONS))).thenReturn(emailChannel);

        summonsRejectedService.sendSummonsRejectionNotification(jsonEnvelope, courtApplication, getRejectionOutcome());

        verify(summonsNotificationEmailPayloadService).getEmailChannelForSummonsRejected(eq(APPLICANT_EMAIL_ADDRESS), eq(courtApplication.getApplicationReference()), eq(partyDetails), eq(REASONS));
        verify(notificationService).sendEmail(eq(jsonEnvelope), eq(null), eq(courtApplication.getId()), eq(null), eq(singletonList(emailChannel)));
    }

    @MethodSource("firstHearingSummons")
    @ParameterizedTest
    public void sendSummonsRejectionNotificationForFirstHearingApplications(final SummonsTemplateType summonsTemplateType, final PartyType partyType) {
        final CourtApplication courtApplication = buildCourtApplication(summonsTemplateType, partyType);
        final List<String> partyDetails = getPartyDetails(courtApplication.getRespondents(), partyType);

        when(summonsNotificationEmailPayloadService.getEmailChannelForSummonsRejected(eq(APPLICANT_EMAIL_ADDRESS), eq(courtApplication.getApplicationReference()), eq(partyDetails), eq(REASONS))).thenReturn(emailChannel);

        summonsRejectedService.sendSummonsRejectionNotification(jsonEnvelope, courtApplication, getRejectionOutcome());

        verify(summonsNotificationEmailPayloadService).getEmailChannelForSummonsRejected(eq(APPLICANT_EMAIL_ADDRESS), eq(courtApplication.getApplicationReference()), eq(partyDetails), eq(REASONS));
        verify(notificationService).sendEmail(eq(jsonEnvelope), eq(null), eq(courtApplication.getId()), eq(null), eq(singletonList(emailChannel)));
    }

    private CourtApplication buildCourtApplication(final SummonsTemplateType summonsTemplateType, final PartyType partyType) {
        return courtApplication()
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(summonsTemplateType).build())
                .withId(applicationId)
                .withApplicationReference(randomAlphabetic(20))
                .withApplicant(getParty(partyType))
                .withSubject(getParty(partyType))
                .withRespondents(newArrayList(getParty(partyType), getParty(partyType)))
                .build();
    }

    private CourtApplicationParty getParty(final PartyType partyType) {
        final CourtApplicationParty.Builder courtApplicationPartyBuilder = courtApplicationParty();
        switch (partyType) {
            case MASTER_DEFENDANT_PERSON:
                final Person defendantPerson = person()
                        .withFirstName(randomAlphabetic(10))
                        .withMiddleName(randomAlphabetic(10))
                        .withLastName(randomAlphabetic(10))
                        .build();
                courtApplicationPartyBuilder.withMasterDefendant(masterDefendant().withMasterDefendantId(randomUUID())
                        .withProsecutionAuthorityReference(randomAlphabetic(20))
                        .withPersonDefendant(personDefendant().withPersonDetails(defendantPerson).build())
                        .build());
                break;

            case MASTER_DEFENDANT_LEGAL_ENTITY:
                courtApplicationPartyBuilder.withMasterDefendant(masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withProsecutionAuthorityReference(randomAlphabetic(20))
                        .withLegalEntityDefendant(legalEntityDefendant().withOrganisation(organisation().withName(randomAlphabetic(10)).build()).build())
                        .build());
                break;
            case INDIVIDUAL:
                final Person individualPerson = person()
                        .withFirstName(randomAlphabetic(10))
                        .withMiddleName(randomAlphabetic(10))
                        .withLastName(randomAlphabetic(10))
                        .build();
                courtApplicationPartyBuilder.withPersonDetails(individualPerson);
                break;
            case ORGANISATION:
                courtApplicationPartyBuilder.withOrganisation(organisation().withName(randomAlphabetic(10)).build());
                break;
            case PROSECUTION_AUTHORITY:
                courtApplicationPartyBuilder.withProsecutingAuthority(prosecutingAuthority().withName(randomAlphabetic(10)).build());
                break;
        }

        return courtApplicationPartyBuilder
                .build();
    }

    private List<String> getPartyDetails(List<CourtApplicationParty> parties, final PartyType partyType) {
        switch (partyType) {
            case MASTER_DEFENDANT_PERSON:
                return parties
                        .stream()
                        .map(p -> {
                            final Person pd = p.getMasterDefendant().getPersonDefendant().getPersonDetails();
                            final String fullName = getFullName(pd.getFirstName(), pd.getMiddleName(), pd.getLastName());
                            return on(", ").join(fullName, p.getMasterDefendant().getProsecutionAuthorityReference());
                        })
                        .collect(toList());

            case MASTER_DEFENDANT_LEGAL_ENTITY:
                return parties
                        .stream()
                        .map(p -> {
                            final String fullName = p.getMasterDefendant().getLegalEntityDefendant().getOrganisation().getName();
                            return on(", ").join(fullName, p.getMasterDefendant().getProsecutionAuthorityReference());
                        })
                        .collect(toList());

            case INDIVIDUAL:
                return parties
                        .stream()
                        .map(p -> {
                            final Person pd = p.getPersonDetails();
                            final String fullName = getFullName(pd.getFirstName(), pd.getMiddleName(), pd.getLastName());
                            return fullName;
                        })
                        .collect(toList());

            case ORGANISATION:
                return parties
                        .stream()
                        .map(p -> p.getOrganisation().getName())
                        .collect(toList());

            case PROSECUTION_AUTHORITY:
                return parties
                        .stream()
                        .map(p -> p.getProsecutingAuthority().getName())
                        .collect(toList());

        }
        return emptyList();
    }

    private SummonsRejectedOutcome getRejectionOutcome() {
        return summonsRejectedOutcome()
                .withReasons(REASONS)
                .withProsecutorEmailAddress(APPLICANT_EMAIL_ADDRESS)
                .build();
    }
}