const { v4: uuidv4 } = require("uuid");
const crypto = require("crypto");

function makeDocId(sha256, language, author_uuid) {
  return `${sha256}:${language}:${author_uuid}`;
}

function makeFirestoreClient(firestore, encryptionKey, altCollection, authorCollection, authCollection) {
  return {
    getAuthor: makeGetAuthor(firestore, authorCollection),
    getOrCreateAuthorUuid: makeGetOrCreateAuthorUuid(
      firestore,
      authorCollection
    ),
    getAlt: makeGetAlt(firestore, altCollection),
    getSpecificAlt: makeGetSpecificAlt(firestore, altCollection),
    saveAlt: makeSaveAlt(firestore, altCollection),
    getRecord: makeGetRecord(firestore),
    getAll: makeGetAll(firestore),
    saveCredentials: makeSaveCreds(firestore, encryptionKey, authCollection),
  };
}

function makeSaveCreds(firestore, encryptionKey, collection) {
  const keyHash = crypto.createHash('sha256').update(String(encryptionKey)).digest('base64');
  
  return async (userId, accessToken, refreshToken, expiry) => {
    const creds = {
      user_id: userId,
      access_token: accessToken,
      refresh_token: refreshToken,
      expiry: Date.parse(expiry)
    };

    const toEncrypt = JSON.stringify(creds);
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv(
      "aes-256-cbc",
      Buffer.from(keyHash, "base64"),
      iv
    );
    const encrypted = cipher.update(toEncrypt);
    const finalized = Buffer.concat([encrypted, cipher.final()]);

    const toSave = { 
      user_id: userId,
      iv: iv.toString("hex"), 
      encrypted_creds: finalized.toString("hex") 
    };
    
    const coll = firestore.collection(collection);
    await coll.doc(userId).set(toSave, {merge: true});
  };
}

function makeGetAll(firestore) {
  return async (collection) => {
    const coll = firestore.collection(collection);
    const snapshot = await coll.get();
    const arr = [];
    snapshot.forEach((doc) => {
      arr.push(doc.data());
    });

    return arr;
  };
}

function makeGetRecord(firestore) {
  return async (collection, id) => {
    const coll = firestore.collection(collection);
    const result = await coll.doc(id).get();
    if (result.exists) {
      return result.data();
    } else {
      return null;
    }
  };
}

function makeGetSpecificAlt(firestore, collection) {
  return async (author_uuid, sha256, lang) => {
    const alt = firestore.collection(collection);

    const result = await alt.doc(makeDocId(sha256, lang, author_uuid)).get();
    if (result && result.exists) {
      const data = result.data();

      return {
        alt_text: data.alt_text,
        image_url: data.image_url,
        language: data.language,
        author_uuid: data.author_uuid,
        sha256: data.sha256,
      };
    }

    return null;
  };
}

function makeGetAlt(firestore, collection) {
  return async (sha256) => {
    const alt = firestore.collection(collection);

    const exact = await alt.where("sha256", "==", sha256).get();

    const result = [];
    exact.forEach((doc) => {
      const data = doc.data();
      result.push({
        alt_text: data.alt_text,
        image_url: data.image_url,
        language: data.language,
        author_uuid: data.author_uuid,
        sha256: data.sha256,
      });
    });

    return result;
  };
}

function makeGetAuthor(firestore, collection) {
  return async (authorUuid) => {
    const authors = firestore.collection(collection);
    const author = await authors
      .where("uuid", "array-contains", authorUuid)
      .get();

    if (author.empty) {
      return null;
    } else {
      const found = [];
      author.forEach((doc) => found.push(doc.data()));
      if (found.length === 0) {
        //Should be caught already, but...
        return null;
      } else if (found.length === 1) {
        return {
          scope: found[0].scope,
          id: found[0].id,
        };
      } else {
        console.log(`Found multiple authors for uuid ${authorUuid}!`);
        return null;
      }
    }
  };
}

function makeSaveAlt(firestore, collection) {
  return async (sha256, image_url, language, alt_text, author_uuid) => {
    const alt = firestore.collection(collection);

    let doc = {
      sha256: sha256,
      image_url: image_url,
      language: language,
      alt_text: alt_text,
      author_uuid: author_uuid,
    };

    await alt.doc(makeDocId(sha256, language, author_uuid)).set(doc);
  };
}

function makeGetOrCreateAuthorUuid(firestore, collection) {
  return async (scope, authorId) => {
    const authors = firestore.collection(collection);
    const doc = authors.doc(`${scope}:${authorId}`);
    const existing = await doc.get();

    if (existing.exists) {
      const uuids = existing.data().uuid;
      if (uuids.length > 0) {
        return uuids[0];
      }
    }

    const uuid = uuidv4();
    await doc.set(
      {
        scope: scope,
        id: authorId,
        uuid: [uuid],
      },
      { merge: true }
    );

    return uuid;
  };
}

exports.makeFirestoreClient = makeFirestoreClient;
