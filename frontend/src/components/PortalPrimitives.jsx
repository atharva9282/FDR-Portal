import { Link } from 'react-router-dom'
import { ArrowLeft, AlertTriangle, FileText, RefreshCw } from 'lucide-react'
import { getApiErrorMessage } from '../api/apiErrors'

const message = getApiErrorMessage

export function Header({ eyebrow, title, description, action, back }) {
  return (
    <div className="page-header">
      <div className="page-heading">
        {back && (
          <Link className="back-link" to={back}>
            <ArrowLeft size={17} /> Back to {back === "/fdrs" ? "FDR repository" : "institutions"}
          </Link>
        )}
        <div className="page-heading-copy">
          <div className="eyebrow">{eyebrow}</div>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      {action && <div className="header-action">{action}</div>}
    </div>
  );
}
export function Button({ children, variant = "primary", ...p }) {
  return (
    <button className={`btn ${variant}`} {...p}>
      {children}
    </button>
  );
}
export function Status({ value }) {
  let v = value || "UNKNOWN",
    tone =
      /ACTIVE|VERIFIED|APPROVED|COMPLETED|RELEASED|RENEWED|EXTERNALLY_CONFIRMED/i.test(
        v,
      )
        ? "green"
        : /DISCREPANCY|REJECTED|ACTION_REQUIRED|OVERDUE/i.test(v)
          ? "red"
          : /PENDING|SUBMITTED|REVIEW|REQUESTED/i.test(v)
            ? "amber"
            : "slate";
  return (
    <span className={`status ${tone}`}>
      <i />
      {v
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase())}
    </span>
  );
}
export function Panel({ title, sub, action, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-head">
        <div>
          <h3>{title}</h3>
          {sub && <p>{sub}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
export function Empty({
  icon: Icon = FileText,
  title = "Nothing to show yet",
  text = "Records will appear here once they are available.",
}) {
  return (
    <div className="empty">
      <span>
        <Icon size={21} />
      </span>
      <b>{title}</b>
      <p>{text}</p>
    </div>
  );
}
export function ErrorBox({ error, retry }) {
  return (
    <div className="error-state">
      <AlertTriangle size={20} />
      <div>
        <b>We couldn’t load this information</b>
        <p>{message(error)}</p>
        {retry && (
          <button className="text-btn" onClick={retry}>
            <RefreshCw size={14} /> Retry
          </button>
        )}
      </div>
    </div>
  );
}
export function Loading() {
  return (
    <div className="skeleton-list">
      <i />
      <i />
      <i />
    </div>
  );
}
