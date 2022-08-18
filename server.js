const crypto = require("crypto");
const fs = require("fs");

const fastify = require("fastify")({
    logger: false,
});
const {initializeApp, cert} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");

const {makePineconeClient} = require("./src/pinecone");
const {makeFirestoreClient} = require("./src/firestore");
const {makeStorage} = require("./src/storage");
const {ocrUrl, ocrBase64} = require("./src/ocr");
const {makeTwitterAuthClient, makeTwitterUserClient} = require("./src/twitter");
const {searchablesForUrl, searchablesForBase64} = require("./src/searchables");

fastify.register(require("@fastify/cors"), {
    methods: ["POST", "PUT", "GET", "OPTIONS"],
});

fastify.register(require("@fastify/rate-limit"), {
    max: 60, timeWindow: "1 minute",
});

const config = {
    googleCreds: process.env.GOOGLE_APPLICATION_CREDENTIALS,
    pineconeApiKey: process.env.PINECONE_API_KEY,
    writerToken: `Bearer ${process.env.WRITER_TOKEN}`,
    firestore: {
        project: process.env.GOOGLE_PROJECT,
    },
    twitter: {
        clientId: process.env.TWITTER_CLIENT_ID,
        clientSecret: process.env.TWITTER_CLIENT_SECRET,
        credEncryptionKey: process.env.TWITTER_AUTH_ENCRYPTION_KEY,
    },
};

const serviceAccount = require(config.googleCreds);
initializeApp({
    credential: cert(serviceAccount),
});

const twitterAuth = makeTwitterAuthClient(config.twitter.clientId, config.twitter.clientSecret);

const pinecone = makePineconeClient(config.pineconeApiKey);
const firestore = makeFirestoreClient(getFirestore(), config.twitter.credEncryptionKey, "alt-text", "authors", "twitter-auth");
const storage = makeStorage(pinecone, firestore);

const twitterStates = {};

function ts() {
    return new Date().toISOString();
}

async function getSearchables(base64, url) {
    if (base64) {
        return await searchablesForBase64(base64);
    } else if (url) {
        return await searchablesForUrl(url);
    } else {
        throw new Error("Neither URL nor base64 image provided");
    }
}

async function getOCR(base64, url) {
    if (base64) {
        return await ocrBase64(base64);
    } else if (url) {
        return await ocrUrl(url);
    } else {
        throw new Error("Neither URL nor base64 image provided");
    }
}

const fetchOpts = {
    schema: {
        body: {
            type: "object", required: ["image"],
            properties: {
                image: {
                    type: "object",
                    properties: {
                        url: {type: "string"},
                        base64: {type: "string"},
                    },
                },
                language: {type: "string"},
            },
        }, response: {
            200: {
                type: "object", properties: {
                    ocr: {type: "string"},
                    exact: {type: "array", items: {type: "object"}},
                    fuzzy: {type: "array", items: {type: "object"}},
                },
            },
        },
    }, handler: (request, reply) => {
        const {image, language} = request.body;
        const start = Date.now()

        if (!image.base64 === !image.url) {
            reply.status(400).send({
                error: "Must include exactly one of image.url and image.base64",
            });
            return;
        }

        getSearchables(image.base64, image.url)
            .then((searchables) => {
                if (!searchables) {
                    console.log(`${ts()}: Failed to get searchables for ${image.url || "<base64>"}`);
                    reply.status(400).send({error: "Couldn't get searchables for requested image"});
                    return null;
                }

                return storage.getAlt(searchables.sha256, searchables.goldberg544, language);
            })
            .then(async (alt) => {
                let ocr = await getOCR(image.base64, image.url);
                if (ocr) {
                    alt.ocr = ocr;
                }

                return alt;
            })
            .then((alt) => {
                if (alt.exact.length + alt.fuzzy.length === 0 && !alt.ocr) {
                    console.log(`${ts()}: No alt text found for '${image.url || '<base64>'}' in ${Date.now() - start}ms`)
                    reply.status(404).send({error: "No matching alt text found"});
                } else {
                    console.log(`${ts()}: Found ${alt.exact.length} exact, ${alt.fuzzy.length} fuzzy, and ocr: ${!!alt.ocr} for '${image.url || '<base64>'}' in ${Date.now() - start}ms`)
                    reply.status(200).send(JSON.stringify(alt));
                }
            })
            .catch((err) => {
                console.log(`${ts()}: Failed to get alt text for ${image.url || "<base64>"}`);
                console.log(err);
                reply.status(500).send({error: "Internal server error"});
            });
    },
};
fastify.post("/library/v1/fetch", fetchOpts);

const saveOpts = {
    schema: {
        body: {
            type: "object", required: ["image", "language", "alt_text", "id_scope", "author_id"],
            properties: {
                image: {
                    type: "object", properties: {
                        url: {type: "string"},
                        base64: {type: "string"},
                    },
                },
                language: {type: "string"},
                alt_text: {type: "string"},
                id_scope: {type: "string"},
                author_id: {type: "string"},
            },
        },
    }, handler: (request, reply) => {
        if (request.headers.Authorization !== config.writerToken) {
            reply.status(401).send()
            return
        }

        const {image, language, alt_text, id_scope, author_id} = request.body;

        if (!alt_text) {
            reply.status(400).send({error: "alt_text must be provided and non-empty"});
        }

        if (!image.base64 === !image.url) {
            reply.status(400).send({
                error: "Must include exactly one of image.url and image.base64",
            });
            return;
        }

        getSearchables(image.base64, image.url).then(async (searchables) => {
            if (!searchables) {
                console.log(`${ts()}: Failed to get searchables for ${image.url || "<base64>"}`);
                reply.status(400).send({error: "Couldn't get searchables for requested image"});
                return;
            }

            const success = await storage.saveAlt(searchables.sha256, searchables.goldberg544, image.url, language, alt_text, id_scope, author_id);
            if (success) {
                console.log(`${ts()}: Successfully saved alt text for ${image.url || "<base64>"}`);
                reply.status(204).send();
            } else {
                console.log(`${ts()}: Failed to save alt text for ${image.url || "<base64>"}`);
                reply.status(500).send({error: "Internal server error"});
            }
        }).catch((err) => {
            console.log(`${ts()}: Error saving alt text for ${image.url || "<base64>"}`);
            console.log(err);
            reply.status(500).send({error: "Internal server error"});
        });
    },
};
fastify.post("/library/v1/save", saveOpts);

