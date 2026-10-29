#!/usr/bin/env python3
"""Signs SandFly scripts (.lua or .zip) with SandFly's script signing key.

The signature is a comment on the last line of main.lua, which ScriptSignature.kt checks. It covers
every file of the script as SandFly reads it, so a signed script that is changed afterwards shows
as changed in the install dialog. Signing again replaces the old signature.

    tools/sign_script.py ~/Code/sandfly-scripts/*.lua
    tools/sign_script.py --check ~/Code/sandfly-scripts/*.lua

The key is ~/.android/sandfly-scripts-signing.pem (or $SANDFLY_SCRIPT_KEY), its password in the
macOS Keychain item "SandFly script signing key" (or $SANDFLY_SCRIPT_KEY_PASSWORD).
"""

import argparse
import base64
import io
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

PREFIX = b"-- SandFly signature: "
HEADER = b"SandFly script signature 1\n"
MAIN = "main.lua"
KEY = os.environ.get("SANDFLY_SCRIPT_KEY", os.path.expanduser("~/.android/sandfly-scripts-signing.pem"))
KEYCHAIN_ITEM = "SandFly script signing key"
# The key ScriptSignature.kt checks against, so that --check needs no password.
PUBLIC_KEY = (
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEX/hmvPKIkIBBtQvSevroddRdNGt5gr9hkcmjZubISke34ICwp6GkWzuZ"
    "DotkGseH3oKZ7NnShYMsRz1agg67AA=="
)


def openssl():
    for path in ("/opt/homebrew/opt/openssl/bin/openssl", "/usr/local/opt/openssl/bin/openssl"):
        if os.path.exists(path):
            return path
    return shutil.which("openssl") or sys.exit("openssl not found")


def password():
    if "SANDFLY_SCRIPT_KEY_PASSWORD" in os.environ:
        return os.environ["SANDFLY_SCRIPT_KEY_PASSWORD"]
    result = subprocess.run(
        ["security", "find-generic-password", "-s", KEYCHAIN_ITEM, "-w"], capture_output=True, text=True
    )
    if result.returncode != 0 or not result.stdout.strip():
        sys.exit(f'No password from the Keychain item "{KEYCHAIN_ITEM}": {result.stderr.strip()}')
    return result.stdout.strip()


def split(main):
    """main.lua up to its signature line, and the signature, as ScriptSignature.split does."""
    for i in range(len(main) - len(PREFIX), -1, -1):
        if (i == 0 or main[i - 1] == ord("\n")) and main.startswith(PREFIX, i):
            return main[:i], main[i + len(PREFIX):].decode("utf-8", "replace").rstrip()
    return main, None


def signed_bytes(files):
    out = io.BytesIO()
    out.write(HEADER)
    for name in sorted(files, key=lambda n: n.encode()):
        content = files[name]
        out.write(name.encode() + b"\n" + str(len(content)).encode() + b"\n" + content + b"\n")
    return out.getvalue()


def zip_files(data):
    """The files of a zip as SandFly reads them, by name, and each one's name in the zip."""
    entries = {}
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for info in z.infolist():
            if info.is_dir():
                continue
            name = info.filename.replace("\\", "/")
            if name.startswith("__MACOSX/") or name.rsplit("/", 1)[-1].startswith("."):
                continue
            entries[name] = (info.filename, z.read(info))
    if MAIN not in entries:
        folders = {n.split("/", 1)[0] if "/" in n else "" for n in entries}
        if len(folders) != 1 or not next(iter(folders)) or f"{next(iter(folders))}/{MAIN}" not in entries:
            sys.exit("The zip has no main.lua")
        folder = next(iter(folders)) + "/"
        entries = {n[len(folder):]: v for n, v in entries.items()}
    return {n: c for n, (_, c) in entries.items()}, {n: z for n, (z, _) in entries.items()}


def sign(content, ssl, key_password):
    with tempfile.NamedTemporaryFile() as f:
        f.write(content)
        f.flush()
        result = subprocess.run(
            [ssl, "dgst", "-sha256", "-sign", KEY, "-passin", "stdin", f.name],
            input=(key_password + "\n").encode(), capture_output=True,
        )
    if result.returncode != 0:
        sys.exit("Signing failed: " + result.stderr.decode(errors="replace"))
    der = result.stdout
    return base64.b64encode(der)


def verify(content, signature, ssl):
    with tempfile.TemporaryDirectory() as d:
        data, sig, pub = (os.path.join(d, n) for n in ("data", "sig", "pub.pem"))
        open(data, "wb").write(content)
        try:
            open(sig, "wb").write(base64.b64decode(signature, validate=True))
        except ValueError:
            return False
        lines = [PUBLIC_KEY[i:i + 64] for i in range(0, len(PUBLIC_KEY), 64)]
        open(pub, "w").write("-----BEGIN PUBLIC KEY-----\n" + "\n".join(lines) + "\n-----END PUBLIC KEY-----\n")
        result = subprocess.run(
            [ssl, "dgst", "-sha256", "-verify", pub, "-signature", sig, data], capture_output=True
        )
        return result.returncode == 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check", action="store_true", help="check the signatures instead of signing")
    parser.add_argument("scripts", nargs="+")
    args = parser.parse_args()
    ssl = openssl()
    key_password = None if args.check else password()
    failed = False
    for path in args.scripts:
        data = open(path, "rb").read()
        is_zip = data[:2] == b"PK"
        if is_zip:
            files, zip_names = zip_files(data)
        else:
            files, zip_names = {MAIN: data}, None
        body, signature = split(files[MAIN])
        if args.check:
            ok = signature is not None and verify(signed_bytes({**files, MAIN: body}), signature, ssl)
            print(f"{'signed' if ok else 'not signed' if signature is None else 'CHANGED'}: {path}")
            failed |= not ok
            continue
        if body and not body.endswith(b"\n"):
            body += b"\n"
        signed = body + PREFIX + sign(signed_bytes({**files, MAIN: body}), ssl, key_password) + b"\n"
        if is_zip:
            out = io.BytesIO()
            with zipfile.ZipFile(io.BytesIO(data)) as src, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as dst:
                for info in src.infolist():
                    content = signed if info.filename == zip_names[MAIN] else src.read(info)
                    dst.writestr(info, content)
            open(path, "wb").write(out.getvalue())
        else:
            open(path, "wb").write(signed)
        print(f"Signed {path}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
