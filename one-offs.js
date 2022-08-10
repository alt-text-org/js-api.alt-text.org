const { createCanvas, loadImage, Image } = require("canvas");
const fetch = require("node-fetch");

const { dct1024Image } = require("./dct")

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

function shrinkImage(image, imageData, edgeLength) {
    let canvas = createCanvas(edgeLength, edgeLength);

    let ctx = canvas.getContext("2d");

    ctx.drawImage(image, 0, 0, imageData.width, imageData.height, 0, 0, edgeLength, edgeLength)
    return ctx.getImageData(0, 0, edgeLength, edgeLength);
}

function toGreyscale(imageData) {
    let rgba = new Uint8Array(imageData.data.buffer);
    let greyscale = new Uint8Array(rgba.length / 4);
    for (let i = 0, j = 0; i < rgba.length; i += 4, j++) {
        let intensity = (rgba[i] + rgba[i + 1] + rgba[i + 2]) * (rgba[i + 3] / 255.0);
        greyscale[j] = Math.round((intensity / 765) * 255);
    }

    return greyscale;
}

function toBoolean(greyed) {
    let avgIntensity = greyed.reduce((prev, curr) => prev + curr, 0) / greyed.length
    return greyed.map(pix => pix >= avgIntensity ? 1 : 0)
}

async function intensity(image, imageData) {
    let shrunk = shrinkImage(image, imageData, 32);
    let greyed = toGreyscale(shrunk);
    let booled = toBoolean(greyed)
    return Array.from(booled)
}

async function portToIntensity(firestore, pinecone) {
  let records = await firestore.getAll("alt-text");
  records.forEach((record) => {
    loadImageFromUrl(record.image_url).then(async (img) => {
      if (img) {
        const canvas = createCanvas(img.width, img.height);
        let context = canvas.getContext("2d");
        context.drawImage(img, 0, 0);

        const imgData = context.getImageData(0, 0, canvas.width, canvas.height);
        const intensityVec = await intensity(img, imgData)
        const szent = await pinecone.upsertIntensity(record.sha256, intensityVec)
        console.log(`URL: ${record.image_url} Sent: ${szent}`)
      } else {
        console.log(`Couldn't fetch ${record.image_url}`);
        return null;
      }
    });
  });
}

async function portToDct1024(firestore, pinecone) {
  let records = await firestore.getAll("alt-text");
  records.forEach((record) => {
    loadImageFromUrl(record.image_url).then(async (img) => {
      if (img) {
        const canvas = createCanvas(img.width, img.height);
        let context = canvas.getContext("2d");
        context.drawImage(img, 0, 0);

        const imgData = context.getImageData(0, 0, canvas.width, canvas.height);
        const vec = await dct1024Image(img, imgData)
        const szent = await pinecone.upsertDCT1024(record.sha256, vec)
        console.log(`URL: ${record.image_url} Sent: ${szent}`)
      } else {
        console.log(`Couldn't fetch ${record.image_url}`);
        return null;
      }
    });
  });
}

exports.portToIntensity = portToIntensity;
exports.portToDct1024 = portToDct1024