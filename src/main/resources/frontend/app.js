// Constants

const PRIMITIVES = new Set([
  'string', 'integer', 'long', 'int', 'double', 'float',
  'boolean', 'bool', 'byte', 'char', 'short', 'void',
  'object', 'date', 'localdatetime', 'localdate', 'number',
  'array', 'map',
  'String', 'Integer', 'Long', 'Double', 'Float', 'Boolean',
  'Byte', 'Char', 'Short', 'Void', 'Object', 'Date',
]);

function isPrimitive(type) {
  return PRIMITIVES.has(type) || /^(int|long|double|float|boolean|string|void)$/i.test(type);
}

function methodColor(m) {
  const map = { GET: 'get', POST: 'post', PUT: 'put', DELETE: 'delete', PATCH: 'patch' };
  return map[m.toUpperCase()] || 'get';
}

function statusClass(code) {
  const n = parseInt(code);
  if (n >= 200 && n < 300) return 'status-2xx';
  if (n >= 400) return 'status-4xx';
  return '';
}

function escapeHtml(s) {
  return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
}

function buildSchemaPreview(modelName, models, depth = 0, visited = new Set()) {
  if (depth > 4 || visited.has(modelName)) return `"${modelName}"`;
  visited = new Set(visited);
  visited.add(modelName);

  const model = models[modelName];
  if (!model) return `"${modelName}"`;

  const indent = '  '.repeat(depth);
  const innerIndent = '  '.repeat(depth + 1);

  const fields = (model.fields || []).map(f => {
    const val = isPrimitive(f.type)
        ? `"<${f.type}>"`
        : buildSchemaPreview(f.type, models, depth + 1, visited);
    return `${innerIndent}"${f.name}": ${val}`;
  });

  return `{\n${fields.join(',\n')}\n${indent}}`;
}

function buildBodyPlaceholder(modelName, models, depth = 0, visited = new Set()) {
  if (depth > 4 || visited.has(modelName)) return '{}';
  visited = new Set(visited);
  visited.add(modelName);

  const model = models[modelName];
  if (!model) return '{}';

  const obj = {};
  (model.fields || []).forEach(f => {
    if (isPrimitive(f.type)) {
      const t = f.type.toLowerCase();
      if (['integer', 'long', 'int', 'double', 'float', 'short', 'byte', 'number'].includes(t)) {
        obj[f.name] = 0;
      } else if (['boolean', 'bool'].includes(t)) {
        obj[f.name] = true;
      } else {
        obj[f.name] = '';
      }
    } else {
      obj[f.name] = JSON.parse(buildBodyPlaceholder(f.type, models, depth + 1, visited));
    }
  });

  return JSON.stringify(obj, null, 2);
}

function groupByTag(paths) {
  const groups = {};
  Object.entries(paths).forEach(([path, methods]) => {
    const parts = path.split('/').filter(p => p.length > 0);
    let tag = parts.find(p => p !== 'api' && !p.startsWith('{')) || 'default';

    if (!groups[tag]) groups[tag] = [];
    Object.entries(methods).forEach(([method, op]) => {
      groups[tag].push({ path, method: method.toUpperCase(), op });
    });
  });
  return groups;
}

function renderEndpoints(data, filter = '') {
  const container = document.getElementById('endpoints-container');
  container.innerHTML = '';
  const groups = groupByTag(data.paths);

  Object.entries(groups).forEach(([tag, operations]) => {
    const filtered = filter
        ? operations.filter(o => o.path.toLowerCase().includes(filter.toLowerCase()))
        : operations;
    if (filtered.length === 0) return;

    const section = document.createElement('div');
    section.className = 'tag-section';

    const header = document.createElement('div');
    header.className = 'tag-header';
    header.innerHTML = `
      <h3>${escapeHtml(tag)}</h3>
      <svg class="arrow" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
      </svg>`;

    const body = document.createElement('div');
    body.className = 'tag-body';

    header.addEventListener('click', () => {
      header.classList.toggle('collapsed');
      body.style.display = header.classList.contains('collapsed') ? 'none' : '';
    });

    filtered.forEach(({ path, method, op }) => {
      body.appendChild(renderOperation(path, method, op, data.models || {}));
    });

    section.appendChild(header);
    section.appendChild(body);
    container.appendChild(section);
  });
}

function renderOperation(path, method, op, models) {
  const wrap = document.createElement('div');
  wrap.className = 'operation';

  const summary = document.createElement('div');
  summary.className = 'op-summary';
  summary.innerHTML = `
    <span class="method-badge method-${methodColor(method)}">${method}</span>
    <span class="op-path">${escapeHtml(path)}</span>
    <span class="op-desc">${escapeHtml(op.description || '')}</span>
    <span class="op-expand-icon">&#8964;</span>`;

  const detail = document.createElement('div');
  detail.className = 'op-detail';
  detail.appendChild(buildDetail(path, method, op, models));

  summary.addEventListener('click', () => {
    const isOpen = detail.classList.toggle('open');
    summary.classList.toggle('open', isOpen);
  });

  wrap.appendChild(summary);
  wrap.appendChild(detail);
  return wrap;
}

