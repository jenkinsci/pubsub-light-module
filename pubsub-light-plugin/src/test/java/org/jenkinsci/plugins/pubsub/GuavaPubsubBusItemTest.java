package org.jenkinsci.plugins.pubsub;

import hudson.model.User;
import org.jenkinsci.plugins.pubsub.message.EventFilter;
import org.jenkinsci.plugins.pubsub.message.ItemMessage;
import org.jenkinsci.plugins.pubsub.message.Message;
import org.jenkinsci.plugins.pubsub.message.SimpleJenkinsMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class GuavaPubsubBusItemTest {
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    private GuavaPubsubBus bus;
    
    @Before
    public void setupRealm() {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
    }

    @Before
    public void startBus() {
        bus = new JenkinsGuavaPubsubBus();
    }

    @After
    public void stop() {
        bus.shutdown();
    }
    
    @Test
    public void test_non_filtered() {
        User alice = User.get("alice");

        ChannelPublisher jobPublisher = bus.publisher("jenkins.job");
        ChannelPublisher slavePublisher = bus.publisher("jenkins.slave");
        MockSubscriber subs1 = new MockSubscriber();
        MockSubscriber subs2 = new MockSubscriber();

        // Subscribers ...
        bus.subscribe("jenkins.job", subs1, alice.impersonate(), null);
        bus.subscribe("jenkins.slave", subs2, alice.impersonate(), null);
        
        // Publish ...
        jobPublisher.publish(new SimpleJenkinsMessage().set("joba", "joba"));
        slavePublisher.publish(new SimpleJenkinsMessage().set("slavea", "slavea"));
        
        // Check receipt ...
        subs1.waitForMessageCount(1);
        assertEquals("joba", subs1.messages.get(0).getProperty("joba"));
        subs2.waitForMessageCount(1);
        assertEquals("slavea", subs2.messages.get(0).getProperty("slavea"));
    }

    @Test
    public void test_filtered() {
        User alice = User.get("alice");

        ChannelPublisher jobPublisher = bus.publisher("jenkins.job");
        MockSubscriber subs = new MockSubscriber();

        // Subscribers ...
        bus.subscribe("jenkins.job", subs, alice.impersonate(), new EventFilter().set("joba", "joba"));
        
        // Publish ...
        jobPublisher.publish(new SimpleJenkinsMessage().set("joba", "joba")); // Should get delivered
        jobPublisher.publish(new SimpleJenkinsMessage().set("joba", "----")); // Should get filtered out
        
        // Check receipt ...
        subs.waitForMessageCount(1);
        assertEquals("joba", subs.messages.get(0).getProperty("joba"));
    }

    @Test
    public void test_has_permissions() throws InterruptedException {
        User alice = User.get("alice");

        ChannelPublisher jobPublisher = bus.publisher("jenkins.job");
        MockSubscriber subs = new MockSubscriber();

        bus.subscribe("jenkins.job", subs, alice.impersonate(), null);
        
        jobPublisher.publish(new ItemMessage(new MockItem().setACL(MockItem.YES_ACL)).set("joba", "1"));
        jobPublisher.publish(new ItemMessage(new MockItem().setACL(MockItem.YES_ACL)).set("joba", "2"));
        subs.waitForMessageCount(2);
        assertEquals(2, subs.messages.size());
        jobPublisher.publish(new ItemMessage(new MockItem().setACL(MockItem.NO_ACL)).set("joba", "3"));
        Thread.sleep(200);
        assertEquals(2, subs.messages.size()); // Should still be 2 because the last message use the NO_ACL
        jobPublisher.publish(new ItemMessage(new MockItem().setACL(MockItem.YES_ACL)).set("joba", "4"));
        subs.waitForMessageCount(3);
        assertEquals(3, subs.messages.size());
        
        // Check and make sure all the messages are clones i.e. do not have a copy of
        // the MockItem used to create the original
        for (Message message : subs.messages) {
            assertNull(((ItemMessage) message).getMessageItem());
        }
    }
}