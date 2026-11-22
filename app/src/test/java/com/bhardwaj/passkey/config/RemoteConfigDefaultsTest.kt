package com.bhardwaj.passkey.config

import org.junit.Test

/**
 * In-app defaults and the deployable template (`<project>/firebase/remoteconfig.template.json`) never drift.
 * Optional feature groups check their own keys (ads: `AdConfigKeysTest`).
 */
class RemoteConfigDefaultsTest {
    @Test
    fun `template has every in-app key with matching defaults and types`() {
        RemoteConfigTemplate.assertMatches(ConfigKeys.all)
    }
}
