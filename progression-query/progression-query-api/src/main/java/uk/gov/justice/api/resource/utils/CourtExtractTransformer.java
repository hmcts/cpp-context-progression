package uk.gov.justice.api.resource.utils;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.COURT_EXTRACT;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getApplicationResultsWithAmendmentsExcludingDefendantLevelResults;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getDefendantResultsWithAmendments;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getDeletedApplicationResultsWithAmendmentsExcludingDefendantLevelResults;
import static uk.gov.justice.api.resource.utils.JudicialResultTransformer.getDeletedDefendantResultsWithAmendments;
import static uk.gov.justice.api.resource.utils.OffenceTransformer.toOffences;
import static uk.gov.justice.api.resource.utils.ResultAmendmentHelper.extractAmendmentsDueToSlipRule;
import static uk.gov.justice.api.resource.utils.ResultAmendmentHelper.getResultDefinitionsInSlipRuleAmendments;
import static uk.gov.justice.api.resource.utils.TransformationHelper.getHearingsSortedByHearingDaysAsc;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.RECORD_SHEET;
import static uk.gov.justice.core.courts.HearingListingStatus.HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantAge;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantDataOfBirth;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantName;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getJudicialResults;

import uk.gov.justice.api.resource.dto.AmendmentType;
import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.HearingQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.Amendments;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.AquittedOffencesDetails;
import uk.gov.justice.progression.courts.exract.AttendanceDayAndType;
import uk.gov.justice.progression.courts.exract.AttendanceDays;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.CompanyRepresentatives;
import uk.gov.justice.progression.courts.exract.ConvictedOffencesDetails;
import uk.gov.justice.progression.courts.exract.CourtApplicationCases;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtOrderOffences;
import uk.gov.justice.progression.courts.exract.CourtOrderOffencesOffenceLevelResults;
import uk.gov.justice.progression.courts.exract.CourtOrders;
import uk.gov.justice.progression.courts.exract.CrownCourtDecisions;
import uk.gov.justice.progression.courts.exract.CustodialEstablishment;
import uk.gov.justice.progression.courts.exract.DefenceCounsels;
import uk.gov.justice.progression.courts.exract.DefenceOrganisations;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.DeletedCourtOrderOffenceOffenceLevelResults;
import uk.gov.justice.progression.courts.exract.JudiciaryNamesByRole;
import uk.gov.justice.progression.courts.exract.ParentGuardian;
import uk.gov.justice.progression.courts.exract.ProsecutionCounsels;
import uk.gov.justice.progression.courts.exract.PublishingCourt;
import uk.gov.justice.progression.courts.exract.Representation;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.HearingService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S1612", "squid:S3655", "squid:S2259", "squid:S1188", "squid:S2789", "squid:S1067", "squid:MethodCyclomaticComplexity", "pmd:NullAssignment", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1172", "common-java:DuplicatedBlocks", "java:S1192"})
public class CourtExtractTransformer {

    public static final String PRESENT_BY_PRISON_VIDEO_LINK = "Present - prison video link";
    public static final String PRESENT_BY_POLICE_VIDEO_LINK = "Present - police video link";
    public static final String PRESENT_IN_PERSON = "Present - in person";
    public static final String PRESENT_BY_VIDEO_DEFAULT = "Present - by video";
    private static final BiPredicate<Hearings, UUID> hearingsDefendantIdBiPredicate = (hearings, defendantId) -> nonNull(hearings.getYouthCourtDefendantIds()) && hearings.getYouthCourtDefendantIds().stream().anyMatch(youthDefendantId -> youthDefendantId.equals(defendantId));
    private static final BiPredicate<uk.gov.justice.core.courts.Hearing, UUID> hearingDefendantIdBiPredicate = (hearing, defendantId) -> nonNull(hearing.getYouthCourtDefendantIds()) && hearing.getYouthCourtDefendantIds().stream().anyMatch(youthDefendantId -> youthDefendantId.equals(defendantId));
    public static final String CATEGORY_INTERMEDIARY = "I";
    public static final String RD_GROUP_COMMITTED_TO_CC = "CommittedToCC";
    public static final String RD_GROUP_SENT_TO_CC = "SentToCC";
    private static final String SLIPRULE_AMENDMENT_REASON_CODE = "EO";
    public static final String CERTIFICATE_OF_CONVICTION = "CertificateOfConviction";
    public static final String CERTIFICATE_OF_ACQUITTAL = "CertificateOfAcquittal";
    public static final String AFTER_TRIAL_ON_INDICTMENT = "After trial on indictment";
    public static final String GUILTY_PLEA = "Following guilty plea";
    public static final String FOUND_TO_BE_IN_BREACH_OF_AN_ORDER_MADE_BY = "Found to be in breach of an order made by";
    private static final String DEFENDANT_LEVEL = "D";
    private static final String OFFENCE_LEVEL = "O";


    @Inject
    TransformationHelper transformationHelper;

    @Inject
    ListingQueryService listingQueryService;
    @Inject
    ReferenceDataService referenceDataService;

    @Inject
    DefenceQueryService defenceQueryService;

    @Inject
    CourtExtractHelper courtExtractHelper;

    @Inject
    private HearingQueryService hearingQueryService;

    @Inject
    private HearingService hearingService;

    @Inject
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;
    @Inject
    private ResultTextFlagBuilder resultTextFlagBuilder;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Inject
    private HearingRepository hearingRepository;


    private static final Logger LOGGER = LoggerFactory.getLogger(CourtExtractTransformer.class);
    private static final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> crownOffencesSortComparator = (offence1, offence2) -> {
        if (isNull(offence1.getCount()) || isNull(offence2.getCount()) || offence1.getCount() == 0 || offence2.getCount() == 0
                && (nonNull(offence1.getOrderIndex()) && nonNull(offence2.getOrderIndex()))) {
            return offence1.getOrderIndex().compareTo(offence2.getOrderIndex());
        }

        if (nonNull(offence1.getCount()) && nonNull(offence2.getCount())
                && nonNull(offence1.getOrderIndex()) && nonNull(offence2.getOrderIndex())) {
            int countCmp = offence1.getCount().compareTo(offence2.getCount());
            if (countCmp != 0) {
                return countCmp;
            }
            return offence1.getOrderIndex().compareTo(offence2.getOrderIndex());
        }
        return 0;
    };
    private static final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> magsOffencesSortComparator = (offence1, offence2) -> {
        if (nonNull(offence1.getOrderIndex()) && nonNull(offence2.getOrderIndex())) {
            return offence1.getOrderIndex().compareTo(offence2.getOrderIndex());
        }
        return 0;
    };

    public JsonObject getTransformedPayload(final JsonEnvelope document, final String defendantId, final String extractType, final List<String> hearingIdList, final UUID userId) throws IOException {
        final JsonObject payload = transformToTemplateConvert(document.payloadAsJsonObject(), defendantId, extractType, hearingIdList, userId);
        JsonObject newPayload = pleaValueDescriptionBuilder.rebuildPleaWithDescription(payload);
        return resultTextFlagBuilder.rebuildWithResultTextFlag(newPayload);
    }

