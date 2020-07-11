const dateService = require('./DateService');
const moment_timeZone = require('moment-timezone');

describe("DateService", () => {

    test("it should return the correct date", () => {

        const year = 2020;
        const day = 29;
        const month = 10;

        const dateToParse = `${year}-${month}-${day}`;

        const parsedDate = dateService.parse(dateToParse);

        expect(parsedDate.getFullYear()).toBe(year);
        expect(parsedDate.getMonth() + 1).toBe(month);
        expect(parsedDate.getDate()).toBe(day);

    });

    test("isGreater should return false when date is newer", () => {

        const date1 = '2020-01-10';
        const date2 = '2020-01-19'
        const isGreater = dateService.isGreater(date1, date2);

        expect(isGreater).toBe(false);
    });

    test("isGreater should return true when date is older", () => {

        const date1 = '2020-01-29';
        const date2 = '2020-01-19'
        const isGreater = dateService.isGreater(date1, date2);

        expect(isGreater).toBe(true);
    });

    test("should return local date", () => {
        const utcDate = '2020-06-19T09:00:00.000Z';
        const localDate = dateService.getLocalDate(utcDate);
        const expectedDate = moment_timeZone.tz(utcDate, "Europe/London").format('YYYY-MM-DD');

        expect(localDate).toBe(expectedDate);
    });

    test("should return local time", () => {
        const utcDate = '2020-06-19T09:00:00.000Z';
        const localTime = dateService.getLocalTime(utcDate);
        const expectedTime = moment_timeZone.tz(utcDate, "Europe/London").format('HH:mm');

        expect(localTime).toBe(expectedTime);
    });

    test("should return local date time", () => {
        const utcDate = '2020-06-19T09:00:00.000Z';
        const localDateTime = dateService.getLocalDateTime(utcDate);
        const expectedDateTime = moment_timeZone.tz(utcDate, "Europe/London").format('YYYY-MM-DDTHH:mm:ss')+'Z';

        expect(localDateTime).toBe(expectedDateTime);
    });
});
