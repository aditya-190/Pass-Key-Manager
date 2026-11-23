# Release 6.0.0 (versionCode 48)

One Play update after 5.8.0 (47). The UI is unchanged (owner rule); everything new is a Settings row, a sheet option,
an onboarding page or a Home card in the existing style.

## What users get
- **Feature tour** on first install, skippable on every page, and replayable from Settings › Feature tour.
- **Backup safety:** Settings › Verify a backup (reads a backup without importing it) and a gentle reminder on Home
  when the vault has no recent backup (3 days after first use if never, 30 days after the last export).
- **Theme:** Same as phone / Light / Dark.
- **Passphrases:** "Use words" in the password generator (4–10 easy-to-type words).
- **Search by field name:** "pin" finds the entry that has a PIN field (values are never searched).
- **Icon shortcuts:** long-press the PassKey icon for Add entry or Search.
- **Android 14+ sign-in sheet:** apps that ask through Credential Manager can be filled from PassKey (after the
  fingerprint or PIN, like autofill).
- **Share with friends** in Settings, and a review request only after a good moment.
- Privacy and Terms are in About only (removed from the Settings list).

## Upgrade from 47
No database change. New preferences only (theme, backup reminder, review pacing), all with safe defaults. The tour is
not shown to people updating. Nothing to migrate.

## Firebase Remote Config (new keys, in-app defaults apply until published)
`review_prompt_enabled` true · `review_min_days` 3 · `review_cooldown_days` 120 · `review_after_fills` 3 ·
`latest_version_code` 48 · `config_version` 2.

## Play "What's new" (500 characters max each)

**en-US**
New: a short feature tour (replay it from Settings), Verify a backup, and a gentle reminder when your vault has no recent backup. Choose Light, Dark or Same as phone. The generator can make easy-to-type passphrases. Search now finds entries by field name. Long-press the PassKey icon to add or search. On Android 14 and newer, PassKey can fill passwords from the system sign-in sheet. Many fixes.

**hi-IN**
नया: छोटा फ़ीचर टूर (सेटिंग्स से दोबारा देखें), बैकअप जाँचें, और हाल का बैकअप न होने पर हल्का रिमाइंडर। लाइट, डार्क या फ़ोन जैसा थीम चुनें। जनरेटर आसान पासफ़्रेज़ बना सकता है। खोज अब फ़ील्ड के नाम से भी ढूँढती है। जोड़ने या खोजने के लिए PassKey आइकन को देर तक दबाएँ। Android 14 और नए पर, PassKey सिस्टम साइन-इन शीट से पासवर्ड भर सकता है। कई सुधार।

**bn-BD**
নতুন: ছোট ফিচার ট্যুর (সেটিংস থেকে আবার দেখুন), ব্যাকআপ যাচাই, আর সাম্প্রতিক ব্যাকআপ না থাকলে হালকা রিমাইন্ডার। লাইট, ডার্ক বা ফোনের মতো থিম বেছে নিন। জেনারেটর সহজ পাসফ্রেজ বানাতে পারে। সার্চ এখন ফিল্ডের নাম দিয়েও খোঁজে। যোগ বা খোঁজার জন্য PassKey আইকন চেপে ধরুন। Android 14 ও নতুনে PassKey সিস্টেম সাইন-ইন শিট থেকে পাসওয়ার্ড পূরণ করতে পারে। অনেক সংশোধন।

**mr-IN**
नवीन: छोटा फीचर टूर (सेटिंग्जमधून पुन्हा पाहा), बॅकअप तपासा, आणि अलीकडचा बॅकअप नसल्यास सौम्य आठवण. लाइट, डार्क किंवा फोनप्रमाणे थीम निवडा. जनरेटर सोपे पासफ्रेज बनवू शकतो. शोध आता फील्डच्या नावानेही शोधतो. जोडण्यासाठी किंवा शोधण्यासाठी PassKey आयकॉन दाबून ठेवा. Android 14 आणि नवीनवर PassKey सिस्टम साइन-इन शीटमधून पासवर्ड भरू शकते. अनेक सुधारणा.

