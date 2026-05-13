import type { Ticket, TicketStatus } from "../../api/types";

interface Props {
  ticket: Ticket;
  onTransition: (next: TicketStatus) => void;
  disabled?: boolean;
}

const NEXT_STEP: Partial<Record<TicketStatus, { next: TicketStatus; label: string }>> = {
  TODO: { next: "IN_PROGRESS", label: "Start work" },
  IN_PROGRESS: { next: "DONE_CANDIDATE", label: "Submit for approval" },
};

export default function StatusActions({ ticket, onTransition, disabled }: Props) {
  const step = NEXT_STEP[ticket.status];
  if (!step) return null;

  return (
    <button
      type="button"
      className="btn btn--secondary"
      onClick={() => onTransition(step.next)}
      disabled={disabled}
      data-testid={`transition-${ticket.id}-${step.next}`}
    >
      {step.label}
    </button>
  );
}
