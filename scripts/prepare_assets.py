#!/usr/bin/env python3
"""
Prepare bundled assets for PixelRooter from a Magisk release APK.

Usage:
    MAGISK_VERSION=27.0 python3 scripts/prepare_assets.py

The script downloads the specified Magisk release from GitHub, extracts
magiskboot and magisk daemon binaries, and places them in the asset
directories expected by the build.

Exploit payload .so files must be sourced separately and placed in
app/src/main/assets/exploits/ before building.
"""

import os
import sys
import urllib.request
import zipfile
import lzma
import shutil
import hashlib
from pathlib import Path

MAGISK_VERSION = os.environ.get("MAGISK_VERSION", "27.0")
REPO = "topjohnwu/Magisk"
RELEASE_URL = f"https://github.com/{REPO}/releases/download/v{MAGISK_VERSION}/Magisk-v{MAGISK_VERSION}.apk"

ASSET_DIRS = {
    "magiskboot": Path("app/src/main/assets/magiskboot"),
    "magisk": Path("app/src/main/assets/magisk"),
}

ABI_MAP = {
    "arm64-v8a": "arm64-v8a",
    "armeabi-v7a": "armeabi-v7a",
    "x86_64": "x86_64",
}


def download_apk(url: str, dest: Path) -> None:
    print(f"Downloading Magisk v{MAGISK_VERSION} from:\n  {url}")
    with urllib.request.urlopen(url) as response, open(dest, "wb") as out:
        total = int(response.headers.get("Content-Length", 0))
        downloaded = 0
        chunk = 65536
        while True:
            data = response.read(chunk)
            if not data:
                break
            out.write(data)
            downloaded += len(data)
            if total:
                pct = downloaded * 100 // total
                print(f"\r  {pct}%", end="", flush=True)
    print()


def extract_assets(apk_path: Path) -> None:
    for d in ASSET_DIRS.values():
        d.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(apk_path, "r") as apk:
        names = apk.namelist()

        # magiskboot — lives at lib/<abi>/libmagiskboot.so
        for abi, target_abi in ABI_MAP.items():
            src = f"lib/{abi}/libmagiskboot.so"
            if src in names:
                dest = ASSET_DIRS["magiskboot"] / f"magiskboot_{target_abi}"
                print(f"  Extracting {src} -> {dest}")
                with apk.open(src) as f_in, open(dest, "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)
                dest.chmod(0o755)

        # magisk daemon binaries (arm64 focus for Pixel)
        blob_map = {
            "lib/arm64-v8a/libmagisk64.so": ("magisk64", True),
            "lib/armeabi-v7a/libmagisk32.so": ("magisk32", True),
            "lib/arm64-v8a/libmagiskinit.so": ("magiskinit", False),
            "lib/arm64-v8a/libstub.apk": ("stub.apk", False),
        }
        for src, (name, compress) in blob_map.items():
            if src not in names:
                print(f"  WARNING: {src} not found in APK — skipping")
                continue
            if compress:
                dest = ASSET_DIRS["magisk"] / f"{name}.xz"
                print(f"  Extracting + compressing {src} -> {dest}")
                with apk.open(src) as f_in:
                    data = f_in.read()
                with lzma.open(dest, "wb", preset=6) as f_out:
                    f_out.write(data)
            else:
                dest = ASSET_DIRS["magisk"] / name
                print(f"  Extracting {src} -> {dest}")
                with apk.open(src) as f_in, open(dest, "wb") as f_out:
                    shutil.copyfileobj(f_in, f_out)


def check_exploit_payloads() -> None:
    exploit_dir = Path("app/src/main/assets/exploits")
    exploit_dir.mkdir(parents=True, exist_ok=True)
    payloads = list(exploit_dir.glob("*.so"))
    if payloads:
        print(f"\nExploit payloads found ({len(payloads)}):")
        for p in payloads:
            print(f"  {p.name}")
    else:
        print("\nWARNING: No exploit payloads found in app/src/main/assets/exploits/")
        print("  Place compiled exploit .so files there before building.")
        print("  See CONTRIBUTING.md for sources.")


def main() -> None:
    apk_cache = Path(f"/tmp/magisk_v{MAGISK_VERSION}.apk")

    if not apk_cache.exists():
        download_apk(RELEASE_URL, apk_cache)
    else:
        print(f"Using cached APK: {apk_cache}")

    print("\nExtracting assets...")
    extract_assets(apk_cache)

    check_exploit_payloads()
    print("\nDone. You can now run: ./gradlew assembleDebug")


if __name__ == "__main__":
    main()
