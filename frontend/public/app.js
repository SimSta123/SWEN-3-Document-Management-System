const status = document.querySelector("#status");
const refresh = document.querySelector("#refresh");

const dbstatus = document.querySelector("#dbstatus");
const dbrefresh = document.querySelector("#dbrefresh");

async function checkAvailability() {
  refresh.disabled = true;
  status.textContent = "Checking availability…";
  console.log("www");
  try {
    const response = await fetch("/api/health", { signal: AbortSignal.timeout(10000) });
    const health = await response.json();
    if (!response.ok || health.status !== "UP") throw new Error("Service unavailable");
    status.textContent = "Ready — the service is available.";
  } catch {
    status.textContent = "The service is unavailable. Please try again shortly.";
  } finally {
    refresh.disabled = false;
  }
}

async function checkAvailabilityDb() {
  dbrefresh.disabled = true;
  status.textContent = "Checking availability…";
  try {
    const works_response = await fetch("/api/one", { signal: AbortSignal.timeout(10000) });
    const works = await works_response.text();
    console.log(works)
    if (!works_response.ok || works !== "Works?") throw new Error("db unavailable");
    status.textContent = "Ready — the db is available.";
  } catch {
    status.textContent = "The db is unavailable. Please try again shortly.";
  } finally {
    dbrefresh.disabled = false;
  }
}


dbrefresh.addEventListener("click", checkAvailabilityDb);
checkAvailabilityDb();

refresh.addEventListener("click", checkAvailability);
checkAvailability();
