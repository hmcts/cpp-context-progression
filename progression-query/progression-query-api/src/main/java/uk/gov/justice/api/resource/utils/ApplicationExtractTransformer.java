package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.progression.courts.exract.Address;
import uk.gov.justice.progression.courts.exract.Applicant;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.ApplicationCourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtDecisions;
import uk.gov.justice.progression.courts.exract.HearingDays;
import uk.gov.justice.progression.courts.exract.Hearings;
import uk.gov.justice.progression.courts.exract.Judiciary;
import uk.gov.justice.progression.courts.exract.Representation;
import uk.gov.justice.progression.courts.exract.Respondent;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S1067", "pmd:NullAssignment", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1172"})
public class ApplicationExtractTransformer {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private TransformationHelper transformationHelper;

    private static final BiPredicate<Hearing, UUID> hearingsDefendantIdBiPredicate = (hearings, defendantId) ->  nonNull(hearings.getYouthCourtDefendantIds()) &&  hearings.getYouthCourtDefendantIds().stream().anyMatch(youthDefendantId -> youthDefendantId.equals(defendantId));


    public ApplicationCourtExtractRequested getApplicationCourtExtractRequested(final CourtApplication courtApplication,
                                                                                final List<Hearing> hearingsForApplication,
                                                                                final String extractType, final UUID userId) {
        return buildApplicationExtractRequested(courtApplication, hearingsForApplication, extractType, userId);
    }

    public List<Hearing> getHearingsForApplication(final JsonArray hearings, final List<String> hearingIds) {
        return Objects.isNull(hearings) || hearings.isEmpty() || isEmpty(hearingIds) ? emptyList() : hearings.getValuesAs(JsonObject.class).stream()
                .map(hearing -> jsonObjectToObjectConverter.convert(hearing, Hearing.class))
                .filter(hearing -> hearingIds.contains(hearing.getId().toString()))
                .collect(toList());
    }


    private ApplicationCourtExtractRequested buildApplicationExtractRequested(final CourtApplication courtApplication,
                                                                              final List<Hearing> hearings,
                                                                              final String extractType, final UUID userId){
        return ApplicationCourtExtractRequested.applicationCourtExtractRequested()
                .withExtractType(extractType)
                .withApplicant(getApplicant(courtApplication))
                .withRespondent(getRespondents(courtApplication))
                .withPublishingCourt(hearings.isEmpty() ? null : getPublishingCourt(hearings, userId, courtApplication.getApplicant()))
                .withCourtDecisions(hearings.isEmpty() ? null : getCourtDecisions(hearings))
                .withReference(courtApplication.getApplicationReference())
                .withHearings(getHearings(hearings, courtApplication.getApplicant()))
                .withCourtApplications(getCourtApplications(hearings, courtApplication))
                .withIsAppealPending((nonNull(courtApplication.getType().getAppealFlag()) && courtApplication.getType().getAppealFlag()) &&
                        (courtApplication.getApplicationStatus().equals(ApplicationStatus.DRAFT) || courtApplication.getApplicationStatus().equals(ApplicationStatus.LISTED))
                        || courtApplication.getApplicationStatus().equals(ApplicationStatus.EJECTED))
                .build();
    }

    private List<CourtApplications> getCourtApplications(final List<Hearing> hearings, final CourtApplication courtApplication) {
        if (isNotEmpty(hearings)) {
            return hearings.stream().flatMap(hearing -> hearing.getCourtApplications().stream())
                    .filter(Objects::nonNull).map(ca -> mapCourtApplication(ca, hearings)).collect(toList());
        } else {
            // Only application has been added and no hearings have been allocated
            return Arrays.asList(mapCourtApplication(courtApplication, emptyList()));
        }
    }

    private CourtApplications mapCourtApplication(final CourtApplication courtApplication, final List<Hearing> hearings) {
        return CourtApplications.courtApplications()
                .withApplicationType(courtApplication.getType().getType())
                .withResults(courtApplication.getJudicialResults())
                .withRepresentation(transformRepresentation(courtApplication, hearings))
                .build();
    }

    private List<Hearings> getHearings(final List<Hearing> hearings, final CourtApplicationParty applicant) {
        return hearings.stream().map(hearing-> mapHearing(hearing, applicant)).collect(toList());
    }

    private Hearings mapHearing(final Hearing hearing, final CourtApplicationParty applicant) {
        return Hearings.hearings()
                .withCourtCentre(getCourtCentre(hearing, applicant))
                .withId(hearing.getId())
                .withJurisdictionType(nonNull(hearing.getJurisdictionType())
                        ? JurisdictionType.valueFor(hearing.getJurisdictionType().toString()).orElse(null) : null)
                .withReportingRestrictionReason(hearing.getReportingRestrictionReason())
                .withType(nonNull(hearing.getType()) ? hearing.getType().getDescription() : null)
                .withHearingDays(getHearingDays(hearing.getHearingDays()))
                .build();
    }

