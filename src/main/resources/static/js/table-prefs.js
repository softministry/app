(function () {
  var configs = {
    "/events": {
      key: "church-office-table-prefs:events",
      params: ["scrollOnly", "sort"]
    },
    "/persons": {
      key: "church-office-table-prefs:persons",
      params: ["scrollOnly"]
    }
  };

  function configForPath(pathname) {
    return configs[pathname] || null;
  }

  function readPrefs(key) {
    try {
      return JSON.parse(window.localStorage.getItem(key) || "{}");
    } catch (ignored) {
      return {};
    }
  }

  function writePrefs(key, prefs) {
    try {
      window.localStorage.setItem(key, JSON.stringify(prefs));
    } catch (ignored) {
      // Preferences are optional; keep the UI functional if storage is blocked.
    }
  }

  function currentParams() {
    return new URLSearchParams(window.location.search);
  }

  function hasFormControl(form, name) {
    return !!form.querySelector('[name="' + name + '"]');
  }

  function appendHiddenControl(form, name, value) {
    var input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    input.setAttribute("data-table-pref-hidden", "true");
    form.appendChild(input);
  }

  function savePresentParams(config) {
    var params = currentParams();
    var prefs = readPrefs(config.key);
    var changed = false;

    config.params.forEach(function (name) {
      if (params.has(name)) {
        prefs[name] = params.get(name);
        changed = true;
      }
    });

    if (changed) {
      writePrefs(config.key, prefs);
    }
  }

  function applyMissingPrefs(config) {
    var prefs = readPrefs(config.key);
    var params = currentParams();
    var changed = false;

    config.params.forEach(function (name) {
      if (!params.has(name) && prefs[name] !== undefined && prefs[name] !== null && prefs[name] !== "") {
        params.set(name, prefs[name]);
        changed = true;
      }
    });

    if (changed) {
      window.location.replace(window.location.pathname + "?" + params.toString());
    }
  }

  function saveFromControl(config, control) {
    var prefs = readPrefs(config.key);
    if (control.type === "checkbox") {
      prefs[control.name] = control.checked ? control.value || "true" : "false";
    } else {
      prefs[control.name] = control.value;
    }
    writePrefs(config.key, prefs);
  }

  function enrichForms(config) {
    var prefs = readPrefs(config.key);
    var params = currentParams();

    document.querySelectorAll('form[method="get"], form:not([method])').forEach(function (form) {
      var action = form.getAttribute("action") || window.location.pathname;
      var actionPath;
      try {
        actionPath = new URL(action, window.location.origin).pathname;
      } catch (ignored) {
        actionPath = window.location.pathname;
      }
      if (actionPath !== window.location.pathname) return;

      config.params.forEach(function (name) {
        if (hasFormControl(form, name)) return;
        var value = params.has(name) ? params.get(name) : prefs[name];
        if (value === undefined || value === null || value === "") return;
        appendHiddenControl(form, name, value);
      });
    });
  }

  function purgeLegacySizePrefs() {
    ["church-office-table-prefs:events", "church-office-table-prefs:persons"].forEach(function (key) {
      try {
        var prefs = JSON.parse(window.localStorage.getItem(key) || "{}");
        if ("size" in prefs) {
          delete prefs["size"];
          window.localStorage.setItem(key, JSON.stringify(prefs));
        }
      } catch (ignored) {}
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    purgeLegacySizePrefs();
    var config = configForPath(window.location.pathname);
    if (!config) return;

    applyMissingPrefs(config);
    savePresentParams(config);
    enrichForms(config);

    config.params.forEach(function (name) {
      document.querySelectorAll('[name="' + name + '"]').forEach(function (control) {
        control.addEventListener("change", function () {
          saveFromControl(config, control);
        });
      });
    });
  });
})();
