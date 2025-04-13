package ru.sosgps.wayrecall.web.api

import java.util

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.{ComponentScan, Configuration}
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.web.authentication.www.{BasicAuthenticationEntryPoint, BasicAuthenticationFilter}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, ResponseBody}
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.{EnableWebMvc, InterceptorRegistry, WebMvcConfigurerAdapter}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import ru.sosgps.wayrecall.core.UserRolesChecker
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.web.ScalaJsonObjectMapper

class WebApiInit extends ApplicationContextInitializer[AnnotationConfigWebApplicationContext] {
  override def initialize(applicationContext: AnnotationConfigWebApplicationContext): Unit = {
    applicationContext.register(classOf[WebApi])
  }
}

@Configuration("webapi")
@EnableWebMvc
@ComponentScan(basePackageClasses = Array(classOf[ObjectsEndpoint]))
class WebApi(@Qualifier("org.springframework.security.authenticationManager")
             @Autowired a: AuthenticationManager,
             rolesChecker: UserRolesChecker) extends WebMvcConfigurerAdapter with grizzled.slf4j.Logging {

  override def configureMessageConverters(converters: util.List[HttpMessageConverter[_]]): Unit = {
    converters.add(new MappingJackson2HttpMessageConverter(new ScalaJsonObjectMapper))
    super.configureMessageConverters(converters)
  }


  val converters = Set(
    new ru.sosgps.wayrecall.utils.web.ScalaMapConverter,
    new ru.sosgps.wayrecall.utils.web.ListConverter,
    new ru.sosgps.wayrecall.utils.web.MapToListConverter,
    new ru.sosgps.wayrecall.utils.web.DateConverter
  )

  override def addFormatters(registry: FormatterRegistry): Unit = {
    for (elem <- converters) {
      registry.addConverter(elem)
    }
    super.addFormatters(registry)
  }

  override def addInterceptors(registry: InterceptorRegistry): Unit = {
    val filter = new BasicAuthenticationFilter(a, new BasicAuthenticationEntryPoint)
    registry.addInterceptor(new HandlerInterceptorAdapter {
      override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: scala.Any): Boolean = {
        var result = false
        try {
          filter.doFilter(request, response, new FilterChain {
            override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse): Unit = {
              if (rolesChecker.getUserName() == "anonymousUser") throw new NotPermitted("anonymousUser is not permitted")
              result = true
            }
          })
          result
        }
        catch {
          case e: NotPermitted =>
            response.addHeader("WWW-Authenticate", "Basic realm=\"" + "Realm" + "\"")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage)
            false
          case e: Exception =>
            error("error", e)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
            false
        }
      }
    })
  }

}

@Controller
class TestApi(rolesChecker: UserRolesChecker) {


  @GetMapping(Array("hello"))
  @ResponseBody
  def hello() = "Heeelo " + rolesChecker.getUserName()

}