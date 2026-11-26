@file:OptIn(ExperimentalMaterial3Api::class)

package com.loka.phonefinder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Accent = Color(0xFFFF9800)
val Bg = Color(0xFF121212)
val Surf = Color(0xFF1E1E1E)
val Muted = Color(0xFF9E9E9E)
val ErrRed = Color(0xFFEF5350)

@Composable
fun LokaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.Black,
            secondary = Accent,
            background = Bg,
            surface = Surf,
            onSurface = Color(0xFFEEEEEE),
            onBackground = Color(0xFFEEEEEE),
            surfaceVariant = Color(0xFF2A2A2A)
        ),
        content = content
    )
}

@Composable
fun tr(ar: String, en: String): String {
    val l = LocalConfiguration.current.locales[0].language
    return if (l == "ar") ar else en
}

fun openUrl(ctx: Context, url: String): Boolean = try {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    true
} catch (e: Exception) {
    false
}

fun fmtTime(t: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(t))

@Composable
fun typeLabel(t: String): String = when (t) {
    "search_engine" -> tr("محرك بحث", "Search engine")
    "website_lookup" -> tr("رابط بحث في موقع", "Website lookup link")
    "homepage_only" -> tr("فتح الموقع فقط", "Website only")
    "app_based" -> tr("يعتمد على تطبيقه", "App-based service")
    "directory_site_search" -> tr("بحث داخل دليل", "Directory site search")
    "deep_link" -> tr("رابط مباشر", "Deep link")
    "site_search" -> tr("بحث داخل منصة", "Platform site search")
    else -> t
}

