package pages;

import base.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SecurePage extends BasePage {

    public SecurePage(WebDriver driver) {
        super(driver);
    }

    @FindBy(xpath = "//div[@id='flash' and contains(text(),'You logged into a secure area!')]")
    private WebElement success;

    public boolean isSecureAreaDisplay() {
        return wait.waitForVisibility(success).isDisplayed();
    }
}
