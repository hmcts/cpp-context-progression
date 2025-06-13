package uk.gov.moj.cpp.progression.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
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
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.Offence;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ApplicationProceedingsHelperTest {

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
                List.of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionWithdrawn() { //1-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionDismissed() { //1-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(ResultConstantsTest.AACD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConvictionDismissedAndSentenceVaried() { //1-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(
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
                List.of(
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
                List.of(
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
                List.of(
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
                List.of(ResultConstantsTest.AACA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACA_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Conviction Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of()
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(randomUUID())
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithInvalidCombination() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(
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
                List.of(ResultConstantsTest.SV)  // Using sentence result for conviction appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(ResultConstantsTest.OREV)  // Using breach result for conviction appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,
                List.of(
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
                List.of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstSentenceWithdrawn() { //2-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstSentenceDismissed() { //2-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.AASD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceVaried() { //2-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.SV)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAppealAgainstSentenceAllowedAndOffenceResultsNotFinal() { // 2-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.AASA),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstSentenceWhenAppealAgainstSentenceAllowedAndOffenceResultsFinal() { // 2-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.AASA),
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
                List.of(APA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceWithdrawn() { //3-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.AW)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AW_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenAppealAgainstConvictionAndSentenceDismissed() { //3-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.AACA)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACA_CODE, result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeForAppealAgainstSentenceWhenAASDandSVandNoOffenceResults() { //3-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(
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
                List.of(
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
                List.of(
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
                List.of(ResultConstantsTest.ASV),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionAndSentenceVariedAndOffenceResultsFinal() { // 3-5
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.ASV),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ASV_CODE, result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeForAppealAgainstConvictionAndSentenceWhenAppealAgainstConvictionDismissedAndAppealAgainstSentenceAllowedAndOffenceResultsNotFinal() { //3-6
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(
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
                List.of(
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
                List.of(ResultConstantsTest.ACSD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ACSD_CODE, result.getApplicationResultCodeForLaa());
    }

    // Statutory Declaration Tests
    @Test
    void shouldNotConcludeForStatutoryDeclarationWhenGandSTDECandOffenceResultsNotFinal() { // 4-1
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                List.of(ResultConstantsTest.G, ResultConstantsTest.STDEC),
                false
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(String.join(" & ", List.of(ResultCodeConstantsTest.G_CODE, ResultCodeConstantsTest.STDEC_CODE)), result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeForStatutoryDeclarationWhenGandSTDECandOffenceResultsFinal() { // 4-1
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                List.of(ResultConstantsTest.G, ResultConstantsTest.STDEC),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(String.join(" & ", List.of(ResultCodeConstantsTest.G_CODE, ResultCodeConstantsTest.STDEC_CODE)), result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenStatutoryDeclarationWithdrawn() { // 4-2
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                List.of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenStatutoryDeclarationDismissed() { // 4-3
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                List.of(ResultConstantsTest.DISM)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.DISM_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenStatutoryDeclarationGrantedWithSTDEC() { // 4-4
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_STATUTORY_DECLARATION_ID,
                List.of(
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
                List.of(ResultConstantsTest.G)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeForReOpenWhenGandROPENEDandOffenceResultsNotFinal() { // 5-2
        CourtApplication result = createApplicationWithApplicationResultsAndOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_REOPEN_CASE_ID,
                List.of(
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
                List.of(
                        ResultConstantsTest.G,
                        ResultConstantsTest.ROPENED
                ),
                true
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.G_CODE + " & " + ResultCodeConstantsTest.ROPENED_CODE,
                result.getApplicationResultCodeForLaa());
    }

    // Breach Application Tests
    @Test
    void shouldConcludeWhenBreachApplicationWithOrderRevoked() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of(ResultConstantsTest.OREV)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachApplicationWithNoAdjudication() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_ENGAGEMENT_SUPPORT_ORDER_ID,
                List.of(ResultConstantsTest.BRO)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.BRO_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachApplicationWithOrderToContinue() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_ID,
                List.of(ResultConstantsTest.OTC)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OTC_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachYROIntensiveWithOrderRevoked() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_YRO_INTENSIVE_ID,
                List.of(ResultConstantsTest.OREV)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachReparationOrderWithNoAdjudication() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_REPARATION_ORDER_ID,
                List.of(ResultConstantsTest.BRO)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.BRO_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldConcludeWhenBreachApplicationWithOrderRevokedAndNoAdjudication() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of(ResultConstantsTest.OREV, ResultConstantsTest.BRO)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE + " & " + ResultCodeConstantsTest.BRO_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderIsRefused() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(ResultConstantsTest.RFSD)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.RFSD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderIsWithdrawn() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }

    // Breach Application Negative Tests
    @Test
    void shouldNotConcludeWhenBreachApplicationWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of()
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenBreachApplicationWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of(randomUUID())
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenBreachApplicationWithNonBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of(APA)  // Using appeal result for breach application
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.APA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenBreachApplicationWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,
                List.of(
                        randomUUID(),
                        randomUUID(),
                        ResultConstantsTest.OREV
                )
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenBreachApplicationWithWrongApplicationType() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_ID,  // Wrong application type
                List.of(ResultConstantsTest.OREV)
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Sentence Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(randomUUID())
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }


    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithConvictionResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.AACD)  // Using conviction result for sentence appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(ResultConstantsTest.OREV)  // Using breach result for sentence appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstSentenceWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_SENTENCE_ID,
                List.of(
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
                List.of(ResultConstantsTest.AASD)  // Valid sentence result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AASD_CODE, result.getApplicationResultCodeForLaa());
    }

    // Appeal against Conviction and Sentence (Combined) Negative Tests
    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithUnknownResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(randomUUID())  // Random unknown result ID
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals("UNKNOWN_RESULT", result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithSentenceOnlyResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.SV)  // Using sentence-only result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.SV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithConvictionOnlyResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.AACD)  // Using conviction-only result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.AACD_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(ResultConstantsTest.OREV)  // Using breach result for combined appeal
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_APPEAL_AGAINST_CONVICTION_AND_SENTENCE_ID,
                List.of(
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

    @Test
    void shouldNotConcludeWhenAppealAgainstConvictionAndSentenceWithWrongApplicationType() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,  // Wrong application type
                List.of(ResultConstantsTest.ACSD)  // Valid combined appeal result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.ACSD_CODE, result.getApplicationResultCodeForLaa());
    }

    // Confiscation Order Positive Tests
    @Test
    void shouldConcludeWhenConfiscationOrderWithConfaaAndConviction() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(
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
                List.of(ResultConstantsTest.WDRN)
        );

        assertTrue(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.WDRN_CODE, result.getApplicationResultCodeForLaa());
    }

    // Confiscation Order Negative Tests
    @Test
    void shouldNotConcludeWhenConfiscationOrderWithNoResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of()  // Empty results list
        );

        assertFalse(result.getProceedingsConcluded());
        assertThat(result.getApplicationResultCodeForLaa(), nullValue());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithOnlyConfaa() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(ResultConstantsTest.CONFAA)  // Only CONFAA result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.CONFAA_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithBreachResult() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(ResultConstantsTest.OREV)  // Using breach result for confiscation order
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.OREV_CODE, result.getApplicationResultCodeForLaa());
    }

    @Test
    void shouldNotConcludeWhenConfiscationOrderWithMultipleInvalidResults() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_CONFISCATION_ORDER_ID,
                List.of(
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
    void shouldNotConcludeWhenConfiscationOrderWithWrongApplicationType() {
        CourtApplication result = createApplicationWithApplicationResultsAndNoOffenceResults(
                ApplicationTypeConstantsTest.APP_TYPE_BREACH_COMMUNITY_ORDER_ID,  // Wrong application type
                List.of(ResultConstantsTest.CONFAA)  // Valid confiscation result
        );

        assertFalse(result.getProceedingsConcluded());
        assertEquals(ResultCodeConstantsTest.CONFAA_CODE, result.getApplicationResultCodeForLaa());
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