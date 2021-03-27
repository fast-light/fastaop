alias install="mvn clean install -Dmaven.test.skip=true"
cd ./fastcore && install && \
cd ../fastapt && install && \
cd ../fastaop && install