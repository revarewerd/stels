package ru.sosgps.wayrecall.billing.tariff

import java.util.Date

/**
  * Created by IVAN on 26.04.2017.
  */
class TariffEDSTestMock extends TariffService {
  def correctReportWorkingDates(interval: (Date, Date)): (Date, Date) = {
    interval
  }
}
