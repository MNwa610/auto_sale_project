import { useState } from 'react';
import { query } from './api.js';
import { Field, ErrorBox, Loading, Pager, date, money, statuses, statusLabel, useAction, useLoad, values } from './ui.jsx';

const links = [
  ['seller', 'Продавец', '/cars/sellers'], ['model', 'Модель', '/catalogs/models'],
  ['bodyType', 'Кузов', '/catalogs/body-types'], ['transmission', 'Коробка передач', '/catalogs/transmissions'],
  ['fuelType', 'Топливо', '/catalogs/fuel-types']
];
const sortFields = { createdAt: 'Добавление', updatedAt: 'Обновление', price: 'Цена', year: 'Год',
  mileage: 'Пробег', externalCode: 'Внешний код' };

export function Cars({ api, user, filters, setFilters }) {
  const [revision, setRevision] = useState(0);
  const [selected, setSelected] = useState([]);
  const [results, setResults] = useState([]);
  const action = useAction();
  const canWrite = user.role !== 'VIEWER';
  const archived = filters.archived === 'true';
  const list = useLoad(api, '/cars' + query(filters), revision);
  const cars = list.data?.content || [];

  function changeFilters(next) {
    setSelected([]);
    setResults([]);
    setFilters(next);
  }
  async function bulk(operation) {
    if (operation === 'DELETE' && !window.confirm('Окончательно удалить выбранные автомобили?')) return;
    const items = cars.filter(car => selected.includes(car.id)).map(({ id, version }) => ({ id, version }));
    setResults(await api.request('/cars/bulk', { method: 'POST', body: { action: operation, items } }));
    setSelected([]);
    setFilters({ ...filters, page: 0 });
    setRevision(value => value + 1);
  }
  async function exportCsv() {
    const blob = await api.request('/cars/csv/export' + query(filters), { blob: true });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'cars.csv';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  return <>
    <div className="title-row"><h1>Автомобили</h1><div className="row">
      <button onClick={() => action.run(exportCsv)} disabled={action.busy}>Экспорт CSV</button>
      {canWrite && <a className="button primary" href="#cars/new">Добавить автомобиль</a>}
    </div></div>
    <form className="panel" onSubmit={event => {
      event.preventDefault();
      changeFilters({ ...values(event), page: 0 });
    }}>
      <div className="fields">
        <Field label="Поиск" name="q" defaultValue={filters.q} maxLength={200} placeholder="Марка, модель, VIN или код" />
        <Field label="Статус" name="status" defaultValue={filters.status || ''} options={statuses}><option value="">Все</option></Field>
        <Field label="Раздел" name="archived" defaultValue={filters.archived}>
          <option value="false">Активные</option><option value="true">Архив</option>
        </Field>
        <Field label="Сортировка" name="sort" defaultValue={filters.sort || 'createdAt,desc'}>
          {Object.entries(sortFields).flatMap(([key, title]) => ['desc', 'asc'].map(dir =>
            <option key={key + dir} value={key + ',' + dir}>{title} {dir === 'asc' ? '↑' : '↓'}</option>))}
        </Field>
      </div>
      <details>
        <summary>Дополнительные фильтры</summary>
        <div className="fields">
          <Field label="Тег" name="tag" defaultValue={filters.tag} maxLength={50} />
          <Field label="Проверка автосалоном" name="certified" defaultValue={filters.certified || ''}>
            <option value="">Все</option><option value="true">Пройдена</option><option value="false">Не пройдена</option>
          </Field>
          {[['brand', 'Марка'], ...links].map(([key, label]) => <Field key={key} label={label + ' (ID)'}
            name={key + 'Id'} type="number" min="1" defaultValue={filters[key + 'Id']} />)}
          {Object.entries({ year: 'Год', mileage: 'Пробег', price: 'Цена', age: 'Возраст', engineVolume: 'Объём двигателя' })
            .flatMap(([key, label]) => ['Min', 'Max'].map(bound => <Field key={key + bound}
              label={label + (bound === 'Min' ? ' от' : ' до')} name={key + bound} type="number"
              min="0" step={['price', 'engineVolume'].includes(key) ? 'any' : '1'} defaultValue={filters[key + bound]} />))}
        </div>
      </details>
      <div className="row">
        <button className="primary">Применить фильтры</button>
        <button type="reset" onClick={event => {
          const form = event.currentTarget.form;
          event.preventDefault();
          for (const control of form.elements) if (control.name) control.value = ({
            archived: 'false', sort: 'createdAt,desc'
          })[control.name] || '';
          changeFilters({ archived: 'false' });
        }}>Сбросить</button>
      </div>
    </form>
    <ErrorBox error={action.error} />
    {selected.length > 0 && <div className="row panel">
      <span>Выбрано {selected.length}</span>
      <button disabled={action.busy} onClick={() => action.run(() => bulk(archived ? 'RESTORE' : 'ARCHIVE'))}>
        {archived ? 'Восстановить' : 'В архив'}
      </button>
      {archived && user.role === 'ADMIN' && <button className="danger" disabled={action.busy}
        onClick={() => action.run(() => bulk('DELETE'))}>Удалить</button>}
    </div>}
    {results.length > 0 && <details className="panel" open>
      <summary>Результаты массовой операции</summary>
      <ul>{results.map(result => <li key={result.id}>{result.car?.externalCode || result.id} — {
        result.status < 300 ? 'Готово' : result.code || result.status}</li>)}</ul>
    </details>}
    <Loading {...list} />
    {list.data && <section className="panel">
      <p className="muted">Найдено {list.data.totalElements}</p>
      <div className="table-scroll"><table>
        <thead><tr>{canWrite && <th><input aria-label="Выбрать все на странице" type="checkbox"
          checked={cars.length > 0 && selected.length === cars.length}
          onChange={event => setSelected(event.target.checked ? cars.map(car => car.id) : [])} /></th>}
          <th>Автомобиль</th><th>Год</th><th>Пробег</th><th>Цена</th><th>Статус</th></tr></thead>
        <tbody>{cars.map(car => <tr key={car.id}>
          {canWrite && <td><input aria-label={'Выбрать ' + car.externalCode} type="checkbox" checked={selected.includes(car.id)}
            onChange={event => setSelected(event.target.checked ? [...selected, car.id] : selected.filter(id => id !== car.id))} /></td>}
          <td><a href={'#cars/' + car.id}>{car.brand.name} {car.model.name}</a><small>{car.externalCode}</small></td>
          <td>{car.year}</td><td>{car.mileage.toLocaleString('ru-RU')} км</td><td>{money(car.price)}</td>
          <td><span className="status" data-status={car.status}>{statusLabel[car.status]}</span></td>
        </tr>)}</tbody>
      </table></div>
      {!cars.length && <p>Автомобили по этим фильтрам не найдены.</p>}
      <Pager data={list.data} onPage={page => changeFilters({ ...filters, page })} />
    </section>}
  </>;
}

function ReferenceField({ api, link: [name, label, endpoint], current }) {
  const { data, error } = useLoad(api, endpoint);
  const enabled = (data || []).filter(item => item.effectiveEnabled !== false || item.id === current?.id);
  if (current && !enabled.some(item => item.id === current.id)) enabled.unshift(current);
  return <Field label={label} name={name + 'Id'} defaultValue={current?.id || ''} required
    key={data ? 'ready' : 'loading'} options={[]}>
    <option value="" disabled>{error ? 'Справочник недоступен' : data ? 'Выберите значение' : 'Загрузка…'}</option>
    {enabled.map(item => <option key={item.id} value={item.id}>{item.name || item.fullName} · {item.id}</option>)}
  </Field>;
}

function CarForm({ api, car, busy, onSave, onCancel }) {
  return <form className="panel" onSubmit={event => {
    event.preventDefault();
    const body = values(event);
    body.certified = body.certified === 'on';
    body.tags = body.tags.split('\n').map(tag => tag.trim()).filter(Boolean);
    if (car) body.version = car.version;
    onSave(body);
  }}>
    <div className="fields">
      <Field label="Внешний код" name="externalCode" defaultValue={car?.externalCode} required maxLength={64} />
      {links.map(link => <ReferenceField key={link[0]} api={api} link={link} current={car?.[link[0]]} />)}
      {[['year', 'Год выпуска', 1950, 2100, 1], ['mileage', 'Пробег, км', 0, undefined, 1],
        ['price', 'Цена, ₽', 0.01, undefined, 0.01], ['engineVolume', 'Объём двигателя, л', 0.1, 99.9, 0.1]]
        .map(([name, label, min, max, step]) => <Field key={name} {...{ name, label, min, max, step }}
          type="number" defaultValue={car?.[name]} required />)}
      <Field label="VIN" name="vin" defaultValue={car?.vin} maxLength={40} />
      <Field label="Статус" name="status" options={statuses} defaultValue={car?.status} />
    </div>
    <label className="field">Описание<textarea name="description" rows={3} maxLength={4000} defaultValue={car?.description} /></label>
    <label className="field">Теги, по одному на строку<textarea name="tags" rows={3} defaultValue={car?.tags.join('\n')} /></label>
    <label className="check"><input name="certified" type="checkbox" defaultChecked={car?.certified} /> Проверен автосалоном</label>
    <div className="row"><button className="primary" disabled={busy}>Сохранить</button>
      <button type="button" onClick={onCancel}>Отмена</button></div>
  </form>;
}

export function CarCard({ api, user, id }) {
  const existing = id !== 'new';
  const [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState(!existing);
  const action = useAction();
  const resource = '/cars/' + id;
  const list = useLoad(api, existing ? resource : null, revision);
  const car = list.data;
  const canWrite = user.role !== 'VIEWER';
  const reload = () => { action.clear(); setEditing(false); setRevision(value => value + 1); };
  if (!existing && !canWrite) return <p role="alert">Недостаточно прав для добавления автомобиля.</p>;
  return <>
    <a className="back" href="#cars">← К автомобилям</a>
    {existing && <Loading {...list} />}
    {(!existing || car) && <div className="title-row">
      <div><h1>{car ? car.brand.name + ' ' + car.model.name : 'Новый автомобиль'}</h1>
        {car && <p className="muted">{car.externalCode} · {statusLabel[car.status]}{car.archived ? ' · Архив' : ''}</p>}</div>
      {car && canWrite && !editing && <div className="row">
        {!car.archived && <button onClick={() => setEditing(true)}>Редактировать</button>}
        <button disabled={action.busy} onClick={() => action.run(async () => {
          await api.request(resource + (car.archived ? '/restore' : '/archive'),
            { method: 'POST', body: { version: car.version } });
          reload();
        })}>{car.archived ? 'Восстановить' : 'В архив'}</button>
        {car.archived && user.role === 'ADMIN' && <button className="danger" disabled={action.busy}
          onClick={() => window.confirm('Окончательно удалить автомобиль?') && action.run(async () => {
            await api.request(resource + query({ version: car.version }), { method: 'DELETE' });
            window.location.hash = 'cars';
          })}>Удалить</button>}
      </div>}
    </div>}
    <ErrorBox error={action.error} reload={existing ? reload : undefined} />
    {editing && (!existing || car) && <CarForm api={api} car={car} busy={action.busy}
      onCancel={() => existing ? setEditing(false) : (window.location.hash = 'cars')}
      onSave={body => action.run(async () => {
        const saved = await api.request(existing ? resource : '/cars', { method: existing ? 'PUT' : 'POST', body });
        if (existing) reload(); else window.location.hash = 'cars/' + saved.id;
      })} />}
    {car && !editing && <>
      <section className="panel">
        <dl className="fields facts">{Object.entries({
          Цена: money(car.price), Год: car.year, Возраст: car.ageYears, Пробег: car.mileage + ' км', VIN: car.vin,
          Кузов: car.bodyType.name, Топливо: car.fuelType.name, Коробка: car.transmission.name,
          Двигатель: car.engineVolume + ' л', Продавец: car.seller.fullName, Проверен: car.certified ? 'Да' : 'Нет',
          Теги: car.tags.join(', '), Обновлено: date(car.updatedAt)
        }).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || (value === 0 ? 0 : '—')}</dd></div>)}</dl>
        {car.description && <p className="preserve">{car.description}</p>}
      </section>
      <Notes api={api} user={user} path={resource + '/notes'} writable={canWrite && !car.archived} />
    </>}
  </>;
}

