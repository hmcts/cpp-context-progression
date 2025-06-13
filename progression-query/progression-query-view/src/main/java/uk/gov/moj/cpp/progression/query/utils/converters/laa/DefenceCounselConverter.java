package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.progression.query.laa.DefenceCounsel;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
@SuppressWarnings("squid:S1168")
public class DefenceCounselConverter extends LAAConverter {

    public List<DefenceCounsel> convert(final List<uk.gov.justice.core.courts.DefenceCounsel> defenceCounsels) {
        if (isEmpty(defenceCounsels)) {
            return null;
        }
        return defenceCounsels.stream().map(defenceCounsel -> DefenceCounsel.defenceCounsel()
                .withId(defenceCounsel.getId())
                .withAttendanceDays(ofNullable(defenceCounsel.getAttendanceDays())
                        .map(attendanceDays -> attendanceDays.stream()
                                .map(LocalDate::toString)
                                .toList())
                        .orElse(null))
                .withDefendants(defenceCounsel.getDefendants())
                .withFirstName(defenceCounsel.getFirstName())
                .withMiddleName(defenceCounsel.getMiddleName())
                .withLastName(defenceCounsel.getLastName())
                .withStatus(defenceCounsel.getStatus())
                .withTitle(defenceCounsel.getTitle())
                .build()).collect(Collectors.toList());
    }
}