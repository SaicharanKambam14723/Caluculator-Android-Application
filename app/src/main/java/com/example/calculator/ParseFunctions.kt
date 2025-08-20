package com.example.calculator

import kotlin.math.pow

fun parseExpression(tokens: MutableList<String>): Double {
    return parseAddOrSubtract(tokens)
}

fun parseFactor(tokens: MutableList<String>): Double {
    if(tokens.isEmpty()) throw Exception("Unexpected end of expression")

    val token = tokens.removeAt(0)

    return when {
        token == "(" -> {
            val value = parseExpression(tokens)
            if(tokens.isEmpty() || tokens.removeAt(0) != ")") {
                throw Exception("Missing closing parenthesis")
            }
            value
        }
        token == "-" -> -parseFactor(tokens)
        token == "+" -> parseFactor(tokens)
        else -> token.toDoubleOrNull() ?: throw Exception("Invalid number: $token")
    }
}

fun parsePower(tokens: MutableList<String>): Double {
    var result = parseFactor(tokens)

    while(tokens.isNotEmpty() && tokens[0] == "**") {
        tokens.removeAt(0)
        val right = parseFactor(tokens)
        result = result.pow(result)
    }

    return result
}

fun parseMultiplyOrDivide(tokens: MutableList<String>): Double {
    var result = parsePower(tokens)
    while(tokens.isNotEmpty() && (tokens[0] == "*" || tokens[0] == "/")) {
        val oper = tokens.removeAt(0)
        val right = parsePower(tokens)
        result = if(oper == "*") {
            result * right
        } else {
            if(right == 0.0) throw Exception("Division by zero")
            result / right
        }
    }
    return result
}

fun parseAddOrSubtract(tokens: MutableList<String>): Double {
    var result = parseMultiplyOrDivide(tokens)

    while(tokens.isNotEmpty() && (tokens[0] == "+" || tokens[0] == "-")) {
        val oper = tokens.removeAt(0)
        val right = parseMultiplyOrDivide(tokens)
        result = if(oper == "+") {
            result + right
        } else {
            result - right
        }
    }
    return result
}