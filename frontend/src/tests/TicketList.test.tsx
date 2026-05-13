import { screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import TicketList from "../components/tickets/TicketList";
import * as ticketApi from "../api/ticketApi";
import * as userApi from "../api/userApi";
import type { Ticket } from "../api/types";

import { renderWithProviders } from "./test-utils";

const TICKETS: Ticket[] = [
  {
    id: "11111111-1111-1111-1111-111111111111",
    title: "Fix login",
    description: "users locked out",
    status: "TODO",
    assignee: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-02T00:00:00Z",
  },
  {
    id: "22222222-2222-2222-2222-222222222222",
    title: "Refactor onboarding",
    description: null,
    status: "IN_PROGRESS",
    assignee: "u-001",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-03T00:00:00Z",
  },
];

beforeEach(() => {
  vi.spyOn(userApi, "listMockUsers").mockResolvedValue([
    { id: "u-001", username: "alice", displayName: "Alice" },
  ]);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("TicketList", () => {
  it("renders an empty state when the API returns no tickets", async () => {
    vi.spyOn(ticketApi, "listTickets").mockResolvedValue([]);
    renderWithProviders(<TicketList />);
    expect(await screen.findByTestId("tickets-empty")).toBeInTheDocument();
  });

  it("renders ticket cards with status badges and assignees", async () => {
    vi.spyOn(ticketApi, "listTickets").mockResolvedValue(TICKETS);
    renderWithProviders(<TicketList />);

    await waitFor(() => expect(screen.getByTestId("tickets-list")).toBeInTheDocument());
    expect(screen.getByText("Fix login")).toBeInTheDocument();
    expect(screen.getByText("Refactor onboarding")).toBeInTheDocument();
    expect(screen.getByTestId("status-TODO")).toBeInTheDocument();
    expect(screen.getByTestId("status-IN_PROGRESS")).toBeInTheDocument();
  });

  it("shows an error state when the API fails", async () => {
    vi.spyOn(ticketApi, "listTickets").mockRejectedValue(new Error("boom"));
    renderWithProviders(<TicketList />);
    expect(await screen.findByTestId("tickets-error")).toBeInTheDocument();
  });
});
