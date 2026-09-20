"""Convert the validated systemd environment file to a private MySQL option file."""
import re
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
values = {}
for line in source.read_text().splitlines():
    if not line.strip() or line.lstrip().startswith('#'):
        continue
    key, separator, raw = line.partition('=')
    if not separator:
        raise SystemExit('Invalid environment line')
    if raw.startswith(('"', "'")):
        raw = raw[1:-1]
    values[key] = raw

match = re.fullmatch(r'jdbc:mysql://([^/:?#]+)(?::([0-9]+))?/([A-Za-z0-9_]+)(?:\?.*)?', values['DB_URL'])
if not match:
    raise SystemExit('DB_URL must contain a simple MySQL hostname, port, and database name')
host, port, database = match.group(1), match.group(2) or '3306', match.group(3)
for value in (host, port, database, values['DB_USERNAME'], values['DB_PASSWORD']):
    if any(character in value for character in ('\n', '\r', '\\', '"')):
        raise SystemExit('Database connection values contain unsupported characters')

target.write_text(
    '[client]\n'
    f'host="{host}"\nport="{port}"\nuser="{values["DB_USERNAME"]}"\n'
    f'password="{values["DB_PASSWORD"]}"\ndefault-character-set=utf8mb4\n',
    encoding='utf-8',
)
print(database)
