import { useQuery } from "@tanstack/react-query";

import { listTickets } from "../../api/ticketApi";
import TicketCard from "./TicketCard";
import "./TicketList.css";

export default function TicketList() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["tickets"],
    queryFn: () => listTickets(),
  });

  if (isLoading) return <p data-testid="tickets-loading">Loading tickets...</p>;
  if (isError) {
    return (
      <p role="alert" data-testid="tickets-error">
        Failed to load tickets.
      </p>
    );
  }
  if (!data || data.length === 0) {
    return (
      <p data-testid="tickets-empty" className="empty-state">
        No tickets yet. Create your first ticket to get started.
      </p>
    );
  }

  return (
    <ul className="card-list" data-testid="tickets-list">
      {data.map((ticket) => (
        <TicketCard key={ticket.id} ticket={ticket} />
      ))}
    </ul>
  );
}
