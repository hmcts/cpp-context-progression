package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantListingNeeds;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NextHearingDefendant;
import uk.gov.justice.core.courts.NextHearingProsecutionCase;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SendCaseForListing;
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
import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_PROCESSOR)
public class AdjournHearingEventProcessor {

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
            final SendCaseForListing sendCaseForListing = SendCaseForListing.sendCaseForListing()
                    .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                            .withCourtCentre(nextHearing.getCourtCentre())
                            .withDefendantListingNeeds(getDefendantListingNeeds(nextHearing.getNextHearingProsecutionCases()))
                            .withProsecutionCases(getProsecutionCases(event, nextHearing.getNextHearingProsecutionCases()))
                            .withEarliestStartDateTime(nextHearing.getEarliestStartDateTime())
                            .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                            .withId(UUID.randomUUID())
                            .withJurisdictionType(JurisdictionType.valueOf(nextHearing.getJurisdictionType().name()))
                            .withJudiciary(nextHearing.getJudiciary())
                            .withType(nextHearing.getType())
                            .withReportingRestrictionReason(nextHearing.getReportingRestrictionReason())
                            .build()))
                    .build();
            listingService.sendCaseForListing(event, sendCaseForListing);
            progressionService.updateHearingListingStatusToSentForListing(event, sendCaseForListing);
        });
    }

    private List<ProsecutionCase> getProsecutionCases(final JsonEnvelope event, final List<NextHearingProsecutionCase> nextHearingProsecutionCases) {
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
