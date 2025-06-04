#!/bin/bash
set -e

# Create password file for Squid authentication
htpasswd -bc /etc/squid/passwd "${PROXY_USERNAME:-doug.finley}" "${PROXY_PASSWORD:-password}"
chown proxy:proxy /etc/squid/passwd
chmod 640 /etc/squid/passwd

# Initialize the SSL certificate database if not already done
if [ ! -f /var/lib/squid/ssl_db/index.txt ]; then
    /usr/lib/squid/security_file_certgen -c -s /var/lib/squid/ssl_db -M 4MB
    chown -R proxy:proxy /var/lib/squid/ssl_db
fi

# Clear any stale cache
rm -rf /var/cache/squid/*
squid -N -z

# Start Squid in foreground mode with debug logging
exec squid -N -d 1
