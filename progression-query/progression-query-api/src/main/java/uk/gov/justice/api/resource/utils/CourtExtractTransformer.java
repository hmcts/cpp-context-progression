package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.api.resource.DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.COURT_EXTRACT;
import static uk.gov.justice.api.resource.service.DefenceQueryService.DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS;
import static uk.gov.justice.api.resource.service.ListingQueryService.LISTING_SEARCH_HEARING;
import static uk.gov.justice.api.resource.service.ReferenceDataService.REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS;
import static uk.gov.justice.api.resource.utils.OffenceTransformer.toOffences;
import static uk.gov.justice.api.resource.utils.TransformationHelper.getHearingsSortedByHearingDaysAsc;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.AttendanceType;
import uk.gov.justice.core.courts.CompanyRepresentative;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.DefendantAttendance;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.progression.courts.DefendantHearings;
import uk.gov.justice.progression.courts.Defendants;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.AttendanceDayAndType;
import uk.gov.justice.progression.courts.exract.AttendanceDays;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.CompanyRepresentatives;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtOrderOffences;
import uk.gov.justice.progression.courts.exract.CourtOrders;
import uk.gov.justice.progression.courts.exract.CrownCourtDecisions;
import uk.gov.justice.progression.courts.exract.CustodialEstablishment;
import uk.gov.justice.progression.courts.exract.DefenceCounsels;
import uk.gov.justice.progression.courts.exract.DefenceOrganisations;
import uk.gov.justice.progression.courts.exract.Defendant;
import uk.gov.justice.progression.courts.exract.JudiciaryNamesByRole;
import uk.gov.justice.progression.courts.exract.ParentGuardian;
import uk.gov.justice.progression.courts.exract.ProsecutionCounsels;
import uk.gov.justice.progression.courts.exract.PublishingCourt;
import uk.gov.justice.progression.courts.exract.Representation;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S1612", "squid:S3655", "squid:S2259", "squid:S1188", "squid:S2789", "squid:S1067", "squid:MethodCyclomaticComplexity", "pmd:NullAssignment", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1172"})
public class CourtExtractTransformer {

    public static final String PRESENT_BY_PRISON_VIDEO_LINK = "Present - prison video link";
    public static final String PRESENT_BY_POLICE_VIDEO_LINK = "Present - police video link";
    public static final String PRESENT_IN_PERSON = "Present - in person";
    public static final String PRESENT_BY_VIDEO_DEFAULT = "Present - by video";
    private static final BiPredicate<Hearings, UUID> hearingsDefendantIdBiPredicate = (hearings, defendantId) -> nonNull(hearings.getYouthCourtDefendantIds()) && hearings.getYouthCourtDefendantIds().stream().anyMatch(youthDefendantId -> youthDefendantId.equals(defendantId));
    public static final String CATEGORY_INTERMEDIARY = "I";
    public static final String RD_GROUP_COMMITTED_TO_CC = "CommittedToCC";
    public static final String RD_GROUP_SENT_TO_CC = "SentToCC";

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

