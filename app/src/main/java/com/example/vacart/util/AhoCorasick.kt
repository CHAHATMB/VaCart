package com.example.vacart.util

class AhoCorasick {
    data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        var isEndOfWord: Boolean = false,
        var failure: TrieNode? = null,
        var output: MutableList<String> = mutableListOf()
    )

    private val root = TrieNode()

    fun addWord(word: String) {
        var current = root
        for (char in word) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        current.isEndOfWord = true
    }

    fun build() {
        val queue = ArrayDeque<TrieNode>()
        root.children.values.forEach { child ->
            child.failure = root
            queue.add(child)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.children.forEach{child, value ->
                var failure = current.failure
                while ((failure != null) && !failure.children.containsKey(child)
                ) {
                    failure = failure.failure
                }
                value.failure = failure?.children?.get(child) ?: root
                value.output.addAll(value.failure!!.output)
                if (value.isEndOfWord) {
                    value.output.add(child.toString())
                }
                queue.add(value)
            }
        }
    }

    fun search(text: String): List<String> {
        val result = mutableListOf<String>()
        var current = root
        for (char in text) {
            while (current != null && !current.children.containsKey(char)) {
                current = current.failure!!
            }
            current = current.children.get(char) ?: root
            current.output.let { result.addAll(it) }
        }
        return result
    }
}