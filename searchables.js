const crypto = require("crypto");
const fetch = require("node-fetch");
const {createCanvas, loadImage, Image} = require('canvas')

const { toGreyscale, shrinkImage, dct1024Image } = require("./dct")

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

    return {
        image: image,
        imageData: ctx.getImageData(0, 0, image.width, image.height)
    };
}

function sha256Image(image, imageData) {
    let resized = shrinkImage(image, imageData, 100)
    let greyscale = toGreyscale(resized)
    return crypto
        .createHash("sha256")
        .update(Buffer.from(greyscale))
        .digest("hex");
}

async function searchablesForImageData(image, imageData) {
    return {
        sha256: sha256Image(image, imageData),
        dct1024: await dct1024Image(image, imageData)
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

    return searchablesForImageData(image, imageData)
}

async function searchablesForBase64(base64) {
  const { image, imageData } = await imageBase64ToImageData(base64).catch(err => {
    console.log(`${ts()}: Error getting image from base64:`)
    console.log(err)
    return {
      image: null,
      imageData: null
    }
  })
  
  if (image && imageData) {
    return searchablesForImageData(image, imageData)  
  } else {
    return null;  
  }
}

exports.searchablesForUrl = searchablesForUrl;
exports.searchablesForBase64 = searchablesForBase64;