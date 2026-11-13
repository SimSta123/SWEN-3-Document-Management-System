"""Check PDF upload, OCR and indexing. Use a disposable stack: failed test messages are consumed."""

import base64
import json
import os
import time
import urllib.parse
import urllib.error
import urllib.request
import uuid


api_url = os.environ.get("API_URL", "http://localhost:8081")
management_url = os.environ.get("RABBITMQ_MANAGEMENT_URL", "http://127.0.0.1:15672")
credentials = "paperless:" + os.environ["RABBITMQ_PASSWORD"]
authorization = "Basic " + base64.b64encode(credentials.encode()).decode()
result_queue = os.environ.get("OCR_RESULT_QUEUE", "document.ocr.completed")
elastic_url = os.environ.get("ELASTICSEARCH_URL", "http://localhost:9200")
index = urllib.parse.quote(os.environ.get("ELASTICSEARCH_INDEX", "documents"), safe="")
elastic_password = os.environ.get("ELASTICSEARCH_PASSWORD") or os.environ["ELASTIC_PASSWORD"]
elastic_credentials = os.environ.get("ELASTICSEARCH_USERNAME", "elastic") + ":" + elastic_password
elastic_authorization = "Basic " + base64.b64encode(elastic_credentials.encode()).decode()


def rabbit_request(path, body):
    request = urllib.request.Request(management_url + path, data=json.dumps(body).encode(), headers={
        "Authorization": authorization, "Content-Type": "application/json",
    })
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response)


def elastic_request(path, body=None):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(elastic_url + path, data=data, headers={
        "Authorization": elastic_authorization, "Content-Type": "application/json",
    })
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)


def wait_for(check, description):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        result = check()
        if result:
            return result
        time.sleep(1)
    raise RuntimeError("Timed out waiting for " + description)


def publish_result(payload, message_id=None):
    properties = {"content_type": "application/json", "delivery_mode": 2}
    if message_id:
        properties["message_id"] = message_id
    result = rabbit_request("/api/exchanges/%2F/amq.default/publish", {
        "properties": properties, "routing_key": result_queue,
        "payload": json.dumps(payload), "payload_encoding": "string",
    })
    assert result["routed"], "Result message did not reach the queue"


def hello_pdf():
    stream = b"BT /F1 36 Tf 60 700 Td (Hello World) Tj ET"
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream",
    ]
    pdf = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for number, obj in enumerate(objects, 1):
        offsets.append(len(pdf))
        pdf.extend(f"{number} 0 obj\n".encode() + obj + b"\nendobj\n")
    xref = len(pdf)
    pdf.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        pdf.extend(f"{offset:010} 00000 n \n".encode())
    pdf.extend(f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode())
    return bytes(pdf)


boundary = "ocr-check-" + uuid.uuid4().hex
body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="HelloWorld.pdf"\r\n'
        'Content-Type: application/pdf\r\n\r\n').encode() + hello_pdf() + f"\r\n--{boundary}--\r\n".encode()
request = urllib.request.Request(api_url + "/documents", data=body, headers={
    "Content-Type": "multipart/form-data; boundary=" + boundary,
})
with urllib.request.urlopen(request, timeout=30) as response:
    assert response.status == 201, response.status
    document = json.load(response)

document_path = "/" + index + "/_doc/" + str(document["id"])


def indexed_document():
    try:
        return elastic_request(document_path)
    except urllib.error.HTTPError as error:
        # A newly created index can briefly exist before its first shard is ready.
        if error.code in (404, 503):
            return None
        raise


indexed = wait_for(indexed_document, "the uploaded PDF in Elasticsearch")
assert indexed["_source"]["documentId"] == document["id"], indexed
assert "Hello World" in indexed["_source"]["text"], indexed


def hello_search_finds_document():
    result = elastic_request("/" + index + "/_search", {"query": {"match": {"text": "Hello"}}})
    return any(hit["_id"] == str(document["id"]) for hit in result["hits"]["hits"])


wait_for(hello_search_finds_document, "Hello to find the uploaded document")
mapping = elastic_request("/" + index + "/_mapping")[index]["mappings"]["properties"]
assert mapping["documentId"]["type"] == "long", mapping
assert mapping["text"]["type"] == "text", mapping

# Redelivery must update the same document instead of creating another search hit.
publish_result(indexed["_source"])
wait_for(lambda: indexed_document()["_version"] > indexed["_version"], "redelivery to update the same document")
result = elastic_request("/" + index + "/_search", {"query": {"term": {"documentId": document["id"]}}})
assert result["hits"]["total"]["value"] == 1, result

# Bad results must remain available in the failed queue rather than being indexed.
marker = "index-check-" + uuid.uuid4().hex
publish_result({"documentId": 0, "text": marker}, marker)
failed_path = "/api/queues/%2F/" + urllib.parse.quote(result_queue + ".failed", safe="")


def rejected_result_saved():
    messages = rabbit_request(failed_path + "/get", {
        "count": 100, "ackmode": "ack_requeue_false", "encoding": "auto",
    })
    for message in messages:
        if message["properties"].get("message_id") == marker:
            assert any(death["reason"] == "rejected" for death in message["properties"]["headers"]["x-death"])
            return True
    return False


wait_for(rejected_result_saved, "the rejected result in the failed queue")
print(f"OCR/indexing flow passed: document {document['id']} is searchable for Hello, redelivery kept one entry, and an invalid result reached the failed queue.")
