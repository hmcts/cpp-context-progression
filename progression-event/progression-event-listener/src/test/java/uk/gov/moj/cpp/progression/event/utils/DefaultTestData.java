package uk.gov.moj.cpp.progression.event.utils;


import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class DefaultTestData {

    public static final UUID CASE_ID = UUID.randomUUID();
    public static final String CASE_ID_STR = CASE_ID.toString();

    public static final UUID DEFENDANT_ID = UUID.randomUUID();
    public static final String DEFENDANT_ID_STR = DEFENDANT_ID.toString();
    public static final String BAIL_STATUS_CONDITIONAL = "conditional";
    public static final String MATERIAL_ID = UUID.randomUUID().toString();
    public static final LocalDate CUSTODY_TIME_LIMIT_DATE = new UtcClock().now().toLocalDate();

}
