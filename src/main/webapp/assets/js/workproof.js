/* ============================================================================
 * WorkProof front-end helpers.
 *
 * Everything here is progressive enhancement: the application stays fully usable
 * when JavaScript is disabled (links, forms and server-side validation all work).
 *
 * No JavaScript libraries are used, so the app runs completely offline and the
 * Content-Security-Policy can forbid inline scripts.
 * ========================================================================== */
(function () {
    "use strict";

    /* ------------------------------------------------- sidebar (mobile) ---- */
    function initSidebar() {
        var toggle = document.getElementById("wpSidebarToggle");
        var sidebar = document.getElementById("wpSidebar");
        if (!toggle || !sidebar) {
            return;
        }
        toggle.addEventListener("click", function () {
            sidebar.classList.toggle("show");
            var backdrop = document.querySelector(".wp-backdrop");
            if (sidebar.classList.contains("show")) {
                if (!backdrop) {
                    backdrop = document.createElement("div");
                    backdrop.className = "wp-backdrop";
                    backdrop.addEventListener("click", function () {
                        sidebar.classList.remove("show");
                        backdrop.remove();
                    });
                    document.body.appendChild(backdrop);
                }
            } else if (backdrop) {
                backdrop.remove();
            }
        });
    }

    /* ------------------------------------------- client-side table filter ---- */
    /* Filters the rows of a table as the user types. The server-side search box
       next to it does the same thing against the database. */
    function initTableFilters() {
        var inputs = document.querySelectorAll("[data-filter-table]");
        Array.prototype.forEach.call(inputs, function (input) {
            var tableId = input.getAttribute("data-filter-table");
            var table = document.getElementById(tableId);
            if (!table) {
                return;
            }
            input.addEventListener("input", function () {
                var needle = input.value.toLowerCase().trim();
                var rows = table.querySelectorAll("tbody tr");
                var visible = 0;
                Array.prototype.forEach.call(rows, function (row) {
                    var matches = row.innerText.toLowerCase().indexOf(needle) !== -1;
                    row.style.display = matches ? "" : "none";
                    if (matches) {
                        visible++;
                    }
                });
                var counter = document.querySelector("[data-filter-count='" + tableId + "']");
                if (counter) {
                    counter.textContent = visible + " shown";
                }
            });
        });
    }

    /* ----------------------------------------------------- confirm actions ---- */
    function initConfirmation() {
        document.addEventListener("submit", function (event) {
            var form = event.target;
            if (!form.matches("[data-confirm]")) {
                return;
            }
            var message = form.getAttribute("data-confirm") || "Are you sure?";
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    }

    /* -------------------------------------------------------- alert fade ---- */
    function initAlerts() {
        var alerts = document.querySelectorAll("[data-autohide]");
        Array.prototype.forEach.call(alerts, function (alert) {
            window.setTimeout(function () {
                alert.style.transition = "opacity .4s ease";
                alert.style.opacity = "0";
                window.setTimeout(function () {
                    alert.remove();
                }, 450);
            }, 6000);
        });
    }

    /* --------------------------------------------------- doughnut charts ---- */
    /* A dependency-free doughnut: the JSP writes real database numbers into a
       data-chart attribute, this function turns them into a conic-gradient. */
    function initDoughnuts() {
        var charts = document.querySelectorAll("[data-chart]");
        Array.prototype.forEach.call(charts, function (chart) {
            var raw = chart.getAttribute("data-chart");
            if (!raw) {
                return;
            }
            var segments;
            try {
                segments = JSON.parse(raw);
            } catch (error) {
                return;
            }
            var total = segments.reduce(function (sum, segment) {
                return sum + Number(segment.value || 0);
            }, 0);

            if (total === 0) {
                chart.style.background = "#eef2f8";
                return;
            }

            var stops = [];
            var position = 0;
            segments.forEach(function (segment) {
                var value = Number(segment.value || 0);
                if (value <= 0) {
                    return;
                }
                var end = position + (value / total) * 100;
                stops.push(segment.color + " " + position.toFixed(2) + "% " + end.toFixed(2) + "%");
                position = end;
            });
            chart.style.background = "conic-gradient(" + stops.join(",") + ")";
        });
    }

    /* ------------------------------------------------------- progress rings ---- */
    function initRings() {
        var rings = document.querySelectorAll("[data-ring]");
        Array.prototype.forEach.call(rings, function (ring) {
            var value = Number(ring.getAttribute("data-ring") || 0);
            ring.style.setProperty("--wp-ring-value", Math.max(0, Math.min(100, value)));
        });
    }

    /* -------------------------------------------------- password strength ---- */
    function initPasswordStrength() {
        var inputs = document.querySelectorAll("[data-password-strength]");
        Array.prototype.forEach.call(inputs, function (input) {
            var meter = document.getElementById(input.getAttribute("data-password-strength"));
            if (!meter) {
                return;
            }
            input.addEventListener("input", function () {
                var value = input.value;
                var score = 0;
                if (value.length >= 8) score++;
                if (/[A-Z]/.test(value)) score++;
                if (/[0-9]/.test(value)) score++;
                if (/[^A-Za-z0-9]/.test(value)) score++;
                if (value.length >= 12) score++;

                var labels = ["Too short", "Weak", "Fair", "Good", "Strong", "Very strong"];
                var colors = ["bg-danger", "bg-danger", "bg-warning", "bg-info", "bg-success", "bg-success"];
                meter.className = "progress-bar " + colors[score];
                meter.style.width = ((score + 1) / 6 * 100) + "%";
                meter.textContent = labels[score];
            });
        });
    }

    /* ------------------------------------------------------ file selection ---- */
    function initFileInputs() {
        var inputs = document.querySelectorAll("input[type=file][data-file-list]");
        Array.prototype.forEach.call(inputs, function (input) {
            var list = document.getElementById(input.getAttribute("data-file-list"));
            if (!list) {
                return;
            }
            input.addEventListener("change", function () {
                list.innerHTML = "";
                Array.prototype.forEach.call(input.files, function (file) {
                    var item = document.createElement("li");
                    var size = file.size < 1024 * 1024
                        ? (file.size / 1024).toFixed(1) + " KB"
                        : (file.size / (1024 * 1024)).toFixed(2) + " MB";
                    item.textContent = file.name + " (" + size + ")";
                    list.appendChild(item);
                });
            });
        });
    }

    /* --------------------------------------------------------- mark-read ---- */
    /* Unread notifications are marked as read through the normal POST endpoint,
       so this uses a real form submit rather than a background request. */
    function initMarkAllRead() {
        var buttons = document.querySelectorAll("[data-mark-all-read]");
        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                var form = document.getElementById("wpMarkAllReadForm");
                if (form) {
                    form.submit();
                }
            });
        });
    }

    /* ----------------------------------------------------------- tooltips ---- */
    function initTooltips() {
        if (!window.bootstrap || !window.bootstrap.Tooltip) {
            return;
        }
        var elements = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        Array.prototype.forEach.call(elements, function (element) {
            new window.bootstrap.Tooltip(element);
        });
    }

    /* --------------------------------------------------------------- boot ---- */
    document.addEventListener("DOMContentLoaded", function () {
        initSidebar();
        initTableFilters();
        initConfirmation();
        initAlerts();
        initDoughnuts();
        initRings();
        initPasswordStrength();
        initFileInputs();
        initMarkAllRead();
        initTooltips();
    });
})();
