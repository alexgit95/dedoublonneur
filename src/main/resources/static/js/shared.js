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
  const HOLD_DELAY_MS = 450;
  const MOVE_CANCEL_DISTANCE = 10;

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

  function getPreviewOverlay() {
    let overlay = document.querySelector("[data-photo-preview]");
    if (overlay) return overlay;

    overlay = document.createElement("div");
    overlay.className = "photo-preview";
    overlay.dataset.photoPreview = "true";
    overlay.hidden = true;
    overlay.setAttribute("role", "dialog");
    overlay.setAttribute("aria-modal", "true");
    overlay.setAttribute("aria-label", "Apercu de la photo");
    overlay.innerHTML = `
      <div class="photo-preview__backdrop" data-preview-close></div>
      <div class="photo-preview__surface" role="document">
        <p class="photo-preview__status" aria-live="polite">Chargement...</p>
        <img class="photo-preview__image" alt="" />
        <button class="photo-preview__close" type="button" data-preview-close aria-label="Fermer l'apercu">Fermer</button>
      </div>
    `;
    document.body.appendChild(overlay);

    const close = () => closePhotoPreview(overlay);
    overlay.querySelectorAll("[data-preview-close]").forEach((element) => element.addEventListener("click", close));
    overlay.addEventListener("pointerup", (event) => {
      if (event.target === overlay || event.target.classList.contains("photo-preview__backdrop")) close();
    });
    return overlay;
  }

  function openPhotoPreview(photo, previousFocus) {
    const overlay = getPreviewOverlay();
    const image = overlay.querySelector(".photo-preview__image");
    const status = overlay.querySelector(".photo-preview__status");
    image.src = photo.thumbnailUrl;
    image.alt = photo.relativePath;
    status.textContent = "Apercu thumbnail";
    overlay.hidden = false;
    document.body.classList.add("photo-preview-open");
    overlay._previousFocus = previousFocus || document.activeElement;
    overlay.querySelector(".photo-preview__close").focus({ preventScroll: true });
    return overlay;
  }

  function closePhotoPreview(overlay) {
    if (!overlay || overlay.hidden) return;
    const previousFocus = overlay._previousFocus;
    overlay.hidden = true;
    document.body.classList.remove("photo-preview-open");
    if (previousFocus && typeof previousFocus.focus === "function") previousFocus.focus({ preventScroll: true });
  }

  function attachPhotoPreview(image, photo) {
    let holdTimer = null;
    let pointerId = null;
    let startX = 0;
    let startY = 0;
    let opened = false;

    const clearHold = () => {
      if (holdTimer !== null) {
        clearTimeout(holdTimer);
        holdTimer = null;
      }
    };

    const release = () => {
      clearHold();
      if (opened) closePhotoPreview(document.querySelector("[data-photo-preview]"));
      opened = false;
      pointerId = null;
    };

    image.addEventListener("pointerdown", (event) => {
      if (event.button !== undefined && event.button !== 0) return;
      if (event.target.closest("input, label, button, a")) return;
      pointerId = event.pointerId;
      startX = event.clientX;
      startY = event.clientY;
      image.setPointerCapture?.(pointerId);
      holdTimer = setTimeout(() => {
        opened = true;
        openPhotoPreview(photo, image);
      }, HOLD_DELAY_MS);
    });

    image.addEventListener("pointermove", (event) => {
      if (pointerId !== event.pointerId || opened) return;
      if (Math.hypot(event.clientX - startX, event.clientY - startY) > MOVE_CANCEL_DISTANCE) release();
    });
    image.addEventListener("pointerup", release);
    image.addEventListener("pointercancel", release);
    image.addEventListener("lostpointercapture", release);
    image.addEventListener("dragstart", (event) => event.preventDefault());
  }

  if (typeof document !== "undefined") {
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") closePhotoPreview(document.querySelector("[data-photo-preview]"));
    });
    document.addEventListener("pointerup", () => {
      closePhotoPreview(document.querySelector("[data-photo-preview]"));
    });
    document.addEventListener("pointercancel", () => {
      closePhotoPreview(document.querySelector("[data-photo-preview]"));
    });
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
    attachPhotoPreview(img, photo);

    const label = document.createElement("label");
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = photo.markedForDeletion;
    checkbox.addEventListener("change", () => toggleDeletion(photo.id, checkbox, card, onToggled));

    label.appendChild(checkbox);
    const stateLabel = document.createElement("span");
    stateLabel.className = "photo-card__state";
    label.appendChild(stateLabel);

    card.appendChild(img);
    card.appendChild(label);
    updateCardState(card, checkbox.checked);
    return card;
  }

  function updateCardState(card, markedForDeletion) {
    card.classList.toggle("photo-card--keeping", !markedForDeletion);
    card.classList.toggle("photo-card--marked-for-deletion", markedForDeletion);
    const stateLabel = card.querySelector(".photo-card__state");
    if (stateLabel) stateLabel.textContent = markedForDeletion ? "A supprimer" : "Conservee";
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

  return {
    debounce,
    buildPaginatedUrl,
    formatBytes,
    workflowStepForStatus,
    createPhotoCard,
    HOLD_DELAY_MS,
    MOVE_CANCEL_DISTANCE,
  };
});
