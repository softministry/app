(function () {
  var storageKey = "church-office-theme";
  var themeToggle = null;

  function preferredTheme() {
    var current = document.documentElement.getAttribute("data-theme");
    return current === "dark" || current === "light" ? current : "light";
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    if (!themeToggle) return;

    var isDark = theme === "dark";
    themeToggle.setAttribute("aria-pressed", String(isDark));
    themeToggle.setAttribute("aria-label", isDark ? "ActiveazÄƒ light mode" : "ActiveazÄƒ dark mode");
    themeToggle.setAttribute("title", isDark ? "Light mode" : "Dark mode");
  }

  function toggleTheme() {
    var nextTheme = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
    try {
      window.localStorage.setItem(storageKey, nextTheme);
    } catch (ignored) {
      // Keep the toggle functional even when persistence is blocked.
    }
    applyTheme(nextTheme);
  }

  window.ChurchTheme = {
    apply: applyTheme,
    toggle: toggleTheme
  };

  function normalizeSection(pathname) {
    if (!pathname || pathname === "/") return "/dashboard";
    if (pathname === "/dashboard") return "/dashboard";
    if (pathname.startsWith("/events")) return "/events";
    if (pathname.startsWith("/persons") || pathname.startsWith("/families")) return "/persons";
    if (pathname.startsWith("/groups")) return "/groups";
    if (pathname.startsWith("/attendance")) return "/attendance";
    if (pathname.startsWith("/visits")) return "/visits";
    if (pathname.startsWith("/follow-ups")) return "/follow-ups";
    if (pathname.startsWith("/calendar")) return "/calendar";
    if (pathname.startsWith("/reports")) return "/reports";
    if (pathname.startsWith("/finance")) return "/finance";
    if (pathname.startsWith("/settings") || pathname.startsWith("/church")) return "/settings";
    return pathname;
  }

  function openHeaderMenus() {
    return document.querySelectorAll("details.nav-group-menu[open], details.church-select-menu[open]");
  }

  function closeHeaderMenus(exceptTarget) {
    openHeaderMenus().forEach(function (menu) {
      if (exceptTarget && menu.contains(exceptTarget)) return;
      menu.removeAttribute("open");
    });
  }

  function isInsideHeaderMenu(target) {
    if (!target || !target.closest) return false;
    return !!target.closest("details.nav-group-menu, details.church-select-menu");
  }

  document.addEventListener("DOMContentLoaded", function () {
    var activeSection = normalizeSection(window.location.pathname);
    document.querySelectorAll(".nav-icon-link[href], .nav-group-link[href]").forEach(function (link) {
      var href = link.getAttribute("href");
      if (normalizeSection(href) === activeSection) {
        link.classList.add("is-active");
        link.setAttribute("aria-current", "page");
      }
    });

    document.querySelectorAll("[data-nav-group]").forEach(function (group) {
      var hasActiveChild = group.querySelector(".nav-group-link.is-active") !== null;
      var trigger = group.querySelector(".nav-group-trigger");
      if (hasActiveChild && trigger) {
        trigger.classList.add("is-active");
      }
    });

    themeToggle = document.querySelector("[data-theme-toggle]");
    applyTheme(preferredTheme());

    document.addEventListener("click", function (event) {
      if (!isInsideHeaderMenu(event.target)) {
        closeHeaderMenus();
        return;
      }
      closeHeaderMenus(event.target);
    });

    var headerNav = document.querySelector(".header-nav");
    if (headerNav) {
      headerNav.addEventListener("click", function (event) {
        var trigger = event.target.closest(".nav-icon-link[href], .nav-group-link[href], .nav-icon-form button");
        if (!trigger) return;
        closeHeaderMenus();
      });
    }

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        closeHeaderMenus();
      }
    });

    // The click handler is inline in templates so the toggle keeps working even if
    // browsers cache an older copy of this shared script.
  });
})();
