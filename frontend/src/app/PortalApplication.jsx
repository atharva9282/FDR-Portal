import React, { useEffect, useMemo, useState } from "react";
import {
  Routes,
  Route,
  NavLink,
  Link,
  useNavigate,
  useParams,
  useLocation,
} from "react-router-dom";
import {
  Activity,
  AlertTriangle,
  ArrowDownToLine,
  ArrowLeft,
  ArrowRight,
  Building2,
  Check,
  CheckCheck,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  FileCheck2,
  FileText,
  Filter,
  Home,
  Landmark,
  LogOut,
  Menu,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Search,
  Shield,
  ShieldCheck,
  SlidersHorizontal,
  TrendingUp,
  Upload,
  Users,
  WalletCards,
  X,
  Bell,
  CalendarDays,
  Eye,
  EyeOff,
  LockKeyhole,
  ClipboardList,
  Settings,
  Download,
  ExternalLink,
} from "lucide-react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import api from "../api/client";
import { authApi } from "../api/authApi";
import { fdrApi } from "../api/fdrApi";
import { useAuth } from "../auth/AuthProvider";
import { useToast } from "./ToastProvider";
import { getApiErrorMessage as message } from "../api/apiErrors";
import {
  Header,
  Button,
  Status,
  Panel,
  Empty,
  ErrorBox,
  Loading,
} from "../components/PortalPrimitives";
import { institutionApi } from "../api/institutionApi";
import { monitoringApi } from "../api/monitoringApi";
import { documentApi } from "../api/documentApi";
import { verificationApi } from "../api/verificationApi";
import { workflowApi } from "../api/workflowApi";
const money = (n) =>
    new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0,
    }).format(Number(n) || 0),
  date = (d) =>
    d
      ? new Intl.DateTimeFormat("en-IN", {
          day: "2-digit",
          month: "short",
          year: "numeric",
          timeZone: "Asia/Kolkata",
        }).format(new Date(`${String(d).slice(0, 10)}T00:00:00+05:30`))
      : "—";
const roleLabel = (r) =>
  ({
    ADMIN: "Administrator",
    INSTITUTION: "Institution user",
    VERIFICATION_OFFICIAL: "Verification official",
    APPROVING_AUTHORITY: "Approving authority",
    REGULATION_OFFICIAL: "Regulation official",
  })[r] || r;
