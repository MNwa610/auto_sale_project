import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { createApi } from './api.js';
import { ErrorBox, Field, useAction, values } from './ui.jsx';
import { Cars, CarCard } from './cars.jsx';
import { Analytics, Audit, Users, Catalogs, Csv } from './screens.jsx';
import './style.css';

const pages = { cars: 'Автомобили', analytics: 'Аналитика', csv: 'Импорт CSV',
  users: 'Пользователи', audit: 'Аудит', catalogs: 'Справочники', password: 'Сменить пароль' };
const route = () => window.location.hash.slice(1) || 'cars';

function AccountForm({ api, user, cancel }) {
  const action = useAction();
  return <section className="panel sign-in">
    <h1>{user ? 'Смена пароля' : 'Вход в автосалон'}</h1>
    {user?.mustChangePassword && <p>Задайте новый пароль, чтобы продолжить работу.</p>}
    <form onSubmit={event => {
      event.preventDefault();
      const body = values(event);
      action.run(async () => {
        if (user) {
          await api.changePassword(body);
          window.location.hash = 'cars';
        } else await api.login(body);
      });
    }}>
      {user
        ? <><Field label="Текущий пароль" type="password" name="currentPassword" autoComplete="current-password" required />
            <Field label="Новый пароль" type="password" name="newPassword" autoComplete="new-password" minLength={12} required /></>
        : <><Field label="Логин" name="login" autoComplete="username" required maxLength={50} />
            <Field label="Пароль" type="password" name="password" autoComplete="current-password" required /></>}
      <ErrorBox error={action.error} />
      <div className="row">
        <button className="primary" disabled={action.busy}>{user ? 'Сохранить пароль' : 'Войти'}</button>
        {user && !user.mustChangePassword && <button type="button" onClick={cancel}>Отмена</button>}
      </div>
    </form>
  </section>;
}

function App() {
  const [user, setUser] = useState(null);
  const [api] = useState(() => createApi(setUser));
  const [path, setPath] = useState(route);
  const [filters, setFilters] = useState({ archived: 'false' });
  const logout = useAction();
  useEffect(() => {
    const update = () => setPath(route());
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);

  const allowed = ['cars', 'analytics'];
  if (user?.role === 'MANAGER' || user?.role === 'ADMIN') allowed.push('csv');
  if (user?.role === 'ADMIN') allowed.push('users', 'audit', 'catalogs');
  const [section, id] = path.split('/');
  const screen = section === 'password' ? section : allowed.includes(section) ? section : 'cars';
  const props = { api, user };
  return <>
    <header className="masthead">
      <a className="wordmark" href="#cars">Автосалон</a>
      {user && <div className="row identity">
        <span>{user.fullName}</span>
        {!user.mustChangePassword && <a href="#password">Сменить пароль</a>}
        <button disabled={logout.busy} onClick={() => {
          setFilters({ archived: 'false' });
          window.location.hash = 'cars';
          logout.run(api.logout);
        }}>Выйти</button>
      </div>}
    </header>
    {user && !user.mustChangePassword && <nav aria-label="Разделы">
      {allowed.map(page => <a key={page} href={'#' + page} aria-current={page === screen ? 'page' : undefined}>{pages[page]}</a>)}
    </nav>}
    <main>
      <ErrorBox error={logout.error} />
      {!user ? <AccountForm key="login" api={api} />
        : user.mustChangePassword || screen === 'password'
          ? <AccountForm key="password" {...props} cancel={() => { window.location.hash = 'cars'; }} />
          : <>
            {screen === 'cars' && (!id ? <Cars {...props} filters={filters} setFilters={setFilters} />
              : <CarCard key={id} {...props} id={id} />)}
            {screen === 'analytics' && <Analytics {...props} />}
            {screen === 'csv' && <Csv {...props} />}
            {screen === 'users' && <Users {...props} />}
            {screen === 'audit' && <Audit {...props} />}
            {screen === 'catalogs' && <Catalogs {...props} />}
          </>}
    </main>
  </>;
}

createRoot(document.getElementById('root')).render(<App />);
