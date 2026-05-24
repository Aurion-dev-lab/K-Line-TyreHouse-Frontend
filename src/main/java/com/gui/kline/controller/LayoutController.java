package com.gui.kline.controller;

import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.service.NavigationService;
import com.gui.kline.service.SyncService;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

public class LayoutController {

    @FXML private VBox sidebarContainer, quickHide;
    @FXML private HBox headerbar;
    @FXML private AnchorPane contentPane;
    @FXML private Label lblPageTitle, lblLogo;
    @FXML private TextField txtSearch;
    @FXML private FontIcon iconCollapse;

    @FXML private Button btnHideQuick, btnCollapse, btnUpload;
    @FXML private Button btnDashboard, btnWorkers, btnInventory, btnInvoices,
            btnSales, btnTyreExports, btnSalary,
            btnAnalytics, btnReports, btnQuickActions;

    private Button activeButton;
    private boolean quickActionsVisible = true, isCollapsed = false;
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

    private static final double SIDEBAR_FULL = 260.0;
    private static final double SIDEBAR_SMALL = 65.0;
    private static final double QUICK_WIDTH = 260.0;

    @FXML
    public void initialize() {
        setActive(btnDashboard, "Dashboard", "dashboard");
        txtSearch.textProperty().addListener((obs, o, n) -> onSearch(n));
    }

    @FXML
    private void onHideQuick() {
        if (!(lblPageTitle.getText().equals("Dashboard") ||
                lblPageTitle.getText().equals("Services"))) {
            return;
        }
        quickActionsVisible = !quickActionsVisible;

        quickHide.setVisible(quickActionsVisible);
        quickHide.setManaged(quickActionsVisible);

        AnchorPane.setRightAnchor(contentPane, quickActionsVisible ? QUICK_WIDTH : 0.0);
        btnHideQuick.setText(quickActionsVisible ? "Hide Quick" : "Show Quick");
    }

    @FXML
    private void onCollapse() {
        isCollapsed = !isCollapsed;

        double sideWidth = isCollapsed ? SIDEBAR_SMALL : SIDEBAR_FULL;

        sidebarContainer.setPrefWidth(sideWidth);
        sidebarContainer.setMinWidth(sideWidth);
        sidebarContainer.setMaxWidth(sideWidth);

        lblLogo.setVisible(!isCollapsed);
        lblLogo.setManaged(!isCollapsed);

        iconCollapse.setIconLiteral(isCollapsed ? "fas-angle-double-right" : "fas-angle-double-left");
        btnCollapse.setText(isCollapsed ? "" : "Collapse");

        updateLeftAnchors(sideWidth);
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

        boolean showQuick = path.equals("dashboard") || path.equals("services");

        quickActionsVisible = showQuick;

        quickHide.setVisible(showQuick);
        quickHide.setManaged(showQuick);
        btnHideQuick.setVisible(showQuick);
        btnHideQuick.setManaged(showQuick);

        AnchorPane.setRightAnchor(contentPane, showQuick ? QUICK_WIDTH : 0.0);
    }

    @FXML private void onDashboard()   { setActive(btnDashboard, "Dashboard", "dashboard"); }
    @FXML private void onWorkers()     { setActive(btnWorkers, "Workers", "workers"); }
    @FXML private void onInventory()   { setActive(btnInventory, "Inventory", "inventory"); }
    @FXML private void onInvoices()    { setActive(btnInvoices, "Invoices & Billing", "invoices"); }
    @FXML private void onSales()       { setActive(btnSales, "Credit Sales", "credit-sales"); }
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

    @FXML
    private void onQuickPolish() {
        enqueueQuickService("Quick Polish", 500);
        System.out.println("Quick Polish added");
    }

    @FXML
    private void onTyreAirFill() {
        enqueueQuickService("Tyre Air Fill", 100);
        System.out.println("Tyre Air Fill added");
    }

    @FXML
    private void onCoolantTopup() {
        enqueueQuickService("Coolant Top-up", 350);
        System.out.println("Coolant Top-up added");
    }

    @FXML
    private void onUpload() {
        SyncService.SyncResult result = new SyncService().syncPending();
        if (result.getFailed() > 0) {
            AlertUtil.showError("Upload failed", result.getMessage());
        } else {
            AlertUtil.showInfo("Upload complete", result.getMessage());
        }
    }

    private void enqueueQuickService(String service, double price) {
        String payload = JsonUtil.obj(
                JsonUtil.field("service", service),
                JsonUtil.field("price", price),
                JsonUtil.field("date", java.time.LocalDate.now().toString())
        );
        syncQueueRepository.enqueue("quick_service", payload);
    }
}