
package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;

public final class CPR implements Serializable {
    
    private DefendantOffenderDomain defendantOffender = null;
    private String cjsCode;
    private String  offenceSequence;

    public CPR(DefendantOffenderDomain defendantOffender,
               String cjsCode,
               String offenceSequence ){
        this.defendantOffender = defendantOffender;
        this.cjsCode = cjsCode;
        this.offenceSequence = offenceSequence;
    }

    public DefendantOffenderDomain getDefendantOffender() {
        return defendantOffender;
    }
    public String getCjsCode() {
        return cjsCode;
    }
    public String getOffenceSequence() {
        return offenceSequence;
    }    
    
}
 