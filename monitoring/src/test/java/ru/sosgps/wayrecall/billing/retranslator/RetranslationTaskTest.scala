package ru.sosgps.wayrecall.billing.retranslator

import org.junit.Test
import org.joda.time.DateTime

/**
 * Created by nickl on 24.02.14.
 */
object RetranslationTaskTest {


  // java -jar $WAYRECALL_HOME/bin/tools.jar send --addr localhost:9087 -f 2014-01-29T00:00 -t 2014-02-06T00:00 -u o351870825327299786


  def main(args: Array[String]) {
    val r = new WialonRetranslationTask("Wialon-test", "localhost", 9087, Seq("o351870825327299786"), "Seniel-dev2", new DateTime(2014, 1, 29, 0, 0).toDate, new DateTime(2014, 2, 6, 0, 0).toDate)
    r.start();

    val r2 = new ODSMosRuRetranslationTask(Seq("o351870825327299786"), "default", new DateTime(2014, 1, 29, 0, 0).toDate, new DateTime(2014, 2, 6, 0, 0).toDate)
    r2.start();

  }

}
