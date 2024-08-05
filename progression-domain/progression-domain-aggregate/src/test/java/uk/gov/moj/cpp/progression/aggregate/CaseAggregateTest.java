package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.core.courts.FormCreated.formCreated;
import static uk.gov.justice.core.courts.FormDefendants.formDefendants;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.core.courts.FormType.PTPH;
import static uk.gov.justice.core.courts.HearingResultedCaseUpdated.hearingResultedCaseUpdated;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;
import static uk.gov.justice.core.courts.JudicialResultCategory.INTERMEDIARY;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.LaaReference.laaReference;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.PetDefendants.petDefendants;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseCreated.prosecutionCaseCreated;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.string;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation.updateMatchedDefendantCustodialInformation;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.READY_FOR_REVIEW;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.SJP_REFERRAL;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.GRANTED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.REFUSED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;
import static uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType.COMPANYONLINEPLEA;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDefendantUpdateRequested;
import uk.gov.justice.core.courts.CaseCpsDetailsUpdatedFromCourtDocument;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.CaseDefendantUpdatedWithDriverNumber;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CaseMarkersSharedWithHearings;
import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.core.courts.CaseRetentionPolicyRecorded;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CpsPersonDefendantDetails;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.DefendantSubject;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Defendants;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsAndListingHearingRequestsAdded;
import uk.gov.justice.core.courts.DefendantsNotAddedToCourtProceedings;
import uk.gov.justice.core.courts.DocumentWithProsecutionCaseIdAdded;
import uk.gov.justice.core.courts.EditFormRequested;
import uk.gov.justice.core.courts.ExactMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.FormCreated;
import uk.gov.justice.core.courts.FormDefendants;
import uk.gov.justice.core.courts.FormDefendantsUpdated;
import uk.gov.justice.core.courts.FormFinalised;
import uk.gov.justice.core.courts.FormOperationFailed;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.FormUpdated;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.LockStatus;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PartialMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormDefendantUpdated;
import uk.gov.justice.core.courts.PetFormFinalised;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.core.courts.PetFormUpdated;
import uk.gov.justice.core.courts.PetOperationFailed;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseCreatedInHearing;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberDecreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseSubject;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.ReapplyMiReportingRestrictions;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.progression.courts.CaseRetentionLengthCalculated;
import uk.gov.justice.progression.courts.CustodyTimeLimitExtended;
import uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdated;
import uk.gov.justice.progression.courts.DefendantsAndListingHearingRequestsStored;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
import uk.gov.justice.progression.courts.HearingEventLogsDocumentCreated;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.progression.courts.HearingRemovedForProsecutionCase;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation;
import uk.gov.moj.cpp.progression.command.handler.HandleOnlinePleaDocumentCreation;
import uk.gov.moj.cpp.progression.domain.CaseToUnlink;
import uk.gov.moj.cpp.progression.domain.CasesToLink;
import uk.gov.moj.cpp.progression.domain.MatchDefendant;
import uk.gov.moj.cpp.progression.domain.MatchedDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.Form;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.FormLockStatus;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.domain.helper.JsonHelper;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.CaseNotFound;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.CpsDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefendantCustodialEstablishmentRemoved;
import uk.gov.moj.cpp.progression.events.DefendantCustodialInformationUpdateRequested;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantNotFound;
import uk.gov.moj.cpp.progression.events.DefendantsMasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.FinanceDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.events.LinkCases;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MergeCases;
import uk.gov.moj.cpp.progression.events.NotificationSentForDefendantDocument;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocument;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocumentFailed;
import uk.gov.moj.cpp.progression.events.OnlinePleaDocumentUploadedAsCaseMaterial;
import uk.gov.moj.cpp.progression.events.OnlinePleaPcqVisitedRecorded;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.moj.cpp.progression.events.PleaDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.events.SplitCases;
import uk.gov.moj.cpp.progression.plea.json.schemas.ContactDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.OffencePleaDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleaType;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;


@RunWith(MockitoJUnitRunner.class)
public class CaseAggregateTest {

    private static final String CASE_ID = randomUUID().toString();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String COURT_CENTRE_NAME = "Warwick Justice Centre";
    private static final String COURT_CENTRE_ID = "1234";
    private static final String HEARING_TYPE = "PTP";
    private static final String SENDING_COMMITTAL_DATE = "01-01-1990";
    private static final String CASE_URN = "87GD9945217";
    private static final String DEFENDANT_PERSON_ID = randomUUID().toString();
    private static final String DEFENDANT_FIRST_NAME = "David";
    private static final String DEFENDANT_LAST_NAME = "Lloyd";
    private static final String DEFENDANT_NATIONALITY = "British";
    private static final String DEFENDANT_GENDER = "Male";
    private static final String DEFENDANT_ADDRESS_1 = "Lima Court";
    private static final String DEFENDANT_ADDRESS_2 = "Bath Road";
    private static final String DEFENDANT_ADDRESS_3 = "Norwich";
    private static final String DEFENDANT_ADDRESS_4 = "UK";
    private static final String DEFENDANT_POSTCODE = "NR11HF";
    private static final String DEFENDANT_DATE_OF_BIRTH = "23-10-1995";
    private static final String BAIL_STATUS = "bailed";
    private static final String CUSTODY_TIME_LIMIT_DATE = "2018-01-30";
    private static final String DEFENCE_ORGANISATION = "Solicitor Jacob M";
    private static final boolean INTERPRETER_NEEDED = false;
    private static final String INTERPRETER_LANGUAGE = "English";
    private static final String OFFENCE_ID = randomUUID().toString();
    private static final String OFFENCE_CODE = "OF61131";
    private static final String SECTION = "S 51";
    private static final String WORDING = "On 10 Oct ...";
    private static final String REASON = "Not stated";
    private static final String DESCRIPTION = "Not available";
    private static final String CATEGORY = "Civil";
    private static final String START_DATE = "10-10-2017";
    private static final String END_DATE = "11-11-2017";
    private static final String CC_HEARING_DATE = "15-10-2017";
    private static final String CC_COURT_CENTRE_NAME = "Liverpool crown court";
    private static final String CC_COURT_CENTRE_ID = randomUUID().toString();
    private static final String INDICATED_PLEA_ID = randomUUID().toString();
    private static final String INDICATED_PLEA_VALUE = "NO_INDICATION";
    private static final String ALLOCATION_DECISION = "COURT_DECLINED";

