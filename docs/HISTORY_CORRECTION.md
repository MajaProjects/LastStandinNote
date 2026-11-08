# Correcting the preparation commit title

Commit `cd055a2ae060d222f15dab30303ffabaa277aaea` currently has the title
`chore(docs): remove outdated documentation copies`, but introduces 36 files
covering timing, diagnostics, presentation, chart adjustments and licensing.

Use this replacement title:

```text
feat(release): improve timing, diagnostics and presentation
```

This operation only changes commit metadata. It rewrites that commit and its
descendants, including the later mode-title fix. The files must remain identical.
The commands below have not been run against the published branch.

## Before rewording

Commit the pending preparation changes first and ensure `git status` is clean.
Coordinate with the other development computer so it does not push the previous
history while this correction is being made. Do not create the release tag yet.

The GitLab mirror workflow uses `git push --mirror`, which attempts forced
updates. Whether GitLab allows them depends on its protected-branch rules;
this document does not assert that the server accepts them. Confirm that the
GitLab branch permits the mirror's update before publishing rewritten history.

## PowerShell

Run these commands from the project working directory, not from a bare `.git`
folder. Keep the same terminal open throughout so the captured lease is retained.

```powershell
git switch main
git fetch origin
git pull --ff-only
git status
git branch backup/pre-release-before-reword
$expectedMain = (git rev-parse origin/main).Trim()
git rebase -i cd055a2ae060d222f15dab30303ffabaa277aaea^
```

If the backup branch already exists, choose a new backup name and use that same
name in the comparison below. Stop if `git status` is not clean or any command
fails; do not discard local changes to make the commands pass.

In the editor, change **only** `pick` to `reword` on the line for `cd055a2`.
Keep the other commits as `pick`. Save and close. In the next editor, replace
the misleading title with the title above, then save and close.

If an unexpected conflict occurs, use `git rebase --abort` and inspect the
repository before continuing. A title-only rebase should not need content edits.

Verify the result:

```powershell
git diff --exit-code backup/pre-release-before-reword HEAD
git log -5 --oneline
```

The diff must be empty. Only then publish with the captured, explicit lease:

```powershell
git push --force-with-lease="refs/heads/main:$expectedMain" origin main
```

If the lease is rejected, do not retry with `--force`; inspect the new remote
commits first. Confirm that both GitHub validation and GitLab mirroring finish
successfully. Keep the backup branch locally until both are verified.

The other development computer must preserve any local work, fetch the new
history and reconcile its `main` before making further pushes. A normal pull
may try to merge the old and new histories, so inspect its state first.
