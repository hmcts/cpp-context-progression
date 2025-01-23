package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;

public class FixList implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

    String key;
    String value;
    Boolean isDefendant;

    public FixList(final String key, final String value, final Boolean isDefendant) {
        this.key = key;
        this.value = value;
        this.isDefendant = isDefendant;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public static String getValue(FixList flv) {
        return flv.getValue();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Boolean getIsDefendant() {
        return isDefendant;
    }

    public void setIsDefendant(final Boolean defendant) {
        isDefendant = defendant;
    }
}