function buildDetail(path, method, op, models) {
  const inner = document.createElement('div');
  inner.className = 'op-detail-inner';

  const tryBtn = document.createElement('button');
  tryBtn.className = 'try-btn';
  tryBtn.textContent = 'Try it out';

  const cancelBtn = document.createElement('button');
  cancelBtn.className = 'cancel-btn';
  cancelBtn.textContent = 'Cancel';
  cancelBtn.style.display = 'none';

  const btnRow = document.createElement('div');
  btnRow.appendChild(tryBtn);
  btnRow.appendChild(cancelBtn);
  inner.appendChild(btnRow);

  const params = op.parameters || [];
  const inputMap = {};

  if (params.length > 0) {
    const sectionTitle = document.createElement('div');
    sectionTitle.className = 'section-title';
    sectionTitle.textContent = 'Parameters';
    inner.appendChild(sectionTitle);

    const table = document.createElement('table');
    table.className = 'params-table';
    table.innerHTML = '<thead><tr><th>In</th><th>Type</th><th>Required</th><th>Default</th><th>Value</th></tr></thead>';
    const tbody = document.createElement('tbody');

    params.forEach(p => {
      const input = document.createElement('textarea');
      input.className = 'param-input';
      input.rows = 1;
      input.placeholder = p.defaultValue != null ? String(p.defaultValue) : (p.type || '');
      if (p.defaultValue != null) input.value = String(p.defaultValue);
      input.disabled = true;

      const key = p.name || `param-${Math.random()}`;
      inputMap[key] = input;

      const valCell = document.createElement('td');
      valCell.appendChild(input);

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><span class="param-in">${escapeHtml(p.in)}</span></td>
        <td>
          <span class="param-type">${escapeHtml(p.type)}</span>
          ${p.required ? '<span class="required-star">*</span>' : ''}
        </td>
        <td>${p.required ? 'required' : 'optional'}</td>
        <td><span class="param-default">${p.defaultValue != null ? escapeHtml(String(p.defaultValue)) : '–'}</span></td>`;
      tr.appendChild(valCell);
      tbody.appendChild(tr);
    });

    table.appendChild(tbody);
    inner.appendChild(table);
  }

  let bodyTextarea = null;
  if (op.requestBody) {
    const sectionTitle = document.createElement('div');
    sectionTitle.className = 'section-title';
    sectionTitle.textContent = 'Request body';
    inner.appendChild(sectionTitle);

    const mediaNote = document.createElement('div');
    mediaNote.style.cssText = 'font-size:12px;color:#888;margin-bottom:6px;';
    mediaNote.textContent = `Content type: application/json  •  Schema: ${op.requestBody}`;
    inner.appendChild(mediaNote);

    bodyTextarea = document.createElement('textarea');
    bodyTextarea.className = 'body-textarea';
    bodyTextarea.disabled = true;
    bodyTextarea.placeholder = buildBodyPlaceholder(op.requestBody, models);
    inner.appendChild(bodyTextarea);
  }

  const executeBtn = document.createElement('button');
  executeBtn.className = 'execute-btn';
  executeBtn.textContent = 'Execute';
  executeBtn.style.display = 'none';
  inner.appendChild(executeBtn);

  const curlWrap = document.createElement('div');
  curlWrap.style.display = 'none';

  const curlTitle = document.createElement('div');
  curlTitle.className = 'section-title';
  curlTitle.textContent = 'Curl';
  curlWrap.appendChild(curlTitle);

  const curlBox = document.createElement('div');
  curlBox.className = 'curl-box';
  curlBox.innerHTML = `<button class="copy-curl-btn">Copy</button><span></span>`;
  curlBox.querySelector('.copy-curl-btn').addEventListener('click', () => {
    navigator.clipboard.writeText(curlBox.querySelector('span').textContent);
  });
  curlWrap.appendChild(curlBox);
  inner.appendChild(curlWrap);

  const liveWrap = document.createElement('div');
  liveWrap.style.display = 'none';

  const liveTitle = document.createElement('div');
  liveTitle.className = 'section-title';
  liveTitle.textContent = 'Server response';
  liveWrap.appendChild(liveTitle);

  const liveBlock = document.createElement('div');
  liveBlock.className = 'live-response';
  liveBlock.innerHTML = `
    <div class="live-response-header">
      <span>Response body</span>
      <span class="live-response-status"></span>
    </div>
    <pre class="live-response-body"></pre>`;
  liveWrap.appendChild(liveBlock);
  inner.appendChild(liveWrap);

  tryBtn.addEventListener('click', () => {
    tryBtn.classList.add('active');
    tryBtn.style.display = 'none';
    cancelBtn.style.display = '';
    executeBtn.style.display = '';

    Object.values(inputMap).forEach(inp => { inp.disabled = false; });
    if (bodyTextarea) bodyTextarea.disabled = false;
  });

  cancelBtn.addEventListener('click', () => {
    tryBtn.classList.remove('active');
    tryBtn.style.display = '';
    cancelBtn.style.display = 'none';
    executeBtn.style.display = 'none';
    curlWrap.style.display = 'none';
    liveWrap.style.display = 'none';

    Object.values(inputMap).forEach(inp => { inp.disabled = true; inp.value = ''; });
    if (bodyTextarea) { bodyTextarea.disabled = true; bodyTextarea.value = ''; }
  });

  executeBtn.addEventListener('click', () => {
    let url = path;
    const queryParts = [];

    params.forEach(p => {
      const val = (inputMap[p.name]?.value || '').trim();
      if (!val) return;
      if (p.in === 'path') {
        url = url.replace(`{${p.name}}`, encodeURIComponent(val));
      } else if (p.in === 'query') {
        queryParts.push(`${encodeURIComponent(p.name)}=${encodeURIComponent(val)}`);
      }
    });

    if (queryParts.length) url += '?' + queryParts.join('&');

    const fullUrl = getBaseUrl() + url;
    const bodyVal = bodyTextarea?.value.trim() || null;
    const hasBody = !!bodyVal;

    // Build curl string
    let curl = `curl -X ${method} \\\n  '${fullUrl}'`;
    params.forEach(p => {
      if (p.in === 'header') {
        const val = (inputMap[p.name]?.value || '').trim();
        if (val) curl += ` \\\n  -H '${p.name}: ${val.replace(/'/g, "\\'")}'`;
      }
    });
    if (hasBody) {
      curl += ` \\\n  -H 'Content-Type: application/json' \\\n  -d '${bodyVal.replace(/'/g, "\\'")}'`;
    }
    curlBox.querySelector('span').textContent = curl;
    curlWrap.style.display = '';

    // Fetch
    const fetchOpts = { method };
    const reqHeaders = {};
    if (hasBody) reqHeaders['Content-Type'] = 'application/json';

    params.forEach(p => {
      if (p.in === 'header') {
        const val = (inputMap[p.name]?.value || '').trim();
        if (val) reqHeaders[p.name] = val;
      }
    });

    if (Object.keys(reqHeaders).length) fetchOpts.headers = reqHeaders;
    if (hasBody) fetchOpts.body = bodyVal;

    liveWrap.style.display = '';
    const lrStatus = liveBlock.querySelector('.live-response-status');
    const lrBody   = liveBlock.querySelector('.live-response-body');
    lrStatus.textContent = 'Loading...';
    lrStatus.className = 'live-response-status';
    lrBody.textContent = '';

    fetch(fullUrl, fetchOpts)
        .then(async res => {
          lrStatus.textContent = `${res.status} ${res.statusText}`;
          lrStatus.className = 'live-response-status ' + (res.ok ? 'ok' : 'err');
          const text = await res.text();
          try {
            lrBody.textContent = JSON.stringify(JSON.parse(text), null, 2);
          } catch {
            lrBody.textContent = text;
          }
        })
        .catch(err => {
          lrStatus.textContent = 'Network error';
          lrStatus.className = 'live-response-status err';
          lrBody.textContent = String(err);
        });
  });

  const respTitle = document.createElement('div');
  respTitle.className = 'section-title';
  respTitle.textContent = 'Responses';
  inner.appendChild(respTitle);

  const respTable = document.createElement('table');
  respTable.className = 'response-table';
  respTable.innerHTML = '<thead><tr><th>Code</th><th>Description</th><th>Schema</th></tr></thead>';
  const rtbody = document.createElement('tbody');

  Object.entries(op.responses || {}).forEach(([code, resp]) => {
    const tr = document.createElement('tr');
    const schemaCell = document.createElement('td');
    const descCell   = document.createElement('td');

    const schemaName = typeof resp === 'string' ? resp : (resp?.schema ?? null);
    const descText   = typeof resp === 'object' && resp !== null ? (resp.description || '') : '';

    descCell.style.cssText = 'color:#555;font-size:13px;';
    descCell.textContent = descText;

    if (schemaName) {
      const schemaLink = document.createElement('span');
      schemaLink.style.cssText = 'font-family:monospace;font-size:12px;color:#4990e2;cursor:pointer;';
      schemaLink.textContent = schemaName;
      schemaLink.addEventListener('click', () => {
        const target = document.getElementById('model-' + schemaName);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth' });
          target.click();
        }
      });
      schemaCell.appendChild(schemaLink);
    } else {
      schemaCell.textContent = '-';
    }

    tr.innerHTML = `<td><span class="status-code ${statusClass(code)}">${code}</span></td>`;
    tr.appendChild(descCell);
    tr.appendChild(schemaCell);
    rtbody.appendChild(tr);
  });

  respTable.appendChild(rtbody);
  inner.appendChild(respTable);

  return inner;
}

