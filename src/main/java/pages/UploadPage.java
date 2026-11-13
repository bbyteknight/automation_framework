package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * the-internet's /upload page: a plain {@code <input type="file">} + submit button, landing on a
 * result page that echoes the uploaded file's name. sendKeys(absolutePath) on the file input is
 * all a caller needs regardless of where the session runs — DriverFactory's LocalFileDetector
 * handles shipping a local path to a remote Grid node transparently.
 */
public class UploadPage extends BasePage {

    public UploadPage(WebDriver driver) {
        super(driver);
    }

    @FindBy(id = "file-upload")
    private WebElement fileInput;

    @FindBy(id = "file-submit")
    private WebElement submitButton;

    private static final By UPLOADED_FILES = By.id("uploaded-files");

    public void open(String uploadPageUrl) {
        driver.get(uploadPageUrl);
    }

    public void selectFile(String absolutePath) {
        type(fileInput, absolutePath);
    }

    public void submit() {
        click(submitButton);
    }

    public String getUploadedFileName() {
        return getText(UPLOADED_FILES);
    }
}
