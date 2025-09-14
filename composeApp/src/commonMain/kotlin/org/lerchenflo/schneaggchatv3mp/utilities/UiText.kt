package org.lerchenflo.schneaggchatv3mp.utilities

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Klasse zum String-Ressourcen überall verwenden

sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    data class StringResourceText(
        val resId: StringResource,
        val args: Array<Any> = emptyArray()
    ) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringResourceText) return false
            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = resId.hashCode()
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResourceText -> stringResource(resId, *args)
        }
    }
}