package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingLanguageNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.NextHearingDefendant;
import uk.gov.justice.core.courts.NextHearingProsecutionCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingAdjourned;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1135", "squid:S3655", "squid:S1481", "squid:S1612", "squid:S1188", "squid:S00112", "squid:S1168"})
@ServiceComponent(EVENT_PROCESSOR)
public class AdjournHearingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdjournHearingEventProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Handles("public.hearing.adjourned")
    public void handleHearingAdjournedPublicEvent(final JsonEnvelope event) {
        final HearingAdjourned hearingAdjourned = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingAdjourned.class);
        hearingAdjourned.getNextHearings().forEach(nextHearing -> {
            final List<CourtApplication> courtApplications =
                    Optional.ofNullable(nextHearing.getNextHearingCourtApplicationId()).map(ids ->
                            ids.stream().map(id -> progressionService.getCourtApplicationByIdTyped(event, id.toString()).<RuntimeException>orElseThrow(
                                    () -> new RuntimeException(String.format("unknown court application: %s ", id))
                                    )
                            ).collect(Collectors.toList())).orElse(Collections.emptyList());

            final HearingListingNeeds.Builder builder = HearingListingNeeds.hearingListingNeeds()
                    .withCourtCentre(nextHearing.getCourtCentre())
                    .withDefendantListingNeeds(getDefendantListingNeeds(nextHearing.getNextHearingProsecutionCases()))
                    .withProsecutionCases(getProsecutionCases(event, nextHearing.getNextHearingProsecutionCases()))
                    .withEarliestStartDateTime(nextHearing.getListedStartDateTime())
                    .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                    .withId(UUID.randomUUID())
                    .withJurisdictionType(JurisdictionType.valueOf(nextHearing.getJurisdictionType().name()))
                    .withJudiciary(nextHearing.getJudiciary())
                    .withType(nextHearing.getType())
                    .withReportingRestrictionReason(nextHearing.getReportingRestrictionReason());

            final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = getCourtApplicationPartyListingNeeds(courtApplications, nextHearing.getHearingLanguage());
            if (CollectionUtils.isNotEmpty(courtApplicationPartyListingNeedsList)) {
                builder.withCourtApplicationPartyListingNeeds(courtApplicationPartyListingNeedsList);
            }

            if (CollectionUtils.isNotEmpty(courtApplications)) {
                builder.withCourtApplications(courtApplications);
            }

            final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing()
                    .withHearings(Arrays.asList(builder.build()))
                    .build();

            listingService.listCourtHearing(event, listCourtHearing);
            progressionService.updateHearingListingStatusToSentForListing(event, listCourtHearing);
        });
    }

    private List<ProsecutionCase> getProsecutionCases(final JsonEnvelope event, final List<NextHearingProsecutionCase> nextHearingProsecutionCases) {
        if (nextHearingProsecutionCases == null) {
            return null;
        }
        List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        for (NextHearingProsecutionCase nextHearingProsecutionCase : nextHearingProsecutionCases) {
            UUID prosecutionCaseId = nextHearingProsecutionCase.getId();
            Optional<JsonObject> prosecutionCaseOptional = progressionService.getProsecutionCaseDetailById(event, prosecutionCaseId.toString());
            final JsonObject prosecutionCaseJson = prosecutionCaseOptional.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
            ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            List<UUID> nextHearingDefendantIds = nextHearingProsecutionCase.getDefendants().stream().map(d -> d.getId()).collect(Collectors.toList());
            List<Defendant> updatedDefendants = getAdjournedDefendantsForProsecutionCase(nextHearingProsecutionCase, prosecutionCase, nextHearingDefendantIds);

            ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                    .withId(prosecutionCase.getId())
                    .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                    .withDefendants(updatedDefendants)
                    .withInitiationCode(prosecutionCase.getInitiationCode())
                    .withCaseStatus(prosecutionCase.getCaseStatus())
                    .withOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation())
                    .withStatementOfFacts(prosecutionCase.getStatementOfFacts())
                    .withStatementOfFactsWelsh(prosecutionCase.getStatementOfFactsWelsh())
                    .build();

            prosecutionCases.add(updatedProsecutionCase);
        }
        return prosecutionCases;
    }

    private List<Defendant> getAdjournedDefendantsForProsecutionCase(NextHearingProsecutionCase nextHearingProsecutionCase, ProsecutionCase prosecutionCase, List<UUID> nextHearingDefendantIds) {
        List<Defendant> updatedDefendants = new ArrayList<>();
        for (Defendant defendant : prosecutionCase.getDefendants()) {
            if (nextHearingDefendantIds.contains(defendant.getId())) {
                Optional<NextHearingDefendant> nextHearingDefendant = nextHearingProsecutionCase.getDefendants().stream().filter(o -> o.getId().equals(defendant.getId())).findFirst();
                List<UUID> nextHearingOffenceIds = nextHearingDefendant.get().getOffences().stream().map(offence -> offence.getId()).collect(Collectors.toList());
                List<Offence> adjournedOffencesForDefendant = getAdjournedOffencesForDefendant(defendant, nextHearingOffenceIds);
                Defendant updatedDefendant = Defendant.defendant()
                        .withId(defendant.getId())
                        .withOffences(adjournedOffencesForDefendant)
                        .withPersonDefendant(defendant.getPersonDefendant())
                        .withAssociatedPersons(defendant.getAssociatedPersons())
                        .withDefenceOrganisation(defendant.getDefenceOrganisation())
                        .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                        .withMitigation(defendant.getMitigation())
                        .withMitigationWelsh(defendant.getMitigationWelsh())
                        .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                        .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                        .withProsecutionCaseId(defendant.getProsecutionCaseId())
                        .withWitnessStatement(defendant.getWitnessStatement())
                        .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                        .withIsYouth(defendant.getIsYouth())
                        .build();
                updatedDefendants.add(updatedDefendant);
            }
        }
        return updatedDefendants;
    }

    private List<Offence> getAdjournedOffencesForDefendant(Defendant defendant, List<UUID> nextHearingOffenceIds) {
        return defendant.getOffences().stream().filter(offence -> nextHearingOffenceIds.contains(offence.getId())).collect(Collectors.toList());

    }

    private List<DefendantListingNeeds> getDefendantListingNeeds(List<NextHearingProsecutionCase> nextHearingProsecutionCases) {
        if (nextHearingProsecutionCases == null) {
            return null;
        } else {
            List<DefendantListingNeeds> defendantListingNeedsList = new ArrayList<>();
            nextHearingProsecutionCases.forEach(nextHearingProsecutionCase ->
                    nextHearingProsecutionCase.getDefendants().forEach(nextHearingDefendant ->
                            defendantListingNeedsList.add(DefendantListingNeeds.defendantListingNeeds()
                                    .withProsecutionCaseId(nextHearingProsecutionCase.getId())
                                    .withDefendantId(nextHearingDefendant.getId())
                                    .build())));
            return defendantListingNeedsList;
        }
    }

    private List<CourtApplicationPartyListingNeeds> getCourtApplicationPartyListingNeeds(final List<CourtApplication> courtApplications, final HearingLanguage hearingLanguage) {

        final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeedsList = new ArrayList<>();
        for (final CourtApplication courtApplication : courtApplications) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("getCourtApplicationPartyListingNeeds courtApplication %s applicant ", courtApplication.getId()));

            }
            if (courtApplication.getApplicant().getProsecutingAuthority() != null) {
                final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withCourtApplicationId(courtApplication.getId())
                        .withCourtApplicationPartyId(courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityId())
                        .withHearingLanguageNeeds(HearingLanguageNeeds.valueOf(hearingLanguage.name()))
                        .build();
                courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
            }

            final List<CourtApplicationRespondent> respondents = courtApplication.getRespondents();
            if (null != respondents && !respondents.isEmpty()) {
                respondents.forEach(courtApplicationRespondent -> {
                    if (null != courtApplicationRespondent.getPartyDetails().getProsecutingAuthority()) {
                        final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds = CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                                .withCourtApplicationId(courtApplication.getId())
                                .withCourtApplicationPartyId(courtApplicationRespondent.getPartyDetails().getId())
                                .withHearingLanguageNeeds(HearingLanguageNeeds.valueOf(hearingLanguage.name()))
                                .build();
                        courtApplicationPartyListingNeedsList.add(courtApplicationPartyListingNeeds);
                    }
                });
            }
        }
        return courtApplicationPartyListingNeedsList;
    }

}
