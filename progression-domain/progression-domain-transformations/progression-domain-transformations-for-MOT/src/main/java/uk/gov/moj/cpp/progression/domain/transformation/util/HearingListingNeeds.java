package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_CENTRE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT_LISTING_NEEDS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.EARLIEST_START_DATE_TIME;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.END_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ESTIMATED_MINUTES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JUDICIARY;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.JURISDICTION_TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LISTING_DIRECTIONS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.REPORTING_RESTRICTION_REASON;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.TYPE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCases;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class HearingListingNeeds {
  private HearingListingNeeds() {
    }

    public static JsonObject transformHearingListingNeeds(final JsonObject hearingListingNeeds) {
        //Add Mandatory Fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(ID, hearingListingNeeds.getString(ID))
                .add(TYPE, hearingListingNeeds.getJsonObject(TYPE))
                .add(JURISDICTION_TYPE, hearingListingNeeds.getString(JURISDICTION_TYPE))
                .add(COURT_CENTRE, hearingListingNeeds.getJsonObject(COURT_CENTRE))
                .add(ESTIMATED_MINUTES, hearingListingNeeds.getString(ESTIMATED_MINUTES));

        if (hearingListingNeeds.containsKey(EARLIEST_START_DATE_TIME)) {
            transformedPayloadObjectBuilder.add(EARLIEST_START_DATE_TIME, hearingListingNeeds.getString(EARLIEST_START_DATE_TIME));
        }

        if (hearingListingNeeds.containsKey(END_DATE)) {
            transformedPayloadObjectBuilder.add(END_DATE, hearingListingNeeds.getString(END_DATE));
        }

        if (hearingListingNeeds.containsKey(PROSECUTOR_DATES_TO_AVOID)) {
            transformedPayloadObjectBuilder.add(PROSECUTOR_DATES_TO_AVOID, hearingListingNeeds.getString(PROSECUTOR_DATES_TO_AVOID));
        }

        if (hearingListingNeeds.containsKey(LISTING_DIRECTIONS)) {
            transformedPayloadObjectBuilder.add(LISTING_DIRECTIONS, hearingListingNeeds.getString(LISTING_DIRECTIONS));
        }
       
        if (hearingListingNeeds.containsKey(REPORTING_RESTRICTION_REASON)) {
            transformedPayloadObjectBuilder.add(REPORTING_RESTRICTION_REASON, hearingListingNeeds.getString(REPORTING_RESTRICTION_REASON));
        }
        if (hearingListingNeeds.containsKey(JUDICIARY)) {
            transformedPayloadObjectBuilder.add(JUDICIARY, hearingListingNeeds.getJsonArray(JUDICIARY));
        }
        if (hearingListingNeeds.containsKey(PROSECUTION_CASES)) {
            transformedPayloadObjectBuilder.add(PROSECUTION_CASES, transformProsecutionCases(hearingListingNeeds.getJsonArray(PROSECUTION_CASES)));
        }
        if (hearingListingNeeds.containsKey(DEFENDANT_LISTING_NEEDS)) {
            transformedPayloadObjectBuilder.add(DEFENDANT_LISTING_NEEDS, hearingListingNeeds.getJsonArray(DEFENDANT_LISTING_NEEDS));
        }

       return transformedPayloadObjectBuilder.build();
    }




    }
