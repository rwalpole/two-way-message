
# two-way-message

## Customer create message API

| Path | Supported Methods | Description
|---|---|---
|`/message/customer/:queueId/submit`|POST|submit a customer two way message (as a customer)

Example JSON body in POST request:
```
{ 
    "contactDetails":{
        "email": "someEmail@test.com"
    },
    "subject":"QUESTION",
    "content":"Some base64-encoded HTML",
}
```

## Return Status Codes

- **201** (OK) with response body below if successfully added to message :

```
{
    "id": "57bac7e90b0000490000b7cf"
}
```

- **400** (Bad Request) if the body is not as per the above definition of two-way-message service
- **502 -> 400** (Bad Gateway) Embedded Bad Request 400 if the tax identifier is not supported by message service
- **502 -> 409** (Bad Gateway) Embedded Conflict 409 if the message hash is a duplicate of an existing message by message service
- **502 -> 4XX or 5XX** (Bad Gateway) For any other message service errors

Error response body:
```
{
    "error": "400"
    "message": "Message error text"
}
```

In the case where the error is being bubbled up from the message service, for instance in the case of a 409 response, the body would look like the following:
```
{
  "error": 409,
  "message": "POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'"
}
```

## Advisor reply message API

| Path | Supported Methods | Description
|---|---|---
|`/message/advsior/:replyTo/reply`|POST|Reply to a customer two way message (as an advisor)

Example JSON body in POST request:
```
{
    "content":"Some base64-encoded HTML",
}
```

## Return Status Codes

- **201** (OK) with response body below if successfully added to message :

```
{
    "id": "57bac7e90b0000490000b7cf"
}
```

- **400** (Bad Request) if the body is not as per the above definition of two-way-message service
- **502 -> 400** (Bad Gateway) Embedded Bad Request 400 if the tax identifier is not supported by message service
- **502 -> 404** (Bad Gateway) Embedded Not Found 404 if the message id is not found by message service
- **502 -> 409** (Bad Gateway) Embedded Conflict 409 if the message hash is a duplicate of an existing message by message service
- **502 -> 4XX or 5XX** (Bad Gateway) For any other message service errors

Error response body:
```
{
    "error": "400"
    "message": "Message error text"
}
```

In the case where the error is being bubbled up from the message service, for instance in the case of a 409 response, the body would look like the following:
```
{
  "error": 409,
  "message": "POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'"
}
```

Or attempting to reply to a non-existing message:
```
{
  "error": 404,
  "message": "GET of 'http://localhost:8910/messages/5c1091ec670000450103e792/metadata' returned 404 (Not Found). Response body: ''"
}
```

## Customer reply message API

| Path | Supported Methods | Description
|---|---|---
|`/message/customer/:queueId/:replyTo/reply`|POST|Reply to an advisor's two way message (as a customer)

Example JSON body in POST request:
```
{
    "content":"Some base64-encoded HTML",
}
```

## Return Status Codes

- **201** (OK) with response body below if successfully added to message :

```
{
    "id": "57bac7e90b0000490000b7cf"
}
```

- **400** (Bad Request) if the body is not as per the above definition of two-way-message service
- **502 -> 400** (Bad Gateway) Embedded Bad Request 400 if the tax identifier is not supported by message service
- **502 -> 404** (Bad Gateway) Embedded Not Found 404 if the message id is not found by message service
- **502 -> 409** (Bad Gateway) Embedded Conflict 409 if the message hash is a duplicate of an existing message by message service
- **502 -> 4XX or 5XX** (Bad Gateway) For any other message service errors

Error response body:
```
{
    "error": "400"
    "message": "Message error text"
}
```

In the case where the error is being bubbled up from the message service, for instance in the case of a 409 response, the body would look like the following:
```
{
  "error": 409,
  "message": "POST of 'http://localhost:8910/messages' returned 409. Response body: '{\"reason\":\"Duplicated message content or external reference ID\"}'"
}
```

Or attempting to reply to a non-existing message:
```
{
  "error": 404,
  "message": "GET of 'http://localhost:8910/messages/5c1091ec670000450103e792/metadata' returned 404 (Not Found). Response body: ''"
}
```

### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
