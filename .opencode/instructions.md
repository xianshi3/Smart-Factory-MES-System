# OpenCode Instructions

## Git Commit

Before committing to Git, you MUST ask for user confirmation first. Never auto-commit.

## CRITICAL: Preventing "Stuck/Loading" Issues

The AI agent frequently gets stuck when processing large frontend files. To prevent this, follow these rules STRICTLY:

### 1. File Reading Limits
- **NEVER read more than 50 lines at once.** Use `offset` and `limit` parameters.
- **NEVER read an entire file** if it is larger than 100 lines.
- **ALWAYS use `grep` first** to find the exact line number before reading.

### 2. Editing Limits
- **ONE edit per message.** Never make multiple `edit` calls in the same response.
- **Use small snippets.** The `oldString` must be 3-5 lines maximum.
- **NO parallel edits.** Never edit the same file twice in parallel.

### 3. If You Get Stuck
- If the tool takes more than 5 seconds, **STOP**.
- Do not retry the same operation.
- Tell the user: "The file is too large to process automatically. Please specify the line number or function you want to modify."

### 4. Workflow for Large Files
1. Ask user for specific area (e.g., "Which function?").
2. Read ONLY that specific area (e.g., lines 50-80).
3. Make ONE edit.
4. Stop.

## General Rules
- Be concise.
- Do not summarize code unless asked.
- If unsure, ASK the user.
