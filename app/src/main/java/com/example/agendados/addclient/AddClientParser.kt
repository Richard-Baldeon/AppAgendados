package com.example.agendados.addclient

import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalTime
import java.util.Locale

private val DIGIT_WORDS = mapOf(
    "cero" to "0",
    "uno" to "1",
    "una" to "1",
    "dos" to "2",
    "tres" to "3",
    "cuatro" to "4",
    "cinco" to "5",
    "seis" to "6",
    "siete" to "7",
    "ocho" to "8",
    "nueve" to "9"
)
private val SMALL_NUMBER_WORDS: Map<String, Long> = mapOf(
    "cero" to 0,
    "un" to 1,
    "uno" to 1,
    "una" to 1,
    "dos" to 2,
    "tres" to 3,
    "cuatro" to 4,
    "cinco" to 5,
    "seis" to 6,
    "siete" to 7,
    "ocho" to 8,
    "nueve" to 9,
    "diez" to 10,
    "once" to 11,
    "doce" to 12,
    "trece" to 13,
    "catorce" to 14,
    "quince" to 15,
    "dieciseis" to 16,
    "diecisiete" to 17,
    "dieciocho" to 18,
    "diecinueve" to 19,
    "veinte" to 20,
    "veintiuno" to 21,
    "veintidos" to 22,
    "veintitres" to 23,
    "veinticuatro" to 24,
    "veinticinco" to 25,
    "veintiseis" to 26,
    "veintisiete" to 27,
    "veintiocho" to 28,
    "veintinueve" to 29
)
private val TENS_NUMBER_WORDS: Map<String, Long> = mapOf(
    "treinta" to 30,
    "cuarenta" to 40,
    "cincuenta" to 50,
    "sesenta" to 60,
    "setenta" to 70,
    "ochenta" to 80,
    "noventa" to 90
)
private val HUNDRED_NUMBER_WORDS: Map<String, Long> = mapOf(
    "cien" to 100,
    "ciento" to 100,
    "doscientos" to 200,
    "trescientos" to 300,
    "cuatrocientos" to 400,
    "quinientos" to 500,
    "seiscientos" to 600,
    "setecientos" to 700,
    "ochocientos" to 800,
    "novecientos" to 900
)
private val LARGE_SCALE_NUMBER_WORDS: Map<String, Long> = mapOf(
    "mil" to 1_000,
    "millon" to 1_000_000,
    "millones" to 1_000_000,
    "billon" to 1_000_000_000,
    "billones" to 1_000_000_000
)
private val NUMBER_IGNORED_TOKENS = setOf("y", "con", "de", "del")
private val STOP_KEYWORDS: List<String> = listOf(
    "fin",
    "listo",
    "terminar",
    "ok",
    "tasa",
    "deuda",
    "monto",
    "saldo",
    "comentario",
    "comentarios",
    "compra",
    "traslado",
    "celular",
    "telefono",
    "teléfono",
    "nombre"
)

private val PHONE_REGEX = Regex("9[\\d\\s.\\-]{8,}")
private val NUMBER_REGEX = Regex("""\\b\\d[\\d.,\\s]*\\d(?:\\s*(?:k|mil))?\\b""", RegexOption.IGNORE_CASE)
private val COMMENT_REGEX = Regex("(?i)comentarios?:\\s*(.*)")
private val NAME_KEYWORD_REGEX = Regex("(?i)(?:nombre|se llama|cliente)\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*)")
private val TIME_REGEX = Regex("""(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?|am|pm)?\\b""")
private val AM_PM_ONLY_REGEX = Regex("(?i)\\b(?:am|pm|a\\.?m\\.?|p\\.?m\\.?)\\b")
private val CD_REGEX = Regex("\\bcd\\b")

private val INVALID_NAME_TOKENS = setOf("celular", "telefono", "teléfono", "numero", "número", "es")

/**
 * Result returned after parsing a dictation text.
 */
data class ParserResult(
    val celular: String? = null,
    val nombre: String? = null,
    val montoPP: String? = null,
    val tasaPP: String? = null,
    val deuda: String? = null,
    val montoCD: String? = null,
    val tasaCD: String? = null,
    val comentarios: String? = null,
    val scheduledTime: LocalTime? = null
)

/**
 * Parses the provided dictation [input] and returns a [ParserResult].
 */
