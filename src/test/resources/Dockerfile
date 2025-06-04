FROM ubuntu:18.04

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
    apache2-utils \
    squid3 \
    ssl-cert \
    openssl \
    ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create SSL certificate directory and initialize it
RUN mkdir -p /var/lib/squid/ssl_db && \
    chown -R proxy:proxy /var/lib/squid && \
    /usr/lib/squid/security_file_certgen -c -s /var/lib/squid/ssl_db -M 4MB && \
    chown -R proxy:proxy /var/lib/squid/ssl_db

# Create cache directory
RUN mkdir -p /var/cache/squid && \
    chown -R proxy:proxy /var/cache/squid

COPY squid.conf /etc/squid/squid.conf
COPY entrypoint.sh /
RUN chmod a+x /entrypoint.sh

# Initialize the Squid cache
RUN squid -N -f /etc/squid/squid.conf -z

EXPOSE 3128

ENTRYPOINT ["/entrypoint.sh"]