function renderModels(models) {
  const section = document.getElementById('models-section');
  const body    = document.getElementById('models-body');
  if (!models || Object.keys(models).length === 0) return;
  section.style.display = '';
  body.innerHTML = '';

  Object.entries(models).forEach(([name, model]) => {
    const item = document.createElement('div');
    item.className = 'model-item';

    const header = document.createElement('div');
    header.className = 'model-header';
    header.id = 'model-' + name;
    header.innerHTML = `
      <span class="model-name">${escapeHtml(name)}</span>
      <svg style="width:16px;height:16px;transition:transform 0.2s" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
      </svg>`;

    const detail = document.createElement('div');
    detail.className = 'model-detail';

    const previewLabel = document.createElement('div');
    previewLabel.style.cssText = 'font-size:12px;color:#888;margin-bottom:4px;';
    previewLabel.textContent = 'Example schema:';
    detail.appendChild(previewLabel);

    const schemaBox = document.createElement('pre');
    schemaBox.className = 'schema-box';
    schemaBox.textContent = buildSchemaPreview(name, models);
    detail.appendChild(schemaBox);

    const fieldsLabel = document.createElement('div');
    fieldsLabel.style.cssText = 'font-size:12px;color:#888;margin:12px 0 4px;';
    fieldsLabel.textContent = 'Fields:';
    detail.appendChild(fieldsLabel);

    const fieldsWrap = document.createElement('div');
    fieldsWrap.className = 'model-fields';

    (model.fields || []).forEach(f => {
      const row = document.createElement('div');
      row.className = 'model-field';

      const nameSpan = document.createElement('span');
      nameSpan.className = 'field-name';
      nameSpan.textContent = f.name;

      const typeSpan = document.createElement('span');
      typeSpan.className = 'field-type' + (isPrimitive(f.type) ? ' primitive' : '');

      if (isPrimitive(f.type)) {
        typeSpan.textContent = f.type;
      } else {
        const link = document.createElement('a');
        link.textContent = f.type;
        link.addEventListener('click', () => {
          const target = document.getElementById('model-' + f.type);
          if (target) { target.scrollIntoView({ behavior: 'smooth' }); target.click(); }
        });
        typeSpan.appendChild(link);
      }

      row.appendChild(nameSpan);
      row.appendChild(typeSpan);
      fieldsWrap.appendChild(row);
    });

    detail.appendChild(fieldsWrap);

    header.addEventListener('click', () => {
      const open = detail.classList.toggle('open');
      header.querySelector('svg').style.transform = open ? '' : 'rotate(-90deg)';
    });

    item.appendChild(header);
    item.appendChild(detail);
    body.appendChild(item);
  });

  const toggle = document.getElementById('models-toggle');
  toggle.addEventListener('click', () => {
    toggle.classList.toggle('collapsed');
    body.style.display = toggle.classList.contains('collapsed') ? 'none' : '';
  });
}

