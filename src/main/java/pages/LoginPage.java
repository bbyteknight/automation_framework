package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Locator strategy for this site: prefer id (stable, already present on both
 * inputs) over xpath/css. Fall back to xpath only for elements with no
 * id/data-test attribute of their own — the submit button and the shared
 * #flash message div, which the site reuses for both success and error text.
 */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(xpath = "//button[@type='submit']")
    private WebElement loginButton;

    private static final By FLASH_MESSAGE = By.id("flash");

    public void login(String username, String password) {
        type(usernameInput, username);
        type(passwordInput, password);
        click(loginButton);
    }

    /** Text of the #flash message div — populated on both successful and failed login attempts. */
    public String getFlashMessage() {
        return getText(FLASH_MESSAGE);
    }
}
