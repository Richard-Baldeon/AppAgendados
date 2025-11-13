package com.example.agendados.addclient

import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalTime
import java.util.Locale

private val PHONE_REGEX = Regex("9[\\d\\s.\\-]{8,}")
private val NAME_KEYWORD_REGEX = Regex("(?i)(?:nombre|se llama|cliente)\\s*[:\\-]?\\s*([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:\\s+[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*)")
private val TIME_REGEX = Regex("""(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?|am|pm)?\\b""")
private val AM_PM_ONLY_REGEX = Regex("(?i)\\b(?:am|pm|a\\.?m\\.?|p\\.?m\\.?)\\b")
private const val NUMBER_CAPTURE_PATTERN = "\\d[\\d.,\\s]*\\d?(?:\\s*(?:k|mil))?"
private val MONTO_PP_SPECIFIC_REGEX = Regex(
    "(?i)monto\\s+(?:del\\s+)?(?:pr[eé]stamo\\s+personal|pp)\\s*[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)"
)
private val MONTO_PP_GENERAL_REGEX = Regex(
    "(?i)monto\\s*(?!del?\\s+pr[eé]stamo\\s+personal|pp|de\\s+compra\\s+de\\s+deuda|compra\\s+de\\s+deuda|cd)[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)"
)
private val TASA_PP_SPECIFIC_REGEX = Regex(
    "(?i)tasa\\s+(?:del\\s+)?(?:pr[eé]stamo\\s+personal|pp)\\s*[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)\\s*(%?)"
)
private val TASA_PP_GENERAL_REGEX = Regex(
    "(?i)tasa\\s*(?!del?\\s+compra\\s+de\\s+deuda|compra\\s+de\\s+deuda|cd)[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)\\s*(%?)"
)
private val DEUDA_REGEX = Regex(
    "(?i)deuda\\s*[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)"
)
private val MONTO_CD_REGEX = Regex(
    "(?i)monto\\s+(?:(?:de\\s+)?compra\\s+de\\s+deuda|cd)\\s*[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)"
)
private val TASA_CD_REGEX = Regex(
    "(?i)tasa\\s+(?:(?:de\\s+)?compra\\s+de\\s+deuda|cd)\\s*[:\\-–—=\\s]*($NUMBER_CAPTURE_PATTERN)\\s*(%?)"
)

private val INVALID_NAME_TOKENS = setOf("celular", "telefono", "teléfono", "numero", "número", "es")
private val NAME_STOP_KEYWORDS = listOf(
    "monto compra de deuda",
    "monto cd",
    "monto pp",
    "monto prestamo personal",
    "monto préstamo personal",
    "monto",
    "tasa compra de deuda",
    "tasa cd",
    "tasa prestamo personal",
    "tasa préstamo personal",
    "tasa pp",
    "tasa",
    "deuda",
    "comentarios",
    "comentario"
)
private val NAME_PREFIX_REGEX = Regex("(?i)^(?:nombre|se llama|cliente|es)\\b[:\\s\\-]*")
private val AMOUNT_VALUE_REGEX = Regex("""^\\s*(?:s\\s*/\\.?\\s*)?($NUMBER_CAPTURE_PATTERN)""", RegexOption.IGNORE_CASE)
private val RATE_VALUE_REGEX = Regex("""^\\s*($NUMBER_CAPTURE_PATTERN)\\s*(%?)""", RegexOption.IGNORE_CASE)

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
    if (input.isBlank()) {
        return ParserResult()
    }
    val normalizedInput = normalizeText(input)
    val phoneDetection = detectPhone(normalizedInput)
    val phone = phoneDetection?.digits
    val name = detectName(input, normalizedInput, phoneDetection?.range)
    val montoPP = extractMontoPP(input, normalizedInput)
    val tasaPP = extractTasaPP(input, normalizedInput)
    val deuda = extractAmountAfterLabels(input, normalizedInput, listOf("deuda"))
    val montoCD = extractAmountAfterLabels(input, normalizedInput, listOf("monto compra de deuda", "monto cd"))
    val tasaCD = extractRateAfterLabels(input, normalizedInput, listOf("tasa compra de deuda", "tasa cd"))
    val comentarios = detectComment(input, normalizedInput)
    val time = detectTime(input)

    return ParserResult(
        celular = phone,
        nombre = name,
        montoPP = montoPP,
        tasaPP = tasaPP,
        deuda = deuda,
        montoCD = montoCD,
        tasaCD = tasaCD,
        comentarios = comentarios,
        scheduledTime = time
    )
}

fun extractPhoneDigits(input: String): String? {
    if (input.isBlank()) {
        return null
    }
    val normalized = normalizeText(input)
    return detectPhone(normalized)?.digits
}

private fun normalizeText(input: String): String {
    val lower = input.lowercase(Locale.getDefault())
    val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
}

