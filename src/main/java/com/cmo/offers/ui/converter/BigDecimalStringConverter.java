package com.cmo.offers.ui.converter;

import javafx.util.StringConverter;

import java.math.BigDecimal;

public class BigDecimalStringConverter extends StringConverter<BigDecimal> {

    @Override
    public String toString(BigDecimal value) {
        if (value == null) return "";
        return value.stripTrailingZeros().toPlainString();
    }

    @Override
    public BigDecimal fromString(String text) {
        if (text == null) return BigDecimal.ZERO;
        String t = text.trim();
        if (t.isEmpty()) return BigDecimal.ZERO;

        /*
        // allow comma decimals (Italian-style input)
        t = t.replace(",", "."); 
        **/

        return new BigDecimal(t);
    }
}
