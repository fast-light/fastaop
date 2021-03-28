alias install="mvn clean install -Dmaven.test.skip=true"
cd ../fastapt && install && \
cd ../fastaop && install