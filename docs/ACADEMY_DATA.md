# Keep Academy runtime data out of source commits

`ACADEMY` may name any otherwise valid, owned training directory. Before the
launcher builds or starts a training server, `ownAcademy()` creates an internal
`.gitignore` containing exactly `*` followed by one LF byte. The rule ignores
immediate files **and directories**; a descendant `.gitignore` cannot re-include
a directory that this parent rule excludes.

This prevents ordinary `git add --all` from publishing a custom-named Academy's
world, models, optimizer/course state, local console credentials and logs.
The repository-level `academy/` rule alone did not cover other configured names.
No Git executable is required to launch or run a server.

## Existing directories and failures

An already owned Academy gains the same rule on its next start. Valid existing
rules are read without rewriting them. A noncanonical file, directory, symlink,
or dangling symlink at `.gitignore` makes startup fail before server launch.
The diagnostic identifies the conflicting path. The launcher does not overwrite
custom rules. Creation uses `CREATE_NEW` with `NOFOLLOW_LINKS`, so a file created
concurrently is not silently replaced.

The original ownership checks still reject a nonempty unowned directory, an
incorrect ownership marker and the checkout root itself. This change does not
read, migrate, delete or reset a model, world or course.

## Limits

Git ignore rules are an accidental-publication guard, not access control,
encryption or a backup. They do not remove already tracked files, undo prior
commits, protect files copied outside the Academy, or prevent an explicit forced
addition. Review staged paths before publishing. Existing data needs the next
successful ownership check to acquire this rule.

The regression test uses synthetic files and a temporary Git repository.
It verifies ordinary staging, nested negations, existing-file preservation and
rejected paths without starting Minecraft or reading any real training data.
