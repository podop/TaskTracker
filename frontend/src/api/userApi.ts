import { apiClient } from "./client";
import type { MockUser } from "./types";

export async function listMockUsers(): Promise<MockUser[]> {
  const { data } = await apiClient.get<MockUser[]>("/users");
  return data;
}
