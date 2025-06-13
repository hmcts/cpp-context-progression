package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.PersonDefendant;
@SuppressWarnings("squid:S1168")
public class DefendantASNConverter extends LAAConverter {

    public String convert(final CourtApplicationParty subject) {
        return ofNullable(subject.getMasterDefendant())
                .map(MasterDefendant::getPersonDefendant)
                .map(PersonDefendant::getArrestSummonsNumber)
                .orElse(null);
    }
}