@Composable
fun App(prefs: SharedPreferences, vm: MainViewModel) {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("home") }
    var input by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("EG") }
    var mode by rememberSaveable { mutableStateOf("quick") }
    var norm by remember { mutableStateOf<Normalized?>(null) }
    var localMs by remember { mutableStateOf(0L) }
    var invalid by remember { mutableStateOf(false) }
    val states = remember { mutableStateMapOf<String, CardState>() }
    var disabled by remember {
        mutableStateOf<Set<String>>(prefs.getStringSet("disabled", emptySet()) ?: emptySet())
    }
    var saveHistory by remember { mutableStateOf(prefs.getBoolean("history", true)) }
    var online by remember { mutableStateOf(prefs.getBoolean("online", true)) }
    val reg by vm.registry.collectAsState()
    val history by vm.history.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val refreshFailed by vm.refreshFailed.collectAsState()
    val isAr = LocalConfiguration.current.locales[0].language == "ar"

    LaunchedEffect(Unit) { if (online) vm.refreshRegistry() }
    BackHandler(enabled = screen != "home") { screen = "home" }

    fun doSearch(m: String) {
        mode = m
        val t0 = System.nanoTime()
        val n = PhoneUtil.normalize(input, region)
        if (n == null || !n.valid) {
            invalid = true
            norm = null
            return
        }
        localMs = (System.nanoTime() - t0) / 1_000_000
        invalid = false
        norm = n
        states.clear()
        screen = "results"
        if (saveHistory) {
            vm.add(
                HistoryEntry(
                    input = input.trim(), e164 = n.e164, region = n.region,
                    mode = m, createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun openProvider(p: Provider) {
        val n = norm ?: return
        val template = if (p.url.isNotEmpty()) p.url else p.homepage
        val url = PhoneUtil.fillUrl(template, n)
        val ok = openUrl(ctx, url)
        states[p.id] = CardState(
            if (ok) CardStatus.EXTERNAL else CardStatus.UNAVAILABLE,
            System.currentTimeMillis(), -1L
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (screen != "home") {
                    TextButton(onClick = { screen = "home" }) {
                        Text(if (isAr) "→" else "←", fontSize = 20.sp, color = Accent)
                    }
                }
                Text(
                    "Loka PhoneFinder", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = Accent
                )
            }
            when (screen) {
                "home" -> HomeScreen(
                    input = input, onInput = { input = it; invalid = false },
                    region = region, onRegion = { region = it },
                    invalid = invalid, onSearch = { doSearch(it) },
                    onNav = { screen = it }
                )
                "results" -> {
                    val n = norm
                    if (n != null) {
                        ResultsScreen(
                            n = n, mode = mode, localMs = localMs, reg = reg,
                            disabled = disabled, states = states,
                            onMode = { mode = it },
                            onOpen = { openProvider(it) }
                        )
                    }
                }
                "history" -> HistoryScreen(
                    history = history,
                    onPick = { e -> input = e.input; region = e.region; invalid = false; screen = "home" },
                    onDelete = { vm.deleteHistory(it) },
                    onClear = { vm.clearHistory() }
                )
                "providers" -> ProvidersScreen(
                    reg = reg, disabled = disabled, refreshing = refreshing,
                    refreshFailed = refreshFailed,
                    onToggle = { id, on ->
                        disabled = if (on) disabled - id else disabled + id
                        prefs.edit().putStringSet("disabled", disabled).apply()
                    },
                    onRefresh = { vm.refreshRegistry() }
                )
                "settings" -> SettingsScreen(
                    isAr = isAr, saveHistory = saveHistory, online = online,
                    onLang = { l ->
                        prefs.edit().putString("lang", l).apply()
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(l))
                    },
                    onSaveHistory = {
                        saveHistory = it
                        prefs.edit().putBoolean("history", it).apply()
                    },
                    onOnline = {
                        online = it
                        prefs.edit().putBoolean("online", it).apply()
                    },
                    onClearHistory = { vm.clearHistory() }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    input: String, onInput: (String) -> Unit,
    region: String, onRegion: (String) -> Unit,
    invalid: Boolean, onSearch: (String) -> Unit, onNav: (String) -> Unit
) {
    val clip = LocalClipboardManager.current
    val list = remember {
        val all = PhoneUtil.regions.map { r ->
            Triple(r, Locale("", r).displayCountry, PhoneUtil.callingCode(r))
        }
        all.filter { it.first == "EG" } + all.filter { it.first != "EG" }.sortedBy { it.second }
    }
    var exp by remember { mutableStateOf(false) }
    val cur = list.firstOrNull { it.first == region }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            tr("ابحث عن أي رقم هاتف", "Search any phone number"),
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold
        )
        Text(
            tr(
                "أدخل الرقم مرة واحدة. التطبيق يجهز صيغ الرقم وعبارات البحث ويفتح كل مصدر في المتصفح. لا يعرض أي نتيجة لم يجلبها فعلًا.",
                "Enter the number once. The app prepares number formats and search phrases and opens each source in your browser. It never shows a result it did not actually retrieve."
            ),
            color = Muted, fontSize = 13.sp
        )
        Box {
            OutlinedButton(onClick = { exp = true }) {
                Text("${cur?.second ?: region} (+${cur?.third ?: ""})")
            }
            DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                list.forEach { (r, name, cc) ->
                    DropdownMenuItem(
                        text = { Text("$name (+$cc)") },
                        onClick = { onRegion(r); exp = false }
                    )
                }
            }
        }
        OutlinedTextField(
            value = input, onValueChange = onInput,
            label = { Text(tr("رقم الهاتف", "Phone number")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (invalid) {
            Text(
                tr(
                    "الرقم غير صالح للدولة المختارة. تأكد من الرقم أو غيّر الدولة.",
                    "This number is not valid for the selected country. Check the number or change the country."
                ),
                color = ErrRed, fontSize = 13.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onInput(clip.getText()?.text ?: "") }) {
                Text(tr("لصق", "Paste"))
            }
            OutlinedButton(onClick = { onInput("") }) { Text(tr("مسح", "Clear")) }
        }
        Button(onClick = { onSearch("quick") }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("بحث سريع", "Quick Search"))
        }
        Button(
            onClick = { onSearch("deep") }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A), contentColor = Accent)
        ) { Text(tr("بحث عميق", "Deep Search")) }
        Button(
            onClick = { onSearch("engines") }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3A3A), contentColor = Accent)
        ) { Text(tr("كل محركات البحث", "Search All Engines")) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { onNav("history") }) { Text(tr("السجل", "History")) }
            TextButton(onClick = { onNav("providers") }) { Text(tr("المصادر", "Providers")) }
            TextButton(onClick = { onNav("settings") }) { Text(tr("الإعدادات", "Settings")) }
        }
    }
}

