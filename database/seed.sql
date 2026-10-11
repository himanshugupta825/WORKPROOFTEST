-- ============================================================================
--  WorkProof - demonstration data (OPTIONAL)
--  File   : database/seed.sql
--  Target : MySQL 8.x (also works on MariaDB 11.x)
--
--  HOW TO RUN (after schema.sql):
--      mysql -u root -p workproof < database/seed.sql
--  or inside the MySQL shell:
--      USE workproof;
--      source database/seed.sql;
--
--  The script is safe to re-run: it clears the tables first (respecting foreign
--  keys) and then inserts a realistic 7-week project scenario with:
--      - 1 administrator, 1 project manager, 3 team members, 1-2 reviewers
--      - 2 projects, 9 tasks in every important status
--      - approved, pending and rejected evidence submissions with reviews
--      - notifications and a populated activity log
--
--  ---------------------------------------------------------------------------
--  LOGIN DETAILS  (all passwords are stored as BCrypt hashes, never plaintext)
--  ---------------------------------------------------------------------------
--   ADMINISTRATOR      admin@workproof.local          Admin@123
--   PROJECT MANAGER    priya.manager@workproof.local  Manager@123
--   TEAM MEMBER        aarav.member@workproof.local   Member@123
--   TEAM MEMBER        ishita.member@workproof.local  Member@123
--   TEAM MEMBER        rohan.member@workproof.local   Member@123
--   REVIEWER / MENTOR  mentor.reviewer@workproof.local Reviewer@123
--   REVIEWER / MENTOR  kavya.reviewer@workproof.local  Reviewer@123
--
--  NOTE: the seeded submissions deliberately have NO file rows, because the
--  upload folder belongs to your machine. The written evidence is enough to
--  demonstrate the review workflow; upload your own files to test file handling.
-- ============================================================================

USE workproof;

-- ---------------------------------------------------------------------------
-- 0. Clear existing data so this script can be re-run safely
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE activity_logs;
TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE submission_files;
TRUNCATE TABLE proof_submissions;
TRUNCATE TABLE task_assignments;
TRUNCATE TABLE tasks;
TRUNCATE TABLE project_members;
TRUNCATE TABLE projects;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 1. Roles
-- ---------------------------------------------------------------------------
INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMIN',    'Administrator - manages accounts, settings and system reports'),
    (2, 'MANAGER',  'Project Manager / Team Leader - creates projects and assigns tasks'),
    (3, 'MEMBER',   'Team Member - works on tasks and submits proof of work'),
    (4, 'REVIEWER', 'Reviewer / Mentor - verifies evidence and gives feedback');

-- ---------------------------------------------------------------------------
-- 2. Users (passwords hashed with BCrypt cost 12)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, full_name, email, password_hash, role_id, status, department, designation, phone, last_login_at) VALUES
    (1, 'Dr. Meera Iyer',      'admin@workproof.local',           '$2a$12$h6SVQSGhdDm6KshDaKSdGeBZiZGYAjiu11lmrndSelmCz/Lu0I.X2', 1, 'ACTIVE', 'Computer Science', 'PBL Coordinator',  '+91 98200 11111', '2026-10-05 09:15:00'),
    (2, 'Priya Verma',         'priya.manager@workproof.local',   '$2a$12$A4LyIUjDa61.MHt8G29o3.Lzp3YmsLqWhUJd9kZ5tzqQpo0gSo/LS', 2, 'ACTIVE', 'Computer Science', 'Team Leader',      '+91 98200 22222', '2026-10-09 10:02:00'),
    (3, 'Aarav Sharma',        'aarav.member@workproof.local',    '$2a$12$nM3nivDAuwR6qHQyuZ1IVuf0cIAVo1dbOYVgiFhSiREn0jK2cc54m', 3, 'ACTIVE', 'Computer Science', 'Backend Developer','+91 98200 33333', '2026-10-10 18:40:00'),
    (4, 'Ishita Rao',          'ishita.member@workproof.local',   '$2a$12$nM3nivDAuwR6qHQyuZ1IVuf0cIAVo1dbOYVgiFhSiREn0jK2cc54m', 3, 'ACTIVE', 'Computer Science', 'Frontend Developer','+91 98200 44444', '2026-10-10 16:22:00'),
    (5, 'Rohan Gupta',         'rohan.member@workproof.local',    '$2a$12$nM3nivDAuwR6qHQyuZ1IVuf0cIAVo1dbOYVgiFhSiREn0jK2cc54m', 3, 'ACTIVE', 'Computer Science', 'Tester',           '+91 98200 55555', '2026-10-08 11:05:00'),
    (6, 'Prof. Sanjay Kulkarni','mentor.reviewer@workproof.local', '$2a$12$ZxxZj8V8e10mMwcyUyS8SOU.1PN0OMG0aN8GhUnFUM6hPWjIGYa0O', 4, 'ACTIVE', 'Computer Science', 'Faculty Mentor',   NULL,              '2026-10-07 14:30:00'),
    (7, 'Kavya Nair',          'kavya.reviewer@workproof.local',  '$2a$12$ZxxZj8V8e10mMwcyUyS8SOU.1PN0OMG0aN8GhUnFUM6hPWjIGYa0O', 4, 'ACTIVE', 'Information Technology', 'Industry Mentor', NULL, '2026-10-04 12:00:00');

