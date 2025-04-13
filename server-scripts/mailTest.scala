#!/bin/sh
L=$WAYRECALL_HOME/bin/libs
cp=`echo $L/*.jar|sed 's/ /:/g'`
exec scala -nc -cp $cp:$WAYRECALL_HOME/bin/tools.jar $0 $@
!#

import ru.sosgps.wayrecall.utils.MailSender

val prps = ru.sosgps.wayrecall.tools.Main.wrcProperties()

val mailSender = new MailSender(
  email = prps.getProperty("global.sysemail"),
  host = prps.getProperty("global.sysemail.smtphost"),
  login = prps.getProperty("global.sysemail.login"),
  password = prps.getProperty("global.sysemail.password"),
  allowedAddress = prps.getProperty("global.sysemail.allowedto", "")
)


mailSender.sendEmail(prps.getProperty("global.errornotificationAddress"), "Test", "test")