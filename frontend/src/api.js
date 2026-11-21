export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || 'Ошибка запроса (' + status + ').');
    this.status = status;
    this.code = problem?.code;
    this.fields = problem?.errors || {};
  }
}

export function query(values) {
  const params = new URLSearchParams(Object.entries(values)
    .filter(([, value]) => value !== '' && value !== null && value !== undefined));
  return params.size ? '?' + params : '';
}

export function createApi(onSession, transport = fetch) {
  let session = null;
  let refreshJob;
  let generation = 0;

  async function send(path, { method = 'GET', body, token, blob } = {}) {
    const headers = {};
    if (token) headers.Authorization = 'Bearer ' + token;
    if (body !== undefined) headers['Content-Type'] = body instanceof Blob ? 'text/csv' : 'application/json';
    const response = await transport('/api/v1' + path, {
      method, headers, body: body === undefined || body instanceof Blob ? body : JSON.stringify(body)
    });
    if (!response.ok) throw new ApiError(response.status, await response.json().catch(() => null));
    if (response.status === 204) return null;
    return blob ? response.blob() : response.json();
  }

  function replace(next) {
    session = next;
    onSession(next?.user || null);
    return next;
  }

  async function refresh() {
    if (!refreshJob) {
      const current = generation;
      const job = send('/auth/refresh', { method: 'POST', body: { refreshToken: session.refreshToken } })
        .then(next => current === generation ? replace(next) : null)
        .catch(error => {
          if (current === generation && error.status === 401) replace(null);
          throw error;
        })
        .finally(() => { if (refreshJob === job) refreshJob = undefined; });
      refreshJob = job;
    }
    return refreshJob;
  }

  async function request(path, options) {
    const token = session?.accessToken;
    const current = generation;
    try {
      return await send(path, { ...options, token });
    } catch (error) {
      if (error.status !== 401 || !session || current !== generation) throw error;
      const next = token === session.accessToken ? await refresh() : session;
      if (!next || current !== generation) throw error;
      return send(path, { ...options, token: next.accessToken });
    }
  }

  return {
    request,
    async login(body) {
      const current = ++generation;
      refreshJob = undefined;
      const next = await send('/auth/login', { method: 'POST', body });
      if (current === generation) replace(next);
    },
    async changePassword(body) {
      // Ошибка проверки текущего пароля тоже имеет статус 401.
      const current = generation;
      let next;
      try {
        next = await send('/auth/password', { method: 'POST', body, token: session?.accessToken });
      } catch (error) {
        if (error.code !== 'INVALID_TOKEN' || !session || current !== generation) throw error;
        const refreshed = await refresh();
        if (!refreshed || current !== generation) throw error;
        next = await send('/auth/password', { method: 'POST', body, token: refreshed.accessToken });
      }
      if (current === generation) {
        ++generation;
        refreshJob = undefined;
        replace(next);
      }
    },
    async logout() {
      const pending = refreshJob;
      const previous = session;
      ++generation;
      refreshJob = undefined;
      replace(null);
      if (pending) await pending.catch(() => null);
      if (previous) await send('/auth/logout', { method: 'POST', body: { refreshToken: previous.refreshToken } });
    }
  };
}
