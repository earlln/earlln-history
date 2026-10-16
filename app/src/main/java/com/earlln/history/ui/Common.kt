package com.earlln.history.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.earlln.history.data.HistoryRepository
import com.earlln.history.data.LINK_REGEX

/** 분류 표시 순서. 여기에 없는 분류는 뒤에 붙는다. */
val CATEGORY_ORDER = listOf("인물", "사건·전쟁", "제도·정책", "국가·단체", "사상·종교", "문화·유산", "경제·사회", "조약·문서")

fun typeLabel(type: String) = when (type) {
    "subject" -> "과목"
    "era" -> "시대"
    "topic" -> "주제"
    "key" -> "키워드"
    else -> type
}

private const val CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

/** 가나다 묶음 머리글자 (쌍자음은 홑자음으로 묶는다) */
fun initialOf(title: String): String {
    val c = title.firstOrNull() ?: return "#"
    if (c in '가'..'힣') {
        val ch = CHOSEONG[(c - '가') / 588]
        return when (ch) {
            'ㄲ' -> "ㄱ"; 'ㄸ' -> "ㄷ"; 'ㅃ' -> "ㅂ"; 'ㅆ' -> "ㅅ"; 'ㅉ' -> "ㅈ"
            else -> ch.toString()
        }
    }
    if (c.isLetter()) return "A–Z"
    return "#"
}

/** 링크 아닌 낱말: 한글·영문·숫자 덩어리 */
private val WORD_REGEX = Regex("[가-힣A-Za-z0-9]+")

/** 낱말 끝의 조사·어미를 떼어 검색어로 (남는 글자가 두 자 이상일 때만) */
private val ENDINGS = listOf(
    "에서는", "으로는", "에서", "으로", "에게", "까지", "부터", "처럼", "보다", "이다", "였다", "했다", "하여", "하고", "하는", "하며", "되어", "되었다",
    "을", "를", "은", "는", "이", "가", "의", "에", "로", "와", "과", "도", "만", "한", "된",
)

fun searchTerm(word: String): String {
    for (e in ENDINGS) {
        if (word.endsWith(e) && word.length - e.length >= 2) return word.dropLast(e.length)
    }
    return word
}

fun googleUrl(term: String): String =
    "https://www.google.com/search?q=" + java.net.URLEncoder.encode(term, "UTF-8")

/** 링크 아닌 글을 낱말마다 구글 검색 링크로 붙인다 (숫자만인 낱말은 그냥 글자) */
private fun AnnotatedString.Builder.appendSearchable(
    text: String,
    styles: TextLinkStyles,
    onSearch: (String) -> Unit,
) {
    var last = 0
    for (m in WORD_REGEX.findAll(text)) {
        append(text.substring(last, m.range.first))
        val word = m.value
        if (word.all { it.isDigit() }) {
            append(word)
        } else {
            val term = searchTerm(word)
            withLink(LinkAnnotation.Clickable(tag = "search:$term", styles = styles) { onSearch(term) }) {
                append(word)
            }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

/**
 * [[id|글자]] 는 앱 안 항목으로 들어가는 링크(밑줄),
 * 나머지 낱말은 누르면 구글 검색으로 넘어가는 링크(겉모양은 보통 글자, 누를 때만 표시)로 보여 준다.
 */
@Composable
fun LinkedText(
    text: String,
    repo: HistoryRepository,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        ),
        pressedStyle = SpanStyle(background = MaterialTheme.colorScheme.primaryContainer),
    )
    val searchStyle = TextLinkStyles(
        pressedStyle = SpanStyle(background = MaterialTheme.colorScheme.secondaryContainer),
    )
    val uriHandler = LocalUriHandler.current
    // 브라우저가 없는 기기에서도 앱이 죽지 않게
    val onSearch: (String) -> Unit = { term -> runCatching { uriHandler.openUri(googleUrl(term)) } }
    val annotated = buildAnnotatedString {
        var last = 0
        for (m in LINK_REGEX.findAll(text)) {
            appendSearchable(text.substring(last, m.range.first), searchStyle, onSearch)
            val id = m.groupValues[1].trim()
            val target = repo.get(id)
            val label = m.groupValues[2].ifBlank { target?.title ?: id }
            if (target != null) {
                withLink(LinkAnnotation.Clickable(tag = id, styles = linkStyle) { onOpen(id) }) {
                    append(label)
                }
            } else {
                appendSearchable(label, searchStyle, onSearch)
            }
            last = m.range.last + 1
        }
        appendSearchable(text.substring(last), searchStyle, onSearch)
    }
    Text(annotated, modifier = modifier, style = style, color = color)
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun TypeTag(type: String) {
    val cs = MaterialTheme.colorScheme
    when (type) {
        "era" -> Tag(typeLabel(type), cs.primaryContainer, cs.onPrimaryContainer)
        "topic" -> Tag(typeLabel(type), cs.secondaryContainer, cs.onSecondaryContainer)
        else -> Tag(typeLabel(type), cs.tertiaryContainer, cs.onTertiaryContainer)
    }
}

@Composable
fun TagRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) { content() }
}
