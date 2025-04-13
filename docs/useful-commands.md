# Poor man's sampler

    cat wayrecall.pswrd | ssh -p 22022 niks@wayrecall.ksb-stels.ru sudo -S -u wayrecall jstack 17744 | pbcopy
    cat wayrecall.pswrd | ssh -p 22022 niks@wayrecall.ksb-stels.ru sudo -S -u wayrecall jstack $(systemctl show --property MainPID --value wayrecall-web)

# Strong man's profiler

    sudo -u wayrecall jcmd $(systemctl show --property MainPID --value wayrecall-web) JFR.start name=my filename=/opt/wayrecall/logs/monitoring/objects4.jfr
    sudo -u wayrecall jcmd $(systemctl show --property MainPID --value wayrecall-web) JFR.stop name=my

# Build with Java 8

    JAVA_HOME=$(/usr/libexec/java_home -v 1.8); zinc -start
    JAVA_HOME=$(/usr/libexec/java_home -v 1.8); mvn --settings ./.mvn/local-settings.xml wrapper:wrapper -Dmaven=3.5.4 install -DskipTests