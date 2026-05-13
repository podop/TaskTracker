import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

import { createTicket } from "../api/ticketApi";

export default function NewTicketPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createTicket,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tickets"] });
      navigate("/tickets");
    },
    onError: (err) => {
      if (axios.isAxiosError(err) && err.response?.data) {
        const detail = (err.response.data as { detail?: string }).detail;
        setError(detail ?? "Failed to create ticket");
      } else {
        setError("Failed to create ticket");
      }
    },
  });

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!title.trim()) {
      setError("Title is required");
      return;
    }
    mutation.mutate({ title: title.trim(), description: description.trim() || undefined });
  }

  return (
    <section>
      <h1>Create ticket</h1>
      <form onSubmit={handleSubmit} className="form" aria-label="create ticket">
        <label className="form__field">
          <span className="form__label">Title</span>
          <input
            type="text"
            value={title}
            maxLength={200}
            onChange={(e) => setTitle(e.target.value)}
            data-testid="title-input"
            required
          />
        </label>
        <label className="form__field">
          <span className="form__label">Description</span>
          <textarea
            value={description}
            maxLength={4000}
            rows={6}
            onChange={(e) => setDescription(e.target.value)}
            data-testid="description-input"
          />
        </label>
        {error && (
          <p role="alert" className="form__error" data-testid="form-error">
            {error}
          </p>
        )}
        <div className="form__actions">
          <button
            type="submit"
            className="btn btn--primary"
            disabled={mutation.isPending}
            data-testid="submit-button"
          >
            {mutation.isPending ? "Creating..." : "Create ticket"}
          </button>
        </div>
      </form>
    </section>
  );
}
