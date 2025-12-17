package uk.gov.moj.cpp.progression.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StatementOfOffence implements Serializable {

    private static final long serialVersionUID = -6662392831197623715L;
    private final String title;
    private final String legislation;

    @JsonCreator
    public StatementOfOffence(@JsonProperty("title") final String title,
                              @JsonProperty("legislation") final String legislation) {

        this.title = title;
        this.legislation = legislation;
    }

    public String getTitle() {
        return title;
    }

    public String getLegislation() {
        return legislation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatementOfOffence)) {
            return false;
        }

        final StatementOfOffence that = (StatementOfOffence) o;

        if (title != null ? !title.equals(that.title) : that.title != null) {
            return false;
        }
        return legislation != null ? legislation.equals(that.legislation) : that.legislation == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (legislation != null ? legislation.hashCode() : 0);
        return result;
    }

}
