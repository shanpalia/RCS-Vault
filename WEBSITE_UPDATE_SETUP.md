# RCS Vault update server — one-time setup

The APK checks these locations (in order):

1. https://shanpalia.github.io/WebsitePaliaAPK_V.2/version.json
2. https://shanpalia.github.io/WebsitePaliaAPK_V.2/rcs-vault/version.json
3. GitHub raw `main/version.json`
4. GitHub raw `main/update-server-template/version.json`
5. GitHub raw `master/version.json`

The **recommended** location is the root of the PaliaAPK Hub GitHub Pages site:

`version.json`

and the APK:

`RCS-Vault.apk`

Both must actually be committed/published in the `WebsitePaliaAPK_V.2` repository. A 404 cannot be repaired from inside the APK because the remote file does not exist at that URL.

Example `version.json`:

```json
{
  "versionCode": 1,
  "versionName": "1.0",
  "apkUrl": "https://shanpalia.github.io/WebsitePaliaAPK_V.2/RCS-Vault.apk",
  "releaseNotes": ["Initial release"],
  "forceUpdate": false
}
```

For the next release, only change `versionCode`/`versionName`, update `apkUrl` if needed, and publish the new APK. The app will show **Update Available** only when the server `versionCode` is greater than the installed one; otherwise it shows **You're up to date**.
