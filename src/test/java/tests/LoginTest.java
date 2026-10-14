package tests;

import base.BaseTest;
import config.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.LoginPage;
import pages.SecurePage;

public class LoginTest extends BaseTest {

    @Test(groups = {"smoke", "regression"})
    public void loginShouldWork() {
        LoginPage loginPage = new LoginPage(getDriver());
        SecurePage securePage = new SecurePage(getDriver());
        loginPage.login(ConfigReader.get("loginUsername"), ConfigReader.get("loginPassword"));
        Assert.assertTrue(securePage.isSecureAreaDisplay());
    }
}
