const {Client, auth} = require("twitter-api-sdk");

function makeAuthClient(clientId, clientSecret) {
    return new auth.OAuth2User({
        client_id: clientId,
        client_secret: clientSecret,
        callback: "https://api.alt-text.org/library/v1/twitter-sign-up-callback",
        scopes: ["tweet.read", "users.read", "offline.access"],
    });
}

function makeUserClient(accessToken) {
    return new Client(new auth.OAuth2Bearer(accessToken))
}

exports.makeTwitterAuthClient = makeAuthClient;
exports.makeTwitterUserClient = makeUserClient