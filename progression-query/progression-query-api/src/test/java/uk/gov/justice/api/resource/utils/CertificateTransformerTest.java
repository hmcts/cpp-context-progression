package uk.gov.justice.api.resource.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.utils.FileUtil.getPayload;
import static uk.gov.justice.progression.courts.Offences.offences;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.HearingQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceDay;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.Jurors;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.courts.YouthCourt;
import uk.gov.justice.progression.courts.CourtApplications;
import uk.gov.justice.progression.courts.CustodialEstablishment;
import uk.gov.justice.progression.courts.DefenceOrganisation;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.Respondents;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtOrders;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CertificateTransformerTest {

    public static final String LEGACY_COMPENSATION = "Legacy Compensation";
    public static final String ORDERING = "Ordering";
    public static final String COMPENSATION = "Compensation";
    public static final String RESTRAINING_ORDER = "Restraining order";
    public static final String LEGACY_NAME = "Legacy Name";
    public static final String MINOR_CREDITOR_FIRST_NAME = "Minor creditor first name";
    public static final String AMOUNT_OF_COMPENSATION = "Amount of compensation";
    public static final String PROTECTED_PERSON_S_NAME = "Protected person's name";
    public static final String PROTECTED_PERSON_S_ADDRESS_ADDRESS_LINE_1 = "Protected person's address address line 1";
    public static final String THIS_ORDER_IS_MADE_ON = "this order is made on";
    public static final String LEGACY_RESULT = "Legacy Result";
    public static final String RESTRAINING_ORDER_DEFENDANT_LEVEL = "Restraining order Defendant Level";
    public static final String COMPENSATION_DEFENDANT_LEVEL = "Compensation Defendant Level";
    public static final String LEGACY_COMPENSATION_DEFENDANT_LEVEL = "Legacy Compensation Defendant Level";
    public static final String ORDERING_DEFENDANT_LEVEL = "Ordering Defendant Level";
    private static final UUID CASE_ID = randomUUID();
    private static final String PROSECUTION_AUTHORITY_CODE = "TFL";
    private static final String GUILTY = "Guilty";
    private static final String PLEA_GUILTY_DESCRIPTION = "plea guilty description";
    private static final String COUNSELS_STATUS = "counsels status";
    private static final String ORGANISATION_NAME = "Liverpool defence";
    private static final String ASSOCIATED_ORGANISATION_NAME = "Associated Defence Org";
    private static final String DESCRIPTION = "Dodged TFL tickets with passion";
    private static final String APPLICATION_TYPE = "Apil";
    private static final String HEARING1 = "HEARING-1";
    private static final String HEARING2 = "HEARING-2";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID DEFENDANT_ID_2ND = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final LocalDate DOB = LocalDate.of(LocalDate.now().getYear() - 30, 01, 01);
    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-06-04T10:00:00.000Z";
    private static final String HEARING_DATE_3 = "2018-07-01T10:00:00.000Z";
    private static final String CUSTODY_TYPE = "Prison";
    private static final String CUSTODY_ESTABLISHMENT_NAME = "HMP Belmarsh";
    private static final UUID CUSTODY_ESTABLISHMENT_UUID = randomUUID();
    private static final String DEFENDANT_NAME = "Harry JackKane Junior";
    private static final String COURT_NAME = "liver pool";
    private static final String HEARING_TYPE = "Sentence";
    private static final String ADDRESS_1 = "22";
    private static final String ADDRESS_2 = "Acacia Avenue";
    private static final String ADDRESS_3 = "Acacia Town";
    private static final String POST_CODE = "CR7 0AA";
    private static final String PAR = "6225bd77";
    private static final String LABEL = "Fine";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Denial";
    private static final UUID USER_ID = randomUUID();
    private static final String FIRST_NAME_2 = "First name 2";
    private static final String LAST_NAME_2 = " Last name 2";
    private static final String PROMPT_VALUE = "10";
    private static final String COURT_EXTRACT = "Y";
    private static final String NO_COURT_EXTRACT = "N";
    private static final String PLEA_GUILTY = "GUILTY";
    private static final LocalDate CONVICTION_DATE = LocalDate.of(2018, 04, 04);
    private static final LocalDate PLEA_DATE = LocalDate.of(2018, 01, 01);
    private static final String DEFENDANT_AGE = "30";
    private static final LocalDate OUTCOME_DATE = LocalDate.of(2019, 07, 20);
    private static final LocalDate RESPONSE_DATE = LocalDate.of(2019, 07, 25);
    private static final LocalDate APPLICATION_DATE = LocalDate.now();
    private static final String APPLICATION_PARTICULARS = "Application particulars";
    private static final LocalDate ASSOCIATION_START_DATE = LocalDate.now();
    private static final LocalDate ASSOCIATION_END_DATE = ASSOCIATION_START_DATE.plusDays(10);
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String CONVICTION_COURT_LOCATION = "courtHearingLocation";
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private final ProsecutionCase prosecutionCase = createProsecutionCase();
    public static final String AFTER_TRIAL_ON_INDICTMENT = "After trial on indictment";

    private ReportsTransformer target;

    @InjectMocks
    private TransformationHelper transformationHelper;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private UsersAndGroupsService usersAndGroupsService;

    @Spy
    private CourtExtractHelper courtExtractHelper = new CourtExtractHelper();

    @Mock
    private ListingQueryService listingQueryService;

    @Mock
    private DefenceQueryService defenceQueryService;

    @Mock
    private HearingQueryService hearingQueryService;

    @BeforeEach
    public void init() {
        target = new ReportsTransformer();
        setField(this.target, "transformationHelper", transformationHelper);
        setField(this.target, "courtExtractHelper", courtExtractHelper);
        setField(this.target, "listingQueryService", listingQueryService);
        setField(this.target, "referenceDataService", referenceDataService);
        setField(this.target, "defenceQueryService", defenceQueryService);
        setField(this.target, "hearingQueryService", hearingQueryService);
        setField(this.courtExtractHelper, "usersAndGroupsService", usersAndGroupsService);

    }

    @Test
    void shouldTransformConvictionOfCertificate() throws IOException {
        Set<String> guiltyPleaTypes = new HashSet<>();
        guiltyPleaTypes.add("GUILTY");
        when(referenceDataService.retrieveGuiltyPleaTypes()).thenReturn(guiltyPleaTypes);

        final LocalDate convictionDate = LocalDate.of(2020, 04, 06);
        final LocalDate convictionDate1 = LocalDate.of(2025, 05, 06);

        final String extractType = "CertificateOfConviction";
        final List<String> hearingIds = asList(HEARING_ID.toString(), HEARING_ID_2.toString());

        final GetHearingsAtAGlance.Builder hearingsAtAGlance = GetHearingsAtAGlance.getHearingsAtAGlance().withId(CASE_ID);
        hearingsAtAGlance.withProsecutionCaseIdentifier(createPCIdentifier());
        hearingsAtAGlance.withDefendantHearings(createDefendantHearing());
        hearingsAtAGlance.withHearings(createHearingsCOC());

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance.build(), DEFENDANT_ID.toString(), extractType, hearingIds, randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getDataVariation(), is(AFTER_TRIAL_ON_INDICTMENT));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getConvictionDate(), is(convictionDate));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getLocation(), is("liver pool"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getSentenceLocation(), is(CONVICTION_COURT_LOCATION));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getOffences().size(), is(1));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(1).getDataVariation(), is(AFTER_TRIAL_ON_INDICTMENT));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(1).getConvictionDate(), is(convictionDate.plusDays(5)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(1).getLocation(), is("liver pool"));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(0).getSentenceLocation(), is(CONVICTION_COURT_LOCATION));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getConvictedOffencesDetails().get(1).getOffences().size(), is(2));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(0).getDataVariation(), is(AFTER_TRIAL_ON_INDICTMENT));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(0).getConvictionDate(), is(convictionDate1));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(0).getOffences().size(), is(1));

        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(1).getDataVariation(), is(AFTER_TRIAL_ON_INDICTMENT));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(1).getConvictionDate(), is(convictionDate1.plusDays(5)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getConvictedOffencesDetails().get(1).getOffences().size(), is(2));
    }

    @Test
    void shouldTransformConvictionOfCertificateForBreachApplicationAndFilterOutBoxHearing() {
        final String extractType = "CertificateOfConviction";
        final UUID breachApplicationHearing = randomUUID();
        final GetHearingsAtAGlance hearingAtAGlance = createHearingAtAGlanceWithBreachTypeApplication(DEFENDANT_ID.toString(), HEARING_ID.toString(), breachApplicationHearing.toString());
        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingAtAGlance, DEFENDANT_ID.toString(), extractType, emptyList(), randomUUID(), prosecutionCase);

        assertThat(courtExtractRequested.getDefendant().getHearings().size(), is((2)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(0).getCourtApplications().isEmpty(), is((true)));
        assertThat(courtExtractRequested.getDefendant().getHearings().get(1).getCourtApplications().size(), is((1)));

        final uk.gov.justice.progression.courts.exract.CourtApplications breachApplication = courtExtractRequested.getDefendant().getHearings().get(1).getCourtApplications().get(0);
        assertThat(breachApplication.getCourtOrders().getCanBeSubjectOfBreachProceedings(), is((true)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().size(), is((1)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getOffenceCode(), is(("CJ03522")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getOffenceTitle(), is(("Possess / control TV set with intent another use install without a licence")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getWording(), is(("Original CaseURN: 28DI8505400, Re-sentenced Original code : CA03012, Original details: Micaela Marks have set up a TV cable without a valid license.")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getResultTextList().size(), is((2)));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getPlea().getPleaValue(), is(("GUILTY")));
        assertThat(breachApplication.getCourtOrders().getCourtOrderOffences().get(0).getIndicatedPlea().getIndicatedPleaValue().name(), is(("INDICATED_GUILTY")));
        assertThat(breachApplication.getConvictionDate().toString(), is("2025-05-21"));
        assertThat(courtExtractRequested.getIsAppealPending(), is(false));
    }

    @Test
    void shouldTransformConvictionOfCertificateForBreachApplication() throws IOException {
        String defendantId = "f1c3ec3c-2a6a-4a59-a2d7-d969616761aa";

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-breach-type-application-COC.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase1 = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "CertificateOfConviction", emptyList(), randomUUID(), prosecutionCase1);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(2));
        assertThat(defendant.getHearings().get(0).getOffences().size(), is(1));
        final List<uk.gov.justice.progression.courts.exract.CourtApplications> courtApplications = defendant.getHearings().get(1).getCourtApplications();
        assertThat(courtApplications.size(), is(1));
        final CourtOrders courtOrders = courtApplications.get(0).getCourtOrders();
        assertThat(courtOrders.getCourtOrderOffences().size(), is(1));
        assertThat(courtOrders.getCourtOrderOffences().get(0).getConvictionDate().toString(), is("2025-05-14"));
        assertThat(courtApplications.get(0).getConvictionDate().toString(), is("2025-05-21"));
        assertThat(defendant.getHearings().get(1).getConvictedOffencesDetails().get(0).getBreachApplicationConvictionDate().toString(), is("2025-05-21"));
    }

    @Test
    void shouldTransformCertificateOfAcquittal() throws IOException {
        String defendantId = "5578d027-5a07-49b0-8317-f1a0dab13657";
        Set<String> guiltyPleaTypes = new HashSet<>();
        guiltyPleaTypes.add(GUILTY);
        when(referenceDataService.retrieveGuiltyPleaTypes()).thenReturn(guiltyPleaTypes);

        final JsonObject prosecutionCasePayload = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-three-offence-COA.json"));
        final JsonObject hearingsAtAGlanceJson = prosecutionCasePayload.getJsonObject("hearingsAtAGlance");
        final JsonObject prosecutionCaseJson = prosecutionCasePayload.getJsonObject("prosecutionCase");
        GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = target.getCourtExtractRequested(hearingsAtAGlance, defendantId, "CertificateOfAcquittal", emptyList(), randomUUID(), prosecutionCase);

        final uk.gov.justice.progression.courts.exract.Defendant defendant = courtExtractRequested.getDefendant();

        assertThat(defendant.getHearings().size(), is(1));
        final List<uk.gov.justice.progression.courts.exract.Offences> offences = defendant.getHearings().get(0).getOffences();
        assertThat(offences.size(), is(3));
        assertThat(offences.get(0).getAquittalDate().toString(), is("2025-05-15"));
        assertThat(offences.get(1).getAquittalDate().toString(), is("2025-05-16"));
        assertThat(offences.get(2).getAquittalDate().toString(), is("2025-05-17"));
    }

    private ProsecutionCase createProsecutionCase() {
        final Defendant defendant = Defendant.defendant().withId(DEFENDANT_ID)
                .withPersonDefendant(createPersonDefendant())
                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                        .withPerson(createPerson())
                        .build()))
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANISATION_NAME)
                        .withAddress(createAddress())
                        .withContact(createContact())
                        .build())
                .withOffences(Collections.emptyList())
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .build();

        final Defendant defendant_2nd = Defendant.defendant().withId(DEFENDANT_ID_2ND)
                .withPersonDefendant(createPersonDefendant())
                .withAssociatedPersons(asList(AssociatedPerson.associatedPerson()
                        .withPerson(createPerson())
                        .build()))
                .withDefenceOrganisation(Organisation.organisation()
                        .withName(ORGANISATION_NAME)
                        .withAddress(createAddress())
                        .withContact(createContact())
                        .build())
                .withAssociatedDefenceOrganisation(AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                        .withDefenceOrganisation(uk.gov.justice.core.courts.DefenceOrganisation.defenceOrganisation()
                                .withOrganisation(Organisation.organisation()
                                        .withName(ASSOCIATED_ORGANISATION_NAME)
                                        .withAddress(createAddress())
                                        .withContact(createContact())
                                        .build()).build())
                        .withAssociationStartDate(ASSOCIATION_START_DATE)
                        .withAssociationEndDate(ASSOCIATION_END_DATE)
                        .build())
                .withOffences(Collections.emptyList())
                .withMasterDefendantId(DEFENDANT_ID_2ND)
                .build();
        final List<Defendant> defendants = new ArrayList<Defendant>() {{
            add(defendant);
            add(defendant_2nd);
        }};

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityReference(PAR)
                        .build())
                .build();

        return prosecutionCase;
    }

    private GetHearingsAtAGlance createHearingAtAGlanceWithBreachTypeApplication(final String defendantId, final String hearingId, final String breachApplicationHearingId) {
        final JsonObject inActiveCaseWithBreachTypeApplication = stringToJsonObjectConverter.convert(getPayload("progression.query.prosecutioncase-breach-type-application.json")
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("BREACH_H_ID", breachApplicationHearingId));
        final JsonObject hearingsAtAGlanceJson = inActiveCaseWithBreachTypeApplication.getJsonObject("hearingsAtAGlance");
        return jsonObjectToObjectConverter.convert(hearingsAtAGlanceJson, GetHearingsAtAGlance.class);
    }

    private List<Hearings> createHearingsCOC() {
        final LocalDate CONVICTION_DATE1 = LocalDate.of(2020, 04, 04);
        final LocalDate CONVICTION_DATE2 = LocalDate.of(2025, 05, 04);

        return asList(
                Hearings.hearings()
                        .withId(HEARING_ID)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(CONVICTION_DATE1, CONVICTION_COURT_LOCATION, asList(DEFENDANT_ID, DEFENDANT_ID_2ND), asList(randomUUID(), randomUUID(), randomUUID()), HEARING1, HEARING_ID, "CertificateOfConviction"))
                        .withDefendantAttendance(createDefendantAttendance(asList(DEFENDANT_ID, DEFENDANT_ID_2ND)))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING1))
                        .withRespondentCounsels(createRespondentCounsels(HEARING1))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING1))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING1))
                        .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                        .build(),
                Hearings.hearings()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription(HEARING_TYPE)
                                .build())
                        .withDefendants(createDefendants(CONVICTION_DATE2, null, asList(DEFENDANT_ID), asList(randomUUID(), randomUUID(), randomUUID()), HEARING2, HEARING_ID_2, "CertificateOfConviction"))
                        .withDefendantReferralReasons(createDefendantReferralReasons())
                        .withApplicantCounsels(createApplicationCounsels(HEARING2))
                        .withRespondentCounsels(createRespondentCounsels(HEARING2))
                        .withCompanyRepresentatives(createCompanyRepresentatives(HEARING2))
                        .withProsecutionCounsels(createProsecutionCounsels(HEARING2))
                        .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                        .build()
        );
    }

    private List<DefendantAttendance> createDefendantAttendance(final List<UUID> defendantIds) {
        return defendantIds.stream().map(id -> DefendantAttendance.defendantAttendance()
                .withDefendantId(id)
                .withAttendanceDays(asList(AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.IN_PERSON)
                                .build(), AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.BY_VIDEO)
                                .build(),
                        AttendanceDay.attendanceDay()
                                .withDay(LocalDate.now())
                                .withAttendanceType(AttendanceType.NOT_PRESENT)
                                .build()))
                .build()).collect(Collectors.toList());
    }

    private List<ApplicantCounsel> createApplicationCounsels(final String hearing) {
        return asList(ApplicantCounsel.applicantCounsel()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withStatus("Jury")
                .build());
    }

    private List<RespondentCounsel> createRespondentCounsels(final String hearing) {
        return asList(RespondentCounsel.respondentCounsel()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withStatus("Solicitor")
                .build());
    }

    private List<ReferralReason> createDefendantReferralReasons() {
        return asList(ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(DEFENDANT_ID)
                .withDescription(DESCRIPTION)
                .build());
    }

    private List<JudicialRole> createJudiciary() {
        return asList(
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(true)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("District Judge")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Magistrate")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Circuit Judge")
                                        .build()
                        )
                        .build()
        );
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName(COURT_NAME)
                .withWelshName("Welsh " + COURT_NAME)
                .build();
    }

    private List<Defendants> createDefendants(final LocalDate convictionDate, final String courtHearingLocation, final List<UUID> defendantIdList, final List<UUID> offenceIdList, final String hearing, final UUID hearingId, final String extractType) {
        return defendantIdList.stream().map(id -> createDefendant(convictionDate, courtHearingLocation, id, offenceIdList, hearing, hearingId, extractType)).collect(toList());
    }

    private Defendants createDefendant(final LocalDate convictionDate, final String courtHearingLocation, final UUID defendantId, final List<UUID> offenceIdList, final String hearing, final UUID hearingId, final String extractType) {
        Defendants.Builder defendantBuilder = Defendants.defendants()
                .withId(defendantId)
                .withMasterDefendantId(defendantId)
                .withAddress(createAddress())
                .withDateOfBirth(DOB)
                .withAge(DEFENDANT_AGE)
                .withLegalAidStatus("legal aid")
                .withJudicialResults(createResults(hearingId))
                .withDefenceOrganisation(createDefenceOrganisation(hearing))
                .withCourtApplications(asList(createCourtApplications()))
                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                        .withCustody(CUSTODY_TYPE)
                        .withId(CUSTODY_ESTABLISHMENT_UUID)
                        .withName(CUSTODY_ESTABLISHMENT_NAME)
                        .build());

        if ("CertificateOfConviction" .equals(extractType)) {
            defendantBuilder.withOffences(createOffenceForCOC(convictionDate, courtHearingLocation, offenceIdList, hearingId));
        } else {
            defendantBuilder.withOffences(createOffence(OFFENCE_ID, hearingId));

        }

        return defendantBuilder.build();
    }

    private PersonDefendant createPersonDefendant() {
        return PersonDefendant.personDefendant()
                .withPersonDetails(Person.person()
                        .withAddress(createAddress())
                        .withDateOfBirth(DOB)
                        .withFirstName("Harry")
                        .withMiddleName("JackKane")
                        .withLastName("Junior")
                        .build()).
                withArrestSummonsNumber("Arrest123").build();

    }

    private List<Offences> createOffence(final UUID offenceId, final UUID hearingId) {
        return asList(
                offences()
                        .withId(offenceId)
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withConvictionDate(CONVICTION_DATE)
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts()).build()
        );
    }

    private List<Offences> createOffenceForCOC(final LocalDate convictionDate, final String courtHearingLocation, final List<UUID> offenceIdList, UUID hearingId) {
        return asList(
                offences()
                        .withId(offenceIdList.get(0))
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withCount(3)
                        .withConvictionDate(convictionDate.plusWeeks(1))
                        .withConvictingCourt(nonNull(courtHearingLocation) ? CourtCentre.courtCentre().withName(courtHearingLocation).build() : null)
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withIndictmentParticular("Offence 1 Indictment Particular")
                        .withVerdicts(createVerdicts()).build(),
                offences()
                        .withId(offenceIdList.get(1))
                        .withCount(1)
                        .withOrderIndex(Integer.valueOf(randomNumeric(1)))
                        .withConvictionDate(convictionDate.plusDays(2))
                        .withConvictingCourt(nonNull(courtHearingLocation) ? CourtCentre.courtCentre().withName(courtHearingLocation).build() : null)
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withIndictmentParticular("Offence 2 Indictment Particular")
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts()).build(),
                offences()
                        .withId(offenceIdList.get(2))
                        .withCount(5)
                        .withOrderIndex(Integer.valueOf(randomNumeric(2)))
                        .withConvictionDate(convictionDate.plusWeeks(1))
                        .withConvictingCourt(CourtCentre.courtCentre().withName(courtHearingLocation).build())
                        .withJudicialResults(createResults(hearingId))
                        .withPleas(createPlea())
                        .withIndicatedPlea(createIndicatedPlea())
                        .withAllocationDecision(createAllocationDecision())
                        .withVerdicts(createVerdicts())
                        .build()
        );
    }

    private AllocationDecision createAllocationDecision() {
        return AllocationDecision.allocationDecision()
                .withAllocationDecisionDate(CONVICTION_DATE)
                .withMotReasonCode("4")
                .withSequenceNumber(40)
                .withOffenceId(OFFENCE_ID)
                .withOriginatingHearingId(HEARING_ID)
                .withMotReasonId(randomUUID())
                .withMotReasonDescription("Defendant chooses trial by jury")
                .build();
    }

    private IndicatedPlea createIndicatedPlea() {
        return IndicatedPlea.indicatedPlea()
                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                .build();
    }

    private List<JudicialResult> createResults(final UUID hearingId) {
        return asList(
                JudicialResult.judicialResult()
                        .withIsAvailableForCourtExtract(true)
                        .withLabel(LABEL)
                        .withOrderedHearingId(hearingId)
                        .withJudicialResultPrompts(createPrompts())
                        .withDelegatedPowers(createDelegatedPower())
                        .withResultText("resultText")
                        .withResultWording("resultWording")
                        .build()
        );
    }

    private DelegatedPowers createDelegatedPower() {
        return DelegatedPowers.delegatedPowers()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withUserId(USER_ID)
                .build();
    }

    private List<JudicialResultPrompt> createPrompts() {
        return asList(
                JudicialResultPrompt.judicialResultPrompt()
                        .withLabel(LABEL)
                        .withCourtExtract(COURT_EXTRACT)
                        .withValue(PROMPT_VALUE)
                        .build()
        );
    }

    private Address createAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withPostcode(POST_CODE)
                .build();
    }

    private List<HearingDay> createHearingDays() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build()
        );
    }

    private List<HearingDay> createHearingDays2() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_3))
                        .build()
        );
    }

    private List<DefendantHearings> createDefendantHearing() {
        final List<UUID> hearingIds = new ArrayList<>();
        hearingIds.add(HEARING_ID);
        hearingIds.add(HEARING_ID_2);

        return Stream.of(DEFENDANT_ID, DEFENDANT_ID_2ND).map(id -> DefendantHearings.defendantHearings()
                .withDefendantId(id)
                .withMasterDefendantId(id)
                .withHearingIds(hearingIds)
                .withDefendantName(DEFENDANT_NAME)
                .build()).collect(toList());

    }

    private ProsecutionCaseIdentifier createPCIdentifier() {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityReference(PAR)
                .build();
    }

    private List<Verdict> createVerdicts() {
        return asList(Verdict.verdict()
                .withVerdictType(VerdictType.verdictType()
                        .withCategory(GUILTY)
                        .withCategoryType(GUILTY)
                        .withDescription(PLEA_GUILTY_DESCRIPTION)
                        .withSequence(1)
                        .build())
                .withVerdictDate(LocalDate.of(2018, 02, 02))
                .withJurors(Jurors.jurors()
                        .withNumberOfJurors(2)
                        .withNumberOfSplitJurors(2)
                        .withUnanimous(true)
                        .build())
                .withOffenceId(UUID.randomUUID())
                .build());
    }

    private List<Plea> createPlea() {
        return asList(
                Plea.plea()
                        .withDelegatedPowers(DelegatedPowers.delegatedPowers()
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withUserId(USER_ID)
                                .build())
                        .withPleaDate(PLEA_DATE)
                        .withPleaValue(PLEA_GUILTY)
                        .build());
    }

    private DefenceOrganisation createDefenceOrganisation(final String hearing) {
        return
                DefenceOrganisation.defenceOrganisation().withDefenceOrganisation(
                                Organisation.organisation()
                                        .withName(ORGANISATION_NAME)
                                        .withAddress(createAddress())
                                        .withContact(createContact())
                                        .build()
                        )
                        .withDefenceCounsels(createDefenceCounsels(hearing))
                        .withDefendantId(DEFENDANT_ID)
                        .build();
    }

    private List<DefenceCounsel> createDefenceCounsels(final String hearing) {
        return asList(
                DefenceCounsel.defenceCounsel()
                        .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                        .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                        .withAttendanceDays(asList(LocalDate.now()))
                        .withStatus(COUNSELS_STATUS)
                        .build()
        );
    }

    private List<ProsecutionCounsel> createProsecutionCounsels(final String hearing) {
        return asList(
                ProsecutionCounsel.prosecutionCounsel()
                        .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                        .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                        .withAttendanceDays(asList(LocalDate.now()))
                        .withStatus(COUNSELS_STATUS)
                        .build()
        );
    }

    private List<CompanyRepresentative> createCompanyRepresentatives(final String hearing) {
        return asList(CompanyRepresentative.companyRepresentative()
                .withFirstName(hearing.equals(HEARING1) ? FIRST_NAME : FIRST_NAME_2)
                .withLastName(hearing.equals(HEARING1) ? LAST_NAME : LAST_NAME_2)
                .withPosition("DIRECTOR")
                .withAttendanceDays(asList(LocalDate.now()))
                .build());
    }

    private ContactNumber createContact() {
        return ContactNumber.contactNumber()
                .withFax("fax")
                .withHome("home")
                .withMobile("mobile")
                .build();
    }

    private Person createPerson() {
        return Person.person()
                .withDateOfBirth(DOB)
                .withFirstName(FIRST_NAME)
                .withMiddleName("D")
                .withLastName(LAST_NAME)
                .withAddress(createAddress())
                .withTitle("MR")
                .build();
    }

    private CourtApplications createCourtApplications() {
        return CourtApplications.courtApplications()
                .withApplicationId(APPLICATION_ID)
                .withApplicationReceivedDate(APPLICATION_DATE)
                .withOutcome("Granted")
                .withOutcomeDate(OUTCOME_DATE)
                .withRespondents(createRespondents())
                .withJudicialResults(createResults(HEARING_ID))
                .build();
    }

    private List<Respondents> createRespondents() {
        return asList(Respondents.respondents()
                .withApplicationResponse("Admitted")
                .withResponseDate(RESPONSE_DATE)
                .build());
    }
}