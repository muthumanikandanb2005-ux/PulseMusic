package com.pulse.music.viewModel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.manikandan.domain.repository.SongRepository
import com.pulse.music.pagination.RecentPagingSource
import com.pulse.music.viewModel.base.BaseViewModel

class RecentlySongsViewModel(
    private val songRepository: SongRepository,
) : BaseViewModel() {
    val recentlySongs =
        Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20,
            ),
        ) {
            RecentPagingSource(songRepository)
        }.flow.cachedIn(viewModelScope)
}