@Composable
fun StatusChip(s: CardStatus) {
    val c = when (s) {
        CardStatus.SUCCESS -> Color(0xFF66BB6A)
        CardStatus.EXTERNAL -> Accent
        CardStatus.READY -> Muted
        CardStatus.NO_RESULTS -> Muted
        CardStatus.UNAVAILABLE -> ErrRed
        CardStatus.AUTH -> Color(0xFFFFCA28)
        CardStatus.ERROR -> ErrRed
    }
    Text(
        tr(s.ar, s.en), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(c, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun ProviderCard(p: Provider, url: String, st: CardState, onOpen: () -> Unit) {
    val dead = p.status == "dead"
    val shown = if (dead) CardStatus.UNAVAILABLE else st.status
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Surf)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).background(Accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.name.take(1).uppercase(), color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontWeight = FontWeight.SemiBold)
                    Text(typeLabel(p.type), fontSize = 12.sp, color = Muted)
                }
                StatusChip(shown)
            }
            Text(tr(p.noteAr, p.noteEn), fontSize = 13.sp)
            if (dead) {
                Text(
                    tr(
                        "تم تعليم هذا المصدر كغير متاح بعد فحص الروابط (${p.lastChecked}).",
                        "Flagged unreachable by the registry check (${p.lastChecked})."
                    ),
                    fontSize = 12.sp, color = ErrRed
                )
            }
            Text(url, fontSize = 11.sp, color = Muted, maxLines = 2)
            val checked = if (st.checkedAt > 0)
                tr("آخر فتح: ", "Last opened: ") + fmtTime(st.checkedAt)
            else if (p.lastChecked.isNotEmpty())
                tr("فحص القائمة: ", "Registry check: ") + p.lastChecked + " (" + p.lastResult + ")"
            else tr("لم يتم الفحص بعد", "Not checked yet")
            Text(checked, fontSize = 11.sp, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen, enabled = !dead) {
                    Text(tr("فتح المصدر", "Open source"))
                }
                if (st.status == CardStatus.EXTERNAL || st.status == CardStatus.UNAVAILABLE) {
                    OutlinedButton(onClick = onOpen, enabled = !dead) {
                        Text(tr("إعادة المحاولة", "Retry"))
                    }
                }
            }
        }
    }
}

@Composable
fun LocalCard(n: Normalized, ms: Long) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Surf)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("تحليل الرقم على الجهاز", "On-device number analysis"), fontWeight = FontWeight.SemiBold)
                    Text(tr("libphonenumber (بدون إنترنت)", "libphonenumber (offline)"), fontSize = 12.sp, color = Muted)
                }
                StatusChip(CardStatus.SUCCESS)
            }
            Text(tr("الدولة: ", "Country: ") + n.region + " (+" + n.cc + ")", fontSize = 13.sp)
            Text(tr("النوع: ", "Line type: ") + n.typeName, fontSize = 13.sp)
            Text(tr("الصيغة الدولية: ", "International: ") + n.intl, fontSize = 13.sp)
            Text(tr("الصيغة المحلية: ", "National: ") + n.natDigits, fontSize = 13.sp)
            Text(
                tr("هذا تحليل لشكل الرقم فقط. لا يكشف اسم المالك.", "This only analyses the number's format. It does not reveal the owner."),
                fontSize = 12.sp, color = Muted
            )
            Text(tr("مدة التحليل: ", "Duration: ") + ms + " ms", fontSize = 11.sp, color = Muted)
            Text(
                tr("صيغ الرقم: ", "Variants: ") + n.variants.joinToString("  |  "),
                fontSize = 11.sp, color = Muted
            )
        }
    }
}

