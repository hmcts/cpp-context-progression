package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;

public class Interpreter implements Serializable {

    private static final long serialVersionUID = 3596692979344216424L;
  
    private Boolean needed;
    private String language;

    public Interpreter() {
    }

    public Interpreter(Boolean needed, String language) {
        this.needed = needed;
        this.language = language;
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

}
