(() => {
  function updateOrderInput(list, input) {
    const order = Array.from(list.querySelectorAll("[data-report-key]"))
      .map((item) => item.getAttribute("data-report-key"))
      .filter(Boolean);
    input.value = order.join(",");
  }

  function init() {
    const form = document.querySelector("[data-reports-order-form]");
    if (!form) return;
    const list = form.querySelector("[data-report-order-list]");
    const input = form.querySelector("[data-report-order-input]");
    if (!list || !input) return;

    let dragItem = null;

    list.querySelectorAll("[data-report-key]").forEach((item) => {
      item.addEventListener("dragstart", () => {
        dragItem = item;
        item.classList.add("is-dragging");
      });

      item.addEventListener("dragend", () => {
        item.classList.remove("is-dragging");
        dragItem = null;
        updateOrderInput(list, input);
      });

      item.addEventListener("dragover", (event) => {
        event.preventDefault();
      });

      item.addEventListener("drop", (event) => {
        event.preventDefault();
        if (!dragItem || dragItem === item) return;
        const rect = item.getBoundingClientRect();
        const after = event.clientY > rect.top + rect.height / 2;
        if (after) {
          item.after(dragItem);
        } else {
          item.before(dragItem);
        }
        updateOrderInput(list, input);
      });
    });

    updateOrderInput(list, input);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
