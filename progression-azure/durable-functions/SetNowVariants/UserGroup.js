const UserGroupType = require('./UserGroupType');

class UserGroup {
    constructor() {
        this.userGroups = [];
        this.type = UserGroupType;
    }
}

module.exports = UserGroup;