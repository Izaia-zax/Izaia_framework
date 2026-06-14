#!/bin/bash
# Script de compilation de Process.java et création de zaxus.jar
# A executer depuis le dossier : Framework/zaxus/

set -e

SRC_DIR="src/main"
OUT_DIR="out"
LIB="lib/servlet-api.jar"
JAR_NAME="zaxus.jar"

echo "=== Nettoyage de l'ancien build ==="
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "=== Compilation de Process.java ==="
javac -cp "$LIB" -d "$OUT_DIR" $(find "$SRC_DIR" -name "*.java")

echo "=== Verification des classes compilees ==="
find "$OUT_DIR" -name "*.class"

echo "=== Creation de zaxus.jar ==="
cd "$OUT_DIR"
jar cf "../$JAR_NAME" .
cd ..

echo "=== Contenu de $JAR_NAME ==="
jar tf "$JAR_NAME"

echo "=== Termine ! $JAR_NAME est pret dans $(pwd) ==="