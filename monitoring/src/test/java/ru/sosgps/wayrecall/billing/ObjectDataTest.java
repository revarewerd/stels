/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.billing;

import com.mongodb.DBObject;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;

/**
 *
 * @author ИВАН
 */
public class ObjectDataTest {
    
    static FileSystemXmlApplicationContext context;

    static ObjectsRepositoryReader objectData;

    static MongoDBManager mdbm;

    public static void main(String[] args) {

        context = new FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml","src/main/webapp/WEB-INF/extdirectspring-controller-servlet.xml");

        objectData = context.getBean(ObjectsRepositoryReader.class);

        mdbm = context.getBean(MongoDBManager.class);

        try {
           getObjectByIMEItest();
        } finally {
            context.close();
        }
    }
    private static void  getObjectByIMEItest(){
      
      System.out.println("getObjectByIMEItest");      
      DBObject obj=objectData.getObjectByIMEI("111");
      System.out.println(obj.toMap().toString());
    } 
}
