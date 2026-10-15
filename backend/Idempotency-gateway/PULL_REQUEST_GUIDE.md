# Pull Request Tutorial — Learn by Doing

This guide uses the real example branch in this repo:

- **Branch:** `feature/payment-request-validation`
- **Feature:** Validate `POST /process-payment` body (`src/main/kotlin/idempotency/api/PaymentRoutes.kt`)
  - `amount` must be `> 0`
  - `currency` must match `^[A-Z]{3}$` (e.g. `RWF`, `USD`)
  - Returns `400 {"error": "..."}` on failure
- **Why this feature?** Small, isolated, easy to review. Perfect for learning PRs.

## 1. The Workflow in 30 Seconds

```
main (stable)
  |
  +--> feature/payment-request-validation (your work)
         | push to GitHub
         v
       Pull Request: feature/... -> main
         | review + approve
         v
       Merge -> main is updated
```

You never commit directly to `main`. You propose changes via a Pull Request (PR).

## 2. Create a Feature Branch (Done for You)

You are already on:

```powershell
git branch --show-current
# feature/payment-request-validation

git status
git log --oneline -3
git diff main...HEAD --stat
```

If you want to redo it yourself from scratch:

```powershell
# from repo root: C:\Workstation\AmaliTech-DEG-Project-based-challenges
git checkout main
git pull origin main
git checkout -b feature/payment-request-validation
# make edits...
git add backend/Idempotency-gateway/src/main/kotlin/idempotency/api/PaymentRoutes.kt
git commit -m "feat: validate payment amount and currency"
git push -u origin feature/payment-request-validation
```

Branch naming convention: `feature/<short-description>`, `fix/<bug>`, `docs/<topic>`.

## 3. Push the Branch to GitHub

Your commits only exist locally until you push:

```powershell
git push -u origin feature/payment-request-validation
```

`-u` sets upstream so next time you can just run `git push`.

Verify:

```powershell
git branch -vv
git log origin/main..HEAD --oneline
```

## 4. Create the Pull Request on GitHub

1. Go to `https://github.com/kelvee21/AmaliTech-DEG-Project-based-challenges`
2. GitHub shows a yellow banner: **Compare & pull request** — click it.
   - If not: go to **Pull requests > New pull request**
   - **base:** `main` ← **compare:** `feature/payment-request-validation`
3. Fill in title + description. Copy this:

**Title:**
```
feat: validate payment amount and currency
```

**Description:**
```markdown
## What
Validate POST /process-payment body before processing.
- amount must be > 0
- currency must be 3-letter uppercase code (RWF, USD)

Returns 400 {"error": "..."} on invalid input.

## Why
Previously negative/zero amounts and bad currencies were accepted and stored as "Charged -5000 xxx".

## How to test
```bash
# invalid amount
curl -X POST http://localhost:8080/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-1" \
  -d '{"amount": -5, "currency": "RWF"}'
# -> 400 {"error":"Amount must be greater than 0"}

# invalid currency
curl -X POST http://localhost:8080/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-2" \
  -d '{"amount": 5000, "currency": "rwf"}'
# -> 400 {"error":"Currency must be a 3-letter uppercase code (e.g. RWF, USD)"}
```

## Checklist
- [x] Builds: `./gradlew compileKotlin`
- [x] No breaking change to idempotency logic
- [ ] Manually tested with curl
```

4. Click **Create pull request**.

## 5. How to Review Code (The Core Skill)

Open your PR > click **Files changed** tab.

### What to look at

For this PR you should see ~20 lines changed in `PaymentRoutes.kt`:

- New `validatePaymentRequest()` function
- Early `400` return before `engine.processPayment()`
- Null-body guard for `gson.fromJson` returning `null`

### Review checklist — apply it to this PR

1. **Correctness:** Does `amount <= 0` catch `0` and negatives? Yes. Does regex `^[A-Z]{3}$` reject `rwf`, `RWF1`, empty? Yes.
2. **Placement:** Validation happens *before* idempotency store write? Yes — check that `engine.processPayment` is called *after* validation.
3. **Status codes / contract:** `400` for client error, JSON `{"error": ...}` consistent with existing `Missing Idempotency-Key` error? Yes.
4. **Edge cases:** What if body is `null` string? Handled by explicit `if (paymentRequest == null)`. What if `currency` is missing? Gson gives `""` or `null` -> fails `isBlank()` check.
5. **Scope:** No unrelated changes? Run `git diff main...HEAD --stat` — should be 1-2 files only.
6. **No secrets / debug code:** No `println`, no keys.

### How to comment

- Hover a changed line > click **+** > type comment > **Start a review** (to batch) or **Add single comment**.
- Try these on this PR:
  - Line `if (request.amount <= 0)`: "Good — should we also cap max amount?"
  - Line regex: "Nice, explicit example in error message helps API users."
- Top right: **Review changes** >choose:
  - **Comment** — feedback, no decision
  - **Approve** — LGTM, ready to merge
  - **Request changes** — must fix before merge

As the author, you can also review your own PR first to catch mistakes — highly recommended.

### Review locally (optional but powerful)

```powershell
# fetch someone else's PR branch
git fetch origin feature/payment-request-validation
git checkout feature/payment-request-validation

# run it
cd backend/Idempotency-gateway
.\gradlew.bat run
# in another terminal, run the curl tests from section 4
```

## 6. Address Feedback and Update the PR

You don't create a new PR for fixes — just push to the *same* branch:

```powershell
git checkout feature/payment-request-validation
# edit files...
git add -p
git commit -m "fix: address review comment on currency message"
git push
```

The PR updates automatically. Resolve conversations on GitHub when done, then **Re-request review** (circular arrow icon next to reviewer name).

## 7. Merge the PR

1. Ensure checks pass and you have 1 approval (for learning, self-approve is OK; in teams require someone else).
2. Click **Merge pull request** > choose strategy:
   - **Squash and merge** (recommended for small features): 1 commit on `main`.
   - **Merge commit**: keeps full history.
   - **Rebase**: linear history, rewrites hashes.
3. Click **Delete branch** on GitHub after merge (keeps repo clean).

## 8. Clean Up Locally

```powershell
git checkout main
git pull origin main
git branch -d feature/payment-request-validation
git branch -D feature/payment-request-validation  # only if GitHub already deleted remote and -d complains
git remote prune origin
```

Verify you are back to clean `main`:

```powershell
git branch --show-current  # main
git status                 # clean
```

## Quick Command Cheat Sheet

```powershell
git checkout -b feature/my-change
git add <file>
git commit -m "feat: short imperative description"
git push -u origin feature/my-change
# -> open PR on GitHub -> review in Files changed -> merge -> delete branch
git checkout main; git pull; git branch -d feature/my-change
```

## Practice Tasks

1. Open the PR for `feature/payment-request-validation` and leave 2 inline comments (1 praise, 1 question).
2. Approve your own PR, then merge with **Squash**.
3. Next time: create your own `feature/<idea>` branch from `main` without help — e.g. return `X-Idempotency-Key` header or improve `/health` to return JSON.
