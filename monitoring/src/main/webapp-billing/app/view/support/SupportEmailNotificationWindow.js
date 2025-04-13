/**
 * Created by IVAN on 22.12.2016.
 */
Ext.define('Billing.view.support.SupportEmailNotificationWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.supportemailnotifwnd',
    title: "E-mail уведомления",
    icon: 'images/ico16_eventsmsgs.png',
    maximizable: true,
    minWidth: 400,
    minHeight: 320,
    width: 600,
    height: 400,
    layout: 'fit',
    items: [
        {
            xtype: 'supportemailnotifgrid'
        }
    ],
})

Ext.define('Billing.view.support.SupportEmailNotificationGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.supportemailnotifgrid',
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    requires: [
        'Billing.view.support.SupportEmailNotificationForm'
    ],
    storeName: 'EDS.store.SupportEmailNotificationEDS',
    itemType: 'supportNotificationEmails',
    dockedToolbar: ['add', 'remove', 'refresh'],
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            columns: [
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    width:10
                },
                {
                    header: '№',
                    xtype: 'rownumberer',
                    width: 40,
                    resizable: true
                },
                {
                    header: 'Имя',
                    width: 170,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                        return val
                    }
                },
                {
                    header: 'E-mail',
                    width: 120,
                    sortable: true,
                    dataIndex: 'email',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Категории',
                    minWidth: 160,
                    flex:1,
                    sortable: true,
                    //dataIndex: 'categories',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        var result=[]
                        if(rec.get("equipment")) result.push("Оборудование")
                        if(rec.get("finance")) result.push("Финансы")
                        if(rec.get("program")) result.push("Программа")
                        return result
                    },
                },
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    width:10
                }
            ],
            listeners: {
                // cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                //     var dataIndex = self.columnManager.columns[cellIndex].dataIndex;
                //     switch (dataIndex) {
                //         case "name" :  {self.showSupportEmailNotificationForm(record);}
                //     }
                // },
                celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    self.showSupportEmailNotificationForm(record);
                }
            },
            onSelectChange: function (selModel, selections) {
                //if (hideRule)
                //    this.down('#delete').setDisabled(true);
                //else
                    this.down('#delete').setDisabled(selections.length === 0);
            }
        });
        this.callParent();
    },
    onAddClick:function () {
        this.showSupportEmailNotificationForm();
    },
    onDeleteClick:function () {
        var self=this;
        var selection = this.getView().getSelectionModel().getSelection();
        var ids=[];
        if (selection) {
            for(var i in selection){
                if(selection.hasOwnProperty(i)) {
                    ids[i] = selection[i].get("_id")
                }
            }
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите безвозвратно удалить ' + selection.length + ' записей?', function (button) {
                if (button === 'yes') {
                    supportEmailNotificationEDS.removeEmails(ids,function(res,e){
                        if (e.type === "exception") {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                        else self.refresh();
                    });
                }
            });
        }
    },
    showSupportEmailNotificationForm:function(record){
        var self=this;
        var id="";
        var title = "Новый подписчик";
        if(record!=undefined) {
            id=record.get("_id");
            title="Подписчик " + record.get('name') + '"'
        }
        var existingWindow = WRExtUtils.createOrFocus('supportEmailNtfFormWnd ' + id, 'Billing.view.support.SupportEmailNotificationFormWindow', {
            title:title
            //hideRule: !self.viewPermit
        });
        if (existingWindow.justCreated) {
            existingWindow.down('form').onLoad(id);
            existingWindow.on("close",function(){
                self.refresh();
            })
        }
        existingWindow.show();
        return existingWindow.down('form');

    }
})
