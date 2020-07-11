const setMdeVariants = require('../index');
const context = require('../../testing/defaultContext');

const NO_MDE = require('./no_mde.json');
const TWO_RESULTS_DIFFERENT_PROMPT_VALUES = require('./two_results_different_values.json');
const TWO_RESULTS_SAME_PROMPT_VALUES = require('./two_results_same_values.json');
const FOUR_RESULTS_WITH_DUPLICATE_SAME_VALUES = require('./four_results_with_duplicate_same_values.json');
const THREE_RESULTS_ONE_DIFFERENT_VALUE = require('./three_results_one_different_value.json');
const ONE_RESULT = require('./one_result.json');
const SIX_RESULTS_TWO_DIFFERENT_RESULT_TYPES = require('./six_results_two_different_result_types.json');
const SIX_RESULTS_DIFFERENT_RESULT_TYPE_SAME_VALUES = require('./six_results_different_result_type_same_values.json');
const SIX_RESULTS_DIFFERENT_RESULT_TYPE_DIFFERENT_VALUES = require('./six_results_different_result_type_different_values.json');
const MULTIPLE_USERGROUPS_MULTIPLE_RESULTS_DIFFERENT_VALUES = require('./multiple_usergroups_multiple_results_different_values.json');
const NEXT_HEARING = require('./nextHearing.json');
const NEXT_HEARING_2 = require('./next-hearing-2.json');

describe('Set MDE Variants', () => {

    test('should return the correct MDE Variants when NO MDE prompts are present', async () => {
        context.bindings = {
            params: {
                userGroupsNowVariants: NO_MDE
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(NO_MDE.length);
    });

    test('should return the correct MDE Variants when prompts values are different', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: TWO_RESULTS_DIFFERENT_PROMPT_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(1);
        expect(mdeVariants[0].results.length).toBe(1);
    });

    test('should return the correct MDE Variants when prompts values are the same', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: TWO_RESULTS_SAME_PROMPT_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(1);
        expect(mdeVariants[0].results.length).toBe(2);
    });

    test('should return the correct MDE Variants when duplicate prompts values are the same', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: FOUR_RESULTS_WITH_DUPLICATE_SAME_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(1);
        expect(mdeVariants[0].results.length).toBe(2);
    });

    test('should return the correct MDE Variants when a group of results have the same prompts', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: THREE_RESULTS_ONE_DIFFERENT_VALUE
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(2);
        expect(mdeVariants[0].results.length).toBe(2);
        expect(mdeVariants[1].results.length).toBe(1);
    });

    test('should return the correct MDE Variants when only one mde result', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: ONE_RESULT
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(1);
        expect(mdeVariants[0].results.length).toBe(1);
    });

    test('should return the correct MDE Variants when UserGroupVariant contains a mixture of results', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: SIX_RESULTS_TWO_DIFFERENT_RESULT_TYPES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(3);
        expect(mdeVariants[0].results.length).toBe(3);
        expect(mdeVariants[1].results.length).toBe(1);
        expect(mdeVariants[2].results.length).toBe(2);
    });

    test('should return the correct MDE Variants when UserGroupVariant contains different prompt definitions', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: SIX_RESULTS_DIFFERENT_RESULT_TYPE_SAME_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(3);
        expect(mdeVariants[0].results.length).toBe(3);
        expect(mdeVariants[1].results.length).toBe(2);
        expect(mdeVariants[2].results.length).toBe(1);

    });

    test('should return the correct MDE Variants when UserGroupVariant contains different result definitions and different values', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: SIX_RESULTS_DIFFERENT_RESULT_TYPE_DIFFERENT_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(2);
        expect(mdeVariants[0].results.length).toBe(3);
        expect(mdeVariants[1].results.length).toBe(1);
    });

    test('should return the correct MDE Variants with multiple UserGroupVariants containing different result definitions and different values', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: MULTIPLE_USERGROUPS_MULTIPLE_RESULTS_DIFFERENT_VALUES
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(4);
        expect(mdeVariants[0].results.length).toBe(3);
        expect(mdeVariants[1].results.length).toBe(1);
        expect(mdeVariants[2].results.length).toBe(3);
        expect(mdeVariants[3].results.length).toBe(1);
    });

    test('should return the correct MDE Variants for next Hearing', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: NEXT_HEARING
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(2);
        expect(mdeVariants[0].results.length).toBe(3);
        expect(mdeVariants[1].results.length).toBe(3);
    });

    test('should return the correct MDE Variants for next Hearing 2', async () => {

        context.bindings = {
            params: {
                userGroupsNowVariants: NEXT_HEARING_2
            }
        };

        const mdeVariants = await setMdeVariants(context);

        expect(mdeVariants.length).toBe(6);
        expect(mdeVariants[0].results.length).toBe(2);
        expect(mdeVariants[1].results.length).toBe(2);
        expect(mdeVariants[2].results.length).toBe(2);
        expect(mdeVariants[3].results.length).toBe(3);
        expect(mdeVariants[4].results.length).toBe(3);
        expect(mdeVariants[5].results.length).toBe(3);
    });

});

