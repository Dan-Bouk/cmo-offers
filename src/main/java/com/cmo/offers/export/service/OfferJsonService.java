package com.cmo.offers.export.service;

import com.cmo.offers.model.OfferBundle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

public class OfferJsonService {

    private final ObjectMapper objectMapper;

    public OfferJsonService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(File file, OfferBundle bundle) throws IOException {
        objectMapper.writeValue(file, bundle);
    }

    public OfferBundle read(File file) throws IOException {
        return objectMapper.readValue(file, OfferBundle.class);
    }
}
