package com.citi.gradle.plugins.helm.release.tags

import assertk.all
import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


object TagExpressionParserTest : Spek({

    describe("parsing tags") {

        it("should parse an empty string to an always-match expression") {
            val expression = TagExpression.parse("")
            assertThat(expression)
                .isInstanceOf(AlwaysMatchTagExpression::class)
        }


        it("should parse a blank string to an always-match expression") {
            val expression = TagExpression.parse("    ")
            assertThat(expression)
                .isInstanceOf(AlwaysMatchTagExpression::class)
        }


        it("should parse * to an always-match expression") {
            val expression = TagExpression.parse("*")
            assertThat(expression)
                .isInstanceOf(AlwaysMatchTagExpression::class)
        }


        it("should parse a symbol as a single tag") {
            val expression = TagExpression.parse("tag")
            assertThat(expression)
                .isInstanceOf(SingleTagExpression::class)
                .prop(SingleTagExpression::tag).isEqualTo("tag")
        }


        it("should parse AND operator") {
            val expression = TagExpression.parse("foo&bar")
            assertThat(expression)
                .isInstanceOf(AllTagsExpression::class)
                .prop(AllTagsExpression::tags).containsOnly("foo", "bar")
        }


        it("should parse OR operator") {
            val expression = TagExpression.parse("foo|bar")
            assertThat(expression)
                .isInstanceOf(AnyTagExpression::class)
                .prop(AnyTagExpression::tags).containsOnly("foo", "bar")
        }


        it("should parse comma as OR operator") {
            val expression = TagExpression.parse("foo,bar")
            assertThat(expression)
                .isInstanceOf(AnyTagExpression::class)
                .prop(AnyTagExpression::tags).containsOnly("foo", "bar")
        }


        it("should parse space-separated tags as OR") {
            val expression = TagExpression.parse("foo bar")
            assertThat(expression)
                .isInstanceOf(AnyTagExpression::class)
                .prop(AnyTagExpression::tags).containsOnly("foo", "bar")
        }


        it("should parse NOT operator") {
            val expression = TagExpression.parse("!foo")
            assertThat(expression)
                .isInstanceOf(NotTagExpression::class)
                .prop(NotTagExpression::expression).isInstanceOf(SingleTagExpression::class)
                .prop(SingleTagExpression::tag).isEqualTo("foo")
        }


        it("NOT operator has precedence before AND") {
            val expression = TagExpression.parse("!A & B")

            assertThat(expression)
                .isInstanceOf(AndTagExpression::class)
                .prop(AndTagExpression::expressions).all {
                    hasSize(2)
                    index(0).isInstanceOf(NotTagExpression::class)
                        .prop(NotTagExpression::expression)
                        .isInstanceOf(SingleTagExpression::class)
                        .prop(SingleTagExpression::tag).isEqualTo("A")
                    index(1).isInstanceOf(SingleTagExpression::class)
                        .prop(SingleTagExpression::tag).isEqualTo("B")
                }
        }


        it("AND operator has precedence before OR") {
            val expression = TagExpression.parse("A & B | C & D")

            assertThat(expression)
                .isInstanceOf(OrTagExpression::class)
                .prop(OrTagExpression::expressions).all {
                    hasSize(2)
                    index(0).isInstanceOf(AllTagsExpression::class)
                        .prop(AllTagsExpression::tags).containsOnly("A", "B")
                    index(1).isInstanceOf(AllTagsExpression::class)
                        .prop(AllTagsExpression::tags).containsOnly("C", "D")
                }
        }


        it("should handle parentheses") {
            val expression = TagExpression.parse("A & (B | !(C & D)) & (E F)")

            assertThat(expression)
                .isInstanceOf(AndTagExpression::class)
                .prop(AndTagExpression::expressions).all {
                    hasSize(3)
                    index(0).isInstanceOf(SingleTagExpression::class)
                        .prop(SingleTagExpression::tag).isEqualTo("A")
                    index(1).isInstanceOf(OrTagExpression::class)
                        .prop(OrTagExpression::expressions).all {
                            hasSize(2)
                            index(0).isInstanceOf(SingleTagExpression::class)
                                .prop(SingleTagExpression::tag).isEqualTo("B")
                            index(1).isInstanceOf(NotTagExpression::class)
                                .prop(NotTagExpression::expression)
                                .isInstanceOf(AllTagsExpression::class)
                                .prop(AllTagsExpression::tags).containsOnly("C", "D")
                        }
                    index(2).isInstanceOf(AnyTagExpression::class)
                        .prop(AnyTagExpression::tags).containsOnly("E", "F")
                }
        }
    }
})
