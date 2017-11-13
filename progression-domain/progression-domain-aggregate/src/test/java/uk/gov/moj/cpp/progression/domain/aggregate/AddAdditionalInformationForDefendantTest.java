package uk.gov.moj.cpp.progression.domain.aggregate;

import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddAdditionalInformationForDefendantTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        //Adding defendant
        caseProgressionAggregate =new CaseProgressionAggregate();
        caseProgressionAggregate.addDefendant(addDefendant);

        //Adding Offences for defendant
        final List<OffenceForDefendant> offenceForDefendants = Arrays.asList(new OffenceForDefendant(randomUUID(), "offenceCode"
                , "indicatedPlea", "section",
                " wording", LocalDate.now(), LocalDate.now(), 0, 0, "modeOfTrial"));


        final OffencesForDefendantUpdated offencesForDefendantUpdated =
                new OffencesForDefendantUpdated(addDefendant.getCaseId(), addDefendant.getDefendantId(), offenceForDefendants);

        caseProgressionAggregate.updateOffencesForDefendant(offencesForDefendantUpdated ).collect(toList());

    }
    @Test
    public void shouldAddAdditionalInformationForDefendant() {

        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWith(addDefendant.getDefendantId());

        List<Object> eventStream = caseProgressionAggregate.addAdditionalInformationForDefendant(defendantCommand ).collect(toList());

        assertThat(eventStream.size(), is(2));

        Object objectCasePendingForSentenceHearing = eventStream.get(0);
        assertThat(objectCasePendingForSentenceHearing.getClass() , is(CoreMatchers.<Class<?>>equalTo(CasePendingForSentenceHearing.class)));


        Object objectDefendantAdditionalInformationAdded = eventStream.get(1);
        assertThat(objectDefendantAdditionalInformationAdded.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdditionalInformationAdded.class)));
        assertAdditionalInformationEvent(addDefendant.getDefendantId(), eventStream);
    }

    @Test
    public void shouldNotAddAdditionalInformationForDefendant() {

        DefendantCommand defendantCommand = DefendantBuilder.defaultDefendantWithoutAdditionalInfo(addDefendant.getDefendantId());;

        List<Object> eventStream = caseProgressionAggregate.addAdditionalInformationForDefendant(defendantCommand ).collect(toList());

        assertThat(eventStream.size(), is(2));

        Object objectCasePendingForSentenceHearing = eventStream.get(0);
        assertThat(objectCasePendingForSentenceHearing.getClass() , is(CoreMatchers.<Class<?>>equalTo(CaseReadyForSentenceHearing.class)));


        Object objectDefendantAdditionalInformationAdded = eventStream.get(1);
        assertThat(objectDefendantAdditionalInformationAdded.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdditionalInformationAdded.class)));
        assertAdditionalInformationEvent(addDefendant.getDefendantId(), eventStream);
    }


//

    private void assertAdditionalInformationEvent(UUID defendantId, List<Object> objectList) {
        final DefendantAdditionalInformationAdded o =
                (DefendantAdditionalInformationAdded) objectList.stream()
                        .filter(obj -> obj instanceof DefendantAdditionalInformationAdded)
                        .findFirst().get();
        assertThat(o.getDefendantId(), is(defendantId));
    }




}