private fun findNameStopIndex(text: String): Int? {
    var best: Int? = null
    for (keyword in NAME_STOP_KEYWORDS) {
        val index = text.indexOf(keyword)
        if (index >= 0 && (best == null || index < best)) {
            best = index
        }
    }
    return best
}

private fun cleanNameCandidate(candidate: String): String? {
    var cleaned = candidate
        .trim()
        .trimStart(',', ':', '-', '—', '.', ';')
        .trimStart()
    cleaned = NAME_PREFIX_REGEX.replaceFirst(cleaned, "").trimStart(' ', ':', '-', '—', '.', ';')
    cleaned = cleaned.trimEnd(',', '.', ';', ':', '-', '—').trim()
    if (cleaned.isEmpty()) {
        return null
    }
    val normalized = normalizeText(cleaned)
    if (normalized.split(" ").any { INVALID_NAME_TOKENS.contains(it) }) {
        return null
    }
    return cleaned
}

private data class PhoneDetection(val digits: String, val range: IntRange)

private fun detectPhone(text: String): PhoneDetection? {
    val match = PHONE_REGEX.find(text) ?: return null
    val digits = match.value.filter { it.isDigit() }
    return if (digits.length == 9) PhoneDetection(digits, match.range) else null
}

private fun detectName(original: String, normalized: String, phoneRange: IntRange?): String? {
    val keywordMatch = NAME_KEYWORD_REGEX.find(original)
    if (keywordMatch != null) {
        val raw = keywordMatch.groupValues[1]
        return cleanNameCandidate(raw)
    }

    if (phoneRange != null) {
        val start = (phoneRange.last + 1).coerceAtMost(original.length)
        if (start >= original.length) {
            return null
        }
        val normalizedTail = normalized.substring(start)
        val stopIndex = findNameStopIndex(normalizedTail)
        val end = if (stopIndex != null) start + stopIndex else original.length
        if (end <= start) {
            return null
        }
        val candidate = original.substring(start, end)
        return cleanNameCandidate(candidate)
    }

    return null
}

private fun detectComment(input: String, normalized: String): String? {
    val labels = listOf("comentarios", "comentario")
    var bestIndex = -1
    var labelLength = 0
    for (label in labels) {
        val index = normalized.indexOf(label)
        if (index >= 0 && (bestIndex == -1 || index < bestIndex)) {
            bestIndex = index
            labelLength = label.length
        }
    }
    if (bestIndex == -1) {
        return null
    }
    val start = (bestIndex + labelLength).coerceAtMost(input.length)
    if (start >= input.length) {
        return null
    }
    val extracted = input.substring(start).trimStart(' ', ':', '-', '—', '=', '.', ',', ';')
    return extracted.takeIf { it.isNotBlank() }
}

private fun extractContext(text: String, range: IntRange): String {
    val start = (range.first - 40).coerceAtLeast(0)
    val end = (range.last + 40).coerceAtMost(text.length)
    return text.substring(start, end)
}

private fun normalizeAmount(raw: String): String {
    val lowered = raw.lowercase(Locale.getDefault())
    var sanitized = lowered
        .replace("s/.", "")
        .replace("s/", "")
        .replace("soles", "")
        .replace("sol", "")
        .replace("pen", "")
        .replace("dolares", "")
        .replace("dólares", "")
        .replace("usd", "")
        .trim()
    val multiplier = when {
        sanitized.contains("k") || sanitized.contains("mil") -> BigDecimal(1_000)
        else -> BigDecimal.ONE
    }
    sanitized = sanitized
        .replace("k", "")
        .replace("mil", "")
        .replace(" ", "")
    val number = parseNumberValue(sanitized) ?: return raw.trim()
    return number.multiply(multiplier).stripTrailingZeros().toPlainString()
}

private fun normalizeRate(raw: String): String {
    val lowered = raw.lowercase(Locale.getDefault())
    var sanitized = lowered
        .replace("%", "")
        .replace("porciento", "")
        .replace("por ciento", "")
        .replace("porcentaje", "")
        .replace("por", "")
        .replace("de", "")
        .trim()
    sanitized = sanitized.replace(" ", "")
    val number = parseNumberValue(sanitized) ?: return raw.trim()
    return number.stripTrailingZeros().toPlainString()
}

private fun parseNumberValue(value: String): BigDecimal? {
    if (value.isBlank()) return null
    val clean = value.filter { it.isDigit() || it == ',' || it == '.' }
    if (clean.isEmpty()) return null
    val lastComma = clean.lastIndexOf(',')
    val lastDot = clean.lastIndexOf('.')
    val decimalIndex = when {
        lastComma >= 0 && lastDot >= 0 -> maxOf(lastComma, lastDot)
        lastComma >= 0 -> if ((clean.length - lastComma - 1) <= 3) lastComma else -1
        lastDot >= 0 -> if ((clean.length - lastDot - 1) <= 3) lastDot else -1
        else -> -1
    }
    if (decimalIndex == -1) {
        return clean.replace(",", "").replace(".", "").toBigDecimalOrNull()
    }
    val builder = StringBuilder()
    clean.forEachIndexed { index, c ->
        when (c) {
            ',', '.' -> if (index == decimalIndex) builder.append('.')
            else -> builder.append(c)
        }
    }
    return builder.toString().toBigDecimalOrNull()
}

