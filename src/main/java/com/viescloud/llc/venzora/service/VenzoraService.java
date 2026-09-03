package com.viescloud.llc.venzora.service;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.eco.viesspringutils.service.ViesService;
import com.viescloud.llc.venzora.util.RequiredRelations;

public abstract class VenzoraService<I, T extends Serializable, D extends JpaRepository<T, I>> extends ViesService<I, T, D> {
    
    public VenzoraService(DatabaseCall<I, T> databaseCall, D repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    /**
     * Adds required-relation validation on top of the framework's not-null and
     * unique checks, so a missing or dangling {@code @ManyToOne} comes back as a
     * 400 naming the field rather than a 500 at flush time.
     *
     * @see RequiredRelations
     */
    @Override
    protected void validatingBeforePost(T input, T oldObject) {
        RequiredRelations.validate(input, this.entityManager);
        super.validatingBeforePost(input, oldObject);
    }

    /** PUT is a full replace, so the same relations must be present. */
    @Override
    protected void validatingBeforePut(T input, T oldObject) {
        RequiredRelations.validate(input, this.entityManager);
        super.validatingBeforePut(input, oldObject);
    }
}
