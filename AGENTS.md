# Agent Instructions

## Pull Requests
All AI coding agents MUST use the `no-mistakes` gate when opening pull requests or pushing branches.
Never use `git push origin <branch>`.

Instead, always use:
```
git push no-mistakes <branch>
```

This enforces our multi-dimensional code review and quality gates via https://github.com/kunchenguid/no-mistakes.
