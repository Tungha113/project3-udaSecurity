module com.udacity.catpoint.security {
    requires com.google.common;
    requires com.google.gson;
    requires com.miglayout.swing;
    requires com.udacity.catpoint.image;
    requires java.desktop;
    requires java.prefs;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.application;
    opens com.udacity.catpoint.security.data to com.google.gson;
}