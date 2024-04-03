/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MonitorExtension.EventSpec;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.adobe.marketing.mobile.optimize.ADBCountDownLatch;
import com.adobe.marketing.mobile.optimize.OptimizeTestConstants;
import com.adobe.marketing.mobile.services.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test helper for functional testing to read, write, reset and assert against eventhub events,
 * shared states and persistence data.
 */
public class TestHelper {
    private static final String TAG = "TestHelper";
    static final int WAIT_TIMEOUT_MS = 1000;
    static final int WAIT_EVENT_TIMEOUT_MS = 2000;
    static Application defaultApplication;

    // List of threads to wait for after test execution
    private static final List<String> knownThreads = new ArrayList<>();

    static {
        knownThreads.add("pool"); // used for threads that execute the listeners code
        knownThreads.add("ADB"); // module internal threads
    }

    /**
     * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
     * tears down the MobileCore after test execution.
     *
     * <p>To use, add the following to your test class:
     *
     * <pre>
     * 	@Rule
     * 	public TestHelper.SetupCoreRule coreRule = new TestHelper.SetupCoreRule();
     * </pre>
     */
    public static class SetupCoreRule implements TestRule {

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (defaultApplication == null) {
                        Context context =
                                InstrumentationRegistry.getInstrumentation().getTargetContext();
                        defaultApplication =
                                Instrumentation.newApplication(CustomApplication.class, context);
                    }

                    MobileCore.setLogLevel(LoggingMode.VERBOSE);
                    MobileCore.setApplication(defaultApplication);

