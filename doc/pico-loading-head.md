# سر پیکو در حالت بیکاری خانه

دارایی: `app/src/main/res/drawable-nodpi/pico_loading_head.png`.
ساخته‌شده با ابزار داخلی `image_gen` با مرجع `pico_hero.png` و پس‌زمینهٔ شفاف. نقطه‌ها جزو تصویر نیستند؛ سه نقطهٔ متحرک در Compose رسم می‌شوند.

خانه پس از ۱۰ ثانیه بدون لمس یا ورودی صفحه‌کلید، پیکوی قدی را با این سر جایگزین می‌کند. لمس، اسکرول یا ورودی صفحه‌کلید آن را برمی‌گرداند. خروج از خانه یا رفتن اپ به پس‌زمینه، زمان بیکاری را متوقف می‌کند. با غیرفعال‌بودن انیمیشن سیستم، متن کامل و نقطه‌های ثابت نمایش داده می‌شوند.

## پرامپت نهایی

Use case: identity-preserve. Create ONE production UI asset: only the floating HEAD of Pico from the reference, for an Android idle/loading state. Preserve exactly the dark rounded monitor head, purple floppy side appendages, orange top accent, soft clay-like 3D rendering and lilac illuminated screen. No torso, neck, hands, legs or props. Front-facing centered head with comfortable transparent padding, square composition, full appendages visible. Expression: comically blank buffering/thinking, two small glowing lilac horizontal dash eyes and a tiny neutral mouth. No punctuation, no text, no dots or loading symbols baked into the image; the app draws animated dots separately. Actual transparent background, no backdrop, no ground shadow, no white rectangle. Match the supplied character identity and palette closely.
