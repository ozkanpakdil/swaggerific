#!/bin/bash -eux

mkdir -p /var/cache/squid
if [[ "${PROXY_PASSWORD:-none}" != "none" && "${PROXY_USERNAME:-none}" != "none" ]]; then
	htpasswd -mbc /etc/squid/passwd "${PROXY_USERNAME}" "${PROXY_PASSWORD}"
else
	echo "Need to provide the environment variable PROXY_PASSWORD AND PROXY_USERNAME"
	exit 1
fi


if grep -q BLOCKLIST /etc/squid/squid.conf; then
	result=""
	if [[ "${PROXY_ALLOWED_DSTDOMAINS:-x}" != "x" ]]; then
		arr=$(echo $PROXY_ALLOWED_DSTDOMAINS | tr ",;" "\n\n")
		for dstdomain in $arr; do
			result=$(echo -e "$result    acl allowedDomains dstdomain $dstdomain\n")
		done
	else
		echo "You may provide PROXY_ALLOWED_DSTDOMAINS environment variable to limit which domains are accessible"
	fi
	if [[ "${PROXY_ALLOWED_DSTDOMAINS_REGEX:-x}" != "x" ]]; then
		result=$(echo -e "$result    acl allowedDomains dstdom_regex $PROXY_ALLOWED_DSTDOMAINS_REGEX\n")
	else
		echo "You may provide PROXY_ALLOWED_DSTDOMAINS_REGEX environment variable to limit which domains are accessible"
	fi
	if [[ "$result" == "" ]]; then
		sed '/allowedDomains/d' -i /etc/squid/squid.conf
	fi
	sed "s/BLOCKLIST/$result/g" -i /etc/squid/squid.conf
	cat /etc/squid/squid.conf
fi

/usr/sbin/squid -z

if [[ "${PROXY_DEBUG:-x}" != "x" ]]; then
	/usr/sbin/squid -N -d 1
else
	/usr/sbin/squid -N
fi
