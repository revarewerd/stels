Ext.define('Billing.view.SleeperMesWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.sleepermeswnd',
    width: 1024,
    height: 768,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
            items:[
                {   xtype: 'tabpanel',
                    border: false,
                    padding: false,
                    listeners: {
                        afterrender: function (panel) {
                            objectData.getObjectSleepers(self.uid,function(data,e){
                                for(i in data){
                                    panel.add(
                                        {
                                            xtype:'sleepermespanel',
                                            hideRule:self.hideRule,
                                            title:data[i].eqMark+data[i].eqModel+' :"'+data[i].simNumber+'"',
                                            uid:self.uid,
                                            phone:data[i].simNumber
                                        })
                                    panel.setActiveTab(0)
                                }
                            })
                        }
                    }
                }        ]
        })
        this.callParent();
    }
})
Ext.define('Billing.view.SleeperMesPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.sleepermespanel',
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
            dockedItems: [
                {xtype: 'toolbar',
                    dock: 'top',
                    align: 'left',
                    items: [
                        {
                            xtype: 'button',
                            itemId: 'sendSMSButton',
                            disabled:self.hideRule,
                            text: 'Отправить SMS',
                            handler: function () {
                                console.log('Отправить SMS')
                                self.showSMSWindow(self.phone)
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
                        }
                    ]
                }
            ],
            items: [
                {
                    xtype: 'grid',
//                    store: Ext.create('EDS.store.SleeperMessages', {
//                        autoLoad: true,
//                        listeners: {
//                            beforeload: function (store, op) {
//                                console.log('uid', self.uid)
//                                store.getProxy().setExtraParam("uid", self.uid);
//                            },
//                            load: function (store, records, successful, eOpts) {
//                                console.log('records', records)
//                                store.filter([{
//                                    filterFn: function(rec) {
//                                        return (self.phone.match(rec.get('senderPhone')))?(true):(false);
//                                    }
//                                }]);
//                                var button= self.down('button[itemId=sendSMSButton]')
//                                button.phone=self.phone
//                                button.setHandler( function (b) {
//                                                console.log('Отправить SMS')
//                                                self.showSMSWindow(b.phone)
//                                            })
//                            }
//                        }
//                    }),
                    store:Ext.create('EDS.store.SleeperMesService',{
                        autoLoad:true,
                        sorters: { property: 'sendDate', direction: 'DESC' },
                        listeners:{
                            beforeload:function (store, op) {
                                console.log('phone',self.phone)
                                store.getProxy().setExtraParam("phone", self.phone);
                            }
                        }
                    }),
                    columns: [
                        {
                            header: '№',
                            xtype: 'rownumberer',
                            width: 40,
                            resizable: true
                        },
                        {
                            header: 'Номер отправителя',
                            //width:170,
                            flex: 3,
                            sortable: true,
                            dataIndex: 'senderPhone'
                        },
                        {
                            header: 'Номер получателя',
                            //width:170,
                            flex: 3,
                            sortable: true,
                            dataIndex: 'targetPhone'
                        },
                        {
                            header: 'Статус',
                            //width:170,
                            flex: 2,
                            sortable: true,
                            dataIndex: 'status'
                        },
                        {
                            header: 'Дата отправки',
                            //width:170,
                            flex: 3,
                            sortable: true,
                            dataIndex: 'sendDate',
                            renderer: function (val, metaData, rec) {
                                if (val != null) {
                                    var lmd = new Date(val),
                                        lms = Ext.Date.format(lmd, "d.m.Y H:i:s")
                                    return lms;
                                }
                                else return '';
                            }
                        },
                        {
                            header: 'Текст',
                            //width:170,
                            flex: 12,
                            sortable: true,
                            dataIndex: 'text',
                            renderer: function (val, metaData, rec) {
                                metaData.tdAttr = 'title="' + val + '"'
                                return val
                            }
                        },
                        {
                            header: 'Положение',
                            width: 80,
                            //flex: 20,
                            sortable: true,
                            dataIndex: 'lonlat',
                            renderer: function (val, metaData, rec) {
                                if (val)
                                    return '<a href="EDS/showbylbs?id=' + encodeURIComponent(rec.get('smsId')) + '" target="_blank"> ' + val + ' </a>';
                                else
                                    return '';
                            }
                        }
                    ]
                }
            ]
        })
        this.callParent();
    },
    showSMSWindow: function (phone) {
        console.log("phone "+phone);
        var existingWindow = WRExtUtils.createOrFocus('SMSSendingWindow' + phone, 'Billing.view.SMSSendingWindow', {
            title: 'Отправить SMS на номер"' + phone/*record.get('name')*/ + '"',
            id: 'TerminalMessages' + phone,//record.get('_id'),
            phone: phone,
            onSmsSent: function (res) {
                console.log("smsres=", res);
                //self.down('grid').getStore().add(new EDS.model.TrackerMesService(res));
            }
        });
        existingWindow.show();
        return  existingWindow;
    }
})

