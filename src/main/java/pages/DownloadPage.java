package pages;

import base.BasePage;
import exceptions.FrameworkException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * the-internet's /download page: a plain list of links, each named after the file it downloads —
 * no id/data-test attribute on them, so link text (the file name itself) is the only stable
 * locator. It's a shared public demo app, so which files are actually listed there varies run to
 * run (other testers' uploads come and go) — callers should pick a file off whatever's currently
 * there (see {@link #firstAvailableFileName()}) rather than hardcode one.
 */
public class DownloadPage extends BasePage {

    private static final By DOWNLOAD_LINKS = By.cssSelector(".example a");

    public DownloadPage(WebDriver driver) {
        super(driver);
    }

    public void open(String downloadPageUrl) {
        driver.get(downloadPageUrl);
    }

    public String firstAvailableFileName() {
        List<WebElement> links = driver.findElements(DOWNLOAD_LINKS);
        if (links.isEmpty()) {
            throw new FrameworkException("❗ No downloadable files found on the download page");
        }
        return links.get(0).getText().trim();
    }

    public void downloadFile(String fileName) {
        click(By.linkText(fileName));
    }
}
