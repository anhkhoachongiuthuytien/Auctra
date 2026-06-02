package com.auction.controller;

import com.auction.app.AppContext;
import com.auction.app.SceneNavigator;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.Admin;
import com.auction.util.UiEffects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.util.Locale;

public class ProfileController {
    @FXML private StackPane rootPane;
    @FXML private Label userInitialsLabel;
    @FXML private Label userAvatarBigLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userIdLabel;
    @FXML private Label profileRoleValue;
    @FXML private Label profileEmailStatusValue;
    @FXML private Label profileAvatarStatusValue;
    @FXML private Label navBrandLabel;
    @FXML private VBox metaRowsContainer;

    // Nav items (giữ để highlight active)
    @FXML private javafx.scene.control.Button navHome;
    @FXML private javafx.scene.control.Button navAuctions;
    @FXML private javafx.scene.control.Button navMyAuctions;
    @FXML private javafx.scene.control.Button navProfile;

    private AppContext appContext;
    private SceneNavigator navigator;
    private User currentUser;

    public void init(AppContext appContext, SceneNavigator navigator, User currentUser) {
        this.appContext = appContext;
        this.navigator = navigator;
        this.currentUser = currentUser;

        String name = currentUser.getUsername();
        String initial = name == null || name.isBlank() ? "U"
                : name.substring(0, 1).toUpperCase(Locale.ROOT);
        userInitialsLabel.setText(initial);
        userAvatarBigLabel.setText(initial);
        userNameLabel.setText(name);
        userEmailLabel.setText(currentUser.getEmail());
        if (emailValueLabel != null) {
            emailValueLabel.setText(currentUser.getEmail());
        }
        userRoleLabel.setText(currentUser.getClass().getSimpleName());
        userIdLabel.setText(currentUser.getId());
        updateProfileStatus(currentUser);

        configureRoleAwareNavigation();

        // Active highlight cho nav Profile
        navProfile.getStyleClass().removeAll("nav-link", "nav-link-active");
        navProfile.getStyleClass().add("nav-link-active");

        renderRoleSpecificDetails();
        loadAvatars();
    }

    private void configureRoleAwareNavigation() {
        if (navAuctions == null) {
            return;
        }
        if (currentUser instanceof Seller) {
            navAuctions.setText("Trang người bán");
        } else if (currentUser instanceof Admin) {
            navAuctions.setText("Tổng quan admin");
        } else {
            navAuctions.setText("Phiên đấu giá");
        }
    }

    private void renderRoleSpecificDetails() {
        if (metaRowsContainer == null) return;

        // Giữ lại 2 phần tử đầu là Mã người dùng và Email đăng nhập
        while (metaRowsContainer.getChildren().size() > 2) {
            metaRowsContainer.getChildren().remove(2);
        }

        if (currentUser instanceof Bidder bidder) {
            addMetaRow("Số điện thoại", bidder.getPhoneNumber() != null && !bidder.getPhoneNumber().isBlank()
                    ? bidder.getPhoneNumber() : "Chưa cập nhật");
            addMetaRow("Địa chỉ giao hàng", bidder.getShippingAddress() != null && !bidder.getShippingAddress().isBlank()
                    ? bidder.getShippingAddress() : "Chưa cập nhật");
        } else if (currentUser instanceof Seller seller) {
            addMetaRow("Tên cửa hàng", seller.getStoreName() != null && !seller.getStoreName().isBlank()
                    ? seller.getStoreName() : "Chưa cập nhật");
            addMetaRow("Mô tả cửa hàng", seller.getStoreDescription() != null && !seller.getStoreDescription().isBlank()
                    ? seller.getStoreDescription() : "Chưa cập nhật");
        } else if (currentUser instanceof Admin admin) {
            addMetaRow("Phòng ban", admin.getDepartment() != null && !admin.getDepartment().isBlank()
                    ? admin.getDepartment() : "Chưa cập nhật");
        }
    }

