import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import NewTicketPage from "../pages/NewTicketPage";
import * as ticketApi from "../api/ticketApi";

import { renderWithProviders } from "./test-utils";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("NewTicketPage", () => {
  it("submits a new ticket and invalidates the cache", async () => {
    const created = {
      id: "00000000-0000-0000-0000-000000000099",
      title: "New ticket",
      description: "desc",
      status: "TODO" as const,
      assignee: null,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    };
    const spy = vi.spyOn(ticketApi, "createTicket").mockResolvedValue(created);
    renderWithProviders(<NewTicketPage />);

    await userEvent.type(screen.getByTestId("title-input"), "New ticket");
    await userEvent.type(screen.getByTestId("description-input"), "desc");
    await userEvent.click(screen.getByTestId("submit-button"));

    await waitFor(() => expect(spy).toHaveBeenCalledTimes(1));
    expect(spy.mock.calls[0]?.[0]).toEqual({ title: "New ticket", description: "desc" });
  });

  it("shows a local validation error when title is empty", async () => {
    renderWithProviders(<NewTicketPage />);
    const form = screen.getByRole("form", { name: /create ticket/i });
    // bypass the native required check by dispatching submit directly
    form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
    expect(await screen.findByTestId("form-error")).toHaveTextContent(/title is required/i);
  });
});
