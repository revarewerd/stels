package ru.sosgps.wayrecall.monitoring.web

import java.io.{InputStreamReader, FilterWriter, Writer}
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.common.cache.CacheBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable}
import ru.sosgps.wayrecall.core.Translations
import ru.sosgps.wayrecall.utils.funcLoadingCache
import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.collection
import scala.collection.mutable

@Controller
class LocalizationManager extends grizzled.slf4j.Logging {


  @Autowired
  var translations: Translations = null

  @RequestMapping(Array("/localization.js"))
  def genLocalizationJs(
                         request: HttpServletRequest,
                         response: HttpServletResponse
                         ) {


    val lang = translations.getLocaleForRequest(request).locale;

    response.setCharacterEncoding("utf-8")
    response.setHeader("Content-Type","application/x-javascript; charset=utf-8")
    val out = response.getWriter
    out.println("curlang = '" + lang + "';");
    out.print("translations = ");
    ScalaJson.generate(translations.tr(lang), new FilterWriter(out) {
      override def close(): Unit = {}
    })
    out.println(";");
    out.println(
      """function tr(name) {
        | var r = translations[name];
        | return r ? r : name;
        |}""".stripMargin
    );

    out.flush();
    out.close();

  }

  @RequestMapping(Array("/localization/ext-lang.js"))
  def genExtJsLocalization(
                         request: HttpServletRequest,
                         response: HttpServletResponse
                         ) {


    val lang = translations.getLocaleForRequest(request).locale;

    debug("request:"+Seq(request.getRequestURI, request.getPathInfo, request.getContextPath).mkString(","))
    response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
    val dispatcher = request.getRequestDispatcher(request.getContextPath+"/extjs-4.2.1/locale/ext-lang-"+lang+".js")
    dispatcher.forward(request, response)
  }

  @RequestMapping(Array("/localization/openlayers-lang.js"))
  def genOpenlayersLocalization(
                            request: HttpServletRequest,
                            response: HttpServletResponse
                            ) {


    val lang = translations.getLocaleForRequest(request).locale;

    debug("request:"+Seq(request.getRequestURI, request.getPathInfo, request.getContextPath).mkString(","))
    response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
    val dispatcher = request.getRequestDispatcher(request.getContextPath+"/openlayers/lang/"+lang+".js")
    dispatcher.forward(request, response)
  }
  
  @RequestMapping(Array("/localization/{mapType}"))
  def getLocalizedMapJS(@PathVariable("mapType") mapType: String,
                        request: HttpServletRequest,
                        response: HttpServletResponse) {
    val lang = translations.getLocaleForRequest(request)
    
    val url = mapType match {
      case "YandexAPI" => "https://api-maps.yandex.ru/1.0/index.xml?lang=" + lang.tr("language") + "_" + lang.tr("country") + "&key=AAKDFFMBAAAA7hC1HgIAoZj68GN4ryARnMZdLxWtNkBYzPQAAAAAAAAAAACwBU_AI81T6hDOZmhHHJnit5rqnQ==~ACX7BlQBAAAADZs_XAMAVs_vov4kYbA3AZTTitMKljhJvq0AAAAAAAAAAAAOVrT6kjbMwWmGJ2ISPT28eRivzw=="
      case "GoogleAPI" => "https://maps.googleapis.com/maps/api/js?v=3.7&key=AIzaSyDLnKGTJX4BNhHf0bZs5WwgBT7McT1tkMA&sensor=false&language=" + lang.tr("language")
      case _ => ""
    }


    println(s"URL is $url")
    if (url.nonEmpty)
      response.sendRedirect(url)
    
  }

}

