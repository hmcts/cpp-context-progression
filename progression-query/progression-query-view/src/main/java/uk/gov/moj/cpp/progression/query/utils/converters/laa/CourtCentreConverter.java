package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import uk.gov.justice.progression.query.laa.CourtCentre;

import java.util.Objects;

import javax.inject.Inject;
@SuppressWarnings("squid:S1168")
public class CourtCentreConverter extends LAAConverter {

    @Inject
    private AddressConverter addressConverter;

    public CourtCentre convert(final uk.gov.justice.core.courts.CourtCentre courtCentre) {
        if (Objects.isNull(courtCentre)) {
            return null;
        }
        return CourtCentre.courtCentre()
                .withId(courtCentre.getId())
                .withName(courtCentre.getName())
                .withAddress(addressConverter.convert(courtCentre.getAddress()))
                .withCode(courtCentre.getCode())
                .withRoomId(courtCentre.getRoomId())
                .withRoomName(courtCentre.getRoomName())
                .withWelshName(courtCentre.getWelshName())
                .withWelshRoomName(courtCentre.getWelshRoomName())
                .build();
    }
}