function Notes({ api, user, path, writable }) {
  const [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState(null);
  const action = useAction();
  const notes = useLoad(api, path, revision);
  const reload = () => { action.clear(); setEditor(null); setRevision(value => value + 1); };
  return <section className="panel">
    <div className="title-row"><h2>Заметки</h2>{writable && <button onClick={() => setEditor({})}>Добавить заметку</button>}</div>
    <ErrorBox error={action.error} reload={reload} />
    {editor && <form key={editor.id || 'new'} onSubmit={event => {
      event.preventDefault();
      const body = values(event);
      body.pinned = body.pinned === 'on';
      if (editor.id) body.version = editor.version;
      action.run(async () => {
        await api.request(editor.id ? path + '/' + editor.id : path, { method: editor.id ? 'PUT' : 'POST', body });
        reload();
      });
    }}>
      <label className="field">Текст заметки<textarea name="content" maxLength={4000} required rows={3} defaultValue={editor.content} /></label>
      <label className="check"><input type="checkbox" name="pinned" defaultChecked={editor.pinned} /> Закрепить</label>
      <div className="row"><button className="primary" disabled={action.busy}>Сохранить</button>
        <button type="button" onClick={() => setEditor(null)}>Отмена</button></div>
    </form>}
    <Loading {...notes} />
    {notes.data?.length === 0 && <p className="muted">Заметок пока нет.</p>}
    {notes.data?.map(note => <article className="note" key={note.id}>
      <div className="title-row"><span><strong>{note.authorName}</strong> · {date(note.createdAt)}
        {note.updatedAt !== note.createdAt && ' · изменена ' + date(note.updatedAt)}
        {note.pinned && ' · Закреплена'}</span>
        {writable && (user.id === note.authorId || user.role === 'ADMIN') && <div className="row">
          <button onClick={() => setEditor(note)}>Изменить</button>
          <button className="danger" disabled={action.busy} onClick={() => window.confirm('Удалить заметку?') &&
            action.run(async () => {
              await api.request(path + '/' + note.id + query({ version: note.version }), { method: 'DELETE' });
              reload();
            })}>Удалить</button>
        </div>}</div>
      <p className="preserve">{note.content}</p>
    </article>)}
  </section>;
}