-- ---------------------------------------------------------------------------
-- 3. Projects
-- ---------------------------------------------------------------------------
INSERT INTO projects (id, name, code, description, start_date, end_date, status, created_by) VALUES
    (1, 'Complaint Management Portal', 'CMP-2026',
     'A web portal where students and staff raise complaints about campus facilities, track their status and view resolution history. The 7-week PBL deliverable includes authentication, complaint workflow, reports and an admin dashboard.',
     '2026-08-01', '2026-11-30', 'ACTIVE', 2),
    (2, 'Attendance Analytics Dashboard', 'ATT-2026',
     'Analytics dashboard that reads attendance records and presents subject-wise and student-wise trends with weekly summaries for the class coordinator.',
     '2026-09-01', '2026-12-15', 'PLANNED', 2);

-- ---------------------------------------------------------------------------
-- 4. Project members (project role inside each project)
-- ---------------------------------------------------------------------------
INSERT INTO project_members (id, project_id, user_id, role_in_project, joined_at) VALUES
    (1, 1, 2, 'MANAGER',  '2026-08-01 09:00:00'),
    (2, 1, 3, 'MEMBER',   '2026-08-01 09:05:00'),
    (3, 1, 4, 'MEMBER',   '2026-08-01 09:06:00'),
    (4, 1, 5, 'MEMBER',   '2026-08-01 09:07:00'),
    (5, 1, 6, 'REVIEWER', '2026-08-02 10:00:00'),
    (6, 2, 2, 'MANAGER',  '2026-09-01 09:00:00'),
    (7, 2, 3, 'MEMBER',   '2026-09-01 09:10:00'),
    (8, 2, 4, 'MEMBER',   '2026-09-01 09:11:00'),
    (9, 2, 7, 'REVIEWER', '2026-09-02 11:00:00');

-- ---------------------------------------------------------------------------
-- 5. Tasks (project 1: ids 1-7, project 2: ids 8-9)
--    Statuses cover the whole workflow so the dashboard and reports look real.
-- ---------------------------------------------------------------------------
INSERT INTO tasks (id, project_id, title, description, priority, status, start_date, due_date, created_by, completed_at) VALUES
    (1, 1, 'Requirement analysis and SRS document',
     'Interview two students and one staff member, list the functional and non-functional requirements and prepare the SRS document with use-case diagrams.',
     'HIGH',     'COMPLETED',   '2026-08-01', '2026-08-12', 2, '2026-08-11 17:30:00'),
    (2, 1, 'Database schema and ER diagram',
     'Design the normalised schema (users, complaints, categories, status history) and produce the ER diagram with primary and foreign keys.',
     'HIGH',     'COMPLETED',   '2026-08-08', '2026-08-20', 2, '2026-08-19 19:05:00'),
    (3, 1, 'Design login and registration screens',
     'Create responsive login and registration pages with client and server-side validation and password strength feedback.',
     'MEDIUM',   'APPROVED',    '2026-08-18', '2026-09-02', 2, NULL),
    (4, 1, 'Implement complaint CRUD APIs',
     'Build the create, read, update and delete operations for complaints using servlets and JDBC with prepared statements.',
     'CRITICAL', 'IN_PROGRESS', '2026-09-01', '2026-10-18', 2, NULL),
    (5, 1, 'Frontend complaint dashboard',
     'Build the complaint list, filters, status badges and progress summary using Bootstrap 5 components.',
     'MEDIUM',   'SUBMITTED',   '2026-09-10', '2026-10-14', 2, NULL),
    (6, 1, 'Unit testing and bug fixes',
     'Write unit tests for the service layer, log defects in the defect sheet and fix all critical and high severity issues.',
     'HIGH',     'REJECTED',    '2026-09-20', '2026-10-09', 2, NULL),
    (7, 1, 'Final report and presentation',
     'Compile the final project report with screenshots, testing evidence and contribution summary, and prepare the demonstration script.',
     'MEDIUM',   'TODO',        '2026-10-20', '2026-11-25', 2, NULL),
    (8, 2, 'Collect attendance dataset',
     'Collect four weeks of attendance data and clean it into CSV files that can be imported for the dashboard.',
     'MEDIUM',   'IN_PROGRESS', '2026-09-05', '2026-10-20', 2, NULL),
    (9, 2, 'Design dashboard wireframes',
     'Sketch the wireframes for the subject-wise and student-wise analytics views and confirm them with the coordinator.',
     'LOW',      'TODO',        '2026-09-15', '2026-10-25', 2, NULL);

