package com.gui.kline.controller.form;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class NewExportDialogController implements Initializable {

    @FXML private TextField        txtCompany;
    @FXML private TextField        txtTyreCount;
    @FXML private TextField        txtServiceFee;
    @FXML private TextField        txtCustomerPaid;
    @FXML private TextField        txtCompanyPrice;
    @FXML private DatePicker       dpDateSent;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button           btnCancel;
    @FXML private Button           btnSave;

    private Consumer<ExportResult> onSave;

    public void setOnSave(Consumer<ExportResult> onSave) {
        this.onSave = onSave;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbStatus.setItems(FXCollections.observableArrayList(
                "PENDING", "IN TRANSPORT", "DELIVERED", "PAID"
        ));
        cmbStatus.getSelectionModel().selectFirst();
        dpDateSent.setValue(LocalDate.now());
    }

    @FXML
    private void handleSave(ActionEvent e) {
        ExportResult result = new ExportResult(
                txtCompany.getText().trim(),
                parseTyres(),
                parseDouble(txtServiceFee),
                parseDouble(txtCustomerPaid),
                parseDouble(txtCompanyPrice),
                dpDateSent.getValue() != null ? dpDateSent.getValue() : LocalDate.now(),
                cmbStatus.getValue()
        );
        if (onSave != null) onSave.accept(result);
        closeStage();
    }

    @FXML
    private void handleCancel(ActionEvent e) {
        closeStage();
    }

    private int    parseTyres()              { try { return Integer.parseInt(txtTyreCount.getText().trim()); } catch (Exception ex) { return 0; } }
    private double parseDouble(TextField f)  { try { return Double.parseDouble(f.getText().trim()); }          catch (Exception ex) { return 0.0; } }
    private void   closeStage()              { ((Stage) btnCancel.getScene().getWindow()).close(); }

    public record ExportResult(
            String    company,
            int       tyres,
            double    serviceFee,
            double    custPrice,
            double    compPrice,
            LocalDate date,
            String    status
    ) {}
}