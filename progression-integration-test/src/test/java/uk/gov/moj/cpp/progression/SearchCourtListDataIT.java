package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupReferenceDataQueryCourtCenterDataByCourtNameStub;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for GET /courtlistdata (court list and prison court list data as JSON).
 */
public class SearchCourtListDataIT extends AbstractIT {

    @BeforeAll
    public static void setUpClass() {
        setupListingQueryStub();
        setupReferenceDataQueryCourtCenterDataByCourtNameStub();
    }

    @Test
    public void shouldReturnCourtListDataAsJson() {
        final String responseBody = pollForResponse(
                "/courtlistdata?listId=STANDARD&courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f",
                "application/vnd.progression.search.court.list.data+json");

        final JsonObject json = getJsonObject(responseBody);
        assertThat(json.getString("listType"), equalTo("public"));
        assertThat(json.containsKey("courtCentreName"), equalTo(true));
        assertThat(json.containsKey("hearingDates"), equalTo(true));
    }

    @Test
    public void shouldReturnPrisonCourtListDataAsJson() {
        final String responseBody = pollForResponse(
                "/courtlistdata?courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f",
                "application/vnd.progression.search.prison.court.list.data+json");

        final JsonObject json = getJsonObject(responseBody);
        assertThat(json.containsKey("listType"), equalTo(true));
        assertThat(json.containsKey("courtCentreName"), equalTo(true));
        assertThat(json.containsKey("hearingDates"), equalTo(true));
    }
}
