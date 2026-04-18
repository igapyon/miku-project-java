# mikuproject-java

Java port workspace for `mikuproject`.

## Upstream Policy

- Keep the Node.js upstream repository under `vendor/mikuproject` using `git subtree`.
- Treat `vendor/mikuproject` as read-only upstream reference unless there is an explicit reason to patch it.
- Keep Java implementation and Java-specific specs outside `vendor/`.
- Keep `workplace/` out of Git tracking as a local working area.
