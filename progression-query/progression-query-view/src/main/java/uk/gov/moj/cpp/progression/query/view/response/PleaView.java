package uk.gov.moj.cpp.progression.query.view.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Created by jchondig on 04/12/2017.
 */
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

    public PleaView(UUID id, String value, LocalDate pleaDate) {
        this.id = id;
        this.value = value;
        this.pleaDate = pleaDate;
    }
}
