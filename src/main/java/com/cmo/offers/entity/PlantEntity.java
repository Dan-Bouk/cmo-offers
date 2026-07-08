package com.cmo.offers.entity;

public class PlantEntity {
	
    private int id;
    private ClientEntity client;
    private String name;
    
    public PlantEntity() {}
    
    public PlantEntity(int id) {
        this.id = id;
    }
    
	public PlantEntity(int id, ClientEntity client, String name) {
		super();
		this.id = id;
		this.client = client;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ClientEntity getClient() {
		return client;
	}

	public void setClient(ClientEntity client) {
		this.client = client;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
	    if (client != null) {
	        return client.getName() + " - " + name;
	    }
	    return name;
	}

}
