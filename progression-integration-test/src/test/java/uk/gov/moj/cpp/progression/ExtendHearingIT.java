package uk.gov.moj.cpp.progression;

import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.util.ExtendHearingHelper;

import java.util.UUID;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.extendHearing;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;

public class ExtendHearingIT {

    ExtendHearingHelper helper;
    private String applicationId;
    private String hearingId;

    @Before
    public void setUp() {
        applicationId = UUID.randomUUID().toString();
        hearingId = UUID.randomUUID().toString();
        helper = new ExtendHearingHelper();
        createMockEndpoints();
    }

    @Test
    public void shouldListCourtHearing() throws Exception {

        extendHearing(applicationId, hearingId, "progression.command.extend-hearing.json");

        verifyPostListCourtHearing(applicationId);
    }

}
