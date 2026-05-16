(() => {
  function initExpandablePanel(panel) {
    const toggle = panel.querySelector("[data-expand-toggle]");
    const content = panel.querySelector("[data-expand-content]");
    if (!toggle || !content) return;

    const setExpanded = (expanded) => {
      panel.classList.toggle("is-expanded", expanded);
      toggle.setAttribute("aria-expanded", expanded ? "true" : "false");
    };

    const collapsedMax = parseFloat(window.getComputedStyle(content).maxHeight || "0");
    const canExpand = content.scrollHeight > collapsedMax + 2;
    if (!canExpand) {
      toggle.style.display = "none";
      panel.classList.remove("is-expanded");
      return;
    }

    setExpanded(panel.classList.contains("is-expanded"));
    toggle.addEventListener("click", () => {
      setExpanded(!panel.classList.contains("is-expanded"));
    });
  }

  function initAll() {
    document.querySelectorAll("[data-expandable-panel]").forEach(initExpandablePanel);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initAll);
  } else {
    initAll();
  }
})();
