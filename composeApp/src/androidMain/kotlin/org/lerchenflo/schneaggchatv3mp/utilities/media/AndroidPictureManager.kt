package org.lerchenflo.schneaggchatv3mp.utilities.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeBase64
import org.lerchenflo.schneaggchatv3mp.GROUPPROFILEPICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.PICTURE_FILE_NAME
import org.lerchenflo.schneaggchatv3mp.STORED_PICTURE_FILE_ENDINGS
import org.lerchenflo.schneaggchatv3mp.USERPROFILEPICTURE_FILE_NAME
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class AndroidPictureManager(private val context: Context) : PictureManager {

    override suspend fun savePictureToStorage(base64Pic: String, filename: String): String =
        withContext(Dispatchers.IO) {
            Log.d("ANDROID-Picturesave", "Saving: $filename")

            // ✅ Correct Base64 decoding
            val bytes = base64Pic.decodeBase64()?.toByteArray()
                ?: throw IllegalArgumentException("Invalid Base64 string")

            savePictureBytes(bytes, filename)
        }

    override suspend fun savePictureToStorage(data: ByteArray, filename: String): String =
        withContext(Dispatchers.IO) {
            savePictureBytes(data, filename)
        }

    private fun savePictureBytes(data: ByteArray, filename: String): String {
        val file = File(context.filesDir, filename)
        file.parentFile?.mkdirs()
        file.outputStream().use { it.write(data) }
        return file.absolutePath
    }

    override suspend fun loadPictureFromStorage(filename: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                Log.d("Android file load", "File not found: $filename")
                return@withContext null
            }

            try {
                file.readBytes()
            } catch (e: Exception) {
                Log.e("Android file load", "Error loading picture: $filename", e)
                null
            }
        }

    override suspend fun deletePicture(filename: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return@withContext false
            file.delete()
        }

    override suspend fun deleteAllStoredPictures(): Int =
        withContext(Dispatchers.IO) {
            context.filesDir
                .listFiles { file -> file.isFile && STORED_PICTURE_FILE_ENDINGS.any { file.name.endsWith(it) } }
                .orEmpty()
                .count { it.delete() }
        }

    override fun getProfilePicFilePath(id: String, gruppe: Boolean) : String {

        val filename = id + if (gruppe) GROUPPROFILEPICTURE_FILE_NAME else USERPROFILEPICTURE_FILE_NAME

        return File(context.filesDir, filename).absolutePath
    }

    override fun getImageMessageFilePath(messageId: String): String {
        val filename = messageId + PICTURE_FILE_NAME
        return File(context.filesDir, filename).absolutePath
    }

    override fun checkImageExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    override suspend fun downscaleImage(
        imageBytes: ByteArray,
        targetSizeBytes: Int,
    ): ByteArray = withContext(Dispatchers.IO) {
        val orientation = readExifOrientation(imageBytes)

        // A rotated image has to be re-encoded even when it is small enough already: the
        // orientation is only an EXIF tag, and every later step (upload, server PNG conversion)
        // drops it, leaving the picture sideways. HEIC / WebP are re-encoded too: the server only
        // takes JPEG, PNG and GIF.
        if (imageBytes.size <= targetSizeBytes && orientation == ExifInterface.ORIENTATION_NORMAL && isUploadableImage(imageBytes)) {
            Log.d("Android downscale", "Image already under target (${imageBytes.size} <= $targetSizeBytes), returning original")
            return@withContext imageBytes
        }

        var bitmap = decodeSampledBitmap(imageBytes)
            ?.let { applyExifOrientation(it, orientation) }
            ?: throw IllegalArgumentException("Invalid image data")

        var quality = 90
        var resultBytes: ByteArray

        // First try with original size at reduced quality
        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            resultBytes = outputStream.toByteArray()

            if (resultBytes.size <= targetSizeBytes) {
                break
            }
            quality -= 10
        } while (quality > 10)

        // If still too large, resize the image
        if (resultBytes.size > targetSizeBytes) {
            var scale = 0.9f
            while (scale > 0.1f) {
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()

                val resizedBitmap = bitmap.scale(newWidth, newHeight)

                // Reset quality for resized bitmap
                quality = 90

                // Try compression again with resized image
                do {
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    resultBytes = outputStream.toByteArray()

                    if (resultBytes.size <= targetSizeBytes) {
                        if (resizedBitmap != bitmap) {
                            bitmap.recycle()
                        }
                        bitmap = resizedBitmap
                        break
                    }
                    quality -= 10
                } while (quality > 10)

                if (resultBytes.size <= targetSizeBytes) {
                    break
                }

                if (resizedBitmap != bitmap) {
                    resizedBitmap.recycle()
                }
                scale -= 0.1f
            }
        }

        // Clean up bitmap
        bitmap.recycle()

        Log.d("Android downscale", "Downscaled image from ${imageBytes.size} to ${resultBytes.size} bytes")
        return@withContext resultBytes
    }

    override suspend fun downloadImage(
        filePath: String,
        fileName: String
    ): String = saveToGallery(filePath, baseName = fileName.substringBeforeLast('.'))

    override suspend fun readPickedImage(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            if (uri.startsWith(CONTENT_URI_SCHEME)) {
                context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() }
            } else {
                File(fileUriToPath(uri)).readBytes()
            }
        }.onFailure { Log.w("Android picked image", "Could not read $uri", it) }.getOrNull()
    }

    override suspend fun encodeJpegKeepingMetadata(imageBytes: ByteArray, maxBytes: Int): ByteArray =
        withContext(Dispatchers.IO) {
            val bitmap = decodeSampledBitmap(imageBytes, FULL_QUALITY_MAX_DECODE_DIMENSION_PX)
                ?: throw IllegalArgumentException("Invalid image data")
            val jpeg = try {
                compressJpegToFit(bitmap, maxBytes)
            } finally {
                bitmap.recycle()
            }
            copyExifInto(jpeg, from = imageBytes)
        }

    /** [jpeg] with the metadata of [from]; ExifInterface only writes files, hence the temp file. */
    private fun copyExifInto(jpeg: ByteArray, from: ByteArray): ByteArray {
        val source = runCatching { ExifInterface(ByteArrayInputStream(from)) }.getOrNull() ?: return jpeg
        val tempFile = File.createTempFile("full_quality", ".jpg", context.cacheDir)
        return try {
            tempFile.writeBytes(jpeg)
            ExifInterface(tempFile).apply {
                COPIED_EXIF_TAGS.forEach { tag -> source.getAttribute(tag)?.let { setAttribute(tag, it) } }
                saveAttributes()
            }
            tempFile.readBytes()
        } catch (e: IOException) {
            Log.w("Android full quality", "Metadata could not be copied, sending without it", e)
            jpeg
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun saveToGallery(filePath: String, baseName: String): String =
        withContext(Dispatchers.IO) {
            val sourceFile = File(filePath)
            if (!sourceFile.exists()) {
                Log.e("PictureManager", "Gallery copy: source file not found: $filePath")
                return@withContext ""
            }
            val header = sourceFile.inputStream().use { input ->
                ByteArray(ImageFileFormat.HEADER_SIZE).also { input.read(it) }
            }
            val format = ImageFileFormat.detect(header) ?: ImageFileFormat.JPEG
            insertIntoGallery(sourceFile, "$baseName.${format.extension}", format.mimeType)
        }

    /**
     * Copies [sourceFile] byte for byte into the album Pictures/Schneaggchat. Own MediaStore
     * entries need no permission (minSdk 29). @return the content uri, "" on failure
     */
    private fun insertIntoGallery(sourceFile: File, displayName: String, mimeType: String): String {
        if (!sourceFile.exists()) {
            Log.e("PictureManager", "Source file not found: ${sourceFile.path}")
            return ""
        }

        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Schneaggchat")
            // Hidden from other apps until the bytes are written
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
            Log.e("PictureManager", "Failed to create MediaStore entry")
            return ""
        }

        return try {
            contentResolver.openOutputStream(imageUri).use { outputStream ->
                if (outputStream == null) throw Exception("Could not open output stream")
                sourceFile.inputStream().use { inputStream -> inputStream.copyTo(outputStream) }
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(imageUri, contentValues, null, null)

            Log.d("PictureManager", "Image exported successfully to Gallery: $imageUri")
            imageUri.toString()
        } catch (e: Exception) {
            Log.e("PictureManager", "Error saving image to the gallery", e)
            contentResolver.delete(imageUri, null, null)
            ""
        }
    }

    override suspend fun encodeImageBitmap(bitmap: ImageBitmap): ByteArray =
        withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.toByteArray()
        }
}

