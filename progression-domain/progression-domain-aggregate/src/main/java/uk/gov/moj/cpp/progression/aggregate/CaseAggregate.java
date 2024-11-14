package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.CaseCpsDetailsUpdatedFromCourtDocument.caseCpsDetailsUpdatedFromCourtDocument;
import static uk.gov.justice.core.courts.CaseRetentionPolicyRecorded.caseRetentionPolicyRecorded;
import static uk.gov.justice.core.courts.EditFormRequested.editFormRequested;
import static uk.gov.justice.core.courts.FormCreated.formCreated;
import static uk.gov.justice.core.courts.FormDefendants.formDefendants;
import static uk.gov.justice.core.courts.FormDefendantsUpdated.formDefendantsUpdated;
import static uk.gov.justice.core.courts.FormFinalised.formFinalised;
import static uk.gov.justice.core.courts.FormOperationFailed.formOperationFailed;
import static uk.gov.justice.core.courts.FormUpdated.formUpdated;
import static uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged;
import static uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent.laaDefendantProceedingConcludedResent;
import static uk.gov.justice.core.courts.LockStatus.lockStatus;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.core.courts.ProsecutionCaseDefendantOrganisationUpdatedByLaa.prosecutionCaseDefendantOrganisationUpdatedByLaa;
import static uk.gov.justice.core.courts.UpdatedOrganisation.updatedOrganisation;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.progression.courts.CaseRetentionLengthCalculated.caseRetentionLengthCalculated;
import static uk.gov.justice.progression.courts.HearingEventLogsDocumentCreated.hearingEventLogsDocumentCreated;
import static uk.gov.justice.progression.courts.RetentionPolicy.retentionPolicy;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createCaseAddedToCrownCourt;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createPsrForDefendantsRequested;
import static uk.gov.moj.cpp.progression.aggregate.ProgressionEventFactory.createSendingCommittalHearingInformationAdded;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyPriorityHelper.getRetentionPolicyByPriority;
import static uk.gov.moj.cpp.progression.aggregate.transformers.ProsecutionCaseTransformer.toUpdatedProsecutionCase;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.CourtApplicationHelper.isAddressMatches;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getAllDefendantsOffences;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getDefendant;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getDefendantEmail;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getDefendantJudicialResultsOfDefendantsAssociatedToTheCase;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getDefendantPostcode;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getMasterDefendant;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getUpdatedDefendantsForOnlinePlea;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getUpdatedOffence;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.hasNewAmendment;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.hearingCaseDefendantsProceedingsConcluded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isConcluded;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.isProceedingConcludedEventTriggered;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.offenceWithSexualOffenceReportingRestriction;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.sendEmailNotificationToDefendant;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.updateOrderIndex;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.updatedDefendantsWithProceedingConcludedState;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.GRANTED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.NO_VALUE;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.PENDING;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.REFUSED;
import static uk.gov.moj.cpp.progression.domain.constant.LegalAidStatusEnum.WITHDRAWN;
import static uk.gov.moj.cpp.progression.events.DefendantCustodialEstablishmentRemoved.defendantCustodialEstablishmentRemoved;
import static uk.gov.moj.cpp.progression.events.CivilCaseExists.civilCaseExists;
import static uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated.defendantDefenceOrganisationDisassociated;
import static uk.gov.moj.cpp.progression.events.Reason.PLEA_ALREADY_SUBMITTED;
import static uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType.COMPANYONLINEPLEA;
import static uk.gov.moj.cpp.progression.plea.json.schemas.PleaNotificationType.INDIVIDUALONLINEPLEA;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestrictions;


import java.util.Collection;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllHearingOffencesUpdatedV2;
import uk.gov.justice.core.courts.ApplicationDefendantUpdateRequested;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CaseCpsDetailsUpdatedFromCourtDocument;
import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.CaseDefendantUpdatedWithDriverNumber;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseGroupInfoUpdated;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.CaseMarkersSharedWithHearings;
import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.core.courts.CaseRetentionPolicyRecorded;
import uk.gov.justice.core.courts.Cases;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CpsPersonDefendantDetails;
import uk.gov.justice.core.courts.CpsProsecutorUpdated;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DefendantPartialMatchCreated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Defendants;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.DefendantsAndListingHearingRequestsAdded;
import uk.gov.justice.core.courts.DefendantsNotAddedToCourtProceedings;
import uk.gov.justice.core.courts.EditFormRequested;
import uk.gov.justice.core.courts.DocumentWithProsecutionCaseIdAdded;
import uk.gov.justice.core.courts.ExactMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.FinancialDataAdded;
import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.core.courts.FormCreated;
import uk.gov.justice.core.courts.FormDefendants;
import uk.gov.justice.core.courts.FormDefendantsUpdated;
import uk.gov.justice.core.courts.FormFinalised;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.core.courts.FormUpdated;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.LockStatus;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.PartialMatchedDefendantSearchResultStored;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormDefendantUpdated;
import uk.gov.justice.core.courts.PetFormFinalised;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.core.courts.PetFormReleased;
import uk.gov.justice.core.courts.PetFormUpdated;
import uk.gov.justice.core.courts.PetOperationFailed;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseCreatedInHearing;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantOrganisationUpdatedByLaa;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberDecreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseSubject;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequested;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReapplyMiReportingRestrictions;
import uk.gov.justice.core.courts.ReceiveRepresentationOrderForDefendant;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.UpdatedOrganisation;
import uk.gov.justice.cpp.progression.events.DefendantDefenceAssociationLocked;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.CaseRetentionLengthCalculated;
import uk.gov.justice.progression.courts.CustodyTimeLimitExtended;
import uk.gov.justice.progression.courts.DefendantLegalaidStatusUpdatedV2;
import uk.gov.justice.progression.courts.DefendantsAndListingHearingRequestsStored;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
import uk.gov.justice.progression.courts.HearingEventLogsDocumentCreated;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.progression.courts.HearingRemovedForProsecutionCase;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.OnlinePleaAllocationAdded;
import uk.gov.justice.progression.courts.OnlinePleaAllocationUpdated;
import uk.gov.justice.progression.courts.RelatedCaseRequestedForAdhocHearing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.rules.DartsRetentionPolicyHelper;
import uk.gov.moj.cpp.progression.aggregate.rules.HearingInfo;
import uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicy;
import uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType;
import uk.gov.moj.cpp.progression.command.UpdateMatchedDefendantCustodialInformation;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.command.handler.HandleOnlinePleaDocumentCreation;
import uk.gov.moj.cpp.progression.domain.CaseToUnlink;
import uk.gov.moj.cpp.progression.domain.CasesToLink;
import uk.gov.moj.cpp.progression.domain.MatchDefendant;
import uk.gov.moj.cpp.progression.domain.MatchedDefendant;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.UnmatchDefendant;
import uk.gov.moj.cpp.progression.domain.UnmatchedDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.Form;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.FormLockStatus;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.HearingHelper;
import uk.gov.moj.cpp.progression.domain.event.CaseAlreadyExistsInCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.DefendantBailDocumentCreated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetInvalidated;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetPreviouslyCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.link.LinkType;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.events.CaseNotFound;
import uk.gov.moj.cpp.progression.events.CasesUnlinked;
import uk.gov.moj.cpp.progression.events.CpsDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationAssociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefendantCustodialInformationUpdateRequested;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationDisassociated;
import uk.gov.moj.cpp.progression.events.DefendantLaaAssociated;
import uk.gov.moj.cpp.progression.events.DefendantMatched;
import uk.gov.moj.cpp.progression.events.DefendantNotFound;
import uk.gov.moj.cpp.progression.events.DefendantUnmatched;
import uk.gov.moj.cpp.progression.events.DefendantUnmatchedV2;
import uk.gov.moj.cpp.progression.events.DefendantsMasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.FinanceDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.events.LinkCases;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdated;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedIntoHearings;
import uk.gov.moj.cpp.progression.events.MasterDefendantIdUpdatedV2;
import uk.gov.moj.cpp.progression.events.MatchedDefendants;
import uk.gov.moj.cpp.progression.events.MergeCases;
import uk.gov.moj.cpp.progression.events.NotificationSentForDefendantDocument;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocument;
import uk.gov.moj.cpp.progression.events.NotificationSentForPleaDocumentFailed;
import uk.gov.moj.cpp.progression.events.OnlinePleaCaseUpdateRejected;
import uk.gov.moj.cpp.progression.events.OnlinePleaDocumentUploadedAsCaseMaterial;
import uk.gov.moj.cpp.progression.events.OnlinePleaPcqVisitedRecorded;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.moj.cpp.progression.events.PleaDocumentForOnlinePleaSubmitted;
import uk.gov.moj.cpp.progression.events.RelatedReferenceAdded;
import uk.gov.moj.cpp.progression.events.RelatedReferenceDeleted;
import uk.gov.moj.cpp.progression.events.RepresentationType;
import uk.gov.moj.cpp.progression.events.SplitCases;
import uk.gov.moj.cpp.progression.events.UnlinkedCases;
import uk.gov.moj.cpp.progression.events.ValidateLinkCases;
import uk.gov.moj.cpp.progression.events.ValidateMergeCases;
import uk.gov.moj.cpp.progression.events.ValidateSplitCases;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleasAllocationDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.TemplateType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3776", "squid:MethodCyclomaticComplexity", "squid:S1948", "squid:S3457", "squid:S1192", "squid:CallToDeprecatedMethod", "squid:S1188", "squid:S2384", "pmd:NullAssignment", "squid:S134", "squid:S1312", "squid:S1612", "pmd:NullAssignment"})
public class CaseAggregate implements Aggregate {

    private static final long serialVersionUID = -2092381865833271660L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm a");
    private static final String HEARING_PAYLOAD_PROPERTY = "hearing";
    private static final String CROWN_COURT_HEARING_PROPERTY = "crownCourtHearing";
    protected static final Logger LOGGER = LoggerFactory.getLogger(CaseAggregate.class);
    private static final String CASE_ID = "caseId";
    private static final String SENDING_SHEET_ALREADY_COMPLETED_MSG = "Sending sheet already completed, not allowed to perform add sentence hearing date  for case Id %s ";
    private static final String CASE_STATUS_EJECTED = "EJECTED";
    private static final String LAA_WITHDRAW_STATUS_CODE = "WD";
    public static final String EMAIL_NOT_FOUND = "Email for the prosecutor not found!";

    //Case collections
    private final Set<UUID> caseIdsWithCompletedSendingSheet = new HashSet<>();

    //defendant collections
    private final Set<String> policeDefendantIds = new HashSet<>();
    private final Set<Defendant> defendants = new HashSet<>();
    private final Map<UUID, BailDocument> defendantsBailDocuments = new HashMap<>();
    private final Map<UUID, Person> personOnDefendantAdded = new HashMap<>();
    private final Map<UUID, Person> personForDefendant = new HashMap<>();
    private final Map<UUID, Interpreter> interpreterForDefendant = new HashMap<>();
    private final Map<UUID, String> bailStatusForDefendant = new HashMap<>();
    private final Map<UUID, String> solicitorFirmForDefendant = new HashMap<>();
    private final Map<UUID, List<UUID>> defendantFinancialDocs = new HashMap<>();
    private final Map<UUID, List<Cases>> partialMatchedDefendants = new HashMap<>();
    private final Map<UUID, List<Cases>> exactMatchedDefendants = new HashMap<>();
    private final Map<UUID, String> defendantLegalAidStatus = new HashMap<>();
    private final Set<UUID> matchedDefendantIds = new HashSet<>();
    private final Map<UUID, UUID> defendantAssociatedDefenceOrganisation = new HashMap<>();

    private final Map<UUID, Boolean> defendantOrganisationAsscociatedByRepOrder = new HashMap<>();

    private final Map<UUID, UUID> defendantLAAUpdatedOrganisation = new HashMap<>();

    private final Map<UUID, uk.gov.justice.core.courts.Defendant> defendantsMap = new HashMap<>();
    private final List<uk.gov.justice.core.courts.Defendant> defendantsToBeAdded = new ArrayList<>();
    private final Map<UUID, Form> formMap = new HashMap<>();
    /**
     * Even though this is a case aggregate there is specific scenario we have to make sure that the
     * case status handler defendant related to this case. Case A - Defendant 1 - Master Defendant
     * Id X Case B - Defendant 21 - Master Defendant Id X So due to defendants are related by master
     * defendant id , we need to make sure that each case status depends on its own defendants
     * rather than related cases and their defendants
     */
    private final Map<UUID, Map<UUID, Boolean>> defendantProceedingConcluded = new HashMap<>();


    //hearing collections
    private final Set<UUID> hearingIds = new HashSet<>();
    private final List<ListHearingRequest> listHearingRequestsToBeAdded = new ArrayList<>();

    //offence collections
    private final Map<UUID, List<OffenceForDefendant>> offenceForDefendants = new HashMap<>();
    private final Map<UUID, Set<UUID>> offenceIdsByDefendantId = new HashMap<>();
    private final Map<UUID, List<uk.gov.justice.core.courts.Offence>> defendantCaseOffences = new HashMap<>();
    private final Map<UUID, List<uk.gov.justice.core.courts.Offence>> offenceProceedingConcluded = new HashMap<>();

    private final Map<UUID, Map<UUID, uk.gov.justice.core.courts.Offence>> defendantOffencesResultedOffenceLevel = new HashMap<>();

    //Online pleas Allocation notices
    private final Map<UUID, OnlinePleasAllocation> onlinePleaAllocations = new HashMap<>();

    //other
    private final Map<UUID, LocalDate> custodyTimeLimitForDefendant = new HashMap<>();
    private final Map<UUID, List<UUID>> applicationFinancialDocs = new HashMap<>();

    private final Map<UUID, RetentionPolicy> hearingCaseRetentionMap = new HashMap<>();

    private String caseStatus;
    private String previousNotInactiveCaseStatus;
    private String courtCentreId;
    private String reference;
    private UUID latestHearingId;
    private ProsecutionCase prosecutionCase;
    private boolean hasProsecutionCaseBeenCreated;
    private final List<UUID> petFormFinalisedDocuments = new ArrayList<>();
    private final List<UUID> bcmFormFinalisedDocuments = new ArrayList<>();
    private final List<UUID> ptphFormFinalisedDocuments = new ArrayList<>();
    private static final String FORM_CREATION_COMMAND_NAME = "form-created";
    private static final String FORM_FINALISATION_COMMAND_NAME = "finalise-form";
    private static final String FORM_EDIT_COMMAND_NAME = "edit-form";
    private static final String FORM_UPDATE_COMMAND_NAME = "update-form";
    private static final String MESSAGE_FOR_DUPLICATE_COURT_FORM_ID = "courtFormId already exists";
    private static final String MESSAGE_FOR_PROSECUTION_NULL = "ProsecutionCase(%s) does not exists.";
    private static final String MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT = "courtFormId (%s) does not exists.";
    private static final String UPDATE_BCM_DEFENDANT_OPERATION_IS_FAILED = "update BCM defendant operation is failed";
    public static final String GUILTY = "GUILTY";
    public static final String NOT_GUILTY = "NOT_GUILTY";
    public static final String ENDORSABLE_FLAG = "endorsableFlag";
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";
    private static final String SEXUAL_OFFENCE_RR_LABEL = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    private static final String SEXUAL_OFFENCE_RR_CODE = "YES";



    private static boolean isConcludedAtDefendantLevel(uk.gov.justice.core.courts.Defendant defendant) {
        final boolean isNotConcluded = isNotEmpty(defendant.getOffences()) && defendant.getOffences().stream()
                .anyMatch(offence -> (Boolean.FALSE).equals(offence.getProceedingsConcluded()));
        return !isNotConcluded;
    }