@Composable
fun ResultsScreen(
    n: Normalized, mode: String, localMs: Long, reg: Registry.Data,
    disabled: Set<String>, states: Map<String, CardState>,
    onMode: (String) -> Unit, onOpen: (Provider) -> Unit
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val providers = reg.providers.filter { it.id !in disabled && mode in it.modes }
    val queries = if (mode == "quick") n.queries.take(3) else n.queries
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(n.intl, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { clip.setText(AnnotatedString(n.e164)) }) {
                        Text(tr("نسخ الرقم", "Copy number"))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == "quick", onClick = { onMode("quick") },
                        label = { Text(tr("سريع", "Quick")) })
                    FilterChip(selected = mode == "deep", onClick = { onMode("deep") },
                        label = { Text(tr("عميق", "Deep")) })
                    FilterChip(selected = mode == "engines", onClick = { onMode("engines") },
                        label = { Text(tr("كل المحركات", "All engines")) })
                }
                Text(
                    tr(
                        "بطاقة لا تظهر \"نجاح\" إلا إذا جلب التطبيق المعلومة بنفسه. الروابط الخارجية تظهر \"بحث خارجي\" فقط.",
                        "A card shows SUCCESS only when the app itself retrieved the information. Opened links show EXTERNAL SEARCH only."
                    ),
                    fontSize = 12.sp, color = Muted
                )
            }
        }
        item { LocalCard(n, localMs) }
        item {
            Text(
                tr("عبارات البحث الجاهزة", "Ready search phrases"),
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)
            )
        }
        items(queries) { q ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(q, Modifier.weight(1f), fontSize = 13.sp)
                TextButton(onClick = { openUrl(ctx, PhoneUtil.searchUrl("google", q)) }) { Text("G") }
                TextButton(onClick = { openUrl(ctx, PhoneUtil.searchUrl("bing", q)) }) { Text("B") }
                TextButton(onClick = { openUrl(ctx, PhoneUtil.searchUrl("ddg", q)) }) { Text("D") }
            }
        }
        item {
            Text(
                tr("المصادر", "Providers") + " (" + providers.size + ")",
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)
            )
        }
        items(providers, key = { it.id }) { p ->
            val template = if (p.url.isNotEmpty()) p.url else p.homepage
            ProviderCard(
                p = p, url = PhoneUtil.fillUrl(template, n),
                st = states[p.id] ?: CardState(), onOpen = { onOpen(p) }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>, onPick: (HistoryEntry) -> Unit,
    onDelete: (Long) -> Unit, onClear: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("سجل البحث", "Search history"), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear, enabled = history.isNotEmpty()) {
                Text(tr("مسح الكل", "Delete all"), color = ErrRed)
            }
        }
        if (history.isEmpty()) {
            Text(tr("لا يوجد سجل.", "No history."), color = Muted)
        }
        LazyColumn {
            items(history, key = { it.id }) { e ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onPick(e) }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(e.e164)
                            Text(
                                e.mode + "  " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(e.createdAt)),
                                fontSize = 11.sp, color = Muted
                            )
                        }
                    }
                    TextButton(onClick = { onDelete(e.id) }) { Text("✕", color = ErrRed) }
                }
            }
        }
    }
}

