# AVECOINS compatibility

## AVECOINS 2.5 and future version numbers

The supplied 2.5 WalletStore, WalletData and CraftingConfig classes are byte-identical to inspected 2.4 classes. The real wallet contract probe passes. Version 2.5 is explicitly reviewed; later version numbers are accepted only if all three class resources match the reviewed SHA-256 fingerprints. Reflection checks remain active. Changed classes are rejected until inspection, even if their public signatures appear unchanged. No unreviewed API change is automatically allowed.

## Current release

See the repository README and latest release notes for installation requirements. AVECOINS is an external optional dependency for currency features; it is not bundled. Wallet failures do not trigger blind economic retries.
