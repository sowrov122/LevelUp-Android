#!/usr/bin/env python3
"""Write a Markdown build/test summary to the GitHub Actions job summary (or stdout when run locally)."""
import glob
import os
import sys
import xml.etree.ElementTree as ET

RESULT_GLOB = "app/build/test-results/**/*.xml"
APK_GLOB = "app/build/outputs/apk/**/*.apk"


def parse_results():
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed = []
    files = sorted(glob.glob(RESULT_GLOB, recursive=True))
    for path in files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        suites = [root] if root.tag == "testsuite" else root.iter("testsuite")
        for suite in suites:
            for key in totals:
                totals[key] += int(suite.get(key, 0) or 0)
            for case in suite.iter("testcase"):
                problem = case.find("failure")
                if problem is None:
                    problem = case.find("error")
                if problem is not None:
                    msg = (problem.get("message") or "").strip().splitlines()
                    failed.append((f"{case.get('classname')}.{case.get('name')}", msg[0] if msg else ""))
    return files, totals, failed


def main():
    files, totals, failed = parse_results()
    lines = ["## Level Up – Android CI summary", ""]

    if not files:
        lines += ["No unit-test result files were found (the build may have failed before tests ran).", ""]
    else:
        bad = totals["failures"] + totals["errors"]
        passed = totals["tests"] - bad - totals["skipped"]
        status = "✅ all tests passed" if bad == 0 else f"❌ {bad} failing"
        lines += [
            f"**Unit tests:** {status}",
            "",
            "| Total | Passed | Failed | Errors | Skipped |",
            "|---:|---:|---:|---:|---:|",
            f"| {totals['tests']} | {passed} | {totals['failures']} | {totals['errors']} | {totals['skipped']} |",
            "",
        ]
        if failed:
            lines += ["### Failing tests", ""]
            for name, msg in failed:
                lines.append(f"- `{name}` — {msg}" if msg else f"- `{name}`")
            lines.append("")

    apks = sorted(glob.glob(APK_GLOB, recursive=True))
    if apks:
        lines += ["### Build output", ""]
        for apk in apks:
            lines.append(f"- `{apk}` ({os.path.getsize(apk) / 1024 / 1024:.1f} MB)")
        lines.append("")
    else:
        lines += ["### Build output", "", "No APK was produced.", ""]

    text = "\n".join(lines)
    target = os.environ.get("GITHUB_STEP_SUMMARY")
    if target:
        with open(target, "a", encoding="utf-8") as fh:
            fh.write(text + "\n")
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