**gu**
નવું: નાની ફીચર ટૂર (સેટિંગ્સમાંથી ફરી જુઓ), બૅકઅપ ચકાસો, અને તાજેતરનો બૅકઅપ ન હોય ત્યારે હળવું રિમાઇન્ડર. લાઇટ, ડાર્ક કે ફોન જેવી થીમ પસંદ કરો. જનરેટર સરળ પાસફ્રેઝ બનાવી શકે છે. શોધ હવે ફીલ્ડના નામથી પણ શોધે છે. ઉમેરવા કે શોધવા PassKey આઇકન દબાવી રાખો. Android 14 અને નવામાં PassKey સિસ્ટમ સાઇન-ઇન શીટમાંથી પાસવર્ડ ભરી શકે છે. ઘણા સુધારા.

**ta-IN**
புதியது: சிறிய அம்சச் சுற்றுலா (அமைப்புகளில் மீண்டும் பார்க்கலாம்), காப்புப்பிரதி சரிபார்ப்பு, சமீபத்திய காப்புப்பிரதி இல்லையெனில் மென்மையான நினைவூட்டல். லைட், டார்க் அல்லது ஃபோனைப் போல தீம். ஜெனரேட்டர் எளிய பாஸ்ஃப்ரேஸ் உருவாக்கும். தேடல் இப்போது புலப் பெயராலும் கண்டறியும். சேர்க்க அல்லது தேட PassKey ஐகானை அழுத்திப் பிடியுங்கள். Android 14 மற்றும் புதியவற்றில் சிஸ்டம் உள்நுழைவுத் தாளிலிருந்து கடவுச்சொற்களை நிரப்பும். பல திருத்தங்கள்.

**te-IN**
కొత్తవి: చిన్న ఫీచర్ టూర్ (సెట్టింగ్‌ల నుండి మళ్లీ చూడండి), బ్యాకప్ తనిఖీ, ఇటీవలి బ్యాకప్ లేకపోతే సున్నితమైన రిమైండర్. లైట్, డార్క్ లేదా ఫోన్‌లాగే థీమ్ ఎంచుకోండి. జనరేటర్ సులభమైన పాస్‌ఫ్రేజ్‌లు చేస్తుంది. శోధన ఇప్పుడు ఫీల్డ్ పేరుతో కూడా వెతుకుతుంది. జోడించడానికి లేదా వెతకడానికి PassKey ఐకాన్‌ను నొక్కి పట్టుకోండి. Android 14 మరియు కొత్తవాటిలో సిస్టమ్ సైన్-ఇన్ షీట్ నుండి పాస్‌వర్డ్‌లు నింపుతుంది. ఎన్నో సవరణలు.

**ar**
الجديد: جولة قصيرة في الميزات (أعدها من الإعدادات)، والتحقق من نسخة احتياطية، وتذكير لطيف عندما لا توجد نسخة حديثة. اختر فاتح أو داكن أو مثل الهاتف. يمكن للمولّد إنشاء عبارات مرور سهلة الكتابة. يجد البحث الآن الإدخالات باسم الحقل. اضغط مطولًا على أيقونة PassKey للإضافة أو البحث. على Android 14 والأحدث، يملأ PassKey كلمات المرور من ورقة تسجيل الدخول في النظام. إصلاحات كثيرة.

**de-DE**
Neu: eine kurze Funktionstour (in den Einstellungen wiederholbar), Backup prüfen und eine sanfte Erinnerung, wenn kein aktuelles Backup existiert. Wähle Hell, Dunkel oder Wie Telefon. Der Generator erstellt leicht tippbare Passphrasen. Die Suche findet Einträge jetzt auch über Feldnamen. Halte das PassKey-Symbol gedrückt zum Hinzufügen oder Suchen. Ab Android 14 füllt PassKey Passwörter im System-Anmeldefenster aus. Viele Korrekturen.

