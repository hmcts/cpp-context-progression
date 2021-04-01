package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.BreachedApplications;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAggregateTest {

    @InjectMocks
    private HearingAggregate hearingAggregate;

    @Test
    public void shouldDoCorrectiononHearingDaysWithoutCourtCentre() throws IOException {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Object response = hearingAggregate.apply(createHearingDaysWithoutCourtCentreCorrected());

        assertThat(response.getClass(), is(CoreMatchers.equalTo(HearingDaysWithoutCourtCentreCorrected.class)));
    }

    @Test
    public void shouldApplyBreachApplicationCreationRequestedEvent() {
        final AddBreachApplication addBreachApplication = AddBreachApplication
                .addBreachApplication()
                .withBreachedApplications(Arrays.asList(BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build(),
                        BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build()
                ))
                .withMasterDefendantId(randomUUID())
                .withHearingId(randomUUID())
                .build();

        final List<Object> response = hearingAggregate.addBreachApplication(addBreachApplication).collect(Collectors.toList());
        assertThat(response.size(), is(2));
        assertThat(response.get(0).getClass(), is(CoreMatchers.equalTo(BreachApplicationCreationRequested.class)));
    }

    private HearingResulted createHearingResulted(final Hearing hearing) {
        return HearingResulted.hearingResulted().withHearing(hearing).build();
    }

    private HearingDaysWithoutCourtCentreCorrected createHearingDaysWithoutCourtCentreCorrected() {
        return HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withCourtCentreId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .withListedDurationMinutes(30)
                        .withListingSequence(1)
                        .withSittingDay(ZonedDateTime.now())
                        .build()))
                .withId(UUID.randomUUID())
                .build();
    }
}
