# Changelog

## 0.1.0-beta.3

- Display the ten Rassvet badges using the original pixels of the approved medal render, embedded as a source atlas; retain medal IDs, earned progress and protocol 2.
- Center a content-sized gold-and-charcoal medal panel, removing the large unused lower area. Add a matching Close button and pagination for small GUI resolutions or larger custom collections.
- Preserve locked/obtained states, metadata tooltips, and custom texture paths. Existing default texture paths select the new atlas artwork; alternate texture paths still use their configured images.
- Add regression coverage for panel bounds/pagination and source atlas regions. Install beta.3 on the client and server; no configuration or saved-ledger migration is required.

## 0.1.0-beta.2

- Add optional LuckPerms permission checks for medal viewing, league challenges and each administrative action. Support both NeoForge and Youer/Bukkit installations.
- Respect explicit permission denials even for operators; deny access when an installed provider cannot be queried. Keep console administration and defaults when LuckPerms is absent.
- Stop league challenges if prerequisite progress cannot be verified.
- Remove expired invitations and clear them on logout and server stop.
- Flush medal writes before replacing the saved ledger; reject malformed null records as persistence errors without rewriting the source file.
- Restore the missing Gradle launcher scripts and wrapper JAR, and run CI on review branches and pull requests.
- Keep the existing medal protocol and world/configuration data formats.
