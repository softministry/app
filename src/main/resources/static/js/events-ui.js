(function () {
  function normalize(value) {
    return (value || "")
      .toString()
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .trim();
  }

  function fullName(person) {
    if (!person) return "";
    return [person.lastName, person.firstName].filter(Boolean).join(" ").trim();
  }

  function parseJsonScript(id) {
    var node = document.getElementById(id);
    if (!node) return null;
    try {
      return JSON.parse(node.textContent || "null");
    } catch (error) {
      return null;
    }
  }

  function setupAutocomplete() {
    var root = document.querySelector("[data-person-autocomplete]");
    if (!root) return;

    var persons = parseJsonScript("event-persons-data") || [];
    var input = root.querySelector("[data-autocomplete-input]");
    var hidden = root.querySelector("[data-autocomplete-hidden]");
    var list = root.querySelector("[data-autocomplete-list]");
    var status = root.querySelector("[data-autocomplete-status]");
    if (!input || !hidden || !list) return;
    var required = root.dataset.required === "true";

    var filtered = [];
    var activeIndex = -1;

    function updateValidity() {
      if (!required) return;
      if (!input.value.trim()) {
        input.setCustomValidity("");
      } else if (!hidden.value) {
        input.setCustomValidity("Selectează responsabilul din listă.");
      } else {
        input.setCustomValidity("");
      }
    }

    function setStatus(message) {
      if (!status) return;
      status.textContent = message || "";
    }

    function closeList() {
      list.hidden = true;
      activeIndex = -1;
    }

    function render(items) {
      list.innerHTML = "";
      filtered = items.slice(0, 8);
      activeIndex = -1;

      if (!filtered.length) {
        closeList();
        setStatus(input.value.trim() ? "Nicio persoană găsită." : "Începe să tastezi pentru a căuta responsabilul.");
        return;
      }

      filtered.forEach(function (person, index) {
        var button = document.createElement("button");
        button.type = "button";
        button.className = "autocomplete-option";
        button.textContent = fullName(person);
        button.addEventListener("mousedown", function (event) {
          event.preventDefault();
          select(person);
        });
        if (index === activeIndex) {
          button.classList.add("active");
        }
        list.appendChild(button);
      });

      list.hidden = false;
      setStatus(filtered.length + " rezultate");
    }

    function updateActive(nextIndex) {
      var buttons = list.querySelectorAll(".autocomplete-option");
      buttons.forEach(function (button, index) {
        button.classList.toggle("active", index === nextIndex);
      });
      activeIndex = nextIndex;
    }

    function select(person) {
      input.value = fullName(person);
      hidden.value = person.id || "";
      updateValidity();
      closeList();
      setStatus("Responsabil selectat.");
    }

    function runSearch() {
      var query = normalize(input.value);
      hidden.value = "";
      updateValidity();

      if (!query) {
        closeList();
        setStatus("Începe să tastezi pentru a căuta responsabilul.");
        return;
      }

      var items = persons
        .filter(function (person) {
          return normalize(fullName(person)).includes(query) ||
            normalize(person.firstName).includes(query) ||
            normalize(person.lastName).includes(query);
        })
        .sort(function (a, b) {
          return fullName(a).localeCompare(fullName(b), "ro", { sensitivity: "base" });
        });

      render(items);
    }

    input.addEventListener("focus", function () {
      if (input.value.trim()) {
        runSearch();
      }
    });

    input.addEventListener("input", runSearch);

    input.addEventListener("keydown", function (event) {
      if (list.hidden) return;

      if (event.key === "ArrowDown") {
        event.preventDefault();
        updateActive(Math.min(activeIndex + 1, filtered.length - 1));
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        updateActive(Math.max(activeIndex - 1, 0));
      } else if (event.key === "Enter") {
        if (activeIndex >= 0 && filtered[activeIndex]) {
          event.preventDefault();
          select(filtered[activeIndex]);
        }
      } else if (event.key === "Escape") {
        closeList();
      }
    });

    input.addEventListener("blur", function () {
      window.setTimeout(function () {
        closeList();
        if (!hidden.value) {
          setStatus("Selectează o persoană din listă.");
        }
        updateValidity();
      }, 120);
    });

    var form = root.closest("form");
    if (form && required) {
      form.addEventListener("submit", function () {
        updateValidity();
      });
    }

    document.addEventListener("click", function (event) {
      if (!root.contains(event.target)) {
        closeList();
      }
    });

    if (root.dataset.initialName) {
      input.value = root.dataset.initialName;
      hidden.value = root.dataset.initialId || "";
      updateValidity();
      setStatus("Responsabil selectat.");
    }
  }

  function fetchText(url) {
    return fetch(url, { headers: { Accept: "text/plain" } })
      .then(function (response) {
        return response.ok ? response.text() : "{}";
      })
      .then(function (text) {
        try {
          return text ? JSON.parse(text) : {};
        } catch (error) {
          return {};
        }
      })
      .catch(function () {
        return {};
      });
  }

  function applyVisualCustomization() {
    var root = document.querySelector("[data-events-visuals]");
    if (!root) return;

    Promise.all([
      fetchText("/api/global-preferences/status-customization"),
      fetchText("/api/global-preferences/priority-customization"),
      fetchText("/api/global-preferences/name-customization")
    ]).then(function (results) {
      var statusCustom = results[0] || {};
      var priorityCustom = results[1] || {};
      var nameCustom = results[2] || {};

      root.querySelectorAll(".status-pill[data-status]").forEach(function (node) {
        var key = node.getAttribute("data-status");
        var custom = statusCustom[key];
        if (!custom) return;
        applyNodeStyle(node, custom, true);
      });

      root.querySelectorAll(".priority-pill[data-priority]").forEach(function (node) {
        var raw = node.getAttribute("data-priority");
        var key = priorityMap(raw);
        var custom = priorityCustom[key];
        if (!custom) return;
        applyNodeStyle(node, custom, true);
      });

      root.querySelectorAll(".event-name").forEach(function (node) {
        if (!nameCustom || (!nameCustom.color && !nameCustom.fullCell)) return;
        applyEventNameStyle(node, nameCustom);
      });
    });
  }

  function formatPeriodRanges() {
    document.querySelectorAll(".js-period-range").forEach(function (node) {
      var start = node.getAttribute("data-start");

      if (!start) {
        node.textContent = "—";
        node.title = "—";
        return;
      }

      var startDate = new Date(start + "T00:00:00");
      var dd = String(startDate.getDate()).padStart(2, "0");
      var mm = String(startDate.getMonth() + 1).padStart(2, "0");
      var yyyy = startDate.getFullYear();
      var text = dd + "-" + mm + "-" + yyyy;

      node.textContent = text;
      node.title = text;
    });
  }

  function setupFilterAutoSubmit() {
    var filterForm = document.querySelector("[data-events-filter-form]");
    if (!filterForm) return;
    var requestSeq = 0;

    function csrfPair() {
      var candidate = document.querySelector("input[type='hidden'][name][value]");
      if (!candidate) return null;
      var name = candidate.getAttribute("name");
      var value = candidate.getAttribute("value");
      if (!name || !value) return null;
      return [name, value];
    }

    function buildBody() {
      var body = new URLSearchParams();
      var formData = new FormData(filterForm);
      formData.forEach(function (value, key) {
        var normalized = (value == null ? "" : String(value)).trim();
        if (!normalized) return;
        body.append(key, normalized);
      });
      var csrf = csrfPair();
      if (csrf) body.append(csrf[0], csrf[1]);
      return body;
    }

    function refreshRowsOnly() {
      var currentResults = document.querySelector("[data-events-results]");
      if (!currentResults) {
        filterForm.submit();
        return;
      }

      var seq = ++requestSeq;
      currentResults.setAttribute("aria-busy", "true");

      fetch("/events/results", {
        method: "POST",
        headers: {
          "X-Requested-With": "XMLHttpRequest",
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: buildBody().toString()
      })
        .then(function (response) {
          if (!response.ok) {
            throw new Error("request_failed");
          }
          return response.text();
        })
        .then(function (html) {
          if (seq !== requestSeq) return;
          var doc = new DOMParser().parseFromString(html, "text/html");
          var nextResults = doc.querySelector("[data-events-results]");
          if (!nextResults) {
            filterForm.submit();
            return;
          }
          currentResults.replaceWith(nextResults);
          applyVisualCustomization();
          formatPeriodRanges();
        })
        .catch(function () {
          filterForm.submit();
        })
        .finally(function () {
          var activeResults = document.querySelector("[data-events-results]");
          if (activeResults) {
            activeResults.removeAttribute("aria-busy");
          }
        });
    }

    filterForm.querySelectorAll("[data-events-auto-submit]").forEach(function (control) {
      control.addEventListener("change", function () {
        var pageField = filterForm.querySelector('input[name="page"]');
        if (pageField) pageField.value = "1";
        refreshRowsOnly();
      });
    });
  }

  function setupEventDeleteConfirm(root) {
    var scope = root || document;
    scope.querySelectorAll("form[data-event-delete-form]").forEach(function (form) {
      if (form.dataset.confirmBound === "1") return;
      form.dataset.confirmBound = "1";
      form.addEventListener("submit", function (event) {
        var message = "Sigur vrei să ștergi acest eveniment?";
        if (window.teamleafI18n && typeof window.teamleafI18n.t === "function") {
          message = window.teamleafI18n.t("events.delete_confirm", message);
        }
        if (!window.confirm(message)) {
          event.preventDefault();
        }
      });
    });
  }

  function priorityMap(raw) {
    switch ((raw || "").toUpperCase()) {
      case "HIGH":
        return 1;
      case "HIGH_MEDIUM":
        return 2;
      case "MEDIUM_LOW":
        return 4;
      case "LOW":
        return 5;
      default:
        return 3;
    }
  }

  function applyNodeStyle(node, custom, forceContrast) {
    if (custom.color) {
      node.style.backgroundColor = custom.color;
      node.style.borderColor = custom.color;
      if (forceContrast) {
        node.style.setProperty("color", readableTextColor(custom.color), "important");
      } else if (custom.fullCell) {
        node.style.setProperty("color", readableTextColor(custom.color), "important");
      } else {
        node.style.color = custom.color;
      }
    }

    if (custom.fullCell) {
      var cell = node.closest(".visual-cell");
      if (cell) {
        cell.classList.add("full-cell");
      }
    }
  }

  function applyEventNameStyle(node, custom) {
    if (!custom.color) return;

    node.style.backgroundColor = "transparent";
    node.style.borderColor = "transparent";
    node.style.setProperty("color", custom.color, "important");

    var cell = node.closest(".visual-cell");
    if (cell) {
      cell.classList.remove("full-cell");
    }
  }

  function readableTextColor(backgroundColor) {
    var hex = (backgroundColor || "").trim().replace("#", "");
    if (hex.length === 3) {
      hex = hex.split("").map(function (char) {
        return char + char;
      }).join("");
    }
    if (!/^[0-9a-fA-F]{6}$/.test(hex)) {
      return "#ffffff";
    }

    var red = parseInt(hex.slice(0, 2), 16);
    var green = parseInt(hex.slice(2, 4), 16);
    var blue = parseInt(hex.slice(4, 6), 16);
    var luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
    return luminance > 0.58 ? "#111827" : "#ffffff";
  }

  document.addEventListener("DOMContentLoaded", function () {
    setupFilterAutoSubmit();
    setupAutocomplete();
    setupEventDeleteConfirm(document);
    applyVisualCustomization();
    formatPeriodRanges();
  });

  document.addEventListener("partial:updated", function (event) {
    var detail = event && event.detail ? event.detail : null;
    if (!detail || detail.targetSelector !== "[data-events-results]") return;
    setupEventDeleteConfirm(document);
    applyVisualCustomization();
    formatPeriodRanges();
  });
})();
