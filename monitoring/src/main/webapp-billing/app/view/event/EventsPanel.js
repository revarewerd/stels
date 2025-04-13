Ext.define('Billing.view.event.EventsWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.eventswnd',
    width: 1024,
    height: 768,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
            items:[
                { xtype:'eventspanel',
                    title: 'Панель событий объекта '+self.aggregateId,
                    storeExtraParam:{
                        aggregateId:self.aggregateId,
                        aggregateType:self.aggregateType
                    }
                }
            ]
        });
        this.callParent();
    }
});


Ext.define('Billing.view.event.EventsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.eventspanel',
    title: 'Панель событий',
    requires: [
    ],
    plugins: 'bufferedrenderer',
    selModel: {
        mode: 'MULTI',
        pruneRemoved: false
    },
    sortType:'remote',
    dockedToolbar: ['refresh','search','fill','period'],
    storeName: 'EDS.store.EventsData',
    //Model("aggregateId","aggregateType" , "payloadType","aggregateName", "userName", "time","eventData")
    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width:50,
            resizable:true
        },
        {
            header: 'Время',
            width:200,
            sortable: true,
            xtype: 'datecolumn',
            format:"Y-m-d H:i:s",
            dataIndex: 'time'
        },
        {
            header: 'ID',
            width:200,
            sortable: true,
            dataIndex: 'aggregateId',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Тип объекта',
            width:150,
            sortable: true,
            filter: {
                type: 'string'
            },
            dataIndex: 'aggregateType'
        },
        {
            header: 'Имя',
            width: 450,
            sortable: true,
            filter: {
                type: 'string'
            },
            dataIndex: 'aggregateName'
        },

        {
            header: 'Изменение',
            width:200,
            sortable: true,
            dataIndex: 'payloadType',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Пользователь',
            width:150,
            sortable: true,
            filter: {
                type: 'string'
            },
            dataIndex: 'userName'
        },
        {
            header: 'Данные',
           // width:300,
            sortable: true,
            dataIndex: 'eventDataPreview',
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = '" title="'+val+'"';
                return val
            },
            flex: 1
        }
    ],
    initComponent: function () {
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },

    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            this.showPayloadWindow(record);
        }
    },

    showPayloadWindow: function(record) {
        var existingWindow = WRExtUtils.createOrFocus('eventpayloadwnd' + record.get('_id'), 'Billing.view.event.PayloadWindow', {
            title: 'Данные'// TODO
        });
        if (existingWindow.justCreated)
            existingWindow.down('eventpayloadform').setActiveRecord(record);
        existingWindow.show();

        return existingWindow.down('eventpayloadform');
    }
});

