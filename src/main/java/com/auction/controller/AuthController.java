package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthController {
    @FXML
    private TextField Username;
    @FXML
    private PasswordField Password;
    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("Username: " + Username.getText());
        System.out.println("Password: " + Password.getText());
    }
}