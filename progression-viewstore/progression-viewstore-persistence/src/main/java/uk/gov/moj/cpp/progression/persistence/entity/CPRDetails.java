
package uk.gov.moj.cpp.progression.persistence.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import java.io.Serializable;

@Embeddable
public class CPRDetails implements Serializable {

    @AttributeOverrides({ @AttributeOverride(name = "year", column = @Column(name = "cpr_defendant_offender_year") ),
            @AttributeOverride(name = "organisationUnit", column = @Column(name = "cpr_defendant_offender_organisation_unit") ),
            @AttributeOverride(name = "number", column = @Column(name = "cpr_defendant_offender_number") ),
            @AttributeOverride(name = "checkDigit", column = @Column(name = "cpr_defendant_offender_check_digit") ) })
    @Embedded
    private DefendantOffenderDetails defendantOffender = null;

    public CPRDetails() {
        super();
    }

    public CPRDetails(DefendantOffenderDetails defendantOffender){
        this.defendantOffender = defendantOffender;
    }


    public DefendantOffenderDetails getDefendantOffender() {
        return defendantOffender;
    }
    public void setDefendantOffender(DefendantOffenderDetails defendantOffender) {
        this.defendantOffender = defendantOffender;
    }

}
