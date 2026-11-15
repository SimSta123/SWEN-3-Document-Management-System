#!/bin/sh
# Disposable deletion test: hold actual recognition until the API has deleted the document.
touch /tmp/deletion-ocr-started
while [ ! -f /tmp/deletion-ocr-resume ]; do sleep 0.1; done
exec tesseract "$@"
