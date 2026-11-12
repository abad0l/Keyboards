# Glass Keyboard

كيبورد أندرويد (IME) بتصميم زجاجي: عربي + إنجليزي + إيموجي + اقتراحات + ٦ ثيمات.

## بناء الـ APK
**أندرويد ستوديو:** Open → اختار فولدر المشروع → Build > Build APK(s).
**سطر الأوامر:** `./gradlew assembleRelease` (محتاج JDK 17 وAndroid SDK) ← الناتج في `app/build/outputs/apk/release/`.
**GitHub Actions:** ارفع المشروع على ريبو، والـ workflow في `.github/workflows/build.yml` هيبني الـ APK ويطلعه Artifact.

## التشغيل
افتح التطبيق ← فعّل الكيبورد ← اختاره.
