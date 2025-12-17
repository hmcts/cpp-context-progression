package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.justice.core.courts.HearingListingStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@SuppressWarnings("squid:S2384")
@Entity
@Table(name = "hearing")
public class HearingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "hearing_id", unique = true, nullable = false)
    private UUID hearingId;

    @Column(name = "payload")
    private String payload;

    @Column(name="listing_status")
    @Enumerated(EnumType.STRING)
    private HearingListingStatus listingStatus;

    @Column(name="confirmed_date")
    private LocalDate confirmedDate;

    @Column(name = "shared_time")
    private ZonedDateTime sharedTime;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hearing", orphanRemoval = true)
    private Set<HearingResultLineEntity> resultLines = new HashSet<>();

        public UUID getHearingId() {
        return hearingId;
    }

    public void setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public HearingListingStatus getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(final HearingListingStatus listingStatus) {
        this.listingStatus = listingStatus;
    }

    public Set<HearingResultLineEntity> getResultLines() {
        return resultLines;
    }

    public void addResultLine(final HearingResultLineEntity resultLine){
        resultLine.setHearing(this);
        resultLines.add(resultLine);
    }

    public void addResultLines(final List<HearingResultLineEntity> resultLines){
        resultLines.stream().forEach(hearingResultLineEntity -> {
            hearingResultLineEntity.setHearing(this);
            this.resultLines.add(hearingResultLineEntity);
        });
    }


    public void setResultLines(final Set<HearingResultLineEntity> resultLines) {
        this.resultLines = resultLines;
    }

    public LocalDate getConfirmedDate() {
        return confirmedDate;
    }

    public void setConfirmedDate(final LocalDate confirmedDate) {
        this.confirmedDate = confirmedDate;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public void setSharedTime(final ZonedDateTime sharedTime) {
        this.sharedTime = sharedTime;
    }
}
