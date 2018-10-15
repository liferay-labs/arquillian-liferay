if exist "%CATALINA_HOME%/jre${jdk.windows.version}/win" (
        if not "%JAVA_HOME%" == "" (
                set JAVA_HOME=
        )

        set "JRE_HOME=%CATALINA_HOME%/jre${jdk.windows.version}/win"
)

set "CATALINA_OPTS=%CATALINA_OPTS% -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true  -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xmx1024m -XX:MaxPermSize=256m"

set JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=8099 -Dcom.sun.management.jmxremote.ssl=false"

set CATALINA_OPTS="%CATALINA_OPTS% %JMX_OPTS%"


set CATALINA_OPTS="%CATALINA_OPTS% %DEBUG_OPTS%"

set DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,address=9000,server=y,suspend=n

