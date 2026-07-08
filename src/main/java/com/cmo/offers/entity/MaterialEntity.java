package com.cmo.offers.entity;

public class MaterialEntity {
	
    private int id;
    private String code;
    
    public MaterialEntity() {}
    
    public MaterialEntity(int id) {
        this.id = id;
    }
    
	public MaterialEntity(int id, String code) {
		super();
		this.id = id;
		this.code = code;
	}

	public int getId() { return id;	}

	public void setId(int id) { this.id = id; }

	public String getCode() { return code; }

	public void setCode(String code) { this.code = code; }
	
	@Override
	public String toString() { return code; }
    
}
