package com.apollosw.omadaimages.ui.imagesearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apollosw.omadaimages.R
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoGridViewState
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoSearchViewState
import com.apollosw.omadaimages.ui.imagesearch.state.PhotoViewData
import com.apollosw.omadaimages.ui.imagesearch.state.PopupViewState
import com.apollosw.omadaimages.ui.theme.OmadaImagesTheme
import com.apollosw.omadaimages.ui.theme.lightGrey

@Composable
fun ImageSearchScreen(viewModel: ImageSearchViewModel = viewModel()) {
    val photoGridViewState by viewModel.photoGridViewState.collectAsState()
    val popupState by viewModel.popupViewState.collectAsState()
    val photoSearchViewState by viewModel.photoSearchViewState.collectAsState()

    ImageSearchContent(
        photoSearchViewState = photoSearchViewState,
        photoGridViewState = photoGridViewState,
        popupState = popupState,
        onSearchClick = viewModel::onSearchClick,
        onScrollChange = viewModel::onScrollVisibleItemChanged,
        onPhotoClick = viewModel::onPhotoClick,
        onPopupDismiss = viewModel::onPopupDismiss
    )
}

@Composable
fun ImageSearchContent(
    photoSearchViewState: PhotoSearchViewState,
    popupState: PopupViewState,
    photoGridViewState: PhotoGridViewState,
    onSearchClick: (String) -> Unit = {},
    onScrollChange: (Int) -> Unit = {},
    onPhotoClick: (PhotoViewData) -> Unit = {},
    onPopupDismiss: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                ImageSearchBar(
                    photoSearchViewState = photoSearchViewState,
                    onSearchClick = onSearchClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp, top = 8.dp)
            )
            ImageGrid(
                photoGridViewState = photoGridViewState,
                onScrollChange = onScrollChange,
                onPhotoClick = onPhotoClick
            )
        }

        if (popupState is PopupViewState.Open) {
            AnimatedVisibility(visible = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(lightGrey)
                )
                Popup(
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnClickOutside = true,
                        dismissOnBackPress = true
                    ),
                    alignment = Alignment.Center,
                    onDismissRequest = { onPopupDismiss() }
                ) {
                    ExpandedPhoto(
                        photoViewData = popupState.photoViewData
                    )
                }
            }
        }
    }
}

@Composable
fun ImageSearchBar(
    photoSearchViewState: PhotoSearchViewState,
    modifier: Modifier = Modifier,
    onSearchClick: (String) -> Unit = { _ -> },
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(id = R.string.screen_search_bar_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            enabled = when (photoSearchViewState) {
                PhotoSearchViewState.NotSearching -> true
                PhotoSearchViewState.Searching -> false
            },
            onClick = { onSearchClick(text) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(
                    id = R.string.content_description_search_button_icon
                )
            )
            Text(text = stringResource(id = R.string.screen_search_button_label))
        }
    }
}

@Composable
fun ImageGrid(
    photoGridViewState: PhotoGridViewState,
    onScrollChange: (Int) -> Unit = { _ -> },
    onPhotoClick: (PhotoViewData) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (photoGridViewState) {
            PhotoGridViewState.Empty -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = stringResource(
                            id = R.string.content_description_empty_image
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(128.dp)
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.screen_empty_text),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            PhotoGridViewState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(
                            id = R.string.content_description_error_image
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(128.dp)
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.screen_error_text),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            is PhotoGridViewState.Loaded -> {
                val gridState = rememberLazyGridState()
                LaunchedEffect(gridState) {
                    snapshotFlow { gridState.firstVisibleItemIndex }
                        .collect {
                            onScrollChange(it)
                        }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(count = 3),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    items(photoGridViewState.photos, { it.thumbnailURL }) { photoInfo ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoInfo.thumbnailURL)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.ic_launcher_background),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize()
                                .clickable {
                                    onPhotoClick(photoInfo)
                                }
                                .padding(2.dp)
                        )
                    }
                }
            }

            PhotoGridViewState.Loading -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 8.dp,
                        modifier = Modifier
                            .size(128.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.screen_searching_text),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandedPhoto(
    photoViewData: PhotoViewData
) {
    ConstraintLayout(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        val (title, image, card) = createRefs()
        Text(
            photoViewData.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(image.top)
                }
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoViewData.popupURL)
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.ic_launcher_background),
            contentScale = ContentScale.Fit,
            contentDescription = stringResource(
                id = R.string.content_description_popup_photo,
                photoViewData.popupURL
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .constrainAs(image) {
                    bottom.linkTo(card.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(title.bottom)
                }
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 128.dp)
                .constrainAs(card) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(image.bottom)
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text(
                    stringResource(
                    id = R.string.screen_popup_details_description,
                    photoViewData.description
                    )
                )
                Text(
                    stringResource(
                        id = R.string.screen_popup_details_date_taken,
                        photoViewData.dateTaken
                    )
                )
                Text(
                    stringResource(
                        id = R.string.screen_popup_details_owner,
                        photoViewData.owner
                    )
                )
                Text(
                    stringResource(
                        id = R.string.screen_popup_details_original_format,
                        photoViewData.originalFormat
                    )
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewImageSearchContent() {
    OmadaImagesTheme {
        ImageSearchContent(
            photoSearchViewState = PhotoSearchViewState.NotSearching,
            popupState = PopupViewState.Closed,
            photoGridViewState = PhotoGridViewState.Loaded(
                listOf(
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    )
                )
            )
        )
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewImageSearchContentEmpty() {
    OmadaImagesTheme {
        ImageSearchContent(
            photoSearchViewState = PhotoSearchViewState.NotSearching,
            popupState = PopupViewState.Closed,
            photoGridViewState = PhotoGridViewState.Empty
        )
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewImageSearchBar() {
    OmadaImagesTheme {
        ImageSearchBar(photoSearchViewState = PhotoSearchViewState.NotSearching)
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewImageSearchBarisSearching() {
    OmadaImagesTheme {
        ImageSearchBar(photoSearchViewState = PhotoSearchViewState.Searching)
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewImageGrid() {
    OmadaImagesTheme {
        ImageGrid(
            photoGridViewState = PhotoGridViewState.Loaded(
                listOf(
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    ),
                    PhotoViewData(
                        title = "Test",
                        description = "This is a test image",
                        owner = "Sally Smith",
                        dateTaken = "8/7/23",
                        originalFormat = "JPEG",
                        thumbnailURL = "ASDF",
                        popupURL = "ASDF"
                    )
                )
            )
        )
    }
}