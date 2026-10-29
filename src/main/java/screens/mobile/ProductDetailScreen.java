package screens.mobile;

import base.BaseScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Product detail screen of Sauce Labs' "My Demo App", reached via ProductsScreen.selectProduct(). */
public class ProductDetailScreen extends BaseScreen {

    private static final By ADD_TO_CART_BUTTON = AppiumBy.id("com.saucelabs.mydemoapp.android:id/cartBt");
    private static final By CART_ICON = AppiumBy.id("com.saucelabs.mydemoapp.android:id/cartRL");
    private static final By PRODUCT_TITLE = AppiumBy.id("com.saucelabs.mydemoapp.android:id/productTV");

    public ProductDetailScreen(WebDriver driver) {
        super(driver);
    }

    public String getProductTitle() {
        return getText(PRODUCT_TITLE);
    }

    public void addToCart() {
        tap(ADD_TO_CART_BUTTON);
    }

    /** The cart icon lives in the header shared across screens, so it's reachable from here too. */
    public CartScreen openCart() {
        tap(CART_ICON);
        return new CartScreen(driver);
    }
}
