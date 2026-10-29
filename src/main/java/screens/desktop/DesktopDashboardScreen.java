package screens.desktop;

import base.DesktopBaseScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Dashboard view of the sample TestLoginApp (WinForms, Card 35), shown after a
 * successful login. Same window as the login view — the app swaps panel
 * visibility rather than opening a new top-level window, so no window
 * switching is needed here (see DesktopBaseScreen's window-handling helpers
 * for apps that do open secondary windows/dialogs).
 */
public class DesktopDashboardScreen extends DesktopBaseScreen {

    private final By dashboardTitle = automationId("lblDashboardTitle");
    private final By welcomeMessage = automationId("lblWelcome");
    private final By status = automationId("lblStatus");
    private final By loginTime = automationId("lblLoginTime");
    private final By logoutButton = automationId("btnLogout");

    public DesktopDashboardScreen(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return isDisplayed(dashboardTitle);
    }

    public String getWelcomeMessage() {
        return getText(welcomeMessage);
    }

    public String getStatus() {
        return getText(status);
    }

    public String getLoginTime() {
        return getText(loginTime);
    }

    public DesktopLoginScreen logout() {
        click(logoutButton);
        return new DesktopLoginScreen(driver);
    }
}
