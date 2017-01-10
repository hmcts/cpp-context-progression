package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;

public class Classification implements Serializable {

    private static final long serialVersionUID = 7148568814869839878L;

    private String classificationType;
    private String classificationCode;
    private String classificationValue;

    public Classification(final String classificationType,
                                final String classificationCode,
                                final String classificationValue) {
        this.classificationType = classificationType;
        this.classificationCode = classificationCode;
        this.classificationValue = classificationValue;
    }

    public String getClassificationCode() {
        return classificationCode;
    }

    public void setClassificationCode(final String classificationCode) {
        this.classificationCode = classificationCode;
    }

    public String getClassificationValue() {
        return classificationValue;
    }

    public void setClassificationValue(final String classificationValue) {
        this.classificationValue = classificationValue;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public void setClassificationType(final String classificationType) {
        this.classificationType = classificationType;
    }

}

