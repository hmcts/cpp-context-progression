const Hearing = require('../../Model/Hearing');

class HearingMapper {
    constructor(hearingJson, prisonCourtRegister, defendant) {
        this.hearingJson = hearingJson;
        this.prisonCourtRegister = prisonCourtRegister;
        this.defendant = defendant;
    }

    isDefendantPresent() {
        if(this.hearingJson.defendantAttendance) {
            const defendantAttendance = this.hearingJson.defendantAttendance.find(d => d.defendantId = this.defendant.id);
            return !!defendantAttendance.attendanceDays.find(a => a.day === this.prisonCourtRegister.registerDate);
        }
        return false;
    }

    getDefendantAppearanceDetails() {
        if(this.hearingJson.defendantAttendance) {
            const defendantAttendance = this.hearingJson.defendantAttendance.find(d => d.defendantId = this.defendant.id);
            const attendanceDay = defendantAttendance.attendanceDays.find(a => a.day === this.prisonCourtRegister.registerDate)
            if(attendanceDay) {
                if (attendanceDay.attendanceType === 'IN_PERSON') {
                    return 'In person';
                } else if (attendanceDay.attendanceType === 'BY_VIDEO') {
                    return 'By video link';
                } else if (attendanceDay.attendanceType === 'NOT_PRESENT') {
                    return 'Not present';
                }
            }
        }
    }

    build() {
        const hearing = new Hearing();
        hearing.jurisdiction = this.hearingJson.jurisdictionType;
        hearing.hearingType = this.hearingJson.type.description;
        hearing.defendantPresent = this.isDefendantPresent();
        hearing.defendantAppearanceDetails = this.getDefendantAppearanceDetails();
        hearing.attendingSolicitorName = this.defendant.associatedDefenceOrganisation ? this.defendant.associatedDefenceOrganisation.defenceOrganisation.organisation.name : undefined;
        return hearing;
    }
}

module.exports = HearingMapper;