alias debug="mvnDebug clean install -Dmaven.test.skip=true"
alias install="mvn clean install -Dmaven.test.skip=true"
install && \
cd ../example && debug