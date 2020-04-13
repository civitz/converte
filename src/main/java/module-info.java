module converte {
    requires org.slf4j;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires static org.immutables.value;
    requires vavr;
    requires ffmpeg;
    requires guava;
    requires log4j;

    exports converte;
    opens converte;
}