import { useMutation, useQueryClient } from "@tanstack/react-query";

import { assignTicket, changeTicketStatus } from "../../api/ticketApi";
import type { Ticket, TicketStatus } from "../../api/types";

import AssigneePicker from "./AssigneePicker";
import StatusActions from "./StatusActions";
import StatusBadge from "./StatusBadge";

interface Props {
  ticket: Ticket;
}

export default function TicketCard({ ticket }: Props) {
  const queryClient = useQueryClient();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["tickets"] });
  };

  const assign = useMutation({
    mutationFn: (assignee: string) => assignTicket(ticket.id, { assignee }),
    onSuccess: invalidate,
  });

  const transition = useMutation({
    mutationFn: (next: TicketStatus) => changeTicketStatus(ticket.id, { status: next }),
    onSuccess: invalidate,
  });

  return (
    <li className="card" data-testid={`ticket-${ticket.id}`}>
      <div className="card__header">
        <span className="card__title">{ticket.title}</span>
        <StatusBadge status={ticket.status} />
      </div>
      {ticket.description && <p className="card__body">{ticket.description}</p>}
      <div className="card__footer">
        <AssigneePicker
          ticket={ticket}
          onChange={(v) => assign.mutate(v)}
          disabled={assign.isPending}
        />
        <StatusActions
          ticket={ticket}
          onTransition={(next) => transition.mutate(next)}
          disabled={transition.isPending}
        />
      </div>
      {(assign.isError || transition.isError) && (
        <p role="alert" className="card__error">
          Update failed. Please retry.
        </p>
      )}
    </li>
  );
}
