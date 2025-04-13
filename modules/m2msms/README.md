# Перегенерация клиента

Для того чтобы заново сгененировать SOAP клиент необходимо в папке `modules/m2msms/src/main/java` выполнить команду:

    wsdl2java -client  -p ru.sosgps.wayrecall.m2msms.generated ../resources/ru/sosgps/wayrecall/m2msms/generated/m2m_api.wsdl
    
Параметер `-client` опционален. Если в процессе произошла ошибка `undefined simple or complex type 'soapenc:Array'`
то следует пропатчить wsdl, убрав упоминания `soapenc:Array`:

```patch
--- modules/m2msms/src/main/resources/ru/sosgps/wayrecall/m2msms/generated/m2m_api.wsdl	(revision 2782+:34d3cca15983+)
+++ modules/m2msms/src/main/resources/ru/sosgps/wayrecall/m2msms/generated/m2m_api.wsdl	(revision 2782:34d3cca15983a4781c9229c21e8342364a5044ab)
@@ -1,4 +1,3 @@
-
 <?xml version="1.0" encoding="utf-8"?>
 <wsdl:definitions xmlns:s="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/" xmlns:http="http://schemas.xmlsoap.org/wsdl/http/" xmlns:mime="http://schemas.xmlsoap.org/wsdl/mime/" xmlns:tns="http://mcommunicator.ru/M2M" xmlns:s1="http://mcommunicator.ru/M2M/AbstractTypes" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tm="http://microsoft.com/wsdl/mime/textMatching/" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/" targetNamespace="http://mcommunicator.ru/M2M" xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">
     <wsdl:types>
@@ -669,13 +668,9 @@
         <s:schema targetNamespace="http://mcommunicator.ru/M2M/AbstractTypes">
             <s:import namespace="http://schemas.xmlsoap.org/soap/encoding/" />
             <s:complexType name="StringArray">
-                <s:complexContent mixed="false">
-                    <s:restriction base="soapenc:Array">
                         <s:sequence>
                             <s:element minOccurs="0" maxOccurs="unbounded" name="String" type="s:string" />
                         </s:sequence>
-                    </s:restriction>
-                </s:complexContent>
             </s:complexType>
         </s:schema>
     </wsdl:types>

```

После этого, чтобы сервис загружал локальный wsdl, нужно указать его в `MTSX0020CommunicatorX0020M2MX0020XMLX0020API`:

```patch
--- modules/m2msms/src/main/java/ru/sosgps/wayrecall/m2msms/generated/MTSX0020CommunicatorX0020M2MX0020XMLX0020API.java	(revision 2782+:34d3cca15983+)
+++ modules/m2msms/src/main/java/ru/sosgps/wayrecall/m2msms/generated/MTSX0020CommunicatorX0020M2MX0020XMLX0020API.java	(revision 2782:34d3cca15983a4781c9229c21e8342364a5044ab)
@@ -27,13 +27,11 @@
     public final static QName MTSX0020CommunicatorX0020M2MX0020XMLX0020APIHttpPost = new QName("http://mcommunicator.ru/M2M", "MTS_x0020_Communicator_x0020_M2M_x0020_XML_x0020_APIHttpPost");
     public final static QName MTSX0020CommunicatorX0020M2MX0020XMLX0020APIHttpGet = new QName("http://mcommunicator.ru/M2M", "MTS_x0020_Communicator_x0020_M2M_x0020_XML_x0020_APIHttpGet");
     static {
-        URL url = null;
-        try {
-            url = new URL("file:../resources/ru/sosgps/wayrecall/m2msms/generated/m2m_api.wsdl");
-        } catch (MalformedURLException e) {
+        URL url = MTSX0020CommunicatorX0020M2MX0020XMLX0020API.class.getResource("m2m_api.wsdl");
+        if (url == null) {
             java.util.logging.Logger.getLogger(MTSX0020CommunicatorX0020M2MX0020XMLX0020API.class.getName())
                 .log(java.util.logging.Level.INFO, 
-                     "Can not initialize the default wsdl from {0}", "file:../resources/ru/sosgps/wayrecall/m2msms/generated/m2m_api.wsdl");
+                     "Can not initialize the default wsdl from {0}", "m2m_api.wsdl");
         }
         WSDL_LOCATION = url;
     }
```

Если нужно потестить клиентов: то не забудьте указать логин и пароль:

        java.lang.String _getBalance_login = "79164108305";
        java.lang.String _getBalance_password = ru.sosgps.wayrecall.utils.io.Utils.toHexString(MessageDigest.getInstance("MD5").digest("752336".getBytes()), "");

Тестировать проще всего на методе `getBalance`