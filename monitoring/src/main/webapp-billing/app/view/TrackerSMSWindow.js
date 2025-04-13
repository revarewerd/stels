Ext.define('Billing.view.TrackerSMSWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.trackersmswnd',
//    requires:[
//        'Billing.view.TrackerSMSWindow.SMSWindow'
//    ],
    //title: 'Объект',
    width: 1024,
    height: 768,
    //constrainHeader: true,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        var teltonikaCommands=[
            {text:'Получить координаты', command:'getgps'},
            {text:'Перезагрузка терминала', command:'cpureset'},
            {text:'Команда на блокировку', command:'setdigout 11'},
            {text:'Команда на разблокировку', command:'setdigout 00'},
            {text:'Перепрошить на WRC',
                handler:function(){
                    console.log('Перепрошить на WRC');
                    self.attachToWRC();
                }
            },
            {text: 'Смена IP и порта на WRС',
                handler:function(){
                    console.log('Смена IP и порта на WRС');
                    self.ipAndPortToWRC();
                }
            },
            {text:'Информация о терминале', command:'getver'},
            {text:'Информация о состоянии терминала', command:'getinfo'},
            {text:'Информация о состоянии модуля', command:'getstatus'},
            {text:'Считать значения входов', command:'readio'},
            {text:'Считать значения выходов', command:'getio'},
            {text:'Стереть все записи', command:'deleterecords'}
        ];
        teltonikaCommands.forEach(function(element){
            if(element.handler==undefined)
            element.handler=function(){
                console.log(element.text)
                console.log(self.phone+" "+element.command)
                Ext.MessageBox.confirm(element.text, 'Вы уверены, что хотите отправить комманду?', function (button) {
                    if (button === 'yes') {
                        trackerMesService.sendTeltonikaCMD(self.phone,element.command, function (res,e) {
                            if (e.type === "exception") {
                                console.log("exception=", e);
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: e.message,
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                            else
                            console.log(element.command+"command sended");
                        });
                    }
                });
                              }
        })

        var fmb920Commands=[
            {text:'Получить координаты', command:'getgps'},
            {text:'Перезагрузка терминала', command:'cpureset'},
            {text:'Команда на блокировку', command:'setdigout 1'},
            {text:'Команда на разблокировку', command:'setdigout 0'},
            {text:'Перепрошить на WRC',
                handler:function(){
                    console.log('Перепрошить на WRC');
                    self.attachToWRC();
                }
            },
            {text: 'Смена IP и порта на WRС',
                handler:function(){
                    console.log('Смена IP и порта на WRС');
                    self.ipAndPortToWRC();
                }
            },
            {text:'Информация о терминале', command:'getver'},
            {text:'Информация о состоянии терминала', command:'getinfo'},
            {text:'Информация о состоянии модуля', command:'getstatus'},
            {text:'Считать значения входов', command:'readio'},
            {text:'Считать значения выходов', command:'getio'},
            {text:'Стереть все записи', command:'deleterecords'}
        ];
        fmb920Commands.forEach(function(element){
            if(element.handler==undefined)
                element.handler=function(){
                    console.log(element.text)
                    console.log(self.phone+" "+element.command)
                    Ext.MessageBox.confirm(element.text, 'Вы уверены, что хотите отправить комманду?', function (button) {
                        if (button === 'yes') {
                            trackerMesService.sendFMB920CMD(self.phone,element.command, function (res,e) {
                                if (e.type === "exception") {
                                    console.log("exception=", e);
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else
                                    console.log(element.command+"command sended");
                            });
                        }
                    });
                }
        })


        var ruptelaCommands=[
            {text: 'Получить координаты', command:'coords'},
            {text: 'Перезагрузка терминала', command:'reset'},
            {text: 'Команда на блокировку', command:'setio 0,0'},
            {text: 'Команда на разблокировку', command:'setio 1,1'},
            {text: 'Информация о терминале', command:'version'},
            {text: 'Информация о состоянии модуля', command:'gsminfo'},
            {text: 'Считать значения выходов', command:'getio'},
            {text: 'Получить APN', command:'getAPN'},
            {text: 'Получить IMEI', command:'imei'},
            {text: 'Соединиться с Виалоном', command:'connect 77.243.111.83,20380,TCP'},
            {text: 'Соединиться с WRC', command:'connect 91.230.215.12,9089,TCP'},
            {text: 'Прописать APN на WRC', command:'setconnection m2m.msk,mts,mts,TCP,91.230.215.12,9089'},
            {text: 'Получить данные CAN', command:'caninfo'},
            {text:'Стереть все записи', command:'delrecords'}
        ];
        ruptelaCommands.forEach(function(element){
            if(element.handler==undefined)
                element.handler=function(){
                    console.log(element.text)
                    console.log(self.phone+" "+element.command)
                    Ext.MessageBox.confirm(element.text, 'Вы уверены, что хотите отправить комманду?', function (button) {
                        if (button === 'yes') {
                            trackerMesService.sendRuptelaCMD(self.phone,element.command, function (res,e) {
                                if (e.type === "exception") {
                                    console.log("exception=", e);
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else{
                                console.log(element.command+"command sended");
                                }
                            });
                        }
                    });
                }
        })
        var umkaCommands=[
            {text: 'Команда на блокировку', command:'OUTPUT0 1'},
            {text: 'Команда на разблокировку', command:'OUTPUT0 0'},
        ];
        umkaCommands.forEach(function(element){
            if(element.handler==undefined)
                element.handler=function(){
                    console.log(element.text)
                    console.log(self.phone+" "+element.command)
                    Ext.MessageBox.confirm(element.text, 'Вы уверены, что хотите отправить комманду?', function (button) {
                        if (button === 'yes') {
                            trackerMesService.sendumkaCMD(self.phone,element.command, function (res,e) {
                                if (e.type === "exception") {
                                    console.log("exception=", e);
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else{
                                console.log(element.command+"command sended");
                                }
                            });
                        }
                    });
                }
        })
        var arnaviCommands=[
            {text: 'Команда на блокировку канала №1', command:'123456*SERV*10.1'},
            {text: 'Команда на блокировку канала №2', command:'123456*SERV*9.1'},
            {text: 'Команда на разблокировку канала №1', command:'123456*SERV*9.0'},
            {text: 'Команда на разблокировку канала №2', command:'123456*SERV*10.0'},
        ];
        arnaviCommands.forEach(function(element){
            if(!element.handler)
                element.handler=function(){
                    console.log(element.text)
                    console.log(self.phone+" "+element.command)
                    Ext.MessageBox.confirm(element.text, 'Вы уверены, что хотите отправить комманду?', function (button) {
                        if (button === 'yes') {
                            trackerMesService.sendArnaviCMD(self.phone,element.command, function (res,e) {
                                if (e.type === "exception") {
                                    console.log("exception=", e);
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else{
                                console.log(element.command+"command sended");
                                }
                            });
                        }
                    });
                }
        })
        var commands=[]
        if(self.eqMark.toLowerCase().match('телтоника')) commands=fmb920Commands
        if(self.eqMark.toLowerCase().match('teltonika')) commands=teltonikaCommands
        if(self.eqMark.toLowerCase().match('ruptela')) commands=ruptelaCommands
        if(self.eqMark.toLowerCase().match('глонассsoft')) commands=umkaCommands
        if(self.eqMark.toLowerCase().match('arnavi')) commands=arnaviCommands
        Ext.apply(this,{
            dockedItems:[
                {xtype: 'toolbar',
                            dock: 'top',
                            align: 'left',
                            items: [
                                {
                                    xtype: 'button',
                                    disabled:self.hideRule,
                                    text:'Отправить SMS',
                                    handler:function(){
                                        console.log('Отправить SMS')
                                        self.showSMSWindow()
                                    }
                                },
                                {
                                    icon: 'extjs-4.2.1/examples/page-analyzer/resources/images/refresh.gif',
                                    text: 'Обновить',
                                    itemId: 'refresh',
                                    scope: self,
                                    handler: function () {
                                        this.down('grid').getStore().load();
                                    }
                                },
                                '->',
                                {
                                    xtype: 'button',
                                    disabled:self.hideRule,
                                    text:'Отправить команду',
                                    menu: commands
                                }
                            ]
                }
            ],
            items: [
            {
                xtype:'grid',
                store:Ext.create('EDS.store.TrackerMesService',{
                    autoLoad:true,
                    sorters: { property: 'sendDate', direction: 'DESC' },
                    listeners:{
                        beforeload:function (store, op) {     
                            console.log('phone',self.phone)
                            store.getProxy().setExtraParam("phone", self.phone);
                        }
                    }
                }),
                columns:[
                {
                    header: '№',
                    xtype: 'rownumberer',
                    width:40,
                    resizable:true
                },
                {
                    header: 'Номер отправителя',
                    //width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'senderPhone'
                },
                {
                    header: 'Номер получателя',
                    //width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'targetPhone'
                },
                {
                    header: 'Статус',
                    //width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'status'
                },
                {
                    header: 'Дата отправки',
                    //width:170,
                    flex: 2,
                    sortable: true,
                    dataIndex: 'sendDate',
                    renderer: function (val, metaData, rec) {
                        if (val != null) {
                        var lmd = new Date(val),
                            lms = Ext.Date.format(lmd, "d.m.Y H:i:s");
                        return lms;
                        }   
                        else return '';
                    }
                },
                {
                    header: 'Текст',
                    //width:170,
                    flex: 4,
                    sortable: true,
                    dataIndex: 'text',
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr='title="'+val+'"';
                    return val;
                    }
                } 
                ]
            }
            ]
        })
        this.callParent();    
    },
    showSMSWindow: function () {
        var self = this;
        var phone=self.phone;
        console.log("phone "+phone);
        var existingWindow = WRExtUtils.createOrFocus('SMSSendingWindow' + phone, 'Billing.view.SMSSendingWindow', {
                title: 'Отправить SMS на номер"' + phone/*record.get('name')*/ + '"',
                id: 'TerminalMessages' + phone,//record.get('_id'),
                phone: phone,
                onSmsSent: function (res) {
                    console.log("smsres=", res);
                    self.down('grid').getStore().add(new EDS.model.TrackerMesService(res));
                }
            });
            existingWindow.show();
            return  existingWindow;
    },
    ipAndPortToWRC:function () {
        var self = this;
        var phone=self.phone;
        Ext.MessageBox.confirm('Перепрошить устройство', 'Вы уверены, что хотите перепрошить объект на WRC?', function (button) {
            if (button === 'yes') {
                trackerMesService.ipAndPortToWRC(phone, function (res,e) {
                    if (e.type === "exception") {
                        console.log("exception=", e);
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    else {
                        console.log("IP And Port to WRC command sended");
                    }
                });
            }
        });
    },
    attachToWRC: function () {
        var self = this;
        var phone=self.phone;
        Ext.MessageBox.confirm('Перепрошить устройство', 'Вы уверены, что хотите перепрошить объект на WRC?', function (button) {
            if (button === 'yes') {
                trackerMesService.attachToWRC(phone, function (res,e) {
                    if (e.type === "exception") {
                        console.log("exception=", e);
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    else {
                        console.log("attach to WRC command sended");
                    }
                });
            }
        });
    }
})

