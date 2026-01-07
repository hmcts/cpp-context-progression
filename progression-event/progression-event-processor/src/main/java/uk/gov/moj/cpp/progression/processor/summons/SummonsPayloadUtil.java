package uk.gov.moj.cpp.progression.processor.summons;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.summons.SummonsAddress.summonsAddress;
import static uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails.summonsHearingCourtDetails;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.TIME_HMMA;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.summons.SummonsAddress;
import uk.gov.justice.core.courts.summons.SummonsAddressee;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;
import uk.gov.moj.cpp.progression.processor.exceptions.InvalidHearingTimeException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class SummonsPayloadUtil {

    private static final JsonObject EMPTY_JSON_OBJECT = createObjectBuilder().build();

    private static final DateTimeFormatter TIME_FORMATTER = ofPattern(TIME_HMMA.getValue());
    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

    private static final String L3_NAME = "oucodeL3Name";
    private static final String L3_NAME_WELSH = "oucodeL3WelshName";
    private static final String COURT_ROOM_NAME = "courtroomName";
    private static final String COURT_ROOM_NAME_WELSH = "welshCourtroomName";
    private static final String SINGLE_SPACE_DELIMITER = " ";
    public static final String POUND_SIGN = "£";
    public static final String UNSPECIFIED = "Unspecified";

    private SummonsPayloadUtil() {
    }

    public static SummonsAddressee populateSummonsAddressee(final Person person) {
        final SummonsAddressee.Builder summonsAddressee = SummonsAddressee.summonsAddressee();
        if (nonNull(person)) {
            summonsAddressee.withName(getFullName(person.getFirstName(), person.getMiddleName(), person.getLastName()));
            summonsAddressee.withAddress(populateSummonsAddress(person.getAddress()));
        } else {
            // defensive coding to make sure empty string is set when no value is present
            summonsAddressee.withName(EMPTY);
            summonsAddressee.withAddress(populateSummonsAddress(address().build()));
        }
        return summonsAddressee.build();
    }

    public static SummonsAddress populateSummonsAddress(final Address address) {
        if (nonNull(address)) {
            return summonsAddress()
                    .withLine1(emptyIfBlank(address.getAddress1()))
                    .withLine2(emptyIfBlank(address.getAddress2()))
                    .withLine3(emptyIfBlank(address.getAddress3()))
                    .withLine4(emptyIfBlank(address.getAddress4()))
                    .withLine5(emptyIfBlank(address.getAddress5()))
                    .withPostCode(emptyIfBlank(address.getPostcode()))
                    .build();
        }

        return summonsAddress().build();
    }

    public static SummonsAddress populateAddress(final JsonObject addressFromRefData) {
        final SummonsAddress.Builder addressBuilder = summonsAddress();
        addressBuilder.withLine1(addressFromRefData.getString("address1", EMPTY))
                .withLine2(addressFromRefData.getString("address2", EMPTY))
                .withLine3(addressFromRefData.getString("address3", EMPTY))
                .withLine4(addressFromRefData.getString("address4", EMPTY))
                .withLine5(addressFromRefData.getString("address5", EMPTY))
                .withPostCode(addressFromRefData.getString("postcode", EMPTY));

        final boolean isWelsh = addressFromRefData.getBoolean("isWelsh", false);
        if (isWelsh) {
            addressBuilder.withLine1Welsh(addressFromRefData.getString("welshAddress1", EMPTY))
                    .withLine2Welsh(addressFromRefData.getString("welshAddress2", EMPTY))
                    .withLine3Welsh(addressFromRefData.getString("welshAddress3", EMPTY))
                    .withLine4Welsh(addressFromRefData.getString("welshAddress4", EMPTY))
                    .withLine5Welsh(addressFromRefData.getString("welshAddress5", EMPTY));
        }
        return addressBuilder.build();
    }

    public static String getFullName(final String firstName, final String middleName, final String lastName) {
        return Stream.of(firstName, middleName, lastName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(SINGLE_SPACE_DELIMITER));
    }

    public static String emptyIfBlank(final String value) {
        return isNotBlank(value) ? value : EMPTY;
    }

    public static String getCourtTime(final ZonedDateTime hearingDateTime) {
        try {
            return TIME_FORMATTER.format(hearingDateTime.withZoneSameInstant(UK_TIME_ZONE));
        } catch (DateTimeException dte) {
            throw new InvalidHearingTimeException(format("Invalid hearing date time [ %s ] for generating notification / summon ", hearingDateTime), dte);
        }
    }

    public static SummonsHearingCourtDetails getSummonsHearingDetails(final JsonObject courtCentreJson, final UUID courtRoomId, final ZonedDateTime hearingDateTime) {

        final JsonObject courtRoomJsonObject = nonNull(courtRoomId) && courtCentreJson.containsKey("courtRooms")
                ? courtCentreJson.getJsonArray("courtRooms").getValuesAs(JsonObject.class).stream().filter(cr -> courtRoomId.toString().equals(cr.getString("id"))).findFirst().orElse(EMPTY_JSON_OBJECT)
                : EMPTY_JSON_OBJECT;

        return summonsHearingCourtDetails()
                .withCourtName(courtCentreJson.getString(L3_NAME, EMPTY))
                .withCourtNameWelsh(courtCentreJson.getString(L3_NAME_WELSH, EMPTY))
                .withCourtRoomName(courtRoomJsonObject.getString(COURT_ROOM_NAME, EMPTY))
                .withCourtRoomNameWelsh(courtRoomJsonObject.getString(COURT_ROOM_NAME_WELSH, EMPTY))
                .withHearingDate(hearingDateTime.toLocalDate().toString())
                .withHearingTime(getCourtTime(hearingDateTime))
                .withCourtAddress(populateAddress(courtCentreJson))
                .build();
    }

    public static String getProsecutorCosts(final String eventProsecutorCostValue) {
        if (isEmpty(eventProsecutorCostValue)) {
            return EMPTY;
        }

        String costValue = eventProsecutorCostValue.replace(POUND_SIGN, EMPTY).trim();

        if (isNotEmpty(costValue)) {
            Double value = Double.parseDouble(costValue);
            if (value == 0) {
                return UNSPECIFIED;
            }
        }
        return eventProsecutorCostValue;
    }


}
