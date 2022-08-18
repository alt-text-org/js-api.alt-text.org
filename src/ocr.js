const vision = require("@google-cloud/vision");

const visionClient = new vision.ImageAnnotatorClient();

function ts() {
    return new Date().toISOString();
}

async function ocrUrl(url) {
    let [result] = await visionClient
        .textDetection(url)
        .catch(err => {
            console.log(err)
            return []
        });

    if (result && result.textAnnotations) {
        return result.textAnnotations
            .filter(text => !!text.locale)
            .map(text => text.description)
            .join(" ")
    } else {
        return "";
    }
}

function normalizeBase64(maybeDataUrl) {
    let normalized = maybeDataUrl
    if (maybeDataUrl.match(/^data:.*/)) {
        const split = maybeDataUrl.split(',')
        if (split.length === 2) {
            normalized = split[1]
        } else {
            console.log(`${ts()}: Got malformed data url, rejecting`)
            normalized = null;
        }
    }

    return normalized
}

async function ocrBase64(base64Image) {
    base64Image = normalizeBase64(base64Image)
    if (!base64Image) {
        return ""
    }

    let requests = [
        {
            image: {
                content: base64Image,
            },
            features: [{type: "TEXT_DETECTION"}],
        },
    ];

    let [result] = await visionClient
        .batchAnnotateImages({requests})
        .catch((err) => {
            console.log(err)
            return []
        });

    if (
        result &&
        result.responses &&
        result.responses[0] &&
        result.responses[0].fullTextAnnotation &&
        result.responses[0].fullTextAnnotation.text
    ) {
        return result.responses[0].fullTextAnnotation.text;
    } else {
        console.log("No text found. Full response: " + JSON.stringify(result));
        return "";
    }
}

exports.ocrUrl = ocrUrl
exports.ocrBase64 = ocrBase64