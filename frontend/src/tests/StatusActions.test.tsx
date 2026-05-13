import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import StatusActions from "../components/tickets/StatusActions";
import type { Ticket } from "../api/types";

function makeTicket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: "00000000-0000-0000-0000-000000000001",
    title: "T",
    description: null,
    status: "TODO",
    assignee: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

describe("StatusActions", () => {
  it("offers Start work on TODO tickets and invokes IN_PROGRESS", async () => {
    const onTransition = vi.fn();
    render(<StatusActions ticket={makeTicket({ status: "TODO" })} onTransition={onTransition} />);
    const btn = screen.getByRole("button", { name: /start work/i });
    await userEvent.click(btn);
    expect(onTransition).toHaveBeenCalledWith("IN_PROGRESS");
  });

  it("offers Submit for approval on IN_PROGRESS tickets", async () => {
    const onTransition = vi.fn();
    render(
      <StatusActions ticket={makeTicket({ status: "IN_PROGRESS" })} onTransition={onTransition} />
    );
    const btn = screen.getByRole("button", { name: /submit for approval/i });
    await userEvent.click(btn);
    expect(onTransition).toHaveBeenCalledWith("DONE_CANDIDATE");
  });

  it("renders nothing for DONE_CANDIDATE", () => {
    const { container } = render(
      <StatusActions ticket={makeTicket({ status: "DONE_CANDIDATE" })} onTransition={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing for DONE", () => {
    const { container } = render(
      <StatusActions ticket={makeTicket({ status: "DONE" })} onTransition={() => {}} />
    );
    expect(container).toBeEmptyDOMElement();
  });
});
