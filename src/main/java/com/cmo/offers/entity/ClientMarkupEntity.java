package com.cmo.offers.entity;

import java.math.BigDecimal;
import java.time.YearMonth;

public class ClientMarkupEntity {
	
	private ClientEntity client;
    private PlantEntity plant;
    private MaterialEntity material;
    private YearMonth period;
    private BigDecimal prime;
    private BigDecimal financialPercent;
    private BigDecimal managementPercent;
    
    public ClientMarkupEntity() {}
    
	public ClientMarkupEntity(ClientEntity client, PlantEntity plant, MaterialEntity material, YearMonth period, BigDecimal prime, BigDecimal financialPercent,
			BigDecimal managementPercent) {
		super();
		this.client = client;
		this.plant = plant;
		this.material = material;
		this.period = period;
		this.prime = prime;
		this.financialPercent = financialPercent;
		this.managementPercent = managementPercent;
	}

	public ClientEntity getClient() {
		return client;
	}
	
	public void setClient(ClientEntity client) {
		this.client = client;
	}
	
	public PlantEntity getPlant() {
		return plant;
	}

	public void setPlant(PlantEntity plant) {
		this.plant = plant;
	}

	public MaterialEntity getMaterial() {
		return material;
	}

	public void setMaterial(MaterialEntity material) {
		this.material = material;
	}

	public YearMonth getPeriod() {
		return period;
	}

	public void setPeriod(YearMonth period) {
		this.period = period;
	}
	
	public BigDecimal getPrime() {
		return prime;
	}
	
	public void setPrime(BigDecimal prime) {
		this.prime = prime;
	}

	public BigDecimal getFinancialPercent() {
		return financialPercent;
	}

	public void setFinancialPercent(BigDecimal financialPercent) {
		this.financialPercent = financialPercent;
	}

	public BigDecimal getManagementPercent() {
		return managementPercent;
	}

	public void setManagementPercent(BigDecimal managementPercent) {
		this.managementPercent = managementPercent;
	}
	
	@Override
	public String toString() {
	    return material + " @ " + period;
	}
    
}
