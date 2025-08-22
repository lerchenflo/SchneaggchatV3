package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializer that accepts:
 *  - JSON number (e.g. 342233) -> used as-is (Int)
 *  - JSON string hex e.g. "#217F12", "217F12", "0x217F12" -> parsed as hex Int
 *  - JSON null -> yields null
 *
 * For hex strings of form RRGGBB (6 hex digits) we OR with 0xFF000000 to make an opaque ARGB int.
 */
object ColorIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ColorInt", PrimitiveKind.STRING) // string descriptor is fine for mixed input

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalStateException("ColorIntSerializer can be used only with Json format")
        val element = jsonDecoder.decodeJsonElement()

        return when {
            element is JsonNull -> null
            element is JsonPrimitive && element.isString -> {
                val raw = element.content.trim()
                if (raw.isEmpty()) return null

                // remove possible prefixes
                val hex = when {
                    raw.startsWith("#") -> raw.substring(1)
                    raw.startsWith("0x") || raw.startsWith("0X") -> raw.substring(2)
                    else -> raw
                }

                // try parse hex (support 3,4,6,8-digit if you want; here we handle common 6/8)
                try {
                    val parsed = hex.toLong(16).toInt()
                    // if given RRGGBB (6 digits) add 0xFF alpha
                    if (hex.length == 6) {
                        parsed or (0xFF shl 24)
                    } else {
                        // if already includes alpha (8 digits) or other length, return as-is
                        parsed
                    }
                } catch (e: NumberFormatException) {
                    null
                }
            }
            element is JsonPrimitive && element.isString.not() && element.content.isNotEmpty() -> {
                // numeric literal (e.g. 342233) or boolean-like; try parse int
                try {
                    element.int
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Int?) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("ColorIntSerializer can be used only with Json format")
        if (value == null) {
            jsonEncoder.encodeNull()
        } else {
            // encode as number (you could also encode as hex string if desired)
            jsonEncoder.encodeInt(value)
        }
    }
}
