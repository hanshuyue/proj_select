"""Fingerprint release source inputs, including uncommitted files, without their contents."""
import hashlib
import json
import subprocess
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
target = Path(sys.argv[2])
paths = subprocess.check_output(
    ['git', '-C', str(root), 'ls-files', '-z', '--cached', '--others', '--exclude-standard']
).decode('utf-8').split('\0')
entries = {}
for name in sorted(set(paths)):
    path = root / name
    if not name or not path.is_file() or path.is_symlink():
        continue
    if not name.startswith(('scaffold-system/', 'scaffold-vue/', 'deploy/')):
        continue
    # Only source inputs; never publish credential/config contents or runtime data.
    if any(part in {'target', 'dist', 'node_modules', 'data', '__pycache__', '.m2-repository'} for part in path.relative_to(root).parts):
        continue
    if path.name.startswith('.env') and not path.name.endswith('.example'):
        continue
    if path.name == 'application-local.yml' or path.suffix in {'.log', '.pyc'}:
        continue
    entries[name] = hashlib.sha256(path.read_bytes()).hexdigest()
canonical = json.dumps(entries, sort_keys=True, ensure_ascii=False, separators=(',', ':'))
target.write_text(json.dumps({'sourceSha256': hashlib.sha256(canonical.encode()).hexdigest(),
                              'files': entries}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