    private List<HearingDays> getHearingDays(final List<HearingDay> hearingDays) {
        final List<LocalDate> hearingDaysList = hearingDays.stream().map(hearingDay -> hearingDay.getSittingDay().toLocalDate())
                .sorted(Comparator.comparing(d -> d))
                .collect(toList());
        if (hearingDaysList.size() > 1) {
            return Arrays.asList(hearingDaysList.get(0), hearingDaysList.get(hearingDaysList.size() - 1)).stream().map(this::getHearingDay)
                    .collect(toList());
        }
        return hearingDaysList.stream()
                .map(this::getHearingDay)
                .collect(toList());
    }

    private HearingDays getHearingDay(final LocalDate hearingDate) {
        return HearingDays.hearingDays()
                .withDay(hearingDate)
                .build();
    }

    private List<CourtDecisions> getCourtDecisions(final List<Hearing> hearings) {
        final List<CourtDecisions> courtDecisions = new ArrayList<>();
        hearings.forEach(h -> {
            if (h.getJudiciary() != null) {
                final CourtDecisions courtDecision = CourtDecisions.courtDecisions()
                        .withDates(transformationHelper.transformDates(h.getHearingDays(), true))
                        .withJudiciary(
                                h.getJudiciary().stream().map(j ->
                                        Judiciary.judiciary()
                                                .withIsDeputy(j.getIsDeputy())
                                                .withRole(j.getJudicialRoleType() != null ? j.getJudicialRoleType().toString() : null)
                                                .withName(transformationHelper.getName(j.getFirstName(), j.getMiddleName(), j.getLastName()))
                                                .withIsBenchChairman(j.getIsBenchChairman())
                                                .build()
                                ).collect(toList()))
                        .withJudicialDisplayName(transformationHelper.transformJudicialDisplayName(h.getJudiciary()))
                        .withRoleDisplayName((!h.getJudiciary().isEmpty() && h.getJudiciary().get(0).getJudicialRoleType() != null) ?
                                transformationHelper.getCamelCase(h.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()) : null)
                        .build();
                courtDecisions.add(courtDecision);
            }
        });
        return courtDecisions;
    }

    private CourtCentre getPublishingCourt(final List<Hearing> hearings, final UUID userId, final CourtApplicationParty applicant) {
        final Hearing latestHearing = transformationHelper.getLatestHearing(hearings);
        final uk.gov.justice.core.courts.Address address = transformationHelper.getCourtAddress(userId, latestHearing.getCourtCentre().getId());
        if(nonNull(applicant.getMasterDefendant()) && hearingsDefendantIdBiPredicate.test(latestHearing, applicant.getMasterDefendant().getMasterDefendantId())) {
            return CourtCentre.courtCentre()
                    .withName(latestHearing.getYouthCourt().getName())
                    .withWelshName(latestHearing.getYouthCourt().getWelshName())
                    .withAddress(getAddress(address))
                    .build();
        } else {
            return CourtCentre.courtCentre()
                    .withName(latestHearing.getCourtCentre().getName())
                    .withAddress(getAddress(address))
                    .withWelshName(latestHearing.getCourtCentre().getWelshName())
                    .build();
        }
    }

    private uk.gov.justice.progression.courts.exract.CourtCentre getCourtCentre(final Hearing hearing, final CourtApplicationParty applicant) {

        if(nonNull(applicant.getMasterDefendant()) && hearingsDefendantIdBiPredicate.test(hearing, applicant.getMasterDefendant().getMasterDefendantId())) {
            return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                    .withId(hearing.getYouthCourt().getYouthCourtId())
                    .withName(hearing.getYouthCourt().getName())
                    .withWelshName(hearing.getYouthCourt().getWelshName())
                    .build();
        } else {
            return uk.gov.justice.progression.courts.exract.CourtCentre.courtCentre()
                    .withId(hearing.getCourtCentre().getId())
                    .withName(hearing.getCourtCentre().getName())
                    .withWelshName(hearing.getCourtCentre().getWelshName())
                    .build();
        }
    }

    private List<Respondent> getRespondents(final CourtApplication courtApplication) {
        return nonNull(courtApplication.getRespondents()) ? courtApplication.getRespondents().stream()
                .map(this::getRespondent)
                .collect(toList()): emptyList();
    }
    private Respondent getRespondent(final CourtApplicationParty partyDetails){
        return Respondent.respondent()
                .withName(getName(partyDetails.getPersonDetails(), partyDetails.getOrganisation()))
                .withAddress(getAddress(partyDetails.getPersonDetails(), partyDetails.getOrganisation()))
                .build();
    }
    private Applicant getApplicant(CourtApplication courtApplication) {
        return Applicant.applicant()
                .withName(getName(courtApplication.getApplicant().getPersonDetails(), courtApplication.getApplicant().getOrganisation()))
                .withAddress(getAddress(courtApplication.getApplicant().getPersonDetails(), courtApplication.getApplicant().getOrganisation()))
                .build();
    }

