// Fetch API: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
// Use textContent for saved titles and project names, so they are displayed as text.
async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, { ...options, signal: AbortSignal.timeout(10000) });
  if (!response.ok) {
    const error = new Error("Request failed");
    error.status = response.status;
    throw error;
  }
  if (response.status === 204) return;
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

function addGroup(container, name, records, onDelete, projectId) {
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
  if (onDelete) {
    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "button delete-project";
    deleteButton.textContent = "Delete";
    deleteButton.setAttribute("aria-label", `Delete project ${name}`);
    deleteButton.addEventListener("click", async event => {
      event.preventDefault();
      event.stopPropagation();
      if (deleteButton.disabled) return;
      deleteButton.disabled = true;
      deleteButton.textContent = "Deleting…";
      try {
        await onDelete();
      } finally {
        deleteButton.disabled = false;
        deleteButton.textContent = "Delete";
      }
    });
    summary.append(deleteButton);
  }
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
  if (projectId != null) addProjectSummary(group, projectId);
  container.append(group);
}

function addProjectSummary(group, projectId) {
  const path = `/projects/${encodeURIComponent(projectId)}/summary`;
  const panel = document.createElement("details");
  panel.className = "project-summary";
  const heading = document.createElement("summary");
  heading.textContent = "AI project summary";

  const content = document.createElement("div");
  content.className = "project-summary-content";
  const button = document.createElement("button");
  button.type = "button";
  button.className = "button secondary";
  button.textContent = "Generate AI summary";
  button.disabled = true;

  const status = document.createElement("p");
  status.setAttribute("role", "status");
  status.hidden = true;
  const output = document.createElement("p");
  output.className = "project-summary-text";
  output.hidden = true;

  content.append(button, status, output);
  panel.append(heading, content);
  group.append(panel);

  let loading = false;
  let active = false;
  let pollTimer;

  function isOpen() {
    return panel.isConnected && panel.open && group.open;
  }

  function showState(state) {
    active = state.status === "WAITING" || state.status === "QUEUED";
    output.textContent = state.summary ?? "";
    output.hidden = !state.summary;
    button.textContent = state.summary ? "Regenerate AI summary" : "Generate AI summary";
    button.disabled = active || state.totalDocuments === 0;

    if (state.status === "WAITING") {
      showMessage(status, `${state.readyDocuments} of ${state.totalDocuments} PDF summaries ready.`);
    } else if (state.status === "QUEUED") {
      showMessage(status, "Generating the project summary…");
    } else if (state.status === "FAILED") {
      showMessage(status, "Could not generate the project summary.", "error");
    } else if (state.status === "COMPLETED") {
      showMessage(status, "Project summary ready.", "success");
    } else if (state.totalDocuments === 0) {
      showMessage(status, "Add a PDF to this project to generate a summary.");
    } else {
      showMessage(status, "");
    }
  }

  async function load(method = "GET") {
    clearTimeout(pollTimer);
    if (loading || !isOpen()) return;

    loading = true;
    button.disabled = true;
    if (method === "POST") showMessage(status, "Starting the project summary…");

    try {
      showState(await api(path, { method }));
    } catch (error) {
      active = active && method === "GET" && (!error.status || error.status >= 500);
      button.disabled = active;
      const fallback = active
        ? "Could not refresh the project summary. Retrying…"
        : "Could not load or start the project summary. Try again.";
      showMessage(status, requestError(error, fallback), "error");
    } finally {
      loading = false;
      if (active && isOpen()) pollTimer = setTimeout(load, 3000);
    }
  }

  panel.addEventListener("toggle", () => load());
  group.addEventListener("toggle", () => load());
  button.addEventListener("click", () => load("POST"));
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

  const uploadForm = document.querySelector("#upload-form");
  const pdfInput = document.querySelector("#pdf-file");
  const uploadButton = document.querySelector("#upload-button");
  const uploadStatus = document.querySelector("#upload-status");
  const projectForm = document.querySelector("#project-form");
  const projectName = document.querySelector("#project-name");
  const projectDescription = document.querySelector("#project-description");
  const createProject = document.querySelector("#create-project");
  const projectStatus = document.querySelector("#project-status");
  const assignmentForm = document.querySelector("#assignment-form");
  const assignmentDocument = document.querySelector("#assignment-document");
  const assignmentProject = document.querySelector("#assignment-project");
  const assignDocument = document.querySelector("#assign-document");
  const assignmentStatus = document.querySelector("#assignment-status");
  const searchForm = document.querySelector("#search-form");
  const searchTerm = document.querySelector("#search-term");
  let activeSearch = "";
  let assigning = false;

  function fillSelect(select, records, placeholder, label) {
    const previous = select.value;
    select.replaceChildren(new Option(placeholder, ""));
    for (const record of records) select.add(new Option(label(record), String(record.id)));
    if (records.some(record => String(record.id) === previous)) select.value = previous;
    select.disabled = !records.length || assigning;
  }

  let loadVersion = 0;

  async function loadLibrary() {
    const version = ++loadVersion;
    const term = activeSearch;
    refresh.disabled = true;
    assignmentDocument.disabled = true;
    assignmentProject.disabled = true;
    assignDocument.disabled = true;
    groups.replaceChildren();
    count.textContent = "Loading documents…";
    showMessage(status, "Loading your library…");
    try {
      const allDocuments = api("/documents");
      const [allRecords, projects, records] = await Promise.all([
        allDocuments,
        api("/projects"),
        term ? api(`/documents?search=${encodeURIComponent(term)}`) : allDocuments,
      ]);
      if (version !== loadVersion) return;
      fillSelect(assignmentDocument, allRecords, allRecords.length ? "Choose a document" : "No documents available", record => `${record.title} (#${record.id})`);
      fillSelect(assignmentProject, projects, projects.length ? "Choose a project" : "Create a project first", project => `${project.name} (#${project.id})`);
      assignDocument.disabled = assigning || !allRecords.length || !projects.length;
      count.textContent = term
        ? `${records.length} matching document${records.length === 1 ? "" : "s"}`
        : `${records.length} document${records.length === 1 ? "" : "s"} · ${projects.length} project${projects.length === 1 ? "" : "s"}`;
      showMessage(status, records.length ? "" : term
        ? "No matching documents."
        : "Your library is empty. Add your first document to get started.");
      for (const project of projects) {
        const projectRecords = records.filter(record => record.projectId === project.id);
        if (!term || projectRecords.length) {
          addGroup(groups, project.name, projectRecords, () => deleteProject(project), project.id);
        }
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

  async function deleteProject(project) {
    showMessage(projectStatus, `Deleting project "${project.name}"…`);
    try {
      await api(`/projects/${encodeURIComponent(project.id)}`, { method: "DELETE" });
      showMessage(projectStatus, `Project "${project.name}" deleted. Its documents are now unassigned.`, "success");
      await loadLibrary();
    } catch (error) {
      showMessage(projectStatus, requestError(error, "Could not confirm project deletion. Refresh the library before trying again."), "error");
    }
  }

  projectForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (createProject.disabled) return;
    const name = projectName.value.trim();
    if (!name) {
      showMessage(projectStatus, "Enter a project name.", "error");
      projectName.focus();
      return;
    }
    createProject.disabled = true;
    projectName.disabled = true;
    projectDescription.disabled = true;
    showMessage(projectStatus, "Creating project…");
    try {
      await api("/projects", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description: projectDescription.value.trim() }),
      });
      projectForm.reset();
      showMessage(projectStatus, "Project created. You can now assign documents to it.", "success");
      await loadLibrary();
    } catch (error) {
      showMessage(projectStatus, requestError(error, "Could not confirm project creation. Refresh the library before trying again."), "error");
    } finally {
      createProject.disabled = false;
      projectName.disabled = false;
      projectDescription.disabled = false;
    }
  });

  assignmentForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (assignDocument.disabled || !assignmentDocument.value || !assignmentProject.value) return;
    assigning = true;
    assignDocument.disabled = true;
    assignmentDocument.disabled = true;
    assignmentProject.disabled = true;
    showMessage(assignmentStatus, "Assigning document…");
    try {
      await api(`/projects/${encodeURIComponent(assignmentProject.value)}/documents/${encodeURIComponent(assignmentDocument.value)}`, { method: "PUT" });
      showMessage(assignmentStatus, "Document assigned to the project.", "success");
    } catch (error) {
      showMessage(assignmentStatus, requestError(error, "Could not confirm the assignment. Check the refreshed library before trying again."), "error");
    } finally {
      assigning = false;
      await loadLibrary();
    }
  });

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
  searchForm.addEventListener("submit", event => {
    event.preventDefault();
    activeSearch = searchTerm.value.trim();
    loadLibrary();
  });
  refresh.addEventListener("click", loadLibrary);
  loadLibrary();

  //pdf upload
  uploadForm.addEventListener("submit", async event => {
    event.preventDefault();

    const file = pdfInput.files[0];
    if (!file || uploadButton.disabled) return;

    const data = new FormData();
    data.append("file", file);

    uploadButton.disabled = true;
    uploadStatus.textContent = "PDF wird hochgeladen…";

    try {
      await api("/documents", {
       method: "POST",
        body: data,
      });

      uploadForm.reset();
      uploadStatus.textContent = "PDF erfolgreich hochgeladen.";
      await loadLibrary();
    } catch (error) {
      uploadStatus.textContent = error.status === 400 ? "Die Datei wurde abgelehnt. Bitte eine gültige PDF auswählen." : "Upload fehlgeschlagen.";
    } finally {
      uploadButton.disabled = false;
    }
  });
}

