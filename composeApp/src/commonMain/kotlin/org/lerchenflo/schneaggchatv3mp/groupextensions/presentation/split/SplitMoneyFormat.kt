package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

/** Currency of a split container without settings (e.g. created by an older client). */
const val DEFAULT_SPLIT_CURRENCY = "EUR"

/** Highest amount one expense may have, mirrors the server limit. */
const val MAX_SPLIT_AMOUNT_CENTS = 100_000_000L
