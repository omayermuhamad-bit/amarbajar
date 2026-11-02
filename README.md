# বাজার লিস্ট (Bajar List) — Android App

Daily ar Monthly bajar-er list banano, tick deya, PDF save ar share korar app.

## Features
- **Daily bajar** ar **Monthly bajar** — duita alada tab
- Item-er nam + quantity + unit (কেজি, গ্রাম, লিটার, পিস, হালি, ডজন, প্যাকেট ইত্যাদি)
- Ekshathe onek item add kora jay, aager nam suggestion hishebe ashe
- Item kena hole checkbox-e tick; progress bar
- Delete korle "ফিরিয়ে আনুন" (undo)
- Aager din / aager mash-er list theke copy
- **PDF** banay (A4, table + tick box) — **Downloads-e save** ar **WhatsApp/Messenger/Email-e share**
- Offline kaj kore, data phone-ei thake
- Light/Dark theme

## APK banano — Way 1: Android Studio (shobcheye sohoj)
1. Android Studio (Koala ba notun) install korun.
2. `File > Open` e ei `BajarList` folder select korun.
3. Gradle sync hoye gele (prothombar kichu minute lagbe) phone USB diye connect kore **Run ▶** chapun,
   ba `Build > Build Bundle(s) / APK(s) > Build APK(s)` diye APK banan.
4. APK thakbe: `app/build/outputs/apk/debug/app-debug.apk`

## APK banano — Way 2: GitHub Actions (Android Studio chhara)
1. GitHub-e notun repository banan, ei folder-er shob file upload/push korun (`main` branch).
2. Repo-er **Actions** tab > **Build APK** workflow chalan (push korlei auto chole).
3. Shesh hole oi run-er niche **Artifacts > bajar-list-apk** download korun, phone-e install korun.

## Notes
- Minimum Android 10 (API 29).
- Unit-er list bodlate: `Models.kt` file-er `UNITS`.
- Color/theme bodlate: `Theme.kt`.
