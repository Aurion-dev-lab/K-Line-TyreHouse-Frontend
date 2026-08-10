package com.gui.kline.view;

import com.gui.kline.controller.DashboardController;
import com.gui.kline.controller.LayoutController;
import com.gui.kline.controller.ReportsController;
import com.gui.kline.controller.ServicesController;
import com.gui.kline.controller.CreditCustomersController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ViewFactory {
    // Storage for primary stage and last opened dialog stage
    private Stage primaryStage;
    private Stage lastDialogStage;
    private LayoutController layoutController;
    private DashboardController dashboardController;
    private ServicesController servicesController;
    private ReportsController reportsController;
    private CreditCustomersController creditCustomersController;

    public void setCreditCustomersController(CreditCustomersController controller) {
        this.creditCustomersController = controller;
    }

    public void refreshCreditCustomers() {
        if (creditCustomersController != null) {
            creditCustomersController.loadData();
        }
    }
    
    public void refreshDashboard() {
        if (dashboardController != null) {
            dashboardController.refreshData();
        }
    }
    
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public void setLayoutController(LayoutController controller) {
        this.layoutController = controller;
    }
    
    public LayoutController getLayoutController() {
        return layoutController;
    }

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    public void refreshDashboardQuickActions() {
        if (dashboardController != null) {
            dashboardController.refreshQuickActions();
        }
    }

    public void setServicesController(ServicesController controller) {
        this.servicesController = controller;
    }

    public void refreshServices() {
        if (servicesController != null) {
            servicesController.refreshData();
        }
    }

    public void setReportsController(ReportsController controller) {
        this.reportsController = controller;
    }

    public void refreshReports() {
        if (reportsController != null) {
            reportsController.refresh();
        }
    }
    
    public void updateQuickStats() {
        if (layoutController != null) {
            layoutController.loadQuickStats();
        }
    }
    
    public void getView(String view) {
        getView(view, primaryStage);
    }
    
/*    public void getView(String view, Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gui/kline/view/" + view + ".fxml"));
            Parent root = loader.load();

            Stage stage;
            if (view.equals("main-layout") && primaryStage != null) {
                // Reuse the real primary stage so window ownership stays consistent
                // and the application only exits when this window is closed.
                stage = primaryStage;
            } else {
                stage = new Stage();
                stage.setTitle("K-Line - " + view);
            }

            // Use provided owner or fall back to primary stage
            Stage actualOwner = ownerStage != null ? ownerStage : primaryStage;

            // If owner stage is available, make this window a child of it
            if (actualOwner != null && !view.equals("main-layout")) {
                stage.initOwner(actualOwner);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            // Store reference
            lastDialogStage = stage;
        } catch (Exception e) {
            System.out.println("Error loading view: " + view + " - " + e.getMessage());
            e.printStackTrace();
        }
    }*/

    private java.net.URL resolveFxmlUrl(String viewOrPage) {
        String path = viewOrPage.startsWith("/") ? viewOrPage : "/com/gui/kline/view/" + viewOrPage + ".fxml";
        java.net.URL url = ViewFactory.class.getResource(path);
        if (url == null) {
            url = getClass().getResource(path);
        }
        if (url == null && path.startsWith("/")) {
            url = Thread.currentThread().getContextClassLoader().getResource(path.substring(1));
        }
        if (url == null) {
            System.err.println("Could not resolve FXML resource for path: " + path);
        }
        return url;
    }

    public void getView(String view, Stage ownerStage) {
        try {
            java.net.URL resource = resolveFxmlUrl(view);
            if (resource == null) {
                System.err.println("Cannot load view: " + view + " because FXML resource is missing.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage stage;
            if (view.equals("main-layout") && primaryStage != null) {
                stage = primaryStage;
            } else {
                stage = new Stage();
                stage.setTitle("K-Line - " + view);
            }

            Stage actualOwner = ownerStage != null ? ownerStage : primaryStage;

            if (actualOwner != null && !view.equals("main-layout")) {
                stage.initOwner(actualOwner);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Maximize only the main application window to fill the screen
            if (view.equals("main-layout")) {
                stage.setMaximized(true);
            }

            stage.show();

            lastDialogStage = stage;
        } catch (Exception e) {
            System.out.println("Error loading view: " + view + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Node getPage(String page){
        try{
            java.net.URL resource = resolveFxmlUrl(page);
            if (resource == null) {
                System.err.println("Cannot load page: " + page + " because FXML resource is missing.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            return loader.load();
        }catch (Exception e){
            System.err.println("Error loading page: " + page + " - " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getForm(String view, Stage ownerStage) {
        try {
            java.net.URL resource = resolveFxmlUrl(view);
            if (resource == null) {
                System.err.println("Cannot load form: " + view + " because FXML resource is missing.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            T controller = loader.getController();

            Stage stage = new Stage();

            Stage actualOwner = ownerStage != null ? ownerStage : primaryStage;

            if (actualOwner != null) {
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initOwner(actualOwner);
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.initStyle(StageStyle.DECORATED);

            stage.show();

            if (actualOwner != null) {
                stage.setX(actualOwner.getX() + (actualOwner.getWidth() - stage.getWidth()) / 2);
                stage.setY(actualOwner.getY() + (actualOwner.getHeight() - stage.getHeight()) / 2);
            } else {
                stage.centerOnScreen();
            }

            lastDialogStage = stage;

            return controller;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // New method to get the last opened dialog stage
    public Stage getLastDialogStage() {
        return lastDialogStage;
    }
}