    private JsonObject transformToTemplateConvert(final JsonObject jsonObject, final String defendantId, final String extractType, final List<String> hearingIdList, final UUID userId) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class);
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);

        final CourtExtractRequested courtExtractRequested = getCourtExtractRequested(hearingsAtAGlance, defendantId, extractType, hearingIdList, userId, prosecutionCase);

        final JsonObject result = objectToJsonObjectConverter.convert(courtExtractRequested);
        return result;
    }

    public CourtExtractRequested getCourtExtractRequested(final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId, final String extractType, final List<String> selectedHearingIdList, final UUID userId, final ProsecutionCase prosecutionCase) {
        final CourtExtractRequested.Builder courtExtract = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();
        final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().toString().equals(defendantId)).findFirst();

        final Optional<UUID> masterDefendantId = caseDefendant.map(uk.gov.justice.core.courts.Defendant::getMasterDefendantId);

        courtExtract.withExtractType(extractType);
        courtExtract.withCaseReference(transformationHelper.getCaseReference(hearingsAtAGlance.getProsecutionCaseIdentifier()));

        if (caseDefendant.isPresent()) {
            final List<AssociatedPerson> associatedPersons = caseDefendant.get().getAssociatedPersons();
            if (nonNull(associatedPersons) && !associatedPersons.isEmpty()) {
                courtExtract.withParentGuardian(transformParentGuardian(associatedPersons)); //parentGuardian
            }
        }

        final DefendantHearings defendantHearings = hearingsAtAGlance.getDefendantHearings().stream()
                .filter(dh -> dh.getDefendantId().toString().equals(defendantId)).findFirst().orElse(null);
        final List<UUID> linkedApplicationsDefendantIds = getLinkedApplicationsDefendantIds(hearingsAtAGlance, defendantId);

        defendantBuilder.withName(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantName()) ? defendantHearings.getDefendantName() : EMPTY);
        defendantBuilder.withId(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantId()) ? defendantHearings.getDefendantId() : fromString(EMPTY));

        final List<UUID> hearingIds = defendantHearings.getHearingIds();
        hearingIds.addAll(hearingsAtAGlance.getHearings().stream()
                .filter(h -> !hearingIds.contains(h.getId()))
                .filter(h -> (h.getDefendants().stream().
                        anyMatch(d -> linkedApplicationsDefendantIds.contains(d.getId()))) ||
                        (isEmpty(h.getDefendants()) &&
                                Optional.ofNullable(h.getDefendantAttendance()).stream().flatMap(Collection::stream).anyMatch( d-> d.getDefendantId().toString().equals(defendantId) || linkedApplicationsDefendantIds.contains(d.getDefendantId()))))
                .map(Hearings::getId)
                .toList());
        //add any hearings associated with breachType courtApplications
        addBreachTypeApplicationHearings(defendantId, hearingIds, hearingsAtAGlance.getHearings(), hearingsAtAGlance.getCourtApplications());

        final List<Hearings> hearingsList = hearingsAtAGlance.getHearings().stream()
                .filter(h -> hearingIds.contains(h.getId()))
                .filter(COURT_EXTRACT.equals(extractType) ? h -> selectedHearingIdList.contains(h.getId().toString()) : h -> true)
                .toList();

        if (RECORD_SHEET.equals(extractType)) {
            buildRecordSheetHearingDetails(hearingsAtAGlance, defendantId, userId, courtExtract, defendantBuilder, caseDefendant, prosecutionCase.getProsecutor(), masterDefendantId);
        } else {
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, courtExtract, defendantBuilder, hearingsList, caseDefendant.get(), prosecutionCase.getProsecutor(), extractType, linkedApplicationsDefendantIds);
        }

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            courtExtract.withIsAppealPending(transformationHelper.getAppealPendingFlag(hearingsAtAGlance.getCourtApplications()));
        }

        return courtExtract.build();
    }

    private static List<UUID> getLinkedApplicationsDefendantIds(final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId) {
        final Set<UUID> linkedApplicationsDefendantIds = new HashSet<>();
        final List<UUID> defendantApplicationIds = Optional.ofNullable(hearingsAtAGlance.getCourtApplications()).stream().flatMap(Collection::stream)
                .filter(c -> nonNull(c.getSubject()) && nonNull(c.getSubject().getMasterDefendant()) && c.getSubject().getMasterDefendant().getMasterDefendantId().toString().equals(defendantId))
                .map(CourtApplication::getId)
                .toList();

        linkedApplicationsDefendantIds.addAll(hearingsAtAGlance.getHearings().stream()
                .map(Hearings::getDefendants)
                .flatMap(List::stream)
                .filter(d -> nonNull(d.getCourtApplications()) && defendantApplicationIds.stream().anyMatch(id -> d.getCourtApplications().stream()
                        .map(uk.gov.justice.progression.courts.CourtApplications::getApplicationId)
                        .toList()
                        .contains(id)))
                .map(Defendants::getId)
                .toList());

        Optional.ofNullable(hearingsAtAGlance.getCourtApplications()).stream().flatMap(Collection::stream)
                .filter(d -> nonNull(d.getSubject()) && nonNull(d.getSubject().getMasterDefendant()) && (defendantId.equals(d.getSubject().getMasterDefendant().getMasterDefendantId().toString()) || d.getSubject().getMasterDefendant().getDefendantCase().stream().anyMatch(dc -> dc.getDefendantId().toString().equals(defendantId))))
                .map(CourtApplication::getSubject)
                .filter(Objects::nonNull)
                .map(CourtApplicationParty::getMasterDefendant)
                .forEach(m -> {
                    linkedApplicationsDefendantIds.add(m.getMasterDefendantId());
                    linkedApplicationsDefendantIds.addAll(m.getDefendantCase().stream().map(DefendantCase::getDefendantId).toList());
                });

        return new ArrayList<>(linkedApplicationsDefendantIds);
    }


    private void buildRecordSheetHearingDetails(final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId, final UUID userId, final CourtExtractRequested.Builder courtExtract,
                                                final Defendant.Builder defendantBuilder, final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant, final Prosecutor prosecutor, final Optional<UUID> masterDefendantId) {
        if (caseDefendant.isPresent() && masterDefendantId.isPresent()) {
            final List<Hearings> filteredHearings = hearingsAtAGlance.getHearings().stream()
                    .filter(h -> h.getDefendants().stream()
                            .anyMatch(d -> d.getId().toString().equals(defendantId) || d.getMasterDefendantId().toString().equals(masterDefendantId.get().toString())))
                    .filter(h -> h.getIsBoxHearing() == null || !h.getIsBoxHearing())
                    .toList();
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, courtExtract, defendantBuilder, filteredHearings, caseDefendant.get(), prosecutor, RECORD_SHEET, emptyList());
        }
    }

    private void addBreachTypeApplicationHearings(final String defendantId, final List<UUID> hearingIds,
                                                  final List<Hearings> hearings, final List<CourtApplication> courtApplications) {

        //Determine the BreachType CourtApplications that defendant associated with
        List<UUID> defendantBreachTypeApplicationIdList = new ArrayList<>();
        if (nonNull(courtApplications)) {
            final List<UUID> applicationIdList = courtApplications.stream()
                    .filter(ca -> nonNull(ca.getCourtOrder()))
                    .filter(ca -> nonNull(ca.getCourtOrder().getDefendantIds()))
                    .filter(ca -> Boolean.TRUE.equals(ca.getCourtOrder().getCanBeSubjectOfBreachProceedings())
                            && ca.getCourtOrder().getDefendantIds().contains(defendantId))
                    .map(CourtApplication::getId).toList();
            defendantBreachTypeApplicationIdList.addAll(applicationIdList);
        }

        //Add missing hearings with BreachType CourtApplications
        if (nonNull(hearings) && !defendantBreachTypeApplicationIdList.isEmpty()) {
            final List<UUID> defendantHearingsWithBreachTypeApplications = hearings.stream()
                    .filter(h -> nonNull(h.getDefendants()) && h.getDefendants().stream().anyMatch(d -> d.getId().equals(fromString(defendantId))))
                    .filter(h -> h.getDefendants().stream()
                            .filter(d -> nonNull(d.getCourtApplications()))
                            .anyMatch(d -> d.getCourtApplications().stream().anyMatch(dca -> defendantBreachTypeApplicationIdList.contains(dca.getApplicationId()))))
                    .map(Hearings::getId)
                    .toList();

            defendantHearingsWithBreachTypeApplications.forEach(hId -> {
                if (!hearingIds.contains(hId)) {
                    hearingIds.add(hId);
                }
            });
        }
    }

    public CourtExtractRequested ejectCase(final ProsecutionCase prosecutionCase, final GetHearingsAtAGlance hearingsAtAGlance, final String defendantId, final UUID userId) {
        final CourtExtractRequested.Builder ejectExtract = CourtExtractRequested.courtExtractRequested();
        final Defendant.Builder defendantBuilder = Defendant.defendant();

        final Optional<uk.gov.justice.core.courts.Defendant> caseDefendant = prosecutionCase.getDefendants().stream()
                .filter(d -> d.getId().toString().equals(defendantId)).findFirst();

        final DefendantHearings defendantHearings = hearingsAtAGlance.getDefendantHearings().stream()
                .filter(dh -> dh.getDefendantId().toString().equals(defendantId)).findFirst().get();

        final List<Hearings> hearingsList = hearingsAtAGlance.getHearings().stream()
                .filter(h -> defendantHearings.getHearingIds().contains(h.getId())).toList();

        if (caseDefendant.isPresent()) {
            defendantBuilder.withId(caseDefendant.get().getId());
            if (nonNull(caseDefendant.get().getPersonDefendant())) {
                defendantBuilder.withName(transformationHelper.getPersonName(caseDefendant.get().getPersonDefendant().getPersonDetails()));

                if (Objects.nonNull(caseDefendant.get().getPersonDefendant().getCustodialEstablishment())) {
                    defendantBuilder.withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                            .withCustody(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getCustody())
                            .withId(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getId())
                            .withName(caseDefendant.get().getPersonDefendant().getCustodialEstablishment().getName())
                            .build());
                }
            } else if (nonNull(caseDefendant.get().getLegalEntityDefendant())) {
                defendantBuilder.withName(caseDefendant.get().getLegalEntityDefendant().getOrganisation().getName());
                defendantBuilder.withAddress(caseDefendant.get().getLegalEntityDefendant().getOrganisation().getAddress());
            }

            if (caseDefendant.get().getAssociatedPersons() != null && !caseDefendant.get().getAssociatedPersons().isEmpty()) {
                ejectExtract.withParentGuardian(transformParentGuardian(caseDefendant.get().getAssociatedPersons()));
            }
        }

        ejectExtract.withExtractType(COURT_EXTRACT);
        ejectExtract.withCaseReference(transformationHelper.getCaseReference(prosecutionCase.getProsecutionCaseIdentifier()));

        LOGGER.info("Hearings {}", isNotEmpty(defendantHearings.getHearingIds()) ? defendantHearings.getHearingIds() : "No hearings present");

        if (isNotEmpty(hearingsList)) {
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, ejectExtract, defendantBuilder, hearingsList, caseDefendant.get(), prosecutionCase.getProsecutor(), COURT_EXTRACT, emptyList());
        } else {
            ejectExtract.withDefendant(transformDefendantWithoutHearingDetails(caseDefendant.get(), defendantBuilder));
            ejectExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(hearingsAtAGlance.getProsecutionCaseIdentifier(), userId));
        }

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            ejectExtract.withIsAppealPending(transformationHelper.getAppealPendingFlag(hearingsAtAGlance.getCourtApplications()));
        }

        return ejectExtract.build();
    }

    private void extractHearingDetails(final GetHearingsAtAGlance hearingsAtAGlance, final UUID defendantId, final UUID userId, final CourtExtractRequested.Builder courtExtract,
                                       final Defendant.Builder defendantBuilder, final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant,
                                       final Prosecutor prosecutor, final String extractType, final List<UUID> linkedApplicationsDefendantIds) {
        final UUID masterDefendantId = caseDefendant.getMasterDefendantId();

        final Hearings latestHearing = hearingsList.size() > 1 ? transformationHelper.getLatestHearings(hearingsList) : hearingsList.get(0);

        courtExtract.withDefendant(transformDefendants(latestHearing.getDefendants(), defendantId, masterDefendantId, userId, defendantBuilder, hearingsList, caseDefendant, hearingsAtAGlance, extractType, linkedApplicationsDefendantIds));

        courtExtract.withPublishingCourt(transformCourtCentre(latestHearing, userId, defendantId));

        if (prosecutor != null) {
            courtExtract.withProsecutingAuthority(transformationHelper.transformProsecutor(prosecutor));
        } else {
            courtExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(hearingsAtAGlance.getProsecutionCaseIdentifier(), userId));
        }

        courtExtract.withCompanyRepresentatives(transformCompanyRepresentatives(hearingsList));

        courtExtract.withReferralReason(getReferralReason(hearingsAtAGlance.getHearings()));
    }

    private List<CourtApplications> transformCourtApplications(final List<CourtApplication> caseCourtApplications, final Hearings hearings, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final UUID masterDefendantId, final UUID userId) {
        final List<CourtApplication> applicationsExtractList = new ArrayList<>();

        caseCourtApplications.forEach(app -> getResultedApplication(app, hearings, masterDefendantId).ifPresent(resultedApplication ->
            applicationsExtractList.add(mergeApplicationResults(app, resultedApplication))
        ));

        return applicationsExtractList.stream()
                .map(ca -> CourtApplications.courtApplications()
                        .withId(ca.getId())
                        .withApplicationResults(getApplicationResultsWithAmendmentsExcludingDefendantLevelResults(ca.getJudicialResults(), resultIdSlipRuleAmendmentsMap))
                        .withDeletedApplicationResults(getDeletedApplicationResultsWithAmendmentsExcludingDefendantLevelResults(ca.getId(), resultIdSlipRuleAmendmentsMap))
                        .withRepresentation(transformRepresentation(ca, hearings))
                        .withApplicationType(ca.getType().getType())
                        .withApplicationDate(ca.getApplicationReceivedDate())
                        .withApplicationParticulars(ca.getApplicationParticulars())
                        .withApplicationLegislation(ca.getType().getLegislation())
                        .withConvictionDate(ca.getConvictionDate())
                        .withPlea(ca.getPlea())
                        .withCourtOrders(transformCourtOrders(ca.getCourtOrder(), null, resultIdSlipRuleAmendmentsMap))
                        .withCourtApplicationCases(getCourtApplicationCases(ca, hearings, resultIdSlipRuleAmendmentsMap, masterDefendantId, userId))
                        .build()).toList();
    }

    private List<CourtApplicationCases> getCourtApplicationCases(final CourtApplication courtApplication, final Hearings hearings, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final UUID masterDefendantId, final UUID userId) {
        return Optional.ofNullable(courtApplication.getCourtApplicationCases()).stream().flatMap(Collection::stream)
                .map(c -> CourtApplicationCases.courtApplicationCases()
                        .withCaseStatus(c.getCaseStatus())
                        .withProsecutionCaseId(c.getProsecutionCaseId())
                        .withIsSJP(c.getIsSJP())
                        .withOffences(getCourtApplicationCasesOffences(c.getOffences(), hearings, resultIdSlipRuleAmendmentsMap, masterDefendantId, userId))
                        .build())
                .toList();

    }

    private List<CourtApplicationCases> getCourtApplicationCases(final CourtApplication courtApplication, final uk.gov.justice.core.courts.Hearing hearing, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        return Optional.ofNullable(courtApplication.getCourtApplicationCases()).stream().flatMap(Collection::stream)
                .map(c -> CourtApplicationCases.courtApplicationCases()
                        .withCaseStatus(c.getCaseStatus())
                        .withProsecutionCaseId(c.getProsecutionCaseId())
                        .withIsSJP(c.getIsSJP())
                        .withOffences(getCourtApplicationCasesOffences(c.getOffences(), hearing, resultIdSlipRuleAmendmentsMap))
                        .build())
                .toList();

    }

    private List<uk.gov.justice.progression.courts.exract.Offences> getCourtApplicationCasesOffences(final List<Offence> offences, final Hearings hearings, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final UUID masterDefendantId, final UUID userId) {
        if (CollectionUtils.isNotEmpty(offences)) {
            final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap = getUuidCommittedForSentenceMap(hearings.getId(), masterDefendantId, userId, List.of(hearings));
            return transformOffenceForCourtApplicationCases(offences, hearings.getId(), offenceCommittedForSentenceMap, hearings.getJurisdictionType(), resultIdSlipRuleAmendmentsMap);
        }
        return Collections.emptyList();
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> getCourtApplicationCasesOffences(final List<Offence> offences, final uk.gov.justice.core.courts.Hearing hearing, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        if (CollectionUtils.isNotEmpty(offences)) {
            return transformOffenceForCourtApplicationCases(offences, hearing.getId(), emptyMap(), hearing.getJurisdictionType(), resultIdSlipRuleAmendmentsMap);
        }
        return Collections.emptyList();
    }


    private List<CourtApplications> transformCourtApplications(final List<CourtApplication> caseCourtApplications, final uk.gov.justice.core.courts.Hearing hearing, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {

        return Optional.ofNullable(caseCourtApplications).stream().flatMap(Collection::stream)
                .map(ca -> CourtApplications.courtApplications()
                        .withId(ca.getId())
                        .withApplicationResults(getApplicationResultsWithAmendmentsExcludingDefendantLevelResults(ca.getJudicialResults(), resultIdSlipRuleAmendmentsMap))
                        .withDeletedApplicationResults(getDeletedApplicationResultsWithAmendmentsExcludingDefendantLevelResults(ca.getId(), resultIdSlipRuleAmendmentsMap))
                        .withRepresentation(transformRepresentation(ca, hearing))
                        .withApplicationType(ca.getType().getType())
                        .withApplicationDate(ca.getApplicationReceivedDate())
                        .withApplicationParticulars(ca.getApplicationParticulars())
                        .withApplicationLegislation(ca.getType().getLegislation())
                        .withConvictionDate(ca.getConvictionDate())
                        .withPlea(ca.getPlea())
                        .withCourtApplicationCases(getCourtApplicationCases(ca, hearing, resultIdSlipRuleAmendmentsMap))
                        .withCourtOrders(transformCourtOrders(ca.getCourtOrder(), extractHearings, resultIdSlipRuleAmendmentsMap))
                        .build()).toList();
    }

    @SuppressWarnings("squid:S3776")
    private CourtOrders transformCourtOrders(final CourtOrder courtOrder, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendments) {
        if (isNull(courtOrder)) {
            return null;
        }
        final CourtOrders.Builder courtOrdersBuilder = CourtOrders.courtOrders()
                .withId(courtOrder.getId())
                .withLabel(courtOrder.getLabel())
                .withOrderDate(courtOrder.getOrderDate())
                .withStartDate(courtOrder.getStartDate())
                .withCanBeSubjectOfBreachProceedings(courtOrder.getCanBeSubjectOfBreachProceedings());

        if (nonNull(courtOrder.getCourtOrderOffences())) {
            final List<UUID> offenceIds = isNull(extractHearings) ? emptyList() : extractHearings.stream()
                    .map(uk.gov.justice.progression.courts.exract.Hearings::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(uk.gov.justice.progression.courts.exract.Offences::getId)
                    .toList();
            courtOrdersBuilder.withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                    .filter(coo -> offenceIds.isEmpty() || offenceIds.contains(coo.getOffence().getId()))
                    .map(coo -> CourtOrderOffences.courtOrderOffences()
                            .withId(coo.getOffence().getId())
                            .withStartDate(coo.getOffence().getStartDate())
                            .withEndDate(coo.getOffence().getEndDate())
                            .withOffenceCode(coo.getOffence().getOffenceCode())
                            .withOffenceTitle(coo.getOffence().getOffenceTitle())
                            .withOffenceLegislation(coo.getOffence().getOffenceLegislation())
                            .withOffenceTitleWelsh(coo.getOffence().getOffenceTitleWelsh())
                            .withWording(coo.getOffence().getWording())
                            .withWordingWelsh(coo.getOffence().getWordingWelsh())
                            .withIndicatedPlea(coo.getOffence().getIndicatedPlea())
                            .withOffenceDefinitionId(coo.getOffence().getOffenceDefinitionId())
                            .withResultTextList(nonNull(coo.getOffence().getJudicialResults()) && !coo.getOffence().getJudicialResults().isEmpty() ?
                                    coo.getOffence().getJudicialResults().stream().map(JudicialResult::getResultText).filter(StringUtils::isNotEmpty).toList() : emptyList())
                            .withCourtOrderOffencesOffenceLevelResults(nonNull(coo.getOffence().getJudicialResults()) && !coo.getOffence().getJudicialResults().isEmpty() ?
                                    coo.getOffence().getJudicialResults().stream()
                                            .filter(j -> OFFENCE_LEVEL.equals(j.getLevel()))
                                            .map(j -> CourtOrderOffencesOffenceLevelResults.courtOrderOffencesOffenceLevelResults()
                                                    .withResult(j)
                                                    .withAmendments(resultIdSlipRuleAmendments.get(j.getJudicialResultId()))
                                                    .build())
                                            .toList() : emptyList())
                            .withDeletedCourtOrderOffenceOffenceLevelResults(getOffenceCourtOrderOffenceResultsWithAmendments(coo.getOffence().getId(), resultIdSlipRuleAmendments))
                            .withPlea(coo.getOffence().getPlea())
                            .withConvictionDate(coo.getOffence().getConvictionDate())
                            .build())
                    .toList()
            );
        }
        return courtOrdersBuilder.build();
    }


    public static List<DeletedCourtOrderOffenceOffenceLevelResults> getOffenceCourtOrderOffenceResultsWithAmendments(final UUID offenceId, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {
        if (nonNull(resultIdSlipRuleAmendmentsMap)) {
            return resultIdSlipRuleAmendmentsMap.entrySet().stream()
                    .flatMap(amendmentList -> amendmentList.getValue().stream())
                    .filter(amendments -> AmendmentType.DELETED.name().equals(amendments.getAmendmentType()))
                    .filter(amendments -> offenceId.equals(amendments.getOffenceId()))
                    .filter(amendments -> amendments.getResultLevel().equals(OFFENCE_LEVEL))
                    .map(amendments -> DeletedCourtOrderOffenceOffenceLevelResults.deletedCourtOrderOffenceOffenceLevelResults()
                            .withDefendantId(amendments.getDefendantId())
                            .withOffenceId(amendments.getOffenceId())
                            .withJudicialResultId(amendments.getJudicialResultId())
                            .withAmendmentDate(amendments.getAmendmentDate())
                            .withAmendmentType(amendments.getAmendmentType())
                            .withApplicationId(amendments.getApplicationId())
                            .withResultText(amendments.getResultText())
                            .withResultLevel(amendments.getResultLevel())
                            .build())
                    .distinct()
                    .sorted(comparing(DeletedCourtOrderOffenceOffenceLevelResults::getAmendmentDate).reversed())
                    .toList();
        }
        return emptyList();
    }

    private Optional<CourtApplication> getResultedApplication(final CourtApplication courtApplication, final Hearings hearings, final UUID masterDefendantId) {
        final boolean isApplicationAssociatedWithCourtApplication = Stream.of(
                        courtApplication.getApplicant(),
                        courtApplication.getSubject()
                ).filter(Objects::nonNull)
                .map(CourtApplicationParty::getMasterDefendant)
                .filter(Objects::nonNull)
                .map(MasterDefendant::getMasterDefendantId)
                .anyMatch(masterDefendantId::equals);
        if (isApplicationAssociatedWithCourtApplication) {
            final HearingEntity hearingEntity = hearingRepository.findBy(hearings.getId());
            if (hearingEntity != null) {
                final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), uk.gov.justice.core.courts.Hearing.class);
                return Optional.ofNullable(hearing.getCourtApplications()).stream().flatMap(Collection::stream)
                        .filter(c -> c.getId().equals(courtApplication.getId()))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private CourtApplication mergeApplicationResults(final CourtApplication courtApplication, final CourtApplication resultedApplication) {
        return (CourtApplication.courtApplication()
                .withId(courtApplication.getId())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withType(courtApplication.getType())
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(updateResponse(courtApplication.getRespondents(), resultedApplication))
                .withJudicialResults(resultedApplication.getJudicialResults())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withPlea(courtApplication.getPlea())
                .withConvictionDate(courtApplication.getConvictionDate())
                .withCourtOrder(resultedApplication.getCourtOrder())
                .withCourtApplicationCases(resultedApplication.getCourtApplicationCases())
                .build());
    }

    private List<CourtApplicationParty> updateResponse(final List<CourtApplicationParty> courtApplicationRespondents, final CourtApplication resultedApplication) {
        final List<CourtApplicationParty> updatedResponseList = new ArrayList<>();
        if (isNotEmpty(courtApplicationRespondents)) {
            updatedResponseList.addAll(courtApplicationRespondents);
        }
        if (nonNull(resultedApplication) && isNotEmpty(resultedApplication.getRespondents())) {
            updatedResponseList.add(CourtApplicationParty.courtApplicationParty().build());
        }
        return updatedResponseList;
    }

    @SuppressWarnings("java:S3358")
    private Representation transformRepresentation(final CourtApplication ca, final Hearings hearings) {
        return Representation.representation()
                .withApplicantRepresentation(transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), nonNull(hearings) ? hearings.getApplicantCounsels() : null))
                .withRespondentRepresentation(ca.getRespondents() != null ? transformRespondentRepresentations(ca.getRespondents(), nonNull(hearings) ? hearings.getRespondentCounsels() : null) : null)
                .build();
    }

    @SuppressWarnings("java:S3358")
    private Representation transformRepresentation(final CourtApplication ca, final uk.gov.justice.core.courts.Hearing hearing) {
        return Representation.representation()
                .withApplicantRepresentation(transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), nonNull(hearing) ? hearing.getApplicantCounsels() : null))
                .withRespondentRepresentation(ca.getRespondents() != null ? transformRespondentRepresentations(ca.getRespondents(), nonNull(hearing) ? hearing.getRespondentCounsels() : null) : null)
                .build();
    }

    private String getReferralReason(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantReferralReasons()))
                .flatMap((h -> h.getDefendantReferralReasons().stream()))
                .filter(Objects::nonNull)
                .findAny()
                .map(ReferralReason::getDescription)
                .orElse(null);
    }

    private List<RespondentRepresentation> transformRespondentRepresentations(final List<CourtApplicationParty> respondents, final List<RespondentCounsel> respondentCounsels) {
        final List<RespondentRepresentation> respondentRepresentations = new ArrayList<>();

        respondents.forEach(r -> respondentRepresentations.add(transformRespondentRepresentation(r, respondentCounsels)));
        return respondentRepresentations;
    }


    private RespondentRepresentation transformRespondentRepresentation(final CourtApplicationParty courtApplicationParty, final List<RespondentCounsel> respondentCounsels) {
        final Organisation representationOrganisation = nonNull(courtApplicationParty) ? courtApplicationParty.getRepresentationOrganisation() : null;
        return RespondentRepresentation.respondentRepresentation()
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withName(nonNull(representationOrganisation) ? representationOrganisation.getName() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withRespondentCounsels(respondentCounsels)
                .build();
    }

    private ApplicantRepresentation transformApplicantRepresentation(final Organisation representationOrganisation, final List<ApplicantCounsel> applicantCounsels) {
        final String orgName = nonNull(representationOrganisation) ? representationOrganisation.getName() : null;
        return ApplicantRepresentation.applicantRepresentation()
                .withName(orgName)
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withApplicantCounsels(applicantCounsels)
                .build();
    }

    private ParentGuardian transformParentGuardian(final List<AssociatedPerson> associatedPersons) {
        final Optional<Person> person = associatedPersons.stream()
                .filter(associatedPerson -> nonNull(associatedPerson.getPerson()))
                .findFirst()
                .map(AssociatedPerson::getPerson);
        return person.map(person1 -> ParentGuardian.parentGuardian()
                .withName(transformationHelper.getName(person1.getFirstName(), person1.getMiddleName(), person1.getLastName()))
                .withAddress(person1.getAddress())
                .build()).orElse(null);
    }

    private List<ProsecutionCounsels> transformProsecutionCounsel(final List<ProsecutionCounsel> prosecutionCounselsList) {
        return prosecutionCounselsList.stream().map(pc -> ProsecutionCounsels.prosecutionCounsels()
                .withName(transformationHelper.getName(pc.getFirstName(), pc.getMiddleName(), pc.getLastName()))
                .withAttendanceDays(transformAttendanceDays(pc.getAttendanceDays()))
                .withRole(pc.getStatus())
                .build()
        ).toList();
    }

    private List<DefenceCounsels> transformDefenceCounsel(final List<DefenceCounsel> defenceCounselList) {
        return defenceCounselList.stream().map(dc -> DefenceCounsels.defenceCounsels()
                .withName(transformationHelper.getName(dc.getFirstName(), dc.getMiddleName(), dc.getLastName()))
                .withAttendanceDays(dc.getAttendanceDays() != null ? transformAttendanceDays(dc.getAttendanceDays()) : new ArrayList<>())
                .withRole(dc.getStatus())
                .build()).toList();
    }

    private List<AttendanceDays> transformAttendanceDays(final List<LocalDate> attendanceDaysList) {
        return attendanceDaysList.stream().distinct().map(ad -> AttendanceDays.attendanceDays()
                .withDay(ad)
                .build()).toList();
    }

    private List<AttendanceDayAndType> transformAttendanceDayAndTypes(final List<AttendanceDayAndType> attendanceDaysList) {
        return attendanceDaysList.stream().distinct().map(ad -> AttendanceDayAndType.attendanceDayAndType()
                .withDay(ad.getDay())
                .withAttendanceType(ad.getAttendanceType())
                .build()).toList();
    }

    private PublishingCourt transformCourtCentre(final Hearings latestHearing, final UUID userId, final UUID defendantId) {
        if (hearingsDefendantIdBiPredicate.test(latestHearing, defendantId)) {
            LOGGER.info("Latest hearing youth Court Name {} ", latestHearing.getYouthCourt().getName());
            return PublishingCourt.publishingCourt()
                    .withName(latestHearing.getYouthCourt().getName())
                    .withWelshName(latestHearing.getYouthCourt().getWelshName())
                    .withAddress(transformationHelper.getCourtAddress(userId, latestHearing.getCourtCentre().getId()))
                    .build();
        } else {
            return PublishingCourt.publishingCourt()
                    .withName(latestHearing.getCourtCentre().getName())
                    .withWelshName(latestHearing.getCourtCentre().getWelshName())
                    .withAddress(transformationHelper.getCourtAddress(userId, latestHearing.getCourtCentre().getId()))
                    .build();
        }
    }

    @SuppressWarnings("squid:S3776")
    private Defendant transformDefendants(final List<Defendants> defendantsList, final UUID defendantId, final UUID masterDefendantId, final UUID userId, final Defendant.Builder defendantBuilder,
                                          final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant, GetHearingsAtAGlance hearingsAtAGlance, final String extractType,
                                          final List<UUID> linkedApplicationDefendantIds) {

        final Optional<Defendants> tempDefendants = defendantsList.stream()
                .filter(d -> d.getId().equals(defendantId) || d.getMasterDefendantId().equals(masterDefendantId) || linkedApplicationDefendantIds.contains(d.getId()))
                .findFirst();
        final Optional<Defendants> defendants = tempDefendants.isPresent() && nonNull(tempDefendants.get().getAge())? tempDefendants : getDefendantsFromCourtApplication(hearingsAtAGlance, linkedApplicationDefendantIds, hearingsList);

        if (defendants.isPresent()) {
            final Defendants defendant = defendants.get();
            defendantBuilder.withDateOfBirth(defendant.getDateOfBirth());
            defendantBuilder.withAge(getDefendantPresentAge(defendant.getDateOfBirth(), defendant.getAge()));
            defendantBuilder.withLegalAidStatus(defendant.getLegalAidStatus());
            List<AttendanceDayAndType> attendanceDays = new ArrayList<>(transformAttendanceDayAndTypes(transformDefendantAttendanceDay(hearingsList, defendant)));
            final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings = new ArrayList<>(getExtractHearings(masterDefendantId, userId, hearingsList, hearingsAtAGlance, extractType, defendant, linkedApplicationDefendantIds));
            if (hearingsList.stream().anyMatch(hearings -> Boolean.FALSE.equals(hearings.getIsApplicationHearing()))) {
                final List<uk.gov.justice.progression.courts.exract.Hearings> extractApplicationHearingsWhichNotInHearingAtAGlance = getExtractApplicationHearings(defendant, masterDefendantId, userId, hearingsAtAGlance, extractHearings, linkedApplicationDefendantIds);
                addApplicationHearingsToExtractHearing(extractApplicationHearingsWhichNotInHearingAtAGlance, extractHearings);
                attendanceDays.addAll(extractHearings.stream().map(uk.gov.justice.progression.courts.exract.Hearings::getAttendanceDays).flatMap(Collection::stream).toList());
            }

            List<uk.gov.justice.progression.courts.exract.Hearings> sortedHearings = getHearingsSortedByHearingDaysAsc(extractHearings);
            defendantBuilder.withHearings(sortedHearings);
            //used for certificates generation

            defendantBuilder.withAttendanceDays(attendanceDays);

            defendantBuilder.withResults(transformJudicialResults(hearingsList, masterDefendantId, defendantId, hearingsAtAGlance, linkedApplicationDefendantIds));
            //used for certificates generation

            buildAssociatedOrganisation(defendantId, userId, defendantBuilder, caseDefendant, defendant);
            if (CERTIFICATE_OF_CONVICTION.equals(extractType) && isNotEmpty(sortedHearings)) {
                buildHearingsForConvictionCertificate(defendantBuilder, sortedHearings);
            }
            if (CERTIFICATE_OF_ACQUITTAL.equals(extractType) && isNotEmpty(sortedHearings)) {
                buildHearingsForAcquittalCertificate(defendantBuilder, sortedHearings);
            }
            return handlePersonDefendantDetails(caseDefendant, defendantBuilder);
        }

        return defendantBuilder.build();
    }

    private static String getDefendantPresentAge(final LocalDate dob, final String defaultAge) {
        return nonNull(dob) ? Integer.toString(Period.between(dob, LocalDate.now()).getYears()) : defaultAge;
    }

    private  Optional<Defendants> getDefendantsFromCourtApplication(final GetHearingsAtAGlance hearingsAtAGlance, final List<UUID> linkedApplicationDefendantIds, final List<Hearings> hearingsList) {
        if (hearingsList.isEmpty()) {
            throw new RuntimeException(String.format("No hearings found for court application hearing. CaseId:%s", hearingsAtAGlance.getId()));
        }
        final Hearings hearings = hearingsList.size() > 1 ? transformationHelper.getLatestHearings(hearingsList) : hearingsList.get(0);

        return  hearingsAtAGlance.getCourtApplications().stream()
                .flatMap(courtApplication -> Stream.of(
                        courtApplication.getApplicant(),
                        courtApplication.getSubject()
                ))
                .filter(Objects::nonNull)
                .map(CourtApplicationParty::getMasterDefendant)
                .filter(Objects::nonNull)
                .filter(masterDefendant ->
                     linkedApplicationDefendantIds.contains(masterDefendant.getMasterDefendantId()) ||
                            Optional.ofNullable(masterDefendant.getDefendantCase()).stream().flatMap(Collection::stream).map(DefendantCase::getDefendantId).anyMatch(linkedApplicationDefendantIds::contains)
                )
                .map(masterDefendant -> Defendants.defendants()
                        .withId(masterDefendant.getMasterDefendantId())
                        .withMasterDefendantId(masterDefendant.getMasterDefendantId())
                        .withName(getDefendantName(masterDefendant.getPersonDefendant(), masterDefendant.getLegalEntityDefendant()))
                        .withAge(getDefendantAge(masterDefendant.getPersonDefendant(), hearings.getHearingDays()))
                        .withDateOfBirth(getDefendantDataOfBirth(masterDefendant.getPersonDefendant()))
                        .withAddress(masterDefendant.getPersonDefendant() != null ?
                                masterDefendant.getPersonDefendant().getPersonDetails().getAddress() : null)
                        .withJudicialResults(getJudicialResults(null))
                        .build())
                .findFirst();
    }

    private void buildAssociatedOrganisation(final UUID defendantId, final UUID userId, final Defendant.Builder defendantBuilder, final uk.gov.justice.core.courts.Defendant caseDefendant, final Defendants defendant) {
        if (nonNull(caseDefendant.getAssociatedDefenceOrganisation())) {
            final List<AssociatedDefenceOrganisation> associatedOrganisations = defenceQueryService.getAllAssociatedOrganisations(userId, defendantId.toString());
            defendantBuilder.withAssociatedDefenceOrganisations(associatedOrganisations);
        } else {
            if (nonNull(caseDefendant.getDefenceOrganisation())) {
                defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(caseDefendant.getDefenceOrganisation()));
            } else if (nonNull(defendant.getDefenceOrganisation())) {
                defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(defendant.getDefenceOrganisation().getDefenceOrganisation()));
            } else {
                defendantBuilder.withDefenceOrganisations(transformDefenceOrganisation(null));
            }
        }
    }

    private static void addApplicationHearingsToExtractHearing(final List<uk.gov.justice.progression.courts.exract.Hearings> extractApplicationHearingsWhichNotInHearingAtAGlance, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {
        final Iterator<uk.gov.justice.progression.courts.exract.Hearings> iterator = extractApplicationHearingsWhichNotInHearingAtAGlance.iterator();
        while (iterator.hasNext()) {
            uk.gov.justice.progression.courts.exract.Hearings appHearing = iterator.next();
            Optional<uk.gov.justice.progression.courts.exract.Hearings> matchingHearing = extractHearings.stream()
                    .filter(h -> h.getId().equals(appHearing.getId()))
                    .findFirst();

            if (matchingHearing.isPresent()) {
                extractHearings.remove(matchingHearing.get());
                final uk.gov.justice.progression.courts.exract.Hearings hearing =
                        uk.gov.justice.progression.courts.exract.Hearings.hearings()
                                .withValuesFrom(matchingHearing.get())
                                .withCourtApplications(appHearing.getCourtApplications())
                                .build();
                extractHearings.add(hearing);
                iterator.remove();
            }
        }
        extractHearings.addAll(extractApplicationHearingsWhichNotInHearingAtAGlance);
    }

    private List<uk.gov.justice.progression.courts.exract.Hearings> getExtractApplicationHearings(final Defendants defendant, final UUID masterDefendantId, final UUID userId, final GetHearingsAtAGlance hearingsAtAGlance, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings,  final List<UUID> linkedApplicationDefendantIds) {
        final List<uk.gov.justice.progression.courts.exract.Hearings> extractApplicationHearings = new ArrayList<>();
        final List<CourtApplication> courtApplications = ofNullable(hearingsAtAGlance.getCourtApplications()).stream().flatMap(Collection::stream)
                .filter(courtApplication -> nonNull(courtApplication.getSubject()) && nonNull(courtApplication.getSubject().getMasterDefendant()))
                .filter(courtApplication -> masterDefendantId.equals(courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()))
                .filter(courtApplication -> hasClonedOffences(courtApplication, extractHearings))
                .filter(courtApplication -> extractHearings.stream()
                        .map(uk.gov.justice.progression.courts.exract.Hearings::getCourtApplications)
                        .flatMap(Collection::stream)
                        .noneMatch(c -> courtApplication.getId().equals(c.getId())))
                .toList();

        final List<uk.gov.justice.core.courts.Hearing> applicationHearings = new ArrayList<>();
        courtApplications.forEach(courtApplication -> {
            final List<UUID> hearingIds = hearingService.getApplicationHearings(courtApplication.getId());
            hearingIds.forEach(id -> {
                final HearingEntity hearingRequestEntity = hearingRepository.findBy(id);
                if (HEARING_RESULTED.equals(hearingRequestEntity.getListingStatus())) {
                    final uk.gov.justice.core.courts.Hearing hearing = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(hearingRequestEntity.getPayload()), uk.gov.justice.core.courts.Hearing.class);
                    if (!isBoxHearing(hearing) && !applicationHearings.contains(hearing)) {
                        applicationHearings.add(hearing);
                    }
                }
            });
        });
        applicationHearings.forEach(h -> {
            if (CollectionUtils.isNotEmpty(h.getCourtApplications())) {
                h.getCourtApplications().removeIf(courtApplication -> nonNull(courtApplication.getSubject()) &&
                        nonNull(courtApplication.getSubject().getMasterDefendant()) &&
                        !courtApplication.getSubject().getMasterDefendant().getMasterDefendantId().equals(masterDefendantId));
                h.getCourtApplications().removeIf(courtApplication -> !hasClonedOffences(courtApplication, extractHearings));
            }
            extractApplicationHearings.add(getExtractHearingFromLinkedApplicationHearing(defendant, userId, h, h.getCourtApplications(), extractHearings, masterDefendantId, linkedApplicationDefendantIds));
        });
        return extractApplicationHearings;

    }

    private boolean hasClonedOffences(final CourtApplication courtApplication, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {
        final List<UUID> applicationOffenceIds = new ArrayList<>();
        if (nonNull(courtApplication.getCourtOrder())) {
            applicationOffenceIds.addAll(Optional.ofNullable(courtApplication.getCourtOrder().getCourtOrderOffences()).stream().flatMap(Collection::stream)
                    .map(CourtOrderOffence::getOffence)
                    .filter(Objects::nonNull)
                    .map(Offence::getId)
                    .toList());
        }
        if(isNotEmpty(courtApplication.getCourtApplicationCases())){
            applicationOffenceIds.addAll(courtApplication.getCourtApplicationCases().stream()
                    .map(CourtApplicationCase::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .map(Offence::getId)
                    .filter(Objects::nonNull)
                    .toList());
        }
        return extractHearings.stream().map(uk.gov.justice.progression.courts.exract.Hearings::getOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(uk.gov.justice.progression.courts.exract.Offences::getId)
                .anyMatch(applicationOffenceIds::contains);
    }

    private static boolean isBoxHearing(final uk.gov.justice.core.courts.Hearing hearing) {
        return nonNull(hearing.getIsBoxHearing()) && hearing.getIsBoxHearing();
    }

    private Defendant handlePersonDefendantDetails(final uk.gov.justice.core.courts.Defendant caseDefendant, Defendant.Builder defendantBuilder) {
        if (nonNull(caseDefendant.getPersonDefendant())) {
            final PersonDefendant personDefendant = caseDefendant.getPersonDefendant();
            if (nonNull(personDefendant.getArrestSummonsNumber())) {
                defendantBuilder.withArrestSummonsNumber(personDefendant.getArrestSummonsNumber());
            }

            if (nonNull(personDefendant.getBailStatus())) {
                defendantBuilder.withRemandStatus(personDefendant.getBailStatus().getDescription());
            }

            final Person personDetails = personDefendant.getPersonDetails();
            if (nonNull(personDetails)) {
                if (nonNull(personDetails.getGender())) {
                    defendantBuilder.withGender(personDetails.getGender().toString());
                }
                if (nonNull(personDetails.getEthnicity())) {
                    defendantBuilder.withEthnicity(personDetails.getEthnicity().getSelfDefinedEthnicityDescription());
                }

                defendantBuilder.withAddress(toAddress(personDetails.getAddress()));
            }
        }
        return defendantBuilder.build();
    }

    private List<uk.gov.justice.progression.courts.exract.Hearings> getExtractHearings(final UUID masterDefendantId, final UUID userId, final List<Hearings> hearingsList, final GetHearingsAtAGlance hearingsAtAGlance, final String extractType, final Defendants defendant, final List<UUID> linkedApplicationDefendantIds) {
        return hearingsList.stream()
                .filter(h -> h.getIsBoxHearing() == null || !h.getIsBoxHearing())
                .filter(h -> (!CERTIFICATE_OF_CONVICTION.equals(extractType) && !CERTIFICATE_OF_ACQUITTAL.equals(extractType))
                        || HEARING_RESULTED.equals(h.getHearingListingStatus()))
                .map(h -> getExtractHearing(defendant, masterDefendantId, userId, h, hearingsAtAGlance, extractType, linkedApplicationDefendantIds))
                .toList();
    }


    private void buildHearingsForConvictionCertificate(final Defendant.Builder defendantBuilder, final List<uk.gov.justice.progression.courts.exract.Hearings> sortedHearings) {
        List<uk.gov.justice.progression.courts.exract.Hearings> updateHearingWithConvictedData = new ArrayList<>();
        // Sort offences within each hearing by conviction date
        for (uk.gov.justice.progression.courts.exract.Hearings hearing : sortedHearings) {
            final List<ConvictedOffencesDetails> convictedOffencesList = new ArrayList<>();
            buildConvictedCaseOffences(hearing, convictedOffencesList);
            buildConvictedCourtOrderOffences(hearing, convictedOffencesList);
            updateHearingWithConvictedData.add(uk.gov.justice.progression.courts.exract.Hearings.hearings().withValuesFrom(hearing)
                    .withConvictedOffencesDetails(convictedOffencesList).build());

        }
        if (!updateHearingWithConvictedData.isEmpty()) {
            defendantBuilder.withHearings(updateHearingWithConvictedData);
        }
    }

    private void buildHearingsForAcquittalCertificate(final Defendant.Builder defendantBuilder, final List<uk.gov.justice.progression.courts.exract.Hearings> sortedHearings) {
        List<uk.gov.justice.progression.courts.exract.Hearings> updateHearingWithAcquittedData = new ArrayList<>();
        // Sort offences within each hearing by Acquittal date
        for (uk.gov.justice.progression.courts.exract.Hearings hearing : sortedHearings) {
            final List<AquittedOffencesDetails> aquittedOffencesDetails = new ArrayList<>();
            if (isNotEmpty(hearing.getOffences())) {
                buildAcquittedCaseOffences(hearing, aquittedOffencesDetails);
                updateHearingWithAcquittedData.add(uk.gov.justice.progression.courts.exract.Hearings.hearings().withValuesFrom(hearing)
                        .withAquittedOffencesDetails(aquittedOffencesDetails).build());
            }
        }
        if (!updateHearingWithAcquittedData.isEmpty()) {
            defendantBuilder.withHearings(updateHearingWithAcquittedData);
        }
    }

    private void buildConvictedCaseOffences(final uk.gov.justice.progression.courts.exract.Hearings hearing, final List<ConvictedOffencesDetails> convictedOffencesList) {
        if (isNotEmpty(hearing.getOffences())) {
            final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> offencesSorted = getOffencesComparator(hearing);
            final Map<LocalDate, List<uk.gov.justice.progression.courts.exract.Offences>> groupedOffencesForCOC = getOffencesByConvictionDate(hearing.getOffences(), offencesSorted);
            groupedOffencesForCOC.forEach((convictionDate, offencesList) -> {
                final String dataVariation = getDataVariation(offencesList);
                ConvictedOffencesDetails.Builder convictedOffencesGroup = ConvictedOffencesDetails.convictedOffencesDetails()
                        .withConvictionDate(convictionDate)
                        .withDataVariation(dataVariation)
                        .withLocation(nonNull(hearing.getCourtCentre()) ? hearing.getCourtCentre().getName() : "")
                        .withSentenceLocation(getSentenceLocation(convictionDate, offencesList))
                        .withOffences(sortOffences(hearing.getJurisdictionType(), offencesList));
                convictedOffencesList.add(convictedOffencesGroup.build());
            });
        }
    }

    private static String getSentenceLocation(final LocalDate convictionDate, final List<uk.gov.justice.progression.courts.exract.Offences> offencesList) {
        return ofNullable(offencesList).orElse(emptyList()).stream()
                .filter(o -> nonNull(convictionDate) && convictionDate.equals(o.getConvictionDate()))
                .filter(o -> nonNull(o.getConvictingCourt()) && nonNull(o.getConvictingCourt().getName()))
                .map(o -> o.getConvictingCourt().getName())
                .findFirst()
                .orElse("");
    }

    private void buildAcquittedCaseOffences(final uk.gov.justice.progression.courts.exract.Hearings hearing, final List<AquittedOffencesDetails> aquittedOffencesList) {
        final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> offencesSorted = getOffencesComparator(hearing);
        final Map<LocalDate, List<uk.gov.justice.progression.courts.exract.Offences>> groupedOffencesForCOA = getOffencesByAquittalDate(hearing.getOffences(), offencesSorted);
        groupedOffencesForCOA.forEach((aquittalDate, offencesList) -> {
            final String dataVariation = getDataVariation(offencesList);
            AquittedOffencesDetails.Builder aquittedOffencesGroup = AquittedOffencesDetails.aquittedOffencesDetails()
                    .withAquittalDate(aquittalDate)
                    .withDataVariation(dataVariation)
                    .withLocation(hearing.getCourtCentre().getName())
                    .withOffences(sortOffences(hearing.getJurisdictionType(), offencesList));
            aquittedOffencesList.add(aquittedOffencesGroup.build());
        });
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> sortOffences(final JurisdictionType jurisdictionType, final List<uk.gov.justice.progression.courts.exract.Offences> offencesList) {
        if (jurisdictionType == JurisdictionType.CROWN) {
            final List<uk.gov.justice.progression.courts.exract.Offences> offencesWithCount = offencesList.stream().filter(o -> nonNull(o.getCount()) && o.getCount() > 0)
                    .sorted(Comparator.comparing(uk.gov.justice.progression.courts.exract.Offences::getCount).thenComparing(uk.gov.justice.progression.courts.exract.Offences::getOrderIndex))
                    .toList();
            final List<uk.gov.justice.progression.courts.exract.Offences> offencesWithoutCount = offencesList.stream().filter(o -> isNull(o.getCount()) || o.getCount() == 0)
                    .sorted(Comparator.comparing(uk.gov.justice.progression.courts.exract.Offences::getOrderIndex))
                    .toList();
            return Stream.concat(offencesWithCount.stream(), offencesWithoutCount.stream()).toList();
        } else {
            return offencesList.stream()
                    .sorted(Comparator.comparing(uk.gov.justice.progression.courts.exract.Offences::getOrderIndex))
                    .toList();
        }
    }

    private Comparator<? super uk.gov.justice.progression.courts.exract.Offences> getOffencesComparator(final uk.gov.justice.progression.courts.exract.Hearings hearing) {
        final JurisdictionType jurisdictionType = hearing.getJurisdictionType();
        return jurisdictionType == JurisdictionType.CROWN ? crownOffencesSortComparator : magsOffencesSortComparator;
    }

    private String getDataVariation(final List<uk.gov.justice.progression.courts.exract.Offences> offencesList) {
        String dataVariation = null;
        final Set<String> guiltyPleaTypes = referenceDataService.retrieveGuiltyPleaTypes();
        if (!getGuiltyVerdicts(offencesList).isEmpty()) {
            dataVariation = AFTER_TRIAL_ON_INDICTMENT;
        } else if (!getGuiltyPleas(offencesList, guiltyPleaTypes).isEmpty() || !getIndicatedGuiltyPleas(offencesList).isEmpty()) {
            dataVariation = GUILTY_PLEA;
        }
        return dataVariation;
    }

    private void buildConvictedCourtOrderOffences(final uk.gov.justice.progression.courts.exract.Hearings hearing, final List<ConvictedOffencesDetails> convictedOffencesList) {
        if (nonNull(hearing.getCourtApplications()) && !hearing.getCourtApplications().isEmpty()) {
            hearing.getCourtApplications().forEach(courtApplication -> {
                if (Boolean.TRUE.equals(nonNull(courtApplication.getCourtOrders())
                        && courtApplication.getCourtOrders().getCanBeSubjectOfBreachProceedings())
                        && !courtApplication.getCourtOrders().getCourtOrderOffences().isEmpty()) {
                    final Map<LocalDate, List<CourtOrderOffences>> groupedCourtOrderOffencesForCOC = getCourtOrderOffencesByConvictionDate(courtApplication);
                    groupedCourtOrderOffencesForCOC.forEach((convictionDate, offencesList) -> {
                        ConvictedOffencesDetails.Builder convictedCourtOrderOffences = ConvictedOffencesDetails.convictedOffencesDetails()
                                .withConvictionDate(convictionDate)
                                .withDataVariation(FOUND_TO_BE_IN_BREACH_OF_AN_ORDER_MADE_BY + " " + hearing.getCourtCentre().getName())
                                .withLocation(hearing.getCourtCentre().getName())
                                .withApplicationDate(courtApplication.getApplicationDate())
                                .withApplicationType(courtApplication.getApplicationType())
                                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                                .withApplicationResults(courtApplication.getApplicationResults())
                                .withApplicationLegislation(courtApplication.getApplicationLegislation())
                                .withBreachApplicationConvictionDate(courtApplication.getConvictionDate())
                                .withCourtOrderOffences(offencesList);
                        convictedOffencesList.add(convictedCourtOrderOffences.build());
                    });
                }
            });
        }
    }

    public static List<uk.gov.justice.progression.courts.exract.Offences> getGuiltyVerdicts(List<uk.gov.justice.progression.courts.exract.Offences> offencesList) {
        return offencesList.stream()
                .filter(offence -> offence.getVerdicts().stream()
                        .anyMatch(verdict -> "GUILTY".equalsIgnoreCase(verdict.getVerdictType().getCategory())))
                .toList();
    }

    public static List<uk.gov.justice.progression.courts.exract.Offences> getIndicatedGuiltyPleas(List<uk.gov.justice.progression.courts.exract.Offences> offencesList) {
        return offencesList.stream()
                .filter(offence -> nonNull(offence.getIndicatedPlea())
                        && offence.getIndicatedPlea().getIndicatedPleaValue() == IndicatedPleaValue.INDICATED_GUILTY)
                .toList();
    }

    public static List<uk.gov.justice.progression.courts.exract.Offences> getGuiltyPleas(List<uk.gov.justice.progression.courts.exract.Offences> offencesList, Set<String> guiltyPleaTypes) {
        return offencesList.stream()
                .filter(offence -> offence.getPleas().stream()
                        .anyMatch(plea -> guiltyPleaTypes.contains(plea.getPleaValue())))
                .toList();
    }

    private Map<LocalDate, List<uk.gov.justice.progression.courts.exract.Offences>> getOffencesByConvictionDate(final List<uk.gov.justice.progression.courts.exract.Offences> defendantOffences, final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> offencesSorted) {

        return defendantOffences.stream()
                .filter(o -> o.getConvictionDate() != null)
                .collect(Collectors.groupingBy(
                        uk.gov.justice.progression.courts.exract.Offences::getConvictionDate,
                        () -> new TreeMap<>(Comparator.naturalOrder()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(offencesSorted)
                                        .toList()
                        )
                ));
    }


    private Map<LocalDate, List<uk.gov.justice.progression.courts.exract.Offences>> getOffencesByAquittalDate(final List<uk.gov.justice.progression.courts.exract.Offences> defendantOffences, final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> offencesSorted) {
        Map<LocalDate, List<uk.gov.justice.progression.courts.exract.Offences>> groupedOffences = defendantOffences.stream()
                .filter(o -> o.getAquittalDate() != null)
                .collect(Collectors.groupingBy(
                        uk.gov.justice.progression.courts.exract.Offences::getAquittalDate,
                        () -> new TreeMap<>(Comparator.naturalOrder()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(offencesSorted)
                                        .toList()
                        )
                ));

        return groupedOffences;
    }

    private Map<LocalDate, List<uk.gov.justice.progression.courts.exract.CourtOrderOffences>> getCourtOrderOffencesByConvictionDate(final CourtApplications courtApplication) {
        return courtApplication.getCourtOrders().getCourtOrderOffences().stream()
                .filter(courtOrderOffence -> Objects.nonNull(courtOrderOffence.getConvictionDate()))
                .collect(Collectors.groupingBy(uk.gov.justice.progression.courts.exract.CourtOrderOffences::getConvictionDate, TreeMap::new, Collectors.toList()));
    }

    private static Address toAddress(final Address address) {
        final Address.Builder addressBuilder = Address.address();
        if (nonNull(address)) {
            addressBuilder
                    .withAddress1(address.getAddress1())
                    .withAddress2(address.getAddress2())
                    .withAddress3(address.getAddress3())
                    .withAddress4(address.getAddress4())
                    .withAddress5(address.getAddress5())
                    .withPostcode(address.getPostcode());
        }
        return addressBuilder.build();
    }

    private List<JudicialResult> transformJudicialResults(final List<Hearings> hearingsList, final UUID masterDefendantId, final UUID defendantId, final GetHearingsAtAGlance hearingsAtAGlance, final List<UUID> linkedApplicationDefendantIds) {
        final List<JudicialResult> judicialResultsList = new ArrayList<>();
        hearingsList.forEach(hearings -> {
            judicialResultsList.addAll(transformJudicialResults(hearings, masterDefendantId, defendantId, hearingsAtAGlance, linkedApplicationDefendantIds));
        });
        return judicialResultsList.stream()
                .map(JudicialResultTransformer::toCourtJudicialResult)
                .toList();
    }

    private List<JudicialResult> transformJudicialResults(final Hearings hearings, final UUID masterDefendantId, final UUID defendantId, final GetHearingsAtAGlance hearingsAtAGlance, final List<UUID> linkedApplicationDefendantIds) {
        final List<JudicialResult> judicialResultsList = new ArrayList<>();
        if (isNotEmpty(hearings.getDefendantJudicialResults())) {
            final List<JudicialResult> defendantLevelJudicialResults = hearings.getDefendantJudicialResults().stream()
                    .filter(Objects::nonNull)
                    .filter(defendantJudicialResult -> masterDefendantId.equals(defendantJudicialResult.getMasterDefendantId()) ||  linkedApplicationDefendantIds.contains(defendantJudicialResult.getMasterDefendantId()))
                    .map(DefendantJudicialResult::getJudicialResult)
                    .filter(Objects::nonNull)
                    .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                    .toList();
            judicialResultsList.addAll(defendantLevelJudicialResults);
        }

        if (isNotEmpty(hearings.getDefendants())) {
            final List<JudicialResult> caseLevelJudicialResults = hearings.getDefendants().stream()
                    .filter(Objects::nonNull)
                    .filter(defendants -> defendantId.equals(defendants.getId()) ||  linkedApplicationDefendantIds.contains(defendants.getId()))
                    .map(Defendants::getJudicialResults)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                    .filter(jr -> !judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList().contains(jr.getJudicialResultId()))
                    .toList();
            judicialResultsList.addAll(caseLevelJudicialResults);
        }


        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            transformApplicationDefendantLevelJudicialResults(hearings, hearingsAtAGlance, judicialResultsList, masterDefendantId);
        }

        return judicialResultsList.stream()
                .map(JudicialResultTransformer::toCourtJudicialResult)
                .toList();
    }

    private void transformApplicationDefendantLevelJudicialResults(final Hearings hearings, final GetHearingsAtAGlance hearingsAtAGlance, final List<JudicialResult> judicialResultsList, final UUID masterDefendantId) {
        final List<CourtApplication> applicationsExtractList = new ArrayList<>();

        hearingsAtAGlance.getCourtApplications().forEach(app -> getResultedApplication(app, hearings, masterDefendantId).ifPresent(resultedApplication ->
                applicationsExtractList.add(mergeApplicationResults(app, resultedApplication))
        ));
        final List<UUID> alreadyAddedJudicialResults = judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList();
        judicialResultsList.addAll(applicationsExtractList.stream()
                .map(CourtApplication::getCourtOrder)
                .filter(Objects::nonNull)
                .map(CourtOrder::getCourtOrderOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(CourtOrderOffence::getOffence)
                .filter(Objects::nonNull)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> !alreadyAddedJudicialResults.contains(jr.getJudicialResultId()))
                .toList());

        judicialResultsList.addAll(applicationsExtractList.stream()
                .map(CourtApplication::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> !alreadyAddedJudicialResults.contains(jr.getJudicialResultId()))
                .toList());
        judicialResultsList.addAll(applicationsExtractList.stream()
                .map(CourtApplication::getCourtApplicationCases)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(CourtApplicationCase::getOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> !alreadyAddedJudicialResults.contains(jr.getJudicialResultId()))
                .toList());
    }

    private List<JudicialResult> transformJudicialResults(final uk.gov.justice.core.courts.Hearing hearing, final UUID masterDefendantId, final UUID defendantId, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {
        final List<JudicialResult> judicialResultsList = new ArrayList<>();
        final List<UUID> originalHearingOffenceIds = isNull(extractHearings) ? emptyList() : extractHearings.stream()
                .map(uk.gov.justice.progression.courts.exract.Hearings::getOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(uk.gov.justice.progression.courts.exract.Offences::getId)
                .toList();
        if (isNotEmpty(hearing.getDefendantJudicialResults())) {
            final List<JudicialResult> defendantLevelJudicialResults = hearing.getDefendantJudicialResults().stream()
                    .filter(Objects::nonNull)
                    .filter(defendantJudicialResult -> masterDefendantId.equals(defendantJudicialResult.getMasterDefendantId()))
                    .map(DefendantJudicialResult::getJudicialResult)
                    .filter(Objects::nonNull)
                    .filter(jr -> isNull(jr.getOffenceId()) || originalHearingOffenceIds.contains(jr.getOffenceId()))
                    .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                    .toList();
            judicialResultsList.addAll(defendantLevelJudicialResults);
        }

        judicialResultsList.addAll(ofNullable(hearing.getProsecutionCases()).stream().flatMap(Collection::stream).map(ProsecutionCase::getDefendants).flatMap(Collection::stream)
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .map(uk.gov.justice.core.courts.Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr -> isNull(jr.getOffenceId()) || originalHearingOffenceIds.contains(jr.getOffenceId()))
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .filter(jr -> !judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList().contains(jr.getJudicialResultId()))
                .toList());

        judicialResultsList.addAll(Optional.ofNullable(hearing.getCourtApplications()).stream().flatMap(Collection::stream)
                .map(CourtApplication::getCourtOrder)
                .filter(Objects::nonNull)
                .map(CourtOrder::getCourtOrderOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(CourtOrderOffence::getOffence)
                .filter(Objects::nonNull)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr -> isNull(jr.getOffenceId()) || originalHearingOffenceIds.contains(jr.getOffenceId()))
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .filter(jr -> !judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList().contains(jr.getJudicialResultId()))
                .toList());

        judicialResultsList.addAll(Optional.ofNullable(hearing.getCourtApplications()).stream().flatMap(Collection::stream)
                .map(CourtApplication::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr -> isNull(jr.getOffenceId()) || originalHearingOffenceIds.contains(jr.getOffenceId()))
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .filter(jr -> !judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList().contains(jr.getJudicialResultId()))
                .toList());
        judicialResultsList.addAll(Optional.ofNullable(hearing.getCourtApplications()).stream().flatMap(Collection::stream)
                .map(CourtApplication::getCourtApplicationCases)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(CourtApplicationCase::getOffences)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(Offence::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr -> isNull(jr.getOffenceId()) || originalHearingOffenceIds.contains(jr.getOffenceId()))
                .filter(j -> DEFENDANT_LEVEL.equals(j.getLevel()))
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .filter(jr -> !judicialResultsList.stream().map(JudicialResult::getJudicialResultId).toList().contains(jr.getJudicialResultId()))
                .toList());

        return judicialResultsList.stream()
                .map(JudicialResultTransformer::toCourtJudicialResult)
                .toList();
    }

    private Defendant transformDefendantWithoutHearingDetails(final uk.gov.justice.core.courts.Defendant caseDefendant, final Defendant.Builder defendantBuilder) {
        if (nonNull(caseDefendant)) {
            if (nonNull(caseDefendant.getPersonDefendant())) {
                final Person personDefendant = caseDefendant.getPersonDefendant().getPersonDetails();
                defendantBuilder.withDateOfBirth(personDefendant.getDateOfBirth());
                defendantBuilder.withAge(transformationHelper.getDefendantAge(caseDefendant));
                defendantBuilder.withAddress(personDefendant.getAddress());
            }

            if (nonNull(caseDefendant.getLegalEntityDefendant()) && nonNull(caseDefendant.getLegalEntityDefendant().getOrganisation().getAddress())) {
                defendantBuilder.withAddress(caseDefendant.getLegalEntityDefendant().getOrganisation().getAddress());
            }
        }
        defendantBuilder.withDefenceOrganisations(nonNull(caseDefendant.getDefenceOrganisation()) ? getDefenceOrganisation(caseDefendant) : null);
        return defendantBuilder.build();
    }

    private List<DefenceOrganisations> transformDefenceOrganisation(final uk.gov.justice.core.courts.Organisation organisation) {
        return Collections.singletonList(
                DefenceOrganisations.defenceOrganisations()
                        .withDefenceOrganisation(organisation)
                        .build()
        );
    }

    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final Hearings hearings, final UUID defendantId, final UUID userId,
                                                                                     final List<Hearings> hearingsList, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap,
                                                                                     final List<UUID> linkedApplicationDefendantIds) {
        final List<uk.gov.justice.progression.courts.exract.Offences> offences = new ArrayList<>();

        final List<Offences> defendantOffences = getDefendantOffences(hearings, defendantId, linkedApplicationDefendantIds);
        if (isNotEmpty(defendantOffences)) {
            final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap = getUuidCommittedForSentenceMap(hearings.getId(), defendantId, userId, hearingsList);
            offences.addAll(transformOffence(defendantOffences, hearings.getId(), offenceCommittedForSentenceMap, hearings.getJurisdictionType(), resultIdSlipRuleAmendmentsMap));
        }
        return offences;
    }


    private static List<Offences> getDefendantOffences(final Hearings hearings, final UUID defendantId, final List<UUID> linkedApplicationDefendantIds) {
        return hearings.getDefendants().stream().filter(Objects::nonNull)
                .filter(d -> d.getId().equals(defendantId) || d.getMasterDefendantId().equals(defendantId) || linkedApplicationDefendantIds.contains(d.getMasterDefendantId()))
                .filter(d -> nonNull(d.getOffences()))
                .map(Defendants::getOffences)
                .flatMap(Collection::stream)
                .toList();
    }

    private Map<UUID, CommittedForSentence> getUuidCommittedForSentenceMap(final UUID hearingId, final UUID defendantId, final UUID userId, final List<Hearings> hearingsList) {
        final Hearing hearingFromListing = listingQueryService.searchHearing(userId, hearingId);
        final List<Offences> offencesFromSeedingHearings = courtExtractHelper.getOffencesFromSeedingHearings(defendantId, hearingFromListing, hearingsList);
        final List<ResultDefinition> filteredResultDefinitions = getResultDefinitionsCommittedToCCOrSentToCC(userId, getResultDefinitionIds(offencesFromSeedingHearings));

        return courtExtractHelper.getOffencesResultedWithCommittedForSentence(defendantId, hearingFromListing, hearingsList, filteredResultDefinitions);
    }


    private List<ResultDefinition> getResultDefinitionsCommittedToCCOrSentToCC(final UUID userId, final List<UUID> uuids) {
        if (isEmpty(uuids)) {
            return emptyList();
        }
        return referenceDataService.getResultDefinitionsByIds(userId, uuids)
                .stream()
                .filter(rd -> nonNull(rd.getCategory()) && CATEGORY_INTERMEDIARY.equals(rd.getCategory()))
                .filter(rd -> nonNull(rd.getResultDefinitionGroup()) && (RD_GROUP_COMMITTED_TO_CC.equals(rd.getResultDefinitionGroup()) || RD_GROUP_SENT_TO_CC.equals(rd.getResultDefinitionGroup())))
                .toList();
    }

    private List<UUID> getResultDefinitionIds(final List<Offences> defendantOffences) {
        return defendantOffences.stream()
                .flatMap(o -> o.getJudicialResults().stream())
                .map(JudicialResult::getJudicialResultTypeId).collect(toList());
    }

    private List<AttendanceDayAndType> transformDefendantAttendancesDay(final List<DefendantAttendance> defendantAttendances, final Defendants defendant) {
        if (isNotEmpty(defendantAttendances)) {
            return defendantAttendances.stream()
                    .filter(Objects::nonNull)
                    .filter(da -> da.getDefendantId().toString().equals(defendant.getId().toString()))
                    .map(DefendantAttendance::getAttendanceDays)
                    .flatMap(Collection::stream)
                    .filter(ad -> !ad.getAttendanceType().equals(AttendanceType.NOT_PRESENT))
                    .map(ad -> AttendanceDayAndType.attendanceDayAndType()
                            .withDay(ad.getDay())
                            .withAttendanceType(ad.getAttendanceType().toString().equals(AttendanceType.IN_PERSON.toString())
                                    ? PRESENT_IN_PERSON : extractAttendanceType(defendant)).build())
                    .toList();
        }

        return emptyList();
    }

    private List<AttendanceDayAndType> transformDefendantAttendancesDay(final List<DefendantAttendance> defendantAttendances, final Defendants defendant,  final List<UUID> linkedApplicationDefendantIds) {
        if (isNotEmpty(defendantAttendances)) {
            return defendantAttendances.stream()
                    .filter(Objects::nonNull)
                    .filter(da -> da.getDefendantId().equals(defendant.getId()) || linkedApplicationDefendantIds.contains(da.getDefendantId()))
                    .map(DefendantAttendance::getAttendanceDays)
                    .flatMap(Collection::stream)
                    .filter(ad -> !ad.getAttendanceType().equals(AttendanceType.NOT_PRESENT))
                    .map(ad -> AttendanceDayAndType.attendanceDayAndType()
                            .withDay(ad.getDay())
                            .withAttendanceType(ad.getAttendanceType().toString().equals(AttendanceType.IN_PERSON.toString())
                                    ? PRESENT_IN_PERSON : extractAttendanceType(defendant)).build())
                    .toList();
        }

        return emptyList();
    }

    private List<AttendanceDayAndType> transformDefendantAttendanceDay(final List<Hearings> hearingsList, final Defendants defendant) {
        final List<AttendanceDayAndType> attendanceDayAndTypeList = new ArrayList<>();
        hearingsList.forEach(hearings -> {
            attendanceDayAndTypeList.addAll(transformDefendantAttendancesDay(hearings.getDefendantAttendance(), defendant));
        });
        return attendanceDayAndTypeList;
    }

    private String extractAttendanceType(final Defendants defendant) {
        if (nonNull(defendant.getCustodialEstablishment())) {
            final String custody = defendant.getCustodialEstablishment().getCustody();
            if ("Prison".equals(custody)) {
                return PRESENT_BY_PRISON_VIDEO_LINK;
            } else if ("Police".equals(custody)) {
                return PRESENT_BY_POLICE_VIDEO_LINK;
            }
        }
        return PRESENT_BY_VIDEO_DEFAULT;
    }

    protected List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final List<Offences> offences, final UUID hearingId,
                                                                                       final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap,
                                                                                       final JurisdictionType jurisdictionType, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        final Set<String> guiltyPleaTypes = referenceDataService.retrieveGuiltyPleaTypes();
        final List<uk.gov.justice.progression.courts.exract.Offences> offencesList = offences.stream()
                .map(o -> {
                    final List<JudicialResult> resultList = transformResults(filterOutResultDefinitionsNotToBeShownInCourtExtract(o.getJudicialResults(), hearingId));
                    return toOffences(o, resultList, offenceCommittedForSentenceMap.get(o.getId()), resultIdSlipRuleAmendmentsMap, guiltyPleaTypes);
                })
                .toList();

        return sortOffences(jurisdictionType, offencesList);
    }

    protected List<uk.gov.justice.progression.courts.exract.Offences> transformCourtOffence(final List<uk.gov.justice.core.courts.Offence> offences, final UUID hearingId,
                                                                                            final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap,
                                                                                            final JurisdictionType jurisdictionType, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        final Set<String> guiltyPleaTypes = referenceDataService.retrieveGuiltyPleaTypes();
        final List<uk.gov.justice.progression.courts.exract.Offences> offencesList = offences.stream()
                .map(o -> {
                    final List<JudicialResult> resultList = transformResults(filterOutResultDefinitionsNotToBeShownInCourtExtract(o.getJudicialResults(), hearingId));
                    return toOffences(o, resultList, offenceCommittedForSentenceMap.get(o.getId()), resultIdSlipRuleAmendmentsMap, guiltyPleaTypes);
                })
                .toList();

        return sortOffences(jurisdictionType, offencesList);
    }

    protected List<uk.gov.justice.progression.courts.exract.Offences> transformOffenceForCourtApplicationCases(final List<uk.gov.justice.core.courts.Offence> offences, final UUID hearingId,
                                                                                                               final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap,
                                                                                                               final JurisdictionType jurisdictionType, final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap) {

        final Set<String> guiltyPleaTypes = referenceDataService.retrieveGuiltyPleaTypes();
        final List<uk.gov.justice.progression.courts.exract.Offences> offencesList = offences.stream()
                .map(o -> {
                    final List<JudicialResult> resultList = Optional.ofNullable(transformResults(filterOutResultDefinitionsNotToBeShownInCourtExtract(o.getJudicialResults(), hearingId))).stream().flatMap(Collection::stream)
                            .filter(j -> !DEFENDANT_LEVEL.equals(j.getLevel()))
                            .toList();
                    return toOffences(o, resultList, offenceCommittedForSentenceMap.get(o.getId()), resultIdSlipRuleAmendmentsMap, guiltyPleaTypes);
                })
                .toList();

        return sortOffences(jurisdictionType, offencesList);
    }


    private List<JudicialResult> filterOutResultDefinitionsNotToBeShownInCourtExtract(final List<JudicialResult> judicialResults, final UUID hearingId) {
        return isNotEmpty(judicialResults) ?
                judicialResults.stream()
                        .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract() &&
                                hearingId.equals(jr.getOrderedHearingId()))
                        .toList()
                : judicialResults;
    }

    private List<JudicialResult> transformResults(final List<JudicialResult> judicialResults) {
        if (isNotEmpty(judicialResults)) {
            return judicialResults.stream()
                    .map(JudicialResultTransformer::toCourtJudicialResult)
                    .toList();
        }
        return judicialResults;
    }

    private uk.gov.justice.progression.courts.exract.Hearings getExtractHearing(final Defendants defendant, final UUID masterDefendantId, final UUID userId, final Hearings hearing, final GetHearingsAtAGlance hearingsAtAGlance, final String extractType, final List<UUID> linkedApplicationDefendantIds) {
        final uk.gov.justice.progression.courts.exract.Hearings.Builder hearingBuilder = uk.gov.justice.progression.courts.exract.Hearings.hearings()
                .withHearingDays(transformationHelper.transformHearingDays(hearing.getHearingDays()))
                .withId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType() != null ? JurisdictionType.valueOf(hearing.getJurisdictionType().toString()) : null)
                .withCourtCentre(transformCourtCenter(hearing, defendant.getId()))
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withType(hearing.getType().getDescription());

        final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap = COURT_EXTRACT.equals(extractType) ? getResultIdAmendmentListMap(hearing, userId) : emptyMap();

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            List<CourtApplications> courtApplications = transformCourtApplications(hearingsAtAGlance.getCourtApplications(), hearing, resultIdSlipRuleAmendmentsMap, masterDefendantId, userId);
            hearingBuilder.withCourtApplications(courtApplications);
            if (!courtApplications.isEmpty()) {
                hearingBuilder.withAuthorisedLegalAdvisors(courtExtractHelper.getApplicationAuthorisedLegalAdvisors(courtApplications));
            }
        }

        if (nonNull(hearing.getProsecutionCounsels())) {
            hearingBuilder.withProsecutionCounsels(transformProsecutionCounsel(hearing.getProsecutionCounsels()));
        }

        List<DefenceCounsels> defenceCounselList = getDefendantDefenceCounsels(hearing, defendant.getId());
        if (nonNull(defenceCounselList) && isNotEmpty(defenceCounselList)) {
            hearingBuilder.withDefenceCounsels(defenceCounselList);
        }

        if (hearing.getJudiciary() != null) {
            final CrownCourtDecisions crownCourtDecisions = getCourtDecisions(hearing.getJudiciary(), hearing.getHearingDays());
            hearingBuilder.withCrownCourtDecisions(crownCourtDecisions);
        }

        final List<uk.gov.justice.progression.courts.exract.Offences> offences = transformOffence(hearing, defendant.getId(), userId, hearingsAtAGlance.getHearings(), resultIdSlipRuleAmendmentsMap, linkedApplicationDefendantIds);

        final boolean proceedingsConcluded = offences.stream()
                .allMatch(offence1 -> TRUE.equals(offence1.getProceedingsConcluded()));
        hearingBuilder.withAllOffencesProceedingsConcluded(proceedingsConcluded);

        if (!offences.isEmpty()) {
            hearingBuilder.withOffences(offences);
            hearingBuilder.withAuthorisedLegalAdvisors(courtExtractHelper.getAuthorisedLegalAdvisors(offences));
        }

        hearingBuilder.withAttendanceDays(transformAttendanceDayAndTypes(transformDefendantAttendancesDay(hearing.getDefendantAttendance(), defendant, linkedApplicationDefendantIds)));

        final List<JudicialResult> judicialResults = transformJudicialResults(hearing, masterDefendantId, defendant.getId(), hearingsAtAGlance, linkedApplicationDefendantIds);
        hearingBuilder.withDefendantResults(getDefendantResultsWithAmendments(judicialResults, resultIdSlipRuleAmendmentsMap));
        hearingBuilder.withDeletedDefendantResults(getDeletedDefendantResultsWithAmendments(resultIdSlipRuleAmendmentsMap));

        if (hearing.getCompanyRepresentatives() != null) {
            final List<CompanyRepresentative> companyRepresentatives = transformCompanyRepresentatives(hearing.getCompanyRepresentatives(), hearing.getHearingDays());
            hearingBuilder.withCompanyRepresentatives(transformCompanyRepresentative(companyRepresentatives));
        }

        return hearingBuilder.build();
    }

    private uk.gov.justice.progression.courts.exract.Hearings getExtractHearingFromLinkedApplicationHearing(final Defendants defendant, final UUID userId, final uk.gov.justice.core.courts.Hearing hearing, final List<CourtApplication> courtApplications, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings, final UUID masterDefendantId, final List<UUID> linkedApplicationDefendantIds) {
        final uk.gov.justice.progression.courts.exract.Hearings.Builder hearingBuilder = uk.gov.justice.progression.courts.exract.Hearings.hearings()
                .withHearingDays(transformationHelper.transformHearingDays(hearing.getHearingDays()))
                .withId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType() != null ? JurisdictionType.valueOf(hearing.getJurisdictionType().toString()) : null)
                .withCourtCentre(transformCourtCenter(hearing, defendant.getId()))
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withType(hearing.getType().getDescription());

        final Map<UUID, List<Amendments>> resultIdSlipRuleAmendmentsMap = getResultIdAmendmentListMapForLinkedApplication(hearing, userId, extractHearings);


        List<CourtApplications> courtApplicationsList = transformCourtApplications(courtApplications, hearing, resultIdSlipRuleAmendmentsMap, extractHearings);
        hearingBuilder.withCourtApplications(courtApplicationsList);

        if (nonNull(hearing.getProsecutionCounsels())) {
            hearingBuilder.withProsecutionCounsels(transformProsecutionCounsel(hearing.getProsecutionCounsels()));
        }

        List<DefenceCounsels> defenceCounselList = getDefendantDefenceCounsels(hearing, defendant.getId(), linkedApplicationDefendantIds);
        if (nonNull(defenceCounselList) && isNotEmpty(defenceCounselList)) {
            hearingBuilder.withDefenceCounsels(defenceCounselList);
        }

        if (hearing.getJudiciary() != null) {
            final CrownCourtDecisions crownCourtDecisions = getCourtDecisions(hearing.getJudiciary(), hearing.getHearingDays());
            hearingBuilder.withCrownCourtDecisions(crownCourtDecisions);
        }

        final List<JudicialResult> judicialResults = transformJudicialResults(hearing, masterDefendantId, defendant.getId(), extractHearings);
        hearingBuilder.withDefendantResults(getDefendantResultsWithAmendments(judicialResults, resultIdSlipRuleAmendmentsMap));
        hearingBuilder.withDeletedDefendantResults(getDeletedDefendantResultsWithAmendments(resultIdSlipRuleAmendmentsMap));

        hearingBuilder.withAttendanceDays(transformAttendanceDayAndTypes(transformDefendantAttendancesDay(hearing.getDefendantAttendance(), defendant,  linkedApplicationDefendantIds)));

        if (hearing.getCompanyRepresentatives() != null) {
            final List<CompanyRepresentative> companyRepresentatives = transformCompanyRepresentatives(hearing.getCompanyRepresentatives(), hearing.getHearingDays());
            hearingBuilder.withCompanyRepresentatives(transformCompanyRepresentative(companyRepresentatives));
        }

        if (!courtApplicationsList.isEmpty()) {
            hearingBuilder.withAuthorisedLegalAdvisors(courtExtractHelper.getApplicationAuthorisedLegalAdvisorsForLinkedApplication(courtApplicationsList, judicialResults));
        }

        return hearingBuilder.build();
    }

    private List<CompanyRepresentative> transformCompanyRepresentatives(final List<CompanyRepresentative> companyRepresentatives, final List<HearingDay> hearingDays) {
        return companyRepresentatives.stream()
                .filter(rep -> new HashSet<>(rep.getAttendanceDays()).containsAll(
                        hearingDays.stream()
                                .map(day -> day.getSittingDay().toLocalDate())
                                .toList()
                ))
                .toList();
    }

    private Map<UUID, List<Amendments>> getResultIdAmendmentListMapForLinkedApplication(final uk.gov.justice.core.courts.Hearing hearing, final UUID userId, final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {
        final List<LocalDate> hearingDayList = hearing.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).toList();
        final List<DraftResultsWrapper> defendantResultsWithAmendments = hearingQueryService.getDraftResultsWithAmendments(userId, hearing.getId(), hearingDayList);
        final UUID slipRuleReasonId = referenceDataService.getAmendmentReasonId(userId, SLIPRULE_AMENDMENT_REASON_CODE);

        final List<UUID> resultDefinitionsInSlipRule = getResultDefinitionsInSlipRuleAmendments(defendantResultsWithAmendments, slipRuleReasonId);
        final List<ResultDefinition> slipRuleResultDefinitionList = isNotEmpty(resultDefinitionsInSlipRule)
                ? referenceDataService.getResultDefinitionsByIds(userId, resultDefinitionsInSlipRule)
                : emptyList();
        return extractAmendmentsDueToSlipRule(defendantResultsWithAmendments, slipRuleResultDefinitionList, slipRuleReasonId);

    }

    private Map<UUID, List<Amendments>> getResultIdAmendmentListMap(final Hearings hearing, final UUID userId) {
        final List<LocalDate> hearingDayList = hearing.getHearingDays().stream().map(hd -> hd.getSittingDay().toLocalDate()).collect(toList());
        final List<DraftResultsWrapper> defendantResultsWithAmendments = hearingQueryService.getDraftResultsWithAmendments(userId, hearing.getId(), hearingDayList);
        final UUID slipRuleReasonId = referenceDataService.getAmendmentReasonId(userId, SLIPRULE_AMENDMENT_REASON_CODE);

        final List<UUID> resultDefinitionsInSlipRule = getResultDefinitionsInSlipRuleAmendments(defendantResultsWithAmendments, slipRuleReasonId);
        final List<ResultDefinition> slipRuleResultDefinitionList = isNotEmpty(resultDefinitionsInSlipRule)
                ? referenceDataService.getResultDefinitionsByIds(userId, resultDefinitionsInSlipRule)
                : emptyList();
        return extractAmendmentsDueToSlipRule(defendantResultsWithAmendments, slipRuleResultDefinitionList, slipRuleReasonId);
    }

    private List<DefenceCounsels> getDefendantDefenceCounsels(final Hearings hearing, final UUID defendantId) {
        if (nonNull(hearing.getDefendants()) && isNotEmpty(hearing.getDefendants())) {
            return hearing.getDefendants().stream()
                    .filter(da -> da.getId().equals(defendantId))
                    .filter(da -> nonNull(da.getDefenceOrganisation()))
                    .map(da -> transformDefenceCounsel(da.getDefenceOrganisation().getDefenceCounsels()))
                    .flatMap(Collection::stream).toList();
        }
        return emptyList();
    }

    private List<DefenceCounsels> getDefendantDefenceCounsels(final uk.gov.justice.core.courts.Hearing hearing, final UUID defendantId, final List<UUID> linkedApplicationDefendantIds) {
        final List<DefenceCounsels> defenceCounsels = new ArrayList<>();
        if (isNotEmpty(hearing.getProsecutionCases())) {
            defenceCounsels.addAll(hearing.getProsecutionCases().stream().map(ProsecutionCase::getDefendants).flatMap(Collection::stream)
                    .filter(da -> da.getId().equals(defendantId) || linkedApplicationDefendantIds.contains(da.getId()))
                    .filter(da -> nonNull(da.getDefenceOrganisation()))
                    .map(da -> transformDefenceCounsel(getDefenceCounselsForDefendant(hearing.getDefenceCounsels(), defendantId, linkedApplicationDefendantIds)))
                    .flatMap(Collection::stream).toList());
        }
        if (isNotEmpty(hearing.getDefenceCounsels()) && defenceCounsels.isEmpty()) {
            defenceCounsels.addAll(hearing.getDefenceCounsels().stream()
                    .filter(da -> da.getDefendants().stream().anyMatch(id -> id.equals(defendantId) || linkedApplicationDefendantIds.contains(id)))
                    .map(da -> transformDefenceCounsel(getDefenceCounselsForDefendant(List.of(da), defendantId, linkedApplicationDefendantIds)))
                    .flatMap(Collection::stream).toList());
        }
        return defenceCounsels;
    }

    private static List<DefenceCounsel> getDefenceCounselsForDefendant(final List<DefenceCounsel> defenceCounsels, final UUID defendantId, final List<UUID> linkedApplicationDefendantIds) {
        if (isNotEmpty(defenceCounsels)) {
            return defenceCounsels.stream()
                    .filter(defenceCounsel -> defenceCounsel.getDefendants().contains(defendantId) ||
                            defenceCounsel.getDefendants().stream().anyMatch(id-> linkedApplicationDefendantIds.contains(id)))
                    .toList();
        }
        return new ArrayList<>();
    }


    private CrownCourtDecisions getCourtDecisions(final List<JudicialRole> judiciary, final List<HearingDay> hearingDays) {

        final List<JudiciaryNamesByRole> judiciaryNamesByRoleList = new ArrayList<>();
        judiciary.stream()
                .filter(j -> nonNull(j.getJudicialRoleType()))
                .map(j -> Pair.of(j.getJudicialRoleType().getJudiciaryType(), transformationHelper.getJudgeName(j.getJudicialId())))
                .collect(groupingBy(Pair::getKey))
                .forEach((k, v) -> {
                    if (nonNull(k) && nonNull(v)) {
                        judiciaryNamesByRoleList.add(JudiciaryNamesByRole.judiciaryNamesByRole()
                                .withRole(k)
                                .withNames(v.stream().map(Pair::getValue).collect(toList()))
                                .build());
                    }
                });

        return CrownCourtDecisions.crownCourtDecisions()
                .withJudiciaryNamesByRole(judiciaryNamesByRoleList)
                .withDates(transformationHelper.transformDates(hearingDays))
                .build();
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre transformCourtCenter(final Hearings hearings, final UUID defendantId) {
        if (hearingsDefendantIdBiPredicate.test(hearings, defendantId)) {
            LOGGER.info("hearings Youth Court Name {}", hearings.getYouthCourt().getName());
            return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                    .withName(hearings.getYouthCourt().getName())
                    .withWelshName(hearings.getYouthCourt().getWelshName())
                    .withId(hearings.getYouthCourt().getYouthCourtId())
                    .build();
        }
        return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                .withName(hearings.getCourtCentre().getName())
                .withId(hearings.getCourtCentre().getId())
                .withWelshName(hearings.getCourtCentre().getWelshName())
                .build();
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre transformCourtCenter(final uk.gov.justice.core.courts.Hearing hearing, final UUID defendantId) {
        if (hearingDefendantIdBiPredicate.test(hearing, defendantId)) {
            LOGGER.info("hearing Youth Court Name {}", hearing.getYouthCourt().getName());
            return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                    .withName(hearing.getYouthCourt().getName())
                    .withWelshName(hearing.getYouthCourt().getWelshName())
                    .withId(hearing.getYouthCourt().getYouthCourtId())
                    .build();
        }
        return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                .withName(hearing.getCourtCentre().getName())
                .withId(hearing.getCourtCentre().getId())
                .withWelshName(hearing.getCourtCentre().getWelshName())
                .build();
    }

    private List<CompanyRepresentatives> transformCompanyRepresentative(final List<CompanyRepresentative> companyRepresentatives) {
        return companyRepresentatives.stream().map(cr -> CompanyRepresentatives.companyRepresentatives()
                .withName(transformationHelper.getName(cr.getFirstName(), EMPTY, cr.getLastName()))
                .withAttendanceDays(transformAttendanceDays(cr.getAttendanceDays()))
                .withRole(cr.getPosition() != null ? cr.getPosition().toString() : EMPTY)
                .build()
        ).toList();
    }

    private List<DefenceCounsels> transformDefenceCounsels(final List<Hearings> hearingsList, final String defendantId) {
        return hearingsList.stream()
                .filter(hearing -> isNotEmpty(hearing.getDefendants()))
                .flatMap((h -> h.getDefendants().stream()))
                .filter(da -> da.getId().toString().equals(defendantId))
                .filter(da -> nonNull(da.getDefenceOrganisation()))
                .map(da -> transformDefenceCounsel(da.getDefenceOrganisation().getDefenceCounsels()))
                .flatMap(Collection::stream).toList();
    }

    private List<CompanyRepresentatives> transformCompanyRepresentatives(final List<Hearings> hearingsList) {
        return hearingsList.stream()
                .filter(hearing -> isNotEmpty(hearing.getCompanyRepresentatives()))
                .map(hearing -> transformCompanyRepresentative(hearing.getCompanyRepresentatives()))
                .flatMap(Collection::stream).toList();
    }

    private static List<DefenceOrganisations> getDefenceOrganisation(final uk.gov.justice.core.courts.Defendant defendant) {
        return Collections.singletonList(DefenceOrganisations.defenceOrganisations()
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .build());
    }
}
