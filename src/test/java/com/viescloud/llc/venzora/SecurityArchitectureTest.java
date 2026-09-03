package com.viescloud.llc.venzora;

import org.junit.jupiter.api.Test;

import com.viescloud.eco.viesspringutils.util.SecurityArchitecture;

/**
 * Default-deny at build time: every hand-written endpoint must declare its gate
 * (@Requires*) or its openness (@PublicEndpoint). A forgotten gate — how the
 * ungated-refund vulnerability happened — is a red build here, not a CVE.
 */
class SecurityArchitectureTest {

    @Test
    void everyEndpointDeclaresItsGateOrOpenness() {
        SecurityArchitecture.assertAllEndpointsGated("com.viescloud.llc.venzora");
    }
}
