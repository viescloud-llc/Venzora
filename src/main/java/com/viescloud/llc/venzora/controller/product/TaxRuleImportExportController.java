package com.viescloud.llc.venzora.controller.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viescloud.eco.viesspringutils.interfaces.annotation.RequiresAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.viescloud.llc.venzora.dao.product.TaxRuleDao;
import com.viescloud.llc.venzora.model.checkout.TaxImportResponse;
import com.viescloud.llc.venzora.model.product.TaxRule;

/**
 * JSON import / export of TaxRule sets. Coexists with the admin CRUD at the
 * same base path; routes by full URL + verb.
 *
 * <p><strong>Auth note:</strong> these endpoints inherit no automatic admin
 * gate from the framework. In production, gate the entire {@code /tax/rules}
 * path at the reverse proxy or place explicit permission checks here.
 */
@RestController
@RequestMapping("/api/v1/tax/rules")
public class TaxRuleImportExportController {

    private final TaxRuleDao taxRuleDao;

    public TaxRuleImportExportController(TaxRuleDao taxRuleDao) {
        this.taxRuleDao = taxRuleDao;
    }

    @RequiresAuthority("rules:read")
    @GetMapping("/export")
    public List<TaxRule> export() {
        return taxRuleDao.findAll();
    }

    /**
     * Append the supplied rules to the existing set (default), or replace the
     * entire set when {@code ?mode=replace} is given. Incoming {@code id} fields
     * are ignored — every imported rule receives a fresh UUID.
     */
    @RequiresAuthority("rules:update")
    @PostMapping("/import")
    @Transactional
    public TaxImportResponse importRules(
            @RequestBody List<TaxRule> rules,
            @RequestParam(value = "mode", required = false, defaultValue = "append") String mode) {
        if (rules == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must be a JSON array of TaxRule");
        }
        boolean replace = "replace".equalsIgnoreCase(mode);
        if (!"append".equalsIgnoreCase(mode) && !replace) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mode must be 'append' or 'replace'");
        }

        int replacedCount = 0;
        if (replace) {
            replacedCount = (int) taxRuleDao.count();
            taxRuleDao.deleteAll();
        }

        for (TaxRule r : rules) {
            validate(r);
            r.setId(null);
            r.setCreatedAt(null);
            r.setUpdatedAt(null);
            if (r.getActive() == null) r.setActive(Boolean.TRUE);
            if (r.getPriority() == null) r.setPriority(0);
        }
        List<TaxRule> saved = taxRuleDao.saveAll(rules);

        return new TaxImportResponse(saved.size(), replacedCount, replace ? "replace" : "append");
    }

    private static void validate(TaxRule r) {
        if (r.getName() == null || r.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each TaxRule must have a name");
        }
        if (r.getRate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each TaxRule must have a rate (\"" + r.getName() + "\")");
        }
        if (r.getRate().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tax rate must be >= 0 (\"" + r.getName() + "\")");
        }
    }
}
