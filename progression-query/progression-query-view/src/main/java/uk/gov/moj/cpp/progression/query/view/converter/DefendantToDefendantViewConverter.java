package uk.gov.moj.cpp.progression.query.view.converter;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.query.view.response.DefendantView;

public class DefendantToDefendantViewConverter {

    public DefendantView convert(Defendant defendant) {
        return new DefendantView(defendant);
    }
}
