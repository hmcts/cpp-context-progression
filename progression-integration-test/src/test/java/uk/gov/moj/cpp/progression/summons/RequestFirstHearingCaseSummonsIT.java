package uk.gov.moj.cpp.progression.summons;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.AddDefendantsToCourtProceedings.addDefendantsToCourtProceedings;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.HearingType.hearingType;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.ListHearingRequest.listHearingRequest;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.MaterialHelper.sendEventToConfirmMaterialAdded;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseWithMatchers;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getLanguagePrefix;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getSubjectDateOfBirth;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.sendPublicEventToConfirmHearingForInitiatedCase;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyMaterialRequestRecordedAndExtractMaterialId;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyTemplatePayloadValues;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.Utilities.JsonUtil.toJsonString;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RequestFirstHearingCaseSummonsIT extends AbstractIT {

    private static final String PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED = "progression.event.nows-material-request-recorded";
    private static final String PUBLIC_LISTING_DEFENDANTS_ADDED = "public.listing.new-defendant-added-for-court-proceedings";

    private static final ZonedDateTime FIRST_HEARING_START_TIME = ZonedDateTimes.fromString(ZonedDateTime.now().plusWeeks(2).toString());

    private static final String PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON = "application/vnd.progression.add-defendants-to-court-proceedings+json";

    private String caseId;
    private String caseUrn;
    private boolean personalService;

    private String defendantId1;
    private String offenceId1;
    private String defendantId2;
    private String defendantId3;
    private String defendant1ProsecutionAuthorityReference;
    private String prosecutorEmailAddress;

    private Map<String, List<List<String>>> defendantNameMap;

    private final String prosecutorCost = "£300.00";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED).getMessageConsumerClient();

    public static Stream<Arguments> firstHearingSummonsSpecifications() {
        return Stream.of(
                // summons code, type, template name, youth defendant, number of documents, isWelsh
                Arguments.of("M", "MCA", "MCA", false, 1, false)
        );
    }

    public static Stream<Arguments> firstHearingAddDefendantSummonsSpecifications() {
        return Stream.of(
                // summons code, type, template name, youth defendant, number of documents
                Arguments.of("E", "EW", "EitherWay", false, 1)
        );
    }


    @BeforeAll
    public static void setUpClass() {
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
    }


    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        caseUrn = generateUrn();

        personalService = BOOLEAN.next();
        prosecutorEmailAddress = randomAlphanumeric(20) + "@random.com";

        initialiseDefendantDetails();
    }

    @MethodSource("firstHearingSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateSummonsPayloadForFirstHearingWhenNotSuppressed(final String summonsCode, final String summonsType, final String templateName, final boolean isYouth, final int numberOfDocuments, final boolean isWelsh) throws IOException {
        final boolean summonsSuppressed = false;
        initiateCourtProceedings(getPayloadForInitiatingCourtProceedings(isYouth, summonsCode, summonsSuppressed, FIRST_HEARING_START_TIME, isWelsh));
        verifySummonsGeneratedOnHearingConfirmed(defendantId1, offenceId1, isWelsh, summonsType, templateName, numberOfDocuments, isYouth);

        final UUID materialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        verifyMaterialCreated(materialId.toString());
        sendEventToConfirmMaterialAdded(materialId);

        final List<String> expectedEmailDetails = newArrayList(prosecutorEmailAddress, this.caseUrn, format("%s %s %s, %s", getFirstName(defendantId1), getMiddleName(defendantId1), getLastName(defendantId1), defendant1ProsecutionAuthorityReference));
        verifyEmailNotificationIsRaisedWithoutAttachment(expectedEmailDetails);
        verifyCreateLetterRequested(of("letterUrl", materialId.toString()));

        if (isYouth) {
            final UUID parentMaterialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
            sendEventToConfirmMaterialAdded(parentMaterialId);
            verifyCreateLetterRequested(of("letterUrl", parentMaterialId.toString()));
        }
    }

    @MethodSource("firstHearingAddDefendantSummonsSpecifications")
    @ParameterizedTest
    public void shouldGenerateSummonsForAddedDefendant(final String summonsCode, final String summonsType, final String templateName, final boolean isYouth, final int numberOfDocuments) throws IOException {
        final boolean summonsSuppressed = false;
        final boolean isWelsh = false;
        initiateCourtProceedings(getPayloadForInitiatingCourtProceedings(isYouth, summonsCode, summonsSuppressed, FIRST_HEARING_START_TIME, isWelsh));
        verifySummonsGeneratedOnHearingConfirmed(defendantId1, offenceId1, true, summonsType, templateName, numberOfDocuments, isYouth);

        // Adding second defendant with hearing details different to first defendant
        final String offenceId2 = randomUUID().toString();
        final AddDefendantsToCourtProceedings addSecondDefendantToCourtProceedings = buildAddDefendantToCourtProceedings(caseId, defendantId2, offenceId2, false, FIRST_HEARING_START_TIME.plusWeeks(2), summonsSuppressed, isYouth);
        whenDefendantIsAddedToCase(addSecondDefendantToCourtProceedings);

        verifySummonsGeneratedOnHearingConfirmed(defendantId2, offenceId2, isWelsh, summonsType, templateName, numberOfDocuments, false, isYouth);

        // Adding third defendant with hearing details matching first defendant
        final String offenceId3 = randomUUID().toString();
        final AddDefendantsToCourtProceedings addThirdDefendantToCourtProceedings = buildAddDefendantToCourtProceedings(caseId, defendantId3, offenceId3, false, FIRST_HEARING_START_TIME, summonsSuppressed, isYouth);
        whenDefendantIsAddedToCase(addThirdDefendantToCourtProceedings);

        verifySummonsGeneratedOnHearingConfirmed(defendantId3, offenceId3, isWelsh, summonsType, templateName, numberOfDocuments, true, isYouth);

    }

    private void whenDefendantIsAddedToCase(final AddDefendantsToCourtProceedings payload) throws IOException {
        postCommand(getWriteUrl("/adddefendantstocourtproceedings"),
                PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                toJsonString(payload));
    }

    private void verifySummonsGeneratedOnHearingConfirmed(final String defendantId, final String offenceId, final boolean isWelsh, final String summonsType, final String templateName, final int numberOfDocuments, final Boolean isYouth) {
        verifySummonsGeneratedOnHearingConfirmed(defendantId, offenceId, isWelsh, summonsType, templateName, numberOfDocuments, false, isYouth);
    }

    private void verifySummonsGeneratedOnHearingConfirmed(final String defendantId, final String offenceId, final boolean isWelsh, final String summonsType, final String templateName, final int numberOfDocuments, final boolean existingHearingMatchedForDefendant, final boolean isYouth) {
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id == '" + defendantId + "')].offences[0]", hasSize(greaterThanOrEqualTo(1))),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId == '" + defendantId + "')].hearingIds[0]", hasSize(greaterThanOrEqualTo(1))));

        if (existingHearingMatchedForDefendant) {
            sendPublicEventForConfirmingDefendantAdditionInListing(hearingId, defendantId, isWelsh);
        } else {
            sendPublicEventToConfirmHearingForInitiatedCase(hearingId, defendantId, offenceId, caseId, isWelsh);
            if (!isYouth) {
                verifyProbationHearingCommandInvoked(singletonList(hearingId));
            }
        }

        verifyDocumentAddedToCdes(defendantId, numberOfDocuments);

        final String defendantTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_" + templateName;
        verifyTemplatePayloadValues(true, defendantTemplateName, summonsType, prosecutorCost, personalService, caseUrn, getFirstName(defendantId), getMiddleName(defendantId), getLastName(defendantId));

        if (numberOfDocuments > 1) {
            final String parentTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_FirstHearingParentGuardian";
            verifyTemplatePayloadValues(true, parentTemplateName, summonsType, prosecutorCost, personalService, caseUrn, getParentFirstName(defendantId), getParentMiddleName(defendantId), getParentLastName(defendantId));
        }
    }

    private void verifyDocumentAddedToCdes(final String defendantId, final int numberOfDocuments) {

        final Matcher[] matchers = {
                withJsonPath("$.documentIndices", hasSize(greaterThanOrEqualTo(numberOfDocuments))),
                withJsonPath("$.documentIndices[*].document.name", hasItems("Summons")),
                withJsonPath("$..defendantIds[?(@ =='" + defendantId + "')]", hasSize(greaterThanOrEqualTo(numberOfDocuments)))
        };
        getCourtDocumentsByCaseWithMatchers(USER_ID, caseId, matchers);
    }

    private void sendPublicEventForConfirmingDefendantAdditionInListing(final String hearingId, final String defendantId, final boolean isWelsh) {
        JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("hearingId", hearingId)
                .add("defendantId", defendantId)
                .add("courtCentre", createObjectBuilder()
                        .add("id", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                        .add("name", "court name")
                )
                .add("hearingDateTime", FIRST_HEARING_START_TIME.toString())
                .build();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_DEFENDANTS_ADDED, randomUUID()), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_DEFENDANTS_ADDED, publicEventEnvelope);
    }

    private String getPayloadForInitiatingCourtProceedings(final boolean isYouth, final String summonsCode, final boolean summonsSuppressed, final ZonedDateTime startDateTime, final boolean isWelsh) {
        final String resourceLocation = isYouth ? "progression.command.initiate-court-proceedings-first-hearing-summons-youth.json" : "progression.command.initiate-court-proceedings-first-hearing-summons.json";
        final String defendantDateOfBirth = getSubjectDateOfBirth(isYouth);

        return getPayload(resourceLocation)
                .replaceAll("CASE_ID", caseId)
                .replace("CASE_REFERENCE", caseUrn)
                .replace("SUMMONS_CODE", summonsCode)
                .replaceAll("DEFENDANT_ID", defendantId1)
                .replaceAll("OFFENCE_ID", offenceId1)
                .replace("DEFENDANT_DOB", defendantDateOfBirth)
                .replace("LISTED_START_DATE_TIME", startDateTime.toString())
                .replace("EARLIEST_START_DATE_TIME", startDateTime.toString())
                .replace("PERSONAL_SERVICE", Boolean.toString(personalService))
                .replace("SUMMONS_SUPPRESSED", Boolean.toString(summonsSuppressed))
                .replace("PROSECUTOR_EMAIL", prosecutorEmailAddress)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replace("PARENT_FIRST_NAME", getParentFirstName(defendantId1))
                .replace("PARENT_MIDDLE_NAME", getParentMiddleName(defendantId1))
                .replace("PARENT_LAST_NAME", getParentLastName(defendantId1))
                .replace("FIRST_NAME", getFirstName(defendantId1))
                .replace("MIDDLE_NAME", getMiddleName(defendantId1))
                .replace("LAST_NAME", getLastName(defendantId1))
                .replace("PROSECUTION_AUTHORITY_REFERENCE", defendant1ProsecutionAuthorityReference);
    }


    public AddDefendantsToCourtProceedings buildAddDefendantToCourtProceedings(final String caseId,
                                                                               final String defendantId,
                                                                               final String offenceId,
                                                                               final boolean isWelsh,
                                                                               final ZonedDateTime hearingStartTime,
                                                                               final boolean summonsSuppressed,
                                                                               final boolean isYouth) {

        final List<Defendant> defendantsList = newArrayList();

        final Offence offence = offence()
                .withId(fromString(offenceId))
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 5, 1))
                .withCount(0)
                .build();

        final Person defendantPerson = person()
                .withFirstName(getFirstName(defendantId))
                .withMiddleName(getMiddleName(defendantId))
                .withLastName(getLastName(defendantId))
                .withDateOfBirth(LocalDate.parse(getSubjectDateOfBirth(isYouth)))
                .withGender(MALE)
                .build();

        final Person parentGuardianPerson = person()
                .withFirstName(getParentFirstName(defendantId))
                .withMiddleName(getParentMiddleName(defendantId))
                .withLastName(getParentLastName(defendantId))
                .withGender(MALE)
                .build();

        final Defendant defendant = defendant()
                .withId(fromString(defendantId))
                .withMasterDefendantId(fromString(defendantId))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(fromString(caseId))
                .withOffences(singletonList(offence))
                .withPersonDefendant(personDefendant().withPersonDetails(defendantPerson).build())
                .withAssociatedPersons(singletonList(associatedPerson().withPerson(parentGuardianPerson).build()))
                .withProsecutionAuthorityReference(randomAlphabetic(15))
                .build();
        defendantsList.add(defendant);

        final ListDefendantRequest listDefendantRequest = listDefendantRequest()
                .withProsecutionCaseId(fromString(caseId))
                .withDefendantOffences(singletonList(fromString(offenceId)))
                .withDefendantId(fromString(defendantId))
                .withSummonsRequired(SummonsType.FIRST_HEARING)
                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                        .withProsecutorEmailAddress("test@test.com")
                        .withSummonsSuppressed(summonsSuppressed)
                        .withProsecutorCost(prosecutorCost)
                        .withPersonalService(personalService)
                        .build())
                .build();

        final HearingType hearingType = hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = courtCentre()
                .withId(fromString(isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID))
                .withName("Court Name 5").build();

        final ListHearingRequest listHearingRequest = listHearingRequest()
                .withCourtCentre(courtCentre)
                .withHearingType(hearingType)
                .withJurisdictionType(MAGISTRATES)
                .withListDefendantRequests(singletonList(listDefendantRequest))
                .withEarliestStartDateTime(hearingStartTime)
                .withListedStartDateTime(hearingStartTime)
                .withEstimateMinutes(20)
                .withEstimatedDuration("1 week")
                .build();

        return addDefendantsToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();

    }

    private void initialiseDefendantDetails() {
        defendantId1 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();

        defendant1ProsecutionAuthorityReference = format("TFL%s-ABC", integer(10000, 99999).next());

        final List<String> defendant1Name = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
        final List<String> defendant1ParentName = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));

        final List<String> defendant2Name = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
        final List<String> defendant2ParentName = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));

        final List<String> defendant3Name = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
        final List<String> defendant3ParentName = newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));

        defendantNameMap = ImmutableMap.of(
                defendantId1, newArrayList(defendant1Name, defendant1ParentName),
                defendantId2, newArrayList(defendant2Name, defendant2ParentName),
                defendantId3, newArrayList(defendant3Name, defendant3ParentName)
        );
    }

    private String getFirstName(final String defendantId) {
        return getDefendantNameObject(defendantId).get(0);
    }

    private String getMiddleName(final String defendantId) {
        return getDefendantNameObject(defendantId).get(1);
    }

    private String getLastName(final String defendantId) {
        return getDefendantNameObject(defendantId).get(2);
    }

    private String getParentFirstName(final String defendantId) {
        return getDefendantParentNameObject(defendantId).get(0);
    }

    private String getParentMiddleName(final String defendantId) {
        return getDefendantParentNameObject(defendantId).get(1);
    }

    private String getParentLastName(final String defendantId) {
        return getDefendantParentNameObject(defendantId).get(2);
    }

    private List<String> getDefendantNameObject(final String defendantId) {
        return defendantNameMap.get(defendantId).get(0);
    }

    private List<String> getDefendantParentNameObject(final String defendantId) {
        return defendantNameMap.get(defendantId).get(1);
    }

}