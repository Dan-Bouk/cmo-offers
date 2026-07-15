package com.cmo.offers.export.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.PlantDAO;
import com.cmo.offers.entity.ClientEntity;
import com.cmo.offers.entity.PlantEntity;
import com.cmo.offers.model.ComponentsExportModel;
import com.cmo.offers.model.GIExportModel;
import com.cmo.offers.model.OfferBundle;
import com.cmo.offers.model.OfferExportModel;
import com.cmo.offers.model.OperationsExportModel;
import com.cmo.offers.model.OtherExportModel;
import com.cmo.offers.model.RMExportModel;
import com.cmo.offers.model.ReferenceExportModel;
import com.cmo.offers.model.ToolingExportModel;
import com.cmo.offers.model.TreatmentsExportModel;
import com.cmo.offers.model.row.MPRow;
import com.cmo.offers.ui.service.MPService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OfferExcelExportService {
	
	private static final String EXCEL_TEMPLATE_RESOURCE = "/Preventivo.xlsx";

	private final ClientDAO clientDAO;
	private final PlantDAO plantDAO;
	private final MPService mpService;

	public OfferExcelExportService(
	        ClientDAO clientDAO,
	        PlantDAO plantDAO,
	        MPService mpService
	) {
	    this.clientDAO = clientDAO;
	    this.plantDAO = plantDAO;
	    this.mpService = mpService;
	}

	public void exportOffer(OfferBundle bundle, File outputFile) throws IOException {
		
        if (bundle == null) {
            throw new IllegalArgumentException("bundle is null");
        }
        if (bundle.getOffer() == null) {
            throw new IllegalArgumentException("bundle.offer is null");
        }

        try (InputStream is = getClass().getResourceAsStream(EXCEL_TEMPLATE_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Excel template not found in resources: " + EXCEL_TEMPLATE_RESOURCE);
            }

            try (Workbook workbook = new XSSFWorkbook(is)) {
            	fillMpSheet(workbook.getSheet("MP"), bundle);
            	
            	fillOfferSheet(workbook.getSheet("OFFER"), bundle);

            	// OFFER no longer depends on SC sheets, so remove them.
            	removeReferenceSheets(workbook);

            	int offerIndex = workbook.getSheetIndex("OFFER");

            	if (offerIndex >= 0) {
            	    workbook.setActiveSheet(offerIndex);
            	    workbook.setSelectedTab(offerIndex);
            	}

                workbook.setForceFormulaRecalculation(true);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    workbook.write(fos);
                }
            }
        }
    }
	
	private void removeReferenceSheets(Workbook workbook) {
	    for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
	        String sheetName = workbook.getSheetName(i);

	        if (sheetName != null
	                && sheetName.matches("(?i)^SC\\d+$")) {
	            workbook.removeSheetAt(i);
	        }
	    }
	}
	
	private void clearMpDataCells(Sheet sheet) {
	    // Copper
	    clearCell(sheet, "B3");
	    clearCell(sheet, "B4");
	    clearCell(sheet, "B7");
	    clearCell(sheet, "C5");
	    clearCell(sheet, "C6");

	    // Aluminium
	    clearCell(sheet, "I3");
	    clearCell(sheet, "I4");
	    clearCell(sheet, "I7");
	    clearCell(sheet, "J5");
	    clearCell(sheet, "J6");

	    // Silver
	    clearCell(sheet, "P3");
	}
	
	// helper
	private void clearCell(Sheet sheet, String address) {
	    CellReference ref = new CellReference(address);

	    Row row = sheet.getRow(ref.getRow());
	    if (row == null) {
	        row = sheet.createRow(ref.getRow());
	    }

	    Cell cell = row.getCell(ref.getCol(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
	    cell.setBlank();
	}
	
	private void fillMpSheet(Sheet sheet, OfferBundle bundle) {
	    if (sheet == null) {
	        return;
	    }

	    clearMpDataCells(sheet); // important: removes template values first

	    if (bundle == null || bundle.getOffer() == null) {
	        return;
	    }

	    try {
	        OfferExportModel offer = bundle.getOffer();

	        String customerName = offer.getCustomer();
	        LocalDate offerDate = offer.getOfferDate();

	        if (customerName == null || customerName.isBlank() || offerDate == null) {
	            return;
	        }

	        ClientEntity client = clientDAO.findByName(customerName)
	                .orElse(null);

	        if (client == null) {
	            return;
	        }

	        List<PlantEntity> plants = plantDAO.findByClientId(client.getId());

	        if (plants == null || plants.isEmpty()) {
	            return;
	        }

	        PlantEntity firstPlant = plants.get(0);

	        YearMonth mpMonth = YearMonth.from(offerDate).minusMonths(1);

	        List<MPRow> mpRows = mpService.loadPricingGrid(client, firstPlant, mpMonth);

	        fillMpSheetFromRows(sheet, mpRows, mpMonth);

	    } catch (SQLException ex) {
	        throw new RuntimeException("Failed to load MP data for Excel export", ex);
	    }
	}
	
	private void fillMpSheetFromRows(Sheet sheet, List<MPRow> rows, YearMonth mpMonth) {
	    if (rows == null) {
	        return;
	    }

	    set(sheet, "F3", "AVG " + mpMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
	            .toUpperCase() + " " + mpMonth.getYear());

	    for (MPRow row : rows) {
	        String code = row.getMaterial().getCode();

	        if ("CU".equalsIgnoreCase(code)) {
	            setNumber(sheet, "B3", row.getLme());
	            setNumber(sheet, "B4", row.getPrime());
	            setNumber(sheet, "B7", row.getFx());
	            setNumber(sheet, "C5", row.getFinancialPercent().divide(BigDecimal.valueOf(100)));
	            setNumber(sheet, "C6", row.getManagementPercent().divide(BigDecimal.valueOf(100)));
	        }

	        if ("AL".equalsIgnoreCase(code)) {
	            setNumber(sheet, "I3", row.getLme());
	            setNumber(sheet, "I4", row.getPrime());
	            setNumber(sheet, "I7", row.getFx());
	            setNumber(sheet, "J5", row.getFinancialPercent().divide(BigDecimal.valueOf(100)));
	            setNumber(sheet, "J6", row.getManagementPercent().divide(BigDecimal.valueOf(100)));
	        }

	        if ("AG".equalsIgnoreCase(code) || "SILVER".equalsIgnoreCase(code)) {
	            setNumber(sheet, "P3", row.getFinalEurPerKg());
	        }
	    }
	}

    private void fillOfferSheet(Sheet sheet, OfferBundle bundle) {
        if (sheet == null) {
            return;
        }

        OfferExportModel offer = bundle.getOffer();
        List<ReferenceExportModel> refs = safeList(bundle.getReferences());
        
        int additionalOperationSlots =
                findAdditionalOperationSlotCount(refs);

        prepareAdditionalOperationColumns(
                sheet,
                additionalOperationSlots
        );

        System.out.println(
                "Additional operation slots required: "
                        + additionalOperationSlots
        );

        // HEADER (unchanged)
        set(sheet, "C4", readCustomer(offer));
        set(sheet, "C5", readRequest(offer));
        set(sheet, "C6", offer.getOfferNr());
        setDate(sheet, "C7", offer.getOfferDate());
        set(sheet, "C8", offer.getRevision());

        // ---- TEMPLATE STRUCTURE ----
        int startRow = 12;        // first reference row
        int templateRows = 10;    // rows 12–21
        int footerStartRow = 23;  // DELIVERY TIME row
        // ----------------------------

        // 👉 STEP 1: expand if needed
        if (refs.size() > templateRows) {
            int extra = refs.size() - templateRows;

            // move footer down
            sheet.shiftRows(
                    footerStartRow - 1,
                    sheet.getLastRowNum(),
                    extra,
                    true,
                    false
            );

            // copy last template row
            int sourceRow = startRow; // use a real normal reference row from the template

            for (int i = 0; i < extra; i++) {
                int targetRow = startRow + templateRows + i;
                copyRowStyleOnly(sheet, sourceRow, targetRow);
            }
        }

        // 👉 STEP 2: fill rows
        int totalRows = Math.max(refs.size(), templateRows);

        for (int i = 0; i < totalRows; i++) {
            int rowNum = startRow + i;
            Row row = getOrCreateRow(sheet, rowNum);

            if (i < refs.size()) {
                row.setZeroHeight(false);
                fillOfferSummaryRow(
                        sheet,
                        rowNum,
                        i + 1,
                        refs.get(i),
                        additionalOperationSlots
                );            } else {
                clearOfferSummaryRow(sheet, rowNum);
                row.setZeroHeight(true);
            }
        }
    }
       
    private void copyRowStyleOnly(Sheet sheet, int sourceRowNum, int targetRowNum) {
        Row sourceRow = sheet.getRow(sourceRowNum);
        Row targetRow = sheet.getRow(targetRowNum);

        if (sourceRow == null) {
            return;
        }

        if (targetRow == null) {
            targetRow = sheet.createRow(targetRowNum);
        }

        targetRow.setHeight(sourceRow.getHeight());

        int rowOffset = targetRowNum - sourceRowNum;

        for (int i = sourceRow.getFirstCellNum(); i < sourceRow.getLastCellNum(); i++) {
            Cell sourceCell = sourceRow.getCell(i);
            Cell targetCell = targetRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

            if (sourceCell == null) {
                targetCell.setBlank();
                continue;
            }

            targetCell.setCellStyle(sourceCell.getCellStyle());

            if (sourceCell.getCellType() == CellType.FORMULA) {
                String adjustedFormula = adjustFormulaRows(sourceCell.getCellFormula(), rowOffset);
                targetCell.setCellFormula(adjustedFormula);
            } else {
                targetCell.setBlank();
            }
        }
    }

    private String adjustFormulaRows(String formula, int rowOffset) {
        Pattern pattern = Pattern.compile("(\\$?[A-Z]{1,3})(\\$?)(\\d+)");
        Matcher matcher = pattern.matcher(formula);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String column = matcher.group(1);
            String absoluteRowMarker = matcher.group(2);
            int row = Integer.parseInt(matcher.group(3));

            // Do NOT modify absolute rows like A$5
            if ("$".equals(absoluteRowMarker)) {
                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(column + absoluteRowMarker + row)
                );
                continue;
            }

            int adjustedRow = row + rowOffset;

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(column + adjustedRow)
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }    
    
    private void fillOfferSummaryRow(
            Sheet sheet,
            int rowNum,
            int refIndex,
            ReferenceExportModel ref,
            int additionalOperationSlots
    ) {
        Row row = getOrCreateRow(sheet, rowNum);
        GIExportModel gi = ref.getGeneralInfo();
        
        /*
         * TEMPORARY TEST:
         * Check how many external additional operations
         * are detected for this reference.
         */
        List<OperationsExportModel> additionalOperations =
                getExternalAdditionalOperations(ref);

        System.out.println(
                "Reference " + refIndex
                        + " matched external additional operations: "
                        + additionalOperations.size()
        );

        for (OperationsExportModel operation : additionalOperations) {
            System.out.println(
                    "Matched operation: "
                            + operation.getOperationName()
            );
        }
        
        System.out.println(
                "Reference " + refIndex
                        + " uses sheet slot count: "
                        + additionalOperationSlots
        );

        // A — reference row number
        set(row, 1, Integer.toString(refIndex));

        // B — reference document
        set(row, 2, gi != null ? gi.getReferenceDoc() : null);

        // C — description
        set(row, 3, gi != null ? gi.getDescription() : null);

        // D — drawing
        set(row, 4, gi != null ? gi.getDrawing() : null);

        // E — drawing revision
        set(row, 5, gi != null ? gi.getDrawingRev() : null);

        // F — quantity per year
        setNumber(
                row,
                6,
                gi != null && gi.getQuantityPerYear() != null
                        ? BigDecimal.valueOf(gi.getQuantityPerYear())
                        : null
        );

        // G — quantity per batch
        setNumber(
                row,
                7,
                gi != null && gi.getQuantityPerBatch() != null
                        ? BigDecimal.valueOf(gi.getQuantityPerBatch())
                        : null
        );

        // H — delivery batch
        setNumber(
                row,
                8,
                gi != null && gi.getDeliveryBatch() != null
                        ? BigDecimal.valueOf(gi.getDeliveryBatch())
                        : null
        );

        /*
         * Keep columns I–AC temporarily connected to the SC sheet.
         * We will replace these with Java calculations in the next step.
         */

        RMExportModel material = getFirstRawMaterial(ref);

     // I — gross weight
     setNumber(
             row,
             9,
             material != null ? material.getGrossWeight() : null
     );

     // J — net weight
     setNumber(
             row,
             10,
             material != null ? material.getNetWeight() : null
     );

     // K — scrap weight
     if (material != null
             && material.getGrossWeight() != null
             && material.getNetWeight() != null) {

    	 setNumber(
    		        row,
    		        11,
    		        material.getGrossWeight().doubleValue()
    		                - material.getNetWeight().doubleValue()
    		);
     } else {
         setNumber(row, 11, null);
     }

     // L — material code
     set(
             row,
             12,
             material != null ? material.getMaterialCode() : null
     );

     // M — material description
     set(
             row,
             13,
             material != null ? material.getDescription() : null
     );

     // N — final material price per kg
     setNumber(
             row,
             14,
             material != null ? material.getFinalEurPerKg() : null
     );

     // O — transformation price
     setNumber(
             row,
             15,
             material != null ? material.getTransformationPrice() : null
     );

     // P — total raw-material price
     setNumber(
             row,
             16,
             material != null ? material.getPrice() : null
     );

  // Q — total operation setup cost
     setNumber(
             row,
             17,
             sumOperationSetupCost(ref.getOperations())
     );

     // R — total operation production cost
     setNumber(
             row,
             18,
             sumOperationProductionCost(ref.getOperations())
     );
  // S — silver quantity
     setNumber(
             row,
             19,
             sumTreatmentSilverQuantity(ref.getTreatments())
     );

     // T — treatment selling price
     setNumber(
             row,
             20,
             sumTreatmentPrice(ref.getTreatments())
     );

     // U — silver cost
     setNumber(
             row,
             21,
             sumTreatmentSilverCost(ref.getTreatments())
     );

        /*
         * Dynamic additional-operation section.
         *
         * Column numbers here are 1-based:
         * W = 23
         * X = 24
         */
        int additionalOperationsStartColumn = 22;

        for (int i = 0; i < additionalOperationSlots; i++) {
            int descriptionColumn =
                    additionalOperationsStartColumn + (i * 2);

            int priceColumn =
                    descriptionColumn + 1;

            if (i < additionalOperations.size()) {
                OperationsExportModel operation =
                        additionalOperations.get(i);

                // Description
                set(
                        row,
                        descriptionColumn,
                        operation.getOperationName()
                );

                // Selling price per piece
                setNumber(
                        row,
                        priceColumn,
                        getOperationSellingPrice(operation)
                );
            } else {
                // Leave unused slots empty
                set(row, descriptionColumn, null);
                setNumber(row, priceColumn, null);
            }
        }
        
        int componentsDescriptionColumn =
                additionalOperationsStartColumn
                        + (additionalOperationSlots * 2);

        int componentsPriceColumn =
                componentsDescriptionColumn + 1;
        
        set(
                row,
                componentsDescriptionColumn,
                buildComponentsDescription(ref.getComponents())
        );

        setNumber(
                row,
                componentsPriceColumn,
                sumComponentPrices(ref.getComponents())
        );
        
        int extraOperationColumns =
                Math.max(0, additionalOperationSlots - 1) * 2;

        int toolingPriceColumn = 25 + extraOperationColumns;
        int packagingColumn = 26 + extraOperationColumns;
        int transportColumn = 27 + extraOperationColumns;
        int finalPriceColumn = 28 + extraOperationColumns;
        int toolingCostColumn = 29 + extraOperationColumns;
        int firstDeliveryColumn = 30 + extraOperationColumns;
        int lastDeliveryColumn = 31 + extraOperationColumns;

        setNumber(
                row,
                toolingPriceColumn,
                sumToolingPrices(ref.getTooling())
        );

        setNumber(
                row,
                packagingColumn,
                findOtherCostPrice(ref.getOtherCosts(), "PACK")
        );

        setNumber(
                row,
                transportColumn,
                findOtherCostPrice(ref.getOtherCosts(), "TRAS")
        );

        setNumber(
                row,
                finalPriceColumn,
                calculateOfferRowPrice(ref)
        );

        setNumber(
                row,
                toolingCostColumn,
                sumToolingCosts(ref.getTooling())
        );

        setDateInRow(
                row,
                firstDeliveryColumn,
                gi != null ? gi.getFirstDeliveryDate() : null
        );

        setDateInRow(
                row,
                lastDeliveryColumn,
                gi != null ? gi.getLastDeliveryDate() : null
        );
    }
    
    private BigDecimal calculateOfferRowPrice(
            ReferenceExportModel ref
    ) {
        RMExportModel material = getFirstRawMaterial(ref);

        BigDecimal rawMaterialPrice =
                material != null && material.getPrice() != null
                        ? material.getPrice()
                        : BigDecimal.ZERO;

        BigDecimal additionalOperationPrice =
                getExternalAdditionalOperations(ref).stream()
                        .map(this::getOperationSellingPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rawMaterialPrice
                .add(sumOperationSetupCost(ref.getOperations()))
                .add(sumOperationProductionCost(ref.getOperations()))
                .add(sumTreatmentPrice(ref.getTreatments()))
                .add(sumTreatmentSilverCost(ref.getTreatments()))
                .add(additionalOperationPrice)
                .add(sumComponentPrices(ref.getComponents()))
                .add(sumToolingPrices(ref.getTooling()))
                .add(findOtherCostPrice(
                        ref.getOtherCosts(),
                        "PACK"
                ))
                .add(findOtherCostPrice(
                        ref.getOtherCosts(),
                        "TRAS"
                ));
    }
    
    private String buildComponentsDescription(
            List<ComponentsExportModel> components
    ) {
        String description = safeList(components).stream()
                .filter(Objects::nonNull)
                .map(ComponentsExportModel::getComponentName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining("/"));

        return description.isEmpty() ? null : description;
    }
    
    private BigDecimal getOperationSellingPrice(
            OperationsExportModel operation
    ) {
        if (operation == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal setupPrice =
                operation.getSetupPrice() != null
                        ? operation.getSetupPrice()
                        : BigDecimal.ZERO;

        BigDecimal productionPrice =
                operation.getProdPrice() != null
                        ? operation.getProdPrice()
                        : BigDecimal.ZERO;

        return setupPrice.add(productionPrice);
    }
    
    private void setDateInRow(
            Row row,
            int col1Based,
            LocalDate value
    ) {
        Cell cell = row.getCell(
                col1Based - 1,
                Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
        );

        cell.setBlank();

        if (value != null) {
            cell.setCellValue(Date.valueOf(value));
        }
    }

    private void clearOfferSummaryRow(
            Sheet sheet,
            int rowNum
    ) {
        Row row = getOrCreateRow(sheet, rowNum);

        for (int col = 1; col <= 31; col++) {
            Cell cell = row.getCell(
                    col - 1,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
            );

            cell.setBlank();
        }

        row.setZeroHeight(false);
    }   
    
    private void prepareAdditionalOperationColumns(
            Sheet sheet,
            int additionalOperationSlots
    ) {
        int extraSlots = additionalOperationSlots - 1;

        if (extraSlots <= 0) {
            return;
        }

        int extraColumns = extraSlots * 2;

        /*
         * Current layout:
         * W = operation description
         * X = operation price
         * Y onward = components, tooling, packaging...
         *
         * Column indexes here are zero-based:
         * W = 22
         * X = 23
         * Y = 24
         */
        int firstColumnToMove = 24; // Y
        int lastColumnToMove = findLastUsedColumn(sheet);

        sheet.shiftColumns(
                firstColumnToMove,
                lastColumnToMove,
                extraColumns
        );

        /*
         * Copy the existing W/X operation slot into each newly
         * created operation pair.
         */
        for (int slot = 1; slot < additionalOperationSlots; slot++) {
            int targetDescriptionColumn = 22 + slot * 2;
            int targetPriceColumn = targetDescriptionColumn + 1;

            copyColumn(
                    sheet,
                    22,
                    targetDescriptionColumn
            );

            copyColumn(
                    sheet,
                    23,
                    targetPriceColumn
            );
        }
    }
    
    private int findLastUsedColumn(Sheet sheet) {
        int lastColumn = 0;

        for (Row row : sheet) {
            if (row != null && row.getLastCellNum() > 0) {
                lastColumn = Math.max(
                        lastColumn,
                        row.getLastCellNum() - 1
                );
            }
        }

        return lastColumn;
    }
    
    private void copyColumn(
            Sheet sheet,
            int sourceColumn,
            int targetColumn
    ) {
        sheet.setColumnWidth(
                targetColumn,
                sheet.getColumnWidth(sourceColumn)
        );

        for (int rowIndex = 0;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            Cell sourceCell = row.getCell(sourceColumn);
            Cell targetCell = row.getCell(
                    targetColumn,
                    Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
            );

            if (sourceCell == null) {
                targetCell.setBlank();
                continue;
            }

            targetCell.setCellStyle(sourceCell.getCellStyle());

            switch (sourceCell.getCellType()) {
                case STRING:
                    targetCell.setCellValue(
                            sourceCell.getStringCellValue()
                    );
                    break;

                case NUMERIC:
                    targetCell.setCellValue(
                            sourceCell.getNumericCellValue()
                    );
                    break;

                case BOOLEAN:
                    targetCell.setCellValue(
                            sourceCell.getBooleanCellValue()
                    );
                    break;

                case FORMULA:
                    targetCell.setCellFormula(
                            sourceCell.getCellFormula()
                    );
                    break;

                case ERROR:
                    targetCell.setCellErrorValue(
                            sourceCell.getErrorCellValue()
                    );
                    break;

                default:
                    targetCell.setBlank();
                    break;
            }
        }
    }

    private String readCustomer(OfferExportModel dto) {
        return dto == null ? null : dto.getCustomer();
    }

    private String readRequest(OfferExportModel dto) {
        return dto == null ? null : dto.getRequest();
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    private Row getOrCreateRow(Sheet sheet, int rowNum1Based) {
        int idx = rowNum1Based - 1;
        Row row = sheet.getRow(idx);
        return row != null ? row : sheet.createRow(idx);
    }

    private Cell getOrCreateCell(Sheet sheet, String ref) {
        CellReference cr = new CellReference(ref);
        Row row = getOrCreateRow(sheet, cr.getRow() + 1);
        Cell cell = row.getCell(cr.getCol());
        return cell != null ? cell : row.createCell(cr.getCol());
    }

    private void set(Sheet sheet, String ref, String value) {
        Cell cell = getOrCreateCell(sheet, ref);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private void setNumber(Sheet sheet, String ref, Number value) {
        Cell cell = getOrCreateCell(sheet, ref);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }
        
    private boolean isExternalAdditionalOperation(
            OperationsExportModel operation
    ) {
        if (operation == null) {
            return false;
        }

        String type = trim(operation.getType());
        String intExt = trim(operation.getIntExt());

        return "ADD".equalsIgnoreCase(type)
                && "E".equalsIgnoreCase(intExt);
    }
    
    private int findAdditionalOperationSlotCount(
            List<ReferenceExportModel> references
    ) {
        int maxOperations = safeList(references).stream()
                .filter(Objects::nonNull)
                .mapToInt(ref ->
                        getExternalAdditionalOperations(ref).size()
                )
                .max()
                .orElse(0);

        // Always keep at least one empty slot
        return Math.max(1, maxOperations);
    }
    
    private String trim(String s) {
        return s == null ? null : s.trim();
    }
    
    private List<OperationsExportModel> getExternalAdditionalOperations(
            ReferenceExportModel ref
    ) {
        if (ref == null) {
            return List.of();
        }

        return safeList(ref.getOperations()).stream()
                .filter(this::isExternalAdditionalOperation)
                .toList();
    }
        
    private RMExportModel getFirstRawMaterial(ReferenceExportModel ref) {
        if (ref == null || ref.getRawMaterials() == null) {
            return null;
        }

        return ref.getRawMaterials().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
        
    private BigDecimal findOtherCostPrice(
            List<OtherExportModel> otherCosts,
            String cdc
    ) {
        return safeList(otherCosts).stream()
                .filter(Objects::nonNull)
                .filter(cost ->
                        cost.getCdc() != null
                        && cost.getCdc().trim().equalsIgnoreCase(cdc)
                )
                .map(OtherExportModel::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal sumTreatmentSilverQuantity(
            List<TreatmentsExportModel> treatments
    ) {
        return safeList(treatments).stream()
                .filter(Objects::nonNull)
                .map(TreatmentsExportModel::getSilverQuantityGr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTreatmentPrice(
            List<TreatmentsExportModel> treatments
    ) {
        return safeList(treatments).stream()
                .filter(Objects::nonNull)
                .map(TreatmentsExportModel::getOperationPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTreatmentSilverCost(
            List<TreatmentsExportModel> treatments
    ) {
        return safeList(treatments).stream()
                .filter(Objects::nonNull)
                .map(TreatmentsExportModel::getSilverCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal sumOperationSetupCost(
            List<OperationsExportModel> operations
    ) {
        return safeList(operations).stream()
                .filter(Objects::nonNull)
                .filter(operation ->
                        !isExternalAdditionalOperation(operation)
                )
                .map(OperationsExportModel::getSetupCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private BigDecimal sumOperationProductionCost(
            List<OperationsExportModel> operations
    ) {
        return safeList(operations).stream()
                .filter(Objects::nonNull)
                .filter(operation ->
                        !isExternalAdditionalOperation(operation)
                )
                .map(OperationsExportModel::getProdCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }    
    
    private BigDecimal sumComponentPrices(List<ComponentsExportModel> components) {
        return safeList(components).stream()
                .filter(Objects::nonNull)
                .map(ComponentsExportModel::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumToolingPrices(List<ToolingExportModel> tooling) {
        return safeList(tooling).stream()
                .filter(Objects::nonNull)
                .map(ToolingExportModel::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumToolingCosts(List<ToolingExportModel> tooling) {
        return safeList(tooling).stream()
                .filter(Objects::nonNull)
                .map(ToolingExportModel::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void setDate(Sheet sheet, String ref, LocalDate value) {
        Cell cell = getOrCreateCell(sheet, ref);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(Date.valueOf(value));
        }
    }

    private void set(Row row, int col1Based, String value) {
        Cell cell = row.getCell(col1Based - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private void setNumber(Row row, int col1Based, Number value) {
        Cell cell = row.getCell(col1Based - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }
}
