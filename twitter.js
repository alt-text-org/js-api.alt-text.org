const { Client, auth } = require("twitter-api-sdk");

function makeAuthClient(clientId, clientSecret) {
  return new auth.OAuth2User({
    client_id: clientId,
    client_secret: clientSecret,
    callback: "https://api.alt-text.org/v1/alt-library/sign-up-callback",
    scopes: ["tweet.read", "users.read", "offline.access"],
  });
}

function makeUserClient(accessToken) {
  return new Client(new auth.OAuth2Bearer(accessToken))
}

exports.makeTwitterAuthClient = makeAuthClient;
exports.makeTwitterUserClient = makeUserClient