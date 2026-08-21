# Launcher icon fix

The release build previously contained both PNG and WEBP files with the same
Android resource names. All `ic_launcher.webp` and `ic_launcher_round.webp`
files have been removed from the project.

The Codemagic workflow also removes any such files immediately before the
release build, so stale files from an incorrectly merged GitHub upload cannot
cause the duplicate-resource error again.

Keep the PNG launcher resources and use the same project root in GitHub.
