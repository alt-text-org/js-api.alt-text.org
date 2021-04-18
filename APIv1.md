Alt-Text.org API
=================

The Alt-Text.org API is a REST API for searching for and publishing image descriptions, keyed by image.


Concepts
--------

Alt-Text.org does not store any images, only text. That text is uniquely identified by a three part key:
`(username-hash, image-hash, language)` this means that a given user may save only one description per image and
language. In addition, searches may pass an *Image Histogram*, a feature vector extracted from the image used to search
for images that are very similar, but not exactly the same.


Protocol Definition
-------------------

All request and response objects are defined using Protocol Buffers. If you are implementing a client we *highly*
suggest using those definitions. They can be found [on GitHub](https://github.com/alt-text-org/alt-text-protos).


Image Histograms
----------------

The API uses image intensity histograms as one method of fuzzily-matching similar images. To calculate a histogram for
submission:

1. For each pixel in the array, calculate the intensity, scale the result by `0.13` (see example for calculation), then
   increment the appropriate bucket.
1. For each bucket, calculate the fraction of pixels in the image that are in that bucket as a 32bit float, they will
   always be in the range `[0.0, 1.0]`
1. Write the floats to a byte buffer in little-endian order
1. URL-safe base64 encode the result.

The Javascript client uses the following code:

```js
function getIntensityHist(image) {
    const maxIntensity = 255 * 3
    const buckets = 100

    // Floor to two places, don't want to round so we don't accidentally round up.
    const scale = Math.floor(buckets / maxIntensity * 100) / 100

    let counts = new Array(100).fill(0)
    let data = image.data

    for (let i = 0; i < data.length; i += 4) {
        let intensity = (data[i] + data[i + 1] + data[i + 2]) * (data[i + 3] / 255.0)
        let bucket = Math.floor(intensity * scale)
        counts[bucket]++
    }

    const pixels = data.length / 4
    let floats = new Float32Array(100)
    for (let i = 0; i < 100; i++) {
        floats[i] = Math.fround(counts[i] / pixels)
    }

    return encodeURLSafe(new Uint8Array(floats.buffer))
}
```

Authentication
--------------

Searching for a description for an image and marking a description as used do not require authentication, but all other
operations require authentication.

The actual auth systems are a work in progress, but will use Google Cloud's IdP provider to allow sign-in via Google,
Twitter, Facebook, or Email/Password.


Rate Limits
-----------

To contain costs and prevent abuse, access to all endpoints is tightly rate limited per IP or account, depending on the
endpoint.


Endpoints
---------

### GET /api/alt-text/public/v1/img/hash/{image_hash}/{language}

Searches the library for descriptions for the specified image and image signature if one is specified.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image being searched
- `language`: The ISO-639-2 language code for the search language

__Optional query parameters__:

- `matches`: The number of matches to return, maximum is 20
- `min_confidence`: A floating point number between 0.0 and 1.0 indicating the minimum match confidence
- `ocr_url`: The URL-encoded publicly accessible address of the image being analyzed. If present, the backend will
  attempt to OCR the image at the provided URL and return the text alongside the other results

__Optional Headers__:

- `X-Alt-Text-Org-Intensity-Hist: <URL-safe base64 encoded histogram as descibed above>`

__Rate Limit__

12 calls/minute, bucketed by IP

__Response body__:

On success, an HTTP 200 OK status code will be returned with a response body JSON object wrapping an array of image
description objects:

```json
{
  "extracted_text": "",
  "texts": [
    {
      "text": "A small brown dog looks contentedly out over a lake",
      "language": "en",
      "confidence": 1.0,
      "times_used": 241,
      "image_hash": "c2850ea37e0976bbb2ecc89f3a1895da",
      "user_hash": "9687abe53659b6a955e6dbdd16ac7631"
    }
  ]
}
```

If an `ocr_url` was specified, `extracted_text` may contain the text extracted from the image. No guarantees are made
about the accuracy or length of the returned text. If `ocr_url` is not specified or if no text is found, the field will
be missing or empty.

All fields of individual texts are guaranteed to be present.

- `text`: A UTF-8 description of the associated image. Guaranteed to be at most 1000 UTF-8 codepoints
- `language`: The publisher-specified ISO-639-2 language code for the text
- `confidence`: A rough estimate of how likely the image described is the same one searched for, always between 0.0 and
  1.0, where 0.0 indicates no confidence and 1.0 indicates that the image hashes were equal
- `times_used`: How many times this description has been selected for use
- `image_hash`: The SHA256 hash of the bitmap of the described image
- `user_hash`: The SHA256 hash of the author's username

If no matching descriptions are found and no text could be extracted, an `HTTP 404 Not Found` will be returned. If the
rate limit is exceeded an `HTTP 429 Too Many Requests` will be returned.

---

### POST /api/alt-text/public/v1/img/hash/{image_hash}/{language}

Publishes an image description.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image being searched
- `language`: The ISO-639-2 language code for the search language

__Required Headers__

- `X-Alt-Text-Org-Timestamp`: A millisecond-precision UTC-zoned ISO-8601 timestamp with the time this action occurred

__Optional Headers__:

- `X-Alt-Text-Org-Intensity-Hist: <URL-safe base64 encoded histogram as descibed above>`

__Rate Limit__

6 calls/minute, bucketed by username

__Request Body__

The body must be a single JSON object with the following format.

```json
{
  "text": "A small brown dog looks contentedly out over a lake",
  "url": "https://example.com/mypic.jpg",
  "public": true
}
```

- `text`: The image description to be saved. Must be at most 1000 UTF-8 codepoints long. Required.
- `url`: The URL of the image being described. Optional.
- `public`: True if other users will be allowed to see this description. Optional, defaults to true.

On success, an `HTTP 202 Accepted` status will be returned. Note that description publication is asynchronous and may
not be immediately visible. If the rate limit is exceeded an `HTTP 429 Too Many Requests` will be returned.

If another user has published the same description for the same image, the API may respond with an `HTTP 409 Conflict`

---

### DELETE /api/alt-text/public/v1/img/hash/{image_hash}/{language}

Deletes the description authored by the calling user for the given image hash and language.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image being searched
- `language`: The ISO-639-2 language code for the search language

__Required Headers__

- `X-Alt-Text-Org-Timestamp`: A millisecond-precision UTC-zoned ISO-8601 timestamp with the time this action occurred

__Rate Limit__

6 calls/minute

This endpoint always returns an `HTTP 202 Accepted` even if the description did not exist. Deletion is asynchronous and
may not be immediately visible. If the rate limit is exceeded an `HTTP 429 Too Many Requests` will be returned.

---

### GET /api/alt-text/public/v1/user

Gets all descriptions authored by the requesting user.

__Rate Limit__

6 calls/minute

Gets all texts published by the requesting user.

__Response Body__

On success, an HTTP 200 OK status code will be returned with a response body JSON object wrapping an array of user image
description objects:

```json
{
  "texts": [
    {
      "text": "A small brown dog looks contentedly out over a lake",
      "url": "https://example.com/mypic.jpg",
      "language": "en",
      "image_hash": "c2850ea37e0976bbb2ecc89f3a1895da",
      "public": true
    }
  ]
}
```

All fields except `url` are guaranteed to be present.

- `text`: A UTF-8 description of the associated image. Guaranteed to be at most 1000 UTF-8 codepoints
- `url`: The URL of the described image, may be absent or an empty string
- `language`: The publisher-specified ISO-639-2 language code for the text
- `image_hash`: The SHA256 hash of the bitmap of the described image
- `public`: True if other users are allowed to see this description, false otherwise

If the user has no published descriptions, an `HTTP 404 Not Found` will be returned. If the rate limit is exceeded
an `HTTP 429 Too Many Requests` will be returned.

---

### GET /api/alt-text/public/v1/favorites

Gets all descriptions favorited by the requesting user

__Rate Limit__

6 calls/minute

Gets all texts favorited by the requesting user.

__Response Body__

On success, an HTTP 200 OK status code will be returned with a response body JSON object wrapping and array of favorite
image description objects:

```json
{
  "texts": [
    {
      "text": "A small brown dog looks contentedly out over a lake",
      "language": "en",
      "image_hash": "c2850ea37e0976bbb2ecc89f3a1895da",
      "user_hash": "9687abe53659b6a955e6dbdd16ac7631"
    }
  ]
}
```

All fields are guaranteed to be present.

- `text`: A UTF-8 description of the associated image. Guaranteed to be at most 1000 UTF-8 codepoints
- `language`: The publisher-specified ISO-639-2 language code for the text
- `image_hash`: The SHA256 hash of the bitmap of the described image
- `user_hash`: The SHA256 hash of the author's username

If the user has no favorited descriptions, an `HTTP 404 Not Found` will be returned. If the rate limit is exceeded
an `HTTP 429 Too Many Requests` will be returned.

---

### POST /api/alt-text/public/v1/favorite/{image_hash}/{user_hash}/{language}

"Favorites" an image description. This stores the description for the user for quick access, as well as saving it for
the user in case the original is modified or deleted. A user my have at most one description favorited for a given image
and language.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image
- `user_hash`: The SHA256 hash of the author's username
- `language`: The ISO-639-2 language code for the description language

__Required Headers__

- `X-Alt-Text-Org-Timestamp`: A millisecond-precision UTC-zoned ISO-8601 timestamp with the time this action occurred

__Rate Limit__

6 calls/minute, bucketed by username

Always returns an `HTTP 202 Accepted` unless a rate limit is hit. If the rate limit is exceeded
an `HTTP 429 Too Many Requests` will be returned.

---

### DELETE /api/alt-text/public/v1/favorite/{image_hash}{user_hash}/{language}

"Unfavorites" an image description.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image
- `user_hash`: The SHA256 hash of the author's username
- `language`: The ISO-639-2 language code for the description language

__Required Headers__

- `X-Alt-Text-Org-Timestamp`: A millisecond-precision UTC-zoned ISO-8601 timestamp with the time this action occurred

__Rate Limit__

6 calls/minute, bucketed by username

Always returns an `HTTP 202 Accepted` unless a rate limit is hit. If the rate limit is exceeded
an `HTTP 429 Too Many Requests` will be returned.
---

### POST /api/alt-text/public/v1/report/{image_hash}/{user_hash}/{language}

Reports an issue with a given description.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image
- `user_hash`: The hex-encoded SHA256 hash of the author's username
- `language`: The ISO-639-2 language code for the search language

__Rate Limit__

2 calls/minute, bucketed by username

__Request Body__

The body must be a single JSON object with the following format.

```json
{
  "reason": "Contains racial slurs not relevant to the image being described"
}
```

- `reason`: The reason for reporting the specified description

Always returns an `HTTP 202 Accepted`. If the rate limit is exceeded an `HTTP 429 Too Many Requests` will be returned.

---

### POST /api/alt-text/public/v1/mark/{image_hash}/{user_hash}/{language}

Marks a given description as having been used.

__Required path parameters__:

- `image_hash`:  The hex-encoded SHA256 hash of the bitmap representation of the image
- `user_hash`: The hex-encoded SHA256 hash of the author's username
- `language`: The ISO-639-2 language code for the search language

__Rate Limit__

1 call/second, bucketed by IP
