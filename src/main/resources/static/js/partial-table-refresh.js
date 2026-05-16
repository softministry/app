(function () {
  function csrfMeta(name) {
    var node = document.querySelector("meta[name='" + name + "']");
    return node ? node.getAttribute("content") : "";
  }

  function csrfPair() {
    var paramName = csrfMeta("_csrf_parameter");
    var token = csrfMeta("_csrf");
    if (paramName && token) return [paramName, token];

    var namedInput = document.querySelector("input[type='hidden'][name='_csrf'][value]");
    if (namedInput) {
      var inputValue = namedInput.getAttribute("value");
      if (inputValue) return ["_csrf", inputValue];
    }

    var hiddenInputs = document.querySelectorAll("input[type='hidden'][name][value]");
    for (var i = 0; i < hiddenInputs.length; i += 1) {
      var input = hiddenInputs[i];
      var inputName = (input.getAttribute("name") || "").toLowerCase();
      if (!inputName.includes("csrf")) continue;
      var inputValue = input.getAttribute("value");
      if (!inputValue) continue;
      return [input.getAttribute("name"), inputValue];
    }
    return null;
  }

  function buildBody(form) {
    var body = new URLSearchParams();
    var formData = new FormData(form);
    formData.forEach(function (value, key) {
      var normalized = value == null ? "" : String(value);
      if (!normalized.trim()) return;
      body.append(key, normalized);
    });
    var csrf = csrfPair();
    if (csrf) body.append(csrf[0], csrf[1]);
    return body.toString();
  }

  var requestSeqByTarget = {};

  function refreshPartial(endpoint, targetSelector, body, onFallback) {
    var currentTarget = document.querySelector(targetSelector);
    if (!currentTarget) {
      if (onFallback) onFallback();
      return;
    }

    var seq = (requestSeqByTarget[targetSelector] || 0) + 1;
    requestSeqByTarget[targetSelector] = seq;
    currentTarget.setAttribute("aria-busy", "true");

    fetch(endpoint, {
      method: "POST",
      headers: {
        "X-Requested-With": "XMLHttpRequest",
        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
      },
      body: body
    })
      .then(function (response) {
        if (!response.ok) throw new Error("request_failed");
        return response.text();
      })
      .then(function (html) {
        if ((requestSeqByTarget[targetSelector] || 0) !== seq) return;
        var doc = new DOMParser().parseFromString(html, "text/html");
        var nextTarget = doc.querySelector(targetSelector);
        if (!nextTarget) {
          if (onFallback) onFallback();
          return;
        }
        currentTarget.replaceWith(nextTarget);
        document.dispatchEvent(new CustomEvent("partial:updated", {
          detail: { endpoint: endpoint, targetSelector: targetSelector, target: nextTarget }
        }));
      })
      .catch(function () {
        if (onFallback) onFallback();
      })
      .finally(function () {
        var activeTarget = document.querySelector(targetSelector);
        if (activeTarget) activeTarget.removeAttribute("aria-busy");
      });
  }

  function enableAutoSubmitControls(form) {
    form.querySelectorAll("[data-partial-auto-submit]").forEach(function (control) {
      control.addEventListener("change", function () {
        var pageField = form.querySelector('input[name="page"]');
        if (pageField) pageField.value = "1";
        if (form.requestSubmit) {
          form.requestSubmit();
          return;
        }
        form.submit();
      });
    });
  }

  function bodyFromLink(link) {
    var body = new URLSearchParams();
    var url = new URL(link.getAttribute("href"), window.location.origin);
    url.searchParams.forEach(function (value, key) {
      if (!value || !String(value).trim()) return;
      body.append(key, value);
    });
    var csrf = csrfPair();
    if (csrf) body.append(csrf[0], csrf[1]);
    return body.toString();
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-partial-form]").forEach(enableAutoSubmitControls);
  });

  document.addEventListener("submit", function (event) {
    var form = event.target.closest("[data-partial-form], [data-partial-pagination-form]");
    if (!form || form.getAttribute("data-partial-bypass") === "true") return;

    var endpoint = form.getAttribute("data-partial-endpoint");
    var targetSelector = form.getAttribute("data-partial-target");
    if (!endpoint || !targetSelector) return;

    event.preventDefault();
    refreshPartial(endpoint, targetSelector, buildBody(form), function () {
      form.setAttribute("data-partial-bypass", "true");
      form.submit();
      form.removeAttribute("data-partial-bypass");
    });
  });

  document.addEventListener("click", function (event) {
    var link = event.target.closest("[data-partial-link]");
    if (!link) return;
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    if (event.button && event.button !== 0) return;

    var endpoint = link.getAttribute("data-partial-endpoint");
    var targetSelector = link.getAttribute("data-partial-target");
    if (!endpoint || !targetSelector) return;

    event.preventDefault();
    refreshPartial(endpoint, targetSelector, bodyFromLink(link), function () {
      window.location.assign(link.getAttribute("href"));
    });
  });
})();
