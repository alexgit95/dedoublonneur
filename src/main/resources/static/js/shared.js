// Utilitaires JS partages entre les pages de revue (Flou / Doublons / Traitement).
// Volontairement sans dependance (pas de framework/bundler) : simple module UMD-like,
// exploitable a la fois depuis le navigateur (window.DedoublonneurUI) et depuis Node
// (pour les tests unitaires legers, cf. src/test/js/shared.test.js).
(function (root, factory) {
  if (typeof module === "object" && module.exports) {
    module.exports = factory();
  } else {
    root.DedoublonneurUI = factory();
  }
})(typeof window !== "undefined" ? window : this, function () {
  /** Retarde l'appel de fn jusqu'a ce que delayMs se soit ecoule sans nouvel appel. */
  function debounce(fn, delayMs) {
    let timer = null;
    return function debounced(...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn(...args), delayMs);
    };
  }

  /** Construit l'URL d'une seule page de resultats (jamais l'ensemble des photos d'un coup). */
  function buildPaginatedUrl(baseUrl, page, size) {
    const separator = baseUrl.includes("?") ? "&" : "?";
    return `${baseUrl}${separator}page=${page}&size=${size}`;
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} o`;
    const units = ["Ko", "Mo", "Go", "To"];
    let value = bytes / 1024;
    let unitIndex = 0;
    while (value >= 1024 && unitIndex < units.length - 1) {
      value /= 1024;
      unitIndex++;
    }
    return `${value.toFixed(1)} ${units[unitIndex]}`;
  }

  function workflowStepForStatus(status) {
    return {
      IDLE: "folder",
      ANALYZING: "analysis",
      READY_FOR_REVIEW: "review",
      PROCESSING: "export",
      DONE: "export",
    }[status] || "folder";
  }

  /**
   * Carte photo (vignette en chargement paresseux + case a cocher) partagee par les
   * onglets Flou et Doublons. Ne s'utilise que dans un navigateur (acces au DOM).
   */
  function createPhotoCard(photo, options) {
    const onToggled = (options && options.onToggled) || null;

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
    checkbox.addEventListener("change", () => toggleDeletion(photo.id, checkbox, card, onToggled));

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

  async function toggleDeletion(photoId, checkbox, card, onToggled) {
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
      if (onToggled) {
        onToggled(updated);
      }
    } catch (error) {
      checkbox.checked = previous;
      updateCardState(card, previous);
    } finally {
      checkbox.disabled = false;
    }
  }

  return { debounce, buildPaginatedUrl, formatBytes, workflowStepForStatus, createPhotoCard };
});
