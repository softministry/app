(function () {
  document.addEventListener("DOMContentLoaded", function () {
    var board = document.querySelector("[data-followup-board]");
    var stageForm = document.querySelector("[data-followup-stage-form]");
    if (!board || !stageForm) return;

    var idField = stageForm.querySelector("[data-followup-stage-id]");
    var stageField = stageForm.querySelector("[data-followup-stage-value]");
    if (!idField || !stageField) return;

    var draggedCard = null;

    function clearColumnHighlights() {
      board.querySelectorAll("[data-followup-column]").forEach(function (column) {
        column.classList.remove("is-drop-target");
      });
    }

    function submitStageChange(card, nextStage) {
      if (!card || !nextStage) return;
      var id = card.getAttribute("data-followup-id");
      var currentStage = card.getAttribute("data-stage");
      if (!id || currentStage === nextStage) return;
      idField.value = id;
      stageField.value = nextStage;
      if (stageForm.requestSubmit) {
        stageForm.requestSubmit();
        return;
      }
      stageForm.submit();
    }

    board.querySelectorAll("[data-followup-card]").forEach(function (card) {
      card.addEventListener("dragstart", function (event) {
        draggedCard = card;
        card.classList.add("is-dragging");
        if (event.dataTransfer) {
          event.dataTransfer.effectAllowed = "move";
          event.dataTransfer.setData("text/plain", card.getAttribute("data-followup-id") || "");
        }
      });

      card.addEventListener("dragend", function () {
        card.classList.remove("is-dragging");
        clearColumnHighlights();
        draggedCard = null;
      });
    });

    board.querySelectorAll("[data-followup-column]").forEach(function (column) {
      column.addEventListener("dragover", function (event) {
        event.preventDefault();
        column.classList.add("is-drop-target");
      });

      column.addEventListener("dragleave", function () {
        column.classList.remove("is-drop-target");
      });

      column.addEventListener("drop", function (event) {
        event.preventDefault();
        var nextStage = column.getAttribute("data-stage");
        clearColumnHighlights();
        submitStageChange(draggedCard, nextStage);
      });
    });
  });
})();
