# Sozo Call Manager — Demo Android App

A complete Android Studio project (Kotlin + Jetpack Compose) — a full call-manager
style app, demo/testable version of the Sozo call-tracking system.

## What's in this version

- **Multi-employee demo login** — 4 employees to test with (see table below)
- **Dialpad** — full numeric keypad, place outgoing calls directly from the app
- **Recents** — call history with All / Missed / Incoming / Outgoing filters,
  tap any call to ring them back. **Shows only the currently logged-in
  employee's own calls** — never another employee's.
- **Contacts** — reads your device's contacts, searchable, tap to call
- **Reports** — Day / Week / Month bar charts, plus today's stats — **scoped
  strictly to the logged-in employee**, with a clear empty-state message when
  they have no calls yet (instead of a misleading blank/aggregate chart)
- **Real call-state-aware in-call screen** — Answer/Decline only shows for
  genuine incoming calls; outgoing calls show "Calling…" + End, exactly
  matching the actual Telecom call state (not a locally-guessed one)
- **Working mute & speaker** — wired to the real `InCallService` audio
  routing, not just a UI toggle
- **Live call timer** — starts counting the moment a call is actually
  answered/connected, using the real connect timestamp
- **Auto-refresh, no relogin needed** — Recents and Reports update
  automatically when a new call happens (via ContentObserver + on-resume +
  a 4-second safety poll)
- **"Sync to Backend" hook** — `BackendApi.kt` posts call data to a placeholder
  URL; swap in your real Spring Boot endpoint when it's ready

**This chat environment has no Android SDK/Gradle, so this is full source code,
not a pre-built APK.** Open it in Android Studio and build it there — steps below.

---

## Important: about calls arriving on the "wrong" device during testing

In this demo, all 4 employees share **one physical phone** — the app can only
tell you *which employee was logged in when a call happened* (via
`TrackedCallStore`, tagged locally on this device). It has no way to control
*which physical device a call rings on* — that is decided entirely by which
phone number/SIM the caller dialed, at the telecom network level, long before
the app ever sees the call.

**In the real deployment this stops being a concern automatically**: every
employee gets their own phone with their own number. A customer calling
Employee A's number will only ever ring Employee A's phone — there's nothing
to "differentiate" in software, because the phones are physically separate.
This confusion only exists because of single-device testing.

---

## Demo employees

| Employee ID | Password  | Role      |
|-------------|-----------|-----------|
| `demo`      | demo123   | Reception |
| `sales1`    | sales123  | Sales     |
| `sales2`    | sales123  | Sales     |
| `doctor`    | doctor123 | Doctor    |

Add or remove employees in `data/DemoEmployee.kt`.

---

## Install & Test Guide

### Option A — No laptop setup needed (GitHub builds the APK for you)

Isमें tumhे Android Studio install karने ki zaroorat bilkul nahi hai — sirf ek free GitHub account chahiye, browser se hi APK mil jाएगा.

1. **github.com** pe jaake ek free account banाओ (agar nahi hai)
2. Upar-daayeं **"+"** icon → **"New repository"** → koई bhी naam do (jaisे `sozo-call-manager`) → **Create repository**
3. Naye repository page pe **"uploading an existing file"** wala link dikhेgा — uspe click karo
4. Is poоре `SozoCallManager` folder ko (jo tumने download kiya hai) **seedha browser mein drag-and-drop** kar do — poора folder structure preserve rahेgा
5. Neeche **"Commit changes"** button dabाओ
6. Upar **"Actions"** tab pe jaao — ek build automatically chalना shuru ho jaाएगा (2-4 minute lagते hain, ek yellow dot dikहेगा jo hara ho jाएगा complete hone pe)
7. Build complete hone pe, usी run pe click karके neeche **"Artifacts"** section mein **"sozo-call-manager-debug-apk"** milेgा — download kar lो (ek `.zip` milेगा, usके andar `app-debug.apk` hoगा)
8. Us APK file ko phone pe bhेजो (Google Drive, email, WhatsApp — koई bhी tareeka), phone pe download karके **install** kar do
   - Pehली baर install karते waqt phone **"Install from unknown sources"** allow karने ko bolेgा — allow kar dो, ye normal hai kyunki Play Store se nahi aa rahी

**Ye tareeka bilkul free hai** aur laptop pe kuछ bhी install nahi karна padता — sirf browser chahिए.

### Option B — Android Studio se (agar future mein code edit karна ho)

### Step 1 — Android Studio install karo (agar nahi hai)
https://developer.android.com/studio se download karo (Windows/Mac/Linux sab available)

### Step 2 — Project open karo
1. Android Studio kholo → "Open" → is folder (`SozoCallManager`) ko select karo
2. Pehli baar "Gradle Sync" chalega (internet chahiye) — 2-5 minute lag sakte hain
3. Agar "Gradle wrapper missing" ka prompt aaye, "Use Gradle from Android Studio" select kar lo

