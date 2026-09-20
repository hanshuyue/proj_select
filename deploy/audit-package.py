"""Audit the actual executable JAR, then produce release integrity metadata."""
import hashlib
import io
import json
import struct
import sys
import zipfile
from pathlib import Path

root = Path(sys.argv[1]).resolve()
jar = root / 'app/gems-platform-server.jar'
requirements = []

def inspect(archive, name):
    maximum, count = 0, 0
    for entry in archive.infolist():
        if not entry.filename.endswith('.class') or entry.filename.startswith('META-INF/versions/'):
            continue  # Multi-release variants are selected only by compatible JVMs.
        if entry.filename.endswith('module-info.class'):
            continue
        with archive.open(entry) as f:
            header = f.read(8)
        if header[:4] != bytes.fromhex('cafebabe'):
            raise RuntimeError(f'Invalid class: {name}!{entry.filename}')
        minor, major = struct.unpack('>HH', header[4:])
        if minor == 65535:
            raise RuntimeError(f'Preview bytecode is not supported: {name}!{entry.filename}')
        maximum = max(maximum, major)
        count += 1
    if count:
        requirements.append({'artifact': name, 'classes': count, 'major': maximum, 'java': maximum - 44})

with zipfile.ZipFile(jar) as app:
    inspect(app, 'application-and-boot-loader')
    for entry in app.infolist():
        if entry.filename.startswith('BOOT-INF/lib/') and entry.filename.endswith('.jar'):
            with zipfile.ZipFile(io.BytesIO(app.read(entry))) as dependency:
                inspect(dependency, entry.filename)
    if 'BOOT-INF/classes/application-prod.yml' not in app.namelist():
        raise RuntimeError('Missing production configuration')
    if any('application-local' in n for n in app.namelist()):
        raise RuntimeError('Local configuration leaked into JAR')
minimum = max(item['java'] for item in requirements)
(root / 'deploy/minimum-java.txt').write_text(str(minimum) + '\n', encoding='utf-8')
(root / 'deploy/java-audit.json').write_text(json.dumps({'minimumJava': minimum, 'scope': 'All base classes in executable JAR and packaged dependencies; multi-release variants excluded', 'artifacts': requirements}, indent=2) + '\n', encoding='utf-8')
lines = []
for path in sorted(root.rglob('*')):
    if path.is_symlink():
        raise RuntimeError(f'Symlink not permitted: {path}')
    if not path.is_file() or path.name == 'SHA256SUMS':
        continue
    relative = path.relative_to(root).as_posix()
    if any(s in path.parts for s in ('node_modules', 'data', '.m2-repository')) or path.suffix == '.env':
        raise RuntimeError(f'Unexpected private/runtime file: {relative}')
    lines.append(hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + relative)
(root / 'deploy/SHA256SUMS').write_text('\n'.join(lines) + '\n', encoding='utf-8')
print(f'Audited {len(requirements)} archives. Minimum Java: {minimum}; {len(lines)} release files.')
