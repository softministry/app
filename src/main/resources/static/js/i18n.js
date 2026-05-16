(function () {
  var STORAGE_KEY = "teamleaf.lang";
  var DEFAULT_LANG = "ro";

  function safeGetLang() {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch (e) {
      return null;
    }
  }

  function safeSetLang(value) {
    try {
      localStorage.setItem(STORAGE_KEY, value);
    } catch (e) {
      // ignore storage failures; language still applies for current request after reload
    }
  }

  function getNestedValue(obj, key) {
    return key.split(".").reduce(function (acc, part) {
      return acc && acc[part] != null ? acc[part] : null;
    }, obj);
  }

  function detectLang() {
    var saved = safeGetLang();
    if (saved === "ro" || saved === "en") return saved;
    var htmlLang = document.documentElement.getAttribute("lang");
    if (htmlLang === "ro" || htmlLang === "en") return htmlLang;
    return DEFAULT_LANG;
  }

  function applyText(root, translations) {
    root.querySelectorAll("[data-i18n]").forEach(function (node) {
      var key = node.getAttribute("data-i18n");
      var value = getNestedValue(translations, key);
      if (typeof value === "string") {
        node.textContent = value;
      }
    });

    root.querySelectorAll("[data-i18n-placeholder]").forEach(function (node) {
      var key = node.getAttribute("data-i18n-placeholder");
      var value = getNestedValue(translations, key);
      if (typeof value === "string") {
        node.setAttribute("placeholder", value);
      }
    });

    root.querySelectorAll("[data-i18n-title]").forEach(function (node) {
      var key = node.getAttribute("data-i18n-title");
      var value = getNestedValue(translations, key);
      if (typeof value === "string") {
        node.setAttribute("title", value);
      }
    });

    root.querySelectorAll("[data-i18n-aria-label]").forEach(function (node) {
      var key = node.getAttribute("data-i18n-aria-label");
      var value = getNestedValue(translations, key);
      if (typeof value === "string") {
        node.setAttribute("aria-label", value);
      }
    });

    root.querySelectorAll("[data-i18n-template]").forEach(function (node) {
      var key = node.getAttribute("data-i18n-template");
      var template = getNestedValue(translations, key);
      if (typeof template !== "string") return;
      var value = template.replace(/\{([a-zA-Z0-9_]+)\}/g, function (_, token) {
        var attr = node.getAttribute("data-i18n-param-" + token);
        return attr != null ? attr : "";
      });
      node.textContent = value;
    });
  }

  window.teamleafI18n = {
    t: function (key, fallback) {
      var translations = window.teamleafTranslations || {};
      var value = getNestedValue(translations, key);
      return typeof value === "string" ? value : fallback;
    },
    apply: function (root) {
      applyText(root || document, window.teamleafTranslations || {});
    }
  };

  function setupLanguageSelector(lang) {
    document.querySelectorAll("[data-language-select]").forEach(function (select) {
      select.value = lang;
      select.addEventListener("change", function () {
        safeSetLang(select.value);
        window.location.reload();
      });
    });
  }

  function loadTranslations(lang) {
    return fetch("/i18n/" + lang + ".json", { headers: { Accept: "application/json" } })
      .then(function (response) {
        if (!response.ok) throw new Error("Failed to load language");
        return response.json();
      });
  }

  document.addEventListener("DOMContentLoaded", function () {
    var lang = detectLang();
    document.documentElement.setAttribute("lang", lang);
    setupLanguageSelector(lang);

    loadTranslations(lang)
      .then(function (translations) {
        window.teamleafTranslations = translations;
        applyText(document, translations);
      })
      .catch(function () {
        if (lang !== DEFAULT_LANG) {
          safeSetLang(DEFAULT_LANG);
          window.location.reload();
        }
      });
  });
})();
