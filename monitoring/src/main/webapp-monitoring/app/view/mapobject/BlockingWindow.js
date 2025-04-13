/**
 * Created by IVAN on 15.04.2015.
 */
Ext.define('Seniel.view.mapobject.BlockingWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: ['Seniel.view.WRWindow'],
    title: 'Команды',
    width: 400,
    height: 205,
    layout: 'fit',
    //resizable:false,
    maximizable: false,
    icon: 'images/ico32_lock.png',
    btnConfig: {
        icon: 'images/ico16_lock.png',
        text: tr("mapobject.objectactions.blocking")
    },
    commandPasswordNeeded:false,
    initComponent:function(){
        var self=this;
        var defaultComboValue="unblock"
        if(!this.record.get("blocked"))
            defaultComboValue="block"
        var noAddParamsField=
            {
                xtype:'displayfield',
                value:'<center>'+tr("mapobject.objectactions.blocking.noadditionaloptions")+'</center>'
            }
        var commandPasswordField =
                {
                    xtype:'textfield',
                    fieldLabel: tr('mapobject.objectactions.blocking.password'),
                    labelWidth:70,
                    name:'commandPassword',
                    inputType: 'password',
                    tooltip: tr('mapobject.objectactions.blocking.password.tooltip')
                }
        var blockTypes={
            'block':[],
            'unblock':[] ,
            'blockAtTime' : [
                {
                    xtype: 'timefield',
                    margin:'10 10 0 10',
                    labelWidth:70,
                    fieldLabel: tr('mapobject.objectactions.blocking.password.time'),
                    format: 'H:i',
                    value: new Date(),
                    name: 'time',
                    tooltip: tr('mapobject.objectactions.blocking.password.time')
                }
            ],
            'blockAtStop':[],
            'blockWhenIgnitionOff':[],
            'cancel':[]
        };
        eventedObjectCommander.countTasks(self.record.get('uid'), function (i) {
            console.log("i",i)
            if (i > 0) {
                self.down("#blockType").getStore().add(
                    {
                        name: tr('mapobject.objectactions.blocking.command.cancel'),
                        value: "cancel"
                    }
                );
            }
        });
        if(self.commandPasswordNeeded) {
            for(i in blockTypes){
                blockTypes[i].push(commandPasswordField)
            }
        }
        else {
            for(i in blockTypes){
                if(blockTypes[i].length==0) blockTypes[i].push(noAddParamsField)
            }
        }
        var callbacker = function (submitResult, e) {
            console.log("Результат", submitResult)
            if (!e.status) {
                Ext.MessageBox.show({
                    title: tr('mapobject.objectactions.error'),
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            } else {
                self.record.set('blocked', 'wait');
                self.close()
            }
        };

        Ext.apply(this, {
            items: [
                {
                    xtype: 'form',
                    layout: {
                        type: "vbox",
                        align: "stretch"
                    },
                    defaults: {
                        textAlign: 'left'
                    },
                    items: [
                        {
                            xtype: 'combobox',
                            itemId:'blockType',
                            margin: "10 10 10 10",
                            fieldLabel: tr('mapobject.objectactions.blocking.type'),
                            name: 'blockType',
                            valueField: 'value',
                            displayField: 'name',
                            forceSelection: true,
                            allowBlank:false,
                            anyMatch:true,
                            queryMode:'local',
                            //value:defaultComboValue,
                            store: Ext.create('Ext.data.Store', {
                                fields: ['value', 'name'],
                                data: [
                                    {name: tr('mapobject.objectactions.blocking.command.block'), value: "block"},
                                    {name: tr('mapobject.objectactions.blocking.attime'), value: "blockAtTime"},
                                    {name: tr('mapobject.objectactions.blocking.whenstops'), value: "blockAtStop"},
                                    {name: tr('mapobject.objectactions.blocking.whenignitionoff'), value: "blockWhenIgnitionOff"},
                                    {name: tr("mapobject.objectactions.blocking.command.unblock"), value: "unblock"},
                                ]
                            }),
                            listeners:{
                                change:function(cmb, newValue, oldValue, eOpts){
                                    var blockParamsForm=cmb.up("window").down('#blockParamsForm');
                                    blockParamsForm.removeAll();
                                    blockParamsForm.add(blockTypes[newValue]);
                                    blockParamsForm.updateLayout();
                                },
                                afterrender:function(cmb,e){
                                    cmb.setValue(defaultComboValue)
                                }
                            }
                        },
                        {
                            xtype: 'form',
                            flex:1,
                            title: tr("mapobject.objectactions.blocking.options"),
                            itemId:"blockParamsForm",
                            layout:{
                                type:'vbox',
                                align:'stretch',
                                pack:'center'
                            },
                            fieldDefaults:{
                                margin: "10 10 10 10"
                            },
                            items: []
                        }
                    ],
                    dockedItems: [
                        {
                            xtype: 'toolbar',
                            dock: 'bottom',
                            items: [
                                '->',
                                {
                                    text: tr("main.apply"),
                                    handler:function(btn) {
                                        var cmb=self.down("#blockType");
                                        var blockType=cmb.getValue();
                                        var password=undefined;
                                        var uid=self.record.get('uid');
                                        if(self.commandPasswordNeeded){
                                            password=self.down('textfield[name=commandPassword]').getValue()
                                        }
                                        switch(blockType)
                                        {
                                            case "block": {
                                                objectsCommander.sendBlockCommand(uid, true, password , callbacker);
                                                break;
                                            }
                                            case "blockAtTime": {
                                                var time=self.down('textfield[name=time]').getValue();
                                                var date=new Date();
                                                var res=date.setHours(time.getHours(),time.getMinutes());
                                                var defaultTimezone = Timezone.getDefaultTimezone();
                                                if (defaultTimezone) {
                                                    var fdate = Ext.Date.format(date, "d.m.Y");
                                                    var ftime = Ext.Date.format(time, "H:i");
                                                    res = moment.tz(fdate + " " + ftime, "DD.MM.YYYY HH:mm", defaultTimezone.name).toDate().getTime();
                                                }
                                                eventedObjectCommander.sendBlockCommandAtDate(uid, true, password,res, callbacker);
                                                break;
                                            }
                                            case "blockAtStop": {
                                                eventedObjectCommander.sendBlockAfterStop(uid, true,password,callbacker);
                                                break;
                                            }
                                            case "blockWhenIgnitionOff": {
                                                eventedObjectCommander.sendBlockAfterIgnition(uid, true,password,callbacker);
                                                break;
                                            }
                                            case "unblock": {
                                                objectsCommander.sendBlockCommand(uid, false, password, callbacker);
                                                break;
                                            }
                                            case "cancel":{
                                                eventedObjectCommander.cancelTasks(uid, function (res,e) {
                                                    if (!e.status) {
                                                        Ext.MessageBox.show({
                                                            title: tr('mapobject.objectactions.error'),
                                                            msg: e.message,
                                                            icon: Ext.MessageBox.ERROR,
                                                            buttons: Ext.Msg.OK
                                                        });
                                                    } else {
                                                        self.record.set('blocked', false);
                                                        self.close()
                                                    }
                                                });
                                                break;
                                            }
                                            default : console.log("no action")
                                        }
                                    }
                                },
                                {
                                    text:tr("main.cancel"),
                                    handler:function(btn){
                                        btn.up('window').close()
                                    }
                                }
                            ]
                        }
                    ]

                }
            ]
        });
        this.callParent();
    }
})