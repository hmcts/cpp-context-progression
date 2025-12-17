package uk.gov.moj.cpp.progression.processor.summons;

import static java.lang.String.format;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.EITHER_WAY;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.MCA;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.WITNESS;

import uk.gov.justice.core.courts.SummonsType;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class SummonsTemplateNameService {

    private static final String TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER = "SP%s_";

    private static final String TEMPLATE_SJP_REFERRAL = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "SjpReferral";
    private static final String TEMPLATE_FIRST_HEARING_EITHER_WAY = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "EitherWay";
    private static final String TEMPLATE_FIRST_HEARING_MCA = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "MCA";
    private static final String TEMPLATE_FIRST_HEARING_PARENT_GUARDIAN = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "FirstHearingParentGuardian";

    private static final String TEMPLATE_GENERIC_APPLICATION = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "Application";
    private static final String TEMPLATE_BREACH = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "Breach";
    private static final String TEMPLATE_BREACH_PARENT_GUARDIAN = TEMPLATE_NAME_PREFIX_WITH_LANGUAGE_PLACEHOLDER + "BreachParent";

    private static final Map<SummonsType, String> APPLICATION_TEMPLATE_NAME_MAP = ImmutableMap.of(
            APPLICATION, TEMPLATE_GENERIC_APPLICATION,
            BREACH, TEMPLATE_BREACH
    );

    private static final Map<SummonsCode, String> FIRST_HEARING_TEMPLATE_NAME_MAP = ImmutableMap.of(
            MCA, TEMPLATE_FIRST_HEARING_MCA,
            EITHER_WAY, TEMPLATE_FIRST_HEARING_EITHER_WAY,
            WITNESS, TEMPLATE_FIRST_HEARING_MCA,
            SummonsCode.APPLICATION, TEMPLATE_GENERIC_APPLICATION,
            SummonsCode.BREACH_OFFENCES, TEMPLATE_BREACH
    );

    public String getCaseSummonsParentTemplateName(final boolean isWelsh) {
        return getFormattedTemplateName(TEMPLATE_FIRST_HEARING_PARENT_GUARDIAN, isWelsh);
    }

    public String getBreachSummonsParentTemplateName(final boolean isWelsh) {
        return getFormattedTemplateName(TEMPLATE_BREACH_PARENT_GUARDIAN, isWelsh);
    }

    public String getCaseSummonsTemplateName(final SummonsType summonsRequired, final SummonsCode summonsCode, final boolean isWelsh) {
        return getFormattedTemplateName(SJP_REFERRAL == summonsRequired ? TEMPLATE_SJP_REFERRAL : getFirstHearingTemplateName(summonsCode), isWelsh);
    }

    public String getApplicationTemplateName(final SummonsType summonsRequired, final boolean isWelsh) {
        return getFormattedTemplateName(APPLICATION_TEMPLATE_NAME_MAP.get(summonsRequired), isWelsh);
    }

    private String getFirstHearingTemplateName(final SummonsCode summonsCode) {
        return FIRST_HEARING_TEMPLATE_NAME_MAP.get(summonsCode);
    }

    private String getFormattedTemplateName(final String templateNamePlaceholder, final boolean isWelsh) {
        return format(templateNamePlaceholder, isWelsh ? "B" : "E");
    }


}
