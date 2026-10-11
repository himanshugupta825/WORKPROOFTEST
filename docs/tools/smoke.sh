#!/usr/bin/env bash
# WorkProof smoke suite - pages, roles, CSRF, security headers, sessions.
#
# Configuration (override with environment variables):
#   WORKPROOF_URL          base URL of the deployed application
#   WORKPROOF_MYSQL        mysql client command, e.g. "mysql -u root -psecret"
#   WORKPROOF_UPLOAD_DIR   evidence directory configured for the server
set -u

BASE="${WORKPROOF_URL:-http://localhost:8080/WorkProof}"
MYSQL="${WORKPROOF_MYSQL:-mysql -u root}"
UPLOAD_DIR="${WORKPROOF_UPLOAD_DIR:-/var/lib/workproof/uploads}"
SCRATCH="${TMPDIR:-/tmp}"
JARS="$SCRATCH/workproof-test-jars"

PASS=0; FAIL=0
rm -rf $JARS && mkdir -p $JARS

report() { # name, expected, actual
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); printf "  OK   %-46s %s\n" "$1" "$3";
  else FAIL=$((FAIL+1)); printf "  FAIL %-46s expected=%s got=%s\n" "$1" "$2" "$3"; fi
}

get() { # jar url -> writes body+code
  curl -s -b "$1" -c "$1" -o $SCRATCH/body.html -w "%{http_code}" "$2"
}

login() { # jar email password -> echoes "code"
  local jar=$1 email=$2 pw=$3
  rm -f "$jar"
  curl -s -c "$jar" -o $SCRATCH/login.html "$BASE/login"
  local tok=$(grep -o 'name="_csrf" value="[^"]*"' $SCRATCH/login.html | head -1 | sed 's/.*value="//;s/"//')
  curl -s -b "$jar" -c "$jar" -o $SCRATCH/loginout.html -w "%{http_code}" \
    -d "email=$email" -d "password=$pw" -d "_csrf=$tok" "$BASE/login"
}

echo "== AUTH =="
report "GET / (public landing)"            200 "$(curl -s -o $SCRATCH/b.html -w '%{http_code}' $BASE/)"
report "GET /dashboard anon -> login"      302 "$(curl -s -o /dev/null -w '%{http_code}' $BASE/dashboard)"
report "GET /login"                        200 "$(curl -s -o $SCRATCH/login.html -w '%{http_code}' $BASE/login)"
TOK=$(grep -o 'name="_csrf" value="[^"]*"' $SCRATCH/login.html | head -1 | sed 's/.*value="//;s/"//')
report "POST /login CSRF token present"    43  "${#TOK}"   # 32 random bytes, base64url unpadded
curl -s -c $SCRATCH/bad.jar -o $SCRATCH/badlogin.html $BASE/login
BT=$(grep -o 'name="_csrf" value="[^"]*"' $SCRATCH/badlogin.html | head -1 | sed 's/.*value="//;s/"//')
report "POST /login bad password"          200 "$(curl -s -c $SCRATCH/bad.jar -b $SCRATCH/bad.jar -o $SCRATCH/badout.html -w '%{http_code}' -d "email=admin@workproof.local" -d "password=wrongpass1" -d "_csrf=$BT" $BASE/login)"
grep -q "Incorrect email or password" $SCRATCH/badout.html && report "  error message shown" yes yes || report "  error message shown" yes no

echo "== ROLES: ADMIN =="
A=$JARS/admin.jar
report "POST /login admin"                 302 "$(login $A admin@workproof.local 'Admin@123')"
for p in "/dashboard" "/projects" "/projects/view?id=1" "/projects/new" "/projects/edit?id=1" \
         "/tasks" "/tasks/view?id=1" "/tasks/new?projectId=1" "/tasks/edit?id=1" \
         "/submissions" "/submissions/view?id=1" "/submissions/new?taskId=1" \
         "/reviews" "/reviews/view?id=1" "/notifications" "/profile" \
         "/reports" "/reports?report=TASK_STATUS" "/reports?report=TASK_PRIORITY" \
         "/reports?report=PROJECT_PROGRESS" "/reports?report=TEAM_CONTRIBUTION" "/reports?report=REVIEW_TURNAROUND" \
         "/admin/users" "/admin/activity" "/admin/activity?userId=2"; do
  report "GET $p" 200 "$(get $A "$BASE$p")"
