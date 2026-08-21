# Notification Access fix

Android 13+ can block sensitive special access for APKs installed from a browser/file manager.
The app cannot bypass this security restriction. The UI now provides:

1. Notification Access — opens the official Notification Access settings.
2. App Info — opens this app's App Info page. If Android shows “App was denied access”, use the App Info ⋮ menu -> Allow restricted settings, then return to Notification access and enable RCS Vault.

The NotificationListenerService declaration uses the official BIND_NOTIFICATION_LISTENER_SERVICE permission.
