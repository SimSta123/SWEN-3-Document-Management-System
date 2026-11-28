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

function addProjectSummary(container, projectId) {
  const path = `/projects/${encodeURIComponent(projectId)}/summary`;
  const previousToggle = container.querySelector(".summary-toggle");
  const wasCollapsed = previousToggle?.dataset.projectId === String(projectId)
    && previousToggle.getAttribute("aria-expanded") === "false";
  const panel = document.createElement("section");
  panel.className = "project-summary";
  const header = document.createElement("div");
  header.className = "project-summary-header";
  const heading = document.createElement("h2");
  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "summary-toggle";
  toggle.dataset.projectId = projectId;
  toggle.setAttribute("aria-controls", "project-summary-body");
  toggle.textContent = "AI project summary";
  heading.append(toggle);

  const button = document.createElement("button");
  button.type = "button";
  button.className = "button quiet summary-action";
  button.disabled = true;
  const buttonLabel = document.createElement("span");
  button.append(icon("refresh"), buttonLabel);

  function setAction(label) {
    buttonLabel.textContent = label;
    button.setAttribute("aria-label", label + " AI summary");
  }
  setAction("Generate");

  const status = document.createElement("p");
  status.setAttribute("role", "status");
  showMessage(status, "Loading summary…");
  const output = document.createElement("p");
  output.className = "project-summary-text";
  output.hidden = true;

  const body = document.createElement("div");
  body.id = "project-summary-body";
  body.append(output, status);
  function setExpanded(expanded) {
    toggle.setAttribute("aria-expanded", String(expanded));
    body.hidden = !expanded;
  }
  setExpanded(!wasCollapsed);
  header.append(heading, button);
  panel.append(header, body);
  container.replaceChildren(panel);

  let loading = false;
  let active = false;
  let pollTimer;

  function showState(state) {
    active = state.status === "WAITING" || state.status === "QUEUED";
    output.textContent = state.summary ?? "";
    output.hidden = !state.summary;
    setAction(state.status === "FAILED" ? "Retry" : state.summary ? "Regenerate" : "Generate");
    button.disabled = active || state.totalDocuments === 0;

    if (state.status === "WAITING") {
      showMessage(status, `${state.readyDocuments} of ${state.totalDocuments} PDF summaries ready.`);
    } else if (state.status === "QUEUED") {
      showMessage(status, "Generating the project summary…");
    } else if (state.status === "FAILED") {
      const message = state.readyDocuments === 0
        ? "No file summaries are available. Add a readable PDF and retry."
        : "Could not generate the project summary. You can retry.";
      showMessage(status, message, "error");
    } else {
      showMessage(status, state.summary ? "" : state.totalDocuments === 0
        ? "Add a PDF to generate a summary."
        : "No AI summary yet.");
    }
  }

  async function load(method = "GET") {
    clearTimeout(pollTimer);
    if (loading || !panel.isConnected) return;

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
      if (active && panel.isConnected) pollTimer = setTimeout(load, 3000);
    }
  }

  toggle.addEventListener("click", () => setExpanded(body.hidden));
  button.addEventListener("click", () => {
    setExpanded(true);
    load("POST");
  });
  load();
}


function viewUrl(view) {
  return view === "all" ? "/" : "/?project=" + encodeURIComponent(view);
}

function icon(name) {
  const svgNamespace = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(svgNamespace, "svg");
  const use = document.createElementNS(svgNamespace, "use");
  svg.setAttribute("class", "icon " + name + "-icon");
  svg.setAttribute("aria-hidden", "true");
  use.setAttribute("href", "/icons.svg#" + name);
  svg.append(use);
  return svg;
}

function showDragPreview(transfer, glyph, title) {
  const card = document.createElement("div");
  card.className = "drag-preview";
  card.setAttribute("aria-hidden", "true");
  const label = document.createElement("span");
  label.className = "drag-preview-label";
  label.textContent = title;
  card.append(glyph.cloneNode(true), label);
  document.body.append(card);
  transfer.setDragImage(card, card.offsetWidth / 2, card.offsetHeight / 2);
  requestAnimationFrame(() => card.remove());
}

