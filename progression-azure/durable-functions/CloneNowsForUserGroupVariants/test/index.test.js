const cloneNowsForUserGroupVariants = require('../index');
const context = require('../../testing/defaultContext');

describe('Clone Nows For UserGroup Variants', () => {

    beforeEach(() => {
        context.log.warn = () => {};
    });

    test('should build valid user group variants based on judicial results', async () => {
        const nowVariantsJson = require('./now-variants.json');
        context.bindings = {
            params: {
                nowsVariants: nowVariantsJson
            }
        };

        const userGroupVariants =  await cloneNowsForUserGroupVariants(context);

        expect(userGroupVariants.length).toBe(7);
        assertUserGroupVariants(userGroupVariants);
    });

    test('should build valid user group variants based on judicial results and prompts', async () => {
        const nowVariantsJson = require('./now-variants-with-results-and-prompts.json');
        context.bindings = {
            params: {
                nowsVariants: nowVariantsJson
            }
        };

        const userGroupVariants = await cloneNowsForUserGroupVariants(context);

        expect(userGroupVariants.length).toBe(5);
        assertUserGroupVariantsWithResultsAndPrompts(userGroupVariants);
    });

    test('should return one user group variant for one judicial result with user group', async () => {
        const nowVariantsJson = require('./now-variants-with-one-result-with-user-group.json');
        context.bindings = {
            params: {
                nowsVariants: nowVariantsJson
            }
        };

        const userGroupVariants = await cloneNowsForUserGroupVariants(context);

        expect(userGroupVariants.length).toBe(1);

        assertUserGroupVariantsWithOneResultAndUserGroup(userGroupVariants);
    });

    test('should return one user group variant for one judicial result with no user group', async () => {
        const nowVariantsJson = require('./now-variants-with-one-result-without-user-groups.json');
        context.bindings = {
            params: {
                nowsVariants: nowVariantsJson
            }
        };

        const userGroupVariants = await cloneNowsForUserGroupVariants(context);

        expect(userGroupVariants.length).toBe(1);

        assertUserGroupVariantsWithOneResultAndWithOutUserGroup(userGroupVariants);
    });

    function assertUserGroupVariants(userGroupVariants) {
        expect(userGroupVariants[0].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d4");
        expect(userGroupVariants[0].userGroup.userGroups[0]).toBe("Listing Officers");
        expect(userGroupVariants[0].userGroup.type).toBe("include");

        expect(userGroupVariants[1].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d4");
        expect(userGroupVariants[1].userGroup.userGroups[0]).toBe("Crown Court Admin");
        expect(userGroupVariants[1].userGroup.type).toBe("include");

        expect(userGroupVariants[2].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[2].results[1].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d4");
        expect(userGroupVariants[2].userGroup.userGroups[0]).toBe("Listing Officers");
        expect(userGroupVariants[2].userGroup.userGroups[1]).toBe("Crown Court Admin");
        expect(userGroupVariants[2].userGroup.type).toBe("exclude");

        expect(userGroupVariants[3].results[0].judicialResultTypeId)
            .toBe("3f608dba-20ad-4710-bebc-d78b4b3ff08e");
        expect(userGroupVariants[3].results[1].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[3].userGroup.userGroups[0]).toBe("Defence");
        expect(userGroupVariants[3].userGroup.type).toBe("include");

        expect(userGroupVariants[4].results[0].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[4].results[1].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[4].userGroup.userGroups[0]).toBe("Listing Officers");
        expect(userGroupVariants[4].userGroup.type).toBe("include");

        expect(userGroupVariants[5].results[0].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[5].results[1].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[5].userGroup.userGroups[0]).toBe("Crown Court Admin");
        expect(userGroupVariants[5].userGroup.type).toBe("include");

        expect(userGroupVariants[6].results[0].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[6].results[1].judicialResultTypeId)
            .toBe("3f608dba-20ad-4710-bebc-d78b4b3ff08e");
        expect(userGroupVariants[6].results[2].judicialResultTypeId)
            .toBe("96349367-2d04-4265-978f-6c6b417497fd");
        expect(userGroupVariants[6].userGroup.userGroups[0]).toBe("Defence");
        expect(userGroupVariants[6].userGroup.userGroups[1]).toBe("Listing Officers");
        expect(userGroupVariants[6].userGroup.userGroups[2]).toBe("Crown Court Admin");
        expect(userGroupVariants[6].userGroup.type).toBe("exclude");

    }

    function assertUserGroupVariantsWithResultsAndPrompts(userGroupVariants) {
        expect(userGroupVariants[0].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[0].results[0].judicialResultPrompts[0].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fb");
        expect(userGroupVariants[0].results[0].judicialResultPrompts[1].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fc");
        expect(userGroupVariants[0].results[1].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d4");
        expect(userGroupVariants[0].results[1].judicialResultPrompts[0].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fa");
        expect(userGroupVariants[0].results[1].judicialResultPrompts[1].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fb");
        expect(userGroupVariants[0].results[1].judicialResultPrompts[2].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fc");
        expect(userGroupVariants[0].results[2].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d6");
        expect(userGroupVariants[0].results[3].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[0].userGroup.userGroups[0]).toBe("Listing Officers");
        expect(userGroupVariants[0].userGroup.type).toBe("include");

        expect(userGroupVariants[1].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[1].results[0].judicialResultPrompts[0].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fa");
        expect(userGroupVariants[1].results[0].judicialResultPrompts[1].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fb");
        expect(userGroupVariants[1].results[0].judicialResultPrompts[2].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fc");
        expect(userGroupVariants[1].results[1].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d4");
        expect(userGroupVariants[1].results[1].judicialResultPrompts[0].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fa");
        expect(userGroupVariants[1].results[1].judicialResultPrompts[1].judicialResultPromptTypeId)
            .toBe("1bcdae76-37f8-4331-9076-fd46dbd4b1fc");
        expect(userGroupVariants[1].results[2].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d5");
        expect(userGroupVariants[1].results[3].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[1].userGroup.userGroups[0]).toBe("Crown Court Admin");
        expect(userGroupVariants[1].userGroup.type).toBe("include");
    }

    function assertUserGroupVariantsWithOneResultAndUserGroup(userGroupVariants) {
        expect(userGroupVariants[0].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[0].userGroup.userGroups[0]).toBe("Listing Officers");
        expect(userGroupVariants[0].userGroup.type).toBe("exclude");
    }

    function assertUserGroupVariantsWithOneResultAndWithOutUserGroup(userGroupVariants) {
        expect(userGroupVariants[0].results[0].judicialResultTypeId)
            .toBe("cfbe83c8-935b-4410-a68c-49452519f2d3");
        expect(userGroupVariants[0].userGroup).toBe(undefined);
    }
});