// Onglet "Doublons" : groupes recalcules a la demande selon le seuil de similarite.
// Utilise le module partage shared.js (debounce/carte photo) - cf. responsive-ui.
function initDuplicateReview(jobId, root = document, defaultThreshold = null) {
  const DEBOUNCE_MS = 300;

  const container = root.querySelector("#duplicate-groups");
  const slider = root.querySelector("#threshold");
  const thresholdValue = root.querySelector("#threshold-value");

  function applyThreshold(value) {
    slider.value = value;
    thresholdValue.textContent = slider.value;
  }

  async function initializeThreshold() {
    if (defaultThreshold === null && jobId) {
      const response = await fetch(`/api/jobs/${jobId}`);
      if (response.ok) {
        defaultThreshold = (await response.json()).defaultSimilarityThreshold;
      }
    }
    applyThreshold(defaultThreshold ?? slider.value);
    await loadGroups(slider.value);
  }

  async function loadGroups(threshold) {
    if (!jobId) {
      container.textContent = "Aucun job specifie (parametre ?jobId= manquant dans l'URL).";
      return;
    }
    const response = await fetch(`/api/jobs/${jobId}/duplicates?threshold=${threshold}`);
    if (!response.ok) {
      container.textContent = "Impossible de charger les groupes de doublons.";
      return;
    }
    const groups = await response.json();
    renderGroups(groups);
  }

  function renderGroups(groups) {
    container.innerHTML = "";
    if (groups.length === 0) {
      container.textContent = "Aucun doublon detecte a ce seuil.";
      return;
    }
    groups.forEach((group, index) => container.appendChild(renderGroup(group, index)));
  }

  function renderGroup(group, index) {
    const section = document.createElement("section");
    section.className = "duplicate-group";

    const title = document.createElement("h2");
    title.textContent = `Groupe ${index + 1} (${group.photos.length} photos)`;
    section.appendChild(title);

    const grid = document.createElement("div");
    grid.className = "photo-grid";
    for (const photo of group.photos) {
      grid.appendChild(DedoublonneurUI.createPhotoCard(photo));
    }
    section.appendChild(grid);
    return section;
  }

  const debouncedLoad = DedoublonneurUI.debounce((threshold) => loadGroups(threshold), DEBOUNCE_MS);

  slider.addEventListener("input", () => {
    thresholdValue.textContent = slider.value;
    debouncedLoad(slider.value);
  });

  initializeThreshold();
  return { reload: () => loadGroups(slider.value) };
}

window.DedoublonneurPanels = window.DedoublonneurPanels || {};
window.DedoublonneurPanels.initDuplicateReview = initDuplicateReview;

if (!document.body.dataset.workflowShell) {
  const params = new URLSearchParams(window.location.search);
  initDuplicateReview(params.get("jobId"));
}