@Composable
fun ProvidersScreen(
    reg: Registry.Data, disabled: Set<String>, refreshing: Boolean, refreshFailed: Boolean,
    onToggle: (String, Boolean) -> Unit, onRefresh: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("إدارة المصادر", "Provider management"), fontWeight = FontWeight.SemiBold)
                Text(
                    tr("مصدر القائمة: ", "Registry source: ") + reg.source + "  |  v" + reg.version +
                        "  |  " + reg.updated + "  |  " + reg.providers.size,
                    fontSize = 12.sp, color = Muted
                )
                Text(
                    tr(
                        "القائمة تُجلب من مستودع GitHub وتُخزن محليًا، وتُفحص الروابط يوميًا. لا توجد مفاتيح API في التطبيق.",
                        "The list is fetched from the GitHub repo and cached; links are re-checked daily by a GitHub Action. No API keys are stored in the app."
                    ),
                    fontSize = 12.sp, color = Muted
                )
                Button(onClick = onRefresh, enabled = !refreshing) {
                    Text(if (refreshing) tr("جارٍ التحديث...", "Updating...") else tr("تحديث القائمة", "Refresh list"))
                }
                if (refreshFailed) {
                    Text(
                        tr("تعذر التحديث. يتم استخدام النسخة المحفوظة.", "Update failed. Using the saved copy."),
                        color = ErrRed, fontSize = 12.sp
                    )
                }
            }
        }
        items(reg.providers, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        p.category + " | " + typeLabel(p.type) + " | " + p.status +
                            (if (p.lastChecked.isNotEmpty()) " | " + p.lastChecked + " " + p.lastResult else ""),
                        fontSize = 11.sp, color = if (p.status == "dead") ErrRed else Muted
                    )
                }
                Switch(checked = p.id !in disabled, onCheckedChange = { onToggle(p.id, it) })
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isAr: Boolean, saveHistory: Boolean, online: Boolean,
    onLang: (String) -> Unit, onSaveHistory: (Boolean) -> Unit,
    onOnline: (Boolean) -> Unit, onClearHistory: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(tr("اللغة", "Language"), fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isAr, onClick = { onLang("ar") }, label = { Text("العربية") })
            FilterChip(selected = !isAr, onClick = { onLang("en") }, label = { Text("English") })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("حفظ سجل البحث على الجهاز", "Save search history on this device"), Modifier.weight(1f))
            Switch(checked = saveHistory, onCheckedChange = onSaveHistory)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("تحديث قائمة المصادر من الإنترنت", "Update the provider list online"), Modifier.weight(1f))
            Switch(checked = online, onCheckedChange = onOnline)
        }
        OutlinedButton(onClick = onClearHistory) {
            Text(tr("حذف السجل", "Delete history"), color = ErrRed)
        }
        Text(tr("الخصوصية", "Privacy"), fontWeight = FontWeight.SemiBold)
        Text(
            tr(
                "الأرقام التي تبحث عنها تبقى على جهازك (سجل اختياري يمكن حذفه). التطبيق لا يرسل الرقم لأي خادم. عند فتح مصدر خارجي يُرسل الرقم إلى ذلك الموقع عبر المتصفح فقط وتنطبق سياسة خصوصيته. الاتصال الوحيد من التطبيق هو جلب ملف قائمة المصادر من GitHub، ولا يحتوي أي رقم. التطبيق لا يطلب أي صلاحيات غير الإنترنت.",
                "Numbers you search stay on your device (optional history you can delete). The app sends the number to no server. When you open an external source, the number goes to that website through your browser and its privacy policy applies. The app's only own connection is fetching the provider list file from GitHub, which contains no number. The app asks for no permission other than internet."
            ),
            fontSize = 13.sp
        )
        Text(tr("حدود التطبيق", "Limits"), fontWeight = FontWeight.SemiBold)
        Text(
            tr(
                "لا يتجاوز التطبيق تسجيل الدخول أو الكابتشا أو حدود الاستخدام، ولا يستخدم بيانات مسربة أو واجهات خاصة. لم يتم التحقق من وجود واجهة بحث عامة لأي مصدر، لذلك كل المصادر تُفتح خارجيًا. لا يؤكد التطبيق أن الرقم مسجل في أي منصة ولا أن اسمًا ما هو اسم المالك.",
                "The app does not bypass sign-in, CAPTCHAs or rate limits and uses no leaked data or private APIs. No public lookup API was verified for any source, so every source opens externally. The app does not confirm that a number is registered on any platform or that any name is the owner's."
            ),
            fontSize = 13.sp
        )
    }
}
