package ru.sosgps.wayrecall.jcommanderutils;

import com.beust.jcommander.JCommander;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 07.12.12
 * Time: 19:39
 * To change this template use File | Settings | File Templates.
 */
public class JCommanderCreator {


   public static JCommander create(Object o){
       return new JCommander(o);
   }


}
