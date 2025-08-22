package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.calculator.ui.theme.CalculatorTheme
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Calculator()
                }
            }
        }
    }
}

class CaluculatorHistory(
    val expression: String,
    val result: String
)


@OptIn(ExperimentalAnimationApi :: class, ExperimentalFoundationApi::class)
@Composable
fun Calculator(modifier: Modifier = Modifier) {

    var currExpression by remember { mutableStateOf("") }
    var displayValue by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf(listOf<CaluculatorHistory>()) }
    var showHistory by remember { mutableStateOf(false) }
    var justComputed by remember { mutableStateOf(false) }


    fun getLastNum(str: String): String {
        val regex = Regex("[+\\-*/^()]")
        val parts = str.split(regex)
        return parts.lastOrNull()?.takeIf { it.isNotEmpty() } ?: ""
    }

    val digitRegex = Regex("\\d")
    val operatorRegex = Regex("[+\\-×÷^]")

    fun input(value: String) {
        if(justComputed && value.matches(digitRegex)) {
            currExpression = value
            displayValue = value
            justComputed = false
        } else if(justComputed && value.matches(operatorRegex)) {
            currExpression = displayValue + value
            justComputed = false
        } else {
            if(currExpression.isEmpty() && value.matches((operatorRegex))) return

            if(currExpression.isNotEmpty() && currExpression.last().toString().matches(operatorRegex) && value.matches(operatorRegex)) {
                currExpression = currExpression.dropLast(1) + value
                return
            }

            currExpression += value
            val lastNum = getLastNum(currExpression)
            displayValue = if(lastNum.isEmpty()) "0" else lastNum
        }
    }

    fun backspace() {
        if(currExpression.isNotEmpty()) {
            currExpression = currExpression.dropLast(1)
            val lastNum = getLastNum(currExpression)
            displayValue = if(lastNum.isEmpty() && currExpression.isNotEmpty()) {
                getLastNum(currExpression.dropLast(1)).takeIf { it.isNotEmpty() } ?: "0"
            } else if(lastNum.isNotEmpty()) {
                lastNum
            } else {
                "0"
            }
        }
        if(currExpression.isEmpty()) {
            displayValue = "0"
        }
        justComputed = false
    }

    fun clear() {
        currExpression = ""
        displayValue = "0"
        justComputed = false
    }

    fun formatResult(value: Double): String {
        return when {
            value.isNaN() -> "Error"
            value.isInfinite() -> if(value > 0) "Infinity" else "-Infinity"
            value == value.toLong().toDouble() && abs(value) < 1e10 -> value.toLong().toString()
            abs(value) >= 1e10 || (abs(value) < 1e-7 && value != 0.0) -> String.format("%.2e", value)
            else -> String.format("%.8g", value)
        }
    }

    fun clearHistory() {
        history = emptyList()
    }

    fun parseExpression(tokens: MutableList<String>): Double {
        return parseAddOrSubtract(tokens)
    }

    fun tokenize(expression: String): MutableList<String> {
        val tokens = mutableListOf<String>()
        var i = 0

        while(i<expression.length) {
            when {
                expression[i].isDigit() || expression[i] == '.' -> {
                    var num = ""
                    while(i<expression.length && (expression[i].isDigit() || expression[i] == '.')) {
                        num += expression[i]
                        i++
                    }
                    tokens.add(num)
                }
                expression.substring(i).startsWith("**") -> {
                    tokens.add("**")
                    i += 2
                }
                expression[i] in "+-*/()" -> {
                    tokens.add(expression[i].toString())
                    i++
                }
                else -> i++
            }
        }
        return tokens
    }

    fun evaluate(expression: String): Double {
        return try {
            val tokens = tokenize(expression)
            parseExpression(tokens)
        } catch(e: Exception) {
            throw Exception("Invalid expression")
        }
    }

    fun handleSplFunctions(expression: String): String {
        var expr = expression

        val sqrtRegex = Regex("√\\(([^)]+)\\)")
        expr = sqrtRegex.replace(expr) { matchResult ->
            val value = evaluate(matchResult.groupValues[1])
            sqrt(value).toString()
        }

        val squareRegex = Regex("([\\d.]+)²")
        expr = squareRegex.replace(expr) { matchResult ->
            val value = matchResult.groupValues[1].toDouble()
            (value * value).toString()
        }

        return expr
    }

    fun evaluateExpression(expression: String): Double {
        var expr = expression
            .replace('×', '*')
            .replace("÷", "/")
            .replace("^", "**")

        expr = handleSplFunctions(expr)

        return evaluate(expr)
    }

    fun caluculate() {
        if(currExpression.isEmpty()) return

        try {
            var result = evaluateExpression(currExpression)
            val formattedResult = formatResult(result)
            history = history + CaluculatorHistory(currExpression, formattedResult)
            displayValue = formattedResult
            currExpression = ""
            justComputed = true
        } catch (e: Exception) {
            displayValue = "Error"
            currExpression = ""
            justComputed = true
        }
    }

    fun applyFun(function: String) {
        val curr = displayValue.toDoubleOrNull() ?: 0.0
        val result = when(function) {
            "sin" -> sin(Math.toRadians(curr))
            "cos" -> cos(Math.toRadians(curr))
            "tan" -> tan(Math.toRadians(curr))
            "ln" -> if(curr > 0) ln(curr) else Double.NaN
            "+/-" -> -curr
            "%" -> curr / 100
            "√" -> if(curr >= 0) sqrt(curr) else Double.NaN
            "x²" -> curr * curr
            else -> throw Exception("Invalid function")
        }

        displayValue = formatResult(result)
        currExpression = displayValue
        justComputed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.LightGray,
                        Color.White,
                        Color.LightGray
                    )
                )
            )
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        Color.LightGray,
                        Color.White,
                    )
                )
            )
            .then(
                if (showHistory) Modifier.blur(8.dp) else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight()
                        .padding(end = 10.dp)
                        .background(
                            Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = Color.DarkGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    ) {
                    Text(
                        text = if(currExpression.length > 25) "...." + currExpression.takeLast(25)  else currExpression,
                        color = Color.DarkGray.copy(alpha = 0.9f),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(
                                start = 15.dp,
                                top = 8.dp
                            )
                    )
                }

                IconButton(
                    onClick = {
                        showHistory = !showHistory
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = Color.DarkGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        painter = if(showHistory) painterResource(id = R.drawable.history_0) else painterResource(id = R.drawable.history_1),
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }



            Spacer(
                modifier = Modifier
                    .height(12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    Color("#f8fafc".toColorInt())
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(26.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = if(displayValue.length > 10) "..." + displayValue.takeLast(10) else displayValue,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalcButton(
                        text = "C",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFEE2E2),
                        textColor = Color(0xFFDC2626)
                    ) { 
                        clear()
                    }

                    CalcButton(
                        text = "⌫",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        backspace()
                    }

                    CalcButton(
                        text = "( )",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        val open = currExpression.count {
                            it == '('
                        }
                        val close = currExpression.count {
                            it == ')'

                        }
                        if(open == close) {
                            input("(")
                        } else {
                            input(")")
                        }
                    }

                    CalcButton(
                        text = "÷",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        input("÷")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "sin",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("sin")
                    }

                    CalcButton(
                        text = "cos",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("cos")
                    }

                    CalcButton(
                        text = "tan",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("tan")
                    }

                    CalcButton(
                        text = "%",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        applyFun("%")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "√",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("√")
                    }

                    CalcButton(
                        text = "x²",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("x²")
                    }

                    CalcButton(
                        text = "ln",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("ln")
                    }

                    CalcButton(
                        text = "pow",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        input("^")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "7",
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 3.dp,
                                color = Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("7")
                    }

                    CalcButton(
                        text = "8",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("8")
                    }

                    CalcButton(
                        text = "9",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("9")
                    }

                    CalcButton(
                        text = "×",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        input("×")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "4",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("4")
                    }

                    CalcButton(
                        text = "5",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("5")
                    }

                    CalcButton(
                        text = "6",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("6")
                    }

                    CalcButton(
                        text = "-",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        input("-")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = "1",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("1")
                    }

                    CalcButton(
                        text = "2",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("2")
                    }

                    CalcButton(
                        text = "3",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("3")
                    }

                    CalcButton(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        input("+")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcButton(
                        text = ".",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input(".")
                    }

                    CalcButton(
                        text = "0",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFFFFFFF),
                        textColor = Color(0xFF1F2937)
                    ) {
                        input("0")
                    }

                    CalcButton(
                        text = "±",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFF3F4F6),
                        textColor = Color(0xFF374151)
                    ) {
                        applyFun("+/-")
                    }

                    CalcButton(
                        text = "=",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF3B82F6),
                        textColor = Color.White
                    ) {
                        caluculate()
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showHistory,
        enter = fadeIn(
            animationSpec = tween(300)
        ),
        exit = fadeOut(
            animationSpec = tween(300)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null
                ) {
                    showHistory = true
                }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(
                initialOffsetY = {-it},
                animationSpec = tween(400)
            ) + fadeIn(
                animationSpec = tween(400)
            ),
            exit = slideOutVertically(
                targetOffsetY = {-it},
                animationSpec = tween(400)
            ) + fadeOut(
                animationSpec = tween(400)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {

                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Caluculation History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W400,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(start = 10.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if(history.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        clearHistory()
                                    }
                                ) {
                                    Text(
                                        text = "Clear",
                                        color = Color(0xFF4285F4),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    showHistory = !showHistory
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color(0xFFF1F5F9),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.DarkGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    painter = if(showHistory) painterResource(id = R.drawable.history_0) else painterResource(id = R.drawable.history_1),
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier
                            .height(10.dp)
                    )
                    if(history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {

                        }
                    }
                    else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            items(history.takeLast(20).reversed()) {item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement()
                                        .clickable {
                                            currExpression = item.expression
                                            displayValue = item.result
                                            justComputed = true
                                            showHistory = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF8F9FA)
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(5.dp)
                                    ) {
                                        Text(
                                            text = item.expression,
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(
                                            modifier = Modifier.height(1.dp)
                                        )
                                        Text(
                                            text = "= ${item.result}",
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if(text.length >= 2) 16.sp else 22.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun CaluculatorPreview() {
    CalculatorTheme {
        Calculator()
    }
}