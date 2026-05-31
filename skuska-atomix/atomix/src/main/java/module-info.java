module com.example.atomix {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.atomix to javafx.fxml;
    exports com.example.atomix;
}