        defendantBuilder.withName(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantName()) ? defendantHearings.getDefendantName() : EMPTY);
        defendantBuilder.withId(nonNull(defendantHearings) && nonNull(defendantHearings.getDefendantId()) ? defendantHearings.getDefendantId() : fromString(EMPTY));

        final List<UUID> hearingIds = defendantHearings.getHearingIds();
        //add any hearings associated with breachType courtApplications
        addBreachTypeApplicationHearings(defendantId, hearingIds, hearingsAtAGlance.getHearings(), hearingsAtAGlance.getCourtApplications());

        final List<Hearings> hearingsList = hearingsAtAGlance.getHearings().stream()
                .filter(h -> hearingIds.contains(h.getId()))
                .filter(COURT_EXTRACT.equals(extractType) ? h -> selectedHearingIdList.contains(h.getId().toString()) : h -> true)
                .toList();

        if (caseDefendant.isPresent() && masterDefendantId.isPresent()) {
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, courtExtract, defendantBuilder, hearingsList, caseDefendant.get());
        }

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            courtExtract.withIsAppealPending(transformationHelper.getAppealPendingFlag(hearingsAtAGlance.getCourtApplications()));
        }

        return courtExtract.build();
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
        ;

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
            extractHearingDetails(hearingsAtAGlance, fromString(defendantId), userId, ejectExtract, defendantBuilder, hearingsList, caseDefendant.get());
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
                                       final Defendant.Builder defendantBuilder, final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant) {
        final UUID masterDefendantId = caseDefendant.getMasterDefendantId();

        final Hearings latestHearing = hearingsList.size() > 1 ? transformationHelper.getLatestHearings(hearingsList) : hearingsList.get(0);

        courtExtract.withDefendant(transformDefendants(latestHearing.getDefendants(), defendantId, masterDefendantId, userId, defendantBuilder, hearingsList, caseDefendant, hearingsAtAGlance));

        courtExtract.withPublishingCourt(transformCourtCentre(latestHearing, userId, defendantId));

        courtExtract.withProsecutingAuthority(transformationHelper.transformProsecutingAuthority(hearingsAtAGlance.getProsecutionCaseIdentifier(), userId));

        courtExtract.withCompanyRepresentatives(transformCompanyRepresentatives(hearingsList));

        courtExtract.withReferralReason(getReferralReason(hearingsList));
    }

    private List<CourtApplications> transformCourtApplications(final List<CourtApplication> caseCourtApplications, final Hearings hearings) {
        final List<CourtApplication> applicationsExtractList = new ArrayList<>();

        caseCourtApplications.forEach(app -> getResultedApplication(app.getId(), hearings).ifPresent(resultedApplication -> {
            applicationsExtractList.add(mergeApplicationResults(app, resultedApplication));
        }));

        return applicationsExtractList.stream()
                .map(ca -> CourtApplications.courtApplications()
                        .withResults(ca.getJudicialResults())
                        .withRepresentation(transformRepresentation(ca, hearings))
                        .withApplicationType(ca.getType().getType())
                        .withApplicationDate(ca.getApplicationReceivedDate())
                        .withApplicationParticulars(ca.getApplicationParticulars())
                        .withPlea(ca.getPlea())
                        .withCourtOrders(transformCourtOrders(ca.getCourtOrder()))
                        .build()).toList();
    }

    private CourtOrders transformCourtOrders(final CourtOrder courtOrder) {
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
            courtOrdersBuilder.withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                    .map(coo -> CourtOrderOffences.courtOrderOffences()
                            .withId(coo.getOffence().getId())
                            .withStartDate(coo.getOffence().getStartDate())
                            .withEndDate(coo.getOffence().getEndDate())
                            .withOffenceCode(coo.getOffence().getOffenceCode())
                            .withOffenceTitle(coo.getOffence().getOffenceTitle())
                            .withOffenceTitleWelsh(coo.getOffence().getOffenceTitleWelsh())
                            .withWording(coo.getOffence().getWording())
                            .withWordingWelsh(coo.getOffence().getWordingWelsh())
                            .withOffenceDefinitionId(coo.getOffence().getOffenceDefinitionId())
                            .withResultTextList(nonNull(coo.getOffence().getJudicialResults()) && !coo.getOffence().getJudicialResults().isEmpty() ?
                                    coo.getOffence().getJudicialResults().stream().map(JudicialResult::getResultText).filter(StringUtils::isNotEmpty).toList() : emptyList())
                            .withPlea(coo.getOffence().getPlea())
                            .build())
                    .toList()
            );
        }
        return courtOrdersBuilder.build();
    }

    private Optional<uk.gov.justice.progression.courts.CourtApplications> getResultedApplication(final UUID applicationId, final Hearings hearings) {
        if (nonNull(hearings) && nonNull(hearings.getDefendants())) {
            return hearings.getDefendants().stream()
                    .filter(d -> nonNull(d.getCourtApplications()))
                    .flatMap(d -> d.getCourtApplications().stream())
                    .filter(Objects::nonNull)
                    .filter(ra -> applicationId.toString().equals(ra.getApplicationId().toString()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private CourtApplication mergeApplicationResults(final CourtApplication courtApplication, final uk.gov.justice.progression.courts.CourtApplications resultedApplication) {
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
                .withCourtOrder(courtApplication.getCourtOrder())
                .build());
    }

    private List<CourtApplicationParty> updateResponse(final List<CourtApplicationParty> courtApplicationRespondents, final uk.gov.justice.progression.courts.CourtApplications resultedApplication) {
        final List<CourtApplicationParty> updatedResponseList = new ArrayList<>();
        if (isNotEmpty(courtApplicationRespondents)) {
            updatedResponseList.addAll(courtApplicationRespondents);
        }
        if (nonNull(resultedApplication) && isNotEmpty(resultedApplication.getRespondents())) {
            updatedResponseList.add(CourtApplicationParty.courtApplicationParty().build());
        }
        return updatedResponseList;
    }

    private Representation transformRepresentation(final CourtApplication ca, final Hearings hearings) {
        return Representation.representation()
                .withApplicantRepresentation(transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), hearings))
                .withRespondentRepresentation(ca.getRespondents() != null ? transformRespondentRepresentations(ca.getRespondents(), hearings) : null)
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

    private List<RespondentRepresentation> transformRespondentRepresentations(final List<CourtApplicationParty> respondents, final Hearings hearings) {
        final List<RespondentRepresentation> respondentRepresentations = new ArrayList<>();

        respondents.forEach(r -> respondentRepresentations.add(transformRespondentRepresentation(r, hearings)));
        return respondentRepresentations;
    }

    private RespondentRepresentation transformRespondentRepresentation(final CourtApplicationParty courtApplicationParty, final Hearings hearings) {
        final Organisation representationOrganisation = nonNull(courtApplicationParty) ? courtApplicationParty.getRepresentationOrganisation() : null;
        return RespondentRepresentation.respondentRepresentation()
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withName(nonNull(representationOrganisation) ? representationOrganisation.getName() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withRespondentCounsels(nonNull(hearings) ? hearings.getRespondentCounsels() : null)
                .build();
    }

    private ApplicantRepresentation transformApplicantRepresentation(final Organisation representationOrganisation, final Hearings hearings) {
        final String orgName = nonNull(representationOrganisation) ? representationOrganisation.getName() : null;
        return ApplicantRepresentation.applicantRepresentation()
                .withName(orgName)
                .withAddress(nonNull(representationOrganisation) ? representationOrganisation.getAddress() : null)
                .withContact(nonNull(representationOrganisation) ? representationOrganisation.getContact() : null)
                .withApplicantCounsels(nonNull(hearings) ? hearings.getApplicantCounsels() : null)
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

    private Defendant transformDefendants(final List<Defendants> defendantsList, final UUID defendantId, final UUID masterDefendantId, final UUID userId, final Defendant.Builder defendantBuilder,
                                          final List<Hearings> hearingsList, final uk.gov.justice.core.courts.Defendant caseDefendant, GetHearingsAtAGlance hearingsAtAGlance) {

        final Optional<Defendants> defendants = defendantsList.stream().filter(d -> d.getId().equals(defendantId)).findFirst();
        if (defendants.isPresent()) {
            final Defendants defendant = defendants.get();
            defendantBuilder.withDateOfBirth(defendant.getDateOfBirth());
            defendantBuilder.withAge(defendant.getAge());
            defendantBuilder.withLegalAidStatus(defendant.getLegalAidStatus());
            defendantBuilder.withAddress(toAddress(defendant.getAddress()));

            final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings = hearingsList.stream()
                    .map(h -> getExtractHearing(defendantId, userId, h, hearingsAtAGlance))
                    .toList();
            defendantBuilder.withHearings(getHearingsSortedByHearingDaysAsc(extractHearings));
            defendantBuilder.withAttendanceDays(transformAttendanceDayAndTypes(transformDefendantAttendanceDay(hearingsList, defendant)));
            defendantBuilder.withResults(transformJudicialResults(hearingsList, masterDefendantId, defendantId));

            if (nonNull(caseDefendant.getAssociatedDefenceOrganisation())) {
                final List<AssociatedDefenceOrganisation> associatedOrganisations = defenceQueryService.getAllAssociatedOrganisations(getDefenceQueryJsonEnvelop(userId), defendantId.toString());
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

            if (nonNull(caseDefendant.getPersonDefendant()) && nonNull(caseDefendant.getPersonDefendant().getArrestSummonsNumber())) {
                defendantBuilder.withArrestSummonsNumber(caseDefendant.getPersonDefendant().getArrestSummonsNumber());
            }
        }

        return defendantBuilder.build();
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

    private List<JudicialResult> transformJudicialResults(final List<Hearings> hearingsList, final UUID masterDefendantId, final UUID defendantId) {
        final List<JudicialResult> judicialResultsList = new ArrayList<>();
        final List<JudicialResult> defendantLevelJudicialResults = hearingsList.stream()
                .map(Hearings::getDefendantJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(defendantJudicialResult -> masterDefendantId.equals(defendantJudicialResult.getMasterDefendantId()))
                .map(DefendantJudicialResult::getJudicialResult)
                .filter(Objects::nonNull)
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .toList();
        judicialResultsList.addAll(defendantLevelJudicialResults);

        final List<JudicialResult> caseLevelJudicialResults = hearingsList.stream().map(Hearings::getDefendants)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(defendants -> defendantId.equals(defendants.getId()))
                .map(Defendants::getJudicialResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract())
                .toList();

        judicialResultsList.addAll(caseLevelJudicialResults);

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

    private List<uk.gov.justice.progression.courts.exract.Offences> transformOffence(final Hearings hearings, final UUID defendantId, final UUID userId, final List<Hearings> hearingsList) {
        final List<uk.gov.justice.progression.courts.exract.Offences> offences = new ArrayList<>();

        final List<Offences> defendantOffences = hearings.getDefendants().stream().filter(Objects::nonNull)
                .filter(d -> d.getId().equals(defendantId))
                .filter(d -> nonNull(d.getOffences()))
                .map(Defendants::getOffences)
                .flatMap(Collection::stream)
                .toList();

        if (isNotEmpty(defendantOffences)) {
            LOGGER.info("Defendant {} aggregated offences: {}", defendantId, defendantOffences.size());
            //getOffence from Mags if sentToCC By Mags resulted with 'Committed for sentence'
            final Hearing hearingFromListing = listingQueryService.searchHearing(getListingQueryJsonEnvelop(userId), hearings.getId());
            final List<Offences> offencesFromSeedingHearings = courtExtractHelper.getOffencesFromSeedingHearings(defendantId, hearingFromListing, hearingsList);
            final List<ResultDefinition> filteredResultDefinitions = getResultDefinitionsCommittedToCCOrSentToCC(userId, getResultDefinitionIds(offencesFromSeedingHearings));

            final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap = courtExtractHelper.getOffencesResultedWithCommittedForSentence(defendantId, hearingFromListing, hearingsList, filteredResultDefinitions);
            offences.addAll(transformOffence(defendantOffences, hearings.getId(), offenceCommittedForSentenceMap, hearings.getJurisdictionType()));
        }
        return offences;
    }

    private List<ResultDefinition> getResultDefinitionsCommittedToCCOrSentToCC(final UUID userId, final List<UUID> uuids) {
        if (isEmpty(uuids)) {
            return emptyList();
        }
        return referenceDataService.getResultDefinitionsByIds(getReferenceDataQueryJsonEnvelop(userId), uuids)
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

    private List<AttendanceDayAndType> transformDefendantAttendanceDay(final List<Hearings> hearingsList, final Defendants defendant) {

        return hearingsList.stream()
                .filter(h -> nonNull(h.getDefendantAttendance()))
                .flatMap((h -> h.getDefendantAttendance().stream()))
                .filter(Objects::nonNull)
                .filter(da -> da.getDefendantId().toString().equals(defendant.getId().toString()))
                .map(DefendantAttendance::getAttendanceDays)
                .flatMap(Collection::stream)
                .filter(ad -> !ad.getAttendanceType().equals(AttendanceType.NOT_PRESENT))
                .map(ad -> AttendanceDayAndType.attendanceDayAndType()
                        .withDay(ad.getDay())
                        .withAttendanceType(ad.getAttendanceType().toString().equals(AttendanceType.IN_PERSON.toString()) ? PRESENT_IN_PERSON : extractAttendanceType(defendant)).build())
                .toList();

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
                                                                                       final Map<UUID, CommittedForSentence> offenceCommittedForSentenceMap, final JurisdictionType jurisdictionType) {

        final Comparator<? super uk.gov.justice.progression.courts.exract.Offences> offencesSorted = jurisdictionType == JurisdictionType.CROWN
                ? crownOffencesSortComparator : magsOffencesSortComparator;

        return offences.stream()
                .map(o -> {
                    final List<JudicialResult> resultList = transformResults(filterOutResultDefinitionsNotToBeShownInCourtExtract(o, hearingId));
                    return toOffences(o, resultList, offenceCommittedForSentenceMap.get(o.getId()));
                })
                .sorted(offencesSorted)
                .toList();
    }

    private List<JudicialResult> filterOutResultDefinitionsNotToBeShownInCourtExtract(final Offences o, final UUID hearingId) {
        return isNotEmpty(o.getJudicialResults()) ?
                o.getJudicialResults().stream()
                        .filter(jr -> nonNull(jr.getIsAvailableForCourtExtract()) && jr.getIsAvailableForCourtExtract() &&
                                hearingId.equals(jr.getOrderedHearingId()))
                        .toList()
                : o.getJudicialResults();
    }

    private List<JudicialResult> transformResults(final List<JudicialResult> judicialResults) {
        if (isNotEmpty(judicialResults)) {
            return judicialResults.stream()
                    .map(JudicialResultTransformer::toCourtJudicialResult)
                    .toList();
        }
        return judicialResults;
    }

    private uk.gov.justice.progression.courts.exract.Hearings getExtractHearing(final UUID defendantId, final UUID userId, final Hearings hearing, final GetHearingsAtAGlance hearingsAtAGlance) {
        final uk.gov.justice.progression.courts.exract.Hearings.Builder hearingBuilder = uk.gov.justice.progression.courts.exract.Hearings.hearings()
                .withHearingDays(transformationHelper.transformHearingDays(hearing.getHearingDays()))
                .withId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType() != null ? JurisdictionType.valueOf(hearing.getJurisdictionType().toString()) : null)
                .withCourtCentre(transformCourtCenter(hearing, defendantId))
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withType(hearing.getType().getDescription());

        if (isNotEmpty(hearingsAtAGlance.getCourtApplications())) {
            hearingBuilder.withCourtApplications(transformCourtApplications(hearingsAtAGlance.getCourtApplications(), hearing));
        }

        if (nonNull(hearing.getProsecutionCounsels())) {
            hearingBuilder.withProsecutionCounsels(transformProsecutionCounsel(hearing.getProsecutionCounsels()));
        }

        List<DefenceCounsels> defenceCounselList = getDefendantDefenceCounsels(hearing, defendantId);
        if (nonNull(defenceCounselList) && isNotEmpty(defenceCounselList)) {
            hearingBuilder.withDefenceCounsels(defenceCounselList);
        }

        if (hearing.getJudiciary() != null) {
            final CrownCourtDecisions crownCourtDecisions = getCourtDecisions(hearing.getJudiciary(), hearing.getHearingDays());
            hearingBuilder.withCrownCourtDecisions(crownCourtDecisions);
        }

        final List<uk.gov.justice.progression.courts.exract.Offences> offences = transformOffence(hearing, defendantId, userId, hearingsAtAGlance.getHearings());
        if (!offences.isEmpty()) {
            hearingBuilder.withOffences(offences);
            hearingBuilder.withAuthorisedLegalAdvisors(courtExtractHelper.getAuthorisedLegalAdvisors(offences));
        }

        return hearingBuilder.build();
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

    private JsonEnvelope getListingQueryJsonEnvelop(final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(LISTING_SEARCH_HEARING)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }

    private JsonEnvelope getReferenceDataQueryJsonEnvelop(final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }

    private JsonEnvelope getDefenceQueryJsonEnvelop(final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }
}
