# Запуск локального сервака с бд из прода

## Использование Брокера с сервака

    ssh -N -L 61616:127.0.0.1:61616 niks@wayrecall.ksb-stels.ru
    
## Использование Mongo с сервака:

Добавить ip `172.16.1.4` как `localhost`, где `172.16.1.4:27017` - адрес PRIMARY MongoDb с точки зрения `wayrecall.ksb-stels.ru` 

    ifconfig lo0 alias 172.16.1.4
    
Пробросить туннель с локального на удаленный `172.16.1.4:27017`

    ssh -N -L 172.16.1.4:27017:172.16.1.4:27017 niks@wayrecall.ksb-stels.ru
    
Убедиться, что в локальном `instance.properties`:

    instance.defaultmongodb.servers=172.16.1.4:27017