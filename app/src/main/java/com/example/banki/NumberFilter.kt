package com.example.banki

import android.text.InputFilter
import android.text.Spanned
//para filtrar numeros ,por ejemplo con lo de las donaciones, con esto evitamos que puedas poner mas de un 5 en un campo
public class NumberFilter : InputFilter {
    private var mIntMin: Int
    private var mIntMax: Int

    constructor(minValue: Int, maxValue: Int) {
        mIntMin = minValue
        mIntMax = maxValue
    }

    constructor(minValue: String, maxValue: String) {
        mIntMin = minValue.toInt()
        mIntMax = maxValue.toInt()
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): String? {
        try {
            val input = (dest.toString() + source.toString()).toInt()
            if (isInRange(mIntMin, mIntMax, input)) return null
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c >= a && c <= b else c >= b && c <= a
    }
}