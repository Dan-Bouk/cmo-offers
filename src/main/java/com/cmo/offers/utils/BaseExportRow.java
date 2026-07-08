package com.cmo.offers.utils;

import java.math.BigDecimal;

import com.cmo.offers.model.ExportRow;

public abstract class BaseExportRow implements ExportRow {

    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    protected boolean isZero(Integer value) {
        return value == null || value == 0;
    }
}
