import type { PropsWithChildren } from "react";
import { NavLink } from "react-router-dom";

import "./AppShell.css";

export default function AppShell({ children }: PropsWithChildren) {
  return (
    <div className="app-shell">
      <header className="app-topbar">
        <span className="app-topbar__brand">Task Tracker</span>
        <span className="app-topbar__spacer" />
        <span className="app-topbar__user" aria-label="current user">demo</span>
      </header>
      <div className="app-body">
        <nav className="app-sidebar" aria-label="primary">
          <NavLink to="/tickets" className={navClass} end>
            Tickets
          </NavLink>
          <NavLink to="/tickets/new" className={navClass}>
            Create
          </NavLink>
          <NavLink to="/manager" className={navClass}>
            Manager
          </NavLink>
        </nav>
        <main className="app-main">{children}</main>
      </div>
    </div>
  );
}

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? "app-sidebar__link app-sidebar__link--active" : "app-sidebar__link";
}
