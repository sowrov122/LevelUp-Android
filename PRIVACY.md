# Privacy summary

Level Up is local-first. There is no account, no server and no analytics, and the app does **not** declare the
INTERNET permission.

| Data | Where it lives | Leaves the device? |
|---|---|---|
| Routine, task history, scores, focus sessions, rewards | Room database in app storage | No |
| Settings (name, quiet hours, toggles) | DataStore in app storage | No |
| Selected apps and interruption history | Room database | No |
| Which app came to the foreground | Read only during a running focus session, only for apps you selected; only a package name and timestamp are stored when it matches | No |

Not collected: screen content, keystrokes, messages, browsing history, passwords, location, contacts.

## Permissions
| Permission | Why | When it is requested |
|---|---|---|
| POST_NOTIFICATIONS | Routine reminders and focus alerts | Only after an in-app explanation |
| RECEIVE_BOOT_COMPLETED | Re-create reminder alarms after reboot | Install-time, no prompt |
| Usage Access (PACKAGE_USAGE_STATS) | Notice selected apps opening during focus | You grant it in Android settings after an explanation; nothing is observed before then |
| FOREGROUND_SERVICE (+ SPECIAL_USE) | Keep the checker alive during a focus session | Install-time; the service runs only while protection is on and a session is running |

## Your controls
Export data (JSON) and Clear all data are in Settings. Protection can be switched off or paused at any time.

## Honest limits
The app cannot see inside other apps, cannot tell whether content is adult or a reel, and cannot stop an app from opening.
Detection can be late or missing on some devices because of battery management.
