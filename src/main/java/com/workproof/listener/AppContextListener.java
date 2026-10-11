package com.workproof.listener;

import com.workproof.service.ActivityService;
import com.workproof.service.NotificationService;
import com.workproof.service.TaskService;
import com.workproof.util.AppConfig;
import com.workproof.util.DataSourceProvider;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application start-up and shutdown hook.
 *
 * On start it:
 *   1. prints a short banner with the configuration in use (never the password),
 *   2. verifies that the upload directory exists and is writable,
 *   3. tests the database connection and prints a clear, actionable message when it
 *      fails,
 *   4. starts the scheduled job that creates deadline notifications.
 *
 * On stop it shuts the job down and closes the connection pool, which is what stops
 * Tomcat from complaining about "leaked" threads during redeployment.
 */
public class AppContextListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("==========================================================");
        System.out.println(" " + AppConfig.appName() + " starting up");
        System.out.println("----------------------------------------------------------");
        System.out.println(" Database URL  : " + AppConfig.dbUrl());
        System.out.println(" Database user : " + AppConfig.dbUser());
        System.out.println(" Password      : " + (AppConfig.dbPassword().isEmpty()
                ? "NOT SET - set WORKPROOF_DB_PASSWORD or workproof-local.properties" : "******** (configured)"));
        System.out.println(" Upload folder : " + AppConfig.uploadDir());
        System.out.println(" Session (min) : " + AppConfig.sessionTimeoutMinutes());
        System.out.println(" Self-review   : " + (AppConfig.allowSelfReview() ? "ALLOWED (documented rule)" : "BLOCKED"));
        System.out.println("----------------------------------------------------------");

        if (AppConfig.ensureUploadDirExists()) {
            System.out.println(" Upload folder is ready.");
        } else {
            System.err.println(" WARNING: the upload folder is not writable. Evidence uploads will fail.");
        }

        if (DataSourceProvider.testConnection()) {
            System.out.println(" Database connection: OK");
        } else {
            System.err.println(" Database connection: FAILED");
            System.err.println("   -> Check that MySQL is running, that the 'workproof' database exists");
            System.err.println("      (run database/schema.sql) and that the credentials are correct.");
            System.err.println("      Error: " + DataSourceProvider.getLastInitError());
        }
        System.out.println("==========================================================");

        startDeadlineJob(event);
    }

    /**
     * Creates "deadline approaching" notifications.
     *
     * The notification table has a UNIQUE (user_id, dedupe_key) constraint, so running
     * this job repeatedly never produces duplicates - a clean way to make a scheduled
     * job idempotent without extra bookkeeping.
     */
    private void startDeadlineJob(ServletContextEvent event) {
        final NotificationService notificationService = new NotificationService();
        final TaskService taskService = new TaskService();
        final ActivityService activityService = new ActivityService();

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "workproof-deadline-job");
            thread.setDaemon(true);
            return thread;
        });

        Runnable job = () -> {
            try {
                java.util.List<com.workproof.model.Task> dueTasks = taskService.tasksDueWithin(3);
                for (com.workproof.model.Task task : dueTasks) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),
                            task.getDueDate());
                    for (com.workproof.model.ProjectMember assignee : task.getAssignees()) {
                        notificationService.notifyDeadlineApproaching(assignee.getUserId(), task, daysLeft);
                    }
                }
                if (!dueTasks.isEmpty()) {
                    activityService.logForUserId(null, "DEADLINE_SCAN", "TASK", null,
                            "Deadline scan at " + LocalDateTime.now() + " checked " + dueTasks.size()
                                    + " task(s) due within 3 days", null);
                }
            } catch (RuntimeException e) {
                // The job must survive a temporary database outage.
                System.err.println("[WorkProof] Deadline job failed: " + e.getMessage());
            }
        };

        // Run once 30 seconds after start-up (so the demo shows reminders quickly)
        // and then every 6 hours.
        scheduler.scheduleAtFixedRate(job, 30, 6 * 60, TimeUnit.SECONDS);
        event.getServletContext().log("WorkProof deadline notification job started.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        DataSourceProvider.shutdown();
        stopMysqlCleanupThread();
        deregisterJdbcDrivers();
        System.out.println("[WorkProof] Application stopped.");
    }

    /** Stops the MySQL connector cleanup thread so Tomcat can unload the webapp cleanly. */
    private void stopMysqlCleanupThread() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
            // The driver may not be present (for example in unit tests) - nothing to do.
        }
    }

    /** Releases JDBC drivers loaded by this web application's class loader. */
    private void deregisterJdbcDrivers() {
        ClassLoader webAppLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == webAppLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ignored) {
                    // Best effort - a failed deregistration must never block shutdown.
                }
            }
        }
    }
}