private fun extractMontoPP(input: String, normalized: String): String? {
    val specific = extractAmountAfterLabels(
        input,
        normalized,
        listOf(
            "monto prestamo personal",
            "monto préstamo personal",
            "monto del prestamo personal",
            "monto del préstamo personal",
            "monto pp"
        )
    )
    if (specific != null) {
        return specific
    }
    return extractGeneralMonto(input, normalized, "monto")
}

private fun extractTasaPP(input: String, normalized: String): String? {
    val specific = extractRateAfterLabels(
        input,
        normalized,
        listOf(
            "tasa prestamo personal",
            "tasa préstamo personal",
            "tasa del prestamo personal",
            "tasa del préstamo personal",
            "tasa pp"
        )
    )
    if (specific != null) {
        return specific
    }
    return extractGeneralTasa(input, normalized)
}

private fun extractGeneralMonto(input: String, normalized: String, label: String): String? {
    var searchIndex = normalized.indexOf(label)
    while (searchIndex >= 0) {
        val labelEnd = searchIndex + label.length
        val valueStart = advanceToValueStart(normalized, labelEnd)
        val lookAhead = normalized.substring(valueStart)
        if (!lookAhead.startsWith("compra") && !lookAhead.startsWith("cd")) {
            val value = extractAmountFromIndex(input, labelEnd)
            if (value != null) {
                return value
            }
        }
        searchIndex = normalized.indexOf(label, labelEnd)
    }
    return null
}

private fun extractGeneralTasa(input: String, normalized: String): String? {
    var searchIndex = normalized.indexOf("tasa")
    while (searchIndex >= 0) {
        val labelEnd = searchIndex + "tasa".length
        val valueStart = advanceToValueStart(normalized, labelEnd)
        val lookAhead = normalized.substring(valueStart)
        if (!lookAhead.startsWith("compra") && !lookAhead.startsWith("cd")) {
            val value = extractRateFromIndex(input, labelEnd)
            if (value != null) {
                return value
            }
        }
        searchIndex = normalized.indexOf("tasa", labelEnd)
    }
    return null
}

private fun extractAmountAfterLabels(
    input: String,
    normalized: String,
    labels: List<String>
): String? {
    var bestIndex = Int.MAX_VALUE
    var bestValue: String? = null
    for (label in labels) {
        var searchIndex = normalized.indexOf(label)
        while (searchIndex >= 0) {
            val labelEnd = searchIndex + label.length
            val normalizedStart = advanceToValueStart(normalized, labelEnd)
            val value = extractAmountFromIndex(input, labelEnd)
            if (value != null) {
                if (normalizedStart < bestIndex) {
                    bestIndex = normalizedStart
                    bestValue = value
                }
                break
            }
            searchIndex = normalized.indexOf(label, labelEnd)
        }
    }
    return bestValue
}

private fun extractRateAfterLabels(
    input: String,
    normalized: String,
    labels: List<String>
): String? {
    var bestIndex = Int.MAX_VALUE
    var bestValue: String? = null
    for (label in labels) {
        var searchIndex = normalized.indexOf(label)
        while (searchIndex >= 0) {
            val labelEnd = searchIndex + label.length
            val normalizedStart = advanceToValueStart(normalized, labelEnd)
            val value = extractRateFromIndex(input, labelEnd)
            if (value != null) {
                if (normalizedStart < bestIndex) {
                    bestIndex = normalizedStart
                    bestValue = value
                }
                break
            }
            searchIndex = normalized.indexOf(label, labelEnd)
        }
    }
    return bestValue
}

private fun advanceToValueStart(text: String, start: Int): Int {
    var index = start
    while (index < text.length) {
        when (text[index]) {
            ' ', ':', '-', '—', '=', '.', ',', ';' -> index++
            else -> break
        }
    }
    return index
}

private fun extractAmountFromIndex(original: String, labelEnd: Int): String? {
    val start = advanceToValueStart(original, labelEnd)
    if (start >= original.length) {
        return null
    }
    val tail = original.substring(start)
    val match = AMOUNT_VALUE_REGEX.find(tail) ?: return null
    val raw = tail.substring(match.range.first, match.range.last + 1)
    return normalizeAmount(raw)
}

private fun extractRateFromIndex(original: String, labelEnd: Int): String? {
    val start = advanceToValueStart(original, labelEnd)
    if (start >= original.length) {
        return null
    }
    val tail = original.substring(start)
    val match = RATE_VALUE_REGEX.find(tail) ?: return null
    val number = match.groupValues[1]
    val percent = match.groupValues.getOrNull(2).orEmpty()
    val raw = if (percent.isNotEmpty()) "$number%" else number
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


