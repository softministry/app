(function () {
  function dismissToast(toast) {
    if (!toast || toast.classList.contains("is-dismissing")) return;
    toast.classList.add("is-dismissing");
    window.setTimeout(function () {
      if (toast && toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 220);
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-header-toast]").forEach(function (toast) {
      var durationAttr = toast.getAttribute("data-toast-duration-ms");
      if (durationAttr == null || durationAttr === "") {
        return;
      }
      var duration = Number(durationAttr);
      if (!Number.isFinite(duration) || duration <= 0) return;
      window.setTimeout(function () {
        dismissToast(toast);
      }, duration);
    });

    document.querySelectorAll(".header-toast-action-form").forEach(function (form) {
      form.addEventListener("submit", function () {
        var toast = form.closest("[data-header-toast]");
        dismissToast(toast);
      });
    });
  });
})();
