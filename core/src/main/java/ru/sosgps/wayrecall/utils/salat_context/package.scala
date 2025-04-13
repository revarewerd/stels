package ru.sosgps.wayrecall.utils

import com.novus.salat.Context

/**
  * Created by ivan on 08.09.16.
  */
package object salat_context {
  implicit val ctx = new Context {
    val name = "Custom Context"


    // ctx.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id"// some overrides or custom behavior
  }
    //ctx.registerGlobalKeyOverride("id", "_id")
}
