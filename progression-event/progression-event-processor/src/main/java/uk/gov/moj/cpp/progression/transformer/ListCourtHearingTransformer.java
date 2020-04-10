package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.core.courts.SummonsRequired;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1188", "squid:S2259"})
public class ListCourtHearingTransformer {


    private static final Logger LOGGER = LoggerFactory.getLogger(ListCourtHearingTransformer.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ReferenceDataService referenceDataService;

    /**
     * earliestDate = add 28 days to the notice date earliest lead date = add 14 days to the
     * referral date Earliest court date is earliest date or earliest lead date, whichever is the
     * latest
     */
    public static LocalDate calculateEarliestHearingDate(final LocalDate noticeDate, final LocalDate referralDate) {

        final LocalDate earliestDate = noticeDate.plusDays(28);
        final LocalDate earliestLeadDate = referralDate.plusDays(14);

        if (earliestDate.isAfter(earliestLeadDate)) {
            return earliestDate;
        }
        return earliestLeadDate;
    }

    @SuppressWarnings("squid:S1188")
    private static List<ProsecutionCase> filterProsecutionCases(final List<ProsecutionCase> prosecutionCases, final
    List<ListDefendantRequest> listDefendantRequests) {
        final Map<UUID, List<Defendant>> mapOfProsecutionCaseIdWithDefendants = new HashMap<>();
        listDefendantRequests.stream().forEach(listDefendantRequest -> {

            final ProsecutionCase matchedProsecutionCase = prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId()
                            .equals(listDefendantRequest.getProsecutionCaseId())).findFirst().orElseThrow(() -> new DataValidationException("Matching caseId missing in referral"));

            final Defendant matchedDefendant = matchedProsecutionCase.getDefendants().stream()
                    .filter(defendant -> listDefendantRequest.getReferralReason().getDefendantId()
                            .equals(defendant.getId())).findFirst().orElseThrow(() -> new DataValidationException("Matching defendant missing in referral"));

            final List<Offence> matchedDefendantOffence = matchedDefendant.getOffences().stream()
                    .filter(offence -> listDefendantRequest.getDefendantOffences()
                            .contains(offence.getId())).collect(Collectors.toList());

            final Defendant defendant = getCopyOfDefendant(matchedDefendant, matchedDefendantOffence);

            if (mapOfProsecutionCaseIdWithDefendants.containsKey(listDefendantRequest.getProsecutionCaseId())) {
                mapOfProsecutionCaseIdWithDefendants.get(listDefendantRequest.getProsecutionCaseId()).add(defendant);
            } else {
                final List<Defendant> defendants = new ArrayList<>();
                defendants.add(defendant);
                mapOfProsecutionCaseIdWithDefendants.put(listDefendantRequest.getProsecutionCaseId(), defendants);
            }

        });

        final List<ProsecutionCase> listOfProsecutionCase = new ArrayList<>();
        mapOfProsecutionCaseIdWithDefendants.keySet().stream().forEach(prosecutionCaseId -> {
            final ProsecutionCase matchedProsecutionCase = prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId()
                            .equals(prosecutionCaseId)).findFirst().orElseThrow(() -> new DataValidationException("Matching caseId missing in referral"));

            listOfProsecutionCase.add(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId)
                    .withCaseStatus(matchedProsecutionCase.getCaseStatus())
                    .withDefendants(mapOfProsecutionCaseIdWithDefendants.get(prosecutionCaseId))
                    .withInitiationCode(matchedProsecutionCase.getInitiationCode())
                    .withOriginatingOrganisation(matchedProsecutionCase.getOriginatingOrganisation())
                    .withProsecutionCaseIdentifier(matchedProsecutionCase.getProsecutionCaseIdentifier())
                    .withStatementOfFacts(matchedProsecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(matchedProsecutionCase.getStatementOfFactsWelsh())
                    .build());

        });
        return listOfProsecutionCase;
    }

    private static Defendant getCopyOfDefendant(final Defendant matchedDefendant, final List<Offence>
            matchedDefendantOffence) {
        return Defendant.defendant()
                .withId(matchedDefendant.getId())
                .withMasterDefendantId(matchedDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(matchedDefendant.getCourtProceedingsInitiated())
                .withOffences(matchedDefendantOffence)
                .withAssociatedPersons(matchedDefendant.getAssociatedPersons())
                .withDefenceOrganisation(matchedDefendant.getDefenceOrganisation())
                .withLegalEntityDefendant(matchedDefendant.getLegalEntityDefendant())
                .withMitigation(matchedDefendant.getMitigation())
                .withMitigationWelsh(matchedDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(matchedDefendant.getNumberOfPreviousConvictionsCited())
                .withPersonDefendant(matchedDefendant.getPersonDefendant())
                .withProsecutionAuthorityReference(matchedDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(matchedDefendant.getProsecutionCaseId())
                .withWitnessStatement(matchedDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(matchedDefendant.getWitnessStatementWelsh())
                .build();
    }

    @SuppressWarnings("squid:S1188")
    private static List<ProsecutionCase> filterProsecutionCasesFromSpi(final List<ProsecutionCase> prosecutionCases, final
    List<ListDefendantRequest> listDefendantRequests) {
        final Map<UUID, List<Defendant>> mapOfProsecutionCaseIdWithDefendants = new HashMap<>();
        listDefendantRequests.stream().forEach(listDefendantRequest -> {

            final ProsecutionCase matchedProsecutionCase = prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId()
                            .equals(listDefendantRequest.getProsecutionCaseId())).findFirst().orElseThrow(() -> new DataValidationException("Matching caseId missing in SPI initiate court proceedings"));

            final Defendant matchedDefendant = matchedProsecutionCase.getDefendants().stream()
                    .filter(defendant -> defendant.getId().equals(listDefendantRequest.getDefendantId()))
                    .findFirst().orElseThrow(() -> new DataValidationException("Matching defendant missing in SPI initiate court proceedings"));

            final List<Offence> matchedDefendantOffence = matchedDefendant.getOffences().stream()
                    .filter(offence -> listDefendantRequest.getDefendantOffences()
                            .contains(offence.getId())).collect(Collectors.toList());

            final Defendant defendant = getCopyOfDefendant(matchedDefendant, matchedDefendantOffence);

            if (mapOfProsecutionCaseIdWithDefendants.containsKey(listDefendantRequest.getProsecutionCaseId())) {
                mapOfProsecutionCaseIdWithDefendants.get(listDefendantRequest.getProsecutionCaseId()).add(defendant);
            } else {
                final List<Defendant> defendants = new ArrayList<>();
                defendants.add(defendant);
                mapOfProsecutionCaseIdWithDefendants.put(listDefendantRequest.getProsecutionCaseId(), defendants);
            }

        });

        final List<ProsecutionCase> listOfProsecutionCase = new ArrayList<>();
        mapOfProsecutionCaseIdWithDefendants.keySet().stream().forEach(prosecutionCaseId -> {
            final ProsecutionCase matchedProsecutionCase = prosecutionCases.stream()
                    .filter(prosecutionCase -> prosecutionCase.getId()
                            .equals(prosecutionCaseId)).findFirst().orElseThrow(() -> new DataValidationException("Matching caseId missing in SPI initiate court proceedings"));

            listOfProsecutionCase.add(ProsecutionCase.prosecutionCase().withId(prosecutionCaseId)
                    .withCaseStatus(matchedProsecutionCase.getCaseStatus())
                    .withDefendants(mapOfProsecutionCaseIdWithDefendants.get(prosecutionCaseId))
                    .withInitiationCode(matchedProsecutionCase.getInitiationCode())
                    .withOriginatingOrganisation(matchedProsecutionCase.getOriginatingOrganisation())
                    .withProsecutionCaseIdentifier(matchedProsecutionCase.getProsecutionCaseIdentifier())
                    .withStatementOfFacts(matchedProsecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(matchedProsecutionCase.getStatementOfFactsWelsh())
                    .withCaseMarkers(matchedProsecutionCase.getCaseMarkers())
                    .build());

        });
        return listOfProsecutionCase;
    }

    private static LocalDate getNoticeDate(final SjpReferral sjpReferral) {
        return sjpReferral.getNoticeDate();
    }

    private static LocalDate getReferralDate(final SjpReferral sjpReferral) {
        return sjpReferral.getReferralDate();
    }

    private static String getProsecutionAuthorityCode(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
    }

    private static String getDefendantPostcode(final List<ProsecutionCase> prosecutionCases) {
        final Optional<Defendant> defendant = Optional.ofNullable(prosecutionCases.get(0).getDefendants().get(0));
        final Optional<PersonDefendant> personDefendant;
        final Optional<LegalEntityDefendant> legalEntityDefendant;

        if (defendant.isPresent()) {
            personDefendant = Optional.ofNullable(defendant.get().getPersonDefendant());
            legalEntityDefendant = Optional.ofNullable(defendant.get().getLegalEntityDefendant());
            if (personDefendant.isPresent()) {
                return getRequiredField(getRequiredField(personDefendant.get().getPersonDetails().getAddress(), "Address is missing for personDefendant")
                        .getPostcode(), "Postcode is missing for personDefendant").split(" ")[0];
            } else if (legalEntityDefendant.isPresent()) {
                return getRequiredField(getRequiredField(legalEntityDefendant.get().getOrganisation().getAddress(), "Address is missing for legalEntityDefendant")
                        .getPostcode(), "Postcode is missing for legalEntityDefendant").split(" ")[0];
            }
        }
        throw new MissingRequiredFieldException("Defendant is missing");
    }

    private static <T> T getRequiredField(final T field, final String message) {
        if (isNull(field)) {
            throw new MissingRequiredFieldException(message);
        }
        return field;
    }

    /**
     * Transform a CourtReferral to ListCourtHearing
     *
     * @return ListCourtHearing
     */
    public ListCourtHearing transform(final JsonEnvelope jsonEnvelope,
                                      final List<ProsecutionCase> prosecutionCases,
                                      final SjpReferral sjpReferral,
                                      final List<ReferredListHearingRequest> referredListHearingRequests,
                                      final UUID hearingId) {
        LOGGER.info("Preparing send case for listing ");
        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        referredListHearingRequests.stream()
                .forEach(referredListHearingRequest -> {

                    final List<ProsecutionCase> listOfProsecutionCase = filterProsecutionCases(prosecutionCases, referredListHearingRequest.getListDefendantRequests());

                    final JsonObject jsonObject = referenceDataService
                            .getHearingType(jsonEnvelope, referredListHearingRequest.getHearingType().getId(), requester)
                            .orElseThrow(() -> new ReferenceDataNotFoundException("Hearing Type", referredListHearingRequest.getHearingType().getId().toString()));
                    final HearingType hearingType = HearingType.hearingType()
                            .withId(referredListHearingRequest.getHearingType().getId())
                            .withDescription(jsonObject.getString("hearingDescription"))
                            .build();
                    final HearingListingNeeds hearings = HearingListingNeeds.hearingListingNeeds()
                            .withEarliestStartDateTime(calculateEarliestHearingDate(getNoticeDate(sjpReferral), getReferralDate(sjpReferral)).atStartOfDay(ZoneId.systemDefault()))
                            .withEstimatedMinutes(getRequiredField(referredListHearingRequest.getEstimateMinutes(), "EstimatedMinutes minutes"))
                            .withId(hearingId)
                            .withJurisdictionType(referredListHearingRequest.getJurisdictionType())
                            .withProsecutionCases(listOfProsecutionCase)
                            .withProsecutorDatesToAvoid(referredListHearingRequest.getProsecutorDatesToAvoid())
                            .withType(hearingType)
                            .withListingDirections(referredListHearingRequest.getListingDirections())
                            .withReportingRestrictionReason(referredListHearingRequest.getReportingRestrictionReason())
                            .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope, referredListHearingRequest.getListDefendantRequests()))
                            .withCourtCentre(referenceDataService.getCourtCentre(jsonEnvelope, getDefendantPostcode(listOfProsecutionCase), getProsecutionAuthorityCode(prosecutionCases), requester))
                            .build();
                    hearingsList.add(hearings);
                });

        return ListCourtHearing.listCourtHearing().withHearings(hearingsList).build();
    }

    public ListCourtHearing transform(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases,
                                      final List<ListHearingRequest> listHearingRequests, final UUID hearingId) {
        LOGGER.info("Transforming SPI cases to ListCourtHearing");
        final List<HearingListingNeeds> hearingsList = new ArrayList<>();

        listHearingRequests.stream().forEach(listHearingRequest -> {
            final List<ProsecutionCase> listOfProsecutionCase = filterProsecutionCasesFromSpi(prosecutionCases, listHearingRequest.getListDefendantRequests());
            final ZonedDateTime expectedListingStartDateTime = nonNull(listHearingRequest.getListedStartDateTime()) ? listHearingRequest.getListedStartDateTime() : listHearingRequest.getEarliestStartDateTime();
            final List<ListDefendantRequest> listDefendantRequests = updateListDefendantRequestsForYouth(expectedListingStartDateTime, listOfProsecutionCase, listHearingRequest.getListDefendantRequests());
            final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                    .withEarliestStartDateTime(listHearingRequest.getEarliestStartDateTime())
                    .withListedStartDateTime(listHearingRequest.getListedStartDateTime())
                    .withEstimatedMinutes(listHearingRequest.getEstimateMinutes())
                    .withId(hearingId)
                    .withJurisdictionType(listHearingRequest.getJurisdictionType())
                    .withProsecutionCases(listOfProsecutionCase)
                    .withProsecutorDatesToAvoid(listHearingRequest.getProsecutorDatesToAvoid())
                    .withType(listHearingRequest.getHearingType())
                    .withListingDirections(listHearingRequest.getListingDirections())
                    .withReportingRestrictionReason(listHearingRequest.getReportingRestrictionReason())
                    .withCourtCentre(listHearingRequest.getCourtCentre())
                    .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope, listDefendantRequests))
                    .build();
            hearingsList.add(hearing);
        });
        return ListCourtHearing.listCourtHearing().withHearings(hearingsList).build();
    }

    private List<ListDefendantRequest> updateListDefendantRequestsForYouth(final ZonedDateTime expectedListingStartDateTime, final List<ProsecutionCase> listOfProsecutionCase, final List<ListDefendantRequest> listDefendantRequests) {

        return listDefendantRequests.stream().map(listDefendantRequest ->
                ListDefendantRequest.listDefendantRequest()
                        .withDatesToAvoid(listDefendantRequest.getDatesToAvoid())
                        // SPI cases will not have referral reason
                        .withDefendantId(listDefendantRequest.getDefendantId())
                        .withHearingLanguageNeeds(listDefendantRequest.getHearingLanguageNeeds())
                        .withProsecutionCaseId(listDefendantRequest.getProsecutionCaseId())
                        .withReferralReason(listDefendantRequest.getReferralReason())
                        .withDefendantOffences(listDefendantRequest.getDefendantOffences())
                        .withSummonsRequired(isDefendantYouth(expectedListingStartDateTime, listDefendantRequest.getDefendantId(), listOfProsecutionCase) ? SummonsRequired.YOUTH : listDefendantRequest.getSummonsRequired())
                        .build()
        ).collect(Collectors.toList());
    }

    private boolean isDefendantYouth(final ZonedDateTime expectedListingStartDateTime, final UUID defendantId, final List<ProsecutionCase> listOfProsecutionCase) {
        boolean isYouth = false;
        final Optional<Defendant> defendant = listOfProsecutionCase.stream()
                .flatMap(pc -> pc.getDefendants().stream())
                .filter(d -> d.getId().equals(defendantId))
                .findFirst();

        if (defendant.isPresent()) {
            final Optional<LocalDate> birthDate = getDateOfBirth(defendant.get());
            if (birthDate.isPresent() && calculateIsYouth(birthDate.get(), expectedListingStartDateTime)) {
                isYouth = true;
            }
        }

        return isYouth;
    }

    private Optional<LocalDate> getDateOfBirth(final uk.gov.justice.core.courts.Defendant defendant) {
        return Optional.ofNullable(defendant)
                .map(uk.gov.justice.core.courts.Defendant::getPersonDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(Person::getDateOfBirth);
    }

    private boolean calculateIsYouth(final LocalDate birthDate, final ZonedDateTime earliestHearingDate) {
        final ZonedDateTime dateOfBirth = ZonedDateTime.of(birthDate.atTime(0, 0), ZoneId.systemDefault());
        return earliestHearingDate.minus(18, ChronoUnit.YEARS).isBefore(dateOfBirth);
    }

    public ListCourtHearing transform(final ApplicationReferredToCourt applicationReferredToCourt) {

        LOGGER.info("Preparing application for list court hearing");

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        final HearingListingNeeds hearingRequest = applicationReferredToCourt.getHearingRequest();

        final HearingListingNeeds.Builder hearingsBuilder = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingRequest.getId())
                .withCourtCentre(hearingRequest.getCourtCentre())
                .withJudiciary(hearingRequest.getJudiciary())
                .withProsecutionCases(hearingRequest.getProsecutionCases())
                .withType(hearingRequest.getType())
                .withJurisdictionType(hearingRequest.getJurisdictionType())
                .withEarliestStartDateTime(hearingRequest.getEarliestStartDateTime())
                .withEstimatedMinutes(hearingRequest.getEstimatedMinutes())
                .withReportingRestrictionReason(hearingRequest.getReportingRestrictionReason())
                .withListingDirections(hearingRequest.getListingDirections())
                .withDefendantListingNeeds(hearingRequest.getDefendantListingNeeds());

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = hearingRequest.getCourtApplicationPartyListingNeeds();

        if (CollectionUtils.isNotEmpty(courtApplicationPartyListingNeedsList)) {
            hearingsBuilder.withCourtApplicationPartyListingNeeds(courtApplicationPartyListingNeedsList);
        }

        final List<CourtApplication> courtApplications = hearingRequest.getCourtApplications();

        if (CollectionUtils.isNotEmpty(courtApplications)) {
            hearingsBuilder.withCourtApplications(hearingRequest.getCourtApplications());
        }

        hearingsList.add(hearingsBuilder.build());

        return ListCourtHearing.listCourtHearing().withHearings(hearingsList).build();
    }

    private String getReferralReasonDescription(final JsonEnvelope jsonEnvelope, final UUID referralId) {
        final JsonObject jsonObject = referenceDataService.getReferralReasonById(jsonEnvelope, referralId, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("ReferralReason", referralId.toString()));
        return jsonObject.getString("reason");
    }

    /**
     * Take list of ListDefendantRequest (progresssion) Returns list of ListDefendantRequests
     * (listing)
     */
    private List<DefendantListingNeeds> getListDefendantRequests(final JsonEnvelope jsonEnvelope, final List<ListDefendantRequest> collectionOfListDefendantRequest) {
        return collectionOfListDefendantRequest.stream().map(listDefendantRequest ->
                DefendantListingNeeds.defendantListingNeeds()
                        .withDatesToAvoid(listDefendantRequest.getDatesToAvoid())
                        // SPI cases will not have referral reason
                        .withDefendantId(isNull(listDefendantRequest.getDefendantId()) ?
                                listDefendantRequest.getReferralReason().getDefendantId() : listDefendantRequest.getDefendantId())
                        .withHearingLanguageNeeds(listDefendantRequest.getHearingLanguageNeeds())
                        .withProsecutionCaseId(listDefendantRequest.getProsecutionCaseId())
                        .withListingReason(isNull(listDefendantRequest.getReferralReason()) ? null
                                : (getReferralReasonDescription(jsonEnvelope, listDefendantRequest.getReferralReason().getId())))
                        .withIsYouth(listDefendantRequest.getSummonsRequired() != null && SummonsRequired.YOUTH.equals(listDefendantRequest.getSummonsRequired()))
                        .build()
        ).collect(Collectors.toList());
    }
}
