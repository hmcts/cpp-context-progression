package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
public class PleaView {

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

    public PleaView(final UUID id, final String value, final LocalDate pleaDate) {
        this.id = id;
        this.value = value;
        this.pleaDate = pleaDate;
    }
}
