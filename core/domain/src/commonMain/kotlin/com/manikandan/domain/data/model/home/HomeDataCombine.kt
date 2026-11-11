package com.manikandan.domain.data.model.home

import com.manikandan.domain.data.model.home.chart.Chart
import com.manikandan.domain.data.model.mood.Mood
import com.manikandan.domain.utils.Resource

data class HomeDataCombine(
    val home: Resource<Pair<String?, List<HomeItem>>>,
    val mood: Resource<Mood>,
    val chart: Resource<Chart>,
    val newRelease: Resource<List<HomeItem>>,
)