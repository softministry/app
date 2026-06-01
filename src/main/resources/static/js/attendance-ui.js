(function () {
  function normalize(value) {
    return (value || "")
      .toString()
      .toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .trim();
  }

  document.addEventListener("DOMContentLoaded", function () {
    var modal = document.querySelector("[data-attendance-modal]");
    var modalContent = document.querySelector("[data-attendance-modal-content]");
    var sessionContextMenu = document.getElementById("attendance-session-context-menu");
    var sessionContextEdit = sessionContextMenu ? sessionContextMenu.querySelector("[data-attendance-context-edit]") : null;
    var sessionContextDelete = sessionContextMenu ? sessionContextMenu.querySelector("[data-attendance-context-delete]") : null;
    var selectedSessionRow = null;
    var attendanceModalRequestId = 0;

    function csrfPair() {
      var param = document.querySelector('meta[name="_csrf_parameter"]');
      var token = document.querySelector('meta[name="_csrf"]');
      var paramName = param ? param.getAttribute("content") : "_csrf";
      var tokenValue = token ? token.getAttribute("content") : "";
      return tokenValue ? [paramName, tokenValue] : null;
    }

    function buildDetailsUrl(date, session) {
      var params = new URLSearchParams();
      params.set("date", date || "");
      params.set("session", session || "");
      var groupInput = document.querySelector('[data-attendance-open-form] select[name="groupId"]');
      if (groupInput && groupInput.value) {
        params.set("groupId", groupInput.value);
      }
      return "/attendance/details?" + params.toString();
    }

    function bindPersonSearch(input, rowSelector) {
      if (!input) return;
      input.addEventListener("input", function () {
        var query = normalize(input.value);
        document.querySelectorAll(rowSelector).forEach(function (row) {
          var name = normalize(row.getAttribute("data-name"));
          row.hidden = query && !name.includes(query);
        });
      });
    }

    function openAttendanceModal(url) {
      if (!modal || !modalContent || !url) return;
      var requestId = ++attendanceModalRequestId;

      fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
        .then(function (response) {
          if (!response.ok) throw new Error("Request failed");
          return response.text();
        })
        .then(function (html) {
          if (requestId !== attendanceModalRequestId) return;
          modalContent.innerHTML = html;
          bindPersonSearch(
            modalContent.querySelector("[data-attendance-modal-search]"),
            "[data-attendance-detail-row]"
          );
          modal.hidden = false;
          document.body.classList.add("modal-open");
        })
        .catch(function () {
          if (requestId !== attendanceModalRequestId) return;
          modalContent.innerHTML = '<p class="flash flash-error">Nu s-au putut încărca detaliile de prezență.</p>';
          modal.hidden = false;
          document.body.classList.add("modal-open");
        });
    }

    function closeAttendanceModal() {
      if (!modal) return;
      attendanceModalRequestId++;
      modal.hidden = true;
      document.body.classList.remove("modal-open");
    }

    function hideSessionContextMenu() {
      if (!sessionContextMenu) return;
      sessionContextMenu.hidden = true;
      selectedSessionRow = null;
    }

    function placeSessionContextMenu(row, event) {
      if (!sessionContextMenu || !row) return;
      var padding = 10;
      var cursorOffset = 8;
      sessionContextMenu.style.left = "0px";
      sessionContextMenu.style.top = "0px";
      sessionContextMenu.hidden = false;

      var rect = row.getBoundingClientRect();
      var menuWidth = sessionContextMenu.offsetWidth;
      var menuHeight = sessionContextMenu.offsetHeight;
      var maxLeft = window.scrollX + window.innerWidth - menuWidth - padding;
      var maxTop = window.scrollY + window.innerHeight - menuHeight - padding;
      var preferredLeft = event ? event.pageX + cursorOffset : window.scrollX + rect.left + 12;
      var preferredTop = window.scrollY + rect.top + ((rect.height - menuHeight) / 2);
      var left = Math.max(window.scrollX + padding, Math.min(preferredLeft, maxLeft));
      var top = Math.max(window.scrollY + padding, Math.min(preferredTop, maxTop));

      sessionContextMenu.style.left = left + "px";
      sessionContextMenu.style.top = top + "px";
    }

    function submitSessionDelete(row) {
      if (!row) return;
      var date = row.getAttribute("data-attendance-date");
      var session = row.getAttribute("data-attendance-session");
      var action = row.getAttribute("data-attendance-delete-url") || "/attendance/delete";
      if (!date || !session) return;
      if (!window.confirm("Sigur vrei să ștergi acest program înregistrat?")) return;

      var form = document.createElement("form");
      form.method = "post";
      form.action = action;

      [
        ["date", date],
        ["session", session],
        ["groupId", row.getAttribute("data-attendance-group-id") || ""]
      ].forEach(function (pair) {
        if (!pair[1]) return;
        var input = document.createElement("input");
        input.type = "hidden";
        input.name = pair[0];
        input.value = pair[1];
        form.appendChild(input);
      });

      var csrf = csrfPair();
      if (csrf) {
        var csrfInput = document.createElement("input");
        csrfInput.type = "hidden";
        csrfInput.name = csrf[0];
        csrfInput.value = csrf[1];
        form.appendChild(csrfInput);
      }

      document.body.appendChild(form);
      form.submit();
    }

    document.querySelectorAll("[data-attendance-details-url]").forEach(function (row) {
      row.addEventListener("click", function () {
        hideSessionContextMenu();
        openAttendanceModal(row.getAttribute("data-attendance-details-url"));
      });

      row.addEventListener("contextmenu", function (event) {
        event.preventDefault();
        selectedSessionRow = row;
        placeSessionContextMenu(row, event);
      });

      row.addEventListener("keydown", function (event) {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          openAttendanceModal(row.getAttribute("data-attendance-details-url"));
        }
      });
    });

    if (sessionContextEdit) {
      sessionContextEdit.addEventListener("click", function () {
        var row = selectedSessionRow;
        hideSessionContextMenu();
        if (row) openAttendanceModal(row.getAttribute("data-attendance-details-url"));
      });
    }

    if (sessionContextDelete) {
      sessionContextDelete.addEventListener("click", function () {
        var row = selectedSessionRow;
        hideSessionContextMenu();
        submitSessionDelete(row);
      });
    }

    document.querySelectorAll("[data-attendance-modal-close]").forEach(function (control) {
      control.addEventListener("click", closeAttendanceModal);
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape" && modal && !modal.hidden) {
        closeAttendanceModal();
      }
      if (event.key === "Escape") {
        hideSessionContextMenu();
      }
    });

    document.addEventListener("click", function (event) {
      if (sessionContextMenu && !sessionContextMenu.hidden && !sessionContextMenu.contains(event.target)) {
        hideSessionContextMenu();
      }
    });

    document.addEventListener("scroll", hideSessionContextMenu, true);
    window.addEventListener("resize", hideSessionContextMenu);

    var filterForm = document.querySelector("[data-attendance-filter-form]");
    if (filterForm) {
      filterForm.querySelectorAll("[data-attendance-auto-submit]").forEach(function (control) {
        control.addEventListener("change", function () {
          if (filterForm.requestSubmit) {
            filterForm.requestSubmit();
            return;
          }
          filterForm.submit();
        });
      });
    }

    var openForm = document.querySelector("[data-attendance-open-form]");
    if (openForm) {
      function buildAttendancePageUrl() {
        var params = new URLSearchParams();
        var dateInput = openForm.querySelector('input[name="date"]');
        var sessionInput = openForm.querySelector('select[name="session"]');
        var groupInput = openForm.querySelector('select[name="groupId"]');
        if (dateInput && dateInput.value) params.set("date", dateInput.value);
        if (sessionInput && sessionInput.value) params.set("session", sessionInput.value);
        if (groupInput && groupInput.value) params.set("groupId", groupInput.value);
        return "/attendance" + (params.toString() ? "?" + params.toString() : "");
      }

      function openFromCurrentForm() {
        var dateInput = openForm.querySelector('input[name="date"]');
        var sessionInput = openForm.querySelector('select[name="session"]');
        if (!dateInput || !sessionInput || !dateInput.value) {
          return;
        }
        openAttendanceModal(buildDetailsUrl(dateInput.value, sessionInput.value));
      }

      openForm.addEventListener("submit", function (event) {
        event.preventDefault();
        openFromCurrentForm();
      });

      var groupSelect = openForm.querySelector('select[name="groupId"]');
      if (groupSelect) {
        groupSelect.addEventListener("change", function () {
          window.location.href = buildAttendancePageUrl();
        });
      }

      var sessionSelect = openForm.querySelector('select[name="session"]');
      if (sessionSelect) {
        sessionSelect.addEventListener("change", openFromCurrentForm);
      }
    }

    bindPersonSearch(document.querySelector("[data-attendance-search]"), "[data-attendance-row]");
  });
})();
