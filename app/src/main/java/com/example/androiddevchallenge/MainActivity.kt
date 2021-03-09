/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.example.androiddevchallenge.ui.theme.MyTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

data class CounterState(
    val count: Int = 0,
    val currentTime: Long = 0L,
    val isTimesUp: Boolean = false,
    val time: Long = 0L,
    val hour: Int = 0,
    val min: Int = 0,
    val sec: Int = 0,
    val selectedActionId: Int? = null,
    val isTimerVisible: Boolean = false,
    val timeToCount: Long = 0L
) : MavericksState {
    val formattedTimeLeft: String = formatToDigitalClock(ceil(currentTime.toDouble()).toLong())
    val timeAsMillis: Long = (hour * 3600 + min * 60 + sec) * 1000L
}

class CounterViewModel(initialState: CounterState) :
    MavericksViewModel<CounterState>(initialState) {
    lateinit var timer: CountDownTimer
    fun updateTimer(time: Long) = setState { copy(currentTime = time) }
    fun setHour(hour: Int) {
        setState { copy(hour = hour) }
    }
    fun setMin(min: Int) {
        setState { copy(min = min) }
    }
    fun setSec(sec: Int) {
        setState { copy(sec = sec) }
    }
    fun isTimesUp(value: Boolean) = setState { copy(isTimesUp = value) }
    fun setTimerVisibility(isVisible: Boolean) = setState { copy(isTimerVisible = isVisible) }
    fun selectAction(actionId: Int?) = setState { copy(selectedActionId = actionId) }
    fun setTimer(value: Long) = setState { copy(time = value) }.apply {
        if (this@CounterViewModel::timer.isInitialized) {
            timer.cancel()
        }
        timer = object : CountDownTimer(value, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimer((ceil((millisUntilFinished / 1000.0)) * 1000).toLong())
            }

            override fun onFinish() {
                isTimesUp(true)
                updateTimer(0L)
            }
        }.start()
    }

    fun clearValues() = setState { copy(currentTime = 0L, time = 0L) }

    @SuppressLint("MissingSuperCall")
    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }
}

class MainActivity : AppCompatActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                MyApp()
            }
        }
    }
}

val HOUR_LIST = (-1..13).toList()
val MINUTE_LIST = (-1..60).toList()
val SECOND_LIST = (-1..60).toList()

