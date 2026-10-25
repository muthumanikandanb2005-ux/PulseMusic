package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.manikandan.domain.extension.toGenericMediaItem
import com.maxrave.common.Config
import com.maxrave.simpmusic.ui.icon.Album
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.empty_unified_shelf
import simpmusic.composeapp.generated.resources.search_unified_shelf
import simpmusic.composeapp.generated.resources.shelf_filter_albums
import simpmusic.composeapp.generated.resources.shelf_filter_all
import simpmusic.composeapp.generated.resources.shelf_filter_personal_audio
import simpmusic.composeapp.generated.resources.shelf_filter_streaming

/**
 * Unified Music Shelf:
 * Keeps streaming songs, purchased/saved albums, and personal local audio files
 * together in one searchable, filterable library view.
 */
@Composable
fun UnifiedShelfView(
    navController: NavController,
    viewModel: LibraryViewModel,
    sharedViewModel: SharedViewModel = koinInject(),
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewModel.getUnifiedShelfData()
    }

    val items by viewModel.filteredUnifiedItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.unifiedQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.unifiedFilter.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val lavenderPrimary = Color(0xFFA78BFA)
    val lavenderSoft = Color(0xFFC4B5FD)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Shelf Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setUnifiedQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = stringResource(Res.string.search_unified_shelf),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = SimpIcons.Search,
                    contentDescription = null,
                    tint = lavenderSoft,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setUnifiedQuery("") }) {
                        Icon(
                            imageVector = SimpIcons.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = lavenderPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        // Filter Pills Row
        val chipScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShelfFilterChip(
                label = stringResource(Res.string.shelf_filter_all),
                icon = SimpIcons.QueueMusic,
                isSelected = currentFilter == UnifiedShelfFilter.ALL,
                onClick = { viewModel.setUnifiedFilter(UnifiedShelfFilter.ALL) },
            )
            ShelfFilterChip(
                label = stringResource(Res.string.shelf_filter_streaming),
                icon = SimpIcons.Search,
                isSelected = currentFilter == UnifiedShelfFilter.STREAMING,
                onClick = { viewModel.setUnifiedFilter(UnifiedShelfFilter.STREAMING) },
            )
            ShelfFilterChip(
                label = stringResource(Res.string.shelf_filter_albums),
                icon = SimpIcons.Album,
                isSelected = currentFilter == UnifiedShelfFilter.ALBUMS,
                onClick = { viewModel.setUnifiedFilter(UnifiedShelfFilter.ALBUMS) },
            )
            ShelfFilterChip(
                label = stringResource(Res.string.shelf_filter_personal_audio),
                icon = SimpIcons.DownloadForOffline,
                isSelected = currentFilter == UnifiedShelfFilter.PERSONAL_AUDIO,
                onClick = { viewModel.setUnifiedFilter(UnifiedShelfFilter.PERSONAL_AUDIO) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Unified List
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.empty_unified_shelf),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + 80.dp,
                    top = 4.dp,
                ),
            ) {
                items(
                    items = items,
                    key = { "${it.filterType.name}_${it.id}" },
                ) { shelfItem ->
                    UnifiedShelfRow(
                        item = shelfItem,
                        onClick = {
                            when (shelfItem) {
                                is UnifiedShelfItem.StreamingSong -> {
                                    sharedViewModel.loadMediaItem(
                                        shelfItem.song.toGenericMediaItem(),
                                        Config.SONG_CLICK,
                                    )
                                }
                                is UnifiedShelfItem.PersonalAudio -> {
                                    sharedViewModel.loadMediaItem(
                                        shelfItem.song.toGenericMediaItem(),
                                        Config.SONG_CLICK,
                                    )
                                }
                                is UnifiedShelfItem.Album -> {
                                    navController.navigate(AlbumDestination(shelfItem.album.browseId))
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val lavenderPrimary = Color(0xFFA78BFA)
    val bgColor by animateColorAsState(
        if (isSelected) lavenderPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    )
    val contentColor by animateColorAsState(
        if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = typo().labelMedium.copy(fontSize = 12.sp),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun UnifiedShelfRow(
    item: UnifiedShelfItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = when (item) {
                        is UnifiedShelfItem.Album -> SimpIcons.Album
                        is UnifiedShelfItem.PersonalAudio -> SimpIcons.DownloadForOffline
                        is UnifiedShelfItem.StreamingSong -> SimpIcons.QueueMusic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typo().bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                style = typo().bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Type Badge
        val badgeText: String
        val badgeBg: Color
        val badgeTextCol: Color
        when (item) {
            is UnifiedShelfItem.StreamingSong -> {
                badgeText = "Streaming"
                badgeBg = Color(0xFFA78BFA).copy(alpha = 0.18f)
                badgeTextCol = Color(0xFFC4B5FD)
            }
            is UnifiedShelfItem.Album -> {
                badgeText = "Album"
                badgeBg = Color(0xFF818CF8).copy(alpha = 0.18f)
                badgeTextCol = Color(0xFFA5B4FC)
            }
            is UnifiedShelfItem.PersonalAudio -> {
                badgeText = "Personal"
                badgeBg = Color(0xFF10B981).copy(alpha = 0.18f)
                badgeTextCol = Color(0xFF34D399)
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeBg,
        ) {
            Text(
                text = badgeText,
                style = typo().labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = badgeTextCol,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
