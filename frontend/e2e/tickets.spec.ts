import { expect, test } from "@playwright/test";

import type { MockUser, Ticket, TicketStatus } from "../src/api/types";

const USERS: MockUser[] = [
  { id: "u-001", username: "alice", displayName: "Alice Johnson" },
  { id: "u-002", username: "bob", displayName: "Bob Smith" },
];

class TicketStore {
  private items: Ticket[] = [];

  create(title: string, description?: string): Ticket {
    const now = new Date().toISOString();
    const ticket: Ticket = {
      id: `00000000-0000-4000-8000-${String(this.items.length + 1).padStart(12, "0")}`,
      title,
      description: description ?? null,
      status: "TODO",
      assignee: null,
      createdAt: now,
      updatedAt: now,
    };
    this.items.unshift(ticket);
    return ticket;
  }

  list(status?: TicketStatus): Ticket[] {
    const all = [...this.items].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
    return status ? all.filter((t) => t.status === status) : all;
  }

  update(id: string, mutator: (t: Ticket) => Ticket): Ticket | undefined {
    const idx = this.items.findIndex((t) => t.id === id);
    if (idx === -1) return undefined;
    const updated = { ...mutator(this.items[idx]!), updatedAt: new Date().toISOString() };
    this.items[idx] = updated;
    return updated;
  }
}

test.beforeEach(async ({ page }) => {
  const store = new TicketStore();
  store.create("Existing ticket", "preloaded");

  await page.route("**/api/users", (route) =>
    route.fulfill({ contentType: "application/json", body: JSON.stringify(USERS) })
  );

  await page.route("**/api/tickets**", async (route, request) => {
    const url = new URL(request.url());
    if (request.method() === "GET" && url.pathname.endsWith("/api/tickets")) {
      const status = url.searchParams.get("status") as TicketStatus | null;
      return route.fulfill({
        contentType: "application/json",
        body: JSON.stringify(store.list(status ?? undefined)),
      });
    }
    if (request.method() === "POST" && url.pathname.endsWith("/api/tickets")) {
      const body = JSON.parse(request.postData() ?? "{}");
      const created = store.create(body.title, body.description);
      return route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify(created) });
    }
    const match = url.pathname.match(/\/api\/tickets\/([^/]+)\/(assignee|status)$/);
    if (match && request.method() === "PATCH") {
      const [, id, action] = match;
      const body = JSON.parse(request.postData() ?? "{}");
      const updated = store.update(id!, (t) =>
        action === "assignee"
          ? { ...t, assignee: USERS.find((u) => u.id === body.assignee || u.username === body.assignee)?.id ?? body.assignee }
          : { ...t, status: body.status }
      );
      if (!updated) {
        return route.fulfill({ status: 404, contentType: "application/json", body: "{}" });
      }
      return route.fulfill({ contentType: "application/json", body: JSON.stringify(updated) });
    }
    return route.fallback();
  });

  await page.route("**/api/manager/tickets/done-candidates", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(store.list("DONE_CANDIDATE")),
    })
  );

  await page.route("**/api/manager/tickets/*/mark-done", async (route, request) => {
    const match = new URL(request.url()).pathname.match(/\/api\/manager\/tickets\/([^/]+)\/mark-done$/);
    if (!match) return route.fallback();
    const id = match[1]!;
    const updated = store.update(id, (t) => ({ ...t, status: "DONE" }));
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(updated ?? {}),
    });
  });
});

test("ticket list renders preloaded tickets", async ({ page }) => {
  await page.goto("/tickets");
  await expect(page.getByTestId("tickets-list")).toBeVisible();
  await expect(page.getByText("Existing ticket")).toBeVisible();
});

test("user creates a ticket and sees it in the list", async ({ page }) => {
  await page.goto("/tickets/new");
  await page.getByTestId("title-input").fill("Brand new ticket");
  await page.getByTestId("description-input").fill("with detail");
  await page.getByTestId("submit-button").click();
  await expect(page).toHaveURL(/\/tickets$/);
  await expect(page.getByText("Brand new ticket")).toBeVisible();
});

test("user assigns and transitions through the workflow", async ({ page }) => {
  await page.goto("/tickets");
  const card = page.getByTestId(/^ticket-/).first();
  const ticketId = (await card.getAttribute("data-testid"))!.replace("ticket-", "");

  await page.getByTestId(`assignee-${ticketId}`).selectOption({ label: "Alice Johnson" });
  await page.getByTestId(`transition-${ticketId}-IN_PROGRESS`).click();
  await expect(card.getByTestId("status-IN_PROGRESS")).toBeVisible();

  await page.getByTestId(`transition-${ticketId}-DONE_CANDIDATE`).click();
  await expect(card.getByTestId("status-DONE_CANDIDATE")).toBeVisible();
});

test("manager marks a done candidate as done", async ({ page }) => {
  await page.goto("/tickets");
  const card = page.getByTestId(/^ticket-/).first();
  const ticketId = (await card.getAttribute("data-testid"))!.replace("ticket-", "");
  await page.getByTestId(`transition-${ticketId}-IN_PROGRESS`).click();
  await expect(card.getByTestId("status-IN_PROGRESS")).toBeVisible();
  await page.getByTestId(`transition-${ticketId}-DONE_CANDIDATE`).click();
  await expect(card.getByTestId("status-DONE_CANDIDATE")).toBeVisible();

  await page.goto("/manager");
  await expect(page.getByTestId("manager-list")).toBeVisible();
  await page.getByTestId(`mark-done-${ticketId}`).click();
  await expect(page.getByTestId("manager-empty")).toBeVisible();
});
