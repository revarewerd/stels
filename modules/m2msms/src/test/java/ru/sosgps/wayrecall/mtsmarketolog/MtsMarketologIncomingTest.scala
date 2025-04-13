package ru.sosgps.wayrecall.mtsmarketolog

import com.google.common.base.Charsets
import org.junit.{Assert, Test}

import java.io.ByteArrayInputStream

class MtsMarketologIncomingTest {
  
  def gate = new MtsMarketologRestSmsGate()

  @Test
  def testFromDoc(): Unit = {

    val smses = gate.parseIncomingBody(new ByteArrayInputStream(
      """
        |{
        |  "messages": [
        |    {
        |      "content": {
        |        "text": "some message"
        |      },
        |      "from": {
        |        "naming": "4444",
        |        "user_contact": "79117903795",
        |        "received_at": 1673784537
        |      },
        |      "channel_id": 1,
        |      "internal_id": "10345678-f123-4694-91d7-233df79f65445"
        |    }
        |  ]
        |}""".stripMargin.getBytes(Charsets.UTF_8))).incomingSms

    Assert.assertEquals(
      """smsId:-7937818783300299707 from:79117903795 to: text:some message sent:Sun Jan 15 15:08:57 MSK 2023 delivered:Sun Jan 15 15:08:57 MSK 2023 status:Delivered""".
        stripMargin, smses.mkString("\n"))

  }

  @Test
  def testDelivered(): Unit = {

    val smses = gate.parseIncomingBody(new ByteArrayInputStream(
      """
        |{
        |  "items": [
        |    {
        |      "call_direction": "MT",
        |      "channel": 1,
        |      "destination": "79778063333",
        |      "event_at": "2023-01-14T16:31:25.000+03:00",
        |      "internal_errors": null,
        |      "internal_id": "5727ba06-f7ca-4dd1-85a7-2fadd2944abf",
        |      "message_id": "86bb1e1754f28ed4",
        |      "naming": "79166400536",
        |      "received_at": "2023-01-14T16:31:24.000+03:00",
        |      "status": 200,
        |      "total_parts": 2
        |    },
        |    {
        |      "call_direction": "MT",
        |      "channel": 1,
        |      "destination": "79160502734",
        |      "event_at": "2023-01-13T20:19:19.000+03:00",
        |      "internal_errors": null,
        |      "internal_id":"7d1fad63-817b-4a58-a9eb-ee41fe6cf127",
        |      "message_id":"b703bde85d45dee8",
        |      "naming": "79166400536",
        |      "received_at": "2023-01-13T20:19:16.000+03:00",
        |      "status": 300,
        |      "total_parts": 1
        |    }
        |  ]
        |}""".stripMargin.getBytes(Charsets.UTF_8)))

    Assert.assertEquals(
      """IncomingInfo(List(),List(smsId:-5259151134143684888 from: to:79160502734 text:<no-content> sent:Fri Jan 13 20:19:16 MSK 2023 delivered:Fri Jan 13 20:19:16 MSK 2023 status:Delivered))""".
        stripMargin, smses.toString)

  }

}
