package uk.gov.moj.cpp.progression.service.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffenceToCommittingCourtConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffenceToCommittingCourtConverter.class.getName());

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067"})
    public Optional<CommittingCourt> convert(final Offence offence, final CourtCentre courtCentre, final Boolean shouldPopulateCommittingCourt) {

        LOGGER.debug("Populating committing court details for offence: {}", offence.getId());

        final List<JudicialResult> judicialResults = offence.getJudicialResults();

        if (shouldPopulateCommittingCourt && nonNull(courtCentre) && nonNull(judicialResults) && isNotEmpty(judicialResults) && isNull(offence.getCommittingCourt())) {
            for (final JudicialResult judicialResult : judicialResults) {
                if (nonNull(judicialResult.getNextHearing()) && nonNull(judicialResult.getNextHearing().getJurisdictionType()) &&
                        judicialResult.getNextHearing().getJurisdictionType().equals(JurisdictionType.CROWN)) {
                    LOGGER.debug("Returning committing court details using court centre: {}", courtCentre.getId());
                    return Optional.of(CommittingCourt.committingCourt()
                            .withCourtHouseName(courtCentre.getName())
                            .withCourtHouseCode(courtCentre.getCode())
                            .withCourtHouseShortName(courtCentre.getCode())
                            .withCourtHouseType(JurisdictionType.MAGISTRATES)
                            .withCourtCentreId(courtCentre.getId())
                            .build());
                }
            }
        }

        LOGGER.debug("Committing court details not populated for offence: {}", offence.getId());

        return Optional.empty();
    }
}
