# Release 5.8.0 (versionCode 47)

One Play update for everyone. It carries the whole revamp from 5.5.2 to 5.8.0. Versions 5.5.2, 5.6.0 and 5.7.0 were
internal milestones and are never uploaded on their own.

## What users get
- **A key of their own.** The vault is re-encrypted with a key created on their phone and kept in Android's secure
  hardware. Before, one key was built into the app.
- **A recovery password**, set once. It is the way back in if fingerprints change or the screen lock is removed.
- **Autofill** in other apps and in Chrome. It always asks for a fingerprint or PIN first and shows no entry names
  before that. It is careful with look-alike apps and sites.
- **Two-factor authenticator codes (TOTP)**, from a QR-code link or a typed key.
- **Password history:** the last 10 values of every secret.
- **Encrypted backups** (`.pkbak`) protected by a password, plus restore of old CSV files.
- **Auto-lock** after a chosen time, a lock after the app is restored from memory, and a clipboard that clears itself.
- **Delete with Undo,** and no more lost typing when a save is rejected.
- **17 languages**, plus honest privacy text and opt-in crash and usage reports.
- **Stability:** crash fixes on Android 9 and 10, accessibility fixes, and a back button everywhere.

## What existing users will see on the first launch
1. **Privacy choices**, once: crash reports and usage statistics, both off until chosen.
2. **"Set a recovery password"**, once and mandatory: at least 10 characters, with a strength meter.
3. **Their screen lock or fingerprint.** The vault is then re-keyed in the background. The original file is kept until
   the new one has been verified.

If the re-key fails, the app keeps the old vault, offers an encrypted rescue export, and logs `vault_migration_failed`
(reason only).

## Play "What's new" (500 characters max each)

**en-US**
Your vault now has its own key, kept in your phone's secure hardware. Set a one-time recovery password when asked.
New: autofill in apps and Chrome (always asks for your fingerprint first), 2FA authenticator codes, password
history, encrypted backups, auto-lock and Undo. Now in 17 languages, with optional, privacy-first crash reports.
Many fixes and accessibility improvements.

**hi-IN**
आपके वॉल्ट को अब अपनी अलग कुंजी मिली है, जो फ़ोन के सुरक्षित हार्डवेयर में रहती है। पूछे जाने पर एक बार रिकवरी पासवर्ड बनाएँ। नया: ऐप्स और Chrome में ऑटोफ़िल (पहले हमेशा फ़िंगरप्रिंट माँगता है), 2FA कोड, पासवर्ड इतिहास, एन्क्रिप्टेड बैकअप, ऑटो-लॉक और Undo। अब 17 भाषाओं में, वैकल्पिक क्रैश रिपोर्ट के साथ। कई सुधार।

**bn-BD**
আপনার ভল্ট এখন নিজস্ব কী পেয়েছে, যা ফোনের সুরক্ষিত হার্ডওয়্যারে থাকে। চাইলে একবার রিকভারি পাসওয়ার্ড সেট করুন। নতুন: অ্যাপ ও Chrome-এ অটোফিল (আগে সবসময় ফিঙ্গারপ্রিন্ট চায়), 2FA কোড, পাসওয়ার্ড ইতিহাস, এনক্রিপ্টেড ব্যাকআপ, অটো-লক ও Undo। এখন 17টি ভাষায়, ঐচ্ছিক ক্র্যাশ রিপোর্টসহ। অনেক সংশোধন।

**mr-IN**
तुमच्या व्हॉल्टला आता स्वतःची किल्ली मिळाली आहे, जी फोनच्या सुरक्षित हार्डवेअरमध्ये राहते. विचारल्यावर एकदा रिकव्हरी पासवर्ड ठेवा. नवीन: ॲप्स आणि Chrome मध्ये ऑटोफिल (आधी नेहमी फिंगरप्रिंट मागते), 2FA कोड, पासवर्ड इतिहास, एन्क्रिप्टेड बॅकअप, ऑटो-लॉक आणि Undo. आता 17 भाषांमध्ये, ऐच्छिक क्रॅश अहवालांसह. अनेक सुधारणा.

**gu-IN**
તમારા વૉલ્ટને હવે પોતાની ચાવી મળી છે, જે ફોનના સુરક્ષિત હાર્ડવેરમાં રહે છે. પૂછવામાં આવે ત્યારે એક વાર રિકવરી પાસવર્ડ સેટ કરો. નવું: ઍપ અને Chrome માં ઑટોફિલ (પહેલાં હંમેશાં ફિંગરપ્રિન્ટ માગે છે), 2FA કોડ, પાસવર્ડ ઇતિહાસ, એન્ક્રિપ્ટેડ બૅકઅપ, ઑટો-લૉક અને Undo. હવે 17 ભાષાઓમાં, વૈકલ્પિક ક્રૅશ રિપોર્ટ સાથે. ઘણા સુધારા.

**ta-IN**
உங்கள் பெட்டகத்துக்கு இப்போது சொந்த விசை உள்ளது; அது ஃபோனின் பாதுகாப்பான வன்பொருளில் இருக்கும். கேட்கும்போது ஒருமுறை மீட்புக் கடவுச்சொல்லை அமைக்கவும். புதியது: ஆப்ஸ், Chrome-இல் தானியங்கு நிரப்பல் (முதலில் கைரேகை கேட்கும்), 2FA குறியீடுகள், கடவுச்சொல் வரலாறு, மறைகுறியாக்கப்பட்ட காப்புப்பிரதி, தானியங்கு பூட்டு, Undo. இப்போது 17 மொழிகளில். பல திருத்தங்கள்.

