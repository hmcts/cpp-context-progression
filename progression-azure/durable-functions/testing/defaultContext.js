module.exports = {
    log: jest.fn(),
    df: {
        getInput() {
            return {
                cjscppuid: undefined
            };
        }
    },
    res: {
        status: 0
    }
};
