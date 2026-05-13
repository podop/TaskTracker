import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import StatusBadge from "../components/tickets/StatusBadge";

describe("StatusBadge", () => {
  it("renders a friendly label for each status", () => {
    const { rerender } = render(<StatusBadge status="TODO" />);
    expect(screen.getByText("To do")).toBeInTheDocument();

    rerender(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText("In progress")).toBeInTheDocument();

    rerender(<StatusBadge status="DONE_CANDIDATE" />);
    expect(screen.getByText("Awaiting approval")).toBeInTheDocument();

    rerender(<StatusBadge status="DONE" />);
    expect(screen.getByText("Done")).toBeInTheDocument();
  });
});
