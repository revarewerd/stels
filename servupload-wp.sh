#!/usr/bin/env bash
WRC_SERVER_PATH=${WRC_SERVER_PATH:-niks@niks.ksb-stels.ru:/home/niks/Wayrecallhome}
rsync --delete -avce ssh ./monitoring/target/dist/billingwebapp $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./monitoring/target/dist/monitoringwebapp $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./monitoring/target/dist/workflowapp $WRC_SERVER_PATH/bin
rsync -avce ssh ./monitoring/target/dist/libs $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./monitoring/target/dist/monitoring.jar $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./packreceiver/target/packreceiver.jar $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./modules/odsmosru/target/odsmosru.jar $WRC_SERVER_PATH/bin
rsync --delete -avce ssh ./tools/target/dist/tools.jar $WRC_SERVER_PATH/bin
rsync -avce ssh ./tools/target/dist/libs $WRC_SERVER_PATH/bin
rsync -avce ssh ./conf/localizations/ $WRC_SERVER_PATH/conf/localizations/
