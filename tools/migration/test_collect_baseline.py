import tempfile
import unittest
from pathlib import Path

from collect_baseline import report_summary


class TestReportSummary(unittest.TestCase):
    def report(self, xml):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        root = Path(folder.name)
        path = root / "TEST-example.xml"
        path.write_text(xml, encoding="utf-8")
        return report_summary(path, root)

    def test_preserves_failures_errors_and_skips(self):
        result = self.report('<testsuite tests="8" failures="2" errors="1" skipped="3"/>')
        self.assertEqual((8, 2, 1, 3), tuple(result[key] for key in
                         ("tests", "failures", "errors", "skipped")))

    def test_sums_child_suites_once(self):
        result = self.report('<testsuites tests="99"><testsuite tests="3" failures="1"/>'
                             '<testsuite tests="5" skipped="2"/></testsuites>')
        self.assertEqual(8, result["tests"])
        self.assertEqual(1, result["failures"])
        self.assertEqual(2, result["skipped"])

    def test_rejects_inconsistent_counts(self):
        with self.assertRaises(ValueError):
            self.report('<testsuite tests="1" failures="2"/>')

    def test_rejects_unrecognised_report(self):
        with self.assertRaises(ValueError):
            self.report('<results/>')


if __name__ == "__main__":
    unittest.main()
