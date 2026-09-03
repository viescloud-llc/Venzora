package com.viescloud.llc.venzora.service;

import com.viescloud.eco.viesspringutils.auto.service.ViesAutoServiceWithUserAccess;
import com.viescloud.eco.viesspringutils.dao.ViesUserAccessJpaRepository;
import com.viescloud.eco.viesspringutils.model.UserAccess;
import com.viescloud.eco.viesspringutils.repository.DatabaseCall;
import com.viescloud.llc.venzora.util.RequiredRelations;

public abstract class VenzoraCustomUserAccessService<I, T extends UserAccess, D extends ViesUserAccessJpaRepository<T, I>> extends ViesAutoServiceWithUserAccess<I, T, D> {

    public VenzoraCustomUserAccessService(DatabaseCall<I, T> databaseCall, D repositoryDao) {
        super(databaseCall, repositoryDao);
    }

    /**
     * Mirrors {@link VenzoraService#validatingBeforePost} — the user-access
     * hierarchy branches off before {@code VenzoraService}, so the hook has to be
     * repeated here rather than inherited.
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
