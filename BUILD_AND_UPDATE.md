# RCS Vault - Build & Website Update

## GitHub Actions
Run:
Actions -> Build RCS Vault Release APK -> Run workflow

The release build requires valid signing configuration/keystore.

## Website update
Upload `version.json` to:
`rcs-vault/version.json`

Upload the signed APK to:
`rcs-vault/`

For each new release increase `versionCode`, update `versionName`, APK filename/URL, and release notes.

IMPORTANT:
Keep the same signing key for every future APK update.
