package com.gui.kline.controller;

import com.gui.kline.controller.form.ExpenseDialogController;
import com.gui.kline.data.DatabaseManager;
import com.gui.kline.data.SyncQueueRepository;
import com.gui.kline.models.Expense;
import com.gui.kline.models.ViewModel;
import com.gui.kline.utils.JsonUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ExpenseController implements Initializable {

    @FXML
    private Button btnAddExpense;

    @FXML
    private TableColumn<Expense, String> colDate;

    @FXML
    private TableColumn<Expense, String> colDescription;

    @FXML
    private TableColumn<Expense, String> colCategory;

    @FXML
    private TableColumn<Expense, Double> colAmount;

    @FXML
    private TableColumn<Expense, String> colActions;

    @FXML
    private DatePicker dpFrom;

    @FXML
    private DatePicker dpTo;

    @FXML
    private Label lblTotalExpenses;

    @FXML
    private TableView<Expense> tblExpenses;

    @FXML
    private TextField txtFilter;

    private final ObservableList<Expense> expenses = FXCollections.observableArrayList();
    private FilteredList<Expense> filteredExpenses;
    private final SyncQueueRepository syncQueueRepository = new SyncQueueRepository();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colDate.setCellValueFactory(data -> data.getValue().dateLabelProperty());
        colDescription.setCellValueFactory(data -> data.getValue().descriptionProperty());
        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colAmount.setCellValueFactory(data -> data.getValue().amountProperty().asObject());

        // Set up actions column with delete button
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button();
            {
                deleteBtn.setGraphic(new FontIcon("fas-trash"));
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    deleteExpense(expense);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Rs. " + String.format("%,.2f", item));
            }
        });

        filteredExpenses = new FilteredList<>(expenses, item -> true);
        tblExpenses.setItems(filteredExpenses);

        dpFrom.setValue(LocalDate.now().minusDays(30));
        dpTo.setValue(LocalDate.now());

        loadExpenses();
        applyFilters();
        refreshTotals();
    }

    @FXML
    void handleAddExpense(ActionEvent event) {
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ExpenseDialogController form = ViewModel.INSTANCE.getViewsFactory().getForm("form/expense-dialog", ownerStage);
        if (form != null) {
            form.setOnSaved(() -> {
                loadExpenses();
                applyFilters();
                refreshTotals();
            });
        }
    }

    @FXML
    void handleDateFilter(ActionEvent event) {
        applyFilters();
        refreshTotals();
    }

    @FXML
    void handleFilter(javafx.scene.input.KeyEvent event) {
        applyFilters();
        refreshTotals();
    }

    private void loadExpenses() {
        expenses.clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, expense_date, description, category, amount FROM expenses ORDER BY expense_date DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    Date date = rs.getDate("expense_date");
                    String description = rs.getString("description");
                    String category = rs.getString("category");
                    double amount = rs.getDouble("amount");

                    Expense expense = new Expense(
                            date != null ? date.toLocalDate() : LocalDate.now(),
                            description,
                            category,
                            amount
                    );
                    expense.setId(id);
                    expenses.add(expense);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error loading expenses: " + ex.getMessage());
        }

        expenses.sort(Comparator.comparing(Expense::getDate).reversed());
    }

    private void applyFilters() {
        String query = txtFilter.getText() == null ? "" : txtFilter.getText().trim().toLowerCase();
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        filteredExpenses.setPredicate(expense -> {
            if (expense == null) {
                return false;
            }

            boolean matchesQuery = query.isEmpty()
                    || expense.getDescription().toLowerCase().contains(query)
                    || expense.getCategory().toLowerCase().contains(query);

            LocalDate date = expense.getDate();
            boolean matchesDate = true;
            if (from != null && date != null && date.isBefore(from)) {
                matchesDate = false;
            }
            if (to != null && date != null && date.isAfter(to)) {
                matchesDate = false;
            }

            return matchesQuery && matchesDate;
        });
    }

    private void refreshTotals() {
        int count = filteredExpenses.size();
        double total = filteredExpenses.stream()
                .collect(Collectors.summingDouble(Expense::getAmount));

        lblTotalExpenses.setText("Rs. " + String.format("%,.2f", total));
    }

    private void deleteExpense(Expense expense) {
        if (expense == null) {
            return;
        }

        // Get owner window to prevent alert from opening as separate window in full-screen mode
        javafx.stage.Window owner = null;
        if (tblExpenses != null && tblExpenses.getScene() != null) {
            owner = tblExpenses.getScene().getWindow();
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Expense");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this expense?");
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        // Delete from database
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM expenses WHERE id = ?")) {
            ps.setString(1, expense.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error deleting expense: " + ex.getMessage());
            return;
        }

        // Remove from list
        expenses.remove(expense);
        applyFilters();
        refreshTotals();
    }
}