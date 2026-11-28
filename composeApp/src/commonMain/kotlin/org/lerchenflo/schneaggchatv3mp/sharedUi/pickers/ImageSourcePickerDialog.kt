package org.lerchenflo.schneaggchatv3mp.sharedUi.pickers

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import schneaggchatv3mp.composeapp.generated.resources.unknown_error

/** What the picked image is for, decides crop and multi-select. */
enum class ImagePickPurpose {
    /** Images for a chat message: several at once, no crop. */
    MESSAGE_IMAGES,

    /** A profile, group or event picture: one image, cropped to a circle. */
    ROUND_PICTURE,
}

/**
 * Camera or gallery, for every image pick in the app.
 *
 * The picker library keeps an [ImagePickerResult.Error] (e.g. "camera not available") until
 * `reset()`; the picker is reset on every close, so the next open starts with the two buttons
 * again, and an error is shown above the buttons, so the other source can still be picked.
 *
 * [rememberImagePickerKMP] stays unconditional (not inside `if (visible)`): its state lives across
 * open/close instead of being recreated on every open.
 *
 * [onImagesPicked] tells whether the photos come from the camera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourcePickerDialog(
    visible: Boolean,
    purpose: ImagePickPurpose,
    onImagesPicked: (photos: List<GalleryPhotoResult>, fromCamera: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val picker = rememberImagePickerKMP(config = purpose.pickerConfig())
    val result = picker.result
    var launchedCamera by remember { mutableStateOf(false) }

    val close = {
        picker.reset()
        onDismiss()
    }

    if (!visible) return

    LaunchedEffect(result) {
        when (result) {
            is ImagePickerResult.Success -> {
                onImagesPicked(result.photos, launchedCamera)
                close()
            }
            is ImagePickerResult.Dismissed -> close()
            else -> Unit
        }
    }

    BasicAlertDialog(onDismissRequest = close) {
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
                if (result is ImagePickerResult.Loading) {
                    CircularProgressIndicator()
                    return@Column
                }

                if (result is ImagePickerResult.Error) {
                    Text(
                        text = stringResource(
                            Res.string.image_picker_error,
                            result.exception.message ?: stringResource(Res.string.unknown_error)
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(stringResource(Res.string.choose_image_source))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            launchedCamera = true
                            picker.launchCamera()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.camera))
                    }
                    Button(
                        onClick = {
                            launchedCamera = false
                            picker.launchGallery()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.gallery))
                    }
                }
            }
        }
    }
}

private fun ImagePickPurpose.pickerConfig(): ImagePickerKMPConfig {
    val roundPicture = this == ImagePickPurpose.ROUND_PICTURE
    return ImagePickerKMPConfig(
        cropConfig = if (roundPicture) {
            CropConfig(
                enabled = true,
                aspectRatioLocked = true,
                circularCrop = true,
                squareCrop = false,
                freeformCrop = false
            )
        } else {
            CropConfig(enabled = false)
        },
        galleryConfig = GalleryConfig(
            allowMultiple = !roundPicture,
            mimeTypes = listOf(MimeType.IMAGE_ALL),
            includeExif = !roundPicture,
            // Round pictures: without a compression level the picker hands back the raw gallery
            // uri and loadBytes() re-encodes it without ever applying the EXIF orientation, so
            // rotated photos get cropped sideways; a level makes the picker bake it in first.
            // Message images stay the untouched original (metadata kept for full quality):
            // ChatViewModel reads the file itself and downscaleImage turns it upright.
            compressionLevel = if (roundPicture) CompressionLevel.LOW else null
        )
    )
}
