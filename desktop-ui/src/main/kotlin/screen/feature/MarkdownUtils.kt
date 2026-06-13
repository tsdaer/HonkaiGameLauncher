package screen.feature

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

internal fun extractMarkdownText(node: ASTNode, source: String): String {
    return source.substring(node.startOffset, node.endOffset)
}

internal fun normalizeMarkdownLineEndings(markdown: String): String {
    return markdown.replace("\r\n", "\n").replace('\r', '\n')
}

internal fun ASTNode.isWhitespaceToken(): Boolean {
    return type == MarkdownTokenTypes.WHITE_SPACE || type == MarkdownTokenTypes.EOL
}

internal fun ASTNode.isMarkdownSyntaxToken(): Boolean {
    return type == MarkdownTokenTypes.EMPH ||
        type == MarkdownTokenTypes.BACKTICK ||
        type == MarkdownTokenTypes.ESCAPED_BACKTICKS ||
        type == MarkdownTokenTypes.LBRACKET ||
        type == MarkdownTokenTypes.RBRACKET ||
        type == MarkdownTokenTypes.LPAREN ||
        type == MarkdownTokenTypes.RPAREN ||
        type == MarkdownTokenTypes.EXCLAMATION_MARK ||
        type == MarkdownTokenTypes.COLON ||
        type == MarkdownTokenTypes.LT ||
        type == MarkdownTokenTypes.GT ||
        type == MarkdownTokenTypes.ATX_HEADER ||
        type == MarkdownTokenTypes.SETEXT_1 ||
        type == MarkdownTokenTypes.SETEXT_2 ||
        type == MarkdownTokenTypes.LIST_BULLET ||
        type == MarkdownTokenTypes.LIST_NUMBER ||
        type == MarkdownTokenTypes.LINK_ID ||
        type == MarkdownTokenTypes.LINK_TITLE ||
        type == MarkdownTokenTypes.CODE_FENCE_START ||
        type == MarkdownTokenTypes.CODE_FENCE_END ||
        type == MarkdownTokenTypes.FENCE_LANG ||
        type == GFMTokenTypes.TILDE ||
        type == GFMTokenTypes.TABLE_SEPARATOR
}

internal fun String.trimCellPipes(): String {
    return trim().trim('|').trim()
}

