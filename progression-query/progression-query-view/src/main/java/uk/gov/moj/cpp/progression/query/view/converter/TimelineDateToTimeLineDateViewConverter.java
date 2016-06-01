package uk.gov.moj.cpp.progression.query.view.converter;

import uk.gov.moj.cpp.progression.persistence.entity.TimeLineDate;
import uk.gov.moj.cpp.progression.query.view.response.TimeLineDateView;

public class TimelineDateToTimeLineDateViewConverter  {

    public TimeLineDateView convert(TimeLineDate tld) {
        return new TimeLineDateView(tld.getType().name(), tld.getStartDate(), tld.getDaysFromStartDate(), tld.getDeadLineDate(),
                tld.getDaysToDeadline());
    }
}
