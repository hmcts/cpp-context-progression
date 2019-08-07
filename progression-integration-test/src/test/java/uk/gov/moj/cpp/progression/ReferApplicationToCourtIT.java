package uk.gov.moj.cpp.progression;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.referCourtApplication;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.moj.cpp.progression.util.ReferApplicationToCourtHelper;
import java.util.UUID;

public class ReferApplicationToCourtIT {

    ReferApplicationToCourtHelper helper;
    private String applicationId;
    private String hearingId;

    @Before
    public void setUp() {
        applicationId = UUID.randomUUID().toString();
        hearingId = UUID.randomUUID().toString();
        helper = new ReferApplicationToCourtHelper();
        createMockEndpoints();
    }

    @Test
    public void shouldListCourtHearing() throws Exception {

        referCourtApplication(applicationId, hearingId, "progression.command.refer-application-to-court.json");

        verifyPostListCourtHearing(applicationId);
    }

}

