package com.citi.gradle.plugins.helm.release.tags

import java.text.ParseException
import java.util.Scanner
import java.util.function.BinaryOperator
import java.util.regex.Pattern


/**
 * Parser for [TagExpression]s.
 *
 * Since tag expressions can be expected to be rather simple, we don't build a full-fledged parser here. Instead,
 * we first scan the input to produce a list of tokens, then successively reduce tokens to [TagExpression]s until
 * only a single expression (or a list of expressions) remains.
 */
internal object TagExpressionParser {

    private const val delimiters = "[,&()|!]"
    private val delimiterPattern = Pattern.compile("\\s+|\\s*(?=$delimiters)|(?<=$delimiters)\\s*")

    /**
     * Marker interface for tokens.
     */
    private interface Token


    /**
     * A token that is read from the scanner.
     */
    private enum class ScanToken : Token {
        LPAREN,
        RPAREN,
        AND,
        OR,
        NOT
    }


    /**
     * A "token" carrying a TagExpression. This will be produced by the scanner for symbol tokens (single tag)
     * and the always-match expression (`*`). Afterwards the reducers will replace tokens by [ExpressionToken]
     * until only [ExpressionToken]s remain.
     */
    private class ExpressionToken(val expression: TagExpression) : Token


    /**
     * Parse the input into a [TagExpression].
     *
     * @param input the input string
     * @return the parsed [TagExpression]
     */
    fun parse(input: String): TagExpression {
        val tokens = scan(input)
        return parse(tokens)
    }


    /**
     * Scan the input, producing a list of tokens.
     *
     * @param input the input string
     * @return a list of [Token]s
     */
    private fun scan(input: String): List<Token> =
        Scanner(input).useDelimiter(delimiterPattern).use { scanner ->
            generateSequence {
                if (scanner.hasNext()) scanner.next() else null
            }.mapNotNull { token ->
                // skip whitespace-only tokens -- this may happen with our delimiter expression if
                // there is whitespace between two operators
                if (token.isBlank()) null
                else when (token) {
                    "(" -> ScanToken.LPAREN
                    ")" -> ScanToken.RPAREN
                    "&" -> ScanToken.AND
                    ",", "|" -> ScanToken.OR
                    "!" -> ScanToken.NOT
                    "*" -> ExpressionToken(TagExpression.alwaysMatch())
                    else -> ExpressionToken(TagExpression.single(token))
                }
            }.toList()
        }


    /**
     * Parse a list of tokens into a [TagExpression].
     *
     * @param tokens the list of tokens
     * @return the parsed [TagExpression]
     */
    private fun parse(tokens: List<Token>): TagExpression {
        if (tokens.isEmpty()) return TagExpression.alwaysMatch()

        try {
            if (tokens.size == 1) {
                return (tokens.first() as? ExpressionToken)?.expression
                    ?: throw ParseException("unexpected token ${tokens.first()}", 0)
            }

            val reducedTokens = tokens
                .reduceWith { reduceParentheses(it) }
                .reduceWith { reduceNotExpression(it) }
                .reduceWith { reduceAndExpression(it) }
                .reduceWith { reduceOrExpression(it) }

            return reducedTokens
                .map { token ->
                    // At this point we should have only ExpressionTokens left, otherwise throw an exception
                    ((token as? ExpressionToken)
                        ?: throw ParseException("unexpected token $token", 0))
                        .expression
                }
                // having multiple ExpressionTokens left is ok, just combine them with OR
                .reduce { t1, t2 -> t1.or(t2) }

        } catch (e: ParseException) {
            throw IllegalArgumentException("Invalid tag expression: ${e.message}", e)
        }
    }


    /**
     * Reduce the list of tokens using the given reducer function.
     *
     * @param reducer a function that takes a list of [Token]s, and returns either a reduced list (having replaced
     *        scanned tokens with expression tokens), or `null` if it cannot reduce the list any further
     * @return the reduced list of [Token]s
     */
    private fun List<Token>.reduceWith(reducer: (List<Token>) -> List<Token>?): List<Token> =
        generateSequence(this, reducer).last()


    private fun reduceParentheses(tokens: List<Token>): List<Token>? {
        val leftParenIndex = tokens.lastIndexOf(ScanToken.LPAREN)
        if (leftParenIndex == -1) return null

        // find matching right parenthesis
        val rightParenIndex = tokens.subList(leftParenIndex + 1, tokens.size)
            .indexOf(ScanToken.RPAREN) + leftParenIndex + 1
        if (rightParenIndex <= leftParenIndex) {
            throw ParseException("unmatched parenthesis", 0)
        }
        val innerExpression = parse(tokens.subList(leftParenIndex + 1, rightParenIndex))
        return tokens.subList(0, leftParenIndex) +
                ExpressionToken(innerExpression) +
                tokens.subList(rightParenIndex + 1, tokens.size)
    }


    private fun reduceNotExpression(tokens: List<Token>): List<Token>? {
        val notIndex = tokens.indexOf(ScanToken.NOT)
        if (notIndex == -1) return null

        val nextToken = tokens.getOrNull(notIndex + 1)
            ?: throw ParseException("unexpected end of input, expected expression", 0)
        val expression = (nextToken as? ExpressionToken)?.expression
            ?: throw ParseException("unexpected token $nextToken, expected expression", 0)
        return tokens.subList(0, notIndex) +
                ExpressionToken(expression.not()) +
                tokens.subList(notIndex + 2, tokens.size)
    }


    private fun reduceAndExpression(tokens: List<Token>): List<Token>? =
        reduceBinaryExpression(tokens, ScanToken.AND) { e1, e2 -> e1.and(e2) }


    private fun reduceOrExpression(tokens: List<Token>): List<Token>? =
        reduceBinaryExpression(tokens, ScanToken.OR, BinaryOperator(TagExpression::or))


    private fun reduceBinaryExpression(
        tokens: List<Token>, operatorToken: ScanToken,
        combiner: BinaryOperator<TagExpression>
    ): List<Token>? {
        val operatorIndex = tokens.indexOf(operatorToken)
        if (operatorIndex == -1) return null
        val previousToken = tokens.getOrNull(operatorIndex - 1)
            ?: throw ParseException("unexpected token: $operatorToken, expected expression", 0)
        val previousExpression = (previousToken as? ExpressionToken)?.expression
            ?: throw ParseException("unexpected token: $operatorToken", 0)
        val nextToken = tokens.getOrNull(operatorIndex + 1)
            ?: throw ParseException("unexpected end of input, expected expression", 0)
        val nextExpressionToken = (nextToken as? ExpressionToken)?.expression
            ?: throw ParseException("unexpected token: $nextToken, expected expression", 0)
        val expression = combiner.apply(previousExpression, nextExpressionToken)
        return tokens.subList(0, operatorIndex - 1) +
                ExpressionToken(expression) +
                tokens.subList(operatorIndex + 2, tokens.size)
    }
}

