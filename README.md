Tag Server
==========

A toy REST server intended to help the author get more familiar with:
 - Swagger
 - Guice
 - Gradle
 - PB&K
 - JDBI
 - Kotlin
 
The server tracks arbitrary tags applied to users, with the expectation that updates may arrive out-of-order.
 
 
Endpoints
---------

Provides a single endpoint `/api/tags` which accepts tag update requests for a user and returns their current set of tags.

Request format:
```json
{
  "user": "bob",
  "timestamp": 1000,
  "add": ["tag1, tag2"],
  "remove": ["tag3"]
}
```

The `user` field is always required. The `add` and `remove` fields are always optional. If neither `add` nor `remove` is
specified then `timestamp` is optional and ignored.

Response format:
```json
{
  "tags": ["tag1", "tag2"]
}
```
where `tags` may be empty.


Running
-------

This service stores tag data in Postgres. An instance matching the provided config.yml can be easily spun up with:
 
 `docker run -e POSTGRES_PASSWORD=lolsecurity -p 5432:5432 postgres`

If running postgres in some other way, edits to config.yml may be required. Running the server itself is done via gradle:

`gradle run --args="server config.yml"`
