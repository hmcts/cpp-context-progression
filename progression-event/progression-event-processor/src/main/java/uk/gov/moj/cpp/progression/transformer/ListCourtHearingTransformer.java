package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static java.util.Arrays.asList;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
    private RefDataService referenceDataService;

    @Inject
    private ProgressionService progressionService;

    private static final String POSTCODE_IS_MISSING = "Postcode is missing for";

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
                    .filter(defendant -> ofNullable(listDefendantRequest.getReferralReason()).map(ReferralReason::getDefendantId).orElse(listDefendantRequest.getDefendantId())
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

            final ProsecutionCase.Builder builder = ProsecutionCase.prosecutionCase().withId(prosecutionCaseId)
                    .withCaseStatus(matchedProsecutionCase.getCaseStatus())
                    .withDefendants(mapOfProsecutionCaseIdWithDefendants.get(prosecutionCaseId))
                    .withInitiationCode(matchedProsecutionCase.getInitiationCode())
                    .withOriginatingOrganisation(matchedProsecutionCase.getOriginatingOrganisation())
                    .withCpsOrganisation(matchedProsecutionCase.getCpsOrganisation())
                    .withCpsOrganisationId(matchedProsecutionCase.getCpsOrganisationId())
                    .withIsCpsOrgVerifyError(matchedProsecutionCase.getIsCpsOrgVerifyError())
                    .withProsecutionCaseIdentifier(matchedProsecutionCase.getProsecutionCaseIdentifier())
                    .withStatementOfFacts(matchedProsecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(matchedProsecutionCase.getStatementOfFactsWelsh());

            if(nonNull(matchedProsecutionCase.getProsecutor())){
                builder.withProsecutor(Prosecutor.prosecutor()
                        .withValuesFrom(matchedProsecutionCase.getProsecutor())
                        .build());
            }

            listOfProsecutionCase.add(builder.build());

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
                .withIsYouth(matchedDefendant.getIsYouth())
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
                    .withCpsOrganisation(matchedProsecutionCase.getCpsOrganisation())
                    .withCpsOrganisationId(matchedProsecutionCase.getCpsOrganisationId())
                    .withIsCpsOrgVerifyError(matchedProsecutionCase.getIsCpsOrgVerifyError())
                    .withProsecutionCaseIdentifier(matchedProsecutionCase.getProsecutionCaseIdentifier())
                    .withStatementOfFacts(matchedProsecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(matchedProsecutionCase.getStatementOfFactsWelsh())
                    .withCaseMarkers(matchedProsecutionCase.getCaseMarkers())
                    .withTrialReceiptType(matchedProsecutionCase.getTrialReceiptType())
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
        final Optional<Defendant> defendant = ofNullable(prosecutionCases.get(0).getDefendants().get(0));
        final Optional<PersonDefendant> personDefendant;
        final Optional<LegalEntityDefendant> legalEntityDefendant;

        if (defendant.isPresent()) {
            personDefendant = ofNullable(defendant.get().getPersonDefendant());
            legalEntityDefendant = ofNullable(defendant.get().getLegalEntityDefendant());
            if (personDefendant.isPresent()) {
                final Address address = getRequiredField(personDefendant.get().getPersonDetails().getAddress(), "Address is missing for personDefendant");
                final String postcode = getRequiredField(address.getPostcode(), POSTCODE_IS_MISSING + " personDefendant");
                return postcode.split(" ")[0];
            } else if (legalEntityDefendant.isPresent()) {
                final Address address = getRequiredField(legalEntityDefendant.get().getOrganisation().getAddress(), "Address is missing for legalEntityDefendant");
                final String postcode = getRequiredField(address.getPostcode(), POSTCODE_IS_MISSING + " legalEntityDefendant");
                return postcode.split(" ")[0];
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
                            .withEstimatedDuration(referredListHearingRequest.getEstimatedDuration())
                            .withId(hearingId)
                            .withJurisdictionType(referredListHearingRequest.getJurisdictionType())
                            .withProsecutionCases(listOfProsecutionCase)
                            .withProsecutorDatesToAvoid(referredListHearingRequest.getProsecutorDatesToAvoid())
                            .withType(hearingType)
                            .withListingDirections(referredListHearingRequest.getListingDirections())
                            .withReportingRestrictionReason(referredListHearingRequest.getReportingRestrictionReason())
                            .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope, referredListHearingRequest.getListDefendantRequests()))
                            .withCourtCentre(calculateCourtCentre(sjpReferral, referenceDataService, jsonEnvelope, listOfProsecutionCase, prosecutionCases, requester))
                            .build();
                    hearingsList.add(hearings);
                });

        return ListCourtHearing.listCourtHearing().withHearings(hearingsList).build();
    }

    public ListCourtHearing transformSjpReferralNextHearing(final JsonEnvelope jsonEnvelope,
                                      final List<ProsecutionCase> prosecutionCases,
                                      final UUID hearingId, final NextHearing nextHearing, final List<ReferredListHearingRequest> referredListHearingRequests) {

        LOGGER.debug("Transforming SJP reference with nextHearing  to ListCourtHearing");

        final HearingListingNeeds hearings = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .withCourtCentre(progressionService.transformCourtCentre(nextHearing.getCourtCentre(), jsonEnvelope))
                .withBookedSlots(nextHearing.getHmiSlots())
                .withListedStartDateTime(nextHearing.getListedStartDateTime())
                .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                .withType(nextHearing.getType())
                .withJurisdictionType(MAGISTRATES)
                .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope, referredListHearingRequests.stream().flatMap(r->r.getListDefendantRequests().stream()).collect(Collectors.toList())))
                .build();

        return ListCourtHearing.listCourtHearing().withHearings(asList(hearings)).build();
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
                    .withEstimatedDuration(listHearingRequest.getEstimatedDuration())
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

    public ListCourtHearing transform(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases,
                                      CourtHearingRequest courtHearingRequest, final UUID hearingId) {
        LOGGER.info("Transforming courtHearingRequest cases to ListCourtHearing");
        final List<HearingListingNeeds> hearingsList = new ArrayList<>();

        final List<ProsecutionCase> listOfProsecutionCase = filterProsecutionCases(prosecutionCases, courtHearingRequest.getListDefendantRequests());
        final ZonedDateTime expectedListingStartDateTime = calculateExpectedStartDate(courtHearingRequest);
        final List<ListDefendantRequest> listDefendantRequests = updateListDefendantRequestsForYouth(expectedListingStartDateTime, listOfProsecutionCase, courtHearingRequest.getListDefendantRequests());
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withEarliestStartDateTime(courtHearingRequest.getEarliestStartDateTime())
                .withListedStartDateTime(courtHearingRequest.getListedStartDateTime())
                .withEstimatedMinutes(courtHearingRequest.getEstimatedMinutes())
                .withBookedSlots(courtHearingRequest.getBookedSlots())
                .withId(hearingId)
                .withJurisdictionType(courtHearingRequest.getJurisdictionType())
                .withProsecutionCases(listOfProsecutionCase)
                .withType(courtHearingRequest.getHearingType())
                .withListingDirections(courtHearingRequest.getListingDirections())
                .withCourtCentre(courtHearingRequest.getCourtCentre())
                .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope, listDefendantRequests))
                .withPriority(courtHearingRequest.getPriority())
                .withBookingType(courtHearingRequest.getBookingType())
                .withSpecialRequirements(courtHearingRequest.getSpecialRequirements())
                .withWeekCommencingDate(courtHearingRequest.getWeekCommencingDate())
                .build();
        hearingsList.add(hearing);

        return ListCourtHearing.listCourtHearing().withHearings(hearingsList).build();
    }

    protected ZonedDateTime calculateExpectedStartDate(final CourtHearingRequest courtHearingRequest) {
        ZonedDateTime expectedListingStartDateTime = nonNull(courtHearingRequest.getListedStartDateTime()) ? courtHearingRequest.getListedStartDateTime() : courtHearingRequest.getEarliestStartDateTime();
        if (expectedListingStartDateTime == null && courtHearingRequest.getWeekCommencingDate() != null) {
            expectedListingStartDateTime = courtHearingRequest.getWeekCommencingDate().getStartDate().atStartOfDay(ZoneOffset.UTC);
        }
        return expectedListingStartDateTime;
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
                        .withSummonsRequired(isDefendantYouth(expectedListingStartDateTime, listDefendantRequest.getDefendantId(), listOfProsecutionCase) ? SummonsType.YOUTH : listDefendantRequest.getSummonsRequired())
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
        return ofNullable(defendant)
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
                .withEstimatedDuration(hearingRequest.getEstimatedDuration())
                .withReportingRestrictionReason(hearingRequest.getReportingRestrictionReason())
                .withListingDirections(hearingRequest.getListingDirections())
                .withDefendantListingNeeds(hearingRequest.getDefendantListingNeeds())
                .withBookingType(hearingRequest.getBookingType())
                .withPriority(hearingRequest.getPriority())
                .withSpecialRequirements(hearingRequest.getSpecialRequirements())
                .withBookedSlots(hearingRequest.getBookedSlots());

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

    private String getReferralReasonDescription(final JsonEnvelope jsonEnvelope, final ReferralReason referralReason) {
        if(referralReason == null){
            return null;
        }
        final UUID referralId = referralReason.getId();
        final JsonObject jsonObject = referenceDataService.getReferralReasonByReferralReasonId(jsonEnvelope, referralId, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("ReferralReason", referralId.toString()));
        return jsonObject.getString("reason", null);
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
                        .withListingReason(getReferralReasonDescription(jsonEnvelope, listDefendantRequest.getReferralReason()))
                        .withIsYouth(listDefendantRequest.getSummonsRequired() != null && SummonsType.YOUTH.equals(listDefendantRequest.getSummonsRequired()))
                        .build()
        ).collect(Collectors.toList());
    }

    private CourtCentre calculateCourtCentre(final SjpReferral sjpReferral,
                                             final RefDataService referenceDataService,
                                             final JsonEnvelope jsonEnvelope,
                                             final List<ProsecutionCase> listOfProsecutionCase,
                                             final List<ProsecutionCase> prosecutionCases,
                                             final Requester requester) {
        try {
            final Optional<JsonObject> responseOuCode =
                    referenceDataService.getCourtsByPostCodeAndProsecutingAuthority(
                            jsonEnvelope,
                            getDefendantPostcode(listOfProsecutionCase),
                            getProsecutionAuthorityCode(prosecutionCases),
                            requester);

            final String oucode;
            if (responseOuCode.isPresent() && !responseOuCode.get().getJsonArray("courts").isEmpty()) {
                final String courtHouseCode = ((JsonObject)
                        responseOuCode.get().getJsonArray("courts").get(0)).getString("oucode");
                oucode = courtHouseCode;
            } else {
                oucode = sjpReferral.getReferringJudicialDecision().getCourtHouseCode();
            }
            return referenceDataService.getCourtCentre(oucode, jsonEnvelope, requester);

        } catch (MissingRequiredFieldException e) {
            if (e.getMessage().contains(POSTCODE_IS_MISSING)) {
                return referenceDataService.getCourtCentre(sjpReferral.getReferringJudicialDecision().getCourtHouseCode(), jsonEnvelope, requester);
            } else {
                throw e;
            }
        }
    }

}
