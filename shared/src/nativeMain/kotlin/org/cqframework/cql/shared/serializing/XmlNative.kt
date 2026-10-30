package org.cqframework.cql.shared.serializing

// Probe only: a small XML parser and serializer for well-formed documents without a DTD.

actual fun parseXml(xml: String): XmlNode.Element {
    var i = 0
    val stack = mutableListOf(MutableXmlNode.Element("", emptyMap(), mutableListOf()))
    fun fail(msg: String): Nothing = throw IllegalArgumentException("XML: $msg at $i")
    while (i < xml.length) {
        if (xml[i] != '<') {
            val end = xml.indexOf('<', i).let { if (it < 0) xml.length else it }
            val text = unescape(xml.substring(i, end))
            if (stack.size > 1) stack.last().children.add(MutableXmlNode.Text(text))
            i = end
            continue
        }
        when {
            xml.startsWith("<?", i) -> i = xml.indexOf("?>", i).takeIf { it >= 0 }?.plus(2) ?: fail("unclosed <?")
            xml.startsWith("<!--", i) -> i = xml.indexOf("-->", i).takeIf { it >= 0 }?.plus(3) ?: fail("unclosed comment")
            xml.startsWith("<![CDATA[", i) -> {
                val end = xml.indexOf("]]>", i).takeIf { it >= 0 } ?: fail("unclosed CDATA")
                stack.last().children.add(MutableXmlNode.Text(xml.substring(i + 9, end)))
                i = end + 3
            }
            xml.startsWith("<!", i) -> i = xml.indexOf('>', i).takeIf { it >= 0 }?.plus(1) ?: fail("unclosed <!")
            xml.startsWith("</", i) -> {
                val end = xml.indexOf('>', i).takeIf { it >= 0 } ?: fail("unclosed end tag")
                if (stack.size < 2) fail("unexpected end tag")
                stack.removeAt(stack.lastIndex)
                i = end + 1
            }
            else -> {
                i++
                val nameStart = i
                while (i < xml.length && !xml[i].isWhitespace() && xml[i] != '>' && xml[i] != '/') i++
                val name = xml.substring(nameStart, i)
                val attributes = linkedMapOf<String, String>()
                while (true) {
                    while (i < xml.length && xml[i].isWhitespace()) i++
                    if (i >= xml.length) fail("unclosed start tag")
                    if (xml[i] == '>' || xml.startsWith("/>", i)) break
                    val an = i
                    while (xml[i] != '=' && !xml[i].isWhitespace()) i++
                    val attrName = xml.substring(an, i)
                    while (xml[i].isWhitespace() || xml[i] == '=') i++
                    val quote = xml[i]
                    val ve = xml.indexOf(quote, i + 1).takeIf { it >= 0 } ?: fail("unclosed attribute")
                    attributes[attrName] = unescape(xml.substring(i + 1, ve))
                    i = ve + 1
                }
                val element = MutableXmlNode.Element(name, attributes, mutableListOf())
                stack.last().children.add(element)
                if (xml[i] == '/') i += 2 else { i++; stack.add(element) }
            }
        }
    }
    val root = stack.first().children.filterIsInstance<MutableXmlNode.Element>().singleOrNull() ?: fail("no root element")
    return root.toImmutable()
}

private fun unescape(s: String): String =
    if ('&' !in s) s
    else Regex("&(#x[0-9a-fA-F]+|#[0-9]+|lt|gt|amp|quot|apos);").replace(s) {
        val e = it.groupValues[1]
        when {
            e == "lt" -> "<"
            e == "gt" -> ">"
            e == "amp" -> "&"
            e == "quot" -> "\""
            e == "apos" -> "'"
            e.startsWith("#x") -> codePoint(e.substring(2).toInt(16))
            else -> codePoint(e.substring(1).toInt())
        }
    }

private fun codePoint(cp: Int): String =
    if (cp < 0x10000) cp.toChar().toString()
    else (cp - 0x10000).let { charArrayOf((0xD800 + (it shr 10)).toChar(), (0xDC00 + (it and 0x3FF)).toChar()).concatToString() }

private fun escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

actual fun toXmlString(element: XmlNode.Element, namespaces: Map<String, String>): String {
    val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    fun write(e: XmlNode.Element, root: Boolean) {
        sb.append('<').append(e.tagName)
        if (root) for ((prefix, uri) in namespaces) {
            sb.append(if (prefix.isEmpty()) " xmlns" else " xmlns:$prefix").append("=\"").append(escape(uri)).append('"')
        }
        for ((k, v) in e.attributes) sb.append(' ').append(k).append("=\"").append(escape(v)).append('"')
        if (e.children.isEmpty()) { sb.append("/>"); return }
        sb.append('>')
        for (c in e.children) when (c) {
            is XmlNode.Text -> sb.append(escape(c.text))
            is XmlNode.Element -> write(c, false)
        }
        sb.append("</").append(e.tagName).append('>')
    }
    write(element, true)
    return sb.toString()
}
