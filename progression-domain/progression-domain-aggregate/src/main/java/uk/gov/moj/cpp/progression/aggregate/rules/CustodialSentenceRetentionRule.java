package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyPriorityHelper.periodToDays;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.CUSTODIAL;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Offence;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustodialSentenceRetentionRule implements RetentionRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustodialSentenceRetentionRule.class);
    private static final String DEFAULT_CUSTODIAL_SENTENCE = "7Y0M0D";
    private static final String PROMPT_VALUE_REGEX = "\\d(\\d)*\\s[YyMmWwDd]";
    static final String TOTAL_CUSTODIAL_PERIOD_PROMPT = "totalCustodialPeriod";
    static final UUID TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID = fromString("b2cf5a1e-8539-45a1-a287-4be5094a0e73");
    static final UUID TIMP_RESULT_DEFINITION_ID = fromString("6cb15971-c945-4398-b7c9-3f8b743a4de3");
    private HearingInfo hearingInfo;
    private final List<DefendantJudicialResult> defendantJudicialResults;
    private final List<Offence> defendantsOffences;
    private String custodialPeriod;

    public CustodialSentenceRetentionRule(final HearingInfo hearingInfo, final List<DefendantJudicialResult> defendantJudicialResults, final List<Offence> defendantsOffences) {
        this.hearingInfo = hearingInfo;
        this.defendantJudicialResults = unmodifiableList(nonNull(defendantJudicialResults) ? defendantJudicialResults : emptyList());
        this.defendantsOffences = unmodifiableList(nonNull(defendantsOffences) ? defendantsOffences : emptyList());
    }

    @Override
    public boolean apply() {
        if (isEmpty(defendantJudicialResults) && isEmpty(defendantsOffences)) {
            return false;
        }

        final List<DefendantJudicialResult> defendantJRsWithTimp = this.defendantJudicialResults.stream()
                .filter(djr -> nonNull(djr.getJudicialResult()) && nonNull(djr.getJudicialResult().getJudicialResultTypeId()))
                .filter(djr -> TIMP_RESULT_DEFINITION_ID.equals(djr.getJudicialResult().getJudicialResultTypeId()))
                .collect(Collectors.toList());

        if (!isEmpty(defendantJRsWithTimp)) {
            final List<JudicialResultPrompt> offenceJudicialResultPrompts = defendantsOffences.stream()
                    .filter(o -> nonNull(o.getJudicialResults()))
                    .flatMap(o -> o.getJudicialResults().stream())
                    .filter(jrs -> nonNull(jrs.getJudicialResultPrompts()))
                    .flatMap(jrs -> jrs.getJudicialResultPrompts().stream())
                    .collect(Collectors.toList());

            final List<String> totalCustodialPeriodList = calculateCustodialSentence(defendantJRsWithTimp, offenceJudicialResultPrompts);
            this.custodialPeriod = getMaxCustodialPeriodOrDefault(totalCustodialPeriodList);

            LOGGER.info("custodialPeriod calculated={}", custodialPeriod);
            return !isEmpty(totalCustodialPeriodList);
        }

        return false;
    }

    @Override
    public RetentionPolicy getPolicy() {
        final RetentionPolicy calculatedRetentionPolicy = new RetentionPolicy(CUSTODIAL, custodialPeriod, hearingInfo);
        final RetentionPolicy defaultRetentionPolicy = new RetentionPolicy(CUSTODIAL, DEFAULT_CUSTODIAL_SENTENCE, hearingInfo);

        return calculatedRetentionPolicy.getPeriodDays() > defaultRetentionPolicy.getPeriodDays()
                ? calculatedRetentionPolicy : defaultRetentionPolicy;
    }

    private List<String> calculateCustodialSentence(final List<DefendantJudicialResult> jrWithTimp, final List<JudicialResultPrompt> offenceJudicialResultPrompts) {
        final List<JudicialResultPrompt> allPrompts = new ArrayList<>();
        if (!isEmpty(jrWithTimp)) {
            final List<JudicialResultPrompt> timpJudicialResultPrompts = jrWithTimp.stream()
                    .filter(djr -> nonNull(djr.getJudicialResult()) && nonNull(djr.getJudicialResult().getJudicialResultPrompts()))
                    .flatMap(djr -> djr.getJudicialResult().getJudicialResultPrompts().stream())
                    .collect(Collectors.toList());

            if (!isEmpty(timpJudicialResultPrompts)) {
                allPrompts.addAll(timpJudicialResultPrompts);
            }
        }
        if (!isEmpty(offenceJudicialResultPrompts)) {
            allPrompts.addAll(offenceJudicialResultPrompts);
        }
        final LocalDate orderedDate = jrWithTimp.get(0).getJudicialResult().getOrderedDate();
        return allPrompts.stream()
                .filter(jrp -> TOTAL_CUSTODIAL_PERIOD_PROMPT_TYPE_ID.equals(jrp.getJudicialResultPromptTypeId()))
                .map(JudicialResultPrompt::getValue)
                .filter(Objects::nonNull)
                .map(p -> getCustodialPeriodStr(p, orderedDate))
                .collect(Collectors.toList());
    }

    private String getMaxCustodialPeriodOrDefault(final List<String> totalCustodialPeriodList) {
        final Map<Integer, String> custodialPeriodDaysMap = new HashMap<>();
        totalCustodialPeriodList.forEach(tcp -> custodialPeriodDaysMap.put(periodToDays(tcp), tcp));
        final Optional<Integer> maxPeriodOptional = custodialPeriodDaysMap.keySet().stream().max(Comparator.naturalOrder());

        return maxPeriodOptional.isPresent() ? custodialPeriodDaysMap.get(maxPeriodOptional.get()) : DEFAULT_CUSTODIAL_SENTENCE;
    }

    private String getCustodialPeriodStr(final String promptValue, final LocalDate orderedDate) {
        final Matcher matcher = compile(PROMPT_VALUE_REGEX).matcher(promptValue);
        final List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group().replaceAll("\\s", ""));
        }
        return formatDate(matches, orderedDate);
    }

    private String formatDate(List<String> matches, final LocalDate orderedDate) {
        final Map<String, AtomicInteger> durationMap = new HashMap<>();
        matches.forEach(match -> {
            final String unit = match.substring(match.length() - 1);
            final int duration = Integer.parseInt(match.substring(0, match.length() - 1));

            durationMap.computeIfAbsent(unit, k -> new AtomicInteger()).set(duration);
        });
        final LocalDate futureDate = orderedDate
                .plusYears(durationMap.getOrDefault("Y", new AtomicInteger()).get())
                .plusMonths(durationMap.getOrDefault("M", new AtomicInteger()).get())
                .plusWeeks(durationMap.getOrDefault("W", new AtomicInteger()).get())
                .plusDays(durationMap.getOrDefault("D", new AtomicInteger()).get());
        final Period period = Period.between(orderedDate, futureDate);
        return String.format("%sY%sM%sD", period.getYears(), period.getMonths(), period.getDays());
    }
}
