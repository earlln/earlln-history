package com.earlln.history.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earlln.history.BuildConfig
import com.earlln.history.data.HistoryRepository

const val HOME = "@home"
const val SEARCH = "@search"

private val stackSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarllnApp(repo: HistoryRepository) {
    // 화면 이동 기록. 맨 끝이 지금 보는 화면.
    val stack = rememberSaveable(saver = stackSaver) { mutableStateListOf(HOME) }
    val current = stack.last()
    // 화면마다 탭 선택 · 스크롤 위치를 기억해 두었다가 뒤로 오면 그대로 보여 준다
    val holder = rememberSaveableStateHolder()
    fun entryKey(index: Int) = "$index:${stack[index]}"

    val open: (String) -> Unit = { id -> if (id != stack.last()) stack.add(id) }
    val back: () -> Unit = {
        if (stack.size > 1) {
            holder.removeState(entryKey(stack.lastIndex))
            stack.removeAt(stack.lastIndex)
        }
    }
    val goHome: () -> Unit = {
        while (stack.size > 1) {
            holder.removeState(entryKey(stack.lastIndex))
            stack.removeAt(stack.lastIndex)
        }
        if (stack[0] != HOME) {
            holder.removeState(entryKey(0))
            stack[0] = HOME
        }
    }

    BackHandler(enabled = stack.size > 1) { back() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "Earlln History",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                },
                navigationIcon = {
                    if (stack.size > 1) {
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    }
                },
                actions = {
                    if (current != SEARCH) {
                        IconButton(onClick = { open(SEARCH) }) {
                            Icon(Icons.Filled.Search, contentDescription = "검색")
                        }
                    }
                    if (current != HOME) {
                        IconButton(onClick = goHome) {
                            Icon(Icons.Filled.Home, contentDescription = "처음으로")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            holder.SaveableStateProvider(entryKey(stack.lastIndex)) {
                when (current) {
                    HOME -> HomeScreen(repo, open)
                    SEARCH -> SearchScreen(repo, open)
                    else -> NodeScreen(repo, current, open)
                }
            }
        }
    }
}
