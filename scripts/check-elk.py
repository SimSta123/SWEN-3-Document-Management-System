"""Check Kibana readiness and send a test JSON event through Logstash to Elasticsearch."""

import base64
import json
import os
import time
import urllib.error
import urllib.request
import uuid


elastic_url = os.environ.get("ELASTICSEARCH_URL", "http://localhost:9200")
kibana_url = os.environ.get("KIBANA_URL", "http://localhost:5601")
logstash_url = os.environ.get("LOGSTASH_URL", "http://localhost:18080")
password = os.environ.get("ELASTICSEARCH_PASSWORD") or os.environ["ELASTIC_PASSWORD"]
credentials = os.environ.get("ELASTICSEARCH_USERNAME", "elastic") + ":" + password
authorization = "Basic " + base64.b64encode(credentials.encode()).decode()


def read_json(url, body=None):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, headers={
        "Authorization": authorization, "Content-Type": "application/json",
    })
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.load(response)


status = read_json(kibana_url + "/api/status")
assert status["status"]["overall"]["level"] == "available", status["status"]["overall"]

marker = "elk-check-" + uuid.uuid4().hex
event = {"message": marker, "service": "swen3-container-check"}
request = urllib.request.Request(logstash_url, data=json.dumps(event).encode(), headers={
    "Content-Type": "application/json",
})
with urllib.request.urlopen(request, timeout=15) as response:
    assert 200 <= response.status < 300, response.status

deadline = time.monotonic() + 60
while time.monotonic() < deadline:
    try:
        result = read_json(elastic_url + "/paperless-logs-*/_search", {
            "query": {"match_phrase": {"message": marker}},
        })
        if any(hit["_source"].get("message") == marker for hit in result["hits"]["hits"]):
            print("ELK check passed: Kibana is available and the Logstash event reached Elasticsearch.")
            break
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
    time.sleep(1)
else:
    raise RuntimeError("Timed out waiting for the Logstash event in Elasticsearch")
