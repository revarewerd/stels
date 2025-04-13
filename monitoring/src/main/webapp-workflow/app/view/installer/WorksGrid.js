/**
 * Created by IVAN on 04.12.2014.
 */

Ext.create('Ext.data.Store', {
    storeId:'worksStore',
    fields:['_id','object','objectName','workType', 'eqtype', 'eqMark','eqModel','eqIMEI',"workStatus"],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'items'
        }
    }
});

Ext.define('Workflow.view.installer.WorksGrid', {
    extend: 'Ext.grid.Panel',
    alias: "widget.worksgrid",
    title: 'Описание работы',
    store: Ext.data.StoreManager.lookup('worksStore'),
    columns: [
        { text:'Объект',dataIndex: 'objectName',flex:2},
        { text: 'Тип работы', dataIndex: 'workType',flex:1,
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
                    if(val.eqMark!=null)
                    {
                        result=result+" : "+val.eqMark
                        if(val.eqModel!=null)
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
                        result=result+" : "+val.eqMark
                        if(val.eqModel!=null)
                            result=result+" "+val.eqModel
                    }
                    return result;
                }
                else return val
            }
        },
        { text: 'Статус', dataIndex: 'workStatus',flex:1,
            renderer: function (val, metaData, rec) {
                switch(val) {
                    case "complete" :
                        return  "Выполнена"
                    case "notcomplete":
                        return "Не выполнена"
                }

            } },
        {
            xtype:'actioncolumn',
            width:50,
            items: [{
                icon: 'images/ico16_show.png',
                tooltip: 'Просмотр',
                handler: function(view, rowIndex, colIndex,item,e,record,row) {
                    var currentWorksPanel=view.up('#instWorksPanel');
                    currentWorksPanel.down("#curWorkDesc").loadRecord(record);
                    var instEqPanel=currentWorksPanel.down("#instEqPanel");
                    var remEqPanel=currentWorksPanel.down("#remEqPanel");
                    var eqTabPanel=currentWorksPanel.down("#eqTabPanel");
                    var install=record.get("install");
                    if(install!=null && install!=undefined) {
                        //instEqPanel.setHidden(false);
                        instEqPanel.enable();
                        instEqPanel.loadData(record);
                        eqTabPanel.setActiveItem(instEqPanel);
                        instEqPanel.down("#eqStore").getStore().load();
                    }
                    var remove=record.get("remove");
                    if(remove!=null && remove!=undefined) {
                        //remEqPanel.setHidden(false);
                        remEqPanel.enable();
                        remEqPanel.loadData(record);
                        eqTabPanel.setActiveItem(remEqPanel)
                    }
                    var layout = currentWorksPanel.getLayout();
                    layout.next()
                }
            }
            ]
        }

    ],
    loadData:function(data){
        var self=this
        var works=data.get("works")
        console.log("works",works)
        self.getStore().loadData(works)
    }
});