-- ---------------------------------------------------------------------------
-- 6. Task assignments
-- ---------------------------------------------------------------------------
INSERT INTO task_assignments (id, task_id, user_id, assigned_by, assigned_at) VALUES
    (1, 1, 3, 2, '2026-08-01 09:30:00'),
    (2, 2, 4, 2, '2026-08-08 09:30:00'),
    (3, 3, 4, 2, '2026-08-18 09:30:00'),
    (4, 4, 3, 2, '2026-09-01 09:30:00'),
    (5, 5, 4, 2, '2026-09-10 09:30:00'),
    (6, 6, 5, 2, '2026-09-20 09:30:00'),
    (7, 6, 3, 2, '2026-09-20 09:31:00'),
    (8, 7, 3, 2, '2026-10-01 09:30:00'),
    (9, 7, 4, 2, '2026-10-01 09:31:00'),
    (10, 8, 3, 2, '2026-09-05 09:30:00'),
    (11, 9, 4, 2, '2026-09-15 09:30:00');

-- ---------------------------------------------------------------------------
-- 7. Proof-of-work submissions and their reviews
--    Version history is kept: task 6 has a rejected version and can be resubmitted.
-- ---------------------------------------------------------------------------
INSERT INTO proof_submissions (id, task_id, submitted_by, title, description, status, version, submitted_at) VALUES
    (1, 1, 3, 'SRS document version 1.0',
     'Prepared the complete SRS with 18 functional requirements, 6 non-functional requirements and three use-case diagrams. The document was reviewed with the mentor and the coordinator signed it off.',
     'APPROVED', 1, '2026-08-11 17:20:00'),
    (2, 2, 4, 'Normalised schema and ER diagram',
     'Designed eleven tables in third normal form, added foreign keys with ON DELETE rules, and exported the ER diagram. The schema was executed successfully in MySQL 8.',
     'APPROVED', 1, '2026-08-19 18:50:00'),
    (3, 3, 4, 'Login and registration screens with validation',
     'Implemented responsive login and registration pages. Server-side validation covers duplicate email, password policy and field lengths. Passwords are stored as BCrypt hashes. Tested on Chrome and Firefox at three screen sizes.',
     'APPROVED', 1, '2026-09-01 20:10:00'),
    (4, 5, 4, 'Complaint dashboard with filters',
     'Built the complaint list view with status badges, category filter, search box and pagination. The progress bars are filled from real database counts. Screenshots of each state are attached in the next upload.',
     'PENDING', 1, '2026-10-08 21:40:00'),
    (5, 6, 5, 'Unit test results - first attempt',
     'Ran the service layer tests. Most passed but the status transition test failed for the rejected to in-progress case and the deadline calculation test is still failing.',
     'REJECTED', 1, '2026-10-05 19:15:00');

INSERT INTO reviews (id, submission_id, reviewer_id, decision, comments, reviewed_at) VALUES
    (1, 1, 6, 'APPROVED', 'Requirements are complete and traceable. The use-case diagrams match the requirement list. Approved.', '2026-08-12 10:15:00'),
    (2, 2, 6, 'APPROVED', 'Schema is properly normalised with sensible foreign key rules. Approved.', '2026-08-20 11:00:00'),
    (3, 3, 6, 'APPROVED', 'Both screens are responsive and the validation rules match the SRS. Good work on hashing the passwords.', '2026-09-02 09:45:00'),
    (4, 5, 6, 'REJECTED', 'Two tests are still failing and no evidence of the failing test output was attached. Please attach the console output and the corrected test code, then submit a new version.', '2026-10-06 12:20:00');

