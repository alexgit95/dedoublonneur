// Action "Traiter le dossier" : demarre la copie, attend la fin par polling, affiche le recap.
// Utilise le module partage shared.js (formatBytes) - cf. responsive-ui.
(() => {
  const POLL_INTERVAL_MS = 2000;

  const params = new URLSearchParams(window.location.search);
  const jobId = params.get("jobId");

  const form = document.getElementById("process-form");
  const outputFolderInput = document.getElementById("output-folder-name");
  const processButton = document.getElementById("process-button");
  const statusMessage = document.getElementById("status-message");
  const recapSection = document.getElementById("recap");
  const progressBar = document.getElementById("progress-bar");

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
      if (status.status === "DONE") {
        return;
      }
      await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
    }
  }

  async function loadRecap() {
    const response = await fetch(`/api/jobs/${jobId}/result`);
    if (!response.ok) {
      throw new Error("Recapitulatif indisponible.");
    }
    const recap = await response.json();
    document.getElementById("recap-kept").textContent = recap.keptCount;
    document.getElementById("recap-deleted").textContent = recap.deletedCount;
    document.getElementById("recap-videos").textContent = recap.videoCount;
    document.getElementById("recap-space-before").textContent = DedoublonneurUI.formatBytes(recap.spaceBeforeBytes);
    document.getElementById("recap-space-after").textContent = DedoublonneurUI.formatBytes(recap.spaceAfterBytes);
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
    try {
      await startProcessing(outputFolderInput.value.trim());
      await pollUntilDone();
      statusMessage.textContent = "Traitement termine.";
      await loadRecap();
    } catch (error) {
      statusMessage.textContent = `Echec du traitement : ${error.message}`;
    } finally {
      processButton.disabled = false;
      progressBar.hidden = true;
    }
  });
})();
