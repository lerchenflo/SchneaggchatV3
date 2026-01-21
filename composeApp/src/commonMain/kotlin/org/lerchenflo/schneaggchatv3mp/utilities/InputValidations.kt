package org.lerchenflo.schneaggchatv3mp.utilities

import org.jetbrains.compose.resources.getString
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.requirement_digit
import schneaggchatv3mp.composeapp.generated.resources.requirement_forbidden
import schneaggchatv3mp.composeapp.generated.resources.requirement_length
import schneaggchatv3mp.composeapp.generated.resources.requirement_lowercase
import schneaggchatv3mp.composeapp.generated.resources.requirement_special
import schneaggchatv3mp.composeapp.generated.resources.requirement_uppercase

fun isEmailValid(email: String): Boolean {
    // More comprehensive RFC 5322 compliant regex
    val emailRegex = "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\$"

    //println("Validating email: $email")

    // Additional checks beyond regex
    if (email.isEmpty()) {
        //println("Email validation failed: Email is empty")
        return false
    }

    if (email.length > 254) {
        //println("Email validation failed: Email exceeds maximum length of 254 characters")
        return false
    }

    // Check for exactly one @ symbol
    val atCount = email.count { it == '@' }
    if (atCount != 1) {
        //println("Email validation failed: Email must contain exactly one @ symbol (found $atCount)")
        return false
    }

    // Split into local and domain parts
    val parts = email.split('@')
    val localPart = parts[0]
    val domainPart = parts[1]

    // Check local part length (max 64 chars per RFC)
    if (localPart.isEmpty()) {
        //println("Email validation failed: Local part (before @) is empty")
        return false
    }

    if (localPart.length > 64) {
        //println("Email validation failed: Local part exceeds maximum length of 64 characters")
        return false
    }

    // Check domain part
    if (domainPart.isEmpty()) {
        //println("Email validation failed: Domain part (after @) is empty")
        return false
    }

    // Check domain part has at least one dot
    if (!domainPart.contains('.')) {
        //println("Email validation failed: Domain part must contain at least one dot")
        return false
    }

    // Check domain part segments
    val domainSegments = domainPart.split('.')
    for (segment in domainSegments) {
        if (segment.isEmpty()) {
            //println("Email validation failed: Domain contains empty segment")
            return false
        }
        if (segment.length > 63) {
            //println("Email validation failed: Domain segment '$segment' exceeds maximum length of 63 characters")
            return false
        }
    }

    // Final regex check
    if (!email.matches(emailRegex.toRegex())) {
        //println("Email validation failed: Email format doesn't match RFC 5322 standard")
        return false
    }

    //println("Email validation passed: $email")
    return true
}

suspend fun getMissingPasswordRequirements(password: String): List<String> {
    val missing = mutableListOf<String>()

    // Length check
    if (password.length < 8) {
        missing.add(getString(Res.string.requirement_length))
    }

    // Digit check
    if (!password.any { it.isDigit() }) {
        missing.add(getString(Res.string.requirement_digit))
    }


    // Special character check
    if (!password.any { !it.isLetterOrDigit() }) {
        missing.add(getString(Res.string.requirement_special))
    }

    // Uppercase letter check
    if (!password.any { it.isUpperCase() }) {
        missing.add(getString(Res.string.requirement_uppercase))
    }

    // Lowercase letter check
    if (!password.any { it.isLowerCase() }) {
        missing.add(getString(Res.string.requirement_lowercase))
    }

    // Common forbidden patterns
    val forbiddenPatterns = listOf(
        "123", "234", "345", "456", "567", "678", "789", "890",
        "987", "876", "765", "654", "543", "432", "321", "210",
        "geheim", "test", "1234", "password", "qwerty", "asdfgh",
        "zxcvbn", "letmein", "admin", "welcome", "111111", "abc123",
        "passwort", "fortnite"
    )

    if (forbiddenPatterns.any { password.contains(it, ignoreCase = true) }) {
        missing.add(getString(Res.string.requirement_forbidden))
    }

    return missing
}