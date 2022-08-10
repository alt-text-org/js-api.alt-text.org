const vision = require("@google-cloud/vision");

const visionClient = new vision.ImageAnnotatorClient();

async function ocrUrl(url) {
    let [result] = await visionClient
        .textDetection(url)
        .catch(err => console.log(err));
    let texts = result.textAnnotations;
    if (texts) {
        return texts
            .filter(text => !!text.locale)
            .map(text => text.description)
            .join(" ")
    } else {
        return "";
    }
}

async function ocrBase64(base64Image) {
    const stripped = base64Image.split(',');
    if (!stripped || stripped.length !== 2) {
        console.log("Couldn't strip base64 image")
        return ""
    }


    let requests = [
        {
            image: {
                content: stripped[1],
            },
            features: [{type: "TEXT_DETECTION"}],
        },
    ];

    let result = await visionClient
        .batchAnnotateImages({requests})
        .catch((err) => console.log(err));

    if (
        result[0] &&
        result[0].responses &&
        result[0].responses[0] &&
        result[0].responses[0].fullTextAnnotation &&
        result[0].responses[0].fullTextAnnotation.text
    ) {
        return result[0].responses[0].fullTextAnnotation.text;
    } else {
        console.log("No text found. Full response: " + JSON.stringify(result));
        return "";
    }
}

exports.ocrUrl = ocrUrl
exports.ocrBase64 = ocrBase64