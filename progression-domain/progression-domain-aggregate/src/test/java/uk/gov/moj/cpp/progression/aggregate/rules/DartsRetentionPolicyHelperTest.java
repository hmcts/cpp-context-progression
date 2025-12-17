package uk.gov.moj.cpp.progression.aggregate.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.aggregate.rules.RetentionPolicyType.NON_CUSTODIAL;
import static uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantHelper.getAllDefendantsOffences;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;

import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class DartsRetentionPolicyHelperTest {

    private static final String CUSTODIAL_LIFE_PAYLOAD = "json/retentionpolicy/2-retention-period-custodial-life.json";
    private static final String CROWN_MAG_REMITTAL_RESULTS_PAYLOAD = "json/retentionpolicy/10-retention-period-crown-mag-remittal-results-added.json";

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    private DartsRetentionPolicyHelper dartsRetentionPolicyHelper;
    final List<String> remitResultIds = asList("2aeb9f97-18a9-4380-80e7-2452c64a6e18", "7e6079c6-b5cb-4994-aa8e-6f3cd0e954f8", "96d9e17a-fd20-4bb8-b00e-8c9350aa9d77");
    private HearingInfo hearingInfo = new HearingInfo(randomUUID(), "hearingType", JurisdictionType.CROWN.name(),
            randomUUID(), "courtCentreName", randomUUID(), "courtRoomName");

    @Test
    public void shouldRunTheRetentionRulesInSpecificOrder() {

        dartsRetentionPolicyHelper = new DartsRetentionPolicyHelper(hearingInfo, emptyList(), emptyList(), remitResultIds);

        final List<RetentionRule> retentionRuleList = dartsRetentionPolicyHelper.getRetentionRuleList();

        assertThat(retentionRuleList.size(), is(6));
        assertThat(retentionRuleList.get(0), is(instanceOf(LifeSentenceRetentionRule.class)));
        assertThat(retentionRuleList.get(1), is(instanceOf(CustodialSentenceRetentionRule.class)));
        assertThat(retentionRuleList.get(2), is(instanceOf(RemittalRetentionRule.class)));
        assertThat(retentionRuleList.get(3), is(instanceOf(AcquittalRetentionRule.class)));
        assertThat(retentionRuleList.get(4), is(instanceOf(NotGuiltyVerdictRetentionRule.class)));
        assertThat(retentionRuleList.get(5), is(instanceOf(NonCustodialSentenceRetentionRule.class)));
    }

    @Test
    public void shouldReturnDefaultPolicy3NonCustodialWhenNoRulesApply() {

        //given
        dartsRetentionPolicyHelper = new DartsRetentionPolicyHelper(hearingInfo, emptyList(), emptyList(), remitResultIds);

        //when
        final RetentionPolicy retentionPolicy = dartsRetentionPolicyHelper.getRetentionPolicy();

        //then
        assertThat(retentionPolicy, is(notNullValue()));
        assertThat(retentionPolicy.getPolicyType(), is(NON_CUSTODIAL));
        assertThat(retentionPolicy.getPolicyType().getPolicyCode(), is("2"));
        assertThat(retentionPolicy.getPeriod(), is("7Y0M0D"));
        assertThat(retentionPolicy.getHearingInfo(), is(hearingInfo));
    }

    @Test
    public void shouldReturnPolicy4WhenDefendantResultedWithLifeSentence() {

        //given
        final Hearing hearing = getHearing(CUSTODIAL_LIFE_PAYLOAD);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final List<DefendantJudicialResult> defendantJudicialResults = hearing.getDefendantJudicialResults();
        dartsRetentionPolicyHelper = new DartsRetentionPolicyHelper(hearingInfo, getAllDefendantsOffences(prosecutionCase.getDefendants()), defendantJudicialResults, remitResultIds);

        //when
        final RetentionPolicy retentionPolicy = dartsRetentionPolicyHelper.getRetentionPolicy();

        //then
        assertThat(retentionPolicy, is(notNullValue()));
        assertThat(retentionPolicy.getPolicyType(), is(RetentionPolicyType.LIFE));
        assertThat(retentionPolicy.getPolicyType().getPolicyCode(), is("4"));
        assertThat(retentionPolicy.getHearingInfo(), is(hearingInfo));
    }

    @Test
    public void shouldReturnPolicy2WhenDefendantResultedWithRemittal() {

        //given
        final Hearing hearing = getHearing(CROWN_MAG_REMITTAL_RESULTS_PAYLOAD);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final List<DefendantJudicialResult> defendantJudicialResults = hearing.getDefendantJudicialResults();
        dartsRetentionPolicyHelper = new DartsRetentionPolicyHelper(hearingInfo, getAllDefendantsOffences(prosecutionCase.getDefendants()), defendantJudicialResults, remitResultIds);

        //when
        final RetentionPolicy retentionPolicy = dartsRetentionPolicyHelper.getRetentionPolicy();

        //then
        assertThat(retentionPolicy, is(notNullValue()));
        assertThat(retentionPolicy.getPolicyType(), is(RetentionPolicyType.REMITTAL));
        assertThat(retentionPolicy.getPolicyType().getPolicyCode(), is("2"));
        assertThat(retentionPolicy.getHearingInfo(), is(hearingInfo));
    }

    private Hearing getHearing(final String pathToScenarioPayload) {
        final String custodialLifeHearing = getPayload(pathToScenarioPayload);
        return jsonObjectConverter.convert(stringToJsonObjectConverter.convert(custodialLifeHearing).getJsonObject("hearing"), Hearing.class);
    }

}