package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.ViewModel;
import com.gui.kline.service.NavigationService;
import com.gui.kline.service.SyncService;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.JsonUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LayoutController {

    @FXML private VBox sidebarContainer, quickHide;
    @FXML private HBox headerbar;
    @FXML private AnchorPane contentPane;
    @FXML private Label lblPageTitle, lblLogo;
    @FXML private TextField txtSearch;
    @FXML private FontIcon iconCollapse;

    @FXML private Button btnHideQuick, btnCollapse, btnUpload;
    @FXML private Button btnDashboard, btnWorkers, btnInventory, btnInvoices,
            btnServices, btnSales, btnTyreExports, btnSalary,
            btnAnalytics, btnReports, btnQuickActions;

    @FXML private VBox quickServiceList;
    @FXML private Label lblTotalQuickCount;
    @FXML private Label lblTotalQuickPrice;

    private Button activeButton;
    private boolean quickActionsVisible = true, isCollapsed = false;
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

    private static final double SIDEBAR_FULL  = 260.0;
    private static final double SIDEBAR_SMALL = 65.0;
    private static final double QUICK_WIDTH   = 260.0;

    private static final String[] NAV_TEXTS = {
            "   Dashboard",
            "   Workers",
            "    Inventory",
            "     Invoices & Billing",
            "    Credit Sales",
            "    Services",
            "   Tyre Exports",
            "      Salary Management",
            "    Analytics Charts",
            "    Reports",
            "    Manage Quick Actions"
    };

    @FXML
    public void initialize() {
        setActive(btnDashboard, "Dashboard", "dashboard");
        txtSearch.textProperty().addListener((obs, o, n) -> onSearch(n));
        loadQuickActionsPanel();
        loadQuickStats();
        // Register this controller with ViewFactory for cross-controller communication
        ViewModel.INSTANCE.getViewsFactory().setLayoutController(this);
    }

    @FXML
    private void onHideQuick() {
        String title = lblPageTitle.getText();
        if (!(title.equals("Dashboard") || title.equals("Services"))) return;

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

        Button[] navButtons = {
                btnDashboard, btnWorkers, btnInventory, btnInvoices,
                btnSales, btnServices, btnTyreExports, btnSalary,
                btnAnalytics, btnReports, btnQuickActions
        };

        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i] != null) {
                navButtons[i].setText(isCollapsed ? "" : NAV_TEXTS[i]);
            }
        }

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
    @FXML private void onServices()    { setActive(btnServices, "Services", "services"); }
    @FXML private void onSales()       { setActive(btnSales, "Credit Sales", "credit-sales"); }
    @FXML private void onTyreExports() { setActive(btnTyreExports, "Tyre Exports", "tyre-exports"); }
    @FXML private void onSalary()      { setActive(btnSalary, "Salary Management", "salary"); }
    @FXML private void onAnalytics()   { setActive(btnAnalytics, "Analytics Charts", "analytics"); }
    @FXML private void onReports()     { setActive(btnReports, "Reports", "reports"); }

    @FXML private void onQuickActions() {
        Stage ownerStage = (Stage) btnQuickActions.getScene().getWindow();
        ViewModel.INSTANCE.getViewsFactory().getForm("form/quick-service-presets-dialog", ownerStage);
    }

    private void onSearch(String q) {
        System.out.println("Searching: " + q);
    }

    @FXML
    private void onQuickPolish() {
        logQuickService("Quick Polish", 500);
    }

    @FXML
    private void onTyreAirFill() {
        logQuickService("Tyre Air Fill", 100);
    }

    @FXML
    private void onCoolantTopup() {
        logQuickService("Coolant Top-up", 350);
    }

    private void logQuickService(String service, double price) {
        String insert = "INSERT INTO quick_services (id, service, price, service_date) VALUES (UUID(), ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, service);
            ps.setDouble(2, price);
            ps.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to log quick service: " + ex.getMessage());
        }
        enqueueQuickService(service, price);
        loadQuickStats();
    }

    private void enqueueQuickService(String service, double price) {
        String payload = JsonUtil.obj(
                JsonUtil.field("service", service),
                JsonUtil.field("price", price),
                JsonUtil.field("date", java.time.LocalDate.now().toString())
        );
        syncQueueRepository.enqueue("quick_service", payload);
    }

    private void loadQuickActionsPanel() {
        if (quickServiceList == null) return;
        quickServiceList.getChildren().clear();

        String sql = "SELECT service, price, icon FROM quick_service_presets WHERE active = 1 ORDER BY service";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String service = rs.getString("service");
                double price = rs.getDouble("price");
                String icon = rs.getString("icon");
                quickServiceList.getChildren().add(buildQuickServiceButton(service, price, icon));
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick action presets: " + ex.getMessage());
        }

        if (quickServiceList.getChildren().isEmpty()) {
            Label empty = new Label("No quick services yet");
            empty.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
            quickServiceList.getChildren().add(empty);
        }
    }

    private Button buildQuickServiceButton(String service, double price, String iconLiteral) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("quick-service-btn");

        FontIcon icon = new FontIcon(iconLiteral != null ? iconLiteral : "fas-bolt");
        icon.setIconSize(18);
        icon.setIconColor(javafx.scene.paint.Color.web("#f59e0b"));

        VBox textBox = new VBox();
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(service);
        title.getStyleClass().add("service-title");

        Label priceLabel = new Label("Rs. " + String.format("%.0f", price));
        priceLabel.getStyleClass().add("service-price");

        textBox.getChildren().addAll(title, priceLabel);

        HBox content = new HBox(12, icon, textBox);
        content.setAlignment(Pos.CENTER_LEFT);
        button.setGraphic(content);

        button.setOnAction(e -> logQuickService(service, price));
        return button;
    }

    public void loadQuickStats() {
        if (lblTotalQuickCount == null || lblTotalQuickPrice == null) return;
        // Daily session stats - only count today's quick services
        String sql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(price),0) AS total_sum FROM quick_services WHERE service_date = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblTotalQuickCount.setText(String.valueOf(rs.getInt("total_count")));
                    lblTotalQuickPrice.setText("Rs. " + String.format("%.0f", rs.getDouble("total_sum")));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Failed to load quick service stats: " + ex.getMessage());
        }
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
}