### Step 3 — Apna phone connect karo (recommended — emulator me calls test nahi hoti)
1. Phone me **Settings → About Phone → Build Number** pe 7 baar tap karo (Developer Options unlock)
2. **Settings → Developer Options → USB Debugging** ON karo
3. Phone ko USB se laptop se connect karo, "Allow USB Debugging?" pe Allow karo

### Step 4 — App run karo
1. Android Studio me toolbar me apna phone device select karo
2. Green "Run ▶" button dabao — app phone pe install ho jayegi

### Step 5 — App test karo
1. **Login** — koi bhi demo employee se login karo (table upar dekho)
2. **Setup** — "Allow" (permissions) aur "Set default" (default dialer) dono complete karo
3. **Dialpad tab** — koi number dabao, hare "call" button se actual call lagao
4. **Recents tab** — apni calls yahan automatically dikhengi, kisi call pe call-back icon dabao
5. **Contacts tab** — apne phone ke contacts search karke call karo
6. **Reports tab** — Day/Week/Month toggle karke bar chart dekho, neeche employee-wise breakdown

### Multiple employees test karna (single device pe)
Chunki real deployment mein har employee ka apna phone hoga, is demo mein
per-employee tagging ek local, testing-only trick se hoti hai
(`TrackedCallStore.kt`):

1. `demo` se login karo → ek test call karo (khud ko doosre number se, ya kisi
   dost se) → Recents/Reports khol ke dekho, call automatically 'demo' ke
   naam tag ho jayegi
2. Logout → `sales1` se login karo → ek aur test call karo → Reports mein
   ab dono employees ka alag data dikhega
3. Har employee ke liye repeat karo

**Real business use mein ye step zaroorat hi nahi padega** — har employee ka
khud ka phone/app instance hoga, tagging automatically sahi hogi.

### Troubleshooting
- **"Set default" dialog nahi aa raha**: Xiaomi/MIUI, Oppo, Vivo jaise phones
  apna khud ka flow rakhte hain — Settings → Apps → Default Apps → Phone App
  me manually "Sozo Call Manager" select karo.
- **Gradle sync fail**: Internet/VPN check karo — Google/Maven servers se
  packages download hote hain.
- **App crash bina permission ke**: Setup screen skip mat karo.
- **Auto-refresh slow lag raha hai**: Safety-net poll har 4 second pe chalta
  hai — agar turant chahiye, ContentObserver-based instant update already
  active hai zyadatar phones pe; kuch MIUI/ColorOS phones is callback ko
  delay karte hain, isliye 4-second poll hai bhi.

---

## Project structure

```
app/src/main/java/com/sozo/callmanager/
├── MainActivity.kt                → Login → Setup → Main navigation
├── ui/
│   ├── LoginScreen.kt              → Multi-employee demo login
│   ├── SetupScreen.kt              → Permissions + default-dialer request
│   ├── MainScaffold.kt             → Bottom nav host (Dialpad/Recents/Contacts/Reports)
│   ├── DialpadScreen.kt            → Numeric keypad + outgoing calling
│   ├── RecentsScreen.kt            → Call history with filters
│   ├── ContactsScreen.kt           → Device contacts, search, call
│   ├── ReportsScreen.kt            → Day/Week/Month charts + employee breakdown
│   ├── CallRefresh.kt              → Auto-refresh trigger (observer + resume + poll)
│   └── theme/Theme.kt              → App colors/branding
├── dialer/
│   ├── SozoInCallService.kt        → Required for default-dialer eligibility
│   ├── CallHolder.kt               → Bridges real Telecom call state into Compose (state, timer, mute/speaker)
│   ├── InCallActivity.kt           → Answer/reject/mute/speaker/end screen, driven by real call state
│   └── CallActionHelper.kt         → Places outgoing calls (dialpad/recents/contacts)
├── data/
│   ├── CallRecord.kt               → Call data model
│   ├── CallLogRepository.kt        → Reads device call log, range queries, auto-refresh observer
│   ├── Contact.kt / ContactsRepository.kt → Device contacts
│   ├── DemoEmployee.kt             → 4 demo employees (edit this list freely)
│   ├── DemoSession.kt              → In-memory login session
│   └── TrackedCallStore.kt         → DEMO-ONLY per-employee call tagging (see note in file)
└── network/
    └── BackendApi.kt               → Sends call data to Spring Boot (placeholder URL)
```

## Next steps (jab backend ready ho)
1. `BackendApi.kt` me `BASE_URL` ko real Spring Boot endpoint se replace karo
2. `DemoSession.login()` ko real `/api/auth/login` API call se replace karo (JWT token)
3. `DemoEmployee.kt` / `TrackedCallStore.kt` dono hata do — real deployment mein
   har employee ka apna phone hoga, per-device tagging ki zaroorat hi nahi
4. Call sync ko background `WorkManager` job bana do (abhi manual hai) taaki
   offline hone pe calls queue ho jayein aur net aane pe apne aap sync ho
