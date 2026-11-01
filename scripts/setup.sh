#!/usr/bin/env bash
# Fetches the things that are too big to keep in git:
#   1. whisper.cpp source (compiled into the app via the NDK)
#   2. llama.cpp source (compiled in too, for on-device grammar fixing)
#   3. the Whisper model that ships inside the APK so dictation works offline
set -euo pipefail
cd "$(dirname "$0")/.."

WHISPER_TAG="v1.7.6"
MODEL="ggml-base.en-q5_1.bin"
MODEL_URL="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${MODEL}"

if [ ! -d third_party/whisper.cpp/.git ]; then
  mkdir -p third_party
  git clone --depth 1 --branch "$WHISPER_TAG" https://github.com/ggml-org/whisper.cpp third_party/whisper.cpp
fi

LLAMA_TAG="b12032"
if [ ! -d third_party/llama.cpp/.git ]; then
  git clone --depth 1 --branch "$LLAMA_TAG" https://github.com/ggml-org/llama.cpp third_party/llama.cpp
fi

mkdir -p app/src/main/assets/models
TARGET="app/src/main/assets/models/${MODEL}"
if [ ! -f "$TARGET" ]; then
  curl -L --fail --retry 3 -o "$TARGET.part" "$MODEL_URL"
  mv "$TARGET.part" "$TARGET"
fi

SIZE=$(wc -c < "$TARGET")
if [ "$SIZE" -lt 40000000 ]; then
  echo "Model download looks truncated ($SIZE bytes). Delete $TARGET and retry." >&2
  exit 1
fi
echo "Ready: whisper.cpp $WHISPER_TAG, llama.cpp $LLAMA_TAG and $MODEL ($SIZE bytes)"
