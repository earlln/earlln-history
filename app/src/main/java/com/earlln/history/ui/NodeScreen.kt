package com.earlln.history.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlln.history.data.HistoryRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NodeScreen(repo: HistoryRepository, id: String, open: (String) -> Unit) {
    val node = repo.get(id)
    if (node == null) {
        Text("항목을 찾을 수 없습니다: $id", modifier = Modifier.padding(20.dp))
        return
    }
    val ancestors = repo.ancestors(id)
    val children = repo.children(id)
    val mentioned = repo.mentionedIn(id)
    val (prev, next) = repo.siblings(id)
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
    ) {
        // 위치: 한국사 › 고려 › 무신 정권
        if (ancestors.isNotEmpty()) {
            item {
                FlowRow(verticalArrangement = Arrangement.Center) {
                    ancestors.forEachIndexed { i, a ->
                        Text(
                            a.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.primary,
                            modifier = Modifier
                                .clickable { open(a.id) }
                                .padding(vertical = 4.dp),
                        )
                        Text(
                            if (i < ancestors.lastIndex) "  ›  " else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }

        item { Column {
            Spacer(Modifier.height(4.dp))
            TagRow {
                TypeTag(node.type)
                if (node.category.isNotBlank()) Tag(node.category, cs.surfaceVariant, cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            LinkedText(
                node.title, repo, open,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            if (node.period.isNotBlank()) {
                Text(node.period, style = MaterialTheme.typography.titleSmall, color = cs.primary)
            }
            Spacer(Modifier.height(12.dp))
        } }

        if (node.summary.isNotBlank()) {
            item { Column {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.primaryContainer.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    LinkedText(
                        node.summary, repo, open,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            } }
        }

        if (node.body.isNotBlank()) {
            item { Body(node.body, repo, open) }
        }

        if (children.isNotEmpty()) {
            item {
                SectionTitle(
                    when (node.type) {
                        "subject" -> "시대"
                        "era" -> "주요 주제"
                        "topic" -> "핵심 키워드"
                        else -> "세부 항목"
                    }
                )
            }
            items(children, key = { it.id }) { child ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { open(child.id) },
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                child.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            if (child.period.isNotBlank()) {
                                Text(child.period, style = MaterialTheme.typography.labelSmall, color = cs.primary)
                            }
                        }
                        if (child.summary.isNotBlank()) {
                            Text(
                                repo.plain(child.summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (mentioned.isNotEmpty()) {
            item { Column {
                SectionTitle("함께 보기 — 이 항목이 나오는 곳")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mentioned.forEach { m ->
                        AssistChip(onClick = { open(m.id) }, label = { Text(m.title) })
                    }
                }
            } }
        }

        if (prev != null || next != null) {
            item { Column {
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (prev != null) {
                        OutlinedButton(onClick = { open(prev.id) }, modifier = Modifier.weight(1f)) {
                            Text("‹ ${prev.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else Spacer(Modifier.weight(1f))
                    if (next != null) {
                        OutlinedButton(onClick = { open(next.id) }, modifier = Modifier.weight(1f)) {
                            Text(
                                "${next.title} ›",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                            )
                        }
                    } else Spacer(Modifier.weight(1f))
                }
            } }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Column {
    Spacer(Modifier.height(18.dp))
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(6.dp))
}

/** 본문: 빈 줄은 문단 구분, "## " 는 소제목, "- " 는 글머리표 */
@Composable
private fun Body(body: String, repo: HistoryRepository, open: (String) -> Unit) {
    Column {
        for (raw in body.lines()) {
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(8.dp))
                line.startsWith("## ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        line.removePrefix("## "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                line.startsWith("- ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    LinkedText(line.removePrefix("- "), repo, open)
                }
                else -> LinkedText(line, repo, open, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
