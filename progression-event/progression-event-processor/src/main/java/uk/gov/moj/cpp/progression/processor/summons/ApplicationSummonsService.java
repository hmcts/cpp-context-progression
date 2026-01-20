package uk.gov.moj.cpp.progression.processor.summons;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static uk.gov.justice.core.courts.summons.BreachSummonsDocumentContent.breachSummonsDocumentContent;
import static uk.gov.justice.core.courts.summons.SummonsAddressee.summonsAddressee;
import static uk.gov.justice.core.courts.summons.SummonsDefendant.summonsDefendant;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.emptyIfBlank;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getFullName;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getProsecutorCosts;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getSummonsHearingDetails;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.populateSummonsAddress;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.summons.ApplicationSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.BreachSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsAddressee;
import uk.gov.justice.core.courts.summons.SummonsDefendant;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;

import java.util.Optional;

import javax.json.JsonObject;

public class ApplicationSummonsService {

    public SummonsDocumentContent generateSummonsDocumentContent(final SummonsDataPrepared summonsDataPrepared,
                                                                 final CourtApplication courtApplication,
                                                                 final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds,
                                                                 final JsonObject courtCentreJson,
                                                                 final Optional<LjaDetails> optionalLjaDetails) {

        final SummonsType summonsRequired = courtApplicationPartyListingNeeds.getSummonsRequired();
        final SummonsData summonsData = summonsDataPrepared.getSummonsData();

        final SummonsDocumentContent.Builder summonsDocumentContent = summonsDocumentContent();
        summonsDocumentContent.withSubTemplateName(summonsRequired.name());
        summonsDocumentContent.withType(summonsRequired.toString());
        summonsDocumentContent.withCaseReference(courtApplication.getApplicationReference());
        summonsDocumentContent.withIssueDate(now());

        optionalLjaDetails.ifPresent(ljaDetails -> {
            final String ljaName = emptyIfBlank(ljaDetails.getLjaName());
            final String ljaNameWelsh = defaultIfBlank(ljaDetails.getWelshLjaName(), ljaName);
            summonsDocumentContent.withLjaCode(emptyIfBlank(ljaDetails.getLjaCode()));
            summonsDocumentContent.withLjaName(ljaName);
            summonsDocumentContent.withLjaNameWelsh(ljaNameWelsh);
        });

        final CourtApplicationParty courtApplicationParty = courtApplication.getSubject();
        final SummonsDefendant summonsDefendant = buildSummonsDefendant(courtApplicationParty);
        summonsDocumentContent.withDefendant(summonsDefendant);

        if (nonNull(summonsDefendant)) {
            final SummonsAddressee summonsAddressee = summonsAddressee().withName(summonsDefendant.getName()).withAddress(summonsDefendant.getAddress()).build();
            summonsDocumentContent.withAddressee(summonsAddressee);
        }

        final SummonsHearingCourtDetails summonsHearingCourtDetails = getSummonsHearingDetails(courtCentreJson, summonsData.getCourtCentre().getRoomId(), summonsData.getHearingDateTime());
        summonsDocumentContent.withHearingCourtDetails(summonsHearingCourtDetails);

        final SummonsApprovedOutcome summonsApprovedOutcome = courtApplicationPartyListingNeeds.getSummonsApprovedOutcome();
        if (nonNull(summonsApprovedOutcome)) {
            summonsDocumentContent.withProsecutorCosts(getProsecutorCosts(summonsApprovedOutcome.getProsecutorCost(), HearingLanguage.WELSH == courtApplicationPartyListingNeeds.getHearingLanguageNeeds()));
            summonsDocumentContent.withPersonalService(summonsApprovedOutcome.getPersonalService());
        }

        summonsDocumentContent.withApplicationContent(buildApplicationContent(courtApplication));
        summonsDocumentContent.withBreachContent(buildBreachContent(courtApplication));

        return summonsDocumentContent.build();
    }


    private SummonsDefendant buildSummonsDefendant(final CourtApplicationParty subject) {
        if (nonNull(subject.getMasterDefendant())) {
            return buildSummonsDefendant(subject.getMasterDefendant());
        }

        if (nonNull(subject.getOrganisation())) {
            return buildSummonsDefendant(subject.getOrganisation());
        }

        if (nonNull(subject.getProsecutingAuthority())) {
            return buildSummonsDefendant(subject.getProsecutingAuthority());
        }

        if (nonNull(subject.getPersonDetails())) {
            return buildSummonsDefendant(subject.getPersonDetails());
        }

        return null;
    }

    private SummonsDefendant buildSummonsDefendant(final MasterDefendant masterDefendant) {
        if (nonNull(masterDefendant)) {
            final PersonDefendant personDefendant = masterDefendant.getPersonDefendant();
            if (nonNull(personDefendant)) {
                final Person person = personDefendant.getPersonDetails();
                return buildSummonsDefendant(person);
            }
            final LegalEntityDefendant legalEntityDefendant = masterDefendant.getLegalEntityDefendant();
            if (nonNull(legalEntityDefendant)) {
                return summonsDefendant()
                        .withName(nonNull(legalEntityDefendant.getOrganisation()) ? legalEntityDefendant.getOrganisation().getName() : EMPTY)
                        .withAddress(populateSummonsAddress(legalEntityDefendant.getOrganisation().getAddress()))
                        .build();
            }
        }
        return null;
    }

    private SummonsDefendant buildSummonsDefendant(final Organisation organisation) {
        return summonsDefendant()
                .withName(organisation.getName())
                .withAddress(populateSummonsAddress(organisation.getAddress()))
                .build();
    }

    private SummonsDefendant buildSummonsDefendant(final ProsecutingAuthority prosecutingAuthority) {
        return summonsDefendant()
                .withName(prosecutingAuthority.getName())
                .withAddress(populateSummonsAddress(prosecutingAuthority.getAddress()))
                .build();
    }

    private SummonsDefendant buildSummonsDefendant(final Person person) {
        return summonsDefendant()
                .withName(getFullName(person.getFirstName(), person.getMiddleName(), person.getLastName()))
                .withDateOfBirth(nonNull(person.getDateOfBirth()) ? person.getDateOfBirth().toString() : EMPTY)
                .withAddress(populateSummonsAddress(person.getAddress()))
                .build();
    }

    private ApplicationSummonsDocumentContent buildApplicationContent(final CourtApplication courtApplication) {
        return ApplicationSummonsDocumentContent.applicationSummonsDocumentContent()
                .withApplicationType(courtApplication.getType().getType())
                .withApplicationTypeWelsh(defaultIfBlank(courtApplication.getType().getTypeWelsh(), courtApplication.getType().getType()))
                .withApplicationLegislation(courtApplication.getType().getLegislation())
                .withApplicationLegislationWelsh(defaultIfBlank(courtApplication.getType().getLegislationWelsh(), courtApplication.getType().getLegislation()))
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationParticularsWelsh(courtApplication.getApplicationParticulars()) // we do not have this value captured in the application
                .withApplicationReference(courtApplication.getApplicationReference())
                .build();
    }

    private BreachSummonsDocumentContent buildBreachContent(final CourtApplication courtApplication) {
        if (nonNull(courtApplication.getCourtOrder())) {
            return breachSummonsDocumentContent()
                    .withBreachedOrderDate(courtApplication.getCourtOrder().getOrderDate().toString())
                    .withOrderingCourt(courtApplication.getCourtOrder().getOrderingCourt().getName())
                    .build();
        }
        return null;
    }

}
