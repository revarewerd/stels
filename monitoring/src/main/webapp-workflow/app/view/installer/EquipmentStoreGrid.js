/**
 * Created by IVAN on 04.12.2014.
 */
Ext.define('Workflow.view.installer.EquipmentStoreGrid', {
    extend: 'Ext.grid.Panel',
    alias: "widget.eqstoregrid",
    title: 'Склад',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            store: Ext.create('EDS.Ext5.store.ObjectsEquipmentStoreService', {
                    //autoLoad: true,
                    listeners: {
                        beforeload: function (store, operation, eOpts) {
                            var record=self.up("#curTicketPanel").down("#curWorkDesc").getRecord();
                            var install=record.get("install")
                            console.log("rec",record);
                            console.log("install",install);
                            var eqtype=install.eqtype;
                            var eqMark=install.eqMark;
                            var eqModel=install.eqModel;
                            store.getProxy().setExtraParam("eqtype", eqtype)
                            store.getProxy().setExtraParam("eqMark", eqMark)
                            store.getProxy().setExtraParam("eqModel", eqModel)
                        }
                    }
                }
            )
        });
        this.callParent();
    },
    plugins: 'bufferedrenderer',
    features: [
        //{
        //    ftype: 'filters',
        //    encode: true,
        //    local: false
        //},
        {
            ftype: 'summary',
            dock:'bottom'
        }
    ],
    dockedItems:[
        {
            xtype:"toolbar",
            dock:"top",
            items:[
                {
                    text:'Обновить',
                    handler:function(btn){
                        btn.up("grid").getStore().load()
                    }
                }
            ]
        }
    ],
    columns: [
        {
            text: 'IMEI',
            dataIndex: 'eqIMEI',
            flex:1,summaryType: 'count',
            summaryRenderer: function(value, summaryData, dataIndex) {
                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
            }
        },
        { text: 'Тип', dataIndex: 'eqtype',flex:1},
        { text: 'Марка', dataIndex: 'eqMark',flex:1 },
        { text: 'Модель', dataIndex: 'eqModel',flex:1 },
        {
            xtype: 'actioncolumn',
            width: 25,
            icon: 'images/ico16_okcrc.png',
            tooltip: 'Выбрать',
            itemId:"selectEq",
            disabled:true,
            handler: function (view, rowIndex, colIndex,item,e,record,row) {
                var instEqPanel=view.up('#instEqPanel')
                console.log("instEqPanel",instEqPanel)
                console.log("record",record)
                var eqForm=instEqPanel.down("#instWorkForm");
                eqForm.loadRecord(record);
            }
        }
    ]
});