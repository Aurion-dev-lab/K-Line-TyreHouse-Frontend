package com.gui.kline.controller;

import com.gui.kline.service.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class Sidebar {

    @FXML private VBox sidebarContainer, quickHide;
    @FXML private HBox headerbar;
    @FXML private AnchorPane contentPane;
    @FXML private Label lblPageTitle, lblLogo;
    @FXML private TextField txtSearch;
    @FXML private FontIcon iconCollapse;

    @FXML private Button btnHideQuick, btnCollapse;
    @FXML private Button btnDashboard, btnWorkers, btnInventory, btnInvoices,
            btnSales, btnServices, btnTyreExports, btnSalary,
            btnAnalytics, btnReports, btnQuickActions;

    private Button activeButton;
    private boolean quickActionsVisible = true, isCollapsed = false;

    private static final double SIDEBAR_FULL = 260.0;
    private static final double SIDEBAR_SMALL = 65.0;
    private static final double QUICK_WIDTH = 214.0;

    @FXML
    public void initialize() {
        setActive(btnDashboard, "Dashboard", "dashboard");
        txtSearch.textProperty().addListener((obs, o, n) -> onSearch(n));
    }

    @FXML
    private void onHideQuick() {
        quickActionsVisible = !quickActionsVisible;

        quickHide.setVisible(quickActionsVisible);
        quickHide.setManaged(quickActionsVisible);

        AnchorPane.setRightAnchor(contentPane, quickActionsVisible ? QUICK_WIDTH : 0.0);
        btnHideQuick.setText(quickActionsVisible ? "Hide Quick" : "Show Quick");
    }

    @FXML
    private void onCollapse() {
        isCollapsed = !isCollapsed;

        double width = isCollapsed ? SIDEBAR_SMALL : SIDEBAR_FULL;

        sidebarContainer.setPrefWidth(width);
        lblLogo.setVisible(!isCollapsed);
        iconCollapse.setIconLiteral(isCollapsed ? "fas-angle-double-right" : "fas-angle-double-left");
        btnCollapse.setText(isCollapsed ? "" : "Collapse");

        updateLeftAnchors(width);
    }

    private void updateLeftAnchors(double width) {
        AnchorPane.setLeftAnchor(headerbar, width);
        AnchorPane.setLeftAnchor(contentPane, width);
    }

    private void setActive(Button btn, String title, String path) {
        if (activeButton != null)
            activeButton.getStyleClass().remove("nav-button-active");

        activeButton = btn;

        if (btn != null) {
            btn.getStyleClass().add("nav-button-active");
            NavigationService.navigate(contentPane, path);
        }

        if (lblPageTitle != null)
            lblPageTitle.setText(title);
    }

    @FXML private void onDashboard()   { setActive(btnDashboard, "Dashboard", "dashboard"); }
    @FXML private void onWorkers()     { setActive(btnWorkers, "Workers", "workers"); }
    @FXML private void onInventory()   { setActive(btnInventory, "Inventory", "inventory"); }
    @FXML private void onInvoices()    { setActive(btnInvoices, "Invoices & Billing", "invoices"); }
    @FXML private void onSales()       { setActive(btnSales, "Sales", "sales"); }
    @FXML private void onServices()    { setActive(btnServices, "Services", "services"); }
    @FXML private void onTyreExports() { setActive(btnTyreExports, "Tyre Exports", "tyre-exports"); }
    @FXML private void onSalary()      { setActive(btnSalary, "Salary Management", "salary"); }
    @FXML private void onAnalytics()   { setActive(btnAnalytics, "Analytics Charts", "analytics"); }
    @FXML private void onReports()     { setActive(btnReports, "Reports", "reports"); }

    @FXML private void onQuickActions() {
        System.out.println("Manage Quick Actions clicked");
    }

    private void onSearch(String q) {
        System.out.println("Searching: " + q);
    }
}