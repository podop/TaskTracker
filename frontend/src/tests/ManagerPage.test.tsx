import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import ManagerPage from "../pages/ManagerPage";
import * as managerApi from "../api/managerApi";
import type { Ticket } from "../api/types";

import { renderWithProviders } from "./test-utils";

afterEach(() => {
  vi.restoreAllMocks();
});

const CANDIDATE: Ticket = {
  id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  title: "Refactor payments",
  description: "ready for review",
  status: "DONE_CANDIDATE",
  assignee: "u-001",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-02T00:00:00Z",
};

describe("ManagerPage", () => {
  it("renders the empty state when nothing is awaiting approval", async () => {
    vi.spyOn(managerApi, "listDoneCandidates").mockResolvedValue([]);
    renderWithProviders(<ManagerPage />);
    expect(await screen.findByTestId("manager-empty")).toBeInTheDocument();
  });

  it("lists candidates and lets the manager mark one done", async () => {
    const listSpy = vi.spyOn(managerApi, "listDoneCandidates").mockResolvedValue([CANDIDATE]);
    const markSpy = vi
      .spyOn(managerApi, "markTicketDone")
      .mockResolvedValue({ ...CANDIDATE, status: "DONE" });

    renderWithProviders(<ManagerPage />);
    await waitFor(() => expect(screen.getByTestId("manager-list")).toBeInTheDocument());

    await userEvent.click(screen.getByTestId(`mark-done-${CANDIDATE.id}`));

    await waitFor(() => expect(markSpy).toHaveBeenCalled());
    expect(markSpy.mock.calls[0]?.[0]).toBe(CANDIDATE.id);
    expect(listSpy).toHaveBeenCalled();
  });
});
