import { Navigate, Route, Routes } from "react-router-dom";

import AppShell from "./components/layout/AppShell";
import TicketsPage from "./pages/TicketsPage";
import NewTicketPage from "./pages/NewTicketPage";
import ManagerPage from "./pages/ManagerPage";

export default function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<Navigate to="/tickets" replace />} />
        <Route path="/tickets" element={<TicketsPage />} />
        <Route path="/tickets/new" element={<NewTicketPage />} />
        <Route path="/manager" element={<ManagerPage />} />
        <Route path="*" element={<Navigate to="/tickets" replace />} />
      </Routes>
    </AppShell>
  );
}
