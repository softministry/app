(function () {
  const form = document.querySelector('[data-appearance-editor]');
  const config = window.settingsAppearanceConfig;

  if (!form || !config) {
    return;
  }

  const statusField = form.querySelector('[name="statusCustomization"]');
  const priorityField = form.querySelector('[name="priorityCustomization"]');
  const nameField = form.querySelector('[name="nameCustomization"]');
  const resetColorsButton = form.querySelector('[data-reset-appearance-colors]');
  const i18n = window.teamleafI18n;

  const t = (key, fallback) => {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key, fallback);
    }
    return fallback;
  };

  const slugify = (value) => {
    return String(value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  };

  const parseJson = (raw, fallback) => {
    try {
      return raw ? JSON.parse(raw) : fallback;
    } catch (_) {
      return fallback;
    }
  };

  const statusState = {};
  config.statuses.forEach((status) => {
    statusState[status] = {
      ...(config.statusDefaults[status] || {}),
      ...(parseJson(statusField.value, {})[status] || {})
    };
  });
  let selectedStatus = config.statuses[0] || null;

  const priorityState = {};
  config.priorities.forEach((priority) => {
    priorityState[priority] = {
      ...(config.priorityDefaults[String(priority)] || config.priorityDefaults[priority] || {}),
      ...(parseJson(priorityField.value, {})[priority] || parseJson(priorityField.value, {})[String(priority)] || {})
    };
  });
  let selectedPriority = config.priorities[0] || null;

  const priorityLabel = (priority) => {
    const base = (config.priorityLabels && (config.priorityLabels[priority] || config.priorityLabels[String(priority)]))
      || `Priority ${priority}`;
    return t(`settings.priorityLabel.${slugify(base)}`, base);
  };

  const statusLabel = (status) => t(`settings.statusLabel.${slugify(status)}`, status);

  const nameState = {
    ...config.nameDefaults,
    ...parseJson(nameField.value, {})
  };

  const syncFields = () => {
    statusField.value = JSON.stringify(statusState);
    priorityField.value = JSON.stringify(priorityState);
    nameField.value = JSON.stringify(nameState);
  };

  const resetToDefaults = () => {
    config.statuses.forEach((status) => {
      statusState[status] = { ...(config.statusDefaults[status] || {}) };
    });
    config.priorities.forEach((priority) => {
      priorityState[priority] = {
        ...(config.priorityDefaults[String(priority)] || config.priorityDefaults[priority] || {})
      };
    });
    Object.keys(nameState).forEach((key) => delete nameState[key]);
    Object.assign(nameState, config.nameDefaults);
    syncFields();
    renderStatusList();
    renderPriorityList();
    renderNameList();
  };

  const paletteMarkup = (selectedColor, onPick) => {
    const wrap = document.createElement('div');
    wrap.className = 'appearance-palette';

    config.palette.forEach((color) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'appearance-swatch';
      button.style.background = color;
      if ((selectedColor || '').toLowerCase() === color.toLowerCase()) {
        button.classList.add('is-selected');
      }
      button.addEventListener('click', () => onPick(color));
      wrap.appendChild(button);
    });

    return wrap;
  };

  const fullCellToggle = (checked, onChange) => {
    const label = document.createElement('label');
    label.className = 'checkbox-inline';

    const input = document.createElement('input');
    input.type = 'checkbox';
    input.checked = !!checked;
    input.addEventListener('change', () => onChange(input.checked));

    const span = document.createElement('span');
    span.textContent = t('settings.fullCell', 'Full cell');

    label.append(input, span);
    return label;
  };

  const preview = (labelText, state) => {
    const element = document.createElement('div');
    element.className = 'appearance-preview';
    if (state.fullCell) {
      element.style.background = state.color;
      element.style.color = '#fff';
      element.style.borderColor = state.color;
    } else {
      element.style.borderLeftColor = state.color;
      element.style.color = state.color;
    }
    element.textContent = labelText;
    return element;
  };

  const renderStatusList = () => {
    const target = form.querySelector('[data-appearance-list="status"]');
    target.innerHTML = '';

    if (!selectedStatus || !statusState[selectedStatus]) {
      selectedStatus = config.statuses[0] || null;
    }
    if (!selectedStatus) {
      return;
    }

    const item = document.createElement('div');
    item.className = 'appearance-item';

    const title = document.createElement('div');
    title.className = 'appearance-label';

    const select = document.createElement('select');
    select.className = 'appearance-status-select';
    config.statuses.forEach((status) => {
      const option = document.createElement('option');
      option.value = status;
      option.textContent = statusLabel(status);
      if (status === selectedStatus) {
        option.selected = true;
      }
      select.appendChild(option);
    });
    select.addEventListener('change', () => {
      selectedStatus = select.value;
      renderStatusList();
    });
    title.appendChild(select);

    const controls = document.createElement('div');
    controls.className = 'appearance-controls';

    controls.appendChild(preview(statusLabel(selectedStatus), statusState[selectedStatus]));
    controls.appendChild(paletteMarkup(statusState[selectedStatus].color, (color) => {
      statusState[selectedStatus].color = color;
      syncFields();
      renderStatusList();
    }));
    controls.appendChild(fullCellToggle(statusState[selectedStatus].fullCell, (value) => {
      statusState[selectedStatus].fullCell = value;
      syncFields();
      renderStatusList();
    }));

    item.append(title, controls);
    target.appendChild(item);
  };

  const renderPriorityList = () => {
    const target = form.querySelector('[data-appearance-list="priority"]');
    target.innerHTML = '';

    if (selectedPriority === null || selectedPriority === undefined || !priorityState[selectedPriority]) {
      selectedPriority = config.priorities[0] || null;
    }
    if (selectedPriority === null || selectedPriority === undefined) {
      return;
    }

    const item = document.createElement('div');
    item.className = 'appearance-item';

    const title = document.createElement('div');
    title.className = 'appearance-label';

    const select = document.createElement('select');
    select.className = 'appearance-status-select';
    config.priorities.forEach((priority) => {
      const option = document.createElement('option');
      option.value = String(priority);
      option.textContent = priorityLabel(priority);
      if (String(priority) === String(selectedPriority)) {
        option.selected = true;
      }
      select.appendChild(option);
    });
    select.addEventListener('change', () => {
      selectedPriority = select.value;
      renderPriorityList();
    });
    title.appendChild(select);

    const controls = document.createElement('div');
    controls.className = 'appearance-controls';

    controls.appendChild(preview(priorityLabel(selectedPriority), priorityState[selectedPriority]));
    controls.appendChild(paletteMarkup(priorityState[selectedPriority].color, (color) => {
      priorityState[selectedPriority].color = color;
      syncFields();
      renderPriorityList();
    }));
    controls.appendChild(fullCellToggle(priorityState[selectedPriority].fullCell, (value) => {
      priorityState[selectedPriority].fullCell = value;
      syncFields();
      renderPriorityList();
    }));

    item.append(title, controls);
    target.appendChild(item);
  };

  const renderNameList = () => {
    const target = form.querySelector('[data-appearance-list="name"]');
    target.innerHTML = '';

    const item = document.createElement('div');
    item.className = 'appearance-item';

    const title = document.createElement('div');
    title.className = 'appearance-label';
    title.textContent = t('settings.eventNameLabel', 'Event name');

    const controls = document.createElement('div');
    controls.className = 'appearance-controls';

    controls.appendChild(preview(t('settings.eventNameLabel', 'Event name'), nameState));
    controls.appendChild(paletteMarkup(nameState.color, (color) => {
      nameState.color = color;
      syncFields();
      renderNameList();
    }));
    controls.appendChild(fullCellToggle(nameState.fullCell, (value) => {
      nameState.fullCell = value;
      syncFields();
      renderNameList();
    }));

    item.append(title, controls);
    target.appendChild(item);
  };

  syncFields();
  renderStatusList();
  renderPriorityList();
  renderNameList();

  if (resetColorsButton) {
    resetColorsButton.addEventListener('click', resetToDefaults);
  }
})();
