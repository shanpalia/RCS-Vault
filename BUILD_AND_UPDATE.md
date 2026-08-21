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


## Codemagic
The repository includes `codemagic.yaml` at the project root.

In Codemagic:
1. Connect the GitHub repository.
2. Select the `rcs_vault_android` workflow.
3. Configure Android code signing with the SAME release keystore for all future versions.
4. The keystore alias must be `upload` because `app/build.gradle.kts` uses that alias.
5. Start the workflow. The signed APK is produced as an artifact.

The workflow expects Codemagic signing variables:
- `CM_KEYSTORE_PATH`
- `CM_KEYSTORE_PASSWORD`
- `CM_KEY_PASSWORD`

The app version is currently versionCode 1 / versionName 1.0.

## Codemagic
The Codemagic workflow installs Gradle 9.3.1 because Android Gradle Plugin 9.1.1 requires Gradle 9.3.1 or newer. It uses the existing `paliaapk-release` Android signing identity.

## Codemagic .env
The project includes an empty `.env.example` so the Secrets Gradle Plugin can configure successfully in CI. Do not commit real secrets to `.env`.
