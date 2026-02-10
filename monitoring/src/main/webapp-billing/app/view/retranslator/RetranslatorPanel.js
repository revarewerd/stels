Ext.Loader.setConfig({enabled: true});
Ext.Loader.setPath('Ext.ux', '/extjs-4.2.1/examples/ux');

Ext.define('Billing.view.retranslator.RetranslatorPanel', {
    extend: 'WRExtUtils.WRGrid',
    //extend:'Ext.grid.Panel',
    alias: 'widget.retranslatorgrid',
    title: 'Ретранслятор',
    requires: [
        'Ext.grid.*',
        'Ext.data.*',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem',
        'Billing.view.retranslator.RetranslationsListPanel'
        //'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        }
//        {
//            ftype: 'summary',
//            dock:'top'
//        }
    ],
    flex: 1,
    //setStore: false,
    storeName: 'EDS.store.RetranslatorsListService',
    storeAutoSync: false,
//    store: Ext.create('Ext.data.Store', {
//        fields: ['name', 'comment', 'enabled', "id"],
//        data: {'items': [
//            { 'name': 'ODS-mos-ru', "comment": "", "enabled": "true", "id": "ODS-mos-ru" },
//            { 'name': 'Wialon', "comment": "", "enabled": "true", "id": "Wialon-1"   }
//        ]},
//        proxy: {
//            type: 'memory',
//            reader: {
//                type: 'json',
//                root: 'items'
//            }
//        }
//    }),
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    viewConfig: {
        trackOver: false
    },
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    listeners: {
        celldblclick: function(/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts){
            var self = this;
            var dataIndex = self.columnManager.columns[cellIndex].dataIndex;
            console.log("celldblclick dataIndex=", dataIndex);
            switch (dataIndex) {
                case "name":
                    this.showRetranslatorForm(record);
                    break;
            }
        }
    },
    initComponent: function(){
        var self = this;
        this.callParent();
        this.down('toolbar').insert(2, {
            icon: 'extjs-4.2.1/examples/menu/images/group16.gif',
            text: 'Текущие ретрансляции',
            scope: self,
            handler: self.activeRetranslations
        });
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    columns: [
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        },
        {
            header: '№',
            xtype: 'rownumberer',
            width: 40,
            resizable: true
        },
        {
            header: 'Имя',
            flex: 1,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            },
            renderer: function(val, metaData, rec){
                metaData.tdAttr = 'style="cursor: pointer !important;"';
                return val;
            }//,
//        summaryType: 'count',
//             summaryRenderer: function(value, summaryData, dataIndex) {
//                    return Ext.String.format('<b>Всего позиций: {0} </b>', value); 
//             }
        },
        {
            header: 'Хост',
            dataIndex: 'host',
            //align: 'right',
            width: 100
        },
        {
            header: 'Порт',
            dataIndex: 'port',
            //align: 'right',
            width: 100
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        }
    ],
    showRetranslatorForm: function(record){
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('retranslatorWnd' + record.get('_id'), 'Billing.view.RetranslatorWindow', {
            retranslatorId: record.get('id'),
            retranslatorRecord: record,
            onSave: function(){
                self.getStore().reload();
            }
        });
        existingWindow.show();
        return existingWindow;
    },

    activeRetranslations: function(){
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('retranslations', 'Billing.view.retranslator.RetranslationsWindow', {
//            retranslatorId: record.get('id'),
//            retranslatorRecord: record,
//            onSave: function () {
//                self.getStore().reload();
//            }
        });
        existingWindow.show();
        return existingWindow;
    },

    onSelectChange: function(selModel, selections){
        this.down('#delete').setDisabled(selections.length === 0);
    },

    onAddClick: function(){
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('retranslatorWnd' + 'new', 'Billing.view.RetranslatorWindow', {
            //retranslatorId: record.get('id'),
            onSave: function(){
                self.getStore().reload();
            }
        });
        existingWindow.show();
        return existingWindow;
    },

    onDeleteClick: function(){
        var selection = this.getView().getSelectionModel().getSelection();
        var self = this;
        Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' ретранслятор?', function(button){
            if (button === 'yes') {
                console.log("removing:", selection);
                retranslatorsListService.remove(Ext.Array.map(selection, function(m){
                    return m.get("id");
                }), function(r, e){
                    if (!e.status) {
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    self.getStore().reload();
                });
            }
        });
    }

});

