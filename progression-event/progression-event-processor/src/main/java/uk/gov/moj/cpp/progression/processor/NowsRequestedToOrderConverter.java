package uk.gov.moj.cpp.progression.processor;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Now;
import uk.gov.justice.core.courts.NowType;
import uk.gov.justice.core.courts.NowVariant;
import uk.gov.justice.core.courts.NowVariantAddressee;
import uk.gov.justice.core.courts.NowVariantDefendant;
import uk.gov.justice.core.courts.NowVariantResult;
import uk.gov.justice.core.courts.NowVariantResultText;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ResultPrompt;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.moj.cpp.progression.event.nows.order.Address;
import uk.gov.moj.cpp.progression.event.nows.order.Cases;
import uk.gov.moj.cpp.progression.event.nows.order.Defendant;
import uk.gov.moj.cpp.progression.event.nows.order.DefendantCaseOffences;
import uk.gov.moj.cpp.progression.event.nows.order.DefendantCaseResults;
import uk.gov.moj.cpp.progression.event.nows.order.FinancialOrderDetails;
import uk.gov.moj.cpp.progression.event.nows.order.NextHearingCourtDetails;
import uk.gov.moj.cpp.progression.event.nows.order.NowResultDefinitionsText;
import uk.gov.moj.cpp.progression.event.nows.order.NowsDocumentOrder;
import uk.gov.moj.cpp.progression.event.nows.order.OrderAddressee;
import uk.gov.moj.cpp.progression.event.nows.order.Prompts;
import uk.gov.moj.cpp.progression.event.nows.order.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;


@SuppressWarnings({"squid:S1188", "squid:S1192", "squid:S00112", "squid:S1125"})
public class NowsRequestedToOrderConverter {

    private static final String CASE = "CASE";
    private static final String OFFENCE = "OFFENCE";
    private static final String DEFENDANT = "DEFENDANT";

    private NowsRequestedToOrderConverter() {
    }

    public static Map<NowsDocumentOrder, NowsNotificationDocumentState> convert(final CreateNowsRequest nowsRequested) {
        final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrders = new HashMap<>();

        final Boolean isCrownCourt = JurisdictionType.CROWN.equals(nowsRequested.getHearing().getJurisdictionType());

        nowsRequested.getNows().forEach(selectedNow -> {

            final NowType matchingNowType = getNowType(nowsRequested, selectedNow);

            for (final NowVariant selectedNowMaterial : selectedNow.getRequestedMaterials()) {
                mapVariant(nowsRequested, selectedNowMaterial, selectedNow, matchingNowType, isCrownCourt, nowsDocumentOrders);
            }
        });

        return nowsDocumentOrders;
    }

