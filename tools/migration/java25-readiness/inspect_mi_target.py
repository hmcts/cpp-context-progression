"""Step 10 static readiness inspection; does not execute MI tests or certify compatibility."""
import argparse
import hashlib
import json
import os
import re
import zipfile
from pathlib import Path

ARTIFACT = re.compile(r'^mireportdata-progression-event-listener-(.+)-(listener-tests|processor-tests)\.jar$')
EE = ('activation annotation batch decorator ejb el enterprise faces inject interceptor jms json jws mail '
      'persistence resource servlet validation websocket xml/bind xml/soap xml/ws xml/rpc xml/registry '
      'security/auth/message security/jacc security/enterprise transaction').split()
SE = ('javax/annotation/processing', 'javax/transaction/xa')

def class_info(data):
    offset, names = 0, set()
    def take(n):
        nonlocal offset
        if offset + n > len(data): raise ValueError('truncated class')
        result, offset = data[offset:offset+n], offset+n
        return result
    def number(n): return int.from_bytes(take(n), 'big')
    if take(4) != b'\xca\xfe\xba\xbe': raise ValueError('invalid class')
    number(2)
    major, count, index = number(2), number(2), 1
    if major < 45 or count < 2: raise ValueError('invalid class header')
    while index < count:
        tag = number(1)
        if tag == 1:
            value = take(number(2)).decode('utf-8', 'replace').replace('.', '/')
            for name in re.findall(r'javax/[A-Za-z0-9_/$]+', value):
                if any(name == p or name.startswith(p+'/') for p in SE): continue
                for package in EE:
                    prefix = 'javax/' + package
                    if name == prefix or name.startswith(prefix+'/'): names.add(prefix.replace('/', '.'))
        else:
            size = {3:4, 4:4, 5:8, 6:8, 7:2, 8:2, 9:4, 10:4, 11:4, 12:4,
                    15:3, 16:2, 17:4, 18:4, 19:2, 20:2}.get(tag)
            if size is None: raise ValueError('invalid constant pool')
            take(size)
            if tag in (5, 6): index += 1
        index += 1
    if index != count: raise ValueError('invalid constant pool slots')
    def attributes():
        for _ in range(number(2)): number(2); take(number(4))
    take(6)
    take(2 * number(2))
    for _ in range(2):
        for _ in range(number(2)): take(6); attributes()
    attributes()
    if offset != len(data): raise ValueError('trailing class bytes')
    return major, names

def inspect(path):
    majors, references = [], set()
    with zipfile.ZipFile(path) as jar:
        entries = jar.infolist()
        if (len(entries) > 100000 or sum(x.file_size for x in entries) > 256 * 1024 * 1024
                or len({x.filename for x in entries}) != len(entries)):
            raise ValueError('invalid jar inventory')
        for entry in entries:
            if not entry.filename.endswith('.class'): continue
            if entry.file_size > 8 * 1024 * 1024: raise ValueError('oversized class')
            major, found = class_info(jar.read(entry))
            majors.append(major); references.update(found)
        if jar.testzip() is not None: raise ValueError('invalid jar CRC')
    if not majors: raise ValueError('jar contains no classes')
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''): digest.update(chunk)
    return {'filename': path.name, 'sha256': digest.hexdigest(), 'class_count': len(majors),
            'classfile_major_min': min(majors), 'classfile_major_max': max(majors),
            'legacy_javaee_namespaces': sorted(references)}

def assess(classpath, version):
    result = {'status': 'failed', 'scope': 'MI classifier bytecode only; no test execution or compatibility certification',
              'artifacts': [], 'errors': []}
    try:
        pinned = bool(re.fullmatch(r'[0-9][A-Za-z0-9_.-]{0,79}', version)) and 'SNAPSHOT' not in version.upper()
        if not pinned:
            result['errors'].append('MI version must be an explicit release pin')
        paths = list(dict.fromkeys(Path(x) for x in classpath.read_text().strip().split(os.pathsep) if x))
        selected = {}
        for path in paths:
            match = ARTIFACT.fullmatch(path.name)
            if not match: continue
            if pinned and match[1] != version: raise ValueError('resolved MI version differs from supplied pin')
            if match[2] in selected: raise ValueError('duplicate MI classifier')
            selected[match[2]] = path
        if set(selected) != {'listener-tests', 'processor-tests'}: raise ValueError('required MI classifier missing')
        for classifier in sorted(selected):
            try: metadata = inspect(selected[classifier])
            except (OSError, ValueError, RuntimeError, zipfile.BadZipFile, EOFError):
                raise ValueError('required MI classifier is unreadable or malformed') from None
            metadata['classifier'] = classifier
            metadata['resolved_version'] = ARTIFACT.fullmatch(selected[classifier].name)[1]
            result['artifacts'].append(metadata)
            if metadata['legacy_javaee_namespaces']: result['errors'].append(classifier + ': legacy Java EE references')
        result['mi_version'] = version
    except (OSError, UnicodeError):
        result['errors'].append('classpath is unreadable')
    except ValueError as error:
        result['errors'].append(str(error))
    result['status'] = 'failed' if result['errors'] else 'passed-static-inspection'
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classpath', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--mi-version', required=True)
    args = parser.parse_args()
    result = assess(args.classpath, args.mi_version)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print('MI classifier inspection:', result['status'])
    for error in result['errors']: print(error)
    return 1 if result['errors'] else 0

if __name__ == '__main__': raise SystemExit(main())
