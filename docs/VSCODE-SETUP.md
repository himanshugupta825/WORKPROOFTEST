# WorkProof — Ubuntu + VS Code Setup Guide

Everything needed to go from a fresh Ubuntu machine to a running WorkProof instance, plus the
commands to use at each milestone of `docs/PROJECT-PLAN.md`.

Tested with: **Ubuntu 22.04/24.04**, **JDK 17 (Temurin)**, **Maven 3.9.x**, **Tomcat 9.0.107**,
**MySQL 8.0**.

---

## 1. Install the toolchain

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mysql-server curl unzip

java -version     # openjdk version "17.x"
mvn -v            # Apache Maven 3.9.x  (Java version: 17.x)
mysql --version   # mysql  Ver 8.0.x
```

If `java -version` still shows 11, select 17 explicitly:

```bash
sudo update-alternatives --config java      # choose the JDK 17 entry
```

### Apache Tomcat 9

```bash
cd /opt
sudo curl -fLO https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.107/bin/apache-tomcat-9.0.107.tar.gz
sudo tar xzf apache-tomcat-9.0.107.tar.gz
sudo mv apache-tomcat-9.0.107 tomcat
sudo chown -R $USER:$USER /opt/tomcat
echo 'export CATALINA_HOME=/opt/tomcat' >> ~/.bashrc
echo 'export PATH=$CATALINA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
$CATALINA_HOME/bin/version.sh
```

> **Use Tomcat 9, not 10 or 11.** Tomcat 10+ implements `jakarta.servlet.*`; this project is built
> on `javax.servlet.*` and will throw `ClassNotFoundException: javax.servlet.http.HttpServlet` there.

## 2. Set up MySQL

```bash
sudo systemctl enable --now mysql

sudo mysql <<'SQL'
CREATE DATABASE IF NOT EXISTS workproof CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'workproof_app'@'localhost' IDENTIFIED BY 'ChangeThis#2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON workproof.* TO 'workproof_app'@'localhost';
FLUSH PRIVILEGES;
SQL
```

Then load the schema and the demo scenario **from the project folder**:

```bash
cd ~/projects/WorkProof
mysql -u root -p < database/schema.sql
mysql -u root -p workproof < database/seed.sql

mysql -u workproof_app -p'ChangeThis#2026' -e "SELECT COUNT(*) FROM workproof.users;"   # 7
```

> The web application never connects as `root`. `workproof_app` holds only the four DML privileges —
> it cannot drop a table even if something goes wrong.

## 3. Configure the application

`src/main/resources/workproof.properties` in the repository contains **safe defaults only** — no
passwords. Provide yours in one of these ways (checked in this order):

**A. Environment variables (recommended for Tomcat):**

```bash
cat > $CATALINA_HOME/bin/setenv.sh <<'EOF'
export WORKPROOF_DB_URL="jdbc:mysql://localhost:3306/workproof?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata&characterEncoding=UTF-8&useUnicode=true"
export WORKPROOF_DB_USER="workproof_app"
export WORKPROOF_DB_PASSWORD="ChangeThis#2026"
export WORKPROOF_UPLOAD_DIR="/var/lib/workproof/uploads"
EOF
chmod +x $CATALINA_HOME/bin/setenv.sh
sudo mkdir -p /var/lib/workproof/uploads && sudo chown -R $USER:$USER /var/lib/workproof
```

(A ready-made template is in `scripts/tomcat-setenv.sh.example`.)

**B. A local properties file (handy inside VS Code):**

```bash
cp src/main/resources/workproof-local.properties.example src/main/resources/workproof-local.properties
# edit it and put your real password in
```

`workproof-local.properties` is in `.gitignore`, so it never reaches GitHub.

| Setting | Environment variable | Default |
|---|---|---|
| JDBC URL | `WORKPROOF_DB_URL` | `jdbc:mysql://localhost:3306/workproof…` |
| DB user / password | `WORKPROOF_DB_USER` / `WORKPROOF_DB_PASSWORD` | — / — |
| Pool size | `WORKPROOF_DB_POOL_SIZE` | `10` |
| Upload directory | `WORKPROOF_UPLOAD_DIR` | `~/workproof-uploads` |
| Max file size (MB) | `WORKPROOF_UPLOAD_MAX_MB` | `10` |
| Session timeout (min) | `WORKPROOF_SESSION_TIMEOUT_MIN` | `30` |
| Allow self-review | `WORKPROOF_ALLOW_SELF_REVIEW` | `false` |

## 4. Open the project in VS Code

```bash
cd ~/projects/WorkProof
code .
```

### Recommended extensions

`extensions.json` in `.vscode/` already lists these, so VS Code offers them as *recommended* when you
open the folder. `tasks.json` (also shipped) adds the build/deploy/test commands from section 7.

| Extension | Why |
|---|---|
| **Extension Pack for Java** (Microsoft) | Compile, run, debug, JUnit integration |
| **Community Server Connectors** (Red Hat) | Start/stop Tomcat and deploy the WAR from the sidebar |
| **XML** (Red Hat) | Schema-aware editing of `pom.xml`, `web.xml`, the TLD |
| **SQLTools** + **SQLTools MySQL/MariaDB driver** | Run the audit queries in `docs/TEST-CHECKLIST.md` without leaving the editor |
| **Live Server** *(optional)* | Only useful for the static `assets/` CSS work |

### Point VS Code at JDK 17