function Protected({ children, roles }) {
  const { user, checking } = useAuth();
  const loc = useLocation();
  if (checking)
    return (
      <div className="screen-center">
        <div className="spinner" />
      </div>
    );
  if (!user) return <NavigateToLogin to={loc.pathname} />;
  if (roles && !roles.includes(user.role))
    return (
      <div className="screen-center">
        <div className="error-card">
          <Shield size={25} />
          <h2>Access restricted</h2>
          <p>This workspace is available to authorized roles only.</p>
          <Link to={homeForRole(user.role)} className="btn primary">
            Back to your workspace
          </Link>
        </div>
      </div>
    );
  return children;
}
function NavigateToLogin({ to }) {
  const n = useNavigate();
  useEffect(() => n("/login", { replace: true, state: { from: to } }), []);
  return null;
}
function homeForRole(role) {
  if (role === "APPROVING_AUTHORITY") return "/renewals";
  if (role === "VERIFICATION_OFFICIAL") return "/verification";
  return "/dashboard";
}
function RoleHome() {
  const { user } = useAuth();
  const n = useNavigate();
  useEffect(() => n(homeForRole(user?.role), { replace: true }), [user?.role]);
  return null;
}
function PortalApplication() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="*"
        element={
          <Protected>
            <Shell />
          </Protected>
        }
      />
    </Routes>
  );
}
function Login() {
  const { user, signIn } = useAuth(),
    n = useNavigate(),
    loc = useLocation(),
    toast = useToast();
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [show, setShow] = useState(false),
    [busy, setBusy] = useState(false),
    [err, setErr] = useState("");
  useEffect(() => {
    if (user) n(homeForRole(user.role), { replace: true });
  }, [user]);
  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    if (!email.trim() || !/^\S+@\S+\.\S+$/.test(email)) {
      setErr("Enter a valid email address.");
      return;
    }
    if (!password) {
      setErr("Enter your password.");
      return;
    }
    setBusy(true);
    try {
      await signIn(email, password);
      toast("Signed in successfully");
      n(loc.state?.from || homeForRole(user.role), { replace: true });
    } catch (e) {
      setErr(message(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="login-page">
      <section className="login-brand">
        <div className="brand-lockup">
          <span className="brand-mark">
            <ShieldCheck size={25} />
          </span>
          <span>
            <b>FDR SENTINEL</b>
            <small>Security Deposit & FDR Management Portal</small>
          </span>
        </div>
        <div className="login-copy">
          <div className="eyebrow light">REGULATORY WORKFLOW PROTOTYPE</div>
          <h1>Clarity and control for every security deposit.</h1>
          <p>
            A centralized workspace for institution records, verification,
            maturity monitoring and controlled release workflows.
          </p>
          <div className="login-features">
            <span>
              <FileCheck2 /> Document review
            </span>
            <span>
              <CalendarDays /> Maturity monitoring
            </span>
            <span>
              <Shield /> Role based access
            </span>
          </div>
        </div>
        <div className="login-legal">
          AICTE regulatory workflow prototype · Not an official AICTE system
        </div>
      </section>
      <section className="login-form-area">
        <form className="login-form" onSubmit={submit}>
          <span className="mobile-brand">
            <ShieldCheck /> FDR SENTINEL
          </span>
          <div className="eyebrow">SECURE PORTAL ACCESS</div>
          <h2>Welcome back</h2>
          <p>Sign in with your authorized account to continue.</p>
          {err && (
            <div className="inline-alert">
              <AlertTriangle size={17} />
              {err}
            </div>
          )}
          <label>
            Email address
            <input
              autoComplete="username"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@institution.edu.in"
            />
          </label>
          <label>
            Password
            <div className="password-input">
              <input
                autoComplete="current-password"
                type={show ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
              />
              <button
                type="button"
                aria-label={show ? "Hide password" : "Show password"}
                onClick={() => setShow(!show)}
              >
                {show ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </label>
          <button className="btn primary full" disabled={busy}>
            {busy ? (
              <>
                <span className="spinner small" /> Signing in…
              </>
            ) : (
              <>
                Sign in <ArrowRight size={17} />
              </>
            )}
          </button>
          <div className="login-note">
            <LockKeyhole size={15} /> Accounts are provisioned by an authorized
            administrator.
          </div>
        </form>
        <footer>
          FDR Sentinel <span>·</span> Prototype environment
        </footer>
      </section>
    </div>
  );
}
const navGroups = [
  {
    title: "WORKSPACE",
    links: [
      ["/dashboard", "Overview", Home],
      ["/fdrs", "FDR repository", WalletCards],
      ["/institutions", "Institutions", Building2],
      ["/monitoring", "Maturity & alerts", CalendarDays],
    ],
  },
  {
    title: "REVIEW & CONTROL",
    links: [
      ["/verification", "Verification desk", FileCheck2],
      ["/renewals", "Renewals", RefreshCw],
      ["/releases", "Release requests", ArrowDownToLine],
    ],
  },
  {
    title: "ADMINISTRATION",
    links: [
      ["/users", "User access", Users],
      ["/reports", "Reports & audit", ClipboardList],
    ],
  },
];
function Shell() {
  const { user, signOut } = useAuth(),
    [collapsed, setCollapsed] = useState(false),
    [mobile, setMobile] = useState(false),
    [notif, setNotif] = useState(false);
  const loc = useLocation(),
    toast = useToast(),
    title = {
      dashboard: "Overview",
      fdrs: "FDR repository",
      institutions: "Institutions",
      monitoring: "Maturity & alerts",
      verification: "Verification desk",
      renewals: "Renewal management",
      releases: "Release & refund",
      users: "User management",
      reports: "Reports & audit",
      details: "FDR details",
    };
  const allowed = (l) => {
    const roles = {
      dashboard: ["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"],
      fdrs: ["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"],
      institutions: ["ADMIN", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"],
      verification: ["ADMIN", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"],
      monitoring: ["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"],
      renewals: ["ADMIN", "INSTITUTION", "APPROVING_AUTHORITY", "REGULATION_OFFICIAL"],
      releases: ["ADMIN", "INSTITUTION", "APPROVING_AUTHORITY", "REGULATION_OFFICIAL"],
      users: ["ADMIN"],
      reports: ["ADMIN", "REGULATION_OFFICIAL"],
    };
    return !roles[l] || roles[l].includes(user.role);
  };
  return (
    <div
      className={`app-shell ${collapsed ? "collapsed" : ""} ${mobile ? "mobile-open" : ""}`}
    >
      <aside className="sidebar">
        <div className="side-brand">
          <span className="brand-mark">
            <ShieldCheck size={22} />
          </span>
          {!collapsed && (
            <span>
              <b>FDR SENTINEL</b>
              <small>SECURITY DEPOSIT PORTAL</small>
            </span>
          )}
          <button className="mobile-close" onClick={() => setMobile(false)}>
            <X size={18} />
          </button>
        </div>
        <div className="workspace-tag">
          <span className="live-dot" /> {!collapsed && "REGULATORY WORKSPACE"}
        </div>
        <nav>
          {navGroups
            .map((g) => ({ ...g, links: g.links.filter(([p]) => allowed(p.slice(1))) }))
            .filter((g) => g.links.length > 0)
            .map((g) => (
            <div className="nav-group" key={g.title}>
              <small>{!collapsed && g.title}</small>
              {g.links.map(([path, label, Icon]) => (
                  <NavLink
                    to={path}
                    key={path}
                    onClick={() => setMobile(false)}
                    className={({ isActive }) =>
                      `nav-item ${isActive ? "active" : ""}`
                    }
                    title={collapsed ? label : ""}
                  >
                    <Icon size={18} />
                    {!collapsed && <span>{label}</span>}
                    {path === "/verification" && !collapsed && (
                      <span className="nav-pip" />
                    )}
                  </NavLink>
                ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="side-help">
            <span className="help-icon">
              <Shield size={18} />
            </span>
            {!collapsed && (
              <span>
                <b>Protected workspace</b>
                <small>Access is role controlled</small>
              </span>
            )}
          </div>
          <button
            onClick={() => {
              signOut();
              toast("You have signed out", "info");
            }}
            className="nav-item signout"
          >
            <LogOut size={18} />
            {!collapsed && "Sign out"}
          </button>
        </div>
      </aside>
      <div className="main-column">
        <header className="topbar">
          <button
            className="icon-btn mobile-menu"
            onClick={() => setMobile(true)}
          >
            <Menu />
          </button>
          <div className="top-title">
            <span>WORKSPACE</span>
            <b>
              {loc.pathname.match(/^\/fdrs\/\d+/)
                ? "FDR details"
                : title[loc.pathname.split("/")[1]] || "Workspace"}
            </b>
          </div>
          <button
            className="collapse-btn icon-btn"
            onClick={() => setCollapsed(!collapsed)}
            title="Toggle navigation"
          >
            <Menu size={18} />
          </button>
          <div className="top-spacer" />
          {user.role !== "APPROVING_AUTHORITY" && (
            <button
              className="icon-btn notification-trigger"
              onClick={() => setNotif(!notif)}
              aria-label="Notifications"
            >
              <Bell size={19} />
            </button>
          )}
          <div className="user-chip">
            <span className="avatar">
              {user.email?.slice(0, 1).toUpperCase()}
            </span>
            <span className="user-meta">
              <b>{user.institutionName || user.email}</b>
              <small>{roleLabel(user.role)}</small>
            </span>
            <ChevronDown size={15} />
          </div>
          {notif && user.role !== "APPROVING_AUTHORITY" && (
            <div className="quick-pop">
              <h4>Notifications</h4>
              <p>
                Open the maturity & alerts workspace to review notification
                history.
              </p>
              <Link to="/monitoring" onClick={() => setNotif(false)}>
                View notifications <ArrowRight size={14} />
              </Link>
            </div>
          )}
        </header>
        <main className="main-content">
          <Routes>
            <Route path="/" element={<RoleHome />} />
            <Route path="/dashboard" element={<Protected roles={["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"]}><Dashboard /></Protected>} />
            <Route path="/fdrs" element={<Protected roles={["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"]}><FdrList /></Protected>} />
            <Route
              path="/fdrs/new"
              element={
                <Protected roles={["ADMIN", "INSTITUTION"]}>
                  <FdrForm />
                </Protected>
              }
            />
            <Route path="/fdrs/:id" element={<Protected roles={["ADMIN", "INSTITUTION", "VERIFICATION_OFFICIAL", "REGULATION_OFFICIAL"]}><FdrDetail /></Protected>} />
            <Route
              path="/institutions"
              element={
                <Protected
                  roles={[
                    "ADMIN",
                    "VERIFICATION_OFFICIAL",
                    "REGULATION_OFFICIAL",
                  ]}
                >
                  <InstitutionList />
                </Protected>
              }
            />
            <Route
              path="/institutions/new"
              element={
                <Protected roles={["ADMIN"]}>
                  <InstitutionForm />
                </Protected>
              }
            />
            <Route
              path="/monitoring"
              element={
                <Protected
                  roles={[
                    "ADMIN",
                    "INSTITUTION",
                    "VERIFICATION_OFFICIAL",
                    "REGULATION_OFFICIAL",
                  ]}
                >
                  <Monitoring />
                </Protected>
              }
            />
            <Route
              path="/verification"
              element={
                <Protected
                  roles={[
                    "ADMIN",
                    "VERIFICATION_OFFICIAL",
                    "REGULATION_OFFICIAL",
                  ]}
                >
                  <Verification />
                </Protected>
              }
            />
            <Route path="/renewals" element={<Protected roles={["ADMIN", "INSTITUTION", "APPROVING_AUTHORITY", "REGULATION_OFFICIAL"]}><Workflow kind="renewal" /></Protected>} />
            <Route path="/releases" element={<Protected roles={["ADMIN", "INSTITUTION", "APPROVING_AUTHORITY", "REGULATION_OFFICIAL"]}><Workflow kind="release" /></Protected>} />
            <Route
              path="/users"
              element={
                <Protected roles={["ADMIN"]}>
                  <UsersPage />
                </Protected>
              }
            />
            <Route
              path="/reports"
              element={
                <Protected roles={["ADMIN", "REGULATION_OFFICIAL"]}>
                  <Reports />
                </Protected>
              }
            />
            <Route path="*" element={<RoleHome />} />
          </Routes>
        </main>
        <footer className="app-footer">
          FDR SENTINEL <span>·</span> AICTE regulatory workflow prototype{" "}
          <span className="footer-right">
            Session data is protected by your assigned role
          </span>
        </footer>
      </div>
    </div>
  );
}
function useLoad(loader, deps = []) {
  const [data, setData] = useState(null),
    [error, setError] = useState(null),
    [loading, setLoading] = useState(true),
    [tick, setTick] = useState(0);
  const reload = () => setTick((x) => x + 1);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    loader()
      .then((d) => {
        if (alive) {
          setData(d);
          setError(null);
        }
      })
      .catch((e) => alive && setError(e))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, [...deps, tick]);
  return { data, error, loading, reload, setData };
}
function Dashboard() {
  const { user } = useAuth();
  const [days, setDays] = useState(30),
    {
      data: d,
      error,
      loading,
      reload,
    } = useLoad(
      () => monitoringApi.dashboard(days).then((r) => r.data),
      [days],
    ),
    { data: m } = useLoad(
      () => monitoringApi.maturities(days).then((r) => r.data),
      [days],
    ),
    { data: n } = useLoad(
      () => monitoringApi.notifications(true).then((r) => r.data),
      [],
    ),
    [fdrs, setFdrs] = useState(null);
  useEffect(() => {
    fdrApi
      .list({ size: 100, sort: "submittedAt,desc" })
      .then((r) => setFdrs(r.data.content || []))
      .catch(() => setFdrs([]));
  }, []);
  const cards = [
    ["Total instruments", d?.totalInstruments, WalletCards, "blue"],
    ["Active instruments", d?.activeInstruments, ShieldCheck, "green"],
    [
      `Maturing in ${days} days`,
      d?.expiringWithinWindow,
      CalendarDays,
      "amber",
    ],
    ["Matured instruments", d?.maturedInstruments, Clock, "red"],
    ["Pending verification", d?.pendingVerifications, FileCheck2, "violet"],
    ["Discrepancy cases", d?.discrepancyCases, AlertTriangle, "red"],
    ["Renewed instruments", d?.renewedInstruments, RefreshCw, "teal"],
    ["Released instruments", d?.releasedInstruments, ArrowDownToLine, "slate"],
  ];
  return (
    <>
      <Header
        eyebrow="REGULATORY OVERVIEW"
        title="Good day. Here's your portfolio."
        description="Monitor security deposits, deadlines and review activity across your authorized institutions."
        action={
          <div className="select-wrap">
            <CalendarDays size={16} />
            <select value={days} onChange={(e) => setDays(+e.target.value)}>
              <option value={7}>Next 7 days</option>
              <option value={30}>Next 30 days</option>
              <option value={60}>Next 60 days</option>
              <option value={90}>Next 90 days</option>
              <option value={365}>Next 12 months</option>
            </select>
            <ChevronDown size={14} />
          </div>
        }
      />
      {error ? (
        <ErrorBox error={error} retry={reload} />
      ) : (
        <>
          <div className="metric-grid">
            {cards.map(([t, v, I, c]) => (
              <div className="metric-card" key={t}>
                <div className={`metric-icon ${c}`}>
                  <I size={18} />
                </div>
                <span>{t}</span>
                <b>{loading ? "—" : (v ?? "—")}</b>
                <small>
                  {t.startsWith("Maturing")
                    ? "Within selected window"
                    : "Live backend count"}
                </small>
              </div>
            ))}
          </div>
          <div className="dashboard-grid">
            <Panel
              title="Maturity overview"
              sub={`Portfolio instruments · next ${days} days`}
              action={
                <Link className="subtle-link" to="/monitoring">
                  View tracker <ArrowRight size={14} />
                </Link>
              }
            >
              <div className="chart-wrap">
                {m?.length ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={m.slice(0, 12)}
                      margin={{ top: 8, right: 12, left: -15, bottom: 0 }}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        vertical={false}
                        stroke="#edf0f4"
                      />
                      <XAxis
                        dataKey="instrumentReference"
                        tick={{ fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip formatter={(v) => money(v)} />
                      <Bar
                        dataKey="depositAmount"
                        fill="#3568d4"
                        radius={[5, 5, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <Empty
                    icon={TrendingUp}
                    title={
                      loading
                        ? "Loading maturity data"
                        : "No maturity data available"
                    }
                    text={
                      loading
                        ? ""
                        : "There are no instruments in this monitoring window."
                    }
                  />
                )}
              </div>
            </Panel>
            <Panel
              title="Priority attention"
              sub="Items that need a timely review"
              action={
                <Link className="subtle-link" to="/verification">
                  Open verification <ArrowRight size={14} />
                </Link>
              }
            >
              <Attention fdrs={fdrs} maturity={m} />
            </Panel>
          </div>
          <div className="dashboard-grid lower-grid">
            <Panel
              title="Upcoming maturities"
              sub="Instruments approaching their maturity date"
              action={
                <Link className="subtle-link" to="/monitoring">
                  All maturities <ArrowRight size={14} />
                </Link>
              }
            >
              <MaturityTable rows={m?.slice(0, 5)} />
            </Panel>
            <Panel
              title="Recent alerts"
              sub="Unread notifications from the monitoring service"
              action={
                <Link className="subtle-link" to="/monitoring">
                  Notification history <ArrowRight size={14} />
                </Link>
              }
            >
              <NotificationList rows={n?.slice(0, 4)} />
            </Panel>
          </div>
          <div className="quick-actions">
            <div>
              <b>Quick actions</b>
              <span>Common tasks for this workspace</span>
            </div>
            {["ADMIN", "INSTITUTION"].includes(user.role) && (
              <Link to="/fdrs/new">
                <Plus size={17} /> Register an FDR
              </Link>
            )}
            <Link to="/renewals">
              <RefreshCw size={16} /> Review renewals
            </Link>
            <Link to="/releases">
              <ArrowDownToLine size={16} /> Release requests
            </Link>
          </div>
        </>
      )}
    </>
  );
}
function Attention({ fdrs, maturity }) {
  if (!fdrs) return <Loading />;
  const rows = (fdrs || [])
    .filter(
      (x) =>
        ["SUBMITTED", "UNDER_REVIEW", "ACTION_REQUIRED", "REJECTED"].includes(
          x.status,
        ) ||
        ["PENDING", "DISCREPANCY_FOUND", "UNDER_REVIEW"].includes(
          x.verificationStatus,
        ),
    )
    .slice(0, 4);
  if (!rows.length)
    return (
      <Empty
        icon={CheckCheck}
        title="No priority cases"
        text="No pending verification or discrepancy cases were returned."
      />
    );
  return (
    <div className="attention-list">
      {rows.map((x) => (
        <Link to={`/fdrs/${x.id}`} key={x.id} className="attention-row">
          <span className="priority-icon">
            <AlertTriangle size={16} />
          </span>
          <span className="attention-info">
            <b>{x.instrumentReference}</b>
            <small>{x.institutionName}</small>
          </span>
          <Status value={x.verificationStatus || x.status} />
          <ChevronRight size={16} />
        </Link>
      ))}
    </div>
  );
}
function MaturityTable({ rows }) {
  if (!rows) return <Loading />;
  if (!rows.length)
    return (
      <Empty
        icon={CalendarDays}
        title="No upcoming maturities"
        text="No maturity dates fall within the selected window."
      />
    );
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>INSTRUMENT</th>
            <th>INSTITUTION</th>
            <th>MATURITY</th>
            <th>REMAINING</th>
            <th>STATUS</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.fdrId}>
              <td>
                <Link className="strong-link" to={`/fdrs/${r.fdrId}`}>
                  {r.instrumentReference}
                </Link>
                <small>{r.bankName}</small>
              </td>
              <td>
                {r.institutionName}
                <small>{r.institutionCode}</small>
              </td>
              <td>{date(r.maturityDate)}</td>
              <td>
                <span
                  className={`days ${r.daysRemaining < 0 ? "overdue" : r.daysRemaining < 15 ? "urgent" : ""}`}
                >
                  {r.daysRemaining < 0
                    ? `${Math.abs(r.daysRemaining)} days overdue`
                    : `${r.daysRemaining} days`}
                </span>
              </td>
              <td>
                <Status value={r.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function NotificationList({ rows }) {
  const toast = useToast(),
    [busy, setBusy] = useState(null);
  if (!rows) return <Loading />;
  if (!rows.length)
    return (
      <Empty
        icon={Bell}
        title="You're all caught up"
        text="There are no unread monitoring notifications."
      />
    );
  const mark = async (id) => {
    setBusy(id);
    try {
      await monitoringApi.markNotificationRead(id);
      toast("Notification marked as read");
      location.reload();
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(null);
    }
  };
  return (
    <div className="notification-list">
      {rows.map((n) => (
        <div className="notification-row" key={n.id}>
          <span className="notif-dot" />
          <div>
            <b>{n.title}</b>
            <p>{n.message}</p>
            <small>{date(n.dueDate || n.createdAt)}</small>
          </div>
          <button
            disabled={busy === n.id}
            className="icon-btn"
            title="Mark as read"
            onClick={() => mark(n.id)}
          >
            <Check size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}
function usePage(url, params) {
  return useLoad(
    () => api.get(url, { params }).then((r) => r.data),
    [url, JSON.stringify(params || {})],
  );
}
function PageControls({ page, totalPages, onPage }) {
  return (
    <div className="pagination">
      <span>
        Page {page + 1} of {Math.max(1, totalPages || 1)}
      </span>
      <div>
        <button
          className="icon-btn"
          disabled={page <= 0}
          onClick={() => onPage(page - 1)}
        >
          <ChevronLeft size={16} />
        </button>
        <button
          className="icon-btn"
          disabled={page >= (totalPages || 1) - 1}
          onClick={() => onPage(page + 1)}
        >
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}
function FdrList() {
  const { user } = useAuth(),
    [page, setPage] = useState(0),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [inst, setInst] = useState(""),
    [insts, setInsts] = useState([]);
  const params = {
    page,
    size: 12,
    sort: "submittedAt,desc",
    ...(status ? { status } : {}),
    ...(user.role === "INSTITUTION" && user.institutionId
      ? { institutionId: user.institutionId }
      : inst
        ? { institutionId: inst }
        : {}),
  };
  const { data, error, loading, reload } = usePage("/fdrs", params);
  useEffect(() => {
    if (user.role !== "INSTITUTION")
      api
        .get("/institutions", { params: { size: 100 } })
        .then((r) => setInsts(r.data.content || []))
        .catch(() => {});
  }, []);
  const rows = (data?.content || []).filter((r) =>
    `${r.instrumentReference} ${r.institutionName} ${r.bankName}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  );
  return (
    <>
      <Header
        eyebrow="INSTRUMENT REGISTER"
        title="FDR repository"
        description="Search and manage fixed deposit records within your authorized scope."
        action={
          user.role === "INSTITUTION" || user.role === "ADMIN" ? (
            <Link className="btn primary" to="/fdrs/new">
              <Plus size={17} /> Register FDR
            </Link>
          ) : null
        }
      />
      <div className="filter-bar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search reference, institution or bank"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="filter-select">
          <Filter size={15} />
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            {[
              "SUBMITTED",
              "UNDER_REVIEW",
              "ACTION_REQUIRED",
              "ACTIVE",
              "RENEWAL_PENDING",
              "RELEASE_REQUESTED",
              "RENEWED",
              "RELEASED",
              "REJECTED",
            ].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </div>
        {user.role !== "INSTITUTION" && (
          <div className="filter-select">
            <Building2 size={15} />
            <select
              value={inst}
              onChange={(e) => {
                setInst(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All institutions</option>
              {insts.map((i) => (
                <option value={i.id} key={i.id}>
                  {i.name}
                </option>
              ))}
            </select>
          </div>
        )}
        <button
          className="btn ghost"
          onClick={() => {
            setSearch("");
            setStatus("");
            setInst("");
            setPage(0);
          }}
        >
          Clear
        </button>
      </div>
      <Panel
        title="Registered instruments"
        sub={`${data?.totalElements ?? "—"} records · live data`}
        className="table-panel"
      >
        {error ? (
          <ErrorBox error={error} retry={reload} />
        ) : loading ? (
          <Loading />
        ) : rows.length ? (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>FDR REFERENCE</th>
                    <th>INSTITUTION</th>
                    <th>BANK</th>
                    <th>DEPOSIT AMOUNT</th>
                    <th>ISSUED</th>
                    <th>MATURITY</th>
                    <th>ADMIN STATUS</th>
                    <th>VERIFICATION</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <Link className="strong-link" to={`/fdrs/${r.id}`}>
                          {r.instrumentReference}
                        </Link>
                        <small>FDR-{String(r.id).padStart(5, "0")}</small>
                      </td>
                      <td>
                        {r.institutionName}
                        <small>{r.institutionCode}</small>
                      </td>
                      <td>
                        {r.bankName}
                        <small>{r.branchName || "—"}</small>
                      </td>
                      <td className="amount">{money(r.depositAmount)}</td>
                      <td>{date(r.issueDate)}</td>
                      <td>{date(r.maturityDate)}</td>
                      <td>
                        <Status value={r.status} />
                      </td>
                      <td>
                        <Status value={r.verificationStatus} />
                      </td>
                      <td>
                        <Link
                          to={`/fdrs/${r.id}`}
                          className="icon-btn"
                          title="View record"
                        >
                          <ExternalLink size={16} />
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PageControls
              page={page}
              totalPages={data.totalPages}
              onPage={setPage}
            />
          </>
        ) : (
          <Empty
            icon={WalletCards}
            title="No FDR records found"
            text="Try adjusting the filters, or register the first instrument."
          />
        )}
      </Panel>
    </>
  );
}
function FdrForm() {
  const { user } = useAuth(),
    n = useNavigate(),
    toast = useToast(),
    [insts, setInsts] = useState([]),
    [busy, setBusy] = useState(false),
    [err, setErr] = useState(""),
    [form, setForm] = useState({
      institutionId: user.institutionId || "",
      instrumentReference: "",
      bankName: "",
      branchName: "",
      depositAmount: "",
      issueDate: "",
      maturityDate: "",
    });
  useEffect(() => {
    if (user.role !== "INSTITUTION")
      api
        .get("/institutions", { params: { size: 100 } })
        .then((r) => setInsts(r.data.content || []))
        .catch((e) => setErr(message(e)));
  }, []);
  const set = (k, v) => setForm((x) => ({ ...x, [k]: v }));
  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    if (
      !form.institutionId ||
      !form.instrumentReference ||
      !form.bankName ||
      +form.depositAmount <= 0 ||
      !form.issueDate ||
      !form.maturityDate
    ) {
      setErr("Complete all required fields and enter a positive amount.");
      return;
    }
    if (form.maturityDate <= form.issueDate) {
      setErr("Maturity date must be after the issue date.");
      return;
    }
    setBusy(true);
    try {
      const { data } = await fdrApi.create({
        ...form,
        institutionId: +form.institutionId,
        depositAmount: +form.depositAmount,
      });
      toast("FDR record created");
      n(`/fdrs/${data.id}`);
    } catch (e) {
      setErr(message(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <Header
        eyebrow="INSTRUMENT REGISTER"
        title="Register a fixed deposit"
        description="Create an institution linked FDR record. Add supporting documents from the record detail page."
        back="/fdrs"
      />
      <form className="form-card" onSubmit={submit}>
        <div className="form-section-title">
          <span>01</span>
          <div>
            <b>Institution & instrument</b>
            <small>
              Enter the details exactly as shown on the deposit instrument.
            </small>
          </div>
        </div>
        {err && (
          <div className="inline-alert">
            <AlertTriangle size={17} />
            {err}
          </div>
        )}
        <div className="form-grid">
          {user.role !== "INSTITUTION" && (
            <Field label="Institution" required>
              <select
                required
                value={form.institutionId}
                onChange={(e) => set("institutionId", e.target.value)}
              >
                <option value="">Select institution</option>
                {insts.map((i) => (
                  <option value={i.id} key={i.id}>
                    {i.institutionCode} · {i.name}
                  </option>
                ))}
              </select>
            </Field>
          )}
          <Field label="Instrument reference" required>
            <input
              required
              maxLength="120"
              value={form.instrumentReference}
              onChange={(e) => set("instrumentReference", e.target.value)}
              placeholder="e.g. FDR/2025/00124"
            />
          </Field>
          <Field label="Bank name" required>
            <input
              required
              value={form.bankName}
              onChange={(e) => set("bankName", e.target.value)}
              placeholder="Bank name"
            />
          </Field>
          <Field label="Branch">
            <input
              value={form.branchName}
              onChange={(e) => set("branchName", e.target.value)}
              placeholder="Branch name"
            />
          </Field>
          <Field label="Deposit amount (₹)" required>
            <input
              required
              type="number"
              min="0.01"
              step="0.01"
              value={form.depositAmount}
              onChange={(e) => set("depositAmount", e.target.value)}
              placeholder="0.00"
            />
          </Field>
          <Field label="Issue date" required>
            <input
              required
              type="date"
              value={form.issueDate}
              onChange={(e) => set("issueDate", e.target.value)}
            />
          </Field>
          <Field label="Maturity date" required>
            <input
              required
              min={form.issueDate || undefined}
              type="date"
              value={form.maturityDate}
              onChange={(e) => set("maturityDate", e.target.value)}
            />
          </Field>
        </div>
        <div className="form-note">
          <ShieldCheck size={17} /> Institution accounts are limited to their
          linked institution by backend authorization.
        </div>
        <div className="form-actions">
          <Link className="btn ghost" to="/fdrs">
            Cancel
          </Link>
          <button disabled={busy} className="btn primary">
            {busy ? (
              "Creating…"
            ) : (
              <>
                Create FDR record <ArrowRight size={16} />
              </>
            )}
          </button>
        </div>
      </form>
    </>
  );
}
function Field({ label, required, children, hint }) {
  return (
    <label className="field">
      <span>
        {label}
        {required && <em> *</em>}
      </span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
function InstitutionList() {
  const { user } = useAuth(),
    [page, setPage] = useState(0),
    [q, setQ] = useState("");
  const { data, error, loading, reload } = usePage("/institutions", {
    page,
    size: 12,
    sort: "name,asc",
  });
  const rows = (data?.content || []).filter((x) =>
    `${x.name} ${x.institutionCode} ${x.district} ${x.state}`
      .toLowerCase()
      .includes(q.toLowerCase()),
  );
  return (
    <>
      <Header
        eyebrow="ORGANIZATION DIRECTORY"
        title="Institutions"
        description="Authorized institutions associated with security deposit records."
        action={
          user.role === "ADMIN" ? (
            <Link className="btn primary" to="/institutions/new">
              <Plus size={17} /> Add institution
            </Link>
          ) : null
        }
      />
      <div className="filter-bar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search institutions"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <div className="records-note">
          <Building2 size={15} /> {data?.totalElements ?? "—"} institutions
        </div>
      </div>
      <Panel
        title="Institution directory"
        sub="Institution contact and location details"
        className="table-panel"
      >
        {error ? (
          <ErrorBox error={error} retry={reload} />
        ) : loading ? (
          <Loading />
        ) : rows.length ? (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>INSTITUTION</th>
                    <th>CONTACT PERSON</th>
                    <th>CONTACT DETAILS</th>
                    <th>DISTRICT / STATE</th>
                    <th>STATUS</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((i) => (
                    <tr key={i.id}>
                      <td>
                        <b>{i.name}</b>
                        <small>{i.institutionCode}</small>
                      </td>
                      <td>{i.contactPerson || "—"}</td>
                      <td>
                        {i.contactEmail || "—"}
                        <small>{i.contactPhone || "—"}</small>
                      </td>
                      <td>
                        {[i.district, i.state].filter(Boolean).join(", ") ||
                          "—"}
                      </td>
                      <td>
                        <Status value={i.active ? "ACTIVE" : "INACTIVE"} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PageControls
              page={page}
              totalPages={data.totalPages}
              onPage={setPage}
            />
          </>
        ) : (
          <Empty
            icon={Building2}
            title="No institutions found"
            text="The backend returned no institution records."
          />
        )}
      </Panel>
    </>
  );
}
function InstitutionForm() {
  const n = useNavigate(),
    toast = useToast(),
    [form, setForm] = useState({
      institutionCode: "",
      name: "",
      contactPerson: "",
      contactEmail: "",
      contactPhone: "",
      address: "",
      district: "",
      state: "",
    }),
    [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  const set = (k, v) => setForm((x) => ({ ...x, [k]: v }));
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      await institutionApi.create(form);
      toast("Institution created");
      n("/institutions");
    } catch (e) {
      setErr(message(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <Header
        eyebrow="ORGANIZATION DIRECTORY"
        title="Add an institution"
        description="Create a directory record for an authorized institution."
        back="/institutions"
      />
      <form className="form-card" onSubmit={submit}>
        {err && (
          <div className="inline-alert">
            <AlertTriangle size={17} />
            {err}
          </div>
        )}
        <div className="form-grid">
          <Field label="Institution code" required>
            <input
              required
              maxLength="60"
              value={form.institutionCode}
              onChange={(e) => set("institutionCode", e.target.value)}
              placeholder="e.g. AICTE-001"
            />
          </Field>
          <Field label="Institution name" required>
            <input
              required
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="Full registered name"
            />
          </Field>
          <Field label="Contact person">
            <input
              value={form.contactPerson}
              onChange={(e) => set("contactPerson", e.target.value)}
            />
          </Field>
          <Field label="Contact email">
            <input
              type="email"
              value={form.contactEmail}
              onChange={(e) => set("contactEmail", e.target.value)}
            />
          </Field>
          <Field label="Contact phone">
            <input
              value={form.contactPhone}
              onChange={(e) => set("contactPhone", e.target.value)}
            />
          </Field>
          <Field label="District">
            <input
              value={form.district}
              onChange={(e) => set("district", e.target.value)}
            />
          </Field>
          <Field label="State">
            <input
              value={form.state}
              onChange={(e) => set("state", e.target.value)}
            />
          </Field>
          <Field label="Address">
            <textarea
              rows="3"
              value={form.address}
              onChange={(e) => set("address", e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions">
          <Link className="btn ghost" to="/institutions">
            Cancel
          </Link>
          <button disabled={busy} className="btn primary">
            {busy ? (
              "Saving…"
            ) : (
              <>
                Create institution <ArrowRight size={16} />
              </>
            )}
          </button>
        </div>
      </form>
    </>
  );
}
function FdrDetail() {
  const { id } = useParams(),
    [tab, setTab] = useState("Overview"),
    { user } = useAuth(),
    {
      data: f,
      error,
      loading,
      reload,
      setData: setFdr,
    } = useLoad(() => fdrApi.get(id).then((r) => r.data), [id]);
  const { data: docs, reload: reloadDocs, setData: setDocs } = useLoad(
      () => fdrApi.documents(id).then((r) => r.data),
      [id],
    ),
    { data: reviews, setData: setReviews } = useLoad(
      () =>
        user.role === "INSTITUTION"
          ? Promise.resolve([])
          : fdrApi.reviews(id).then((r) => r.data),
      [id, user.role],
    ),
    { data: renewals } = useLoad(
      () => user.role === "VERIFICATION_OFFICIAL"
        ? Promise.resolve([])
        : fdrApi.renewals(id).then((r) => r.data),
      [id, user.role],
    ),
    { data: releases } = useLoad(
      () => user.role === "VERIFICATION_OFFICIAL"
        ? Promise.resolve([])
        : fdrApi.releases(id).then((r) => r.data),
      [id, user.role],
    );
  const refreshDocuments = async () => {
    const response = await fdrApi.documents(id);
    setDocs(response.data);
  };
  const refreshVerification = async () => {
    const [fdrResponse, reviewResponse] = await Promise.all([
      fdrApi.get(id),
      user.role === "INSTITUTION"
        ? Promise.resolve({ data: [] })
        : fdrApi.reviews(id),
    ]);
    setFdr(fdrResponse.data);
    setReviews(reviewResponse.data);
  };
  if (loading)
    return (
      <>
        <Header
          eyebrow="INSTRUMENT REGISTER"
          title="FDR details"
          description="Loading instrument details…"
          back="/fdrs"
        />
        <Loading />
      </>
    );
  if (error)
    return (
      <>
        <Header
          eyebrow="INSTRUMENT REGISTER"
          title="FDR details"
          description="The instrument could not be opened."
          back="/fdrs"
        />
        <ErrorBox error={error} retry={reload} />
      </>
    );
  const days = Math.ceil(
    (new Date(`${f.maturityDate}T00:00:00+05:30`) - new Date()) / 86400000,
  );
  const institutionUser = user.role === "INSTITUTION",
    canUpload = institutionUser || user.role === "ADMIN";
  return (
    <>
      <Header
        eyebrow="INSTRUMENT REGISTER / RECORD"
        title={f.instrumentReference}
        description={`${f.institutionName} · ${f.bankName}${f.branchName ? ` · ${f.branchName}` : ""}`}
        back="/fdrs"
        action={
          <span className="record-id">
            RECORD FDR-{String(f.id).padStart(5, "0")}
          </span>
        }
      />
      <div className="detail-summary">
        <div className="amount-block">
          <span>DEPOSIT AMOUNT</span>
          <b>{money(f.depositAmount)}</b>
          <small>Issue date {date(f.issueDate)}</small>
        </div>
        <div className="detail-stat">
          <span>MATURITY DATE</span>
          <b>{date(f.maturityDate)}</b>
          <small className={days < 0 ? "text-red" : ""}>
            {days < 0
              ? `${Math.abs(days)} days overdue`
              : `${days} days remaining`}
          </small>
        </div>
        <div className="detail-stat">
          <span>ADMINISTRATIVE STATUS</span>
          <Status value={f.status} />
        </div>
        <div className="detail-stat">
          <span>VERIFICATION</span>
          <Status value={f.verificationStatus} />
        </div>
      </div>
      <div className="tabs">
        {[
          "Overview",
          "Documents",
          "Verification",
          ...(user.role === "VERIFICATION_OFFICIAL" ? [] : ["Renewal history", "Release requests"]),
        ].map((t) => (
          <button
            className={tab === t ? "selected" : ""}
            onClick={() => setTab(t)}
            key={t}
          >
            {t}
          </button>
        ))}
      </div>
      {tab === "Overview" && (
        <div className="detail-grid">
          <Panel
            title="Instrument details"
            sub="Record information returned by the registry"
          >
            <div className="detail-fields">
              {[
                ["Institution", f.institutionName],
                ["Institution code", f.institutionCode],
                ["Instrument reference", f.instrumentReference],
                ["Bank", f.bankName],
                ["Branch", f.branchName || "—"],
                ["Deposit amount", money(f.depositAmount)],
                ["Issue date", date(f.issueDate)],
                ["Maturity date", date(f.maturityDate)],
                ["Submitted", date(f.submittedAt)],
                [
                  "Verification remarks",
                  f.verificationRemarks || "No remarks recorded",
                ],
              ].map(([a, b]) => (
                <div key={a}>
                  <small>{a}</small>
                  <b>{b}</b>
                </div>
              ))}
            </div>
          </Panel>
          <Panel
            title="Available actions"
            sub="Actions depend on your role and this instrument's current state"
          >
            <div className="action-stack">
              {canUpload && (
                <button
                  className="action-card"
                  onClick={() => setTab("Documents")}
                >
                  <span className="action-icon blue">
                    <Upload size={18} />
                  </span>
                  <span>
                    <b>Upload supporting document</b>
                    <small>
                      Add FDR copy, deposit confirmation or replacement
                      evidence.
                    </small>
                  </span>
                  <ChevronRight />
                </button>
              )}
              {institutionUser && (
                <>
                  <button
                    className="action-card"
                    onClick={() => setTab("Renewal history")}
                  >
                    <span className="action-icon violet">
                      <RefreshCw size={18} />
                    </span>
                    <span>
                      <b>Submit replacement request</b>
                      <small>
                        Propose replacement instrument details for review.
                      </small>
                    </span>
                    <ChevronRight />
                  </button>
                  <button
                    className="action-card"
                    onClick={() => setTab("Release requests")}
                  >
                    <span className="action-icon amber">
                      <ArrowDownToLine size={18} />
                    </span>
                    <span>
                      <b>Request release / refund</b>
                      <small>
                        Submit this instrument for controlled release review.
                      </small>
                    </span>
                    <ChevronRight />
                  </button>
                </>
              )}
            </div>
          </Panel>
        </div>
      )}
      {tab === "Documents" && (
        <Documents
          fdrId={id}
          docs={docs}
          reload={reloadDocs}
          refreshDocuments={refreshDocuments}
          canUpload={canUpload}
        />
      )}
      {tab === "Verification" && (
        <VerificationDetail
          f={f}
          reviews={reviews}
          refresh={refreshVerification}
          canViewHistory={!institutionUser}
        />
      )}
      {tab === "Renewal history" && (
        <RenewalDetail f={f} rows={renewals} canSubmit={institutionUser} />
      )}
      {tab === "Release requests" && (
        <ReleaseDetail f={f} rows={releases} canSubmit={institutionUser} />
      )}
    </>
  );
}
function Documents({ fdrId, docs, reload, refreshDocuments, canUpload }) {
  const [drag, setDrag] = useState(false),
    [file, setFile] = useState(null),
    [fileInputKey, setFileInputKey] = useState(0),
    [type, setType] = useState("FDR_COPY"),
    [busy, setBusy] = useState(false),
    toast = useToast();
  const submit = async () => {
    if (!file) return;
    const valid = ["application/pdf", "image/png", "image/jpeg"];
    if (!valid.includes(file.type)) {
      toast("Select a PDF, PNG, or JPEG file.", "error");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast("Files must be 10 MB or smaller.", "error");
      return;
    }
    const form = new FormData();
    form.append("file", file);
    form.append("documentType", type);
    setBusy(true);
    try {
      await documentApi.upload(fdrId, form);
      setFile(null);
      setFileInputKey((key) => key + 1);
      try {
        await refreshDocuments();
        toast("Document uploaded and list refreshed");
      } catch {
        reload();
        toast(
          "Document uploaded, but the list could not refresh. Reopen this record to see the latest documents.",
          "info",
        );
      }
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(false);
    }
  };
  const download = async (d) => {
    try {
      const r = await documentApi.download(fdrId, d.id);
      const url = URL.createObjectURL(r.data),
        a = document.createElement("a");
      a.href = url;
      a.download = d.originalFilename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toast(message(e), "error");
    }
  };
  return (
    <div className="detail-grid">
      <Panel
        title="Supporting documents"
        sub="Files are stored and retrieved through the authenticated document service"
      >
        {docs?.length ? (
          <div className="document-list">
            {docs.map((d) => (
              <div className="document-row" key={d.id}>
                <span className="doc-icon">
                  <FileText />
                </span>
                <div className="doc-info">
                  <b>{d.originalFilename}</b>
                  <small>
                    {d.documentType.replaceAll("_", " ")} ·{" "}
                    {(d.sizeBytes / 1024).toFixed(0)} KB · {date(d.uploadedAt)}
                  </small>
                  <small className="hash">SHA-256 · {d.sha256}</small>
                </div>
                <button
                  className="icon-btn"
                  onClick={() => download(d)}
                  title="Download document"
                >
                  <Download size={17} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <Empty
            icon={FileText}
            title="No supporting documents"
            text="Documents attached to this instrument will be listed here."
          />
        )}
      </Panel>
      <Panel title="Upload document" sub="PDF, PNG or JPEG · up to 10 MB">
        {canUpload ? (
          <>
            <div
              className={`dropzone ${drag ? "dragging" : ""}`}
              onDragOver={(e) => {
                e.preventDefault();
                setDrag(true);
              }}
              onDragLeave={() => setDrag(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDrag(false);
                setFile(e.dataTransfer.files[0] || null);
              }}
            >
              <input
                key={fileInputKey}
                type="file"
                accept="application/pdf,image/png,image/jpeg"
                onChange={(e) => setFile(e.target.files[0] || null)}
              />
              <span className="upload-circle">
                <Upload />
              </span>
              <b>{file?.name || "Drop a file here, or browse"}</b>
              <small>
                {file
                  ? `${(file.size / 1024).toFixed(0)} KB selected`
                  : "PDF, PNG or JPEG up to 10 MB"}
              </small>
            </div>
            <Field label="Document category">
              <select value={type} onChange={(e) => setType(e.target.value)}>
                {[
                  "FDR_COPY",
                  "DEPOSIT_CONFIRMATION",
                  "SUPPORTING_DOCUMENT",
                  "RENEWAL_REPLACEMENT",
                ].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            <button
              className="btn primary full upload-btn"
              disabled={!file || busy}
              onClick={submit}
            >
              {busy ? (
                "Uploading…"
              ) : (
                <>
                  <Upload size={16} /> Upload document
                </>
              )}
            </button>
            <p className="secure-note">
              <LockKeyhole size={14} /> Files are private and require an
              authenticated session to download.
            </p>
          </>
        ) : (
          <Empty
            icon={Shield}
            title="Upload restricted"
            text="Only the linked institution or an administrator can add documents."
          />
        )}
      </Panel>
    </div>
  );
}
function VerificationDetail({ f, reviews, refresh, canViewHistory = true }) {
  const { user } = useAuth(),
    toast = useToast(),
    [form, setForm] = useState({
      outcome: "UNDER_REVIEW",
      verificationMethod: "OFFICIAL_DOCUMENT_REVIEW",
      sourceReference: "",
      remarks: "",
    }),
    [busy, setBusy] = useState(false);
  const can = [
    "ADMIN",
    "VERIFICATION_OFFICIAL",
    "APPROVING_AUTHORITY",
    "REGULATION_OFFICIAL",
  ].includes(user.role);
  const submit = async (e) => {
    e.preventDefault();
    if (!form.remarks.trim()) {
      toast("Remarks are required for each verification review.", "error");
      return;
    }
    setBusy(true);
    try {
      await verificationApi.submitReview(f.id, form);
      // Reset the review form immediately after a successful save, even if
      // the follow-up GET requests fail.
      setForm({
        outcome: "UNDER_REVIEW",
        verificationMethod: "OFFICIAL_DOCUMENT_REVIEW",
        sourceReference: "",
        remarks: "",
      });
      try {
        await refresh();
        toast("Verification review recorded and details refreshed");
      } catch {
        toast(
          "Review recorded, form reset, but this view could not refresh. Reopen the FDR to see the latest status and review history.",
          "info",
        );
      }
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="detail-grid">
      <Panel
        title="Verification history"
        sub="Internal review and independent confirmation are separate stages"
      >
        {!canViewHistory ? (
          <Empty
            icon={Shield}
            title="Internal review history"
            text="Detailed reviewer notes are restricted to authorized regulatory roles. The current verification status and summary remarks remain visible in the instrument overview."
          />
        ) : reviews?.length ? (
          <Timeline rows={reviews} />
        ) : (
          <Empty
            icon={FileCheck2}
            title="No verification reviews"
            text="Review outcomes and remarks will be recorded here."
          />
        )}
      </Panel>
      <Panel
        title="Record an outcome"
        sub="The backend identifies the authenticated reviewer"
      >
        {can ? (
          <form className="compact-form" onSubmit={submit}>
            <Field label="Verification outcome" required>
              <select
                value={form.outcome}
                onChange={(e) => setForm({ ...form, outcome: e.target.value })}
              >
                {[
                  "UNDER_REVIEW",
                  "DISCREPANCY_FOUND",
                  "VERIFIED_BY_OFFICIAL",
                  "EXTERNAL_CONFIRMATION_PENDING",
                  "EXTERNALLY_CONFIRMED",
                ].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            <Field label="Verification method" required>
              <select
                value={form.verificationMethod}
                onChange={(e) =>
                  setForm({ ...form, verificationMethod: e.target.value })
                }
              >
                {[
                  "OFFICIAL_DOCUMENT_REVIEW",
                  "BANK_ISSUED_CONFIRMATION",
                  "OTHER_AUTHORIZED_SOURCE",
                ].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            <Field label="Source reference">
              <input
                value={form.sourceReference}
                onChange={(e) =>
                  setForm({ ...form, sourceReference: e.target.value })
                }
                placeholder="Bank letter / case reference"
              />
            </Field>
            <Field label="Remarks" required>
              <textarea
                required
                rows="4"
                value={form.remarks}
                onChange={(e) => setForm({ ...form, remarks: e.target.value })}
                placeholder="Document findings and verification notes"
              />
            </Field>
            <div className="form-note">
              <AlertTriangle size={16} /> Document scrutiny does not represent
              independent bank confirmation.
            </div>
            <button className="btn primary full" disabled={busy}>
              {busy ? "Saving review…" : "Record review"}
            </button>
          </form>
        ) : (
          <Empty
            icon={Shield}
            title="Review restricted"
            text="Your role cannot record verification outcomes."
          />
        )}
      </Panel>
    </div>
  );
}
function Timeline({ rows }) {
  return (
    <div className="timeline">
      {rows.map((r, i) => (
        <div className="timeline-item" key={r.id || i}>
          <span className="timeline-dot" />
          <div>
            <div className="timeline-title">
              <b>{r.outcome || r.targetStatus || r.status}</b>
              <small>
                {date(r.createdAt || r.reviewedAt || r.submittedAt)}
              </small>
            </div>
            <p>{r.remarks || "No remarks supplied."}</p>
            {r.verificationMethod && (
              <small>
                {r.verificationMethod.replaceAll("_", " ")}
                {r.sourceReference ? ` · ${r.sourceReference}` : ""}
              </small>
            )}
            {r.reviewerEmail && <small>Reviewer · {r.reviewerEmail}</small>}
          </div>
        </div>
      ))}
    </div>
  );
}
function RenewalDetail({ f, rows, canSubmit }) {
  const toast = useToast(),
    [open, setOpen] = useState(false),
    [busy, setBusy] = useState(false),
    [form, setForm] = useState({
      instrumentReference: "",
      bankName: "",
      branchName: "",
      depositAmount: f.depositAmount,
      issueDate: "",
      maturityDate: "",
      remarks: "",
    });
  const submit = async (e) => {
    e.preventDefault();
    if (form.maturityDate <= form.issueDate) {
      toast("Maturity date must follow issue date.", "error");
      return;
    }
    setBusy(true);
    try {
      await workflowApi.createRenewal(f.id, {
        ...form,
        depositAmount: +form.depositAmount,
      });
      toast("Replacement request submitted");
      setOpen(false);
      location.reload();
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="detail-grid">
      <Panel
        title="Renewal & replacement history"
        sub="Request approval is separate from verification of the replacement instrument"
      >
        {rows?.length ? (
          <Timeline rows={rows} />
        ) : (
          <Empty
            icon={RefreshCw}
            title="No replacement requests"
            text="Submitted replacement instrument proposals will appear here."
          />
        )}
      </Panel>
      <Panel
        title="Replacement request"
        sub="Submit proposed details for authorized review"
      >
        {canSubmit ? (
          !open ? (
            <div className="action-stack">
              <div className="callout violet">
                <RefreshCw />
                <div>
                  <b>Propose a replacement instrument</b>
                  <p>
                    Provide the replacement FDR details. Attach the evidence
                    under the Documents tab using RENEWAL_REPLACEMENT.
                  </p>
                </div>
              </div>
              <Button onClick={() => setOpen(true)}>
                <Plus size={16} /> Submit replacement request
              </Button>
            </div>
          ) : (
            <form className="compact-form" onSubmit={submit}>
              {[
                ["instrumentReference", "Replacement reference", "text"],
                ["bankName", "Bank name", "text"],
                ["branchName", "Branch", "text"],
                ["depositAmount", "Deposit amount (₹)", "number"],
                ["issueDate", "Issue date", "date"],
                ["maturityDate", "Maturity date", "date"],
              ].map(([k, l, t]) => (
                <Field key={k} label={l} required={k !== "branchName"}>
                  <input
                    type={t}
                    required={k !== "branchName"}
                    min={k === "maturityDate" ? form.issueDate : undefined}
                    value={form[k]}
                    onChange={(e) => setForm({ ...form, [k]: e.target.value })}
                  />
                </Field>
              ))}
              <Field label="Remarks">
                <textarea
                  value={form.remarks}
                  onChange={(e) =>
                    setForm({ ...form, remarks: e.target.value })
                  }
                />
              </Field>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => setOpen(false)}
                >
                  Cancel
                </button>
                <button className="btn primary" disabled={busy}>
                  {busy ? "Submitting…" : "Submit request"}
                </button>
              </div>
            </form>
          )
        ) : (
          <Empty
            icon={Shield}
            title="Request restricted"
            text="Only the institution account linked to this record can submit a replacement request."
          />
        )}
      </Panel>
    </div>
  );
}
function ReleaseDetail({ f, rows, canSubmit }) {
  const toast = useToast(),
    [open, setOpen] = useState(false),
    [reason, setReason] = useState(""),
    [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    if (!reason.trim()) return;
    setBusy(true);
    try {
      await workflowApi.createRelease(f.id, { reason });
      toast("Release request submitted");
      setOpen(false);
      location.reload();
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="detail-grid">
      <Panel
        title="Release & refund history"
        sub="Approval is not evidence that funds have been released"
      >
        {rows?.length ? (
          <Timeline rows={rows} />
        ) : (
          <Empty
            icon={ArrowDownToLine}
            title="No release requests"
            text="Requests and subsequent decisions will appear here."
          />
        )}
      </Panel>
      <Panel
        title="Request release / refund"
        sub="Submit a reason for the competent authority to review"
      >
        {canSubmit ? (
          open ? (
            <form className="compact-form" onSubmit={submit}>
              <Field label="Request reason" required>
                <textarea
                  required
                  rows="5"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Explain why release or refund is requested"
                />
              </Field>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn ghost"
                  onClick={() => setOpen(false)}
                >
                  Cancel
                </button>
                <button className="btn primary" disabled={busy}>
                  {busy ? "Submitting…" : "Submit request"}
                </button>
              </div>
            </form>
          ) : (
            <div className="action-stack">
              <div className="callout amber">
                <AlertTriangle />
                <div>
                  <b>Controlled release workflow</b>
                  <p>
                    An authorized official reviews the request; completion is
                    recorded separately after the release process occurs.
                  </p>
                </div>
              </div>
              <Button onClick={() => setOpen(true)}>
                <Plus size={16} /> Request release
              </Button>
            </div>
          )
        ) : (
          <Empty
            icon={Shield}
            title="Request restricted"
            text="Only the institution account linked to this record can submit a request."
          />
        )}
      </Panel>
    </div>
  );
}
function Monitoring() {
  const [days, setDays] = useState(30),
    {
      data: m,
      error,
      loading,
      reload,
    } = useLoad(
      () => monitoringApi.maturities(days).then((r) => r.data),
      [days],
    ),
    {
      data: notes,
      error: ne,
      reload: nr,
    } = useLoad(
      () =>
        api
          .get("/monitoring/notifications", { params: { unreadOnly: false } })
          .then((r) => r.data),
      [],
    );
  return (
    <>
      <Header
        eyebrow="PORTFOLIO OVERSIGHT"
        title="Maturity & alerts"
        description="Review upcoming deadlines, matured instruments and monitoring notifications."
        action={
          <div className="select-wrap">
            <CalendarDays size={16} />
            <select value={days} onChange={(e) => setDays(+e.target.value)}>
              <option value={0}>Matured only</option>
              <option value={7}>Next 7 days</option>
              <option value={30}>Next 30 days</option>
              <option value={90}>Next 90 days</option>
              <option value={365}>Next 12 months</option>
            </select>
            <ChevronDown size={14} />
          </div>
        }
      />
      <Panel
        title="Maturity tracker"
        sub={`Backend-calculated maturity window · ${days} days`}
        className="table-panel"
      >
        {error ? (
          <ErrorBox error={error} retry={reload} />
        ) : loading ? (
          <Loading />
        ) : (
          <MaturityTable rows={m} />
        )}
      </Panel>
      <div className="section-gap" />
      <Panel
        title="Notification history"
        sub="Notifications returned by the monitoring service"
      >
        {ne ? (
          <ErrorBox error={ne} retry={nr} />
        ) : (
          <NotificationList rows={notes} />
        )}
      </Panel>
    </>
  );
}
function Verification() {
  const [page, setPage] = useState(0),
    [q, setQ] = useState(""),
    [status, setStatus] = useState("");
  const { data, error, loading, reload } = usePage("/fdrs", {
    page,
    size: 30,
    sort: "submittedAt,desc",
  });
  const rows = (data?.content || [])
    .filter(
      (x) =>
        (!status || x.verificationStatus === status) &&
        `${x.instrumentReference} ${x.institutionName} ${x.bankName}`
          .toLowerCase()
          .includes(q.toLowerCase()),
    )
    .filter(
      (x) =>
        [
          "PENDING",
          "UNDER_REVIEW",
          "DISCREPANCY_FOUND",
          "EXTERNAL_CONFIRMATION_PENDING",
        ].includes(x.verificationStatus) ||
        ["SUBMITTED", "ACTION_REQUIRED", "UNDER_REVIEW"].includes(x.status),
    );
  return (
    <>
      <Header
        eyebrow="OFFICIAL WORKSPACE"
        title="Verification desk"
        description="Review submitted instruments and document evidence; record outcomes with traceable remarks."
      />
      <div className="filter-bar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search instrument or institution"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <div className="filter-select">
          <SlidersHorizontal size={15} />
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All review states</option>
            {[
              "PENDING",
              "UNDER_REVIEW",
              "DISCREPANCY_FOUND",
              "EXTERNAL_CONFIRMATION_PENDING",
            ].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </div>
      </div>
      <Panel
        title="Cases requiring scrutiny"
        sub="Filtered from the live FDR register by administrative and verification status"
        className="table-panel"
      >
        {error ? (
          <ErrorBox error={error} retry={reload} />
        ) : loading ? (
          <Loading />
        ) : rows.length ? (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>INSTRUMENT</th>
                  <th>INSTITUTION</th>
                  <th>BANK</th>
                  <th>AMOUNT</th>
                  <th>ADMIN STATUS</th>
                  <th>VERIFICATION STATUS</th>
                  <th>MATURITY</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((x) => (
                  <tr key={x.id}>
                    <td>
                      <Link className="strong-link" to={`/fdrs/${x.id}`}>
                        {x.instrumentReference}
                      </Link>
                      <small>Submitted {date(x.submittedAt)}</small>
                    </td>
                    <td>
                      {x.institutionName}
                      <small>{x.institutionCode}</small>
                    </td>
                    <td>{x.bankName}</td>
                    <td className="amount">{money(x.depositAmount)}</td>
                    <td>
                      <Status value={x.status} />
                    </td>
                    <td>
                      <Status value={x.verificationStatus} />
                    </td>
                    <td>{date(x.maturityDate)}</td>
                    <td>
                      <Link
                        className="btn small-btn outline"
                        to={`/fdrs/${x.id}`}
                      >
                        Review
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            icon={CheckCheck}
            title="No cases require scrutiny"
            text="There are no submitted, pending or discrepancy cases in the loaded results."
          />
        )}
      </Panel>
    </>
  );
}
function Workflow({ kind }) {
  const renewal = kind === "renewal",
    base = "/fdrs",
    roles = useAuth(),
    toast = useToast(),
    [rows, setRows] = useState(null),
    [error, setError] = useState(null),
    [loading, setLoading] = useState(true),
    [q, setQ] = useState(""),
    [busy, setBusy] = useState(null),
    [modal, setModal] = useState(null);
  const { user } = roles;
  const approvalQueue = user.role === "APPROVING_AUTHORITY" || user.role === "ADMIN";
  const refresh = () => {
    setLoading(true);
    const request = approvalQueue
      ? api.get(renewal ? "/renewals" : "/release-requests").then((r) =>
          (r.data || []).map((y) => ({
            ...y,
            fdrId: renewal ? y.originalFdrId : y.fdrId,
            reference: renewal ? y.originalReference : y.instrumentReference,
            institution: y.institutionName || "Institution",
            amount: renewal ? y.replacementDepositAmount : y.depositAmount,
            maturityDate: renewal ? y.replacementMaturityDate : y.maturityDate,
            submittedAt: y.createdAt || y.requestedAt,
            reason: renewal ? y.remarks : y.reason,
          })),
        )
      : api
          .get(base, { params: { size: 100, sort: "submittedAt,desc" } })
          .then(async (r) => {
            const fs = r.data.content || [];
            const arr = [];
            for (let i = 0; i < fs.length; i += 5) {
              const group = await Promise.all(
                fs.slice(i, i + 5).map(async (f) => {
                  try {
                    const x = await (renewal
                      ? workflowApi.listRenewals(f.id)
                      : workflowApi.listReleases(f.id));
                    return (x.data || []).map((y) => ({
                      ...y,
                      fdrId: f.id,
                      reference: f.instrumentReference,
                      institution: f.institutionName,
                      amount: f.depositAmount,
                      maturityDate: f.maturityDate,
                    }));
                  } catch (e) {
                    if (e.response?.status === 403) return [];
                    throw e;
                  }
                }),
              );
              arr.push(...group.flat());
            }
            return arr;
          });
    request
      .then((arr) => {
        setRows(arr);
        setError(null);
      })
      .catch((e) => setError(e))
      .finally(() => setLoading(false));
  };
  useEffect(() => {
    refresh();
  }, [kind]);
  const authReview = ["ADMIN", "APPROVING_AUTHORITY"].includes(user.role);
  const review = async (action) => {
    setBusy(modal.item.id);
    try {
      const body = renewal
        ? { targetStatus: action, remarks: modal.remarks }
        : {
            targetStatus: action,
            remarks: modal.remarks,
            ...(action === "COMPLETED"
              ? { completionReference: modal.ref }
              : {}),
          };
      await workflowApi.review(
        renewal ? "renewals" : "release-requests",
        modal.item.id,
        body,
      );
      toast(`${renewal ? "Renewal" : "Release"} request updated`);
      setModal(null);
      refresh();
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setBusy(null);
    }
  };
  const filtered = (rows || []).filter((r) =>
    `${r.reference} ${r.institution} ${r.instrumentReference || ""}`
      .toLowerCase()
      .includes(q.toLowerCase()),
  );
  return (
    <>
      <Header
        eyebrow="WORKFLOW QUEUE"
        title={renewal ? "Renewal management" : "Release & refund"}
        description={
          renewal
            ? "Track replacement instrument requests through submission, review and approval."
            : "Review release requests and record actual completion separately from approval."
        }
      />
      <div className="workflow-callout">
        <ShieldCheck />
        <span>
          {renewal
            ? "Approval of a replacement request does not verify the replacement financial instrument."
            : "An approved release request is not proof that funds have been released. Completion must be recorded after the process."}
        </span>
      </div>
      <div className="filter-bar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search request or institution"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <span className="records-note">
          {rows?.length ?? "—"} requests · {approvalQueue ? "request-only approval queue" : "collected from FDR histories"}
        </span>
      </div>
      <Panel
        title={renewal ? "Replacement requests" : "Release requests"}
        sub={approvalQueue ? "Request details are scoped to renewal and release decisions." : "Live records aggregated from authorized FDR request histories"}
        className="table-panel"
      >
        {error ? (
          <ErrorBox error={error} retry={refresh} />
        ) : loading ? (
          <Loading />
        ) : filtered.length ? (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>REQUEST</th>
                  <th>INSTITUTION / FDR</th>
                  <th>DEPOSIT</th>
                  <th>MATURITY</th>
                  <th>SUBMITTED</th>
                  <th>STATUS</th>
                  <th>REMARKS</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <b>#{r.id}</b>
                      <small>
                        {renewal ? "Replacement" : "Release / refund"}
                      </small>
                    </td>
                    <td>
                      {approvalQueue ? (
                        <span className="strong-link">{r.reference || r.originalReference || r.instrumentReference}</span>
                      ) : (
                        <Link className="strong-link" to={`/fdrs/${r.fdrId}`}>
                          {r.reference}
                        </Link>
                      )}
                      <small>{r.institution}</small>
                      {renewal && r.replacementReference && (
                        <small>
                          Replacement · {r.replacementReference}
                          {r.replacementBankName ? ` · ${r.replacementBankName}` : ""}
                          {r.replacementBranchName ? ` · ${r.replacementBranchName}` : ""}
                        </small>
                      )}
                    </td>
                    <td className="amount">{money(r.amount)}</td>
                    <td>{date(r.maturityDate)}</td>
                    <td>{date(r.submittedAt || r.createdAt)}</td>
                    <td>
                      <Status value={r.status} />
                    </td>
                    <td className="remarks-cell">
                      {r.remarks || r.reason || "—"}
                    </td>
                    <td>
                      {authReview &&
                      actionFor(r.status, renewal, user.role).length ? (
                        <button
                          className="btn small-btn outline"
                          onClick={() =>
                            setModal({
                              item: r,
                              actions: actionFor(r.status, renewal, user.role),
                              remarks: "",
                              ref: "",
                            })
                          }
                        >
                          Review
                        </button>
                      ) : approvalQueue ? (
                        <span className="muted">No action available</span>
                      ) : (
                        <Link className="text-btn" to={`/fdrs/${r.fdrId}`}>
                          Open <ArrowRight size={13} />
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            icon={renewal ? RefreshCw : ArrowDownToLine}
            title={`No ${renewal ? "renewal" : "release"} requests found`}
            text="Requests will appear here after an institution submits them."
          />
        )}
      </Panel>
      {modal && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setModal(null)}
        >
          <div className="modal">
            <button className="modal-x icon-btn" onClick={() => setModal(null)}>
              <X />
            </button>
            <div className="eyebrow">REQUEST #{modal.item.id}</div>
            <h2>
              {renewal
                ? "Review replacement request"
                : "Review release request"}
            </h2>
            <p>
              {modal.item.reference} · {modal.item.institution}
            </p>
            <div className="modal-current">
              <small>Current state</small>
              <Status value={modal.item.status} />
            </div>
            <Field label="Review remarks" required>
              <textarea
                rows="4"
                value={modal.remarks}
                onChange={(e) =>
                  setModal({ ...modal, remarks: e.target.value })
                }
                placeholder="Explain the decision or next step"
              />
            </Field>
            {modal.actions.includes("COMPLETED") && (
              <Field label="Completion reference" required>
                <input
                  value={modal.ref}
                  onChange={(e) => setModal({ ...modal, ref: e.target.value })}
                  placeholder="Release reference / completion record"
                />
              </Field>
            )}
            <div className="modal-actions">
              {modal.actions.map((a) => (
                <button
                  key={a}
                  disabled={
                    busy === modal.item.id ||
                    !modal.remarks.trim() ||
                    (a === "COMPLETED" && !modal.ref.trim())
                  }
                  className={`btn ${a === "REJECTED" ? "danger" : "primary"}`}
                  onClick={() => review(a)}
                >
                  {busy === modal.item.id ? "Saving…" : a.replaceAll("_", " ")}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
function actionFor(status, renewal, role) {
  const approver = role === "ADMIN" || role === "APPROVING_AUTHORITY";
  if (!approver) return [];
  if (status === "SUBMITTED") return ["UNDER_REVIEW"];
  if (status === "UNDER_REVIEW") return ["APPROVED", "REJECTED"];
  if (!renewal && status === "APPROVED") return ["COMPLETED"];
  return [];
}
function UsersPage() {
  const [insts, setInsts] = useState([]),
    toast = useToast(),
    [err, setErr] = useState(""),
    [busy, setBusy] = useState(false),
    [done, setDone] = useState(null),
    [form, setForm] = useState({
      email: "",
      password: "",
      role: "INSTITUTION",
      institutionId: "",
    });
  useEffect(() => {
    api
      .get("/institutions", { params: { size: 100 } })
      .then((r) => setInsts(r.data.content || []))
      .catch(() => {});
  }, []);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const { data } = await authApi.createUser({
        ...form,
        institutionId: form.role === "INSTITUTION" ? +form.institutionId : null,
      });
      setDone(data);
      toast("Authorized account created");
      setForm({
        email: "",
        password: "",
        role: "INSTITUTION",
        institutionId: "",
      });
    } catch (e) {
      setErr(message(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <Header
        eyebrow="ACCESS CONTROL"
        title="User management"
        description="Provision authorized accounts and assign the appropriate workflow role."
      />
      <div className="admin-grid">
        <Panel
          title="Create an authorized account"
          sub="New credentials are never displayed again after creation"
        >
          {err && (
            <div className="inline-alert">
              <AlertTriangle size={17} />
              {err}
            </div>
          )}
          {done && (
            <div className="success-card">
              <CheckCircleIcon />
              <div>
                <b>Account created</b>
                <p>
                  {done.email} · {roleLabel(done.role)}
                </p>
              </div>
              <button className="icon-btn" onClick={() => setDone(null)}>
                <X size={16} />
              </button>
            </div>
          )}
          <form className="compact-form" onSubmit={submit}>
            <Field label="Email address" required>
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="name@organization.gov.in"
              />
            </Field>
            <Field
              label="Initial password"
              required
              hint="At least 12 characters. Share through an approved secure channel."
            >
              <input
                required
                minLength="12"
                maxLength="72"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </Field>
            <Field label="Assigned role" required>
              <select
                value={form.role}
                onChange={(e) =>
                  setForm({ ...form, role: e.target.value, institutionId: "" })
                }
              >
                {[
                  "INSTITUTION",
                  "VERIFICATION_OFFICIAL",
                  "APPROVING_AUTHORITY",
                  "REGULATION_OFFICIAL",
                ].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            {form.role === "INSTITUTION" && (
              <Field label="Linked institution" required>
                <select
                  required
                  value={form.institutionId}
                  onChange={(e) =>
                    setForm({ ...form, institutionId: e.target.value })
                  }
                >
                  <option value="">Select institution</option>
                  {insts.map((x) => (
                    <option value={x.id} key={x.id}>
                      {x.institutionCode} · {x.name}
                    </option>
                  ))}
                </select>
              </Field>
            )}
            <button className="btn primary full" disabled={busy}>
              {busy ? (
                "Creating account…"
              ) : (
                <>
                  <Plus size={16} /> Create account
                </>
              )}
            </button>
          </form>
        </Panel>
        <Panel
          title="Role reference"
          sub="Access is enforced by backend security rules"
        >
          <div className="role-list">
            {[
              [
                "ADMIN",
                "Manage accounts, institution records and all workflows",
              ],
              [
                "INSTITUTION",
                "Manage records linked to the assigned institution",
              ],
              ["VERIFICATION_OFFICIAL", "Review documents and submitted FDRs"],
              ["APPROVING_AUTHORITY", "Approve workflow requests"],
              ["REGULATION_OFFICIAL", "Monitor compliance and review records"],
            ].map(([r, desc]) => (
              <div key={r}>
                <span className="role-avatar">
                  <Users size={16} />
                </span>
                <span>
                  <b>{roleLabel(r)}</b>
                  <small>{desc}</small>
                </span>
              </div>
            ))}
          </div>
          <div className="form-note">
            <Shield size={16} /> Role checks improve navigation. Spring Security
            remains authoritative for permissions.
          </div>
        </Panel>
      </div>
    </>
  );
}
function CheckCircleIcon() {
  return (
    <span className="success-round">
      <Check size={17} />
    </span>
  );
}
function Reports() {
  const year = new Date().getFullYear();
  const [from, setFrom] = useState(`${year}-01-01`);
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [exporting, setExporting] = useState(false);
  const [auditPageNumber, setAuditPageNumber] = useState(0);
  const toast = useToast();
  const {
    data: summary,
    error,
    loading,
    reload,
  } = useLoad(
    () => api.get("/reports/summary", { params: { from, to } }).then((r) => r.data),
    [from, to],
  );
  const {
    data: auditPage,
    error: auditError,
    loading: auditLoading,
    reload: reloadAudit,
  } = useLoad(
    () => api.get("/audit-events", {
      params: { from, to, page: auditPageNumber, size: 12, sort: "occurredAt,desc" },
    }).then((r) => r.data),
    [from, to, auditPageNumber],
  );

  const exportCsv = async () => {
    setExporting(true);
    try {
      const response = await api.get("/reports/fdrs.csv", {
        params: { from, to },
        responseType: "blob",
      });
      const url = window.URL.createObjectURL(
        new Blob([response.data], { type: "text/csv;charset=utf-8;" }),
      );
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `fdr-report-${from}-to-${to}.csv`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(url);
      toast("CSV report downloaded");
    } catch (e) {
      toast(message(e), "error");
    } finally {
      setExporting(false);
    }
  };

  const cards = summary
    ? [
        ["Records submitted", summary.recordsSubmittedInPeriod, ClipboardList, "blue"],
        ["Submitted deposit total", money(summary.submittedDepositTotal), WalletCards, "green"],
        ["Pending verification", summary.pendingVerifications, FileCheck2, "violet"],
        ["Discrepancy cases", summary.discrepancyCases, AlertTriangle, "red"],
        ["Active records", summary.activeRecords, ShieldCheck, "green"],
        ["Renewal pending", summary.renewalPendingRecords, RefreshCw, "violet"],
        ["Release requested", summary.releaseRequestedRecords, ArrowDownToLine, "amber"],
        ["Maturing in period", summary.maturityDatesInPeriod, CalendarDays, "teal"],
      ]
    : [];

  return (
    <>
      <Header
        eyebrow="GOVERNANCE"
        title="Reports & audit trail"
        description="Review FDR submissions for a date range, export the register, and inspect recorded administrative activity."
        action={
          <button
            className="btn primary"
            onClick={exportCsv}
            disabled={exporting || !from || !to || from > to}
          >
            <Download size={16} />
            {exporting ? "Exporting…" : "Export FDR CSV"}
          </button>
        }
      />
      <div className="filter-bar report-filter-bar">
        <label className="report-date-field">
          <span>From date</span>
          <input type="date" value={from} max={to || undefined} onChange={(e) => { setFrom(e.target.value); setAuditPageNumber(0); }} />
        </label>
        <label className="report-date-field">
          <span>To date</span>
          <input type="date" value={to} min={from || undefined} onChange={(e) => { setTo(e.target.value); setAuditPageNumber(0); }} />
        </label>
        <button className="btn ghost" onClick={() => { reload(); reloadAudit(); }} disabled={loading || auditLoading}>
          <RefreshCw size={15} /> Refresh data
        </button>
      </div>

      {error ? (
        <ErrorBox error={error} retry={reload} />
      ) : (
        <>
          <Panel
            title="Submission-period summary"
            sub={`Records submitted from ${from} through ${to}. Status counts reflect each record's current state.`}
          >
            {loading ? (
              <Loading />
            ) : summary ? (
              <>
                <div className="metric-grid">
                  {cards.map(([label, value, Icon, tone]) => (
                    <div className="metric-card" key={label}>
                      <div className={`metric-icon ${tone}`}><Icon size={18} /></div>
                      <span>{label}</span>
                      <b>{value ?? 0}</b>
                      <small>{label === "Submitted deposit total" ? "Sum of submitted FDR amounts" : "Live backend calculation"}</small>
                    </div>
                  ))}
                </div>
                <div className="report-status-table table-scroll">
                  <table>
                    <thead>
                      <tr><th>Current status</th><th>Records in selected submission period</th></tr>
                    </thead>
                    <tbody>
                      {Object.entries(summary.currentStatusBreakdown || {}).map(([status, count]) => (
                        <tr key={status}>
                          <td><Status value={status} /></td>
                          <td>{count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : (
              <Empty icon={ClipboardList} title="No summary available" text="Choose a valid date range and refresh the report." />
            )}
          </Panel>

          <Panel
            title="Recent audit events"
            sub="Audit entries are recorded for successful workflow actions and handled API failures."
          >
            {auditError ? (
              <ErrorBox error={auditError} retry={reloadAudit} />
            ) : auditLoading ? (
              <Loading />
            ) : auditPage?.content?.length ? (
              <>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>WHEN</th>
                        <th>ACTOR</th>
                        <th>ACTION</th>
                        <th>TARGET</th>
                        <th>RESULT</th>
                        <th>DETAILS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auditPage.content.map((event) => (
                        <tr key={event.id}>
                          <td>{new Date(event.occurredAt).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" })}</td>
                          <td>{event.actorEmail}<small>{roleLabel(event.actorRole)}</small></td>
                          <td><b>{event.action}</b></td>
                          <td>{event.targetType}{event.targetId ? <small>#{event.targetId}</small> : null}</td>
                          <td><Status value={event.outcome} /></td>
                          <td>{event.details || "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <PageControls
                  page={auditPage.number || 0}
                  totalPages={auditPage.totalPages || 1}
                  onPage={setAuditPageNumber}
                />
              </>
            ) : (
              <Empty icon={ClipboardList} title="No audit events in this range" text="Workflow actions and rejected requests will appear here when recorded." />
            )}
          </Panel>
        </>
      )}
    </>
  );
}
export default PortalApplication;