    private void addMetaRow(String key, String value) {
        HBox row = new HBox();
        row.getStyleClass().add("meta-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("meta-key");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("meta-value");

        row.getChildren().addAll(keyLabel, spacer, valueLabel);
        metaRowsContainer.getChildren().add(row);
    }

    @FXML private void goToHome()        throws IOException { navigator.showHome(currentUser); }
    @FXML private void goToAuctions()    throws IOException { navigator.showHome(currentUser); }
    @FXML private void goToMyAuctions()  throws IOException { navigator.showMyAuctions(currentUser); }
    @FXML private void goToProfile()     throws IOException { /* đang ở đây */ }

    @FXML
    private void handleToggleTheme() {
        if (rootPane != null && rootPane.getScene() != null) {
            com.auction.ui.ThemeManager.toggle(rootPane.getScene());
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        navigator.showLogin();
    }

    @FXML
    private void handleEditProfileV2() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-backdrop");

        HBox card = new HBox(24);
        card.getStyleClass().addAll("google-modal-card", "profile-edit-modal");
        card.setMaxWidth(780);
        card.setPadding(new javafx.geometry.Insets(28));
        StackPane.setAlignment(card, javafx.geometry.Pos.CENTER);

        java.util.concurrent.atomic.AtomicReference<java.io.File> selectedAvatar = new java.util.concurrent.atomic.AtomicReference<>();

        VBox avatarColumn = new VBox(14);
        avatarColumn.getStyleClass().add("profile-edit-avatar-panel");
        avatarColumn.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        avatarColumn.setMinWidth(210);

        StackPane avatarPreview = new StackPane();
        avatarPreview.getStyleClass().add("profile-avatar-large");
        Label avatarPreviewLabel = new Label(getInitial(currentUser.getUsername()));
        avatarPreviewLabel.getStyleClass().add("profile-avatar-text");
        avatarPreview.getChildren().add(avatarPreviewLabel);
        renderAvatarPreview(avatarPreview, avatarPreviewLabel, currentUser.getAvatarPath());

        Label avatarTitle = new Label("Ảnh đại diện");
        avatarTitle.getStyleClass().add("admin-card-title");
        Label avatarHint = new Label("Ảnh sẽ hiển thị ở topbar, hồ sơ và bảng quản trị.");
        avatarHint.getStyleClass().add("muted-label");
        avatarHint.setWrapText(true);
        avatarHint.setMaxWidth(180);

        javafx.scene.control.Button chooseAvatarBtn = new javafx.scene.control.Button("Chọn ảnh mới");
        chooseAvatarBtn.getStyleClass().addAll("button-ghost", "button-row");
        chooseAvatarBtn.setMaxWidth(Double.MAX_VALUE);
        chooseAvatarBtn.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Chọn ảnh hồ sơ");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg)",
                            "*.png", "*.jpg", "*.jpeg"));
            java.io.File file = chooser.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) {
                selectedAvatar.set(file);
                renderAvatarPreview(avatarPreview, avatarPreviewLabel, file.getAbsolutePath());
            }
        });
        avatarColumn.getChildren().addAll(avatarPreview, avatarTitle, avatarHint, chooseAvatarBtn);

        VBox formColumn = new VBox(16);
        HBox.setHgrow(formColumn, Priority.ALWAYS);

        Label titleLabel = new Label("Chỉnh sửa hồ sơ");
        titleLabel.getStyleClass().add("google-modal-title");
        Label subtitleLabel = new Label("Cập nhật thông tin cá nhân và ảnh đại diện trong cùng một nơi.");
        subtitleLabel.getStyleClass().add("muted-label");
        subtitleLabel.setWrapText(true);

        TextField usernameField = new TextField(currentUser.getUsername());
        usernameField.getStyleClass().add("google-text-field");
        TextField emailField = new TextField(currentUser.getEmail());
        emailField.getStyleClass().add("google-text-field");

        FlowPane fieldsContainer = new FlowPane(12, 12);
        fieldsContainer.getStyleClass().add("profile-edit-fields");
        fieldsContainer.getChildren().addAll(
                createProfileField("Tên người dùng", usernameField),
                createProfileField("Email", emailField));

        TextField extraField1 = new TextField();
        extraField1.getStyleClass().add("google-text-field");
        TextField extraField2 = new TextField();
        extraField2.getStyleClass().add("google-text-field");

        if (currentUser instanceof Bidder bidder) {
            extraField1.setText(bidder.getPhoneNumber() != null ? bidder.getPhoneNumber() : "");
            extraField2.setText(bidder.getShippingAddress() != null ? bidder.getShippingAddress() : "");
            fieldsContainer.getChildren().addAll(
                    createProfileField("Số điện thoại", extraField1),
                    createProfileField("Địa chỉ giao hàng", extraField2));
        } else if (currentUser instanceof Seller seller) {
            extraField1.setText(seller.getStoreName() != null ? seller.getStoreName() : "");
            extraField2.setText(seller.getStoreDescription() != null ? seller.getStoreDescription() : "");
            fieldsContainer.getChildren().addAll(
                    createProfileField("Tên cửa hàng", extraField1),
                    createProfileField("Mô tả cửa hàng", extraField2));
        } else if (currentUser instanceof Admin admin) {
            extraField1.setText(admin.getDepartment() != null ? admin.getDepartment() : "");
            fieldsContainer.getChildren().add(createProfileField("Phòng ban", extraField1));
        }

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Hủy");
        cancelBtn.getStyleClass().addAll("button-ghost", "button-row");
        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("Lưu thay đổi");
        saveBtn.getStyleClass().addAll("button-primary", "button-row");
        buttonRow.getChildren().addAll(cancelBtn, saveBtn);

        formColumn.getChildren().addAll(titleLabel, subtitleLabel, fieldsContainer, buttonRow);
        card.getChildren().addAll(avatarColumn, formColumn);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
        javafx.application.Platform.runLater(usernameField::requestFocus);

        overlay.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlay);
        fadeIn.setToValue(1);
        card.setScaleX(0.94);
        card.setScaleY(0.94);
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(220), card);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();

        Runnable closeModal = () -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), overlay);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> rootPane.getChildren().remove(overlay));
            fadeOut.play();
        };

        cancelBtn.setOnAction(e -> closeModal.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeModal.run();
            }
        });

        saveBtn.setOnAction(e -> {
            String newUsername = usernameField.getText();
            String newEmail = emailField.getText();
            String phone = currentUser instanceof Bidder ? extraField1.getText() : null;
            String address = currentUser instanceof Bidder ? extraField2.getText() : null;
            String storeName = currentUser instanceof Seller ? extraField1.getText() : null;
            String storeDesc = currentUser instanceof Seller ? extraField2.getText() : null;
            String dept = currentUser instanceof Admin ? extraField1.getText() : null;

            if (newUsername == null || newUsername.trim().isEmpty()) {
                UiEffects.showToast(rootPane, "Tên người dùng không được để trống", UiEffects.ToastType.ERROR, 2000);
                return;
            }
            if (newEmail == null || newEmail.trim().isEmpty()) {
                UiEffects.showToast(rootPane, "Email không được để trống", UiEffects.ToastType.ERROR, 2000);
                return;
            }

            try {
                String avatarPath = selectedAvatar.get() == null ? null : copyAvatarToStorage(selectedAvatar.get());
                User updated = appContext.getGateway().updateUser(
                        currentUser.getId(), newUsername, newEmail,
                        address, phone, storeName, storeDesc, dept, avatarPath);
                this.currentUser = updated;
                updateProfileHeader(updated);
                renderRoleSpecificDetails();
                loadAvatars();
                closeModal.run();
                UiEffects.showToast(rootPane, "Cập nhật hồ sơ thành công",
                        UiEffects.ToastType.SUCCESS, 2000);
            } catch (Exception ex) {
                UiEffects.showToast(rootPane, "Lỗi: " + ex.getMessage(),
                        UiEffects.ToastType.ERROR, 2400);
            }
        });
    }

    private VBox createProfileField(String label, TextField field) {
        VBox box = new VBox(6);
        box.getStyleClass().add("google-field-container");
        box.setPrefWidth(230);
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("google-field-label");
        field.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(fieldLabel, field);
        return box;
    }

    private void renderAvatarPreview(StackPane container, Label fallbackLabel, String avatarPath) {
        container.getChildren().removeIf(node -> node instanceof javafx.scene.image.ImageView);
        fallbackLabel.setVisible(true);
        if (avatarPath == null || avatarPath.isBlank()) {
            return;
        }
        java.io.File file = new java.io.File(avatarPath);
        if (!file.exists()) {
            return;
        }
        try {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(file.toURI().toString(), 96, 96, false, true));
            imageView.setFitWidth(96);
            imageView.setFitHeight(96);
            imageView.setPreserveRatio(false);
            imageView.setClip(new javafx.scene.shape.Circle(48, 48, 48));
            fallbackLabel.setVisible(false);
            container.getChildren().add(imageView);
        } catch (Exception ignored) {
            fallbackLabel.setVisible(true);
        }
    }

    private String getInitial(String username) {
        return username == null || username.isBlank()
                ? "U"
                : username.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String copyAvatarToStorage(java.io.File file) throws IOException {
        java.nio.file.Path avatarsDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".auctionx", "avatars");
        java.nio.file.Files.createDirectories(avatarsDir);
        java.nio.file.Path targetPath = avatarsDir.resolve(currentUser.getId() + ".png");
        java.nio.file.Files.copy(file.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }

    private void updateProfileHeader(User user) {
        userNameLabel.setText(user.getUsername());
        userEmailLabel.setText(user.getEmail());
        if (emailValueLabel != null) {
            emailValueLabel.setText(user.getEmail());
        }
        String initial = getInitial(user.getUsername());
        userInitialsLabel.setText(initial);
        userAvatarBigLabel.setText(initial);
        userRoleLabel.setText(user.getClass().getSimpleName());
        userIdLabel.setText(user.getId());
        updateProfileStatus(user);
    }

    private void updateProfileStatus(User user) {
        if (profileRoleValue != null) {
            profileRoleValue.setText(user.getClass().getSimpleName());
        }
        if (profileEmailStatusValue != null) {
            profileEmailStatusValue.setText(user.getEmail() == null || user.getEmail().isBlank()
                    ? "Chưa có"
                    : "Đã liên kết");
        }
        if (profileAvatarStatusValue != null) {
            profileAvatarStatusValue.setText(hasUsableAvatar(user) ? "Đã có ảnh" : "Chưa có ảnh");
        }
    }

    private boolean hasUsableAvatar(User user) {
        String avatarPath = user.getAvatarPath();
        return avatarPath != null && !avatarPath.isBlank() && new java.io.File(avatarPath).exists();
    }

    @FXML
    private void handleEditProfile() {
        // Create custom modal backdrop (overlay)
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-backdrop");

        // Create Google-styled dialog card
        VBox card = new VBox(20);
        card.getStyleClass().add("google-modal-card");
        card.setMaxWidth(460);
        card.setPadding(new javafx.geometry.Insets(28));
        StackPane.setAlignment(card, javafx.geometry.Pos.CENTER);

        // Header Title
        Label titleLabel = new Label("Chỉnh sửa hồ sơ");
        titleLabel.getStyleClass().add("google-modal-title");

        // Fields Container
        VBox fieldsContainer = new VBox(16);

        // 1. Tên người dùng field
        VBox nameBox = new VBox(6);
        nameBox.getStyleClass().add("google-field-container");
        Label nameLbl = new Label("Tên người dùng");
        nameLbl.getStyleClass().add("google-field-label");
        TextField usernameField = new TextField(currentUser.getUsername());
        usernameField.getStyleClass().add("google-text-field");
        nameBox.getChildren().addAll(nameLbl, usernameField);

        // 2. Email field
        VBox emailBox = new VBox(6);
        emailBox.getStyleClass().add("google-field-container");
        Label emailLbl = new Label("Email");
        emailLbl.getStyleClass().add("google-field-label");
        TextField emailField = new TextField(currentUser.getEmail());
        emailField.getStyleClass().add("google-text-field");
        emailBox.getChildren().addAll(emailLbl, emailField);

        fieldsContainer.getChildren().addAll(nameBox, emailBox);

        // Subclass-specific fields
        TextField extraField1 = new TextField();
        extraField1.getStyleClass().add("google-text-field");
        TextField extraField2 = new TextField();
        extraField2.getStyleClass().add("google-text-field");

        if (currentUser instanceof Bidder bidder) {
            VBox phoneBox = new VBox(6);
            phoneBox.getStyleClass().add("google-field-container");
            Label phoneLbl = new Label("Số điện thoại");
            phoneLbl.getStyleClass().add("google-field-label");
            extraField1.setText(bidder.getPhoneNumber() != null ? bidder.getPhoneNumber() : "");
            phoneBox.getChildren().addAll(phoneLbl, extraField1);

            VBox addressBox = new VBox(6);
            addressBox.getStyleClass().add("google-field-container");
            Label addressLbl = new Label("Địa chỉ giao hàng");
            addressLbl.getStyleClass().add("google-field-label");
            extraField2.setText(bidder.getShippingAddress() != null ? bidder.getShippingAddress() : "");
            addressBox.getChildren().addAll(addressLbl, extraField2);

            fieldsContainer.getChildren().addAll(phoneBox, addressBox);
        } else if (currentUser instanceof Seller seller) {
            VBox storeNameBox = new VBox(6);
            storeNameBox.getStyleClass().add("google-field-container");
            Label storeNameLbl = new Label("Tên cửa hàng");
            storeNameLbl.getStyleClass().add("google-field-label");
            extraField1.setText(seller.getStoreName() != null ? seller.getStoreName() : "");
            storeNameBox.getChildren().addAll(storeNameLbl, extraField1);

            VBox storeDescBox = new VBox(6);
            storeDescBox.getStyleClass().add("google-field-container");
            Label storeDescLbl = new Label("Mô tả cửa hàng");
            storeDescLbl.getStyleClass().add("google-field-label");
            extraField2.setText(seller.getStoreDescription() != null ? seller.getStoreDescription() : "");
            storeDescBox.getChildren().addAll(storeDescLbl, extraField2);

            fieldsContainer.getChildren().addAll(storeNameBox, storeDescBox);
        } else if (currentUser instanceof Admin admin) {
            VBox deptBox = new VBox(6);
            deptBox.getStyleClass().add("google-field-container");
            Label deptLbl = new Label("Phòng ban");
            deptLbl.getStyleClass().add("google-field-label");
            extraField1.setText(admin.getDepartment() != null ? admin.getDepartment() : "");
            deptBox.getChildren().addAll(deptLbl, extraField1);

            fieldsContainer.getChildren().add(deptBox);
        }

        // Action Buttons Row
        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Hủy");
        cancelBtn.getStyleClass().add("google-btn-cancel");

        javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("Lưu");
        saveBtn.getStyleClass().add("google-btn-save");

        buttonRow.getChildren().addAll(cancelBtn, saveBtn);

        card.getChildren().addAll(titleLabel, fieldsContainer, buttonRow);
        overlay.getChildren().add(card);

        // Add overlay to root StackPane
        rootPane.getChildren().add(overlay);

        // Focus default field
        javafx.application.Platform.runLater(usernameField::requestFocus);

        // Transitions (fade in & scale in)
        overlay.setOpacity(0);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlay);
        fadeIn.setToValue(1);

        card.setScaleX(0.9);
        card.setScaleY(0.9);
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(250), card);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();

        // Close logic
        Runnable closeModal = () -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), overlay);
            fadeOut.setToValue(0);

            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), card);
            scaleOut.setToX(0.95);
            scaleOut.setToY(0.95);

            javafx.animation.ParallelTransition fadeScaleOut = new javafx.animation.ParallelTransition(fadeOut, scaleOut);
            fadeScaleOut.setOnFinished(e -> rootPane.getChildren().remove(overlay));
            fadeScaleOut.play();
        };

        cancelBtn.setOnAction(e -> closeModal.run());

        // Cancel on clicking backdrop (except clicking inside the card)
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeModal.run();
            }
        });

        // Save logic
        saveBtn.setOnAction(e -> {
            String newUsername = usernameField.getText();
            String newEmail = emailField.getText();
            String phone = currentUser instanceof Bidder ? extraField1.getText() : null;
            String address = currentUser instanceof Bidder ? extraField2.getText() : null;
            String storeName = currentUser instanceof Seller ? extraField1.getText() : null;
            String storeDesc = currentUser instanceof Seller ? extraField2.getText() : null;
            String dept = currentUser instanceof Admin ? extraField1.getText() : null;

            if (newUsername == null || newUsername.trim().isEmpty()) {
                UiEffects.showToast(rootPane, "Tên người dùng không được để trống", UiEffects.ToastType.ERROR, 2000);
                return;
            }
            if (newEmail == null || newEmail.trim().isEmpty()) {
                UiEffects.showToast(rootPane, "Email không được để trống", UiEffects.ToastType.ERROR, 2000);
                return;
            }

            try {
                User updated = appContext.getGateway().updateUser(
                        currentUser.getId(), newUsername, newEmail,
                        address, phone, storeName, storeDesc, dept);
                this.currentUser = updated;

                updateProfileHeader(updated);
                renderRoleSpecificDetails();
                loadAvatars();
                closeModal.run();

                UiEffects.showToast(rootPane, "Cập nhật hồ sơ thành công!",
                        UiEffects.ToastType.SUCCESS, 2000);
            } catch (Exception ex) {
                UiEffects.showToast(rootPane, "Lỗi: " + ex.getMessage(),
                        UiEffects.ToastType.ERROR, 2400);
            }
        });
    }

    private void loadAvatars() {
        if (currentUser != null) {
            com.auction.util.UserImageHelper.setupAvatar(userInitialsLabel, currentUser.getId(), currentUser.getAvatarPath());
            com.auction.util.UserImageHelper.setupAvatar(userAvatarBigLabel, currentUser.getId(), currentUser.getAvatarPath());
        }
    }

    @FXML
    private void handleChangeAvatar() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn ảnh hồ sơ");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Ảnh (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
        );
        java.io.File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                String avatarPath = copyAvatarToStorage(file);
                
                String phone = currentUser instanceof Bidder ? ((Bidder) currentUser).getPhoneNumber() : null;
                String address = currentUser instanceof Bidder ? ((Bidder) currentUser).getShippingAddress() : null;
                String storeName = currentUser instanceof Seller ? ((Seller) currentUser).getStoreName() : null;
                String storeDesc = currentUser instanceof Seller ? ((Seller) currentUser).getStoreDescription() : null;
                String dept = currentUser instanceof Admin ? ((Admin) currentUser).getDepartment() : null;

                User updated = appContext.getGateway().updateUser(
                        currentUser.getId(), currentUser.getUsername(), currentUser.getEmail(),
                        address, phone, storeName, storeDesc, dept, avatarPath);
                this.currentUser = updated;

                updateProfileHeader(updated);
                loadAvatars();
                UiEffects.showToast(rootPane, "Đã cập nhật ảnh hồ sơ thành công!", UiEffects.ToastType.SUCCESS, 2000);
            } catch (Exception ex) {
                UiEffects.showToast(rootPane, "Lỗi cập nhật ảnh: " + ex.getMessage(), UiEffects.ToastType.ERROR, 2400);
            }
        }
    }
}
