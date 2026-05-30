module com.example.pakoresan {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.pakoresan to javafx.fxml;
    exports com.example.pakoresan;
}