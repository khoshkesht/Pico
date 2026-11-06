# مرحلهٔ سوم: زمینهٔ گفتگو (Context)

داده: [stage-03.json](stage-03.json). پنج فعالیت آموزشی و یک آزمون با همان لحن کوتاه و سه تعامل موجود؛ بدون صدا و بدون تغییر موتور `engine`.

| فعالیت | مفهوم | تعامل | کلید تألیف |
| --- | --- | --- | --- |
| ابزار از کجا باید بدونه؟ | رساندن اطلاعات موقعیت همراه درخواست | مقایسه | ب |
| همهٔ جزئیات به درد نمی‌خورن! | انتخاب زمینهٔ مرتبط | چندگزینه‌ای | الف، ج، د |
| عکس هم می‌تونه زمینه بده | استخراج زمینهٔ قابل‌مشاهده و پرهیز از حدس | چندگزینه‌ای، با تصویر تازه | الف، د |
| «همونو» رو کی یادشه؟ | خلاصهٔ لازم در گفت‌وگوی فاقد پیام‌های قبلی | مقایسه | ب |
| شرایط عوض شد؛ زمینه هم عوض می‌شه | به‌روزکردن اطلاعات موقعیت | تک‌گزینه‌ای | ج |
| چالش آخر: زمینهٔ درست برسون! | موقعیت تازهٔ ساخت ویدئوی مدرسه؛ انتخاب زمینهٔ مؤثر | چندگزینه‌ای | الف، د |

تعداد پاسخ‌ها و کلیدها در رابط پیش از ثبت آشکار نمی‌شوند. بازخورد هر گزینه حداکثر ۵۴ کاراکتر است. فعالیت‌ها ۵۰ امتیاز با جریمهٔ ۱۰ و کف صفر دارند؛ حداکثر این مرحله ۳۰۰ و مجموع سه مرحله ۱۰۰۰ است. عبور همچنان به پاسخ صحیح درس‌ها و آزمون نیاز دارد. فعالیت‌های ۱، ۳ و ۵ برای مرور روزانه استفاده می‌شوند.

## تنها تصویر تازه

[تصویر اتاق کارگاه](../../app/src/main/assets/images/context_workshop.png) با ابزار داخلی image_gen ساخته و در بستهٔ اپ ثبت شده است. مقوای رنگی، ماژیک، چسب و پنجرهٔ بارانی ورودیِ سؤال‌اند؛ تصویر نه قیمت و بودجه دارد، نه چاپگر. توضیح دسترس‌پذیری همان اطلاعات دیداری را منتقل می‌کند. همان یک فایل در فعالیت و مرور روزانه استفاده می‌شود. تصاویر پیکو همان سه تصویر قبلی‌اند.

نمایش تصویر محلی در لایهٔ رابط و loader عمومی قرار گرفته است؛ حالت تعامل، ارزیابی و قواعد موتور تغییر نکرده‌اند. منطق یا صفحهٔ مخصوص فعالیت سوم ساخته نشده است.

## حرکت‌ها

- نوار پیشرفت خانه در هر ورود و بازگشت به پیش‌زمینه از صفر به درصد واقعی پر می‌شود؛ متن درصد واقعی ثابت است.
- سیاره و هالهٔ پشت پیکو با چرخهٔ آرام شناوری، چرخش اندک و تغییر مقیاس و شفافیت حرکت می‌کنند.
- مقدار `activityBackground=ORBITAL_LINKS` در دادهٔ مرحلهٔ سوم مدارها و گره‌های نورانی متحرک را فعال می‌کند؛ شهاب‌باران دو مرحلهٔ قبل حفظ شده است.
- حرکت‌های پیوسته در مرحلهٔ draw/layer اجرا می‌شوند و انتخاب‌ها را جابه‌جا نمی‌کنند. خاموش‌بودن انیمیشن سیستم رعایت می‌شود.

## حفظ پیشرفت و نصب

افزودن `context@1` یک ارتقای سازگار است؛ پیشرفت، امتیاز، جریمه‌ها، انتخاب نیمه‌کاره و سابقهٔ مراحل قبلی حفظ می‌شوند. مرحلهٔ سوم فقط بعد از پایان صحیح مرحلهٔ دوم باز می‌شود. هیچ نصب یا بازنشانی خودکاری انجام نمی‌شود؛ شروع نصب طبق درخواست کاربر منتظر تأیید نهایی است.

## بررسی نسخهٔ ۰.۶.۰

ساخت Debug و Lint موفق‌اند؛ ۲۸ آزمون با صفر شکست و صفر خطای Lint اجرا شدند. شش آزمون تازه، تعداد فعالیت‌ها و فایل تصویر، قفل مرحله تا موفقیت قبلی، حفظ امتیاز و انتخاب نیمه‌کاره در ارتقا، کسر و ادامهٔ مرحلهٔ سوم و امتیاز نهایی ۱۰۰۰ برای سه مرحله را پوشش می‌دهند. وجود JSON مرحلهٔ سوم و تصویر داخل خود APK نیز بررسی شده است. تست نصب روی گوشی عمداً تا تأیید کاربر انجام نشده است.

## پرامپت تصویر

Use case: scientific-educational. One clear polished 3D illustration as the visual evidence for a teen AI-literacy lesson about supplying context. Landscape 4:3 image, moderate resolution suitable for a mobile card. Soft clay-like stylized 3D, navy/violet room with gentle lavender lighting, muted orange and teal accents, matching a playful space-themed educational app without depicting outer space. Scene: small indoor school craft workshop. Foreground single tabletop with CLEARLY identifiable colored cardstock sheets, several capped marker pens, one glue stick and a small roll of adhesive tape. Keep objects well separated and recognizable on a phone. Background a large closed window with obvious rain droplets and slanted rain streaks outside; interior dry. Include a plain wall and one modest chair to establish an indoor classroom. No humans, no robots, no other characters, no text, no letters, no numbers, no price tags, no signage, no printer or 3D printer, no electronics, no hidden second scene. Focus on visible materials plus indoor rainy-day context. Do not depict budget, attendance or schedule, since these cannot be inferred from the illustration. Friendly and readable, no visual clutter, no collage, no border. The illustration itself is instructional input, never an answer-key diagram.
