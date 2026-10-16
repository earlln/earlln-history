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

/** [[id|글자]] 를 눌러서 들어갈 수 있는 링크로 바꿔 보여 준다 */
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
    val annotated = buildAnnotatedString {
        var last = 0
        for (m in LINK_REGEX.findAll(text)) {
            append(text.substring(last, m.range.first))
            val id = m.groupValues[1].trim()
            val target = repo.get(id)
            val label = m.groupValues[2].ifBlank { target?.title ?: id }
            if (target != null) {
                withLink(LinkAnnotation.Clickable(tag = id, styles = linkStyle) { onOpen(id) }) {
                    append(label)
                }
            } else {
                append(label)
            }
            last = m.range.last + 1
        }
        append(text.substring(last))
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
