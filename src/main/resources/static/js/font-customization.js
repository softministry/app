(function () {
  const contextMenu = document.getElementById('global-font-context-menu');
  const editBtn = document.getElementById('fontContextEditBtn');
  const modal = document.getElementById('font-customization-modal');
  const form = document.getElementById('font-customization-form');
  const colorInput = document.getElementById('font-custom-color');
  const sizeInput = document.getElementById('font-custom-size');
  const categoryInput = document.getElementById('font-target-category');

  let currentTarget = null;
  let currentCategory = null;

  function getCategory(element) {
    if (element.classList.contains('person-name')) return 'person';
    if (element.classList.contains('event-name')) return 'event';
    if (element.classList.contains('group-name')) return 'group';
    return null;
  }

  function hideContextMenu() {
    if (contextMenu) contextMenu.style.display = 'none';
  }

  function showModal(category) {
    if (!modal) return;
    currentCategory = category;
    categoryInput.value = category;

    // Try to get current values from CSS variables
    const computedStyle = getComputedStyle(document.documentElement);
    const colorVar = `--${category}-name-color`;
    const sizeVar = `--${category}-name-font-size`;

    let currentColor = computedStyle.getPropertyValue(colorVar).trim();
    let currentSize = computedStyle.getPropertyValue(sizeVar).trim();

    if (!currentColor) currentColor = '#374151';
    if (!currentSize) currentSize = '0.93rem';

    colorInput.value = currentColor.startsWith('#') ? currentColor : '#374151';
    sizeInput.value = parseFloat(currentSize) || 0.93;

    modal.hidden = false;
    document.body.classList.add('modal-open');
  }

  function closeModal() {
    if (modal) {
      modal.hidden = true;
      document.body.classList.remove('modal-open');
    }
  }

  document.addEventListener('contextmenu', function (e) {
    const target = e.target.closest('.person-name, .event-name, .group-name');
    if (target) {
      e.preventDefault();
      currentTarget = target;
      const category = getCategory(target);

      contextMenu.style.display = 'block';
      contextMenu.style.left = e.pageX + 'px';
      contextMenu.style.top = e.pageY + 'px';

      // Ensure menu stays within viewport
      const rect = contextMenu.getBoundingClientRect();
      if (rect.right > window.innerWidth) contextMenu.style.left = (e.pageX - rect.width) + 'px';
      if (rect.bottom > window.innerHeight) contextMenu.style.top = (e.pageY - rect.height) + 'px';
    } else {
      hideContextMenu();
    }
  });

  document.addEventListener('click', function (e) {
    if (contextMenu && !contextMenu.contains(e.target)) {
      hideContextMenu();
    }
  });

  editBtn.addEventListener('click', function () {
    const category = getCategory(currentTarget);
    if (category) {
      showModal(category);
    }
    hideContextMenu();
  });

  document.querySelectorAll('[data-font-modal-close]').forEach(el => {
    el.addEventListener('click', closeModal);
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    const formData = new FormData(form);
    const category = formData.get('category');
    const color = formData.get('color');
    const fontSize = formData.get('fontSize');

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (!csrfToken || !csrfHeader) {
      console.warn('CSRF token or header not found. Request might fail.');
    }

    const params = new URLSearchParams();
    params.append('category', category);
    params.append('color', color);
    params.append('fontSize', fontSize);

    console.log(`Saving font customization for ${category}: color=${color}, fontSize=${fontSize}`);

    fetch('/settings/font-customization', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
      },
      body: params
    })
    .then(response => {
      if (response.ok) {
        console.log('Font customization saved successfully.');
        // Update CSS variables dynamically
        document.documentElement.style.setProperty(`--${category}-name-color`, color);
        document.documentElement.style.setProperty(`--${category}-name-font-size`, fontSize + 'rem');
        closeModal();
      } else {
        console.error('Failed to save font customization:', response.statusText);
        alert('Eroare la salvarea setărilor.');
      }
    })
    .catch(error => {
      console.error('Error:', error);
      alert('Eroare la salvarea setărilor.');
    });
  });

  window.addEventListener('scroll', hideContextMenu, true);
})();
