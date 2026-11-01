# Manual device test plan

**Status: not run yet.** Nothing below has been verified on a real device. Fill in the result column as you test
and record the device model and Android version for every run.

| # | Area | Steps | Expected | Result |
|---|---|---|---|---|
| 1 | Install & launch | Install debug APK, open app | Onboarding shows; Skip works; Today opens with the sample routine | |
| 2 | Offline | Turn on airplane mode, use Today/Routine/Focus/Progress/Rewards | Everything works; no network errors | |
| 3 | Notification permission (Android 13+) | Add a task with a reminder → Turn on reminders → Allow | Explanation dialog first, then system prompt; test reminder arrives | |
| 4 | Permission denied | Deny the prompt, then re-enable in Android settings, return to app | Banner explains; after enabling, banner disappears (on resume) | |
| 5 | Reminder tap | Tap a reminder notification | App opens on Today with that task highlighted | |
| 6 | Snooze | Tap "Snooze 10 min" | Reminder returns about 10 minutes later | |
| 7 | Duplicates | Edit the same task 5 times, wait for its reminder | Exactly one reminder | |
| 8 | Quiet hours | Enable quiet hours covering "now", wait for a reminder | No notification during the window | |
| 9 | Reboot | Reboot the phone, wait for the next reminder | Reminder still arrives | |
| 10 | Time zone / date | Change time zone and then the date | Today shows the right day; reminders move accordingly | |
| 11 | Midnight task | Create 23:00–00:00 task; check duration and completion | Duration 60 min; shows "(next day)" only when end ≤ start | |
| 12 | Complete / undo | Tick a task, untick, tick again | XP awarded once; ledger shows reversal rows | |
| 13 | Focus rotate/background | Start focus, rotate, background 2 min, return | Remaining time is correct | |
| 14 | Process death | Start focus, swipe app away / force-stop, reopen | Session still active with correct time | |
| 15 | Focus reboot | Start focus, reboot, open app | Session paused with explanation, no invented time | |
| 16 | Early finish / cancel | Finish early; start another and cancel | Confirmation shown; cancel earns nothing | |
| 17 | Usage Access denied | Turn protection on, don't allow Usage Access | Status says waiting; no service runs | |
| 18 | Protection | Allow Usage Access, select an app, start focus, open that app | Notification; −points once; second launch within cooldown only logged | |
| 19 | Essentials | Open Phone / Messages / Settings during focus | No penalty; they are not in the picker | |
| 20 | Emergency pause | Use "Emergency pause 10 min" then open the app again | No penalty; history shows override | |
| 21 | Battery optimisation / OEM | Repeat 18 with battery saver and on an OEM skin | Note any missed or late detection | |
| 22 | Rewards | Earn coins, buy an item, try to buy without coins | Cannot go negative; item stays owned after restart | |
| 23 | Export | Settings → Export data | Valid JSON file opens in a text viewer | |
| 24 | Clear data | Settings → Clear all data | Everything reset; sample routine restored | |
| 25 | Accessibility | Turn on TalkBack, move through Today and Focus | Controls have readable labels | |
