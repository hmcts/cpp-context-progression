
package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public final class CPR implements Serializable {
    
    private DefendantOffenderDomain defendantOffender = null;
    private final String cjsCode;
    private final String  offenceSequence;

    public CPR(final DefendantOffenderDomain defendantOffender,
               final String cjsCode,
               final String offenceSequence ){
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
 