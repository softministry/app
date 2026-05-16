(function () {
  const searchInput = document.querySelector('[data-member-search]');
  const memberList = document.querySelector('[data-member-list]');
  const emptyState = document.querySelector('[data-member-empty]');
  const selectedContainer = document.querySelector('[data-member-selected]');
  const countLabel = document.querySelector('[data-member-count]');
  const modalCountLabel = document.querySelector('[data-member-count-modal]');
  const selectVisibleButton = document.querySelector('[data-member-select-visible]');
  const clearButton = document.querySelector('[data-member-clear]');
  const modal = document.querySelector('[data-member-picker-modal]');
  const openButton = document.querySelector('[data-member-picker-open]');
  const closeButtons = document.querySelectorAll('[data-member-picker-close]');
  const groupFormModal = document.querySelector('[data-group-form-modal]');
  const groupCreateButton = document.querySelector('[data-group-create]');
  const groupEditButtons = document.querySelectorAll('[data-group-edit]');
  const groupFormCloseButtons = document.querySelectorAll('[data-group-form-close]');
  const groupForm = document.querySelector('[data-group-form]');
  const groupTitle = document.querySelector('[data-group-form-title]');
  const groupIdField = document.querySelector('[data-group-id-field]');
  const groupNameField = document.querySelector('[data-group-name-field]');
  const groupTypeField = document.querySelector('[data-group-type-field]');
  const groupLeaderField = document.querySelector('[data-group-leader-field]');
  const groupDescriptionField = document.querySelector('[data-group-description-field]');
  const groupSubmitButton = document.querySelector('[data-group-submit]');

  if (!searchInput || !memberList || !emptyState || !selectedContainer || !countLabel) {
    return;
  }

  const items = Array.from(memberList.querySelectorAll('[data-member-name]')).map((item, index) => ({
    item,
    checkbox: item.querySelector('input[type="checkbox"]'),
    id: item.getAttribute('data-member-id'),
    name: item.getAttribute('data-member-name') || 'Persoană fără nume',
    normalizedName: normalize(item.getAttribute('data-member-name')),
    initialIndex: index
  })).filter((entry) => entry.checkbox);

  function normalize(value) {
    return (value || '')
      .toString()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  function selectedEntries() {
    return items.filter((entry) => entry.checkbox.checked);
  }

  function updateSelectedChips() {
    const selected = selectedEntries();
    const countText = `${selected.length} ${selected.length === 1 ? 'selectat' : 'selectați'}`;
    countLabel.textContent = countText;
    if (modalCountLabel) {
      modalCountLabel.textContent = countText;
    }
    selectedContainer.classList.toggle('is-empty', selected.length === 0);
    selectedContainer.innerHTML = '';

    if (selected.length === 0) {
      const empty = document.createElement('span');
      empty.className = 'muted';
      empty.textContent = 'Niciun membru selectat.';
      selectedContainer.appendChild(empty);
      return;
    }

    selected.forEach((entry) => {
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'group-member-chip';
      chip.setAttribute('data-remove-member-id', entry.id);
      chip.setAttribute('aria-label', `Elimină ${entry.name}`);
      chip.innerHTML = `<span>${escapeHtml(entry.name)}</span><strong aria-hidden="true">×</strong>`;
      selectedContainer.appendChild(chip);
    });
  }

  function escapeHtml(value) {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function sortItems() {
    items
      .slice()
      .sort((left, right) => {
        if (left.checkbox.checked !== right.checkbox.checked) {
          return left.checkbox.checked ? -1 : 1;
        }
        return left.initialIndex - right.initialIndex;
      })
      .forEach((entry) => memberList.insertBefore(entry.item, emptyState));
  }

  function applyFilter() {
    const query = normalize(searchInput.value);
    let visibleCount = 0;

    items.forEach((entry) => {
      const visible = !query || entry.normalizedName.includes(query);
      entry.item.hidden = !visible;
      if (visible) visibleCount += 1;
    });

    emptyState.classList.toggle('is-hidden', visibleCount > 0);
  }

  function refresh() {
    sortItems();
    applyFilter();
    updateSelectedChips();
  }

  function translated(key, fallback) {
    if (window.teamleafI18n && typeof window.teamleafI18n.t === 'function') {
      return window.teamleafI18n.t(key, fallback);
    }
    return fallback;
  }

  function updateBodyModalState() {
    const groupFormOpen = groupFormModal && !groupFormModal.hidden;
    const memberPickerOpen = modal && !modal.hidden;
    document.body.classList.toggle('modal-open', Boolean(groupFormOpen || memberPickerOpen));
  }

  function openModal() {
    if (!modal) return;
    modal.hidden = false;
    updateBodyModalState();
    window.setTimeout(() => searchInput.focus(), 0);
  }

  function closeModal() {
    if (!modal) return;
    modal.hidden = true;
    updateBodyModalState();
    if (openButton) {
      openButton.focus();
    }
  }

  function setSelectedMemberIds(memberIds) {
    const selectedIds = new Set(memberIds.map((id) => id.toString()));
    items.forEach((entry) => {
      entry.checkbox.checked = selectedIds.has(entry.id);
    });
    refresh();
  }

  function openGroupForm(mode, data) {
    if (!groupFormModal || !groupForm) return;

    const isEdit = mode === 'edit';
    groupForm.action = isEdit ? `/groups/${data.id}` : '/groups';
    if (groupTitle) groupTitle.textContent = isEdit ? translated('actions.edit', 'Editează') : translated('groups.add', 'Adaugă grup');
    if (groupSubmitButton) groupSubmitButton.textContent = isEdit ? translated('actions.save', 'Salvează') : translated('actions.create', 'Creează');
    if (groupIdField) groupIdField.value = isEdit ? data.id : '';
    if (groupNameField) groupNameField.value = isEdit ? data.name : '';
    if (groupTypeField) groupTypeField.value = isEdit ? data.type : 'SMALL_GROUP';
    if (groupLeaderField) groupLeaderField.value = isEdit ? data.leaderId : '';
    if (groupDescriptionField) groupDescriptionField.value = isEdit ? data.description : '';

    searchInput.value = '';
    setSelectedMemberIds(isEdit && data.memberIds ? data.memberIds.split(',').filter(Boolean) : []);
    groupFormModal.hidden = false;
    updateBodyModalState();
    window.setTimeout(() => groupNameField && groupNameField.focus(), 0);
  }

  function closeGroupForm() {
    if (!groupFormModal) return;
    groupFormModal.hidden = true;
    if (modal) {
      modal.hidden = true;
    }
    updateBodyModalState();
  }

  memberList.addEventListener('change', (event) => {
    if (event.target.matches('input[type="checkbox"]')) {
      refresh();
    }
  });

  selectedContainer.addEventListener('click', (event) => {
    const removeButton = event.target.closest('[data-remove-member-id]');
    if (!removeButton) return;

    const id = removeButton.getAttribute('data-remove-member-id');
    const entry = items.find((item) => item.id === id);
    if (entry) {
      entry.checkbox.checked = false;
      refresh();
    }
  });

  searchInput.addEventListener('input', applyFilter);

  if (selectVisibleButton) {
    selectVisibleButton.addEventListener('click', () => {
      items.forEach((entry) => {
        if (!entry.item.hidden) {
          entry.checkbox.checked = true;
        }
      });
      refresh();
    });
  }

  if (clearButton) {
    clearButton.addEventListener('click', () => {
      items.forEach((entry) => {
        entry.checkbox.checked = false;
      });
      refresh();
    });
  }

  if (openButton) {
    openButton.addEventListener('click', openModal);
  }

  closeButtons.forEach((button) => {
    button.addEventListener('click', closeModal);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') return;
    if (modal && !modal.hidden) {
      closeModal();
      return;
    }
    if (groupFormModal && !groupFormModal.hidden) {
      closeGroupForm();
    }
  });

  if (groupCreateButton) {
    groupCreateButton.addEventListener('click', () => openGroupForm('create', {}));
  }

  groupEditButtons.forEach((button) => {
    button.addEventListener('click', () => {
      openGroupForm('edit', {
        id: button.getAttribute('data-group-id') || '',
        name: button.getAttribute('data-group-name') || '',
        type: button.getAttribute('data-group-type') || 'SMALL_GROUP',
        leaderId: button.getAttribute('data-group-leader-id') || '',
        description: button.getAttribute('data-group-description') || '',
        memberIds: button.getAttribute('data-group-member-ids') || ''
      });
    });
  });

  groupFormCloseButtons.forEach((button) => {
    button.addEventListener('click', closeGroupForm);
  });

  refresh();
})();
