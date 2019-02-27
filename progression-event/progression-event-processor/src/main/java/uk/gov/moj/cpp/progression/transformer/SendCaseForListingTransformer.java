package uk.gov.moj.cpp.progression.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferredListHearingRequest;
import uk.gov.justice.core.courts.SendCaseForListing;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.MissingRequiredFieldException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@SuppressWarnings({"squid:S1188", "squid:S2259"})
public class SendCaseForListingTransformer {


    private static final Logger LOGGER = LoggerFactory.getLogger(SendCaseForListingTransformer.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ReferenceDataService referenceDataService;

     /**
     * Transform a CourtReferral to SendCaseForListing
     *
     * @return SendCaseForListing
     */
    public SendCaseForListing transform(final JsonEnvelope jsonEnvelope,
                                        final List<ProsecutionCase> prosecutionCases,
                                        final SjpReferral sjpReferral,
                                        final List<ReferredListHearingRequest> referredListHearingRequests,
                                        final UUID hearingId) {
        LOGGER.info("Preparing send case for listing ");
        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        referredListHearingRequests.stream()
                .forEach(referredListHearingRequest ->  {

                    final List<ProsecutionCase> listOfProsecutionCase = filterProsecutionCases(prosecutionCases, referredListHearingRequest);

                    final JsonObject jsonObject= referenceDataService
                            .getHearingType(jsonEnvelope,referredListHearingRequest.getHearingType().getId())
                            .orElseThrow( () -> new ReferenceDataNotFoundException("Hearing Type",referredListHearingRequest.getHearingType().getId().toString()));
                    final HearingType hearingType=HearingType.hearingType()
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
                            .withDefendantListingNeeds(getListDefendantRequests(jsonEnvelope,referredListHearingRequest.getListDefendantRequests()))
                            .withCourtCentre(referenceDataService.getCourtCentre(jsonEnvelope, getPostcode(listOfProsecutionCase), getProsecutionAuthorityCode(prosecutionCases)))
                            .build();
                    hearingsList.add(hearings);
                });

        return SendCaseForListing.sendCaseForListing().withHearings(hearingsList).build();
    }


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

    private String getReferralReasonDescription(final JsonEnvelope jsonEnvelope, final UUID referralId){
       final JsonObject jsonObject = referenceDataService.getReferralReasonById(jsonEnvelope,referralId)
                .orElseThrow(()-> new ReferenceDataNotFoundException("ReferralReason",referralId.toString()));
        return jsonObject.getString("reason");
    }


    /**
     * Take list of ListDefendantRequest (progresssion) Returns list of ListDefendantRequests
     * (listing)
     */
    private  List<DefendantListingNeeds> getListDefendantRequests(final JsonEnvelope jsonEnvelope,final List<ListDefendantRequest> collectionOfListDefendantRequest) {
        return collectionOfListDefendantRequest.stream().map(listDefendantRequest ->
                DefendantListingNeeds.defendantListingNeeds()
                        .withDatesToAvoid(listDefendantRequest.getDatesToAvoid())
                        .withDefendantId(listDefendantRequest.getReferralReason().getDefendantId())
                        .withHearingLanguageNeeds(listDefendantRequest.getHearingLanguageNeeds())
                        .withProsecutionCaseId(listDefendantRequest.getProsecutionCaseId())
                        .withListingReason((getReferralReasonDescription(jsonEnvelope,listDefendantRequest.getReferralReason().getId())))
                        .build()
        ).collect(Collectors.toList());
    }

    @SuppressWarnings("squid:S1188")
    private static List<ProsecutionCase> filterProsecutionCases(final List<ProsecutionCase> prosecutionCases, final
    ReferredListHearingRequest listHearingRequest) {
        final Map<UUID, List<Defendant>> mapOfProsecutionCaseIdWithDefendants = new HashMap<>();
        listHearingRequest.getListDefendantRequests().stream().forEach(listDefendantRequest -> {

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
                final List<Defendant> defendants=new ArrayList<>();
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

    private static LocalDate getNoticeDate(final SjpReferral sjpReferral) {
        return LocalDate.parse(sjpReferral.getNoticeDate());
    }

    private static LocalDate getReferralDate(final SjpReferral sjpReferral) {
        return LocalDate.parse(sjpReferral.getReferralDate());
    }

    private static String getProsecutionAuthorityCode(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
    }

    private static String getPostcode(final List<ProsecutionCase> prosecutionCases) {
        return getRequiredField(
                    getRequiredField(
                        getRequiredField(prosecutionCases.get(0).getDefendants().get(0).getPersonDefendant(), "Person Defendant missing")
                                .getPersonDetails().getAddress(), "Address missing for Person defendant")
                            .getPostcode(), "Postcode missing for person defendant address").split(" ")[0];
    }
    private static <T> T getRequiredField(final T field, final String message){
        if(isNull(field)){
            throw new MissingRequiredFieldException(message);
        }
        return field;
    }
}
