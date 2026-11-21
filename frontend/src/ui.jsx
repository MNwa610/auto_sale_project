import { useEffect, useId, useState } from 'react';

export const roles = ['VIEWER', 'MANAGER', 'ADMIN'];
export const statuses = ['AVAILABLE', 'RESERVED', 'SOLD'];
export const statusLabel = { AVAILABLE: 'В продаже', RESERVED: 'В резерве', SOLD: 'Продан' };
export const money = value => new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB' }).format(value);
export const date = value => value ? new Date(value).toLocaleString('ru-RU') : '—';
export const values = event => Object.fromEntries(new FormData(event.currentTarget));

export function Field({ label, options, children, ...props }) {
  const id = useId();
  return <div className="field"><label htmlFor={id}>{label}</label>
    {options || children
      ? <select id={id} {...props}>{children}{options?.map(value => <option key={value} value={value}>{statusLabel[value] || value}</option>)}</select>
      : <input id={id} {...props} />}
  </div>;
}

export function ErrorBox({ error, reload }) {
  if (!error) return null;
  return <div role="alert" className="error">{error.message}
    {Object.entries(error.fields || {}).map(([field, messages]) => <div key={field}>{field} — {messages.join(', ')}</div>)}
    {error.status === 409 && reload && <button type="button" onClick={reload}>Загрузить актуальные данные</button>}
  </div>;
}

export function Pager({ data, onPage }) {
  if (!data || data.totalPages <= 1) return null;
  return <div className="row pager">
    <button disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>Назад</button>
    <span>Страница {data.page + 1} из {data.totalPages}</span>
    <button disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)}>Далее</button>
  </div>;
}

export function useLoad(api, path, revision = 0) {
  const [result, setResult] = useState({});
  useEffect(() => {
    if (!path) return;
    let active = true;
    setResult({ path, revision });
    api.request(path).then(
      data => { if (active) setResult({ data, path, revision }); },
      error => { if (active) setResult({ error, path, revision }); }
    );
    return () => { active = false; };
  }, [api, path, revision]);
  return result.path === path && result.revision === revision ? result : {};
}

export function useAction() {
  const [error, setError] = useState();
  const [busy, setBusy] = useState(false);
  async function run(action) {
    setError(undefined);
    setBusy(true);
    try { await action(); } catch (failure) { setError(failure); } finally { setBusy(false); }
  }
  return { error, busy, run, clear: () => setError(undefined) };
}

export function Loading({ data, error }) {
  return error ? <ErrorBox error={error} /> : !data ? <p role="status">Загрузка…</p> : null;
}
