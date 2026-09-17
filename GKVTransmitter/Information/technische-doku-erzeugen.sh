#!/usr/bin/env bash
# Erzeugt aus Technische_Dokumentation.md die Word- und die PDF-Fassung.
#
# Aufruf im Ordner Information:   bash technische-doku-erzeugen.sh
#
# Die .md ist die Quelle. .docx und .pdf sind abgeleitet und gehoeren nicht
# von Hand bearbeitet - beim naechsten Lauf waere die Aenderung weg.
#
# --- Warum es dieses Skript neben dokumentation/erzeugen.sh gibt ------------
# erzeugen.sh setzt die 23-teilige Projektdokumentation und nimmt fuer die PDF
# den Umweg ueber LibreOffice: .md -> .docx -> .pdf, damit die PDF garantiert
# dasselbe Dokument ist, das auch in Word aufgeht.
#
# Hier geht die PDF direkt aus der Markdown-Quelle ueber XeLaTeX. Der Grund ist
# schlicht, dass auf diesem Rechner kein LibreOffice steht. Die Folge ist zu
# kennen: .docx und .pdf entstehen auf zwei verschiedenen Wegen und koennen im
# Umbruch voneinander abweichen. Inhaltlich sind sie dieselbe Quelle.
#
# --- Was gebraucht wird -----------------------------------------------------
#   pandoc    ~/.local/opt/pandoc-3.11, verlinkt nach ~/.local/bin
#             (portables Archiv von github.com/jgm/pandoc/releases, kein root)
#   xelatex   TinyTeX unter ~/.TinyTeX, verlinkt nach ~/.local/bin
#             (Pakete bei Bedarf nachziehen: tlmgr install <name>)
#   Pakete    float, pdflscape, seqsplit - siehe pdf-kopf.tex
#
# Die Diagramme werden hier NICHT neu erzeugt. Einzeln, wenn eine .puml sich
# geaendert hat:   plantuml -tpng D05_Klassen_Persistenz.puml

set -e
cd "$(dirname "$0")"

# TinyTeX und das portable pandoc liegen im Benutzerprofil und sind je nach
# Shell nicht im PATH.
export PATH="$HOME/.local/bin:$HOME/.local/opt/pandoc-3.11/bin:$HOME/.TinyTeX/bin/x86_64-linux:$PATH"

QUELLE="Technische_Dokumentation.md"
[ -f "$QUELLE" ] || { echo "$QUELLE nicht gefunden."; exit 1; }

command -v pandoc >/dev/null 2>&1 || {
  echo "pandoc fehlt. Portables Archiv entpacken nach ~/.local/opt/ und nach"
  echo "~/.local/bin verlinken - fuer beides braucht es kein root."
  exit 1
}

# --- Word-Fassung ----------------------------------------------------------
# --resource-path=. weil die Bilder neben der Quelle liegen, nicht darunter.
pandoc "$QUELLE" -o "${QUELLE%.md}.docx" \
       --resource-path=. --toc --toc-depth=2
echo "  ${QUELLE%.md}.docx"

# --- PDF-Fassung -----------------------------------------------------------
# mainfont: die Grundschrift muss die deutschen Umlaute und die Gedankenstriche
# tragen, die im Text durchgaengig vorkommen.
#
# papersize: ohne Angabe setzt pandoc US Letter. Das faellt im Bildschirmlauf
# nicht auf und beim Ausdrucken sofort. Die .docx braucht die Angabe nicht -
# sie legt gar keine Seitengroesse fest, Word nimmt dort die des Systems.
if ! command -v xelatex >/dev/null 2>&1; then
  echo
  echo "xelatex fehlt - PDF uebersprungen. Die .docx ist erzeugt."
  echo "TinyTeX einrichten: https://yihui.org/tinytex/ (ohne root)"
  exit 0
fi

pandoc "$QUELLE" -o "${QUELLE%.md}.pdf" \
       --resource-path=. --toc --toc-depth=2 \
       --pdf-engine=xelatex \
       -V papersize=a4 \
       -V geometry:margin=2.5cm \
       -V mainfont="DejaVu Serif" \
       -H pdf-kopf.tex
echo "  ${QUELLE%.md}.pdf"
