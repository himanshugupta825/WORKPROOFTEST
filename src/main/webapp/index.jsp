<%--
  WorkProof - public landing page (served as the welcome file for /WorkProof/).

  It is intentionally the only JSP outside WEB-INF/views: it is the public entry
  point with no data access at all. Every other page lives under WEB-INF so that it
  can only be reached through a servlet that performed the permission checks.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="wp" uri="http://workproof.com/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>WorkProof - Work Management and Proof-of-Work Verification</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/bootstrap-icons/bootstrap-icons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/workproof.css">
    <link rel="icon" href="${pageContext.request.contextPath}/assets/img/favicon.svg" type="image/svg+xml">
</head>
<body>

<!-- ============================ navigation ============================ -->
<nav class="navbar navbar-expand-lg navbar-dark" style="background:#0f1d33;">
    <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="${pageContext.request.contextPath}/">
            <span class="wp-logo d-inline-grid" style="width:32px;height:32px;border-radius:8px;
                  background:linear-gradient(135deg,#2563eb,#0ea5e9);place-items:center;">
                <i class="bi bi-patch-check-fill text-white"></i>
            </span>
            WorkProof
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#landingNav"
                aria-controls="landingNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="landingNav">
            <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                <li class="nav-item"><a class="nav-link" href="#features">Features</a></li>
                <li class="nav-item"><a class="nav-link" href="#workflow">How it works</a></li>
                <c:choose>
                    <c:when test="${not empty sessionScope.currentUser}">
                        <li class="nav-item">
                            <a class="btn btn-primary btn-sm px-3"
                               href="${pageContext.request.contextPath}/dashboard">
                                <i class="bi bi-speedometer2 me-1"></i>Go to dashboard
                            </a>
                        </li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item">
                            <a class="nav-link" href="${pageContext.request.contextPath}/login">Sign in</a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-primary btn-sm px-3"
                               href="${pageContext.request.contextPath}/register">Create account</a>
                        </li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </div>
    </div>
</nav>

<!-- ============================== hero ============================== -->
<section class="wp-hero">
    <div class="container">
        <div class="row align-items-center g-5">
            <div class="col-lg-6">
                <span class="wp-badge badge-status-primary mb-3">Academic PBL Project &middot; Java 17 &middot; Servlets &middot; MySQL</span>
                <h1 class="display-5 fw-bold mb-3">Manage the work.<br>Prove the work.</h1>
                <p class="lead mb-4">
                    WorkProof keeps team projects transparent. Managers assign tasks, members upload
                    real evidence, reviewers approve or reject it with written feedback, and every
                    action is stored in the database with the person and time that performed it.
                </p>
                <div class="d-flex flex-wrap gap-2">
                    <a class="btn btn-primary btn-lg px-4" href="${pageContext.request.contextPath}/register">
                        <i class="bi bi-person-plus me-2"></i>Get started
                    </a>
                    <a class="btn btn-outline-light btn-lg px-4" href="${pageContext.request.contextPath}/login">
                        <i class="bi bi-box-arrow-in-right me-2"></i>Sign in
                    </a>
                </div>
                <p class="small mt-3 mb-0" style="color:#9fb3d1;">
                    Demo accounts are provided in <code>database/seed.sql</code> after you import it.
                </p>
            </div>
            <div class="col-lg-6">
                <div class="wp-card p-4" style="background:rgba(255,255,255,.06);border-color:rgba(255,255,255,.12);">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <strong class="text-white">Project progress</strong>
                        <span class="wp-badge badge-status-info">Live from MySQL</span>
                    </div>
                    <div class="mb-3">
                        <div class="d-flex justify-content-between small" style="color:#c8d6ec;">
                            <span>Capstone web app</span><span>72%</span>
                        </div>
                        <div class="wp-progress mt-1"><div class="progress-bar bg-success" style="width:72%"></div></div>
                    </div>
                    <div class="mb-3">
                        <div class="d-flex justify-content-between small" style="color:#c8d6ec;">
                            <span>Database module</span><span>45%</span>
                        </div>
                        <div class="wp-progress mt-1"><div class="progress-bar bg-primary" style="width:45%"></div></div>
                    </div>
                    <div>
                        <div class="d-flex justify-content-between small" style="color:#c8d6ec;">
                            <span>Documentation</span><span>18%</span>
                        </div>
                        <div class="wp-progress mt-1"><div class="progress-bar bg-warning" style="width:18%"></div></div>
                    </div>
                    <hr style="border-color:rgba(255,255,255,.15);">
                    <div class="row text-center text-white g-2">
                        <div class="col-4">
                            <div class="fw-bold fs-5">Tasks</div>
                            <div class="small" style="color:#9fb3d1;">assigned &amp; tracked</div>
                        </div>
                        <div class="col-4">
                            <div class="fw-bold fs-5">Evidence</div>
                            <div class="small" style="color:#9fb3d1;">files &amp; notes</div>
                        </div>
                        <div class="col-4">
                            <div class="fw-bold fs-5">Reviews</div>
                            <div class="small" style="color:#9fb3d1;">approve / reject</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- ============================ features ============================ -->
<section class="py-5" id="features">
    <div class="container">
        <div class="text-center mb-5">
            <h2 class="fw-bold">Everything a project team needs</h2>
            <p class="text-muted-soft">Four roles, one transparent record of who did what.</p>
        </div>
        <div class="row g-4">
            <div class="col-md-6 col-lg-3">
                <div class="wp-card h-100"><div class="wp-card-body">
                    <div class="wp-feature-icon"><i class="bi bi-kanban"></i></div>
                    <h5>Project management</h5>
                    <p class="text-muted-soft mb-0">Create projects, add members, track progress and
                        archive finished work without losing history.</p>
                </div></div>
            </div>
            <div class="col-md-6 col-lg-3">
                <div class="wp-card h-100"><div class="wp-card-body">
                    <div class="wp-feature-icon"><i class="bi bi-list-check"></i></div>
                    <h5>Task workflow</h5>
                    <p class="text-muted-soft mb-0">Priorities, deadlines and a controlled status flow:
                        To Do &rarr; In Progress &rarr; Submitted &rarr; In Review &rarr; Approved.</p>
                </div></div>
            </div>
            <div class="col-md-6 col-lg-3">
                <div class="wp-card h-100"><div class="wp-card-body">
                    <div class="wp-feature-icon"><i class="bi bi-cloud-arrow-up"></i></div>
                    <h5>Proof of work</h5>
                    <p class="text-muted-soft mb-0">Upload screenshots, documents and archives with
                        server-side type and size checks. Files stay outside the web root.</p>
                </div></div>
            </div>
            <div class="col-md-6 col-lg-3">
                <div class="wp-card h-100"><div class="wp-card-body">
                    <div class="wp-feature-icon"><i class="bi bi-clipboard2-check"></i></div>
                    <h5>Verification</h5>
                    <p class="text-muted-soft mb-0">Reviewers approve or reject with mandatory feedback.
                        Every version and decision is kept forever.</p>
                </div></div>
            </div>
        </div>
    </div>
</section>

<!-- ============================ workflow ============================ -->
<section class="py-5" id="workflow" style="background:#fff;border-top:1px solid #e4e9f2;border-bottom:1px solid #e4e9f2;">
    <div class="container">
        <div class="row g-5 align-items-center">
            <div class="col-lg-5">
                <h2 class="fw-bold mb-3">A workflow your evaluator can follow</h2>
                <ol class="wp-timeline">
                    <li>
                        <strong>Manager creates a project</strong>
                        <div class="text-muted-soft">Members are added with a project role: manager, member or reviewer.</div>
                    </li>
                    <li>
                        <strong>Tasks are assigned</strong>
                        <div class="text-muted-soft">Each task has a priority, a start date and a deadline, and the
                            assignee receives a notification.</div>
                    </li>
                    <li>
                        <strong>Member submits evidence</strong>
                        <div class="text-muted-soft">Description plus up to five files. The task moves to
                            "Submitted" automatically.</div>
                    </li>
                    <li>
                        <strong>Reviewer decides</strong>
                        <div class="text-muted-soft">Approval completes the work; rejection requires feedback and
                            allows a new version.</div>
                    </li>
                    <li>
                        <strong>Reports and audit log</strong>
                        <div class="text-muted-soft">Progress, contribution and verification reports, exportable to CSV.</div>
                    </li>
                </ol>
            </div>
            <div class="col-lg-7">
                <div class="row g-3">
                    <div class="col-sm-6">
                        <div class="wp-card h-100"><div class="wp-card-body">
                            <span class="wp-badge badge-status-primary mb-2"><i class="bi bi-shield-lock"></i>Security</span>
                            <ul class="mb-0 small text-muted-soft ps-3">
                                <li>BCrypt password hashing</li>
                                <li>Prepared statements everywhere</li>
                                <li>Role checks on the server, not in the UI</li>
                                <li>CSRF tokens on every form</li>
                                <li>Safe file uploads with magic-byte checks</li>
                            </ul>
                        </div></div>
                    </div>
                    <div class="col-sm-6">
                        <div class="wp-card h-100"><div class="wp-card-body">
                            <span class="wp-badge badge-status-success mb-2"><i class="bi bi-graph-up"></i>Insight</span>
                            <ul class="mb-0 small text-muted-soft ps-3">
                                <li>Role-specific dashboards</li>
                                <li>Project and task progress</li>
                                <li>Individual contribution summary</li>
                                <li>Pending verification backlog</li>
                                <li>CSV export for every report</li>
                            </ul>
                        </div></div>
                    </div>
                    <div class="col-12">
                        <div class="wp-card"><div class="wp-card-body">
                            <div class="row text-center">
                                <div class="col-3 border-end">
                                    <div class="fw-bold fs-4">4</div><div class="small text-muted-soft">Roles</div>
                                </div>
                                <div class="col-3 border-end">
                                    <div class="fw-bold fs-4">11</div><div class="small text-muted-soft">Database tables</div>
                                </div>
                                <div class="col-3 border-end">
                                    <div class="fw-bold fs-4">7</div><div class="small text-muted-soft">Task states</div>
                                </div>
                                <div class="col-3">
                                    <div class="fw-bold fs-4">5</div><div class="small text-muted-soft">Reports</div>
                                </div>
                            </div>
                        </div></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- ============================== cta ============================== -->
<section class="py-5">
    <div class="container text-center">
        <h3 class="fw-bold">Ready to demonstrate your project?</h3>
        <p class="text-muted-soft mb-4">Sign in with a demo account, or register a new one in seconds.</p>
        <a class="btn btn-primary btn-lg px-4" href="${pageContext.request.contextPath}/login">
            <i class="bi bi-box-arrow-in-right me-2"></i>Sign in to WorkProof
        </a>
    </div>
</section>

<footer class="py-4" style="background:#0f1d33;color:#9fb3d1;">
    <div class="container d-flex flex-wrap justify-content-between gap-2 small">
        <span>&copy; 2026 WorkProof &middot; Academic PBL project</span>
        <span>Java 17 &middot; JSP &middot; Servlets &middot; Tomcat 9 &middot; MySQL 8 &middot; Bootstrap 5</span>
    </div>
</footer>

<script src="${pageContext.request.contextPath}/assets/vendor/bootstrap/bootstrap.bundle.min.js"></script>
</body>
</html>
