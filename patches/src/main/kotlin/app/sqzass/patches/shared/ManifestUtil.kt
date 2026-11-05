package app.sqzass.patches.shared

import org.w3c.dom.Document
import org.w3c.dom.Element

/** Manifest tags that carry a uses-permission declaration. */
val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23")

/**
 * Collect matching descendant elements of [parent] in document order.
 */
fun collectDescendants(parent: Element, tag: String): List<Element> = buildList {
    val nodes = parent.getElementsByTagName(tag)
    for (i in 0 until nodes.length) {
        add(nodes.item(i) as? Element ?: continue)
    }
}

/**
 * Collect matching elements across [tags] without mutating the document.
 * Matches are gathered into a list first so callers can remove them
 * without invalidating the live NodeList returned by [Document.getElementsByTagName].
 */
fun collectElements(
    document: Document,
    tags: List<String>,
    predicate: (Element) -> Boolean,
): List<Element> = buildList {
    for (tag in tags) {
        val nodes = document.getElementsByTagName(tag)
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            if (predicate(element)) add(element)
        }
    }
}

/**
 * Remove every element under [tags] matching [predicate].
 *
 * @return The android:name of the removed elements, for logging.
 */
fun removeElements(
    document: Document,
    tags: List<String>,
    predicate: (Element) -> Boolean,
): List<String> {
    val removed = mutableListOf<String>()
    for (element in collectElements(document, tags, predicate)) {
        removed += element.getAttribute("android:name")
        element.parentNode?.removeChild(element)
    }
    return removed
}