alias install="mvn clean install -Dmaven.test.skip=true"
install && \
cd ../example && install