fun parseDictation(input: String): ParserResult {
    val normalizedInput = normalizeText(input)
    val phoneDetection = detectPhone(normalizedInput)
    val phone = phoneDetection?.digits
    val name = detectName(input, phoneDetection?.range)
    val comment = detectComment(input)
    val monetaryMatches = NUMBER_REGEX.findAll(input)
    var montoPP: String? = extractLabeledAmount(
        input,
        listOf("monto pp", "monto prestamo personal", "monto préstamo personal", "monto del prestamo personal")
    )
    var tasaPP: String? = extractLabeledRate(
        input,
        listOf("tasa pp", "tasa prestamo personal", "tasa préstamo personal")
    )
    var deuda: String? = extractLabeledAmount(
        input,
        listOf("deuda", "saldo pendiente", "saldo")
    )
    var montoCD: String? = extractLabeledAmount(
        input,
        listOf("monto cd", "monto compra de deuda", "compra de deuda", "monto traslado")
    )
    var tasaCD: String? = extractLabeledRate(
        input,
        listOf("tasa cd", "tasa compra de deuda", "tasa traslado")
    )

    for (match in monetaryMatches) {
        val rawValue = match.value
        val context = extractContext(normalizedInput, match.range)
        val digitSequence = rawValue.filter { it.isDigit() }
        if (digitSequence.length == 9 && digitSequence.startsWith("9")) {
            continue
        }
        val normalizedAmount = normalizeAmount(rawValue)
        val normalizedRate = normalizeRate(rawValue)
        val before = normalizedInput.substring((match.range.first - 20).coerceAtLeast(0), match.range.first)
        val after = normalizedInput.substring(match.range.last + 1, (match.range.last + 21).coerceAtMost(normalizedInput.length))
        val combined = before + after
        val isRateContext = before.contains("tasa") || after.contains("tasa")
        val isCdMentioned = CD_REGEX.containsMatchIn(before) || CD_REGEX.containsMatchIn(after)
        val compraMentioned = combined.contains("compra")
        val trasladoMentioned = combined.contains("traslado")
        val montoCdMentioned = combined.contains("monto cd")
        val isCompraDeDeudaPhrase = combined.contains("compra de deuda")
        val isCdContext = isCdMentioned || compraMentioned || trasladoMentioned || montoCdMentioned || isCompraDeDeudaPhrase
        val isDebtContext = before.contains("deuda") || after.contains("deuda") || before.contains("saldo") || after.contains("saldo")
        val isMontoKeyword = before.contains("monto") || after.contains("monto") ||
            before.contains("prestamo") || after.contains("prestamo") ||
            before.contains("préstamo") || after.contains("préstamo")

        if (isRateContext || context.contains("tasa")) {
            if (isCdContext || context.contains("cd") || context.contains("compra")) {
                if (tasaCD == null) {
                    tasaCD = normalizedRate
                }
            } else if (tasaPP == null) {
                tasaPP = normalizedRate
            }
            continue
        }

        if (isCdContext || context.contains("compra") || context.contains("traslado") || context.contains("monto cd")) {
            if (montoCD == null) {
                montoCD = normalizedAmount
            }
            continue
        }

        if (isDebtContext || context.contains("deuda") || context.contains("saldo")) {
            if (!isCompraDeDeudaPhrase && !context.contains("compra de deuda") && deuda == null) {
                deuda = normalizedAmount
            }
            continue
        }

        if (isMontoKeyword || context.contains("monto") || context.contains("prestamo") || context.contains("préstamo")) {
            if (!isCdContext && !context.contains("cd") && montoPP == null) {
                montoPP = normalizedAmount
            }
            continue
        }

        if (montoPP == null) {
            montoPP = normalizedAmount
        }
    }

    val time = detectTime(input)

    return ParserResult(
        celular = phone,
        nombre = name,
        montoPP = montoPP,
        tasaPP = tasaPP,
        deuda = deuda,
        montoCD = montoCD,
        tasaCD = tasaCD,
        comentarios = comment,
        scheduledTime = time
    )
}

private fun normalizeText(input: String): String {
    val lower = input.lowercase(Locale.getDefault())
    val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
}

private data class PhoneDetection(val digits: String, val range: IntRange)

private fun detectPhone(text: String): PhoneDetection? {
    val match = PHONE_REGEX.find(text) ?: return null
    val digits = match.value.filter { it.isDigit() }
    return if (digits.length == 9) PhoneDetection(digits, match.range) else null
}

private fun detectName(original: String, phoneRange: IntRange?): String? {
    val keywordMatch = NAME_KEYWORD_REGEX.find(original)
    if (keywordMatch != null) {
        val raw = keywordMatch.groupValues[1]
        return raw.trim().uppercase(Locale.getDefault())
    }

    if (phoneRange != null) {
        val afterPhone = original.substring((phoneRange.last + 1).coerceAtMost(original.length))
        val tokens = afterPhone.split(" ", ",", ".", "-", "\n")
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val normalizedToken = normalizeText(trimmed)
            if (INVALID_NAME_TOKENS.contains(normalizedToken)) continue
            if (trimmed.firstOrNull()?.isUpperCase() == true) {
                return trimmed.uppercase(Locale.getDefault())
            }
        }
    }

    return null
}

