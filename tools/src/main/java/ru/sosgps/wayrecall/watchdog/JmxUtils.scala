package ru.sosgps.wayrecall.watchdog

import java.lang.management.{RuntimeMXBean, ManagementFactory}
import javax.management.{Attribute, ObjectName, MBeanServerConnection}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.runtimeMirror

/**
 * Created by nmitropo on 18.8.2015.
 */
object JmxUtils {

  implicit class MBeanServerConnectionOps(remote: MBeanServerConnection) {

    def proxy[T <: java.lang.management.PlatformManagedObject: ClassTag]: T = {
      ManagementFactory.getPlatformMXBean(
        remote,
        //implicitly[TypeTag[T]].mirror.runtimeClass(implicitly[TypeTag[T]].tpe)
        implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
         //classOf[T]
      );
    }

    def cpuUsage: Double = {
      val name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
      val list = remote.getAttributes(name,  Array("ProcessCpuLoad"));

      if (list.isEmpty())
        return Double.NaN;

      val att:Attribute = list.get(0).asInstanceOf[Attribute];
      att.getValue().asInstanceOf[Double];
    }


  }

}
