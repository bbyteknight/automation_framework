package screens.mobile;

import base.BaseScreen;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** "My Cart" screen of Sauce Labs' "My Demo App", reached via the header cart icon. */
public class CartScreen extends BaseScreen {

    private static final By ITEMS_COUNT = AppiumBy.id("com.saucelabs.mydemoapp.android:id/itemsTV");
    private static final By TOTAL_PRICE = AppiumBy.id("com.saucelabs.mydemoapp.android:id/totalPriceTV");
    private static final By FIRST_ITEM_TITLE = AppiumBy.id("com.saucelabs.mydemoapp.android:id/titleTV");
    private static final By PROCEED_TO_CHECKOUT = AppiumBy.id("com.saucelabs.mydemoapp.android:id/cartBt");

    public CartScreen(WebDriver driver) {
        super(driver);
    }

    public String getItemsCount() {
        return getText(ITEMS_COUNT);
    }

    public String getTotalPrice() {
        return getText(TOTAL_PRICE);
    }

    public String getFirstItemTitle() {
        return getText(FIRST_ITEM_TITLE);
    }

    public void proceedToCheckout() {
        tap(PROCEED_TO_CHECKOUT);
    }
}
