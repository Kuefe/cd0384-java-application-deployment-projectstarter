module security {
    requires java.desktop;
    requires java.sql;
    requires com.google.common;
    requires miglayout.swing;
    requires gson;
    requires java.prefs;
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;

    opens com.udacity.security.data to gson;
}