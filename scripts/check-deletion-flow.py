"""Check deletion against a disposable Compose stack, including queued and running OCR.

Stops/restarts only this Compose project's worker. Deletes only documents created here.
"""
import base64
import datetime
import hashlib
import hmac
import json
import os
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


api = os.environ.get("API_URL", "http://localhost:8081").rstrip("/")
rabbit = os.environ.get("RABBITMQ_MANAGEMENT_URL", "http://127.0.0.1:15672").rstrip("/")
elastic = os.environ.get("ELASTICSEARCH_URL", "http://localhost:9200").rstrip("/")
rustfs = os.environ.get("RUSTFS_URL", "http://127.0.0.1:9000").rstrip("/")
index = urllib.parse.quote(os.environ.get("ELASTICSEARCH_INDEX", "documents"), safe="")
upload_queue = os.environ.get("OCR_QUEUE", "document.uploaded")
result_queue = os.environ.get("OCR_RESULT_QUEUE", "document.ocr.completed")
bucket = os.environ.get("STORAGE_BUCKET", "documents")
elastic_password = os.environ.get("ELASTICSEARCH_PASSWORD") or os.environ["ELASTIC_PASSWORD"]


def auth(user, password):
    return "Basic " + base64.b64encode((user + ":" + password).encode()).decode()


def request(url, method="GET", body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=35) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        return error.code, None


def rabbit_request(path, body=None):
    status, result = request(rabbit + path, "POST" if body is not None else "GET", body,
                             {"Authorization": auth("paperless", os.environ["RABBITMQ_PASSWORD"]),
                              "Content-Type": "application/json"})
    assert status == 200, ("RabbitMQ request failed", status)
    return result


def compose(*args):
    return subprocess.run(["docker", "compose", *args], check=True, capture_output=True,
                          text=True, timeout=90).stdout.strip()


def wait_for(check, description):
    end = time.monotonic() + 90
    while time.monotonic() < end:
        if check():
            return
        time.sleep(.25)
    raise AssertionError("Timed out waiting for " + description)


