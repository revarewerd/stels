Ext.Loader.setConfig({enabled: true});
Ext.Loader.setPath('Ext.ux', '/extjs-4.2.1/examples/ux');

Ext.define('WRExtUtils.WRGrid', {
    extend: 'Ext.grid.Panel',
    requires: [
        'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: false,
            local: true
        }
    ],
    sortType: 'local',
    selModel: {mode: 'MULTI'},
    loadMask: true,
    storeName: null,
    storeAutoSync: undefined,
    storeExtraParam: null,
    presaveSelection: false,
    loadBeforeActivated: true,
    setStore: true,
    searchStringWidth: 150,
    searchFieldWidth: 100,
    dockedToolbar: ['add', 'remove', 'refresh', 'search'],
    viewConfig: {
        trackOver: false,
        preserveScrollOnRefresh: true
    },
    specialFilters: {},
    refresh: function () {
        var self = this;
        if (self.presaveSelection) {
            self.restoreSelected = [];
            var selected = this.getSelectionModel().selected;
            for (var i = 0; i < selected.getCount(); i++) {
                var selectedItem = this.store.indexOf(selected.getAt(i));
                self.restoreSelected.push(selectedItem)
            }
        }
        if (this.store.pageMap)
            this.store.guaranteeRange(0, 199);
        this.store.load();
    },
    initComponent: function () {
        console.log("wrGrid init", this)
        var self = this;
        if (!self.loadBeforeActivated) {
            self.on('activate', function (/*Ext.Component*/ self, /*Object*/ eOpts) {
                //console.log('activate!!!')
                var store = this.store;
                if (store.getTotalCount() == 0) {
                    store.load();
                }
            });
        }
        if (self.setStore) {
            var storeProps = {
                autoLoad: self.loadBeforeActivated,
                listeners: {
                    write: function (proxy, operation) {
//                        if (operation.action == 'destroy') {
//                            self.child('#form').setActiveRecord(null);
//                        }
                        console.log(operation.action + " " + operation.resultSet.message);
                    },
                    beforeload: function (store, op) {
                        console.log("WRgrid storeExtraParam=", self.storeExtraParam);
                        if (self.storeExtraParam) {
                            for (var key in self.storeExtraParam)
                                store.getProxy().setExtraParam(key, self.storeExtraParam[key]);
                        }

                    },
                    load: function (/*Ext.data.Store*/ self0, /*Ext.data.Model[]*/ records, /*Boolean*/ successful, /*Object*/ eOpts) {
                        if (self.presaveSelection) {
                            if (self.restoreSelected) {
                                for (var i = 0; i < self.restoreSelected.length; i++) {
                                    self.getSelectionModel().select(self.restoreSelected[i], true);
                                }
                            }
                        }
                    }
                }
            };
            if (self.storeAutoSync != undefined)
                storeProps.autoSync = self.storeAutoSync;

            Ext.apply(this, {
                store: Ext.create(self.storeName, storeProps)
            });
        }
        if (self.sortType == "remote") {
            for (i in self.features)
                if (self.features[i].ftype == "filters") {
                    self.features[i].encode = true;
                    self.features[i].local = false;
                }
        }

        WRExtUtils.AtmosphereExtJSBroadcaster.on("dataChanged", function (data) {
            self.updateData(data)
        });

        Ext.apply(this, {
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: function () {
                        var allitems = {

                            'add': self.getAdd(),
                            'remove': {
                                icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                                text: 'Удалить',
                                disabled: true,
                                itemId: 'delete',
                                scope: self,
                                handler: self.onDeleteClick
                            },
                            'restore': {
                                icon: 'extjs-4.2.1/examples/build/KitchenSink/ext-theme-access/resources/images/icons/fam/accept.gif',
                                text: "Восстановить",
                                itemId: 'restore',
                                scope: self,
                                handler: self.onRestoreClick
                            },
                            'refresh': {
                                icon: 'extjs-4.2.1/examples/page-analyzer/resources/images/refresh.gif',
                                text: 'Обновить',
                                itemId: 'refresh',
                                scope: self,
                                handler: function () {
                                    self.refresh();
                                }
                            },
                            'selectAll': {
                                icon: 'extjs-4.2.1/resources/ext-theme-gray/images/sizer/square.gif',
                                text: "Выделить всё",
                                itemId: 'selectAll',
                                scope: self,
                                handler: function () {
                                    self
                                        .getView()
                                        .getSelectionModel()
                                        .selectAll();
                                }
                            },
                            'fill': {
                                xtype: 'tbfill'
                            },
                            'search': [
                                {
                                    xtype: 'label',
                                    text: 'Искать: '
                                },
                                {
                                    xtype: 'triggerfield',
                                    name: 'searchstring',
                                    checkChangeBuffer: 500,
                                    triggerCls: 'x-form-clear-trigger',
                                    width: self.searchStringWidth,
                                    listeners: {
                                        change: function () {
                                            this.up().processSearch()
                                        }
                                    },
                                    onTriggerClick: function () {
                                        this.setValue("")
                                    }
                                },
                                {
                                    xtype: 'combobox',
                                    name: 'searchfield',
                                    width: self.searchFieldWidth,
                                    store: function () {
                                        var elems = [];
                                        if (self.sortType == "local")
                                            elems.push(['any', '- Все -']);
                                        for (var i = 0; i < self.columns.length; i++) {
                                            var o = self.columns[i];
                                            var readableName = o.header ? o.header : o.text;
                                            if (readableName && o.filter)
                                                elems.push([o.dataIndex, readableName]);
                                        }
                                        return elems;
                                    }(),
                                    listeners: {
                                        afterrender: function () {
                                            this.select(this.getStore().data.items[0]);
                                            this.on('change', function () {
                                                this.up().processSearch();
                                            });
                                        }
                                    },
                                    editable: false,
                                    allowBlank: false
                                }
                            ],
                            'balanceEntryTypes': [
                                {
                                    xtype: 'label',
                                    html: '<b>Тип</b>'
                                },
                                WRExtUtils.makeBalanceEntryTypePicker(self, self.itemType)
                            ],
                            'gridDataExport': {
                                icon: 'images/ico16_download.png',
                                text: 'Выгрузить',
                                itemId: 'gridDataExport',
                                scope: self,
                                menu: [
                                    {
                                        text: 'в Excel',
                                        handler: function () {
                                            Ext.MessageBox.confirm('Выгрузка данных', 'Загрузка данных может занять длительное время. Продолжить?', function (button) {
                                                if (button === 'yes') {
                                                    var params = Ext.Object.toQueryString(self.getStore().getProxy().extraParams)
                                                    var address = "EDS/gridDataExport?entity=" + self.itemType;
                                                    if (params != '')
                                                        address += '&' + params;
                                                    window.open(address);
                                                }
                                            });
                                        }
                                    }
                                ]
                            },

                            'period': [
                                {
                                    xtype: 'label',
                                    html: '<b>Период:</b>'
                                },
                                {
                                    xtype: 'tbspacer'
                                },
                                {
                                    xtype: 'datefield',
                                    width: 110,
                                    fieldLabel: 'с',
                                    labelWidth: 10,
                                    //value:new Date(),
                                    name: 'dateFrom',
                                    format: 'd.m.Y',
                                    maxValue: new Date(),
                                    altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                                },
                                {
                                    xtype: 'timefield',
                                    width: 60,
                                    itemId: 'timeFrom',
                                    //labelWidth: 50,
                                    anchor: '100%',
                                    //fieldLabel: 'Время',
                                    name: 'timeFrom',
                                    format: 'H:i',
                                    value: '00:00'
                                },
                                {
                                    xtype: 'datefield',
                                    width: 110,
                                    fieldLabel: 'по',
                                    labelWidth: 15,
                                    //value:new Date(),
                                    name: 'dateTo',
                                    format: 'd.m.Y',
                                    maxValue: new Date(),
                                    altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                                },
                                {
                                    xtype: 'timefield',
                                    width: 60,
                                    itemId: 'timeTo',
                                    //labelWidth: 50,
                                    anchor: '100%',
                                    //fieldLabel: 'Время',
                                    name: 'timeTo',
                                    format: 'H:i',
                                    value: '23:59'
                                },
                                {
                                    xtype: 'button',
                                    text: 'Установить период',
                                    icon: 'images/ico16_okcrc.png',
                                    handler: function () {
                                        var store = self.getStore();
                                        var dateFrom = self.down('[name=dateFrom]').getValue();
                                        var timeFrom = self.down('[name=timeFrom]').getValue();
                                        var dateTo = self.down('[name=dateTo]').getValue();
                                        var timeTo = self.down('[name=timeTo]').getValue();
                                        if (dateFrom != null && timeFrom != null) {
                                            var from = sumDateAndTime(dateFrom, timeFrom);
                                            var to = sumDateAndTime(dateTo, timeTo);
                                            console.log('from', from, 'to', to);
                                            if (from <= to) {
                                                //store.getProxy().setExtraParam("uid", self.up('window').uid);
                                                store.getProxy().setExtraParam("dateFrom", from);
                                                store.getProxy().setExtraParam("dateTo", to);
                                                store.load();
                                            }
                                            else {
                                                Ext.MessageBox.show({
                                                    title: 'Ошибка',
                                                    msg: 'Укажите корректный диапазон дат',
                                                    icon: Ext.MessageBox.ERROR,
                                                    buttons: Ext.Msg.OK
                                                });
                                            }
                                        }
                                        else {
                                            store.getProxy().setExtraParam("dateFrom", null);
                                            store.getProxy().setExtraParam("dateTo", null);
                                            store.load();
                                        }

                                    }
                                }
                            ]


                        };
                        var items = [];
                        for (var i = 0; i < self.dockedToolbar.length; i++) {
                            var obj = self.dockedToolbar[i];

                            var e = allitems[obj];
                            if (!e)
                                throw new Error("not found dockedToolbar:" + obj);

                            if (Ext.isArray(e))
                                items = items.concat(e);
                            else
                                items.push(e);
                        }

                        return items;
                    }(),

                    processSearch: function () {
                        console.log("processSearch")
                        var searchString = this.down('[name=searchstring]').getValue();
                        var field = this.down('[name=searchfield]').getValue();
                        var searchfieldRecords = this.down('[name=searchfield]').getStore().getRange();
                        var searchItems = [];
                        for (var i in searchfieldRecords) {
                            var item = searchfieldRecords[i].get('field1');
                            if (item != 'any')
                                searchItems.push(item);
                        }
                        var store = self.getStore();

                        if (self.sortType != "remote") {
                            store.clearFilter();
                            if (field != null && searchString != null && searchString != '') {
                                if (field != 'any') {
                                    searchItems.splice(0, searchItems.length, field);
                                    console.log('фильтр по полю', searchItems);
                                    for (var i in self.columns)
                                        if (self.columns[i].dataIndex == field && self.columns[i].hidden) {
                                            self.columns[i].setVisible(true);
                                        }
                                }
                                else console.log('фильтр по любому полю');
                                console.log('локальная фильтрация');
                                var filter = new Ext.util.Filter({
                                    filterFn: function (items) {
                                        var flag = false;
                                        if (searchItems.length != 0 && searchString != null) {
                                            for (var i in searchItems) {
                                                if (items.get(searchItems[i])) {
                                                    if (self.specialFilters[searchItems[i]] != undefined) {
                                                        flag = self.specialFilters[searchItems[i]].filterFn(items, searchString)
                                                        if (flag) break;
                                                    }
                                                    else if (items.get(searchItems[i]).toString().toLowerCase().indexOf(searchString.toString().toLowerCase()) > -1) {
                                                        flag = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        else flag = true;
                                        return flag;
                                    }
                                });
                                store.filter(filter);
                            }
                        }
                        else {
                            console.log('удаленная фильтрация');
                            var store = this.up('grid').store;
                            console.log('searchfield', field)
                            console.log('searchstring', searchString)
                            store.getProxy().setExtraParam('searchfield', field);
                            store.getProxy().setExtraParam('searchstring', searchString);
                            store.load();
                        }
                    }
                }
            ]
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    onSelectChange: function () {

    },
    updateData: function (data) {
        //this.refresh()
    },
    changeData: function (data, aggregate) {
        if (data.aggregate == aggregate) {
            var self = this
            var store = self.getStore()
            console.log("data=", data.data)
            var rec = store.getById(data.itemId)
            console.log("change rec=", rec)
            store.suspendAutoSync()
            switch (data.action) {
                case "create" : {
                    console.log("create " + aggregate + " _id=" + data.itemId);
                    if (rec == null) {
                        if (data.data._id == undefined) data.data._id = data.itemId
                        store.insert(0, data.data)
                    }
                    break;
                }
                case "update" :
                    console.log("update " + aggregate + " _id=" + data.itemId);
                    if (rec != null) {
                        rec.beginEdit()
                        for (fname in data.data) {
                            //console.log("fname="+fname+" data[fname]="+data.data[fname]);
                            rec.set(fname, data.data[fname])
                        }
                        rec.endEdit()
                        rec.commit()
                    }
                    break;
                case "delete" : {
                    console.log("delete " + aggregate + " _id=" + data.itemId);
                    if (rec != null) {
                        store.remove(rec)
                    }
                    break;
                }
                case "remove" : {
                    console.log("remove " + aggregate + " _id=" + data.itemId);
                    if (rec != null) {
                        store.remove(rec)
                    }
                    break;
                }
                case "restore" : {
                    console.log("restore " + aggregate + " _id=" + data.itemId);

                    if (rec == null) {
                        store.insert(0, data.data)
                    }
                    break;
                }
            }
            store.initSortable()
            store.sort()
            store.filter()
            store.resumeAutoSync()
        }
    },
    getAdd: function () {
        var self = this;
        var btn = Ext.create('Ext.Button', {
            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
            text: 'Добавить',
            defaultType: 'button',
            itemId: 'add',
            handler: function () {
                self.onAddClick();
            }
        });
        return btn;
    }
});
