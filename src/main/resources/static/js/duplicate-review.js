// Onglet "Doublons" : groupes recalcules a la demande selon le seuil de similarite.
// Utilise le module partage shared.js (debounce/carte photo) - cf. responsive-ui.
(() => {
  const DEBOUNCE_MS = 300;

  const params = new URLSearchParams(window.location.search);
  const jobId = params.get("jobId");

  const container = document.getElementById("duplicate-groups");
  const slider = document.getElementById("threshold");
  const thresholdValue = document.getElementById("threshold-value");

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

  loadGroups(slider.value);
})();
