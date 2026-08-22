# Update behavior

The app should compare the installed versionCode with the versionCode from
`version.json`.

- Server versionCode > installed versionCode: show "Update available" with the
  new version and update button.
- Server versionCode <= installed versionCode: show "You're up to date".
- Server unreachable/HTTP error: show the connection/server error, not
  "You're up to date".

For a new release, increase `versionCode`, update `versionName`, and upload
the new `RCS-Vault.apk` to the website.
