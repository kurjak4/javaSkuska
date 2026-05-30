module com.example.hamusamundo {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.hamusamundo to javafx.fxml;
    exports com.example.hamusamundo;
}