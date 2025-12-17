#!/bin/bash

SRC_ICON="$(pwd)/assets/icon.png"
RES_DIR="$(pwd)/android/app/src/main/res"

resize_and_copy() {
  SIZE=$1
  FOLDER=$2
  mkdir -p "$RES_DIR/$FOLDER"
  echo "Resizing to $SIZE in $FOLDER"
  rm -f "$RES_DIR/$FOLDER/ic_launcher.webp"
  rm -f "$RES_DIR/$FOLDER/ic_launcher_round.webp"
  rm -f "$RES_DIR/$FOLDER/ic_launcher_foreground.webp"
  sips -z $SIZE $SIZE -s format png "$SRC_ICON" --out "$RES_DIR/$FOLDER/ic_launcher.png"
  sips -z $SIZE $SIZE -s format png "$SRC_ICON" --out "$RES_DIR/$FOLDER/ic_launcher_round.png"
  sips -z $SIZE $SIZE -s format png "$SRC_ICON" --out "$RES_DIR/$FOLDER/ic_launcher_foreground.png"
}

resize_and_copy 48 "mipmap-mdpi"
resize_and_copy 72 "mipmap-hdpi"
resize_and_copy 96 "mipmap-xhdpi"
resize_and_copy 144 "mipmap-xxhdpi"
resize_and_copy 192 "mipmap-xxxhdpi"
