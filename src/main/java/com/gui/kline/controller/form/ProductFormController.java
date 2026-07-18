package com.gui.kline.controller.form;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import com.gui.kline.models.Product;
import com.gui.kline.utils.AlertUtil;
import com.gui.kline.utils.ImagePathUtil;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ProductFormController implements Initializable {

    @FXML private Label            lblTitle;
    @FXML private Label            lblBadge;
    @FXML private TextField        txtProductName;
    @FXML private TextField        txtProductCode;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField        txtBuyPrice;
    @FXML private TextField        txtSellPrice;
    @FXML private TextField        txtQuantity;
    @FXML private TextField        txtMinStockAlert;
    @FXML private TextField        txtBrand;
    @FXML private TextField        txtDescription;
    @FXML private TextField        txtVehicleType;
    @FXML private TextField        txtMaterial;
    @FXML private TextField        txtSupplierName;
    @FXML private Button           btnChooseImage;
    @FXML private Button           btnClearImage;
    @FXML private ImageView        imgPreview;
    @FXML private VBox             imageGallery;
    @FXML private Button           btnSubmit;
    @FXML private Button           btnCancel;

    private Product           editingProduct = null;
    private Consumer<Product> onSave;
    private java.util.List<String> selectedImagePaths = new java.util.ArrayList<>();

    public void setProduct(Product product) {
        this.editingProduct = product;
        applyEditMode();
    }

    public void setOnSave(Consumer<Product> onSave) {
        this.onSave = onSave;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbCategory.setItems(FXCollections.observableArrayList(
                "Lubricants", "Tyres", "Spare Parts",
                "Batteries", "Filters", "Accessories", "Other"
        ));
        enforceNumeric(txtBuyPrice);
        enforceNumeric(txtSellPrice);
        enforceNumeric(txtQuantity);
        enforceNumeric(txtMinStockAlert);
        
        // Ensure product_images directory exists
        ImagePathUtil.getImageDirectory();
    }

    private void applyEditMode() {
        lblTitle.setText("Edit Product");

        lblBadge.setVisible(true);
        lblBadge.setManaged(true);

        btnSubmit.setText("Update Product");
        btnSubmit.setStyle(
                "-fx-background-color: #f97316; -fx-background-radius: 14; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 14px; -fx-cursor: hand;"
        );

        txtProductName.setText(editingProduct.getName());
        txtProductCode.setText(editingProduct.getCode());
        txtProductCode.setDisable(true);
        cmbCategory.setValue(editingProduct.getCategory());
        txtBuyPrice.setText(String.format("%.2f", editingProduct.getBuyPrice()));
        txtSellPrice.setText(String.format("%.2f", editingProduct.getSellPrice()));
        txtQuantity.setText(String.valueOf(editingProduct.getStock()));
        txtMinStockAlert.setText(String.valueOf(editingProduct.getMinimumStockAlert()));
        txtBrand.setText(editingProduct.getBrand());
        txtDescription.setText(editingProduct.getDescription());
        txtVehicleType.setText(editingProduct.getVehicleType());
        txtMaterial.setText(editingProduct.getMaterial());
        txtSupplierName.setText(editingProduct.getSupplierName());
        
        // Load existing images if available
        if (editingProduct.getImagePaths() != null && !editingProduct.getImagePaths().isEmpty()) {
            selectedImagePaths.addAll(editingProduct.getImagePaths());
            if (!selectedImagePaths.isEmpty()) {
                displayImagePreview(selectedImagePaths.get(0));
                btnClearImage.setVisible(true);
                btnClearImage.setManaged(true);
                refreshImageGallery();
            }
        }
    }

    @FXML
    private void handleSubmit() {
        if (!validate()) return;

        if (editingProduct == null) {
            Product newProduct = new Product(
                    txtProductName.getText().trim(),
                    cmbCategory.getValue(),
                    parseDouble(txtBuyPrice),
                    parseDouble(txtSellPrice),
                    parseInt(txtQuantity)
            );
            newProduct.setCode(txtProductCode.getText().trim());
            newProduct.setMinimumStockAlert(parseInt(txtMinStockAlert));
            newProduct.setBrand(txtBrand.getText().trim());
            newProduct.setDescription(txtDescription.getText().trim());
            newProduct.setVehicleType(txtVehicleType.getText().trim());
            newProduct.setMaterial(txtMaterial.getText().trim());
            newProduct.setSupplierName(txtSupplierName.getText().trim());
            // Add all selected images
            for (String imagePath : selectedImagePaths) {
                newProduct.addImagePath(imagePath);
            }
            if (onSave != null) onSave.accept(newProduct);
            AlertUtil.showSuccess(currentWindow(), "Success", "Product Added Successfully");

        } else {
            editingProduct.setName(txtProductName.getText().trim());
            editingProduct.setCode(txtProductCode.getText().trim());
            editingProduct.setCategory(cmbCategory.getValue());
            editingProduct.setBuyPrice(parseDouble(txtBuyPrice));
            editingProduct.setSellPrice(parseDouble(txtSellPrice));
            editingProduct.setStock(parseInt(txtQuantity));
            editingProduct.setMinimumStockAlert(parseInt(txtMinStockAlert));
            editingProduct.setBrand(txtBrand.getText().trim());
            editingProduct.setDescription(txtDescription.getText().trim());
            editingProduct.setVehicleType(txtVehicleType.getText().trim());
            editingProduct.setMaterial(txtMaterial.getText().trim());
            editingProduct.setSupplierName(txtSupplierName.getText().trim());
            // Clear and add all selected images
            editingProduct.clearImagePaths();
            for (String imagePath : selectedImagePaths) {
                editingProduct.addImagePath(imagePath);
            }
            if (onSave != null) onSave.accept(editingProduct);
            AlertUtil.showSuccess(currentWindow(), "Success", "Product Updated Successfully");
        }

        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Images");
        
        // Add file filters for image types
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"
        );
        fileChooser.getExtensionFilters().add(imageFilter);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        Stage stage = (Stage) btnChooseImage.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File selectedFile : selectedFiles) {
                try {
                    // Copy image to product_images directory with unique name
                    String newImagePath = copyImageToDirectory(selectedFile);
                    selectedImagePaths.add(newImagePath);
                    
                    // Display preview of first image
                    if (selectedImagePaths.size() == 1) {
                        displayImagePreview(newImagePath);
                    }
                    
                    // Show clear button
                    btnClearImage.setVisible(true);
                    btnClearImage.setManaged(true);
                    
                } catch (IOException ex) {
                    showError("Image Error", "Failed to copy image: " + ex.getMessage());
                }
            }
        }
    }
    
    @FXML
    private void handleClearImage() {
        // Delete all selected image files from disk before clearing
        ImagePathUtil.deleteImageFiles(selectedImagePaths);
        selectedImagePaths.clear();
        imgPreview.setImage(null);
        imgPreview.setVisible(false);
        imgPreview.setManaged(false);
        btnClearImage.setVisible(false);
        btnClearImage.setManaged(false);
        imageGallery.setVisible(false);
        imageGallery.setManaged(false);
    }
    
    private void deleteImage(String imagePath) {
        selectedImagePaths.remove(imagePath);
        
        // Delete file from filesystem
        ImagePathUtil.deleteImageFile(imagePath);
        
        // Update preview
        if (selectedImagePaths.isEmpty()) {
            imgPreview.setImage(null);
            imgPreview.setVisible(false);
            imgPreview.setManaged(false);
            imageGallery.setVisible(false);
            imageGallery.setManaged(false);
        } else {
            displayImagePreview(selectedImagePaths.get(0));
            refreshImageGallery();
        }
    }
    
    private void refreshImageGallery() {
        if (imageGallery == null) return;
        
        HBox imagesContainer = (HBox) imageGallery.getChildren().get(1);
        imagesContainer.getChildren().clear();
        
        for (String imagePath : selectedImagePaths) {
            File imageFile = ImagePathUtil.resolve(imagePath);
            if (imageFile != null && imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView thumbView = new ImageView(image);
                    thumbView.setFitWidth(80);
                    thumbView.setFitHeight(80);
                    thumbView.setPreserveRatio(true);
                    thumbView.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
                    
                    // Delete button
                    Button deleteBtn = new Button("✕");
                    deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10; -fx-cursor: hand;");
                    deleteBtn.setOnAction(e -> deleteImage(imagePath));
                    
                    VBox imageItem = new VBox(4, thumbView, deleteBtn);
                    imageItem.setAlignment(Pos.CENTER);
                    
                    imagesContainer.getChildren().add(imageItem);
                } catch (Exception ex) {
                    // Skip invalid images
                }
            }
        }
        
        imageGallery.setVisible(true);
        imageGallery.setManaged(true);
    }

    private boolean validate() {
        StringBuilder errors = new StringBuilder();
        if (txtProductName.getText().trim().isEmpty())
            errors.append("• Product name is required.\n");
        if (txtProductCode.getText().trim().isEmpty())
            errors.append("• Product code is required.\n");
        if (cmbCategory.getValue() == null)
            errors.append("• Please select a category.\n");
        if (txtBuyPrice.getText().trim().isEmpty())
            errors.append("• Buying price is required.\n");
        if (txtSellPrice.getText().trim().isEmpty())
            errors.append("• Selling price is required.\n");
        if (txtQuantity.getText().trim().isEmpty())
            errors.append("• Quantity is required.\n");
        if (txtMinStockAlert.getText().trim().isEmpty())
            errors.append("• Minimum stock alert is required.\n");

        if (!errors.isEmpty()) {
            showError("Validation Error", errors.toString().trim());
            return false;
        }
        if (parseDouble(txtSellPrice) < parseDouble(txtBuyPrice)) {
            showError("Price Error", "Selling price must be greater than buying price.");
            return false;
        }
        if (parseInt(txtMinStockAlert) < 0) {
            showError("Validation Error", "Minimum stock alert must be 0 or greater.");
            return false;
        }
        return true;
    }

    private void enforceNumeric(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) field.setText(oldVal);
        });
    }

    private double parseDouble(TextField field) {
        try { return Double.parseDouble(field.getText().trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInt(TextField field) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private String copyImageToDirectory(File sourceFile) throws IOException {
        // Create unique filename: productId_timestamp.extension
        String extension = getFileExtension(sourceFile.getName());
        String filename = System.currentTimeMillis() + "_" + (editingProduct != null ? editingProduct.getId() : java.util.UUID.randomUUID().toString()) + extension;
        
        Path targetPath = Paths.get(ImagePathUtil.getImageDirectory(), filename);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return absolute on-disk path so it can be resolved regardless of CWD
        return ImagePathUtil.storePath(filename);
    }
    
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf);
    }
    
    private void displayImagePreview(String imagePath) {
        try {
            File imageFile = ImagePathUtil.resolve(imagePath);
            if (imageFile != null && imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString(), 200, 200, true, true, true);
                imgPreview.setImage(image);
                imgPreview.setVisible(true);
                imgPreview.setManaged(true);
            }
        } catch (Exception ex) {
            System.err.println("Failed to load image preview: " + ex.getMessage());
        }
    }
    
    private void closeStage() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private Window currentWindow() {
        return btnCancel != null && btnCancel.getScene() != null
                ? btnCancel.getScene().getWindow()
                : null;
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        if (currentWindow() != null) {
            alert.initOwner(currentWindow());
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.showAndWait();
    }
}