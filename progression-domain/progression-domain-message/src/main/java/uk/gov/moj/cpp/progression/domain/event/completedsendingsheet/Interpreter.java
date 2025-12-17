package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import java.io.Serializable;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class Interpreter implements Serializable{
    private static final long serialVersionUID = 6361746508921393405L;
    
    private boolean needed;
    private String language;

    public boolean getNeeded() {
        return this.needed;
    }

    public void setNeeded(final boolean needed) {
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }
}