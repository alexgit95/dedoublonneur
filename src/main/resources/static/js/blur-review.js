// Onglet "Flou" : liste paginee des photos floues, cochees par defaut pour suppression.
// NOTE: les aides partagees (debounce, lazy-load generique) seront extraites dans un
// module commun lors de la section "responsive-ui" ; ce fichier reste autonome pour l'instant.
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
    const response = await fetch(`/api/jobs/${jobId}/blurred?page=${page}&size=${PAGE_SIZE}`);
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
      grid.appendChild(renderPhotoCard(photo));
    }
  }

  function renderPhotoCard(photo) {
    const card = document.createElement("article");
    card.className = "photo-card";
    card.dataset.photoId = photo.id;

    const img = document.createElement("img");
    img.src = photo.thumbnailUrl;
    img.loading = "lazy";
    img.alt = photo.relativePath;
    img.onerror = () => {
      img.src = "";
      img.alt = "Vignette indisponible";
    };

    const label = document.createElement("label");
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = photo.markedForDeletion;
    checkbox.addEventListener("change", () => onToggle(photo.id, checkbox, card));

    label.appendChild(checkbox);
    label.appendChild(document.createTextNode("A supprimer"));

    card.appendChild(img);
    card.appendChild(label);
    updateCardState(card, checkbox.checked);
    return card;
  }

  function updateCardState(card, markedForDeletion) {
    card.classList.toggle("photo-card--keeping", !markedForDeletion);
  }

  async function onToggle(photoId, checkbox, card) {
    const previous = !checkbox.checked;
    checkbox.disabled = true;
    try {
      const response = await fetch(`/api/photos/${photoId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ markedForDeletion: checkbox.checked }),
      });
      if (!response.ok) {
        throw new Error("Echec de la mise a jour");
      }
      const updated = await response.json();
      updateCardState(card, updated.markedForDeletion);
    } catch (error) {
      checkbox.checked = previous;
      updateCardState(card, previous);
    } finally {
      checkbox.disabled = false;
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
