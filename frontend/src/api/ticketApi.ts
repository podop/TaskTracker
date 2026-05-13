import { apiClient } from "./client";
import type {
  AssignTicketRequest,
  CreateTicketRequest,
  StatusChangeRequest,
  Ticket,
  TicketStatus,
} from "./types";

export async function listTickets(status?: TicketStatus): Promise<Ticket[]> {
  const { data } = await apiClient.get<Ticket[]>("/tickets", {
    params: status ? { status } : undefined,
  });
  return data;
}

export async function createTicket(req: CreateTicketRequest): Promise<Ticket> {
  const { data } = await apiClient.post<Ticket>("/tickets", req);
  return data;
}

export async function assignTicket(id: string, req: AssignTicketRequest): Promise<Ticket> {
  const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/assignee`, req);
  return data;
}

export async function changeTicketStatus(id: string, req: StatusChangeRequest): Promise<Ticket> {
  const { data } = await apiClient.patch<Ticket>(`/tickets/${id}/status`, req);
  return data;
}
