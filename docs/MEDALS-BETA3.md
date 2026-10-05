# Approved medal artwork — beta.3

The ten default Rassvet badges use the exact original render approved by the project owner on October 5, 2026. No second image generation or image retouching was used during integration. The unmodified 1817 x 866 PNG is embedded at `assets/zianrct/textures/gui/medals/approved_atlas.png`; `MedalArtwork` selects its source regions, and Minecraft scales them to the display size.

The embedded image was created with the built-in image generation tool. Its complete original preview remains in the atlas for provenance. Badge regions omit the preview's labels and frame; labels, ownership dimming, locks, tooltips, and navigation remain live UI elements. The game panel is a compact gold-and-charcoal layout rather than a flattened screenshot.

Default IDs and their configured texture paths are preserved. Custom definitions with alternate texture paths still render their own textures. A resource pack replacing the old default badge PNGs should configure alternate paths to bypass the approved atlas.

The saved medal ledger, battle listener, progression rules, LuckPerms checks, and medal protocol 2 are unchanged. Beta.3 adds no trainer rewards or trainer editor.

Validation: open the medallero at GUI scales 2 and 3, inspect both earned and locked badges, hover for metadata, use Close/ESC, and check pagination at small GUI resolutions. Reconnect and restart to verify existing earned medals are still present.
