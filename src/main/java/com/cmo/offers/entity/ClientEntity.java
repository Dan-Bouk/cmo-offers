package com.cmo.offers.entity;

public class ClientEntity {
	
	private int id;
	private String name;
	
	public ClientEntity() {}
	
	public ClientEntity(int id) {
	    this.id = id;
	}
	
	public ClientEntity(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public int getId() { return id; }

	public void setId(int id) { this.id = id; }

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }
	
	@Override
	public String toString() { return name;	}
	

}
