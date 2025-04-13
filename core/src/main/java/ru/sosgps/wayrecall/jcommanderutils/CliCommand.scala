package ru.sosgps.wayrecall.jcommanderutils

import com.beust.jcommander.{Parameters, ParameterException, JCommander}
import scala.collection.JavaConversions._
import java.lang.Object

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 03.01.13
 * Time: 14:50
 * To change this template use File | Settings | File Templates.
 */
trait CliCommand {

  val commandName: String;

  def process(): Unit;

}


abstract class MultiCommandCli {

  val commands: Seq[CliCommand]

  val helps = Set("-h", "--help")

  def main(args: Array[String]): Unit = {

    val jc = new JCommander();

    commands.foreach(c => jc.addCommand(c.commandName, c))

    try {
      jc.parse(args: _*);

      commands.find(jc.getParsedCommand == _.commandName) match {
        case Some(c) => {
          if (args.exists(helps.contains)) {
            jc.usage(jc.getParsedCommand)
          }
          else {
            c.process()
          }
        }
        case None => printCommands(jc)
      }
    }
    catch {

      case ex: ParameterException => {
        System.out.println(ex.getLocalizedMessage);
        if (jc.getParsedCommand != null)
          jc.usage(jc.getParsedCommand)
        else {
          printCommands(jc)
        }
      }

    }

  }


  protected def printCommands(jc: JCommander) {
    println("Avaliable commands:")
    for ((k, v) <- jc.getCommands) {
      val commandObject = v.getObjects.get(0)
      val commandParameters = Option(commandObject.getClass.getAnnotation(classOf[Parameters]))
      println(k + "\t - " + commandParameters.map(_.commandDescription()).filter(_ != "").getOrElse("(no description)"))
    }

  }
}