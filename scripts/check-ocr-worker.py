"""Send test messages and check that the worker receives them. Use a test stack."""

import base64
import json
import os
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


url = os.environ.get("RABBITMQ_MANAGEMENT_URL", "http://127.0.0.1:15672")
queue = os.environ.get("OCR_QUEUE", "document.uploaded")
credentials = "paperless:" + os.environ["RABBITMQ_PASSWORD"]
authorization = "Basic " + base64.b64encode(credentials.encode()).decode()
queue_path = "/api/queues/%2F/" + urllib.parse.quote(queue, safe="")


def request(path, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url + path, data=data, headers={
        "Authorization": authorization,
        "Content-Type": "application/json",
    })
    with urllib.request.urlopen(req, timeout=5) as response:
        return json.load(response)


def wait_for(check, description):
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(1)
    raise RuntimeError("Timed out waiting for " + description)


def worker_ready():
    try:
        return request(queue_path).get("consumers", 0) > 0
    except (urllib.error.URLError, TimeoutError):
        return False


wait_for(worker_ready, "the worker to connect")
marker = "worker-check-" + uuid.uuid4().hex
payloads = [
    (marker + "-text", "text/plain"),
    (json.dumps({"documentId": 42, "filename": marker + ".pdf"}), "application/json"),
]
for payload, content_type in payloads:
    result = request("/api/exchanges/%2F/amq.default/publish", {
        "properties": {"content_type": content_type, "delivery_mode": 2},
        "routing_key": queue,
        "payload": payload,
        "payload_encoding": "string",
    })
    if not result["routed"]:
        raise RuntimeError("The message did not reach the queue")


def messages_received():
    logs = subprocess.run(
        ["docker", "compose", "logs", "--no-color", "ocr-worker"],
        check=True, capture_output=True, text=True, timeout=10,
    ).stdout
    return all("Received upload for OCR: " + payload in logs for payload, _ in payloads)


wait_for(messages_received, "both messages in the worker logs")


def messages_acknowledged():
    info = request(queue_path)
    return info.get("messages_ready") == 0 and info.get("messages_unacknowledged") == 0


wait_for(messages_acknowledged, "the worker to finish the messages")
failed_ids = {marker + "-blank", marker + "-utf8"}
for message_id, payload, encoding in [
    (marker + "-blank", " ", "string"),
    (marker + "-utf8", base64.b64encode(bytes([0xc3, 0x28])).decode(), "base64"),
]:
    result = request("/api/exchanges/%2F/amq.default/publish", {
        "properties": {"message_id": message_id, "delivery_mode": 2},
        "routing_key": queue, "payload": payload, "payload_encoding": encoding,
    })
    if not result["routed"]:
        raise RuntimeError("The bad test message did not reach the queue")

failed_path = "/api/queues/%2F/" + urllib.parse.quote(queue + ".failed", safe="")
received_failed_ids = set()


def bad_messages_saved():
    messages = request(failed_path + "/get", {
        "count": 100, "ackmode": "ack_requeue_false", "encoding": "auto",
    })
    for message in messages:
        message_id = message["properties"].get("message_id")
        if message_id in failed_ids:
            deaths = message["properties"]["headers"]["x-death"]
            if not any(death["reason"] == "rejected" for death in deaths):
                raise RuntimeError("The bad message was not rejected by the worker")
            received_failed_ids.add(message_id)
    return received_failed_ids == failed_ids


wait_for(bad_messages_saved, "both bad messages in the failed queue")
wait_for(messages_acknowledged, "the upload queue to clear")
print("Worker check passed. Good messages were logged and bad messages went to the failed queue.")
