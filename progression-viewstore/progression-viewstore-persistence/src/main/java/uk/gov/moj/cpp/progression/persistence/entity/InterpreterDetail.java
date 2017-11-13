package uk.gov.moj.cpp.progression.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class InterpreterDetail implements Serializable{

    private static final long serialVersionUID = 1L;

    @Column(name="interpreter_needed")
    private Boolean needed;
    
    @Column(name="interpreter_language")
    private String language;

    public InterpreterDetail(Boolean needed, String language) {
        this.needed = needed;
        this.language = language;
    }
    
    public InterpreterDetail(){
    }

    public Boolean getNeeded() {
        return needed;
    }

    public void setNeeded(Boolean needed) {
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "InterpreterDetail [needed=" + needed + ", language=" + language + "]";
    }
    
    
}
