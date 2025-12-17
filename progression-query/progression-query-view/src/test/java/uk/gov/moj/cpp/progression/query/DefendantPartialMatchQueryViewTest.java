package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantPartialMatchEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantPartialMatchRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.data.api.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@WireMockTest
@ExtendWith(MockitoExtension.class)
public class DefendantPartialMatchQueryViewTest {

    private static final String CASE_REFERENCE = "20GB12345666";
    private static final String DEFENDANT_NAME = "Teagan SMITT";
    private static final ZonedDateTime CASE_RECEIVED_DATETIME = ZonedDateTime.now();
    public static final String PAGE = "page";
    public static final String PAGE_SIZE = "pageSize";
    public static final String SORT_FIELD = "sortField";
    public static final String SORT_ORDER = "sortOrder";

    @InjectMocks
    private DefendantPartialMatchQueryView defendantPartialMatchQueryView;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private DefendantPartialMatchRepository defendantPartialMatchRepository;

    @Mock
    private QueryResult<DefendantPartialMatchEntity> queryResult;

    @Captor
    private ArgumentCaptor<Integer> page;

    @Captor
    private ArgumentCaptor<Integer> size;

    private final UUID defendantId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        setField(this.listToJsonArrayConverter, "mapper", objectMapper);
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter);
        setField(this.jsonObjectConverter, "objectMapper", objectMapper);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCallRepositoryPageAndSizeSortFieldAndSortOrderWhenSortFieldIsAddedAsParameter(
            final JsonObject jsonObject,
            final Integer expectedPage,
            final Integer expectedSize,
            final long expectedCount,
            final String invokedMethodName,
            final boolean shouldStub) {
        //given
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.defendant-partial-match").build(),
                jsonObject);
        mockRepository(expectedCount, invokedMethodName, shouldStub);

        //when
        JsonEnvelope response = defendantPartialMatchQueryView.getDefendantPartialMatches(jsonEnvelope);

        //then
        verifyRepository(invokedMethodName);
        assertEquals(expectedCount, response.payloadAsJsonObject().getInt("totalMatchedDefendants"));

        if (StringUtils.isEmpty(invokedMethodName)) {
            assertNull(response.payloadAsJsonObject().getJsonArray("matchedDefendants"));
        } else {
            assertEquals(expectedSize, size.getValue());
            assertEquals(expectedPage, page.getValue());
            assertMatchedDefendant(response.payloadAsJsonObject().getJsonArray("matchedDefendants").getJsonObject(0));
        }
    }

    private void assertMatchedDefendant(JsonObject matchedDefendant) {
        assertEquals("db0522a6-8563-4fff-89f2-2050b3b6c431", matchedDefendant.getJsonObject("defendantId").getString("string"));
        assertEquals("9e4932f7-97b2-3010-b942-ddd2624e4dd8", matchedDefendant.getJsonObject("masterDefendantId").getString("string"));
        assertEquals("db0522a6-8563-4fff-89f2-2050b3b6c432", matchedDefendant.getJsonObject("prosecutionCaseId").getString("string"));
        assertEquals("2020-03-02T10:00:00.000Z", matchedDefendant.getJsonObject("courtProceedingsInitiated").getString("string"));
        assertEquals("20GB12345666", matchedDefendant.getJsonObject("caseReference").getString("string"));
        assertEquals("Teagan", matchedDefendant.getJsonObject("firstName").getString("string"));
        assertEquals("SMITT", matchedDefendant.getJsonObject("lastName").getString("string"));
        assertEquals("2019-04-21", matchedDefendant.getJsonObject("dateOfBirth").getString("string"));
        assertEquals("addressLine1", matchedDefendant.getJsonObject("address").getJsonObject("addressLine1").getString("string"));
        assertEquals("addressLine2", matchedDefendant.getJsonObject("address").getJsonObject("addressLine2").getString("string"));
        assertEquals("addressLine3", matchedDefendant.getJsonObject("address").getJsonObject("addressLine3").getString("string"));
        assertEquals("addressLine4", matchedDefendant.getJsonObject("address").getJsonObject("addressLine4").getString("string"));
        assertEquals("addressLine5", matchedDefendant.getJsonObject("address").getJsonObject("addressLine5").getString("string"));
        assertEquals("HA1 1QF", matchedDefendant.getJsonObject("address").getJsonObject("postcode").getString("string"));
        assertEquals("415 1231111", matchedDefendant.getJsonObject("pncId").getString("string"));
        assertEquals("1234", matchedDefendant.getJsonObject("croNumber").getString("string"));
        assertEquals(2, matchedDefendant.getJsonObject("defendantsMatchedCount").size());

        JsonObject defendantMatched = matchedDefendant.getJsonArray("defendantsMatched").getJsonObject(0);
        assertEquals("db0522a6-8563-4fff-89f2-2050b3b6c433", defendantMatched.getJsonObject("defendantId").getString("string"));
        assertEquals("9e4932f7-97b2-3010-b942-ddd2624e4dd7", defendantMatched.getJsonObject("masterDefendantId").getString("string"));
        assertEquals("2020-03-02T11:00:00.000Z", defendantMatched.getJsonObject("courtProceedingsInitiated").getString("string"));
        assertEquals("db0522a6-8563-4fff-89f2-2050b3b6c434", defendantMatched.getJsonObject("prosecutionCaseId").getString("string"));
        assertEquals("20GB12345666", defendantMatched.getJsonObject("caseReference").getString("string"));
        assertEquals("Teagan", defendantMatched.getJsonObject("firstName").getString("string"));
        assertEquals("SMITT", defendantMatched.getJsonObject("lastName").getString("string"));
        assertEquals("2019-04-21", defendantMatched.getJsonObject("dateOfBirth").getString("string"));
        assertEquals("addressLine1", defendantMatched.getJsonObject("address").getJsonObject("addressLine1").getString("string"));
        assertEquals("addressLine2", defendantMatched.getJsonObject("address").getJsonObject("addressLine2").getString("string"));
        assertEquals("addressLine3", defendantMatched.getJsonObject("address").getJsonObject("addressLine3").getString("string"));
        assertEquals("addressLine4", defendantMatched.getJsonObject("address").getJsonObject("addressLine4").getString("string"));
        assertEquals("addressLine5", defendantMatched.getJsonObject("address").getJsonObject("addressLine5").getString("string"));
        assertEquals("HA1 1QF", defendantMatched.getJsonObject("address").getJsonObject("postcode").getString("string"));
        assertEquals("415 1231111", defendantMatched.getJsonObject("pncId").getString("string"));
        assertEquals("1234", defendantMatched.getJsonObject("croNumber").getString("string"));
    }

    private void mockRepository(
            final long expectedCount,
            final String invokedMethodName,
            final boolean shouldStub) {


        when(defendantPartialMatchRepository.count()).thenReturn(expectedCount);
        if(shouldStub) {
            when(queryResult.toPage(page.capture())).thenReturn(queryResult);
            when(queryResult.withPageSize(size.capture())).thenReturn(queryResult);
            when(queryResult.getResultList()).thenReturn(getDefendantPartialMatchData(defendantId));
        }
        if (StringUtils.equals(invokedMethodName, "findAllOrderByDefendantNameAsc")) {
            when(defendantPartialMatchRepository.findAllOrderByDefendantNameAsc()).thenReturn(queryResult);
        } else if (StringUtils.equals(invokedMethodName, "findAllOrderByDefendantNameDesc")) {
            when(defendantPartialMatchRepository.findAllOrderByDefendantNameDesc()).thenReturn(queryResult);
        } else if (StringUtils.equals(invokedMethodName, "findAllOrderByCaseReceivedDatetimeAsc")) {
            when(defendantPartialMatchRepository.findAllOrderByCaseReceivedDatetimeAsc()).thenReturn(queryResult);
        } else if (StringUtils.equals(invokedMethodName, "findAllOrderByCaseReceivedDatetimeDesc")) {
            when(defendantPartialMatchRepository.findAllOrderByCaseReceivedDatetimeDesc()).thenReturn(queryResult);
        }
    }

    private int getRepositoryVerifyTimes(final String invokedMethodName, final String methodName) {
        if (StringUtils.equals(invokedMethodName, methodName)) {
            return 1;
        }
        return 0;
    }

    private void verifyRepository(final String invokedMethodName) {
        verify(defendantPartialMatchRepository, atLeast(getRepositoryVerifyTimes(invokedMethodName, "findAllOrderByCaseReceivedDatetimeAsc"))).findAllOrderByCaseReceivedDatetimeAsc();
        verify(defendantPartialMatchRepository, atLeast(getRepositoryVerifyTimes(invokedMethodName, "findAllOrderByCaseReceivedDatetimeDesc"))).findAllOrderByCaseReceivedDatetimeDesc();
        verify(defendantPartialMatchRepository, atLeast(getRepositoryVerifyTimes(invokedMethodName, "findAllOrderByDefendantNameAsc"))).findAllOrderByDefendantNameAsc();
        verify(defendantPartialMatchRepository, atLeast(getRepositoryVerifyTimes(invokedMethodName, "findAllOrderByDefendantNameDesc"))).findAllOrderByDefendantNameDesc();
    }

    private List<DefendantPartialMatchEntity> getDefendantPartialMatchData(UUID defendantId) {
        List<DefendantPartialMatchEntity> defendantPartialMatches = new ArrayList<>();
        DefendantPartialMatchEntity defendantPartialMatch = new DefendantPartialMatchEntity();
        defendantPartialMatch.setCaseReceivedDatetime(CASE_RECEIVED_DATETIME);
        defendantPartialMatch.setCaseReference(CASE_REFERENCE);
        defendantPartialMatch.setDefendantId(defendantId);
        defendantPartialMatch.setDefendantName(DEFENDANT_NAME);
        defendantPartialMatch.setPayload(PAYLOAD);
        defendantPartialMatches.add(defendantPartialMatch);

        return defendantPartialMatches;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{Json.createObjectBuilder().build(), 0, 20, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder().add("page", 1).build(), 0, 20, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "XXXX")
                        .add(SORT_ORDER, "XXXX")
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "caseReceivedDate")
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "caseReceivedDate")
                        .add(SORT_ORDER, "XXXX")
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "caseReceivedDate")
                        .add(SORT_ORDER, "Desc")
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "caseReceivedDate")
                        .add(SORT_ORDER, "Asc")
                        .build(), 1, 10, 22, "findAllOrderByCaseReceivedDatetimeAsc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .build(), 1, 10, 22, "findAllOrderByDefendantNameAsc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .add(SORT_ORDER, "XXXX")
                        .build(), 1, 10, 22, "findAllOrderByDefendantNameAsc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .add(SORT_ORDER, "Asc")
                        .build(), 1, 10, 22, "findAllOrderByDefendantNameAsc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 2)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .add(SORT_ORDER, "Desc")
                        .build(), 1, 10, 22, "findAllOrderByDefendantNameDesc",true},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 7)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .add(SORT_ORDER, "Desc")
                        .build(), null, null, 59, "",false},
                new Object[]{Json.createObjectBuilder()
                        .add(PAGE, 1)
                        .add(PAGE_SIZE, 10)
                        .add(SORT_FIELD, "defendantName")
                        .add(SORT_ORDER, "Desc")
                        .build(), 0, 10, 2, "findAllOrderByDefendantNameDesc",true}
        );
    }

    private static final String PAYLOAD = "{\n" +
            "  \"defendantId\": \"db0522a6-8563-4fff-89f2-2050b3b6c431\",\n" +
            "  \"masterDefendantId\": \"9e4932f7-97b2-3010-b942-ddd2624e4dd8\",\n" +
            "  \"caseReference\": \"20GB12345666\",\n" +
            "  \"courtProceedingsInitiated\": \"2020-03-02T10:00:00.000Z\",\n" +
            "  \"prosecutionCaseId\": \"db0522a6-8563-4fff-89f2-2050b3b6c432\",\n" +
            "  \"firstName\": \"Teagan\",\n" +
            "  \"middleName\": \"\",\n" +
            "  \"lastName\": \"SMITT\",\n" +
            "  \"dateOfBirth\": \"2019-04-21\",\n" +
            "  \"address\": {\n" +
            "     \"addressLine1\": \"addressLine1\",\n" +
            "     \"addressLine2\": \"addressLine2\",\n" +
            "     \"addressLine3\": \"addressLine3\",\n" +
            "     \"addressLine4\": \"addressLine4\",\n" +
            "     \"addressLine5\": \"addressLine5\",\n" +
            "     \"postcode\": \"HA1 1QF\"\n" +
            "  },\n" +
            "  \"pncId\": \"415 1231111\",\n" +
            "  \"croNumber\": \"1234\",\n" +
            "  \"defendantsMatchedCount\": 2,\n" +
            "  \"defendantsMatched\": [\n" +
            "    {\n" +
            "      \"defendantId\": \"db0522a6-8563-4fff-89f2-2050b3b6c433\",\n" +
            "      \"masterDefendantId\": \"9e4932f7-97b2-3010-b942-ddd2624e4dd7\",\n" +
            "      \"caseReference\": \"20GB12345666\",\n" +
            "      \"courtProceedingsInitiated\": \"2020-03-02T11:00:00.000Z\",\n" +
            "      \"prosecutionCaseId\": \"db0522a6-8563-4fff-89f2-2050b3b6c434\",\n" +
            "      \"firstName\": \"Teagan\",\n" +
            "      \"middleName\": \"\",\n" +
            "      \"lastName\": \"SMITT\",\n" +
            "      \"dateOfBirth\": \"2019-04-21\",\n" +
            "      \"address\": {\n" +
            "         \"addressLine1\": \"addressLine1\",\n" +
            "         \"addressLine2\": \"addressLine2\",\n" +
            "         \"addressLine3\": \"addressLine3\",\n" +
            "         \"addressLine4\": \"addressLine4\",\n" +
            "         \"addressLine5\": \"addressLine5\",\n" +
            "         \"postcode\": \"HA1 1QF\" \n" +
            "      },\n" +
            "      \"pncId\": \"415 1231111\",\n" +
            "      \"croNumber\": \"1234\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"defendantId\": \"db0522a6-8563-4fff-89f2-2050b3b6c435\",\n" +
            "      \"masterDefendantId\": \"9e4932f7-97b2-3010-b942-ddd2624e4dd8\",\n" +
            "      \"caseReference\": \"64GB12345623\",\n" +
            "      \"courtProceedingsInitiated\": \"2020-03-02T11:00:00.000Z\",\n" +
            "      \"prosecutionCaseId\": \"db0522a6-8563-4fff-89f2-2050b3b6c436\",\n" +
            "      \"firstName\": \"Teagan\",\n" +
            "      \"middleName\": \".T.\",\n" +
            "      \"lastName\": \"SMITT\",\n" +
            "      \"dateOfBirth\": \"\",\n" +
            "      \"address\": {\n" +
            "         \"addressLine1\": \"addressLine1\",\n" +
            "         \"addressLine2\": \"addressLine2\",\n" +
            "         \"addressLine3\": \"addressLine3\",\n" +
            "         \"addressLine4\": \"addressLine4\",\n" +
            "         \"addressLine5\": \"addressLine5\",\n" +
            "         \"postcode\": \"HA1 1QF\" \n" +
            "      },\n" +
            "      \"pncId\": \"415 1231111\",\n" +
            "      \"croNumber\": \"543 565454\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
}
