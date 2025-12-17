package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

public class FeeAggregate implements Aggregate {

    private static final long serialVersionUID = -923818658332716L;

    private CivilFees civilFees;

    @Override
    public Object apply(final Object event) {

        return match(event).with(
                when(CivilFeesUpdated.class).apply(e -> {

                    civilFees = CivilFees.civilFees()
                            .withFeeId(e.getFeeId())
                            .withFeeType(FeeType.valueOf(e.getFeeType()))
                            .withFeeStatus(e.getFeeStatus())
                            .withPaymentReference(e.getPaymentReference())
                            .build();
                    // Do something here if needed.
                }),
                when(CivilFeesAdded.class).apply(e -> {
                    civilFees = CivilFees.civilFees()
                            .withFeeId(e.getFeeId())
                            .withFeeType(FeeType.valueOf(e.getFeeType()))
                            .withFeeStatus(e.getFeeStatus())
                            .withPaymentReference(e.getPaymentReference())
                            .build();
                    // Do something here if needed.
                }),

                otherwiseDoNothing());
    }

    public Stream<Object> updateCivilFees(final CivilFees civilFee) {
        return apply(Stream.of(CivilFeesUpdated
                .civilFeesUpdated()
                .withFeeId(civilFee.getFeeId())
                .withFeeType(civilFee.getFeeType().name())
                .withFeeStatus(civilFee.getFeeStatus())
                .withPaymentReference(civilFee.getPaymentReference())
                .build()));
    }

    public Stream<Object> addCivilFee(final CivilFees civilFee) {
        return apply(Stream.of(CivilFeesAdded
                .civilFeesAdded()
                .withFeeId(civilFee.getFeeId())
                .withFeeType(civilFee.getFeeType().name())
                .withFeeStatus(civilFee.getFeeStatus())
                .withPaymentReference(civilFee.getPaymentReference())
                .build()));
    }

    public CivilFees getCivilFees() {
        return civilFees;
    }
}


