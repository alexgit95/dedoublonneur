// Onglet "Flou" : liste paginee des photos floues, cochees par defaut pour suppression.
// Utilise le module partage shared.js (debounce/pagination/carte photo) - cf. responsive-ui.
function initBlurReview(jobId, root = document) {
  const PAGE_SIZE = 24;

  const grid = root.querySelector("#blurred-grid");
  const prevButton = root.querySelector("#prev-page");
  const nextButton = root.querySelector("#next-page");
  const pageIndicator = root.querySelector("#page-indicator");

  let currentPage = 0;
  let totalPages = 0;

  async function loadPage(page) {
    if (!jobId) {
      grid.textContent = "Aucun job specifie (parametre ?jobId= manquant dans l'URL).";
      return;
    }
    const url = DedoublonneurUI.buildPaginatedUrl(`/api/jobs/${jobId}/blurred`, page, PAGE_SIZE);
    const response = await fetch(url);
    if (!response.ok) {
      grid.textContent = "Impossible de charger les photos floues.";
      return;
    }
    const data = await response.json();
    currentPage = data.number;
    totalPages = data.totalPages;
    renderGrid(data.content);
    updatePaginationControls();
  }

  function renderGrid(photos) {
    grid.innerHTML = "";
    for (const photo of photos) {
      grid.appendChild(DedoublonneurUI.createPhotoCard(photo));
    }
  }

  function updatePaginationControls() {
    pageIndicator.textContent = totalPages === 0 ? "Aucune photo floue" : `Page ${currentPage + 1} / ${totalPages}`;
    prevButton.disabled = currentPage <= 0;
    nextButton.disabled = currentPage >= totalPages - 1;
  }

  prevButton.addEventListener("click", () => loadPage(currentPage - 1));
  nextButton.addEventListener("click", () => loadPage(currentPage + 1));

  loadPage(0);
  return { reload: () => loadPage(currentPage) };
}

window.DedoublonneurPanels = window.DedoublonneurPanels || {};
window.DedoublonneurPanels.initBlurReview = initBlurReview;

if (!document.body.dataset.workflowShell) {
  const params = new URLSearchParams(window.location.search);
  initBlurReview(params.get("jobId"));
}