    private static void mapVariant(final CreateNowsRequest nowsRequested, final NowVariant selectedNowMaterial, final Now selectedNow,
                                   final NowType matchingNowType, final boolean isCrownCourt,
                                   final Map<NowsDocumentOrder, NowsNotificationDocumentState> nowsDocumentOrders
    ) {
        final String courtClerkName = format("%s %s", nowsRequested.getCourtClerk().getFirstName(), nowsRequested.getCourtClerk().getLastName());
        final Defendant orderDefendant = getOrderDefendant(selectedNowMaterial.getNowVariantDefendant());
        final List<String> caseUrns = getCaseUrns(nowsRequested, selectedNow);
        final String courtCentreName = nowsRequested.getHearing().getCourtCentre().getName();
        final String defendantName = orderDefendant != null ? orderDefendant.getName() : null;

        final List<Cases> cases = getOrderCases(nowsRequested, selectedNowMaterial);

        final Optional<uk.gov.justice.core.courts.FinancialOrderDetails> financialOrderDetailsOptional = isNull(selectedNow.getFinancialOrders()) ? empty()
                : Optional.of(selectedNow.getFinancialOrders());

        final FinancialOrderDetails financialOrderDetails = getFinancialOrderDetails(financialOrderDetailsOptional.orElse(null), selectedNow.getLjaDetails());

        final Boolean amended = nonNull(selectedNowMaterial.getIsAmended()) ? selectedNowMaterial.getIsAmended() : false;

        final String subTemplateName = matchingNowType.getSubTemplateName();

        final NowsNotificationDocumentState nowsNotificationDocumentState = new NowsNotificationDocumentState()
                .setUsergroups(new ArrayList<>(selectedNowMaterial.getKey().getUsergroups()))
                .setOriginatingCourtCentreId(nowsRequested.getHearing().getCourtCentre().getId())
                .setDefendantName(defendantName)
                .setCourtClerkName(courtClerkName)
                .setCaseUrns(caseUrns)
                .setNowsTypeId(matchingNowType.getId())
                .setJurisdiction(matchingNowType.getJurisdiction())
                .setCourtCentreName(courtCentreName)
                .setOrderName(matchingNowType.getDescription())
                .setPriority(matchingNowType.getPriority())
                .setMaterialId(selectedNowMaterial.getMaterialId())
                .setIsRemotePrintingRequired(selectedNowMaterial.getIsRemotePrintingRequired());

        final NowsDocumentOrder nowsDocumentOrder = NowsDocumentOrder.nowsDocumentOrder()
                .withMaterialId(selectedNowMaterial.getMaterialId())
                .withOrderName(matchingNowType.getDescription())
                .withLjaCode(nonNull(selectedNow.getLjaDetails()) ? selectedNow.getLjaDetails().getLjaCode() : null)
                .withLjaName(nonNull(selectedNow.getLjaDetails()) ? selectedNow.getLjaDetails().getLjaName() : null)
                .withNowText(matchingNowType.getStaticText())
                .withPriority(matchingNowType.getPriority())
                .withCourtCentreName(courtCentreName)
                .withCourtClerkName(courtClerkName)
                .withOrderDate(findOrderDate(nowsRequested.getSharedResultLines(), selectedNowMaterial, selectedNow.getReferenceDate()))
                .withOrderAddressee(getOrderAddressee(selectedNowMaterial.getNowVariantAddressee()))
                .withFinancialOrderDetails(financialOrderDetails)
                .withCaseUrns(caseUrns)
                .withDefendant(orderDefendant)
                .withCases(cases)
                .withIsAmended(amended)
                .withNowResultDefinitionsText(createAdditionalPropertiesForNowResultDefinitionsText(cases))
                .withSubTemplateName(subTemplateName)
                .withNextHearingCourtDetails(createNextHearingCourtDetails(selectedNow.getNextHearingCourtDetails()))
                .withIsCrownCourt(isCrownCourt)
                .build();

        nowsDocumentOrders.put(nowsDocumentOrder, nowsNotificationDocumentState);

    }

