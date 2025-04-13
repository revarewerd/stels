package ru.sosgps.wayrecall.billing.security;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
import org.bson.types.ObjectId;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import ru.sosgps.wayrecall.core.MongoDBManager;

import java.util.List;
import java.util.Map;

import static ru.sosgps.wayrecall.utils.ScalaConverters.asJavaList;
import static  ru.sosgps.wayrecall.utils.ScalaConverters.asScalaMapImm;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 30.03.13
 * Time: 15:47
 * To change this template use File | Settings | File Templates.
 */
public class PermissionsEditorTest {

    static FileSystemXmlApplicationContext context;

    static PermissionsEditor permissionsEditor;

    static MongoDBManager mdbm;

    public static void main(String[] args) {

        context = new FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml");

        permissionsEditor = context.getBean(PermissionsEditor.class);

        mdbm = context.getBean(MongoDBManager.class);

        try {
            test2();
        } finally {
            context.close();
        }


    }


    private static void test2() {
        //Seq<ObjectId> account = permissionsEditor.getPermittedUsers("account", new ObjectId("5134fbd924ac02857da84a7b"));

        ObjectId objectId1 = new ObjectId("502b733ce4b023201480a583");

        System.out.println(getObject(objectId1).get("name"));


        ObjectId userObjectId = new ObjectId("516e977e84ae863382a2abde");

        System.out.println(getUser(userObjectId));
        
        Map <String, Object> TestMap= new HashMap<String, Object>();
        TestMap.put("view", true);
        TestMap.put("sleepersView", true);
        TestMap.put("control", true);
        permissionsEditor.setPermissions(
                userObjectId, "object", objectId1, asScalaMapImm(TestMap)
        );

        for (DBObject dbo : mdbm.getDatabase().underlying().getCollection("objects").find()) {
            System.out.println(dbo.get("name") + " - " + permissionsEditor.getPermissionsRecord(userObjectId, "object", (ObjectId) dbo.get("_id")));
        }

    }

    private static void test1() {
        //Seq<ObjectId> account = permissionsEditor.getPermittedUsers("account", new ObjectId("5134fbd924ac02857da84a7b"));

        ObjectId objectId1 = new ObjectId("513640d8509a947e0a022dc5");

        System.out.println(getObject(objectId1).get("name"));


        List<ObjectId> objectIds = asJavaList(permissionsEditor.getPermittedUsers("object", objectId1));

        System.out.println("size=" + objectIds.size());

        for (ObjectId objectId : objectIds) {

            System.out.println(getUser(objectId).get("name"));
        }
    }

    private static DBObject getUser(ObjectId objectId) {
        return mdbm.getDatabase().underlying().getCollection("users")
                .findOne(
                        new BasicDBObject("_id", objectId));
    }

    private static DBObject getObject(ObjectId objectId1) {
        return mdbm.getDatabase().underlying().getCollection("objects")
                .findOne(
                        new BasicDBObject("_id", objectId1));
    }

}
