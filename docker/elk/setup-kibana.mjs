// Kibana uses its built-in service user to connect to Elasticsearch.
const { ELASTIC_PASSWORD, KIBANA_PASSWORD } = process.env;
if (!ELASTIC_PASSWORD || !KIBANA_PASSWORD) {
  throw new Error("Elasticsearch and Kibana passwords must be configured");
}

const response = await fetch("http://elasticsearch:9200/_security/user/kibana_system/_password", {
  method: "POST",
  headers: {
    Authorization: `Basic ${Buffer.from(`elastic:${ELASTIC_PASSWORD}`).toString("base64")}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify({ password: KIBANA_PASSWORD }),
  signal: AbortSignal.timeout(15000),
});
if (!response.ok) {
  throw new Error(`Kibana service-user setup failed with HTTP ${response.status}`);
}
console.log("Kibana service user configured.");
