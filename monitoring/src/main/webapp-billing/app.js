Ext.require('Ext.direct.*', function(){
    Ext.direct.Manager.addProvider(Ext.app.REMOTING_API);
});

Ext.application({
    name: 'Billing',
    requires: [
        'Ext.Array',
        'WRExtUtils.WRGrid',
        'Billing.view.account.AllAccountsPanel',
        'Billing.view.object.AllObjectsPanel',
        'Billing.view.equipment.EquipmentStorePanel',
        'Billing.view.user.AllUsersPanel',
        'Billing.view.tariff.AllTariffsPanel',
        'Billing.view.equipment.EquipmentTypesPanel',
        'Billing.view.retranslator.RetranslatorPanel',
        'Billing.view.event.EventsPanel',
        'Billing.view.user.RolesPanel',
        'Billing.view.trash.RecycleBinPanel',
        'WRExtUtils.AtmosphereExtJSBroadcaster'
    ],
    launch: function(){

        //window.atmosphereExtJSBroadcaster = new AtmosphereExtJSBroadcaster();
        var panels=[];
        var ad = rolesService.checkAdminRole(function(isAdmin){
            panels.push(Ext.create('Billing.view.account.AllAccountsPanel', {viewPermit: isAdmin}),
                Ext.create('Billing.view.object.AllObjectsPanel', {viewPermit: isAdmin}),
                Ext.create('Billing.view.object.GroupsOfObjectsPanel', {viewPermit: isAdmin}),
                Ext.create('Billing.view.equipment.EquipmentStorePanel', {viewPermit: isAdmin}),
                Ext.create('Billing.view.user.AllUsersPanel', {viewPermit: isAdmin})
            );
            if (isAdmin) {
                panels.push(
                    Ext.create('Billing.view.tariff.AllTariffsPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.equipment.EquipmentTypesPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.retranslator.RetranslatorPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.user.RolesPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.event.EventsPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.dealer.AllSubdealersPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.trash.RecycleBinPanel', {viewPermit: isAdmin}),
                    Ext.create('Billing.view.support.SupportPanel', {viewPermit: isAdmin})
            );
            }
        window.viewport = Ext.create('Ext.container.Viewport', {
            layout: 'border',
            items: [
                {
                    xtype: 'container',
                    itemId: 'centerpanel',
                    region: 'center',
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'tabpanel',
                            items: panels,
                            tabBar: {
                                items: [
                                    {
                                        xtype: 'tbfill'
                                    },
                                    {
                                        closable: false,
                                        text: 'Выйти',
                                        //icon: 'images/ico24_shutdown.png',
                                        tooltip: 'Выход из системы',
                                        tooltipType: 'title',
                                        handler: function(){
                                            loginService.logout(function(){
                                                window.location = "/billing/login.html";
                                            });
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                },

                {
                    xtype: 'toolbar',
                    itemId: 'activepanel',
                    autoScroll: true,
                    region: 'south',
                    defaults: {
                        cls: 'x-btn-default-toolbar-medium-over',
                        //overCls: 'x-btn-default-toolbar-medium-pressed',
                        scale: 'medium'
                    },
                    border: 'false',
                    items: [
                        {
                            //text: 'Свернуть всё',
                            icon: 'images/ico24_hideall2.png',
                            tooltip: 'Свернуть все окна',
                            disabled: true,
                            handler: function(){
                                Ext.WindowManager.each(function(comp){
                                    //console.log('xtype',comp.getXType())
                                    //console.log('parent',comp.getXTypes().search('window'))
                                    if (comp.xtype == 'window' || comp.getXTypes().search('window') != -1) comp.hide()
                                })
                            }
                        },
                        //{
                        //handler: function () {console.log( Ext.WindowManager )}
                        //},
                        '-'
                    ]
                }
            ]
        });
        })

        var socket = atmosphere;

        var request = new atmosphere.AtmosphereRequest();
        request.timeout = 400000;
        request.url = 'pubsub/servermes';
        request.transport = 'websocket';
        request.fallbackTransport = 'long-polling';
        request.maxReconnectOnClose = 1000000;
        request.reconnect = true;
        request.reconnectInterval = 1000;
        request.logLevel = 'debug';


        request.onReopen = function(req, resp){
            console.log("onReopen:", this, req, resp);
            //this.execute();
            //servermesAtmosphere = req;
            //this.push('message={"time":new Date(),"text":"Переподключеие"}');
        };

        request.onMessage = function(response){
            if (response.status == 200) {
                var data = Ext.JSON.decode(response.responseBody);
                console.log("atmresponse=", data);
                switch (data.eventType) {
                    case 'unreadSupportTickets':
                    case 'aggregateEvent':
                        WRExtUtils.AtmosphereExtJSBroadcaster.fireEvent("dataChanged", data);
                        break;
                    case 'aggregateEventBatch':
//                        data.aggregate = 'object';
//                        data.action = 'batchUpdate';
//                        WRExtUtils.AtmosphereExtJSBroadcaster.fireEvent("dataChanged", data);
////                        Ext.Array.forEach(data.events, function(event) {
////                            event.notrefilter = true;// (i != length - 1);
////                            setImmediate(function(){
////                                //console.log("event:", event);
////                                WRExtUtils.AtmosphereExtJSBroadcaster.fireEvent("dataChanged", event);
////                            });

                        var events = data.events;
                        var blockLen = 10;

                    function updater(events, start, blockLen){
                        if (start < data.events.length) {
                            setTimeout(function(){
                                //console.log("event:", event);
                                WRExtUtils.AtmosphereExtJSBroadcaster.fireEvent("dataChanged", {
                                    aggregate: 'object',
                                    action: 'batchUpdate',
                                    refresh: (start + blockLen >= data.events.length),
                                    events: events.slice(start, start + blockLen)
                                });
                                updater(events, start + blockLen, blockLen);
                            },0);
                        }
                    }

                        updater(events, 0, blockLen);
                        break;
                    case 'textMessage':
                        var msgdata = [];
                        console.log("textMessage", data);
                        msgdata.push({'time': Ext.Date.format(new Date(data.time), "d.m.Y H:i:s"), "text": data.text});
                        var tip = Ext.getCmp('objectMessages');
                        if (!tip/* && resp.data.length > 0*/) {
                            tip = Ext.create('Ext.tip.ToolTip', {
                                renderTo: Ext.getBody(),
                                id: 'objectMessages',
                                style: {opacity: '0.8'},
                                header: {title: 'Новые сообщения'},
                                autoHide: false,
                                autoScroll: true,
                                closable: true,
                                resizable: true,
                                draggable: true,
                                border: false,
                                padding: false,
                                width: 480,
                                maxWidth: 480,
                                maxHeight: 240,
                                items: [
                                    {
                                        xtype: 'grid',
                                        autoScroll: true,
                                        hideHeaders: false,
                                        disableSelection: true,
                                        rowLines: true,
                                        border: false,
                                        padding: false,
                                        columns: [
                                            {text: 'Время', dataIndex: 'time', width: 120},
                                            {text: 'Текст сообщения', dataIndex: 'text', flex: 1}
                                        ],
                                        store: {
                                            fields: ['time', 'text'],
                                            data: {
                                                'items': msgdata
                                            },
                                            proxy: {
                                                type: 'memory',
                                                reader: {
                                                    type: 'json',
                                                    root: 'items'
                                                }
                                            }
                                        }
                                    }
                                ],
                                listeners: {
                                    hide: function(comp){
                                        comp.down('grid').getStore().removeAll();
                                        comp.destroy();
                                    }
                                }
                            });
                            var w = viewport.getWidth();
                            tip.showAt([w - 500, 44]);
                        } else {
                            tip.down('grid').getStore().add(msgdata);
                            tip.down('grid').getStore().sort('time', 'DESC');
                        }
                        break;

                }
            }
        };


        servermesAtmosphere = socket.subscribe(request);
        //servermesAtmosphere.push('message={"time":new Date(),"text":"Подключение"}');

    }

//autoCreateViewport: true

});