                    try {
                        base.evaluate();
                    } catch (Throwable e) {
                        Log.debug(
                                OptimizeTestConstants.LOG_TAG,
                                "SetupCoreRule",
                                "Wait after test failure.");
                        throw e; // rethrow test failure
                    } finally {
                        // After test execution
                        Log.debug(
                                OptimizeTestConstants.LOG_TAG,
                                "SetupCoreRule",
                                "Finished '" + description.getMethodName() + "'");
                        waitForThreads(5000); // wait to allow thread to run after test execution

                        MobileCore.resetSDK();
                        resetTestExpectations();
                    }
                }
            };
        }
    }

    /**
     * {@code TestRule} which registers the {@code MonitorExtension}, allowing test cases to assert
     * events passing through the {@code EventHub}. This {@code TestRule} must be applied after the
     * {@link SetupCoreRule} to ensure the {@code MobileCore} is setup for testing first.
     *
     * <p>To use, add the following to your test class:
     *
     * <pre>
     *  @Rule
     * 	public RuleChain rule = RuleChain.outerRule(new SetupCoreRule())
     * 							.around(new RegisterMonitorExtensionRule());
     * </pre>
     */
    public static class RegisterMonitorExtensionRule implements TestRule {

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    MonitorExtension.registerExtension();

                    try {
                        base.evaluate();
                    } finally {
                        MonitorExtension.reset();
                    }
                }
            };
        }
    }

    /**
     * Waits for all the {@code #knownThreads} to finish or fails the test after timeoutMillis if
     * some of them are still running when the timer expires. If timeoutMillis is 0, a default
     * timeout will be set = 1000ms
     *
     * @param timeoutMillis max waiting time
     */
    public static void waitForThreads(final int timeoutMillis) {
        int TEST_DEFAULT_TIMEOUT_MS = 1000;
        int TEST_DEFAULT_SLEEP_MS = 50;
        int TEST_INITIAL_SLEEP_MS = 100;

        long startTime = System.currentTimeMillis();
        int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
        int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

        sleep(TEST_INITIAL_SLEEP_MS);
        Set<Thread> threadSet = getEligibleThreads();

        while (threadSet.size() > 0
                && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
            Log.debug(
                    OptimizeTestConstants.LOG_TAG,
                    TAG,
                    "waitForThreads - Still waiting for " + threadSet.size() + " thread(s)");

            for (Thread t : threadSet) {

                Log.debug(
                        OptimizeTestConstants.LOG_TAG,
                        TAG,
                        "waitForThreads - Waiting for thread "
                                + t.getName()
                                + " ("
                                + t.getId()
                                + ")");
                boolean done = false;
                boolean timedOut = false;

                while (!done && !timedOut) {
                    if (t.getState().equals(Thread.State.TERMINATED)
                            || t.getState().equals(Thread.State.TIMED_WAITING)
                            || t.getState().equals(Thread.State.WAITING)) {
                        // Cannot use the join() API since we use a cached thread pool, which
                        // means that we keep idle threads around for 60secs (default timeout).
                        done = true;
                    } else {
                        // blocking
                        sleep(sleepTime);
                        timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
                    }
                }

                if (timedOut) {
                    Log.debug(
                            OptimizeTestConstants.LOG_TAG,
                            TAG,
                            "waitForThreads - Timeout out waiting for thread "
                                    + t.getName()
                                    + " ("
                                    + t.getId()
                                    + ")");
                } else {
                    Log.debug(
                            OptimizeTestConstants.LOG_TAG,
                            TAG,
                            "waitForThreads - Done waiting for thread "
                                    + t.getName()
                                    + " ("
                                    + t.getId()
                                    + ")");
                }
            }

            threadSet = getEligibleThreads();
        }

        Log.debug(
                OptimizeTestConstants.LOG_TAG,
                TAG,
                "waitForThreads - All known threads are terminated.");
    }

    /**
     * Retrieves all the known threads that are still running
     *
     * @return set of running Threads
     */
    private static Set<Thread> getEligibleThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Set<Thread> eligibleThreads = new HashSet<>();

        for (Thread t : threadSet) {
            if (isAppThread(t)
                    && !t.getState().equals(Thread.State.WAITING)
                    && !t.getState().equals(Thread.State.TERMINATED)
                    && !t.getState().equals(Thread.State.TIMED_WAITING)) {
                eligibleThreads.add(t);
            }
        }

        return eligibleThreads;
    }

    /**
     * Checks if current thread is not a daemon and its name starts with one of the known thread
     * names specified here {@link #knownThreads}
     *
     * @param t current thread to verify
     * @return true if it is a known thread, false otherwise
     */
    private static boolean isAppThread(final Thread t) {
        if (t.isDaemon()) {
            return false;
        }

        for (String prefix : knownThreads) {
            if (t.getName().startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /** Resets the network and event test expectations. */
    public static void resetTestExpectations() {
        Log.debug(
                OptimizeTestConstants.LOG_TAG,
                TAG,
                "Resetting functional test expectations for events");
        MonitorExtension.reset();
    }

    // ---------------------------------------------------------------------------------------------
    // Event Test Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     *
     * @param type the event type as in the expectation
     * @param source the event source as in the expectation
     * @return list of events with the provided {@code type} and {@code source}, or empty if none
     *     was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(final String type, final String source)
            throws InterruptedException {
        return getDispatchedEventsWith(type, source, WAIT_EVENT_TIMEOUT_MS);
    }

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     *
     * @param source the event source as in the expectation
     * @param timeout how long should this method wait for the expected event, in milliseconds.
     * @return list of events with the provided {@code type} and {@code source}, or empty if none
     *     was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(
            final String type, final String source, int timeout) throws InterruptedException {
        EventSpec eventSpec = new EventSpec(source, type);

        Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
        Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

        ADBCountDownLatch expectedEventLatch = expectedEvents.get(eventSpec);

        if (expectedEventLatch != null) {
            boolean awaitResult = expectedEventLatch.await(timeout, TimeUnit.MILLISECONDS);
            Assert.assertTrue(
                    "Timed out waiting for event type "
                            + eventSpec.type
                            + " and source "
                            + eventSpec.source,
                    awaitResult);
        } else {
            sleep(WAIT_TIMEOUT_MS);
        }

        return receivedEvents.containsKey(eventSpec)
                ? receivedEvents.get(eventSpec)
                : Collections.emptyList();
    }

    /**
     * Synchronous call to get the shared state for the specified {@code stateOwner}. This API
     * throws an assertion failure in case of timeout.
     *
     * @param stateOwner the owner extension of the shared state (typically the name of the
     *     extension)
     * @param timeout how long should this method wait for the requested shared state, in
     *     milliseconds
     * @return latest shared state of the given {@code stateOwner} or null if no shared state was
     *     found
     * @throws InterruptedException
     */
    public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout)
            throws InterruptedException {
        Event event =
                new Event.Builder(
                                "Get Shared State Request",
                                OptimizeTestConstants.EventType.MONITOR,
                                OptimizeTestConstants.EventSource.SHARED_STATE_REQUEST)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put(
                                                OptimizeTestConstants.EventDataKeys.STATE_OWNER,
                                                stateOwner);
                                    }
                                })
                        .build();

        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, Object> sharedState = new HashMap<>();
        MobileCore.dispatchEventWithResponseCallback(
                event,
                WAIT_EVENT_TIMEOUT_MS,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        Log.error(
                                OptimizeTestConstants.LOG_TAG,
                                TAG,
                                "Failed to get shared state for "
                                        + stateOwner
                                        + ": "
                                        + adobeError.getErrorName());
                    }

                    @Override
                    public void call(Event event) {
                        if (event.getEventData() != null) {
                            sharedState.putAll(event.getEventData());
                        }

                        latch.countDown();
                    }
                });

        Assert.assertTrue(
                "Timeout waiting for shared state " + stateOwner,
                latch.await(timeout, TimeUnit.MILLISECONDS));
        return sharedState.isEmpty() ? null : sharedState;
    }

    /**
     * Pause test execution for the given {@code milliseconds}
     *
     * @param milliseconds the time to sleep the current thread.
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Clear all the shared states.. */
    public static void clearSharedState() {
        MobileCore.resetSDK();
    }

    /** Dummy Application for the test instrumentation */
    public static class CustomApplication extends Application {
        public CustomApplication() {}
    }
}
