import axios from "axios";

const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "/api";

export const apiClient = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});
