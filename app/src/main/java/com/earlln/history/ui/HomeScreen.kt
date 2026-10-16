package com.earlln.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlln.history.data.HistoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repo: HistoryRepository, open: (String) -> Unit) {
    var subjectIndex by rememberSaveable { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0: 연대별, 1: 키워드별
    val subject = repo.subjects[subjectIndex.coerceIn(0, repo.subjects.lastIndex)]

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subjectIndex,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            repo.subjects.forEachIndexed { i, id ->
                Tab(
                    selected = subjectIndex == i,
                    onClick = { subjectIndex = i },
                    text = { Text(repo.get(id)?.title ?: id, fontWeight = FontWeight.SemiBold) },
                )
            }
        }
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            listOf("연대별 흐름", "핵심 키워드").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = mode == i,
                    onClick = { mode = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }
        key(subject, mode) {
            if (mode == 0) EraTimeline(repo, subject, open) else KeywordIndex(repo, subject, open)
        }
    }
}

/** 1단계: 시대별로 가장 간단한 필수 흐름 */
@Composable
private fun EraTimeline(repo: HistoryRepository, subject: String, open: (String) -> Unit) {
    val eras = repo.children(subject)
    val subjectNode = repo.get(subject)
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (subjectNode != null && subjectNode.summary.isNotBlank()) {
            item {
                LinkedText(
                    subjectNode.summary, repo, open,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
        items(eras, key = { it.id }) { era ->
            val topics = repo.children(era.id)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { open(era.id) }
                    .padding(horizontal = 16.dp)
            ) {
                // 연표 선
                Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier
                            .padding(top = 18.dp)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                Column(Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp)) {
                    Text(
                        era.period,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        era.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(repo.plain(era.summary), style = MaterialTheme.typography.bodyMedium)
                    if (topics.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            topics.joinToString(" · ") { it.title },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(
                Modifier.padding(start = 56.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** 연대와 상관없이 핵심 키워드만 모아 분류 · 가나다순으로 */
@Composable
private fun KeywordIndex(repo: HistoryRepository, subject: String, open: (String) -> Unit) {
    val all = remember(subject) { repo.keywords(subject) }
    val categories = remember(all) {
        val present = all.map { it.category }.filter { it.isNotBlank() }.toSet()
        CATEGORY_ORDER.filter { it in present } + (present - CATEGORY_ORDER.toSet()).sorted()
    }
    var selected by rememberSaveable { mutableStateOf("") }
    val shown = if (selected.isEmpty()) all else all.filter { it.category == selected }
    val groups = shown.groupBy { initialOf(it.title) }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                FilterChip(
                    selected = selected.isEmpty(),
                    onClick = { selected = "" },
                    label = { Text("전체 ${all.size}") },
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selected == cat,
                    onClick = { selected = if (selected == cat) "" else cat },
                    label = { Text("$cat ${all.count { it.category == cat }}") },
                )
            }
        }
        LazyColumn(
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            groups.forEach { (initial, list) ->
                item(key = "h:$initial") {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { k ->
                    val era = repo.eraOf(k.id)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { open(k.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                k.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            if (k.category.isNotBlank()) {
                                Tag(k.category, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(Modifier.width(4.dp))
                            if (era != null) {
                                Tag(era.title, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Text(
                            repo.plain(k.summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
