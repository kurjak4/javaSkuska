module com.example.skuska_ak {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.skuska_ak to javafx.fxml;
    exports com.example.skuska_ak;
}