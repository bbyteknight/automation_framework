package screens;

import base.BaseScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Product catalog screen of Sauce Labs' "My Demo App" (com.saucelabs.mydemoapp.android).
 * Locators were captured from the live app via `adb shell uiautomator dump`, not guessed.
 */
public class ProductsScreen extends BaseScreen {

    private static final String PRODUCT_TITLE_RESOURCE_ID = "com.saucelabs.mydemoapp.android:id/titleTV";
    private static final String PRODUCT_IMAGE_RESOURCE_ID = "com.saucelabs.mydemoapp.android:id/productIV";
    private static final By CART_ICON = AppiumBy.id("com.saucelabs.mydemoapp.android:id/cartRL");
    private static final By MENU_ICON = AppiumBy.id("com.saucelabs.mydemoapp.android:id/menuIV");

    public ProductsScreen(WebDriver driver) {
        super(driver);
    }

    /**
     * Taps a product card by its visible title (e.g. "Sauce Labs Backpack") and opens its detail
     * screen. The title TextView itself isn't clickable — only the product image is — so this
     * locates the title by text, then taps its preceding-sibling product image (verified against
     * the live UI hierarchy via `adb shell uiautomator dump`).
     */
    public ProductDetailScreen selectProduct(String productName) {
        By productImage = By.xpath("//*[@resource-id='" + PRODUCT_TITLE_RESOURCE_ID + "' and @text='"
                + productName + "']/preceding-sibling::*[@resource-id='" + PRODUCT_IMAGE_RESOURCE_ID + "']");
        tap(productImage);
        return new ProductDetailScreen(driver);
    }

    public CartScreen openCart() {
        tap(CART_ICON);
        return new CartScreen(driver);
    }

    public void openMenu() {
        tap(MENU_ICON);
    }
}
