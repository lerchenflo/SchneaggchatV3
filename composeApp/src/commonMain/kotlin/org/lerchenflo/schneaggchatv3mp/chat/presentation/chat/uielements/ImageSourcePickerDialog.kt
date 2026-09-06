package org.lerchenflo.schneaggchatv3mp.chat.presentation.chat.uielements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.config.CropConfig
import io.github.ismoy.imagepickerkmp.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.picker.CompressionLevel
import io.github.ismoy.imagepickerkmp.picker.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.picker.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerResult
import io.github.ismoy.imagepickerkmp.picker.MimeType
import io.github.ismoy.imagepickerkmp.picker.rememberImagePickerKMP
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.camera
import schneaggchatv3mp.composeapp.generated.resources.choose_image_source
import schneaggchatv3mp.composeapp.generated.resources.gallery
import schneaggchatv3mp.composeapp.generated.resources.image_picker_error

/**
 * Camera/gallery source picker for attaching images to a message.
 *
 * [rememberImagePickerKMP] is deliberately kept unconditional (not inside `if (visible)`) -
 * gating only the dialog itself on [visible] keeps the picker's own state alive across
 * open/close instead of recreating it on every open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourcePickerDialog(
    visible: Boolean,
    onImagesSelected: (List<GalleryPhotoResult>) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker = rememberImagePickerKMP(
        config = ImagePickerKMPConfig(
            cropConfig = CropConfig(enabled = false),
            galleryConfig = GalleryConfig(
                allowMultiple = true,
                mimeTypes = listOf(MimeType.IMAGE_ALL),
                includeExif = true,
                // Without a compression level the picker hands back the raw gallery uri and
                // loadBytes() re-encodes it without ever applying the EXIF orientation, so
                // rotated photos arrive sideways. Setting one makes the picker bake the
                // orientation into the pixels first.
                compressionLevel = CompressionLevel.LOW
            )
        )
    )
    val result = picker.result

    if (visible) {

        // Handle side effects safely
        LaunchedEffect(result) {
            when (result) {
                is ImagePickerResult.Success -> {
                    onImagesSelected(result.photos)
                    picker.reset()
                    onDismiss()
                }

                is ImagePickerResult.Dismissed -> {
                    picker.reset()
                    onDismiss()
                }

                else -> Unit
            }
        }

        BasicAlertDialog(
            onDismissRequest = onDismiss
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    when (result) {

                        is ImagePickerResult.Loading -> {
                            CircularProgressIndicator()
                        }

                        is ImagePickerResult.Error -> {
                            Text(
                                text = stringResource(Res.string.image_picker_error, result.exception.message ?: "Unknown error"),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is ImagePickerResult.Idle -> {

                            Text(
                                stringResource(Res.string.choose_image_source)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        picker.launchCamera()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.camera))
                                }

                                Button(
                                    onClick = {
                                        picker.launchGallery()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.gallery))
                                }
                            }
                        }

                        is ImagePickerResult.Success,
                        is ImagePickerResult.Dismissed -> {
                            // handled in LaunchedEffect
                        }
                    }
                }
            }
        }
    }
}
