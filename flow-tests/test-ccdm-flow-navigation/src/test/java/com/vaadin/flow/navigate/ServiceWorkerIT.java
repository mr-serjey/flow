/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.navigate;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.mobile.NetworkConnection;

import com.vaadin.flow.testutil.ChromeDeviceTest;

import static com.vaadin.flow.navigate.HelloWorldView.NAVIGATE_ABOUT;

public class ServiceWorkerIT extends ChromeDeviceTest {

    @Test
    public void onlineRoot_serviceWorkerInstalled_serviceWorkerActive() {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();

        boolean serviceWorkerActive = (boolean) ((JavascriptExecutor) getDriver())
                .executeAsyncScript("const resolve = arguments[arguments.length - 1];"
                        + "navigator.serviceWorker.ready.then( function(reg) { resolve(!!reg.active); });");
        Assert.assertTrue("service worker not installed", serviceWorkerActive);
    }

    @Test
    public void offlineRoot_reload_viewReloaded() throws IOException {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();

        // Confirm that app shell is loaded
        Assert.assertNotNull("Should have outlet when loaded online",
                findElement(By.id("outlet")));

        // Confirm that client side view is loaded
        Assert.assertNotNull("Should have <about-view> in DOM when loaded online",
                findElement(By.tagName("about-view")));

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

        try {
            Assert.assertEquals("navigator.onLine should be false", false,
                    executeScript("return navigator.onLine"));

            // Reload the page in offline mode
            executeScript("window.location.reload();");
            waitUntil(webDriver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            // Confirm that app shell is loaded
            Assert.assertNotNull("Should have outlet when loaded offline",
                    findElement(By.id("outlet")));

            // Confirm that client side view is loaded
            Assert.assertNotNull("Should have <about-view> in DOM when loaded offline",
                    findElement(By.tagName("about-view")));
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineNonRoot_reload_viewReloaded() throws IOException {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

        try {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div").id("another-content")
                    .isDisplayed());

            // Reload the page in offline mode
            executeScript("window.location.reload();");
            waitUntil(webDriver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            Assert.assertTrue(getInShadowRoot(findElement(By.tagName("another-view")),
                    By.id("another-content")).isDisplayed());
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineTsView_navigateToOtherTsView_navigationSuccessful() throws IOException {
        getDriver().get(getRootURL() + "/about");
        waitForServiceWorkerReady();

        MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                CoreMatchers.endsWith("/about"));

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

        try {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div").id("another-content")
                    .isDisplayed());

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            Assert.assertTrue(getInShadowRoot(findElement(By.tagName("another-view")),
                    By.id("another-content")).isDisplayed());
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineServerView_navigateToTsView_navigationSuccessful() throws IOException {
        getDriver().get(getRootURL() + "/hello");
        waitForServiceWorkerReady();

        // Set offline network conditions in ChromeDriver
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);

        try {
            $("main-view").first().$("a").id("menu-another").click();

            // Wait for component inside shadow root as there is no vaadin
            // to wait for as with server-side
            waitUntil(input -> $("another-view").first().$("div").id("another-content")
                    .isDisplayed());

            MatcherAssert.assertThat(getDriver().getCurrentUrl(),
                    CoreMatchers.endsWith("/another"));
            Assert.assertTrue(getInShadowRoot(findElement(By.tagName("another-view")),
                    By.id("another-content")).isDisplayed());
        } finally {
            // Reset network conditions back
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineTsView_navigateToServerView_offlineStubShown()
            throws IOException {
        getDriver().get(getRootURL() + "/another");
        waitForServiceWorkerReady();

        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            $("main-view").first().$("a").id("menu-hello").click();

            waitForElementPresent(By.tagName("vaadin-offline-stub"));
            WebElement offlineStub = findElement(
                    By.tagName("vaadin-offline-stub"));

            Assert.assertFalse(
                    "vaadin-offline-stub shadow root expected to contain an element with class offline",
                    findInShadowRoot(offlineStub, By.className("offline"))
                            .isEmpty());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineServerView_navigateToServerView_offlineStubShown()
            throws IOException {
        getDriver().get(getRootURL() + "/hello");
        waitForServiceWorkerReady();

        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            $("main-view").first().$("a").id("menu-hello").click();

            waitForElementPresent(By.tagName("vaadin-offline-stub"));
            WebElement offlineStub = findElement(
                    By.tagName("vaadin-offline-stub"));

            Assert.assertFalse(
                    "vaadin-offline-stub shadow root expected to contain an element with class offline",
                    findInShadowRoot(offlineStub, By.className("offline"))
                            .isEmpty());
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }

    @Test
    public void offlineStub_backOnline_stubRemoved_serverViewShown()
            throws IOException {
        getDriver().get(getRootURL() + "/");
        waitForServiceWorkerReady();
        setConnectionType(NetworkConnection.ConnectionType.AIRPLANE_MODE);
        try {
            $("main-view").first().$("a").id("menu-hello").click();
            waitForElementPresent(By.tagName("vaadin-offline-stub"));

            setConnectionType(NetworkConnection.ConnectionType.ALL);

            waitForElementNotPresent(By.tagName("vaadin-offline-stub"));
            Assert.assertNotNull(findElement(By.id(NAVIGATE_ABOUT)));
        } finally {
            setConnectionType(NetworkConnection.ConnectionType.ALL);
        }
    }
}
