/**
 * Created by IVAN on 27.04.2015.
 */
Ext.define('Seniel.view.mapobject.SendCommandWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: ['Seniel.view.WRWindow'],
    okhandler: null,
    border: 0,
    height:150,
    width: 300,
    commandPasswordNeeded:false,
    maximizable: false,
    //modal: true,
    // resizable: false,
    layout: 'fit',
    closable: true,
    title: tr('mapobject.objectactions.sendcommand'),
    initComponent:function() {
        var self = this;
        Ext.apply(this, {
            items: [
                {
                    xtype: 'form',
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    fieldDefaults:{
                        margin:'10 10 10 10'
                    },
                    items: [
                        {
                            xtype: 'displayfield',
                            itemId: 'question',
                            value: tr('mapobject.objectactions.sendcommand.sure')
                        },
                        {
                            xtype: 'textfield',
                            labelWidth: 70,
                            fieldLabel: tr('mapobject.objectactions.blocking.password'),
                            name: 'password',
                            inputType: 'password',
                            tooltip: tr('mapobject.objectactions.blocking.password.tooltip'),
                            hidden: !self.commandPasswordNeeded,
                            disabled:!self.commandPasswordNeeded
                        }
                    ]
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
                            handler: function (btn) {
                                var command = self.command;
                                var uid=self.record.get("uid");
                                var password=null;
                                if(self.commandPasswordNeeded){
                                    password=self.down("[name=password]").getValue()
                                }
                                switch (command) {
                                    case "getcoords":
                                    {
                                        objectsCommander.sendGetCoordinatesCommand(uid, password, function (res,e) {
                                            console.log("result=", res)
                                            if (!e.status) {
                                                Ext.MessageBox.show({
                                                    title: tr('mapobject.objectactions.error'),
                                                    msg: e.message,
                                                    icon: Ext.MessageBox.ERROR,
                                                    buttons: Ext.Msg.OK
                                                });
                                            } else {
                                                self.close()
                                            }
                                        })
                                        break;
                                    }
                                    case "restartterminal":
                                    {
                                        objectsCommander.sendRestartTerminalCommand(uid, password, function (res,e) {
                                            console.log("result=", res);
                                            if (!e.status) {
                                                Ext.MessageBox.show({
                                                    title: tr('mapobject.objectactions.error'),
                                                    msg: e.message,
                                                    icon: Ext.MessageBox.ERROR,
                                                    buttons: Ext.Msg.OK
                                                });
                                            } else {
                                                self.close();
                                            }
                                        })
                                        break;
                                    }
                                }
                            }
                        },
                        {
                            text: tr("main.cancel"),
                            handler: function (btn) {
                                btn.up('window').close()
                            }
                        }
                    ]

                }
            ]
        })
        this.callParent();
    }
})