val actions = listOf(
    ActionData(
        id = 0,
        title = "Brush teeth",
        time = Time(hour = 0, min = 2, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 1,
        title = "Face mask",
        time = Time(hour = 0, min = 15, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 2,
        title = "Steam eggs",
        time = Time(hour = 0, min = 10, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 3,
        title = "Pomodoro",
        time = Time(hour = 0, min = 25, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 4,
        title = "Hand washing",
        time = Time(hour = 0, min = 0, sec = 20),
        type = ActionType.TIME
    ),
    ActionData(
        id = 5,
        title = "Homework",
        time = Time(hour = 0, min = 35, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 6,
        title = "Exercise",
        time = Time(hour = 0, min = 20, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 7,
        title = "Cycling",
        time = Time(hour = 1, min = 15, sec = 0),
        type = ActionType.TIME
    ),
    ActionData(
        id = 8,
        title = null,
        time = null,
        type = ActionType.NEW
    )
)

fun findActionById(id: Int): ActionData? {
    actions.forEach {
        if (it.id == id)
            return it
    }
    return null
}

data class Time(
    val hour: Int,
    val min: Int,
    val sec: Int
) {
    override fun toString(): String {
        return "${formatTime(hour)}:${formatTime(min)}:${formatTime(sec)}"
    }
}

data class ActionData(
    val id: Int,
    val title: String?,
    val time: Time?,
    val type: ActionType
)

enum class ActionType {
    TIME,
    NEW
}

// Start building your app here!
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyApp() {
    val viewModel: CounterViewModel = mavericksViewModel()
    val currentTime by viewModel.collectAsState(CounterState::currentTime)
    val isTimerVisible by viewModel.collectAsState(CounterState::isTimerVisible)
    val timeAsMillis by viewModel.collectAsState(CounterState::timeAsMillis)
    val formattedTimeLeft by viewModel.collectAsState(CounterState::formattedTimeLeft)
    val time by viewModel.collectAsState(CounterState::time)
    val selectedActionId by viewModel.collectAsState(CounterState::selectedActionId)

    Scaffold(
        content = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(
                                top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp
                            ),
                        tint = colorResource(id = R.color.red)
                    )
                    Crossfade(targetState = isTimerVisible) {
                        if (it) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(all = 32.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(300.dp),
                                        strokeWidth = 3.dp,
                                        color = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
                                        progress = 1f,
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(300.dp),
                                        strokeWidth = 3.dp,
                                        color = colorResource(id = R.color.red),
                                        progress = 1f - ((currentTime) * 100 / (time)) / 100F,
                                    )
                                    Column(
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Text(
                                            text = formattedTimeLeft,
                                            fontSize = 48.sp,
                                            modifier = Modifier
                                                .wrapContentHeight()
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.ExtraLight,
                                        )
                                        if (selectedActionId != null) {
                                            Box(
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text(
                                                    text = findActionById(selectedActionId!!)?.title ?: "",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center,
                                                    style = TextStyle(
                                                        color = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                ) {
                                    VerticalList(HOUR_LIST, viewModel, LIST.HOURS)
                                    SuffixText("hours")
                                    Spacer(modifier = Modifier.width(16.dp))
                                    VerticalList(MINUTE_LIST, viewModel, LIST.MINS)
                                    SuffixText("min")
                                    Spacer(modifier = Modifier.width(16.dp))
                                    VerticalList(SECOND_LIST, viewModel, LIST.SECS)
                                    SuffixText("sec")
                                }
                                ActionsGridList(viewModel)
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier
                        .width(200.dp)
                        .height(64.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    onClick = {
                        viewModel.setTimerVisibility(isVisible = !isTimerVisible)
                        if (!isTimerVisible)
                            viewModel.setTimer(timeAsMillis)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = colorResource(id = R.color.red),
                        contentColor = colorResource(id = R.color.white),
                    ),
                    shape = RoundedCornerShape(32),
                ) {
                    Text(text = if (!isTimerVisible) "Start" else "Stop")
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionsGridList(viewModel: CounterViewModel) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(3),
        content = {
            items(
                actions,
                itemContent = { item ->
                    when (item.type) {
                        ActionType.TIME -> {
                            ActionCircularButtonWithText(
                                actionData = item,
                                viewModel = viewModel
                            )
                        }
                        ActionType.NEW -> {
                            ActionCircularButtonWithIcon()
                        }
                    }
                }
            )
        },
        contentPadding = PaddingValues(all = 12.dp),
    )
}

@Composable
fun ActionCircularButtonWithIcon() {
    Box(
        modifier = Modifier.padding(all = 8.dp)
    ) {
        Card(
            backgroundColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(
                        onClick = {
                        },
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add new action")
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCircularButtonWithText(actionData: ActionData, viewModel: CounterViewModel) {
    val selectedActionId by viewModel.collectAsState(CounterState::selectedActionId)

    val backgroundColor by animateColorAsState(
        targetValue = if (selectedActionId == actionData.id) colorResource(id = R.color.dark_red) else (if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray),
        animationSpec = tween(durationMillis = 250)
    )

    val textColor by animateColorAsState(
        targetValue = if (selectedActionId == actionData.id) colorResource(id = R.color.red) else Color.Unspecified,
        animationSpec = tween(durationMillis = 250)
    )

    Box(
        modifier = Modifier.padding(all = 8.dp)
    ) {
        Card(
            backgroundColor = backgroundColor,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable(
                        onClick = {
                            viewModel.apply {
                                selectAction(actionData.id)
                                setHour(actionData.time?.hour ?: 0)
                                setMin(actionData.time?.min ?: 0)
                                setSec(actionData.time?.sec ?: 0)
                            }
                        },
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = actionData.title ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Light,
                        style = TextStyle(
                            color = textColor
                        )
                    )
                    Box(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = actionData.time.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraLight,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                color = textColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuffixText(text: String) {
    RowScope.apply {
        Box(
            modifier = Modifier
                .padding(top = 26.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Start,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun VerticalList(list: List<Int>, viewModel: CounterViewModel, listType: LIST) {
    val hour by viewModel.collectAsState(CounterState::hour)
    val min by viewModel.collectAsState(CounterState::min)
    val sec by viewModel.collectAsState(CounterState::sec)
    val selectedAction by viewModel.collectAsState(CounterState::selectedActionId)

    val listState = rememberLazyListState(
        when (listType) {
            LIST.HOURS -> hour
            LIST.MINS -> min
            LIST.SECS -> sec
        },
        0
    )
    val coroutineScope = rememberCoroutineScope()

    val isScrollInProgress = remember { derivedStateOf { listState.isScrollInProgress } }
    val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset =
        remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    val density = LocalDensity.current.density
    val width = remember { mutableStateOf(0f) }
    val height = remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .width(74.dp)
            .height(220.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .scrollable(
                    state = rememberScrollableState(
                        consumeScrollDelta = { delta ->
                            viewModel.selectAction(null)
                            delta
                        }
                    ),
                    orientation = Orientation.Vertical
                )
                .onGloballyPositioned {
                    width.value = it.size.width / density
                    height.value = it.size.height / density
                }

        ) {
            itemsIndexed(items = list) { index, item ->
                when (index) {
                    0 -> {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        )
                    }
                    list.size - 1 -> {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(86.dp)
                        )
                    }
                    else -> {
                        if (firstVisibleItemIndex.value == item && selectedAction == null) {
                            when (listType) {
                                LIST.HOURS -> {
                                    if (hour != item) viewModel.setHour(item)
                                }
                                LIST.MINS -> {
                                    if (min != item) viewModel.setMin(item)
                                }
                                LIST.SECS -> {
                                    if (sec != item) viewModel.setSec(item)
                                }
                            }
                        }
                        Text(
                            text = formatTime(value = item),
                            fontSize = 54.sp,
                            modifier = Modifier
                                .wrapContentHeight()
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.ExtraLight,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .size(width.value.dp, height.value.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colors.surface,
                            MaterialTheme.colors.surface,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colors.surface,
                            MaterialTheme.colors.surface,
                        )
                    )
                )
        )
    }

    if (selectedAction != null) {
        coroutineScope.launch {
            when (listType) {
                LIST.HOURS -> listState.animateScrollToItem(hour)
                LIST.MINS -> listState.animateScrollToItem(min)
                LIST.SECS -> listState.animateScrollToItem(sec)
            }
        }
    }

    if (!isScrollInProgress.value) {
        coroutineScope.launch {
            if (firstVisibleItemScrollOffset.value < 85 / 2) {
                if (listState.layoutInfo.totalItemsCount == 0)
                    return@launch
                listState.animateScrollToItem(index = firstVisibleItemIndex.value)
            } else {
                listState.animateScrollToItem(index = firstVisibleItemIndex.value + 1)
            }
        }
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        MyApp()
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        MyApp()
    }
}

enum class LIST {
    HOURS,
    MINS,
    SECS
}

fun formatTime(value: Int): String =
    if (value in (0..9))
        String.format("%02d", value)
    else
        String.format("%2d", value)

fun formatToDigitalClock(millis: Long): String {
    return String.format(
        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
        TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                millis
            )
        ),
        TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                millis
            )
        )
    )
}
/*
fun Modifier.gradientTint(
    colors: List<Color>,
    blendMode: BlendMode,
    brushProvider: (List<Color>, Size) -> LinearGradient
) = composed {
    var size by remember { mutableStateOf(Size.Zero) }
    val gradient = remember(colors, size) { brushProvider(colors, size) }
    drawWithContent {
        drawContent()
        size = this.size
        drawRect(
            brush = gradient,
            blendMode = blendMode
        )
    }
}*/
