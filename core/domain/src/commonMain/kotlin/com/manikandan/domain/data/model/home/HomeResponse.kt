package com.manikandan.domain.data.model.home

import com.manikandan.domain.data.model.home.chart.Chart
import com.manikandan.domain.data.model.mood.Mood
import com.manikandan.domain.utils.Resource

data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)