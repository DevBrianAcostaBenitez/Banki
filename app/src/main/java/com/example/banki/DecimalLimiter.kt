package com.example.banki

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Matcher
import java.util.regex.Pattern

//queremos que solo se puedan poner 2 decimales
public class DecimalLimiter(digitsBeforeDecimals: Int, Decimals: Int) :
    InputFilter {
    var mPattern: Pattern

    init {
        mPattern =
            Pattern.compile("[0-9]{0," + (digitsBeforeDecimals - 1) + "}+((\\.[0-9]{0," + (Decimals - 1) + "})?)||(\\.)?")
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): String? {
        val matcher: Matcher = mPattern.matcher(dest)
        return if (!matcher.matches()) "" else null
    }
}