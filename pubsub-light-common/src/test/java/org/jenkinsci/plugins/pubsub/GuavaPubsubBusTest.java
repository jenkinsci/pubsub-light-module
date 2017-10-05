package org.jenkinsci.plugins.pubsub;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GuavaPubsubBusTest {
    @Test
    public void getBus() throws Exception {
        assertTrue(PubsubBus.getBus() instanceof GuavaPubsubBus);
    }

    @Test
    public void getBus_specify() throws Exception {
        assertTrue(PubsubBus.getBus("GuavaPubsubBus") instanceof GuavaPubsubBus);
    }
}