`.vscode/settings.json` — **local only** (it contains a machine-specific JDK path, so `.gitignore`
keeps it out of Git). Create it if it does not exist:

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "/usr/lib/jvm/java-17-openjdk-amd64",
      "default": true
    }
  ],
  "java.compile.nullAnalysis.mode": "automatic",
  "files.exclude": {
    "**/target": true
  }
}
```

Then: **Ctrl+Shift+P → Java: Clean Java Language Server Workspace → Restart and delete**.

### How the folders map to what you edit

| Path | What it is |
|---|---|
| `src/main/java/com/workproof/controller` | Servlets — read the request, call a service, render a view |
| `…/service` | Business rules, transactions, `Permissions` (all RBAC) |
| `…/dao` | **All SQL** — JDBC with `PreparedStatement` via `BaseDao` |
| `…/model` | POJOs and enums (`TaskStatus.allowedNext()` is the workflow) |
| `…/filter`, `…/listener` | Security filters and the start-up/shutdown hook |
| `src/main/webapp/WEB-INF/views` | JSPs — presentation only, never SQL |
| `src/main/webapp/assets` | CSS, JS and the vendored Bootstrap build |
| `database/` | `schema.sql` and `seed.sql` |
| `docs/` | This guide, SRS, ERD, test checklist, plan, test scripts |

## 5. Build and run

```bash
# 1. compile only (fast feedback while editing Java)
mvn -q compile

# 2. run the unit tests
mvn test

# 3. build the deployable archive
mvn clean package                 # -> target/WorkProof.war

# 4. deploy and start
cp target/WorkProof.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
tail -f $CATALINA_HOME/logs/catalina.out     # watch the start-up banner
```

Open **http://localhost:8080/WorkProof/** and sign in as `admin@workproof.local` / `Admin@123`.

### Redeploying after a change

```bash
mvn clean package && \
cp target/WorkProof.war $CATALINA_HOME/webapps/ && \
$CATALINA_HOME/bin/shutdown.sh && sleep 3 && \
rm -rf $CATALINA_HOME/work/Catalina/localhost/WorkProof && \
$CATALINA_HOME/bin/startup.sh
```

Deleting the `work/` directory forces Jasper to recompile every JSP — useful when a JSP change does
not seem to take effect.

### Deploying from VS Code instead

`Community Server Connectors` → **Servers** view → `+` → *Tomcat 9* → point at `/opt/tomcat` →
right-click **Start**. Then right-click the server → **Add Deployment** → choose
`target/WorkProof.war` → **Publish**.

## 6. Milestone checklist

| Milestone | Command that proves it |
|---|---|
| Week 1 — schema and seed | `mysql -u root -p < database/schema.sql && mysql -u root -p workproof < database/seed.sql` |
| Week 2 — compiles and connects | `mvn -q compile` then check the start-up banner for `Database connection: OK` |
| Week 3 — filters active | `curl -i localhost:8080/WorkProof/dashboard` → 302 to `/login` with security headers |
| Week 4 — accounts and dashboards | log in as all four seeded roles |
| Week 5 — projects and tasks | `bash docs/tools/flow.sh` → sections up to *MEMBER MOVES TASK STATUS* |
| Week 6 — upload and review | `bash docs/tools/flow.sh` → the submission and review sections |
| Week 7 — everything | `mvn test` (80) + `bash docs/tools/smoke.sh` (82) + `bash docs/tools/flow.sh` (94) |

## 7. VS Code tasks (optional convenience)

`.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    { "label": "mvn test",     "type": "shell", "command": "mvn test",         "group": "test" },
    { "label": "mvn package",  "type": "shell", "command": "mvn clean package","group": "build" },
    { "label": "deploy to Tomcat", "type": "shell",
      "command": "mvn clean package && cp target/WorkProof.war $CATALINA_HOME/webapps/ && $CATALINA_HOME/bin/shutdown.sh; sleep 3; $CATALINA_HOME/bin/startup.sh" },
    { "label": "smoke suite",  "type": "shell", "command": "bash docs/tools/smoke.sh" },
    { "label": "flow suite",   "type": "shell", "command": "bash docs/tools/flow.sh" }
  ]
}
```

Run them with **Ctrl+Shift+P → Tasks: Run Task**.

## 8. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `ClassNotFoundException: javax.servlet.http.HttpServlet` | You deployed to Tomcat 10+. Use Tomcat 9. |
| `Access denied for user 'workproof_app'@'localhost'` | Password mismatch in `setenv.sh`, or the `GRANT` was never run. |
| `Public Key Retrieval is not allowed` | Add `allowPublicKeyRetrieval=true` to the JDBC URL. |
| `Unknown database 'workproof'` | `schema.sql` was not imported. |
| `Port 8080 already in use` | `sudo lsof -i :8080` — another Tomcat is running; stop it or change the port in `conf/server.xml`. |
| Login says "Please sign in to continue." in a loop | Cookies are disabled, or a proxy is stripping `JSESSIONID`. |
| Old JSP still shown after a change | Stop Tomcat, delete `$CATALINA_HOME/work/Catalina/localhost/WorkProof`, start again. |
| Uploads fail: "storage directory is not writable" | `sudo mkdir -p /var/lib/workproof/uploads && sudo chown -R $USER:$USER /var/lib/workproof`. |
| `mvn test` fails with "invalid target release: 17" | Maven is running on an older JDK — `sudo update-alternatives --config java` and check `mvn -v`. |
| VS Code shows errors but `mvn compile` is clean | **Java: Clean Java Language Server Workspace**, then reload the window. |

## 9. Git hygiene

```bash
git init
git add .
git commit -m "WorkProof 1.0.0 - complete work management and proof-of-work verification system"
git branch -M main
git remote add origin https://github.com/<you>/workproof.git
git push -u origin main
```

`.gitignore` already excludes `target/`, IDE folders, `workproof-local.properties`, uploads, logs and
key files. **Never** commit a real database password, and rotate it if one ever appears in history:

```bash
git log -p | grep -i "password" | head        # check before pushing
```
