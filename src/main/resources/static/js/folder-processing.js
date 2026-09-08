// Action "Traiter le dossier" : demarre la copie, attend la fin par polling, affiche le recap.
// Utilise le module partage shared.js (formatBytes) - cf. responsive-ui.
function initFolderProcessing(jobId, root = document) {
  const POLL_INTERVAL_MS = 2000;

  const form = root.querySelector("#process-form");
  const outputFolderInput = root.querySelector("#output-folder-name");
  const processButton = root.querySelector("#process-button");
  const statusMessage = root.querySelector("#status-message");
  const recapSection = root.querySelector("#recap");
  const progressBar = root.querySelector("#progress-bar");
  const previewButton = root.querySelector("#preview-processing");
  const previewSection = root.querySelector("#processing-preview");
  const progressFill = root.querySelector("#processing-progress-fill");
  const progressLabel = root.querySelector("#processing-progress-label");

  async function startProcessing(outputFolderName) {
    const response = await fetch(`/api/jobs/${jobId}/process`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ outputFolderName }),
    });
    if (!response.ok) {
      const body = await response.text();
      throw new Error(body || `Erreur ${response.status}`);
    }
  }

  async function pollUntilDone() {
    while (true) {
      const response = await fetch(`/api/workflow/status`);
      const status = await response.json();
      const progressResponse = await fetch(`/api/jobs/${jobId}/processing-progress`);
      if (progressResponse.ok) {
        const progress = await progressResponse.json();
        progressFill.style.width = `${progress.progressPercent}%`;
        progressLabel.hidden = false;
        progressLabel.textContent = `${progress.processedItems} / ${progress.totalItems} elements traites (${progress.progressPercent} %) - ${DedoublonneurUI.formatBytes(progress.bytesCopied)} copies`;
      }
      if (status.status === "DONE" || status.lastCompletedJobId === Number(jobId)) {
        return;
      }
      if (status.status === "CANCELLED") throw new Error("Le traitement a ete annule.");
      await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
    }
  }

  async function loadPreview() {
    const response = await fetch(`/api/jobs/${jobId}/process-preview`);
    if (!response.ok) throw new Error("Impossible de calculer le recapitulatif.");
    const preview = await response.json();
    root.querySelector("#preview-kept").textContent = preview.keptPhotoCount;
    root.querySelector("#preview-deleted").textContent = preview.deletedPhotoCount;
    root.querySelector("#preview-videos").textContent = preview.videoCount;
    root.querySelector("#preview-kept-bytes").textContent = DedoublonneurUI.formatBytes(preview.keptBytes);
    root.querySelector("#preview-saved-bytes").textContent = DedoublonneurUI.formatBytes(preview.estimatedSavedBytes);
    previewSection.hidden = false;
  }

  async function loadRecap() {
    const response = await fetch(`/api/jobs/${jobId}/result`);
    if (!response.ok) {
      throw new Error("Recapitulatif indisponible.");
    }
    const recap = await response.json();
    root.querySelector("#recap-kept").textContent = recap.keptCount;
    root.querySelector("#recap-deleted").textContent = recap.deletedCount;
    root.querySelector("#recap-videos").textContent = recap.videoCount;
    root.querySelector("#recap-space-before").textContent = DedoublonneurUI.formatBytes(recap.spaceBeforeBytes);
    root.querySelector("#recap-space-after").textContent = DedoublonneurUI.formatBytes(recap.spaceAfterBytes);
    recapSection.hidden = false;
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!jobId) {
      statusMessage.textContent = "Aucun job specifie (parametre ?jobId= manquant dans l'URL).";
      return;
    }
    processButton.disabled = true;
    statusMessage.textContent = "Traitement en cours...";
    progressBar.hidden = false;
    progressFill.style.width = "0%";
    progressLabel.hidden = false;
    progressLabel.textContent = "0 / 0 elements traites (0 %)";
    try {
      await startProcessing(outputFolderInput.value.trim());
      window.dispatchEvent(new CustomEvent("workflow-processing-started"));
      await pollUntilDone();
      window.dispatchEvent(new CustomEvent("workflow-processing-done"));
      statusMessage.textContent = "Traitement termine.";
      await loadRecap();
    } catch (error) {
      statusMessage.textContent = `Echec du traitement : ${error.message}`;
    } finally {
      processButton.disabled = false;
      progressBar.hidden = true;
      progressLabel.hidden = true;
    }
  });

  previewButton.addEventListener("click", () => loadPreview().catch((error) => {
    statusMessage.textContent = `Echec du calcul : ${error.message}`;
  }));

  return { loadRecap };
}

window.DedoublonneurPanels = window.DedoublonneurPanels || {};
window.DedoublonneurPanels.initFolderProcessing = initFolderProcessing;

if (!document.body.dataset.workflowShell) {
  const params = new URLSearchParams(window.location.search);
  initFolderProcessing(params.get("jobId"));
}