    private static NowType getNowType(final CreateNowsRequest nowsRequested, final Now selectedNow) {
        return nowsRequested.getNowTypes().stream()
                .filter(nowType -> nowType.getId().equals(selectedNow.getNowsTypeId()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("invalid now type id supplied."));
    }

    private static NextHearingCourtDetails createNextHearingCourtDetails(final uk.gov.justice.core.courts.NextHearingCourtDetails nextHearingCourtDetails) {

        if (nonNull(nextHearingCourtDetails)) {
            return new NextHearingCourtDetails(
                    nextHearingCourtDetails.getCourtCentre().getName(),
                    nextHearingCourtDetails.getHearingDate(),
                    nextHearingCourtDetails.getHearingTime(),
                    getAddress(nextHearingCourtDetails.getCourtCentre().getAddress()));
        }

        return null;
    }

    private static NowResultDefinitionsText createAdditionalPropertiesForNowResultDefinitionsText(final List<Cases> cases) {

        final NowResultDefinitionsText nowResultDefinitionsText = NowResultDefinitionsText.nowResultDefinitionsText().build();

        final List<Results> results = cases.stream()
                .flatMap(caseOffence -> caseOffence.getDefendantCaseOffences().stream())
                .filter(caseOffence -> nonNull(caseOffence.getResults()))
                .flatMap(defendantCaseOffence -> defendantCaseOffence.getResults().stream())
                .collect(toList());

        final List<DefendantCaseResults> results2 = cases.stream()
                .filter(caseResults -> nonNull(caseResults.getDefendantCaseResults()))
                .flatMap(c -> c.getDefendantCaseResults().stream())
                .collect(toList());

        results.forEach(result -> {
            final Map<String, Object> resultAdditionalProperties = result.getAdditionalProperties();
            final Set<String> keys = resultAdditionalProperties.keySet();
            keys.forEach(key -> {
                        nowResultDefinitionsText.setAdditionalProperty(key, resultAdditionalProperties.get(key));
                        nowResultDefinitionsText.setAdditionalProperty(additionalPropertyLabelKey(key), result.getLabel());
                    }
            );
        });

        results2.forEach(result -> {
            final Map<String, Object> resultAdditionalProperties = result.getAdditionalProperties();
            final Set<String> keys = resultAdditionalProperties.keySet();
            keys.forEach(key ->
            {
                nowResultDefinitionsText.setAdditionalProperty(key, resultAdditionalProperties.get(key));
                nowResultDefinitionsText.setAdditionalProperty(additionalPropertyLabelKey(key), result.getLabel());
            });

        });

        return nowResultDefinitionsText;
    }

    private static String additionalPropertyLabelKey(String key) {
        return ("equivPlea".equals(key) ? "adjournmentReason" : key) + "Label";
    }


    private static FinancialOrderDetails getFinancialOrderDetails(final uk.gov.justice.core.courts.FinancialOrderDetails financialOrdersDetails, final LjaDetails ljaDetails) {
        if (nonNull(financialOrdersDetails)) {
            final FinancialOrderDetails.Builder financialOrderDetailsBuilder =
                    FinancialOrderDetails.financialOrderDetails()
                            .withAccountPaymentReference(financialOrdersDetails.getAccountReference())
                            .withTotalAmountImposed(financialOrdersDetails.getTotalAmountImposed())
                            .withTotalBalance(financialOrdersDetails.getTotalBalance())
                            .withIsCrownCourt(financialOrdersDetails.getIsCrownCourt())
                            .withPaymentTerms(financialOrdersDetails.getPaymentTerms());

            if (ljaDetails != null) {
                financialOrderDetailsBuilder.withAccountingDivisionCode(ljaDetails.getAccountDivisionCode())
                        .withBacsAccountNumber(ljaDetails.getBacsAccountNumber())
                        .withBacsBankName(ljaDetails.getBacsBankName())
                        .withBacsSortCode(ljaDetails.getBacsSortCode())
                        .withEnforcementAddress(getAddress(ljaDetails.getEnforcementAddress()))
                        .withEnforcementEmail(ljaDetails.getEnforcementEmail())
                        .withEnforcementPhoneNumber(ljaDetails.getEnforcementPhoneNumber());
            }
            return financialOrderDetailsBuilder.build();
        }
        return null;
    }

    private static OrderAddressee getOrderAddressee(final NowVariantAddressee nowVariantAddressee) {
        return
                OrderAddressee.orderAddressee()
                        .withName(nowVariantAddressee.getName())
                        .withAddress(getAddress(nowVariantAddressee.getAddress())).build();
    }

    private static Address getAddress(final uk.gov.justice.core.courts.Address address) {
        return Address.address()
                .withLine1(address.getAddress1())
                .withLine2(address.getAddress2())
                .withLine3(address.getAddress3())
                .withLine4(address.getAddress4())
                .withLine5(address.getAddress5())
                .withPostCode(address.getPostcode())
                .build();
    }

    private static <T> T expectOrThrow(T optional, final String message, final Object... args) {
        if (isNull(optional)) {
            throw new RuntimeException(String.format(message, args));
        }
        return optional;
    }


    private static String findOrderDate(List<SharedResultLine> sharedResultLines, NowVariant material, final String referenceDate) {

        if (material.getNowResults() != null) {
            for (final NowVariantResult nowResult : material.getNowResults()) {
                final Optional<String> orderDate = sharedResultLines.stream()
                        .filter(l -> l.getId().equals(nowResult.getSharedResultId()))
                        .map(SharedResultLine::getOrderedDate)
                        .map(StringUtils::defaultString)
                        .findAny();
                if (orderDate.isPresent()) {
                    return orderDate.orElse("");
                }
            }
        }

        if (nonNull(referenceDate)) {
            return referenceDate;
        }

        return EMPTY;
    }

    private static List<ResultPrompt> getMatchingPrompts(NowVariantResult selectedNowResult, SharedResultLine sharedResultLine) {
        if (selectedNowResult.getPromptRefs() == null || sharedResultLine.getPrompts() == null) {
            return emptyList();
        } else {
            final List<UUID> nowResultPromptLabels = new ArrayList<>(selectedNowResult.getPromptRefs());
            return sharedResultLine.getPrompts().stream()
                    .filter(prompt -> nowResultPromptLabels.contains(prompt.getId()))
                    .collect(toList());
        }
    }

    private static String prosecutionCaseRef(final ProsecutionCaseIdentifier id) {
        return isEmpty(id.getCaseURN()) ? id.getProsecutionAuthorityReference() : id.getCaseURN();
    }

    private static List<String> getCaseUrns(CreateNowsRequest nowsRequested, Now now) {
        return nowsRequested.getHearing().getProsecutionCases().stream()
                .filter(isCaseBelongingToDefendant(now.getDefendantId()))
                .map(c -> prosecutionCaseRef(c.getProsecutionCaseIdentifier()))
                .collect(toList());
    }

    private static Predicate<ProsecutionCase> isCaseBelongingToDefendant(UUID defendantId) {
        return c -> c.getDefendants().stream().anyMatch(d -> d.getId().equals(defendantId));
    }

    private static Defendant getOrderDefendant(final NowVariantDefendant nowVariantDefendant) {
        return Defendant.defendant()
                .withName(nowVariantDefendant.getName())
                .withDateOfBirth(nowVariantDefendant.getDateOfBirth())
                .withAddress(getAddress(nowVariantDefendant.getAddress()))
                .build();
    }

    @SuppressWarnings({"squid:S3776", "squid:S134"})
    private static List<Cases> getOrderCases(CreateNowsRequest nowsRequested, NowVariant selectedMaterial) {

        if (isNull(selectedMaterial.getNowResults())) {
            return new ArrayList<>();
        }

        final Map<String, Cases> orderCasesMap = new HashMap<>();

        final List<String> caseRefs = new ArrayList<>();

        final Map<String, Set<DefendantCaseResults>> defendantCaseResultsMap = new HashMap<>();

        final Map<String, Set<DefendantCaseOffences>> defendantCaseOffencesMap = new HashMap<>();

        final Map<UUID, List<Results>> offenceResults = new HashMap<>();

        final Map<String, List<Offence>> caseOffencesMap = new HashMap<>();

        for (final NowVariantResult selectedNowResult : selectedMaterial.getNowResults()) {

            nowsRequested.getSharedResultLines().stream()
                    .filter(sharedResultLine -> sharedResultLine.getId().equals(selectedNowResult.getSharedResultId()))
                    .findAny()
                    .ifPresent(sharedResultLine -> {

                        final List<Prompts> orderPrompts = preparePrompts(selectedNowResult, sharedResultLine);

                        final ProsecutionCase prosecutionCase = nowsRequested.getHearing().getProsecutionCases().stream()
                                .filter(isCaseBelongingToDefendant(expectOrThrow(sharedResultLine.getDefendantId(), "empty defendant id")))
                                .findAny()
                                .orElseThrow(() -> new IllegalArgumentException("invalid data, could not find case for defendant supplied by result line"));

                        final String caseRef = prosecutionCaseRef(prosecutionCase.getProsecutionCaseIdentifier());

                        if (!caseRefs.contains(caseRef)) {
                            caseRefs.add(caseRef);
                        }

                        defendantCaseResultsMap.putIfAbsent(caseRef, new HashSet<>());

                        defendantCaseOffencesMap.putIfAbsent(caseRef, new HashSet<>());

                        if (CASE.equalsIgnoreCase(sharedResultLine.getLevel()) || DEFENDANT.equalsIgnoreCase(sharedResultLine.getLevel())) {

                            final DefendantCaseResults defendantCaseResults = DefendantCaseResults.defendantCaseResults()
                                    .withLabel(sharedResultLine.getLabel())
                                    .withPrompts(orderPrompts)
                                    .build();

                            final NowVariantResultText nowVariantResultTextOptional = selectedNowResult.getNowVariantResultText();

                            if (nonNull(nowVariantResultTextOptional)) {

                                final Map<String, Object> additionalProperties = nowVariantResultTextOptional.getAdditionalProperties();

                                additionalProperties.forEach((k, v) -> defendantCaseResults.getAdditionalProperties().put(k, v));
                            }

                            defendantCaseResultsMap.get(caseRef).add(defendantCaseResults);
                        }

                        if (OFFENCE.equalsIgnoreCase(sharedResultLine.getLevel())) {

                            final Results results = getResults(selectedNowResult, sharedResultLine.getLabel(), orderPrompts);

                            prosecutionCase.getDefendants().stream()
                                    .filter(d -> d.getId().equals(sharedResultLine.getDefendantId()))
                                    .flatMap(d -> d.getOffences().stream())
                                    .filter(o -> o.getId().equals(sharedResultLine.getOffenceId()))
                                    .findAny()
                                    .ifPresent(offence -> {

                                        final List<Offence> offenceList = caseOffencesMap.getOrDefault(caseRef, new ArrayList<>());

                                        offenceList.add(offence);

                                        caseOffencesMap.put(caseRef, offenceList);

                                        final List<Results> resultsList = offenceResults.getOrDefault(offence.getId(), new ArrayList<>());

                                        resultsList.add(results);

                                        offenceResults.put(offence.getId(), resultsList);
                                    });
                        }
                    });
        }

        caseRefs.forEach(caseRef -> {

            final Set<DefendantCaseResults> defendantCaseResults = defendantCaseResultsMap.get(caseRef);

            final Set<DefendantCaseOffences> defendantCaseOffences = defendantCaseOffencesMap.get(caseRef);

            final List<Offence> offences = caseOffencesMap.get(caseRef);
            if (nonNull(offences)) {
                offences.forEach(offence -> defendantCaseOffences.add(DefendantCaseOffences.defendantCaseOffences()
                        .withConvictionDate(offence.getConvictionDate())
                        .withStartDate(offence.getStartDate())
                        .withWording(offence.getWording())
                        .withResults(offenceResults.get(offence.getId()))
                        .build()));
            }
            orderCasesMap.put(caseRef, Cases.cases()
                    .withUrn(caseRef)
                    .withDefendantCaseResults(defendantCaseResults)
                    .withDefendantCaseOffences(defendantCaseOffences).build());
        });

        return new ArrayList<>(orderCasesMap.values());
    }

    private static Results getResults(final NowVariantResult selectedNowResult, final String label, final List<Prompts> orderPrompts) {

        final NowVariantResultText nowVariantResultTextOptional = selectedNowResult.getNowVariantResultText();

        if (nonNull(nowVariantResultTextOptional)) {

            final Map<String, Object> additionalProperties = nowVariantResultTextOptional.getAdditionalProperties();

            final Results results = Results.results()
                    .withLabel(label)
                    .withPrompts(orderPrompts)
                    .build();

            additionalProperties.forEach((k, v) -> results.getAdditionalProperties().put(k, v));

            return results;

        } else {
            return Results.results()
                    .withLabel(label)
                    .withPrompts(orderPrompts)
                    .build();
        }
    }

    private static List<Prompts> preparePrompts(NowVariantResult selectedNowResult, SharedResultLine sharedResultLine) {
        final List<ResultPrompt> nowResultPrompts = getMatchingPrompts(selectedNowResult, sharedResultLine);
        return nowResultPrompts.stream()
                .map(prompt -> new Prompts(prompt.getLabel(), prompt.getValue()))
                .collect(toList());
    }
}
