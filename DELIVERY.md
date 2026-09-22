# Delivery contract

Version 0.4.0 is a source-first repository, not a private runtime ZIP.
The executable application lives in app/, learning/, launcher/, bridge/ and
scripts/. The clone root's start.sh builds the pinned native code and starts a
new owned Academy. All required source and download pins are tracked.

Do not publish binaries, .env, accepted EULAs, worlds, policies, credentials,
compiler caches or recovered server dependency caches in the Git tree.
`experimental/rl-next/` is preserved as research work, not a runtime upgrade.

See docs/VALIDATION.md for actual build/live evidence. Historical 0.3.1 repair
records refer to the earlier supplied ZIP, not to source-built 0.4.0 verification.