private const val MAX_DECODE_DIMENSION_PX = 2048

private fun decodeSampledBitmap(imageBytes: ByteArray, maxDimension: Int = MAX_DECODE_DIMENSION_PX): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxDimension)
    }
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun readExifOrientation(imageBytes: ByteArray): Int =
    runCatching {
        ExifInterface(ByteArrayInputStream(imageBytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        .also { if (it != bitmap) bitmap.recycle() }
}

private const val CONTENT_URI_SCHEME = "content://"

/** Full quality decodes up to this long side (about 24 MP); bigger photos are sampled down once. */
private const val FULL_QUALITY_MAX_DECODE_DIMENSION_PX = 6000

/** [bitmap] as a JPEG of at most [maxBytes]: lower quality first, then smaller. */
private fun compressJpegToFit(bitmap: Bitmap, maxBytes: Int): ByteArray {
    var current = bitmap
    try {
        repeat(FULL_QUALITY_SCALE_STEPS) {
            FULL_QUALITY_JPEG_QUALITIES.forEach { quality ->
                val jpeg = current.toJpeg(quality)
                if (jpeg.size <= maxBytes) return jpeg
            }
            val smaller = current.scale(
                (current.width * FULL_QUALITY_SCALE_FACTOR).toInt(),
                (current.height * FULL_QUALITY_SCALE_FACTOR).toInt()
            )
            if (current !== bitmap) current.recycle()
            current = smaller
        }
    } finally {
        if (current !== bitmap) current.recycle()
    }
    throw IllegalStateException("Image does not fit into $maxBytes bytes")
}

private fun Bitmap.toJpeg(quality: Int): ByteArray =
    ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()

/** Metadata a re-encoded full-quality image keeps (capture, camera, place); sizes are left out. */
private val COPIED_EXIF_TAGS = listOf(
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
)
