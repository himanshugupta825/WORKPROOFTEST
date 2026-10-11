#!/usr/bin/env bash
# WorkProof workflow suite - the full write pipeline plus edge cases.
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

dbq() { $MYSQL -N -B workproof -e "$1"; }

PASS=0; FAIL=0
ok()   { PASS=$((PASS+1)); printf "  OK   %-52s %s\n" "$1" "$2"; }
bad()  { FAIL=$((FAIL+1)); printf "  FAIL %-52s %s\n" "$1" "$2"; }
chk()  { [ "$2" = "$3" ] && ok "$1" "$3" || bad "$1" "expected=$2 got=$3"; }
code() { curl -s -b "$1" -c "$1" -o $SCRATCH/flowbody.html -w "%{http_code}" "${@:2}"; }
loc()  { grep -io "^location: .*" $SCRATCH/headers.txt | tr -d '\r' | sed 's/.*: //'; }

tok() { # jar -> csrf token from any rendered page
  curl -s -b "$1" -c "$1" "$BASE/dashboard" | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'
}

login() { # jar email password
  rm -f "$1"
  curl -s -c "$1" -o $SCRATCH/l.html "$BASE/login"
  local t=$(grep -o 'name="_csrf" value="[^"]*"' $SCRATCH/l.html | head -1 | sed 's/.*value="//;s/"//')
  curl -s -b "$1" -c "$1" -o /dev/null -w "%{http_code}" -d "email=$2" -d "password=$3" -d "_csrf=$t" "$BASE/login"
}

