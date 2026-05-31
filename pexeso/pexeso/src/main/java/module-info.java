module com.example.pexeso {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pexeso to javafx.fxml;
    exports com.example.pexeso;
}