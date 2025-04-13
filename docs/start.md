# Руководство разработчика

## Работа с исходным кодом

Исходные коды проекта располагаются по адресу [https://bitbucket.org/ksbstels/wayrecall/src/master/](https://bitbucket.org/ksbstels/wayrecall/src/master/)

Для работы с исходным кодом используется система контроя версий [Git](https://git-scm.com/).
После установки Git и имея доступ к репозиторию на bitbucket.org можно получить локальную копию командой

    git clone https://username@bitbucket.org/ksbstels/wayrecall.git

или

    git clone git@bitbucket.org:ksbstels/wayrecall.git
    
после этого в каталоге `wayrecall` на вашем компьютере будет располагаться полная копия исходников проекта

После редактирования исходников сделайте `git push` чтобы ваши изменения попали в репозиторий на bitbucket.org
Если вы добавили новые файлы - не забудьте выполнить `git add` и `git commit`


## Сборка

Для сборки проекта вам потребуется установленный

- Java Development Kit версии 8. Например отсюда [https://adoptopenjdk.net](https://adoptopenjdk.net)
- [Maven](https://maven.apache.org/download.cgi)
- опционально: [Zinc](https://github.com/typesafehub/zinc) - ускоряет компиляцию

    wget -c http://downloads.typesafe.com/zinc/0.3.13/zinc-0.3.13.tgz -O - | tar -xz -C ~/apps

Сборка осуществляется командой 

    mvn wrapper:wrapper -Dmaven=3.5.4 install -DskipTests
    
    
## Запуск приложения

### Переменные окружения

Для работы приложения должны быть заданы следующие переменные окружения:

    WAYRECALL_HOME=<путь к рабочему каталогу, в котором нвходится папка conf>
    ThirdPartyJS=<путь к папке, где хранятся сторонние  JS-файлы: OpenLayers и Ext-js>
    
ThirdPartyJS проще всего стянуть с сервера, на котором уже развернут Wayrecall. 
Еще можно из [http://maven.uits-labs.ru/ru/sosgps/wayrecall/ThirdPartyJS.txz](http://maven.uits-labs.ru/ru/sosgps/wayrecall/ThirdPartyJS.txz)
    
### Базы данных

Для работы приложения необходима база данных Mongodb версии 3.6 и Postgres с установленным плагином Postgis
адреса и пароли для доступа в базы данных задаются в конфигурационном файле`conf/global.properties`

Кроме этого база данных Postgres должна быть инициализирована скриптом `monitoring/src/main/resources/ru/sosgps/wayrecall/monitoring/geozones.sql`

Для упрощения установки и запуска баз данных можно использовать заготовленный файл `docker-compose.yml` из корня проекта

### Запуск серверов

Для запуска приложения нужно запустить два процесса:

* **packreciver**: main-класс `ru.sosgps.wayrecall.packreceiver.Main` с параметром `receiverserver`.
  В продакшене он запускается через `java -jar ${WAYRECALL_HOME}/bin/packreceiver.jar receiverserver`
  см [packreceiver.service](../ansible/systemd-definitions/packreceiver.service).

* **web-server**: main-класс `ru.sosgps.wayrecall.initialization.JettyServer`. Полезные параметры для режима разработки можно
посмотреть в [startmods.md](startmods.md).
  В продакшене он запускается через `java -jar ${WAYRECALL_HOME}/bin/monitoring.jar 9080`
  см [wayrecall-web.service](../ansible/systemd-definitions/wayrecall-web.service).
  
При первом запуске нужно пройти по пути [http://localhost:5193/billing/](http://localhost:5193/billing/),
логин пароль администраторов хранятся по пути `${WAYRECALL_HOME}/conf/wrcinstances/default/billingAdmins.properties`
и создать там пользователя для работы в основном сервисе: [http://localhost:5193/](http://localhost:5193/)


    