FROM ubuntu:18.04

RUN apt-get update && \
    apt-get -qqy install apache2-utils squid3 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY squid.conf /etc/squid/squid.conf
COPY entrypoint.sh /
RUN chmod a+x /entrypoint.sh

EXPOSE 3128
RUN mkdir -p /var/cache/squid && chown proxy -R /var/cache/squid

ENTRYPOINT ["/entrypoint.sh"]
