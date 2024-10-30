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
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.PROSECUTION_CASE_OFFENCES_UPDATED;
import static uk.gov.moj.cpp.progression.domain.transformation.judicialresult.domain.EventToTransform.isEventToTransform;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventToTransformTest {

    public static Stream<Arguments> validEventToMatch() {
        return Stream.of(
                Arguments.of(BOXWORK_APPLICATION_REFERRED.getEventName()),
                Arguments.of(HEARING_EXTENDED.getEventName()),
                Arguments.of(HEARING_RESULTED.getEventName()),
                Arguments.of(COURT_APPLICATION_UPDATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_OFFENCES_UPDATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_DEFENDANT_UPDATED.getEventName()),
                Arguments.of(HEARING_RESULTED_CASE_UPDATED.getEventName()),
                Arguments.of(HEARING_INITIATE_ENRICHED.getEventName()),
                Arguments.of(COURT_APPLICATION_CREATED.getEventName()),
                Arguments.of(COURT_APPLICATION_ADDED_TO_CASE.getEventName()),
                Arguments.of(LISTED_COURT_APPLICATION_CHANGED.getEventName()),
                Arguments.of(APPLICATION_REFERRED_TO_COURT.getEventName()),
                Arguments.of(HEARING_APPLICATION_LINK_CREATED.getEventName()),
                Arguments.of(PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED.getEventName()),
                Arguments.of(PROSECUTION_CASE_DEFENDANT_LISTING_STATUS_CHANGED_V2.getEventName()),
                Arguments.of(HEARING_CONFIRMED_CASE_STATUS_UPDATED.getEventName())
        );
    }

    @ParameterizedTest
    @MethodSource("validEventToMatch")
    public void shouldReturnTrueIfEventNameIsAMatch(final String eventName) {
        assertThat(isEventToTransform(eventName), is(true));
    }

    @Test
    public void shouldReturnFalseIfEventNameIsNotAMatch() {
        assertThat(isEventToTransform(STRING.next()), is(false));
    }
}