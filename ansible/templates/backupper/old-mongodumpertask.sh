#!/bin/bash
for i in Seniel-dev2 Seniel-3gprotex Seniel-autofon Seniel-hittele Seniel-m2m-trade;
do
  ssh niks@wayrecall.ksb-stels.ru java -Xmx32m -jar ./mongodbdumper-1.0-SNAPSHOT-jar-with-dependencies.jar -v -zip -d $i -c objects -c accounts -c equipments -c equipmentTypes -c eqhistory -c events -c tariffs -c users -c usersPermissions -c balanceHistory -c balanceHistoryWithDetails -c notificationRules -c notificationRulesStates -c smses -c domainEvents -c snapshotEvents > dump-$i-`date +%Y-%m-%d_%H-%M-%S`.zip
done
exit

#ssh niks@wayrecall.ksb-stels.ru java -Xmx32m -jar ./mongodbdumper-1.0-SNAPSHOT-jar-with-dependencies.jar --host 172.16.1.5  -d Seniel-dev2 -p objPacks.o.* -q "{time:{'\$gte':{'\$date':\\\"2020-06-01T00:00:00Z\\\"}}}" -zip -v > db.zip