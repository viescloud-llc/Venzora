package com.viescloud.llc.venzora.service;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.eco.viesspringutils.service.ViesService;

public abstract class VenzoraService<I, T extends Serializable, D extends JpaRepository<T, I>> extends ViesService<I, T, D> {
    
    public VenzoraService(DatabaseCall<I, T> databaseCall, D repositoryDao) {
        super(databaseCall, repositoryDao);
    }
}
