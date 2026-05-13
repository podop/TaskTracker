export type TicketStatus = "TODO" | "IN_PROGRESS" | "DONE_CANDIDATE" | "DONE";

export interface Ticket {
  id: string;
  title: string;
  description: string | null;
  status: TicketStatus;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MockUser {
  id: string;
  username: string;
  displayName: string;
}

export interface CreateTicketRequest {
  title: string;
  description?: string;
}

export interface AssignTicketRequest {
  assignee: string;
}

export interface StatusChangeRequest {
  status: TicketStatus;
}
