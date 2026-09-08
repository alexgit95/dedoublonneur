(() => {
  const POLL_INTERVAL_MS = 2000;
  const STEP_ORDER = ["folder", "analysis", "review", "export"];
  const state = {
    status: "IDLE",
    jobId: null,
    folderName: null,
    selectedStep: "folder",
    selectedReview: "blur",
    reviewInitialized: false,
    processingInitialized: false,
    processingController: null,
    pollTimer: null,
  };

  const stepper = [...document.querySelectorAll("[data-step]")];
  const panels = [...document.querySelectorAll("[data-panel]")];
  const workflowStatus = document.getElementById("workflow-status");
  const context = document.getElementById("workflow-context");
  const eventFolders = document.getElementById("event-folders");
  const startAnalysisButton = document.getElementById("start-analysis");
  const resumeAnalysisButton = document.getElementById("resume-analysis");
  const analysisProgressFill = document.getElementById("analysis-progress-fill");
  const analysisProgressLabel = document.getElementById("analysis-progress-label");
  const reviewTabs = [...document.querySelectorAll("[data-review-tab]")];
  const reviewPanels = [...document.querySelectorAll("[data-review-panel]")];
  const resetButtons = [...document.querySelectorAll("[data-workflow-reset]")];

  function accessibleSteps() {
    if (state.status === "IDLE") return ["folder"];
    if (state.status === "ANALYZING") return ["folder", "analysis"];
    if (state.status === "READY_FOR_REVIEW") return ["folder", "analysis", "review", "export"];
    return STEP_ORDER;
  }

  function setWorkflowMessage(message) {
    workflowStatus.textContent = message || "";
  }

  function updateContext() {
    context.hidden = !state.folderName;
    context.textContent = state.folderName ? `Dossier en cours : ${state.folderName}` : "";
    document.querySelectorAll("[data-current-folder]").forEach((element) => {
      element.textContent = state.folderName || "votre dossier";
    });
  }

  function updateUrl() {
    const url = new URL(window.location.href);
    if (state.jobId) {
      url.searchParams.set("jobId", state.jobId);
    } else {
      url.searchParams.delete("jobId");
    }
    window.history.replaceState({}, "", url);
  }

  function renderStepper() {
    const currentStep = DedoublonneurUI.workflowStepForStatus(state.status);
    const currentIndex = STEP_ORDER.indexOf(currentStep);
    const available = accessibleSteps();
    stepper.forEach((button) => {
      const step = button.dataset.step;
      const index = STEP_ORDER.indexOf(step);
      button.classList.toggle("is-active", step === currentStep);
      button.classList.toggle("is-complete", index < currentIndex);
      button.disabled = !available.includes(step);
      button.setAttribute("aria-current", step === currentStep ? "step" : "false");
    });
    resetButtons.forEach((button) => {
      button.hidden = !["ANALYZING", "READY_FOR_REVIEW"].includes(state.status);
    });
  }

  function showStep(step) {
    if (!accessibleSteps().includes(step)) {
      setWorkflowMessage("Cette etape sera disponible lorsque l'etape precedente sera terminee.");
      return;
    }
    state.selectedStep = step;
    panels.forEach((panel) => {
      panel.hidden = panel.dataset.panel !== step;
    });
    renderStepper();
  }

  function renderFolders(folders) {
    eventFolders.innerHTML = "";
    if (folders.length === 0) {
      eventFolders.textContent = "Aucun dossier evenement disponible.";
      startAnalysisButton.disabled = true;
      return;
    }
    folders.forEach((folder) => {
      const label = document.createElement("label");
      label.className = "folder-option";
      const input = document.createElement("input");
      input.type = "radio";
      input.name = "event-folder";
      input.value = folder.name;
      input.addEventListener("change", () => {
        startAnalysisButton.disabled = false;
      });
      const name = document.createElement("span");
      name.className = "folder-option__name";
      name.textContent = folder.name;
      label.append(input, name);
      if (folder.processed) {
        const badge = document.createElement("span");
        badge.className = "folder-option__badge";
        badge.textContent = "Deja exporte";
        badge.setAttribute("aria-label", "Dossier deja exporte");
        label.appendChild(badge);
      }
      eventFolders.appendChild(label);
    });
  }

  async function loadFolders() {
    const response = await fetch("/api/events");
    if (!response.ok) throw new Error("Impossible de charger les dossiers evenement.");
    renderFolders(await response.json());
  }

  async function startAnalysis() {
    const selected = document.querySelector("input[name=event-folder]:checked");
    if (!selected) return;
    startAnalysisButton.disabled = true;
    setWorkflowMessage("Demarrage de l'analyse...");
    const response = await fetch(`/api/events/${encodeURIComponent(selected.value)}/analysis`, { method: "POST" });
    if (!response.ok) {
      setWorkflowMessage("Impossible de demarrer l'analyse. Un autre workflow est peut-etre deja actif.");
      startAnalysisButton.disabled = false;
      return;
    }
    const job = await response.json();
    state.jobId = job.id;
    state.folderName = job.eventFolderName;
    updateUrl();
    await syncStatus();
  }

  async function loadAnalysisProgress() {
    if (!state.jobId) return;
    const response = await fetch(`/api/jobs/${state.jobId}`);
    if (!response.ok) return;
    const progress = await response.json();
    const percent = progress.totalCount === 0 ? 100 : Math.round((progress.analyzedCount / progress.totalCount) * 100);
    analysisProgressFill.style.width = `${percent}%`;
    analysisProgressLabel.textContent = `${progress.analyzedCount} / ${progress.totalCount} photos analysees (${percent} %)`;
    resumeAnalysisButton.hidden = progress.running || progress.status !== "ANALYZING";
  }

  function initializeReview() {
    if (state.reviewInitialized || !state.jobId) return;
    DedoublonneurPanels.initBlurReview(state.jobId, document.querySelector('[data-review-panel="blur"]'));
    DedoublonneurPanels.initDuplicateReview(state.jobId, document.querySelector('[data-review-panel="duplicate"]'));
    state.reviewInitialized = true;
  }

  function initializeProcessing() {
    if (state.processingInitialized || !state.jobId) return;
    state.processingController = DedoublonneurPanels.initFolderProcessing(
      state.jobId,
      document.querySelector('[data-panel="export"]'),
    );
    state.processingInitialized = true;
  }

  function selectReviewTab(tabName) {
    state.selectedReview = tabName;
    reviewTabs.forEach((tab) => tab.setAttribute("aria-selected", tab.dataset.reviewTab === tabName ? "true" : "false"));
    reviewPanels.forEach((panel) => {
      panel.hidden = panel.dataset.reviewPanel !== tabName;
    });
  }

  async function syncStatus() {
    const response = await fetch("/api/workflow/status");
    if (!response.ok) throw new Error("Impossible de lire l'etat du workflow.");
    const status = await response.json();
    state.status = status.status;
    state.jobId = status.jobId;
    state.folderName = status.eventFolderName;
    updateContext();
    updateUrl();
    renderStepper();

    if (state.status === "IDLE") {
      showStep("folder");
      setWorkflowMessage("Choisissez un dossier pour commencer.");
      await loadFolders();
    } else if (state.status === "ANALYZING") {
      showStep("analysis");
      setWorkflowMessage("Analyse en cours...");
      await loadAnalysisProgress();
      schedulePoll(syncStatus);
    } else if (state.status === "READY_FOR_REVIEW") {
      initializeReview();
      showStep("review");
      selectReviewTab(state.selectedReview);
      setWorkflowMessage("Analyse terminee. Verifiez les photos avant l'export.");
      clearPoll();
    } else if (state.status === "PROCESSING") {
      initializeProcessing();
      showStep("export");
      setWorkflowMessage("Export en cours...");
      schedulePoll(syncStatus);
    } else if (state.status === "DONE") {
      initializeProcessing();
      showStep("export");
      document.getElementById("process-form").hidden = true;
      document.getElementById("new-workflow").hidden = false;
      document.getElementById("status-message").textContent = "Traitement termine.";
      await state.processingController.loadRecap();
      setWorkflowMessage("Votre dossier trie est pret.");
      clearPoll();
    }
  }

  function clearPoll() {
    if (state.pollTimer) {
      clearTimeout(state.pollTimer);
      state.pollTimer = null;
    }
  }

  function schedulePoll(callback) {
    clearPoll();
    state.pollTimer = setTimeout(() => callback().catch((error) => setWorkflowMessage(error.message)), POLL_INTERVAL_MS);
  }

  async function resumeAnalysis() {
    if (!state.jobId) return;
    const response = await fetch(`/api/jobs/${state.jobId}/resume`, { method: "POST" });
    if (response.ok) {
      resumeAnalysisButton.hidden = true;
      await syncStatus();
    } else {
      setWorkflowMessage("Cette analyse ne peut pas etre reprise pour le moment.");
    }
  }

  async function resetWorkflow() {
    if (!["ANALYZING", "READY_FOR_REVIEW"].includes(state.status)) return;
    const confirmed = window.confirm(
      "Reinitialiser ce workflow ? Les analyses, les vignettes et les choix de revue seront supprimes.",
    );
    if (!confirmed) return;

    resetButtons.forEach((button) => { button.disabled = true; });
    setWorkflowMessage("Reinitialisation en cours...");
    const response = await fetch("/api/workflow/cancel", { method: "POST" });
    if (!response.ok) {
      resetButtons.forEach((button) => { button.disabled = false; });
      setWorkflowMessage("Impossible de reinitialiser le workflow en toute securite.");
      return;
    }

    clearPoll();
    state.status = "IDLE";
    state.jobId = null;
    state.folderName = null;
    state.reviewInitialized = false;
    state.processingInitialized = false;
    state.processingController = null;
    resetButtons.forEach((button) => { button.disabled = false; });
    updateUrl();
    await syncStatus();
  }

  stepper.forEach((button) => button.addEventListener("click", () => showStep(button.dataset.step)));
  reviewTabs.forEach((tab) => tab.addEventListener("click", () => selectReviewTab(tab.dataset.reviewTab)));
  startAnalysisButton.addEventListener("click", () => startAnalysis().catch((error) => setWorkflowMessage(error.message)));
  resumeAnalysisButton.addEventListener("click", () => resumeAnalysis().catch((error) => setWorkflowMessage(error.message)));
  resetButtons.forEach((button) => {
    button.addEventListener("click", () => resetWorkflow().catch((error) => {
      resetButtons.forEach((resetButton) => { resetButton.disabled = false; });
      setWorkflowMessage(error.message);
    }));
  });
  document.getElementById("open-export").addEventListener("click", () => {
    initializeProcessing();
    showStep("export");
  });
  document.getElementById("new-workflow").addEventListener("click", () => {
    clearPoll();
    state.status = "IDLE";
    state.jobId = null;
    state.folderName = null;
    state.reviewInitialized = false;
    state.processingInitialized = false;
    state.processingController = null;
    document.getElementById("process-form").hidden = false;
    document.getElementById("new-workflow").hidden = true;
    document.getElementById("recap").hidden = true;
    document.getElementById("status-message").textContent = "";
    document.getElementById("output-folder-name").value = "";
    updateUrl();
    showStep("folder");
    setWorkflowMessage("Choisissez un dossier pour commencer.");
    loadFolders().catch((error) => setWorkflowMessage(error.message));
  });

  window.addEventListener("workflow-processing-started", () => {
    state.status = "PROCESSING";
    renderStepper();
    setWorkflowMessage("Export en cours...");
  });

  window.addEventListener("workflow-processing-done", () => {
    state.status = "DONE";
    renderStepper();
    document.getElementById("process-form").hidden = true;
    document.getElementById("new-workflow").hidden = false;
    setWorkflowMessage("Votre dossier trie est pret.");
  });

  updateContext();
  syncStatus().catch((error) => setWorkflowMessage(error.message));
})();
