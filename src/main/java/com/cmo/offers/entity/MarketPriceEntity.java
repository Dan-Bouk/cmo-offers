package com.cmo.offers.entity;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MarketPriceEntity {
	private MaterialEntity material;
	private YearMonth period;
	private BigDecimal lme;
	private BigDecimal fx;
	private BigDecimal eurPerKg;
	
	public MarketPriceEntity() {}
	
	public MarketPriceEntity(MaterialEntity material, YearMonth period, BigDecimal lme, 
			BigDecimal fx, BigDecimal eurPerKg) {
		super();
		this.material = material;
		this.period = period;
		this.lme = lme;
		this.fx = fx;
		this.eurPerKg = eurPerKg;
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

	public BigDecimal getLme() {
		return lme;
	}

	public void setLme(BigDecimal lme) {
		this.lme = lme;
	}

	public BigDecimal getFx() {
		return fx;
	}

	public void setFx(BigDecimal fx) {
		this.fx = fx;
	}
	
	public BigDecimal getEurPerKg() {
		return eurPerKg;
	}
	
	public void setEurPerKg(BigDecimal eurPerKg) {
		this.eurPerKg = eurPerKg;
	}
	
	@Override
	public String toString() {
	    return material + " @ " + period;
	}
	
}
