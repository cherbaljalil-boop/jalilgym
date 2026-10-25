#!/usr/bin/env python3
"""Checks every translated strings.xml against the English one.

Each translation must have exactly the English keys (minus translatable="false"),
the same format placeholders in each string, and every plural quantity its
language needs. Run from the repository root: python3 tools/i18n/check.py
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

PLURALS = {"fr": {"one", "many", "other"}, "ar": {"zero", "one", "two", "few", "many", "other"}}
PLACEHOLDER = re.compile(r"%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfx%]")


def entries(path):
    out = {}
    for e in ET.parse(path).getroot():
        if e.get("translatable") == "false":
            continue
        if e.tag == "string":
            # formatted="false" strings are shown as written, so "%" there is just text.
            text = "".join(e.itertext())
            out[e.get("name")] = {"": "" if e.get("formatted") == "false" else text}
        elif e.tag == "plurals":
            out[e.get("name")] = {i.get("quantity"): "".join(i.itertext()) for i in e}
    return out


def placeholders(text):
    return sorted(m.group(0) for m in PLACEHOLDER.finditer(text) if m.group(0) != "%%")


def main():
    errors = []
    for english in sorted(pathlib.Path(".").glob("**/src/main/res/values/strings.xml")):
        if "build" in english.parts:
            continue
        base = entries(english)
        for lang, quantities in PLURALS.items():
            path = english.parent.parent / f"values-{lang}" / "strings.xml"
            if not path.exists():
                errors.append(f"{path}: missing")
                continue
            translated = entries(path)
            for key in base.keys() - translated.keys():
                errors.append(f"{path}: missing {key}")
            for key in translated.keys() - base.keys():
                errors.append(f"{path}: extra {key}")
            for key in base.keys() & translated.keys():
                source, target = base[key], translated[key]
                if "" in source:
                    if placeholders(source[""]) != placeholders(target.get("", "")):
                        errors.append(f"{path}: {key} placeholders differ")
                    continue
                if set(target) != quantities:
                    errors.append(f"{path}: {key} needs quantities {sorted(quantities)}, has {sorted(target)}")
                english_ph = placeholders(source["other"])
                for quantity, text in target.items():
                    found = placeholders(text)
                    # A language may drop the number where the word says it ("une", "واحد").
                    if found != english_ph and not set(found) <= set(english_ph):
                        errors.append(f"{path}: {key}#{quantity} placeholders differ")
    for error in errors:
        print(error)
    if errors:
        sys.exit(1)
    print("Translations OK")


if __name__ == "__main__":
    main()
