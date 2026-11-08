package com.lambdatest;

import java.net.URI;
import java.time.Duration;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class TestNG_Scenarios {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String ALERT_MESSAGE = "I am an alert box!";

    private final ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<RemoteWebDriver>();
    private final ThreadLocal<String> testStatus = new ThreadLocal<String>();

    @BeforeMethod
    @Parameters({ "browserName", "browserVersion", "platformName", "baseUrl" })
    public void setUp(String browserName, String browserVersion, String platformName, String baseUrl) throws Exception {
        String username = System.getenv("LT_USERNAME");
        String accessKey = System.getenv("LT_ACCESS_KEY");

        if (isBlank(username) || isBlank(accessKey)) {
            throw new SkipException("Set LT_USERNAME and LT_ACCESS_KEY before running the cloud test.");
        }

        MutableCapabilities ltOptions = new MutableCapabilities();
        ltOptions.setCapability("build", "TestNG Assignment");
        ltOptions.setCapability("name", "TestNG Scenarios - " + browserName);
        ltOptions.setCapability("network", true);
        ltOptions.setCapability("video", true);
        ltOptions.setCapability("visual", true);
        ltOptions.setCapability("console", true);
        ltOptions.setCapability("plugin", "git-testng");

        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("browserName", browserName);
        capabilities.setCapability("browserVersion", browserVersion);
        capabilities.setCapability("platformName", platformName);
        capabilities.setCapability("LT:Options", ltOptions);

        URI gridUri = new URI("https", username + ":" + accessKey,
                "hub.lambdatest.com", -1, "/wd/hub", null, null);
        RemoteWebDriver remoteDriver = new RemoteWebDriver(gridUri.toURL(), capabilities);
        remoteDriver.manage().timeouts().pageLoadTimeout(TIMEOUT);
        remoteDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        remoteDriver.get(baseUrl);

        driver.set(remoteDriver);
        testStatus.set("failed");
    }

    @Test
    public void shouldContinueAfterSoftTitleAssertionFailure() {
        WebDriver webDriver = driver.get();
        WebDriverWait wait = new WebDriverWait(webDriver, TIMEOUT);
        wait.until(webDriverInstance -> "complete".equals(
                ((JavascriptExecutor) webDriverInstance).executeScript("return document.readyState")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("main")));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(webDriver.getTitle(), "TestMu AI",
                "Intentional assignment check: the page title is not exactly 'TestMu AI'.");
        softAssert.assertTrue(webDriver.getCurrentUrl().contains("selenium-playground"),
                "Execution continues after the soft assertion.");
        softAssert.assertAll();
    }

    @Test
    public void shouldSelectAndUnselectSingleCheckbox() {
        WebDriver webDriver = driver.get();
        WebDriverWait wait = new WebDriverWait(webDriver, TIMEOUT);
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Checkbox Demo"))).click();

        WebElement singleCheckbox = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//h2[normalize-space()='Single Checkbox Demo']/following::input[@type='checkbox'][1]")));
        singleCheckbox.click();
        Assert.assertTrue(singleCheckbox.isSelected(), "The single checkbox should be selected.");

        singleCheckbox.click();
        Assert.assertFalse(singleCheckbox.isSelected(), "The single checkbox should be unselected.");
        testStatus.set("passed");
    }

    @Test
    public void shouldValidateAndAcceptJavaScriptAlert() {
        WebDriver webDriver = driver.get();
        WebDriverWait wait = new WebDriverWait(webDriver, TIMEOUT);
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Javascript Alerts"))).click();

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//p[contains(., 'JavaScript Alerts')]/button[normalize-space()='Click Me']"))).click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        Assert.assertEquals(alert.getText(), ALERT_MESSAGE, "The JavaScript alert message is incorrect.");
        alert.accept();
        testStatus.set("passed");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        RemoteWebDriver remoteDriver = driver.get();
        try {
            if (remoteDriver != null) {
                remoteDriver.executeScript("lambda-status=" + testStatus.get());
                remoteDriver.quit();
            }
        } finally {
            driver.remove();
            testStatus.remove();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
