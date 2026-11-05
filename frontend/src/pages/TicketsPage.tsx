import { Link } from "react-router-dom";

import TicketList from "../components/tickets/TicketList";

export default function TicketsPage() {
  return (
    <section>
      <header className="page-header">
        <h1>Tickets</h1>
        <Link to="/tickets/new" className="btn btn--primary" data-testid="create-ticket-link">
          + Create ticket one
        </Lin>
      </header>
      <TicketList />
    </section>
  );
}
