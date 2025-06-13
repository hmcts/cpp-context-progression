package uk.gov.moj.cpp.progression.aggregate.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;
import static java.util.Objects.isNull;

import java.util.Collection;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.JudicialResultCategory.FINAL;

/**
 * Helper class to determine if court application proceedings are concluded.
 * Handles different types of applications (Appeals, Statutory Declarations, Reopen Cases)
 * and their respective result types.
 */
public class ApplicationProceedingsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProceedingsHelper.class);

    // Result Type Constants


    // Result Type Sets for Appeal against Conviction
    public static final Set<UUID> APPEAL_CONVICTION_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.APA,
            ResultConstants.AW,
            ResultConstants.AACD,
            ResultConstants.AACA
    );
    public static final Set<UUID> APPEAL_CONVICTION_NO_CONCLUSION = Set.of(
            ResultConstants.ASV
    );

    // Result Type Sets for Appeal against Sentence
    public static final Set<UUID> APPEAL_SENTENCE_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.APA,
            ResultConstants.AW,
            ResultConstants.AASD
    );
    public static final Set<UUID> APPEAL_SENTENCE_NO_CONCLUSION = Set.of(
            ResultConstants.AASA
    );

    // Result Type Sets for Combined Appeals
    public static final Set<UUID> APPEAL_COMBINED_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.APA,
            ResultConstants.AW,
            ResultConstants.ACSD,
            ResultConstants.AACA
    );
    public static final Set<UUID> APPEAL_COMBINED_NO_CONCLUSION = Set.of(
            ResultConstants.ASV
    );

    // Result Type Sets for Statutory Declaration
    public static final Set<UUID> STATUTORY_DECLARATION_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.RFSD,
            ResultConstants.WDRN,
            ResultConstants.DISM
    );
    public static final Set<UUID> STATUTORY_DECLARATION_NO_CONCLUSION = Set.of();

    // Result Type Sets for Reopen Case
    public static final Set<UUID> REOPEN_CASE_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.RFSD,
            ResultConstants.WDRN
    );

    public static final Set<UUID> REOPEN_CASE_NO_CONCLUSION = Set.of();

    // Result Type Sets for Breach Applications
    public static final Set<UUID> BREACH_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.OREV,
            ResultConstants.OTC,
            ResultConstants.DISM,
            ResultConstants.WDRN
    );
    public static final Set<UUID> BREACH_NO_CONCLUSION = Set.of();

    // Result Type Sets for Confiscation Order
    public static final Set<UUID> CONFISCATION_CONCLUDE_IMMEDIATELY = Set.of(
            ResultConstants.RFSD, ResultConstants.WDRN
    );
    public static final Set<UUID> CONFISCATION_NO_CONCLUSION = Set.of();

    // Map to store UUID to Result Code mapping
    public static final Map<UUID, String> RESULT_CODE_MAP = Map.ofEntries(

            // Conviction, Sentence and Conviction and Sentence Shared
            Map.entry(ResultConstants.APA, ResultCodeConstants.APA_CODE),
            Map.entry(ResultConstants.AW, ResultCodeConstants.AW_CODE),

            // Conviction
            Map.entry(ResultConstants.AACD, ResultCodeConstants.AACD_CODE),
            Map.entry(ResultConstants.ASV, ResultCodeConstants.ASV_CODE),

            // Sentence
            Map.entry(ResultConstants.AASD, ResultCodeConstants.AASD_CODE),
            Map.entry(ResultConstants.SV, ResultCodeConstants.SV_CODE),
            Map.entry(ResultConstants.AASA, ResultCodeConstants.AASA_CODE),

            // Combined
            Map.entry(ResultConstants.ACSD, ResultCodeConstants.ACSD_CODE),
            Map.entry(ResultConstants.AACA, ResultCodeConstants.AACA_CODE),

            // Statutory and Reopen Shared
            Map.entry(ResultConstants.RFSD, ResultCodeConstants.RFSD_CODE),
            Map.entry(ResultConstants.G, ResultCodeConstants.G_CODE),
            Map.entry(ResultConstants.WDRN, ResultCodeConstants.WDRN_CODE),

            // Statutory Declaration
            Map.entry(ResultConstants.STDEC, ResultCodeConstants.STDEC_CODE),
            Map.entry(ResultConstants.DISM, ResultCodeConstants.DISM_CODE),

            // Reopen Case
            Map.entry(ResultConstants.ROPENED, ResultCodeConstants.ROPENED_CODE),

            // Breach Results
            Map.entry(ResultConstants.OREV, ResultCodeConstants.OREV_CODE),
            Map.entry(ResultConstants.BRO, ResultCodeConstants.BRO_CODE),
            Map.entry(ResultConstants.OTC, ResultCodeConstants.OTC_CODE),

            // Confiscation Order Results
            Map.entry(ResultConstants.CONFAA, ResultCodeConstants.CONFAA_CODE)
    );
    public static final String UNKNOWN_RESULT = "UNKNOWN_RESULT";

    /**
     * Determines if the court application proceedings are concluded.
     *
     * @param application The court application to check
     * @return The court application with proceedings concluded status updated
     */
    public static CourtApplication determineApplicationProceedingsConcluded(CourtApplication application) {
        if (isNull(application)) {
            LOGGER.warn("Application is null");
            return null;
        }

        final List<ApplicationResult> resultList = determineProceedingsConcluded(application);

        boolean concluded = resultList.stream()
                .map(ApplicationResult::concluded)
                .reduce(true, (a, b) -> a && b);

        final String resultCode = determineResultCode(application, resultList);

        return CourtApplication.courtApplication()
                .withValuesFrom(application)
                .withProceedingsConcluded(concluded)
                .withApplicationResultCodeForLaa(resultCode)
                .build();
    }

    private static String determineResultCode(final CourtApplication application, final List<ApplicationResult> resultList) {
        String resultCode = resultList.stream()
                .map(ApplicationResult::getResultCode)
                .filter(code -> !code.equals(UNKNOWN_RESULT))
                .collect(Collectors.joining(" & "));

        if(isEmpty(application.getJudicialResults())){
            resultCode = null;
        } else if (resultCode.isEmpty()) {
            resultCode = UNKNOWN_RESULT;
        }
        return resultCode;
    }

    private static List<ApplicationResult> determineProceedingsConcluded(CourtApplication application) {
        if (isNull(application.getType())) {
            LOGGER.warn("Application type is null for application ID: {}", application.getId());
            return List.of(new ApplicationResult(false, UNKNOWN_RESULT));
        }

        final List<UUID> resultTypeIds = getJudicialResultTypeIds(application);
        final List<ApplicationResult> results = new ArrayList<>();

        if(isNotEmpty(resultTypeIds)){
            handleResultingApplicationAndOffences(application, resultTypeIds, results);
        } else {
            //if resultTypeIds is empty, application results done in previous hearing. Subsequent hearing has only offence level result.
            //hence need to check if any offence result is finalised. Then LAA application outcome should be true.
            handleResultingOffencesOnly(application, results);
        }

        return results.isEmpty() ? List.of(new ApplicationResult(false, UNKNOWN_RESULT)) : results;
    }

    private static void handleResultingOffencesOnly(final CourtApplication application, final List<ApplicationResult> results) {
        results.add(new ApplicationResult(
                hasAllOffencesFinalResults(application),
                null
        ));
    }

    private static void handleResultingApplicationAndOffences(final CourtApplication application, final List<UUID> resultTypeIds, final List<ApplicationResult> results) {
        if (isAppealAgainstConviction(application)) { // Appeal against Conviction
            results.addAll(checkResults(application, resultTypeIds,
                    APPEAL_CONVICTION_CONCLUDE_IMMEDIATELY,
                    APPEAL_CONVICTION_NO_CONCLUSION));
        } else if (isAppealAgainstSentence(application)) { // Appeal against Sentence
            results.addAll(checkResults(application, resultTypeIds,
                    APPEAL_SENTENCE_CONCLUDE_IMMEDIATELY,
                    APPEAL_SENTENCE_NO_CONCLUSION));
        } else if (isAppealAgainstConvictionAndSentence(application)) { //Appeal against Conviction and sentence
            results.addAll(checkResults(application, resultTypeIds,
                    APPEAL_COMBINED_CONCLUDE_IMMEDIATELY,
                    APPEAL_COMBINED_NO_CONCLUSION));
        } else if (isStatutoryDeclaration(application)) { // Statutory Declaration
            results.addAll(checkResults(application, resultTypeIds,
                    STATUTORY_DECLARATION_CONCLUDE_IMMEDIATELY,
                    STATUTORY_DECLARATION_NO_CONCLUSION));
        } else if (isReopenCase(application)) { // Re-Open
            results.addAll(checkResults(application, resultTypeIds,
                    REOPEN_CASE_CONCLUDE_IMMEDIATELY,
                    REOPEN_CASE_NO_CONCLUSION));
        } else if (isBreachApplication(application)) {
            results.addAll(checkResults(application, resultTypeIds,
                    BREACH_CONCLUDE_IMMEDIATELY,
                    BREACH_NO_CONCLUSION));
        } else if (isConfiscationOrder(application)) {
            results.addAll(checkResults(application, resultTypeIds,
                    CONFISCATION_CONCLUDE_IMMEDIATELY,
                    CONFISCATION_NO_CONCLUSION));
        }
    }

    private static List<ApplicationResult> checkResults(
            CourtApplication application,
            List<UUID> resultTypeIds,
            Set<UUID> resultsCheckForOffencesNotRequired,
            Set<UUID> resultsCheckForOffencesRequired) {

        List<ApplicationResult> specialCaseResults = handleSpecialCases(application, resultTypeIds);
        if (!specialCaseResults.isEmpty()) {
            return specialCaseResults;
        }

        return handleRegularCases(application, resultTypeIds, resultsCheckForOffencesNotRequired, resultsCheckForOffencesRequired);
    }

    private static List<ApplicationResult> handleSpecialCases(CourtApplication application, List<UUID> resultTypeIds) {
        if (isAppealAgainstSentence(application)) {
            return handleAppealAgainstSentence(application, resultTypeIds);
        }
        if (isAppealAgainstConvictionAndSentence(application)) {
            return handleAppealAgainstConvictionAndSentence(application, resultTypeIds);
        }
        if (isStatutoryDeclaration(application)) {
            return handleStatutoryDeclaration(application, resultTypeIds);
        }
        if (isReopenCase(application)) {
            return handleReopenCase(application, resultTypeIds);
        }
        if (isBreachApplication(application)) {
            return handleBreachType(application, resultTypeIds);
        }
        if (isConfiscationOrder(application)) {
            return handleConfiscationOrder(resultTypeIds);
        }
        return List.of();
    }

    private static List<ApplicationResult> handleAppealAgainstSentence(
            CourtApplication application,
            List<UUID> resultTypeIds) {

        if (!resultTypeIds.contains(ResultConstants.AASD)) {
            return List.of();
        }

        boolean hasSentenceVaried = resultTypeIds.contains(ResultConstants.SV);
        if (hasSentenceVaried) {
            return List.of(new ApplicationResult(
                hasAllOffencesFinalResults(application),
                    ResultCodeConstants.AASD_CODE + " & " + ResultCodeConstants.SV_CODE
            ));
        }

        return List.of(new ApplicationResult(true, ResultCodeConstants.AASD_CODE));
    }

    private static List<ApplicationResult> handleAppealAgainstConvictionAndSentence(
            CourtApplication application,
            List<UUID> resultTypeIds) {
        boolean hasRequiredResults = resultTypeIds.contains(ResultConstants.AACD) &&
                resultTypeIds.contains(ResultConstants.AASA);

        if (!hasRequiredResults) {
            return List.of();
        }

        return List.of(new ApplicationResult(
            hasAllOffencesFinalResults(application),
                ResultCodeConstants.AACD_CODE + " & " + ResultCodeConstants.AASA_CODE
        ));
    }

    private static List<ApplicationResult> handleStatutoryDeclaration(
            CourtApplication application,
            List<UUID> resultTypeIds) {

        boolean hasRequiredResults = resultTypeIds.contains(ResultConstants.G) &&
                resultTypeIds.contains(ResultConstants.STDEC);

        if (!hasRequiredResults) {
            return List.of();
        }

        return List.of(new ApplicationResult(
            hasAllOffencesFinalResults(application),
                ResultCodeConstants.G_CODE + " & " + ResultCodeConstants.STDEC_CODE
        ));
    }

    private static List<ApplicationResult> handleReopenCase(
            CourtApplication application,
            List<UUID> resultTypeIds) {

        boolean hasRequiredResults = resultTypeIds.contains(ResultConstants.G) &&
                resultTypeIds.contains(ResultConstants.ROPENED);

        if (!hasRequiredResults) {
            return List.of();
        }

        return List.of(new ApplicationResult(
            hasAllOffencesFinalResults(application),
                ResultCodeConstants.G_CODE + " & " + ResultCodeConstants.ROPENED_CODE
        ));
    }

    private static List<ApplicationResult> handleConfiscationOrder(
            List<UUID> resultTypeIds) {

        // Check for special cases: if both CONFAA and Granted are present
        boolean hasConfaa = resultTypeIds.contains(ResultConstants.CONFAA);
        boolean hasConviction = resultTypeIds.contains(ResultConstants.G);

        if (hasConfaa && hasConviction) {
            return List.of(new ApplicationResult(
                    true,
                    ResultCodeConstants.CONFAA_CODE + " & " + ResultCodeConstants.G_CODE
            ));
        } else return List.of();

    }
    private static List<ApplicationResult> handleBreachType(
            CourtApplication application,
            List<UUID> resultTypeIds) {

        // Check for special cases: if both OREV and BRO are present
        boolean hasOrev = resultTypeIds.contains(ResultConstants.OREV);
        boolean hasBro = resultTypeIds.contains(ResultConstants.BRO);

        if (hasOrev && hasBro) {
            return List.of(new ApplicationResult(
                    hasAllOffencesFinalResults(application),
                    ResultCodeConstants.OREV_CODE + " & " + ResultCodeConstants.BRO_CODE
            ));
        } else return List.of();

    }
    private static List<ApplicationResult> handleRegularCases(
            CourtApplication application,
            List<UUID> resultTypeIds,
            Set<UUID> resultsCheckForOffencesNotRequired,
            Set<UUID> resultsCheckForOffencesRequired) {

        List<ApplicationResult> results = resultTypeIds.stream()
                .map(id -> createApplicationResult(id, application, resultsCheckForOffencesNotRequired, resultsCheckForOffencesRequired))
                .toList();

        if (results.isEmpty()) {
            LOGGER.info("No matching result type found for application ID: {}", application.getId());
            return List.of(new ApplicationResult(false, UNKNOWN_RESULT));
        }

        return results;
    }

    private static ApplicationResult createApplicationResult(
            UUID id,
            CourtApplication application,
            Set<UUID> resultsCheckForOffencesNotRequired,
            Set<UUID> resultsCheckForOffencesRequired) {

        String resultCode = RESULT_CODE_MAP.getOrDefault(id, UNKNOWN_RESULT);
        if (UNKNOWN_RESULT.equals(resultCode)) {
            LOGGER.warn("Unknown result type ID found: {} for application ID: {}", id, application.getId());
        }

        boolean concluded = resultsCheckForOffencesNotRequired.contains(id) ||
                (resultsCheckForOffencesRequired.contains(id) && hasAllOffencesFinalResults(application));

        return new ApplicationResult(concluded, resultCode);
    }

    private static boolean isAppealType(final CourtApplication application, final String expectedTypeCode) {
        return Optional.ofNullable(application)
                .map(CourtApplication::getType)
                .map(CourtApplicationType::getCode)
                .map(code -> code.equals(expectedTypeCode))
                .orElse(false);
    }

    private static boolean isAppealAgainstConviction(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID);
    }

    private static boolean isAppealAgainstSentence(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID);
    }

    private static boolean isAppealAgainstConvictionAndSentence(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID);
    }

    private static boolean isStatutoryDeclaration(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_STATUTORY_DECLARATION_ID);
    }

    private static boolean isReopenCase(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_REOPEN_CASE_ID);
    }

    private static boolean isBreachApplication(CourtApplication application) {
        final String code = application.getType().getCode();
        return code.equals(ApplicationTypeConstants.APP_TYPE_BREACH_COMMUNITY_ORDER_ID) ||
               code.equals(ApplicationTypeConstants.APP_TYPE_BREACH_ENGAGEMENT_SUPPORT_ORDER_ID) ||
               code.equals(ApplicationTypeConstants.APP_TYPE_BREACH_YRO_INTENSIVE_ID) ||
               code.equals(ApplicationTypeConstants.APP_TYPE_BREACH_YRO_ID) ||
               code.equals(ApplicationTypeConstants.APP_TYPE_BREACH_REPARATION_ORDER_ID);
    }

    private static boolean isConfiscationOrder(CourtApplication application) {
        return isAppealType(application, ApplicationTypeConstants.APP_TYPE_CONFISCATION_ORDER_ID);
    }

    private static List<UUID> getJudicialResultTypeIds(CourtApplication application) {
        return Optional.ofNullable(application.getJudicialResults())
                .map(results -> results.stream()
                        .filter(ApplicationProceedingsHelper::isRootJudicialResult)
                        .map(JudicialResult::getJudicialResultTypeId)
                        .filter(Objects::nonNull)
                        .toList())
                .orElse(Collections.emptyList());
    }

    private static boolean isRootJudicialResult(final JudicialResult judicialResult) {
        return judicialResult.getJudicialResultTypeId().equals(judicialResult.getRootJudicialResultTypeId());
    }

    private static boolean hasAllOffencesFinalResults(CourtApplication application) {
        return Optional.ofNullable(application.getCourtApplicationCases())
                .map(cases -> cases.stream()
                        .flatMap(cac -> Optional.ofNullable(cac.getOffences()).stream().flatMap(Collection::stream))
                        .allMatch(ApplicationProceedingsHelper::offenceHasFinalResults))
                .orElse(false);
    }


    private static boolean offenceHasFinalResults(Offence offence) {
        return isNotEmpty(offence.getJudicialResults()) &&
                offence.getJudicialResults()
                        .stream()
                        .filter(Objects::nonNull)
                        .allMatch(ApplicationProceedingsHelper::isFinalResult);
    }

    private static boolean isFinalResult(JudicialResult result) {
        return FINAL.equals(result.getCategory());
    }

    private record ApplicationResult(boolean concluded, String resultCode) {
        public String getResultCode() {
            return resultCode != null ? resultCode : UNKNOWN_RESULT;
        }
    }
}