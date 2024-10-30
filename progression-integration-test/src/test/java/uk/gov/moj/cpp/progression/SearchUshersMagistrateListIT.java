package uk.gov.moj.cpp.progression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupForUshersMagistrateListQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupReferenceDataQueryCourtCenterDataByCourtNameStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupStagingPubHubCommandStub;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SearchUshersMagistrateListIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();

    @BeforeEach
    public void setUp() {
        setupForUshersMagistrateListQueryStub();
        setupStagingPubHubCommandStub();
        setupReferenceDataQueryCourtCenterDataByCourtNameStub();
        stubDocumentCreate(DOCUMENT_TEXT);
    }

    @Test
    public void shouldCreateUshersDocument() {
        final String documentContentResponse = pollForResponse("/courtlist?listId=USHERS_MAGISTRATE&courtCentreId=f8254db1-1683-483e-afb3-b87fde5a0a26&startDate=2022-12-19&endDate=2022-12-19&_=bc9153c0-8278-494e-8f72-d63973bab35f", "application/vnd.progression.search.court.list+json");
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }
}