Ext.define('Billing.view.ObjectsSelectionGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objectsselectiongrid',
    header: false,
    requires: [
        'Ext.grid.plugin.CellEditing',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem',
        'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: true
        }
    ],
    //autoScroll : true,
    flex: 1,
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    viewConfig: {
        trackOver: false
    },
    loadMask: true,
    multiSelect: true,
    storeName: 'EDS.store.AllObjectsService',
    storeAutoSync: false,
    dockedToolbar: ["refresh", "search"],
    listeners: {
        afterrender: function(){
            var rstore = this.up('window').down('retranslatinggrid').getStore();

            this.store.on('load', function(store, records){
                var toRemove = [];
                Ext.Array.forEach(records, function(rec, i){
                    var r = rstore.uids[rec.get('uid')];
                    if (r) {
                            toRemove.push(rec);
                    }
                });
                store.remove(toRemove);
            });
        }},
    initComponent: function(){
        this.callParent();
    },
    columns: [
        {
            header: 'Имя',
            flex: 1,
            width: 170,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Учетная запись',
            width: 170,
            flex: 1,
            sortable: true,
            dataIndex: 'accountName',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'IMEI',
            width: 170,
            hidden: true,
            flex: 1,
            sortable: true,
            dataIndex: 'eqIMEI',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'УИД',
            width: 170,
            flex: 1,
            hidden: true,
            dataIndex: 'uid',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        }
    ]

});

