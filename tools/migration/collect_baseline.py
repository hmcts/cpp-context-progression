#!/usr/bin/env python3
"""Collect bounded source/tool/test-summary evidence without building or deploying."""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
CI_FIELDS = ("BUILD_BUILDID", "BUILD_REASON", "BUILD_SOURCEVERSION",
             "SYSTEM_PULLREQUEST_SOURCECOMMITID")


def command(root, *args):
    result = subprocess.run(args, cwd=root, capture_output=True, text=True,
                            check=False, timeout=30)
    if result.returncode:
        raise RuntimeError("Unable to read " + args[0] + " metadata")
    return result.stdout.strip()


def tool_version(root, executable, arguments, pattern):
    try:
        result = subprocess.run([executable, *arguments], cwd=root,
                                capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.TimeoutExpired):
        return None
    match = re.search(pattern, result.stdout + result.stderr)
    return match.group(1) if match else None


def report_summary(path, root):
    document = ET.parse(path).getroot()
    suites = [document] if document.tag == "testsuite" else list(document)
    if not suites or any(suite.tag != "testsuite" for suite in suites):
        raise ValueError("Unsupported test report structure")
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    for suite in suites:
        for name in totals:
            value = int(suite.get(name, "0"))
            if value < 0:
                raise ValueError("Negative test report count")
            totals[name] += value
    if totals["failures"] + totals["errors"] + totals["skipped"] > totals["tests"]:
        raise ValueError("Inconsistent test report counts")
    return {"path": str(path.relative_to(root)), **totals}


def collect(root, environ):
    pom = ET.parse(root / "pom.xml").getroot()
    reports = []
    invalid_reports = []
    for folder in ("surefire-reports", "failsafe-reports"):
        for path in sorted(root.glob("**/target/" + folder + "/TEST-*.xml")):
            try:
                reports.append(report_summary(path, root))
            except (ET.ParseError, ValueError, OSError):
                invalid_reports.append(str(path.relative_to(root)))
    totals = {key: sum(item[key] for item in reports)
              for key in ("tests", "failures", "errors", "skipped")}
    return {
        "source_at_collection": command(root, "git", "rev-parse", "HEAD"),
        "worktree_dirty": bool(command(root, "git", "status", "--porcelain")),
        "parent_version": pom.findtext("m:parent/m:version", namespaces=NS),
        "project_version": pom.findtext("m:version", namespaces=NS),
        "top_level_modules": [item.text for item in pom.findall("m:modules/m:module", NS)],
        "tools": {
            "java": tool_version(root, "java", ["-version"], r'version "([^"]+)"'),
            "maven": tool_version(root, "mvn", ["--version"], r"Apache Maven ([^\s\x1b]+)"),
        },
        "ci": {name: environ[name] for name in CI_FIELDS if environ.get(name)},
        "test_reports": reports,
        "invalid_reports": invalid_reports,
        "totals": totals,
        "limitations": [
            "Counts describe report files present; clean-build provenance must be established separately.",
            "No commands that build, deploy, migrate databases or replay messages were executed.",
            "Tool settings, environment secrets and test payloads are not collected.",
        ],
    }


def main():
    import os
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--require-passing-reports", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    evidence = collect(root, os.environ)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, indent=2) + "\n", encoding="utf-8")
    totals = evidence["totals"]
    print("Recorded source and test-summary evidence. Tests: {tests}; failures: {failures}; "
          "errors: {errors}; skipped: {skipped}.".format(**totals))
    if evidence["invalid_reports"]:
        return 2
    if args.require_passing_reports and (not evidence["test_reports"]
            or totals["tests"] <= totals["skipped"]
            or totals["failures"] or totals["errors"]):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
