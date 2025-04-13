
Ext.define('Billing.view.event.PayloadWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.eventpayloadwnd',
    width: 500,
    height: 400,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
            items:[
                {
                    xtype:"eventpayloadform",
                    closeAction: function () {
                        this.up('window').close();
                    }
                }
            ]
        });
        this.callParent();
    }
});

Ext.define('Billing.view.event.PayloadForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.eventpayloadform',
    initComponent: function () {
        var self = this;

        Ext.apply(this, {
            layout:'fit',
            items: [
                {
                    xtype: 'treepanel',
                    rootVisible: false,
                    singleExpand: true,
                    cls: 'event-tree-panel',
                    store: {
                        xtype: 'store.tree',
                        fields: [
                            {name: 'key', type: 'string'},
                            {name: 'value', type: 'string'}
                        ]
                    },
                    columns  : [
                        {
                            xtype: 'treecolumn',
                            text : 'Поле',
                            dataIndex : 'key',
                            flex: 1,
                            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"'
                                return value;
                            }
                        },
                        {
                            text : 'Значение',
                            dataIndex : 'value',
                            flex: 2,
                            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"'
                                return value;
                            }
                        }
                    ]
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            xtype:'tbspacer',
                            flex:1
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: 'Закрыть',
                            scope: this,
                            handler: this.closeAction
                        }]
                }
            ]
        });
        this.callParent();
    },
    setActiveRecord: function (data) {
        var grid=this.down('treepanel');
        this.activeRecord = data;
        var store=grid.getStore();
        var rows = [];
        rows.push({key: "ID элемента", value: data.get('aggregateId'), leaf: true });
        rows.push({key: 'Имя элемента', value: data.get('aggregateName'), leaf: true});
        rows.push({key: 'Тип элемента', value: data.get('aggregateType'), leaf: true});
        rows.push({key: 'Событие', value: data.get('payloadType'), leaf: true});
        var payload = data.get("eventData");

        function fill(result, data) {
            for(var k in data) {
                var val = data[k];
                if( val !== null && typeof val === 'object') {
                    result.push({ key: k, value: '-', children: fill([],val)});
                }
                else result.push({key: k, value: val, leaf: true});
            }
            return result;
        }

        fill(rows,payload);
        console.log(rows);
        //console.log("data = " +   JSON.stringify(data));
        console.log(store);
        store.setProxy(Ext.create('Ext.data.proxy.Memory',
            {
                reader: {
                    type: 'json'
                  //  root: 'rows'
                },
                data:  rows
            })
        );
        store.load();
      // store.loadData(rows);

    }
});