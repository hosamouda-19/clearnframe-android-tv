# CleanFrame لـ Android TV

واجهة CleanFrame (من nthnclrk، رخصة MIT) محوّلة إلى Launcher لشاشات Android TV.
الواجهة نفسها تعمل داخل WebView، وملف `device.js` بدّل واجهات Tizen بجسر Android
(`MainActivity.java`).

## البناء (الطريقة الأسهل: بدون Android Studio)
1. أنشئ مستودعاً جديداً على GitHub وارفع محتويات هذا المجلد.
2. افتح تبويب Actions، شغّل "Build APK"، ثم حمّل الملف `CleanFrame-apk` (فيه app-debug.apk).

## البناء من Android Studio
افتح المجلد، ثم Build > Build APK(s). الملف يظهر في app/build/outputs/apk/debug/.

## التثبيت على الشاشة
```
adb connect <IP-الشاشة>:5555
adb install -r app-debug.apk
```
أو انسخ الـ APK على فلاشة وافتحه بمدير ملفات على الشاشة.

## تشغيله
- افتحه من قائمة التطبيقات (CleanFrame).
- لجعله الواجهة الافتراضية: اضغط زر Home واختر CleanFrame > دائماً.
- ابقِ الـ Launcher الأصلي مثبتاً للرجوع إليه.

## ملاحظات
- أزرار: الأسهم للتنقل، OK للفتح، Back للرجوع.
- "Screensaver" يشغّل شاشة التوقف إن وُجدت (بديل Art Mode).
- حزم التطبيقات الافتراضية (Netflix, YouTube...) أسماؤها Android؛ ما لم يكن مثبتاً أزله من Settings > Arrange.
- الخطوط والصور لها تراخيصها: THIRD_PARTY_NOTICES.md في المستودع الأصلي.
- الخلفيات مقلّصة إلى اثنتين (Misty coast وNight) لتخفيف الذاكرة.