    private static final String FORM_CREATION_COMMAND_NAME = "form-created";
    private static final String FORM_FINALISATION_COMMAND_NAME = "finalise-form";
    private static final String FORM_EDIT_COMMAND_NAME = "edit-form";
    private static final String MESSAGE_FOR_DUPLICATE_COURT_FORM_ID = "courtFormId already exists";
    private static final String URN = "URN";
    private final String MESSAGE_FOR_PROSECUTION_NULL = "ProsecutionCase(%s) does not exists.";
    private final String MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT = "courtFormId (%s) does not exists.";
    private static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    private static final uk.gov.justice.core.courts.Offence offence = offence()
            .withId(randomUUID())
            .build();
    private static final Defendant defendant = Defendant.defendant()
            .withId(randomUUID())
            .withMasterDefendantId(randomUUID())
            .withPersonDefendant(PersonDefendant.personDefendant()
                    .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                            .withFirstName("firstName")
                            .withLastName("lastName")
                            .withDateOfBirth(LocalDate.now().minusYears(20))
                            .build())
                    .build())
            .withCourtProceedingsInitiated(ZonedDateTime.now())
            .withOffences(singletonList(offence().withId(randomUUID()).build()))
            .build();

    private static final Defendant legalEntityDefendant = Defendant.defendant()
            .withId(randomUUID())
            .withMasterDefendantId(randomUUID())
            .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                    .withOrganisation(Organisation.organisation().build()).build())
            .withCourtProceedingsInitiated(ZonedDateTime.now())
            .build();


    private static final List<Defendant> defendants = new ArrayList<Defendant>() {{
        add(defendant);
    }};

    private static final List<Defendant> legalEntityDefendants = new ArrayList<Defendant>() {{
        add(legalEntityDefendant);
    }};

    private static final ProsecutionCase prosecutionCase = createProsecutionCase(defendants);

    private static final ProsecutionCase prosecutionCaseWithLegalEntity = createProsecutionCase(legalEntityDefendants);

    @Mock
    JsonEnvelope envelope;

    @Mock
    JsonObject jsonObj;

    @Mock
    private Logger logger;

    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CaseAggregate caseAggregate;

    private final UUID hearingId = randomUUID();
    private final String hearingType = "Trial";

    private static final Map<FormType, Integer> lockDurationMapByFormType = new HashMap<FormType, Integer>() {{
        put(BCM, 60);
        put(PTPH, 60);
        put(PET, 60);
    }};

    private static LaaReference generateRecordLAAReferenceForOffence(final String statusCode, final String defendantLevelStatus) {
        return laaReference()
                .withApplicationReference("AB746921")
                .withStatusDate(LocalDate.now())
                .withStatusId(randomUUID())
                .withStatusCode(statusCode)
                .withStatusDescription("statusDescription")
                .withEffectiveStartDate(LocalDate.now())
                .withEffectiveEndDate(LocalDate.now())
                .withOffenceLevelStatus(defendantLevelStatus)
                .build();

    }

    @Before
    public void setUp() {
        this.caseAggregate = new CaseAggregate();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldNotRiseEventWhenNoLinkedHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final List<Object> response = caseAggregate.updateCaseMarkers(singletonList(Marker.marker().build()), caseId, hearingId).collect(toList());
        assertThat(response.size(), is(1));
        assertThat(response.get(0).getClass().toString(), is(CaseMarkersUpdated.class.toString()));

    }

    @Test
    public void shouldRaiseEventWhenLinkedHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        caseAggregate.linkProsecutionCaseToHearing(hearingId, caseId);

        final List<Object> response = caseAggregate.updateCaseMarkers(singletonList(Marker.marker().build()), caseId, hearingId).collect(toList());
        assertThat(response.size(), is(2));
        assertThat(response.get(0).getClass().toString(), is(CaseMarkersUpdated.class.toString()));
        assertThat(response.get(1).getClass().toString(), is(CaseMarkersSharedWithHearings.class.toString()));
    }

    @Test
    public void shouldReturnDocumentAdded() {

        final List<Object> eventStream = caseAggregate.addDocument(CourtDocument.courtDocument().build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object documentWithProsecutionCaseIdAdded = eventStream.get(0);
        assertThat(documentWithProsecutionCaseIdAdded.getClass(), is(CoreMatchers.<Class<?>>equalTo(DocumentWithProsecutionCaseIdAdded.class)));
    }

    @Test
    public void shouldHandleSentenceHearingDateAdded() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate sentenceHearingDate = LocalDate.now();
        createDefendant(defendantId);

        final SentenceHearingDateAdded sentenceHearingDateAdded =
                new SentenceHearingDateAdded(sentenceHearingDate, caseId);

        final Object response = this.caseAggregate.apply(sentenceHearingDateAdded);

        assertThat(((SentenceHearingDateAdded) response).getCaseId(), is(caseId));
        assertThat(((SentenceHearingDateAdded) response).getSentenceHearingDate(), is(sentenceHearingDate));
    }

    @Test
    public void shouldAddCaseToCrownCourt() {
        final UUID caseId = randomUUID();
        final UUID courtCentreId = randomUUID();


        final CaseAddedToCrownCourt caseAddedToCrownCourt =
                new CaseAddedToCrownCourt(caseId, courtCentreId.toString());

        final Object response = this.caseAggregate.apply(caseAddedToCrownCourt);

        assertThat(((CaseAddedToCrownCourt) response).getCaseId(), is(caseId));
        assertThat(((CaseAddedToCrownCourt) response).getCourtCentreId(), is(courtCentreId.toString()));
    }

    @Test
    public void shouldDoHearingResultedUpdateCase() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase().withDefendants(defendants).withId(caseId).build();
        final HearingResultedCaseUpdated prosecutionCaseUpdated = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);
        final Map<UUID, uk.gov.justice.core.courts.Offence> defendantCaseOffences = ReflectionUtil.getValueOfField(this.caseAggregate, "defendantCaseOffences", Map.class);

        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getId(), is(caseId));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(0).getId().toString(), is(defendantId1.toString()));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(1).getId().toString(), is(defendantId2.toString()));
        assertThat(((HearingResultedCaseUpdated) response).getProsecutionCase().getDefendants().get(2).getId().toString(), is(defendantId3.toString()));
        assertThat(defendantCaseOffences.containsKey(defendantId1), is(true));
        assertThat(defendantCaseOffences.containsKey(defendantId2), is(true));
        assertThat(defendantCaseOffences.containsKey(defendantId3), is(true));

    }

    @Test
    public void shouldChangeCaseStatusFromInactiveToReadyForReviewIfAllDefendantProceedingsAreNotConcluded() {
        final String caseURN = "case" + string(6).next();
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final List<Defendant> defendantsWithProceedingsConcluded = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendantsWithProceedingsConcluded).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = ProsecutionCaseCreated.prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();
        this.caseAggregate.apply(prosecutionCaseUpdated);

        final ProsecutionCase prosecutionCaseWithReadyForReview = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsWithProceedingsConcluded).withId(caseId).withCaseStatus(READY_FOR_REVIEW.toString()).build();

        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult);
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedReadyForReview = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithReadyForReview).build();

        final Stream<Object> eventStreamWithReadyForReview = this.caseAggregate.updateCase(prosecutionCaseWithReadyForReview, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedReadyForReview);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedReadyForReviewAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithReadyForReview.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedReadyForReviewAfterAggregate, caseId, INACTIVE);

        final List<Defendant> defendantsWithInactive = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);
        final ProsecutionCase prosecutionCaseWithInactive = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsWithInactive).withId(caseId).withCaseStatus(hearingResultedCaseUpdatedReadyForReview.getProsecutionCase().getCaseStatus()).build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedWithInactive = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithInactive).build();

        final Stream<Object> eventStreamForInactiveCase = this.caseAggregate.updateCase(prosecutionCaseWithInactive, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedWithInactive);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedWithInactiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamForInactiveCase.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedWithInactiveAfterAggregate, caseId, INACTIVE);

        final List<Defendant> defendantsAfterUpdate = updatedDefendantOffences(defendantsWithInactive, defendantId1, offenceId1, false);
        final ProsecutionCase prosecutionCaseAfterUpdate = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsAfterUpdate).withId(caseId).withCaseStatus(hearingResultedCaseUpdatedWithInactive.getProsecutionCase().getCaseStatus()).build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdate = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseAfterUpdate).build();

        final List<DefendantJudicialResult> updatedDefendantJudicialResults = new ArrayList<>();
        final Stream<Object> eventStreamAfterUpdateAfterAggregate = this.caseAggregate.updateCase(prosecutionCaseAfterUpdate, updatedDefendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedAfterUpdate);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdateAfterAggregate = (HearingResultedCaseUpdated) eventStreamAfterUpdateAfterAggregate.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedAfterUpdateAfterAggregate, caseId, READY_FOR_REVIEW);
        assertThat(getProceedingsConcludedStatus(defendantId1, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(false));
        assertThat(getProceedingsConcludedStatus(defendantId2, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(true));
        assertThat(getProceedingsConcludedStatus(defendantId3, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(true));
    }

    @Test
    public void shouldUpdateListingNumberOfCase() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendants).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);

        final List<ProsecutionCaseListingNumberUpdated> events = this.caseAggregate.updateListingNumber(singletonList(OffenceListingNumbers.offenceListingNumbers()
                .withListingNumber(10)
                .withOffenceId(defendants.get(0).getOffences().get(0).getId())
                .build())).map(ProsecutionCaseListingNumberUpdated.class::cast).collect(toList());

        final ProsecutionCase ProsecutionCaseInAggregate = ReflectionUtil.getValueOfField(this.caseAggregate, "prosecutionCase", ProsecutionCase.class);

        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(10));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getListingNumber(), is(5));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(2).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getListingNumber(), is(nullValue()));

        assertThat(events.get(0).getProsecutionCaseId(), is(caseId));
        assertThat(events.get(0).getOffenceListingNumbers().get(0).getListingNumber(), is(10));
        assertThat(events.get(0).getOffenceListingNumbers().get(0).getOffenceId(), is(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId()));
    }

    @Test
    public void shouldDecreaseListingNumberOfCase() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendants).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);


        final List<ProsecutionCaseListingNumberDecreased> events = this.caseAggregate.decreaseListingNumbers(singletonList(defendants.get(1).getOffences().get(0).getId())).map(ProsecutionCaseListingNumberDecreased.class::cast).collect(toList());

        final ProsecutionCase ProsecutionCaseInAggregate = ReflectionUtil.getValueOfField(this.caseAggregate, "prosecutionCase", ProsecutionCase.class);

        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(nullValue()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getListingNumber(), is(4));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(2).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getListingNumber(), is(nullValue()));

        assertThat(events.get(0).getProsecutionCaseId(), is(caseId));
        assertThat(events.get(0).getOffenceIds().get(0), is(defendants.get(1).getOffences().get(0).getId()));

    }

    @Test
    public void shouldNotUpdateListingNumberOfCaseWhenNewNumberSmall() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendants).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);


        final List<ProsecutionCaseListingNumberUpdated> events = this.caseAggregate.updateListingNumber(singletonList(OffenceListingNumbers.offenceListingNumbers()
                .withListingNumber(4)
                .withOffenceId(defendants.get(1).getOffences().get(0).getId())
                .build())).map(ProsecutionCaseListingNumberUpdated.class::cast).collect(toList());

        final ProsecutionCase prosecutionCaseInAggregate = ReflectionUtil.getValueOfField(this.caseAggregate, "prosecutionCase", ProsecutionCase.class);

        assertThat(prosecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(prosecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(nullValue()));
        assertThat(prosecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId()));
        assertThat(prosecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getListingNumber(), is(5));
        assertThat(prosecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(2).getOffences().get(0).getId()));
        assertThat(prosecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getListingNumber(), is(nullValue()));

        assertThat(events.get(0).getProsecutionCaseId(), is(caseId));
        assertThat(events.get(0).getOffenceListingNumbers().get(0).getListingNumber(), is(4));
        assertThat(events.get(0).getOffenceListingNumbers().get(0).getOffenceId(), is(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId()));
    }

    @Test
    public void shouldChangeCaseStatusReadyForReviewToInactive() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final String caseURN = "case" + string(6).next();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendantsWithReadyForReview = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);

        final ProsecutionCase prosecutionCaseWithNoCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .withId(randomUUID()).withDefendants(defendantsWithReadyForReview).withId(caseId).build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        this.caseAggregate.createProsecutionCase(prosecutionCaseWithNoCaseStatus);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final ProsecutionCase prosecutionCaseWithReadyForReview = prosecutionCase()
                .withDefendants(defendantsWithReadyForReview)
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .withId(caseId)
                .withCaseStatus(READY_FOR_REVIEW.toString())
                .build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedReadyForReview = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithReadyForReview).build();

        final Stream<Object> eventStreamWithReadyForReview = this.caseAggregate.updateCase(prosecutionCaseWithReadyForReview, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedReadyForReview);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedReadyForReviewAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithReadyForReview.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedReadyForReviewAfterAggregate, caseId, INACTIVE);
        assertThat(hearingResultedCaseUpdatedReadyForReviewAfterAggregate.getProsecutionCase().getDefendants().stream().allMatch(Defendant::getProceedingsConcluded), is(true));
    }

    @Test
    public void shouldUpdateCaseStatusFromInactiveToActiveOnHearingResultedWithNonFinalOffences() {

        final UUID caseId = randomUUID();
        final String caseURN = "case" + string(6).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final UUID hearingId = randomUUID();

        final List<Defendant> defendantsWithProceedingsConcluded = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);

        final ProsecutionCase prosecutionCaseWithNoCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .withId(randomUUID()).withDefendants(defendantsWithProceedingsConcluded).withId(caseId).build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        this.caseAggregate.createProsecutionCase(prosecutionCaseWithNoCaseStatus);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();

        // Case with Inactive Status
        final ProsecutionCase prosecutionCaseWithInactiveCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsWithProceedingsConcluded).withCaseStatus(INACTIVE.getDescription()).withId(caseId).build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedInactiveStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithInactiveCaseStatus).build();

        final Stream<Object> eventStreamWithInActiveCaseStatus = this.caseAggregate.updateCase(prosecutionCaseWithInactiveCaseStatus, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedInactiveStatus);

        final HearingResultedCaseUpdated hearingResultedCaseUpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCaseStatus.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate, caseId, INACTIVE);

        final List<Defendant> defendantsWithOffencesNotConcludedToFinal = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, false, true, true, 5);
        final ProsecutionCase prosecutionCaseAfterUpdate = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsWithOffencesNotConcludedToFinal).withId(caseId).withCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate.getProsecutionCase().getCaseStatus()).build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdate = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseAfterUpdate).build();

        final Stream<Object> eventStreamAfterUpdateAfterAggregate = this.caseAggregate.updateCase(prosecutionCaseAfterUpdate, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedAfterUpdate);

        // Case status to be back Active
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdateAfterAggregate = (HearingResultedCaseUpdated) eventStreamAfterUpdateAfterAggregate.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedAfterUpdateAfterAggregate, caseId, ACTIVE);
        assertThat(getProceedingsConcludedStatus(defendantId1, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(false));
        assertThat(getProceedingsConcludedStatus(defendantId2, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(true));
        assertThat(getProceedingsConcludedStatus(defendantId3, hearingResultedCaseUpdatedAfterUpdateAfterAggregate.getProsecutionCase()), is(true));
    }

    private boolean getProceedingsConcludedStatus(final UUID defendantId, final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants().stream().filter(defendant -> defendant.getId().equals(defendantId)).findFirst().get().getProceedingsConcluded();
    }

    @Test
    public void shouldKeepCaseStatusAsActiveUntilAllDefendantProceedingsIsConcludedToFinalAcrossMultipleHearings() {

        final UUID caseId1 = randomUUID();
        final String caseURN1 = "case" + string(6).next();

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final UUID masterDefendantId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final List<Defendant> defendantsWithProceedingsConcluded = getDefendantsWithProceedingsConcluded(caseId1, defendantId1, defendantId2,
                offenceId1, offenceId2, false, false);

        final ProsecutionCase prosecutionCase1 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendantsWithProceedingsConcluded).withId(caseId1).withCaseStatus(ACTIVE.getDescription()).build();
        this.caseAggregate.createProsecutionCase(prosecutionCase1);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final List<Defendant> defendant1withProceedingsConcludedTrue = getDefendantsWithMasterDefendantId(caseId1, defendantId1, masterDefendantId,
                offenceId1, true);
        final ProsecutionCase prosecutionCaseForHearing1WithDefendant1 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendant1withProceedingsConcludedTrue).withId(caseId1).withCaseStatus(ACTIVE.getDescription()).build();
        final HearingResultedCaseUpdated hearing1WithDefendant1Only = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseForHearing1WithDefendant1).build();
        final Stream<Object> eventStreamWithActiveCaseStatusFromHearing1 = this.caseAggregate.updateCase(hearing1WithDefendant1Only.getProsecutionCase(), defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());

        caseAggregate.apply(hearing1WithDefendant1Only);
        final HearingResultedCaseUpdated hearing1ResultedCaseUpdateAfterUpdateCase = (HearingResultedCaseUpdated) eventStreamWithActiveCaseStatusFromHearing1.collect(toList()).get(0);
        assertCaseStatus(hearing1ResultedCaseUpdateAfterUpdateCase, caseId1, ACTIVE);

        final List<Defendant> defendant2WithProceedingsConcludedTrue = getDefendantsWithMasterDefendantId(caseId1, defendantId2, masterDefendantId,
                offenceId2, true);
        final ProsecutionCase prosecutionCaseForHearing2WithDefendant2 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendant2WithProceedingsConcludedTrue).withId(caseId1).withCaseStatus(INACTIVE.getDescription()).build();
        final HearingResultedCaseUpdated hearing2WithDefendant2Only = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseForHearing2WithDefendant2).build();
        final Stream<Object> eventStreamWithInactiveCaseStatusFromHearing2 = this.caseAggregate.updateCase(hearing2WithDefendant2Only.getProsecutionCase(), defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());

        caseAggregate.apply(hearing2WithDefendant2Only);
        final HearingResultedCaseUpdated hearingResultedCase2UpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInactiveCaseStatusFromHearing2.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCase2UpdateInActiveAfterAggregate, caseId1, INACTIVE);
    }

    @Test
    public void shouldSetCaseStatusOnResultingAnHearingWhenNoCaseStatusWasSet() {

        final UUID caseId = randomUUID();
        final String caseURN = "case" + string(6).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID hearingId = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendantsWithProceedingsConcluded = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);

        final ProsecutionCase prosecutionCaseWithNoCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .withDefendants(defendantsWithProceedingsConcluded).withId(caseId).build();

        this.caseAggregate.createProsecutionCase(prosecutionCaseWithNoCaseStatus);

        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult);
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedWithNoCaseStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithNoCaseStatus).build();

        final Stream<Object> eventStreamWithInActiveCaseStatus = this.caseAggregate.updateCase(prosecutionCaseWithNoCaseStatus, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedWithNoCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCaseUpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCaseStatus.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate, caseId, INACTIVE);

        final List<Defendant> defendantsAfterUpdate = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, false, true, true, 5);

        final ProsecutionCase prosecutionCaseAfterUpdate = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(caseURN).build())
                .withDefendants(defendantsAfterUpdate).withId(caseId).withCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate.getProsecutionCase().getCaseStatus()).build();
        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdate = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseAfterUpdate).build();

        final Stream<Object> eventStreamAfterUpdateAfterAggregate = this.caseAggregate.updateCase(prosecutionCaseAfterUpdate, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedAfterUpdate);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedAfterUpdateAfterAggregate = (HearingResultedCaseUpdated) eventStreamAfterUpdateAfterAggregate.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdatedAfterUpdateAfterAggregate, caseId, ACTIVE);
    }

    @Test
    public void shouldChangeCaseStatusToInactiveWhenCaseDefendantsAllOffencesConcludedToFinal() {
        final UUID caseId = randomUUID();
        final String caseURN1 = "case" + string(6).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final List<Defendant> defendantsWithProceedingsConcluded = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, true, true, true, 5);

        final ProsecutionCase prosecutionCaseWithNoCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendantsWithProceedingsConcluded).withId(caseId).build();

        this.caseAggregate.createProsecutionCase(prosecutionCaseWithNoCaseStatus);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedWithNoCaseStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithNoCaseStatus).build();

        final Stream<Object> eventStreamWithInActiveCaseStatus = this.caseAggregate.updateCase(prosecutionCaseWithNoCaseStatus, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedWithNoCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCaseUpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCaseStatus.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate, caseId, INACTIVE);
    }

    @Test
    public void shouldNotChangeCaseStatusToInactiveIfAllOffencesAreNotResultedToFinal() {
        final UUID caseId = randomUUID();
        final String caseURN1 = "case" + string(6).next();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();
        final List<Defendant> defendantsWithAOffenceNotConcludedToFinal = getDefendantsWithProceedingsConcluded(caseId, defendantId1, defendantId2, defendantId3,
                offenceId1, offenceId2, offenceId3, false, true, true, 5);


        final ProsecutionCase prosecutionCaseWithNoCaseStatus = prosecutionCase()
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendantsWithAOffenceNotConcludedToFinal).withId(caseId).withCaseStatus(ACTIVE.getDescription()).build();

        this.caseAggregate.createProsecutionCase(prosecutionCaseWithNoCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCaseUpdatedWithNoCaseStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCaseWithNoCaseStatus).build();

        final Stream<Object> eventStreamWithWithUpdatedCaseStatus = this.caseAggregate.updateCase(prosecutionCaseWithNoCaseStatus, emptyList(), courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCaseUpdatedWithNoCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCaseUpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithWithUpdatedCaseStatus.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCaseUpdateInActiveAfterAggregate, caseId, ACTIVE);
    }

    @Test
    public void shouldChangeCaseStatusToInactiveIfAllOffencesOnTheCaseAreResultedFinal() {
        final UUID caseId = randomUUID();
        final String caseURN1 = "case" + string(6).next();
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final List<Defendant> defendantWithProceedingsConcludedTrue = getDefendantsWithMasterDefendantId(caseId, defendantId, masterDefendantId, offenceId, true);

        final ProsecutionCase prosecutionCase2 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendantWithProceedingsConcludedTrue).withId(caseId).withCaseStatus(ACTIVE.getDescription()).build();
        this.caseAggregate.createProsecutionCase(prosecutionCase2);

        final HearingResultedCaseUpdated hearingResultedCase2UpdatedWithActiveCaseStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase2).build();

        final Stream<Object> eventStreamWithInActiveCase2Status = this.caseAggregate.updateCase(hearingResultedCase2UpdatedWithActiveCaseStatus.getProsecutionCase(), Collections.emptyList(), courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCase2UpdatedWithActiveCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCase2UpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCase2Status.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCase2UpdateInActiveAfterAggregate, caseId, INACTIVE);
    }


    @Test
    public void shouldNotChangeCaseStatusIfAllOffencesOnTheCaseAreNotResultedFinal() {
        final UUID caseId = randomUUID();
        final String caseURN1 = "case" + string(6).next();
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final List<Defendant> defendant1WithProceedingsConcludedFalse = getDefendantsWithMasterDefendantId(caseId, defendantId, masterDefendantId, offenceId, false);

        final ProsecutionCase prosecutionCase1 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendant1WithProceedingsConcludedFalse).withId(caseId).withCaseStatus(ACTIVE.getDescription()).build();
        this.caseAggregate.createProsecutionCase(prosecutionCase1);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final HearingResultedCaseUpdated hearingResultedCase1UpdatedWithActiveCaseStatus = hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase1).build();

        final Stream<Object> eventStreamWithInActiveCase1Status = this.caseAggregate.updateCase(hearingResultedCase1UpdatedWithActiveCaseStatus.getProsecutionCase(), defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCase1UpdatedWithActiveCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCase1UpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCase1Status.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCase1UpdateInActiveAfterAggregate, caseId, ACTIVE);
    }

    @Test
    public void shouldUpdateCaseStatusToInactiveOnlyToACaseForWhichProceedingsAreConcludedInCaseOfRelatedHearing() {
        final String caseURN1 = "case" + string(6).next();
        final UUID caseId1 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID offenceId = randomUUID();
        final List<uk.gov.justice.core.courts.Defendant> defendants2withProceedingsConcludedFalse = getDefendantsWithMasterDefendantId(caseId1, defendantId1, masterDefendantId, offenceId, false);

        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final ProsecutionCase prosecutionCase1 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendants2withProceedingsConcludedFalse)
                .withGroupId(randomUUID()).withIsCivil(true).withId(caseId1)
                .withCaseStatus(ACTIVE.getDescription())
                .build();
        this.caseAggregate.createProsecutionCase(prosecutionCase1);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final HearingResultedCaseUpdated hearingResultedCase1UpdatedWithActiveCaseStatus = HearingResultedCaseUpdated.hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase1).build();

        final Stream<Object> eventStreamWithInActiveCase1Status = this.caseAggregate.updateCase(hearingResultedCase1UpdatedWithActiveCaseStatus.getProsecutionCase(), defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCase1UpdatedWithActiveCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCase1UpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCase1Status.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCase1UpdateInActiveAfterAggregate, caseId1, ACTIVE);

        final UUID caseId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final List<uk.gov.justice.core.courts.Defendant> defendants2withProceedingsConcludedTrue = getDefendantsWithMasterDefendantId(caseId2, defendantId2, masterDefendantId, offenceId2, true);

        final ProsecutionCase prosecutionCase2 = prosecutionCase().withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN1))
                .withDefendants(defendants2withProceedingsConcludedTrue).withGroupId(randomUUID()).withIsCivil(true).withId(caseId2).withCaseStatus(ACTIVE.getDescription()).build();
        this.caseAggregate.createProsecutionCase(prosecutionCase2);

        final HearingResultedCaseUpdated hearingResultedCase2UpdatedWithActiveCaseStatus = HearingResultedCaseUpdated.hearingResultedCaseUpdated().withProsecutionCase(prosecutionCase2).build();

        final Stream<Object> eventStreamWithInActiveCase2Status = this.caseAggregate.updateCase(hearingResultedCase2UpdatedWithActiveCaseStatus.getProsecutionCase(), defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList());
        caseAggregate.apply(hearingResultedCase2UpdatedWithActiveCaseStatus);

        final HearingResultedCaseUpdated hearingResultedCase2UpdateInActiveAfterAggregate = (HearingResultedCaseUpdated) eventStreamWithInActiveCase2Status.collect(toList()).get(0);
        assertCaseStatus(hearingResultedCase2UpdateInActiveAfterAggregate, caseId2, INACTIVE);
    }

    @Test
    public void shouldAddNewCaseDocumentReceivedEvent() {
        final UUID caseId = randomUUID();
        final NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent =
                new NewCaseDocumentReceivedEvent(caseId, "fileId", "fileMimeType", "fileName");

        final Object response = this.caseAggregate.apply(newCaseDocumentReceivedEvent);

        assertThat(((NewCaseDocumentReceivedEvent) response).getCppCaseId(), is(caseId));

    }

    @Test
    public void shouldApplyPreSentenceReportForDefendantsRequested() {
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final PreSentenceReportForDefendantsRequested preSentenceReportForDefendantsRequested =
                new PreSentenceReportForDefendantsRequested(caseId, asList(new DefendantPSR(defendantId, Boolean.TRUE)));

        final Object response = this.caseAggregate.apply(preSentenceReportForDefendantsRequested);

        assertThat(((PreSentenceReportForDefendantsRequested) response).getCaseId(), is(caseId));
        assertThat(((PreSentenceReportForDefendantsRequested) response).getDefendants().get(0).getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldApplySendingCommittalHearingInformationAdded() {
        final LocalDate localDate = LocalDate.now();
        final String courtCentre = "birmingham";
        final UUID caseId = randomUUID();
        final SendingCommittalHearingInformationAdded sendingCommittalHearingInformationAdded =
                new SendingCommittalHearingInformationAdded(caseId, courtCentre, localDate, randomUUID().toString());

        final Object response = this.caseAggregate.apply(sendingCommittalHearingInformationAdded);

        assertThat(((SendingCommittalHearingInformationAdded) response).getCaseId(), is(caseId));
        assertThat(((SendingCommittalHearingInformationAdded) response).getSendingCommittalDate(), is(localDate));
    }

    @Test
    public void shouldApplyCompleteSendingSheet() {
        final List<Object> objects = applySendingSheet(a -> {
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetCompleted.class));
        assertSendingSheetCompletedValues((SendingSheetCompleted) obj);
    }

    @Test
    public void shouldInvalidateSendingSheetWrongCourtCentre() {
        final List<Object> objects = applySendingSheet(a -> {
            Whitebox.setInternalState(this.caseAggregate, "courtCentreId", null);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertTrue(sendingSheetInvalidated.getDescription().contains(CC_COURT_CENTRE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetNoDefendants() {
        final List<Object> objects = applySendingSheet(a -> {
            Whitebox.setInternalState(this.caseAggregate, "defendants", new HashSet<>());
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongDefendants() {
        final List<Object> objects = applySendingSheet(a -> {
            final uk.gov.moj.cpp.progression.domain.event.Defendant defendant = new uk.gov.moj.cpp.progression.domain.event.Defendant();
            defendant.setId(UUID.randomUUID());
            Whitebox.setInternalState(this.caseAggregate, "defendants", new HashSet<>(asList(defendant)));
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
    }

    @Test
    public void shouldInvalidateSendingSheetWrongOffences() {
        final List<Object> objects = applySendingSheet(a -> {
            final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
            Whitebox.setInternalState(this.caseAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetInvalidated.class));
        final SendingSheetInvalidated sendingSheetInvalidated = (SendingSheetInvalidated) obj;
        assertEquals(sendingSheetInvalidated.getCaseId(), fromString(CASE_ID));
        assertTrue(sendingSheetInvalidated.getDescription().contains(OFFENCE_ID));

    }

    @Test
    public void shouldHandleConvictionDateAdded() {

        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.builder().withCaseId(caseId)
                .withOffenceId(offenceId).withConvictionDate(convictionDate).build();

        final Object response = this.caseAggregate.apply(convictionDateAdded);

        assertThat(((ConvictionDateAdded) response).getCaseId(), is(caseId));
        assertThat(((ConvictionDateAdded) response).getOffenceId(), is(offenceId));
        assertThat(((ConvictionDateAdded) response).getConvictionDate(), is(convictionDate));
    }

    @Test
    public void shouldHandleConvictionDateRemoved() {

        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.builder().withCaseId(caseId)
                .withOffenceId(offenceId).build();

        final Object response = this.caseAggregate.apply(convictionDateRemoved);

        assertThat(((ConvictionDateRemoved) response).getCaseId(), is(caseId));
        assertThat(((ConvictionDateRemoved) response).getOffenceId(), is(offenceId));
    }

    private List<Object> applySendingSheet(final Consumer<CaseAggregate> adjustInternals) {
        createCompleteSendingSheetEnvelope();
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        final Hearing hearing = new Hearing();
        hearing.setCaseId(UUID.fromString("4daefec6-5f77-4109-82d9-1e60544a6c05"));
        sendingSheetCompleted.setHearing(hearing);
        final Set<uk.gov.moj.cpp.progression.domain.event.Defendant> defendants = new HashSet<>();
        final uk.gov.moj.cpp.progression.domain.event.Defendant defendant = new uk.gov.moj.cpp.progression.domain.event.Defendant();
        defendants.add(defendant);
        defendant.setId(UUID.fromString(DEFENDANT_ID));
        final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
        offenceIdsByDefendantId.put(UUID.fromString(DEFENDANT_ID), new HashSet(asList(UUID.fromString(OFFENCE_ID))));
        //green path internals
        Whitebox.setInternalState(this.caseAggregate, "courtCentreId", CC_COURT_CENTRE_ID);
        Whitebox.setInternalState(this.caseAggregate, "defendants", defendants);
        Whitebox.setInternalState(this.caseAggregate, "offenceIdsByDefendantId", offenceIdsByDefendantId);
        adjustInternals.accept(this.caseAggregate);

        final Stream<Object> stream = this.caseAggregate.completeSendingSheet(this.envelope);
        return stream.collect(Collectors.toList());

    }

    @Test
    public void shouldApplyCompleteSendingSheetPreviouslyCompleted() {
        final List<Object> objects = applySendingSheet(a -> {
            final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>(asList(UUID.fromString(CASE_ID)));
            Whitebox.setInternalState(this.caseAggregate, "caseIdsWithCompletedSendingSheet", caseIdsWithCompletedSendingSheet);
        });
        assertThat(objects.size(), is(1));
        final Object obj = objects.get(0);
        assertThat(obj, instanceOf(SendingSheetPreviouslyCompleted.class));
        assertThat(CASE_ID, equalTo(((SendingSheetPreviouslyCompleted) obj).getCaseId().toString()));
    }

    private void createDefendant(final UUID defendantId) {
        final UUID caseId = randomUUID();
        final uk.gov.moj.cpp.progression.domain.event.Defendant defendant = new uk.gov.moj.cpp.progression.domain.event.Defendant(defendantId);
        final Offence offence = new Offence(randomUUID(),
                randomUUID().toString(),
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now());
        final Person person = new Person(randomUUID(), "", "", "", LocalDate.now(), "", "", "", "", "", "", "", null);
        final DefendantAdded defendantAdded = new DefendantAdded(caseId, defendantId, person, "", asList(offence), "CaseUrn");
        this.caseAggregate.apply(defendantAdded);
    }

    private void createCompleteSendingSheetEnvelope() {
        when(this.envelope.payloadAsJsonObject()).thenReturn(this.jsonObj);
        when(this.jsonObj.getString(Mockito.eq("caseId"))).thenReturn(CASE_ID);
        when(this.jsonObj.getString(Mockito.eq("isKeyEvidence"))).thenReturn("true");
        when(this.jsonObj.getString(Mockito.eq("planDate"))).thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("sendingCommittalDate")))
                .thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("sentenceHearingDate")))
                .thenReturn(LocalDate.now().toString());
        when(this.jsonObj.getString(Mockito.eq("courtCentreId")))
                .thenReturn(COURT_CENTRE_ID);

        final UUID defendantId = randomUUID();
        when(this.jsonObj.getJsonArray(Mockito.eq("defendants"))).thenReturn(Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("id", defendantId.toString()).build())
                .build());


        when(this.jsonObj.getJsonObject("hearing")).thenReturn(Json.createObjectBuilder()
                .add("courtCentreName", COURT_CENTRE_NAME)
                .add("courtCentreId", COURT_CENTRE_ID).add("type", HEARING_TYPE)
                .add("sendingCommittalDate", SENDING_COMMITTAL_DATE).add("caseId", CASE_ID)
                .add("caseUrn", CASE_URN)
                .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("id", DEFENDANT_ID)
                        .add("personId", DEFENDANT_PERSON_ID)
                        .add("firstName", DEFENDANT_FIRST_NAME).add("lastName", DEFENDANT_LAST_NAME)
                        .add("nationality", DEFENDANT_NATIONALITY).add("gender", DEFENDANT_GENDER)
                        .add("address", Json.createObjectBuilder()
                                .add("address1", DEFENDANT_ADDRESS_1)
                                .add("address2", DEFENDANT_ADDRESS_2)
                                .add("address3", DEFENDANT_ADDRESS_3)
                                .add("address4", DEFENDANT_ADDRESS_4)
                                .add("postcode", DEFENDANT_POSTCODE).build())
                        .add("dateOfBirth", DEFENDANT_DATE_OF_BIRTH)
                        .add("bailStatus", BAIL_STATUS)
                        .add("custodyTimeLimitDate", CUSTODY_TIME_LIMIT_DATE)
                        .add("defenceOrganisation", DEFENCE_ORGANISATION)
                        .add("interpreter", Json.createObjectBuilder()
                                .add("needed", INTERPRETER_NEEDED)
                                .add("language", INTERPRETER_LANGUAGE).build())
                        .add("offences", Json.createArrayBuilder().add(Json
                                .createObjectBuilder()
                                .add("id", OFFENCE_ID)
                                .add("offenceCode", OFFENCE_CODE)
                                .add("indicatedPlea", Json.createObjectBuilder().add("id", INDICATED_PLEA_ID).add("value", INDICATED_PLEA_VALUE).add("allocationDecision", ALLOCATION_DECISION).build())
                                .add("section", SECTION)
                                .add("wording", WORDING)
                                .add("reason", REASON)
                                .add("description", DESCRIPTION)
                                .add("category", CATEGORY)
                                .add("startDate", START_DATE)
                                .add("endDate", END_DATE).build()))
                        .build()).build())
                .build());
        when(this.jsonObj.getJsonObject("crownCourtHearing"))
                .thenReturn(Json.createObjectBuilder().add("ccHearingDate", CC_HEARING_DATE)
                        .add("courtCentreName", CC_COURT_CENTRE_NAME).add("courtCentreId", CC_COURT_CENTRE_ID)
                        .build());
    }

    private void assertSendingSheetCompletedValues(final SendingSheetCompleted ssCompleted) {
        assertThat(CC_HEARING_DATE, equalTo(ssCompleted.getCrownCourtHearing().getCcHearingDate()));
        assertThat(CC_COURT_CENTRE_ID, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreId().toString()));
        assertThat(CC_COURT_CENTRE_NAME, equalTo(ssCompleted.getCrownCourtHearing().getCourtCentreName()));
        assertSendingSheetCompletedHearingValues(ssCompleted.getHearing());
    }

    private void assertSendingSheetCompletedHearingValues(final Hearing hearing) {
        assertThat(COURT_CENTRE_NAME, equalTo(hearing.getCourtCentreName()));
        assertThat(COURT_CENTRE_ID, equalTo(hearing.getCourtCentreId()));
        assertThat(HEARING_TYPE, equalTo(hearing.getType()));
        assertThat(SENDING_COMMITTAL_DATE, equalTo(hearing.getSendingCommittalDate()));
        assertThat(CASE_URN, equalTo(hearing.getCaseUrn()));
        assertThat(CASE_ID, equalTo(hearing.getCaseId().toString()));
        assertHearingDefendant(hearing.getDefendants().get(0));
    }

    private void assertHearingDefendant(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant defendant) {
        assertThat(DEFENDANT_ID, equalTo(defendant.getId().toString()));
        assertThat(DEFENDANT_PERSON_ID, equalTo(defendant.getPersonId().toString()));
        assertThat(DEFENDANT_FIRST_NAME, equalTo(defendant.getFirstName()));
        assertThat(DEFENDANT_LAST_NAME, equalTo(defendant.getLastName()));
        assertThat(DEFENDANT_ADDRESS_1, equalTo(defendant.getAddress().getAddress1()));
        assertThat(DEFENDANT_ADDRESS_2, equalTo(defendant.getAddress().getAddress2()));
        assertThat(DEFENDANT_ADDRESS_3, equalTo(defendant.getAddress().getAddress3()));
        assertThat(DEFENDANT_ADDRESS_4, equalTo(defendant.getAddress().getAddress4()));
        assertThat(DEFENDANT_POSTCODE, equalTo(defendant.getAddress().getPostcode()));
        assertThat(DEFENDANT_DATE_OF_BIRTH, equalTo(defendant.getDateOfBirth()));
        assertThat(BAIL_STATUS, equalTo(defendant.getBailStatus()));
        assertThat(CUSTODY_TIME_LIMIT_DATE, equalTo(defendant.getCustodyTimeLimitDate()));
        assertThat(DEFENCE_ORGANISATION, equalTo(defendant.getDefenceOrganisation()));
        assertThat(INTERPRETER_NEEDED, equalTo(defendant.getInterpreter().getNeeded()));
        assertThat(INTERPRETER_LANGUAGE, equalTo(defendant.getInterpreter().getLanguage()));
        assertDefendantOffence(defendant.getOffences().get(0));
    }

    private void assertDefendantOffence(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence) {
        assertThat(OFFENCE_ID, equalTo(offence.getId().toString()));
        assertThat(OFFENCE_CODE, equalTo(offence.getOffenceCode()));
        assertThat(INDICATED_PLEA_ID, equalTo(offence.getIndicatedPlea().getId().toString()));
        assertThat(INDICATED_PLEA_VALUE, equalTo(offence.getIndicatedPlea().getValue()));
        assertThat(ALLOCATION_DECISION, equalTo(offence.getIndicatedPlea().getAllocationDecision()));
        assertThat(SECTION, equalTo(offence.getSection()));
        assertThat(WORDING, equalTo(offence.getWording()));
        assertThat(REASON, equalTo(offence.getReason()));
        assertThat(DESCRIPTION, equalTo(offence.getDescription()));
        assertThat(CATEGORY, equalTo(offence.getCategory()));
        assertThat(START_DATE, equalTo(offence.getStartDate()));
        assertThat(END_DATE, equalTo(offence.getEndDate()));
    }

    @Test
    public void shouldReturnProsecutionCaseCreated() {

        final List<Object> eventStream = caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object, instanceOf(ProsecutionCaseCreated.class));
    }

    @Test
    public void shouldDefendantsNotAddedToCourtProceedings() {

        final DefendantsNotAddedToCourtProceedings defendantsNotAddedToCourtProceedings = DefendantsNotAddedToCourtProceedings
                .defendantsNotAddedToCourtProceedings()
                .withDefendants(new ArrayList<>())
                .withListHearingRequests(new ArrayList<>())
                .build();

        final List<Object> eventStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsNotAddedToCourtProceedings.getDefendants(),
                defendantsNotAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsNotAddedToCourtProceedings.class)));

        //Assert total defendants are empty
        assertThat(defendantsNotAddedToCourtProceedings.getDefendants().isEmpty(), is(true));
        //Assert total listHearingRequests are empty
        assertThat(((DefendantsNotAddedToCourtProceedings) object).getListHearingRequests().isEmpty(), is(true));
    }

    @Test
    public void shouldDefendantsAddedToCourtProceedings() {

        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final List<Object> eventStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));

        //Assert total defendants with count 3 including duplicates
        assertThat(defendantsAddedToCourtProceedings.getDefendants().size(), is(3));
        //Assert total defendants with count 2 excluded duplicates
        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().size(), is(2));
    }


    @Test
    public void shouldCreateProsecutionCaseAndUpdateMasterDefendantId() {

        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID masterDefendantId = randomUUID();
        ExactMatchedDefendantSearchResultStored exactMatchedDefendantSearchResultStored = ExactMatchedDefendantSearchResultStored.exactMatchedDefendantSearchResultStored()
                .withDefendantId(defendant.getId())
                .withCases(asList(Cases.cases()
                        .withCaseReference("REF")
                        .withProsecutionCaseId(randomUUID().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(masterDefendantId.toString())
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build(), Defendants.defendants()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(randomUUID().toString())
                                .build()))
                        .build()))
                .build();
        caseAggregate.apply(exactMatchedDefendantSearchResultStored);

        final List<Object> eventStream = caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        ;

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ProsecutionCaseCreated.class)));
        // Asser Master defendant Id is present from exact match stored
        assertThat(((ProsecutionCaseCreated) object).getProsecutionCase().getDefendants().stream().filter(x -> masterDefendantId.equals(x.getMasterDefendantId())).collect(toList()).size(), is(1));
    }

    @Test
    public void shouldAddDefendantsToCourtProceedingsAndUpdateMasterDefendantId() {

        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID masterDefendantId = randomUUID();
        ExactMatchedDefendantSearchResultStored exactMatchedDefendantSearchResultStored = ExactMatchedDefendantSearchResultStored.exactMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(asList(Cases.cases()
                        .withCaseReference("REF")
                        .withProsecutionCaseId(randomUUID().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(masterDefendantId.toString())
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build(), Defendants.defendants()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(randomUUID().toString())
                                .build()))
                        .build()))
                .build();
        caseAggregate.apply(exactMatchedDefendantSearchResultStored);

        final List<Object> eventStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));

        //Assert total defendants with count 3 including duplicates
        assertThat(defendantsAddedToCourtProceedings.getDefendants().size(), is(3));
        //Assert total defendants with count 2 excluded duplicates
        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().size(), is(2));

        // Asser Master defendant Id is present from exact match stored
        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(x -> masterDefendantId.equals(x.getMasterDefendantId())).collect(toList()).size(), is(1));
    }

    @Test
    public void shouldReturnCaseEjected() {
        final List<Object> eventStream = caseAggregate.ejectCase(randomUUID(), "Legal").collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseEjected.class)));
    }

    @Test
    public void shouldNotReturnCaseEjected() {
        Whitebox.setInternalState(this.caseAggregate, "caseStatus", "EJECTED");
        final List<Object> eventStream = caseAggregate.ejectCase(randomUUID(), "Legal").collect(toList());

        assertThat(eventStream.size(), is(0));
    }


    @Test
    public void shouldDefendantMasterDefendantIdUpdatedEvent_whenDefendantAddToCourtProceedings_expectMandatoryAttributes() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);
        final UUID masterDefendantId = defendantsAddedToCourtProceedings.getDefendants().get(0).getMasterDefendantId();
        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final List<Object> defendantStream = caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());
        caseAggregate.apply(defendantStream);
        final Stream<Object> eventStream = caseAggregate.updateMatchedDefendant(caseId, defendantId, masterDefendantId);
        final DefendantsMasterDefendantIdUpdated defendantsMasterDefendantIdUpdated = (DefendantsMasterDefendantIdUpdated) eventStream.collect(toList()).get(0);
        assertThat(defendantsMasterDefendantIdUpdated.getProsecutionCaseId(), is(caseId));
        assertThat(defendantsMasterDefendantIdUpdated.getDefendant().getId(), is(defendantId));
        assertThat(defendantsMasterDefendantIdUpdated.getDefendant().getOffences().size(), is(1));
        assertNotNull(defendantsMasterDefendantIdUpdated.getDefendant().getCourtProceedingsInitiated());


    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsGranted_expectGrantedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(), is(LegalAidStatusEnum.GRANTED.getDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));
    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsGranted_expectReportingRestrictionsNotRemoved() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());

        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(), is(LegalAidStatusEnum.GRANTED.getDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getReportingRestrictions().stream().findAny().get().getLabel(), is("Victim Criminal offence"));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getReportingRestrictions().stream().findAny().get().getLabel(), is("Victim Criminal offence"));
    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsRefused_expectRefusedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(),Optional.of(createJsonList())).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("FM", REFUSED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(LegalAidStatusEnum.REFUSED.getDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));
    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsWithDrawn_expectWithDrawnInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final UUID defendantId2 = randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedings(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().size(), is(1));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveStartDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(WITHDRAWN.getDescription()));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));

        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().size(), is(1));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(laaReference.getApplicationReference()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusId(), is(laaReference.getStatusId()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is(laaReference.getStatusCode()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveStartDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getEffectiveEndDate(), is(laaReference.getEffectiveEndDate()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is(laaReference.getStatusDescription()));
        assertThat(((OffencesForDefendantChanged) object2).getUpdatedOffences().get(0).getOffences().get(0).getDvlaOffenceCode(), is("BA76004"));
    }

    @Test
    public void shouldCheckYouthFlagForDefendantAndReportingRestrictionsForYouthForAllOffencesWhenDefendantAddedForCourtProceeding() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedingsWithPersonalAndOrganisationalDefendant(
                caseId, defendantId, defendantId2, offenceId);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final List<Object> eventStream =  caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(createJsonList())).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));


        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().size(), is(2));

        final Defendant defendant1 = ((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(defendant ->defendant.getId().equals(defendantId)).findFirst().get();

        assertThat(defendant1.getIsYouth(), is(true));

        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is (YOUTH_RESTRICTION));

        final Defendant defendant2 = ((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(defendant ->defendant.getId().equals(defendantId2)).findFirst().get();

        assertNull(defendant2.getIsYouth());

    }

    @Test
    public void shouldCheckYouthFlagForDefendantAndYouthAndSexualReportingRestrictionsForAllOffencesWhenDefendantAddedForCourtProceeding() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedingsWithPersonalAndOrganisationalDefendant(
                caseId, defendantId, defendantId2, offenceId);
        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, "offenceCode",
                SEXUAL_OFFENCE_RR_DESCRIPTION,
                "json/referencedataoffences.offences-list.json");

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final List<Object> eventStream =  caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(), Optional.of(referencedataOffencesJsonObject)).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));

        final Defendant defendant1 = ((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(defendant ->defendant.getId().equals(defendantId)).findFirst().get();

        assertThat(defendant1.getIsYouth(), is(true));

        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is (YOUTH_RESTRICTION));
        assertThat(defendant1.getOffences().get(0).getReportingRestrictions().get(1).getLabel(), is (SEXUAL_OFFENCE_RR_DESCRIPTION));


        final Defendant defendant2 = ((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(defendant ->defendant.getId().equals(defendantId2)).findFirst().get();

        assertNull(defendant2.getIsYouth());

        assertThat(defendant2.getOffences().get(0).getReportingRestrictions().get(0).getLabel(), is (SEXUAL_OFFENCE_RR_DESCRIPTION));





    }

    @Test
    public void shouldReturnYouthFlagAsFalseWhenDefendantDOBisMissingForDefendantAddedForCourtProceeding() {

        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = buildDefendantsAddedToCourtProceedingsWithoutDOB(caseId, defendantId, offenceId);
        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final List<Object> eventStream =  caseAggregate.defendantsAddedToCourtProceedings(defendantsAddedToCourtProceedings.getDefendants(),
                defendantsAddedToCourtProceedings.getListHearingRequests(),Optional.of(createJsonList())).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(DefendantsAddedToCourtProceedings.class)));


        assertThat(((DefendantsAddedToCourtProceedings) object).getDefendants().size(), is(1));

        final Defendant defendant = ((DefendantsAddedToCourtProceedings) object).getDefendants().stream().filter(def ->def.getId().equals(defendantId)).findFirst().get();

        assertThat(defendant.getIsYouth(), is(false));



    }

    @Test
    public void shouldLAAReferenceUpdatedForOffence_whenOneOfTheOffenceIsRefusedAndOtherOneIsGranted_expectGrantedInDefendantLevelLegalAidStatus() {
        final UUID caseId = fromString(CASE_ID);
        final String caseURN = "case" + string(6).next();
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(asList(defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withOffences(asList(offence().withId(offenceId1).build()
                                , offence().withId(offenceId2).build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("FM", REFUSED.getDescription());
        caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1);

        final LaaReference laaReference2 = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId2, laaReference2).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(LegalAidStatusEnum.GRANTED.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

    }

   @Test
   public void shouldLAAReferenceUpdatedForOffenceWithIndicatedPleaRetained() {
        final UUID caseId = fromString(CASE_ID);
       final String caseURN = "case" + string(6).next();
       final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();

       final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
               .withOriginatingOrganisation("originatingOrganisation")
               .withDefendants(asList(defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                       .withOffences(asList(offence()
                                        .withId(offenceId1)
                                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                                .withOffenceId(offenceId1)
                                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                                .withSource(uk.gov.justice.core.courts.Source.IN_COURT)
                                                .build())
                                        .build()
                                , offence().withId(offenceId2).build()))
                       .build()))
               .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);
        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("FM", REFUSED.getDescription());
        caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1);
        final LaaReference laaReference2 = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId2, laaReference2).collect(toList());
        assertThat(eventStream.size(), is(3));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
       assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
               is(LegalAidStatusEnum.GRANTED.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

       assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getIndicatedPlea().getOffenceId(),
                is(offenceId1));
       assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getIndicatedPlea().getIndicatedPleaValue(),
                is(IndicatedPleaValue.INDICATED_GUILTY));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getOffences().get(0).getIndicatedPlea().getSource(),
                is(uk.gov.justice.core.courts.Source.IN_COURT));

    }

    @Test
    public void shouldDisassociateAssociatedDefenceOrganisation_whenLaaReferenceIsWithDrawnAndDefendantLevelStatusIsNotGranted() {
        final UUID caseId = fromString(CASE_ID);
        final String caseURN = "case" + string(6).next();
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();
        final UUID associateDefenceOrganisationId = randomUUID();

        final HashMap<UUID, UUID> map = new HashMap();
        map.put(defendantId, associateDefenceOrganisationId);

        ReflectionUtil.setField(caseAggregate, "defendantAssociatedDefenceOrganisation", map);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(singletonList(defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withAssociationLockedByRepOrder(true)
                        .withOffences(asList(offence().withId(offenceId1).build()
                                , offence().withId(offenceId2)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(WITHDRAWN.getDescription()).build())
                                        .build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1).collect(toList());
        assertThat(eventStream.size(), is(6));

        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(WITHDRAWN.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

        final Object object3 = eventStream.get(2);
        assertThat(object3.getClass(), is(equalTo(DefendantLegalaidStatusUpdated.class)));

        final Object object4 = eventStream.get(3);
        assertThat(object4.getClass(), is(equalTo(DefendantDefenceOrganisationChanged.class)));

        final Object object5 = eventStream.get(4);
        assertThat(object5.getClass(), is(equalTo(DefendantDefenceOrganisationDisassociated.class)));

    }


    @Test
    public void shouldNotDisassociateAssociatedDefenceOrganisation_whenLaaReferenceIsWithDrawnAndDefendantLevelStatusIsNotGranted() {
        final UUID caseId = fromString(CASE_ID);

        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId1 = fromString(OFFENCE_ID);
        final UUID offenceId2 = randomUUID();
        final UUID associateDefenceOrganisationId = randomUUID();
        final String caseURN = "case" + string(6).next();

        final HashMap<UUID, UUID> map = new HashMap();
        map.put(defendantId, associateDefenceOrganisationId);

        ReflectionUtil.setField(caseAggregate, "defendantAssociatedDefenceOrganisation", map);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(singletonList(defendant().
                        withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withAssociationLockedByRepOrder(true)
                        .withOffences(asList(offence().withId(offenceId1)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(REFUSED.getDescription()).build())
                                        .build()
                                , offence().withId(offenceId2)
                                        .withLaaApplnReference(laaReference().withStatusCode("WD").withOffenceLevelStatus(REFUSED.getDescription()).build())
                                        .build()))
                        .build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(getProsecutionCaseIdentifier(caseURN))
                .build();
        caseAggregate.createProsecutionCase(prosecutionCase);

        final LaaReference laaReference1 = generateRecordLAAReferenceForOffence("WD", WITHDRAWN.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId1, laaReference1).collect(toList());
        assertThat(eventStream.size(), is(6));

        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(ProsecutionCaseOffencesUpdated.class)));
        assertThat(((ProsecutionCaseOffencesUpdated) object1).getDefendantCaseOffences().getLegalAidStatus(),
                is(REFUSED.getDescription()));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(OffencesForDefendantChanged.class)));
        assertThat(((OffencesForDefendantChanged) object2).getAddedOffences(), is(nullValue()));
        assertThat(((OffencesForDefendantChanged) object2).getDeletedOffences(), is(nullValue()));

        final Object object3 = eventStream.get(2);
        assertThat(object3.getClass(), is(equalTo(DefendantLegalaidStatusUpdated.class)));
    }

    @Test
    public void shouldNotLAAReferenceUpdatedForOffence() {
        final UUID caseId = fromString(CASE_ID);
        final UUID defendantId = fromString(DEFENDANT_ID);
        final UUID offenceId = fromString(OFFENCE_ID);
        final LaaReference laaReference = generateRecordLAAReferenceForOffence("G2", GRANTED.getDescription());
        final List<Object> eventStream = caseAggregate.recordLAAReferenceForOffence(caseId, defendantId, offenceId, laaReference).collect(toList());
        assertThat(eventStream.size(), is(0));

    }


    @Test
    public void shouldLinkCaseToHearing() {
        final List<Object> eventStream = caseAggregate.linkProsecutionCaseToHearing(randomUUID(), randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseLinkedToHearing.class)));
    }

    @Test
    public void shouldAddCaseNote() {
        final List<Object> eventStream = caseAggregate.addNote(randomUUID(), randomUUID(), "This is a Note", false, "Bob", "Marley").collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CaseNoteAddedV2.class)));
    }

    @Test
    public void shouldHandleCaseNoteAdded() {
        final UUID caseId = randomUUID();
        final CaseNoteAdded caseNoteAdded = CaseNoteAdded.caseNoteAdded()
                .withCaseId(caseId)
                .withNote("Note")
                .withFirstName("Russell")
                .withLastName("Crow")
                .withCreatedDateTime(ZonedDateTime.now())
                .build();

        final Object response = this.caseAggregate.apply(caseNoteAdded);

        assertThat(response, is(caseNoteAdded));
    }

    @Test
    public void shouldUpdateCaseStatus() {
        final List<Object> eventStream = caseAggregate.updateCaseStatus(prosecutionCase().build(), SJP_REFERRAL.getDescription()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingConfirmedCaseStatusUpdated.class)));
    }

    @Test
    public void shouldUpdateCaseStatusToInactiveWhenAllOffencesAreFinalised() {
        final UUID caseId = randomUUID();

        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withId(UUID.randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(FINAL)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withCpsOrganisation("A01")
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated);

        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult);
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(4));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(INACTIVE.getDescription()));
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCpsOrganisation(), is("A01"));

    }

    @Test
    public void shouldGenerateHearingEventLogDocumentWhenCaseIsInactiveWhenAllOffencesAreFinalised() {
        final UUID caseId = randomUUID();

        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withId(UUID.randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(FINAL)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withCpsOrganisation("A01")
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();
        this.caseAggregate.apply(prosecutionCaseCreated);

        final DefendantJudicialResult defendantJudicialResult = defendantJudicialResult()
                .withJudicialResult(JudicialResult.judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())
                .withMasterDefendantId(UUID.randomUUID())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = singletonList(defendantJudicialResult);
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(4));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(INACTIVE.getDescription()));
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCpsOrganisation(), is("A01"));
        final Object object1 = eventStream.get(3);
        assertThat(object1.getClass(), is(equalTo(HearingEventLogsDocumentCreated.class)));
    }

    @Test
    public void shouldNotChangeCaseStatusIfAllTheOffencesAreNotResultedFinal() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object eventOne = eventStream.get(0);
        assertThat(eventOne.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(SJP_REFERRAL.getDescription()));

        final Object eventTwo = eventStream.get(1);
        assertThat(eventTwo.getClass(), is(equalTo(CaseRetentionPolicyRecorded.class)));
        final CaseRetentionPolicyRecorded caseRetentionPolicyRecorded = (CaseRetentionPolicyRecorded) eventStream.get(1);
        assertThat(caseRetentionPolicyRecorded.getHearingId(), is(hearingId));
        assertThat(caseRetentionPolicyRecorded.getHearingType(), is(hearingType));
        assertThat(caseRetentionPolicyRecorded.getCourtCentreId(), is(courtCentre.getId()));
        assertThat(caseRetentionPolicyRecorded.getCourtCentreName(), is(courtCentre.getName()));
        assertThat(caseRetentionPolicyRecorded.getCourtRoomName(), is(courtCentre.getRoomName()));
        assertThat(caseRetentionPolicyRecorded.getCourtRoomId(), is(courtCentre.getRoomId()));
        assertThat(caseRetentionPolicyRecorded.getJurisdictionType(), is(CROWN.name()));
        assertThat(caseRetentionPolicyRecorded.getPolicyType(), is("NON_CUSTODIAL"));
        assertThat(caseRetentionPolicyRecorded.getPeriod(), is("7Y0M0D"));
    }

    @Test
    public void shouldGenereateHearingEventLogForActiveCase() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<Object> eventStream = caseAggregate.getHearingEventLogsDocuments(caseId, Optional.empty()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object eventOne = eventStream.get(0);
        assertThat(eventOne.getClass(), is(equalTo(HearingEventLogsDocumentCreated.class)));
    }

    @Test
    public void shouldGenereateHearingEventLogForApplication() {
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<Object> eventStream = caseAggregate.getHearingEventLogsDocuments(caseId, Optional.of(applicationId)).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object eventOne = eventStream.get(0);
        assertThat(eventOne.getClass(), is(equalTo(HearingEventLogsDocumentCreated.class)));
    }

    @Test
    public void shouldNotCalculateAndRecordCaseRetentionWhenJurisdictionIsNotCrown() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).withCode("code").build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final Boolean isBoxHearing = Boolean.FALSE;
        final JurisdictionType jurisdictionType = MAGISTRATES;
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, jurisdictionType,
                isBoxHearing, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object eventOne = eventStream.get(0);
        assertThat(eventOne.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
    }

    @Test
    public void shouldNotCalculateAndRecordCaseRetentionWhenBoxHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withOffences(
                        singletonList(offence()
                                .withId(randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).withCode("code").build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final Boolean isBoxHearing = Boolean.TRUE;
        final List<Object> eventStream = caseAggregate.updateCase(prosecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, isBoxHearing, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object eventOne = eventStream.get(0);
        assertThat(eventOne.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
    }

    @Test
    public void shouldChangeCaseStatusToInactiveWhenAllOffencesResultedFinal() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withId(UUID.randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(FINAL)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
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

        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();

        final List<Object> eventStream = this.caseAggregate.updateCase(prosecutionCase, asList(defendantJudicialResult), courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(4));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(INACTIVE.getDescription()));
    }

    @Test
    public void shouldUpdateOffenceListOnProsecutionCaseOffencesUpdated() {
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final UUID newOffenceId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withOffences(
                        Arrays.asList(uk.gov.justice.core.courts.Offence.offence()
                                .withId(offenceId1)
                                .withListingNumber(1)
                                .withProceedingsConcluded(true)
                                .build()))
                .build();


        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withOffences(
                        singletonList(uk.gov.justice.core.courts.Offence.offence()
                                .withId(offenceId2)
                                .withListingNumber(1)
                                .withProceedingsConcluded(true)
                                .build()))
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(Arrays.asList(defendant1, defendant2))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(1).getOffences().size(), is(1));

        ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated = ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(DefendantCaseOffences.defendantCaseOffences()
                        .withDefendantId(defendantId1)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence()
                                .withId(newOffenceId)
                                .withProceedingsConcluded(false)
                                .build()))
                        .build())
                .build();

        this.caseAggregate.apply(prosecutionCaseOffencesUpdated);

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(1).getOffences().size(), is(1));

        ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated2 = ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(DefendantCaseOffences.defendantCaseOffences()
                        .withDefendantId(defendantId1)
                        .withProsecutionCaseId(caseId)
                        .withOffences(singletonList(offence()
                                .withId(newOffenceId)
                                .withProceedingsConcluded(false)
                                .build()))
                        .build())
                .build();

        this.caseAggregate.apply(prosecutionCaseOffencesUpdated2);

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(1).getOffences().size(), is(1));
    }

    @Test
    public void shouldKeepOriginalListingNumbersWhenProsecutionCaseUpdated() {
        final UUID caseId = randomUUID();
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(false)
                .withOffences(
                        singletonList(uk.gov.justice.core.courts.Offence.offence()
                                .withId(randomUUID())
                                .withListingNumber(2)
                                .withProceedingsConcluded(false)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();
        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(ACTIVE.getDescription())
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();

        final ProsecutionCaseCreated prosecutionCaseCreated = ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(def -> Defendant.defendant().withValuesFrom(def)
                                .withOffences(def.getOffences().stream()
                                        .map(off -> uk.gov.justice.core.courts.Offence.offence().withValuesFrom(off)
                                                .withListingNumber(0)
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCase(updatedProsecutionCase, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(ACTIVE.getDescription()));
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getDefendants().get(0).getOffences().get(0).getListingNumber(), is(2));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().get(0).getListingNumber(), is(2));
    }

    @Test
    public void shouldUpdateCaseStatusAsReadyForReviewWhenOneApplicationIsResultedAndAnotherApplicationIsNotResulted() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(false)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withProceedingsConcluded(false)
                                .withId(UUID.randomUUID())
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(INTERMEDIARY)
                                                .build())).build()))
                .build();

        final Defendant defendant1 = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(
                        singletonList(offence()
                                .withId(UUID.randomUUID())
                                .withProceedingsConcluded(true)
                                .withJudicialResults(
                                        singletonList(JudicialResult.judicialResult()
                                                .withCategory(FINAL)
                                                .build())).build()))
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(asList(defendant1, defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        // Resulted and Shared only one defendant
        final ProsecutionCase prosecutionCaseUpdate = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(READY_FOR_REVIEW.getDescription())
                .withDefendants(asList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();
        final List<DefendantJudicialResult> defendantJudicialResults = new ArrayList<>();
        final CourtCentre courtCentre = courtCentre()
                .withId(randomUUID())
                .withCode("code")
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCase(prosecutionCaseUpdate, defendantJudicialResults, courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(READY_FOR_REVIEW.getDescription()));

        final Object eventTwo = eventStream.get(1);
        assertThat(eventTwo.getClass(), is(equalTo(CaseRetentionPolicyRecorded.class)));
        final CaseRetentionPolicyRecorded caseRetentionPolicyRecorded = (CaseRetentionPolicyRecorded) eventStream.get(1);
        assertThat(caseRetentionPolicyRecorded.getCaseURN(), is("URN"));
        assertThat(caseRetentionPolicyRecorded.getHearingId(), is(hearingId));
        assertThat(caseRetentionPolicyRecorded.getPolicyType(), is("NON_CUSTODIAL"));
        assertThat(caseRetentionPolicyRecorded.getPeriod(), is("7Y0M0D"));

    }

    @Test
    public void shouldCalculateCaseRetentionPolicyAndRaiseCaseRetentionLengthCalculatedEventWhenCaseInActive() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final Defendant defendant = defendant()
                .withId(randomUUID())
                .withProceedingsConcluded(true)
                .withProsecutionCaseId(caseId)
                .withOffences(singletonList(offence()
                        .withId(randomUUID()).withListingNumber(2)
                        .withProceedingsConcluded(true)
                        .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                .withCategory(FINAL).build()))
                        .build()))
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

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(defendant))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .build();

        final ProsecutionCaseCreated prosecutionCaseCreated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();
        final CourtCentre courtCentre = courtCentre().withId(randomUUID()).withName("Court Name").withCode("code")
                .withRoomId(randomUUID()).withRoomName("roomName").build();

        this.caseAggregate.apply(prosecutionCaseCreated);

        final ProsecutionCase updatedProsecutionCase = prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(def -> defendant().withValuesFrom(def)
                                .withOffences(def.getOffences().stream()
                                        .map(off -> offence().withValuesFrom(off)
                                                .withListingNumber(0)
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCase(updatedProsecutionCase, singletonList(defendantJudicialResult),
                courtCentre, hearingId, hearingType, CROWN, Boolean.FALSE, emptyList()).collect(toList());

        assertThat(eventStream.size(), is(4));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedCaseUpdated.class)));
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = (HearingResultedCaseUpdated) eventStream.get(0);
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(INACTIVE.getDescription()));

        final Object eventTwo = eventStream.get(1);
        assertThat(eventTwo.getClass(), is(equalTo(CaseRetentionPolicyRecorded.class)));
        final CaseRetentionPolicyRecorded caseRetentionPolicyRecorded = (CaseRetentionPolicyRecorded) eventStream.get(1);
        assertThat(caseRetentionPolicyRecorded.getCaseURN(), is("URN"));
        assertThat(caseRetentionPolicyRecorded.getHearingId(), is(hearingId));
        assertThat(caseRetentionPolicyRecorded.getHearingType(), is(hearingType));
        assertThat(caseRetentionPolicyRecorded.getCourtCentreId(), is(courtCentre.getId()));
        assertThat(caseRetentionPolicyRecorded.getCourtCentreName(), is(courtCentre.getName()));
        assertThat(caseRetentionPolicyRecorded.getCourtRoomName(), is(courtCentre.getRoomName()));
        assertThat(caseRetentionPolicyRecorded.getCourtRoomId(), is(courtCentre.getRoomId()));
        assertThat(caseRetentionPolicyRecorded.getJurisdictionType(), is(CROWN.name()));
        assertThat(caseRetentionPolicyRecorded.getPolicyType(), is("NON_CUSTODIAL"));
        assertThat(caseRetentionPolicyRecorded.getPeriod(), is("7Y0M0D"));

        final CaseRetentionLengthCalculated caseRetentionLengthCalculated = (CaseRetentionLengthCalculated) eventStream.get(2);
        assertThat(caseRetentionLengthCalculated.getCaseURN(), is("URN"));
        assertThat(caseRetentionLengthCalculated.getCaseStatus(), is(INACTIVE.getDescription()));
        assertThat(caseRetentionLengthCalculated.getCourtCentreId(), is(courtCentre.getId()));
        assertThat(caseRetentionLengthCalculated.getCourtCentreName(), is(courtCentre.getName()));
        assertThat(caseRetentionLengthCalculated.getCourtRoomName(), is(courtCentre.getRoomName()));
        assertThat(caseRetentionLengthCalculated.getCourtRoomId(), is(courtCentre.getRoomId()));
        assertThat(caseRetentionLengthCalculated.getJurisdictionType(), is(CROWN.name()));
        assertThat(caseRetentionLengthCalculated.getRetentionPolicy().getPolicyType(), is("2"));
        assertThat(caseRetentionLengthCalculated.getRetentionPolicy().getPeriod(), is("7Y0M0D"));
        assertThat(caseRetentionLengthCalculated.getHearingType(), is(hearingType));
    }

    @Test
    public void shouldEditCaseNote() {
        final List<Object> eventStream = caseAggregate.editNote(randomUUID(), randomUUID(), false).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(CaseNoteEditedV2.class));
    }

    @Test
    public void shouldMarkHearingsDuplicate() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<UUID> defendantIds = asList(randomUUID(), randomUUID());

        final List<Object> eventStream = caseAggregate.markHearingAsDuplicate(hearingId, caseId, defendantIds).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase = (HearingMarkedAsDuplicateForCase) eventStream.get(0);
        assertThat(hearingMarkedAsDuplicateForCase.getHearingId(), is(hearingId));
        assertThat(hearingMarkedAsDuplicateForCase.getCaseId(), is(caseId));
        assertThat(hearingMarkedAsDuplicateForCase.getDefendantIds(), is(defendantIds));

    }
    @Test
    public void shouldMarkHearingsDuplicateAndShouldNotNoLongerBeLatestHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<UUID> defendantIds = asList(randomUUID(), randomUUID());

        caseAggregate.linkProsecutionCaseToHearing(hearingId, caseId);
        final List<Object> eventStream = caseAggregate.markHearingAsDuplicate(hearingId, caseId, defendantIds).collect(toList());


        assertThat(eventStream.size(), is(1));
        final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase = (HearingMarkedAsDuplicateForCase) eventStream.get(0);
        assertThat(caseAggregate.getLatestHearingId(), is(nullValue()));


    }

    @Test
    public void shouldSkipMasterDefendantIdUpdateV2UponDeletedHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .build();

        final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase = HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .withDefendantIds(asList(defendantId))
                .build();

        MatchDefendant matchDefendant = MatchDefendant.matchDefendant()
                .withDefendantId(defendantId)
                .withProsecutionCaseId(caseId)
                .withMatchedDefendants(asList(
                        MatchedDefendant.matchedDefendant()
                                .withDefendantId(defendantId)
                                .withProsecutionCaseId(caseId)
                                .withMasterDefendantId(defendantId)
                                .build(),
                        MatchedDefendant.matchedDefendant()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(defendantId)
                                .withProsecutionCaseId(caseId)
                                .withMasterDefendantId(defendantId)
                                .build()))
                .build();
        CaseAggregate caseAggregate = new CaseAggregate();

        // Linking Case and Hearing
        caseAggregate.apply(caseLinkedToHearing);

        // Marking Hearing a duplicate for case
        caseAggregate.apply(hearingMarkedAsDuplicateForCase);

        final List<Object> eventStream = caseAggregate.matchPartiallyMatchedDefendants(matchDefendant).collect(toList());
        assertThat(eventStream.size(), is(2));

        //Verifying there is just one stream and that is not stream of MasterDefendantIdUpdated or MasterDefendantIdUpdatedV2
        final DefendantMatched defendantMatched = (DefendantMatched) eventStream.get(0);
        assertThat(defendantMatched.getDefendantId(), is(defendantId));
        assertThat(defendantMatched.getHasDefendantAlreadyBeenDeleted(), is(false));

    }

    @Test
    public void shouldReapplyMiReportingRestrictions() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendants).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();

        this.caseAggregate.apply(prosecutionCaseUpdated);

        final List<Object> eventStream = caseAggregate.reApplyMiReportingRestrictions(caseId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final ReapplyMiReportingRestrictions reapplyMiReportingRestrictions = (ReapplyMiReportingRestrictions) eventStream.get(0);
        assertThat(reapplyMiReportingRestrictions.getCaseId(), is(caseId));
    }

    @Test
    public void shouldUpdateCpsProsecutorWhenCpsOrganisationIsValidAndOldCpsOrganisationIsNull() {
        handleCpsOrganisationCasesWith(null);
    }

    @Test
    public void shouldUpdateCpsProsecutorWhenCpsOrganisationIsValidAndOldCpsOrganisationIsBlank() {
        handleCpsOrganisationCasesWith("");
    }

    @Test
    public void shouldDeleteProsecutionCaseRelatedToHearing() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<Object> eventStream = caseAggregate.deleteHearingRelatedToProsecutionCase(hearingId, prosecutionCaseId).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingDeletedForProsecutionCase hearingDeletedForProsecutionCase = (HearingDeletedForProsecutionCase) eventStream.get(0);
        assertThat(hearingDeletedForProsecutionCase.getHearingId(), is(hearingId));
        assertThat(hearingDeletedForProsecutionCase.getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(hearingDeletedForProsecutionCase.getDefendantIds(), hasItems(defendant.getId()));
    }

    @Test
    public void shouldRemovedProsecutionCaseRelatedToHearing() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<Object> eventStream = caseAggregate.removeHearingRelatedToProsecutionCase(hearingId, prosecutionCaseId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingRemovedForProsecutionCase hearingDeletedForProsecutionCase = (HearingRemovedForProsecutionCase) eventStream.get(0);
        assertThat(hearingDeletedForProsecutionCase.getHearingId(), is(hearingId));
        assertThat(hearingDeletedForProsecutionCase.getProsecutionCaseId(), is(prosecutionCaseId));
    }

    @Test
    public void shouldGeneratePetFormCreatedWhenDefendantAndOffenceIsValid() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID submissionId = randomUUID();
        final String userName = "cps user name";

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.createPetForm(petId, caseId, formId, Optional.empty(), defendantOffenceIds, formData, userId, submissionId, userName, PET).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PetFormCreated.class)));
        assertThat(((PetFormCreated) object).getIsYouth(), is(false));

    }

    @Test
    public void shouldGeneratePetFormCreatedWhenDefendantAndOffenceIsValidForYouth() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final boolean isYouth = true;
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID submissionId = randomUUID();
        final String userName = "cps user name";

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.createPetForm(petId, caseId, formId, Optional.ofNullable(isYouth), defendantOffenceIds, formData, userId, submissionId, userName, PET).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(((PetFormCreated) object).getIsYouth(), is(true));
    }

    @Test
    public void shouldGeneratePetFormReceivedEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID petId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.receivePetForm(petId, caseId, formId, defendantOffenceIds).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PetFormReceived.class)));
    }

    @Test
    public void shouldGeneratePetFormUpdated() {
        final UUID caseId = randomUUID();
        final String formData = "test data";
        final UUID offenceId = randomUUID();
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID formId = randomUUID();
        final UUID defendantId = randomUUID();

        final PetFormCreated petFormCreated = PetFormCreated.petFormCreated()
                .withCaseId(caseId)
                .withPetId(petId)
                .withIsYouth(true)
                .withFormId(formId)
                .withPetDefendants(asList(petDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormType(PET)
                .withPetFormData(formData)
                .build();

        final Object object = caseAggregate.apply(petFormCreated);
        assertThat(object, instanceOf(PetFormCreated.class));
        final PetFormCreated form = (PetFormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(PET, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getPetFormData()));

        final List<Object> eventStream = caseAggregate.updatePetForm(caseId, formData, petId, userId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object result = eventStream.get(0);
        assertThat(result.getClass(), is(equalTo(PetFormUpdated.class)));
    }

    @Test
    public void shouldGeneratePetFormFinalised() {
        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID caseId = randomUUID();
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        PetDefendants petDefendants = petDefendants().withDefendantId(defendantId).build();
        formMap.put(petId, new Form(asList(petDefendants), petId, PET, new FormLockStatus(), submissionId));
        setField(caseAggregate, "formMap", formMap);
        setField(caseAggregate, "prosecutionCase", prosecutionCase()
                .withDefendants(asList(defendant()
                                .withId(randomUUID())
                                .build(),
                        defendant()
                                .withId(randomUUID())
                                .build()))
                .build());

        final List<Object> eventStream = caseAggregate.finalisePetForm(caseId, petId, userId, asList("{}", "{}", "{}")).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PetFormFinalised.class)));
        final PetFormFinalised petFormFinalised = (PetFormFinalised) object;
        assertThat(petFormFinalised.getProsecutionCase().getDefendants().size(), is(2));
        assertThat(petFormFinalised.getSubmissionId(), notNullValue());
    }

    @Test
    public void shouldNotGeneratePetFormFinalised() {

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID caseId = randomUUID();
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID submissionId = randomUUID();

        PetDefendants petDefendants = petDefendants().withDefendantId(defendantId).build();
        formMap.put(petId, new Form(asList(petDefendants), petId, PET, new FormLockStatus(), submissionId));
        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.finalisePetForm(caseId, petId, userId, asList("{}", "{}", "{}")).collect(toList());
        assertThat(eventStream.size(), is(1));
        final PetOperationFailed petOperationFailed = (PetOperationFailed) eventStream.get(0);
        assertThat(petOperationFailed.getClass(), is(equalTo(PetOperationFailed.class)));
        assertThat(petOperationFailed.getSubmissionId(), is(submissionId));
    }

    @Test
    public void shouldGeneratePetDetailUpdated() {
        final UUID caseId = randomUUID();
        final String formData = "test data";
        final UUID offenceId = randomUUID();
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();


        final List<Object> eventStream = caseAggregate.updatePetDetail(caseId, petId, Optional.empty(), null, userId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PetDetailUpdated.class)));
    }

    @Test
    public void shouldGeneratePetDetailReceived() {
        final UUID caseId = randomUUID();
        final String formData = "test data";
        final UUID offenceId = randomUUID();
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final List<Object> eventStream = caseAggregate.receivePetDetail(caseId, petId, null, userId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(PetDetailReceived.class)));
    }

    @Test
    public void shouldGeneratePetFormDefendantUpdatedEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String defendantData = "{}";
        final UUID petId = randomUUID();
        final UUID userId = randomUUID();
        final UUID formId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, randomUUID()));

        final Map<UUID, List<UUID>> petIdOffenceIdsMap = new HashMap<>();
        petIdOffenceIdsMap.put(petId, asList(offenceId));

        final PetFormCreated petFormCreated = PetFormCreated.petFormCreated()
                .withCaseId(caseId)
                .withPetId(petId)
                .withIsYouth(true)
                .withFormId(formId)
                .withPetDefendants(asList(petDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormType(PET)
                .withPetFormData(defendantData)
                .build();

        final Object object = caseAggregate.apply(petFormCreated);
        assertThat(object, instanceOf(PetFormCreated.class));
        final PetFormCreated form = (PetFormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(PET, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(defendantData, is(form.getPetFormData()));

        final List<Object> eventStream = caseAggregate.updatePetFormForDefendant(petId, caseId, defendantId, defendantData, userId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object result = eventStream.get(0);
        assertThat(result.getClass(), is(equalTo(PetFormDefendantUpdated.class)));
    }

    private ProsecutionCase createProsecutionCase(final UUID defendantId, final UUID offenceId) {
        return prosecutionCase()
                .withDefendants(asList(defendant()
                        .withId(defendantId)
                        .withOffences(asList(offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

    @Test
    public void shouldRaiseDefendantsAndListingHearingRequestsStoredEvent() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();

        final List<Object> eventStream = caseAggregate.addOrStoreDefendantsAndListingHearingRequests(asList(defendant), asList(listHearingRequest)).collect(toList());

        assertThat(eventStream.size(), is(1));

        final DefendantsAndListingHearingRequestsStored defendantsAndListingHearingRequestsStored = (DefendantsAndListingHearingRequestsStored) eventStream.get(0);

        assertThat(defendantsAndListingHearingRequestsStored.getDefendants(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsStored.getDefendants().get(0).getId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsStored.getDefendants().get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(defendantsAndListingHearingRequestsStored.getListHearingRequests(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsStored.getListHearingRequests().get(0).getListDefendantRequests().get(0).getDefendantId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsStored.getListHearingRequests().get(0).getListDefendantRequests().get(0).getProsecutionCaseId(), is(prosecutionCaseId));

    }

    @Test
    public void shouldRaiseDefendantsAndListingHearingRequestsAddedEvent() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        caseAggregate.apply(new ProsecutionCaseCreatedInHearing(prosecutionCaseId));

        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();

        final List<Object> eventStream = caseAggregate.addOrStoreDefendantsAndListingHearingRequests(asList(defendant), asList(listHearingRequest)).collect(toList());

        assertThat(eventStream.size(), is(1));

        final DefendantsAndListingHearingRequestsAdded defendantsAndListingHearingRequestsAdded = (DefendantsAndListingHearingRequestsAdded) eventStream.get(0);

        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants().get(0).getId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants().get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests().get(0).getListDefendantRequests().get(0).getDefendantId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests().get(0).getListDefendantRequests().get(0).getProsecutionCaseId(), is(prosecutionCaseId));

    }

    @Test
    public void shouldRaiseOnlyProsecutionCaseCreatedInHearingEventWhenDefendantsAreNotStored() {

        final UUID prosecutionCaseId = randomUUID();

        final List<Object> eventStream = caseAggregate.createProsecutionCaseInHearing(prosecutionCaseId).collect(toList());

        assertThat(eventStream.size(), is(1));

        final ProsecutionCaseCreatedInHearing prosecutionCaseCreatedInHearing = (ProsecutionCaseCreatedInHearing) eventStream.get(0);

        assertThat(prosecutionCaseCreatedInHearing.getProsecutionCaseId(), is(prosecutionCaseId));

    }

    @Test
    public void shouldRaiseProsecutionCaseCreatedInHearingEventAndDefendantsAndListingHearingRequestsAddedWhenDefendantsAreStored() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();

        caseAggregate.apply(new DefendantsAndListingHearingRequestsStored(asList(defendant), asList(listHearingRequest)));

        final List<Object> eventStream = caseAggregate.createProsecutionCaseInHearing(prosecutionCaseId).collect(toList());

        assertThat(eventStream.size(), is(2));

        final ProsecutionCaseCreatedInHearing prosecutionCaseCreatedInHearing = (ProsecutionCaseCreatedInHearing) eventStream.get(0);
        assertThat(prosecutionCaseCreatedInHearing.getProsecutionCaseId(), is(prosecutionCaseId));

        final DefendantsAndListingHearingRequestsAdded defendantsAndListingHearingRequestsAdded = (DefendantsAndListingHearingRequestsAdded) eventStream.get(1);

        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants().get(0).getId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants().get(0).getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests().get(0).getListDefendantRequests().get(0).getDefendantId(), is(defendantId));
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests().get(0).getListDefendantRequests().get(0).getProsecutionCaseId(), is(prosecutionCaseId));

    }

    @Test
    public void shouldNotRaiseDefendantsAndListingHearingRequestsAddedAfterStoredDefendantsAndListHearingRequestsAreCleared() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();

        // Store defendants and list hearing requests
        caseAggregate.apply(new DefendantsAndListingHearingRequestsStored(asList(defendant), asList(listHearingRequest)));

        final List<Object> eventStream = caseAggregate.createProsecutionCaseInHearing(prosecutionCaseId).collect(toList());

        // Should have two events (ProsecutionCaseCreatedInHearing, DefendantsAndListingHearingRequestsAdded)
        assertThat(eventStream.size(), is(2));

        final ProsecutionCaseCreatedInHearing prosecutionCaseCreatedInHearing = (ProsecutionCaseCreatedInHearing) eventStream.get(0);
        assertThat(prosecutionCaseCreatedInHearing.getProsecutionCaseId(), is(prosecutionCaseId));

        final DefendantsAndListingHearingRequestsAdded defendantsAndListingHearingRequestsAdded = (DefendantsAndListingHearingRequestsAdded) eventStream.get(1);
        assertThat(defendantsAndListingHearingRequestsAdded.getDefendants(), notNullValue());
        assertThat(defendantsAndListingHearingRequestsAdded.getListHearingRequests(), notNullValue());

        // Calling same command should after clearing stored defendants and list hearing requests
        final List<Object> events = caseAggregate.createProsecutionCaseInHearing(prosecutionCaseId).collect(toList());

        // Should have only one event ProsecutionCaseCreatedInHearing and should not raise event DefendantsAndListingHearingRequestsAdded.
        assertThat(events.size(), is(1));

        final ProsecutionCaseCreatedInHearing prosecutionCaseCreatedInHearingEvent = (ProsecutionCaseCreatedInHearing) events.get(0);
        assertThat(prosecutionCaseCreatedInHearingEvent.getProsecutionCaseId(), is(prosecutionCaseId));

    }


    @Test
    public void shouldHandleDefendantPartialMatchCreatedForPersonDefendant() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId();

        final String defendantName = RandomGenerator.STRING.next();
        final PartialMatchedDefendantSearchResultStored partialMatchedDefendantSearchResultStored = PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(asList(Cases.cases()
                        .withProsecutionCaseId(prosecutionCase.getId().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(defendantId.toString())
                                .withMasterDefendantId(defendantId.toString())
                                .withFirstName(defendantName)
                                .withLastName(defendantName)
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build()))
                        .withCaseReference("REF")
                        .withProsecutionCaseId("caseId")
                        .build()))
                .build();

        this.caseAggregate.apply(partialMatchedDefendantSearchResultStored); //populate partialMatchedDefendants map so we can compare in the nextstep

        final List<Object> eventStream = this.caseAggregate.storeMatchedDefendants(prosecutionCase.getId()).collect(toList());

        assertThat(eventStream.size(), is(1));

        final DefendantPartialMatchCreated defendantPartialMatchCreated = (DefendantPartialMatchCreated) eventStream.get(0);

        assertThat(defendantPartialMatchCreated.getProsecutionCaseId(), is(prosecutionCase.getId()));
        assertThat(defendantPartialMatchCreated.getDefendantId(), is(defendantId));

    }

    @Test
    public void shouldNotIncludeInvalidDefendantDefendantPartialMatchCreated() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID defendantId = prosecutionCase.getDefendants().get(0).getId();
        final UUID defendantId2 = randomUUID();;

        final String defendantName = RandomGenerator.STRING.next();
        final PartialMatchedDefendantSearchResultStored partialMatchedDefendantSearchResultStored = PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(asList(Cases.cases()
                        .withProsecutionCaseId(prosecutionCase.getId().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(defendantId.toString())
                                .withMasterDefendantId(defendantId.toString())
                                .withFirstName(defendantName)
                                .withLastName(defendantName)
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build(), Defendants.defendants()
                                .withDefendantId(defendantId2.toString())
                                .withMasterDefendantId(defendantId2.toString())
                                .build()))
                        .withCaseReference("REF")
                        .withProsecutionCaseId("caseId")
                        .build()))
                .build();

        this.caseAggregate.apply(partialMatchedDefendantSearchResultStored); //populate partialMatchedDefendants map so we can compare in the nextstep

        final List<Object> eventStream = this.caseAggregate.storeMatchedDefendants(prosecutionCase.getId()).collect(toList());

        assertThat(eventStream.size(), is(1));

        final DefendantPartialMatchCreated defendantPartialMatchCreated = (DefendantPartialMatchCreated) eventStream.get(0);

        assertThat(defendantPartialMatchCreated.getProsecutionCaseId(), is(prosecutionCase.getId()));
        assertThat(defendantPartialMatchCreated.getDefendantId(), is(defendantId));
        assertFalse(defendantPartialMatchCreated.getPayload().contains(defendantId2.toString()));
    }

    @Test
    public void shouldNotRaiseDefendantPartialMatchCreatedForLegalEntityDefendant() {

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID defendantId = prosecutionCaseWithLegalEntity.getDefendants().get(0).getId();

        final String defendantName = RandomGenerator.STRING.next();
        final PartialMatchedDefendantSearchResultStored partialMatchedDefendantSearchResultStored = PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(asList(Cases.cases()
                        .withProsecutionCaseId(prosecutionCase.getId().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(defendantId.toString())
                                .withMasterDefendantId(defendantId.toString())
                                .withFirstName(defendantName)
                                .withLastName(defendantName)
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .build()))
                        .withCaseReference("REF")
                        .withProsecutionCaseId("caseId")
                        .build()))
                .build();

        this.caseAggregate.apply(partialMatchedDefendantSearchResultStored); //populate partialMatchedDefendants map so we can compare in the nextstep

        final List<Object> eventStream = this.caseAggregate.storeMatchedDefendants(prosecutionCase.getId()).collect(toList());

        assertThat(eventStream.size(), is(0));

    }


    @Test
    public void shouldGenerateFormCreated_WhenDefendantAndOffenceIsValid() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();
        final UUID submissionId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.createForm(courtFormId, caseId, formId, defendantOffenceIds, formData, userId, BCM, submissionId, null).collect(toList());

        assertThat(eventStream, hasSize(1));
        assertFormCreatedFromEventStream(caseId, courtFormId, formId, formData, BCM, eventStream);
    }


    @Test
    public void shouldGenerateFormCreatedForMultipleForms_WhenDefendantAndOffenceIsValid() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();
        final UUID submissionId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.createForm(courtFormId, caseId, formId, defendantOffenceIds, formData, userId, BCM, submissionId, null).collect(toList());

        assertThat(eventStream, hasSize(1));
        assertFormCreatedFromEventStream(caseId, courtFormId, formId, formData, BCM, eventStream);

        final UUID defendantId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData2 = "test data2";
        final UUID courtFormId2 = randomUUID();
        final UUID userId2 = randomUUID();
        final UUID submissionId2 = randomUUID();

        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId2);

        final List<Object> eventStream2 = caseAggregate.createForm(courtFormId2, caseId, formId2, defendantOffenceIds2, formData2, userId2, BCM, submissionId2, null).collect(toList());

        assertThat(eventStream2, hasSize(1));
        assertFormCreatedFromEventStream(caseId, courtFormId2, formId2, formData2, BCM, eventStream2);
        assertThat(caseAggregate.getFormMap().size(), is(2));
        assertThat(caseAggregate.getFormMap().get(courtFormId), notNullValue());
        assertThat(caseAggregate.getFormMap().get(courtFormId2), notNullValue());
    }


    @Test
    public void shouldGenerateFormCreatedForMultipleForms_WithDifferentFormType_EvenWhenDefendantIsDuplicate() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();
        final UUID submissionid = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);

        final List<Object> eventStream = caseAggregate.createForm(courtFormId, caseId, formId, defendantOffenceIds, formData, userId, PTPH, submissionid, null).collect(toList());

        assertThat(eventStream, hasSize(1));
        assertFormCreatedFromEventStream(caseId, courtFormId, formId, formData, PTPH, eventStream);

        final UUID courtFormId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData2 = "test data2";
        final UUID userId2 = randomUUID();
        final UUID submissionId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId);

        final List<Object> eventStream2 = caseAggregate.createForm(courtFormId2, caseId, formId2, defendantOffenceIds2, formData2, userId2, BCM, submissionId2, null).collect(toList());
        assertThat(eventStream2, hasSize(1));
        assertFormCreatedFromEventStream(caseId, courtFormId2, formId2, formData2, BCM, eventStream2);
        assertThat(caseAggregate.getFormMap().size(), is(2));
        assertThat(caseAggregate.getFormMap().get(courtFormId), notNullValue());
        assertThat(caseAggregate.getFormMap().get(courtFormId2), notNullValue());
    }


    @Test
    public void shouldNotGenerateFormCreated_WhenFormIdPresentAlready() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final Map<UUID, List<UUID>> defendantOffenceIds = new HashMap<>();
        defendantOffenceIds.put(defendantId, asList(offenceId));
        final FormCreated formCreated = formCreated()
                .withFormType(BCM)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertFormCreatedFromObject(caseId, courtFormId, formId, formData, BCM, object);

        final UUID defendantId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData2 = "test data2";
        final UUID userId2 = randomUUID();
        final UUID submissionId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId2);

        List<Object> eventStream = caseAggregate.createForm(courtFormId, caseId, formId2, defendantOffenceIds2, formData2, userId2, BCM, submissionId2, null).collect(toList());
        assertThat(eventStream, hasSize(1));
        assertFormOperationFailedFromEventStream(caseId, courtFormId, FORM_CREATION_COMMAND_NAME, MESSAGE_FOR_DUPLICATE_COURT_FORM_ID, BCM, eventStream);
    }


    @Test
    public void shouldNotGenerateFormCreated_WhenFormIdPresentAlready_EvenFormTypeIsDifferent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final Map<UUID, List<UUID>> defendantOffenceIds = new HashMap<>();
        defendantOffenceIds.put(defendantId, asList(offenceId));
        final FormCreated formCreated = formCreated()
                .withFormType(PTPH)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertFormCreatedFromObject(caseId, courtFormId, formId, formData, PTPH, object);

        final UUID defendantId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData2 = "test data2";
        final UUID userId2 = randomUUID();
        final UUID submissionId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId2);

        final List<Object> eventStream = caseAggregate.createForm(courtFormId, caseId, formId2, defendantOffenceIds2, formData2, userId2, BCM, submissionId2, null).collect(toList());
        assertThat(eventStream, hasSize(1));
        assertFormOperationFailedFromEventStream(caseId, courtFormId, FORM_CREATION_COMMAND_NAME, MESSAGE_FOR_DUPLICATE_COURT_FORM_ID, BCM, eventStream);
    }


    @Test
    public void shouldGenerateFormCreated_WhenDefendantPresentAlreadyForBcmForm() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final List<UUID> defendantOffenceIds = new ArrayList<>();
        defendantOffenceIds.add(defendantId);
        final FormCreated formCreated = formCreated()
                .withFormType(BCM)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertFormCreatedFromObject(caseId, courtFormId, formId, formData, BCM, object);

        final UUID courtFormId2 = randomUUID();
        final UUID formId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData2 = "test data2";
        final UUID userId2 = randomUUID();
        final UUID submissionId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId);

        final List<Object> eventStream = caseAggregate.createForm(courtFormId2, caseId, formId2, defendantOffenceIds2, formData2, userId2, BCM, submissionId2, null).collect(toList());
        assertThat(eventStream, hasSize(1));
        assertFormCreatedFromObject(caseId, courtFormId2, formId2, formData2, BCM, eventStream.get(0));
    }

    @Test
    public void shouldGenerateFormFinalised() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();
        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withCaseStatus(SJP_REFERRAL.getDescription())
                .withDefendants(singletonList(defendant))
                .withCpsOrganisation("A01")
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(URN).build())
                .withDefendants(asList(defendant()
                                .withId(defendantId)
                                .withOffences(asList(offence()
                                        .withId(offenceId)
                                        .build()))
                                .build(),
                        defendant()
                                .withId(defendantId2)
                                .withOffences(asList(offence()
                                        .withId(offenceId2)
                                        .build()))
                                .build()))
                .build();

        setField(caseAggregate, "prosecutionCase", prosecutionCase);

        final List<Object> eventStream = caseAggregate.finaliseForm(caseId,
                courtFormId,
                userId,
                asList("{}", "{}", "{}"),
                hearingDateTime).collect(toList());
        assertFormFinalisedFromEventStream(caseId, courtFormId, userId, defendantId, offenceId, BCM, eventStream);
    }


    @Test
    public void shouldNotGenerateFormFinalised_WhenIdDoesNotExist_VerifyFormOperationFailedEvent() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();
        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        ProsecutionCase prosecutionCase = prosecutionCase()
                .withDefendants(asList(defendant()
                                .withId(defendantId)
                                .withOffences(asList(offence()
                                        .withId(offenceId)
                                        .build()))
                                .build(),
                        defendant()
                                .withId(defendantId2)
                                .withOffences(asList(offence()
                                        .withId(offenceId2)
                                        .build()))
                                .build()))
                .build();
        setField(caseAggregate, "prosecutionCase", prosecutionCase);

        final UUID courtFormId3 = randomUUID();
        final List<Object> eventStream = caseAggregate.finaliseForm(caseId, courtFormId3, userId, asList("{}", "{}", "{}"), hearingDateTime).collect(toList());
        assertFormOperationFailedFromEventStream(caseId, courtFormId3, FORM_FINALISATION_COMMAND_NAME,
                format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, courtFormId3), null, eventStream);
    }


    @Test
    public void shouldNotGenerateFormFinalised_WhenProsecutionCaseIsNull_VerifyFormOperationFailedEvent() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();
        final ZonedDateTime hearingDateTime = ZonedDateTime.now();
        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        ProsecutionCase prosecutionCase = null;
        setField(caseAggregate, "prosecutionCase", prosecutionCase);

        final List<Object> eventStream = caseAggregate.finaliseForm(caseId, courtFormId, userId, asList("{}", "{}", "{}"), hearingDateTime).collect(toList());
        assertFormOperationFailedFromEventStream(caseId, courtFormId, FORM_FINALISATION_COMMAND_NAME,
                format(MESSAGE_FOR_PROSECUTION_NULL, caseId), null, eventStream);
    }


    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsNotLockedAtAll() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);
    }

    @Test
    public void shouldGenerateEditFormRequested_WhenPetFormIsNotLockedAtAll() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID petId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(petId, new Form(asList(formDefendants), petId, PET, new FormLockStatus(false, null, null, null)));

        final UUID petId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(petId2, new Form(asList(formDefendants2), petId2, PET, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, petId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(petId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, petId, userId, userId, false, expiryTime, eventStream);
    }

    @Test
    public void shouldGenerateFormOperationFailed_WhenFormIsNotPresent() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final UUID courtFormId3 = randomUUID();
        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId3, userId, lockDurationMapByFormType, ZonedDateTime.now(), true, 5).collect(toList());
        assertFormOperationFailedFromEventStream(caseId, courtFormId3, FORM_EDIT_COMMAND_NAME,
                format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, courtFormId3), null, eventStream);

    }


    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInFuture() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, expiryTime, eventStream2);

    }


    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInPast() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));
        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime(), eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final UUID userId2 = randomUUID();
        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime nextExpiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, nextExpiryTime, eventStream3);

    }


    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInFuture_EditRequestSentBySameUser() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime(), eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final UUID userId2 = randomUUID();
        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        final ZonedDateTime nextExpiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, nextExpiryTime, eventStream3);
        assertThat(nextExpiryTime, notNullValue());
        assertThat(nextExpiryTime, greaterThanOrEqualTo(ZonedDateTime.now().minusMinutes(10L)));

        //same user is editing again so should get form unlocked for same user.
        final List<Object> eventStream4 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        final ZonedDateTime nextExpiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        final Object object = eventStream4.get(0);
        final EditFormRequested editFormRequested = (EditFormRequested) object;
        final LockStatus lockStatus = editFormRequested.getLockStatus();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, lockStatus.getExpiryTime(), eventStream4);
        assertThat(nextExpiryTime, is(nextExpiryTime2));
    }

    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInFuture_EditRequestSentBySameUser_WithExtendTime() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime(), eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final UUID userId2 = randomUUID();
        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        final ZonedDateTime nextExpiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, nextExpiryTime, eventStream3);
        assertThat(nextExpiryTime, notNullValue());
        assertThat(nextExpiryTime, greaterThanOrEqualTo(ZonedDateTime.now().minusMinutes(10L)));

        //same user is editing again so should get form unlocked for same user.
        final List<Object> eventStream4 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), true, 5).collect(toList());
        final ZonedDateTime nextExpiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        final Object object = eventStream4.get(0);
        final EditFormRequested editFormRequested = (EditFormRequested) object;
        final LockStatus lockStatus = editFormRequested.getLockStatus();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, lockStatus.getExpiryTime(), eventStream4);
        assertThat(ZonedDateTime.now().plusMinutes(6), greaterThan(nextExpiryTime2));
    }

    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInFuture_EditRequestSentBySameUser_WithDefaultExtendTime() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime(), eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final UUID userId2 = randomUUID();
        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        final ZonedDateTime nextExpiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, nextExpiryTime, eventStream3);
        assertThat(nextExpiryTime, notNullValue());
        assertThat(nextExpiryTime, greaterThanOrEqualTo(ZonedDateTime.now().minusMinutes(10L)));

        //same user is editing again so should get form unlocked for same user.
        final List<Object> eventStream4 = caseAggregate.requestEditForm(caseId, courtFormId, userId2, lockDurationMapByFormType, ZonedDateTime.now(), true, 0).collect(toList());
        final ZonedDateTime nextExpiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        final Object object = eventStream4.get(0);
        final EditFormRequested editFormRequested = (EditFormRequested) object;
        final LockStatus lockStatus = editFormRequested.getLockStatus();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId2, userId2, false, lockStatus.getExpiryTime(), eventStream4);
        assertThat(ZonedDateTime.now().plusMinutes(11), greaterThan(nextExpiryTime2));
    }

    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInPast_EditRequestSentBySameUser() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, expiryTime, eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        final ZonedDateTime expiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId1, userId1, false, expiryTime2, eventStream3);
        assertThat(expiryTime2, notNullValue());
        assertThat(expiryTime2, greaterThanOrEqualTo(ZonedDateTime.now().minusMinutes(10L)));
    }


    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInPast_EditRequestSentBySameUser_WithExtendTime() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, expiryTime, eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), true, 5).collect(toList());
        final ZonedDateTime expiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId1, userId1, false, expiryTime2, eventStream3);
        assertThat(expiryTime2, notNullValue());
        assertThat(ZonedDateTime.now().plusMinutes(6L), greaterThan(expiryTime2));
    }

    @Test
    public void shouldGenerateEditFormRequested_WhenFormIsLocked_WithExpiryTimeInPast_EditRequestSentBySameUser_WithDefaultExtendTime() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, expiryTime, eventStream2);

        caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().setLockExpiryTime(ZonedDateTime.now().minusHours(2));

        final List<Object> eventStream3 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), true, 0).collect(toList());
        final ZonedDateTime expiryTime2 = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId1, userId1, false, expiryTime2, eventStream3);
        assertThat(expiryTime2, notNullValue());
        assertThat(ZonedDateTime.now().plusMinutes(11L), greaterThan(expiryTime2));
    }

    @Test
    public void shouldUpdateFormMap_WhenFormDataUpdated() {
        final UUID caseId = randomUUID();
        final UUID userId = randomUUID();

        final Map<UUID, Form> formMap = new HashMap<>();
        final UUID courtFormId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        FormDefendants formDefendants = formDefendants().withDefendantId(defendantId).build();
        formMap.put(courtFormId, new Form(asList(formDefendants), courtFormId, BCM, new FormLockStatus(false, null, null, null)));

        final UUID courtFormId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        FormDefendants formDefendants2 = formDefendants().withDefendantId(defendantId2).build();
        formMap.put(courtFormId2, new Form(asList(formDefendants2), courtFormId2, BCM, new FormLockStatus(false, null, null, null)));

        setField(caseAggregate, "formMap", formMap);

        final List<Object> eventStream = caseAggregate.requestEditForm(caseId, courtFormId, userId, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(courtFormId).getFormLockStatus().getLockExpiryTime();
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId, false, expiryTime, eventStream);

        final UUID userId1 = randomUUID();
        final List<Object> eventStream2 = caseAggregate.requestEditForm(caseId, courtFormId, userId1, lockDurationMapByFormType, ZonedDateTime.now(), false, 0).collect(toList());
        assertEditFormRequestedFromEventStream(caseId, courtFormId, userId, userId1, true, expiryTime, eventStream2);

        //form updated by same userId which got the lock earlier
        final List<Object> eventStream3 = caseAggregate.updateForm(caseId, "{}", courtFormId, userId).collect(toList());
        final Object formUpdatedObject = eventStream3.get(0);
        assertThat(formUpdatedObject, instanceOf(FormUpdated.class));
        Form form = caseAggregate.getFormMap().get(courtFormId);
        assertThat(form.getFormLockStatus().getLockedBy(), nullValue());
        assertThat(form.getFormLockStatus().getLockExpiryTime(), nullValue());
        assertThat(form.getFormLockStatus().getLockRequestedBy(), nullValue());
        assertThat(form.getFormLockStatus().getLocked(), is(false));

    }


    @Test
    public void shouldUpdateDefendantDetails_WhenPersonDefendantIsNull_CustodialInformationIsAbsent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantDetails(payload, asList()).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant(), nullValue());

    }

    @Test
    public void shouldUpdateDefendantDetails_WhenPersonDefendantIsNonNull_CustodialInformationIsAbsent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantDetails(payload, asList()).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant(), notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant().getCustodialEstablishment(), nullValue());

    }

    @Test
    public void isDefendantAddressToBeChangedWhenAddressIsChanged() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                        uk.gov.justice.core.courts.Person.person().withAddress(Address.address().withAddress1("Address-defendantId1").build()).build()
                ).build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                        uk.gov.justice.core.courts.Person.person().withAddress(Address.address().withAddress1("Address-defendantId2").build()).build()
                ).build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withAddress(Address.address().withAddress1("Address-defendantId11").build()).build())
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantAddress(payload).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant(), notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant().getCustodialEstablishment(), nullValue());
    }

    @Test
    public void isDefendantAddressToBeChangedWhenAddressIsSame() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                        uk.gov.justice.core.courts.Person.person().withAddress(Address.address().withAddress1("Address-defendantId1").build()).build()
                ).build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                        uk.gov.justice.core.courts.Person.person().withAddress(Address.address().withAddress1("Address-defendantId2").build()).build()
                ).build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withAddress(Address.address().withAddress1("Address-defendantId1").build()).build())
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantAddress(payload).collect(toList());
        assertThat(eventList, hasSize(0));
    }

    @Test
    public void isDefendantAddressToBeChangedWhenLeAddressIsChanged() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withIsYouth(true)
                .withCroNumber("number")
                .withMasterDefendantId(masterDefendantId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address2")
                                        .build())
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address2")
                                        .build())
                                .build())
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantAddress(payload).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getLegalEntityDefendant(), notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getLegalEntityDefendant().getOrganisation().getAddress(), notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getLegalEntityDefendant().getOrganisation().getAddress().getAddress1(), is("Address2"));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getIsYouth(), is(true));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getCroNumber(), is("number"));
    }

    @Test
    public void isDefendantAddressToBeChangedWhenLeAddressIsSame() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withIsYouth(true)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address2")
                                        .build())
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .build())
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantAddress(payload).collect(toList());
        assertThat(eventList, hasSize(0));
    }
    @Test
    public void shouldUpdateDefendantDetails_WhenOriginalMasterDefendantIdIsNull_CustodialInformationIsAbsent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(randomUUID())
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantDetails(payload, asList()).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));

    }

    @Test
    public void shouldUpdateDefendantDetails_CustodialInformationIsAbsent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(randomUUID())
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final CustodialEstablishment custodialEstablishment = CustodialEstablishment.custodialEstablishment()
                .withName("name")
                .withId(custodialId)
                .withCustody("custody")
                .build();

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(custodialEstablishment)
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantDetails(payload, asList()).collect(toList());
        assertThat(eventList, hasSize(2));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));

        assertThat(eventList.get(1), Matchers.instanceOf(DefendantCustodialInformationUpdateRequested.class));
        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = (DefendantCustodialInformationUpdateRequested) eventList.get(1);
        assertThat(defendantCustodialInformationUpdateRequested.getDefendantId(), is(defendantId1));
        assertThat(defendantCustodialInformationUpdateRequested.getCustodialEstablishment().getCustody(), is(custodialEstablishment.getCustody()));
        assertThat(defendantCustodialInformationUpdateRequested.getCustodialEstablishment().getId(), is(custodialEstablishment.getId()));
        assertThat(defendantCustodialInformationUpdateRequested.getCustodialEstablishment().getName(), is(custodialEstablishment.getName()));
        assertThat(defendantCustodialInformationUpdateRequested.getMasterDefendantId(), is(masterDefendantId));
        assertThat(defendantCustodialInformationUpdateRequested.getProsecutionCaseId(), is(caseId));

    }

    @Test
    public void shouldUpdateDefendantDetails_WhenSameCustodialInformationIsPresent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final CustodialEstablishment custodialEstablishment = CustodialEstablishment.custodialEstablishment()
                .withName("name1")
                .withId(custodialId)
                .withCustody("custody1")
                .build();

        final DefendantUpdate payload = DefendantUpdate.defendantUpdate()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(custodialEstablishment)
                        .build())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantDetails(payload, asList()).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
    }


    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_CustodialInformationIsAbsentInViewStore() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody")
                        .withId(randomUUID())
                        .withName("name")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
    }

    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_EmptyCustodialInformationIsUpdated() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withCustody("custody")
                                .withId(randomUUID())
                                .withName("name")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withCustody("custody")
                                .withId(randomUUID())
                                .withName("name")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(1));
        assertThat(eventList.get(0), Matchers.instanceOf(ProsecutionCaseDefendantUpdated.class));
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = (uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated) eventList.get(0);
        assertThat(prosecutionCaseDefendantUpdated.getDefendant(), Matchers.notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getId(), is(defendantId1));
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant(), notNullValue());
        assertThat(prosecutionCaseDefendantUpdated.getDefendant().getPersonDefendant().getCustodialEstablishment(), nullValue());
    }

    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_WhenMatchingCustodialInformationIsPresent() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(0));
    }

    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_WhenEmptyCustodialInformationPresentOnBothSide() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(0));
    }

    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_WhenDefendantNotPresentInMap() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(0));
    }


    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_WhenMasterDefendantIdMismatching() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(randomUUID())
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(0));
    }

    @Test
    public void shouldUpdateDefendantCustodialInformationDetails_WhenDefendantProceedingIsConcluded() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);
        final Map<UUID, Boolean> defendantsConcludedMap = new HashMap<>();
        defendantsConcludedMap.put(defendantId1, true);
        defendantsConcludedMap.put(defendantId2, true);
        final Map<UUID, Map<UUID, Boolean>> defendantProceedingConcluded = new HashMap<>();
        defendantProceedingConcluded.put(caseId, defendantsConcludedMap);
        setField(caseAggregate, "defendantProceedingConcluded", defendantProceedingConcluded);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<Object> eventList = caseAggregate.updateDefendantCustodialInformationDetails(updateMatchedDefendantCustodialInformation).collect(toList());
        assertThat(eventList, hasSize(0));
    }

    @Test
    public void shouldRemoveDefendantCustodial_WhenDefendantProceedingIsConcluded() {
        final UUID caseId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);
        final Map<UUID, Boolean> defendantsConcludedMap = new HashMap<>();
        defendantsConcludedMap.put(defendantId1, true);
        defendantsConcludedMap.put(defendantId2, true);
        final Map<UUID, Map<UUID, Boolean>> defendantProceedingConcluded = new HashMap<>();
        defendantProceedingConcluded.put(caseId, defendantsConcludedMap);
        setField(caseAggregate, "defendantProceedingConcluded", defendantProceedingConcluded);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();
        final List<UUID> allHearingIdsForCase = new ArrayList<>();

        final List<Object> eventList = caseAggregate.removeDefendantCustodialEstablishment(masterDefendantId, defendantId1, caseId, allHearingIdsForCase).collect(toList());
        assertThat(eventList, hasSize(2));
        assertThat(eventList.get(0).getClass(), is(equalTo(DefendantCustodialEstablishmentRemoved.class)));
        assertThat(eventList.get(1).getClass(), is(equalTo(ProsecutionCaseDefendantUpdated.class)));

    }

    @Test
    public void shouldRemoveDefendantCustodial_WhenDefendantProceedingIsConcluded_AndHaveApplicationHearing() {
        final UUID caseId = randomUUID();
        final UUID caseHearingId = randomUUID();
        final UUID applicationHearingId = randomUUID();
        final Map<UUID, Defendant> defendantsMap = new HashMap<>();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID custodialId = randomUUID();

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendantId1)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name1")
                                .withId(custodialId)
                                .withCustody("custody1")
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = Defendant.defendant()
                .withId(defendantId2)
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withName("name2")
                                .withId(randomUUID())
                                .withCustody("custody2")
                                .build())
                        .build())
                .build();
        defendantsMap.put(defendantId1, defendant1);
        defendantsMap.put(defendantId2, defendant2);
        setField(caseAggregate, "defendantsMap", defendantsMap);
        final Map<UUID, Boolean> defendantsConcludedMap = new HashMap<>();
        defendantsConcludedMap.put(defendantId1, true);
        defendantsConcludedMap.put(defendantId2, true);
        final Map<UUID, Map<UUID, Boolean>> defendantProceedingConcluded = new HashMap<>();
        defendantProceedingConcluded.put(caseId, defendantsConcludedMap);
        setField(caseAggregate, "defendantProceedingConcluded", defendantProceedingConcluded);

        Set<UUID> hearingIds = new HashSet<>();
        hearingIds.add(caseHearingId);

        setField(caseAggregate, "hearingIds", hearingIds);

        final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation = updateMatchedDefendantCustodialInformation()
                .withCaseId(caseId)
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment.custodialEstablishment()
                        .withCustody("custody1")
                        .withId(custodialId)
                        .withName("name1")
                        .build())
                .withDefendants(Arrays.asList(defendantId1))
                .withMasterDefendantId(masterDefendantId)
                .build();

        final List<UUID> allHearingIdsForCase = asList(applicationHearingId);

        final List<Object> eventList = caseAggregate.removeDefendantCustodialEstablishment(masterDefendantId, defendantId1, caseId, allHearingIdsForCase).collect(toList());
        assertThat(eventList, hasSize(3));
        assertThat(eventList.get(0).getClass(), is(equalTo(DefendantCustodialEstablishmentRemoved.class)));
        assertThat(eventList.get(1).getClass(), is(equalTo(ApplicationDefendantUpdateRequested.class)));
        assertThat(eventList.get(2).getClass(), is(equalTo(ProsecutionCaseDefendantUpdated.class)));

    }

    private void assertEditFormRequestedFromEventStream(final UUID caseId, final UUID courtFormId, final UUID lockedBy, final UUID lockRequestedBy, final boolean isLocked, final ZonedDateTime expiryTime, final List<Object> eventStream) {
        final Object object = eventStream.get(0);
        assertThat(object, instanceOf(EditFormRequested.class));
        final EditFormRequested editFormRequested = (EditFormRequested) object;
        assertThat(caseId, is(editFormRequested.getCaseId()));
        assertThat(courtFormId, is(editFormRequested.getCourtFormId()));
        assertThat(editFormRequested.getLockStatus(), notNullValue());
        final LockStatus lockStatus = editFormRequested.getLockStatus();
        assertThat(lockStatus.getIsLocked(), is(isLocked));
        assertThat(lockStatus.getExpiryTime(), is(expiryTime));
        assertThat(lockStatus.getLockedBy(), is(lockedBy));
        assertThat(lockStatus.getLockRequestedBy(), is(lockRequestedBy));
    }


    private void assertFormFinalisedFromEventStream(final UUID caseId, final UUID courtFormId, final UUID userId, final UUID defendantId, final UUID offenceId, final FormType formType, final List<Object> eventStream) {
        final Object object = eventStream.get(0);
        assertThat(object, instanceOf(FormFinalised.class));
        final FormFinalised formFinalised = (FormFinalised) object;
        assertThat(caseId, is(formFinalised.getCaseId()));
        assertThat(courtFormId, is(formFinalised.getCourtFormId()));
        assertThat(userId, is(formFinalised.getUserId()));
        assertThat(formFinalised.getFormType(), is(formType));
    }


    private void assertFormOperationFailedFromEventStream(final UUID caseId, final UUID courtFormId, final String operation, final String message, final FormType formType, final List<Object> eventStream) {
        final Object object = eventStream.get(0);
        assertThat(object, instanceOf(FormOperationFailed.class));
        final FormOperationFailed form = (FormOperationFailed) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(form.getCourtFormId()));
        assertThat(formType, is(form.getFormType()));
        assertThat(operation, is(form.getOperation()));
        assertThat(message, is(form.getMessage()));
        if (nonNull(formType)) {
            assertThat(formType, is(form.getFormType()));
        }
    }

    @Test
    public void shouldReturnTwoDefendants_WhenAddingNewDefendantExistingDefendants_VerifyFormDefendantsUpdated() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final Map<UUID, List<UUID>> defendantOffenceIds = new HashMap<>();
        defendantOffenceIds.put(defendantId, asList(offenceId));
        final FormCreated formCreated = formCreated()
                .withFormType(BCM)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertThat(object, instanceOf(FormCreated.class));
        final FormCreated form = (FormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(BCM, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getFormData()));

        final UUID defendantId2 = randomUUID();
        final UUID userId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId);
        defendantOffenceIds2.add(defendantId2);

        final List<Object> eventStream = caseAggregate.updateFormDefendants(courtFormId, caseId, defendantOffenceIds2, userId2, BCM).collect(toList());
        assertThat(eventStream, hasSize(1));
        final Object result = eventStream.get(0);
        assertThat(result, instanceOf(FormDefendantsUpdated.class));
        final FormDefendantsUpdated formDefendantsUpdated = (FormDefendantsUpdated) result;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(formDefendantsUpdated.getCourtFormId()));
        assertThat(BCM, is(formDefendantsUpdated.getFormType()));

        final List<FormDefendants> formDefendants = caseAggregate.getFormMap().get(courtFormId).getFormDefendants();

        assertThat(formDefendants, hasSize(2));
        verifyDefendants(defendantId, asList(offenceId, offenceId2), formDefendants);
        verifyDefendants(defendantId2, asList(), formDefendants);
    }

    @Test
    public void shouldReturnThreeDefendants_WhenAddingNewDefendantUpdatingExistingOneDefendants_VerifyFormDefendantsUpdated1() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final Map<UUID, List<UUID>> defendantOffenceIds = new HashMap<>();
        defendantOffenceIds.put(defendantId, asList(offenceId));
        final FormCreated formCreated = formCreated()
                .withFormType(BCM)
                .withCaseId(caseId)
                .withFormDefendants(asList(
                        formDefendants()
                                .withDefendantId(defendantId)
                                .build(),
                        formDefendants()
                                .withDefendantId(defendantId2)
                                .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertThat(object, instanceOf(FormCreated.class));
        final FormCreated form = (FormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(BCM, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getFormData()));

        final UUID defendantId3 = randomUUID();
        final UUID userId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId);
        defendantOffenceIds2.add(defendantId2);
        defendantOffenceIds2.add(defendantId3);

        final List<Object> eventStream = caseAggregate.updateFormDefendants(courtFormId, caseId, defendantOffenceIds2, userId2, BCM).collect(toList());
        assertThat(eventStream, hasSize(1));
        final Object result = eventStream.get(0);
        assertThat(result, instanceOf(FormDefendantsUpdated.class));
        final FormDefendantsUpdated formDefendantsUpdated = (FormDefendantsUpdated) result;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(formDefendantsUpdated.getCourtFormId()));
        assertThat(BCM, is(formDefendantsUpdated.getFormType()));

        final List<FormDefendants> formDefendants = caseAggregate.getFormMap().get(courtFormId).getFormDefendants();

        assertThat(formDefendants, hasSize(3));
        verifyDefendants(defendantId, asList(offenceId), formDefendants);
        verifyDefendants(defendantId2, asList(offenceId2), formDefendants);
        verifyDefendants(defendantId3, asList(), formDefendants);

    }

    @Test
    public void shouldReturnTwoDefendants_WhenRemovingFirstDefendantFromExistingOneDefendants_VerifyFormDefendantsUpdated1() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID formId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String formData = "test data";
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();

        setField(caseAggregate, "prosecutionCase", createProsecutionCase(defendantId, offenceId));

        final Map<UUID, List<UUID>> defendantOffenceIds = new HashMap<>();
        defendantOffenceIds.put(defendantId, asList(offenceId));
        final FormCreated formCreated = formCreated()
                .withFormType(BCM)
                .withCaseId(caseId)
                .withFormDefendants(asList(
                        formDefendants()
                                .withDefendantId(defendantId)
                                .build(),
                        formDefendants()
                                .withDefendantId(defendantId2)
                                .build(),
                        formDefendants()
                                .withDefendantId(defendantId3)
                                .build()))
                .withFormId(formId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .build();

        final Object object = caseAggregate.apply(formCreated);
        assertThat(object, instanceOf(FormCreated.class));
        final FormCreated form = (FormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(BCM, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getFormData()));


        final UUID userId2 = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId2);
        defendantOffenceIds2.add(defendantId3);

        final List<Object> eventStream = caseAggregate.updateFormDefendants(courtFormId, caseId, defendantOffenceIds2, userId2, BCM).collect(toList());
        assertThat(eventStream, hasSize(1));
        final Object result = eventStream.get(0);
        assertThat(result, instanceOf(FormDefendantsUpdated.class));
        final FormDefendantsUpdated formDefendantsUpdated = (FormDefendantsUpdated) result;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(formDefendantsUpdated.getCourtFormId()));
        assertThat(BCM, is(formDefendantsUpdated.getFormType()));

        final List<FormDefendants> formDefendants = caseAggregate.getFormMap().get(courtFormId).getFormDefendants();

        assertThat(formDefendants, hasSize(2));
        verifyDefendants(defendantId2, asList(offenceId2), formDefendants);
        verifyDefendants(defendantId3, asList(), formDefendants);

    }

    private void verifyDefendants(final UUID defendantId, final List<UUID> offenceIds, final List<FormDefendants> formDefendantsList) {
        final FormDefendants formDefendants = formDefendantsList.stream().filter(a -> a.getDefendantId().equals(defendantId)).findFirst().get();
        assertThat(formDefendants.getDefendantId(), is(defendantId));
    }


    @Test
    public void shouldNotFormDefendantUpdated_WhenBcmFormNotPresent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID courtFormId = randomUUID();
        final UUID userId = randomUUID();
        final List<UUID> defendantOffenceIds2 = new ArrayList<>();
        defendantOffenceIds2.add(defendantId);

        final List<Object> eventStream = caseAggregate.updateFormDefendants(courtFormId, caseId, defendantOffenceIds2, userId, BCM).collect(toList());
        assertThat(eventStream, hasSize(1));
        final Object result = eventStream.get(0);
        assertThat(result, instanceOf(FormOperationFailed.class));
        final FormOperationFailed formOperationFailed = (FormOperationFailed) result;
        assertThat(courtFormId, is(formOperationFailed.getCourtFormId()));
        assertThat(BCM, is(formOperationFailed.getFormType()));

    }


    private void assertFormCreatedFromEventStream(final UUID caseId, final UUID courtFormId, final UUID formId, final String formData, final FormType formType, final List<Object> eventStream) {
        final Object object = eventStream.get(0);
        assertThat(object, instanceOf(FormCreated.class));
        final FormCreated form = (FormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(form.getCourtFormId()));
        assertThat(formType, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getFormData()));
    }

    private void assertFormCreatedFromObject(final UUID caseId, final UUID courtFormId, final UUID formId, final String formData, final FormType formType, final Object object) {
        assertThat(object, instanceOf(FormCreated.class));
        final FormCreated form = (FormCreated) object;
        assertThat(caseId, is(form.getCaseId()));
        assertThat(courtFormId, is(form.getCourtFormId()));
        assertThat(formType, is(form.getFormType()));
        assertThat(formId, is(form.getFormId()));
        assertThat(formData, is(form.getFormData()));
    }

    @Test
    public void shouldUpdateCpsOrganisationWhenEventRaised() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final ProsecutionCaseSubject prosecutionCaseSubject = ProsecutionCaseSubject.prosecutionCaseSubject()
                .withOuCode("AB12345")
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCpsDetails(prosecutionCase.getId(), defendant.getId(), prosecutionCaseSubject, "AB12345").collect(toList());

        assertThat(eventStream.size(), is(1));

        final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument = (CaseCpsDetailsUpdatedFromCourtDocument) eventStream.get(0);

        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCaseId(), is(prosecutionCase.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getDefendantId(), is(defendant.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation(), is("AB12345"));

        final ProsecutionCase updatedProsecutionCase = this.caseAggregate.getProsecutionCase();
        assertThat(updatedProsecutionCase.getCpsOrganisation(), is("AB12345"));
        assertThat(updatedProsecutionCase.getDefendants().get(0).getCpsDefendantId(), is(nullValue()));

    }

    @Test
    public void shouldNotUpdateCpsOrganisationWhenEventRaisedWithNoCPS() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final ProsecutionCaseSubject prosecutionCaseSubject = ProsecutionCaseSubject.prosecutionCaseSubject()
                .withOuCode("AB12345")
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCpsDetails(prosecutionCase.getId(), defendant.getId(), prosecutionCaseSubject, null).collect(toList());

        assertThat(eventStream.size(), is(1));

        final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument = (CaseCpsDetailsUpdatedFromCourtDocument) eventStream.get(0);

        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCaseId(), is(prosecutionCase.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getDefendantId(), is(defendant.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation(), is(nullValue()));

        final ProsecutionCase updatedProsecutionCase = this.caseAggregate.getProsecutionCase();
        assertThat(updatedProsecutionCase.getCpsOrganisation(), is(nullValue()));
        assertThat(updatedProsecutionCase.getDefendants().get(0).getCpsDefendantId(), is(nullValue()));

    }

    @Test
    public void shouldUpdateCpsDefendantIdWhenEventRaised() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID cpsDefendantId = randomUUID();
        final ProsecutionCaseSubject prosecutionCaseSubject = ProsecutionCaseSubject.prosecutionCaseSubject()
                .withOuCode("AB12345")
                .withDefendantSubject(DefendantSubject.defendantSubject()
                        .withCpsDefendantId(cpsDefendantId.toString())
                        .build())
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCpsDetails(prosecutionCase.getId(), defendant.getId(), prosecutionCaseSubject, "AB12345").collect(toList());

        assertThat(eventStream.size(), is(1));

        final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument = (CaseCpsDetailsUpdatedFromCourtDocument) eventStream.get(0);

        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCaseId(), is(prosecutionCase.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getDefendantId(), is(defendant.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation(), is("AB12345"));

        final ProsecutionCase updatedProsecutionCase = this.caseAggregate.getProsecutionCase();
        assertThat(updatedProsecutionCase.getCpsOrganisation(), is("AB12345"));
        assertThat(updatedProsecutionCase.getDefendants().get(0).getCpsDefendantId(), is(cpsDefendantId.toString()));

    }

    @Test
    public void shouldUpdateCpsDefendantIdWhenEventRaisedWithCpsOrganisationDefendantDetails() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final UUID cpsDefendantId = randomUUID();
        final ProsecutionCaseSubject prosecutionCaseSubject = ProsecutionCaseSubject.prosecutionCaseSubject()
                .withOuCode("AB12345")
                .withDefendantSubject(DefendantSubject.defendantSubject()
                        .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                                .withCpsDefendantId(cpsDefendantId.toString())
                                .build())
                        .build())
                .build();

        final List<Object> eventStream = this.caseAggregate.updateCpsDetails(prosecutionCase.getId(), defendant.getId(), prosecutionCaseSubject, "AB12345").collect(toList());

        assertThat(eventStream.size(), is(1));

        final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument = (CaseCpsDetailsUpdatedFromCourtDocument) eventStream.get(0);

        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCaseId(), is(prosecutionCase.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getDefendantId(), is(defendant.getId()));
        assertThat(caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation(), is("AB12345"));

        final ProsecutionCase updatedProsecutionCase = this.caseAggregate.getProsecutionCase();
        assertThat(updatedProsecutionCase.getCpsOrganisation(), is("AB12345"));
        assertThat(updatedProsecutionCase.getDefendants().get(0).getCpsDefendantId(), is(cpsDefendantId.toString()));

    }

    @Test
    public void shouldSkipMatchedDefendantWithoutCourtProceedingsInitiatedPartiallyMatch() {
        CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .build();

        MatchDefendant matchDefendant = MatchDefendant.matchDefendant()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withMatchedDefendants(asList(
                        MatchedDefendant.matchedDefendant()
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build(),
                        MatchedDefendant.matchedDefendant()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build()))
                .build();
        CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(caseLinkedToHearing);
        Stream<Object> objectStream = caseAggregate.matchPartiallyMatchedDefendants(matchDefendant);
        Optional<Object> obj = objectStream.filter(s -> s instanceof MasterDefendantIdUpdated).findFirst();
        assertThat(obj.isPresent(), is(true));
        assertThat(obj.map(s -> (MasterDefendantIdUpdated) s).get().getMatchedDefendants().size(), is(1));

    }

    @Test
    public void shouldSkipMatchedDefendantWithoutCourtProceedingsInitiatedFullMatch() {
        CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .build();
        ExactMatchedDefendantSearchResultStored exactMatchedDefendantSearchResultStored = ExactMatchedDefendantSearchResultStored.exactMatchedDefendantSearchResultStored()
                .withDefendantId(randomUUID())
                .withCases(asList(Cases.cases()
                        .withCaseReference("REF")
                        .withProsecutionCaseId(randomUUID().toString())
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(randomUUID().toString())
                                .build(), Defendants.defendants()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID().toString())
                                .withMasterDefendantId(randomUUID().toString())
                                .build()))
                        .build()))
                .build();
        caseAggregate.apply(caseLinkedToHearing);
        caseAggregate.apply(exactMatchedDefendantSearchResultStored);


        Stream<Object> objectStream = caseAggregate.storeMatchedDefendants(randomUUID());
        Optional<Object> obj = objectStream.filter(s -> s instanceof MasterDefendantIdUpdated).findFirst();
        assertThat(obj.isPresent(), is(true));
        assertThat(obj.map(s -> (MasterDefendantIdUpdated) s).get().getMatchedDefendants().size(), is(1));

    }

    @Test
    public void shouldRecordOnlinePlea() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));
        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getId())
                .withComeToCourt(true)
                .withLegalEntityDefendant(uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityDefendant.legalEntityDefendant().withAddress(Address.address().withPostcode("CR0 5QT").build()).withContactDetails(ContactDetails.contactDetails().withEmail("dummy@gmail.com").build()).build())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).withPlea(PleaType.NOT_GUILTY).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(4));
        assertThat(eventStream.get(0).getClass(), is(equalTo(OnlinePleaRecorded.class)));
        assertThat(eventStream.get(1).getClass(), is(equalTo(FinanceDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(2).getClass(), is(equalTo(PleaDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(3).getClass(), is(equalTo(NotificationSentForDefendantDocument.class)));
    }

    @Test
    public void shouldRaiseDefendantNotFoundForOnlinePlea() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(randomUUID())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DefendantNotFound.class)));

    }

    @Test
    public void shouldRaiseCaseNotFoundForOnlinePlea() {

        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(randomUUID())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(CaseNotFound.class)));

    }

    @Test
    public void shouldReturnTrueWhenAnyOffenceHasAlreadyPleaSubmittedFlagTrue() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();

        final ProsecutionCase prosecutionCase = createProsecutionCase(asList(defendant()
                        .withId(randomUUID())
                        .withOffences(asList(offence()
                                        .withId(randomUUID())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .build()))
                        .build(),
                defendant()
                        .withId(defendantId)
                        .withOffences(asList(offence()
                                        .withId(randomUUID())
                                        .build(),
                                offence()
                                        .withId(offenceId)
                                        .withOnlinePleaReceived(true)
                                        .build()))
                        .build()));

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final boolean isOffenceAlreadyPlead = caseAggregate.isOffenceAlreadyPlead(asList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence()
                .withId(offenceId.toString())
                .build()), defendantId);

        assertThat(isOffenceAlreadyPlead, is(true));

    }

    @Test
    public void shouldReturnFalseWhenNoneOfTheOffencesHasPleaSubmittedFlagTrue() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();

        final ProsecutionCase prosecutionCase = createProsecutionCase(asList(defendant()
                        .withId(randomUUID())
                        .withOffences(singletonList(offence()
                                .withId(randomUUID())
                                .build()))
                        .build(),
                defendant()
                        .withId(defendantId)
                        .withOffences(asList(offence()
                                        .withId(randomUUID())
                                        .build(),
                                offence()
                                        .withId(randomUUID())
                                        .withOnlinePleaReceived(true)
                                        .build(),
                                offence()
                                        .withId(offenceId)
                                        .withOnlinePleaReceived(false)
                                        .build()))
                        .build()));

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final boolean isOffenceAlreadyPlead = caseAggregate.isOffenceAlreadyPlead(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence()
                .withId(offenceId.toString())
                .build()), defendantId);

        assertThat(isOffenceAlreadyPlead, is(false));

    }

    @Test
    public void shouldSkipMasterDefendantIdUpdatedUponDeletedHearing() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .build();

        final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase = HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .withDefendantIds(asList(defendantId))
                .build();

        MatchDefendant matchDefendant = MatchDefendant.matchDefendant()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withMatchedDefendants(asList(
                        MatchedDefendant.matchedDefendant()
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build(),
                        MatchedDefendant.matchedDefendant()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build()))
                .build();
        CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(caseLinkedToHearing);
        // Marking Hearing a duplicate for case
        caseAggregate.apply(hearingMarkedAsDuplicateForCase);

        Stream<Object> objectStream = caseAggregate.matchPartiallyMatchedDefendants(matchDefendant);

        Optional<Object> masterDefendantIdUpdated = objectStream.filter(s -> s instanceof MasterDefendantIdUpdated).findFirst();
        assertThat(masterDefendantIdUpdated.isPresent(), is(true));
    }





    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedingsWithPersonalAndOrganisationalDefendant(
            final UUID caseId, final UUID defendantId, final UUID defendantId2, final UUID offenceId) {
        ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withLabel("Victim Criminal offence").withJudicialResultId(randomUUID()).build();
        final uk.gov.justice.core.courts.Offence offence = offence()
                .withId(offenceId)
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withDvlaOffenceCode("BA76004")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 05, 01))
                .withReportingRestrictions(Lists.newArrayList(reportingRestriction))
                .withCount(0)
                .build();
        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(Lists.newArrayList(offence))
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withDateOfBirth(LocalDate.now().minusYears(16))
                                .build())
                        .build())
                .build();
        final Defendant defendant2 = defendant()
                .withId(defendantId2)
                .withMasterDefendantId(randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withProsecutionCaseId(caseId)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withAddress(Address.address()
                                        .withAddress1("Address1")
                                        .build())
                                .build())
                        .build())
                .withOffences(Lists.newArrayList(offence))
                .build();

        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ReferralReason referralReason2 = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId2)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(Lists.newArrayList(offenceId))
                .withReferralReason(referralReason)
                .build();
        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(Lists.newArrayList(offenceId))
                .withReferralReason(referralReason2)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(MAGISTRATES)
                .withListDefendantRequests(Lists.newArrayList(listDefendantRequest, listDefendantRequest2))
                .withListedStartDateTime(ZonedDateTime.now())
                .build();
        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(asList(defendant, defendant2))
                .withListHearingRequests(Lists.newArrayList(listHearingRequest))
                .build();
    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedingsWithoutDOB(
            final UUID caseId, final UUID defendantId,  final UUID offenceId) {
        ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withLabel("Victim Criminal offence").withJudicialResultId(randomUUID()).build();
        final uk.gov.justice.core.courts.Offence offence = offence()
                .withId(offenceId)
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withDvlaOffenceCode("BA76004")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 05, 01))
                .withReportingRestrictions(Lists.newArrayList(reportingRestriction))
                .withCount(0)
                .build();
        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(Lists.newArrayList(offence))
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                .withFirstName("XYYZ")
                                .withLastName("ABC")
                                .build()
                        )
                        .build())
                .build();


        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withDescription("Dodged TFL tickets with passion")
                .build();



        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(Lists.newArrayList(offenceId))
                .withReferralReason(referralReason)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(MAGISTRATES)
                .withListDefendantRequests(Lists.newArrayList(listDefendantRequest))
                .withListedStartDateTime(ZonedDateTime.now())
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(Lists.newArrayList(defendant))
                .withListHearingRequests(Lists.newArrayList(listHearingRequest))
                .build();

    }

    private DefendantsAddedToCourtProceedings buildDefendantsAddedToCourtProceedings(
            final UUID caseId, final UUID defendantId, final UUID defendantId2, final UUID offenceId) {
        ReportingRestriction reportingRestriction = ReportingRestriction.reportingRestriction().withLabel("Victim Criminal offence").withJudicialResultId(randomUUID()).build();
        final uk.gov.justice.core.courts.Offence offence = offence()
                .withId(offenceId)
                .withOffenceDefinitionId(UUID.randomUUID())
                .withOffenceCode("TFL123")
                .withDvlaOffenceCode("BA76004")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 05, 01))
                .withReportingRestrictions(Lists.newArrayList(reportingRestriction))
                .withCount(0)
                .build();
        final Defendant defendant = defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        //Add duplicate defendant
        final Defendant defendant1 = defendant()
                .withId(defendantId)
                .withMasterDefendantId(randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        final Defendant defendant2 = defendant()
                .withId(defendantId2)
                .withMasterDefendantId(randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withProsecutionCaseId(caseId)
                .withOffences(new ArrayList<>(asList(offence)))
                .build();

        final ReferralReason referralReason = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ReferralReason referralReason2 = ReferralReason.referralReason()
                .withId(UUID.randomUUID())
                .withDefendantId(defendantId2)
                .withDescription("Dodged TFL tickets with passion")
                .build();

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(asList(offenceId))
                .withReferralReason(referralReason)
                .build();
        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(caseId)
                .withDefendantOffences(asList(offenceId))
                .withReferralReason(referralReason2)
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(UUID.randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = courtCentre().withId(UUID.randomUUID()).build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(MAGISTRATES)
                .withListDefendantRequests(asList(listDefendantRequest, listDefendantRequest2))
                .withListedStartDateTime(ZonedDateTime.now())
                .build();

        return DefendantsAddedToCourtProceedings
                .defendantsAddedToCourtProceedings()
                .withDefendants(asList(defendant, defendant1, defendant2))
                .withListHearingRequests(asList(listHearingRequest))
                .build();

    }

    private void handleCpsOrganisationCasesWith(String oldCpsOrganisation) {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));


        ProsecutionCaseIdentifier prosecutionCaseIdentifier = ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityCode("ProsecutionAuthorityCode")
                .withProsecutionAuthorityId(randomUUID())
                .withProsecutionAuthorityName("ProsecutionAuthorityName")
                .withAddress(Address.address().build())
                .withProsecutionAuthorityOUCode("OUCode")
                .withContact(ContactNumber.contactNumber().build())
                .withMajorCreditorCode("MajorCreditorCode")
                .build();

        final List<Object> eventStream = caseAggregate.updateCaseProsecutorDetails(prosecutionCaseIdentifier, oldCpsOrganisation).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(CaseCpsProsecutorUpdated.class)));

        JsonObject expectedProsecutionCaseIdentifier = objectToJsonObjectConverter.convert(prosecutionCase.getProsecutionCaseIdentifier());
        expectedProsecutionCaseIdentifier = JsonHelper.addProperty(expectedProsecutionCaseIdentifier, "caseURN", "caseUrn");
        expectedProsecutionCaseIdentifier = JsonHelper.addProperty(expectedProsecutionCaseIdentifier, "prosecutionAuthorityReference", "reference");
        assertThat(objectToJsonObjectConverter.convert(caseAggregate.getProsecutionCase().getProsecutionCaseIdentifier()), is(expectedProsecutionCaseIdentifier));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorId(), is(prosecutionCaseIdentifier.getProsecutionAuthorityId()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorCode(), is(prosecutionCaseIdentifier.getProsecutionAuthorityCode()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getProsecutorName(), is(prosecutionCaseIdentifier.getProsecutionAuthorityName()));
        assertThat(caseAggregate.getProsecutionCase().getProsecutor().getAddress(), is(prosecutionCaseIdentifier.getAddress()));

    }

    private List<Defendant> getDefendants(final UUID caseId, final UUID defendantId1, final UUID defendantId2, final UUID defendantId3,
                                          final UUID offenceId1, final UUID offenceId2, final UUID offenceId3) {
        final Defendant defendant1 = Defendant.defendant().withOffences(offencesWith(offenceId1, false, null))
                .withProsecutionCaseId(caseId).withId(defendantId1).build();
        final Defendant defendant2 = Defendant.defendant().withOffences(offencesWith(offenceId2, false, 5))
                .withProsecutionCaseId(caseId).withId(defendantId2).build();
        final Defendant defendant3 = Defendant.defendant().withOffences(offencesWith(offenceId3, false, null))
                .withProsecutionCaseId(caseId).withId(defendantId3).build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        defendants.add(defendant2);
        defendants.add(defendant3);
        return defendants;
    }

    private List<Defendant> getDefendantsWithMasterDefendantId(final UUID caseId, final UUID defendantId, final UUID masterDefendantId,
                                                               final UUID offenceId, final boolean proceedingsConcluded) {

        final Defendant defendant1 = Defendant.defendant()
                .withProsecutionCaseId(caseId)
                .withMasterDefendantId(masterDefendantId)
                .withId(defendantId)
                .withProceedingsConcluded(proceedingsConcluded)
                .withOffences(offencesWith(offenceId, proceedingsConcluded, null))
                .build();

        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(defendant1);
        return defendants;
    }

    private static ProsecutionCase createProsecutionCase(final List<Defendant> defendants) {
        return prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(randomUUID())
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(defendants)
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .withCaseURN("caseUrn")
                        .build())
                .build();
    }

    private List<Defendant> getDefendantsWithProceedingsConcluded(final UUID caseId,
                                                                  final UUID defendantId1,
                                                                  final UUID defendantId2,
                                                                  final UUID offenceId1,
                                                                  final UUID offenceId2,
                                                                  final boolean proceedingsConcluded1,
                                                                  final boolean proceedingsConcluded2) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(getDefendant(caseId, defendantId1, offenceId1, proceedingsConcluded1, 5));
        defendants.add(getDefendant(caseId, defendantId2, offenceId2, proceedingsConcluded2, 5));

        return defendants;
    }

    private List<Defendant> updatedDefendantOffences(final List<Defendant> currentDefendantsList, final UUID defendantId, final UUID offenceId, boolean proceedingsConcluded) {

        final Optional<Defendant> matchingDefendantOpt = currentDefendantsList.stream().filter(currentDef -> currentDef.getId().equals(defendantId)).findFirst();
        final Defendant updatedDefendantAndOffence = matchingDefendantOpt.map(matchingDefendant -> {

            final List<uk.gov.justice.core.courts.Offence> otherExistingOffences = matchingDefendant.getOffences().stream().filter(o -> !o.getId().equals(offenceId)).collect(toList());
            final ArrayList<uk.gov.justice.core.courts.Offence> updatedOffences = new ArrayList<>(otherExistingOffences);
            final Optional<uk.gov.justice.core.courts.Offence> amendedOffence = matchingDefendant.getOffences().stream().filter(o -> o.getId().equals(offenceId)).findFirst()
                    .map(o -> getAmendedOffence(o, proceedingsConcluded));
            amendedOffence.ifPresent(updatedOffences::add);

            return getDefendant(matchingDefendant.getProsecutionCaseId(), matchingDefendant.getId(), updatedOffences, proceedingsConcluded);
        }).orElse(null);

        final List<Defendant> allDefendants = new ArrayList<>(currentDefendantsList.stream().filter(currentDef -> !currentDef.getId().equals(defendantId)).collect(toList()));
        allDefendants.add(updatedDefendantAndOffence);


        return allDefendants;
    }

    private List<Defendant> getDefendantsWithProceedingsConcluded(final UUID caseId,
                                                                  final UUID defendantId1,
                                                                  final UUID defendantId2,
                                                                  final UUID defendantId3,
                                                                  final UUID offenceId1,
                                                                  final UUID offenceId2,
                                                                  final UUID offenceId3,
                                                                  final boolean proceedingsConcluded1,
                                                                  final boolean proceedingsConcluded2,
                                                                  final boolean proceedingsConcluded3,
                                                                  final Integer listingNumber
    ) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(getDefendant(caseId, defendantId1, offenceId1, proceedingsConcluded1, listingNumber));
        defendants.add(getDefendant(caseId, defendantId2, offenceId2, proceedingsConcluded2, listingNumber));
        defendants.add(getDefendant(caseId, defendantId3, offenceId3, proceedingsConcluded3, listingNumber));

        return defendants;
    }

    private Defendant getDefendant(final UUID caseId, final UUID defendantId, final UUID offenceId, final boolean proceedingsConcluded, final Integer listingNumber) {
        return Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(proceedingsConcluded)
                .withOffences(offencesWith(offenceId, proceedingsConcluded, listingNumber))
                .build();
    }

    private Defendant getDefendant(final UUID caseId, final UUID defendantId, final List<uk.gov.justice.core.courts.Offence> offences, final boolean proceedingsConcluded) {
        return Defendant.defendant()
                .withId(defendantId)
                .withProsecutionCaseId(caseId)
                .withProceedingsConcluded(proceedingsConcluded)
                .withOffences(offences)
                .build();
    }

    private List<uk.gov.justice.core.courts.Offence> offencesWith(final UUID offenceId, final boolean proceedingsConcluded, final Integer listingNUmber) {
        final JudicialResult judicialResult = JudicialResult.judicialResult().withCategory(proceedingsConcluded ? FINAL : INTERMEDIARY).withIsNewAmendment(Boolean.TRUE).build();
        final ArrayList<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(judicialResult);
        final uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence().withId(offenceId).withListingNumber(listingNUmber).withProceedingsConcluded(proceedingsConcluded)
                .withJudicialResults(judicialResults).build();
        final ArrayList<uk.gov.justice.core.courts.Offence> offences = new ArrayList<>();
        offences.add(offence);
        return offences;
    }

    private uk.gov.justice.core.courts.Offence getAmendedOffence(final uk.gov.justice.core.courts.Offence currentOffence, final boolean proceedingsConcluded) {
        final JudicialResult judicialResult = JudicialResult.judicialResult().withIsNewAmendment(Boolean.TRUE).withCategory(proceedingsConcluded ? FINAL : INTERMEDIARY).build();
        final ArrayList<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(judicialResult);
        return uk.gov.justice.core.courts.Offence.offence().withId(currentOffence.getId()).withListingNumber(currentOffence.getListingNumber()).withProceedingsConcluded(proceedingsConcluded)
                .withJudicialResults(judicialResults).build();
    }

    private void assertCaseStatus(final HearingResultedCaseUpdated hearingResultedCaseUpdated, final UUID caseId, final CaseStatusEnum caseStatusEnum) {
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getId(), is(caseId));
        assertThat(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus(), is(caseStatusEnum.toString()));
    }

    private ProsecutionCaseIdentifier getProsecutionCaseIdentifier(String caseURN) {
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withProsecutionAuthorityReference("reference")
                .withProsecutionAuthorityCode("code")
                .withProsecutionAuthorityId(randomUUID())
                .withCaseURN(caseURN)
                .build();
    }

    @Test
    public void shouldHandleOnlinePleaDocumentCreationWithPrimaryEmail() {
        final UUID caseId = randomUUID();
        final UUID fileId = randomUUID();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withContact(ContactNumber.contactNumber()
                                .withPrimaryEmail("primary-email@hmcts.net")
                                .withSecondaryEmail("secondary-email@hmcts.net")
                                .build())
                        .build())
                .build();

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final HandleOnlinePleaDocumentCreation handleOnlinePleaDocumentCreation = HandleOnlinePleaDocumentCreation.handleOnlinePleaDocumentCreation()
                .withCaseId(caseId)
                .withPleaNotificationType(COMPANYONLINEPLEA)
                .withSystemDocGeneratorId(fileId)
                .build();

        final List<Object> eventStream = this.caseAggregate.handleOnlinePleaDocumentCreation(handleOnlinePleaDocumentCreation).collect(toList());

        assertThat(eventStream.size(), is(2));
        assertThat(eventStream.get(0).getClass(), is(equalTo(NotificationSentForPleaDocument.class)));
        final NotificationSentForPleaDocument notificationSentForPleaDocument = (NotificationSentForPleaDocument) eventStream.get(0);
        assertThat(notificationSentForPleaDocument.getCaseId(), is(caseId));
        assertThat(notificationSentForPleaDocument.getEmail(), is(prosecutionCase.getProsecutionCaseIdentifier().getContact().getPrimaryEmail()));

        assertThat(eventStream.get(1).getClass(), is(equalTo(OnlinePleaDocumentUploadedAsCaseMaterial.class)));
        final OnlinePleaDocumentUploadedAsCaseMaterial uploadedAsCaseMaterial = (OnlinePleaDocumentUploadedAsCaseMaterial) eventStream.get(1);
        assertThat(uploadedAsCaseMaterial.getFileId(), is(fileId));
    }

    @Test
    public void shouldHandleOnlinePleaDocumentCreationWithSecondaryEmail() {
        final UUID caseId = randomUUID();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withContact(ContactNumber.contactNumber()
                                .withSecondaryEmail("secondary-email@hmcts.net")
                                .build())
                        .build())
                .build();

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final HandleOnlinePleaDocumentCreation handleOnlinePleaDocumentCreation = HandleOnlinePleaDocumentCreation.handleOnlinePleaDocumentCreation()
                .withCaseId(caseId)
                .withPleaNotificationType(COMPANYONLINEPLEA)
                .withSystemDocGeneratorId(randomUUID())
                .build();

        final List<Object> eventStream = this.caseAggregate.handleOnlinePleaDocumentCreation(handleOnlinePleaDocumentCreation).collect(toList());

        assertThat(eventStream.size(), is(2));
        assertThat(eventStream.get(0).getClass(), is(equalTo(NotificationSentForPleaDocument.class)));
        final NotificationSentForPleaDocument notificationSentForPleaDocument = (NotificationSentForPleaDocument) eventStream.get(0);
        assertThat(notificationSentForPleaDocument.getCaseId(), is(caseId));
        assertThat(notificationSentForPleaDocument.getEmail(), is(prosecutionCase.getProsecutionCaseIdentifier().getContact().getSecondaryEmail()));

    }

    @Test
    public void shouldHandleOnlinePleaDocumentCreationWithNoProsecutorContact() {
        final UUID caseId = randomUUID();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(defendants)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .build())
                .build();

        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        final HandleOnlinePleaDocumentCreation handleOnlinePleaDocumentCreation = HandleOnlinePleaDocumentCreation.handleOnlinePleaDocumentCreation()
                .withCaseId(caseId)
                .withPleaNotificationType(COMPANYONLINEPLEA)
                .withSystemDocGeneratorId(randomUUID())
                .build();

        final List<Object> eventStream = this.caseAggregate.handleOnlinePleaDocumentCreation(handleOnlinePleaDocumentCreation).collect(toList());

        assertThat(eventStream.size(), is(2));
        assertThat(eventStream.get(0).getClass(), is(equalTo(NotificationSentForPleaDocumentFailed.class)));
        final NotificationSentForPleaDocumentFailed notificationSentForPleaDocumentFailed = (NotificationSentForPleaDocumentFailed) eventStream.get(0);
        assertThat(notificationSentForPleaDocumentFailed.getCaseId(), is(caseId));

    }

    @Test
    public void shouldSendEmailToDefendantForOnlineGuiltyPleaCourtHearing() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getId())
                .withComeToCourt(true)
                .withLegalEntityDefendant(uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityDefendant.legalEntityDefendant().withAddress(Address.address().withPostcode("CR0 5QT").build()).withContactDetails(ContactDetails.contactDetails().withEmail("dummy@gmail.com").build()).build())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).withPlea(PleaType.GUILTY).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(4));
        assertThat(eventStream.get(0).getClass(), is(equalTo(OnlinePleaRecorded.class)));
        assertThat(eventStream.get(1).getClass(), is(equalTo(FinanceDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(2).getClass(), is(equalTo(PleaDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(3).getClass(), is(equalTo(NotificationSentForDefendantDocument.class)));
    }

    @Test
    public void shouldSendEmailToDefendantForOnlineGuiltyPleaNoCourtHearing() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getId())
                .withComeToCourt(false)
                .withLegalEntityDefendant(uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityDefendant.legalEntityDefendant().withAddress(Address.address().withPostcode("CR0 5QT").build()).withContactDetails(ContactDetails.contactDetails().withEmail("dummy@gmail.com").build()).build())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).withPlea(PleaType.GUILTY).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(4));
        assertThat(eventStream.get(0).getClass(), is(equalTo(OnlinePleaRecorded.class)));
        assertThat(eventStream.get(1).getClass(), is(equalTo(FinanceDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(2).getClass(), is(equalTo(PleaDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(3).getClass(), is(equalTo(NotificationSentForDefendantDocument.class)));
    }

    @Test
    public void shouldSendEmailToDefendantForOnlineNotGuiltyPlea() {
        caseAggregate.apply(new ProsecutionCaseCreated(prosecutionCase, null));

        List<Object> eventStream = caseAggregate.recordOnlinePlea(PleadOnline.pleadOnline()
                .withCaseId(prosecutionCase.getId())
                .withDefendantId(defendant.getId())
                .withComeToCourt(false)
                .withLegalEntityDefendant(uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityDefendant.legalEntityDefendant().withAddress(Address.address().withPostcode("CR0 5QT").build()).withContactDetails(ContactDetails.contactDetails().withEmail("dummy@gmail.com").build()).build())
                .withOffences(singletonList(uk.gov.moj.cpp.progression.plea.json.schemas.Offence.offence().withId(offence.getId().toString()).withPlea(PleaType.NOT_GUILTY).build()))
                .build()).collect(toList());

        assertThat(eventStream.size(), is(4));
        assertThat(eventStream.get(0).getClass(), is(equalTo(OnlinePleaRecorded.class)));
        assertThat(eventStream.get(1).getClass(), is(equalTo(FinanceDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(2).getClass(), is(equalTo(PleaDocumentForOnlinePleaSubmitted.class)));
        assertThat(eventStream.get(3).getClass(), is(equalTo(NotificationSentForDefendantDocument.class)));
    }

    @Test
    public void shouldAddNewOffenceWithLaaApplicationReferenceIfExistingOffenceHasLaaApplicationReference() {
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID offenceId = randomUUID();
        final String applicationReference = "AB746921";

        this.caseAggregate.apply(createProsecutionCaseCreated(caseId, defendantId1, offenceId, applicationReference, true));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(1));
        assertThat(this.caseAggregate.getDefendantCaseOffences().get(defendantId1).size(), is(1));

        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence()
                .withId(randomUUID())
                .withProceedingsConcluded(false)
                .withCount(7)
                .withIndictmentParticular("Indictment Particular")
                .build();

        this.caseAggregate.updateOffences(Arrays.asList(offence), caseId, defendantId1, Optional.of(createJsonList()));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is(applicationReference));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().get(1).getLaaApplnReference().getApplicationReference(), is(applicationReference));
    }

    @Test
    public void shouldNotAddNewOffenceWithLaaApplicationReferenceIfExistingOffenceHasLaaApplicationReferenceAndNewOffenceWithoutCount() {
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID offenceId = randomUUID();
        final String applicationReference = "AB746921";
        final UUID offenceId2 = randomUUID();

        this.caseAggregate.apply(createProsecutionCaseCreated(caseId, defendantId1, offenceId, applicationReference, true));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(1));
        assertThat(this.caseAggregate.getDefendantCaseOffences().get(defendantId1).size(), is(1));

        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence()
                .withId(offenceId2)
                .withProceedingsConcluded(false)
                .build();

        this.caseAggregate.updateOffences(Arrays.asList(offence), caseId, defendantId1, Optional.of(createJsonList()));

        final List<uk.gov.justice.core.courts.Offence> offences = this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences();
        final Optional<uk.gov.justice.core.courts.Offence> offence1 = offences.stream().filter(o -> o.getId().equals(offenceId)).findFirst();
        final Optional<uk.gov.justice.core.courts.Offence> offence2 = offences.stream().filter(o -> o.getId().equals(offenceId2)).findFirst();

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(2));
        assertThat(offence1.get().getLaaApplnReference().getApplicationReference(), is(applicationReference));
        assertThat(offence2.get().getLaaApplnReference(), nullValue());
    }

    @Test
    public void shouldAddNewOffenceWithoutLaaApplicationReferenceIfExistingOffenceDoesNotHaveLaaApplicationReference() {
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID offenceId = randomUUID();


        this.caseAggregate.apply(createProsecutionCaseCreated(caseId, defendantId1, offenceId, null, false));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(1));
        assertThat(this.caseAggregate.getDefendantCaseOffences().get(defendantId1).size(), is(1));

        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence()
                .withId(randomUUID())
                .withProceedingsConcluded(false)
                .build();

        this.caseAggregate.updateOffences(Arrays.asList(offence), caseId, defendantId1, Optional.of(createJsonList()));

        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().size(), is(1));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().size(), is(2));
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().get(0).getLaaApplnReference(), nullValue());
        assertThat(this.caseAggregate.getProsecutionCase().getDefendants().get(0).getOffences().get(1).getLaaApplnReference(), nullValue());
    }

    private ProsecutionCaseCreated createProsecutionCaseCreated(final UUID caseId, final UUID defendantId1, final UUID offenceId, final String applicationReference, final boolean flag) {
        final uk.gov.justice.core.courts.Defendant defendant1 = uk.gov.justice.core.courts.Defendant.defendant()
                .withId(defendantId1)
                .withOffences(
                        Arrays.asList(uk.gov.justice.core.courts.Offence.offence()
                                .withId(offenceId)
                                .withListingNumber(1)
                                .withProceedingsConcluded(true)
                                .withLaaApplnReference(flag ? LaaReference.laaReference().withApplicationReference(applicationReference).build() : null)
                                .build()))
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withId(caseId)
                .withDefendants(Arrays.asList(defendant1))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("URN").build())
                .build();

        return ProsecutionCaseCreated.prosecutionCaseCreated()
                .withProsecutionCase(prosecutionCase)
                .build();

    }

    private List<JsonObject> createJsonList() {
        return Arrays.asList(createArrayBuilder().add(
                createObjectBuilder()
                        .add("cjsOffenceCode", "TTH105HY")
                        .build()).build().getJsonObject(0));
    }

    private List<JsonObject> prepareReferenceDataOffencesJsonObject(final UUID offenceId,
                                                                    final String offenceCode,
                                                                    final String legislation,
                                                                    final String payloadPath) {
        final String referenceDataOffenceJsonString = getPayload(payloadPath)
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("OFFENCE_CODE", offenceCode)
                .replace("LEGISLATION", legislation);
        final JsonReader jsonReader = Json.createReader(new StringReader(referenceDataOffenceJsonString));


        final List<JsonObject> referencedataOffencesJsonObject = jsonReader.readObject().getJsonArray("offences").getValuesAs(JsonObject.class);
        return referencedataOffencesJsonObject;
    }

    @Test
    public void shouldAggregatePartialMatchedDefendantSearchResult() {
        final List<Cases> cases = new ArrayList<>();
        cases.add(Cases.cases()
                .withProsecutionCaseId(randomUUID().toString())
                .withCaseReference("CaseURN1")
                .build());
        final Stream<Object> eventStream = caseAggregate.aggregatePartialMatchedDefendantSearchResult(defendant.getId(), cases);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(PartialMatchedDefendantSearchResultStored.class));
    }

    @Test
    public void shouldReceiveDisAssociateDefenceOrganisation() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        final Stream<Object> eventStream = caseAggregate.receiveDisAssociateDefenceOrganisation(defendant.getId(), prosecutionCase.getId(), randomUUID());

        doNothing().when(logger).debug(any());

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(DefendantDefenceOrganisationChanged.class));
        assertThat(events.get(1), instanceOf(DefenceOrganisationDissociatedByDefenceContext.class));
    }

    @Test
    public void shouldReceiveAssociateDefenceOrganisation() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        final Stream<Object> eventStream = caseAggregate.receiveAssociateDefenceOrganisation("orgName", defendant.getId(), prosecutionCase.getId(), "LAANumber", ZonedDateTime.now(), "REPRESENTATION_ORDER", OrganisationDetails.newBuilder().build());

        doNothing().when(logger).debug(any());

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(DefendantDefenceOrganisationChanged.class));
        assertThat(events.get(1), instanceOf(DefenceOrganisationAssociatedByDefenceContext.class));
    }

    @Test
    public void shouldAggregateExactMatchedDefendantSearchResult() {
        final List<Cases> cases = new ArrayList<>();
        cases.add(Cases.cases()
                .withProsecutionCaseId(randomUUID().toString())
                .withCaseReference("CaseURN1")
                .build());
        final Stream<Object> eventStream = caseAggregate.aggregateExactMatchedDefendantSearchResult(defendant.getId(), cases);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(ExactMatchedDefendantSearchResultStored.class));
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        caseAggregate.linkProsecutionCaseToHearing(randomUUID(), prosecutionCase.getId()).collect(toList());

        final Stream<Object> eventStream = caseAggregate.updateDefendantWithDriverNumber(prosecutionCase.getDefendants().get(0).getId(), prosecutionCase.getId(), "DRIVE123");

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(ProsecutionCaseDefendantUpdated.class));
        assertThat(events.get(1), instanceOf(CaseDefendantUpdatedWithDriverNumber.class));
    }

    @Test
    public void shouldIncreaseListingNumberOfCase() {
        final UUID caseId = randomUUID();

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();

        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();

        final List<Defendant> defendants = getDefendants(caseId, defendantId1, defendantId2, defendantId3, offenceId1, offenceId2, offenceId3);

        final ProsecutionCase prosecutionCase = prosecutionCase()
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                .withDefendants(defendants).withId(caseId).build();
        final ProsecutionCaseCreated prosecutionCaseUpdated = prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build();

        final Object response = this.caseAggregate.apply(prosecutionCaseUpdated);


        final List<ProsecutionCaseListingNumberIncreased> events = this.caseAggregate.increaseListingNumber(singletonList(defendants.get(1).getOffences().get(0).getId()), UUID.randomUUID()).map(ProsecutionCaseListingNumberIncreased.class::cast).collect(toList());

        final ProsecutionCase ProsecutionCaseInAggregate = ReflectionUtil.getValueOfField(this.caseAggregate, "prosecutionCase", ProsecutionCase.class);

        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(0).getOffences().get(0).getListingNumber(), is(nullValue()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(1).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(1).getOffences().get(0).getListingNumber(), is(6));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getId(), is(prosecutionCase.getDefendants().get(2).getOffences().get(0).getId()));
        assertThat(ProsecutionCaseInAggregate.getDefendants().get(2).getOffences().get(0).getListingNumber(), is(nullValue()));

        assertThat(events.get(0).getProsecutionCaseId(), is(caseId));
    }

    @Test
    public void shouldExtendCustodyTimeLimit() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        caseAggregate.linkProsecutionCaseToHearing(randomUUID(), prosecutionCase.getId()).collect(toList());

        final Stream<Object> eventStream = caseAggregate.extendCustodyTimeLimit(randomUUID(), randomUUID(), LocalDate.now());

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(CustodyTimeLimitExtended.class));
    }

    @Test
    public void shouldNotExtendCustodyTimeLimit() {
        final UUID hearingId = randomUUID();
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        caseAggregate.linkProsecutionCaseToHearing(hearingId, prosecutionCase.getId()).collect(toList());

        final Stream<Object> eventStream = caseAggregate.extendCustodyTimeLimit(hearingId, randomUUID(), LocalDate.now());

        final List events = eventStream.collect(toList());
        assertThat(events.size(), is(0));
    }

    @Test
    public void shouldExtendHearing() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        caseAggregate.linkProsecutionCaseToHearing(randomUUID(), prosecutionCase.getId()).collect(toList());

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
        final ExtendHearing extendHearing = ExtendHearing.extendHearing().build();

        final Stream<Object> eventStream = caseAggregate.extendHearing(hearingListingNeeds, extendHearing);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(HearingExtended.class));
    }

    @Test
    public void shouldCreateOnlinePleaPcqVisited() {
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = PleadOnlinePcqVisited.pleadOnlinePcqVisited().build();

        final Stream<Object> eventStream = caseAggregate.createOnlinePleaPcqVisited(pleadOnlinePcqVisited);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(OnlinePleaPcqVisitedRecorded.class));
    }

    @Test
    public void shouldUpdateHearingForPartialAllocation() {
        final List<ProsecutionCasesToRemove> prosecutionCasesToRemove = new ArrayList<>();
        prosecutionCasesToRemove.add(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                .build());
        final Stream<Object> eventStream = caseAggregate.updateHearingForPartialAllocation(randomUUID(), prosecutionCasesToRemove);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(HearingUpdatedForPartialAllocation.class));
    }

    @Test
    public void shouldUpdateCpsDefendantId() {
        caseAggregate.createProsecutionCase(prosecutionCase).collect(toList());
        final Stream<Object> eventStream = caseAggregate.updateCpsDefendantId(randomUUID(), randomUUID(), "CPS123");

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(CpsDefendantIdUpdated.class));
    }

    @Test
    public void shouldRaiseFormOperationFailedWhenFormDataUpdated1() {
        final List<Object> eventStream = caseAggregate.updateForm(randomUUID(), "{}", randomUUID(), randomUUID()).collect(toList());
        final Object formUpdatedObject = eventStream.get(0);
        assertThat(formUpdatedObject, instanceOf(FormOperationFailed.class));
    }

    @Test
    public void shouldUnlinkCases() {
        final List<CaseToUnlink> casesToUnlink = new ArrayList<>();
        casesToUnlink.add(CaseToUnlink.caseToUnlink()
                .withCaseId(randomUUID())
                .build());
        final List<Object> eventStream = caseAggregate.unlinkCases(randomUUID(), "CaseURN123", casesToUnlink).collect(toList());
        final Object formUpdatedObject = eventStream.get(0);
        assertThat(formUpdatedObject, instanceOf(CasesUnlinked.class));
    }

    @Test
    public void shouldProcessLinkStreams() {
        final List<CasesToLink> casesToLink = new ArrayList<>();
        casesToLink.add(CasesToLink.casesToLink()
                .withProsecutionCaseId(randomUUID())
                .withLinkType(LinkType.LINK)
                .build());
        final List<Object> eventStream = caseAggregate.processLinkSplitOrMergeStreams(casesToLink).collect(toList());
        final Object formUpdatedObject = eventStream.get(0);
        assertThat(formUpdatedObject, instanceOf(LinkCases.class));
    }

    @Test
    public void shouldProcessSplitStreams() {
        final List<CasesToLink> casesToLink = new ArrayList<>();
        casesToLink.add(CasesToLink.casesToLink()
                .withProsecutionCaseId(randomUUID())
                .withLinkType(LinkType.SPLIT)
                .build());
        final List<Object> eventStream = caseAggregate.processLinkSplitOrMergeStreams(casesToLink).collect(toList());
        final Object formUpdatedObject = eventStream.get(0);
        assertThat(formUpdatedObject, instanceOf(SplitCases.class));
    }

    @Test
    public void shouldProcessMergeStreams() {
        final List<CasesToLink> casesToLink = new ArrayList<>();
        casesToLink.add(CasesToLink.casesToLink()
                .withProsecutionCaseId(randomUUID())
                .withLinkType(LinkType.MERGE)
                .build());
        final List<Object> eventStream = caseAggregate.processLinkSplitOrMergeStreams(casesToLink).collect(toList());
        final Object formUpdatedObject = eventStream.get(0);
        assertThat(formUpdatedObject, instanceOf(MergeCases.class));
    }

    @Test
    public void shouldMarkHearingsDuplicateOtherThanLatestHearingId() {
        final UUID caseId = randomUUID();
        final UUID hearingIdToLinkWithCase = randomUUID();
        final CaseLinkedToHearing caseLinkedToHearing = CaseLinkedToHearing.caseLinkedToHearing()
                .withCaseId(caseId)
                .withHearingId(hearingIdToLinkWithCase)
                .build();
        final UUID hearingIdToMarkAsDuplicateForCase = randomUUID();
        final UUID defendantId = randomUUID();

        final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase = HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingIdToMarkAsDuplicateForCase)
                .withDefendantIds(asList(defendantId))
                .build();

        MatchDefendant matchDefendant = MatchDefendant.matchDefendant()
                .withDefendantId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withMatchedDefendants(asList(
                        MatchedDefendant.matchedDefendant()
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build(),
                        MatchedDefendant.matchedDefendant()
                                .withCourtProceedingsInitiated(ZonedDateTime.now())
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .build()))
                .build();
        CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.apply(caseLinkedToHearing);
        caseAggregate.apply(hearingMarkedAsDuplicateForCase);

        Stream<Object> objectStream = caseAggregate.matchPartiallyMatchedDefendants(matchDefendant);

        Optional<Object> masterDefendantIdUpdated = objectStream.filter(s -> s instanceof MasterDefendantIdUpdated).findFirst();
        assertThat(masterDefendantIdUpdated.isPresent(), is(true));
        assertThat(masterDefendantIdUpdated.map(s -> (MasterDefendantIdUpdated) s).get().getMatchedDefendants().size(), is(1));
    }

    @Test
    public void shouldGenerateOnlinePleaAllocationAddedEvent() {
        final UUID hearingId = randomUUID();
        final UUID allocationIdOne = randomUUID();
        final UUID allocationIdTwo = randomUUID();
        final Map<UUID, OnlinePleasAllocation> onlinePleaAllocations = new HashMap<>();
        final PleasAllocationDetails pleasAllocationDetails1st = getPleasAllocationDetails(allocationIdOne);
        final PleasAllocationDetails pleasAllocationDetailsSecond = getPleasAllocationDetails(allocationIdTwo);

        setField(caseAggregate, "onlinePleaAllocations", onlinePleaAllocations);

        caseAggregate.addOnlinePleaAllocation(pleasAllocationDetails1st, hearingId);
        caseAggregate.addOnlinePleaAllocation(pleasAllocationDetailsSecond, hearingId);

        assertThat(onlinePleaAllocations.size(), is(2));
        assertPleaAllocationsStoredCorrectly(onlinePleaAllocations, pleasAllocationDetails1st, hearingId);
        assertPleaAllocationsStoredCorrectly(onlinePleaAllocations, pleasAllocationDetailsSecond, hearingId);
    }

    @Test
    public void shouldGenerateOnlinePleaAllocationUpdatedEvent() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Map<UUID, OnlinePleasAllocation> onlinePleaAllocations = new HashMap<>();
        final PleasAllocationDetails pleasAllocationDetails1st = getPleasAllocationDetails(defendantId);
        final OffencePleaDetails offence = getOffencePleaDetails(randomUUID(), "NOT_GUILTY");
        final PleasAllocationDetails updatedPleasAllocation = PleasAllocationDetails.pleasAllocationDetails()
                .withValuesFrom(pleasAllocationDetails1st)
                .withOffencePleas(singletonList(offence)).build();

        setField(caseAggregate, "onlinePleaAllocations", onlinePleaAllocations);

        caseAggregate.addOnlinePleaAllocation(pleasAllocationDetails1st, hearingId);
        caseAggregate.updateOnlinePleaAllocation(updatedPleasAllocation);

        assertThat(onlinePleaAllocations.size(), is(1));
        final OnlinePleasAllocation pleaAllocation = onlinePleaAllocations.get(pleasAllocationDetails1st.getDefendantId());
        assertThat(pleaAllocation.getOffences().get(0).getIndicatedPlea(), is(offence.getIndicatedPlea()));
        assertThat(pleaAllocation.getOffences().get(0).getPleaDate(), is(offence.getPleaDate()));
        assertThat(pleaAllocation.getOffences().get(0).getOffenceId(), is(offence.getOffenceId()));
    }


    @Test
    public void shouldGetOnlinePleasAllocation () {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final Map<UUID, OnlinePleasAllocation> onlinePleaAllocations = new HashMap<>();
        final PleasAllocationDetails pleasAllocationDetails1st = getPleasAllocationDetails(defendantId);

        setField(caseAggregate, "onlinePleaAllocations", onlinePleaAllocations);
        caseAggregate.addOnlinePleaAllocation(pleasAllocationDetails1st, hearingId);

        final OnlinePleasAllocation onlinePleasAllocation = caseAggregate.getOnlinePleasAllocation(defendantId);

        assertThat(defendantId, Matchers.is(onlinePleasAllocation.getDefendantId()));
    }

    private void assertPleaAllocationsStoredCorrectly(final Map<UUID, OnlinePleasAllocation> onlinePleaAllocations,
                                                      final PleasAllocationDetails allocationDetails,
                                                      final UUID hearingId) {
        final OnlinePleasAllocation pleaAllocation = onlinePleaAllocations.get(allocationDetails.getDefendantId());

        assertThat(pleaAllocation.getCaseId(), is(allocationDetails.getCaseId()));
        assertThat(pleaAllocation.getDefendantId(), is(allocationDetails.getDefendantId()));
        assertThat(pleaAllocation.getHearingId(), is(hearingId));
        assertThat(pleaAllocation.getOffences().get(0).getIndicatedPlea(), is(allocationDetails.getOffencePleas().get(0).getIndicatedPlea()));
        assertThat(pleaAllocation.getOffences().get(0).getPleaDate(), is(allocationDetails.getOffencePleas().get(0).getPleaDate()));
        assertThat(pleaAllocation.getOffences().get(0).getOffenceId(), is(allocationDetails.getOffencePleas().get(0).getOffenceId()));
    }

    private PleasAllocationDetails getPleasAllocationDetails(final UUID defendantId) {
        final OffencePleaDetails offence = getOffencePleaDetails(randomUUID(), "GUILTY");
        final List<OffencePleaDetails> offences = Collections.singletonList(offence);

        return PleasAllocationDetails.pleasAllocationDetails()
                .withCaseId(randomUUID())
                .withDefendantId(defendantId)
                .withOffencePleas(offences)
                .build();
    }

    private static OffencePleaDetails getOffencePleaDetails(final UUID id, final String indicatedPlea) {
        return OffencePleaDetails.offencePleaDetails()
                .withOffenceId(id)
                .withIndicatedPlea(indicatedPlea)
                .withPleaDate(LocalDate.now())
                .build();
    }
}
