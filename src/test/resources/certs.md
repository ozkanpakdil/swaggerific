# create jks file
keytool -genkey -keyalg RSA -noprompt -alias localhost -dname "CN=localhost, OU=NA, O=NA, L=NA, S=NA, C=NA" -keystore keystore.jks -validity 9999 -storepass changeme -keypass changeme
# create pfx from jks
keytool -importkeystore -srckeystore keystore.jks -srcstoretype JKS -destkeystore keystore.pfx -deststoretype PKCS12 -storepass changeme -keypass changeme  -srcstorepass changeme -deststorepass changeme