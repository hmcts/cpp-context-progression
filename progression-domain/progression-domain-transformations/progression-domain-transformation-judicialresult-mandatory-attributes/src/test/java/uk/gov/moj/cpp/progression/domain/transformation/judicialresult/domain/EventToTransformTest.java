package uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.APPLICATION_REFERRED_TO_COURT;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.BOXWORK_APPLICATION_REFERRED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_ADDED_TO_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.COURT_APPLICATION_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_APPLICATION_LINK_CREATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_CONFIRMED_CASE_STATUS_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_EXTENDED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_INITIATE_ENRICHED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.HEARING_RESULTED_CASE_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.LISTED_COURT_APPLICATION_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_OFFENCES_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.isEventToTransform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class EventToTransformTest {

    @DataProvider
    public static Object[][] validEventToMatch() {
        return new Object[][]{
                {BOXWORK_APPLICATION_REFERRED.getEventName()},
                {HEARING_EXTENDED.getEventName()},
                {HEARING_RESULTED.getEventName()},
                {COURT_APPLICATION_UPDATED.getEventName()},
                {PROSECUTION_CASE_OFFENCES_UPDATED.getEventName()},
                {PROSECUTION_CASE_DEFENDANT_UPDATED.getEventName()},
                {HEARING_RESULTED_CASE_UPDATED.getEventName()},
                {HEARING_INITIATE_ENRICHED.getEventName()},
                {COURT_APPLICATION_CREATED.getEventName()},
                {COURT_APPLICATION_ADDED_TO_CASE.getEventName()},
                {LISTED_COURT_APPLICATION_CHANGED.getEventName()},
                {APPLICATION_REFERRED_TO_COURT.getEventName()},
                {HEARING_APPLICATION_LINK_CREATED.getEventName()},
                {PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED.getEventName()},
                {HEARING_CONFIRMED_CASE_STATUS_UPDATED.getEventName()}
        };
    }

    @Test
    @UseDataProvider("validEventToMatch")
    public void shouldReturnTrueIfEventNameIsAMatch(final String eventName) {
        assertThat(isEventToTransform(eventName), is(true));
    }

    @Test
    public void shouldReturnFalseIfEventNameIsNotAMatch() {
        assertThat(isEventToTransform(STRING.next()), is(false));
    }
}