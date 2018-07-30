package uk.gov.moj.cpp.external.domain.hearing;

import java.io.Serializable;

public class Interpreter implements Serializable{

    private static final long serialVersionUID = 6361746508921393405L;

    private Boolean needed;
    private String language;

    public Boolean getNeeded() {
        return this.needed;
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
}