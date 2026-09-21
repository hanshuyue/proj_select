#!/usr/bin/env bash
# Manual, explicit database preparation. The application and installer never invoke this script.
set -euo pipefail
umask 077
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ $# -eq 1 && ( "$1" == fresh || "$1" == upgrade ) ]] || fail 'Usage: sudo bash database.sh fresh|upgrade'
[[ $EUID -eq 0 ]] || fail 'Run with sudo so the production env remains private.'
for command in mysql mysqldump python3 mktemp; do
    command -v "$command" >/dev/null || fail "Missing prerequisite: $command"
done
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
python3 "$script_dir/validate-release.py" release "$(dirname -- "$script_dir")"
environment_file=/etc/selectproject/selectproject.env
[[ -f "$environment_file" ]] || fail "Missing $environment_file"
python3 "$script_dir/validate-release.py" env "$environment_file"
option_file=$(mktemp /tmp/selectproject-mysql.XXXXXX)
cleanup() { rm -f -- "$option_file"; }
trap cleanup EXIT
database=$(python3 "$script_dir/db-config.py" "$environment_file" "$option_file")
chmod 0600 "$option_file"

if [[ "$1" == fresh ]]; then
    table_count=$(mysql --defaults-extra-file="$option_file" --batch --skip-column-names \
        -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${database}'")
    [[ "$table_count" == 0 ]] || fail "Fresh initialization refused: database $database already has $table_count tables."
    mysql --defaults-extra-file="$option_file" "$database" < "$script_dir/sql/10-scaffold-base.sql"
else
    backup_root=/home/glory/workmobile/selectproject/backups/database
    install -d -o glory -g glory -m 0700 "$backup_root"
    backup_file="$backup_root/${database}-$(date +%Y%m%d-%H%M%S).sql"
    mysqldump --defaults-extra-file="$option_file" --single-transaction --routines --triggers \
        "$database" > "$backup_file"
    chmod 0600 "$backup_file"
    chown glory:glory "$backup_file"
    printf 'Database backup: %s\n' "$backup_file"
fi

mysql --defaults-extra-file="$option_file" "$database" < "$script_dir/sql/20-selection.sql"
mysql --defaults-extra-file="$option_file" "$database" < "$script_dir/sql/30-initiation.sql"
mysql --defaults-extra-file="$option_file" "$database" < "$script_dir/sql/40-collaboration.sql"
printf 'Database %s completed for %s. No database was dropped or truncated.\n' "$1" "$database"
