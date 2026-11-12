import { ArrowRight, CircleAlert, LoaderCircle, Sprout } from "lucide-react";
import type { ComponentProps, ReactNode } from "react";
import { STATUS, type StatusProjeto } from "@/types/domain";

export function Button({
  children,
  className = "",
  variant = "primary",
  busy,
  disabled,
  ...props
}: ComponentProps<"button"> & {
  variant?: "primary" | "secondary" | "danger";
  busy?: boolean;
}) {
  return (
    <button
      type="button"
      {...props}
      disabled={disabled || busy}
      className={`button button-${variant} ${className}`}
    >
      {busy && (
        <LoaderCircle size={17} className="animate-spin" aria-hidden="true" />
      )}
      {children}
    </button>
  );
}
export function Field({
  name,
  label,
  error,
  children,
  hint,
}: {
  name: string;
  label: string;
  error?: string[];
  children: ReactNode;
  hint?: string;
}) {
  return (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      {children}
      {hint && (
        <p id={`${name}-hint`} className="field-hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${name}-error`} className="field-error">
          {error.join(" ")}
        </p>
      )}
    </div>
  );
}
export function Feedback({
  children,
  kind = "error",
}: {
  children: ReactNode;
  kind?: "error" | "success" | "info";
}) {
  return (
    <div
      className={`feedback feedback-${kind}`}
      role={kind === "error" ? "alert" : "status"}
    >
      <CircleAlert size={18} aria-hidden="true" />
      <div>{children}</div>
    </div>
  );
}
export function Loading({ label = "Carregando…" }: { label?: string }) {
  return (
    <div className="loading-state" role="status">
      <LoaderCircle className="animate-spin" size={24} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
export function EmptyState({
  title,
  children,
  action,
}: {
  title: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <Sprout size={36} strokeWidth={1.4} aria-hidden="true" />
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}
export function StatusBadge({ status }: { status: StatusProjeto }) {
  return (
    <span className={`status-badge status-${status}`}>
      <span aria-hidden="true" />
      {STATUS[status] || "Status não informado"}
    </span>
  );
}
export function LinkArrow() {
  return <ArrowRight size={18} aria-hidden="true" />;
}