-- ---------------------------------------------------------------------------
-- 8. Notifications
-- ---------------------------------------------------------------------------
INSERT INTO notifications (id, user_id, title, message, type, link, dedupe_key, is_read, created_at) VALUES
    (1, 3, 'New task assigned',         'Priya Verma assigned you the task ''Implement complaint CRUD APIs'' in Complaint Management Portal (due 2026-10-18).', 'TASK_ASSIGNED', '/tasks/view?id=4', NULL, 1, '2026-09-01 09:32:00'),
    (2, 4, 'Evidence awaiting verification', 'Ishita Rao submitted proof for ''Frontend complaint dashboard'' (version 1). Please review it.', 'SUBMISSION', '/reviews/view?id=4', NULL, 0, '2026-10-08 21:41:00'),
    (3, 5, 'Submission rejected',       'Prof. Sanjay Kulkarni rejected your evidence for ''Unit testing and bug fixes''. Feedback: Two tests are still failing and no evidence of the failing test output was attached. You may correct the work and submit a new version.', 'REVIEW', '/submissions/view?id=5', NULL, 0, '2026-10-06 12:21:00'),
    (4, 4, 'Submission approved',       'Prof. Sanjay Kulkarni approved your evidence for ''Design login and registration screens''. Feedback: Both screens are responsive and the validation rules match the SRS.', 'REVIEW', '/submissions/view?id=3', NULL, 1, '2026-09-02 09:46:00'),
    (5, 3, 'Deadline reminder',         'Task ''Final report and presentation'' is due in 3 day(s) (2026-11-25).', 'DEADLINE', '/tasks/view?id=7', 'deadline:7:2026-11-22:3', 0, '2026-11-22 06:00:00'),
    (6, 4, 'Deadline reminder',         'Task ''Frontend complaint dashboard'' is due in 2 day(s) (2026-10-14).', 'DEADLINE', '/tasks/view?id=5', 'deadline:5:2026-10-12:2', 0, '2026-10-12 06:00:00'),
    (7, 3, 'Added to project',          'Priya Verma added you to ''Attendance Analytics Dashboard'' as Member.', 'PROJECT', '/projects/view?id=2', NULL, 1, '2026-09-01 09:12:00'),
    (8, 6, 'Evidence awaiting verification', 'Ishita Rao submitted proof for ''Complaint dashboard with filters'' (version 1). Please review it.', 'SUBMISSION', '/reviews/view?id=4', NULL, 0, '2026-10-08 21:42:00'),
    (9, 2, 'Task status updated',       'Aarav Sharma moved ''Implement complaint CRUD APIs'' from To Do to In Progress.', 'TASK_STATUS', '/tasks/view?id=4', NULL, 1, '2026-09-04 10:05:00'),
    (10, 2, 'System welcome',           'Your WorkProof account is ready. Explore your dashboard to see the projects and tasks assigned to you.', 'SYSTEM', '/dashboard', 'welcome:2', 1, '2026-08-01 08:55:00');