**es-ES**
Novedades: un breve recorrido por las funciones (repítelo desde Ajustes), Verificar una copia y un aviso amable si no tienes una copia reciente. Elige Claro, Oscuro o Igual que el teléfono. El generador crea frases de contraseña fáciles de escribir. La búsqueda encuentra entradas por nombre de campo. Mantén pulsado el icono de PassKey para añadir o buscar. En Android 14 y posteriores, PassKey rellena contraseñas desde la hoja de inicio de sesión del sistema. Muchas correcciones.

**fr-FR**
Nouveau : une courte visite guidée (à revoir depuis les Paramètres), Vérifier une sauvegarde et un rappel discret sans sauvegarde récente. Choisissez Clair, Sombre ou Comme le téléphone. Le générateur crée des phrases secrètes faciles à saisir. La recherche trouve les entrées par nom de champ. Appui long sur l'icône PassKey pour ajouter ou rechercher. Sur Android 14 et plus, PassKey remplit les mots de passe depuis la fenêtre de connexion du système.

**it-IT**
Novità: un breve tour delle funzioni (ripetibile dalle Impostazioni), Verifica un backup e un promemoria discreto se manca un backup recente. Scegli Chiaro, Scuro o Come il telefono. Il generatore crea passphrase facili da digitare. La ricerca trova le voci anche per nome del campo. Tieni premuta l'icona di PassKey per aggiungere o cercare. Da Android 14 in poi, PassKey compila le password dal foglio di accesso del sistema. Molte correzioni.

**pt-BR**
Novidades: um tour rápido pelos recursos (repita nas Configurações), Verificar um backup e um lembrete gentil quando não há backup recente. Escolha Claro, Escuro ou Igual ao celular. O gerador cria frases-senha fáceis de digitar. A busca encontra entradas pelo nome do campo. Toque e segure o ícone do PassKey para adicionar ou buscar. No Android 14 e mais novos, o PassKey preenche senhas pela folha de login do sistema. Muitas correções.

**ru-RU**
Новое: короткий обзор возможностей (повторите из настроек), проверка резервной копии и мягкое напоминание, если свежей копии нет. Тема: светлая, тёмная или как в телефоне. Генератор создаёт удобные парольные фразы. Поиск находит записи по названию поля. Удерживайте значок PassKey, чтобы добавить или найти. На Android 14 и новее PassKey заполняет пароли из системного окна входа. Много исправлений.

**ja-JP**
新機能: 短い機能ツアー（設定から再表示可）、バックアップの確認、最近のバックアップがないときの控えめなお知らせ。ライト・ダーク・端末の設定に従うを選択可能。生成機能で入力しやすいパスフレーズを作成。フィールド名でも検索可能に。PassKey アイコンを長押しで追加・検索。Android 14 以降ではシステムのログイン画面からパスワードを入力できます。多数の修正。

**ko-KR**
새 기능: 짧은 기능 둘러보기(설정에서 다시 보기), 백업 확인, 최근 백업이 없을 때 부드러운 알림. 라이트, 다크 또는 휴대전화 설정과 동일 테마 선택. 생성기로 입력하기 쉬운 암호 문구 생성. 필드 이름으로도 검색. PassKey 아이콘을 길게 눌러 추가하거나 검색. Android 14 이상에서는 시스템 로그인 시트에서 비밀번호를 채울 수 있습니다. 다양한 수정 사항.

**zh-CN**
新功能：简短的功能导览（可在设置中重看）、验证备份，以及在没有近期备份时的温和提醒。可选浅色、深色或跟随手机。生成器可创建易于输入的密码短语。搜索现在也能按字段名称查找。长按 PassKey 图标即可添加或搜索。在 Android 14 及更高版本上，PassKey 可通过系统登录面板填充密码。多项修复。