    private Address getAddress(final Person personDetails, final Organisation organisation) {
        return nonNull(personDetails) && nonNull(personDetails.getAddress()) ?
                transformationHelper.getAddress(personDetails.getAddress().getAddress1(),
                        personDetails.getAddress().getAddress2(),
                        personDetails.getAddress().getAddress3(),
                        personDetails.getAddress().getAddress4(),
                        personDetails.getAddress().getAddress5(),
                        personDetails.getAddress().getPostcode()
                ) :
                getOrganisationAddress(organisation);
    }

    private Address getOrganisationAddress(Organisation organisation) {
        return nonNull(organisation) && nonNull(organisation.getAddress()) ?
                transformationHelper.getAddress(organisation.getAddress().getAddress1(),
                        organisation.getAddress().getAddress2(),
                        organisation.getAddress().getAddress3(),
                        organisation.getAddress().getAddress4(),
                        organisation.getAddress().getAddress5(),
                        organisation.getAddress().getPostcode())
                : null;
    }

    private String getName(final Person personDetails, final Organisation organisation) {
        return nonNull(personDetails) ? transformationHelper.getName(personDetails.getFirstName(), personDetails.getMiddleName(), personDetails.getLastName()) :
                getOrganisationName(organisation);
    }

    private String getOrganisationName(Organisation organisation) {
        return nonNull(organisation) ? organisation.getName() : null;
    }

    private Representation transformRepresentation(final CourtApplication ca, final List<Hearing> hearingsList) {
        if (nonNull(ca.getApplicant().getRepresentationOrganisation()) || nonNull(ca.getRespondents())) {
            return Representation.representation()
                    .withApplicantRepresentation(nonNull(ca.getApplicant().getRepresentationOrganisation()) ?
                            transformApplicantRepresentation(ca.getApplicant().getRepresentationOrganisation(), hearingsList) : null)
                    .withRespondentRepresentation(nonNull(ca.getRespondents()) ?
                            transformRespondentRepresentations(ca.getRespondents(), hearingsList) : null)
                    .build();
        }
        return null;
    }


    private List<RespondentRepresentation> transformRespondentRepresentations(final List<CourtApplicationParty> respondents, final List<Hearing> hearingsList) {
        return respondents.stream().filter(r -> nonNull(r) && nonNull(r.getRepresentationOrganisation()))
                .map(r -> transformRespondentRepresentation(r.getRepresentationOrganisation(), hearingsList)).collect(toList());

    }

    private RespondentRepresentation transformRespondentRepresentation(final Organisation representationOrganisation, final List<Hearing> hearingsList) {
        return RespondentRepresentation.respondentRepresentation()
                .withAddress(getAddress(representationOrganisation.getAddress()))
                .withName(representationOrganisation.getName())
                .withContact(representationOrganisation.getContact())
                .withRespondentCounsels(hearingsList.stream()
                        .filter(hearings -> nonNull(hearings.getRespondentCounsels()))
                        .flatMap(hearings -> hearings.getRespondentCounsels().stream())
                        .collect(Collectors.toList()))
                .build();
    }

    private ApplicantRepresentation transformApplicantRepresentation(final Organisation representationOrganisation, final List<Hearing> hearingsList) {
        return ApplicantRepresentation.applicantRepresentation()
                .withAddress(getAddress(representationOrganisation.getAddress()))
                .withContact(representationOrganisation.getContact())
                .withName(representationOrganisation.getName())
                //.withSynonym(synonym)
                .withApplicantCounsels(hearingsList.stream()
                        .filter(hearings -> nonNull(hearings.getApplicantCounsels()))
                        .flatMap(hearings -> hearings.getApplicantCounsels().stream())
                        .collect(Collectors.toList()))
                .build();
    }

    private uk.gov.justice.core.courts.Address getAddress(final uk.gov.justice.core.courts.Address address) {
        return uk.gov.justice.core.courts.Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(isNotEmpty(address.getAddress3())
                        && isNotEmpty(address.getAddress2()) ? address.getAddress2()
                        + SPACE + address.getAddress3() : address.getAddress2())
                .withAddress3(isNotEmpty(address.getAddress5())
                        && isNotEmpty(address.getAddress4()) ? address.getAddress4()
                        + SPACE + address.getAddress5() : address.getAddress4())
                .withPostcode(address.getPostcode())
                .build();
    }
}
