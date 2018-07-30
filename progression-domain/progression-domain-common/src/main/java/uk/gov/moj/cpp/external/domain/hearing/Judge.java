package uk.gov.moj.cpp.external.domain.hearing;

import java.io.Serializable;
import java.util.UUID;

public class Judge implements Serializable {

    private static final long serialVersionUID = 4809327475924680554L;

    private UUID id;
    private String title;
    private String firstName;
    private String lastName;

    public Judge(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