-- ---------------------------------------------------------------------------
-- 9. Activity log (audit trail)
-- ---------------------------------------------------------------------------
INSERT INTO activity_logs (user_id, action, entity_type, entity_id, details, ip_address, created_at) VALUES
    (2, 'AUTH_LOGIN',          'USER',     2, 'Priya Verma signed in', '127.0.0.1', '2026-10-09 10:02:00'),
    (2, 'PROJECT_CREATED',     'PROJECT',  1, 'Created project ''Complaint Management Portal'' (CMP-2026)', '127.0.0.1', '2026-08-01 09:00:00'),
    (2, 'PROJECT_MEMBER_ADDED','PROJECT',  1, 'Added Aarav Sharma to project ''Complaint Management Portal'' as Member', '127.0.0.1', '2026-08-01 09:05:00'),
    (2, 'PROJECT_MEMBER_ADDED','PROJECT',  1, 'Added Ishita Rao to project ''Complaint Management Portal'' as Member', '127.0.0.1', '2026-08-01 09:06:00'),
    (2, 'PROJECT_MEMBER_ADDED','PROJECT',  1, 'Added Rohan Gupta to project ''Complaint Management Portal'' as Member', '127.0.0.1', '2026-08-01 09:07:00'),
    (2, 'PROJECT_MEMBER_ADDED','PROJECT',  1, 'Added Prof. Sanjay Kulkarni to project ''Complaint Management Portal'' as Reviewer', '127.0.0.1', '2026-08-02 10:00:00'),
    (2, 'TASK_CREATED',        'TASK',     1, 'Created task ''Requirement analysis and SRS document'' in project ''Complaint Management Portal''', '127.0.0.1', '2026-08-01 09:30:00'),
    (3, 'TASK_STATUS_CHANGED', 'TASK',     1, 'Moved ''Requirement analysis and SRS document'' from In Progress to Submitted', '127.0.0.1', '2026-08-11 17:20:00'),
    (6, 'REVIEW_APPROVED',     'SUBMISSION', 1, 'Approved submission v1 for task ''Requirement analysis and SRS document'' - Requirements are complete and traceable.', '127.0.0.1', '2026-08-12 10:15:00'),
    (2, 'TASK_STATUS_CHANGED', 'TASK',     1, 'Moved ''Requirement analysis and SRS document'' from Approved to Completed', '127.0.0.1', '2026-08-11 17:30:00'),
    (3, 'SUBMISSION_CREATED',  'SUBMISSION', 1, 'Submitted evidence ''SRS document version 1.0'' (v1) for task ''Requirement analysis and SRS document''', '127.0.0.1', '2026-08-11 17:20:00'),
    (4, 'SUBMISSION_CREATED',  'SUBMISSION', 3, 'Submitted evidence ''Login and registration screens with validation'' (v1) for task ''Design login and registration screens''', '127.0.0.1', '2026-09-01 20:10:00'),
    (6, 'REVIEW_APPROVED',     'SUBMISSION', 3, 'Approved submission v1 for task ''Design login and registration screens'' - Both screens are responsive.', '127.0.0.1', '2026-09-02 09:45:00'),
    (5, 'SUBMISSION_CREATED',  'SUBMISSION', 5, 'Submitted evidence ''Unit test results - first attempt'' (v1) for task ''Unit testing and bug fixes''', '127.0.0.1', '2026-10-05 19:15:00'),
    (6, 'REVIEW_REJECTED',     'SUBMISSION', 5, 'Rejected submission v1 for task ''Unit testing and bug fixes'' - Two tests are still failing and no evidence was attached.', '127.0.0.1', '2026-10-06 12:20:00'),
    (4, 'SUBMISSION_CREATED',  'SUBMISSION', 4, 'Submitted evidence ''Complaint dashboard with filters'' (v1) for task ''Frontend complaint dashboard''', '127.0.0.1', '2026-10-08 21:40:00'),
    (1, 'USER_CREATED',        'USER',     6, 'Created account mentor.reviewer@workproof.local with role Reviewer / Mentor', '127.0.0.1', '2026-08-02 09:45:00'),
    (1, 'USER_STATUS_CHANGED', 'USER',     7, 'Deactivated kavya.reviewer@workproof.local', '127.0.0.1', '2026-10-03 15:30:00'),
    (1, 'USER_STATUS_CHANGED', 'USER',     7, 'Activated kavya.reviewer@workproof.local', '127.0.0.1', '2026-10-04 11:55:00'),
    (1, 'AUTH_LOGIN',          'USER',     1, 'Dr. Meera Iyer signed in', '127.0.0.1', '2026-10-05 09:15:00');

-- ---------------------------------------------------------------------------
-- 10. Keep AUTO_INCREMENT in step with the explicit ids used above
-- ---------------------------------------------------------------------------
ALTER TABLE roles            AUTO_INCREMENT = 10;
ALTER TABLE users            AUTO_INCREMENT = 20;
ALTER TABLE projects         AUTO_INCREMENT = 20;
ALTER TABLE project_members  AUTO_INCREMENT = 20;
ALTER TABLE tasks            AUTO_INCREMENT = 20;
ALTER TABLE task_assignments AUTO_INCREMENT = 20;
ALTER TABLE proof_submissions AUTO_INCREMENT = 20;
ALTER TABLE submission_files AUTO_INCREMENT = 20;
ALTER TABLE reviews          AUTO_INCREMENT = 20;
ALTER TABLE notifications    AUTO_INCREMENT = 20;
ALTER TABLE activity_logs    AUTO_INCREMENT = 100;

-- ============================================================================
--  Quick check after importing:
--    SELECT u.full_name, r.name AS role, u.status FROM users u JOIN roles r ON r.id = u.role_id;
--    SELECT * FROM v_project_progress;
-- ============================================================================
