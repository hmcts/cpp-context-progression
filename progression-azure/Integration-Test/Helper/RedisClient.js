const {createClient} = require('redis');
const {promisify} = require('util');

let redisClient;

class RedisClient {

    constructor(redisHost, redisPort, redisAuthKey) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisAuthKey = redisAuthKey;
    }

    getRedisClient() {
        if (redisClient == null) {
            redisClient = createClient(this.redisPort, this.redisHost, {
                auth_pass: this.redisAuthKey
            });
        }
        return redisClient;
    }

    async addKey(key, value) {
        const getAsync = promisify(this.getRedisClient().set).bind(redisClient);
        return getAsync(key, value);
    }
    
    async deleteKey(key){
        const getAsync = promisify(redisClient.del).bind(redisClient);
        return getAsync(key);
    }
}

module.exports = RedisClient;
