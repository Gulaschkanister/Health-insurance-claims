#!/usr/bin/env bash
# Baut gkv-core und startet danach die JavaFX-Oberflaeche.
#
# "mvn -pl gkv-ui javafx:run" allein bricht mit
# "Could not find artifact de.gkv:gkv-core" ab, wenn der Kern noch nie oder
# nicht seit der letzten Aenderung ins lokale Repository installiert wurde
# (README, Abschnitt "Bauen und starten"). Dieses Skript nimmt den Schritt
# bei jedem Start ab, statt ihn von Hand merken zu muessen.
#
# Aufruf im Projektordner GKVTransmitter:   bash run.sh

set -e
cd "$(dirname "$0")"

mvn -q -pl gkv-core install -DskipTests
mvn -pl gkv-ui javafx:run
