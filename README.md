Alt-Text.org API
================

Status: Open beta

This is the primary API for the Alt-Text.org backend. It exposes 
a fairly small set of public endpoints. All endpoints consume and 
return JSON objects if they consume or return anything. Any non-2xx
response will have a JSON body with an `error` field describing 
what went wrong.


Save
----
Saves alt text for a provided image to the library.

`POST /library/v1/save`

Authorization: Bearer token

Example request body: 
```json
{
  "image": {
      "url": "https://example.com/image.jpg"
  },
  "language": "en",
  "alt_text": "The description of the image to save",
  "id_scope": "twitter",
  "author_id": "453899876"
}
```

Instead of `url` the client could have also included 

```json
{
  "image": {
    "base64": "data:image/png;base64,iVBORw0KGgoAA..."
  },
  ...
}
```

Exactly one of `url` and `base64` must be provided. 
All other fields are required.

- `image.url`: HTTP(S) URL of the image to be saved
- `image.base64`: Data URL of the image to be saved
- `language`: ISO 639-1 2-letter language code for the alt text
- `alt_text`: The alt text to be saved
- `author_id`: An identifier for the user who wrote the alt text
- `id_scope`: The context within which the `author_id` uniquely
identifies a user

Response Code:
- `204 No Content`: If the request was successful
- `400 Bad Request`: If there was any issue with the request
- `401 Unauthorized`: If the correct bearer token was not sent
- `500 Internal Server Error`: If there was any other issue


Fetch
-----
Searches the library for alt text for a provided image.

`POST /library/v1/fetch`

Authorization: None required

Example request body:
```json
{
  "image": {
      "url": "https://example.com/image.jpg"
  },
  "language": "en"
}
```

Instead of `url` the client could have also included

```json
{
  "image": {
    "base64": "data:image/png;base64,iVBORw0KGgoAA..."
  },
  ...
}
```

Exactly one of `url` and `base64` must be provided.
- `image.url`: HTTP(S) URL of the image to be searched
- `image.base64`: Data URL of the image to be searched
- `language`: ISO 639-1 2-letter language code to search for

Response Code:
- `200 OK`: If the request was successful and a result body is included
- `400 Bad Request`: If there was any issue with the request
- `404 Not Found`: If no alt text could be found
- `500 Internal Server Error`: If there was any other issue

Response Body:
```json
{
  "ocr": "Any text extracted from the image",
  "exact": [
    {
      "alt_text": "Alt text for this entry",
      "image_url": "URL for the image for which this alt text was saved",
      "language": "ISO 639-1 2-letter language code of this alt text",
      "author_uuid": "A v4 UUID identifying the author",
      "sha256": "The sha256 hash of the image for which this alt text was saved"
    },
    ...
  ],
  "fuzzy": [
    {
      "alt_text": "Same as above",
      "image_url": "Same as above",
      "language": "Same as above",
      "author_uuid": "Same as above",
      "sha256": "Same as above",
      "score": 0.98
    },
    ...
  ]
}
```

All fields with the exception of `image_url` are guaranteed to appear.
If `exact` or `fuzzy` are empty, an empty array will be present. In 
the `fuzzy` array, the `score` field is a float in (0,1) indicating
approximately how similar the searched image is to the one for which
the given alt text was saved.

Report
-----
Files a report of an abusive entry in the library

`POST /library/v1/report`

Authorization: None required

Example request body:

```json
{
  "reason": "Reason for reporting the entry, at most 1000 characters",
  "author_uuid": "v4 UUID identifying the author of the alt text being reported",
  "sha256": "Hash of the image for which the alt text was written",
  "language": "ISO 639-1 2-letter language code of alt text being reported"
}
```

All fields except `reason` are required.

Response Code:
- `204 No Content`: If the request was successful
- `400 Bad Request`: If there was any issue with the request
- `404 Not Found`: If no entry could be found for the provided author, hash, and language
- `500 Internal Server Error`: If there was any other issue

Twitter Sign-Up
---------------
Retrieve a URL to be hit as part of an OAuth 2.0 PKCE flow

`GET /library/v1/twitter-sign-up`

Response Code:
- `200 OK`: If the request was successful and a result body is included

Response Body:

```json
{
  "auth_url": "https://url.to.send/user/to"
}
```