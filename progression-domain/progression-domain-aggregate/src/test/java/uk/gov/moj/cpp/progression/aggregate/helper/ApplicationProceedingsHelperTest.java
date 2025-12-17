package uk.gov.moj.cpp.progression.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.List.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.moj.cpp.progression.aggregate.helper.ApplicationProceedingsHelper.determineApplicationProceedingsConcluded;
import static uk.gov.moj.cpp.progression.aggregate.helper.ResultConstantsTest.APA;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicationProceedingsHelperTest {

    public static Stream<Arguments> getBreachTypeApplications() {
        return Stream.of(
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID),
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID_2),
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_ENGAGEMENT_SUPPORT_ORDER_ID),
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID),
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID_2),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_SUPERVISION_REQUIREMENTS),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_SUPERVISION_REQUIREMENTS_2),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_YOUTH_REHABILITATION_REQUIREMENTS),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_YOUTH_REHABILITATION_REQUIREMENTS_2),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_POST_CUSTODIAL_SUPERVISION_REQUIREMENTS),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_COMMUNITY_REQUIREMENTS),
                Arguments.of(ApplicationTypeConstantsTest.FAIL_TO_COMPLY_WITH_COMMUNITY_REQUIREMENTS_2),
                Arguments.of(ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID_2)
        );
    }

    // Basic Validation Tests
    @Test
    void shouldHandleNullApplication() {
        CourtApplication result = determineApplicationProceedingsConcluded(null);
        assertNull(result);
    }

    @Test
    void shouldHandleApplicationWithNullType() {
        CourtApplication result = determineApplicationProceedingsConcluded(
                courtApplication()
                        .withId(randomUUID())
                        .build()
        );

        assert result != null;
        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    // Appeal against Conviction Tests
    @Test
    void shouldConcludeWhenAppealAgainstConvictionAbandoned() { //1-1
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionWithdrawn() { //1-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionDismissed() { //1-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(ResultConstantsTest.AACD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConvictionDismissedAndSentenceVaried() { //1-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        ResultConstantsTest.AACD,
                        ResultConstantsTest.ASV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE + " & " + ResultCodeConstantsTest.ASV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForConvictionWhenASVAndNoOffenceResults() { //1-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        ResultConstantsTest.ASV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForConvictionWhenASVAndOffenceResultsNotFinal() { //1-4
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        ResultConstantsTest.ASV
                ),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForConvictionWhenASVAndOffenceResultsFinal() { //1-4
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        ResultConstantsTest.ASV
                ),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAllowed() { //1-5
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(ResultConstantsTest.AACA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACA_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Conviction Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of()
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(randomUUID())
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithInvalidCombination() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        ResultConstantsTest.AACD,
                        ResultConstantsTest.AASA  // Contradictory results - both dismissed and allowed
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE + " & " + ResultCodeConstantsTest.AASA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithSentenceResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(ResultConstantsTest.SV)  // Using sentence result for conviction appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(ResultConstantsTest.OREV)  // Using breach result for conviction appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                of(
                        randomUUID(),
                        ResultConstantsTest.SV,
                        ResultConstantsTest.OREV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE + " & " + ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Sentence Tests
    @Test
    void shouldConcludeWhenAppealAgainstSentenceAbandoned() { //2-1
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstSentenceWithdrawn() { //2-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstSentenceDismissed() { //2-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.AASD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceVaried() { //2-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.SV)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAppealAgainstSentenceAllowedAndOffenceResultsNotFinal() { // 2-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.AASA),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstSentenceWhenAppealAgainstSentenceAllowedAndOffenceResultsFinal() { // 2-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.AASA),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASA_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Conviction and Sentence Tests
    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceAbandoned() { //3-1
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceWithdrawn() { //3-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceDismissed() { //3-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.AACA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACA_CODE, result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAASDandSVandNoOffenceResults() { //3-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(
                        ResultConstantsTest.AASD,
                        ResultConstantsTest.SV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE + " & " + ResultCodeConstantsTest.SV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAASDandSVandOffenceResultsNotFinal() { //3-4
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(
                        ResultConstantsTest.AASD,
                        ResultConstantsTest.SV
                ),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE + " & " + ResultCodeConstantsTest.SV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstSentenceWhenAASDandSVandOffenceResultsFinal() { //3-4
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(
                        ResultConstantsTest.AASD,
                        ResultConstantsTest.SV
                ),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE + " & " + ResultCodeConstantsTest.SV_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionAndSentenceVariedAndOffenceResultsNotFinal() { // 3-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.ASV),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionAndSentenceVariedAndOffenceResultsFinal() { // 3-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.ASV),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE, result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionDismissedAndAppealAgainstSentenceAllowedAndOffenceResultsNotFinal() { //3-6
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(
                        ResultConstantsTest.AACD,
                        ResultConstantsTest.AASA
                ),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE + " & " + ResultCodeConstantsTest.AASA_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionDismissedAndAppealAgainstSentenceAllowedAndOffenceResultsNFinal() { //3-6
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(
                        ResultConstantsTest.AACD,
                        ResultConstantsTest.AASA
                ),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE + " & " + ResultCodeConstantsTest.AASA_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceCombinedAllowed() { //3-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.ACSD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ACSD_CODE, result.getApplicationResultCodeForLaa());
    }

    // Statutory Declaration Tests
    @Test
    void shouldNotConcludeForStatutoryDeclarationWhenGandSTDECandOffenceResultsNotFinal() { // 4-1
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                of(ResultConstantsTest.G, ResultConstantsTest.STDEC),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(String.join(" & ", of(ResultCodeConstantsTest.G_CODE, ResultCodeConstantsTest.STDEC_CODE)), result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForStatutoryDeclarationWhenGandSTDECandOffenceResultsFinal() { // 4-1
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                of(ResultConstantsTest.G, ResultConstantsTest.STDEC),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(String.join(" & ", of(ResultCodeConstantsTest.G_CODE, ResultCodeConstantsTest.STDEC_CODE)), result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenStatutoryDeclarationWithdrawn() { // 4-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenStatutoryDeclarationDismissed() { // 4-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                of(ResultConstantsTest.DISM)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.DISM_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenStatutoryDeclarationGrantedWithSTDEC() { // 4-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                of(
                        ResultConstantsTest.G,
                        ResultConstantsTest.STDEC
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE + " & " + ResultCodeConstantsTest.STDEC_CODE,
                result.getApplicationResultCodeForLaa());
    }

    // Reopen Case Tests
    @Test
    void shouldConcludeWhenReopenCaseGrantedOnly() {  // 5-1
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_REOPEN_CASE_ID,
                of(ResultConstantsTest.G)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForReOpenWhenGandROPENEDandOffenceResultsNotFinal() { // 5-2
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_REOPEN_CASE_ID,
                of(
                        ResultConstantsTest.G,
                        ResultConstantsTest.ROPENED
                ),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE + " & " + ResultCodeConstantsTest.ROPENED_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForReOpenWhenGandROPENEDandOffenceResultsFinal() { // 5-2
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_REOPEN_CASE_ID,
                of(
                        ResultConstantsTest.G,
                        ResultConstantsTest.ROPENED
                ),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE + " & " + ResultCodeConstantsTest.ROPENED_CODE,
                result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachApplicationWithNoAdjudication() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_ENGAGEMENT_SUPPORT_ORDER_ID,
                of(ResultConstantsTest.BRO)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.BRO_CODE, result.getApplicationResultCodeForLaa());
    }

    @ParameterizedTest
    @ValueSource(strings = {ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID, ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID_2})
    void shouldConcludeWhenBreachApplicationWithOrderToContinue(final String applicationTypeConstantsTest) {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                applicationTypeConstantsTest,
                of(ResultConstantsTest.OTC)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OTC_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachYROIntensiveWithOrderRevoked() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_INTENSIVE_ID,
                of(ResultConstantsTest.OREV)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachReparationOrderWithNoAdjudication() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_REPARATION_ORDER_ID,
                of(ResultConstantsTest.BRO)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.BRO_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderIsRefused() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(ResultConstantsTest.RFSD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.RFSD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderIsWithdrawn() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }





    @Test
    void shouldNotConcludeWhenBreachApplicationWithWrongApplicationType() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,  // Wrong application type
                of(ResultConstantsTest.OREV)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Sentence Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(randomUUID())
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithConvictionResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.AACD)  // Using conviction result for sentence appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(ResultConstantsTest.OREV)  // Using breach result for sentence appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                of(
                        randomUUID(),
                        ResultConstantsTest.AACD,  // Conviction result
                        ResultConstantsTest.OREV  // Breach result
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE + " & " + ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithWrongApplicationType() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,  // Wrong application type
                of(ResultConstantsTest.AASD)  // Valid sentence result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Conviction and Sentence (Combined) Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(randomUUID())  // Random unknown result ID
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithSentenceOnlyResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.SV)  // Using sentence-only result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithConvictionOnlyResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.AACD)  // Using conviction-only result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(ResultConstantsTest.OREV)  // Using breach result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                of(
                        randomUUID(),
                        ResultConstantsTest.SV,
                        ResultConstantsTest.OREV,
                        ResultConstantsTest.AACD
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE + " & " + ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.AACD_CODE,
                result.getApplicationResultCodeForLaa());
    }

    // Confiscation Order Positive Tests
    @Test
    void shouldConcludeWhenConfiscationOrderWithConfaaAndConviction() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(
                        ResultConstantsTest.CONFAA,
                        ResultConstantsTest.G
                )
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.CONFAA_CODE + " & " + ResultCodeConstantsTest.G_CODE,
                result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldConcludeWhenConfiscationOrderIsWithdrawn() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }

    // Confiscation Order Negative Tests
    @Test
    void shouldNotConcludeWhenConfiscationOrderWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithOnlyConfaa() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(ResultConstantsTest.CONFAA)  // Only CONFAA result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.CONFAA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(ResultConstantsTest.OREV)  // Using breach result for confiscation order
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                of(
                        randomUUID(),
                        ResultConstantsTest.OREV,
                        ResultConstantsTest.SV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.SV_CODE,
                result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldConcludeForAppealAgainstSentenceWhenAPAandOneChildResult() {

        final List<JudicialResult> resultList = asList(judicialResult()
                        .withJudicialResultTypeId(APA)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(APA)
                        .build(),
                judicialResult() // in child results JudicialResultTypeId is not equal to RootJudicialResultTypeId
                        .withJudicialResultTypeId(randomUUID())
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(randomUUID())
                        .build()
        );

        CourtApplication result = createApplicationWithApplicationChildResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                resultList
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAPAandTwoMainResult() {

        final UUID judicialResultTypeId = randomUUID();
        final List<JudicialResult> resultList = asList(judicialResult()
                        .withJudicialResultTypeId(APA)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(APA)
                        .build(),
                judicialResult() // in main results JudicialResultTypeId is equal to RootJudicialResultTypeId
                        .withJudicialResultTypeId(judicialResultTypeId)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(judicialResultTypeId)
                        .build()
        );

        CourtApplication result = createApplicationWithApplicationChildResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                resultList
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenSecondHearingHasNoApplicationLevelResultsAndOffencesHasNoFinalResult() {

        final CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                null,
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldConcludeForAppealAgainstSentenceWhenSecondHearingHasNoApplicationLevelResultsAndOffencesHasFinalResult() {

        final CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                null,
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }


    // Breach Application Tests scenarios

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationResultHasOREVAndBRO_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OREV, ResultConstantsTest.BRO),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationHasNoResultAndOffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                null,
                null
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationResultHasOREVAndBRO_OffenceResultsHaveIntermediary(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OREV, ResultConstantsTest.BRO),
                of(JudicialResultCategory.INTERMEDIARY, JudicialResultCategory.FINAL)
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasOREVAndBRO_OffenceResultsAreFinalOrAncillary(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OREV, ResultConstantsTest.BRO),
                of(JudicialResultCategory.ANCILLARY, JudicialResultCategory.FINAL)
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasOREVAndBRO_HasNoCourtOrderButApplicationCases(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                applicationTypeCode,
                of(ResultConstantsTest.OREV, ResultConstantsTest.BRO),
                true
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasOREVAndBRO_OffenceResultsAreFinal(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OREV, ResultConstantsTest.BRO),
                of(JudicialResultCategory.FINAL, JudicialResultCategory.FINAL)
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationHasNoResult_OffenceResultsAreFinal(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                null,
                of(JudicialResultCategory.FINAL, JudicialResultCategory.FINAL)
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationHasNoResult_OffenceResultsHasIntermediary(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                null,
                of(JudicialResultCategory.FINAL, JudicialResultCategory.INTERMEDIARY)
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationHasNoResult_OffenceResultsAreFinalOrAncillary(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                null,
                of(JudicialResultCategory.FINAL, JudicialResultCategory.ANCILLARY)
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasOREV_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OREV),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasOTC_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.OTC),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OTC_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasDismissed_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.DISM),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.DISM_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasWithdrawn_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.WDRN),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.WDRN_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldConcludeBreachApplicationWhenApplicationResultHasWithdrawn_OffenceHasNoFinalResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(ResultConstantsTest.WDRN),
                of(JudicialResultCategory.ANCILLARY, JudicialResultCategory.INTERMEDIARY)
        );

        assertThat(result.getProceedingsConcluded(), is(true));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.WDRN_CODE));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationResultHasRandomValue_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(randomUUID()),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), is("UNKNOWN_RESULT"));
    }

    @ParameterizedTest
    @MethodSource("getBreachTypeApplications")
    void shouldNotConcludeBreachApplicationWhenApplicationResultHasMultipleMismatchingValues_OffenceHasNoResult(final String applicationTypeCode) {
        CourtApplication result = createApplicationWithCourtOrder(
                applicationTypeCode,
                of(randomUUID(), randomUUID(), ResultConstantsTest.OREV),
                null
        );

        assertThat(result.getProceedingsConcluded(), is(false));
        assertThat(result.getApplicationResultCodeForLaa(), is(ResultCodeConstantsTest.OREV_CODE));
    }

    private CourtApplication createApplicationWithApplicationResultsAndNoOffenceResults(String applicationTypeCode, List<UUID> resultTypeIds) {
        List<JudicialResult> judicialResults = resultTypeIds.stream()
                .map(id -> judicialResult()
                        .withJudicialResultTypeId(id)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(id)
                        .build())
                .collect(Collectors.toList());

        return determineApplicationProceedingsConcluded(
                courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withCode(applicationTypeCode)
                                .build())
                        .withJudicialResults(judicialResults)
                        .build()
        );
    }

    private CourtApplication createApplicationWithCourtOrder(final String applicationTypeCode, final List<UUID> applicationLevelResultIds, final List<JudicialResultCategory> judicialResultCategories) {
        final List<JudicialResult> applicationLevelResults = ofNullable(applicationLevelResultIds)
                .map(ids -> ids.stream()
                        .map(id -> judicialResult()
                                .withJudicialResultTypeId(id)
                                .withCategory(JudicialResultCategory.FINAL)
                                .withRootJudicialResultTypeId(id)
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);

        final List<JudicialResult> offenceLevelResults = ofNullable(judicialResultCategories)
                .map(judicialResultCategoryList -> judicialResultCategoryList.stream()
                        .map(judicialResultCategory -> judicialResult()
                                .withJudicialResultTypeId(randomUUID())
                                .withCategory(judicialResultCategory)
                                .withRootJudicialResultTypeId(randomUUID())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);

        return determineApplicationProceedingsConcluded(
                courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withCode(applicationTypeCode)
                                .build())
                        .withJudicialResults(applicationLevelResults)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withId(randomUUID())
                                .withCourtOrderOffences(asList(CourtOrderOffence.courtOrderOffence()
                                        .withOffence(Offence.offence()
                                                .withId(randomUUID())
                                                .withJudicialResults(offenceLevelResults)
                                                .build())
                                        .build()))
                                .build())
                        .build()
        );
    }

    private CourtApplication createApplicationWithApplicationChildResultsAndNoOffenceResults(final String applicationTypeCode, final List<JudicialResult> judicialResults) {

        return determineApplicationProceedingsConcluded(
                courtApplication()
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withCode(applicationTypeCode)
                                .build())
                        .withJudicialResults(judicialResults)
                        .build()
        );
    }

    private CourtApplication createApplicationWithApplicationResultsAndOffenceResults(final String applicationTypeCode, final List<UUID> resultTypeIds, final boolean isOffenceResultsFinal) {
        final List<JudicialResult> judicialResults = ofNullable(resultTypeIds).orElse(emptyList()).stream()
                .map(id -> judicialResult()
                        .withJudicialResultTypeId(id)
                        .withCategory(JudicialResultCategory.FINAL)
                        .withRootJudicialResultTypeId(id)
                        .build())
                .collect(Collectors.toList());

        return determineApplicationProceedingsConcluded(
                courtApplication()
                        .withId(randomUUID())
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withOffences(buildOffences(isOffenceResultsFinal))
                                .build()))
                        .withType(courtApplicationType()
                                .withCode(applicationTypeCode)
                                .build())
                        .withJudicialResults(isEmpty(judicialResults) ? null : judicialResults)
                        .build()
        );
    }

    private List<Offence> buildOffences(final boolean isOffenceResultsFinal) {
        return Collections.singletonList(Offence.offence()
                .withId(randomUUID())
                .withJudicialResults(asList(
                        JudicialResult.judicialResult()
                                .withCategory(isOffenceResultsFinal ? JudicialResultCategory.FINAL : JudicialResultCategory.ANCILLARY)
                                .build(),
                        JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build()
                ))
                .build());
    }


} 