(function () {
  const form = document.querySelector('[data-church-form]');
  if (!form) {
    return;
  }

  const fileInput = form.querySelector('[data-avatar-file]');
  const uploadButton = form.querySelector('[data-avatar-upload]');
  const previewWrap = form.querySelector('[data-avatar-preview-wrap]');
  const clearButton = form.querySelector('[data-avatar-clear]');
  const urlInput = form.querySelector('[data-avatar-url]');
  const preview = form.querySelector('[data-avatar-preview]');
  const placeholder = form.querySelector('[data-avatar-placeholder]');
  const errorNode = form.querySelector('[data-avatar-error]');

  if (!fileInput || !uploadButton || !previewWrap || !clearButton || !urlInput || !preview || !placeholder || !errorNode) {
    return;
  }

  const allowedFileTypes = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];
  const urlPattern = /^https?:\/\/\S+$/i;
  const localUploadPattern = /^\/uploads\/church\/\S+$/i;
  const legacyDataImagePattern = /^data:image\/(png|jpeg|jpg|webp|gif);base64,[a-z0-9+/=\s]+$/i;
  const hasValidAvatarValue = (value) => {
    if (!value) return true;
    return urlPattern.test(value) || localUploadPattern.test(value) || legacyDataImagePattern.test(value);
  };

  const syncPreview = () => {
    const value = (urlInput.value || '').trim();
    if (value) {
      preview.src = value;
      preview.classList.remove('is-hidden');
      placeholder.classList.add('is-hidden');
      clearButton.disabled = false;
      previewWrap.classList.add('has-image');
    } else {
      preview.removeAttribute('src');
      preview.classList.add('is-hidden');
      placeholder.classList.remove('is-hidden');
      clearButton.disabled = true;
      previewWrap.classList.remove('has-image');
    }
  };

  const setError = (message) => {
    errorNode.textContent = message || '';
  };

  preview.addEventListener('error', () => {
    const value = (urlInput.value || '').trim();
    if (!value) return;
    preview.classList.add('is-hidden');
    placeholder.classList.remove('is-hidden');
    setError('Imaginea nu se poate încărca. Verifică URL-ul sau alege alt fișier.');
  });

  preview.addEventListener('load', () => {
    if ((urlInput.value || '').trim()) {
      setError('');
      preview.classList.remove('is-hidden');
      placeholder.classList.add('is-hidden');
    }
  });

  uploadButton.addEventListener('click', () => fileInput.click());

  clearButton.addEventListener('click', () => {
    urlInput.value = '';
    fileInput.value = '';
    setError('');
    syncPreview();
  });

  urlInput.addEventListener('input', () => {
    const value = (urlInput.value || '').trim();
    setError(hasValidAvatarValue(value) ? '' : 'Folosește un URL http(s), o cale /uploads/church sau reîncarcă imaginea locală.');
    syncPreview();
  });

  fileInput.addEventListener('change', () => {
    const file = fileInput.files && fileInput.files[0];
    if (!file) {
      return;
    }

    if (!allowedFileTypes.includes(file.type)) {
      setError('Format invalid. Folosește PNG, JPG, WEBP sau GIF.');
      fileInput.value = '';
      return;
    }

    if (file.size > 1024 * 1024) {
      setError('Imaginea este prea mare. Maxim 1 MB.');
      fileInput.value = '';
      return;
    }

    const uploadUrl = form.getAttribute('data-avatar-upload-url') || '/church/avatar-upload';
    const data = new FormData();
    data.append('file', file);

    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const fetchHeaders = { 'Accept': 'application/json' };
    if (csrfHeader && csrfToken) fetchHeaders[csrfHeader] = csrfToken;

    uploadButton.disabled = true;
    setError('Se încarcă imaginea...');

    fetch(uploadUrl, {
      method: 'POST',
      body: data,
      headers: fetchHeaders
    })
      .then(async (response) => {
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(body.error || 'Nu am putut salva imaginea.');
        }
        if (!body.avatarUrl) {
          throw new Error('Răspuns invalid de la server.');
        }
        urlInput.value = body.avatarUrl;
        setError('');
        syncPreview();
      })
      .catch((error) => {
        setError(error && error.message ? error.message : 'Nu am putut salva imaginea.');
      })
      .finally(() => {
        uploadButton.disabled = false;
        fileInput.value = '';
      });
  });

  form.addEventListener('submit', (event) => {
    const value = (urlInput.value || '').trim();
    urlInput.value = value;
    if (!hasValidAvatarValue(value)) {
      event.preventDefault();
      setError('Folosește un URL http(s), o cale /uploads/church sau reîncarcă imaginea locală.');
      urlInput.focus();
    }
  });

  syncPreview();
})();