function startDetail() {
  const refresh = document.querySelector("#refresh");
  const status = document.querySelector("#detail-status");
  const details = document.querySelector("#document-details");
  const heading = document.querySelector("#document-heading");
  const deleteButton = document.querySelector("#delete-document");
  const id = new URLSearchParams(window.location.search).get("id");

  async function loadDocument() {
    details.hidden = true;
    deleteButton.hidden = true;
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
      document.querySelector("#detail-summary").textContent = record.summary ?? "No summary available.";
      details.hidden = false;
      deleteButton.hidden = false;
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
  deleteButton.addEventListener("click", async () => {
    if (deleteButton.disabled) return;
    if (!window.confirm(`Delete document "${heading.textContent}"? This cannot be undone.`)) return;

    deleteButton.disabled = true;
    refresh.disabled = true;
    showMessage(status, "Deleting document…");
    try {
      await api(`/documents/${encodeURIComponent(id)}`, { method: "DELETE" });
      window.location.assign("/");
    } catch (error) {
      showMessage(status, requestError(error, "Could not confirm document deletion. Refresh the document before trying again."), "error");
      deleteButton.disabled = false;
      refresh.disabled = false;
    }
  });
  refresh.addEventListener("click", loadDocument);
  loadDocument();
}

if (document.body.dataset.page === "dashboard") startDashboard();
if (document.body.dataset.page === "detail") startDetail();
