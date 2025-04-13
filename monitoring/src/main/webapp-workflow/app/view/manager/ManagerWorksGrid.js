/**
 * Created by IVAN on 17.12.2014.
 */
Ext.create('Ext.data.Store', {
    storeId:'manWorksStore',
    fields:['_id','object','objectName','workType', 'eqtype', 'eqMark','eqModel','eqIMEI'],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'items'
        }
    }
});

Ext.define('Workflow.view.manager.ManagerWorksGrid', {
    extend: 'Ext.grid.Panel',
    alias: "widget.manworksgrid",
    title: 'Описание работы',
    itemId:'manWorksGrid',
    store: Ext.data.StoreManager.lookup('manWorksStore'),
    columns: [
        { text:'Объект',dataIndex: 'objectName',flex:3},
        { text: 'Работа', dataIndex: 'workType',flex:2,
            renderer: function (val, metaData, rec) {
               switch(val) {
                   case "install" :
                       return  "Установка"
                   case "remove":
                       return "Удаление"
                   case "replace":
                       return "Замена"
                }

            }
        },
        { text: 'Установка', dataIndex: 'install',flex:3,
            renderer: function (val, metaData, rec) {
                if(val!=null && val!=undefined)
                {
                    var result=val.eqtype;
                    if(val.eqMark!=null && val.eqMark!="")
                        {
                            result=val.eqMark
                            if(val.eqModel!=null && val.eqModel!="")
                                result=result+" "+val.eqModel
                        }
                    return result;
                }
                else return val
            }
        },
        { text: 'Удаление', dataIndex: 'remove',flex:3,
            renderer: function (val, metaData, rec) {
                if(val!=null && val!=undefined)
                {
                    var result=val.eqtype;
                    if(val.eqMark!=null)
                    {
                        result=val.eqMark
                        if(val.eqModel!=null)
                            result=result+" "+val.eqModel
                    }
                    return result;
                }
                else return val
            }
        },
        { text: 'Статус', dataIndex: 'workStatus',flex:2,
            renderer: function (val, metaData, rec) {
                switch(val) {
                    case "complete" :
                        return "Выполнена"
                    case "notcomplete":
                        return "Не выполнена"
                }
            }
        },
        {
            xtype:'actioncolumn',
            width:25,
            items: [{
                icon: 'images/ico16_show.png',  // Use a URL in the icon config
                tooltip: 'Просмотр',
                handler: function(view, rowIndex, colIndex,item,e,record,row) {
                    view.getSelectionModel().select(rowIndex);
                    view.up('grid').showRecord(record);
                }
            }
            ]
        },
        {
            xtype:'actioncolumn',
            width:25,
            items: [{
                icon: 'images/ico16_crossinc.png',
                tooltip: 'Удалить',
                handler: function (view, rowIndex, colIndex, item, e, record, row) {
                    view.getStore().remove(record);
                }
            }
            ]
        }

    ],
    showRecord:function(record){
        var grid=this
        var manWorksPanel=grid.up('#manWorksPanel');
        manWorksPanel.reset();
        console.log("record",record);
        manWorksPanel.loadData(record);
    },
    loadData:function(data){
        var self=this
        var works=data.get("works")
        console.log("works",works)
        var worksRec= new Array()
        for(i in works){
            worksRec.push(Ext.create('Work',works[i]))
        }
        self.getStore().loadData(worksRec)
    }
});