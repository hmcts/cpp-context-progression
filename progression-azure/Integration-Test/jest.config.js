module.exports = {
    setupFilesAfterEnv: ['./jest.setup.js'],
    collectCoverage: true,
    collectCoverageFrom: ['**/*.js', '!./*', '!coverage/**/*.js', '!testing/**/*.js'],
    moduleDirectories:['node_modules', './NowsHelper/service']
};
