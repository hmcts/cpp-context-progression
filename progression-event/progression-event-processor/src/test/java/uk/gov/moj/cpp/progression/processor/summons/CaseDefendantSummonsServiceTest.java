package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.core.courts.SummonsType.SJP_REFERRAL;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnAssociatedPersonAddress;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnCourtAddress;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnDefendant;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnDefendantAddress;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnHearingCourtDetails;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnOffences;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnProsecutor;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnReferralReason;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.assertOnSummonsData;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateCivilProsecutionCase;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateCourtCentreJson;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateProsecutionCase;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateReferralReasonsJson;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateSummonsData;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateSummonsDataWithCostAndLanguageNeeds;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.getLjaDetails;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.getProsecutor;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.getRefDataOffences;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.APPLICATION;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.BREACH_OFFENCES;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.EITHER_WAY;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.MCA;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsCode.WITNESS;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDefendantSummonsServiceTest {

    public static final String DEFENDANT_NAME = "Harry Jack Kane";
    public static final String PARENT_NAME = "William Senior Kane";
    private static final UUID CASE_ID = randomUUID();
    private static final UUID REFERRAL_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.fromString("f8254db1-1683-483e-afb3-b87fde5a0a26");

    // sections of template payload
    private static final String ADDRESS = "address";
    private static final String OFFENCES = "offences";
    private static final String ADDRESSEE = "addressee";
    private static final String HEARING_COURT_DETAILS = "hearingCourtDetails";
    private static final String COURT_ADDRESS = "courtAddress";
    private static final String PROSECUTOR = "prosecutor";

    private static final ObjectToJsonObjectConverter OBJECT_TO_JSON_OBJECT_CONVERTER = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope envelope;

    @InjectMocks
    private CaseDefendantSummonsService caseDefendantSummonsService;

    public static Stream<Arguments> caseSummonsSpecification() {
        return Stream.of(
                Arguments.of(FIRST_HEARING, MCA, MCA.getSubType()),
                Arguments.of(FIRST_HEARING, WITNESS, WITNESS.getSubType()),
                Arguments.of(FIRST_HEARING, EITHER_WAY, EITHER_WAY.getSubType()),
                Arguments.of(SJP_REFERRAL, null, "SJP_REFERRAL")
        );
    }

    public static Stream<Arguments> civilCaseSummonsSpecification() {
        return Stream.of(
                Arguments.of(FIRST_HEARING, MCA, MCA.getSubType()),
                Arguments.of(FIRST_HEARING, WITNESS, WITNESS.getSubType()),
                Arguments.of(FIRST_HEARING, EITHER_WAY, EITHER_WAY.getSubType()),
                Arguments.of(FIRST_HEARING, APPLICATION, APPLICATION.getSubType()),
                Arguments.of(FIRST_HEARING, BREACH_OFFENCES, BREACH_OFFENCES.getSubType()),
                Arguments.of(SJP_REFERRAL, null, "SJP_REFERRAL")
        );
    }

    public static Stream<Arguments> caseSummonsCostSpecification() {
        return Stream.of(
                Arguments.of(FIRST_HEARING, MCA, "£0", false),
                Arguments.of(FIRST_HEARING, MCA, "£0", true),
                Arguments.of(FIRST_HEARING, MCA, "£12.5", true),
                Arguments.of(FIRST_HEARING, MCA, "£40.12", false),
                Arguments.of(FIRST_HEARING, MCA, null, false),
                Arguments.of(FIRST_HEARING, MCA, "£", true)
        );
    }

    @MethodSource("caseSummonsSpecification")
    @ParameterizedTest
    void shouldGenerateEnglishSummonsPayloadForFirstHearing(final SummonsType summonsRequired, final SummonsCode summonsCode, final String summonsType) {
        verifySummonsPayloadGeneratedFor(summonsRequired, summonsCode, summonsType);
    }

    @MethodSource("caseSummonsSpecification")
    @ParameterizedTest
    void shouldGenerateEnglishSummonsPayloadForFirstHearingCivilCase(final SummonsType summonsRequired, final SummonsCode summonsCode, final String summonsType) {
        verifyCivilSummonsPayloadGeneratedFor(summonsRequired, summonsCode, summonsType);
    }

    @MethodSource("caseSummonsCostSpecification")
    @ParameterizedTest
    void shouldGenerateSummonsPayloadForFirstHearingCaseWithUnspecifiedCost(final SummonsType summonsRequired, final SummonsCode summonsCode, final String costValue, final boolean isWelsh) {
        verifySummonsPayloadGeneratedForUnspecifiedCost(summonsRequired, summonsCode, costValue, isWelsh);
    }

    public void verifySummonsPayloadGeneratedFor(final SummonsType summonsRequired, final SummonsCode summonsCode, final String summonsType) {

        if (SJP_REFERRAL == summonsRequired) {
            when(referenceDataService.getReferralReasonByReferralReasonId(envelope, REFERRAL_ID, requester)).thenReturn(Optional.of(generateReferralReasonsJson(REFERRAL_ID.toString())));
        }

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(envelope), eq(requester), eq(Optional.empty()))).thenReturn(getRefDataOffences());

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(generateSummonsData(summonsRequired, CASE_ID, DEFENDANT_ID, COURT_CENTRE_ID, REFERRAL_ID, BOOLEAN.next())).build();
        final String summonsCodeAsString = nonNull(summonsCode) ? summonsCode.getCode() : StringUtils.EMPTY;
        final ProsecutionCase prosecutionCase = generateProsecutionCase(CASE_ID.toString(), DEFENDANT_ID.toString(), summonsCodeAsString, true);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final ListDefendantRequest listDefendantRequest = summonsDataPrepared.getSummonsData().getListDefendantRequests().get(0);
        final JsonObject courtCentreJson = generateCourtCentreJson(true);
        final Optional<LjaDetails> optionalLjaDetails = getLjaDetails();
        final SummonsProsecutor summonsProsecutor = getProsecutor();

        final SummonsDocumentContent summonsDocumentContent = caseDefendantSummonsService.generateSummonsPayloadForDefendant(envelope, summonsDataPrepared, prosecutionCase, defendant, listDefendantRequest, courtCentreJson, optionalLjaDetails, summonsProsecutor);

        //Then
        assertTemplatePayloadValues(summonsRequired, summonsType, OBJECT_TO_JSON_OBJECT_CONVERTER.convert(summonsDocumentContent), true);
    }

    private void verifySummonsPayloadGeneratedForUnspecifiedCost(final SummonsType summonsRequired, final SummonsCode summonsCode, final String costValue, final boolean isWelsh) {

        if (SJP_REFERRAL == summonsRequired) {
            when(referenceDataService.getReferralReasonByReferralReasonId(envelope, REFERRAL_ID, requester)).thenReturn(Optional.of(generateReferralReasonsJson(REFERRAL_ID.toString())));
        }

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(envelope), eq(requester), eq(Optional.empty()))).thenReturn(getRefDataOffences());

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(generateSummonsDataWithCostAndLanguageNeeds(summonsRequired, CASE_ID, DEFENDANT_ID, COURT_CENTRE_ID, REFERRAL_ID, BOOLEAN.next(), costValue, isWelsh)).build();
        final String summonsCodeAsString = nonNull(summonsCode) ? summonsCode.getCode() : StringUtils.EMPTY;
        final ProsecutionCase prosecutionCase = generateProsecutionCase(CASE_ID.toString(), DEFENDANT_ID.toString(), summonsCodeAsString, true);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final ListDefendantRequest listDefendantRequest = summonsDataPrepared.getSummonsData().getListDefendantRequests().get(0);
        final JsonObject courtCentreJson = generateCourtCentreJson(true);
        final Optional<LjaDetails> optionalLjaDetails = getLjaDetails();
        final SummonsProsecutor summonsProsecutor = getProsecutor();

        final SummonsDocumentContent summonsDocumentContent = caseDefendantSummonsService.generateSummonsPayloadForDefendant(envelope, summonsDataPrepared, prosecutionCase, defendant, listDefendantRequest, courtCentreJson, optionalLjaDetails, summonsProsecutor);
        assertThat(summonsDocumentContent.getProsecutorCosts(), notNullValue());
        if (isEmpty(costValue)) {
            assertThat(summonsDocumentContent.getProsecutorCosts(), is(""));
        } else if (costValue.contains("£0")) {
            assertThat(summonsDocumentContent.getProsecutorCosts(), is(isWelsh ? "Heb ei bennu" : "Unspecified"));
        } else {
            assertThat(summonsDocumentContent.getProsecutorCosts(), is(costValue));
        }
    }

    public void verifyCivilSummonsPayloadGeneratedFor(final SummonsType summonsRequired, final SummonsCode summonsCode, final String summonsType) {

        if (SJP_REFERRAL == summonsRequired) {
            when(referenceDataService.getReferralReasonByReferralReasonId(envelope, REFERRAL_ID, requester)).thenReturn(Optional.of(generateReferralReasonsJson(REFERRAL_ID.toString())));
        }

        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(envelope), eq(requester), eq(Optional.of("MoJ")))).thenReturn(getRefDataOffences());

        final SummonsDataPrepared summonsDataPrepared = summonsDataPrepared().withSummonsData(generateSummonsData(summonsRequired, CASE_ID, DEFENDANT_ID, COURT_CENTRE_ID, REFERRAL_ID, BOOLEAN.next())).build();
        final String summonsCodeAsString = nonNull(summonsCode) ? summonsCode.getCode() : StringUtils.EMPTY;
        final ProsecutionCase prosecutionCase = generateCivilProsecutionCase(CASE_ID.toString(), DEFENDANT_ID.toString(), summonsCodeAsString, true);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final ListDefendantRequest listDefendantRequest = summonsDataPrepared.getSummonsData().getListDefendantRequests().get(0);
        final JsonObject courtCentreJson = generateCourtCentreJson(true);
        final Optional<LjaDetails> optionalLjaDetails = getLjaDetails();
        final SummonsProsecutor summonsProsecutor = getProsecutor();

        final SummonsDocumentContent summonsDocumentContent = caseDefendantSummonsService.generateSummonsPayloadForDefendant(envelope, summonsDataPrepared, prosecutionCase, defendant, listDefendantRequest, courtCentreJson, optionalLjaDetails, summonsProsecutor);

        //Then
        assertTemplatePayloadValues(summonsRequired, summonsType, OBJECT_TO_JSON_OBJECT_CONVERTER.convert(summonsDocumentContent), true);
    }


    private void assertTemplatePayloadValues(final SummonsType summonsRequired, final String summonsType, final JsonObject summonsDataJson, final boolean defendantVersion) {
        assertOnSummonsData(summonsDataJson, summonsRequired, summonsType);

        final JsonObject defendantJson = assertOnDefendant(summonsDataJson, true);

        final JsonObject defendantAddressJson = defendantJson.getJsonObject(ADDRESS);
        assertOnDefendantAddress(defendantAddressJson);

        final JsonArray offencesJson = summonsDataJson.getJsonArray(OFFENCES);
        assertOnOffences(offencesJson);

        final JsonObject addresseeJson = summonsDataJson.getJsonObject(ADDRESSEE);
        if (defendantVersion) {
            assertThat(addresseeJson, notNullValue());
            assertThat(addresseeJson.getString("name"), is(DEFENDANT_NAME));
            final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
            assertOnDefendantAddress(addresseeAddressJson);
        } else {
            // parent guardian version
            assertThat(addresseeJson, notNullValue());
            assertThat(addresseeJson.getString("name"), is(PARENT_NAME));
            final JsonObject addresseeAddressJson = addresseeJson.getJsonObject(ADDRESS);
            assertOnAssociatedPersonAddress(addresseeAddressJson);
        }

        final JsonObject hearingCourtDetails = summonsDataJson.getJsonObject(HEARING_COURT_DETAILS);
        assertOnHearingCourtDetails(hearingCourtDetails);
        final JsonObject courtAddress = hearingCourtDetails.getJsonObject(COURT_ADDRESS);
        assertOnCourtAddress(courtAddress, false);

        final JsonObject prosecutor = summonsDataJson.getJsonObject(PROSECUTOR);
        assertOnProsecutor(prosecutor);

        if (SJP_REFERRAL == summonsRequired) {
            final JsonObject referralReason = summonsDataJson.getJsonObject("referralContent");
            assertOnReferralReason(referralReason);
        }
    }
}