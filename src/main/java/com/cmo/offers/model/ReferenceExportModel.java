package com.cmo.offers.model;

import java.util.ArrayList;
import java.util.List;

public class ReferenceExportModel {
    private String referenceId;
    private GIExportModel generalInfo;
    private List<ToolingExportModel> tooling = new ArrayList<>();
    private List<ComponentsExportModel> components = new ArrayList<>();
    private List<RMExportModel> rawMaterials = new ArrayList<>();
    private List<OperationsExportModel> operations = new ArrayList<>();
    private List<TreatmentsExportModel> treatments = new ArrayList<>();
    private List<OtherExportModel> otherCosts = new ArrayList<>();

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public GIExportModel getGeneralInfo() {
        return generalInfo;
    }

    public void setGeneralInfo(GIExportModel generalInfo) {
        this.generalInfo = generalInfo;
    }

    public List<ToolingExportModel> getTooling() {
        return tooling;
    }

    public void setTooling(List<ToolingExportModel> tooling) {
        this.tooling = tooling;
    }

    public List<ComponentsExportModel> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentsExportModel> components) {
        this.components = components;
    }

    public List<RMExportModel> getRawMaterials() {
        return rawMaterials;
    }

    public void setRawMaterials(List<RMExportModel> rawMaterials) {
        this.rawMaterials = rawMaterials;
    }

    public List<OperationsExportModel> getOperations() {
        return operations;
    }

    public void setOperations(List<OperationsExportModel> operations) {
        this.operations = operations;
    }

    public List<TreatmentsExportModel> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<TreatmentsExportModel> treatments) {
        this.treatments = treatments;
    }

    public List<OtherExportModel> getOtherCosts() {
        return otherCosts;
    }

    public void setOtherCosts(List<OtherExportModel> otherCosts) {
        this.otherCosts = otherCosts;
    }
}
