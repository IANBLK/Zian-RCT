# Cobblemon 1.8.2 / RCT API 0.16.2-beta validation

The official NeoForge RCT API 0.16.2-beta artifact was downloaded from Modrinth and its SHA512 verified. The maintainer confirmed and closed the IRON_BALL binary issue in GitLab #34. Upstream code remains a separate dependency; no integration or relicensing is performed.

Using dependency substitution only for the isolated compatibility test, the current code built and passed 135 tests against Cobblemon 1.8.2 and the official new API. Two dedicated SQLite server boots passed singles/doubles, team release, forced-end handling, native winner callbacks, legacy progress/NPC persistence, level-cap XP/candy enforcement and legendary delivery/replay protection. Compiled production dependency defaults remain unchanged pending broader version-range testing.

Related tests passed for Manager, GTS, GUI and Utilities. Utilities had two isolated SQLite boots covering genetics, Gacha metadata/codec, parent ownership, pending/replay handling and registered equipment. This validates the tested paths, not every third-party mod or every possible trainer team.

Official fixed version: https://modrinth.com/mod/rctapi/version/mYGAO3MT
