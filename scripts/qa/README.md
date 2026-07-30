# Exam Experience QA seed

This QA-only script targets `owlexa_qa` and never runs against `owlexa_db`.

Prerequisites: start the backend on port `18081` with `FILE_LOCAL_ROOT` set to a QA-only directory, `FILE_PUBLIC_BASE_URL=http://localhost:18081/uploads`, and set `QA_TEACHER_PASSWORD` only in the current shell. The script performs a database/Flyway guard before any mutation and uses API calls for file lifecycle, assessment snapshots, and recipients.

Run:

```powershell
$env:MYSQL_PWD = '<local database secret>'
$env:QA_TEACHER_PASSWORD = '<local teacher secret>'
.\scripts\qa\seed-exam-experience.ps1
```

The script is fail-fast. It reuses only an unambiguous, verified QA namespace state; it never performs cleanup.