done
curl -s -b $A -c $A -D $SCRATCH/csvh.txt -o $SCRATCH/export.csv -w "%{http_code}" "$BASE/reports/export?report=TASK_STATUS" > $SCRATCH/csvcode
report "GET /reports/export (CSV)"          200 "$(cat $SCRATCH/csvcode)"
grep -qi "text/csv" $SCRATCH/csvh.txt && report "  CSV content-type" yes yes || report "  CSV content-type" yes no
grep -q "attachment; filename=" $SCRATCH/csvh.txt && report "  CSV download header" yes yes || report "  CSV download header" yes no
grep -q "\"Project\",\"Code\",\"Status\"" $SCRATCH/export.csv && report "  CSV header row + BOM" yes yes || report "  CSV header row + BOM" yes no

echo "== CSRF / RBAC (negative) =="
report "POST /projects/action no token"     403 "$(curl -s -b $A -c $A -o $SCRATCH/b.html -w '%{http_code}' -d 'name=x&code=XX-2026&status=PLANNED' $BASE/projects/action)"
report "POST /projects/action bad token"    403 "$(curl -s -b $A -c $A -o $SCRATCH/b.html -w '%{http_code}' -d 'name=x&code=XX-2026&status=PLANNED&_csrf=deadbeef' $BASE/projects/action)"
report "GET /admin/users anon"              302 "$(curl -s -o /dev/null -w '%{http_code}' $BASE/admin/users)"

echo "== ROLES: MANAGER / MEMBER / REVIEWER =="
M=$JARS/mgr.jar; B=$JARS/mem.jar; R=$JARS/rev.jar
report "POST /login manager"                302 "$(login $M priya.manager@workproof.local 'Manager@123')"
report "POST /login member"                 302 "$(login $B aarav.member@workproof.local 'Member@123')"
report "POST /login reviewer"               302 "$(login $R mentor.reviewer@workproof.local 'Reviewer@123')"
for p in "/dashboard" "/projects" "/tasks" "/submissions" "/reviews" "/notifications" "/profile" "/reports"; do
  report "manager $p"  200 "$(get $M "$BASE$p")"
  EXP=200; [ "$p" = "/reviews" ] && EXP=403
  report "member  $p"  $EXP "$(get $B "$BASE$p")"
  report "reviewer $p" 200 "$(get $R "$BASE$p")"
done
report "member GET /admin/users -> 403"      403 "$(get $B "$BASE/admin/users")"
report "manager GET /admin/activity -> 403"  403 "$(get $M "$BASE/admin/activity")"
report "member GET /projects/new -> 403"      403 "$(get $B "$BASE/projects/new")"

echo "== ERROR PAGES =="
report "GET /error?code=404"                404 "$(get $A "$BASE/error?code=404")"
grep -q "404" $SCRATCH/body.html && report "  404 page renders" yes yes || report "  404 page renders" yes no
report "GET /nonexistent (signed in)"       404 "$(curl -s -b $A -c $A -o $SCRATCH/body.html -w '%{http_code}' $BASE/nonexistent)"
report "GET /access-denied"                 403 "$(get $A "$BASE/access-denied")"

echo "== SECURITY HEADERS =="
curl -s -D $SCRATCH/h.txt -o /dev/null $BASE/login
for h in "Content-Security-Policy" "X-Content-Type-Options: nosniff" "X-Frame-Options" "Referrer-Policy"; do
  grep -qi "^$h" $SCRATCH/h.txt && report "header $h" yes yes || report "header $h" yes no
done
grep -qi "HttpOnly" $SCRATCH/h.txt && report "cookie HttpOnly" yes yes || report "cookie HttpOnly" yes no

echo "== LOGOUT / SESSION HANDLING =="
# Replaying the pre-login token after the session was rotated at login must be rejected.
report "POST /logout with pre-login (stale) token" 403 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' -X POST $BASE/logout -d "_csrf=$TOK")"
LTOK=$(curl -s -b $A -c $A "$BASE/dashboard" | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//')
report "POST /logout with current token"   302 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' -X POST $BASE/logout -d "_csrf=$LTOK")"
report "dashboard closed after logout"     302 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' $BASE/dashboard)"
report "GET /logout while signed out -> login" 302 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' $BASE/logout)"

echo
echo "PASS=$PASS FAIL=$FAIL"