const reportOpts = {
    schema: {
        body: {
            type: "object", required: ["author_uuid", "sha256", "language", "reason"], properties: {
                reason: {type: "string"},
                author_uuid: {type: "string"},
                sha256: {type: "string"},
                language: {type: "string"},
            },
        },
    }, handler: async (request, reply) => {
        const {reason, author_uuid, sha256, language} = request.body;

        const alt = await firestore.getSpecificAlt(author_uuid, sha256, language);
        if (!alt) {
            console.log(`${ts()}: Got report for a non-existent alt text at author: '${author_uuid}' lang: ${language} sha: ${sha256}`);
            reply.status(404).send({
                error: "No alt text found for that author, sha, and language",
            });
            return;
        }

        if (reason.length > 1000) {
            reply.status(400).send({error: "Reason must be at most 1000 characters"});
            return;
        }

        const report = {
            reason: reason, reported: alt,
        };

        try {
            fs.appendFileSync(".data/reports.nldjson", JSON.stringify(report) + "\n", "utf8");
            reply.status(204).send();
        } catch (err) {
            console.log(err);
            reply.status(500).send({error: "Internal server error"});
        }
    },
};
fastify.post("/library/v1/report", reportOpts);

const ocrOpts = {
    schema: {
        body: {
            type: "object", required: ["ocrTarget"], properties: {
                ocrTarget: {
                    type: "object", properties: {
                        base64Image: {type: "string"},
                    },
                },
            },
        },
    }, handler: (request, reply) => {
        const {ocrTarget} = request.body;

        if (!ocrTarget.base64Image) {
            reply.status(400).send(new Error("Missing base64Image"));
        }

        ocrBase64(ocrTarget.base64Image)
            .then((text) => {
                const resp = {
                    result: {
                        extractedText: text,
                    },
                };

                reply.status(200).send(JSON.stringify(resp));
            })
            .catch((err) => {
                console.log(`${ts()}: Failed to fetch text: ${err}`);
                reply.status(500).send();
            });
    },
};
fastify.post("/ocr/v1", ocrOpts);

const signupOpts = {
    handler: (request, reply) => {
        const state = crypto.randomBytes(16).toString("base64");
        twitterStates[state] = Date.now();

        const authUrl = twitterAuth.generateAuthURL({
            state: state, code_challenge_method: "plain", code_challenge: crypto.randomBytes(64).toString("base64"),
        });

        const body = {
            auth_url: authUrl,
        };

        reply.status(200).send(JSON.stringify(body));
    },
};
fastify.get("/library/v1/twitter-sign-up", signupOpts);

const MAX_STATE_LIFETIME_MILLIS = 5 * 60 * 1000;
const signupCallbackOpts = {
    schema: {
        querystring: {
            type: "object", properties: {
                url: {
                    type: "string",
                },
            },
        },
    }, handler: async (request, reply) => {
        try {
            const {code, state} = request.query;

            const stateIssueTime = twitterStates[state];
            if (!stateIssueTime) {
                reply.status(400).send({error: "Unknown state"});
            } else if (Date.now() - stateIssueTime > MAX_STATE_LIFETIME_MILLIS) {
                reply.status(400).send({
                    error: `State expired, must hit callback within ${MAX_STATE_LIFETIME_MILLIS} milliseconds`,
                });
            }

            delete twitterStates[state];
            const token = await twitterAuth.requestAccessToken(code);
            if (token && token.token && token.token.access_token) {
                const userClient = makeTwitterUserClient(token.token.access_token);
                const user = await userClient.users.findMyUser();
                if (user) {
                    await firestore.saveCredentials(user.data.id, token.token.access_token, token.token.refresh_token, token.token.expires_at);
                    console.log(`${ts()}: Saved credentials for user ${user.data.id}: @${user.data.username}`);
                    reply.redirect("https://alt-text.org/signed-up.html");
                } else {
                    reply.status(500).send({error: "Couldn't get user ID"});
                }
            } else {
                reply.status(500).send({error: "Couldn't get access token"});
            }
        } catch (error) {
            console.log(error);
            reply.status(500).send({error: "Internal Server Error"});
        }
    },
};
fastify.get("/library/v1/twitter-sign-up-callback", signupCallbackOpts);

function cleanTwitterStates() {
    for (let state in twitterStates) {
        let ts = twitterStates[state];
        if (Date.now() - ts > MAX_STATE_LIFETIME_MILLIS * 3) {
            console.log(`${ts}: Deleting expired state`);
            delete twitterStates[state];
        }
    }
}

const TEN_MIN_IN_MS = 10 * 60 * 1000;
setInterval(cleanTwitterStates, TEN_MIN_IN_MS);

fastify.listen({port: process.env.PORT}, function (err, address) {
    if (err) {
        fastify.log.error(err);
        process.exit(1);
    }
    console.log(`Your app is listening on ${address}`);
    fastify.log.info(`server listening on ${address}`);
});

