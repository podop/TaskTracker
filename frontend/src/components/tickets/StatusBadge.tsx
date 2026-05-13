import type { TicketStatus } from "../../api/types";
import "./StatusBadge.css";

const LABELS: Record<TicketStatus, string> = {
  TODO: "To do",
  IN_PROGRESS: "In progress",
  DONE_CANDIDATE: "Awaiting approval",
  DONE: "Done",
};

interface Props {
  status: TicketStatus;
}

export default function StatusBadge({ status }: Props) {
  const className = `status-badge status-badge--${status.toLowerCase().replace(/_/g, "-")}`;
  return (
    <span className={className} data-testid={`status-${status}`}>
      {LABELS[status]}
    </span>
  );
}
