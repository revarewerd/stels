package ru.sosgps.wayrecall.core

import java.nio.charset.Charset
import java.nio.file.{Paths, Files}
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest

import com.google.common.cache.{LoadingCache, CacheBuilder}
import ru.sosgps.wayrecall.utils.{web, funcLoadingCache}

import scala.collection
import scala.collection.mutable

/**
 * Created by nickl on 05.08.14.
 */
class Translations{

  import scala.collection.JavaConversions.asScalaIterator
  import scala.collection.JavaConverters.propertiesAsScalaMapConverter
  import scala.collection.JavaConverters.mapAsScalaMapConverter

  val defaultLocale = "ru"

  def getRequestFromSpringOrDefault = {
    web.springCurrentRequestOption.map(getLocaleForRequest).getOrElse(new LocaleTranslations(defaultLocale))
  }

  def getLocaleForRequest(request: HttpServletRequest ): LocaleTranslations = {
    val localeName = Option(request.getCookies).flatMap(_.find(_.getName == "lang").map(_.getValue).filter(tr.isDefinedAt)).getOrElse(defaultLocale)
    new LocaleTranslations(localeName)
  }

  def tr: Map[String, collection.Map[String, String]] = cache(None)

  val cache: LoadingCache[Object, Map[String, collection.Map[String, String]]] = CacheBuilder.newBuilder()
    .expireAfterWrite(2, TimeUnit.MINUTES).buildWithFunction[Object, Map[String, collection.Map[String, String]]](v => {
    val newDirectoryStream = Files.newDirectoryStream(Paths.get(System.getenv("WAYRECALL_HOME"), "conf", "localizations"))
    val translations: Map[String, collection.Map[String, String]] = newDirectoryStream.iterator().map(p => {
      val properties = new Properties()
      val stream = Files.newBufferedReader(p, Charset.forName("UTF8"))
      properties.load(stream)
      stream.close()
      (p.getName(p.getNameCount - 1).toString.stripPrefix("tr_").stripSuffix(".properties"), properties.asScala.withDefault(k => k))
    }).toMap
    newDirectoryStream.close()
    translations
  })

  def translate(lang: String, key: String) = tr(lang)(key)

  class LocaleTranslations(val locale: String){
    def tr(key: String) =  Translations.this.tr(locale)(key)
  }

}
