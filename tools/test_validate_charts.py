"""Regression checks for visible text coverage without copying the audio files."""
import shutil
import tempfile
import unittest
from pathlib import Path

from validate_charts import validate


class VisibleTextTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        project = Path(__file__).resolve().parents[1]
        files = [project / "resources/images/font/font.txt", project / "resources/CREDITS.txt",
                 project / "resources/songs/songs.txt", *sorted((project / "resources/songs").glob("*/chart_*.txt"))]
        for source in files:
            dest = self.root / source.relative_to(project)
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)

    def replace_title(self, text):
        path = self.root / "resources/songs/songs.txt"
        lines = path.read_text(encoding="utf-8").splitlines()
        for i, line in enumerate(lines):
            if line.strip() and not line.lstrip().startswith("#"):
                lines[i] = line.split(";", 1)[0] + ";" + text
                break
        path.write_text("\n".join(lines), encoding="utf-8")

    def test_current_resources_pass(self):
        self.assertEqual([], validate(self.root))

    def test_unsupported_song_title_reports_location_and_codepoint(self):
        for char in ("’", "'", "/", '"', "È", "Ç", "Ö", "🦊"):
            with self.subTest(char=char):
                self.replace_title("Don" + char + "t buy something")
                errors = validate(self.root)
                self.assertTrue(any("resources/songs/songs.txt:" in error and
                                    f"U+{ord(char):04X}" in error and "song title" in error
                                    for error in errors), errors)

    def test_composed_accents_and_drawn_punctuation_are_supported(self):
        self.replace_title("¿ÁÉÍÓÚÜÑ! Música: 99.9%; _".replace(";", ","))
        self.assertEqual([], validate(self.root))

    def test_credit_authors_are_checked_but_urls_are_not(self):
        path = self.root / "resources/CREDITS.txt"
        path.write_text("MUSIC;Artist’Name;Work;Menu music;CC0 1.0;https://example.org/a/b;"
                        "https://example.org/license;Converted / audio\n", encoding="utf-8")
        errors = validate(self.root)
        self.assertEqual(1, len(errors), errors)
        self.assertIn("creator", errors[0])
        self.assertIn("U+2019", errors[0])
        path.write_text(path.read_text(encoding="utf-8").replace("Artist’Name", "Artist"), encoding="utf-8")
        self.assertEqual([], validate(self.root))

    def test_chart_titles_are_checked(self):
        path = next((self.root / "resources/songs").glob("*/chart_*.txt"))
        lines = path.read_text(encoding="utf-8").splitlines()
        lines = ["CANCION;Song/Title" if line.startswith("CANCION;") else line for line in lines]
        path.write_text("\n".join(lines), encoding="utf-8")
        errors = validate(self.root)
        self.assertTrue(any("U+002F" in error and "chart title" in error for error in errors), errors)

    def test_fallback_glyph_is_required(self):
        path = self.root / "resources/images/font/font.txt"
        lines = [line for line in path.read_text(encoding="utf-8").splitlines()
                 if not line.startswith("CARACTER;63;")]
        path.write_text("\n".join(lines), encoding="utf-8")
        self.assertTrue(any("missing required '?' fallback" in error for error in validate(self.root)))


if __name__ == "__main__":
    unittest.main()
