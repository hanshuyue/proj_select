"""Local preflight for the installer. No network/database connections."""
import glob
import hashlib
import os
import re
import shlex
import sys
from pathlib import Path

ROOT = '/home/glory/workmobile/selectproject'

def release(root):
    expected = {}
    for line in (root / 'deploy/SHA256SUMS').read_text().splitlines():
        digest, name = line.split('  ', 1)
        if not re.fullmatch('[0-9a-f]{64}', digest) or name.startswith('/') or '..' in Path(name).parts:
            raise ValueError('Invalid checksum manifest')
        if name in expected:
            raise ValueError('Duplicate manifest entry')
        expected[name] = digest
    actual = set()
    for path in root.rglob('*'):
        if path.is_symlink():
            raise ValueError('Release symlinks are not permitted')
        if path.is_file():
            name = path.relative_to(root).as_posix()
            if name == 'deploy/SHA256SUMS':
                continue
            actual.add(name)
            if expected.get(name) != hashlib.sha256(path.read_bytes()).hexdigest():
                raise ValueError('Release integrity failure: ' + name)
    if actual != set(expected):
        raise ValueError('Missing release files')
    for required in ('app/gems-platform-server.jar','web/index.html','templates/initiation/standard.pptx','templates/selection/standard.pptx','deploy/minimum-java.txt'):
        if required not in actual:
            raise ValueError('Missing required asset: ' + required)

def environment(path):
    values = {}
    for line in path.read_text().splitlines():
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        key, sep, raw = line.partition('=')
        if not sep or not re.fullmatch('[A-Z_]+', key) or key in values:
            raise ValueError('Invalid or duplicate environment key')
        # Deliberately accept a strict, single-line subset of systemd syntax.
        if raw.startswith(('"', "'")):
            if len(raw) < 2 or raw[-1] != raw[0] or '\\' in raw or raw[0] in raw[1:-1]:
                raise ValueError('Use a simple quoted single-line value for ' + key)
            value = raw[1:-1]
        else:
            if any(c.isspace() or c in "'\"\\" for c in raw):
                raise ValueError('Quote the value for ' + key)
            value = raw
        values[key] = value
    required = {'DB_URL','DB_USERNAME','DB_PASSWORD','REDIS_HOST','REDIS_PORT','REDIS_PASSWORD','JWT_SECRET','SERVER_ADDRESS','SERVER_PORT','DOCUMENT_STORAGE_PATH','SELECTION_STORAGE_PATH','INITIATION_STORAGE_PATH','SELECTION_BOOTSTRAP_TEMPLATE_PATH','INITIATION_BOOTSTRAP_TEMPLATE_PATH'}
    if values.keys() != required:
        raise ValueError('Environment keys must match selectproject.env.example exactly')
    if any('CHANGE_ME' in v for v in values.values()):
        raise ValueError('Fill all CHANGE_ME placeholders before installation')
    if any(not v for k,v in values.items() if k != 'REDIS_PASSWORD'):
        raise ValueError('Required environment value is empty')
    if len(values['JWT_SECRET'].encode()) < 32 or 'development' in values['JWT_SECRET'].lower():
        raise ValueError('JWT_SECRET must be a new random secret of at least 32 bytes')
    if values['SERVER_ADDRESS'] != '127.0.0.1' or values['SERVER_PORT'] != '18082':
        raise ValueError('Backend must bind 127.0.0.1:18082')
    if not values['REDIS_PORT'].isdigit() or not 1 <= int(values['REDIS_PORT']) <= 65535:
        raise ValueError('Invalid Redis port')
    if not re.match(r'jdbc:mysql://[^/]+/[^?]+', values['DB_URL']):
        raise ValueError('Invalid MySQL JDBC URL')
    for key,sub in [('DOCUMENT_STORAGE_PATH','documents'),('SELECTION_STORAGE_PATH','selection'),('INITIATION_STORAGE_PATH','initiation')]:
        if values[key] != ROOT + '/data/' + sub:
            raise ValueError('Unexpected storage path for ' + key)
    for kind in ('selection','initiation'):
        if values[kind.upper()+'_BOOTSTRAP_TEMPLATE_PATH'] != ROOT+'/data/'+kind+'/templates/standard.pptx':
            raise ValueError('Unexpected bootstrap path')

def nginx():
    destinations, users, visited = [], [], set()
    def parse(path, inherited):
        key = (str(path),tuple(inherited))
        if key in visited: return
        visited.add(key)
        lexer = shlex.shlex(path.read_text(), posix=True, punctuation_chars='{};')
        lexer.whitespace_split = True
        tokens = list(lexer)
        stack, statement = list(inherited), []
        for token in tokens:
            if token == '{':
                stack.append(statement[0] if statement else '?');statement=[]
            elif token == '}':
                if not stack: raise ValueError('Unexpected nginx brace')
                stack.pop();statement=[]
            elif token == ';':
                if statement[:1] == ['user'] and not stack: users.append(statement[1])
                if statement[:1] == ['include']:
                    pattern = statement[1]
                    if not pattern.startswith('/'): pattern='/etc/nginx/'+pattern
                    if stack == ['http'] and pattern in ('/etc/nginx/conf.d/*.conf','/etc/nginx/sites-enabled/*','/etc/nginx/sites-enabled/*.conf'):
                        destinations.append(pattern.replace('*','selectproject'))
                    for child in glob.glob(pattern): parse(Path(child),stack)
                if statement[:1] == ['listen'] and len(statement)>1 and re.search(r'(^|:)3333$', statement[1]):
                    if path.name not in ('selectproject','selectproject.conf') or '# Managed by selectproject' not in path.read_text():
                        raise ValueError('Nginx port 3333 already belongs to another site: '+str(path))
                statement=[]
            else: statement.append(token)
    parse(Path('/etc/nginx/nginx.conf'),[])
    if not destinations or len(set(users)) != 1:
        raise ValueError('Cannot determine HTTP include directory or explicit nginx worker user; configure them before installing')
    if not re.fullmatch('[a-z_][a-z0-9_-]*',users[0]): raise ValueError('Unexpected worker username')
    print(destinations[0]);print(users[0])

try:
    if sys.argv[1] == 'release': release(Path(sys.argv[2]))
    elif sys.argv[1] == 'env': environment(Path(sys.argv[2]))
    elif sys.argv[1] == 'nginx': nginx()
    else: raise ValueError('Unknown validation mode')
except (ValueError,OSError,KeyError) as error:
    sys.exit('Preflight failed: '+str(error))
