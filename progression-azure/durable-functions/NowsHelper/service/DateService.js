const moment = require('moment');
const moment_timeZone = require('moment-timezone');

class DateService {

    parse(dateString, format = 'YYYY/MM/DD') {

        const momentDate = moment(dateString, format);

        if (momentDate.isValid()) {
            return momentDate.toDate();
        }

        throw new Error('Invalid date format');
    }

    isGreater(dateStringA, dateStringB) {

        const date1 = this.parse(dateStringA);
        const date2 = this.parse(dateStringB);

        return date1.getTime() > date2.getTime();
    }

    getLocalDate(dateString) {
        return moment_timeZone.tz(dateString, "Europe/London").format('YYYY-MM-DD');
    }

    getLocalTime(dateString) {
        return moment_timeZone.tz(dateString, "Europe/London").format('HH:mm');
    }

    getLocalDateTime(dateString) {
        return moment_timeZone.tz(dateString, "Europe/London").format('YYYY-MM-DDTHH:mm:ss')+'Z';
    }
}

module.exports = new DateService();