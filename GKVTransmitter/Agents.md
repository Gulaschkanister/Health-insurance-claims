# Agents Notes

## Ziel dieser Session

- Blaupause für Rechnungsvorlagen erstellen, damit neue Abrechnungsarten schneller integrierbar sind.
- Formular-Eingabe durch Vorlagen/Presets weiter vereinfachen.
- UI-Basis über Framework modernisieren, um Stil- und Komponentenaufwand zu reduzieren.

## Entscheidungen

1. **Template-first Ansatz beibehalten**
   - Vorlagen werden als JSON in `resources/invoices` gepflegt.
   - Rendering erfolgt datengetrieben über die bestehende Formularlogik in `View`.

2. **Registrierung zentral halten**
   - Neue Vorlagen werden ausschließlich über `JsonParserFactory.INVOICE_FILES` eingebunden.

3. **UI-Konsistenz erhöhen**
   - AtlantaFX (`PrimerLight`) global aktiviert.
   - Dadurch bessere Standarddarstellung ohne zusätzliche CSS-Komplexität pro Formular.

## Nächste sinnvolle Schritte

- Optional: Inline-Styles in `View.java` schrittweise in zentrale CSS/Theme-Klassen überführen.
- Optional: Eigene Template-Metadaten (z. B. Kategorie, Sichtbarkeit, Sortierung) in Invoice-JSON ergänzen.
- Optional: „Als Vorlage speichern“-Flow für benutzerdefinierte Rechnungsdefaults ergänzen.
