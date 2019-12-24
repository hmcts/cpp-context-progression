package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.referCourtApplication;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;

import org.junit.Before;
import org.junit.Test;

public class ReferApplicationToCourtIT extends AbstractIT {

    private String applicationId;
    private String hearingId;

    @Before
    public void setUp() {
        applicationId = randomUUID().toString();
        hearingId = randomUUID().toString();
    }

    @Test
    public void shouldListCourtHearing() throws Exception {
        referCourtApplication(applicationId, hearingId, "progression.command.refer-application-to-court.json");

        verifyPostListCourtHearing(applicationId);
    }

}

