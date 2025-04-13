package ru.sosgps.wayrecall.initialization

import java.io.File
import java.net.{URL, URI}
import java.nio.file.{Paths, DirectoryStream, Files, Path}
import java.util.{Objects, Properties}

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.{BeanDefinitionRegistry, BeanDefinitionRegistryPostProcessor}
import org.springframework.context.support.{AbstractXmlApplicationContext, AbstractRefreshableConfigApplicationContext, ClassPathXmlApplicationContext, FileSystemXmlApplicationContext}
import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource, StandardEnvironment}
import ru.sosgps.wayrecall.core.InstanceConfig
import ru.sosgps.wayrecall.utils

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.io.Source

class MultiserverConfig(confPath: Path) {

  def this(uri: String) = this(Paths.get(new URI(uri.replaceAll( """\\""", "/"))))

  val gloabalProps: Properties = utils.loadProperties(confPath.resolve("global.properties"))

  val wrcInstanceConfigs: Path = confPath.resolve("wrcinstances")

  lazy val instances: Iterable[InstanceConfig] = {
    val directoryStream: DirectoryStream[Path] = Files.newDirectoryStream(wrcInstanceConfigs)
    try{
      directoryStream.map(p => new InstanceConfig(p/*, gloabalProps*/)).toList
    }
    finally {
      directoryStream.close()
    }
  }

}

object MultiserverConfig{

  def fromClassPath(resourceName: String) = {
    val resource = Objects.requireNonNull(ClassLoader.getSystemResource(resourceName),resourceName+" not found on classpath")
    new MultiserverConfig(Paths.get(resource.toURI));
  }

}