# --- reset the data this suite creates, so it can be re-run ---
$MYSQL workproof -e "DELETE FROM projects WHERE code='VIVA-2026'; DELETE FROM users WHERE email IN ('test.student@workproof.local','weak@workproof.local');"
rm -rf "$UPLOAD_DIR"/* 2>/dev/null || true

J="$JARS"; mkdir -p $J
M=$J/f_mgr.jar; B=$J/f_mem.jar; R=$J/f_rev.jar

echo "== LOGIN =="
chk "manager login"  302 "$(login $M priya.manager@workproof.local 'Manager@123')"
chk "member login"   302 "$(login $B aarav.member@workproof.local 'Member@123')"
chk "reviewer login" 302 "$(login $R mentor.reviewer@workproof.local 'Reviewer@123')"
TM=$(tok $M); TB=$(tok $B); TR=$(tok $R)
chk "csrf token on dashboard (manager)" 43 "${#TM}"
chk "csrf token on dashboard (member)"  43 "${#TB}"

echo "== CREATE PROJECT (manager) =="
CREATE_CODE=$(curl -s -b $M -c $M -D $SCRATCH/headers.txt -o /dev/null -w "%{http_code}" -X POST \
  --data-urlencode "name=Viva Demo Project" --data-urlencode "code=VIVA-2026" \
  --data-urlencode "description=End-to-end smoke test project created through the UI." \
  --data-urlencode "startDate=2026-10-01" --data-urlencode "endDate=2026-12-15" \
  --data-urlencode "status=ACTIVE" --data-urlencode "_csrf=$TM" "$BASE/projects/new")
chk "POST /projects/new" 302 "$CREATE_CODE"
PLOC=$(loc $SCRATCH/headers.txt); echo "      -> $PLOC"
PID=$(echo "$PLOC" | sed 's/.*id=//')
DBPID=$(dbq "SELECT id FROM projects WHERE code='VIVA-2026';")
chk "project row in DB" "$PID" "$DBPID"

echo "== PROJECT MEMBERS =="
curl -s -b $M -c $M -o /dev/null -w "%{http_code}" -X POST \
  -d "action=addMember" -d "projectId=$PID" -d "userId=3" -d "roleInProject=MEMBER" -d "_csrf=$TM" \
  "$BASE/projects/action" > $SCRATCH/c2
chk "add member aarav" 302 "$(cat $SCRATCH/c2)"
curl -s -b $M -c $M -o /dev/null -w "%{http_code}" -X POST \
  -d "action=addMember" -d "projectId=$PID" -d "userId=6" -d "roleInProject=REVIEWER" -d "_csrf=$TM" \
  "$BASE/projects/action" > $SCRATCH/c3
chk "add reviewer mentor" 302 "$(cat $SCRATCH/c3)"
chk "memberships in DB (manager+member+reviewer)" 3 "$(dbq "SELECT COUNT(*) FROM project_members WHERE project_id=$PID;")"

echo "== CREATE TASK (manager assigns to member) =="
curl -s -b $M -c $M -D $SCRATCH/headers.txt -o /dev/null -X POST \
  --data-urlencode "projectId=$PID" --data-urlencode "title=Write proof-of-work test script" \
  --data-urlencode "description=Exercise the full submit-review-complete pipeline." \
  --data-urlencode "priority=HIGH" --data-urlencode "startDate=2026-10-05" \
  --data-urlencode "dueDate=2026-10-20" --data-urlencode "assigneeIds=3" \
  --data-urlencode "_csrf=$TM" "$BASE/tasks/new"
TLOC=$(loc $SCRATCH/headers.txt); echo "      -> $TLOC"
TID=$(echo "$TLOC" | sed 's/.*id=//')
chk "task row in DB" "$(dbq "SELECT id FROM tasks WHERE project_id=$PID;")" "$TID"
chk "task assigned to member" 1 "$(dbq "SELECT COUNT(*) FROM task_assignments WHERE task_id=$TID AND user_id=3;")"
chk "task starts TODO" TODO "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"

echo "== MEMBER MOVES TASK STATUS =="
curl -s -b $B -c $B -o /dev/null -w "%{http_code}" -X POST \
  -d "taskId=$TID" -d "action=status" -d "status=IN_PROGRESS" -d "comment=Starting work" -d "_csrf=$TB" \
  "$BASE/tasks/action" > $SCRATCH/c4
chk "member TODO->IN_PROGRESS" 302 "$(cat $SCRATCH/c4)"
chk "status in DB" IN_PROGRESS "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"
# invalid transition must be refused
curl -s -b $B -c $B -o /dev/null -w "%{http_code}" -X POST \
  -d "taskId=$TID" -d "action=status" -d "status=COMPLETED" -d "_csrf=$TB" "$BASE/tasks/action" > $SCRATCH/c5
chk "illegal transition IN_PROGRESS->COMPLETED blocked" 302 "$(cat $SCRATCH/c5)"
chk "status unchanged after illegal move" IN_PROGRESS "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"

echo "== MEMBER SUBMITS PROOF (multipart upload) =="
printf 'design notes for the smoke test\n' > $SCRATCH/evidence.txt
printf '%%PDF-1.4 fake pdf for validation test\n' > $SCRATCH/evidence.pdf
curl -s -b $B -c $B -D $SCRATCH/headers.txt -o /dev/null -X POST \
  -F "taskId=$TID" -F "title=Smoke test evidence" -F "description=Uploading two files to prove the storage + validation pipeline works end to end." \
  -F "files=@$SCRATCH/evidence.txt" -F "files=@$SCRATCH/evidence.pdf" -F "_csrf=$TB" \
  "$BASE/submissions/new"
SLOC=$(loc $SCRATCH/headers.txt); echo "      -> $SLOC"
SID=$(echo "$SLOC" | sed 's/.*id=//')
chk "submission row" 1 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$TID AND version=1;")"
chk "files stored in DB" 2 "$(dbq "SELECT COUNT(*) FROM submission_files sf JOIN proof_submissions s ON s.id=sf.submission_id WHERE s.task_id=$TID;")"
chk "files on disk (outside webroot)" 2 "$(find $UPLOAD_DIR -type f | wc -l)"
chk "task moved to SUBMITTED" SUBMITTED "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"
curl -s -b $B -c $B -o $SCRATCH/exe.html -w "%{http_code}" -X POST -F "taskId=$TID" -F "title=Bad file" -F "description=Attempting to upload an executable which must be rejected by the server." -F "files=@/bin/ls;filename=payload.exe" -F "_csrf=$TB" $BASE/submissions/new > $SCRATCH/execode
chk "upload of .exe re-renders form (200)" 200 "$(cat $SCRATCH/execode)"
grep -qi "not allowed\|not accepted\|file type" $SCRATCH/exe.html && chk "  rejection reason shown to user" yes yes || chk "  rejection reason shown" yes no
chk "no exe file stored" 0 "$(find $UPLOAD_DIR -name '*.exe' | wc -l)"

echo "== MEMBER CANNOT SELF-APPROVE =="
chk "member blocked from /reviews/view" 403 "$(code $B "$BASE/reviews/view?id=$SID")"

echo "== REVIEWER APPROVES =="
chk "reviewer sees submission" 200 "$(code $R "$BASE/reviews/view?id=$SID")"
curl -s -b $R -c $R -o /dev/null -w "%{http_code}" -X POST \
  --data-urlencode "submissionId=$SID" --data-urlencode "decision=APPROVED" \
  --data-urlencode "comments=Verified the evidence, both files open correctly. Approved." \
  --data-urlencode "_csrf=$TR" "$BASE/reviews/decision" > $SCRATCH/c6
chk "review decision POST" 302 "$(cat $SCRATCH/c6)"
chk "review row" APPROVED "$(dbq "SELECT decision FROM reviews WHERE submission_id=$SID;")"
chk "submission APPROVED" APPROVED "$(dbq "SELECT status FROM proof_submissions WHERE id=$SID;")"
chk "task APPROVED" APPROVED "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"
chk "notification to member (approval)" 1 "$(dbq "SELECT COUNT(*) FROM notifications WHERE user_id=3 AND link='/submissions/view?id=$SID';")"
chk "activity log written" 0 "$(dbq "SELECT COUNT(*) FROM activity_logs WHERE entity_type='SUBMISSION' AND entity_id=$SID;" | awk '{print ($1>0)?0:1}')"

echo "== REJECTION REQUIREMENT (feedback mandatory) =="
RID=$(dbq "SELECT id FROM tasks t WHERE t.project_id=$PID AND t.id<>$TID LIMIT 1;")
echo "      (reusing submission $SID for a second review is blocked: only PENDING can be decided)"
chk "already-decided submission cannot be re-decided" 302 "$(curl -s -b $R -c $R -o /dev/null -w '%{http_code}' -X POST --data-urlencode "submissionId=$SID" --data-urlencode "decision=REJECTED" --data-urlencode "comments=Changing my mind about this one now." --data-urlencode "_csrf=$TR" $BASE/reviews/decision)"
chk "decision unchanged" APPROVED "$(dbq "SELECT decision FROM reviews WHERE submission_id=$SID;")"

echo "== MANAGER COMPLETES TASK =="
curl -s -b $M -c $M -o /dev/null -w "%{http_code}" -X POST \
  -d "taskId=$TID" -d "action=status" -d "status=COMPLETED" -d "_csrf=$TM" "$BASE/tasks/action" > $SCRATCH/c7
chk "APPROVED->COMPLETED" 302 "$(cat $SCRATCH/c7)"
chk "task completed in DB" COMPLETED "$(dbq "SELECT status FROM tasks WHERE id=$TID;")"

echo "== AUTHORISED DOWNLOAD =="
FID=$(dbq "SELECT sf.id FROM submission_files sf JOIN proof_submissions s ON s.id=sf.submission_id WHERE s.task_id=$TID LIMIT 1;")
chk "member downloads own file"  200 "$(code $B "$BASE/submissions/download?id=$FID")"
chk "reviewer downloads file"    200 "$(code $R "$BASE/submissions/download?id=$FID")"
OUT=$(sed -n '3p' $SCRATCH/jars/f_mem.jar >/dev/null; echo)
chk "unauthenticated download blocked" 302 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/submissions/download?id=$FID")"

echo "== ADMIN MODULE =="
A=$J/f_admin.jar
chk "admin login" 302 "$(login $A admin@workproof.local 'Admin@123')"
TA=$(tok $A)
chk "admin create user (missing confirm -> refused)" 302 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' -X POST --data-urlencode "action=create" --data-urlencode "fullName=Test Student" --data-urlencode "email=test.student@workproof.local" --data-urlencode "password=Student@123" --data-urlencode "role=MEMBER" --data-urlencode "_csrf=$TA" $BASE/admin/users/action)"
chk "  no user row created" 0 "$(dbq "SELECT COUNT(*) FROM users WHERE email='test.student@workproof.local';")"
chk "admin create user" 302 "$(curl -s -b $A -c $A -o /dev/null -w '%{http_code}' -X POST --data-urlencode "action=create" --data-urlencode "fullName=Test Student" --data-urlencode "email=test.student@workproof.local" --data-urlencode "password=Student@123" --data-urlencode "confirmPassword=Student@123" --data-urlencode "role=MEMBER" --data-urlencode "department=CSE" --data-urlencode "_csrf=$TA" $BASE/admin/users/action)"
chk "new user in DB with BCrypt hash" yes "$(dbq "SELECT IF(password_hash LIKE '\$2a\$%' AND password_hash NOT LIKE '%Student%','yes','no') FROM users WHERE email='test.student@workproof.local';")"
chk "new user can log in" 302 "$(login $J/f_new.jar test.student@workproof.local 'Student@123')"

echo "== FILTERS / SEARCH / EXPORT =="
chk "task search + status filter" 200 "$(code $M "$BASE/tasks?q=proof&status=COMPLETED&projectId=$PID")"
chk "reports date filter" 200 "$(code $M "$BASE/reports?report=TEAM_CONTRIBUTION&from=2026-10-01&to=2026-12-31")"
chk "notifications mark all read" 302 "$(curl -s -b $B -c $B -o /dev/null -w '%{http_code}' -X POST -d 'action=readAll' -d "_csrf=$TB" $BASE/notifications/action)"
chk "unread count zero" 0 "$(dbq "SELECT COUNT(*) FROM notifications WHERE user_id=3 AND is_read=0;")"

echo "== REJECTION + RESUBMISSION (history preserved) =="
curl -s -b $M -c $M -D $SCRATCH/headers.txt -o /dev/null -X POST \
  --data-urlencode "projectId=$PID" --data-urlencode "title=Second deliverable needing rework" \
  --data-urlencode "description=Task used to demonstrate the reject and resubmit cycle." \
  --data-urlencode "priority=MEDIUM" --data-urlencode "dueDate=2026-11-30" \
  --data-urlencode "assigneeIds=3" --data-urlencode "_csrf=$TM" "$BASE/tasks/new"
T2=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
curl -s -b $B -c $B -o /dev/null -X POST -d "taskId=$T2" -d "action=status" -d "status=SUBMITTED" -d "_csrf=$TB" "$BASE/tasks/action"
printf 'v1 evidence\n' > $SCRATCH/v1.txt
curl -s -b $B -c $B -D $SCRATCH/headers.txt -o /dev/null -X POST \
  -F "taskId=$T2" -F "title=First attempt" -F "description=First attempt at this deliverable, reviewer feedback expected." \
  -F "files=@$SCRATCH/v1.txt" -F "_csrf=$TB" "$BASE/submissions/new"
S2=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
chk "second submission v1 created" 1 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T2 AND version=1;")"
chk "task is SUBMITTED" SUBMITTED "$(dbq "SELECT status FROM tasks WHERE id=$T2;")"
# rejecting without feedback must be refused
curl -s -b $R -c $R -o $SCRATCH/rej.html -w "%{http_code}" -X POST \
  --data-urlencode "submissionId=$S2" --data-urlencode "decision=REJECTED" --data-urlencode "comments=bad" \
  --data-urlencode "_csrf=$TR" "$BASE/reviews/decision" > $SCRATCH/rejcode
chk "reject with <10 char feedback refused" 302 "$(cat $SCRATCH/rejcode)"
chk "  submission still PENDING" PENDING "$(dbq "SELECT status FROM proof_submissions WHERE id=$S2;")"
FLASH=$(curl -s -b $R -c $R -L "$BASE/reviews/view?id=$S2")
echo "$FLASH" | grep -qi "at least 10" && chk "  reason explains the rule" yes yes || chk "  reason explains the rule" yes no
# proper rejection
curl -s -b $R -c $R -o /dev/null -w "%{http_code}" -X POST \
  --data-urlencode "submissionId=$S2" --data-urlencode "decision=REJECTED" \
  --data-urlencode "comments=The report is missing the test-results section and two screenshots. Please add them and resubmit." \
  --data-urlencode "_csrf=$TR" "$BASE/reviews/decision" > $SCRATCH/rej2code
chk "reject with feedback" 302 "$(cat $SCRATCH/rej2code)"
chk "submission REJECTED" REJECTED "$(dbq "SELECT status FROM proof_submissions WHERE id=$S2;")"
chk "task back to REJECTED" REJECTED "$(dbq "SELECT status FROM tasks WHERE id=$T2;")"
chk "feedback stored" 1 "$(dbq "SELECT COUNT(*) FROM reviews WHERE submission_id=$S2 AND decision='REJECTED' AND CHAR_LENGTH(comments)>10;")"
chk "member notified of rejection" 1 "$(dbq "SELECT COUNT(*) FROM notifications WHERE user_id=3 AND title LIKE '%ejected%' AND link='/submissions/view?id=$S2';")"
# resubmission -> version 2, history kept
printf 'v2 evidence with fixes\n' > $SCRATCH/v2.txt
curl -s -b $B -c $B -D $SCRATCH/headers.txt -o /dev/null -X POST \
  -F "taskId=$T2" -F "title=Second attempt with fixes" \
  -F "description=Added the test-results section and the two requested screenshots as the reviewer asked." \
  -F "files=@$SCRATCH/v2.txt" -F "_csrf=$TB" "$BASE/submissions/new"
S3=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
chk "resubmission got a NEW id" "$([ "$S3" != "$S2" ] && echo yes)" yes
chk "version 2 recorded" 2 "$(dbq "SELECT MAX(version) FROM proof_submissions WHERE task_id=$T2;")"
chk "rejected v1 keeps its decision (history not rewritten)" REJECTED "$(dbq "SELECT status FROM proof_submissions WHERE id=$S2;")"
chk "old review still stored" 1 "$(dbq "SELECT COUNT(*) FROM reviews WHERE submission_id=$S2;")"
chk "new submission pending" PENDING "$(dbq "SELECT status FROM proof_submissions WHERE id=$S3;")"
chk "task SUBMITTED again" SUBMITTED "$(dbq "SELECT status FROM tasks WHERE id=$T2;")"
chk "resubmission is a distinct row (history kept)" 2 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T2;")"

echo "== DUPLICATE / VALIDATION GUARDS =="
chk "duplicate project code refused (form re-rendered)" 200 "$(curl -s -b $M -c $M -o /dev/null -w '%{http_code}' -X POST --data-urlencode "name=Duplicate code attempt" --data-urlencode "code=VIVA-2026" --data-urlencode "description=A second project trying to reuse an existing code value." --data-urlencode "startDate=2026-10-01" --data-urlencode "endDate=2026-12-01" --data-urlencode "status=PLANNED" --data-urlencode "_csrf=$TM" $BASE/projects/new)"
chk "  only one project with that code" 1 "$(dbq "SELECT COUNT(*) FROM projects WHERE code='VIVA-2026';")"
rm -f $SCRATCH/reg.jar
curl -s -c $SCRATCH/reg.jar -o $SCRATCH/regform.html "$BASE/register"
RT=$(grep -o 'name="_csrf" value="[^"]*"' $SCRATCH/regform.html | head -1 | sed 's/.*value="//;s/"//')
chk "weak password refused at registration" 200 "$(curl -s -b $SCRATCH/reg.jar -c $SCRATCH/reg.jar -o $SCRATCH/regout.html -w '%{http_code}' -X POST -d 'email=weak@workproof.local' -d 'password=password' -d "fullName=Weak Tester" -d "confirmPassword=password" -d 'role=MEMBER' -d "_csrf=$RT" $BASE/register)"
chk "  no account created" 0 "$(dbq "SELECT COUNT(*) FROM users WHERE email='weak@workproof.local';")"

echo "== ONE LIVE SUBMISSION PER TASK + SUPERSEDED =="
curl -s -b $M -c $M -D $SCRATCH/headers.txt -o /dev/null -X POST \
  --data-urlencode "projectId=$PID" --data-urlencode "title=Third deliverable for supersede test" \
  --data-urlencode "description=Task used to demonstrate the superseded status." \
  --data-urlencode "priority=LOW" --data-urlencode "dueDate=2026-12-01" \
  --data-urlencode "assigneeIds=3" --data-urlencode "_csrf=$TM" "$BASE/tasks/new"
T3=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
printf 'attempt A\n' > $SCRATCH/a.txt; printf 'attempt B improved\n' > $SCRATCH/b.txt
curl -s -b $B -c $B -D $SCRATCH/headers.txt -o /dev/null -X POST -F "taskId=$T3" -F "title=Attempt A" -F "description=First upload, still waiting for a reviewer." -F "files=@$SCRATCH/a.txt" -F "_csrf=$TB" "$BASE/submissions/new"
SA=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
chk "upload A created (PENDING)" PENDING "$(dbq "SELECT status FROM proof_submissions WHERE id=$SA;")"
# while a review is pending a second upload must be refused (reviewer must not chase a moving target)
curl -s -b $B -c $B -o $SCRATCH/dup.html -w "%{http_code}" -X POST -F "taskId=$T3" -F "title=Attempt A again" -F "description=Trying to replace evidence that is already in the review queue." -F "files=@$SCRATCH/b.txt" -F "_csrf=$TB" "$BASE/submissions/new" > $SCRATCH/dupcode
chk "second upload while PENDING refused (200)" 200 "$(cat $SCRATCH/dupcode)"
grep -qi "already has evidence waiting" $SCRATCH/dup.html && chk "  rule explained to the member" yes yes || chk "  rule explained" yes no
chk "still exactly one submission" 1 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T3;")"
# reopen the task (SUBMITTED -> IN_PROGRESS) and upload the improved version
chk "member reopens task (SUBMITTED->IN_PROGRESS)" 302 "$(curl -s -b $B -c $B -o /dev/null -w '%{http_code}' -X POST -d "taskId=$T3" -d "action=status" -d "status=IN_PROGRESS" -d "comment=Adding missing screenshots" -d "_csrf=$TB" $BASE/tasks/action)"
curl -s -b $B -c $B -D $SCRATCH/headers.txt -o /dev/null -X POST -F "taskId=$T3" -F "title=Attempt B improved" -F "description=Replacing the earlier upload with a corrected version and screenshots." -F "files=@$SCRATCH/b.txt" -F "_csrf=$TB" "$BASE/submissions/new"
SB=$(loc $SCRATCH/headers.txt | sed 's/.*id=//')
chk "upload B created (PENDING)" PENDING "$(dbq "SELECT status FROM proof_submissions WHERE id=$SB;")"
chk "older waiting version marked SUPERSEDED" SUPERSEDED "$(dbq "SELECT status FROM proof_submissions WHERE id=$SA;")"
chk "review queue has ONE pending for the task" 1 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T3 AND status='PENDING';")"
chk "both versions kept (history preserved)" 2 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T3;")"
chk "reviewer sees the superseded version in history" 1 "$(dbq "SELECT COUNT(*) FROM proof_submissions WHERE task_id=$T3 AND status='SUPERSEDED';")"

echo "== EDGE CASES (bad input must never produce a 500) =="
for probe in "/projects/view?id=999999" "/tasks/view?id=999999" "/submissions/view?id=999999" \
             "/reviews/view?id=999999" "/projects/view?id=abc" "/tasks/view?id=" \
             "/projects/edit?id=999999" "/submissions/new?taskId=999999"; do
  C=$(code $M "$BASE$probe")
  case "$C" in
    404|403|302) ok "no 500 for $probe" "$C" ;;
    500) bad "no 500 for $probe" "$C" ;;
    *)   ok "no 500 for $probe" "$C" ;;
  esac
done
chk "unknown report slug falls back safely" 200 "$(code $M "$BASE/reports?report=does-not-exist")"
chk "broken date filter ignored"            200 "$(code $M "$BASE/reports?report=project-progress&from=not-a-date")"
chk "page=abc tolerated"                    200 "$(code $M "$BASE/tasks?page=abc")"
chk "page=99999 tolerated"                  200 "$(code $M "$BASE/tasks?page=99999")"
chk "unknown task status filter tolerated"  200 "$(code $M "$BASE/tasks?status=NONSENSE")"
chk "search with SQL metacharacters"        200 "$(curl -s -b $M -c $M -o /dev/null -w '%{http_code}' --get --data-urlencode "q=' OR 1=1 --" "$BASE/tasks")"

echo
echo "PASS=$PASS FAIL=$FAIL"
