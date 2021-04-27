module converte {
    requires org.slf4j;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires static org.immutables.value;
    requires ffmpeg;
//    requires guava;
    requires vavr;

    opens converte to javafx.fxml;
    exports converte;
}