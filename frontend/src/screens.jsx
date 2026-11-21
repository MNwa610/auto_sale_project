import { useState } from 'react';
import { query } from './api.js';
import { Field, ErrorBox, Loading, Pager, roles, date, money, useAction, useLoad, values } from './ui.jsx';

export function Analytics({ api }) {
  const [archived, setArchived] = useState(false);
  const report = useLoad(api, '/analytics' + query({ archived }));
  const totals = report.data?.totals;
  return <>
    <div className="title-row"><h1>Аналитика</h1>
      <Field label="Раздел" value={String(archived)} onChange={event => setArchived(event.target.value === 'true')}>
        <option value="false">Активные</option><option value="true">Архив</option>
      </Field>
    </div>
    <Loading {...report} />
    {totals && <>
      <dl className="metric-grid">{Object.entries({
        'Всего автомобилей': totals.count, 'В продаже': totals.available, 'В резерве': totals.reserved,
        'Продано': totals.sold, 'Проверено': totals.certified, 'Сумма цен': money(totals.totalPrice),
        'Средняя цена': totals.averagePrice === null ? '—' : money(totals.averagePrice)
      }).map(([label, value]) => <div className="panel" key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
      <section className="panel"><h2>Автомобили по маркам</h2>
        {!report.data.brands.length && <p>В этом разделе пока нет автомобилей.</p>}
        {report.data.brands.map(brand => <div className="bar-row" key={brand.id}>
          <span>{brand.name}</span><meter aria-label={brand.name} value={brand.count} max={totals.count} /><strong>{brand.count}</strong>
        </div>)}
      </section>
    </>}
  </>;
}

const actions = ['CREATED', 'UPDATED', 'ARCHIVED', 'RESTORED', 'DELETED', 'IMPORTED', 'BOOTSTRAP_ADMIN_CREATED',
  'PASSWORD_CHANGED', 'LOGIN_SUCCEEDED', 'REFRESH_ROTATED', 'LOGOUT', 'REFRESH_REUSE_DETECTED',
  'TEMPORARY_PASSWORD_SET', 'SESSIONS_REVOKED'];
const targets = ['USER', 'CAR', 'CAR_NOTE', 'REFRESH_SESSION', 'BRAND', 'CAR_MODEL', 'BODY_TYPE', 'TRANSMISSION', 'FUEL_TYPE'];

export function Audit({ api }) {
  const [filters, setFilters] = useState({});
  const result = useLoad(api, '/audit' + query(filters));
  return <>
    <h1>Журнал аудита</h1>
    <form className="panel" onSubmit={event => {
      event.preventDefault();
      const form = values(event);
      for (const key of ['from', 'to']) if (form[key]) form[key] = new Date(form[key]).toISOString();
      setFilters(form);
    }}>
      <div className="fields">
        <Field label="Логин автора" name="actorLogin" maxLength={50} />
        <Field label="Действие" name="action" options={actions}><option value="">Все</option></Field>
        <Field label="Тип объекта" name="entityType" options={targets}><option value="">Все</option></Field>
        <Field label="ID объекта" name="entityId" maxLength={64} />
        <Field label="Начало периода" type="datetime-local" name="from" />
        <Field label="Конец периода" type="datetime-local" name="to" />
        <Field label="Сортировка" name="sort">
          <option value="createdAt,desc">Сначала новые</option><option value="createdAt,asc">Сначала старые</option>
          <option value="action,asc">Действие А–Я</option><option value="action,desc">Действие Я–А</option>
        </Field>
      </div>
      <button className="primary">Применить фильтры</button>
    </form>
    <Loading {...result} />
    {result.data && <section className="panel">
      <div className="table-scroll"><table><thead><tr><th>Время</th><th>Автор</th><th>Действие</th><th>Объект</th><th>Детали</th></tr></thead>
        <tbody>{result.data.content.map(event => <tr key={event.id}>
          <td>{date(event.createdAt)}</td><td>{event.actorLogin || (event.actorId ? '#' + event.actorId : 'Система')}</td>
          <td>{event.action}</td><td>{event.entityType}<small>{event.entityId}</small></td>
          <td>{Object.keys(event.details || {}).length > 0 && <details><summary>Посмотреть</summary>
            <pre>{JSON.stringify(event.details, null, 2)}</pre></details>}</td>
        </tr>)}</tbody>
      </table></div>
      {result.data.content.length === 0 && <p>События по этим фильтрам не найдены.</p>}
      <Pager data={result.data} onPage={page => setFilters({ ...filters, page })} />
    </section>}
  </>;
}

export function Users({ api, user }) {
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const [notice, setNotice] = useState('');
  const action = useAction();
  const base = '/admin/users';
  const result = useLoad(api, base + query({ page }), revision);
  const reload = () => { action.clear(); setEditor(null); setRevision(value => value + 1); };
  function edit(mode, account = {}) {
    action.clear();
    setNotice('');
    setEditor({ ...account, mode });
  }
  return <>
    <div className="title-row"><h1>Пользователи</h1><button className="primary" onClick={() => edit('create')}>Создать пользователя</button></div>
    <ErrorBox error={action.error} reload={reload} />
    {notice && <p role="status">{notice}</p>}
    {editor && <form className="panel" key={editor.mode + editor.id} onSubmit={event => {
      event.preventDefault();
      const body = values(event);
      action.run(async () => {
        if (editor.mode === 'create') {
          await api.request(base, { method: 'POST', body });
          setNotice('Пользователь создан.');
        } else {
          body.version = editor.version;
          if (editor.mode === 'update') body.enabled = body.enabled === 'true';
          await api.request(base + '/' + editor.id + (editor.mode === 'reset' ? '/temporary-password' : ''),
            { method: editor.mode === 'reset' ? 'POST' : 'PUT', body });
          setNotice(editor.mode === 'reset' ? 'Временный пароль установлен. Сессии отозваны.' : 'Настройки сохранены.');
        }
        reload();
      });
    }}>
      <h2>{editor.mode === 'create' ? 'Новый пользователь' : editor.fullName}</h2>
      <div className="fields">
        {editor.mode === 'create' && <>
          <Field label="Логин" name="login" required maxLength={50} />
          <Field label="Имя" name="fullName" required maxLength={100} />
        </>}
        {editor.mode !== 'reset' && <Field label="Роль" name="role" options={roles} defaultValue={editor.role} />}
        {editor.mode === 'update' && <Field label="Доступ" name="enabled" defaultValue={String(editor.enabled)}>
          <option value="true">Разрешён</option><option value="false">Заблокирован</option>
        </Field>}
        {editor.mode !== 'update' && <Field label="Временный пароль" name="temporaryPassword"
          type="password" autoComplete="new-password" minLength={12} required />}
      </div>
      <div className="row"><button className="primary" disabled={action.busy}>Сохранить</button>
        <button type="button" onClick={() => setEditor(null)}>Отмена</button></div>
    </form>}
    <Loading {...result} />
    {result.data && <section className="panel"><div className="table-scroll"><table>
      <thead><tr><th>Имя</th><th>Логин</th><th>Роль</th><th>Доступ</th><th>Действия</th></tr></thead>
      <tbody>{result.data.content.map(account => <tr key={account.id}>
        <td>{account.fullName}{account.id === user.id && <small>Ваша учётная запись</small>}</td>
        <td>{account.login || '—'}</td><td>{account.role}</td>
        <td>{account.enabled ? 'Разрешён' : 'Заблокирован'}{account.mustChangePassword && <small>Требуется смена пароля</small>}</td>
        <td>{account.id !== user.id && <div className="row">
          <button onClick={() => edit('update', account)}>Роль и доступ</button>
          {account.login && <button onClick={() => edit('reset', account)}>Временный пароль</button>}
          <button disabled={action.busy} onClick={() => action.run(async () => {
            const count = await api.request(base + '/' + account.id + '/revoke-sessions', { method: 'POST' });
            setNotice('Отозвано сессий — ' + count);
          })}>Отозвать сессии</button>
        </div>}</td>
      </tr>)}</tbody>
    </table></div><Pager data={result.data} onPage={setPage} /></section>}
  </>;
}

const kinds = { brands: 'Марки', models: 'Модели', 'body-types': 'Типы кузова',
  transmissions: 'Коробки передач', 'fuel-types': 'Виды топлива' };

export function Catalogs({ api }) {
  const [kind, setKind] = useState('brands');
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const action = useAction();
  const base = '/catalogs/' + kind;
  const items = useLoad(api, base, revision);
  const brands = useLoad(api, kind === 'models' ? '/catalogs/brands' : null, revision);
  const reload = () => { action.clear(); setEditor(null); setRevision(value => value + 1); };
  return <>
    <div className="title-row"><h1>Справочники</h1><button className="primary" onClick={() => setEditor({})}>Добавить запись</button></div>
    <Field label="Справочник" value={kind} onChange={event => { setKind(event.target.value); setEditor(null); action.clear(); }}>
      {Object.entries(kinds).map(([id, name]) => <option key={id} value={id}>{name}</option>)}
    </Field>
    <ErrorBox error={action.error} reload={reload} />
    {editor && <form className="panel" key={editor.id ?? 'new'} onSubmit={event => {
      event.preventDefault();
      const body = values(event);
      body.enabled = body.enabled === 'on';
      if (editor.id) body.version = editor.version;
      action.run(async () => {
        await api.request(base + (editor.id ? '/' + editor.id : ''), { method: editor.id ? 'PUT' : 'POST', body });
        reload();
      });
    }}>
      <h2>{editor.id ? 'Редактировать запись' : 'Новая запись'}</h2>
      <div className="fields">
        <Field label="Название" name="name" defaultValue={editor.name} required
          maxLength={kind === 'models' ? 100 : kind === 'brands' ? 50 : 30} />
        {kind === 'models' && <Field label="Марка" name="brandId" defaultValue={editor.brandId || ''}
          key={brands.data ? 'ready' : 'loading'} options={[]} required>
          <option value="" disabled>{brands.error ? 'Справочник недоступен' : 'Выберите марку'}</option>
          {brands.data?.filter(item => item.enabled || item.id === editor.brandId).map(item =>
            <option key={item.id} value={item.id}>{item.name}</option>)}
        </Field>}
      </div>
      <label className="check"><input name="enabled" type="checkbox" defaultChecked={editor.enabled ?? true} /> Активна</label>
      <div className="row"><button className="primary" disabled={action.busy}>Сохранить</button>
        <button type="button" onClick={() => setEditor(null)}>Отмена</button></div>
    </form>}
    <Loading {...items} />
    {items.data && <section className="panel"><div className="table-scroll"><table>
      <thead><tr><th>ID</th><th>Название</th>{kind === 'models' && <th>Марка</th>}<th>Состояние</th><th>Действия</th></tr></thead>
      <tbody>{items.data.map(item => <tr key={item.id}>
        <td>{item.id}</td><td>{item.name}</td>
        {kind === 'models' && <td>{brands.data?.find(brand => brand.id === item.brandId)?.name || item.brandId}</td>}
        <td>{item.effectiveEnabled ? 'Активна' : item.enabled ? 'Марка отключена' : 'Отключена'}</td>
        <td className="row"><button onClick={() => setEditor(item)}>Изменить</button>
          <button disabled={action.busy} onClick={() => action.run(async () => {
            await api.request(base + '/' + item.id + '/enabled', { method: 'PATCH',
              body: { enabled: !item.enabled, version: item.version } });
            reload();
          })}>{item.enabled ? 'Отключить' : 'Включить'}</button></td>
      </tr>)}</tbody>
    </table></div>{items.data.length === 0 && <p>В справочнике пока нет записей.</p>}</section>}
  </>;
}

export function Csv({ api }) {
  const [preview, setPreview] = useState(null);
  const [imported, setImported] = useState(false);
  const action = useAction();
  function reset() { setPreview(null); setImported(false); action.clear(); }
  return <>
    <h1>Импорт CSV</h1>
    <form className="panel" onChange={reset} onSubmit={event => {
      event.preventDefault();
      const { mode, file } = values(event);
      reset();
      action.run(async () => setPreview(await api.request('/cars/csv/preview' + query({ mode }),
        { method: 'POST', body: file })));
    }}>
      <div className="fields">
        <Field label="CSV-файл (UTF-8)" type="file" name="file" accept=".csv,text/csv" required disabled={action.busy} />
        <Field label="Режим" name="mode" disabled={action.busy}>
          <option value="NEW_ONLY">Только новые</option><option value="UPDATE_BY_EXTERNAL_CODE">Обновить по внешнему коду</option>
        </Field>
      </div>
      <button className="primary" disabled={action.busy}>Проверить файл</button>
    </form>
    <ErrorBox error={action.error} />
    {imported && <p role="status">Импорт завершён. <a href="#cars">Открыть автомобили</a></p>}
    {preview && <section className="panel">
      <div className="title-row"><div><h2>Результат проверки</h2>
        <p>Корректных строк — {preview.validRows} · С ошибками — {preview.invalidRows}</p>
        <p className="muted">Проверка действительна до {date(preview.expiresAt)}</p></div>
        <button className="primary" disabled={action.busy || preview.invalidRows > 0 || Date.now() >= Date.parse(preview.expiresAt)}
          onClick={() => action.run(async () => {
            await api.request('/cars/csv/' + preview.id + '/apply', { method: 'POST' });
            setPreview(null);
            setImported(true);
          })}>Применить импорт</button></div>
      <div className="table-scroll"><table><thead><tr><th>Строка</th><th>Внешний код</th><th>Результат</th></tr></thead>
        <tbody>{preview.rows.map(row => <tr key={row.line}><td>{row.line}</td><td>{row.data?.externalCode || '—'}</td>
          <td>{row.error || (row.carId ? 'Обновление' : 'Создание')}</td></tr>)}</tbody>
      </table></div>
    </section>}
  </>;
}
