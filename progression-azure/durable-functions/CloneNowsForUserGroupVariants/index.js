const lodash = require('lodash');
const UserGroup = require('../SetNowVariants/UserGroup');
const UserGroupType = require('../SetNowVariants/UserGroupType');

class UserGroupVariants {
    constructor(context, nowsVariants) {
        this.context = context;
        this.nowsVariants = nowsVariants;
    }

    buildUserGroupVariants() {

        let clonedUserGroupVariants = [];

        this.nowsVariants.forEach(nowVariant => {

            const variantUserGroups = this.buildVariantUserGroupsWithResults(nowVariant);

            const clonedNowVariants = this.cloneUserGroupVariants(variantUserGroups.size, nowVariant);

            const flattenUserGroups = Array.from(variantUserGroups);

            flattenUserGroups.forEach((userGroup, i) => {
                const clonedNowVariant = clonedNowVariants[i];
                this.filterResultsAndPrompts(userGroup, clonedNowVariant);
            });

            if (flattenUserGroups.length) {
                this.enrichPublicNowVariantForExcludedUserGroups(
                    clonedNowVariants[variantUserGroups.size], flattenUserGroups);
            }

            const filteredClonedNowVariantsWithResults = clonedNowVariants.filter(nowVariant => nowVariant.results.length);

            [...filteredClonedNowVariantsWithResults].forEach(nowVariant => clonedUserGroupVariants.push(nowVariant));

        });

        clonedUserGroupVariants.forEach(clonedUserGroupVariant => {
            if(clonedUserGroupVariant.userGroup) {
                const type = clonedUserGroupVariant.userGroup.type;
                const usergroup = type === 'exclude' || type === undefined ? 'public' : clonedUserGroupVariant.userGroup.userGroups;
                this.context.log.warn(JSON.stringify(clonedUserGroupVariant.now.name) + ' is ' + usergroup + ' copy.');
            }
        });

        this.context.log.warn('Total Number of User Group Variants created : ' + clonedUserGroupVariants.length);
        return clonedUserGroupVariants;
    }

    enrichPublicNowVariantForExcludedUserGroups(nowVariant, flattenUserGroups) {
        // Add excluded userGroups to Now Variant
        const userGroup = new UserGroup();
        userGroup.userGroups = flattenUserGroups;
        userGroup.type = UserGroupType.EXCLUDE;
        nowVariant.userGroup = userGroup;
    }

    filterResultsAndPrompts(groupName, clonedNowVariant) {
        const filteredResults = clonedNowVariant.results.filter(result => {
            if (!result.usergroups) {
                return true;
            }
            return !result.usergroups.includes(groupName);
        });

        filteredResults.forEach(result => {
            if (result.judicialResultPrompts) {
                result.judicialResultPrompts =
                    result.judicialResultPrompts.filter(prompt => {
                        if (!prompt.usergroups) {
                            return true;
                        }
                        return !prompt.usergroups.includes(groupName);
                    });
            }
        });

        // Add userGroup name to Now Variant
        const userGroup = new UserGroup();
        userGroup.userGroups.push(groupName);
        userGroup.type = UserGroupType.INCLUDE;
        clonedNowVariant.userGroup = userGroup;

        clonedNowVariant.results = filteredResults;

    }

    cloneUserGroupVariants(variantUserGroupsSize, nowVariant) {
        const clonedNowVariants = [];

        for (let i = 0; i < variantUserGroupsSize + 1; i++) {
            clonedNowVariants.push(lodash.cloneDeep(nowVariant));
        }

        return clonedNowVariants;
    }

    buildVariantUserGroupsWithResults(nowVariant) {
        const variantUserGroups = new Set();

        nowVariant.results.forEach(result => {
            if (result.usergroups) {
                result.usergroups.forEach(userGroup => {
                    variantUserGroups.add(userGroup);
                });

                if (result.judicialResultPrompts) {
                    result.judicialResultPrompts.forEach(prompt => {
                        if (prompt.usergroups) {
                            prompt.usergroups.forEach(userGroup => {
                                variantUserGroups.add(userGroup);
                            });
                        }
                    });
                }
            }
        });

        return variantUserGroups;
    }
}

module.exports = async (context) => {
    const nowsVariants = context.bindings.params.nowsVariants;
    return await new UserGroupVariants(context, nowsVariants).buildUserGroupVariants();
};