document.getElementById('filter-input').addEventListener('input', e => {
  if (apiData) renderEndpoints(apiData, e.target.value);
});

let apiData = null;
let currentBaseUrl = 'http://localhost:8081';

function getBaseUrl() {
  return (document.getElementById('base-url-input')?.value || '').replace(/\/$/, '') || currentBaseUrl;
}

async function loadDocs() {
  try {
    const res = await fetch('./doc/api-docs.json');
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    apiData = data;

    const titleNode = document.getElementById('api-title');
    titleNode.childNodes[0].textContent =
        data.info?.title ? data.info.title + ' ' : 'REST API ';
    if (data.info?.version) {
      document.getElementById('api-version').textContent = data.info.version;
    }

    if (data.info?.baseUrl) {
      currentBaseUrl = data.info.baseUrl.replace(/\/$/, '');
    }
    const baseUrlInput = document.getElementById('base-url-input');
    if (baseUrlInput) baseUrlInput.value = currentBaseUrl;

    renderEndpoints(data);
    renderModels(data.models || {});

  } catch (err) {
    document.getElementById('endpoints-container').innerHTML = `
      <div class="empty-state">
        <p>⚠️ Could not load <code>api-docs.json</code>: ${escapeHtml(err.message)}</p>
        <p style="margin-top:8px;color:#bbb">
          Place your <code>api-docs.json</code> in the same directory as this HTML file.
        </p>
      </div>`;
  }
}

loadDocs();