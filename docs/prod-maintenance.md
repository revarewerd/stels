#  Известные проблемы при поддержании работы prod-сервера

## На сервере закончилось место

Для чистки логов есть специальный скрипт:

    sudo -u wayrecall bash /opt/wayrecall/bin/scripts/cleanup.sh

Который удалит старые логи.

Еще бывает нужно почистить логи докера в `/var/lib/docker/containers/[id-контейнера]/[id-контейнера]-json.log`,
Это можно сделать путем `echo "" > /var/lib/docker/containers/[id-контейнера]/[id-контейнера]-json.log`
Может быть вместо этого стоит настроить ротацию логов докера

## Истек сертификат https

Нужно перезапустить сервер с параметром `global.security.acme.server=acme://letsencrypt.org`,
а потом перезапустить его еще раз
Это можно сделать раскомментировал соответствующую строчку в файле `ansible/templates/global.properties.j2` 
и перезапустить сервер через ansible:

    ansible-playbook playbook.yaml -i ./inventrories/prod/prod/inventory.yaml -l wrc_server  --tags="config"

Потом закомментировать её обратно и выполнить эту команду еще раз.
Либо отредактировать `global.properties` на сервере напрямую

    sudo -u wayrecall-static vi /opt/wayrecall/conf/global.properties

