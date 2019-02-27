package uk.gov.moj.cpp.progression.domain.event.defendant;


import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class OffencePlea implements Serializable {

    private static final long serialVersionUID = 182447333590503937L;

    private final UUID id;

    private final String value;

    private final LocalDate pleaDate;

    public UUID getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }

    public OffencePlea(final UUID id, final String value, final LocalDate pleaDate) {
        this.id = id;
        this.value = value;
        this.pleaDate = pleaDate;
    }
}
