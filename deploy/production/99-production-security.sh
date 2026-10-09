#!/bin/sh
set -eu
# Public seed/demo credentials must not authenticate in production.
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot "$MYSQL_DATABASE" -e 'UPDATE sys_user SET status=0;'
echo 'Production initialization: public demo logins disabled.'
