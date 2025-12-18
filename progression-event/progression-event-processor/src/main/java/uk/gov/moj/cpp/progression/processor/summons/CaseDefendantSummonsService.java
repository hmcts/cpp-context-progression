package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.core.courts.summons.ReferralSummonsDocumentContent.referralSummonsDocumentContent;
import static uk.gov.justice.core.courts.summons.SummonsAddressee.summonsAddressee;
import static uk.gov.justice.core.courts.summons.SummonsDefendant.summonsDefendant;
import static uk.gov.justice.core.courts.summons.SummonsDocumentContent.summonsDocumentContent;
import static uk.gov.justice.core.courts.summons.SummonsOffence.summonsOffence;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.getSummonsCode;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.emptyIfBlank;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getFullName;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getSummonsHearingDetails;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.populateSummonsAddress;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.DVLA_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.ENDORSABLE_FLAG;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.summons.ReferralSummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsAddressee;
import uk.gov.justice.core.courts.summons.SummonsDefendant;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;
import uk.gov.justice.core.courts.summons.SummonsOffence;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CaseDefendantSummonsService {

    private static final JsonObject EMPTY_JSON_OBJECT = createObjectBuilder().build();

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    private static final String SOW_REF_VALUE = "MoJ";

    public SummonsDocumentContent generateSummonsPayloadForDefendant(final JsonEnvelope jsonEnvelope,
                                                                     final SummonsDataPrepared summonsDataPrepared,
                                                                     final ProsecutionCase prosecutionCaseQueried,
                                                                     final Defendant defendantQueried,
                                                                     final ListDefendantRequest defendantRequest,
                                                                     final JsonObject courtCentreJson,
                                                                     final Optional<LjaDetails> optionalLjaDetails,
                                                                     final SummonsProsecutor summonsProsecutor) {

        final SummonsDocumentContent.Builder summonsDocumentContent = summonsDocumentContent();

        final SummonsType summonsRequired = defendantRequest.getSummonsRequired();
        summonsDocumentContent.withSubTemplateName(summonsRequired.name());
        if (FIRST_HEARING == summonsRequired) {
            summonsDocumentContent.withType(getSummonsCode(prosecutionCaseQueried.getSummonsCode()).getSubType());
        } else {
            summonsDocumentContent.withType(summonsRequired.toString());
        }

        optionalLjaDetails.ifPresent(ljaDetails -> {
            final String ljaName = emptyIfBlank(ljaDetails.getLjaName());
            final String ljaNameWelsh = defaultIfBlank(ljaDetails.getWelshLjaName(), ljaName);
            summonsDocumentContent.withLjaCode(emptyIfBlank(ljaDetails.getLjaCode()));
            summonsDocumentContent.withLjaName(ljaName);
            summonsDocumentContent.withLjaNameWelsh(ljaNameWelsh);
        });

        summonsDocumentContent.withCaseReference(extractCaseReference(prosecutionCaseQueried.getProsecutionCaseIdentifier()));

        summonsDocumentContent.withIssueDate(LocalDate.now());

        final SummonsDefendant summonsDefendant = extractSummonsDefendant(defendantQueried);
        summonsDocumentContent.withDefendant(summonsDefendant);

        summonsDocumentContent.withOffences(extractOffences(jsonEnvelope, defendantQueried, prosecutionCaseQueried));

        final SummonsAddressee summonsAddressee = summonsAddressee().withName(summonsDefendant.getName()).withAddress(summonsDefendant.getAddress()).build();
        summonsDocumentContent.withAddressee(summonsAddressee);

        final UUID roomId = summonsDataPrepared.getSummonsData().getCourtCentre().getRoomId();
        final ZonedDateTime hearingDateTime = summonsDataPrepared.getSummonsData().getHearingDateTime();
        final SummonsHearingCourtDetails summonsHearingCourtDetails = getSummonsHearingDetails(courtCentreJson, roomId, hearingDateTime);
        summonsDocumentContent.withHearingCourtDetails(summonsHearingCourtDetails);

        summonsDocumentContent.withProsecutor(summonsProsecutor);

        final SummonsApprovedOutcome summonsApprovedOutcome = defendantRequest.getSummonsApprovedOutcome();
        if (nonNull(summonsApprovedOutcome)) {
            summonsDocumentContent.withProsecutorCosts(emptyIfBlank(summonsApprovedOutcome.getProsecutorCost()));
            summonsDocumentContent.withPersonalService(summonsApprovedOutcome.getPersonalService());
        }
        summonsDocumentContent.withStatementOfFacts(emptyIfBlank(prosecutionCaseQueried.getStatementOfFacts()));
        summonsDocumentContent.withStatementOfFactsWelsh(emptyIfBlank(prosecutionCaseQueried.getStatementOfFactsWelsh()));

        if (SJP_REFERRAL == summonsRequired) {
            final UUID referralReasonId = nonNull(defendantRequest.getReferralReason()) ? defendantRequest.getReferralReason().getId() : null;
            if (nonNull(referralReasonId)) {
                final Optional<JsonObject> referralReasonsJsonOptional = referenceDataService.getReferralReasonByReferralReasonId(jsonEnvelope, referralReasonId, requester);
                final JsonObject referralReasonsJson = referralReasonsJsonOptional.orElseThrow(IllegalArgumentException::new);
                summonsDocumentContent.withReferralContent(populateSummonsReferral(referralReasonsJson));
                }
            }
        return summonsDocumentContent.build();
    }


    private List<SummonsOffence> extractOffences(final JsonEnvelope jsonEnvelope, final Defendant defendant, final ProsecutionCase prosecutionCase) {
        final List<SummonsOffence> summonsOffences = newArrayList();
        final List<Offence> offencesFromDefendant = defendant.getOffences();
        final List<String> cjsOffenceCodes = offencesFromDefendant.stream().map(Offence::getOffenceCode).collect(toList());
        final Optional<String> sowRef = getSowRef(prosecutionCase);
        final Optional<List<JsonObject>> offencesPayloadFromRefData = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(cjsOffenceCodes, jsonEnvelope, requester, sowRef);
        offencesFromDefendant.forEach(offenceFromDefendant -> {

            JsonObject offenceForCjsCodeFromRefData = EMPTY_JSON_OBJECT;
            if (offencesPayloadFromRefData.isPresent()) {
                offenceForCjsCodeFromRefData = offencesPayloadFromRefData.get()
                        .stream().filter(o -> o.containsKey(CJS_OFFENCE_CODE) && o.getString(CJS_OFFENCE_CODE).equalsIgnoreCase(offenceFromDefendant.getOffenceCode()))
                        .findFirst()
                        .orElse(EMPTY_JSON_OBJECT);
            }
            summonsOffences.add(extractOffence(offenceForCjsCodeFromRefData, offenceFromDefendant));
        });

        return summonsOffences;
    }

    private SummonsOffence extractOffence(final JsonObject offenceForCjsCodeFromRefData, final Offence offenceFromDefendant) {
        final String offenceCjsCode = offenceFromDefendant.getOffenceCode();
        final String offenceTitle = emptyIfBlank(offenceFromDefendant.getOffenceTitle());
        final String offenceLegislation = emptyIfBlank(offenceFromDefendant.getOffenceLegislation());
        final String offenceWording = emptyIfBlank(offenceFromDefendant.getWording());
        final SummonsOffence.Builder builder = summonsOffence()
                .withOffenceTitle(offenceTitle)
                .withOffenceLegislation(offenceLegislation)
                .withWording(offenceWording);

        final String welshTitle = defaultIfBlank(offenceForCjsCodeFromRefData.getString(WELSH_OFFENCE_TITLE, EMPTY), offenceTitle);
        final String welshLegislation = defaultIfBlank(offenceForCjsCodeFromRefData.getString(LEGISLATION_WELSH, EMPTY), offenceLegislation);
        // get wording from the offenceQueried on the defendant from viewstore
        final String welshWording = defaultIfBlank(offenceFromDefendant.getWordingWelsh(), offenceFromDefendant.getWording());

        builder.withOffenceTitleWelsh(welshTitle)
                .withOffenceLegislationWelsh(welshLegislation)
                .withWordingWelsh(welshWording)
                .withDvlaCode(offenceForCjsCodeFromRefData.getString(DVLA_CODE, EMPTY))
                .withOffenceCode(offenceCjsCode)
                .withIsEndorsable(offenceForCjsCodeFromRefData.getBoolean(ENDORSABLE_FLAG, false));
        return builder.build();
    }

    private SummonsDefendant extractSummonsDefendant(final Defendant defendantQueried) {
        if (null != defendantQueried.getPersonDefendant()) {
            final Person personDetails = defendantQueried.getPersonDefendant().getPersonDetails();
            if (null == personDetails) {
                return summonsDefendant().build();
            }

            final String dateOfBirth = nonNull(personDetails.getDateOfBirth()) ? personDetails.getDateOfBirth().toString() : EMPTY;
            return summonsDefendant()
                    .withName(getFullName(personDetails.getFirstName(), personDetails.getMiddleName(), personDetails.getLastName()))
                    .withDateOfBirth(dateOfBirth)
                    .withAddress(populateSummonsAddress(personDetails.getAddress()))
                    .build();
        }

        if (null != defendantQueried.getLegalEntityDefendant()) {
            final String organisationName = nonNull(defendantQueried.getLegalEntityDefendant().getOrganisation()) ? defendantQueried.getLegalEntityDefendant().getOrganisation().getName() : EMPTY;
            return summonsDefendant()
                    .withName(organisationName)
                    .withAddress(populateSummonsAddress(defendantQueried.getLegalEntityDefendant().getOrganisation().getAddress()))
                    .build();
        }

        return summonsDefendant().build();
    }

    private String extractCaseReference(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isNotBlank(prosecutionCaseIdentifier.getProsecutionAuthorityReference()) ? prosecutionCaseIdentifier.getProsecutionAuthorityReference() :
                prosecutionCaseIdentifier.getCaseURN();
    }

    private ReferralSummonsDocumentContent populateSummonsReferral(final JsonObject referralReason) {
        return referralSummonsDocumentContent()
                .withId(UUID.fromString(referralReason.getString("id")))
                .withReferralReason(referralReason.getString("reason", EMPTY))
                .withReferralReasonWelsh(referralReason.getString("welshReason", EMPTY))
                .withReferralText(referralReason.getString("subReason", EMPTY))
                .withReferralTextWelsh(referralReason.getString("welshSubReason", EMPTY))
                .withSummonsWording(referralReason.getString("summonsWording", EMPTY))
                .withSummonsWordingWelsh(referralReason.getString("summonsWordingWelsh", EMPTY))
                .build();
    }

    private static Optional<String> getSowRef(final ProsecutionCase prosecutionCase) {
        boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();
        return isCivil ? Optional.of(SOW_REF_VALUE) : Optional.empty();
    }

}
