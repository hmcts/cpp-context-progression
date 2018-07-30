package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.progression.activiti.common.EndDate;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SendingSheetCompleteToListingCaseConverter implements Converter<SendingSheetCompleted, ListingCase> {

    private final Function<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence, Offence> mapToListingOffence = progressionOffence -> new Offence(progressionOffence.getId(), progressionOffence.getOffenceCode(),
            progressionOffence.getStartDate(), (progressionOffence.getEndDate() == null ? EndDate.VALUE : progressionOffence.getEndDate()),
            null
    );

    private final Function<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant, Defendant> mapToListingDefendant = (uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant defendant) -> {
        final List<Offence> offences = defendant.getOffences().stream().map(mapToListingOffence)
                .collect(Collectors.toList());

        return new Defendant(
                defendant.getId(),
                defendant.getPersonId(),
                defendant.getFirstName(),
                defendant.getLastName(),
                defendant.getDateOfBirth(),
                defendant.getBailStatus(),
                defendant.getCustodyTimeLimitDate(),
                defendant.getDefenceOrganisation(),
                offences);
    };


    @Override
    public ListingCase convert(final SendingSheetCompleted sendingSheetCompleted) {
        return transformListingCase(sendingSheetCompleted);
    }

    private ListingCase transformListingCase(final SendingSheetCompleted sendingSheetCompleted) {

        final List<Hearing> hearings = new ArrayList<>();

        final List<Defendant> listingDefendants = sendingSheetCompleted.getHearing().getDefendants().stream().map(mapToListingDefendant).collect(Collectors.toList());

        final Hearing hearing = new Hearing(
                UUID.randomUUID(),
                sendingSheetCompleted.getCrownCourtHearing().getCourtCentreId(),
                null,
                sendingSheetCompleted.getCrownCourtHearing().getCcHearingDate(),
                null,
                listingDefendants,
                null,null,null
        );
        hearings.add(hearing);

        return new ListingCase(
                sendingSheetCompleted.getHearing().getCaseId(),
                sendingSheetCompleted.getHearing().getCaseUrn(),
                hearings
        );
    }

}
