#!/usr/bin/env bash
# Managed by selectproject. Installs ONLY this project; never imports SQL.
set -euo pipefail
umask 027
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ $# -eq 2 ]] || fail 'Usage: sudo bash install.sh APP_ROOT RELEASE_DIR'
[[ $EUID -eq 0 ]] || fail 'Run with sudo.'
expected=/home/glory/workmobile/selectproject
[[ "$1" == "$expected" ]] || fail "APP_ROOT must be $expected"
for command in realpath python3 nginx systemctl rsync setfacl runuser flock ss getent; do
    command -v "$command" >/dev/null || fail "Missing prerequisite: $command"
done
[[ "$(realpath -m -- "$1")" == "$expected" ]] || fail 'Deployment path must not traverse a symlink.'
root=$expected
release=$(realpath -e -- "$2")
[[ "$release" == "$root/releases/"* && "$release" != "$root/releases" ]] || fail 'Release must be inside APP_ROOT/releases.'
[[ -d "$release" && -f "$release/deploy/validate-release.py" ]] || fail 'Incomplete release.'
exec 9>/run/lock/selectproject-install.lock
flock -n 9 || fail 'Another installation is running.'
getent passwd glory >/dev/null || fail 'Missing glory account.'
getent group glory >/dev/null || fail 'Missing glory group.'
python3 "$release/deploy/validate-release.py" release "$release"
for sub in app web data logs backups data/documents data/selection data/initiation data/selection/templates data/initiation/templates; do
    [[ "$(realpath -m -- "$root/$sub")" == "$root/$sub" ]] || fail "Symlink path rejected: $sub"
done
for kind in selection initiation; do
    [[ ! -L "$root/data/$kind/templates/standard.pptx" ]] || fail 'Template symlink rejected.'
done
[[ ! -L /etc/selectproject && ! -L /etc/selectproject/selectproject.env ]] || fail 'Configuration symlink rejected.'
install -d -m 0700 /etc/selectproject
if [[ ! -f /etc/selectproject/selectproject.env ]]; then
    install -m 0600 "$release/deploy/selectproject.env.example" /etc/selectproject/selectproject.env
    printf 'Created /etc/selectproject/selectproject.env (0600). Fill placeholders, prepare database, then rerun. Nothing started.\n' >&2
    exit 2
fi
chown root:root /etc/selectproject/selectproject.env
chmod 0600 /etc/selectproject/selectproject.env
python3 "$release/deploy/validate-release.py" env /etc/selectproject/selectproject.env
java=${SELECTPROJECT_JAVA:-$(command -v java || true)}
[[ -n "$java" && -x "$java" ]] || fail 'Install a compatible Java runtime or set SELECTPROJECT_JAVA to its executable.'
java=$(realpath -e -- "$java")
[[ "$java" =~ ^/[a-zA-Z0-9_./+-]+$ ]] || fail 'Java executable path must not contain spaces or shell syntax.'
version=$(runuser -u glory -- "$java" -version 2>&1)
major=$(sed -nE 's/.*version "([0-9]+).*/\1/p' <<<"$version" | head -n1)
minimum=$(cat "$release/deploy/minimum-java.txt")
[[ "$major" =~ ^[0-9]+$ && "$minimum" =~ ^[0-9]+$ && "$major" -ge "$minimum" ]] || fail "Java $minimum or newer required; detected: ${major:-unknown}"
nginx -t
nginx_info=$(python3 "$release/deploy/validate-release.py" nginx)
site=$(sed -n '1p' <<<"$nginx_info")
worker=$(sed -n '2p' <<<"$nginx_info")
getent passwd "$worker" >/dev/null || fail 'Nginx worker account does not exist.'
[[ ! -L "$site" ]] || fail 'Existing site symlink requires manual review; do not enable this site twice.'
unit=/etc/systemd/system/selectproject.service
for file in "$site" "$unit"; do
    [[ ! -e "$file" ]] || grep -q '^# Managed by selectproject' "$file" || fail "Refusing to replace unmanaged file: $file"
