package com.cmo.offers.export.mapper;

import com.cmo.offers.model.GIExportModel;
import com.cmo.offers.model.row.GeneralInfoRow;

public class GeneralInfoMapper {

    public static GIExportModel toDto(GeneralInfoRow row) {
    	GIExportModel dto = new GIExportModel();
        dto.setCustomerName(row.getCustomerName());
        dto.setRequestNr(row.getRequestNr());
        dto.setOfferNr(row.getOfferNr());
        dto.setOfferDate(row.getOfferDate());
        dto.setRevision(row.getRevision());
        dto.setReferenceDoc(row.getReferenceDoc());
        dto.setDescription(row.getDescription());
        dto.setDrawing(row.getDrawing());
        dto.setDrawingRev(row.getDrawingRev());
        dto.setQuantityPerYear(row.getQuantityPerYear());
        dto.setQuantityPerBatch(row.getQuantityPerBatch());
        dto.setFirstDeliveryDate(row.getFirstDeliveryDate());
        dto.setLastDeliveryDate(row.getLastDeliveryDate());
        dto.setDeliveryBatch(row.getDeliveryBatch());
        return dto;
    }

    public static GeneralInfoRow toRow(GIExportModel dto) {
        GeneralInfoRow row = new GeneralInfoRow();
        row.setCustomerName(dto.getCustomerName());
        row.setRequestNr(dto.getRequestNr());
        row.setOfferNr(dto.getOfferNr());
        row.setOfferDate(dto.getOfferDate());
        row.setRevision(dto.getRevision());
        row.setReferenceDoc(dto.getReferenceDoc());
        row.setDescription(dto.getDescription());
        row.setDrawing(dto.getDrawing());
        row.setDrawingRev(dto.getDrawingRev());
        row.setQuantityPerYear(dto.getQuantityPerYear());
        row.setQuantityPerBatch(dto.getQuantityPerBatch());
        row.setFirstDeliveryDate(dto.getFirstDeliveryDate());
        row.setLastDeliveryDate(dto.getLastDeliveryDate());
        row.setDeliveryBatch(dto.getDeliveryBatch());
        return row;
    }
}
