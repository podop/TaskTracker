import { useQuery } from "@tanstack/react-query";

import type { Ticket } from "../../api/types";
import { listMockUsers } from "../../api/userApi";

interface Props {
  ticket: Ticket;
  onChange: (assignee: string) => void;
  disabled?: boolean;
}

export default function AssigneePicker({ ticket, onChange, disabled }: Props) {
  const { data: users } = useQuery({
    queryKey: ["mock-users"],
    queryFn: listMockUsers,
    staleTime: 60_000,
  });

  return (
    <select
      className="assignee-picker"
      value={ticket.assignee ?? ""}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      data-testid={`assignee-${ticket.id}`}
      aria-label={`Assignee for ${ticket.title}`}
    >
      <option value="" disabled>
        Unassigned
      </option>
      {users?.map((u) => (
        <option key={u.id} value={u.id}>
          {u.displayName}
        </option>
      ))}
    </select>
  );
}