function startDashboard() {
  const libraryLinks = document.querySelector("#library-links");
  const projectLinks = document.querySelector("#project-links");
  const projectInformation = document.querySelector("#project-information");
  const libraryStatus = document.querySelector("#library-status");
  const rows = document.querySelector("#document-rows");
  const refresh = document.querySelector("#refresh");
  const search = document.querySelector("#search-term");
  const deleteProjectButton = document.querySelector("#delete-project");
  const uploadDialog = document.querySelector("#upload-dialog");
  const projectDialog = document.querySelector("#project-dialog");
  const projectIconButton = document.querySelector("#edit-project-icon");
  const projectIconDialog = document.querySelector("#project-icon-dialog");
  const projectIconOptions = document.querySelector("#project-icon-options");
  const projectIconStatus = document.querySelector("#project-icon-status");
  const moveDialog = document.querySelector("#move-dialog");
  const uploadForm = document.querySelector("#upload-form");
  const pdfInput = document.querySelector("#pdf-file");
  const projectForm = document.querySelector("#project-form");
  const uploadButton = document.querySelector("#upload-button");
  const createProjectButton = document.querySelector("#create-project");
  const uploadStatus = document.querySelector("#upload-status");
  const projectStatus = document.querySelector("#project-status");
  const moveStatus = document.querySelector("#move-status");

  let view = new URLSearchParams(window.location.search).get("project") || "all";
  let term = "";
  let projects = [];
  let loadVersion = 0;
  let draggedDocument = null;
  let moving = false;
  const unassignedFolder = { id: null, name: "Unassigned" };

  function selectView(next) {
    view = next;
    term = "";
    search.value = "";
    window.history.replaceState(null, "", viewUrl(view));
  }

  function removeDropHighlights() {
    document.querySelectorAll(".folder-link.drop-target").forEach(link => link.classList.remove("drop-target"));
  }

  function projectEmojiKey(id) {
    return "paperless.projectEmoji." + id;
  }

  function readProjectEmoji(id) {
    try {
      return localStorage.getItem(projectEmojiKey(id)) ?? "";
    } catch {
      return "";
    }
  }

  function projectIcon(id) {
    return projectGlyph(id == null ? "" : readProjectEmoji(id));
  }

  function projectGlyph(emoji) {
    if (!emoji) return icon("folder");
    const glyph = document.createElement("span");
    glyph.className = "project-emoji";
    glyph.setAttribute("aria-hidden", "true");
    glyph.textContent = emoji;
    return glyph;
  }

  function folderLink(name, target, count, folderIcon) {
    const link = document.createElement("a");
    link.href = viewUrl(target);
    link.className = "folder-link";
    if (String(target) === view) link.setAttribute("aria-current", "page");

    const label = document.createElement("span");
    label.className = "folder-name";
    label.textContent = name;
    const total = document.createElement("span");
    total.className = "folder-count";
    total.textContent = count;
    link.append(folderIcon, label, total);
    link.addEventListener("dragstart", event => {
      showDragPreview(event.dataTransfer, link.firstElementChild, name);
    });
    return link;
  }

  function renderFolders(allRecords) {
    const unassigned = allRecords.filter(record => record.projectId == null).length;
    const unassignedLink = folderLink(unassignedFolder.name, "unassigned", unassigned, icon("folder"));
    enableDrop(unassignedLink, unassignedFolder);
    libraryLinks.replaceChildren(
      folderLink("All documents", "all", allRecords.length, icon("library")),
      unassignedLink,
    );
    projectLinks.replaceChildren();
    for (const project of projects) {
      const count = allRecords.filter(record => record.projectId === project.id).length;
      const link = folderLink(project.name, project.id, count, projectIcon(project.id));
      enableDrop(link, project);
      projectLinks.append(link);
    }
  }

  function enableDrop(link, folder) {
    link.addEventListener("dragover", event => {
      if (!draggedDocument || moving || draggedDocument.projectId === folder.id) return;
      event.preventDefault();
      event.dataTransfer.dropEffect = "move";
      removeDropHighlights();
      link.classList.add("drop-target");
    });
    link.addEventListener("dragleave", event => {
      if (!link.contains(event.relatedTarget)) link.classList.remove("drop-target");
    });
    link.addEventListener("drop", event => {
      event.preventDefault();
      const record = draggedDocument;
      draggedDocument = null;
      removeDropHighlights();
      if (record) moveDocument(record, folder);
    });
  }

  function folderName(record) {
    return projects.find(project => project.id === record.projectId)?.name ?? unassignedFolder.name;
  }

  function renderFiles(records) {
    rows.replaceChildren();
    for (const record of records) {
      const row = document.createElement("li");
      row.className = "file-row";

      const file = document.createElement("a");
      const params = new URLSearchParams({ id: record.id });
      if (view !== "all") params.set("project", view);
      file.href = "/document.html?" + params;
      file.className = "file-link";
      file.draggable = true;
      file.addEventListener("dragstart", event => {
        draggedDocument = record;
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", String(record.id));
        showDragPreview(event.dataTransfer, file.firstElementChild, record.title);
        row.classList.add("is-dragging");
      });
      file.addEventListener("dragend", () => {
        draggedDocument = null;
        row.classList.remove("is-dragging");
        removeDropHighlights();
      });

      const name = document.createElement("span");
      name.className = "file-name";
      name.textContent = record.title;
      file.append(icon("pdf"), name);

      const project = document.createElement("span");
      project.className = "file-project";
      project.textContent = folderName(record);
      const move = document.createElement("button");
      move.type = "button";
      move.className = "button quiet icon-button file-move";
      move.title = "Move document";
      move.setAttribute("aria-label", "Move " + record.title);
      move.append(icon("move"));
      move.disabled = record.projectId == null && projects.length === 0;
      move.addEventListener("click", () => openMoveDialog(record));

      row.append(file, project, move);
      rows.append(row);
    }
  }

  function renderLibrary(allRecords, records) {
    const selected = projects.find(project => String(project.id) === view);
    if (view !== "all" && view !== "unassigned" && !selected) selectView("all");
    renderFolders(allRecords);

    const visible = records.filter(record => {
      if (selected) return record.projectId === selected.id;
      if (view === "unassigned") return record.projectId == null;
      return true;
    });
    document.querySelector("#library-title").textContent =
      selected?.name ?? (view === "unassigned" ? "Unassigned" : "All documents");
    projectIconButton.hidden = !selected;
    if (selected) projectIconButton.replaceChildren(projectIcon(selected.id));
    const description = selected?.description ?? "";
    document.querySelector("#project-description-text").textContent = description;
    document.querySelector("#project-description-view").hidden = !description;
    if (selected) addProjectSummary(projectInformation, selected.id);
    else projectInformation.replaceChildren();

    const count = document.querySelector("#library-count");
    const countParent = document.querySelector(selected ? "#document-list-heading" : "#library-heading");
    if (count.parentElement !== countParent) countParent.append(count);
    const countLabel = term ? "search result" : "document";
    count.textContent = visible.length + " " + countLabel + (visible.length === 1 ? "" : "s");
    deleteProjectButton.hidden = !selected;

    renderFiles(visible);
    document.querySelector("#document-list").hidden = !visible.length;
    document.querySelector("#empty-view").hidden = Boolean(visible.length);
    document.querySelector("#empty-upload").hidden = Boolean(term || selected);
    document.querySelector("#empty-text").textContent = term ? "No documents match this search."
      : selected ? "This project is empty. Move a document here from the library."
      : "No documents here yet.";
  }

  async function loadLibrary() {
    const version = ++loadVersion;
    refresh.disabled = true;
    try {
      const allDocuments = api("/documents");
      const [allRecords, fetchedProjects, records] = await Promise.all([
        allDocuments,
        api("/projects"),
        term ? api("/documents?search=" + encodeURIComponent(term)) : allDocuments,
      ]);
      if (version !== loadVersion) return;
      projects = fetchedProjects;
      renderLibrary(allRecords, records);
      return true;
    } catch (error) {
      if (version === loadVersion) {
        showMessage(libraryStatus, requestError(error, "Could not load the library. Select Refresh to try again."), "error");
      }
      return false;
    } finally {
      if (version === loadVersion) refresh.disabled = false;
    }
  }

  function openMoveDialog(record) {
    const options = document.querySelector("#move-options");
    document.querySelector("#move-document-title").textContent = record.title;
    document.querySelector("#move-source").textContent = folderName(record);
    options.replaceChildren();
    showMessage(moveStatus, "");
    for (const folder of [unassignedFolder, ...projects]) {
      if (folder.id === record.projectId) continue;
      const button = document.createElement("button");
      button.type = "button";
      button.className = "folder-link";
      const label = document.createElement("span");
      label.className = "folder-name";
      label.textContent = folder.name;
      button.append(projectIcon(folder.id), label);
      button.addEventListener("click", () => moveDocument(record, folder));
      options.append(button);
    }
    moveDialog.showModal();
  }

  async function moveDocument(record, folder) {
    if (moving || record.projectId === folder.id) return;
    moving = true;
    const fromDialog = moveDialog.open;
    const status = fromDialog ? moveStatus : libraryStatus;
    moveDialog.toggleAttribute("data-busy", true);
    showMessage(status, "");
    try {
      if (folder.id == null) {
        await api("/documents/" + record.id + "/project", { method: "DELETE" });
      } else {
        await api("/projects/" + folder.id + "/documents/" + record.id, { method: "PUT" });
      }
      await loadLibrary();
      if (fromDialog) {
        moveDialog.close();
        document.querySelector("#library-title").focus({ preventScroll: true });
      }
    } catch (error) {
      const message = folder.id == null && (error.status === 404 || error.status === 405)
        ? "Could not move to Unassigned. Refresh the library and check that the backend is up to date."
        : "Could not move the document. Try again.";
      showMessage(status, requestError(error, message), "error");
    } finally {
      moveDialog.removeAttribute("data-busy");
      moving = false;
    }
  }

  for (const dialog of document.querySelectorAll("dialog")) {
    dialog.querySelector("[data-close]").addEventListener("click", () => {
      if (!dialog.hasAttribute("data-busy")) dialog.close();
    });
    dialog.addEventListener("cancel", event => {
      if (dialog.hasAttribute("data-busy")) event.preventDefault();
    });
  }

  function openUpload() {
    showMessage(uploadStatus, "");
    uploadDialog.showModal();
  }

  function updateFilePicker() {
    const file = pdfInput.files[0];
    document.querySelector("#pdf-name").textContent = file?.name ?? "PDF up to 10 MB";
    document.querySelector("#choose-pdf-label").textContent = file ? "Change PDF" : "Choose PDF";
    uploadButton.disabled = !file;
  }

  pdfInput.addEventListener("change", updateFilePicker);
  uploadDialog.addEventListener("close", () => {
    uploadForm.reset();
    updateFilePicker();
  });
  document.querySelector("#open-upload").addEventListener("click", openUpload);
  document.querySelector("#empty-upload").addEventListener("click", openUpload);
  document.querySelector("#open-project").addEventListener("click", () => {
    showMessage(projectStatus, "");
    projectDialog.showModal();
  });
  function saveProjectIcon(emoji) {
    try {
      if (emoji) localStorage.setItem(projectEmojiKey(view), emoji);
      else localStorage.removeItem(projectEmojiKey(view));
    } catch {
      showMessage(projectIconStatus, "Could not save the project icon in this browser.", "error");
      return;
    }
    projectIconButton.replaceChildren(projectIcon(view));
    projectLinks.querySelector('[aria-current="page"]')?.firstElementChild.replaceWith(projectIcon(view));
    projectIconDialog.close();
  }

  const projectIconChoices = [
    ["", "Default folder"],
    ["📚", "Books"], ["🎓", "Graduation cap"], ["📝", "Notes"], ["📅", "Calendar"],
    ["💻", "Laptop"], ["💼", "Briefcase"], ["🏠", "Home"], ["⭐", "Star"],
    ["🎨", "Art palette"], ["🧪", "Test tube"], ["🔬", "Microscope"], ["🔧", "Wrench"],
    ["🧩", "Puzzle piece"], ["⚙️", "Gear"], ["💡", "Light bulb"], ["🎯", "Target"],
    ["📊", "Bar chart"], ["💰", "Money bag"], ["🧾", "Receipt"], ["📦", "Package"],
    ["🚀", "Rocket"], ["🗺️", "Map"], ["✈️", "Airplane"], ["🚗", "Car"],
    ["🌍", "Globe"], ["🌿", "Herb"], ["🌙", "Moon"], ["☀️", "Sun"],
    ["🏔️", "Mountain"], ["📷", "Camera"], ["🎵", "Music note"],
  ];
  for (const [emoji, label] of projectIconChoices) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "button quiet";
    button.value = emoji;
    button.title = label;
    button.setAttribute("aria-label", label);
    button.append(projectGlyph(emoji));
    button.addEventListener("click", () => saveProjectIcon(emoji));
    projectIconOptions.append(button);
  }
  projectIconButton.addEventListener("click", () => {
    const emoji = readProjectEmoji(view);
    for (const button of projectIconOptions.children) {
      button.setAttribute("aria-pressed", String(button.value === emoji));
    }
    showMessage(projectIconStatus, "");
    projectIconDialog.showModal();
    const selected = projectIconOptions.querySelector('[aria-pressed="true"]');
    (selected ?? projectIconOptions.firstElementChild).focus();
  });

  uploadForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (uploadButton.disabled) return;
    const file = pdfInput.files[0];
    if (!file) return;

    const data = new FormData();
    data.append("file", file);
    uploadButton.disabled = true;
    pdfInput.disabled = true;
    uploadDialog.toggleAttribute("data-busy", true);
    showMessage(uploadStatus, "Uploading PDF.");
    try {
      await api("/documents", { method: "POST", body: data });
      uploadDialog.close();
      selectView("unassigned");
      if (await loadLibrary()) showMessage(libraryStatus, "PDF uploaded to Unassigned.", "success");
    } catch (error) {
      const message = error.status === 400 || error.status === 413
        ? "Select a valid PDF up to 10 MB."
        : "Could not confirm the upload. Refresh the library before retrying.";
      showMessage(uploadStatus, requestError(error, message), "error");
    } finally {
      uploadDialog.removeAttribute("data-busy");
      pdfInput.disabled = false;
      updateFilePicker();
    }
  });

  projectForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (createProjectButton.disabled) return;
    const name = document.querySelector("#project-name").value.trim();
    const description = document.querySelector("#project-description").value.trim();
    if (!name) {
      showMessage(projectStatus, "Enter a project name.", "error");
      document.querySelector("#project-name").focus();
      return;
    }
    createProjectButton.disabled = true;
    projectDialog.toggleAttribute("data-busy", true);
    showMessage(projectStatus, "Creating project.");
    try {
      const project = await api("/projects", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, description }),
      });
      projectForm.reset();
      projectDialog.close();
      selectView(String(project.id));
      if (await loadLibrary()) showMessage(libraryStatus, "Project created.", "success");
    } catch (error) {
      showMessage(projectStatus, requestError(error, "Could not confirm project creation. Try again."), "error");
    } finally {
      projectDialog.removeAttribute("data-busy");
      createProjectButton.disabled = false;
    }
  });

  deleteProjectButton.addEventListener("click", async () => {
    const project = projects.find(item => String(item.id) === view);
    if (!project || !window.confirm("Delete " + project.name + "? Its documents will become unassigned.")) return;
    deleteProjectButton.disabled = true;
    try {
      await api("/projects/" + project.id, { method: "DELETE" });
      try { localStorage.removeItem(projectEmojiKey(project.id)); } catch { /* Browser storage is optional. */ }
      selectView("all");
      if (await loadLibrary()) showMessage(libraryStatus, "Project deleted. Its documents are now unassigned.", "success");
    } catch (error) {
      showMessage(libraryStatus, requestError(error, "Could not confirm project deletion. Refresh before retrying."), "error");
    } finally {
      deleteProjectButton.disabled = false;
    }
  });

  document.querySelector("#search-form").addEventListener("submit", event => {
    event.preventDefault();
    showMessage(libraryStatus, "");
    term = search.value.trim();
    loadLibrary();
  });
  refresh.addEventListener("click", () => {
    showMessage(libraryStatus, "");
    loadLibrary();
  });
  loadLibrary().then(loaded => {
    if (loaded) showMessage(libraryStatus, "");
  });
}