    /**
     * Even though this is a case aggregate there is specific scenario we have to make sure that the
     * case status handler defendant related to this case. Case A - Defendant 1 - Master Defendant
     * Id X Case B - Defendant 21 - Master Defendant Id X So due to defendants are related by master
     * defendant id , we need to make sure that each case status depends on its own defendants
     * rather than related cases and their defendants
     */

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DefendantAdded.class).apply(e -> {
                            this.defendants.add(new Defendant(e.getDefendantId()));
                            this.policeDefendantIds.add(e.getPoliceDefendantId());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(Offence::getId).collect(Collectors.toSet()));
                            if (e.getPerson() != null) {
                                this.personOnDefendantAdded.put(e.getDefendantId(), e.getPerson());
                                this.personForDefendant.put(e.getDefendantId(), e.getPerson());
                            }
                            this.offenceForDefendants.put(e.getDefendantId(), e
                                    .getOffences().stream()
                                    .map(o -> new OffenceForDefendant(o.getId(), o.getCjsCode(), null,
                                            o.getWording(), o.getStartDate(), o.getEndDate(),
                                            0, null, null,
                                            null, o.getChargeDate()))
                                    .collect(toList()));
                        }
                ), when(DefendantUpdated.class).apply(this::updateDefendantRelatedMaps),
                when(OffencesForDefendantUpdated.class).apply(e ->
                        e.getOffences().forEach(o -> {
                            this.offenceForDefendants.put(e.getDefendantId(), e.getOffences());
                            this.offenceIdsByDefendantId.put(
                                    e.getDefendantId(),
                                    e.getOffences().stream().map(OffenceForDefendant::getId).collect(Collectors.toSet()));
                        })
                ),
                when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class)
                        .apply(e -> this.courtCentreId = e.getCourtCentreId()),
                when(CaseEjected.class)
                        .apply(e ->
                                this.caseStatus = CASE_STATUS_EJECTED
                        ),
                when(DefendantUpdated.class).apply(e ->
                        this.defendantsBailDocuments.put(e.getDefendantId(),
                                e.getBailDocument())),
                when(SendingSheetCompleted.class).apply(e ->
                        caseIdsWithCompletedSendingSheet.add(e.getHearing().getCaseId())),
                when(ProsecutionCaseCreated.class)
                        .apply(e -> {
                                    setProsecutionCase(e.getProsecutionCase());
                                    this.caseStatus = e.getProsecutionCase().getCaseStatus();
                                    this.previousNotInactiveCaseStatus = ACTIVE.getDescription();
                                    final ProsecutionCaseIdentifier prosecutionCaseIdentifier = e.getProsecutionCase().getProsecutionCaseIdentifier();
                                    if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
                                        reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                                    }
                                    if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
                                        reference = e.getProsecutionCase().getProsecutionCaseIdentifier().getCaseURN();
                                    }
                                    if (nonNull(e.getProsecutionCase()) && !e.getProsecutionCase().getDefendants().isEmpty()) {
                                        e.getProsecutionCase().getDefendants().forEach(defendant -> {
                                            this.defendantCaseOffences.put(defendant.getId(), getOffencesWithDefaultOrderIndex(defendant.getOffences()));
                                            this.offenceProceedingConcluded.put(defendant.getId(), defendant.getOffences());
                                            this.defendantLegalAidStatus.put(defendant.getId(), NO_VALUE.getDescription());
                                            updateDefendantProceedingConcluded(defendant, false);
                                        });
                                    }
                                    e.getProsecutionCase().getDefendants().forEach(d -> defendantsMap.put(d.getId(), d));
                                }
                        ),
                when(ProsecutionCaseDefendantUpdated.class).apply(e -> defendantsMap.put(e.getDefendant().getId(), updateDefendantFrom(e.getDefendant()))),
                when(ProsecutionCaseUpdateDefendantsWithMatchedRequested.class).apply(e ->
                        defendantsMap.put(e.getDefendant().getId(), updateDefendantFrom(e.getDefendant()))),

                when(ProsecutionCaseOffencesUpdated.class).apply(e -> {
                            if (e.getDefendantCaseOffences().getOffences() != null && !e.getDefendantCaseOffences().getOffences().isEmpty()) {
                                this.defendantCaseOffences.put(e.getDefendantCaseOffences().getDefendantId(), getOffencesWithDefaultOrderIndex(e.getDefendantCaseOffences().getOffences()));
                                this.offenceProceedingConcluded.put(e.getDefendantCaseOffences().getDefendantId(), e.getDefendantCaseOffences().getOffences());
                                this.defendantLegalAidStatus.put(e.getDefendantCaseOffences().getDefendantId(), e.getDefendantCaseOffences().getLegalAidStatus());
                                this.handleProsecutionCaseOffencesUpdated(e);
                            }
                        }
                ),
                when(DefendantsAddedToCourtProceedings.class).apply(
                        e ->
                        {
                            if (!e.getDefendants().isEmpty()) {
                                e.getDefendants().forEach(
                                        defendant -> {
                                            this.defendantCaseOffences.put(defendant.getId(), getOffencesWithDefaultOrderIndex(defendant.getOffences()));
                                            this.offenceProceedingConcluded.put(defendant.getId(), defendant.getOffences());
                                            updateDefendantProceedingConcluded(defendant, false);
                                            this.defendantsMap.putIfAbsent(defendant.getId(), defendant);
                                        });
                            }
                            this.prosecutionCase.getDefendants().addAll(e.getDefendants());
                        }
                ),
                when(CaseLinkedToHearing.class).apply(
                        e -> {
                            this.hearingIds.add(e.getHearingId());
                            this.latestHearingId = e.getHearingId();
                        }
                ),
                when(FinancialDataAdded.class).apply(this::populateFinancialData),
                when(FinancialMeansDeleted.class).apply(this::deleteFinancialData),
                when(HearingResultedCaseUpdated.class).apply(this::updateDefendantProceedingConcludedAndCaseStatus),
                when(HearingConfirmedCaseStatusUpdated.class).apply(this::hearingConfirmedCaseStatusUpdated),
                when(DefendantDefenceOrganisationAssociated.class).apply(this::updateDefendantAssociatedDefenceOrganisation),
                when(DefendantDefenceOrganisationDisassociated.class).apply(this::removeDefendantAssociatedDefenceOrganisation),
                when(DefenceOrganisationDissociatedByDefenceContext.class).apply(this::removeDefendantAssociatedDefenceOrganisation),
                when(DefenceOrganisationAssociatedByDefenceContext.class).apply(this::updateDefendantAssociatedDefenceOrganisation),
                when(HearingMarkedAsDuplicateForCase.class).apply(this::onHearingMarkedAsDuplicateForCase),
                when(HearingDeletedForProsecutionCase.class).apply(this::onHearingDeletedForProsecutionCase),
                when(DefendantMatched.class).apply(
                        e -> this.matchedDefendantIds.add(e.getDefendantId())
                ),
                when(ExactMatchedDefendantSearchResultStored.class).apply(
                        e -> this.exactMatchedDefendants.put(e.getDefendantId(), e.getCases())
                ),
                when(PartialMatchedDefendantSearchResultStored.class).apply(
                        e -> this.partialMatchedDefendants.put(e.getDefendantId(), e.getCases())
                ),
                when(MasterDefendantIdUpdated.class).apply(
                        e -> this.exactMatchedDefendants.remove(e.getDefendantId())
                ),
                when(MasterDefendantIdUpdatedV2.class).apply(
                        e -> {
                            this.exactMatchedDefendants.remove(e.getDefendant().getId());
                            this.defendantsMap.put(e.getDefendant().getId(), updateDefendantFromMatchedDefendant(e));
                        }
                ),
                when(DefendantPartialMatchCreated.class).apply(
                        e -> this.partialMatchedDefendants.remove(e.getDefendantId())
                ),
                when(DefendantsMasterDefendantIdUpdated.class).apply(
                        e -> this.defendantsMap.put(e.getDefendant().getId(), e.getDefendant())
                ),

                when(CaseCpsProsecutorUpdated.class).apply(this::updateProsecutionCaseIdentifier),
                when(HearingRemovedForProsecutionCase.class).apply(this::onHearingRemovedForProsecutionCase),
                when(CustodyTimeLimitExtended.class).apply(this::onCustodyTimeLimitExtended),
                when(DefendantsAndListingHearingRequestsStored.class).apply(
                        e -> {
                            this.defendantsToBeAdded.addAll(e.getDefendants());
                            this.listHearingRequestsToBeAdded.addAll(e.getListHearingRequests());
                        }
                ),
                when(ProsecutionCaseCreatedInHearing.class).apply(e -> this.hasProsecutionCaseBeenCreated = true),
                when(ProsecutionCaseListingNumberUpdated.class).apply(this::handleListingNumberUpdated),
                when(ProsecutionCaseListingNumberIncreased.class).apply(this::handleListingNumberIncreased),
                when(ProsecutionCaseListingNumberDecreased.class).apply(this::handleListingNumberDecreased),
                when(DefendantsAndListingHearingRequestsAdded.class).apply(
                        e -> {
                            this.defendantsToBeAdded.clear();
                            this.listHearingRequestsToBeAdded.clear();
                        }
                ),
                when(LaaDefendantProceedingConcludedChanged.class).apply(
                        e -> {
                            if (!e.getDefendants().isEmpty()) {
                                e.getDefendants().forEach(
                                        defendant ->
                                                this.offenceProceedingConcluded.put(defendant.getId(), defendant.getOffences()));
                            }
                        }
                ),
                when(CaseGroupInfoUpdated.class).apply(
                        e -> this.prosecutionCase = e.getProsecutionCase()
                ),
                when(CaseCpsDetailsUpdatedFromCourtDocument.class).apply(this::handleCaseCpsDetailsUpdatedFromCourtDocument),
                when(CpsDefendantIdUpdated.class).apply(this::handleCpsDefendantIdUpdated),
                when(FormCreated.class).apply(this::onFormCreated),
                when(FormDefendantsUpdated.class).apply(this::onFormDefendantsUpdated),

                when(EditFormRequested.class).apply(this::editFormRequest),
                when(FormUpdated.class).apply(this::updateFormOnFormUpdate),
                when(PetFormReleased.class).apply(this::updateFormOnPetFormRelease),
                when(PetFormDefendantUpdated.class).apply(this::updateFormOnPetFormDefendantUpdated),
                when(PetDetailUpdated.class).apply(this::onPetDetailUpdated),
                when(PetFormCreated.class).apply(this::onPetFormCreated),
                when(PetFormFinalised.class).apply(this::addPetFormFinalisedDocument),
                when(FormFinalised.class).apply(this::addFormFinalisedDocument),
                when(OnlinePleaRecorded.class).apply(this::updateOffenceForOnlinePlea),
                when(ProsecutionCaseDefendantOrganisationUpdatedByLaa.class).apply(e ->
                        this.defendantLAAUpdatedOrganisation.put(e.getDefendantId(), e.getUpdatedOrganisation().getId())
                ),
                when(CaseRetentionPolicyRecorded.class).apply(e ->
                        {
                            final HearingInfo hearingInfo = new HearingInfo(e.getHearingId(), e.getHearingType(), e.getJurisdictionType(),
                                    e.getCourtCentreId(), e.getCourtCentreName(), e.getCourtRoomId(), e.getCourtRoomName());
                            final RetentionPolicy retentionPolicy = new RetentionPolicy(RetentionPolicyType.valueOf(e.getPolicyType()), e.getPeriod(), hearingInfo);
                            this.hearingCaseRetentionMap.put(e.getHearingId(), retentionPolicy);
                        }
                ),
                when(OnlinePleaAllocationAdded.class).apply(this::addOnlinePleaAllocation),
                when(OnlinePleaAllocationUpdated.class).apply(this::updateOnlinePleaAllocation),

                otherwiseDoNothing());

    }

    private List<uk.gov.justice.core.courts.Offence> getOffencesWithDefaultOrderIndex(final List<uk.gov.justice.core.courts.Offence> offences) {
        return offences.stream().map(offence -> uk.gov.justice.core.courts.Offence.offence()
                .withValuesFrom(offence)
                .withOrderIndex(isNull(offence.getOrderIndex()) ? 0 : offence.getOrderIndex())
                .build()).collect(toList());
    }

    private void setProsecutionCase(final ProsecutionCase prosecutionCase) {
        this.prosecutionCase = dedupAllReportingRestrictions(prosecutionCase);
    }


    private void handleCpsDefendantIdUpdated(final CpsDefendantIdUpdated cpsDefendantIdUpdated) {
        this.prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withCpsDefendantId(defendant.getId().equals(cpsDefendantIdUpdated.getDefendantId()) ?
                                        cpsDefendantIdUpdated.getCpsDefendantId() : defendant.getCpsDefendantId())
                                .build())
                        .collect(toList()))
                .build();
    }

    private void updateOffenceForOnlinePlea(final OnlinePleaRecorded onlinePleaRecorded) {
        this.prosecutionCase = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(getUpdatedDefendantsForOnlinePlea(prosecutionCase.getDefendants(), onlinePleaRecorded.getPleadOnline().getDefendantId(), onlinePleaRecorded.getPleadOnline().getOffences()))
                .build();
    }

    private void handleCaseCpsDetailsUpdatedFromCourtDocument(final CaseCpsDetailsUpdatedFromCourtDocument caseCpsDetailsUpdatedFromCourtDocument) {
        this.prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withCpsOrganisation(isNull(prosecutionCase.getCpsOrganisation()) ? caseCpsDetailsUpdatedFromCourtDocument.getCpsOrganisation() : prosecutionCase.getCpsOrganisation())
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withCpsDefendantId(defendant.getId().equals(caseCpsDetailsUpdatedFromCourtDocument.getDefendantId()) &&
                                        !isNull(caseCpsDetailsUpdatedFromCourtDocument.getCpsDefendantId()) ?
                                        caseCpsDetailsUpdatedFromCourtDocument.getCpsDefendantId() : defendant.getCpsDefendantId())
                                .build())
                        .collect(toList()))
                .build();

    }

    private void handleProsecutionCaseOffencesUpdated(final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated) {


        final List<uk.gov.justice.core.courts.Defendant> defendantList = new ArrayList<>();
        for (final uk.gov.justice.core.courts.Defendant defendant : prosecutionCase.getDefendants()) {
            if (defendant.getId().equals(prosecutionCaseOffencesUpdated.getDefendantCaseOffences().getDefendantId())) {

                final Set<uk.gov.justice.core.courts.Offence> offenceSet = new TreeSet<>((o1, o2) -> o1.getId().compareTo(o2.getId()));
                offenceSet.addAll(prosecutionCaseOffencesUpdated.getDefendantCaseOffences().getOffences());
                offenceSet.addAll(defendant.getOffences());

                defendantList.add(uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                        .withOffences(offenceSet.stream().collect(toList()))
                        .build());
            } else {
                defendantList.add(defendant);

            }
        }

        this.prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(defendantList)
                .build();
    }

    private void handleListingNumberDecreased(final ProsecutionCaseListingNumberDecreased prosecutionCaseListingNumberDecreased) {
        final Set<UUID> listingSet = prosecutionCaseListingNumberDecreased.getOffenceIds().stream().collect(Collectors.toSet());

        handleListingNumber(listingSet, -1);
    }

    private void handleListingNumberIncreased(final ProsecutionCaseListingNumberIncreased prosecutionCaseListingNumberIncreased) {
        final Set<UUID> listingSet = prosecutionCaseListingNumberIncreased.getOffenceListingNumbers().stream().map(OffenceListingNumbers::getOffenceId).collect(Collectors.toSet());
        handleListingNumber(listingSet, 1);
    }

    private void handleListingNumber(final Set<UUID> listingSet, int accumulator) {
        this.prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream()
                                        .map(offence -> uk.gov.justice.core.courts.Offence.offence().withValuesFrom(offence)
                                                .withListingNumber((listingSet.contains(offence.getId()) ?
                                                        Integer.valueOf(ofNullable(offence.getListingNumber()).orElse(0) + accumulator) :
                                                        offence.getListingNumber()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();

    }

    private void handleListingNumberUpdated(final ProsecutionCaseListingNumberUpdated prosecutionCaseListingNumberUpdated) {
        final Map<UUID, Integer> listingMap = prosecutionCaseListingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber, Math::max));
        this.prosecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                .withDefendants(prosecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream()
                                        .map(offence -> uk.gov.justice.core.courts.Offence.offence().withValuesFrom(offence)
                                                .withListingNumber(getListingNumber(listingMap, offence))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();

    }

    private Integer getListingNumber(final Map<UUID, Integer> listingMap, final uk.gov.justice.core.courts.Offence offence) {
        final int oldListingNumber = ofNullable(offence.getListingNumber()).orElse(Integer.MIN_VALUE);
        return ofNullable(listingMap.get(offence.getId())).map(listingNumber -> Math.max(listingNumber, oldListingNumber)).orElse(offence.getListingNumber());
    }

    private void updateDefendantProceedingConcluded(final uk.gov.justice.core.courts.Defendant defendant,
                                                    final Boolean proceedingsConcluded) {
        if (nonNull(proceedingsConcluded)) {
            final Map<UUID, Boolean> caseProceedingConcludedMap = defendantProceedingConcluded.get(defendant.getProsecutionCaseId());
            if (caseProceedingConcludedMap != null && !caseProceedingConcludedMap.isEmpty()) {
                caseProceedingConcludedMap.put(defendant.getId(), proceedingsConcluded);
            } else {
                final Map<UUID, Boolean> newDefendantProceedingsConcludedMap = new HashMap<>();
                newDefendantProceedingsConcludedMap.put(defendant.getId(), proceedingsConcluded);
                defendantProceedingConcluded.put(defendant.getProsecutionCaseId(), newDefendantProceedingsConcludedMap);
            }
        }
    }

    private uk.gov.justice.core.courts.Defendant updateDefendantFrom(final DefendantUpdate defendantUpdate) {
        final uk.gov.justice.core.courts.Defendant.Builder builder = uk.gov.justice.core.courts.Defendant.defendant();

        if (defendantsMap.containsKey(defendantUpdate.getId())) {
            builder.withValuesFrom(defendantsMap.get(defendantUpdate.getId()));
        }

        builder.withId(defendantUpdate.getId());
        builder.withMasterDefendantId(defendantUpdate.getMasterDefendantId());
        builder.withProsecutionCaseId(defendantUpdate.getProsecutionCaseId());
        builder.withNumberOfPreviousConvictionsCited(defendantUpdate.getNumberOfPreviousConvictionsCited());
        builder.withProsecutionAuthorityReference(defendantUpdate.getProsecutionAuthorityReference());
        builder.withWitnessStatement(defendantUpdate.getWitnessStatement());
        builder.withWitnessStatementWelsh(defendantUpdate.getWitnessStatementWelsh());
        builder.withMitigation(defendantUpdate.getMitigation());
        builder.withMitigationWelsh(defendantUpdate.getMitigationWelsh());
        builder.withAssociatedPersons(defendantUpdate.getAssociatedPersons());
        builder.withDefenceOrganisation(defendantUpdate.getDefenceOrganisation());
        builder.withPersonDefendant(defendantUpdate.getPersonDefendant());
        builder.withLegalEntityDefendant(defendantUpdate.getLegalEntityDefendant());
        builder.withPncId(defendantUpdate.getPncId());
        builder.withAliases(defendantUpdate.getAliases());
        builder.withIsYouth(defendantUpdate.getIsYouth());

        return builder.build();
    }

    private uk.gov.justice.core.courts.Defendant updateDefendantFromMatchedDefendant(final MasterDefendantIdUpdatedV2 masterDefendantIdUpdatedV2) {
        final uk.gov.justice.core.courts.Defendant.Builder builder = uk.gov.justice.core.courts.Defendant.defendant();

        final uk.gov.justice.core.courts.Defendant defendant = masterDefendantIdUpdatedV2.getDefendant();

        if (defendantsMap.containsKey(defendant.getId())) {
            builder.withValuesFrom(defendantsMap.get(defendant.getId()));
        }

        builder.withId(defendant.getId());
        builder.withMasterDefendantId(isNotEmpty(masterDefendantIdUpdatedV2.getMatchedDefendants())
                ? masterDefendantIdUpdatedV2.getMatchedDefendants().get(0).getMasterDefendantId()
                : defendant.getId());
        builder.withProsecutionCaseId(defendant.getProsecutionCaseId());
        builder.withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited());
        builder.withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference());
        builder.withWitnessStatement(defendant.getWitnessStatement());
        builder.withWitnessStatementWelsh(defendant.getWitnessStatementWelsh());
        builder.withMitigation(defendant.getMitigation());
        builder.withMitigationWelsh(defendant.getMitigationWelsh());
        builder.withAssociatedPersons(defendant.getAssociatedPersons());
        builder.withDefenceOrganisation(defendant.getDefenceOrganisation());
        builder.withPersonDefendant(defendant.getPersonDefendant());
        builder.withLegalEntityDefendant(defendant.getLegalEntityDefendant());
        builder.withPncId(defendant.getPncId());
        builder.withAliases(defendant.getAliases());
        builder.withIsYouth(defendant.getIsYouth());

        return builder.build();
    }

    public Map<UUID, List<UUID>> getApplicationFinancialDocs() {
        return applicationFinancialDocs;
    }

    public Map<UUID, List<UUID>> getDefendantFinancialDocs() {
        return defendantFinancialDocs;
    }

    public List<UUID> getPetFormFinalisedDocuments() {
        return new ArrayList<>(petFormFinalisedDocuments);
    }

    public List<UUID> getBcmFormFinalisedDocuments() {
        return new ArrayList<>(bcmFormFinalisedDocuments);
    }

    public List<UUID> getPtphFormFinalisedDocuments() {
        return new ArrayList<>(ptphFormFinalisedDocuments);
    }

    public Map<UUID, List<uk.gov.justice.core.courts.Offence>> getDefendantCaseOffences() {
        return defendantCaseOffences;
    }

    public Map<UUID, Form> getFormMap() {
        return formMap;
    }


    private void deleteFinancialData(final FinancialMeansDeleted financialMeansDeleted) {
        this.defendantFinancialDocs.remove(financialMeansDeleted.getDefendantId());
    }

    private void updateDefendantProceedingConcludedAndCaseStatus(final HearingResultedCaseUpdated hearingResultedCaseUpdated) {
        final ProsecutionCase pc = hearingResultedCaseUpdated.getProsecutionCase();
        final String currentProsecutionCaseStatus = pc.getCaseStatus();
        hearingResultedCaseUpdated.getProsecutionCase().getDefendants().forEach(defendant -> {
            final List<uk.gov.justice.core.courts.Offence> existingOffences = this.defendantCaseOffences.get(defendant.getId());
            this.defendantCaseOffences.put(defendant.getId(), defendant.getOffences());
            updateDefendantProceedingConcluded(defendant, defendant.getProceedingsConcluded());
            if (existingOffences != null) {
                final List<UUID> updatedDefendantOffences = defendant.getOffences().stream().map(uk.gov.justice.core.courts.Offence::getId).collect(toList());
                final List<uk.gov.justice.core.courts.Offence> noLongerInHearing = existingOffences.stream().filter(x -> !updatedDefendantOffences.contains(x.getId())).collect(toList());
                final List<uk.gov.justice.core.courts.Offence> concludedPlusOngoingOffences = new ArrayList<>();
                concludedPlusOngoingOffences.addAll(defendant.getOffences());
                concludedPlusOngoingOffences.addAll(noLongerInHearing);
                this.defendantCaseOffences.put(defendant.getId(), concludedPlusOngoingOffences);
            } else {
                this.defendantCaseOffences.put(defendant.getId(), defendant.getOffences());
            }

            //capture all the offences that are resulted/actioned with ANY category offence level JudicialResults
            if (isNull(defendantOffencesResultedOffenceLevel.get(defendant.getId()))) {
                defendantOffencesResultedOffenceLevel.put(defendant.getId(), new HashMap<>());
            }
            if (nonNull(defendant.getOffences())) {
                final Map<UUID, uk.gov.justice.core.courts.Offence> previouslyResultedOffenceMap = defendantOffencesResultedOffenceLevel.get(defendant.getId());
                defendant.getOffences().forEach(offence -> {
                    //note: when offence is resulted/amended it will always have isNewAmendment flag set to true
                    if (hasNewAmendment(offence)) {
                        previouslyResultedOffenceMap.put(offence.getId(), offence);
                    }
                });
            }
        });

        if (nonNull(currentProsecutionCaseStatus) && !currentProsecutionCaseStatus.equalsIgnoreCase(INACTIVE.getDescription())) {
            previousNotInactiveCaseStatus = pc.getCaseStatus();
        }

    }

    private void populateFinancialData(final FinancialDataAdded financialDataAdded) {

        if (financialDataAdded.getApplicationId() != null) {
            this.applicationFinancialDocs.put(financialDataAdded.getApplicationId(), financialDataAdded.getMaterialIds());
        } else if (financialDataAdded.getDefendantId() != null) {
            this.defendantFinancialDocs.put(financialDataAdded.getDefendantId(), financialDataAdded.getMaterialIds());
        }
    }

    private void addPetFormFinalisedDocument(final PetFormFinalised petFormFinalised) {
        if (petFormFinalised.getMaterialId() != null) {
            this.petFormFinalisedDocuments.add(petFormFinalised.getMaterialId());
        }
    }

    private void addFormFinalisedDocument(final FormFinalised formFinalised) {
        if (formFinalised.getMaterialId() != null) {
            if (FormType.BCM.equals(formFinalised.getFormType())) {
                this.bcmFormFinalisedDocuments.add(formFinalised.getMaterialId());
            }
            if (FormType.PTPH.equals(formFinalised.getFormType())) {
                this.ptphFormFinalisedDocuments.add(formFinalised.getMaterialId());
            }
        }
    }

    public Stream<Object> addCaseToCrownCourt(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        final String centreId = jsonEnvelope.payloadAsJsonObject().getString("courtCentreId");
        if (centreId.equals(this.courtCentreId)) {
            LOGGER.info("Case already exists in crown court with Id %s", caseId);
            return apply(Stream.of(new CaseAlreadyExistsInCrownCourt(caseId, "Case already exists in crown court with Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createCaseAddedToCrownCourt(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> requestPsrForDefendant(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform requestPsrForDefendant  for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform requestPsrForDefendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createPsrForDefendantsRequested(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendingHearingCommittal(final JsonEnvelope jsonEnvelope) {
        final UUID caseId = fromString(jsonEnvelope.payloadAsJsonObject().getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform sending hearing committal  for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform sending hearing committal after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(createSendingCommittalHearingInformationAdded(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addSentenceHearingDate(final UUID caseId, final LocalDate sentenceHearingDate) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(new SentenceHearingDateAdded(sentenceHearingDate, caseId));
        return apply(streamBuilder.build());
    }

    public Stream<Object> addConvictionDateToOffence(final UUID caseId,
                                                     final UUID offenceId, final LocalDate convictionDate) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
        return apply(Stream.of(ConvictionDateAdded.builder()
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build()));
    }

    public Stream<Object> removeConvictionDateFromOffence(final UUID caseId, final UUID offenceId) {
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info(SENDING_SHEET_ALREADY_COMPLETED_MSG, caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add sentence hearing date after sending sheet completed with case Id " + caseId)));
        }
        return apply(Stream.of(ConvictionDateRemoved.builder()
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> addDocument(final CourtDocument courtDocument) {
        LOGGER.debug("Court document being added");

        final Stream.Builder<Object> streamBuilder = prepareEventsForAddDocument(courtDocument);

        return apply(streamBuilder.build());
    }

    public Stream.Builder<Object> prepareEventsForAddDocument(final CourtDocument courtDocument) {

        final Stream.Builder<Object> streamBuilder = builder();

        streamBuilder.add(DocumentWithProsecutionCaseIdAdded.documentWithProsecutionCaseIdAdded()
                .withCourtDocument(courtDocument)
                .withProsecutionCase(prosecutionCase)
                .build());

        return streamBuilder;
    }


    private Map<UUID, Set<UUID>> sendingSheetToDefendantOffenceMap(final JsonObject payload) {
        final JsonArray jsonDefendants = payload.getJsonObject(HEARING_PAYLOAD_PROPERTY).getJsonArray("defendants");
        final Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = new HashMap<>();
        if (jsonDefendants != null) {
            jsonDefendants.forEach(
                    jd -> {
                        final JsonObject jdo = (JsonObject) jd;
                        final UUID defendantId = fromString(jdo.getString("id"));
                        final JsonArray jsonOffences = ((JsonObject) jd).getJsonArray("offences");
                        final Set<UUID> offenceIds = jsonOffences.stream().map(jo -> (fromString(((JsonObject) jo).getString("id")))).collect(Collectors.toSet());
                        incomingDefendantId2OffenceIds.put(defendantId, offenceIds);
                    }
            );
        }
        return incomingDefendantId2OffenceIds;
    }

    public Stream<Object> completeSendingSheet(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final UUID caseId =
                fromString(payload
                        .getJsonObject(HEARING_PAYLOAD_PROPERTY)
                        .getString(CASE_ID));
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.error("Sending sheet already completed for case with id: {}", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "Sending sheet already completed for case with id " + caseId)));
        }

        if (defendants.isEmpty()) {
            LOGGER.error("CaseId:{} doesn't exist for sending sheet.", caseId);
            return apply(Stream.of(new SendingSheetInvalidated(caseId, "Invalid Case Id")));
        }

        final String incomingCourtCentreId = payload.getJsonObject(CROWN_COURT_HEARING_PROPERTY).getString("courtCentreId");

        if (incomingCourtCentreId == null || !incomingCourtCentreId.equals(courtCentreId)) {
            LOGGER.error("CourtCentreId mismatch for case with id: {}. courtCentreId:{}, incomingCourtCentreId:{}", caseId, courtCentreId, incomingCourtCentreId);
            return apply(Stream.of(new SendingSheetInvalidated(caseId, format("CourtCentreId mismatch. courtCentreId:%s, incomingCourtCentreId:%s", courtCentreId, incomingCourtCentreId))));
        }

        final Map<UUID, Set<UUID>> incomingDefendantId2OffenceIds = sendingSheetToDefendantOffenceMap(payload);

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Set<UUID> defendantUUIDs = defendants.stream().map(Defendant::getId).collect(Collectors.toSet());
        final Function<Set<UUID>, String> uuidsToString = uuids ->
                uuids == null ? null : uuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        if (!incomingDefendantId2OffenceIds.keySet().equals(defendantUUIDs)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("CaseId: {}: invalid sending sheet defendant ids specified do not match history: ({}) / ({}) ", caseId,
                        uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs));
            }
            return apply(Stream.of(new SendingSheetInvalidated(caseId, format("defendant ids specified do not match history: (%s) / (%s) ",
                    uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs)))));

        }
        for (final UUID defendantUUID : defendantUUIDs) {

            if (defendantsBailDocuments.get(defendantUUID) != null) {
                streamBuilder.add(new DefendantBailDocumentCreated(caseId,
                        defendantUUID,
                        defendantsBailDocuments.get(defendantUUID).getMaterialId(),
                        defendantsBailDocuments.get(defendantUUID).getId()));
            }

            final Set<UUID> incomingOffenceIds = incomingDefendantId2OffenceIds.get(defendantUUID);
            final Set<UUID> offenceIds = offenceIdsByDefendantId.get(defendantUUID);
            if (!incomingOffenceIds.equals(offenceIds)) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("CaseId: %s: invalid sending sheet, offence ids for DefendantId: %s ids specified do not match history: ({}) / ({}) ", caseId, defendantUUID,
                            uuidsToString.apply(incomingDefendantId2OffenceIds.keySet()), uuidsToString.apply(defendantUUIDs));
                }
                return apply(Stream.of(new SendingSheetInvalidated(caseId, format("invalid sending sheet, offence ids for DefendantId: %s ids specified do not match history: (%s) / (%s) ",
                        defendantUUID, uuidsToString.apply(incomingOffenceIds), uuidsToString.apply(offenceIds)))));
            }
        }

        streamBuilder.add(ProgressionEventFactory.completedSendingSheet(jsonEnvelope));
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateOffencesForDefendant(final OffencesForDefendantUpdated offencesForDefendantUpdated) {
        final UUID caseId = offencesForDefendantUpdated.getCaseId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform update offences for defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform update offences for defendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(offencesForDefendantUpdated);
        return apply(streamBuilder.build());
    }

    public Stream<Object> addDefendant(final AddDefendant addDefendantCommand) {
        final UUID defendantId = addDefendantCommand.getDefendantId();
        final UUID caseId = addDefendantCommand.getCaseId();
        final String caseUrn = addDefendantCommand.getCaseUrn();
        final String policeDefendantId = addDefendantCommand.getPoliceDefendantId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform add defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform add defendant after sending sheet completed with case Id " + caseId)));
        }
        if (offenceIdsByDefendantId.containsKey(defendantId)
                || policeDefendantIds.contains(policeDefendantId)) {
            LOGGER.error("Defendant already exists with ID: {} or PoliceDefendantId: {}",
                    defendantId, policeDefendantId);
            return apply(Stream.of(
                    new DefendantAdditionFailed(caseId.toString(), defendantId.toString(),
                            "Add Defendant failed as defendant already exists")));
        }

        return apply(Stream.of(new DefendantAdded(addDefendantCommand.getCaseId(),
                addDefendantCommand.getDefendantId(), addDefendantCommand.getPerson(),
                addDefendantCommand.getPoliceDefendantId(),
                addDefendantCommand.getOffences(), caseUrn)));
    }

    public Stream<Object> updateDefendant(
            final UpdateDefendantCommand updateDefendantCommandCommand) {
        final UUID caseId = updateDefendantCommandCommand.getCaseId();
        if (caseIdsWithCompletedSendingSheet.contains(caseId)) {
            LOGGER.info("Sending sheet already completed, not allowed to perform update defendant for case Id %s ", caseId);
            return apply(Stream.of(new SendingSheetPreviouslyCompleted(caseId, "not allowed to perform update defendant after sending sheet completed with case Id " + caseId)));
        }
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (!this.offenceIdsByDefendantId
                .containsKey(updateDefendantCommandCommand.getDefendantId())) {
            streamBuilder.add(new DefendantUpdateFailed(
                    updateDefendantCommandCommand.getCaseId().toString(),
                    updateDefendantCommandCommand.getDefendantId().toString(),
                    "Defendant not foind for the case"));
        } else {
            final BailDocument bailDocument = (updateDefendantCommandCommand.getDocumentId() == null
                    ? null
                    : new BailDocument(randomUUID(),
                    updateDefendantCommandCommand.getDocumentId()));
            streamBuilder.add(new DefendantUpdated(updateDefendantCommandCommand.getCaseId(),
                    updateDefendantCommandCommand.getDefendantId(),
                    updateDefendantCommandCommand.getPerson(), bailDocument,
                    updateDefendantCommandCommand.getInterpreter(),
                    updateDefendantCommandCommand.getBailStatus(),
                    updateDefendantCommandCommand.getCustodyTimeLimitDate(),
                    updateDefendantCommandCommand.getDefenceSolicitorFirm()));
        }
        return apply(streamBuilder.build());

    }

    public Stream<Object> ejectCase(final UUID prosecutionCaseId, final String removalReason) {
        if (CASE_STATUS_EJECTED.equals(caseStatus)) {
            LOGGER.info("Case with id {} already ejected", prosecutionCaseId);
            return empty();
        }
        return apply(Stream.of(CaseEjected.caseEjected()
                .withProsecutionCaseId(prosecutionCaseId).withRemovalReason(removalReason).build()));
    }

    private void updateDefendantRelatedMaps(final DefendantUpdated e) {
        if (e.getPerson() != null) {
            this.personForDefendant.put(e.getDefendantId(), e.getPerson());
        }
        if (e.getInterpreter() != null) {
            this.interpreterForDefendant.put(e.getDefendantId(), e.getInterpreter());
        }
        if (e.getBailDocument() != null) {
            this.defendantsBailDocuments.put(e.getDefendantId(), new BailDocument(
                    e.getBailDocument().getId(), e.getBailDocument().getMaterialId()));
        }
        if (e.getBailStatus() != null) {
            this.bailStatusForDefendant.put(e.getDefendantId(), e.getBailStatus());
        }
        if (e.getCustodyTimeLimitDate() != null) {
            this.custodyTimeLimitForDefendant.put(e.getDefendantId(), e.getCustodyTimeLimitDate());
        }
        if (e.getDefenceSolicitorFirm() != null) {
            this.solicitorFirmForDefendant.put(e.getDefendantId(), e.getDefenceSolicitorFirm());
        }
    }

    private uk.gov.justice.core.courts.Defendant getUpdatedDefendantWithIsYouth(final uk.gov.justice.core.courts.Defendant defendant, final List<ListHearingRequest> listHearingRequests) {
        if (nonNull(defendant.getPersonDefendant())) {
            final LocalDate earliestHearingDate = HearingHelper.getEarliestListedStartDateTime(listHearingRequests).toLocalDate();
            return uk.gov.justice.core.courts.Defendant.defendant()
                    .withValuesFrom(defendant)
                    .withIsYouth(nonNull(defendant.getPersonDefendant().getPersonDetails()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()) && LocalDateUtils.isYouth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), earliestHearingDate))
                    .build();

        }
        return defendant;
    }

    @SuppressWarnings("squid:S2589")
    public Stream<Object> createProsecutionCase(final ProsecutionCase prosecutionCase) {
        LOGGER.debug("Prosecution case is being referred To Court .");
        if (nonNull(this.prosecutionCase) && nonNull(this.prosecutionCase.getIsCivil())
                && this.prosecutionCase.getIsCivil() && isNull(this.prosecutionCase.getGroupId())) {
            return apply(Stream.of(
                    civilCaseExists()
                            .withCaseUrn(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                            .withProsecutionCaseId(prosecutionCase.getId())
                            .build())
            );
        }
        if (null != exactMatchedDefendants) {
           final List<uk.gov.justice.core.courts.Defendant> defendantList = prosecutionCase.getDefendants().stream().filter(x -> exactMatchedDefendants.containsKey(x.getId())).collect(toList());
            defendantList.stream().forEach(x -> DefendantHelper.getUpdatedDefendantWithMasterDefendantId(prosecutionCase, x, transformToExactMatchedDefendants(exactMatchedDefendants.get(x.getId()))));
        }
        return apply(Stream.of(ProsecutionCaseCreated.prosecutionCaseCreated().withProsecutionCase(prosecutionCase).build()));
    }

    public Stream<Object> defendantsAddedToCourtProceedings(final List<uk.gov.justice.core.courts.Defendant> addedDefendants,
                                                            final List<ListHearingRequest> listHearingRequests,
                                                            final Optional<List<JsonObject>> offencesJsonObjectOptional) {
        LOGGER.debug("Defendants Added To CourtProceedings");

        final List<uk.gov.justice.core.courts.Defendant> newDefendantsList =
                addedDefendants.stream().filter(d -> !this.defendantCaseOffences.containsKey(d.getId())).collect(toMap(uk.gov.justice.core.courts.Defendant::getId, d -> d, (i, d) -> d)).values().stream().collect(toList());

        if (newDefendantsList.isEmpty()) {
            return apply(Stream.of(DefendantsNotAddedToCourtProceedings.defendantsNotAddedToCourtProceedings()
                    .withDefendants(addedDefendants)
                    .withListHearingRequests(listHearingRequests)
                    .build())
            );
        }
        final List<uk.gov.justice.core.courts.Defendant> updatedDefendantsWithYouthFlag = newDefendantsList.stream().map(defendant -> getUpdatedDefendantWithIsYouth(defendant, listHearingRequests)).collect(toList());

        final List<uk.gov.justice.core.courts.Defendant> defendantListWithMasterDefendants = updatedDefendantsWithYouthFlag.stream().filter(x -> exactMatchedDefendants.containsKey(x.getId())).collect(toList());

        final List<uk.gov.justice.core.courts.Defendant> updatedDefendantsWitMasterDefendantIdsSet = defendantListWithMasterDefendants.stream().filter(x -> getMasterDefendant(transformToExactMatchedDefendants(exactMatchedDefendants.get(x.getId()))) != null).map(x ->
                uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(x).withMasterDefendantId(getMasterDefendant(transformToExactMatchedDefendants(exactMatchedDefendants.get(x.getId()))).getMasterDefendantId()
                ).build()).collect(toList());
        final List<UUID> updatedDefendantIds = updatedDefendantsWitMasterDefendantIdsSet.stream().map(x -> x.getId()).collect(toList());
        updatedDefendantsWithYouthFlag.removeIf(x -> updatedDefendantIds.contains(x.getId()));
        updatedDefendantsWithYouthFlag.addAll(updatedDefendantsWitMasterDefendantIdsSet);
        updatedDefendantsWithYouthFlag.forEach(defendantWithYouthFlag -> populateReportingRestrictionsForOffences(offencesJsonObjectOptional, defendantWithYouthFlag, listHearingRequests));
        return apply(Stream.of(DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
                .withDefendants(updatedDefendantsWithYouthFlag)
                .withListHearingRequests(listHearingRequests)
                .build())
        );
    }

    /**
     * Stores defendants and list hearing requests (to be added to hearing) in aggregate if the
     * prosecution case has not been created in hearing context - event
     * DefendantsAndListingHearingRequestsStored. Releases defendants and list hearing requests (to
     * be added to hearing) in aggregate if the prosecution case has already been created in hearing
     * context - event DefendantsAndListingHearingRequestsAdded.
     *
     * @param defendants
     * @param listHearingRequests
     * @return
     */
    public Stream<Object> addOrStoreDefendantsAndListingHearingRequests(final List<uk.gov.justice.core.courts.Defendant> defendants, final List<ListHearingRequest> listHearingRequests) {

        if (hasProsecutionCaseBeenCreated) {
            return apply(Stream.of(DefendantsAndListingHearingRequestsAdded.defendantsAndListingHearingRequestsAdded()
                    .withDefendants(defendants)
                    .withListHearingRequests(listHearingRequests)
                    .build())
            );
        }

        return apply(Stream.of(DefendantsAndListingHearingRequestsStored.defendantsAndListingHearingRequestsStored()
                .withDefendants(defendants)
                .withListHearingRequests(listHearingRequests)
                .build())
        );

    }

    /**
     * This method is invoked after the prosecution case has been created in hearing context - event
     * DefendantsAndListingHearingRequestsAdded. Releases any defendants and list hearing requests
     * (to be added to hearing) stored in aggregate to hearing context - event
     * DefendantsAndListingHearingRequestsAdded.
     *
     * @param prosecutionCaseId
     * @return
     */
    public Stream<Object> createProsecutionCaseInHearing(final UUID prosecutionCaseId) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(ProsecutionCaseCreatedInHearing.prosecutionCaseCreatedInHearing()
                .withProsecutionCaseId(prosecutionCaseId)
                .build());

        if (isNotEmpty(this.defendantsToBeAdded) && isNotEmpty(this.listHearingRequestsToBeAdded)) {

            final List<uk.gov.justice.core.courts.Defendant> copyOfDefendantsToBeAdded = new ArrayList<>(this.defendantsToBeAdded);
            final List<ListHearingRequest> copyOfListHearingRequestsToBeAdded = new ArrayList<>(this.listHearingRequestsToBeAdded);

            streamBuilder.add(DefendantsAndListingHearingRequestsAdded.defendantsAndListingHearingRequestsAdded()
                    .withDefendants(copyOfDefendantsToBeAdded)
                    .withListHearingRequests(copyOfListHearingRequestsToBeAdded)
                    .build());
        }

        return apply(streamBuilder.build());

    }

    public Stream<Object> updateDefendantDetails(final DefendantUpdate updatedDefendant, final List<UUID> allHearingIdsForCase) {
        LOGGER.debug("Defendant information is being updated.");
        final Stream.Builder<Object> builder = builder();
        // keep title if new title is blank.
        final uk.gov.justice.core.courts.Defendant orgDefendant = defendantsMap.get(updatedDefendant.getId());
        final Optional<String> orgTitle = ofNullable(orgDefendant)
                .map(uk.gov.justice.core.courts.Defendant::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(uk.gov.justice.core.courts.Person::getTitle);

        final Optional<String> newTitle = ofNullable(updatedDefendant)
                .map(DefendantUpdate::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(uk.gov.justice.core.courts.Person::getTitle);

        final DefendantUpdate newDefendant;
        if (newTitle.isPresent() || !orgTitle.isPresent() || updatedDefendant.getPersonDefendant() == null) {
            newDefendant = updatedDefendant;
        } else {
            newDefendant = DefendantUpdate.defendantUpdate().withValuesFrom(updatedDefendant)
                    .withPersonDefendant(PersonDefendant.personDefendant().withValuesFrom(updatedDefendant.getPersonDefendant())
                            .withPersonDetails(uk.gov.justice.core.courts.Person.person().withValuesFrom(updatedDefendant.getPersonDefendant().getPersonDetails())
                                    .withTitle(orgTitle.orElse(null))
                                    .build())
                            .build())
                    .build();
        }

        final List<UUID> filteredApplicationHearingIds = allHearingIdsForCase.stream()
                                        .filter(id->!hearingIds.contains(id))
                                        .collect(Collectors.toList());
        if (isNotEmpty(filteredApplicationHearingIds)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("filteredApplicationHearingIds for which event raised : {}",filteredApplicationHearingIds);
            }
            filteredApplicationHearingIds.forEach(hearingId ->
                    builder.add(ApplicationDefendantUpdateRequested.applicationDefendantUpdateRequested()
                            .withDefendant(newDefendant)
                            .withHearingId(hearingId)
                            .build())
            );
        }
        builder.add(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(newDefendant)
                .withHearingIds(CollectionUtils.isNotEmpty(hearingIds) ? new ArrayList<>(hearingIds) : emptyList())
                .build());

        if (shouldUpdateCustodialEstablishment(newDefendant.getId(), newDefendant.getPersonDefendant(), defendantsMap)) {

            final DefendantCustodialInformationUpdateRequested.Builder defendantCustodialInformationUpdateRequestedBuilder = DefendantCustodialInformationUpdateRequested.defendantCustodialInformationUpdateRequested()
                    .withDefendantId(newDefendant.getId())
                    .withMasterDefendantId(newDefendant.getMasterDefendantId())
                    .withProsecutionCaseId(newDefendant.getProsecutionCaseId());

            if (nonNull(newDefendant.getPersonDefendant()) && nonNull(newDefendant.getPersonDefendant().getCustodialEstablishment())) {
                defendantCustodialInformationUpdateRequestedBuilder.withCustodialEstablishment(uk.gov.moj.cpp.progression.events.CustodialEstablishment.custodialEstablishment()
                        .withName(newDefendant.getPersonDefendant().getCustodialEstablishment().getName())
                        .withId(newDefendant.getPersonDefendant().getCustodialEstablishment().getId())
                        .withCustody(newDefendant.getPersonDefendant().getCustodialEstablishment().getCustody()).build());
            }

            builder.add(defendantCustodialInformationUpdateRequestedBuilder.build());
        }

        return apply(builder.build());
    }

    public Stream<Object> updateDefendantAddress(final DefendantUpdate updatedDefendant) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Defendant Address to be changed, defendantId : {}",updatedDefendant.getMasterDefendantId());
        }
        final uk.gov.justice.core.courts.Defendant orgDefendant = defendantsMap.get(updatedDefendant.getId());

        final uk.gov.justice.core.courts.Defendant enrichedDefendant  ;
        if (isOrganisationAddressToBeUpdated(orgDefendant, updatedDefendant)){
            LOGGER.debug("Updating Organisation Defendant Address , defendantId : {}",updatedDefendant.getMasterDefendantId());
            enrichedDefendant = uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(orgDefendant)
                    .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                            .withValuesFrom(orgDefendant.getLegalEntityDefendant())
                            .withOrganisation(organisation()
                                    .withValuesFrom(orgDefendant.getLegalEntityDefendant().getOrganisation())
                                    .withAddress(updatedDefendant.getLegalEntityDefendant().getOrganisation().getAddress())
                                    .build())
                            .build())
                    .build();
            return updateDefendantDetails(buildDefendantUpdateFromDefendant(enrichedDefendant), asList());
        } else if (isPersonAddressToBeUpdated(orgDefendant, updatedDefendant)){
            LOGGER.debug("Updating Person Defendant Address , defendantId : {}",updatedDefendant.getMasterDefendantId());
            enrichedDefendant = uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(orgDefendant)
                    .withPersonDefendant(PersonDefendant.personDefendant()
                            .withValuesFrom(orgDefendant.getPersonDefendant())
                            .withPersonDetails(uk.gov.justice.core.courts.Person.person()
                                    .withValuesFrom(orgDefendant.getPersonDefendant().getPersonDetails())
                                    .withAddress(updatedDefendant.getPersonDefendant().getPersonDetails().getAddress()).build()
                            ).build()).build();
            return updateDefendantDetails(buildDefendantUpdateFromDefendant(enrichedDefendant), asList());
        }
        return apply(empty());
    }

    private boolean isOrganisationAddressToBeUpdated(uk.gov.justice.core.courts.Defendant orgDefendant, DefendantUpdate updatedDefendant) {
        final Optional<Address> orgLeAddress = ofNullable(orgDefendant)
                .map(uk.gov.justice.core.courts.Defendant::getLegalEntityDefendant)
                .map(LegalEntityDefendant::getOrganisation)
                .map(Organisation::getAddress);
        final Optional<Address> newLeAddress = ofNullable(updatedDefendant)
                .map(DefendantUpdate::getLegalEntityDefendant)
                .map(LegalEntityDefendant::getOrganisation)
                .map(Organisation::getAddress);

        return orgLeAddress.isPresent() && newLeAddress.isPresent() &&  !isAddressMatches(orgLeAddress.get(),newLeAddress.get());
    }

    private boolean  isPersonAddressToBeUpdated(uk.gov.justice.core.courts.Defendant orgDefendant, DefendantUpdate updatedDefendant) {
        final Optional<Address> orgAddress = ofNullable(orgDefendant)
                .map(uk.gov.justice.core.courts.Defendant::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(uk.gov.justice.core.courts.Person::getAddress);
        final Optional<Address> newAddress = ofNullable(updatedDefendant)
                .map(DefendantUpdate::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(uk.gov.justice.core.courts.Person::getAddress);

        return orgAddress.isPresent() && newAddress.isPresent() && !isAddressMatches(orgAddress.get(),newAddress.get());
    }

    public Stream<Object> updateDefendantCustodialInformationDetails(final UpdateMatchedDefendantCustodialInformation updateMatchedDefendantCustodialInformation) {
        LOGGER.info("Defendant Custodial Information to be updated for defendants {} and case {}", updateMatchedDefendantCustodialInformation.getDefendants(), updateMatchedDefendantCustodialInformation.getCaseId());
        final Stream.Builder<Object> builder = builder();
        final UUID caseId = updateMatchedDefendantCustodialInformation.getCaseId();

        for (final UUID defendantId : updateMatchedDefendantCustodialInformation.getDefendants()) {
            LOGGER.info("CaseAggregateInfo: updateDefendantCustodialInformationDetails case {} defendant {} : in loop", caseId, defendantId);

            if (!defendantsMap.containsKey(defendantId)
                    || (nonNull(defendantProceedingConcluded.get(caseId)) && defendantProceedingConcluded.get(caseId).get(defendantId))) {

                if (!defendantsMap.containsKey(defendantId)) {
                    LOGGER.info("CaseAggregateInfo: updateDefendantCustodialInformationDetails case {} defendant {} condition failed [defendantsMap.containsKey()]", caseId, defendantId);
                }

                if (nonNull(defendantProceedingConcluded.get(caseId)) && defendantProceedingConcluded.get(caseId).get(defendantId)) {
                    LOGGER.info("CaseAggregateInfo: updateDefendantCustodialInformationDetails defendant {} : defendantProceedingConcluded.get(caseId).get(defendantId) is true", defendantId);
                }
                continue;
            }

            final uk.gov.justice.core.courts.Defendant originalDefendant = defendantsMap.get(defendantId);

            final PersonDefendant.Builder updatedPersonDefendantBuilder = PersonDefendant.personDefendant();
            updatedPersonDefendantBuilder
                    .withValuesFrom(originalDefendant.getPersonDefendant())
                    .withCustodialEstablishment(null);
            if (nonNull(updateMatchedDefendantCustodialInformation.getCustodialEstablishment())) {
                updatedPersonDefendantBuilder.withCustodialEstablishment(buildCoreCourtCustodialEstablishment(updateMatchedDefendantCustodialInformation.getCustodialEstablishment()));
            }
            final PersonDefendant updatedPersonDefendant = updatedPersonDefendantBuilder.build();
            final uk.gov.justice.core.courts.Defendant updatedDefendant = uk.gov.justice.core.courts.Defendant.defendant()
                    .withValuesFrom(originalDefendant)
                    .withMasterDefendantId(updateMatchedDefendantCustodialInformation.getMasterDefendantId())
                    .withPersonDefendant(updatedPersonDefendant)
                    .build();

            if (shouldUpdateCustodialEstablishment(defendantId, updatedPersonDefendant, defendantsMap)) {
                final DefendantUpdate newDefendantUpdate = buildDefendantUpdateFromDefendant(updatedDefendant);

                LOGGER.info("Prosecution case defendant updated for defendant {} with caseId {} ", newDefendantUpdate.getId(), newDefendantUpdate.getProsecutionCaseId());
                builder.add(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                        .withDefendant(newDefendantUpdate)
                        .withHearingIds(CollectionUtils.isNotEmpty(hearingIds) ? new ArrayList<>(this.hearingIds) : emptyList())
                        .build());
            }
        }
        return apply(builder.build());
    }


    public Stream<Object> removeDefendantCustodialEstablishment(final UUID masterDefendantId, final UUID defendantId, final UUID caseId, final List<UUID> allHearingIdsForCase) {
        final Stream.Builder<Object> builder = builder();

        final Optional<uk.gov.justice.core.courts.Defendant> defendantOptional = defendantsMap
                .values()
                .stream()
                .filter(defendant -> masterDefendantId.equals(defendant.getMasterDefendantId()))
                .findAny();

        if (defendantOptional.isPresent()) {
            final uk.gov.justice.core.courts.Defendant defendant = defendantOptional.get();
            final PersonDefendant personDefendant = PersonDefendant.personDefendant()
                    .withValuesFrom(defendant.getPersonDefendant())
                    .withCustodialEstablishment(null)
                    .build();

            final uk.gov.justice.core.courts.Defendant newDefendant = uk.gov.justice.core.courts.Defendant.defendant()
                    .withValuesFrom(defendant)
                    .withPersonDefendant(personDefendant)
                    .build();

            builder.add(defendantCustodialEstablishmentRemoved()
                    .withMasterDefendantId(masterDefendantId)
                    .withDefendantId(defendantId)
                    .withProsecutionCaseId(caseId)
                    .build());

            final List<UUID> filteredApplicationHearingIds = allHearingIdsForCase.stream()
                    .filter(id->!hearingIds.contains(id))
                    .collect(Collectors.toList());
            if (isNotEmpty(filteredApplicationHearingIds)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("filteredApplicationHearingIds for which event raised : {}",filteredApplicationHearingIds);
                }
                filteredApplicationHearingIds.forEach(hearingId ->
                        builder.add(ApplicationDefendantUpdateRequested.applicationDefendantUpdateRequested()
                                .withDefendant(buildDefendantUpdateFromDefendant(newDefendant))
                                .withHearingId(hearingId)
                                .build())
                );
            }

            builder.add(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                    .withDefendant(buildDefendantUpdateFromDefendant(newDefendant))
                    .withHearingIds(CollectionUtils.isNotEmpty(hearingIds) ? new ArrayList<>(this.hearingIds) : emptyList())
                    .build());

        }

        return apply(builder.build());
    }

    public Stream<Object> updateDefendantWithDriverNumber(final UUID defendantId, final UUID caseId, final String driverNumber) {
        final uk.gov.justice.core.courts.Defendant orgDefendant = defendantsMap.get(defendantId);
        final uk.gov.justice.core.courts.Defendant updatedDefendant = uk.gov.justice.core.courts.Defendant.defendant()
                .withValuesFrom(orgDefendant)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withValuesFrom(orgDefendant.getPersonDefendant())
                        .withDriverNumber(driverNumber)
                        .build())
                .build();

        final DefendantUpdate newDefendant = getDefendantUpdateFromDefendant(updatedDefendant);

        return apply(Stream.of(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                        .withDefendant(newDefendant)
                        .withHearingIds(CollectionUtils.isNotEmpty(hearingIds) ? new ArrayList<>(this.hearingIds) : emptyList())
                        .build(),
                CaseDefendantUpdatedWithDriverNumber.caseDefendantUpdatedWithDriverNumber()
                        .withDefendantId(defendantId)
                        .withProsecutionCaseId(caseId)
                        .withDriverNumber(driverNumber)
                        .build()));

    }

    /**
     * If All the offences of a defendant was given a final result then the proceedings of the
     * defendant is complete. All defendant proceedings completed then Case Status is INACTIVE If
     * anyone of the defendants proceedings not completed then use incoming new case status (current
     * caseStatus is not INACTIVE) If anyone of the defendants proceedings not completed and when
     * current caseStatus is INACTIVE then use Previous Case Status(previousNotInactiveCaseStatus)
     *
     * @param prosecutionCase
     * @param defendantJudicialResults
     * @param courtCentre
     * @param hearingId
     * @param hearingType
     * @param jurisdictionType
     * @param isBoxHearing
     * @param remitResultIds
     * @return Stream<Object>
     */
    public Stream<Object> updateCase(final ProsecutionCase prosecutionCase, final List<DefendantJudicialResult> defendantJudicialResults,
                                     final CourtCentre courtCentre, final UUID hearingId, final String hearingType,
                                     final JurisdictionType jurisdictionType, final Boolean isBoxHearing, final List<String> remitResultIds) {

        LOGGER.debug(" ProsecutionCase is being updated ");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final List<uk.gov.justice.core.courts.Defendant> defendantListForProceedingsConcludedEventTrigger = new ArrayList<>();
        final List<uk.gov.justice.core.courts.Defendant> updatedDefendantsForProceedingsConcludedEvent = new ArrayList<>();

        updatedDefendantsWithProceedingConcludedState(prosecutionCase, offenceProceedingConcluded, updatedDefendantsForProceedingsConcludedEvent, defendantJudicialResults);

        updatedDefendantsForProceedingsConcludedEvent.forEach(defendant -> {
            final uk.gov.justice.core.courts.Defendant updatedDefendant =
                    uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant).withProceedingsConcluded(isConcludedAtDefendantLevel(defendant)).build();
            if (isProceedingConcludedEventTriggered(defendant, offenceProceedingConcluded, defendantProceedingConcluded.get(prosecutionCase.getId()))) {
                defendantListForProceedingsConcludedEventTrigger.add(updatedDefendant);
            }
        });

        if (isNotEmpty(defendantListForProceedingsConcludedEventTrigger)) {
            final UUID resultedHearingId = hearingId != null ? hearingId : latestHearingId;
            streamBuilder.add(laaDefendantProceedingConcludedChanged()
                    .withDefendants(defendantListForProceedingsConcludedEventTrigger)
                    .withHearingId(resultedHearingId)
                    .withProsecutionCaseId(prosecutionCase.getId())
                    .build());
        }

        final String updatedCaseStatus = getUpdatedCaseStatus(prosecutionCase);
        final ProsecutionCase updatedProsecutionCase = toUpdatedProsecutionCase(prosecutionCase,
                updateDefendantWithProceedingsConcludedStatusAndOriginalListingNumbers(prosecutionCase),
                updatedCaseStatus);

        streamBuilder.add(HearingResultedCaseUpdated.hearingResultedCaseUpdated()
                .withProsecutionCase(updatedProsecutionCase)
                .build());

        if (JurisdictionType.CROWN == jurisdictionType && !TRUE.equals(isBoxHearing)) {
            final HearingInfo hearingInfo = new HearingInfo(hearingId, hearingType, jurisdictionType.name(),
                    courtCentre.getId(), courtCentre.getName(), courtCentre.getRoomId(), courtCentre.getRoomName());
            final List<uk.gov.justice.core.courts.Offence> defendantsOffences = getAllDefendantsOffences(prosecutionCase.getDefendants());
            final List<DefendantJudicialResult> defendantJudicialResultsOfDefendants = getDefendantJudicialResultsOfDefendantsAssociatedToTheCase(prosecutionCase.getDefendants(), defendantJudicialResults);
            final DartsRetentionPolicyHelper dartsRetentionPolicyHelper = new DartsRetentionPolicyHelper(hearingInfo, defendantsOffences, defendantJudicialResultsOfDefendants, remitResultIds);

            final RetentionPolicy retentionPolicy = dartsRetentionPolicyHelper.getRetentionPolicy();
            LOGGER.info("Case Retention policy={} calculated for CaseURN={} with caseStatus={}", retentionPolicy, getCaseURN(prosecutionCase.getProsecutionCaseIdentifier()), updatedCaseStatus);

            final RetentionPolicy caseRetentionPolicyByHearing = getCaseRetentionPolicyByHearing(hearingId, retentionPolicy);
            hearingCaseRetentionMap.put(hearingId, caseRetentionPolicyByHearing);

            streamBuilder.add(getCaseRetentionPolicyRecorded(getCaseURN(prosecutionCase.getProsecutionCaseIdentifier()), courtCentre, hearingId, hearingType, jurisdictionType, caseRetentionPolicyByHearing));
        }

        if (INACTIVE.getDescription().equalsIgnoreCase(updatedCaseStatus) && !hearingCaseRetentionMap.isEmpty()) {
            final RetentionPolicy retentionPolicy = getRetentionPolicyByPriority(hearingCaseRetentionMap.values());
            streamBuilder.add(getCaseRetentionLengthCalculatedEvent(getCaseURN(prosecutionCase.getProsecutionCaseIdentifier()),
                    updatedCaseStatus, retentionPolicy));
            streamBuilder.add(getHearingEventLogsDocumentCreated(prosecutionCase.getId()).build());
        }

        return apply(streamBuilder.build());
    }

    private CaseRetentionPolicyRecorded getCaseRetentionPolicyRecorded(final String caseURN, final CourtCentre courtCentre,
                                                                       final UUID hearingId, final String hearingType, final JurisdictionType jurisdictionType,
                                                                       final RetentionPolicy caseRetentionPolicyByHearing) {
        return caseRetentionPolicyRecorded()
                .withCaseURN(caseURN)
                .withHearingId(hearingId)
                .withHearingType(hearingType)
                .withJurisdictionType(jurisdictionType.name())
                .withCourtCentreId(courtCentre.getId())
                .withCourtCentreName(courtCentre.getName())
                .withCourtRoomId(courtCentre.getRoomId())
                .withCourtRoomName(courtCentre.getRoomName())
                .withPolicyType(caseRetentionPolicyByHearing.getPolicyType().name())
                .withPeriod(caseRetentionPolicyByHearing.getPeriod())
                .build();
    }

    public Stream<Object> getHearingEventLogsDocuments(final UUID caseId, final Optional<UUID> applicationId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if(applicationId.isPresent()) {
            streamBuilder.add(getHearingEventLogsDocumentCreated(caseId)
                    .withApplicationId(applicationId.get()).build());
        } else {
            streamBuilder.add(getHearingEventLogsDocumentCreated(caseId).build());
        }
        return apply(streamBuilder.build());
    }

    private HearingEventLogsDocumentCreated.Builder getHearingEventLogsDocumentCreated(final UUID caseId) {
        return hearingEventLogsDocumentCreated()
                .withCaseId(caseId);
    }

    private String getCaseURN(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isNotEmpty(prosecutionCaseIdentifier.getCaseURN())
                ? prosecutionCaseIdentifier.getCaseURN()
                : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    private RetentionPolicy getCaseRetentionPolicyByHearing(final UUID hearingId, final RetentionPolicy retentionPolicy) {
        if (isNull(hearingCaseRetentionMap.get(hearingId))) {
            return retentionPolicy;
        } else {
            //if retentionPolicy already exists for hearingId, then replace previous retentionPolicy with new as per the priority order
            final RetentionPolicy previouslyCalculatedRetention = hearingCaseRetentionMap.get(hearingId);
            if (previouslyCalculatedRetention.getPolicyType().getPriority() > retentionPolicy.getPolicyType().getPriority()) {
                return previouslyCalculatedRetention;
            } else if (previouslyCalculatedRetention.getPolicyType().getPriority() == retentionPolicy.getPolicyType().getPriority() && (
                    previouslyCalculatedRetention.getPolicyType() == RetentionPolicyType.CUSTODIAL ||
                            previouslyCalculatedRetention.getPolicyType() == RetentionPolicyType.REMITTAL)) {
                //calculate the max retention period and set
                return getRetentionPolicyByPriority(Arrays.asList(previouslyCalculatedRetention, retentionPolicy));
            } else {
                return retentionPolicy;
            }
        }
    }

    private CaseRetentionLengthCalculated getCaseRetentionLengthCalculatedEvent(final String caseUrn, final String updatedCaseStatus,
                                                                                final RetentionPolicy retentionPolicy) {

        final HearingInfo hearingInfo = retentionPolicy.getHearingInfo();

        return caseRetentionLengthCalculated()
                .withCaseURN(caseUrn)
                .withCaseStatus(updatedCaseStatus)
                .withJurisdictionType(hearingInfo.getJurisdictionType())
                .withCourtCentreId(hearingInfo.getCourtCentreId())
                .withCourtCentreName(hearingInfo.getCourtCentreName())
                .withCourtRoomId(hearingInfo.getCourtRoomId())
                .withCourtRoomName(hearingInfo.getCourtRoomName())
                .withHearingType(hearingInfo.getHearingType())
                .withRetentionPolicy(retentionPolicy()
                        .withPolicyType(retentionPolicy.getPolicyType().getPolicyCode())
                        .withPeriod(retentionPolicy.getPeriod()).build())
                .build();
    }

    /**
     * Resend LAA defendant proceedings concluded outcome to LAA using BDF in case LAA system is not updated.
     *
     * @param laaDefendantProceedingConcludedChangedList List of laaDefendantProceedingConcludedChanged events
     * @return Stream of events
     */
    public Stream<Object> resendLaaOutcomeConcluded(final List<LaaDefendantProceedingConcludedChanged> laaDefendantProceedingConcludedChangedList) {
        final Stream.Builder<Object> builder = Stream.builder();
        laaDefendantProceedingConcludedChangedList.stream()
                .map(laaDefendantProceedingConcludedChanged -> laaDefendantProceedingConcludedResent().withLaaDefendantProceedingConcludedChanged(laaDefendantProceedingConcludedChanged).build())
                .forEach(builder::add);
        return builder.build();
    }

    /**
     * Retrieves the defendants and related proceedings concluded status for a given case Retrieves
     * other defendants for a given case from defendantProceedingConcluded Map after filtering the
     * case defendants from previous step IF all defendant proceedings completed for all defendants
     * including other case defendants then Case Status is INACTIVE If anyone of the defendants
     * proceedings not completed then case status is Previous Case Status (when current caseStatus
     * is INACTIVE) If anyone of the defendants proceedings not completed then use latest case
     * status (current caseStatus is not INACTIVE)
     *
     * @param prosecutionCase
     * @return String
     */
    @SuppressWarnings("squid:S2159")
    private String getUpdatedCaseStatus(final ProsecutionCase prosecutionCase) {
        final Map<UUID, Boolean> caseDefendantsFromDefendantProceedingsConcluded = defendantProceedingConcluded.get(prosecutionCase.getId());

        final List<UUID> caseDefendantsFromCurrentHearing = prosecutionCase.getDefendants().stream()
                .map(uk.gov.justice.core.courts.Defendant::getId)
                .collect(Collectors.toList());

        final Map<UUID, Boolean> otherCaseDefendantsNotRepresentedOnCurrentHearing = getOtherCaseDefendantsNotRepresentedOnCurrentHearing(caseDefendantsFromDefendantProceedingsConcluded, caseDefendantsFromCurrentHearing);

        final String currentProsecutionCaseStatus = prosecutionCase.getCaseStatus();
        String updatedCaseStatus = currentProsecutionCaseStatus;

        final boolean currentHearingProceedingsConcluded = hearingCaseDefendantsProceedingsConcluded(prosecutionCase);

        // Check for each defendant from the Map of defendantProceedingConcluded if defendant are concluded
        final boolean isAllDefendantsOffencesConcludedAsFinal = isAllOffencesForEachDefendantResultedFinal(prosecutionCase);
        final boolean otherDefendantProceedingConcluded = isOtherCaseDefendantsProceedingsConcludedValue(otherCaseDefendantsNotRepresentedOnCurrentHearing);

        if (isAllDefendantsOffencesConcludedAsFinal && otherDefendantProceedingConcluded) {
            updatedCaseStatus = INACTIVE.getDescription();
        }

        if (nonNull(currentProsecutionCaseStatus) && !currentHearingProceedingsConcluded
                && INACTIVE.getDescription().equalsIgnoreCase(currentProsecutionCaseStatus) && nonNull(previousNotInactiveCaseStatus)) {
            updatedCaseStatus = previousNotInactiveCaseStatus;
        }
        return updatedCaseStatus;
    }

    /**
     * Function to check if all defendants offences on this case are resulted to final
     *
     * @param prosecutionCase The incoming input payload
     * @return true if all offences for each defendant are resulted FINAL on this case
     */
    private boolean isAllOffencesForEachDefendantResultedFinal(final ProsecutionCase prosecutionCase) {
        final Map<UUID, Boolean> defendantsConcludedMap = defendantProceedingConcluded.get(prosecutionCase.getId());
        if (isNull(defendantsConcludedMap) || defendantsConcludedMap.isEmpty()) {
            // Execution should not arrive here.
            return false;
        }
        //Check for each defendant
        final Set<UUID> defendantsIds = defendantsConcludedMap.keySet();
        for (final UUID defId : defendantsIds) {
            final List<uk.gov.justice.core.courts.Offence> defendantOffencesFromPayload = getCurrentDefendantOffencesFromProsecutionCase(prosecutionCase, defId);

            final List<uk.gov.justice.core.courts.Offence> updatedOffences = new ArrayList<>();
            updateCurrentOffencesWithProceedingsConcludedStatus(defId, defendantOffencesFromPayload, updatedOffences);
            final boolean isDefendantProceedingConcluded = checkIfDefendantProceedingsConcluded(defId, updatedOffences);

            if (!isDefendantProceedingConcluded) {
                return false;
            }
        }
        return true;
    }

    private uk.gov.justice.core.courts.Offence getOffenceById(final UUID offenceId,
                                                              final List<uk.gov.justice.core.courts.Offence> offencesList) {
        return Optional.ofNullable(offencesList)
                .flatMap(currentHearingOffenceList -> currentHearingOffenceList.stream()
                        .filter(offences -> offences.getId().equals(offenceId))
                        .findAny())
                .orElse(null);
    }

    /**
     * Returns the list of offences from the payload by defendant ID
     *
     * @param prosecutionCase The incoming input payload
     * @param defId           The Defendant ID
     * @return The list of offences from the payload by defendant ID
     */
    private List<uk.gov.justice.core.courts.Offence> getCurrentDefendantOffencesFromProsecutionCase(final ProsecutionCase prosecutionCase, final UUID defId) {
        return prosecutionCase.getDefendants().stream()
                .filter(def -> def.getId().equals(defId))
                .findAny()
                .map(uk.gov.justice.core.courts.Defendant::getOffences)
                .orElse(emptyList());
    }

    public Stream<Object> updateOffences(final List<uk.gov.justice.core.courts.Offence> offences, final UUID prosecutionCaseId, final UUID defendantId, final Optional<List<JsonObject>> referenceDataOffences) {
        LOGGER.debug("Offences information is being updated.");
        final AtomicInteger maxOrderIndex = new AtomicInteger(Collections.max(this.defendantCaseOffences.get(defendantId), Comparator.comparing(uk.gov.justice.core.courts.Offence::getOrderIndex)).getOrderIndex());
        final List<uk.gov.justice.core.courts.Offence> newOffences = new ArrayList<>();

        final List<uk.gov.justice.core.courts.Offence> allOffences = offences.stream().map(commandOffence -> {
            uk.gov.justice.core.courts.Offence offence;
            Optional<uk.gov.justice.core.courts.Offence> existingOffence = this.defendantCaseOffences.get(defendantId).stream().filter(o -> o.getId().equals(commandOffence.getId())).findFirst();
            if (existingOffence.isPresent()) {
                offence = updateOrderIndex(commandOffence, existingOffence.get().getOrderIndex(), referenceDataOffences);
            } else {
                offence = updateLaaApplicationReference(defendantId,
                        offenceWithSexualOffenceReportingRestriction(
                                updateOrderIndex(commandOffence, maxOrderIndex.addAndGet(1), referenceDataOffences), referenceDataOffences));
                newOffences.add(offence);
            }
            return offence;
        }).collect(toList());

        final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                .withOffences(allOffences)
                .withDefendantId(defendantId)
                .withLegalAidStatus(defendantLegalAidStatus.get(defendantId))
                .withProsecutionCaseId(prosecutionCaseId)
                .build();
        final Stream.Builder<Object> streamBuilder = builder();
        streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(newDefendantCaseOffences).build());

        if (!hearingIds.isEmpty()) {
            final AllHearingOffencesUpdatedV2.Builder allHearingOffencesUpdatedV2 = AllHearingOffencesUpdatedV2.allHearingOffencesUpdatedV2();
            allHearingOffencesUpdatedV2.withHearingIds(new ArrayList<>(hearingIds)).
                    withDefendantId(defendantId).build();
            final List<uk.gov.justice.core.courts.Offence> updatedOffences = allOffences.stream().filter(offence -> ! newOffences.contains(offence)).collect(toList());
            if(! updatedOffences.isEmpty()){
                allHearingOffencesUpdatedV2.withUpdatedOffences(updatedOffences);
            }
            if(! newOffences.isEmpty()){
                allHearingOffencesUpdatedV2.withNewOffences(newOffences);
            }

            streamBuilder.add(allHearingOffencesUpdatedV2.build());
        }

        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantChanged(allOffences, this.defendantCaseOffences.get(defendantId), prosecutionCaseId, defendantId, referenceDataOffences);
            offencesForDefendantChanged.ifPresent(streamBuilder::add);
        }
        return apply(streamBuilder.build());

    }

    public Stream<Object> updateOffences(final List<uk.gov.justice.core.courts.Offence> updatedOffences, final List<uk.gov.justice.core.courts.Offence> existingOffences,
                                         final UUID prosecutionCaseId, final UUID defendantId, final Optional<List<JsonObject>> referenceDataOffences) {

        LOGGER.info("Offences courtCentre is being updated for convicting court.");
        final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                .withOffences(updatedOffences)
                .withDefendantId(defendantId)
                .withLegalAidStatus(defendantLegalAidStatus.get(defendantId))
                .withProsecutionCaseId(prosecutionCaseId)
                .build();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                .withDefendantCaseOffences(newDefendantCaseOffences).build());
        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper
                    .getOffencesForDefendantChanged(updatedOffences, existingOffences, prosecutionCaseId, defendantId, referenceDataOffences);
            offencesForDefendantChanged.ifPresent(streamBuilder::add);
        }
        LOGGER.info("Offences courtCentre update being applied.");
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateCaseMarkers(final List<Marker> caseMarkers, final UUID prosecutionCaseId, final UUID hearingId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(CaseMarkersUpdated.caseMarkersUpdated()
                .withCaseMarkers(caseMarkers)
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearingId(hearingId)
                .build()
        );
        if (!hearingIds.isEmpty()) {
            streamBuilder.add(CaseMarkersSharedWithHearings.caseMarkersSharedWithHearings()
                    .withCaseMarkers(caseMarkers)
                    .withHearingIds(new ArrayList<>(hearingIds))
                    .withProsecutionCaseId(prosecutionCaseId)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> updateCaseStatus(final ProsecutionCase prosecutionCase, final String caseStatus) {
        LOGGER.debug(" ProsecutionCase  updateCaseStatus is being updated ");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(HearingConfirmedCaseStatusUpdated.hearingConfirmedCaseStatusUpdated()
                .withProsecutionCase(prosecutionCase)
                .withCaseStatus(caseStatus)
                .build());
        return apply(streamBuilder.build());

    }

    public Stream<Object> recordPrintRequest(final UUID caseId,
                                             final UUID notificationId,
                                             final UUID materialId,
                                             boolean postage) {
        return apply(Stream.of(new PrintRequested(notificationId, null, caseId, materialId, postage)));
    }

    public Stream<Object> recordEmailRequest(final UUID caseId,
                                             final UUID materialId,
                                             final List<Notification> notifications) {
        return apply(Stream.of(new EmailRequested(caseId, materialId, null, notifications)));
    }

    public Stream<Object> recordNotificationRequestFailure(final UUID caseId,
                                                           final UUID notificationId,
                                                           final ZonedDateTime failedTime,
                                                           final String errorMessage,
                                                           final Optional<Integer> statusCode) {
        return apply(Stream.of(new NotificationRequestFailed(caseId, null, null, notificationId, failedTime, errorMessage, statusCode)));
    }

    public Stream<Object> recordNotificationRequestSuccess(final UUID caseId,
                                                           final UUID notificationId,
                                                           final ZonedDateTime sentTime,
                                                           final ZonedDateTime completedAt) {
        return apply(Stream.of(new NotificationRequestSucceeded(caseId, null, null, notificationId, sentTime, completedAt)));
    }

    public Stream<Object> recordNotificationRequestAccepted(final UUID caseId,
                                                            final UUID materialId,
                                                            final UUID notificationId,
                                                            final ZonedDateTime acceptedTime) {
        return apply(Stream.of(new NotificationRequestAccepted(caseId, null, materialId, notificationId, acceptedTime)));
    }

    public Stream<Object> addConvictionDate(final UUID prosecutionCaseId, final UUID offenceId, final LocalDate convictionDate) {
        return apply(Stream.of(uk.gov.justice.core.courts.ConvictionDateAdded.convictionDateAdded()
                .withCaseId(prosecutionCaseId)
                .withConvictionDate(convictionDate)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> removeConvictionDate(final UUID prosecutionCaseId, final UUID offenceId) {
        return apply(Stream.of(uk.gov.justice.core.courts.ConvictionDateRemoved.convictionDateRemoved()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .build()));
    }

    public Stream<Object> linkProsecutionCaseToHearing(final UUID hearingId, final UUID caseId) {
        return apply(Stream.of(CaseLinkedToHearing.caseLinkedToHearing()
                .withHearingId(hearingId).withCaseId(caseId).build()));
    }

    public Stream<Object> addFinancialMeansData(final UUID prosecutionCaseId, final UUID defendantId, final UUID applicationId, final Material material) {

        final List<UUID> materialIds = new ArrayList<>();
        materialIds.add(material.getId());
        return apply(Stream.of(FinancialDataAdded.financialDataAdded()
                .withCaseId(prosecutionCaseId)
                .withApplicationId(applicationId)
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .build())
        );
    }

    public Stream<Object> deleteFinancialMeansData(final UUID defendantId, final UUID caseId) {
        final List<UUID> materialIds = this.defendantFinancialDocs.get(defendantId);
        return apply(Stream.of(FinancialMeansDeleted.financialMeansDeleted()
                .withDefendantId(defendantId)
                .withMaterialIds(materialIds)
                .withCaseId(caseId)
                .build())
        );

    }

    public Stream<Object> recordLAAReferenceForOffence(final UUID prosecutionCaseId, final UUID defendantId, final UUID offenceId, final LaaReference laaReference) {
        LOGGER.debug("LAA reference is recorded for Offence.");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final List<uk.gov.justice.core.courts.Offence> offencesList = defendantCaseOffences.get(defendantId);
            final List<uk.gov.justice.core.courts.Offence> updatedOffenceList = new ArrayList<>(offencesList);
            final Optional<uk.gov.justice.core.courts.Offence> optionalOffence = offencesList.stream().filter(offence -> offence.getId().equals(offenceId)).findAny();
            if (optionalOffence.isPresent()) {
                final uk.gov.justice.core.courts.Offence matchingOffence = optionalOffence.get();
                updatedOffenceList.remove(matchingOffence);
                final uk.gov.justice.core.courts.Offence updatedOffence = uk.gov.justice.core.courts.Offence.offence()
                        .withValuesFrom(matchingOffence)
                        .withLaaApplnReference(laaReference)
                        .build();
                updatedOffenceList.add(updatedOffence);
                final String legalAidStatus = getDefendantLevelLegalStatus(updatedOffenceList);
                final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                        .withOffences(updatedOffenceList)
                        .withDefendantId(defendantId)
                        .withLegalAidStatus(legalAidStatus)
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build();
                final DefendantLegalaidStatusUpdatedV2 defendantLegalaidStatusUpdated = DefendantLegalaidStatusUpdatedV2.defendantLegalaidStatusUpdatedV2() //Not used on listener
                        .withCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withLegalAidStatus(legalAidStatus)
                        .withLaaContractNumber(laaReference.getLaaContractNumber())
                        .build();

                final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantUpdated(Arrays.asList(updatedOffence), offencesList, prosecutionCaseId, defendantId);
                if (offencesForDefendantChanged.isPresent()) {
                    streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                            .withDefendantCaseOffences(newDefendantCaseOffences).build());
                    streamBuilder.add(offencesForDefendantChanged.get()); //Not used on listener
                    streamBuilder.add(defendantLegalaidStatusUpdated);
                }

                if (LAA_WITHDRAW_STATUS_CODE.equalsIgnoreCase(laaReference.getStatusCode()) && !GRANTED.getDescription().equals(legalAidStatus)
                        && this.defendantAssociatedDefenceOrganisation.containsKey(defendantId)) {
                    //progression.event.defendant-defence-organisation-disassociated not used in Listener
                    final DefendantDefenceOrganisationDisassociated defendantDefenceOrganisationDisassociated = defendantDefenceOrganisationDisassociated()
                            .withDefendantId(defendantId)
                            .withOrganisationId(defendantAssociatedDefenceOrganisation.get(defendantId))
                            .withCaseId(prosecutionCaseId)
                            .build();
                    streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                            .withProsecutionCaseId(prosecutionCaseId)
                            .withDefendantId(defendantId)
                            .build());
                    streamBuilder.add(defendantDefenceOrganisationDisassociated);
                    streamBuilder.add(DefendantDefenceAssociationLocked.defendantDefenceAssociationLocked()
                            .withDefendantId(defendantId)
                            .withProsecutionCaseId(prosecutionCaseId)
                            .withLockedByRepOrder(false)
                            .build());
                }
            }
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveRepresentationOrderForDefendant(final ReceiveRepresentationOrderForDefendant receiveRepresentationOrderForDefendant, final LaaReference laaReference,
                                                                 final OrganisationDetails organisationDetails, final String associatedOrganisationId) {
        LOGGER.debug("Receive Representation Order for Defendant.");
        final UUID defendantId = receiveRepresentationOrderForDefendant.getDefendantId();
        final UUID prosecutionCaseId = receiveRepresentationOrderForDefendant.getProsecutionCaseId();
        final UUID offenceId = receiveRepresentationOrderForDefendant.getOffenceId();
        final String laaContractNumber = receiveRepresentationOrderForDefendant.getDefenceOrganisation().getLaaContractNumber();
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {
            final List<uk.gov.justice.core.courts.Offence> offencesList = defendantCaseOffences.get(defendantId);
            final List<uk.gov.justice.core.courts.Offence> updatedOffenceList = new ArrayList<>(offencesList);
            final Optional<uk.gov.justice.core.courts.Offence> optionalOffence = offencesList.stream().filter(offence -> offence.getId().equals(offenceId)).findAny();
            if (optionalOffence.isPresent()) {
                final uk.gov.justice.core.courts.Offence matchingOffence = optionalOffence.get();
                updatedOffenceList.remove(matchingOffence);
                final uk.gov.justice.core.courts.Offence updatedOffence = uk.gov.justice.core.courts.Offence.offence()
                        .withValuesFrom(matchingOffence)
                        .withLaaApplnReference(laaReference)
                        .build();
                updatedOffenceList.add(updatedOffence);
            }
            final String legalAidStatus = getDefendantLevelLegalStatus(updatedOffenceList);
            final DefendantLegalaidStatusUpdatedV2 defendantLegalaidStatusUpdated = DefendantLegalaidStatusUpdatedV2.defendantLegalaidStatusUpdatedV2()
                    .withCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withLegalAidStatus(legalAidStatus)
                    .withLaaContractNumber(laaReference.getLaaContractNumber())
                    .build();
            final DefendantCaseOffences newDefendantCaseOffences = DefendantCaseOffences.defendantCaseOffences()
                    .withOffences(updatedOffenceList)
                    .withDefendantId(defendantId)
                    .withLegalAidStatus(legalAidStatus)
                    .withProsecutionCaseId(receiveRepresentationOrderForDefendant.getProsecutionCaseId())
                    .build();
            final Optional<OffencesForDefendantChanged> offencesForDefendantChanged = DefendantHelper.getOffencesForDefendantUpdated(updatedOffenceList, offencesList, prosecutionCaseId, defendantId);
            handleDefenceOrganisationAssociationAndDisassociation(organisationDetails.getId(), organisationDetails.getName(), associatedOrganisationId, prosecutionCaseId, defendantId, laaContractNumber, streamBuilder);
            final Organisation organisation = receiveRepresentationOrderForDefendant.getDefenceOrganisation().getOrganisation();

            final AssociatedDefenceOrganisation associatedDefenceOrganisation = nonNull(organisation.getAddress()) &&  StringUtils.isNotEmpty(organisation.getAddress().getAddress1()) ? AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(receiveRepresentationOrderForDefendant.getDefenceOrganisation())
                    .withIsAssociatedByLAA(true)
                    .withFundingType(FundingType.REPRESENTATION_ORDER)
                    .withApplicationReference(receiveRepresentationOrderForDefendant.getApplicationReference())
                    .withAssociationStartDate(receiveRepresentationOrderForDefendant.getEffectiveStartDate())
                    .withAssociationEndDate(receiveRepresentationOrderForDefendant.getEffectiveEndDate())
                    .build() : AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                            .withLaaContractNumber(receiveRepresentationOrderForDefendant.getDefenceOrganisation().getLaaContractNumber())
                            .withOrganisation(Organisation.organisation()
                                    .withName(organisation.getName())
                                    .withContact(organisation.getContact())
                                    .withIncorporationNumber(organisation.getIncorporationNumber())
                                    .withRegisteredCharityNumber(organisation.getRegisteredCharityNumber())
                                    .build())
                            .build())
                    .withIsAssociatedByLAA(true)
                    .withFundingType(FundingType.REPRESENTATION_ORDER)
                    .withApplicationReference(receiveRepresentationOrderForDefendant.getApplicationReference())
                    .withAssociationStartDate(receiveRepresentationOrderForDefendant.getEffectiveStartDate())
                    .withAssociationEndDate(receiveRepresentationOrderForDefendant.getEffectiveEndDate())
                    .build();
            final uk.gov.justice.core.courts.Defendant defendant = defendantsMap.get(defendantId);
            if (offencesForDefendantChanged.isPresent()) {
                UpdatedOrganisation updatedOrganisation = null;
                if (nonNull(organisationDetails.getId())) {
                    updatedOrganisation = updatedOrganisation()
                            .withAddressLine1(organisationDetails.getAddressLine1())
                            .withAddressLine2(organisationDetails.getAddressLine2())
                            .withAddressLine3(organisationDetails.getAddressLine3())
                            .withAddressLine4(organisationDetails.getAddressLine4())
                            .withId(organisationDetails.getId())
                            .withAddressPostcode(organisationDetails.getAddressPostcode())
                            .withEmail(organisationDetails.getEmail())
                            .withLaaContractNumber(organisationDetails.getLaaContractNumber())
                            .withPhoneNumber(organisationDetails.getPhoneNumber())
                            .withName(organisationDetails.getName())
                            .build();

                    streamBuilder.add(prosecutionCaseDefendantOrganisationUpdatedByLaa().
                            withDefendantId(defendantId).withUpdatedOrganisation(updatedOrganisation).build());
                    if (this.defendantLAAUpdatedOrganisation.containsKey(defendantId) && updatedOrganisation.getId().equals(this.defendantLAAUpdatedOrganisation.get(defendantId))) {
                        updatedOrganisation = null;
                    }
                }

                streamBuilder.add(ProsecutionCaseOffencesUpdated.prosecutionCaseOffencesUpdated()
                        .withDefendantCaseOffences(newDefendantCaseOffences).build());
                streamBuilder.add(offencesForDefendantChanged.get());
                streamBuilder.add(defendantLegalaidStatusUpdated);
                streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                        .build());
                streamBuilder.add(ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                        .withDefendant(DefendantUpdate.defendantUpdate()
                                .withId(defendantId)
                                .withMasterDefendantId(defendant.getMasterDefendantId())
                                .withProsecutionCaseId(prosecutionCaseId)
                                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                                .withIsYouth(defendant.getIsYouth())
                                .withOffences(updatedOffenceList)
                                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                                .withCroNumber(defendant.getCroNumber())
                                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                                .withWitnessStatement(defendant.getWitnessStatement())
                                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                                .withPncId(defendant.getPncId())
                                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                                .withMitigationWelsh(defendant.getMitigationWelsh())
                                .withMitigation(defendant.getMitigation())
                                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                                .withAssociatedPersons(defendant.getAssociatedPersons())
                                .withAliases(defendant.getAliases())
                                .withPersonDefendant(defendant.getPersonDefendant())
                                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                                .build())
                        .withProsecutionAuthorityId(getProsecutorId(prosecutionCase))
                        .withCaseUrn(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .withUpdatedOrganisation(updatedOrganisation)
                        .build());

            }
            streamBuilder.add(DefendantDefenceAssociationLocked.defendantDefenceAssociationLocked()
                    .withDefendantId(defendantId)
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withLockedByRepOrder(true)
                    .build());

        }
        return apply(streamBuilder.build());
    }

    private DefendantUpdate getDefendantUpdateFromDefendant(final uk.gov.justice.core.courts.Defendant defendant) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withIsYouth(defendant.getIsYouth())
                .withOffences(defendant.getOffences())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withCroNumber(defendant.getCroNumber())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withPncId(defendant.getPncId())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withMitigation(defendant.getMitigation())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withAliases(defendant.getAliases())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .build();
    }

    public Stream<Object> receiveAssociateDefenceOrganisation(final String organisationName, final UUID defendantId, final UUID prosecutionCaseId, final String laaContractNumber, final ZonedDateTime startTime, final String representationType, final OrganisationDetails organisationDetails) {
        LOGGER.debug("Receive AssociateDefenceOrganisation");

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId) && !isAlreadyAssociated(defendantId)) {

            final AssociatedDefenceOrganisation associatedDefenceOrganisation = StringUtils.isNotEmpty(organisationDetails.getAddressLine1()) ? AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisation(organisation()
                                    .withAddress(address()
                                            .withAddress1(organisationDetails.getAddressLine1())
                                            .withAddress2(organisationDetails.getAddressLine2())
                                            .withAddress3(organisationDetails.getAddressLine3())
                                            .withAddress4(organisationDetails.getAddressLine4())
                                            .withPostcode(organisationDetails.getAddressPostcode())
                                            .build())
                                    .withName(organisationName)
                                    .build())
                            .build())
                    .withIsAssociatedByLAA(false)
                    .withFundingType(FundingType.valueOf(representationType))
                    .withAssociationStartDate(LocalDate.from(startTime))
                    .build() : AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                    .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisation(organisation()
                                    .withName(organisationName)
                                    .build())
                            .build())
                    .withIsAssociatedByLAA(false)
                    .withFundingType(FundingType.valueOf(representationType))
                    .withAssociationStartDate(LocalDate.from(startTime))
                    .build();

            streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                    .build());

            streamBuilder.add(DefenceOrganisationAssociatedByDefenceContext.defenceOrganisationAssociatedByDefenceContext()
                    .withDefendantId(defendantId)
                    .withLaaContractNumber(laaContractNumber)
                    .withOrganisationId(organisationDetails.getId())
                    .withOrganisationName(organisationName)
                    .withRepresentationType(RepresentationType.valueOf(representationType))
                    .withStartDate(startTime)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveDisAssociateDefenceOrganisation(final UUID defendantId, final UUID prosecutionCaseId, final UUID organisationId) {
        LOGGER.debug("Receive DisAssociateDefenceOrganisation");

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (this.defendantCaseOffences.containsKey(defendantId)) {


            streamBuilder.add(DefendantDefenceOrganisationChanged.defendantDefenceOrganisationChanged()
                    .withProsecutionCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .build());

            streamBuilder.add(DefenceOrganisationDissociatedByDefenceContext.defenceOrganisationDissociatedByDefenceContext()
                    .withCaseId(prosecutionCaseId)
                    .withDefendantId(defendantId)
                    .withOrganisationId(organisationId)
                    .build());
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> addNote(final UUID caseNoteId, final UUID caseId, final String note, final boolean isPinned, final String firstName, final String lastName) {
        return apply(Stream.of(CaseNoteAddedV2.caseNoteAddedV2()
                .withCaseNoteId(caseNoteId)
                .withCaseId(caseId)
                .withNote(note)
                .withIsPinned(isPinned)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withCreatedDateTime(now())
                .build()));
    }

    public Stream<Object> editNote(final UUID caseId, final UUID caseNoteId, final Boolean isPinned) {
        return apply(Stream.of(CaseNoteEditedV2.caseNoteEditedV2()
                .withCaseId(caseId)
                .withCaseNoteId(caseNoteId)
                .withIsPinned(isPinned)
                .build()));
    }

    public Stream<Object> aggregateExactMatchedDefendantSearchResult(final UUID defendantId, final List<Cases> cases) {
        return apply(Stream.of(ExactMatchedDefendantSearchResultStored.exactMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(cases)
                .build()));
    }

    public Stream<Object> aggregatePartialMatchedDefendantSearchResult(final UUID defendantId, final List<Cases> cases) {
        return apply(Stream.of(PartialMatchedDefendantSearchResultStored.partialMatchedDefendantSearchResultStored()
                .withDefendantId(defendantId)
                .withCases(cases)
                .build()));
    }

    public Stream<Object> updateMatchedDefendant(final UUID prosecutionCaseId, final UUID defendantId, final UUID masterDefendantId) {
        return apply(Stream.of(DefendantsMasterDefendantIdUpdated.defendantsMasterDefendantIdUpdated()
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendant(uk.gov.justice.core.courts.Defendant.defendant()
                        .withValuesFrom(defendantsMap.get(defendantId))
                        .withMasterDefendantId(masterDefendantId)
                        .build())
                .build()));
    }

    public Stream<Object> storeMatchedDefendants(final UUID prosecutionCaseId) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        final Map<UUID, List<Cases>> partiallyMatchedDefendants = this.partialMatchedDefendants;
        final Map<UUID, List<Cases>> fullyMatchedDefendants = this.exactMatchedDefendants;

        for (final Map.Entry<UUID, List<Cases>> defendantEntry : partiallyMatchedDefendants.entrySet()) {
            // Partial matched defendant events
            final Optional<uk.gov.justice.core.courts.Defendant> defendant = prosecutionCase.getDefendants().stream()
                    .filter(d -> d.getId().equals(defendantEntry.getKey()) && nonNull(d.getPersonDefendant()))
                    .findFirst();
            if (defendant.isPresent()) {

                final uk.gov.justice.core.courts.Defendant matchedDefendant = defendant.get();

                streamBuilder.add(DefendantPartialMatchCreated.defendantPartialMatchCreated()
                        .withDefendantId(matchedDefendant.getId())
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantName(getDefendantName(matchedDefendant))
                        .withCaseReference(reference)
                        .withPayload(transformToPartialMatchDefendantPayload(matchedDefendant, prosecutionCaseId, defendantEntry.getValue()))
                        .withCaseReceivedDatetime(matchedDefendant.getCourtProceedingsInitiated())
                        .build());
            }
        }

        for (final Map.Entry<UUID, List<Cases>> defendantEntry : fullyMatchedDefendants.entrySet()) {

            final UUID defendantId = defendantEntry.getKey();
            final List<MatchedDefendants> matchedDefendants = transformToExactMatchedDefendants(defendantEntry.getValue());

            if (defendantsMap.containsKey(defendantId)) {

                streamBuilder.add(MasterDefendantIdUpdatedV2.masterDefendantIdUpdatedV2()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withHearingId(this.latestHearingId)
                        .withMatchedDefendants(matchedDefendants)
                        .withDefendant(defendantsMap.get(defendantId))
                        .build());
            } else {

                // Fully matched defendant events
                streamBuilder.add(MasterDefendantIdUpdated.masterDefendantIdUpdated()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantId(defendantId)
                        .withHearingId(this.latestHearingId)
                        .withMatchedDefendants(matchedDefendants)
                        .build());
            }
        }
        return apply(streamBuilder.build());
    }

    private String getDefendantName(final uk.gov.justice.core.courts.Defendant defendant) {
        final uk.gov.justice.core.courts.Person personDetails = defendant.getPersonDefendant().getPersonDetails();
        return Stream.of(personDetails.getFirstName(), personDetails.getMiddleName(), personDetails.getLastName())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));

    }

    private void addToJsonObjectNullSafe(final JsonObjectBuilder jsonObjectBuilder, final String key, final String value) {
        if (nonNull(value)) {
            jsonObjectBuilder.add(key, value);
        }
    }

    private String transformToPartialMatchDefendantPayload(final uk.gov.justice.core.courts.Defendant defendant, final UUID prosecutionCaseId, final List<Cases> casesList) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("defendantId", defendant.getId().toString());
        jsonObjectBuilder.add("masterDefendantId", defendant.getMasterDefendantId().toString());
        jsonObjectBuilder.add("prosecutionCaseId", prosecutionCaseId.toString());
        jsonObjectBuilder.add("caseReference", reference);
        jsonObjectBuilder.add("courtProceedingsInitiated", ZONE_DATETIME_FORMATTER.format(defendant.getCourtProceedingsInitiated()));
        final uk.gov.justice.core.courts.Person personDetails = defendant.getPersonDefendant().getPersonDetails();
        addToJsonObjectNullSafe(jsonObjectBuilder, "firstName", personDetails.getFirstName());
        addToJsonObjectNullSafe(jsonObjectBuilder, "middleName", personDetails.getMiddleName());
        jsonObjectBuilder.add("lastName", personDetails.getLastName());
        if (nonNull(personDetails.getDateOfBirth())) {
            addToJsonObjectNullSafe(jsonObjectBuilder, "dateOfBirth", DATE_FORMATTER.format(personDetails.getDateOfBirth()));
        }
        addToJsonObjectNullSafe(jsonObjectBuilder, "pncId", defendant.getPncId());
        addToJsonObjectNullSafe(jsonObjectBuilder, "croNumber", defendant.getCroNumber());
        if (nonNull(defendant.getPersonDefendant().getPersonDetails().getAddress())) {
            addAddress(defendant.getPersonDefendant().getPersonDetails().getAddress(), jsonObjectBuilder);
        }
        jsonObjectBuilder.add("defendantsMatchedCount", casesList.size());

        final JsonArrayBuilder jsonDefendantsMatchedBuilder = Json.createArrayBuilder();
        casesList.forEach(cases -> convertToJsonArray(jsonDefendantsMatchedBuilder, cases, cases.getDefendants()));
        jsonObjectBuilder.add("defendantsMatched", jsonDefendantsMatchedBuilder.build());
        return jsonObjectBuilder.build().toString();
    }

    private JsonArrayBuilder convertToJsonArray(final JsonArrayBuilder jsonArrayBuilder, final Cases cases, final List<Defendants> defendants) {
        defendants.forEach(defendant -> {
            if (isNull(defendant.getDefendantId()) || isNull(defendant.getMasterDefendantId()) || isNull(defendant.getCourtProceedingsInitiated())) {
                return;
            }

            final JsonObjectBuilder defendantJsonObjectBuilder = Json.createObjectBuilder();
            defendantJsonObjectBuilder.add("defendantId", defendant.getDefendantId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "masterDefendantId", defendant.getMasterDefendantId());
            defendantJsonObjectBuilder.add("courtProceedingsInitiated", ZONE_DATETIME_FORMATTER.format(defendant.getCourtProceedingsInitiated()));
            defendantJsonObjectBuilder.add("caseReference", cases.getCaseReference());
            defendantJsonObjectBuilder.add("prosecutionCaseId", cases.getProsecutionCaseId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "firstName", defendant.getFirstName());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "middleName", defendant.getMiddleName());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "lastName", defendant.getLastName());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "dateOfBirth", defendant.getDateOfBirth());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "pncId", defendant.getPncId());
            addToJsonObjectNullSafe(defendantJsonObjectBuilder, "croNumber", defendant.getCroNumber());
            if (nonNull(defendant.getAddress())) {
                addAddress(defendant.getAddress(), defendantJsonObjectBuilder);
            }
            jsonArrayBuilder.add(defendantJsonObjectBuilder.build());
        });
        return jsonArrayBuilder;
    }

    private void addAddress(final Address address, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonObjectBuilder addressJsonObjectBuilder = Json.createObjectBuilder();
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine1", address.getAddress1());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine2", address.getAddress2());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine3", address.getAddress3());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine4", address.getAddress4());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "addressLine5", address.getAddress5());
        addToJsonObjectNullSafe(addressJsonObjectBuilder, "postcode", address.getPostcode());
        jsonObjectBuilder.add("address", addressJsonObjectBuilder.build());
    }

    public Stream<Object> matchPartiallyMatchedDefendants(final MatchDefendant matchDefendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (matchedDefendantIds.contains(matchDefendant.getDefendantId())) {
            streamBuilder.add(DefendantMatched.defendantMatched()
                    .withDefendantId(matchDefendant.getDefendantId())
                    .withHasDefendantAlreadyBeenDeleted(TRUE)
                    .build());
        } else {
            streamBuilder.add(DefendantMatched.defendantMatched()
                    .withDefendantId(matchDefendant.getDefendantId())
                    .withHasDefendantAlreadyBeenDeleted(FALSE)
                    .build());

            final UUID defendantId = matchDefendant.getDefendantId();
            if (defendantsMap.containsKey(defendantId)) {

                    streamBuilder.add(MasterDefendantIdUpdatedV2.masterDefendantIdUpdatedV2()
                            .withProsecutionCaseId(matchDefendant.getProsecutionCaseId())
                            .withHearingId(this.latestHearingId)
                            .withMatchedDefendants(transform(matchDefendant.getMatchedDefendants()))
                            .withDefendant(defendantsMap.get(defendantId))
                            .build());

                streamBuilder.add(MasterDefendantIdUpdatedIntoHearings.masterDefendantIdUpdatedIntoHearings()
                        .withDefendant(defendantsMap.get(defendantId))
                        .withMatchedDefendants(transform(matchDefendant.getMatchedDefendants()))
                        .withHearingIds(this.hearingIds.stream().collect(toList()))
                        .withProsecutionCaseId(matchDefendant.getProsecutionCaseId())
                        .build());
            } else {
                streamBuilder.add(MasterDefendantIdUpdated.masterDefendantIdUpdated()
                        .withProsecutionCaseId(matchDefendant.getProsecutionCaseId())
                        .withDefendantId(defendantId)
                        .withHearingId(this.latestHearingId)
                        .withMatchedDefendants(transform(matchDefendant.getMatchedDefendants()))
                        .build());
            }
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> unmatchDefendants(final UnmatchDefendant unmatchDefendant) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        unmatchDefendant.getUnmatchedDefendants()
                .forEach(d -> streamBuilder.add(buildDefendantUnmatchedEvent(d))
                );
        return apply(streamBuilder.build());
    }

    /**
     * Creates the defendant unmatched event. Using the stored map of defendant information and
     * overwriting the master defendant id back to the original defendant id. Also can create old
     * version of the event for backwards compatibility.
     *
     * @param unmatchedDefendant - the defendant to unmatch master defendant id for
     * @return event to trigger master defendant unmatching.
     */
    private Object buildDefendantUnmatchedEvent(final UnmatchedDefendant unmatchedDefendant) {
        if (defendantsMap.containsKey(unmatchedDefendant.getDefendantId())) {
            return DefendantUnmatchedV2.defendantUnmatchedV2()
                    .withDefendantId(unmatchedDefendant.getDefendantId())
                    .withProsecutionCaseId(unmatchedDefendant.getProsecutionCaseId())
                    .withDefendant(uk.gov.justice.core.courts.Defendant.defendant()
                            .withValuesFrom(defendantsMap.get(unmatchedDefendant.getDefendantId()))
                            .withMasterDefendantId(unmatchedDefendant.getDefendantId())
                            .build())
                    .build();
        } else {
            return DefendantUnmatched.defendantUnmatched()
                    .withDefendantId(unmatchedDefendant.getDefendantId())
                    .withProsecutionCaseId(unmatchedDefendant.getProsecutionCaseId())
                    .build();
        }
    }

    public Stream<Object> validateLinkSplitOrMergeStreams(final List<CasesToLink> casesToLink) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        casesToLink.forEach(ctl -> {

                    if (ctl.getLinkType().toString().equals(LinkType.LINK.name())) {
                        streamBuilder.add(ValidateLinkCases.validateLinkCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.SPLIT.name())) {
                        streamBuilder.add(ValidateSplitCases.validateSplitCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.MERGE.name())) {
                        streamBuilder.add(ValidateMergeCases.validateMergeCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());
                    }
                }
        );
        return apply(streamBuilder.build());
    }

    public Stream<Object> addRelatedReference(RelatedReferenceAdded relatedReferenceAdded) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(relatedReferenceAdded);
        return streamBuilder.build();
    }

    public Stream<Object> deleteRelatedReference(RelatedReferenceDeleted relatedReferenceDeleted) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(relatedReferenceDeleted);
        return streamBuilder.build();
    }

    public Stream<Object> processLinkSplitOrMergeStreams(final List<CasesToLink> casesToLink) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        casesToLink.forEach(ctl -> {

                    if (ctl.getLinkType().toString().equals(LinkType.LINK.name())) {
                        streamBuilder.add(LinkCases.linkCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.SPLIT.name())) {
                        streamBuilder.add(SplitCases.splitCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());

                    } else if (ctl.getLinkType().toString().equals(LinkType.MERGE.name())) {
                        streamBuilder.add(MergeCases.mergeCases().withProsecutionCaseId(ctl.getProsecutionCaseId())
                                .withCaseUrns(ctl.getCaseUrns())
                                .build());
                    }
                }
        );
        return apply(streamBuilder.build());
    }

    public Stream<Object> unlinkCases(final UUID prosecutionCaseId, final String prosecutionCaseUrn, final List<CaseToUnlink> casesToUnlink) {
        final CasesUnlinked casesUnlinked = CasesUnlinked.casesUnlinked()
                .withProsecutionCaseId(prosecutionCaseId)
                .withProsecutionCaseUrn(prosecutionCaseUrn)
                .withUnlinkedCases(
                        casesToUnlink.stream()
                                .map(ctu -> UnlinkedCases.unlinkedCases()
                                        .withCaseId(ctu.getCaseId())
                                        .withCaseUrn(ctu.getCaseUrn())
                                        .withLinkGroupId(ctu.getLinkGroupId())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();

        return apply(Stream.builder()
                .add(casesUnlinked)
                .build());
    }

    public Stream<Object> extendHearing(final HearingListingNeeds hearingListingNeeds, final ExtendHearing extendHearing) {
        LOGGER.debug("hearing has been extended");
        return apply(Stream.of(
                HearingExtended.hearingExtended()
                        .withExtendedHearingFrom(extendHearing.getExtendedHearingFrom())
                        .withHearingRequest(hearingListingNeeds)
                        .withIsAdjourned(extendHearing.getIsAdjourned())
                        .withIsPartiallyAllocated(extendHearing.getIsPartiallyAllocated())
                        .withShadowListedOffences(extendHearing.getShadowListedOffences())
                        .withIsUnAllocatedHearing(extendHearing.getIsUnAllocatedHearing())
                        .build()));
    }

    public Stream<Object> updateHearingForPartialAllocation(final UUID hearingId, final List<ProsecutionCasesToRemove> prosecutionCasesToRemove) {
        LOGGER.debug("hearing has been updated for partial allocation");
        return apply(Stream.of(HearingUpdatedForPartialAllocation.hearingUpdatedForPartialAllocation()
                .withHearingId(hearingId)
                .withProsecutionCasesToRemove(prosecutionCasesToRemove)
                .build()));
    }

    public Stream<Object> updateCpsDefendantId(final UUID caseId, final UUID defendantId, final String cpsDefendantId) {
        return apply(Stream.of(CpsDefendantIdUpdated.cpsDefendantIdUpdated()
                .withCaseId(caseId)
                .withCpsDefendantId(cpsDefendantId)
                .withDefendantId(defendantId)
                .build()));
    }

    public Stream<Object> markHearingAsDuplicate(final UUID hearingId, final UUID caseId, final List<UUID> defendantIds) {
        return apply(Stream.of(HearingMarkedAsDuplicateForCase.hearingMarkedAsDuplicateForCase()
                .withCaseId(caseId)
                .withHearingId(hearingId)
                .withDefendantIds(defendantIds)
                .build()));
    }

    public Stream<Object> deleteHearingRelatedToProsecutionCase(final UUID hearingId, final UUID prosecutionCaseId) {
        return apply(Stream.of(HearingDeletedForProsecutionCase.hearingDeletedForProsecutionCase()
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearingId(hearingId)
                .withDefendantIds(prosecutionCase.getDefendants().stream().map(uk.gov.justice.core.courts.Defendant::getId).collect(toList()))
                .build()));
    }

    public Stream<Object> updateCaseProsecutorDetails(final ProsecutionCaseIdentifier prosecutionCaseIdentifier,
                                                      final String oldCpsProsecutor) {
        LOGGER.debug("update case Prosecution details for caseId");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(buildCaseCpsProsecutorUpdated(prosecutionCaseIdentifier, oldCpsProsecutor));

        if (oldCpsProsecutor != null && !oldCpsProsecutor.isEmpty()) {
            streamBuilder.add(buildCpsProsecutorUpdated(prosecutionCaseIdentifier, oldCpsProsecutor));
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> updateListingNumber(final List<OffenceListingNumbers> offenceListingNumbers) {
        if (nonNull(this.prosecutionCase)) {
            return apply(Stream.builder()
                    .add(ProsecutionCaseListingNumberUpdated.prosecutionCaseListingNumberUpdated()
                            .withProsecutionCaseId(this.prosecutionCase.getId())
                            .withOffenceListingNumbers(offenceListingNumbers)
                            .build())
                    .build());
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> reApplyMiReportingRestrictions(final UUID caseId) {
        if (nonNull(this.prosecutionCase)) {
            return apply(Stream.of(ReapplyMiReportingRestrictions.reapplyMiReportingRestrictions()
                    .withCaseId(caseId)
                    .build()));
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> updateCpsDetails(final UUID caseId, final UUID defendantId, final ProsecutionCaseSubject prosecutionCaseSubject, final String ouCode) {
        final String cpsDefendantId = ofNullable(prosecutionCaseSubject.getDefendantSubject())
                .map(defendantSubject -> ofNullable(defendantSubject.getCpsDefendantId())
                        .orElseGet(() -> ofNullable(defendantSubject.getCpsPersonDefendantDetails()).map(CpsPersonDefendantDetails::getCpsDefendantId).orElse(null)))
                .orElse(null);

        return apply(Stream.of(
                caseCpsDetailsUpdatedFromCourtDocument()
                        .withCaseId(caseId)
                        .withCpsDefendantId(cpsDefendantId)
                        .withCpsOrganisation(ouCode)
                        .withDefendantId(defendantId)
                        .build()));
    }

    public Stream<Object> increaseListingNumber(final List<UUID> offenceIds, final UUID hearingId) {
        if (nonNull(this.prosecutionCase)) {
            final List<OffenceListingNumbers> offenceListingNumbers = this.prosecutionCase.getDefendants().stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> offenceIds.contains(offence.getId()))
                    .map(offence -> OffenceListingNumbers.offenceListingNumbers()
                            .withListingNumber(ofNullable(offence.getListingNumber()).orElse(0) + 1)
                            .withOffenceId(offence.getId())
                            .build())
                    .collect(toList());

            return apply(Stream.builder()
                    .add(ProsecutionCaseListingNumberIncreased.prosecutionCaseListingNumberIncreased()
                            .withProsecutionCaseId(this.prosecutionCase.getId())
                            .withHearingId(hearingId)
                            .withOffenceListingNumbers(offenceListingNumbers)
                            .build())
                    .build());
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> decreaseListingNumbers(final List<UUID> offenceIds) {
        if (nonNull(this.prosecutionCase)) {
            return apply(Stream.builder()
                    .add(ProsecutionCaseListingNumberDecreased.prosecutionCaseListingNumberDecreased()
                            .withProsecutionCaseId(this.prosecutionCase.getId())
                            .withOffenceIds(offenceIds)
                            .build())
                    .build());
        } else {
            return Stream.empty();
        }
    }

    /**
     * When CPS sends the document , if either the prosecutor(cpsOrg) details is not in reference
     * data or if the cpsFlag is false then set isCPSVerifyError to true
     *
     * @param prosecutionCaseIdentifier
     * @return
     */
    public Stream<Object> updateCaseProsecutorDetails(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        LOGGER.debug("update case Prosecution details for caseId");
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (isNull(prosecutionCaseIdentifier)) {
            streamBuilder.add(buildCaseCpsProsecutorUpdatedWithCpsOrgVerifyError());
        } else {
            streamBuilder.add(buildCaseCpsProsecutorUpdated(prosecutionCaseIdentifier, null));
        }
        return apply(streamBuilder.build());
    }

    private CpsProsecutorUpdated buildCpsProsecutorUpdated(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final String oldCpsProsecutor) {
        return CpsProsecutorUpdated.cpsProsecutorUpdated()
                .withProsecutionCaseId(this.prosecutionCase.getId())
                .withOldCpsProsecutor(oldCpsProsecutor)
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .build();
    }

    private CaseCpsProsecutorUpdated buildCaseCpsProsecutorUpdated(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final String oldCpsProsecutor) {
        return CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(this.prosecutionCase.getId())
                .withAddress(prosecutionCaseIdentifier.getAddress())
                .withCaseURN(this.prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                .withOldCpsProsecutor(oldCpsProsecutor)
                .withProsecutionAuthorityCode(prosecutionCaseIdentifier.getProsecutionAuthorityCode())
                .withProsecutionAuthorityId(prosecutionCaseIdentifier.getProsecutionAuthorityId())
                .withProsecutionAuthorityName(prosecutionCaseIdentifier.getProsecutionAuthorityName())
                .withProsecutionAuthorityReference(this.prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                .withContact(prosecutionCaseIdentifier.getContact())
                .withMajorCreditorCode(prosecutionCaseIdentifier.getMajorCreditorCode())
                .withProsecutionAuthorityOUCode(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())
                .withIsCpsOrgVerifyError(false)// this method is called for valid cps organisation so we should set the error flag as false.
                .build();
    }

    public ProsecutionCase getProsecutionCase() {
        return this.prosecutionCase;
    }

    public Stream<Object> removeHearingRelatedToProsecutionCase(final UUID hearingId, final UUID prosecutionCaseId) {
        return apply(Stream.of(HearingRemovedForProsecutionCase.hearingRemovedForProsecutionCase()
                .withProsecutionCaseId(prosecutionCaseId)
                .withHearingId(hearingId)
                .build()));
    }

    private List<MatchedDefendants> transform(final List<MatchedDefendant> matchedDefendants) {
        return matchedDefendants.stream().filter(s -> s.getCourtProceedingsInitiated() != null)
                .map(matchedDefendant -> MatchedDefendants.matchedDefendants()
                        .withDefendantId(matchedDefendant.getDefendantId())
                        .withProsecutionCaseId(matchedDefendant.getProsecutionCaseId())
                        .withMasterDefendantId(matchedDefendant.getMasterDefendantId())
                        .withCourtProceedingsInitiated(matchedDefendant.getCourtProceedingsInitiated())
                        .build())
                .collect(toList());
    }

    private List<MatchedDefendants> transformToExactMatchedDefendants(final List<Cases> casesList) {
        final List<MatchedDefendants> matchedDefendantsList = new ArrayList<>();
        casesList
                .forEach(cases -> cases.getDefendants().stream().filter(s -> s.getCourtProceedingsInitiated() != null)
                        .forEach(def -> matchedDefendantsList.add(MatchedDefendants.matchedDefendants()
                                .withProsecutionCaseId(UUID.fromString(cases.getProsecutionCaseId()))
                                .withDefendantId(UUID.fromString(def.getDefendantId()))
                                .withMasterDefendantId(UUID.fromString(def.getMasterDefendantId()))
                                .withCourtProceedingsInitiated(def.getCourtProceedingsInitiated())
                                .build())
                        ));
        return matchedDefendantsList;
    }

    private void handleDefenceOrganisationAssociationAndDisassociation(final UUID organisationId, final String organisationName, final String alreadyAssociatedOrganisationId,
                                                                       final UUID prosecutionCaseId, final UUID defendantId, final String laaContractNumber, final Stream.Builder<Object> streamBuilder) {

        if (organisationId == null && alreadyAssociatedOrganisationId != null) {
            LOGGER.error("Organisation not set up for LAA Contract Number {}", laaContractNumber);
            //Raise event to store orphaned defendants so that it can be fetched when organisation is Set up.
            streamBuilder.add(DefendantLaaAssociated.defendantLaaAssociated()
                    .withDefendantId(defendantId)
                    .withLaaContractNumber(laaContractNumber)
                    .withIsAssociatedByLAA(false)
                    .build());

            //disassociate the existing defence organisation
            streamBuilder.add(defendantDefenceOrganisationDisassociated()
                    .withDefendantId(defendantId)
                    .withCaseId(prosecutionCaseId)
                    .withOrganisationId(fromString(alreadyAssociatedOrganisationId))
                    .build());
        } else if (organisationId != null && alreadyAssociatedOrganisationId != null) {
            if (!organisationId.toString().equals(alreadyAssociatedOrganisationId)) {
                // Disassociate the existing defence organisation and associate the one which is linked with one from payload
                streamBuilder.add(defendantDefenceOrganisationDisassociated()
                        .withDefendantId(defendantId)
                        .withCaseId(prosecutionCaseId)
                        .withOrganisationId(fromString(alreadyAssociatedOrganisationId))
                        .build());
                streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                        .withDefendantId(defendantId)
                        .withOrganisationId(organisationId)
                        .withOrganisationName(organisationName)
                        .withLaaContractNumber(laaContractNumber)
                        .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                        .build());
            } else {
                if (!isAlreadyAssociatedToOrganisation(defendantId, organisationId) || !(defendantOrganisationAsscociatedByRepOrder.getOrDefault(defendantId, false))) {                    // Handle wrong payload case, when it is not actually associated by payload says organisation is associated.
                    streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                            .withDefendantId(defendantId)
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisationId(organisationId)
                            .withOrganisationName(organisationName)
                            .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                            .build());
                } else {
                    LOGGER.info("Organisation for LAA Contract Number {} is already associated", laaContractNumber);
                }

            }
        } else {
            if (organisationId != null) {
                // Associate the one which is linked with one from payload
                if (!isAlreadyAssociatedToOrganisation(defendantId, organisationId) || !(defendantOrganisationAsscociatedByRepOrder.getOrDefault(defendantId, false))) {
                    streamBuilder.add(DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                            .withDefendantId(defendantId)
                            .withLaaContractNumber(laaContractNumber)
                            .withOrganisationId(organisationId)
                            .withOrganisationName(organisationName)
                            .withRepresentationType(RepresentationType.REPRESENTATION_ORDER)
                            .build());
                }
            } else {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Organisation not set up for LAA Contract Number {} and there is no existing association for defendant {} ", laaContractNumber, defendantId.toString());
                }
                //Raise event to store orphaned defendants so that it can be fetched when organisation is Set up.
                streamBuilder.add(DefendantLaaAssociated.defendantLaaAssociated()
                        .withDefendantId(defendantId)
                        .withLaaContractNumber(laaContractNumber)
                        .withIsAssociatedByLAA(false)
                        .build());
            }
        }
    }

    private String getDefendantLevelLegalStatus(final List<uk.gov.justice.core.courts.Offence> offencesList) {
        final List<String> defendantLevelStatusList = offencesList.stream().filter(offence -> nonNull(offence.getLaaApplnReference()))
                .map(uk.gov.justice.core.courts.Offence::getLaaApplnReference)
                .map(LaaReference::getOffenceLevelStatus).collect(toList());
        if (defendantLevelStatusList.stream().anyMatch
                (defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(GRANTED.getDescription()))) {
            return GRANTED.getDescription();
        } else if (defendantLevelStatusList.stream().anyMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(REFUSED.getDescription()))) {
            return REFUSED.getDescription();
        } else if (defendantLevelStatusList.stream().anyMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(WITHDRAWN.getDescription()))) {
            return WITHDRAWN.getDescription();
        } else if (defendantLevelStatusList.stream().anyMatch(defendantLevelStatus -> defendantLevelStatus != null && defendantLevelStatus.equals(PENDING.getDescription()))) {
            return PENDING.getDescription();
        } else {
            return NO_VALUE.getDescription();
        }

    }

    private String getProsecutorId(final ProsecutionCase prosecutionCase) {
        String prosecutorId = null;
        if (nonNull(prosecutionCase.getProsecutor()) && nonNull(prosecutionCase.getProsecutor().getProsecutorId())) {
            prosecutorId = prosecutionCase.getProsecutor().getProsecutorId().toString();
        } else if (nonNull(prosecutionCase.getProsecutionCaseIdentifier()) && nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())) {
            prosecutorId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId().toString();
        }
        return prosecutorId;
    }

    private void hearingConfirmedCaseStatusUpdated(final HearingConfirmedCaseStatusUpdated hearingConfirmedCaseStatusUpdated) {
        this.caseStatus = hearingConfirmedCaseStatusUpdated.getProsecutionCase().getCaseStatus();
    }

    private void removeDefendantAssociatedDefenceOrganisation(final DefendantDefenceOrganisationDisassociated defendantDefenceOrganisationDisassociated) {
        removeFromOrganisationMap(defendantDefenceOrganisationDisassociated.getDefendantId());
    }

    private void removeDefendantAssociatedDefenceOrganisation(final DefenceOrganisationDissociatedByDefenceContext defenceOrganisationDissociatedByDefenceContext) {
        removeFromOrganisationMap(defenceOrganisationDissociatedByDefenceContext.getDefendantId());
    }

    private void removeFromOrganisationMap(final UUID defendantId) {
        this.defendantAssociatedDefenceOrganisation.remove(defendantId);
    }

    private void updateDefendantAssociatedDefenceOrganisation(final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated) {
        this.defendantAssociatedDefenceOrganisation.put(defendantDefenceOrganisationAssociated.getDefendantId(), defendantDefenceOrganisationAssociated.getOrganisationId());
        final boolean byRepOrder = defendantDefenceOrganisationAssociated.getRepresentationType() == RepresentationType.REPRESENTATION_ORDER;
        this.defendantOrganisationAsscociatedByRepOrder.put(defendantDefenceOrganisationAssociated.getDefendantId(), byRepOrder);
    }

    private void updateDefendantAssociatedDefenceOrganisation(final DefenceOrganisationAssociatedByDefenceContext defenceOrganisationAssociatedByDefenceContext) {
        this.defendantAssociatedDefenceOrganisation.put(defenceOrganisationAssociatedByDefenceContext.getDefendantId(), defenceOrganisationAssociatedByDefenceContext.getOrganisationId());
    }

    private void onHearingMarkedAsDuplicateForCase(final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase) {
        this.hearingIds.remove(hearingMarkedAsDuplicateForCase.getHearingId());
        if(nonNull(this.latestHearingId) && this.latestHearingId.equals(hearingMarkedAsDuplicateForCase.getHearingId())){
            this.latestHearingId = null;
        }
    }

    private void onHearingDeletedForProsecutionCase(final HearingDeletedForProsecutionCase hearingDeletedForProsecutionCase) {
        this.hearingIds.remove(hearingDeletedForProsecutionCase.getHearingId());
        if(hearingDeletedForProsecutionCase.getHearingId().equals(latestHearingId)) {
            latestHearingId = null;
        }

    }

    private void onHearingRemovedForProsecutionCase(final HearingRemovedForProsecutionCase hearingRemovedForProsecutionCase) {
        this.hearingIds.remove(hearingRemovedForProsecutionCase.getHearingId());
        if(hearingRemovedForProsecutionCase.getHearingId().equals(latestHearingId)) {
            latestHearingId = null;
        }
    }

    private void onCustodyTimeLimitExtended(final CustodyTimeLimitExtended custodyTimeLimitExtended) {
        final UUID offenceId = custodyTimeLimitExtended.getOffenceId();
        this.prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getOffences().stream()
                        .anyMatch(offence -> offence.getId().equals(offenceId)))
                .forEach(defendant -> {
                    final Optional<uk.gov.justice.core.courts.Offence> offence = defendant.getOffences().stream()
                            .filter(o -> o.getId().equals(offenceId))
                            .findFirst();

                    if (offence.isPresent()) {
                        final int index = defendant.getOffences().indexOf(offence.get());
                        defendant.getOffences().remove(index);
                        defendant.getOffences().add(index, uk.gov.justice.core.courts.Offence.offence()
                                .withValuesFrom(offence.get())
                                .withCustodyTimeLimit(CustodyTimeLimit.custodyTimeLimit()
                                        .withValuesFrom(nonNull(offence.get().getCustodyTimeLimit()) ?
                                                offence.get().getCustodyTimeLimit() : CustodyTimeLimit.custodyTimeLimit().build())
                                        .withTimeLimit(custodyTimeLimitExtended.getExtendedTimeLimit())
                                        .withIsCtlExtended(true)
                                        .build())
                                .build());
                    }
                });

    }

    private boolean isAlreadyAssociated(final UUID defendantId) {
        return !isNull(this.defendantAssociatedDefenceOrganisation.get(defendantId));
    }

    private boolean isAlreadyAssociatedToOrganisation(final UUID defendantId, final UUID organisationId) {
        return this.defendantAssociatedDefenceOrganisation.containsKey(defendantId) && this.defendantAssociatedDefenceOrganisation.get(defendantId).equals(organisationId);
    }

    private void updateProsecutionCaseIdentifier(final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated) {
        Prosecutor prosecutor = null;
        if (isNull(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) || !caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) {
            prosecutor = Prosecutor.prosecutor()
                    .withAddress(caseCpsProsecutorUpdated.getAddress())
                    .withProsecutorCode(caseCpsProsecutorUpdated.getProsecutionAuthorityCode())
                    .withProsecutorId(caseCpsProsecutorUpdated.getProsecutionAuthorityId())
                    .withProsecutorName(caseCpsProsecutorUpdated.getProsecutionAuthorityName())
                    .withIsCps(true)
                    .build();
        }

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withPoliceOfficerInCase(prosecutionCase.getPoliceOfficerInCase())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withProsecutor(prosecutor)
                .withId(prosecutionCase.getId())
                .withDefendants(prosecutionCase.getDefendants())
                .withInitiationCode(prosecutionCase.getInitiationCode())
                .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(prosecutionCase.getCpsOrganisation())
                .withCpsOrganisationId(prosecutionCase.getCpsOrganisationId())
                .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                .withCaseMarkers(prosecutionCase.getCaseMarkers())
                .withAppealProceedingsPending(prosecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(prosecutionCase.getBreachProceedingsPending())
                .withRemovalReason(prosecutionCase.getRemovalReason())
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withTrialReceiptType(prosecutionCase.getTrialReceiptType())
                .withIsCpsOrgVerifyError(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError())
                .build();
        this.prosecutionCase = updatedProsecutionCase;

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = this.prosecutionCase.getProsecutionCaseIdentifier();
        if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityReference())) {
            reference = caseCpsProsecutorUpdated.getProsecutionAuthorityReference();
        }
        if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            reference = caseCpsProsecutorUpdated.getCaseURN();
        }
    }

    private CaseCpsProsecutorUpdated buildCaseCpsProsecutorUpdatedWithCpsOrgVerifyError() {
        final UUID caseId = this.prosecutionCase.getId();
        return CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionCaseId(caseId)
                .withProsecutionAuthorityCode(this.prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode())
                .withIsCpsOrgVerifyError(true).build();
    }

    public Stream<Object> extendCustodyTimeLimit(final UUID hearingId, final UUID offenceId, final LocalDate extendedTimeLimit) {
        final List<UUID> extendHearingIds = this.hearingIds.stream()
                .filter(id -> !id.equals(hearingId))
                .collect(toList());

        if (extendHearingIds.isEmpty()) {
            return empty();
        }

        return apply(Stream.of(CustodyTimeLimitExtended.custodyTimeLimitExtended()
                .withHearingIds(extendHearingIds)
                .withOffenceId(offenceId)
                .withExtendedTimeLimit(extendedTimeLimit)
                .build()));
    }


    private List<uk.gov.justice.core.courts.Defendant> updateDefendantWithProceedingsConcludedStatusAndOriginalListingNumbers(final ProsecutionCase prosecutionCaseFromCommand) {
        final List<uk.gov.justice.core.courts.Defendant> updatedDefendants = prosecutionCaseFromCommand.getDefendants().stream()
                .map(defendant -> getUpdatedDefendant(defendant))
                .collect(Collectors.toList());

        final Map<UUID, Integer> listingMap = this.prosecutionCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getListingNumber()))
                .collect(toMap(uk.gov.justice.core.courts.Offence::getId, uk.gov.justice.core.courts.Offence::getListingNumber, Math::max));

        return updatedDefendants.stream().map(defendant -> uk.gov.justice.core.courts.Defendant.defendant()
                .withValuesFrom(defendant)
                .withOffences(defendant.getOffences().stream().map(offence -> uk.gov.justice.core.courts.Offence.offence()
                        .withValuesFrom(offence)
                        .withListingNumber(listingMap.get(offence.getId()))
                        .build()).collect(toList()))
                .build()).collect(toList());
    }

    private uk.gov.justice.core.courts.Defendant getUpdatedDefendant(final uk.gov.justice.core.courts.Defendant defendant) {
        final List<uk.gov.justice.core.courts.Offence> updatedOffences = new ArrayList<>();
        updateCurrentOffencesWithProceedingsConcludedStatus(defendant.getId(), defendant.getOffences(), updatedOffences);

        //defendant proceedingsConcluded status is true when all the offences have a FINAL result; offence may be resulted in multiple hearings
        final boolean proceedingConcluded = checkIfDefendantProceedingsConcluded(defendant.getId(), updatedOffences);
        return getDefendant(defendant, updatedOffences, proceedingConcluded);
    }

    private void updateCurrentOffencesWithProceedingsConcludedStatus(final UUID defendantId, final List<uk.gov.justice.core.courts.Offence> defendantOffences,
                                                                     final List<uk.gov.justice.core.courts.Offence> updatedOffences) {

        if (nonNull(defendantOffences)) {
            defendantOffences.forEach(offence -> {
                boolean isOffenceConcluded;
                //check current offence is newly resulted and has FINAL result
                if (hasNewAmendment(offence)) {
                    isOffenceConcluded = isConcluded(offence);
                } else {
                    isOffenceConcluded = isOffencePreviouslyConcluded(defendantId, offence.getId());
                }
                getUpdatedOffence(updatedOffences, offence, isOffenceConcluded);
            });
        }
    }

    /**
     * Check for all offences of defendant are resulted to FINAL
     *
     * @param defendantId            - Id of the defendant
     * @param currentUpdatedOffences - All offences of the defendant updated with proceedings concluded  flag
     * @return True if all the offences are resulted to FINAL.
     */
    private boolean checkIfDefendantProceedingsConcluded(final UUID defendantId, final List<uk.gov.justice.core.courts.Offence> currentUpdatedOffences) {

        final List<uk.gov.justice.core.courts.Offence> defendantAllOffences = defendantCaseOffences.get(defendantId);
        final Map<UUID, Boolean> offenceProceedingsConcludedMap = new HashMap<>();

        if (nonNull(defendantAllOffences)) {
            defendantAllOffences.forEach(previousOffence -> {
                final uk.gov.justice.core.courts.Offence currentOffence = getOffenceById(previousOffence.getId(), currentUpdatedOffences);

                if (hasNewAmendment(currentOffence)) {
                    offenceProceedingsConcludedMap.put(previousOffence.getId(), isConcluded(currentOffence));
                } else {
                    //previously resulted but not in current hearing / payload
                    if (isOffencePreviouslyConcluded(defendantId, previousOffence.getId())) {
                        offenceProceedingsConcludedMap.put(previousOffence.getId(), TRUE);
                    } else {
                        //not yet resulted - concluded is false
                        offenceProceedingsConcludedMap.put(previousOffence.getId(), isConcluded(previousOffence));
                    }
                }
            });
        }

        return offenceProceedingsConcludedMap.entrySet().stream().allMatch(offenceProceedingsConcludedStatus -> TRUE.equals(offenceProceedingsConcludedStatus.getValue()));
    }

    private boolean isOffencePreviouslyConcluded(UUID defendantId, UUID offenceId) {
        final Map<UUID, uk.gov.justice.core.courts.Offence> resultedOffenceMap = defendantOffencesResultedOffenceLevel.get(defendantId);
        return nonNull(resultedOffenceMap) && nonNull(resultedOffenceMap.get(offenceId)) && isConcluded(resultedOffenceMap.get(offenceId));
    }

    /**
     * Returns a map of Defendants who are not part of the current hearing and their related
     * Proceedings concluded status
     *
     * @param caseDefendantsFromDefendantProceedingsConcluded
     * @param caseDefendantsFromCurrentHearing
     * @return Map<UUID ,   Boolean>
     */
    private Map<UUID, Boolean> getOtherCaseDefendantsNotRepresentedOnCurrentHearing(final Map<UUID, Boolean> caseDefendantsFromDefendantProceedingsConcluded,
                                                                                    final List<UUID> caseDefendantsFromCurrentHearing) {
        Map<UUID, Boolean> otherCaseDefendantsNotRepresentedOnCurrentHearing = null;

        if (nonNull(caseDefendantsFromDefendantProceedingsConcluded)) {
            otherCaseDefendantsNotRepresentedOnCurrentHearing = caseDefendantsFromDefendantProceedingsConcluded.entrySet().stream()
                    .filter(defendantId -> !caseDefendantsFromCurrentHearing.contains(defendantId.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return otherCaseDefendantsNotRepresentedOnCurrentHearing;
    }


    private boolean isOtherCaseDefendantsProceedingsConcludedValue(final Map<UUID, Boolean> otherCaseDefendantsNotRepresentedOnCurrentHearing) {
        if (nonNull(otherCaseDefendantsNotRepresentedOnCurrentHearing)) {
            return otherCaseDefendantsNotRepresentedOnCurrentHearing.values().stream().allMatch(proceedingsConcluded -> proceedingsConcluded.equals(TRUE));
        }
        return true;
    }

    public Stream<Object> createPetForm(final UUID petId, final UUID caseId, final UUID formId, final Optional<Boolean> isYouthOptional, List<UUID> defendantIds, final String petFormData,
                                        final UUID userId, final UUID submissionId, final String userName, final FormType formType) {

        return apply(Stream.of(PetFormCreated.petFormCreated()
                .withCaseId(caseId)
                .withPetDefendants(buildPetDefendants(defendantIds))
                .withFormId(formId)
                .withPetFormData(petFormData)
                .withPetId(petId)
                .withUserId(userId)
                .withIsYouth(isYouthOptional.orElse(false))
                .withSubmissionId(submissionId)
                .withFormType(formType)
                .withUserName(userName)
                .build()));
    }

    public Stream<Object> updatePetFormForDefendant(final UUID petId, final UUID caseId, final UUID defendantId, final String defendantData, final UUID userId) {

        return apply(Stream.of(PetFormDefendantUpdated.petFormDefendantUpdated()
                .withPetId(petId)
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withDefendantData(defendantData)
                .withUserId(userId)
                .build()));
    }

    public Stream<Object> receivePetForm(final UUID petId, final UUID caseId, final UUID formId, List<UUID> defendantIds) {


        return apply(Stream.of(PetFormReceived.petFormReceived()
                .withCaseId(caseId)
                .withPetDefendants(buildPetDefendants(defendantIds))
                .withFormId(formId)
                .withPetId(petId)
                .build()));
    }

    private List<PetDefendants> buildPetDefendants(final List<UUID> defendantIds) {
        return defendantIds.stream().map(entry -> PetDefendants.petDefendants()
                .withDefendantId(entry)
                .build()).collect(toList());
    }

    public Stream<Object> updatePetForm(final UUID caseId, final String petFormData,
                                        final UUID petId, final UUID userId) {

        return apply(Stream.of(PetFormUpdated.petFormUpdated()
                .withCaseId(caseId)
                .withPetFormData(petFormData)
                .withPetId(petId)
                .withUserId(userId)
                .build()));
    }

    public Stream<Object> releasePetForm(final UUID caseId, final UUID petId, final UUID userId) {

        return apply(Stream.of(PetFormReleased.petFormReleased()
                .withCaseId(caseId)
                .withPetId(petId)
                .withUserId(userId)
                .build()));
    }

    public Stream<Object> finalisePetForm(final UUID caseId,
                                          final UUID petId, final UUID userId,
                                          final List<String> finalisedFormData) {
        final UUID submissionId = nonNull(formMap.get(petId)) ? formMap.get(petId).getSubmissionId() : null;
        if (isNull(prosecutionCase)) {
            return apply(Stream.of(PetOperationFailed.petOperationFailed()
                    .withCaseId(caseId)
                    .withPetId(petId)
                    .withCommand("finalise-pet-form")
                    .withMessage(format("ProsecutionCase(%s) does not exists.", caseId))
                    .withSubmissionId(submissionId)
                    .build()));
        }

        final ProsecutionCase petProsecutionCase = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(filterPetDefendants(prosecutionCase.getDefendants()))
                .build();

        return apply(Stream.of(PetFormFinalised.petFormFinalised()
                .withCaseId(caseId)
                .withProsecutionCase(petProsecutionCase)
                .withPetId(petId)
                .withUserId(userId)
                .withFinalisedFormData(finalisedFormData)
                .withSubmissionId(submissionId)
                .withMaterialId(UUID.randomUUID())
                .build()));
    }

    public Stream<Object> updatePetDetail(final UUID caseId,
                                          final UUID petId, final Optional<Boolean> isYouthOptional, final List<PetDefendants> petDefendants, final UUID userId) {


        return apply(Stream.of(PetDetailUpdated.petDetailUpdated()
                .withCaseId(caseId)
                .withPetId(petId)
                .withIsYouth(isYouthOptional.orElse(false))
                .withPetDefendants(petDefendants)
                .withUserId(userId)
                .build()));

    }

    public Stream<Object> receivePetDetail(final UUID caseId,
                                           final UUID petId, final List<PetDefendants> petDefendants, final UUID userId) {

        return apply(Stream.of(PetDetailReceived.petDetailReceived()
                .withCaseId(caseId)
                .withPetId(petId)
                .withPetDefendants(petDefendants)
                .withUserId(userId)
                .build()));

    }

    public Stream<Object> addOnlinePleaAllocation(final PleasAllocationDetails pleasAllocation, final UUID hearingId) {
        return apply(Stream.of(
            OnlinePleaAllocationAdded.onlinePleaAllocationAdded()
                    .withCaseId(pleasAllocation.getCaseId())
                    .withDefendantId(pleasAllocation.getDefendantId())
                    .withHearingId(hearingId)
                    .withOffences(pleasAllocation.getOffencePleas())
                    .build()));
    }

    public Optional<Stream<Object>> updateOnlinePleaAllocation(final PleasAllocationDetails pleasAllocation) {
        return ofNullable(onlinePleaAllocations.get(pleasAllocation.getDefendantId())).map( originalAllocation ->
            apply(Stream.of(
                OnlinePleaAllocationUpdated.onlinePleaAllocationUpdated()
                        .withCaseId(originalAllocation.getCaseId())
                        .withDefendantId(originalAllocation.getDefendantId())
                        .withHearingId(originalAllocation.getHearingId())
                        .withOffences(pleasAllocation.getOffencePleas())
                        .build())));
    }

    public OnlinePleasAllocation getOnlinePleasAllocation (final UUID defendantId) {
        return onlinePleaAllocations.get(defendantId);
    }

    private void addOnlinePleaAllocation(final OnlinePleaAllocationAdded e) {
        final OnlinePleasAllocation pleaAllocation = OnlinePleasAllocation.onlinePleasAllocation()
                .withCaseId(e.getCaseId())
                .withDefendantId(e.getDefendantId())
                .withHearingId(e.getHearingId())
                .withOffences(e.getOffences()).build();
        onlinePleaAllocations.put(e.getDefendantId(), pleaAllocation);
    }


    private void updateOnlinePleaAllocation(final OnlinePleaAllocationUpdated e) {
        final OnlinePleasAllocation pleaAllocation = OnlinePleasAllocation.onlinePleasAllocation()
                .withValuesFrom(onlinePleaAllocations.get(e.getDefendantId()))
                .withOffences(e.getOffences()).build();

        onlinePleaAllocations.put(e.getDefendantId(), pleaAllocation);
    }



    private List<uk.gov.justice.core.courts.Defendant> filterPetDefendants(final List<uk.gov.justice.core.courts.Defendant> defendants) {
        return defendants.stream()
                .map(this::buildDefendantForPet)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    public Stream<Object> recordOnlinePlea(final PleadOnline pleadOnline) {
        final Stream<Object> failureEvents = createRejectionEventsForOnlinePlea(pleadOnline);

        if (nonNull(failureEvents)) {
            return failureEvents;
        }

        return createSuccessEventsForOnlinePlea(pleadOnline);
    }

    public Stream<Object> createRejectionEventsForOnlinePlea(final PleadOnline pleadOnline) {
        if (isNull(this.prosecutionCase)) {
            LOGGER.warn("Case not found. CaseId: {}", pleadOnline.getCaseId());
            return of(CaseNotFound.caseNotFound()
                    .withCaseId(pleadOnline.getCaseId())
                    .build());
        } else if (isDefendantNotFound(pleadOnline.getDefendantId())) {
            LOGGER.warn("Defendant not found. DefendantId: {}", pleadOnline.getDefendantId());
            return of(DefendantNotFound.defendantNotFound()
                    .withCaseId(pleadOnline.getCaseId())
                    .withDefendantId(pleadOnline.getDefendantId())
                    .build());
        } else if (isOffenceAlreadyPlead(pleadOnline.getOffences(), pleadOnline.getDefendantId())) {
            LOGGER.warn("offence already plead. CaseId: {}", pleadOnline.getCaseId());
            return of(OnlinePleaCaseUpdateRejected.onlinePleaCaseUpdateRejected()
                    .withCaseId(pleadOnline.getCaseId())
                    .withReason(PLEA_ALREADY_SUBMITTED)
                    .build());
        }

        return null;
    }

    public boolean isOffenceAlreadyPlead(final List<uk.gov.moj.cpp.progression.plea.json.schemas.Offence> offences, final UUID defendantId) {
        return ofNullable(offences).orElse(emptyList()).stream()
                .anyMatch(offence -> isOffencePlead(offence.getId(), getExistingOffencesOfTheDefendant(defendantId)));
    }

    public Set<UUID> getLinkedHearingIds() {
        return unmodifiableSet(hearingIds);
    }

    private boolean isOffencePlead(final String offenceId, final List<uk.gov.justice.core.courts.Offence> existingOffencesOfTheDefendant) {
        return existingOffencesOfTheDefendant.stream()
                .filter(offence -> offence.getId().toString().equals(offenceId))
                .findAny().map(uk.gov.justice.core.courts.Offence::getOnlinePleaReceived).orElse(false);
    }

    private List<uk.gov.justice.core.courts.Offence> getExistingOffencesOfTheDefendant(final UUID defendantId) {
        return prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getId().equals(defendantId))
                .findAny().map(uk.gov.justice.core.courts.Defendant::getOffences).orElse(emptyList());

    }

    private boolean isDefendantNotFound(final UUID defendantId) {
        return this.prosecutionCase.getDefendants().stream().noneMatch(defendant -> defendant.getId().equals(defendantId));
    }

    private Stream<Object> createSuccessEventsForOnlinePlea(final PleadOnline pleadOnline) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(OnlinePleaRecorded.onlinePleaRecorded()
                .withCaseId(pleadOnline.getCaseId())
                .withPleadOnline(pleadOnline)
                .build());
        streamBuilder.add(FinanceDocumentForOnlinePleaSubmitted.financeDocumentForOnlinePleaSubmitted()
                .withCaseId(pleadOnline.getCaseId())
                .withPleadOnline(pleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(ZonedDateTime.of(LocalDate.now(), LocalTime.now().plusHours(1), ZoneId.of("Europe/London"))))
                .build());
        streamBuilder.add(PleaDocumentForOnlinePleaSubmitted.pleaDocumentForOnlinePleaSubmitted()
                .withCaseId(pleadOnline.getCaseId())
                .withPleadOnline(pleadOnline)
                .withDateOfPlea(DATE_FORMATTER.format(LocalDate.now()))
                .withTimeOfPlea(TIME_FORMATTER.format(ZonedDateTime.of(LocalDate.now(), LocalTime.now().plusHours(1), ZoneId.of("Europe/London"))))
                .build());
        final Optional<TemplateType> templateType = sendEmailNotificationToDefendant(pleadOnline);
        if (templateType.isPresent()) {
            streamBuilder.add(NotificationSentForDefendantDocument.notificationSentForDefendantDocument()
                    .withCaseId(pleadOnline.getCaseId())
                    .withUrn(pleadOnline.getUrn())
                    .withEmail(getDefendantEmail(pleadOnline))
                    .withPostcode(getDefendantPostcode(pleadOnline))
                    .withTemplateType(templateType.get())
                    .withNotificationId(randomUUID())
                    .build());
        }

        return apply(streamBuilder.build());
    }

    public Stream<Object> createOnlinePleaPcqVisited(final PleadOnlinePcqVisited pleadOnlinePcqVisited) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(OnlinePleaPcqVisitedRecorded.onlinePleaPcqVisitedRecorded()
                .withCaseId(pleadOnlinePcqVisited.getCaseId())
                .withPleadOnlinePcqVisited(pleadOnlinePcqVisited)
                .build());
        return apply(streamBuilder.build());
    }

    private Optional<uk.gov.justice.core.courts.Defendant> buildDefendantForPet(final uk.gov.justice.core.courts.Defendant defendant) {

        return Optional.of(uk.gov.justice.core.courts.Defendant.defendant()
                .withValuesFrom(defendant)
                .build());
    }

    private void onPetFormCreated(final PetFormCreated petFormCreated) {
        updatePetFormState(petFormCreated.getPetId(), petFormCreated.getPetDefendants(), petFormCreated.getFormType());
    }

    private void updatePetFormState(final UUID petId, final List<PetDefendants> formDefendants, final FormType formType) {
        final Form form = formMap.containsKey(petId) ? formMap.get(petId) : new Form();
        form.setFormType(FormType.PET.equals(formType) ? formType : FormType.PET);
        form.setPetDefendants(formDefendants);
        form.setCourtFormId(petId);
        form.setFormLockStatus(new FormLockStatus(false, null, null, null));
        formMap.put(petId, form);
    }

    public Stream<Object> updateFormDefendants(final UUID courtFormId, final UUID caseId, final List<UUID> defendantIds, final UUID userId, final FormType formType) {
        if (!formMap.containsKey(courtFormId)) {
            return apply(Stream.of(formOperationFailed()
                    .withCourtFormId(courtFormId)
                    .withFormType(formType)
                    .withCaseId(caseId)
                    .withOperation(FORM_UPDATE_COMMAND_NAME)
                    .withMessage(UPDATE_BCM_DEFENDANT_OPERATION_IS_FAILED)
                    .build()));
        }

        return apply(Stream.of(formDefendantsUpdated()
                .withCourtFormId(courtFormId)
                .withFormType(formType)
                .withCaseId(caseId)
                .withFormDefendants(buildDefendants(defendantIds))
                .withUserId(userId)
                .build()));
    }

    public Stream<Object> createForm(final UUID courtFormId, final UUID caseId, final UUID formId, final List<UUID> defendantIds, final String formData, final UUID userId, final FormType formType, final UUID submissionId, final String userName) {
        if (formMap.containsKey(courtFormId)) {
            return apply(Stream.of(formOperationFailed()
                    .withCourtFormId(courtFormId)
                    .withFormType(formType)
                    .withCaseId(caseId)
                    .withOperation(FORM_CREATION_COMMAND_NAME)
                    .withMessage(MESSAGE_FOR_DUPLICATE_COURT_FORM_ID)
                    .withSubmissionId(submissionId)
                    .build()));
        }

        return apply(Stream.of(formCreated()
                .withCourtFormId(courtFormId)
                .withFormType(formType)
                .withFormData(formData)
                .withCaseId(caseId)
                .withFormDefendants(buildDefendants(defendantIds))
                .withFormId(formId)
                .withFormData(formData)
                .withUserId(userId)
                .withSubmissionId(submissionId)
                .withUserName(userName)
                .build()));
    }

    public Stream<Object> updateForm(final UUID caseId, final String formData,
                                     final UUID courtFormId, final UUID userId) {
        if (!formMap.containsKey(courtFormId)) {
            return apply(Stream.of(formOperationFailed()
                    .withCaseId(caseId)
                    .withCourtFormId(courtFormId)
                    .withOperation(FORM_UPDATE_COMMAND_NAME)
                    .withMessage(format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, courtFormId))
                    .build()));
        }

        return apply(Stream.of(formUpdated()
                .withCaseId(caseId)
                .withFormData(formData)
                .withCourtFormId(courtFormId)
                .withUserId(userId)
                .withFormType(formMap.get(courtFormId).getFormType())
                .build()));
    }


    public Stream<Object> finaliseForm(final UUID caseId, final UUID courtFormId, final UUID userId, final List<String> finalisedFormData, final ZonedDateTime hearingDateTime) {
        final UUID submissionId = nonNull(formMap.get(courtFormId)) ? formMap.get(courtFormId).getSubmissionId() : null;
        if (isNull(prosecutionCase)) {
            return apply(Stream.of(formOperationFailed()
                    .withCaseId(caseId)
                    .withCourtFormId(courtFormId)
                    .withOperation(FORM_FINALISATION_COMMAND_NAME)
                    .withMessage(format(MESSAGE_FOR_PROSECUTION_NULL, caseId))
                    .withSubmissionId(submissionId)
                    .build()));
        }

        if (!formMap.containsKey(courtFormId)) {
            return apply(Stream.of(formOperationFailed()
                    .withCaseId(caseId)
                    .withCourtFormId(courtFormId)
                    .withOperation(FORM_FINALISATION_COMMAND_NAME)
                    .withMessage(format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, courtFormId))
                    .withSubmissionId(submissionId)
                    .build()));
        }
        final ProsecutionCaseIdentifier caseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        final String caseURN = isBlank(caseIdentifier.getCaseURN()) ? caseIdentifier.getProsecutionAuthorityReference() : caseIdentifier.getCaseURN();


        final List<FormFinalised> formFinalisedList = finalisedFormData.stream()
                .map(finalisedDataString -> formFinalised()
                        .withCaseId(caseId)
                        .withFormType(formMap.get(courtFormId).getFormType())
                        .withCourtFormId(courtFormId)
                        .withUserId(userId)
                        .withFinalisedFormData(singletonList(finalisedDataString))
                        .withCaseURN(caseURN)
                        .withSubmissionId(submissionId)
                        .withMaterialId(UUID.randomUUID())
                        .withHearingDateTime(hearingDateTime)
                        .build())
                .collect(toList());

        return apply(formFinalisedList.stream().map(formFinalised -> formFinalised));
    }

    public Stream<Object> requestEditForm(final UUID caseId, final UUID courtFormId, final UUID userId, final Map<FormType, Integer> durationMapByFormType,
                                          final ZonedDateTime requestEditTime, final boolean extend, final int extendTime) {
        if (!formMap.containsKey(courtFormId)) {
            return apply(Stream.of(formOperationFailed()
                    .withCaseId(caseId)
                    .withCourtFormId(courtFormId)
                    .withOperation(FORM_EDIT_COMMAND_NAME)
                    .withMessage(format(MESSAGE_FOR_COURT_FORM_ID_NOT_PRESENT, courtFormId))
                    .build()));
        }

        final Form form = formMap.get(courtFormId);
        final FormLockStatus formLockStatus = form.getFormLockStatus();

        final EditFormRequested.Builder editFormRequested = editFormRequested()
                .withCaseId(caseId)
                .withCourtFormId(courtFormId);
        final LockStatus.Builder lockStatusBuilder = lockStatus()
                .withLockRequestedBy(userId);

        if (formLockStatus.getLockExpiryTime() == null || hasLockExpired(requestEditTime, formLockStatus.getLockExpiryTime())) {
            lockStatusBuilder
                    .withExpiryTime(extend ? calculateExpiryTimeWithExtension(requestEditTime, extendTime) : requestEditTime.plusMinutes(durationMapByFormType.get(form.getFormType())))
                    .withIsLocked(false)
                    .withLockedBy(userId)
                    .withIsLockAcquirable(true);
        } else if (userId.equals(formLockStatus.getLockedBy())) {
            lockStatusBuilder
                    .withExpiryTime(extend ? calculateExpiryTimeWithExtension(requestEditTime, extendTime) : formLockStatus.getLockExpiryTime())
                    .withIsLocked(false)
                    .withLockedBy(formLockStatus.getLockedBy())
                    .withIsLockAcquirable(true);
        } else {
            lockStatusBuilder
                    .withIsLocked(true)
                    .withExpiryTime(formLockStatus.getLockExpiryTime())
                    .withLockedBy(formLockStatus.getLockedBy());
        }

        return apply(Stream.of(editFormRequested.withLockStatus(lockStatusBuilder.build()).build()));
    }


    public UUID getLatestHearingId() {
        return latestHearingId;
    }

    private ZonedDateTime calculateExpiryTimeWithExtension(final ZonedDateTime fromTime, final int extendTime) {
        return fromTime.plusMinutes(extendTime);
    }

    private boolean hasLockExpired(final ZonedDateTime requestedLockTime, final ZonedDateTime lockExpiryTime) {
        return requestedLockTime.compareTo(lockExpiryTime) >= 0;
    }

    private List<FormDefendants> buildDefendants(final List<UUID> defendantIds) {
        return defendantIds.stream().map(entry -> formDefendants()
                .withDefendantId(entry)
                .build()).collect(toList());
    }

    private void updateFormOnFormUpdate(final FormUpdated formUpdated) {
        final Form form = formMap.get(formUpdated.getCourtFormId());
        final FormLockStatus lockStatus = form.getFormLockStatus();
        if (formUpdated.getUserId().equals(lockStatus.getLockedBy())) {
            lockStatus.setLockedBy(null);
            lockStatus.setLocked(false);
            lockStatus.setLockExpiryTime(null);
            lockStatus.setLockRequestedBy(null);
            form.setFormLockStatus(lockStatus);
            formMap.put(formUpdated.getCourtFormId(), form);
        }
    }

    private void updateFormOnPetFormRelease(final PetFormReleased petFormReleased) {
        final Form form = formMap.get(petFormReleased.getPetId());
        final FormLockStatus lockStatus = form.getFormLockStatus();
        if (petFormReleased.getUserId().equals(lockStatus.getLockedBy())) {
            lockStatus.setLockedBy(null);
            lockStatus.setLocked(false);
            lockStatus.setLockExpiryTime(null);
            lockStatus.setLockRequestedBy(null);
            form.setFormLockStatus(lockStatus);
            formMap.put(petFormReleased.getPetId(), form);
        }
    }

    private void updateFormOnPetFormDefendantUpdated(final PetFormDefendantUpdated petFormDefendantUpdated) {
        final Form form = formMap.get(petFormDefendantUpdated.getPetId());
        final FormLockStatus lockStatus = form.getFormLockStatus();
        if (petFormDefendantUpdated.getUserId().equals(lockStatus.getLockedBy())) {
            lockStatus.setLockedBy(null);
            lockStatus.setLocked(false);
            lockStatus.setLockExpiryTime(null);
            lockStatus.setLockRequestedBy(null);
            form.setFormLockStatus(lockStatus);
            formMap.put(petFormDefendantUpdated.getPetId(), form);
        }
    }

    private void editFormRequest(final EditFormRequested editFormRequested) {
        final Form form = formMap.get(editFormRequested.getCourtFormId());
        final FormLockStatus lockStatus = form.getFormLockStatus();

        if (nonNull(editFormRequested.getLockStatus().getIsLockAcquirable()) && editFormRequested.getLockStatus().getIsLockAcquirable()) {
            lockStatus.setLockedBy(editFormRequested.getLockStatus().getLockRequestedBy());
            lockStatus.setLockExpiryTime(editFormRequested.getLockStatus().getExpiryTime());
            form.setFormLockStatus(lockStatus);
            formMap.put(editFormRequested.getCourtFormId(), form);
        }

    }

    private void onFormCreated(final FormCreated formCreated) {
        updateFormState(formCreated.getCourtFormId(), formCreated.getFormDefendants(), formCreated.getFormType());
    }

    private void onFormDefendantsUpdated(final FormDefendantsUpdated formDefendantsUpdated) {
        if (!formMap.containsKey(formDefendantsUpdated.getCourtFormId())) {
            return;
        }

        final Form form = formMap.get(formDefendantsUpdated.getCourtFormId());
        form.setFormDefendants(formDefendantsUpdated.getFormDefendants());
    }

    public Stream<Object> handleOnlinePleaDocumentCreation(final HandleOnlinePleaDocumentCreation handleOnlinePleaDocumentCreation) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (COMPANYONLINEPLEA == handleOnlinePleaDocumentCreation.getPleaNotificationType() || INDIVIDUALONLINEPLEA == handleOnlinePleaDocumentCreation.getPleaNotificationType()) {
            final Optional<String> prosecutorEmail = getProsecutorEmail();
            if (prosecutorEmail.isPresent()) {
                streamBuilder.add(NotificationSentForPleaDocument.notificationSentForPleaDocument()
                        .withCaseId(handleOnlinePleaDocumentCreation.getCaseId())
                        .withEmail(prosecutorEmail.get())
                        .withPleaNotificationType(handleOnlinePleaDocumentCreation.getPleaNotificationType())
                        .withSystemDocGeneratorId(handleOnlinePleaDocumentCreation.getSystemDocGeneratorId())
                        .withUrn(this.prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .withNotificationId(randomUUID())
                        .build());
            } else {
                streamBuilder.add(NotificationSentForPleaDocumentFailed.notificationSentForPleaDocumentFailed()
                        .withCaseId(handleOnlinePleaDocumentCreation.getCaseId())
                        .withErrorMessage(EMAIL_NOT_FOUND)
                        .withPleaNotificationType(handleOnlinePleaDocumentCreation.getPleaNotificationType())
                        .withSystemDocGeneratorId(handleOnlinePleaDocumentCreation.getSystemDocGeneratorId())
                        .build());
            }
        }

        streamBuilder.add(OnlinePleaDocumentUploadedAsCaseMaterial.onlinePleaDocumentUploadedAsCaseMaterial()
                .withCaseId(this.prosecutionCase.getId())
                .withFileId(handleOnlinePleaDocumentCreation.getSystemDocGeneratorId())
                .withMaterialId(randomUUID())
                .withDefendantId(handleOnlinePleaDocumentCreation.getDefendantId())
                .withPleaNotificationType(handleOnlinePleaDocumentCreation.getPleaNotificationType())
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> extendCaseToExistingHearingForAdhocHearing(final CourtHearingRequest existingHearing, final Boolean sendNotificationToParties) {
        final Set<UUID> defendantsIds = existingHearing.getListDefendantRequests().stream().map(ListDefendantRequest::getDefendantId).collect(Collectors.toSet());

        final Set<UUID> offenceIds = existingHearing.getListDefendantRequests().stream()
                .map(ListDefendantRequest::getDefendantOffences)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        final ProsecutionCase caseForHearing = ProsecutionCase.prosecutionCase().withValuesFrom(this.getProsecutionCase())
                .withDefendants(this.getProsecutionCase().getDefendants().stream()
                        .filter(def-> defendantsIds.contains(def.getId()))
                        .map(def -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(def)
                                .withOffences(def.getOffences().stream().filter(offence -> offenceIds.contains(offence.getId())).collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        streamBuilder.add(RelatedCaseRequestedForAdhocHearing.relatedCaseRequestedForAdhocHearing()
                .withListNewHearing(existingHearing)
                .withProsecutionCase(caseForHearing)
                .withSendNotificationToParties(sendNotificationToParties)
                .build());
        return apply(streamBuilder.build());
    }

    private Optional<String> getProsecutorEmail() {
        final ContactNumber contactNumber = this.prosecutionCase.getProsecutionCaseIdentifier().getContact();
        if (isNull(contactNumber)) {
            return Optional.empty();
        }
        return ofNullable(ofNullable(contactNumber.getPrimaryEmail()).orElse(contactNumber.getSecondaryEmail()));
    }

    private void updateFormState(final UUID courtFormId, final List<FormDefendants> formDefendants, final FormType formType) {
        Form form;
        form = formMap.containsKey(courtFormId) ? formMap.get(courtFormId) : new Form();
        form.setFormType(formType);
        form.setFormDefendants(formDefendants);
        form.setCourtFormId(courtFormId);
        form.setFormLockStatus(new FormLockStatus(false, null, null, null));
        formMap.put(courtFormId, form);
    }

    private void onPetDetailUpdated(final PetDetailUpdated petDetailUpdated) {
        if (!formMap.containsKey(petDetailUpdated.getPetId())) {
            return;
        }

        final Form form = formMap.get(petDetailUpdated.getPetId());
        form.setPetDefendants(petDetailUpdated.getPetDefendants());
    }

    private DefendantUpdate buildDefendantUpdateFromDefendant(final uk.gov.justice.core.courts.Defendant defendant) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withAliases(defendant.getAliases())
                .withAssociatedDefenceOrganisation(defendant.getAssociatedDefenceOrganisation())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withCroNumber(defendant.getCroNumber())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withIsYouth(defendant.getIsYouth())
                .withJudicialResults(defendant.getDefendantCaseJudicialResults())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withOffences(defendant.getOffences())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withPncId(defendant.getPncId())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .build();
    }

    private boolean shouldUpdateCustodialEstablishment(final UUID defendantId, final PersonDefendant updatedPersonDefendant, Map<UUID, uk.gov.justice.core.courts.Defendant> defendantsMap) {
        LOGGER.info("CaseAggregateInfo: shouldUpdateCustodialEstablishment defendant {} : in shouldUpdateCustodialEstablishment-start", defendantId);

        if (isNull(updatedPersonDefendant) || isNull(defendantsMap.get(defendantId))) {
            LOGGER.info("CaseAggregateInfo: shouldUpdateCustodialEstablishment defendant {} : null check failed. Either updatedPersonDefendant is null ", defendantId);
            LOGGER.info("CaseAggregateInfo: shouldUpdateCustodialEstablishment defendant {} : defendantsMap.get(defendantId) is null", defendantId);
            return false;
        }

        final CustodialEstablishment updatedCustodialEstablishment = updatedPersonDefendant.getCustodialEstablishment();
        final uk.gov.justice.core.courts.Defendant defendant = defendantsMap.get(defendantId);

        if (isNull(defendant.getPersonDefendant())) {
            LOGGER.info("CaseAggregateInfo: shouldUpdateCustodialEstablishment defendant {} : defendant.getPersonDefendant() is null", defendantId);
            return false;
        }

        final CustodialEstablishment originalCustodialEstablishment = defendant.getPersonDefendant().getCustodialEstablishment();

        if (isNull(updatedCustodialEstablishment) && isNull(originalCustodialEstablishment)) {
            return false;
        } else if (nonNull(originalCustodialEstablishment) && isNull(updatedCustodialEstablishment)) {
            return true;
        } else if (!updatedCustodialEstablishment.equals(originalCustodialEstablishment)) {
            return true;
        } else {
            LOGGER.info("CaseAggregateInfo: shouldUpdateCustodialEstablishment defendant {} : updatedCustodialEstablishment.equals(originalCustodialEstablishment) is same", defendantId);
        }
        return false;
    }

    private CustodialEstablishment buildCoreCourtCustodialEstablishment(uk.gov.moj.cpp.progression.command.CustodialEstablishment custodialEstablishment) {
        return CustodialEstablishment.custodialEstablishment()
                .withName(custodialEstablishment.getName())
                .withId(custodialEstablishment.getId())
                .withCustody(custodialEstablishment.getCustody())
                .build();
    }

    private uk.gov.justice.core.courts.Offence updateLaaApplicationReference(final UUID defendantId, final uk.gov.justice.core.courts.Offence offence) {
        final uk.gov.justice.core.courts.Offence.Builder builder = uk.gov.justice.core.courts.Offence.offence().withValuesFrom(offence);
        if(nonNull(offence.getCount())) {
            final Optional<uk.gov.justice.core.courts.Defendant> defendant = this.getProsecutionCase().getDefendants().stream()
                    .filter(caseDefendant -> caseDefendant.getId().equals(defendantId))
                    .findFirst();
            if (defendant.isPresent()) {
                final Optional<uk.gov.justice.core.courts.Offence> offenceWithLAAReference = defendant.get().getOffences().stream()
                        .filter(existingOffence -> nonNull(existingOffence.getLaaApplnReference()))
                        .findFirst();
                if (offenceWithLAAReference.isPresent()) {
                    builder.withLaaApplnReference(offenceWithLAAReference.get().getLaaApplnReference()).build();
                }
            }
        }
        return builder.build();
    }

    @SuppressWarnings("squid:S1188")
    private void populateReportingRestrictionsForOffences(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final uk.gov.justice.core.courts.Defendant defendant, final List<ListHearingRequest> listHearingRequests) {
        final Function<JsonObject, String> offenceKey = offenceJsonObject -> offenceJsonObject.getString("cjsOffenceCode");

        final List<uk.gov.justice.core.courts.Offence> offencesWithReportingRestriction = new ArrayList<>();
        for (final uk.gov.justice.core.courts.Offence offence : defendant.getOffences()) {
            populateReportingRestrictionForOffence(referenceDataOffencesJsonObjectOptional, defendant, offenceKey, offencesWithReportingRestriction, offence, listHearingRequests);
        }
        if (isNotEmpty(defendant.getOffences())) {
            defendant.getOffences().clear();
            defendant.getOffences().addAll(offencesWithReportingRestriction);
        }
    }

    private void populateReportingRestrictionForOffence(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final uk.gov.justice.core.courts.Defendant defendant, final Function<JsonObject, String> offenceKey, final List<uk.gov.justice.core.courts.Offence> offencesWithReportingRestriction, final uk.gov.justice.core.courts.Offence offence, final List<ListHearingRequest> listHearingRequests) {
        final List<ReportingRestriction> reportingRestrictions = new ArrayList<>();
        populateYouthReportingRestriction(defendant, reportingRestrictions, listHearingRequests);
        final uk.gov.justice.core.courts.Offence.Builder builder = new uk.gov.justice.core.courts.Offence.Builder().withValuesFrom(offence);
        populateSexualReportingRestriction(referenceDataOffencesJsonObjectOptional, offenceKey, offence, reportingRestrictions, builder);

        if (isNotEmpty(offence.getReportingRestrictions())) {
            reportingRestrictions.addAll(offence.getReportingRestrictions());
        }

        if (isNotEmpty(reportingRestrictions)) {
            builder.withReportingRestrictions(dedupReportingRestrictions(reportingRestrictions));
        }

        offencesWithReportingRestriction.add(builder.build());
    }

    private void populateSexualReportingRestriction(final Optional<List<JsonObject>> referenceDataOffencesJsonObjectOptional, final Function<JsonObject, String> offenceKey, final uk.gov.justice.core.courts.Offence offence, final List<ReportingRestriction> reportingRestrictions, final uk.gov.justice.core.courts.Offence.Builder builder) {
        if (referenceDataOffencesJsonObjectOptional.isPresent()) {
            final Map<String, JsonObject> offenceCodeMap = referenceDataOffencesJsonObjectOptional.get().stream().collect(Collectors.toMap(offenceKey, Function.identity()));
            final JsonObject referenceDataOffenceInfo = offenceCodeMap.get(offence.getOffenceCode());
            if (nonNull(referenceDataOffenceInfo)) {
                builder.withEndorsableFlag(referenceDataOffenceInfo.getBoolean(ENDORSABLE_FLAG, false));
            }
            if (nonNull(referenceDataOffenceInfo) && equalsIgnoreCase(referenceDataOffenceInfo.getString("reportRestrictResultCode", StringUtils.EMPTY), SEXUAL_OFFENCE_RR_CODE)) {
                reportingRestrictions.add(new ReportingRestriction.Builder()
                        .withId(randomUUID())
                        .withLabel(SEXUAL_OFFENCE_RR_LABEL)
                        .withOrderedDate(LocalDate.now())
                        .build());
            }
        }
    }

    private void populateYouthReportingRestriction(final uk.gov.justice.core.courts.Defendant defendant, final List<ReportingRestriction> reportingRestrictions, final List<ListHearingRequest> listHearingRequests) {
        final LocalDate earliestHearingDate = HearingHelper.getEarliestListedStartDateTime(listHearingRequests).toLocalDate();

        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()) && LocalDateUtils.isYouth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), earliestHearingDate).booleanValue()) {
            reportingRestrictions.add(new ReportingRestriction.Builder()
                    .withId(randomUUID())
                    .withOrderedDate(LocalDate.now())
                    .withLabel(YOUTH_RESTRICTION).build());
        }
    }

    public Stream<Object> updateCivilFees(final UUID caseId, final List<CivilFees> civilFees) {
        return apply(Stream.of(CivilFeesUpdated
                .civilFeesUpdated()
                .withCaseId(caseId)
                .withCivilFees(civilFees)
                .build()));
    }

    public Stream<Object> updateCaseGroupInfo(final Boolean isGroupMaster, final Boolean isGroupMember) {
        return apply(Stream.of(CaseGroupInfoUpdated.caseGroupInfoUpdated()
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withValuesFrom(this.prosecutionCase)
                        .withIsGroupMember(isGroupMember)
                        .withIsGroupMaster(isGroupMaster)
                        .build())
                .build()));
    }
}