private fun detectComment(input: String): String? {
    // Future parser extension: keep this logic isolated to allow replacing the detection
    // strategy without touching the main pipeline.
    val match = COMMENT_REGEX.find(input) ?: return null
    return match.groupValues[1].trim().takeIf { it.isNotEmpty() }
}

private fun extractContext(text: String, range: IntRange): String {
    val start = (range.first - 40).coerceAtLeast(0)
    val end = (range.last + 40).coerceAtMost(text.length)
    return text.substring(start, end)
}

private fun normalizeAmount(raw: String): String {
    val lowered = raw.lowercase(Locale.getDefault()).trim()
    if (lowered.isEmpty()) return raw.trim()

    var sanitized = lowered
        .replace("s/.", " ")
        .replace("s/", " ")
        .replace("soles", " ")
        .replace("sol", " ")
        .replace("pen", " ")
        .replace("dolares", " ")
        .replace("dólares", " ")
        .replace("usd", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val containsDigits = sanitized.any { it.isDigit() }
    var working = sanitized
    var multiplier = BigDecimal.ONE
    if (containsDigits) {
        if (working.contains("k")) {
            multiplier = BigDecimal(1_000)
            working = working.replace("k", "")
        }
        if (Regex("\\bmil\\b").containsMatchIn(working)) {
            multiplier = BigDecimal(1_000)
            working = working.replace(Regex("\\bmil\\b"), "")
        }
    }

    val digitsOnly = working
        .replace(" ", "")
        .replace(".", "")
        .replace(",", "")

    val amountDecimal = if (containsDigits && digitsOnly.any { it.isDigit() }) {
        digitsOnly.toBigDecimalOrNull()?.multiply(multiplier)
    } else {
        parseNumberPhrase(working)?.multiply(multiplier)
    }

    val amount = amountDecimal ?: return raw.trim()
    val rounded = amount.setScale(0, java.math.RoundingMode.HALF_UP)
    return try {
        formatAmount(rounded.toBigInteger().longValueExact())
    } catch (ex: ArithmeticException) {
        raw.trim()
    }
}

private fun formatAmount(value: Long): String {
    val formatted = String.format(Locale.US, "%,d", value)
    return formatted.replace(",", ".")
}

private fun normalizeRate(raw: String): String {
    val lowered = raw.lowercase(Locale.getDefault())
    val cleaned = lowered
        .replace("%", "")
        .replace("porciento", "")
        .replace("por ciento", "")
        .replace("porcentaje", "")
        .replace("por cien", "")
        .replace(Regex("\\s+"), " ")
        .trim()

    val numericCandidate = cleaned
        .replace("punto", ".")
        .replace("coma", ".")
        .replace(" ", "")

    val decimal = numericCandidate.toBigDecimalOrNull()
        ?: parseNumberPhrase(cleaned)
        ?: return raw.trim()

    val bounded = when {
        decimal < BigDecimal.ZERO -> BigDecimal.ZERO
        decimal > BigDecimal("200") -> BigDecimal("200")
        else -> decimal
    }
    val normalized = bounded.stripTrailingZeros().toPlainString()
    return "$normalized%"
}

private fun extractLabeledAmount(input: String, labels: List<String>): String? {
    val raw = extractValueAfterLabels(input, labels) ?: return null
    return normalizeAmount(raw)
}

private fun extractLabeledRate(input: String, labels: List<String>): String? {
    val raw = extractValueAfterLabels(input, labels) ?: return null
    return normalizeRate(raw)
}

private fun detectTime(input: String): LocalTime? {
    val amPmOnly = AM_PM_ONLY_REGEX.find(input)
    if (amPmOnly != null && !TIME_REGEX.containsMatchIn(input)) {
        return LocalTime.of(10, 30)
    }

    val match = TIME_REGEX.find(input) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
    val meridianRaw = match.groupValues.getOrNull(3)
    return when {
        meridianRaw.isNullOrBlank() -> {
            if (hour in 0..23) {
                LocalTime.of(hour % 24, minute.coerceIn(0, 59))
            } else null
        }

        meridianRaw.contains("p", ignoreCase = true) -> {
            LocalTime.of(if (hour % 12 == 0) 12 else (hour % 12 + 12), minute.coerceIn(0, 59))
        }

        else -> {
            LocalTime.of(hour % 12, minute.coerceIn(0, 59))
        }
    }
}

private fun extractValueAfterLabels(input: String, labels: List<String>): String? {
    val normalizedInput = normalizeText(input)
    for (label in labels) {
        val normalizedLabel = normalizeText(label)
        val index = normalizedInput.indexOf(normalizedLabel)
        if (index >= 0) {
            val start = index + normalizedLabel.length
            if (start >= input.length) continue
            val tail = input.substring(start)
            val cleaned = tail.trimStart(' ', ':', '-', '—', '=', '.', ',', ';')
            val extracted = cleaned.takeUntilStop()
            if (extracted.isNotBlank()) {
                return extracted.trim()
            }
        }
    }
    return null
}

private fun String.takeUntilStop(): String {
    if (isEmpty()) return this
    var end = length
    val normalized = normalizeText(this)
    val texto = this
    STOP_KEYWORDS.forEach { kw: String ->
        val idxOriginal = texto.indexOf(kw, startIndex = 0, ignoreCase = true)
        val idxNormalized = normalized.indexOf(kw, startIndex = 0, ignoreCase = true)
        val idx = listOf(idxOriginal, idxNormalized)
            .filter { it >= 0 }
            .minOrNull()
        if (idx != null && idx in 0 until end) {
            end = idx
        }
    }
    val newlineIndex = indexOf('\n')
    if (newlineIndex in 0 until end) end = newlineIndex
    val semicolonIndex = indexOf(';')
    if (semicolonIndex in 0 until end) end = semicolonIndex
    val colonIndex = indexOf(':')
    if (colonIndex in 0 until end) end = colonIndex
    for (i in 0 until end) {
        val ch = this[i]
        if ((ch == ',' || ch == '.') && getOrNull(i + 1)?.isDigit() != true) {
            end = i
            break
        }
    }
    val trimmedEnd = end.coerceIn(0, length)
    return substring(0, trimmedEnd).trimEnd(',', '.', '-', ':', ' ')
}

private fun parseNumberPhrase(text: String): BigDecimal? {
    val sanitized = normalizeText(text)
        .replace('-', ' ')
        .replace("punto", " coma ")
        .replace(",", " coma ")
        .replace(Regex("\s+"), " ")
        .trim()
    if (sanitized.isEmpty()) return null

    val tokens = sanitized.split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return null

    val commaIndex = tokens.indexOf("coma")
    if (commaIndex >= 0) {
        val integerTokens = tokens.subList(0, commaIndex)
        val fractionTokens = tokens.subList(commaIndex + 1, tokens.size)
        val integerValue = parseSpanishInteger(integerTokens)
        val fractionDigits = fractionTokens
            .mapNotNull { token -> DIGIT_WORDS[token] ?: token.takeIf { it.all(Char::isDigit) } }
            .joinToString("")
        if (integerValue != null) {
            val integerDecimal = BigDecimal.valueOf(integerValue)
            val fractionalDecimal = if (fractionDigits.isNotEmpty()) {
                ("0.$fractionDigits").toBigDecimalOrNull()
            } else {
                null
            }
            return fractionalDecimal?.let { integerDecimal + it } ?: integerDecimal
        }
        if (fractionDigits.isNotEmpty()) {
            return ("0.$fractionDigits").toBigDecimalOrNull()
        }
    }

    val integer = parseSpanishInteger(tokens)
    return integer?.let { BigDecimal.valueOf(it) }
}

private fun parseSpanishInteger(tokens: List<String>): Long? {
    if (tokens.isEmpty()) return null
    var total = 0L
    var current = 0L
    var hasValue = false

    for (token in tokens) {
        if (token.isBlank() || NUMBER_IGNORED_TOKENS.contains(token)) {
            continue
        }

        val digitWord = DIGIT_WORDS[token]?.toLong()
        if (digitWord != null) {
            current += digitWord
            hasValue = true
            continue
        }

        val small = SMALL_NUMBER_WORDS[token]
        if (small != null) {
            current += small
            hasValue = true
            continue
        }

        if (token.startsWith("veinti") && token != "veinte") {
            val unitToken = token.removePrefix("veinti")
            val unitValue = SMALL_NUMBER_WORDS[unitToken] ?: DIGIT_WORDS[unitToken]?.toLong()
            if (unitValue != null) {
                current += 20 + unitValue
                hasValue = true
                continue
            }
        }

        val tens = TENS_NUMBER_WORDS[token]
        if (tens != null) {
            current += tens
            hasValue = true
            continue
        }

        val hundreds = HUNDRED_NUMBER_WORDS[token]
        if (hundreds != null) {
            if (hundreds == 100L && current >= 100) {
                // Already accounted for a preceding hundred.
            } else {
                current += hundreds
            }
            hasValue = true
            continue
        }

        val largeScale = LARGE_SCALE_NUMBER_WORDS[token]
        if (largeScale != null) {
            if (current == 0L) {
                current = 1
            }
            total += current * largeScale
            current = 0L
            hasValue = true
            continue
        }

        val digitsToken = token.filter { it.isDigit() }
        if (digitsToken.isNotEmpty()) {
            return digitsToken.toLongOrNull()
        }
    }

    total += current
    return if (hasValue) total else null
}
