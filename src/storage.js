const MIN_RESULTS = 5;
const MIN_MATCH = 0

function makeStorage(pineconeClient, firestoreClient) {
    return {
        saveAlt: makeSaveAlt(pineconeClient, firestoreClient),
        getAlt: makeGetAlt(pineconeClient, firestoreClient),
    };
}

function makeSaveAlt(pinecone, firestore) {
    return async (sha256, dct1024, image_url, language, alt_text, scope, author_id) => {
        const uuid = await firestore.getOrCreateAuthorUuid(scope, author_id);
        await firestore.saveAlt(sha256, image_url, language, alt_text, uuid);
        return await pinecone.upsertDCT1024(sha256, dct1024);
    };
}

function makeGetAlt(pinecone, firestore) {
    return async (sha256, dct1024, language) => {
        const exact = await firestore.getAlt(sha256);
        if (!dct1024 || exact.length >= MIN_RESULTS) {
            return {
                exact: exact,
                fuzzy: []
            }
        }

        const fuzzyDCTShas = await pinecone.queryDCT1024(dct1024, MIN_RESULTS - exact.length);
        const fuzzy = []

        for (let fuzzySha of fuzzyDCTShas) {
            const alts = await firestore.getAlt(fuzzySha.sha256);
            alts.forEach(alt => {
                if (fuzzySha.score >= MIN_MATCH && fuzzySha.sha256 !== sha256) {
                    alt.score = fuzzySha.score;
                    fuzzy.push(alt);
                }
            })
        }

        return {
            exact: exact,
            fuzzy: fuzzy
        }
    }
}

exports.makeStorage = makeStorage