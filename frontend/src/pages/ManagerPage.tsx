import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { listDoneCandidates, markTicketDone } from "../api/managerApi";
import StatusBadge from "../components/tickets/StatusBadge";

export default function ManagerPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError } = useQuery({
    queryKey: ["manager", "done-candidates"],
    queryFn: listDoneCandidates,
  });

  const markDone = useMutation({
    mutationFn: markTicketDone,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager", "done-candidates"] });
      queryClient.invalidateQueries({ queryKey: ["tickets"] });
    },
  });

  return (
    <section>
      <h1>Manager approvals</h1>
      <p className="page-subtitle">Review tickets submitted as done candidates.</p>

      {isLoading && <p data-testid="manager-loading">Loading...</p>}
      {isError && (
        <p role="alert" data-testid="manager-error">
          Failed to load candidates.
        </p>
      )}

      {data && data.length === 0 && (
        <p data-testid="manager-empty">No tickets are awaiting approval.</p>
      )}

      {data && data.length > 0 && (
        <ul className="card-list" data-testid="manager-list">
          {data.map((ticket) => (
            <li key={ticket.id} className="card" data-testid="manager-row">
              <div className="card__header">
                <span className="card__title">{ticket.title}</span>
                <StatusBadge status={ticket.status} />
              </div>
              {ticket.description && (
                <p className="card__body">{ticket.description}</p>
              )}
              <div className="card__footer">
                <span className="card__meta">
                  Assignee: {ticket.assignee ?? "Unassigned"}
                </span>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={() => markDone.mutate(ticket.id)}
                  disabled={markDone.isPending}
                  data-testid={`mark-done-${ticket.id}`}
                >
                  Mark done
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
