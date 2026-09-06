# StarFlowTV v1.3.0 Release Candidate

## Highlights

- Android TV live-first navigation with OK channel list, channel/line switching and TVBoxOS Movies secondary entry.
- Local-first remote configuration with HTTPS, Ed25519, SHA-256, schema validation, atomic activation and rollback.
- ABI-aware update policy for armeabi-v7a, arm64-v8a and universal packages; downgrade, package-name, certificate and digest mismatches are rejected.
- Source catalog baseline: 171 channels, 194 lines, 185 FHD, 6 HD, 3 measured 4K, 0 8K.

## Validation

- Android CI #24: tests and six debug/release ABI outputs passed.
- Source Checks #24: catalog/schema/secret-scan/render/checksum gates passed.
- TCL 65T8G Max and Xiaomi TV 4X physical acceptance remain pending.

## Distribution status

- Target config: `https://config.yuying.beauty/starflow/config/manifest.json`
- Target update: `https://update.yuying.beauty/starflow/update/latest.json`
- VPS bundle is prepared locally; static HTTPS deployment and endpoint verification are pending.
- Production Android and Ed25519 signing is `production-signing-pending`; this RC must not be advertised as OTA-installable.

## Scope

This RC does not merge either PR and does not create a Stable Release. No credentials, private playlists or production private keys are included.