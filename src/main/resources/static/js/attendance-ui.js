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
      modal.hidden = false;
      document.body.classList.add("modal-open");
      modalContent.innerHTML = '<p class="muted">Se încarcă...</p>';

      fetch(url, { headers: { "X-Requested-With": "XMLHttpRequest" } })
        .then(function (response) {
          if (!response.ok) throw new Error("Request failed");
          return response.text();
        })
        .then(function (html) {
          modalContent.innerHTML = html;
          bindPersonSearch(
            modalContent.querySelector("[data-attendance-modal-search]"),
            "[data-attendance-detail-row]"
          );
        })
        .catch(function () {
          modalContent.innerHTML = '<p class="flash flash-error">Nu s-au putut încărca detaliile de prezență.</p>';
        });
    }

    function closeAttendanceModal() {
      if (!modal) return;
      modal.hidden = true;
      document.body.classList.remove("modal-open");
    }

    document.querySelectorAll("[data-attendance-details-url]").forEach(function (row) {
      row.addEventListener("click", function () {
        openAttendanceModal(row.getAttribute("data-attendance-details-url"));
      });

      row.addEventListener("keydown", function (event) {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          openAttendanceModal(row.getAttribute("data-attendance-details-url"));
        }
      });
    });

    document.querySelectorAll("[data-attendance-modal-close]").forEach(function (control) {
      control.addEventListener("click", closeAttendanceModal);
    });

    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape" && modal && !modal.hidden) {
        closeAttendanceModal();
      }
    });

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
