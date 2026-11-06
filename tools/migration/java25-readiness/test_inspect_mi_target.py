import os
import struct
import tempfile
import unittest
import zipfile
from pathlib import Path
from inspect_mi_target import assess, class_info

def bytecode(namespace):
    def utf(value):
        value = value.encode()
        return b'\x01' + struct.pack('>H', len(value)) + value
    pool = utf('Example') + b'\x07\x00\x01' + utf('java/lang/Object') + b'\x07\x00\x03' + utf(namespace)
    return struct.pack('>IHHH', 0xcafebabe, 0, 69, 6) + pool + struct.pack('>7H', 0x21, 2, 4, 0, 0, 0, 0)

class InspectionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name); self.cp = self.root / 'classpath.txt'
    def fixture(self, namespace='javax/xml/parsers/DocumentBuilder', version='1.2.3'):
        paths = [self.root / f'mireportdata-progression-event-listener-{version}-{c}-tests.jar' for c in ('listener', 'processor')]
        for path in paths:
            with zipfile.ZipFile(path, 'w') as jar: jar.writestr('Example.class', bytecode(namespace))
        self.cp.write_text(os.pathsep.join(map(str, paths))); return paths
    def test_java_se_namespaces_remain_allowed(self):
        for namespace in ('javax/xml/parsers/DocumentBuilder', 'javax/transaction/xa/XAResource', 'javax/annotation/processing/Processor'):
            self.fixture(namespace); result = assess(self.cp, '1.2.3')
            self.assertEqual(result['errors'], []); self.assertEqual(result['artifacts'][0]['classfile_major_max'], 69)
    def test_java_ee_descriptor_is_rejected(self):
        self.fixture('Ljavax/persistence/Entity;'); result = assess(self.cp, '1.2.3')
        self.assertTrue(result['errors']); self.assertEqual(result['artifacts'][0]['legacy_javaee_namespaces'], ['javax.persistence'])
    def test_missing_and_malformed_classifier_fail_closed(self):
        paths = self.fixture(); self.cp.write_text(str(paths[0])); self.assertTrue(assess(self.cp, '1.2.3')['errors'])
        self.cp.write_text(os.pathsep.join(map(str, paths))); paths[1].unlink(); self.assertTrue(assess(self.cp, '1.2.3')['errors'])
        paths[1].write_bytes(b'not a zip'); self.assertTrue(assess(self.cp, '1.2.3')['errors'])
    def test_unpinned_and_mismatched_versions_fail_closed(self):
        self.fixture()
        for pin in ('RELEASE', 'LATEST', '1.2.3-SNAPSHOT', '1.2.4'): self.assertTrue(assess(self.cp, pin)['errors'])
        self.assertEqual(len(assess(self.cp, 'RELEASE')['artifacts']), 2)
    def test_empty_or_truncated_class_is_rejected(self):
        paths = self.fixture()
        for payload in (b'', bytecode('javax/xml/parsers/DocumentBuilder')[:-1]):
            with zipfile.ZipFile(paths[0], 'w') as jar: jar.writestr('Example.class', payload)
            self.assertTrue(assess(self.cp, '1.2.3')['errors'])
    def test_newer_java_and_preview_classes_are_rejected(self):
        payload = bytecode('java/lang/Object')
        for minor, major in ((0, 70), (65535, 69)):
            with self.assertRaisesRegex(ValueError, 'newer Java version or preview'):
                class_info(payload[:4] + struct.pack('>HH', minor, major) + payload[8:])

if __name__ == '__main__': unittest.main()
