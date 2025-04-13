package ru.sosgps.wayrecall.web.api

import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam, RestController}
import ru.sosgps.wayrecall.core.UserRolesChecker
import ru.sosgps.wayrecall.monitoring.geozones.GeozonesStore

import scala.collection.mutable

@RestController
@RequestMapping(Array("/geo"))
class GeozonesEndpoint(roleChecker: UserRolesChecker, store: GeozonesStore) {

  @RequestMapping(Array("/all"))
  def allGeozines(@RequestParam(value = "points", required = false, defaultValue = "false") showPoints: Boolean) = {
    val name = roleChecker.getUserName
    store.getUsersGeozones(name).map(g => {
      val geozoneData = mutable.Map("id" -> g.id, "name" -> g.name, "color" -> g.color.trim, "rgba" -> rgbaString(g.color.trim))
      if (showPoints)
        geozoneData += "points" -> g.points
      geozoneData
    })
  }

  private def rgbaString(clrStr: String) = {
    val values = clrStr.stripPrefix("#").sliding(2, 2).map(b => Integer.parseInt(b, 16)).toSeq
    s"rgba(${values.mkString(",")},0.5)"
  }

  @RequestMapping(Array("/byid/{id}"))
  def geoById(@PathVariable id: Int) = {
    val name = roleChecker.getUserName
    val g = store.getByIdAndUser(id, name)
    Map("id" -> g.id, "name" -> g.name, "color" -> g.color.trim, "rgba" -> rgbaString(g.color.trim), "points" -> g.points)
  }

}
