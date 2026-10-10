// Fetch API: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
// Use textContent for saved titles and project names, so they are displayed as text.
async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, { ...options, signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    const error = new Error("Request failed");
    error.status = response.status;
    throw error;
  }
  return response.json();
}

function showMessage(element, text, type = "") {
  element.textContent = text;
  element.className = `message ${type}`;
  element.hidden = !text;
}

function requestError(error, fallback) {
  return error.status === 401 || error.status === 403
    ? "You do not have access. Please sign in and try again."
    : fallback;
}

function addGroup(container, name, records) {
  const group = document.createElement("details");
  group.className = "project-group";
  group.open = true;
  const summary = document.createElement("summary");
  const label = document.createElement("span");
  label.className = "group-name";
  label.textContent = name;
  const count = document.createElement("span");
  count.className = "group-count";
  count.textContent = records.length;
  summary.append(label, count);
  group.append(summary);
  if (!records.length) {
    const empty = document.createElement("p");
    empty.className = "empty-group";
    empty.textContent = "No documents in this project yet.";
    group.append(empty);
  } else {
    const list = document.createElement("ul");
    list.className = "document-list";
    for (const record of records) {
      const item = document.createElement("li");
      const link = document.createElement("a");
      link.className = "document-link";
      link.href = `/document.html?id=${encodeURIComponent(record.id)}`;
      const icon = document.createElement("span");
      icon.className = "document-icon";
      icon.textContent = "≡";
      icon.setAttribute("aria-hidden", "true");
      const name = document.createElement("span");
      name.className = "document-name";
      name.textContent = record.title;
      const id = document.createElement("span");
      id.className = "document-id";
      id.textContent = `Document #${record.id}`;
      name.append(id);
      const arrow = document.createElement("span");
      arrow.className = "document-arrow";
      arrow.textContent = "↗";
      arrow.setAttribute("aria-hidden", "true");
      link.append(icon, name, arrow);
      item.append(link);
      list.append(item);
    }
    group.append(list);
  }
  container.append(group);
}

function startDashboard() {
  const refresh = document.querySelector("#refresh");
  const groups = document.querySelector("#document-groups");
  const status = document.querySelector("#library-status");
  const count = document.querySelector("#library-count");
  const form = document.querySelector("#document-form");
  const title = document.querySelector("#document-title");
  const save = document.querySelector("#save");
  const formError = document.querySelector("#form-error");
  const formStatus = document.querySelector("#form-status");
  let loadVersion = 0;

  async function loadLibrary() {
    const version = ++loadVersion;
    refresh.disabled = true;
    groups.replaceChildren();
    count.textContent = "Loading documents…";
    showMessage(status, "Loading your library…");
    try {
      const [records, projects] = await Promise.all([api("/documents"), api("/projects")]);
      if (version !== loadVersion) return;
      count.textContent = `${records.length} document${records.length === 1 ? "" : "s"} · ${projects.length} project${projects.length === 1 ? "" : "s"}`;
      showMessage(status, records.length ? "" : "Your library is empty. Add your first document to get started.");
      for (const project of projects) {
        addGroup(groups, project.name, records.filter(record => record.projectId === project.id));
      }
      const unassigned = records.filter(record => record.projectId == null);
      if (unassigned.length) addGroup(groups, "Unassigned", unassigned);
    } catch (error) {
      if (version !== loadVersion) return;
      count.textContent = "Library unavailable";
      showMessage(status, requestError(error, "Could not load your documents. Check that the server is running, then select Refresh."), "error");
    } finally {
      if (version === loadVersion) refresh.disabled = false;
    }
  }

  title.addEventListener("input", () => {
    title.removeAttribute("aria-invalid");
    showMessage(formError, "");
    showMessage(formStatus, "");
  });
  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (save.disabled) return;
    showMessage(formError, "");
    showMessage(formStatus, "");
    const value = title.value.trim();
    if (!value || value.length > 255) {
      title.setAttribute("aria-invalid", "true");
      showMessage(formError, !value ? "Enter a document title." : "The title must be 255 characters or fewer.", "error");
      title.focus();
      return;
    }
    title.removeAttribute("aria-invalid");
    save.disabled = true;
    title.disabled = true;
    showMessage(formStatus, "Saving document…");
    try {
      await api("/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: value }),
      });
      form.reset();
      showMessage(formStatus, "Document saved. You can open it from your library.", "success");
      await loadLibrary();
    } catch (error) {
      showMessage(formStatus, "");
      showMessage(formError, requestError(error, error.status === 400
        ? "The server rejected this title. Use between 1 and 255 characters."
        : "Could not confirm the save. Refresh the library before trying again."), "error");
    } finally {
      save.disabled = false;
      title.disabled = false;
      title.focus();
    }
  });
  refresh.addEventListener("click", loadLibrary);
  loadLibrary();
}

function startDetail() {
  const refresh = document.querySelector("#refresh");
  const status = document.querySelector("#detail-status");
  const details = document.querySelector("#document-details");
  const heading = document.querySelector("#document-heading");
  const id = new URLSearchParams(window.location.search).get("id");

  async function loadDocument() {
    details.hidden = true;
    if (!id || !/^[1-9]\d*$/.test(id)) {
      showMessage(status, "No valid document was selected. Go back to the dashboard and choose a document.", "error");
      refresh.hidden = true;
      return;
    }
    refresh.disabled = true;
    showMessage(status, "Loading document…");
    try {
      const record = await api(`/documents/${encodeURIComponent(id)}`);
      let projectName = "Unassigned";
      if (record.projectId != null) {
        projectName = `Project #${record.projectId}`;
        // Show the document even if the optional project-name lookup fails.
        try {
          const projects = await api("/projects");
          projectName = projects.find(project => project.id === record.projectId)?.name ?? projectName;
        } catch { /* Keep the saved project ID as a fallback. */ }
      }
      heading.textContent = record.title;
      document.title = `${record.title} · Paperless`;
      document.querySelector("#detail-title").textContent = record.title;
      document.querySelector("#detail-id").textContent = record.id;
      document.querySelector("#detail-project").textContent = projectName;
      details.hidden = false;
      showMessage(status, "");
    } catch (error) {
      heading.textContent = error.status === 404 ? "Document not found" : "Document";
      showMessage(status, error.status === 404
        ? "This document does not exist. Go back to your library to choose another document."
        : requestError(error, "Could not load this document. Please select Refresh to try again."), "error");
    } finally {
      refresh.disabled = false;
    }
  }
  refresh.addEventListener("click", loadDocument);
  loadDocument();
}

if (document.body.dataset.page === "dashboard") startDashboard();
if (document.body.dataset.page === "detail") startDetail();
