package com.cmo.offers.utils;

import com.cmo.offers.dao.ClientDAO;
import com.cmo.offers.dao.ClientMarkupDAO;
import com.cmo.offers.dao.MarketPriceDAO;
import com.cmo.offers.dao.MaterialDAO;
import com.cmo.offers.dao.OfferDAO;
import com.cmo.offers.dao.OfferRefDAO;
import com.cmo.offers.dao.OfferTreeDAO;
import com.cmo.offers.dao.PlantDAO;
import com.cmo.offers.dao.UserDAO;
import com.cmo.offers.service.AuthService;
import com.cmo.offers.ui.service.MPService;
import com.cmo.offers.ui.service.OfferService;
import com.cmo.offers.ui.service.RawMaterialService;
import com.cmo.offers.ui.window.ReferenceWindowManager;

public class AppContext {

    // =========================
    // DAOs
    // =========================
    public final OfferDAO offerDAO;
    public final OfferTreeDAO offerTreeDAO;
    public final OfferRefDAO offerRefDAO;

    public final ClientDAO clientDAO;
    public final PlantDAO plantDAO;
    public final MaterialDAO materialDAO;
    public final MarketPriceDAO marketPriceDAO;
    public final ClientMarkupDAO clientMarkupDAO;

    // =========================
    // SERVICES
    // =========================
    public final MPService mpService;
    public final RawMaterialService rawMaterialService;
    public final OfferService offerService;
    public final AuthService authService;

    
    public final ReferenceWindowManager windowManager; 
    
    public AppContext() {

        // ---- DAOs ----
        this.offerDAO = new OfferDAO();
        this.offerTreeDAO = new OfferTreeDAO();
        this.offerRefDAO = new OfferRefDAO();

        this.clientDAO = new ClientDAO();
        this.plantDAO = new PlantDAO();
        this.materialDAO = new MaterialDAO();
        this.marketPriceDAO = new MarketPriceDAO();
        this.clientMarkupDAO = new ClientMarkupDAO();

        // ---- Services (dependency order matters!) ----
        this.mpService = new MPService(
                materialDAO,
                marketPriceDAO,
                clientMarkupDAO
        );

        this.rawMaterialService = new RawMaterialService(
                marketPriceDAO,
                clientMarkupDAO,
                mpService
        );

        this.offerService = new OfferService(
                offerDAO,
                offerTreeDAO,
                offerRefDAO
        );

        this.authService = new AuthService(
                new UserDAO()
        );
        
    	this.windowManager = new ReferenceWindowManager(
    	        offerService,
    	        clientDAO,
    	        plantDAO,
    	        materialDAO,
    	        marketPriceDAO,
    	        rawMaterialService
    	);
        
    }
}
