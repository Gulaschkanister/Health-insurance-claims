#!/usr/bin/env bash
# Erzeugt aus den Teilen der Projektdokumentation die Word-Fassungen:
#   - je Teil eine .docx
#   - dazu eine zusammengefuegte Gesamtfassung ueber alle Teile
#
# Aufruf im Ordner Information/dokumentation:   bash erzeugen.sh
#
# Die .md-Dateien sind die Quelle. Die .docx sind abgeleitet und gehoeren
# nicht von Hand bearbeitet - beim naechsten Lauf waere die Aenderung weg.

set -e
cd "$(dirname "$0")"

TEILE=$(ls [0-9][0-9]-*.md 2>/dev/null | grep -v '^00-' | sort)
[ -n "$TEILE" ] || { echo "Keine Teile gefunden."; exit 1; }

echo "Einzelne Teile:"
for f in $TEILE; do
  pandoc "$f" -o "${f%.md}.docx" --resource-path=.:..
  echo "  ${f%.md}.docx"
done

# --- Gesamtfassung ---------------------------------------------------------
# Die Teile werden aneinandergehaengt. Dabei ist zweierlei zu tun:
#
#   1. Der YAML-Kopf jedes Teils muss weg, sonst stolpert pandoc ueber den
#      zweiten.
#   2. Vor jeden Teil kommt eine Ueberschrift "# Teil NN — Titel", damit die
#      Teile Kapitel eines Dokuments werden.
#
# Eine Verschiebung der Ueberschriften ist NICHT noetig: die Abschnitte in den
# Teilen beginnen bei "##" und liegen damit richtig unter der Teilueberschrift.
# Hier stand einmal eine Verschiebung, und sie war falsch - die Abschnitte
# landeten auf "###" und liessen eine Ebene aus.

GESAMT=$(mktemp)

{
  echo '---'
  echo 'title: "GKV-Abrechnung — Projektdokumentation"'
  echo 'subtitle: "Von der Vision bis zum Abschluss"'
  echo 'lang: de'
  echo 'toc-title: "Inhaltsverzeichnis"'
  echo '---'
  echo

  for f in $TEILE; do
    NUMMER=$(echo "$f" | cut -d- -f1)
    TITEL=$(sed -n 's/^title: "Teil [0-9]* — \(.*\)"$/\1/p' "$f" | head -1)
    echo "# Teil $NUMMER — ${TITEL:-${f%.md}}"
    echo
    awk '
      NR == 1 && $0 == "---" { imKopf = 1; next }
      imKopf && $0 == "---"  { imKopf = 0; next }
      imKopf                 { next }
      { print }
    ' "$f"
    echo
  done
} > "$GESAMT"

pandoc -f markdown "$GESAMT" -o 00-Gesamtdokumentation.docx \
       --toc --toc-depth=2 --resource-path=.:..
mv "$GESAMT" 00-Gesamtdokumentation.md

echo
echo "Gesamtfassung: 00-Gesamtdokumentation.docx ($(echo "$TEILE" | wc -l) Teile)"

# --- PDF-Fassung -----------------------------------------------------------
# Umgewandelt wird die .docx, nicht die Markdown-Quelle: so ist die PDF
# garantiert dasselbe Dokument, das auch in Word aufgeht.
#
# Unter Windows steht LibreOffice in der WSL. Fehlt es, wird der Schritt
# uebersprungen - die .docx sind das eigentliche Ergebnis, die PDF ist
# Beiwerk fuer alle, die kein Word haben.

WEG=""
if command -v soffice >/dev/null 2>&1; then
  WEG="lokal"
elif command -v wsl >/dev/null 2>&1 && wsl -- bash -lc 'command -v soffice' >/dev/null 2>&1; then
  WEG="wsl"
  # soffice muss in der WSL im Zielordner stehen. Wird der Pfad nur als
  # Argument uebergeben, meldet es "source file could not be loaded" -
  # deshalb der Umweg ueber cd in einer Login-Shell.
  ORDNER=$(wsl -- wslpath -a "$(pwd -W)")
fi

echo
if [ -z "$WEG" ]; then
  echo "Kein LibreOffice gefunden - PDF uebersprungen."
else
  echo "PDF-Fassungen:"
  for d in [0-9][0-9]-*.docx; do
    [ -e "$d" ] || continue
    rm -f "${d%.docx}.pdf"
    if [ "$WEG" = "lokal" ]; then
      soffice --headless --convert-to pdf --outdir . "$d" >/dev/null 2>&1
    else
      wsl -- bash -lc "cd '$ORDNER' && soffice --headless --convert-to pdf --outdir . '$d'" >/dev/null 2>&1
    fi
    if [ -e "${d%.docx}.pdf" ]; then
      echo "  ${d%.docx}.pdf"
    else
      echo "  ${d%.docx}.pdf FEHLGESCHLAGEN - ist die Datei gerade geoeffnet?"
    fi
  done
fi
