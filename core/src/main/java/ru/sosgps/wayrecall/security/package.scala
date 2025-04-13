package ru.sosgps.wayrecall

import ru.sosgps.wayrecall.utils.errors.NotPermitted

/**
 * Created by nickl on 31.03.15.
 */
package object security {
  @inline final def requirePermission(requirement: Boolean, message: => Any) {
    if (!requirement)
      throw new NotPermitted(message.toString)
  }
}
