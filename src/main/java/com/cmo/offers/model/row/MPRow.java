package com.cmo.offers.model.row;
  
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.cmo.offers.entity.MaterialEntity;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MPRow {
    private final MaterialEntity material;
    private final YearMonth period;

    ObjectProperty<BigDecimal> lme =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    ObjectProperty<BigDecimal> prime =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    ObjectProperty<BigDecimal> fx =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    ObjectProperty<BigDecimal> financialPercent =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    ObjectProperty<BigDecimal> managementPercent =
            new SimpleObjectProperty<>(BigDecimal.ZERO);
    ObjectProperty<BigDecimal> finalEurPerKg =
            new SimpleObjectProperty<>(BigDecimal.ZERO);

    public MPRow(MaterialEntity material,
                      YearMonth period,
                      BigDecimal lme,
                      BigDecimal prime,
                      BigDecimal fx,
                      BigDecimal financialPercent,
                      BigDecimal managementPercent,
                      BigDecimal finalEurPerKg) {
        this.material = material;
        this.period = period;
        setLme(lme);
        setPrime(prime);
        setFx(fx);
        setFinancialPercent(financialPercent);
        setManagementPercent(managementPercent);
        setFinalEurPerKg(finalEurPerKg);
    }

    public MaterialEntity getMaterial() { return material; }
    public YearMonth getPeriod() { return period; }

    public BigDecimal getLme() { return lme.get(); }
    public BigDecimal getPrime() { return prime.get(); }
    public BigDecimal getFx() { return fx.get(); }

    public BigDecimal getFinancialPercent() { return financialPercent.get(); }
    public BigDecimal getManagementPercent() { return managementPercent.get(); }

    public BigDecimal getFinalEurPerKg() { return finalEurPerKg.get(); }
    
    public void setLme(BigDecimal l) { this.lme.set(l == null ? BigDecimal.ZERO : l); }
    public void setPrime(BigDecimal p) { this.prime.set(p == null ? BigDecimal.ZERO : p); }
    public void setFx(BigDecimal f) { this.fx.set(f == null ? BigDecimal.ZERO : f); }

    public void setFinancialPercent(BigDecimal fp) { this.financialPercent.set(fp == null ? BigDecimal.ZERO : fp); }
    public void setManagementPercent(BigDecimal mp) { this.managementPercent.set(mp == null ? BigDecimal.ZERO : mp); }

    public void setFinalEurPerKg(BigDecimal epk) { this.finalEurPerKg.set(epk == null ? BigDecimal.ZERO : epk); }
    
	public ObjectProperty<BigDecimal> lmeProperty() { return lme; }
	
	public ObjectProperty<BigDecimal> primeProperty() { return prime; }
	
	public ObjectProperty<BigDecimal> fxProperty() { return fx; }
	
	public ObjectProperty<BigDecimal> financialPercentProperty() { return financialPercent; }

	public ObjectProperty<BigDecimal> managementPercentProperty() { return managementPercent; }
	
	public ObjectProperty<BigDecimal> finalEurPerKgProperty() { return finalEurPerKg; }
	
}