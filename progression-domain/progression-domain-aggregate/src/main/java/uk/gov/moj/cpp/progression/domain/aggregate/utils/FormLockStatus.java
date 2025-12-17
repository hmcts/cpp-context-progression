package uk.gov.moj.cpp.progression.domain.aggregate.utils;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class FormLockStatus implements Serializable {

    private static final long serialVersionUID = -93626292552177485L;
    private Boolean isLocked;
    private UUID lockedBy;
    private UUID lockRequestedBy;
    private ZonedDateTime lockExpiryTime;

    public FormLockStatus() {
    }

    public FormLockStatus(final Boolean isLocked, final UUID lockedBy, final UUID lockRequestedBy, final ZonedDateTime lockExpiryTime) {
        this.isLocked = isLocked;
        this.lockedBy = lockedBy;
        this.lockRequestedBy = lockRequestedBy;
        this.lockExpiryTime = lockExpiryTime;
    }

    public Boolean getLocked() {
        return isLocked;
    }

    public void setLocked(final Boolean locked) {
        isLocked = locked;
    }

    public UUID getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(final UUID lockedBy) {
        this.lockedBy = lockedBy;
    }

    public UUID getLockRequestedBy() {
        return lockRequestedBy;
    }

    public void setLockRequestedBy(final UUID lockRequestedBy) {
        this.lockRequestedBy = lockRequestedBy;
    }

    public ZonedDateTime getLockExpiryTime() {
        return lockExpiryTime;
    }

    public void setLockExpiryTime(final ZonedDateTime lockExpiryTime) {
        this.lockExpiryTime = lockExpiryTime;
    }
}