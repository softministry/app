(() => {
  function serializeOrder(grid) {
    return Array.from(grid.querySelectorAll("[data-report-key]"))
      .map((section) => section.getAttribute("data-report-key"))
      .filter(Boolean)
      .join(",");
  }

  function applyOrder() {
    const grid = document.querySelector("[data-reports-grid]");
    if (!grid) return;
    const orderRaw = grid.getAttribute("data-report-order");
    if (!orderRaw) return;

    const keys = orderRaw.split(",").map((item) => item.trim()).filter(Boolean);
    const byKey = new Map();
    grid.querySelectorAll("[data-report-key]").forEach((section) => {
      byKey.set(section.getAttribute("data-report-key"), section);
    });

    const orderedSections = [];
    keys.forEach((key) => {
      const section = byKey.get(key);
      if (section) {
        orderedSections.push(section);
      }
    });
    grid.querySelectorAll("[data-report-key]").forEach((section) => {
      if (!orderedSections.includes(section)) {
        orderedSections.push(section);
      }
    });
    orderedSections.forEach((section) => grid.appendChild(section));

    let dragItem = null;
    let initialOrder = serializeOrder(grid);
    const saveButton = document.querySelector("[data-save-report-layout]");
    const saveStatus = document.querySelector("[data-report-layout-save-status]");
    let statusTimer = null;

    const updateSaveState = () => {
      if (!saveButton) return;
      const isDirty = serializeOrder(grid) !== initialOrder;
      saveButton.disabled = !isDirty;
      saveButton.classList.toggle("is-dirty", isDirty);
      saveButton.classList.toggle("is-hidden", !isDirty);
      if (saveStatus && !isDirty && (!saveStatus.textContent || saveStatus.textContent.trim() === "")) {
        saveStatus.classList.add("is-hidden");
      }
    };

    grid.querySelectorAll("[data-report-key]").forEach((section) => {
      section.setAttribute("draggable", "true");

      section.addEventListener("dragstart", () => {
        dragItem = section;
        section.classList.add("is-dragging");
      });

      section.addEventListener("dragend", () => {
        section.classList.remove("is-dragging");
        dragItem = null;
        updateSaveState();
      });

      section.addEventListener("dragover", (event) => {
        event.preventDefault();
      });

      section.addEventListener("drop", (event) => {
        event.preventDefault();
        if (!dragItem || dragItem === section) return;
        const rect = section.getBoundingClientRect();
        const centerY = rect.top + rect.height / 2;
        const centerX = rect.left + rect.width / 2;
        const sameVisualRow = Math.abs(event.clientY - centerY) < rect.height * 0.35;
        const after = sameVisualRow
          ? event.clientX > centerX
          : event.clientY > centerY;
        if (after) {
          section.after(dragItem);
        } else {
          section.before(dragItem);
        }
        updateSaveState();
      });
    });

    if (saveButton) {
      saveButton.addEventListener("click", async () => {
        const order = serializeOrder(grid);
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        const headers = { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" };
        if (csrfToken && csrfHeader) {
          headers[csrfHeader] = csrfToken;
        }
        const response = await fetch("/reports/layout-order", {
          method: "POST",
          headers,
          body: new URLSearchParams({ layoutOrder: order }).toString()
        });
        if (response.ok) {
          initialOrder = order;
          updateSaveState();
          if (saveStatus) {
            saveStatus.textContent = "Ordinea componentelor a fost salvată.";
            saveStatus.classList.remove("is-error");
            saveStatus.classList.remove("is-hidden");
            if (statusTimer) window.clearTimeout(statusTimer);
            statusTimer = window.setTimeout(() => {
              saveStatus.textContent = "";
              saveStatus.classList.add("is-hidden");
            }, 2500);
          }
        } else if (saveStatus) {
          saveStatus.textContent = "Nu am putut salva ordinea. Încearcă din nou.";
          saveStatus.classList.add("is-error");
          saveStatus.classList.remove("is-hidden");
          if (statusTimer) window.clearTimeout(statusTimer);
          statusTimer = window.setTimeout(() => {
            saveStatus.textContent = "";
            saveStatus.classList.remove("is-error");
            saveStatus.classList.add("is-hidden");
          }, 3500);
        }
      });
      saveButton.classList.add("is-hidden");
      if (saveStatus) saveStatus.classList.add("is-hidden");
      updateSaveState();
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", applyOrder);
  } else {
    applyOrder();
  }
})();
