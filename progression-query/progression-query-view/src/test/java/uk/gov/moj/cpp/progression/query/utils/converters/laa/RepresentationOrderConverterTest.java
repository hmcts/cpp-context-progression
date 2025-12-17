package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.progression.query.laa.CourtCentre;
import uk.gov.justice.progression.query.laa.RepresentationOrder;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepresentationOrderConverterTest {

    @InjectMocks
    private RepresentationOrderConverter representationOrderConverter;

    @Test
    void shouldReturnNullWhenAssociatedDefenceOrganisationIsNull() {
        final RepresentationOrder result = representationOrderConverter.convert(null);

        assertThat(result, nullValue());
    }

    @Test
    void shouldConvertAssociatedDefenceOrganisation() {
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withApplicationReference("application reference")
                .withAssociationStartDate(LocalDate.now())
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber("laa contract number")
                        .build())
                .withAssociationEndDate(LocalDate.now())
                .build();

        final RepresentationOrder result = representationOrderConverter.convert(associatedDefenceOrganisation);

        assertThat(result.getApplicationReference(), is(associatedDefenceOrganisation.getApplicationReference()));
        assertThat(result.getEffectiveFromDate(), is(associatedDefenceOrganisation.getAssociationStartDate().toString()));
        assertThat(result.getLaaContractNumber(), is(associatedDefenceOrganisation.getDefenceOrganisation().getLaaContractNumber()));
        assertThat(result.getEffectiveToDate(), is(associatedDefenceOrganisation.getAssociationEndDate().toString()));
    }

    @Test
    void shouldConvertAssociatedDefenceOrganisationWhenThereAreNullValues() {
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withApplicationReference("application reference")
                .withAssociationStartDate(null)
                .withDefenceOrganisation(null)
                .withAssociationEndDate(null)
                .build();

        final RepresentationOrder result = representationOrderConverter.convert(associatedDefenceOrganisation);

        assertThat(result.getApplicationReference(), is(associatedDefenceOrganisation.getApplicationReference()));
        assertThat(result.getEffectiveFromDate(), nullValue());
        assertThat(result.getLaaContractNumber(), nullValue());
        assertThat(result.getEffectiveToDate(), nullValue());
    }

}