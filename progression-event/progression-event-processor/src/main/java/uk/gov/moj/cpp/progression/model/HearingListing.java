package uk.gov.moj.cpp.progression.model;

import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;

import java.util.List;
import java.util.UUID;

public record HearingListing(UUID hearingId, String key, List<ListHearingRequest> listHearingRequestList, List<ListDefendantRequest> listDefendantRequests) {

}