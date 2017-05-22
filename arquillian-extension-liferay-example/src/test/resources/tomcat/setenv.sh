CATALINA_OPTS="$CATALINA_OPTS -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true  -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xmx1024m -XX:MaxPermSize=256m"

JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=8099 -Dcom.sun.management.jmxremote.ssl=false"

CATALINA_OPTS="${CATALINA_OPTS} ${JMX_OPTS}"

DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,address=9000,server=y,suspend=n"

CATALINA_OPTS="${CATALINA_OPTS} ${DEBUG_OPTS}"

JACOCO_OPTS="-javaagent:../jacocoagent.jar=destfile=target/jacocoIntegration.exec,output=file,append=true,jmx=true,dumponexit=true"

CATALINA_OPTS="${CATALINA_OPTS} ${JACOCO_OPTS}"
