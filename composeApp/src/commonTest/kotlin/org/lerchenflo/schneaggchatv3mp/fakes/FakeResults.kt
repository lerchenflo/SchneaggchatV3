package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.app.domain.DataError
import org.lerchenflo.schneaggchatv3mp.app.domain.DataResult
import org.lerchenflo.schneaggchatv3mp.app.domain.EmptyDataResult

/** What a fake write answers. [DataError.Unknown] stands for any failure. */
fun Boolean.toFakeResult(): EmptyDataResult<DataError> =
    if (this) DataResult.Success(Unit) else DataResult.Error(DataError.Unknown())

/** What a fake call with a value answers. Null means it failed. */
fun <T : Any> T?.toFakeDataResult(): DataResult<T, DataError> =
    if (this != null) DataResult.Success(this) else DataResult.Error(DataError.Unknown())