def pdf():
    stream = b"BT /F1 36 Tf 60 700 Td (Deletion Test Hello World) Tj ET"
    objects = [b"<< /Type /Catalog /Pages 2 0 R >>", b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
               b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
               b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
               b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream"]
    content = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for number, obj in enumerate(objects, 1):
        offsets.append(len(content))
        content.extend(f"{number} 0 obj\n".encode() + obj + b"\nendobj\n")
    xref = len(content)
    content.extend(f"xref\n0 {len(offsets)}\n0000000000 65535 f \n".encode())
    for offset in offsets[1:]:
        content.extend(f"{offset:010} 00000 n \n".encode())
    content.extend(f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n".encode())
    return bytes(content)


def upload():
    boundary = "delete-check-" + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="deletion-test.pdf"\r\n'
            'Content-Type: application/pdf\r\n\r\n').encode() + pdf() + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(api + "/documents", data=body,
                                 headers={"Content-Type": "multipart/form-data; boundary=" + boundary})
    with urllib.request.urlopen(req, timeout=35) as response:
        assert response.status == 201
        doc = json.load(response)
    document_id = int(doc["id"])
    assert document_id > 0
    key = compose("exec", "-T", "database", "psql", "-U", "paperless", "-d", "paperless", "-At", "-c",
                  f"select object_key from documents where id = {document_id}")
    assert key
    assert s3_status(key) == 200, "Uploaded PDF missing from RustFS"
    return document_id, key


def s3_status(key):
    # AWS Signature V4 HEAD request, using only the standard library.
    now = datetime.datetime.now(datetime.timezone.utc)
    date, stamp = now.strftime("%Y%m%d"), now.strftime("%Y%m%dT%H%M%SZ")
    host = urllib.parse.urlsplit(rustfs).netloc
    uri = "/" + urllib.parse.quote(bucket, safe="") + "/" + urllib.parse.quote(key, safe="/~")
    digest = hashlib.sha256(b"").hexdigest()
    signed = "host;x-amz-content-sha256;x-amz-date"
    canonical = f"HEAD\n{uri}\n\nhost:{host}\nx-amz-content-sha256:{digest}\nx-amz-date:{stamp}\n\n{signed}\n{digest}"
    scope = date + "/us-east-1/s3/aws4_request"
    signing = ("AWS4" + os.environ["RUSTFS_SECRET_KEY"]).encode()
    for part in [date, "us-east-1", "s3", "aws4_request"]:
        signing = hmac.new(signing, part.encode(), hashlib.sha256).digest()
    to_sign = f"AWS4-HMAC-SHA256\n{stamp}\n{scope}\n" + hashlib.sha256(canonical.encode()).hexdigest()
    signature = hmac.new(signing, to_sign.encode(), hashlib.sha256).hexdigest()
    headers = {"x-amz-date": stamp, "x-amz-content-sha256": digest,
               "Authorization": f"AWS4-HMAC-SHA256 Credential={os.environ['RUSTFS_ACCESS_KEY']}/{scope}, SignedHeaders={signed}, Signature={signature}"}
    req = urllib.request.Request(rustfs + uri, method="HEAD", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code


def indexed(document_id):
    status, _ = request(elastic + "/" + index + "/_doc/" + str(document_id),
                        headers={"Authorization": auth("elastic", elastic_password)})
    assert status in (200, 404, 503), ("Elasticsearch request failed", status)
    return status


def assert_deleted(document_id, key):
    assert request(api + "/documents/" + str(document_id))[0] == 404, "Database record survived deletion"
    assert s3_status(key) == 404, "RustFS PDF survived deletion"
    assert indexed(document_id) == 404, "Deleted document was recreated in Elasticsearch"
    status, results = request(api + "/documents?search=Deletion")
    assert status == 200
    assert all(doc["id"] != document_id for doc in results), "Deleted document returned in website search"


def delete(document_id, key):
    assert request(api + "/documents/" + str(document_id), "DELETE")[0] == 204
    assert_deleted(document_id, key)


def publish(queue, payload, content_type="text/plain"):
    result = rabbit_request("/api/exchanges/%2F/amq.default/publish", {
        "properties": {"delivery_mode": 2, "content_type": content_type}, "routing_key": queue,
        "payload": payload, "payload_encoding": "string"})
    assert result["routed"], "Stale message did not reach its queue"


def queues_idle():
    for queue in [upload_queue, result_queue]:
        status = rabbit_request("/api/queues/%2F/" + urllib.parse.quote(queue, safe=""))
        if status.get("consumers", 0) == 0 or status.get("messages_ready", 0) or status.get("messages_unacknowledged", 0):
            return False
    return True


def failed_counts():
    return [rabbit_request("/api/queues/%2F/" + urllib.parse.quote(queue + ".failed", safe="")).get("messages", 0)
            for queue in [upload_queue, result_queue]]


def stale_jobs_processed(document_id, count):
    # Management queue counts are sampled. These per-document log markers prove that
    # both consumers actually handled the messages before we assert final absence.
    logs = compose("logs", "--no-color", "--tail", "200", "ocr-worker") + "\n"
    return all(logs.count(f"{message}: documentId={document_id}\n") >= count for message in [
        "Skipped OCR for deleted document", "Skipped indexing for deleted document"])


wait_for(queues_idle, "the worker to be ready")
initial_failures = failed_counts()

# An already indexed document must disappear from all three stores.
document_id, key = upload()
wait_for(lambda: indexed(document_id) == 200, "normal OCR/indexing")
delete(document_id, key)
print("Passed: normal deletion removes database record, RustFS PDF and Elasticsearch entry.")

# Messages queued before deletion AND delivered again afterwards must be harmless.
compose("stop", "ocr-worker")
try:
    document_id, key = upload()
    stale_result = json.dumps({"documentId": document_id, "text": "Deletion stale OCR text"})
    publish(result_queue, stale_result, "application/json")
    delete(document_id, key)
finally:
    compose("start", "ocr-worker")
wait_for(queues_idle, "queued stale upload/result acknowledgement")
wait_for(lambda: stale_jobs_processed(document_id, 1), "both queued stale jobs to be skipped")
assert_deleted(document_id, key)
publish(upload_queue, f"Document created: {document_id} with key: {key}")
publish(result_queue, stale_result, "application/json")
wait_for(queues_idle, "late redelivery acknowledgement")
wait_for(lambda: stale_jobs_processed(document_id, 2), "both redelivered jobs to be skipped")
assert_deleted(document_id, key)
print("Passed: queued uploads, queued results and later redeliveries cannot resurrect the document.")

# Hold the real Tesseract invocation, delete while it runs, then allow OCR to finish.
compose("stop", "ocr-worker")
fixture = Path(__file__).resolve().parent / "fixtures" / "blocked-tesseract.sh"
worker_name = "swen3-deletion-race-" + uuid.uuid4().hex[:10]
try:
    compose("run", "--detach", "--no-deps", "--name", worker_name,
            "--volume", str(fixture) + ":/race/blocked-tesseract.sh:ro",
            "--env", "TESSERACT_COMMAND=/tmp/blocked-tesseract", "--entrypoint", "/bin/sh", "ocr-worker",
            "-c", "tr -d '\\r' < /race/blocked-tesseract.sh > /tmp/blocked-tesseract; chmod +x /tmp/blocked-tesseract; exec java -jar /app/app.jar")
    wait_for(queues_idle, "the controlled OCR worker")
    document_id, key = upload()

    def recognition_started():
        return subprocess.run(["docker", "exec", worker_name, "test", "-f", "/tmp/deletion-ocr-started"],
                              capture_output=True, timeout=10).returncode == 0

    wait_for(recognition_started, "Tesseract to start on the downloaded PDF")
    delete(document_id, key)
    subprocess.run(["docker", "exec", worker_name, "touch", "/tmp/deletion-ocr-resume"], check=True, timeout=10)
    wait_for(queues_idle, "OCR to finish and discard its result")
    assert_deleted(document_id, key)
    logs = subprocess.run(["docker", "logs", worker_name], check=True, capture_output=True, text=True).stdout
    assert f"Discarded OCR for deleted document: documentId={document_id}" in logs
finally:
    subprocess.run(["docker", "rm", "--force", worker_name], capture_output=True, timeout=30)
    compose("start", "ocr-worker")
wait_for(queues_idle, "the normal worker to resume")
assert failed_counts() == initial_failures, "Stale jobs should be acknowledged, not sent to failed queues"
print("Passed: deletion while real OCR is running discards its output without recreating any data.")
