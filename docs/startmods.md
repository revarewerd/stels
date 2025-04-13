## Параметры запуска в режиме разработки

### Отключение прелодинга последних сообщений

`-Dwayrecall.skipCachePreload=true`

### Редактирование веб-страницы без перезапуска

Параметры при запуске jetty:

`-Dwayrecall.webappres.monitoringwebapp=monitoring/src/main/webapp-monitoring`

`-Dwayrecall.webappres.billingwebapp=monitoring/src/main/webapp-billing`

`-Dwayrecall.webappres.workflowapp=monitoring/src/main/webapp-workflow`

*Возможно, для правильной работы нужно будет указать полный путь*

### Headless Mode

    -Djava.awt.headless=true



