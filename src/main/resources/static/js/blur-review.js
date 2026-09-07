// Onglet "Flou" : liste paginee des photos floues, cochees par defaut pour suppression.
// Utilise le module partage shared.js (debounce/pagination/carte photo) - cf. responsive-ui.
(() => {
  const PAGE_SIZE = 24;

  const params = new URLSearchParams(window.location.search);
  const jobId = params.get("jobId");

  const grid = document.getElementById("blurred-grid");
  const prevButton = document.getElementById("prev-page");
  const nextButton = document.getElementById("next-page");
  const pageIndicator = document.getElementById("page-indicator");

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
})();
