package com.gui.kline.controller;

import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.SyncDataRepository;
import com.gui.kline.models.ViewModel;
import com.gui.kline.service.NavigationService;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.BackgroundTask;
import com.gui.kline.utils.JsonUtil;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LayoutController {

    @FXML private VBox sidebarContainer, quickHide;
    @FXML private HBox headerbar;
    @FXML private AnchorPane contentPane;
    @FXML private Label lblPageTitle, lblLogo;
    @FXML private TextField txtSearch;
    @FXML private FontIcon iconCollapse;

    @FXML private Button btnHideQuick, btnCollapse, btnUpload;
    @FXML private Button btnDashboard, btnWorkers, btnInventory, btnInvoices,
            btnServices, btnSales, btnTyreExports, btnExpenses, btnSalary,
            btnAnalytics, btnReports, btnQuickActions;
    @FXML private FontIcon connectionBulb;

    @FXML private VBox quickServiceList;
    @FXML private Label lblTotalQuickCount;
    @FXML private Label lblTotalQuickPrice;

    // Search dropdown (Popup floats above all other nodes)
    private final Popup searchPopup = new Popup();
    private final VBox searchResultsList = new VBox();

    private Button activeButton;
    private boolean quickActionsVisible = true, isCollapsed = false;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));

    private static final double SIDEBAR_FULL = 260.0;
    private static final double SIDEBAR_SMALL = 65.0;
    private static final double QUICK_WIDTH = 260.0;

    @FXML
    public void initialize() {
        setActive(btnDashboard, "Dashboard", "dashboard");

        // Debounced search: wait until user stops typing for 250ms
        searchDebounce.setOnFinished(e -> performSearch(txtSearch.getText()));
        txtSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        // Hide search results on Escape key
        txtSearch.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchResults();
            }
        });

        // Hide search results when focus is lost (with small delay for click handling)
        txtSearch.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                PauseTransition delay = new PauseTransition(Duration.millis(200));
                delay.setOnFinished(e -> hideSearchResults());
                delay.play();
            }
        });

        // Initialize search popup
        buildSearchPopup();

        loadQuickActionsPanel();
        loadQuickStats();
        // Register this controller with ViewFactory for cross-controller communication
        ViewModel.INSTANCE.getViewsFactory().setLayoutController(this);

        // Start connectivity monitoring
        startConnectivityMonitor();
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
    @FXML private void onServices()    { setActive(btnServices, "Services", "services"); }
    @FXML private void onSales()       { setActive(btnSales, "Credit Sales", "credit-sales"); }
    @FXML private void onTyreExports() { setActive(btnTyreExports, "Tyre Exports", "tyre-exports"); }
    @FXML private void onExpenses()    { setActive(btnExpenses, "Expenses", "expenses"); }
    @FXML private void onSalary()      { setActive(btnSalary, "Salary Management", "salary"); }
    @FXML private void onAnalytics()   { setActive(btnAnalytics, "Analytics Charts", "analytics"); }
    @FXML private void onReports()     { setActive(btnReports, "Reports", "reports"); }

    @FXML private void onQuickActions() {
        Stage ownerStage = (Stage) btnQuickActions.getScene().getWindow();
        QuickServicePresetsController controller = ViewModel.INSTANCE.getViewsFactory()
                .getForm("form/quick-service-presets-dialog", ownerStage);
        if (controller != null) {
            controller.setOnSaved(() -> {
                loadQuickActionsPanel();
                loadQuickStats();
                ViewModel.INSTANCE.getViewsFactory().refreshDashboardQuickActions();
                ViewModel.INSTANCE.getViewsFactory().refreshServices();
            });
        }
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

    private String faIconToEmoji(String iconLiteral) {
        if (iconLiteral == null) return "⚡";
        switch (iconLiteral) {
            case "fas-bolt": return "⚡";
            case "fas-wrench": return "🔧";
            case "fas-tools": return "🛠";
            case "fas-cog": case "fas-cogs": return "⚙";
            case "fas-oil-can": return "🛢";
            case "fas-tint": return "💧";
            case "fas-water": return "🌊";
            case "fas-wind": return "💨";
            case "fas-car": return "🚗";
            case "fas-truck": return "🚛";
            case "fas-fire": return "🔥";
            case "fas-fan": return "🌀";
            case "fas-broom": return "🧹";
            case "fas-shield-alt": return "🛡";
            case "fas-battery-full": return "🔋";
            case "fas-temperature-high": return "🌡";
            case "fas-charging-station": return "⚡";
            case "fas-filter": return "🔽";
            default: return "⚡";
        }
    }

    private Button buildQuickServiceButton(String service, double price, String iconLiteral) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("quick-service-btn");

        Label iconLabel = new Label(faIconToEmoji(iconLiteral));
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #f59e0b;");

        VBox textBox = new VBox();
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(service);
        title.getStyleClass().add("service-title");

        Label priceLabel = new Label("Rs. " + String.format("%.0f", price));
        priceLabel.getStyleClass().add("service-price");

        textBox.getChildren().addAll(title, priceLabel);

        HBox content = new HBox(12, iconLabel, textBox);
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
        SyncDataRepository sync = new SyncDataRepository();
        int pendingRows = sync.getTotalUnsyncedCount();
        int pendingDeletions = sync.getPendingDeletions().size();
        int totalPending = pendingRows + pendingDeletions;
        
        if (totalPending == 0) {
            AlertUtil.showInfo("Sync", "Everything is up to date! No records to sync.");
            return;
        }

        boolean proceed = AlertUtil.showConfirmation("Sync Data", "Found " + totalPending + " records to sync (" + pendingRows + " updates, " + pendingDeletions + " deletions). Do you want to proceed?");
        
        if (proceed) {
            String syncInitiationTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String jsonPayload = sync.getSyncPayloadAsJson();
            System.out.println(jsonPayload);
            
            // Show loading or disable button here if desired, using BackgroundTask
            BackgroundTask.runVoid(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/sync")) // TODO: Update with SYNC_API_URL
                            .header("Content-Type", "application/json")
                            // .header("X-API-KEY", "YOUR_API_KEY") // Uncomment if needed
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        sync.markAsSynced(syncInitiationTimestamp);
                        javafx.application.Platform.runLater(() -> {
                            AlertUtil.showInfo("Sync Complete", "Successfully synced " + totalPending + " records to the cloud.");
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            AlertUtil.showError("Sync Failed", "Server responded with status: " + response.statusCode());
                        });
                    }
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        AlertUtil.showError("Sync Error", "Failed to connect to the server: " + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }, null);
        }
    }

    // ── Global Search Implementation ──

    private void buildSearchPopup() {
        // Wrap the results list in a ScrollPane inside a styled VBox
        ScrollPane scrollPane = new ScrollPane(searchResultsList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("search-results-scroll");

        VBox container = new VBox(scrollPane);
        container.getStyleClass().add("search-results-popup");
        container.setPrefWidth(360);
        container.setMaxHeight(400);
        container.getStylesheets().add(
                getClass().getResource("/com/gui/kline/css/search-dropdown.css").toExternalForm()
        );

        searchPopup.getContent().add(container);
        searchPopup.setAutoHide(false); // We control hiding manually
    }

    private void hideSearchResults() {
        searchPopup.hide();
        searchResultsList.getChildren().clear();
    }

    private void showSearchResults() {
        if (searchPopup.isShowing()) return;

        // Position the popup below the search field
        Stage stage = (Stage) txtSearch.getScene().getWindow();
        double x = txtSearch.localToScreen(txtSearch.getBoundsInLocal()).getMinX();
        double y = txtSearch.localToScreen(txtSearch.getBoundsInLocal()).getMaxY() + 4;
        searchPopup.show(stage, x, y);
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            hideSearchResults();
            return;
        }

        String queryTrimmed = query.trim();
        String searchTerm = "%" + queryTrimmed + "%";

        // Run all DB queries on a background thread to prevent UI lag
        BackgroundTask.run(
                () -> {
                    List<SearchResult> allResults = new ArrayList<>();
                    searchProducts(searchTerm, allResults);
                    searchWorkers(searchTerm, allResults);
                    searchInvoices(searchTerm, allResults);
                    searchCreditSales(searchTerm, allResults);
                    searchTyreExports(searchTerm, allResults);
                    searchServices(searchTerm, allResults);
                    return allResults;
                },
                results -> renderSearchResults(results, queryTrimmed)
        );
    }

    private void renderSearchResults(List<SearchResult> results, String query) {
        searchResultsList.getChildren().clear();

        if (results.isEmpty()) {
            Label noResults = new Label("No results found for \"" + query + "\"");
            noResults.getStyleClass().add("search-no-results");
            searchResultsList.getChildren().add(noResults);
            showSearchResults();
            return;
        }

        // Group results by category
        String currentCategory = "";
        for (SearchResult result : results) {
            if (!result.category.equals(currentCategory)) {
                currentCategory = result.category;
                Label sectionHeader = new Label(currentCategory.toUpperCase());
                sectionHeader.getStyleClass().add("search-section-header");
                searchResultsList.getChildren().add(sectionHeader);
            }
            searchResultsList.getChildren().add(buildSearchResultItem(result));
        }

        showSearchResults();
    }

    private HBox buildSearchResultItem(SearchResult result) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("search-result-item");

        // Icon based on category
        FontIcon icon = new FontIcon(getIconForCategory(result.category));
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.web(getIconColorForCategory(result.category)));
        icon.getStyleClass().add("search-result-icon");

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(result.title);
        title.getStyleClass().add("search-result-title");

        Label subtitle = new Label(result.subtitle);
        subtitle.getStyleClass().add("search-result-subtitle");

        textBox.getChildren().addAll(title, subtitle);

        item.getChildren().addAll(icon, textBox);

        // Click handler to navigate
        item.setOnMouseClicked(e -> {
            hideSearchResults();
            txtSearch.clear();
            navigateToSearchResult(result);
        });

        // Set cursor to hand
        item.setCursor(javafx.scene.Cursor.HAND);

        return item;
    }

    private void navigateToSearchResult(SearchResult result) {
        switch (result.targetPage) {
            case "inventory":
                setActive(btnInventory, "Inventory", "inventory");
                break;
            case "workers":
                setActive(btnWorkers, "Workers", "workers");
                break;
            case "invoices":
                setActive(btnInvoices, "Invoices & Billing", "invoices");
                break;
            case "credit-sales":
                setActive(btnSales, "Credit Sales", "credit-sales");
                break;
            case "tyre-exports":
                setActive(btnTyreExports, "Tyre Exports", "tyre-exports");
                break;
            case "services":
                setActive(btnServices, "Services", "services");
                break;
            default:
                break;
        }
    }

    private String getIconForCategory(String category) {
        switch (category) {
            case "Products": return "fas-box";
            case "Workers": return "fas-users";
            case "Invoices": return "fas-file-alt";
            case "Credit Sales": return "fas-shopping-cart";
            case "Tyre Exports": return "fas-truck";
            case "Services": return "fas-tools";
            default: return "fas-search";
        }
    }

    private String getIconColorForCategory(String category) {
        switch (category) {
            case "Products": return "#3b82f6";
            case "Workers": return "#10b981";
            case "Invoices": return "#f59e0b";
            case "Credit Sales": return "#8b5cf6";
            case "Tyre Exports": return "#06b6d4";
            case "Services": return "#ef4444";
            default: return "#6b7280";
        }
    }

    // ── Individual Search Methods ──

    private void searchProducts(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, name, category, product_code, brand, stock FROM products " +
                "WHERE name LIKE ? OR product_code LIKE ? OR brand LIKE ? OR category LIKE ? " +
                "OR supplier_name LIKE ? OR description LIKE ? OR vehicle_type LIKE ? OR material LIKE ? " +
                "ORDER BY name LIMIT 8";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 8; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String category = rs.getString("category");
                    String code = rs.getString("product_code");
                    String brand = rs.getString("brand");
                    int stock = rs.getInt("stock");
                    String subtitle = (brand != null && !brand.isBlank() ? brand + " | " : "") +
                            (category != null ? category : "") +
                            " | Stock: " + stock;
                    String displayCode = (code != null && !code.isBlank()) ? code + " - " : "";
                    results.add(new SearchResult("Products", displayCode + name, subtitle, id, "inventory"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search products error: " + ex.getMessage());
        }
    }

    private void searchWorkers(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, name, phone, role FROM workers " +
                "WHERE name LIKE ? OR phone LIKE ? OR role LIKE ? " +
                "ORDER BY name LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone");
                    String role = rs.getString("role");
                    String subtitle = (role != null ? role : "") + (phone != null ? " | " + phone : "");
                    results.add(new SearchResult("Workers", name, subtitle, id, "workers"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search workers error: " + ex.getMessage());
        }
    }

    private void searchInvoices(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, invoice_id, customer, grand_total, status FROM invoices " +
                "WHERE invoice_id LIKE ? OR customer LIKE ? " +
                "ORDER BY invoice_date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 2; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String invoiceId = rs.getString("invoice_id");
                    String customer = rs.getString("customer");
                    double total = rs.getDouble("grand_total");
                    String status = rs.getString("status");
                    String displayId = (invoiceId != null && !invoiceId.isBlank()) ? invoiceId : id.substring(0, 8);
                    String subtitle = (customer != null ? customer : "N/A") +
                            " | Rs. " + String.format("%.0f", total) +
                            (status != null ? " | " + status : "");
                    results.add(new SearchResult("Invoices", "Invoice " + displayId, subtitle, id, "invoices"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search invoices error: " + ex.getMessage());
        }
    }

    private void searchCreditSales(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, credit_id, customer_name, amount, status FROM credit_sales " +
                "WHERE customer_name LIKE ? OR credit_id LIKE ? OR customer LIKE ? " +
                "ORDER BY sale_date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String creditId = rs.getString("credit_id");
                    String customer = rs.getString("customer_name");
                    double amount = rs.getDouble("amount");
                    String status = rs.getString("status");
                    String displayId = (creditId != null && !creditId.isBlank()) ? creditId : id.substring(0, 8);
                    String subtitle = (customer != null ? customer : "N/A") +
                            " | Rs. " + String.format("%.0f", amount) +
                            (status != null ? " | " + status : "");
                    results.add(new SearchResult("Credit Sales", "Sale " + displayId, subtitle, id, "credit-sales"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search credit sales error: " + ex.getMessage());
        }
    }

    private void searchTyreExports(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, export_id, company, operation, total_amount FROM tyre_exports " +
                "WHERE company LIKE ? OR export_id LIKE ? OR operation LIKE ? " +
                "ORDER BY export_date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String exportId = rs.getString("export_id");
                    String company = rs.getString("company");
                    String operation = rs.getString("operation");
                    double total = rs.getDouble("total_amount");
                    String displayId = (exportId != null && !exportId.isBlank()) ? exportId : id.substring(0, 8);
                    String subtitle = (company != null ? company : "N/A") +
                            (operation != null ? " | " + operation : "") +
                            " | Rs. " + String.format("%.0f", total);
                    results.add(new SearchResult("Tyre Exports", "Export " + displayId, subtitle, id, "tyre-exports"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search tyre exports error: " + ex.getMessage());
        }
    }

    private void searchServices(String searchTerm, List<SearchResult> results) {
        String sql = "SELECT id, name, price, remark FROM services " +
                "WHERE name LIKE ? OR remark LIKE ? " +
                "ORDER BY service_date DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 2; i++) ps.setString(i, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    String remark = rs.getString("remark");
                    String subtitle = "Rs. " + String.format("%.0f", price) +
                            (remark != null && !remark.isBlank() ? " | " + remark : "");
                    results.add(new SearchResult("Services", name, subtitle, id, "services"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Search services error: " + ex.getMessage());
        }
    }

    // ── Connectivity Monitor ──

    private void startConnectivityMonitor() {
        // Initial check
        checkConnectivity();

        // Schedule periodic checks on a background thread to avoid blocking the JavaFX pulse thread
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "connectivity-monitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::checkConnectivity, 30, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkConnectivity() {
        if (connectionBulb == null) return;

        boolean isOnline = false;
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();


            // Fallback: Google connectivity check endpoint
            if (!isOnline) {
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://clients3.google.com/generate_204"))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .GET()
                        .build();
                java.net.http.HttpResponse<Void> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
                isOnline = response.statusCode() == 204;
            }
        } catch (Exception ex) {
            isOnline = false;
        }

        // Update UI on JavaFX Application Thread
        final boolean online = isOnline;
        javafx.application.Platform.runLater(() -> {
            if (online) {
                connectionBulb.setIconColor(javafx.scene.paint.Color.web("#10b981")); // green
                connectionBulb.setIconLiteral("fas-circle");
            } else {
                connectionBulb.setIconColor(javafx.scene.paint.Color.web("#ef4444")); // red
                connectionBulb.setIconLiteral("fas-circle");
            }
        });
    }

    // ── Helper class for search results ──

    private static class SearchResult {
        final String category;
        final String title;
        final String subtitle;
        final String entityId;
        final String targetPage;

        SearchResult(String category, String title, String subtitle, String entityId, String targetPage) {
            this.category = category;
            this.title = title;
            this.subtitle = subtitle;
            this.entityId = entityId;
            this.targetPage = targetPage;
        }
    }
}

