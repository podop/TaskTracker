import { apiClient } from "./client";
import type { Ticket } from "./types";

export async function listDoneCandidates(): Promise<Ticket[]> {
  const { data } = await apiClient.get<Ticket[]>("/manager/tickets/done-candidates");
  return data;
}

export async function markTicketDone(id: string): Promise<Ticket> {
  const { data } = await apiClient.patch<Ticket>(`/manager/tickets/${id}/mark-done`);
  return data;
}
