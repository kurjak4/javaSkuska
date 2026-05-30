module com.example.skuskaparkovisko {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.skuskaparkovisko to javafx.fxml;
    exports com.example.skuskaparkovisko;
}