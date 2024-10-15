package uk.gov.moj.cpp.progression.processor.summons;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;

import uk.gov.justice.core.courts.SummonsType;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsTemplateNameServiceTest {

    public static Stream<Arguments> defendantCaseSummonsSpecifications() {
        return Stream.of(
                Arguments.of(FIRST_HEARING, SummonsCode.MCA, "MCA", "E", false),
                Arguments.of(FIRST_HEARING, SummonsCode.WITNESS, "MCA", "E", false),
                Arguments.of(FIRST_HEARING, SummonsCode.EITHER_WAY, "EitherWay", "E", false),
                Arguments.of(FIRST_HEARING, SummonsCode.MCA, "MCA", "B", true),
                Arguments.of(FIRST_HEARING, SummonsCode.WITNESS, "MCA", "B", true),
                Arguments.of(FIRST_HEARING, SummonsCode.EITHER_WAY, "EitherWay", "B", true),
                Arguments.of(SJP_REFERRAL, null, "SjpReferral", "E", false),
                Arguments.of(SJP_REFERRAL, null, "SjpReferral", "B", true)
        );
    }

    public static Stream<Arguments> applicationSummonsSpecifications() {
        return Stream.of(
                Arguments.of(APPLICATION, "Application", "E", false),
                Arguments.of(APPLICATION, "Application", "B", true),
                Arguments.of(BREACH, "Breach", "E", false),
                Arguments.of(BREACH, "Breach", "B", true)
        );
    }

    @InjectMocks
    private SummonsTemplateNameService service;

    @MethodSource("defendantCaseSummonsSpecifications")
    @ParameterizedTest
    public void getCaseSummonsDefendantTemplateName(final SummonsType summonsRequired, final SummonsCode summonsCode, final String templateName, final String language, final boolean isWelsh) {
        assertThat(service.getCaseSummonsTemplateName(summonsRequired, summonsCode, isWelsh), is("SP" + language + "_" + templateName));
    }

    @Test
    public void getEnglishFirstHearingCaseSummonsParentTemplateName() {
        assertThat(service.getCaseSummonsParentTemplateName(false), is("SPE_FirstHearingParentGuardian"));
    }

    @Test
    public void getBilingualFirstHearingCaseSummonsParentTemplateName() {
        assertThat(service.getCaseSummonsParentTemplateName(true), is("SPB_FirstHearingParentGuardian"));
    }

    @MethodSource("applicationSummonsSpecifications")
    @ParameterizedTest
    public void getApplicationTemplateName(final SummonsType summonsRequired, final String templateName, final String language, final boolean isWelsh) {
        assertThat(service.getApplicationTemplateName(summonsRequired, isWelsh), is("SP" + language + "_" + templateName));
    }

    @Test
    public void getEnglishBreachSummonsParentTemplateName() {
        assertThat(service.getBreachSummonsParentTemplateName(false), is("SPE_BreachParent"));
    }

    @Test
    public void getBilingualBreachSummonsParentTemplateName() {
        assertThat(service.getBreachSummonsParentTemplateName(true), is("SPB_BreachParent"));
    }



}