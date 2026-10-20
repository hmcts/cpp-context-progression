package uk.gov.moj.cpp.progression.domain.helper;

import static java.util.UUID.nameUUIDFromBytes;

import uk.gov.moj.cpp.progression.domain.constant.RegisterType;

import java.util.UUID;

public class CourtRegisterHelper {
    public static UUID getPrisonCourtRegisterStreamId(final String courtCentreId, final String hearingDate) {
        return nameUUIDFromBytes((courtCentreId + hearingDate + RegisterType.PRISON_COURT.name()).getBytes());
    }
}
