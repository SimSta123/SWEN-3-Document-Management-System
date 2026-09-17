const status = document.querySelector("#status");
const refresh = document.querySelector("#refresh");

async function checkAvailability() {
  refresh.disabled = true;
  status.textContent = "Checking availability…";
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

refresh.addEventListener("click", checkAvailability);
checkAvailability();
