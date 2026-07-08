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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OfferExcelExportService {

	private final String templateResourcePath;
	private final ClientDAO clientDAO;
	private final PlantDAO plantDAO;
	private final MPService mpService;

	public OfferExcelExportService(
	        String templateResourcePath,
	        ClientDAO clientDAO,
	        PlantDAO plantDAO,
	        MPService mpService
	) {
	    this.templateResourcePath = templateResourcePath;
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

        try (InputStream is = getClass().getResourceAsStream(templateResourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Excel template not found in resources: " + templateResourcePath);
            }

            try (Workbook workbook = new XSSFWorkbook(is)) {
            	fillMpSheet(workbook.getSheet("MP"), bundle);
            	
                List<ReferenceExportModel> references = safeList(bundle.getReferences());

                syncReferenceSheets(workbook, references.size());
            	
                fillOfferSheet(workbook.getSheet("OFFER"), bundle);

                for (int i = 0; i < references.size(); i++) {
                    Sheet refSheet = workbook.getSheet("SC" + (i + 1));
                    fillReferenceSheet(refSheet, bundle.getOffer(), references.get(i));
                }

                workbook.setForceFormulaRecalculation(true);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    workbook.write(fos);
                }
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
                fillOfferSummaryRow(sheet, rowNum, i + 1, refs.get(i));
            } else {
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
    
    private Sheet ensureReferenceSheet(Workbook workbook, int index) {
        String targetName = "SC" + index;

        int existingIndex = findSheetIndexByName(workbook, targetName);
        if (existingIndex >= 0) {
            return workbook.getSheetAt(existingIndex);
        }

        int templateIndex = findLastReferenceSheetIndex(workbook);
        if (templateIndex < 0) {
            throw new IllegalStateException("No SC template sheet found.");
        }

        workbook.cloneSheet(templateIndex);

        int newSheetIndex = workbook.getNumberOfSheets() - 1;
        workbook.setSheetName(newSheetIndex, targetName);
        workbook.setSheetHidden(newSheetIndex, false);

        return workbook.getSheetAt(newSheetIndex);
    }
    
    private void syncReferenceSheets(Workbook workbook, int referenceCount) {
        if (referenceCount < 1) {
            referenceCount = 1;
        }

        int offerIndex = findSheetIndexByName(workbook, "OFFER");
        if (offerIndex >= 0) {
            workbook.setActiveSheet(offerIndex);
            workbook.setSelectedTab(offerIndex);
        }

        // Ensure SC1..SC(referenceCount) exist
        for (int i = 1; i <= referenceCount; i++) {
            ensureReferenceSheet(workbook, i);
        }

        // Hide unused SC sheets instead of removing them
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (name != null && name.matches("^SC\\d+$")) {
                int sheetNumber = Integer.parseInt(name.substring(2));
                if (sheetNumber > referenceCount) {
                    workbook.setSheetVisibility(i, SheetVisibility.VERY_HIDDEN);
                } else {
                    workbook.setSheetVisibility(i, SheetVisibility.VISIBLE);
                }
            }
        }
    }
    
    private void fillReferenceSheet(Sheet sheet, OfferExportModel offer, ReferenceExportModel ref) {
        GIExportModel gi = ref.getGeneralInfo();

        set(sheet, "H1", gi != null ? gi.getOfferNr() : offer.getOfferNr());
        setDate(sheet, "J1", gi != null ? gi.getOfferDate() : offer.getOfferDate());
        set(sheet, "L1", gi != null ? gi.getRevision() : offer.getRevision());

        set(sheet, "C2", gi != null ? gi.getCustomerName() : readCustomer(offer));
        set(sheet, "H2", gi != null ? gi.getRequestNr() : readRequest(offer));

        set(sheet, "C3", gi == null ? null : gi.getReferenceDoc());
        set(sheet, "H3", gi == null ? null : gi.getDescription());

        set(sheet, "C4", gi == null ? null : gi.getDrawing());
        set(sheet, "G4", gi == null ? null : gi.getDrawingRev());
        setNumber(sheet, "J4", gi == null ? null : gi.getQuantityPerYear());
        setNumber(sheet, "L4", gi == null ? null : gi.getQuantityPerBatch());

        setDate(sheet, "C5", gi == null ? null : gi.getFirstDeliveryDate());
        setDate(sheet, "G5", gi == null ? null : gi.getLastDeliveryDate());
        setNumber(sheet, "J5", gi == null ? null : gi.getDeliveryBatch());

        fillTooling(sheet, safeList(ref.getTooling()));
        fillComponents(sheet, safeList(ref.getComponents()));
        fillRawMaterials(sheet, safeList(ref.getRawMaterials()));
        fillOperations(sheet, safeList(ref.getOperations()));
        fillTreatments(sheet, safeList(ref.getTreatments()));
        fillOtherCosts(sheet, safeList(ref.getOtherCosts()));
    }

    private void fillTooling(Sheet sheet, List<ToolingExportModel> rows) {
    	fillVariableTable(sheet, rows, 8, 5, false, (row, dto) -> {
    	    set(row, 1, dto.getCdc());
    	    set(row, 2, dto.getToolName());
    	    setNumber(row, 3, dto.getQuantity());
    	    setNumber(row, 4, dto.getUnitCost());
    	    setNumber(row, 6, dto.getMarkup());
    	    }, 
    		row -> {
    			clear(row, 1);
    		    clear(row, 2);
    		    clear(row, 3);
    		    clear(row, 4);
    		    clear(row, 6);
    		});
    }

    private void fillComponents(Sheet sheet, List<ComponentsExportModel> rows) {
        fillVariableTable(sheet, rows, 16, 5, false, (row, dto) -> {
            set(row, 1, dto.getCdc());
            set(row, 2, dto.getComponentName());
            setNumber(row, 3, dto.getQuantity());
            setNumber(row, 4, dto.getUnitCost());
            setNumber(row, 6, dto.getMarkup());
            // Keep E and G formulas
        	},
        	row -> {
        		clear(row, 1);
       		    clear(row, 2);
       		    clear(row, 3);
       		    clear(row, 4);
       		    clear(row, 6);
        	});
    }

    private void fillRawMaterials(Sheet sheet, List<RMExportModel> rows) {
        fillVariableTable(sheet, rows, 24, 1, false, (row, dto) -> {
            set(row, 1, dto.getMaterialCode());
            set(row, 2, dto.getDescription());
            setNumber(row, 3, dto.getGrossWeight());
            setNumber(row, 4, dto.getNetWeight());
            setNumber(row, 6, dto.getScrapValuePercentage());
            setNumber(row, 7, dto.getTransformationPrice());
            // Keep E/H/J/K/L/M formulas
        	},
        	row -> {
       			clear(row, 1);
       		    clear(row, 2);
       		    clear(row, 3);
       		    clear(row, 4);
       		    clear(row, 6);
       		    clear(row, 7);
       		});
    }

    private void fillOperations(Sheet sheet, List<OperationsExportModel> rows) {
        fillVariableTable(sheet, rows, 28, 10, false, (row, dto) -> {
            setNumber(row, 1, dto.getFase());
            set(row, 2, dto.getOperationName());
            set(row, 3, dto.getType());
            set(row, 4, dto.getIntExt());
            set(row, 5, dto.getCenter());
            setNumber(row, 6, dto.getCost());
            setNumber(row, 7, dto.getSetupMinutes());
            setNumber(row, 8, dto.getProductionSeconds());
            setNumber(row, 11, dto.getMarkup());
        	},
       		row -> {
           		clear(row, 1);
           	    clear(row, 2);
           	    clear(row, 3);
           	    clear(row, 4);
           	    clear(row, 5);
           	    clear(row, 6);
           	    clear(row, 7);
           	    clear(row, 8);
           	    clear(row, 11);
           	});	
    }

    private void fillTreatments(Sheet sheet, List<TreatmentsExportModel> rows) {
        fillVariableTable(sheet, rows, 42, 1, false, (row, dto) -> {
            setNumber(row, 1, dto.getFase());
            set(row, 2, dto.getTreatmentName());
            set(row, 3, dto.getType());
            set(row, 4, dto.getIntExt());
            set(row, 5, dto.getCenter());
            setNumber(row, 7, dto.getOperationCost());
            setNumber(row, 8, dto.getOperationMarkup());
            setNumber(row, 10, dto.getSilverQuantityGr());
            setNumber(row, 12, dto.getSilverMarkup());
            // Keep I/K/M formulas
        	},
        	row -> {
        		clear(row, 1);
        		clear(row, 2);
        		clear(row, 3);
        		clear(row, 4);
        		clear(row, 5);
        		clear(row, 7);
        		clear(row, 8);
        		clear(row, 10);
        		clear(row, 12);
        	});
    }

    private void fillOtherCosts(Sheet sheet, List<OtherExportModel> rows) {
        final int startRow = 46;
        final int maxRows = 4;

        if (rows == null || rows.isEmpty()) {
            // Keep the 4 default template rows exactly as they are.
            for (int i = 0; i < maxRows; i++) {
                Row row = getOrCreateRow(sheet, startRow + i);
                row.setZeroHeight(false);
            }
            return;
        }

        for (int i = 0; i < maxRows; i++) {
            Row row = getOrCreateRow(sheet, startRow + i);
            row.setZeroHeight(false);

            OtherExportModel dto = i < rows.size() ? rows.get(i) : null;

            if (dto == null) {
                continue;
            }

            set(row, 1, dto.getCdc());
            set(row, 2, dto.getDescription());

            // For rows like TRAS/PACK/MP/MACH the template often uses formulas/bindings.
            // We only write editable/value cells when present.
            if (!isSpecialOtherCost(dto)) {
                setNumber(row, 3, dto.getQuantity());
                setNumber(row, 4, dto.getUnitCost());
                setNumber(row, 7, dto.getMarkup());
            }
        }
    }
    
    private void fillOfferSummaryRow(Sheet sheet, int rowNum, int refIndex, ReferenceExportModel ref) {
        Row row = getOrCreateRow(sheet, rowNum);
        String sc = "'SC" + refIndex + "'!";

        set(row, 1, Integer.toString(refIndex));          // A

        setFormula(row, 2,  sc + "$C$3");                 // B
        setStringReference(row, 3,  sc + "$H$3");         // C
        setStringReference(row, 4,  sc + "$C$4");         // D
        setStringReference(row, 5,  sc + "$G$4");                 // E
        setFormula(row, 6,  sc + "$J$4");                 // F
        setFormula(row, 7,  sc + "$L$4");                 // G
        setFormula(row, 8,  sc + "$J$5");                 // H

        setFormula(row, 9,  sc + "$C$25");                // I
        setFormula(row, 10, sc + "$D$25");                // J
        setFormula(row, 11, "I" + rowNum + "-J" + rowNum);// K

        setFormula(row, 12, sc + "$A$24");                // L
        setStringReference(row, 13, sc + "$B$24");        // M
        setFormula(row, 14, sc + "$M$24");                // N Base Price €/Kg
        setFormula(row, 15, sc + "$G$24");                // O
        setFormula(row, 16, sc + "$J$25");                // P

        setFormula(row, 17, sc + "$L$38");                // Q
        setFormula(row, 18, sc + "$M$38");                // R
        setFormula(row, 19, sc + "$J$42");                // S
        setFormula(row, 20, sc + "$M$42");                // T
        setFormula(row, 21, sc + "$I$42");                // U

        set(row, 22, "Operations");                       // V

        setFormula(row, 23, sc + "$M$39");                // W
        setFormula(row, 24, sc + "$B$15");                // X
        setFormula(row, 25, sc + "$G$21");                // Y
        setFormula(row, 26, sc + "$E$47");                // Z
        setFormula(row, 27, sc + "$E$46");                // AA
        setPriceFormula(row);                             // AB
        setFormula(row, 29, sc + "$G$13");				  // AC
        
        // AD
        CellStyle dateStyleAD = sheet.getRow(11).getCell(29).getCellStyle(); 
        setFormulaKeepingStyle(row, 30, sc + "$C$5",  dateStyleAD);
        
        // AE
        CellStyle dateStyleAE = sheet.getRow(11).getCell(30).getCellStyle(); 
        setFormulaKeepingStyle(row, 31, sc + "$G$5", dateStyleAE);
    }

    private void clearOfferSummaryRow(Sheet sheet, int rowNum) {
        Row row = getOrCreateRow(sheet, rowNum);

        // Clear the full OFFER summary row area
        for (int col = 1; col <= 27; col++) {
            clear(row, col);
        }

        row.setZeroHeight(false);
    }

    private void clear(Row row, int col1Based) {
        Cell cell = row.getCell(col1Based - 1);
        if (cell == null) {
            return;
        }

        // Do not delete formulas from the Excel template
        if (cell.getCellType() == CellType.FORMULA) {
            return;
        }

        cell.setBlank();
    }

    private boolean isSpecialOtherCost(OtherExportModel dto) {
        String cdc = trim(dto.getCdc());
        return "MP".equalsIgnoreCase(cdc) || "MACH".equalsIgnoreCase(cdc);
    }

    // ---------------- helpers ----------------
        
    private int findSheetIndexByName(Workbook workbook, String sheetName) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (sheetName.equals(workbook.getSheetName(i))) {
                return i;
            }
        }
        return -1;
    }
    
    private int findLastReferenceSheetIndex(Workbook workbook) {
        int bestIndex = -1;
        int bestNumber = -1;

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (name != null && name.matches("^SC\\d+$")) {
                int number = Integer.parseInt(name.substring(2));
                if (number > bestNumber) {
                    bestNumber = number;
                    bestIndex = i;
                }
            }
        }

        return bestIndex;
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

    private String trim(String s) {
        return s == null ? null : s.trim();
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
    
    private void setFormula(Row row, int col1Based, String formula) {
        Cell cell = row.getCell(col1Based - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

        if (formula.startsWith("=")) {
            formula = formula.substring(1);
        }

        cell.setCellFormula(formula);
    }
    
    private void setFormulaKeepingStyle(Row row, int col1Based, String formula, CellStyle style) {
        Cell cell = row.getCell(col1Based - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

        if (style != null) {
            cell.setCellStyle(style);
        }

        if (formula.startsWith("=")) {
            formula = formula.substring(1);
        }

        cell.setCellFormula(formula);
    }
    
    private void setPriceFormula(Row row) {
        int r = row.getRowNum() + 1;

        setFormula(
            row,
            28, // AB
            String.format(
                "SUM(P%d:R%d)+SUM(T%d:U%d)+W%d+Y%d+Z%d+AA%d",
                r, r,
                r, r,
                r, r, r, r
            )
        );
    }
    
    private void setStringReference(Row row, int col1Based, String reference) {
        setFormula(
            row,
            col1Based,
            "IF(" + reference + "=\"\",\"\","
                + reference + ")"
        );
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
    
    // Helper: hide unused rows
    private <T> void fillVariableTable(
            Sheet sheet,
            List<T> rows,
            int startRow,
            int templateRows,
            boolean keepFirstTemplateRowWhenEmpty,
            BiConsumer<Row, T> fillRow,
            Consumer<Row> clearDataCells
    ) {
        if (rows == null) {
            rows = List.of();
        }

        int rowsToKeep = keepFirstTemplateRowWhenEmpty
                ? Math.max(1, rows.size())
                : rows.size();

        for (int i = 0; i < rowsToKeep; i++) {
            Row row = getOrCreateRow(sheet, startRow + i);
            row.setZeroHeight(false);

            T dto = i < rows.size() ? rows.get(i) : null;

            if (i == 0 && dto == null && keepFirstTemplateRowWhenEmpty) {
                continue;
            }

            fillRow.accept(row, dto);
        }

        for (int i = rowsToKeep; i < templateRows; i++) {
            Row row = getOrCreateRow(sheet, startRow + i);
            
            clearDataCells.accept(row);
            row.setZeroHeight(true);
        }
    }
}
