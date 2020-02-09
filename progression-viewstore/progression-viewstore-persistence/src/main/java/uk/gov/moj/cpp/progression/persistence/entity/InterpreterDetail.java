package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Embeddable
public class InterpreterDetail implements Serializable{

    private static final long serialVersionUID = 1L;

    @Column(name="interpreter_needed")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean needed;
    
    @Column(name="interpreter_language")
    private String language;

    public InterpreterDetail(final Boolean needed, final String language) {
        this.needed = needed;
        this.language = language;
    }
    
    public InterpreterDetail(){
    }

    public Boolean getNeeded() {
        return needed;
    }

    public void setNeeded(final Boolean needed) {
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "InterpreterDetail [needed=" + needed + ", language=" + language + "]";
    }
    
    
}
