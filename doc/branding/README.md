# هویت نصب‌شدهٔ پیکو

- نام روی گوشی: **پیکو**؛ شناسهٔ نصب: `picko.mokho.com`. نام هوش‌آموز در صفحات داخلی حفظ شده است.
- فایل اصلی کاربر: [picko-icon-source.png](picko-icon-source.png). پس‌زمینه با ابزار داخلی image_gen جدا شد: [picko-icon-cutout.png](picko-icon-cutout.png).
- آیکون نهایی [۵۱۲ پیکسل](picko-512.png)، [پیش‌نمایش گرد](preview-circle.png) و [گوشه‌گرد](preview-rounded.png).
- منابع لانچر برای ldpi/mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi: نسخهٔ عادی، گرد و foreground تطبیقی. foreground در لایهٔ ۱۰۸dp با عرض ۵۰dp و درون دایرهٔ امن ۶۶dp قرار دارد. [راهنمای رسمی Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
- بازسازی اندازه‌ها با `tools/build-branding.ps1` انجام می‌شود؛ خود تصویر سر بازطراحی نمی‌شود.
- [logo.png](logo.png) همان لوگوی پیوست کاربر است و با نام `studio_logo.png` همراه اپ قرار گرفته است.
- کارت «قلق استدیو» در تنظیمات به [وب‌سایت استدیو](http://gheleghstudio.ir/) با مرورگر گوشی لینک می‌دهد؛ اپ همچنان برای آموزش به اینترنت نیاز ندارد.
- کارت بازنشانی فقط دکمه دارد؛ تأیید پیش از حذف پیشرفت حفظ شده است.
- دکمهٔ خانه دارای پس‌زمینه، کادر و آیکون است. منوی پایین از هندسهٔ برداری با خانهٔ پنجره‌دار، مسیر سیاره‌ای و چرخ‌دنده استفاده می‌کند.

## پرامپت جداسازی پس‌زمینه

Use case: background-extraction. Edit target: the supplied exact Pico robot-head image. Remove ONLY the pale lavender background to genuine transparency. Preserve the same head, same asymmetrical glowing eyes, question mark, wavy mouth, purple floppy appendages, orange accent, camera angle, material and proportions. Do not invent a body or change the expression. Isolate the single main complete robot head; remove the tiny unrelated clipped purple fragment at the far right edge if it belongs to a neighboring drawing. No new shapes, no text, no shadows outside the head, no border, no rounded tile. Keep the entire visible head and appendages within frame with a little transparent padding. This cutout will be resized deterministically for an Android launcher icon.
