package de.gkvtransmitter.presentation.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.gkvtransmitter.presentation.UiFactory;
import de.gkvtransmitter.util.AppMessages;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * MenuBuilder vereinheitlicht die Erstellung des Navigationsmenüs.
 *
 * Dies reduziert Code-Duplikation und macht es einfach, neue Menüeinträge
 * oder Menüstrukturen hinzuzufügen.
 */
public class MenuBuilder {
    private final UiFactory componentFactory;
    private final AppMessages messages;
    private final List<MenuItem> invoiceMenuItems = new ArrayList<>();
    private final List<MenuItem> patientMenuItems = new ArrayList<>();
    private final List<MenuItem> selfMenuItems = new ArrayList<>();
    private final List<MenuItem> groupMenuItems = new ArrayList<>();

    public MenuBuilder(UiFactory componentFactory, AppMessages messages) {
        this.componentFactory = componentFactory;
        this.messages = messages;
    }

    /**
     * Fügt ein Rechnungstemplate zum Invoice-Menü hinzu.
     *
     * @param name Der Name des Templates
     * @param onAction Der Handler beim Klick
     * @return this für Fluent API
     */
    public MenuBuilder addInvoiceItem(String name, Runnable onAction) {
        MenuItem item = componentFactory.createMenuItem(name);
        item.setOnAction(event -> onAction.run());
        invoiceMenuItems.add(item);
        return this;
    }

    /**
     * Fügt mehrere Rechnungstemplates auf einmal hinzu.
     *
     * @param templates Map von Template-Name zu Handler
     * @return this für Fluent API
     */
    public MenuBuilder addAllInvoiceItems(Map<String, Runnable> templates) {
        templates.forEach(this::addInvoiceItem);
        return this;
    }

    /**
     * Fügt ein Patientenmenü-Item hinzu.
     *
     * @param label Der Menülabel
     * @param onAction Der Handler
     * @return this für Fluent API
     */
    public MenuBuilder addPatientItem(String label, Runnable onAction) {
        MenuItem item = componentFactory.createMenuItem(label);
        item.setOnAction(event -> onAction.run());
        patientMenuItems.add(item);
        return this;
    }

    /**
     * Fügt ein ServiceProvider-Menü-Item hinzu.
     *
     * @param label Der Menülabel
     * @param onAction Der Handler
     * @return this für Fluent API
     */
    public MenuBuilder addSelfItem(String label, Runnable onAction) {
        MenuItem item = componentFactory.createMenuItem(label);
        item.setOnAction(event -> onAction.run());
        selfMenuItems.add(item);
        return this;
    }

    /**
     * Fügt ein Gruppen-Menü-Item hinzu.
     *
     * @param label Der Menülabel
     * @param onAction Der Handler
     * @return this für Fluent API
     */
    public MenuBuilder addGroupItem(String label, Runnable onAction) {
        MenuItem item = componentFactory.createMenuItem(label);
        item.setOnAction(event -> onAction.run());
        groupMenuItems.add(item);
        return this;
    }

    /**
     * Baut die vollständige MenuBar.
     *
     * @return Die konstruierte MenuBar
     */
    public MenuBar build() {
        Menu invoiceMenu = componentFactory.createMenu(
                messages.get("menu.invoice"),
                invoiceMenuItems.toArray(MenuItem[]::new));

        Menu patientMenu = componentFactory.createMenu(
                messages.get("menu.patient"),
                patientMenuItems.toArray(MenuItem[]::new));

        Menu selfMenu = componentFactory.createMenu(
                messages.get("menu.self"),
                selfMenuItems.toArray(MenuItem[]::new));

        Menu groupMenu = componentFactory.createMenu(
            messages.get("menu.groups"),
            groupMenuItems.toArray(MenuItem[]::new));

        return componentFactory.createMenuBar(invoiceMenu, patientMenu, selfMenu, groupMenu);
    }

    /**
     * Setzt alle Invoice-Items auf einmal.
     */
    public MenuBuilder withInvoiceItems(List<MenuItem> items) {
        invoiceMenuItems.clear();
        invoiceMenuItems.addAll(items);
        return this;
    }

    /**
     * Setzt alle Patient-Items auf einmal.
     */
    public MenuBuilder withPatientItems(List<MenuItem> items) {
        patientMenuItems.clear();
        patientMenuItems.addAll(items);
        return this;
    }

    /**
     * Setzt alle Self-Items auf einmal.
     */
    public MenuBuilder withSelfItems(List<MenuItem> items) {
        selfMenuItems.clear();
        selfMenuItems.addAll(items);
        return this;
    }

    /**
     * Setzt alle Gruppen-Items auf einmal.
     */
    public MenuBuilder withGroupItems(List<MenuItem> items) {
        groupMenuItems.clear();
        groupMenuItems.addAll(items);
        return this;
    }
}
