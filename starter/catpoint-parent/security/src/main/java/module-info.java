module security {
    requires java.desktop;
    requires java.sql;
    requires java.prefs;
    requires com.google.common;
    requires miglayout.swing;
    requires gson;
    requires image;

    opens com.udacity.security.data to gson;
    opens com.udacity.security.service;
}