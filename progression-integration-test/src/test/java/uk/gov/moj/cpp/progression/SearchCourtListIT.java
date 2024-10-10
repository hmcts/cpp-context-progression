package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupListingQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupReferenceDataQueryCourtCenterDataByCourtNameStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupStagingPubHubCommandStub;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchCourtListIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();

    @BeforeEach
    public void setUp() {
        setupListingQueryStub();
        setupStagingPubHubCommandStub();
        setupReferenceDataQueryCourtCenterDataByCourtNameStub();
        stubDocumentCreate(DOCUMENT_TEXT);
    }

    @Test
    public void shouldCreatePetForm() {
        final String documentContentResponse = pollForResponse("/courtlist?listId=STANDARD&courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f", "application/vnd.progression.search.court.list+json");
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

    @Test
    public void shouldCreatePrisonCourtList() {
        final String documentContentResponse = pollForResponse("/courtlist?courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-07-12&endDate=2022-07-12&_=bc9153c0-8278-494e-8f72-d63973bab35f", "application/vnd.progression.search.prison.court.list+json");
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

}
