package com.earlln.history.data

import android.content.Context
import org.json.JSONObject

/** 역사 정보 한 칸. subject(한국사·세계사) → era(시대) → topic(주제) → key(키워드) 순으로 내려간다. */
data class HistoryNode(
    val id: String,
    val type: String,
    val title: String,
    val period: String,
    val category: String,
    val summary: String,
    val body: String,
    val parent: String?,
    val children: List<String>,
)

/** 본문 속 링크 표기: [[id]] 또는 [[id|보이는 글자]] */
val LINK_REGEX = Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?]]""")

class HistoryRepository private constructor(
    private val nodes: Map<String, HistoryNode>,
    val subjects: List<String>,
    val contentVersion: String,
) {
    private val backlinks: Map<String, List<String>> by lazy {
        val map = HashMap<String, MutableList<String>>()
        for (node in nodes.values) {
            val targets = LINK_REGEX.findAll(node.summary + "\n" + node.body)
                .map { it.groupValues[1].trim() }
                .toSet()
            for (target in targets) {
                if (target != node.id) map.getOrPut(target) { mutableListOf() }.add(node.id)
            }
        }
        map
    }

    fun get(id: String): HistoryNode? = nodes[id]

    fun children(id: String): List<HistoryNode> =
        nodes[id]?.children.orEmpty().mapNotNull { nodes[it] }

    /** 맨 위(과목)부터 바로 위 부모까지 */
    fun ancestors(id: String): List<HistoryNode> {
        val result = ArrayList<HistoryNode>()
        var cur = nodes[id]?.parent
        while (cur != null) {
            val node = nodes[cur] ?: break
            result.add(node)
            cur = node.parent
        }
        return result.reversed()
    }

    fun siblings(id: String): Pair<HistoryNode?, HistoryNode?> {
        val parent = nodes[id]?.parent ?: return null to null
        val list = nodes[parent]?.children.orEmpty()
        val i = list.indexOf(id)
        if (i < 0) return null to null
        return list.getOrNull(i - 1)?.let { nodes[it] } to list.getOrNull(i + 1)?.let { nodes[it] }
    }

    /** 이 항목을 본문에서 언급한 다른 항목들 (부모 · 자식은 이미 보이므로 뺀다) */
    fun mentionedIn(id: String): List<HistoryNode> {
        val node = nodes[id] ?: return emptyList()
        return backlinks[id].orEmpty()
            .filter { it != node.parent && it !in node.children }
            .mapNotNull { nodes[it] }
    }

    /** 이 항목이 속한 과목(한국사 · 세계사) id */
    fun subjectOf(id: String): String? {
        var cur: HistoryNode? = nodes[id]
        while (cur != null && cur.type != "subject") cur = cur.parent?.let { nodes[it] }
        return cur?.id
    }

    /** 과목의 핵심 키워드 전부 (가나다순) */
    fun keywords(subject: String): List<HistoryNode> =
        nodes.values.filter { it.type == "key" && subjectOf(it.id) == subject }
            .sortedBy { it.title }

    /** 키워드가 속한 시대 */
    fun eraOf(id: String): HistoryNode? = ancestors(id).firstOrNull { it.type == "era" }

    fun plain(text: String): String = LINK_REGEX.replace(text) { m ->
        m.groupValues[2].ifBlank { nodes[m.groupValues[1].trim()]?.title ?: m.groupValues[1] }
    }

    fun search(query: String): List<HistoryNode> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val inTitle = nodes.values.filter { it.type != "subject" && it.title.contains(q, ignoreCase = true) }
        val inText = nodes.values.filter {
            it.type != "subject" && it !in inTitle &&
                (plain(it.summary).contains(q, ignoreCase = true) || plain(it.body).contains(q, ignoreCase = true))
        }
        return (inTitle.sortedBy { it.title.length } + inText).take(80)
    }

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null

        fun get(context: Context): HistoryRepository =
            instance ?: synchronized(this) { instance ?: load(context).also { instance = it } }

        private fun load(context: Context): HistoryRepository {
            val text = context.assets.open("history.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val subjectsJson = root.getJSONArray("subjects")
            val subjects = (0 until subjectsJson.length()).map { subjectsJson.getString(it) }
            val nodesJson = root.getJSONObject("nodes")
            val nodes = HashMap<String, HistoryNode>()
            for (key in nodesJson.keys()) {
                val o = nodesJson.getJSONObject(key)
                val childrenJson = o.optJSONArray("children")
                val children = if (childrenJson == null) emptyList()
                else (0 until childrenJson.length()).map { childrenJson.getString(it) }
                nodes[key] = HistoryNode(
                    id = key,
                    type = o.getString("type"),
                    title = o.getString("title"),
                    period = o.optString("period", ""),
                    category = o.optString("category", ""),
                    summary = o.optString("summary", ""),
                    body = o.optString("body", ""),
                    parent = if (o.isNull("parent")) null else o.optString("parent").ifBlank { null },
                    children = children,
                )
            }
            return HistoryRepository(nodes, subjects, root.optString("version", ""))
        }
    }
}
