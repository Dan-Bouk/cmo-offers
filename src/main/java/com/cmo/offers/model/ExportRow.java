package com.cmo.offers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ExportRow {
	
    @JsonIgnore
    boolean isEmptyRow();

}
