#!/usr/bin/env bash
read -s -p "Enter Password: " PASS
sshpass -p $PASS rsync --delete -avce ssh ./monitoring/target/dist/billingwebapp niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync --delete -avce ssh ./monitoring/target/dist/monitoringwebapp niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync --delete -avce ssh ./monitoring/target/dist/workflowapp niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync -avce ssh ./monitoring/target/dist/libs niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync --delete -avce ssh ./monitoring/target/dist/monitoring.jar niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync --delete -avce ssh ./packreceiver/target/packreceiver.jar niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync --delete -avce ssh ./tools/target/dist/tools.jar niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync -avce ssh ./tools/target/dist/libs niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/bin
sshpass -p $PASS rsync -avce ssh ./conf/localizations/ niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome/conf/localizations/