done
# Refuse a backend port owned by another service.
listeners=$(ss -H -ltnp 'sport = :18082')
if [[ -n "$listeners" ]]; then
    pid=$(systemctl show selectproject -p MainPID --value 2>/dev/null || true)
    [[ "$pid" =~ ^[1-9][0-9]*$ && "$listeners" == *"pid=$pid,"* ]] || fail 'Backend port 18082 is occupied by another process.'
fi
install -d -o glory -g glory -m 0750 "$root/app" "$root/web" "$root/logs" "$root/data" "$root/data/documents" "$root/data/selection" "$root/data/initiation" "$root/data/selection/templates" "$root/data/initiation/templates"
backup="$root/backups/$(date +%Y%m%d-%H%M%S)-$$"
install -d -m 0700 "$root/backups" "$backup"
cp -a "$root/app" "$root/web" "$backup/"
[[ ! -f "$unit" ]] || cp -a "$unit" "$backup/selectproject.service"
[[ ! -f "$site" ]] || cp -a "$site" "$backup/selectproject.conf"
printf '%s\n' "$site" > "$backup/nginx-site-path.txt"
was_active=false
systemctl is-active --quiet selectproject && was_active=true
changed=false
rollback() {
    result=$?
    trap - ERR
    if [[ "$changed" == true ]]; then
        echo "Install failed. Restoring program/config backup: $backup" >&2
        systemctl stop selectproject || true
        rsync -a --delete "$backup/app/" "$root/app/" || true
        rsync -a --delete "$backup/web/" "$root/web/" || true
        if [[ -f "$backup/selectproject.service" ]]; then cp -a "$backup/selectproject.service" "$unit"; else rm -f -- "$unit"; fi
        if [[ -f "$backup/selectproject.conf" ]]; then cp -a "$backup/selectproject.conf" "$site"; else rm -f -- "$site"; fi
        systemctl daemon-reload || true
        if [[ "$was_active" == true ]]; then systemctl start selectproject || true; fi
        nginx -t && systemctl reload nginx || true
    fi
    exit "$result"
}
trap rollback ERR
changed=true
if [[ "$was_active" == true ]]; then systemctl stop selectproject; fi
rsync -a --delete --chown=glory:glory "$release/app/" "$root/app/"
rsync -a --delete --chown=glory:glory "$release/web/" "$root/web/"
for kind in selection initiation; do
    if [[ ! -e "$root/data/$kind/templates/standard.pptx" ]]; then
        install -o glory -g glory -m 0640 "$release/templates/$kind/standard.pptx" "$root/data/$kind/templates/standard.pptx"
    fi
done
for directory in /home /home/glory /home/glory/workmobile "$root"; do
    setfacl -m "u:$worker:--x" "$directory"
done
setfacl -R -m "u:$worker:rX" "$root/web"
find "$root/web" -type d -exec setfacl -m "d:u:$worker:r-x" {} +
runuser -u "$worker" -- test -r "$root/web/index.html"
runuser -u glory -- test -r "$root/app/gems-platform-server.jar"
sed "s|@JAVA@|$java|g" "$release/deploy/selectproject.service" > "$unit"
chmod 0644 "$unit"
install -m 0644 "$release/deploy/selectproject.conf" "$site"
nginx -t
systemctl daemon-reload
systemctl reload nginx
if [[ "$was_active" == true ]]; then systemctl start selectproject; fi
trap - ERR
printf 'Installed release: %s\nBackup: %s\nJava: %s (requires >= %s)\n' "$release" "$backup" "$java" "$minimum"
printf 'No SQL was executed. On first install run systemctl enable --now selectproject after database preparation.\n'
printf 'Verify journalctl -u selectproject and /api/health; this script does not claim application readiness.\n'