Ext.define('Billing.view.RetranslatingObjectsGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.retranslatinggrid',
    header: false,
    multiSelect: true,
    //rootVisible: false,
    requires: [
        'Ext.grid.plugin.CellEditing',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem'
    ],
    features: [
        {
            ftype: 'filters',
            encode: false,
            local: true
//            encode: true,
//            local: false
        }
    ],
    flex: 1,
    storeName: 'EDS.store.RetranslatorsService',
    storeAutoSync: false,
    loadBeforeActivated: true,
    dockedToolbar: ["refresh", "remove", "search"],
    columns: [
        {
            header: 'Имя',
            flex: 1,
            width: 170,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'IMEI',
            width: 170,
            flex: 1,
            dataIndex: 'eqIMEI',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Учетная запись',
            width: 170,
            flex: 1,
            sortable: true,
            dataIndex: 'accountName',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'УИД',
            width: 170,
            //flex: 1,
            hidden: true,
            dataIndex: 'uid',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        }
    ],
    initComponent: function(){
        var self = this;
        self.on('boxready', function(comp, width, height, eOpts){
            var retransButton = {
                xtype: 'button',
                text: 'Добавить за период',
                icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                handler: function(){
                    self.onAddForPeriodClick();
                }
            };
            console.log('self', self)
            var toolbar = self.getDockedItems('toolbar[dock="top"]');
            console.log("toolbar", toolbar)
            toolbar[0].insert(0, retransButton)
        });
        this.callParent();

        this.getStore().on('datachanged', function(store){
            store.uids = {};
            store.each(function(r){
                store.uids[r.get('uid')] = true;
            });
            console.log("store.uids =", store.uids);
        });
    },
    onSelectChange: function(selModel, selections){
        this.down('#delete').setDisabled(selections.length === 0);


    },

    onSync: function(){
        this.store.sync();
    },
    onAddForPeriodClick: function(){
        var self = this;
        var grapams = self.up('window').down('#gparams').getForm().getValues();
        var existingWindow = WRExtUtils.createOrFocus('addForPeriodWnd' + self.up('window').retranslatorId, 'Billing.view.AddForPeriodWindow', {
            selectedObjects: self.getSelectionModel().getSelection(),
            retranslatorId: self.up('window').retranslatorId,
            grapams: grapams
        });
        if (existingWindow.justCreated) {
            var recordCount = existingWindow.down('[name=recordCount]');
            recordCount.setValue(self.getSelectionModel().getCount());
        }
        existingWindow.show();
    },
    onDeleteClick: function(){
        var selection = this.getView().getSelectionModel().getSelection();
        var self = this;
        if (selection) {
            var store = this.store;
            store.remove(selection);
        }
    }
});
Ext.define('Billing.view.AddForPeriodWindow', {
    extend: 'Ext.window.Window',
    alias: "widget.addforperiodwnd",
    title: 'Добавить за период',
    width: 600,
    height: 300,
    layout: 'border',
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: ['->', {
                icon: 'images/ico16_okcrc.png',
                itemId: 'save',
                text: 'Добавить',
                handler: function(){
                    var self = this;
                    self.up('window').onAddClick();
                }
            }, {
                icon: 'images/ico16_cancel.png',
                text: 'Отменить',
                handler: function(){
                    var self = this;
                    self.up('window').close();
                }
            }
            ]
        }
    ],
    items: [
        {
            xtype: 'panel',
            region: 'center',
            layout: {type: 'vbox',
                align: 'stretch'},
            defaults: {
                margin: '0 20 5 20',
                labelWidth: 120
            },
            items: [
                {
                    margin: '20 20 5 20',
                    xtype: 'displayfield',
                    fieldLabel: 'Количество записей',
                    name: 'recordCount',
                    renderer: function(value){
                        return '<div class="ballink">' + value + '</div>';
                    }
                },
                {

                    xtype: 'datefield',
                    fieldLabel: 'Начало периода',
                    value: new Date(),
                    name: 'startDate',
                    format: 'd.m.Y',
                    altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                },
                {
                    xtype: 'timefield',
                    anchor: '100%',
                    fieldLabel: 'Время',
                    name: 'startTime',
                    format: 'H:i',
                    value: '00:00'
                },
                {
                    xtype: 'datefield',
                    fieldLabel: 'Окончание периода',
                    value: new Date(),
                    name: 'endDate',
                    format: 'd.m.Y',
                    altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                },
                {
                    xtype: 'timefield',
                    anchor: '100%',
                    fieldLabel: 'Время',
                    name: 'endTime',
                    format: 'H:i',
                    value: '23:59'
                }
            ]
        }
    ],
    listeners: {
        'show': function(wnd, eOpts){
            var recordCount = wnd.down('[name=recordCount]');
            var records = wnd.selectedObjects;
            var tip = Ext.create('Ext.tip.ToolTip', {
                target: recordCount.el,
                //style: {opacity: '0.8'},
                header: {title: 'Ретранслируемые объекты'},
                autoHide: false,
                autoScroll: true,
                closable: true,
                resizable: true,
                draggable: true,
                border: false,
                padding: false,
                width: 300,
                items: [
                    {
                        xtype: 'grid',
                        autoScroll: true,
                        hideHeaders: false,
                        disableSelection: true,
                        rowLines: true,
                        border: false,
                        padding: false,
                        columns: [
                            {text: 'Объект', dataIndex: 'name', flex: 1}
                        ],
                        store: {
                            fields: ['name'],
                            data: {
                                'items': records
                            },
                            proxy: {
                                type: 'memory',
                                reader: {
                                    type: 'json',
                                    root: 'items'
                                }
                            }
                        }
                    }
                ]
            });
        }
    },
    onAddClick: function(){
        var self = this;
        var date = self.getWorkingDates();
        if (!date) {
            Ext.MessageBox.show({
                title: 'Указана некорректная дата',
                msg: 'Укажите корректную дату!',
                buttons: Ext.MessageBox.OK,
                icon: Ext.Msg.WARNING
            });
        }
        else {

            var uids = Ext.Array.map(self.selectedObjects, function(record){
                return record.getData().uid;
            });
            retranslationTasks.addRetranslation(self.retranslatorId,
                self.grapams.name,
                self.grapams.host,
                self.grapams.port,
                self.grapams.protocol,
                uids, date.from, date.to, function(){
                    self.close();
                });
        }
    },
    getWorkingDates: function(){
        var self = this;
        var form = self.down('panel');
        var fromdate = form.down('datefield[name="startDate"]').getValue();
        var fromtime = form.down('timefield[name="startTime"]').getValue();
        var todate = form.down('datefield[name="endDate"]').getValue();
        var totime = form.down('timefield[name="endTime"]').getValue();
        var currnt = new Date(),
            compto = sumDateAndTime(todate, totime);

        if ((compto.toLocaleDateString() == currnt.toLocaleDateString()) && (compto.getTime() > currnt.getTime())) {
            totime = currnt;
        }

        if (fromdate > todate)
            return null;
        else
            return {
                from: sumDateAndTime(fromdate, fromtime),
                to: sumDateAndTime(todate, totime)
            };
    }
});
function sumDateAndTime(date, time){
    var res = new Date(date);
    res.setHours(time.getHours(), time.getMinutes(), time.getSeconds(), time.getMilliseconds());
    return res;
}
Ext.define('Billing.view.RetranslatorWindow', {
    extend: 'Ext.window.Window',
    alias: "widget.retranslatorwindow",
    title: 'Права',
    width: 1024,
    height: 600,
    layout: 'border',
    //constrainHeader: true,
    initComponent: function(){
        var self = this;

        Ext.apply(this, {
            title: 'Ретранслятор',
            items: [
                {
                    xtype: 'form',
                    itemId: 'gparams',
                    region: 'north',
                    defaultType: 'textfield',
                    fieldDefaults: {
                        margin: '0 20 5 20',
                        anchor: '100%',
                        labelAlign: 'right'
                    },
                    items: [
                        {
                            margin: '10 20 5 20',
                            fieldLabel: 'Название',
                            name: 'name',
                            allowBlank: false,
                            value: self.retranslatorRecord ? self.retranslatorRecord.get('name') : ""
                        },
                        {
                            fieldLabel: 'Хост',
                            name: 'host',
                            allowBlank: false,
                            value: self.retranslatorRecord ? self.retranslatorRecord.get('host') : ""
                        } ,
                        {
                            fieldLabel: 'Порт',
                            name: 'port',
                            regex: /\d+/,
                            allowBlank: false,
                            value: self.retranslatorRecord ? self.retranslatorRecord.get('port') : ""
                        },
                        {
                            fieldLabel: 'Протокол',
                            xtype: 'combobox',
//                            labelWidth: 150,
//                            width:400,
                            name: 'protocol',
                            forceSelection: true,
                            triggerAction: 'all',
                            minChars: 0,
                            selectOnTab: true,
                            store: [
                                ['wialon','Wialon'],
                                ['nis','НИС']
                            ],
                            value: self.retranslatorRecord ? self.retranslatorRecord.get('protocol') : "wialon"
                            //displayField: 'name',
                            //valueField: '_id'
                        }
                    ]
                },
                {
                    xtype: 'panel',
                    split: true,
                    //title: 'Добавить элемент',
                    width: 400,
                    collapsible: true,
                    region: 'west',
                    layout: 'border',
                    title: 'Объекты',
                    items: [
                        {
                            region: 'center',
                            xtype: 'objectsselectiongrid',
                            viewConfig: {
                                plugins: {
                                    ptype: 'gridviewdragdrop',
                                    dragGroup: "retransobjs",
                                    dropGroup: "retransobjs"
                                }
                                //                        listeners: {
//                            beforedrop: function (node, data, overModel, dropPosition, dropHandlers, eOpts) {
//                                console.log("drop", data);
//                                var dropableRecs = new Array();
//                                for (var i in data.records) {
//                                    if (data.records[i].data.recordType == 'object') {
//                                        dropableRecs.push(data.records[i]);
//                                    }
//                                    else console.log(data.records[i].data, " not object");
//                                }
//                                console.log("dropableRecs", dropableRecs);
//                                if (dropableRecs.length == 0)
//                                    dropHandlers.cancelDrop();
//                                else {
//                                    data.records = dropableRecs;
//                                    dropHandlers.processDrop();
//                                }
//                            },
//                            drop: function (node, data, overModel, dropPosition, eOpts) {
//                                console.log("drop", data);
//                                console.log("drop", data.records[0].data.recordType);
//                            }
//                        }
                            }
                        }
                    ]
                },
                {
                    xtype: 'panel',
                    title: 'Ретранслируемые',
                    region: 'center',
                    layout: 'border',
                    items: [
                        {
                            region: 'center',
                            xtype: 'retranslatinggrid',
                            storeExtraParam: {
                                retranslatorId: self.retranslatorId
                            },
                            viewConfig: {
                                plugins: {
                                    ptype: 'gridviewdragdrop',
                                    dragGroup: "retransobjs",
                                    dropGroup: "retransobjs"
                                },
                                listeners: {
//                            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
//                                console.log("beforedrop")
//                                for(var i in data.records){
//                                    data.records[i].beginEdit()
//                                    var itemId=data.records[i].get('item_id')
//                                    //console.log(data.records[i].data)
//                                    if(itemId == '')  data.records[i].set('item_id',data.records[i].get('_id'))
//                                    data.records[i].set('userId',self.userId)
//                                    data.records[i].endEdit(false)
//                                }
//                            },
                                    drop: function(node, data, overModel, dropPosition, eOpts){
                                        console.log("drop", data);
                                        var permStore = self.down('retranslatinggrid').getStore();
                                        console.log("permStore", permStore);
                                        for (var i in data.records) {
                                            data.records[i].setDirty();
                                            for (var j in permStore.removed) {
                                                if (data.records[i] == permStore.removed[j]);
                                                {
                                                    console.log('not removed', permStore.removed.splice(j, 1));
                                                }
                                            }
                                        }
                                        if (permStore.removed.length == 0) permStore.removed = [];
                                        console.log("drop", data.records[0].data.recordType);
                                    }
                                }
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
                    items: ['->', {
                        icon: 'images/ico16_okcrc.png',
                        itemId: 'save',
                        text: 'Сохранить',
                        //scope: this,
                        handler: function(){
                            console.log('self', self.down('retranslatinggrid'));
                            var store = self.down('retranslatinggrid').getStore();
                            store.clearFilter(true);
                            console.log('store', store);
                            console.log('data to remove', store.getRemovedRecords());
                            console.log('data to add', store.getNewRecords());
                            console.log('data to update', store.getUpdatedRecords());
                            console.log('modified data ', store.getModifiedRecords());
                            console.log('data', store.data);
                            //var data = store.data;
                            var data = Ext.Array.map(store.data.items, function(record){
                                return record.getData();
                            });
                            console.log('all data ', data);

                            var grapams = self.down('#gparams').getForm().getValues();

                            console.log('grapams=', grapams);

                            retranslatorsService.updateData(self.retranslatorId,
                                grapams.name,
                                grapams.host,
                                grapams.port,
                                grapams.protocol,
                                data, function(submitResult, e){
                                    console.log("Результат onSave", submitResult);
                                    if (!e.status) {
                                        Ext.MessageBox.show({
                                            title: 'Произошла ошибка',
                                            msg: e.message,
                                            icon: Ext.MessageBox.ERROR,
                                            buttons: Ext.Msg.OK
                                        });
                                    }
                                    else {
                                        if (self.onSave)
                                            self.onSave();
                                        self.close();
                                    }
                                });


                        }
                    }, {
                        icon: 'images/ico16_cancel.png',
                        text: 'Отменить',
                        scope: this,
                        handler: function(){
                            self.close();
                        }
                    }
                    ]
                }
            ]

        });
        this.callParent();
    }
});
