const {createCanvas, loadImage} = require("canvas");
const fetch = require("node-fetch");

const {goldberg} = require("./goldberg/image_signature");

function ts() {
    return new Date().toISOString();
}

async function loadImageFromUrl(url) {
    return await fetch(url)
        .then(async (resp) => {
            if (resp && resp.ok) {
                return await resp.arrayBuffer();
            } else {
                console.log(
                    `${ts()}: Failed to fetch ${url}: ${resp.status} ${resp.statusText}`
                );
                return null;
            }
        })
        .then(async (buf) => {
            if (buf) {
                return await loadImage(Buffer.from(buf));
            } else {
                return null;
            }
        })
        .catch((err) => {
            console.log(`${ts()}: Failed to fetch ${url}: ${err}`);
            return null;
        });
}

async function portToGoldberg(firestore, pinecone) {
    let records = await firestore.getAll("alt-text");
    records.forEach((record) => {
        loadImageFromUrl(record.image_url).then(async (img) => {
            if (img) {
                const canvas = createCanvas(img.width, img.height);
                let context = canvas.getContext("2d");
                context.drawImage(img, 0, 0);

                const imgData = context.getImageData(0, 0, canvas.width, canvas.height);
                const vec = await goldberg(img, imgData)
                const sent = await pinecone.upsert(record.sha256, vec)
                console.log(`URL: ${record.image_url} Sent: ${sent}`)
            } else {
                console.log(`Couldn't fetch ${record.image_url}`);
                return null;
            }
        });
    });
}

exports.portToGoldberg = portToGoldberg