function startDetail() {
  const refresh = document.querySelector("#refresh");
  const status = document.querySelector("#detail-status");
  const details = document.querySelector("#document-details");
  const openPdf = document.querySelector("#open-pdf");
  const heading = document.querySelector("#document-heading");
  const deleteButton = document.querySelector("#delete-document");
  const backLabel = document.querySelector("#back-label");
  const projectLink = document.querySelector("#detail-project");
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");
  const backView = params.get("project") || "all";
  const libraryUrl = viewUrl(backView);
  document.querySelector("#back-to-library").href = libraryUrl;
  if (backView !== "all") backLabel.textContent = backView === "unassigned" ? "Unassigned" : "Back to project";

  async function loadDocument() {
    if (!id || !/^[1-9]\d*$/.test(id)) {
      showMessage(status, "No document selected. Go back to the library and choose a document.", "error");
      refresh.hidden = true;
      return;
    }
    refresh.disabled = true;
    deleteButton.disabled = true;
    details.setAttribute("aria-busy", "true");
    showMessage(status, details.hidden ? "Loading document…" : "");
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
      document.querySelector("#detail-id").textContent = record.id;
      projectLink.textContent = projectName;
      projectLink.href = viewUrl(record.projectId ?? "unassigned");
      if (String(record.projectId) === backView) backLabel.textContent = projectName;
      const summary = document.querySelector("#detail-summary");
      summary.textContent = record.summary || "No summary available.";
      summary.classList.toggle("muted", !record.summary);
      openPdf.href = "/api/documents/" + encodeURIComponent(id) + "/pdf";
      openPdf.hidden = !record.hasPdf;
      details.hidden = false;
      deleteButton.hidden = false;
      showMessage(status, "");
    } catch (error) {
      openPdf.hidden = true;
      details.hidden = true;
      deleteButton.hidden = true;
      heading.textContent = error.status === 404 ? "Document not found" : "Document";
      showMessage(status, error.status === 404
        ? "This document does not exist. Go back to your library to choose another document."
        : requestError(error, "Could not load this document. Please select Refresh to try again."), "error");
    } finally {
      details.removeAttribute("aria-busy");
      refresh.disabled = false;
      deleteButton.disabled = false;
    }
  }
  deleteButton.addEventListener("click", async () => {
    if (deleteButton.disabled) return;
    if (!window.confirm(`Delete document "${heading.textContent}"? This cannot be undone.`)) return;

    deleteButton.disabled = true;
    refresh.disabled = true;
    showMessage(status, "");
    try {
      await api(`/documents/${encodeURIComponent(id)}`, { method: "DELETE" });
      window.location.assign(libraryUrl);
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