**te-IN**
మీ వాల్ట్‌కు ఇప్పుడు సొంత కీ ఉంది, అది ఫోన్ సురక్షిత హార్డ్‌వేర్‌లో ఉంటుంది. అడిగినప్పుడు ఒకసారి రికవరీ పాస్‌వర్డ్ సెట్ చేయండి. కొత్తవి: యాప్‌లు, Chromeలో ఆటోఫిల్ (ముందు ఎప్పుడూ వేలిముద్ర అడుగుతుంది), 2FA కోడ్‌లు, పాస్‌వర్డ్ చరిత్ర, ఎన్‌క్రిప్టెడ్ బ్యాకప్, ఆటో-లాక్, Undo. ఇప్పుడు 17 భాషల్లో, ఐచ్ఛిక క్రాష్ నివేదికలతో. ఎన్నో సవరణలు.

**ar**
أصبح لخزنتك الآن مفتاحها الخاص المحفوظ في العتاد الآمن لهاتفك. عيّن كلمة مرور للاسترداد مرة واحدة عند الطلب. الجديد: ملء تلقائي في التطبيقات وChrome (يطلب بصمتك أولًا دائمًا)، ورموز المصادقة الثنائية، وسجل كلمات المرور، ونسخ احتياطية مشفّرة، وقفل تلقائي، وتراجع. متوفر الآن بـ17 لغة مع تقارير أعطال اختيارية. إصلاحات كثيرة.

**de-DE**
Dein Tresor hat jetzt einen eigenen Schlüssel in der sicheren Hardware deines Telefons. Lege bei Aufforderung einmalig ein Wiederherstellungspasswort fest. Neu: Autofill in Apps und Chrome (fragt immer zuerst nach deinem Fingerabdruck), 2FA-Codes, Passwortverlauf, verschlüsselte Backups, automatische Sperre und Rückgängig. Jetzt in 17 Sprachen, mit optionalen Absturzberichten. Viele Korrekturen.

**es-ES**
Tu bóveda tiene ahora su propia clave, guardada en el hardware seguro del teléfono. Crea una contraseña de recuperación una sola vez cuando se te pida. Novedades: autocompletar en apps y Chrome (siempre pide tu huella primero), códigos 2FA, historial de contraseñas, copias cifradas, bloqueo automático y Deshacer. Ahora en 17 idiomas, con informes de errores opcionales. Muchas correcciones.

**fr-FR**
Votre coffre a désormais sa propre clé, conservée dans le matériel sécurisé du téléphone. Créez une seule fois un mot de passe de récupération quand on vous le demande. Nouveau : saisie automatique dans les apps et Chrome (demande toujours votre empreinte d'abord), codes 2FA, historique des mots de passe, sauvegardes chiffrées, verrouillage auto et Annuler. En 17 langues, rapports de plantage facultatifs.

**it-IT**
La tua cassaforte ora ha una chiave tutta sua, custodita nell'hardware sicuro del telefono. Imposta una sola volta una password di recupero quando richiesto. Novità: compilazione automatica in app e Chrome (chiede sempre prima l'impronta), codici 2FA, cronologia password, backup crittografati, blocco automatico e Annulla. Ora in 17 lingue, con report di arresto anomalo facoltativi. Molte correzioni.

**pt-BR**
Seu cofre agora tem uma chave própria, guardada no hardware seguro do celular. Crie uma senha de recuperação uma única vez quando solicitado. Novidades: preenchimento automático em apps e no Chrome (sempre pede sua digital antes), códigos 2FA, histórico de senhas, backups criptografados, bloqueio automático e Desfazer. Agora em 17 idiomas, com relatórios de falhas opcionais. Muitas correções.

**ru-RU**
Теперь у вашего хранилища собственный ключ в защищённом оборудовании телефона. Один раз задайте пароль восстановления, когда приложение попросит. Новое: автозаполнение в приложениях и Chrome (всегда сначала спрашивает отпечаток), коды 2FA, история паролей, зашифрованные копии, автоблокировка и отмена удаления. Теперь на 17 языках, с необязательными отчётами о сбоях. Много исправлений.

**ja-JP**
保管庫に専用の鍵が付き、スマートフォンの安全なハードウェアに保管されるようになりました。求められたら一度だけ復旧用パスワードを設定してください。新機能: アプリと Chrome での自動入力（必ず先に指紋を確認）、2FA コード、パスワード履歴、暗号化バックアップ、自動ロック、元に戻す。17 言語に対応し、クラッシュレポートは任意です。多数の修正。

**ko-KR**
이제 보관함에 휴대전화의 보안 하드웨어에 저장되는 전용 키가 생겼습니다. 요청 시 복구 비밀번호를 한 번만 설정하세요. 새 기능: 앱과 Chrome 자동 완성(항상 먼저 지문 확인), 2FA 코드, 비밀번호 기록, 암호화 백업, 자동 잠금, 실행 취소. 이제 17개 언어 지원, 비정상 종료 보고서는 선택 사항입니다. 다양한 수정 사항.

**zh-CN**
你的保险库现在拥有自己的密钥，保存在手机的安全硬件中。按提示设置一次恢复密码即可。新功能：在应用和 Chrome 中自动填充（始终先验证指纹）、两步验证码、密码历史、加密备份、自动锁定和撤销。现支持 17 种语言，崩溃报告可自愿开启。多项修复。
