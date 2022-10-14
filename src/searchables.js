const crypto = require("crypto");
const path = require('path');
const fetch = require("node-fetch");
const Piscina = require('piscina');
const {createCanvas, loadImage, Image} = require('canvas')

const piscina = new Piscina({
    filename: path.resolve(__dirname, 'worker.js')
});

function ts() {
    return new Date().toISOString();
}

async function loadImageFromUrl(url) {
    return await fetch(url)
        .then(async resp => {
            if (resp && resp.ok) {
                return await resp.arrayBuffer()
            } else {
                console.log(`${ts()}: Failed to fetch ${url}: ${resp.status} ${resp.statusText}`)
                return null;
            }
        })
        .then(async buf => {
            if (buf) {
                return await loadImage(Buffer.from(buf))
            } else {
                return null
            }
        })
        .catch(err => {
            console.log(`${ts()}: Failed to fetch ${url}: ${err}`)
            return null
        })
}

async function imageBase64ToImageData(imageBase64) {
    const image = new Image()

    let prom = new Promise((res, rej) => {
        image.onload = () => res();
        image.onerror = (err) => rej(err);
    })

    image.src = imageBase64;
    await prom

    const canvas = createCanvas(image.width, image.height)
    const ctx = canvas.getContext("2d");
    canvas.width = image.width;
    canvas.height = image.height;
    ctx.clearRect(0, 0, image.width, image.height)
    ctx.drawImage(image, 0, 0)

    return ctx.getImageData(0, 0, image.width, image.height)
}

function sha256Image(imageData) {
    return crypto
        .createHash("sha256")
        .update(Buffer.from(imageData.data.buffer))
        .digest("hex");
}

async function searchablesForImageData(imageData) {
    return {
        sha256: sha256Image(imageData),
        goldberg544: await piscina.run({
            dataBuffer: imageData.data.buffer,
            height: imageData.height,
            width: imageData.width
        })
    }
}

async function searchablesForUrl(url) {
    let image = await loadImageFromUrl(url)
    if (!image) {
        console.log(`${ts()}: Failed to load image for ${url}`)
        return null
    }

    const canvas = createCanvas(image.width, image.height);
    let context = canvas.getContext("2d");
    context.drawImage(image, 0, 0);

    const imageData = context
        .getImageData(0, 0, canvas.width, canvas.height);

    return await searchablesForImageData(imageData)
}

async function searchablesForBase64(base64) {
    const imageData = await imageBase64ToImageData(base64).catch(err => {
        console.log(`${ts()}: Error getting image from base64:`)
        console.log(err)
        return null
    })

    if (imageData) {
        return await searchablesForImageData(imageData)
    } else {
        return null;
    }
}

exports.searchablesForUrl = searchablesForUrl;
exports.searchablesForBase64 = searchablesForBase64;