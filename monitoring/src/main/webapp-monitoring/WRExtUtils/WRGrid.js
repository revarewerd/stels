Ext.Loader.setConfig({enabled: true});
Ext.Loader.setPath('Ext.ux', 'extjs-4.2.1/examples/ux');

Ext.define('WRExtUtils.WRGrid', {
    extend: 'Ext.grid.Panel',
    requires: [
        'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        }
    ],
    sortType:'local',
    selModel: {mode: 'MULTI'},
    loadMask: true,
    setStore: true,
    storeName: null,
    storeAutoSync: undefined,
    presaveSelection: false,
    loadBeforeActivated: true,
    manualLoad: false,
    searchStringWidth:150,
    searchFieldWidth:100,
    dockedToolbar: ['add', 'remove', 'refresh', 'search'],
    specialFilters:{},
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
        var self = this
        if (!self.loadBeforeActivated && !self.manualLoad) {
            self.on('activate', function (/*Ext.Component*/ self, /*Object*/ eOpts) {
                console.log('activate!!!')
                var store = this.store
                if (store.getTotalCount() == 0) {
                    store.load()
                }
            })
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
        if(self.sortType=="remote"){
            for(i in self.features)
                if(self.features[i].ftype=="filters")
                {
                    self.features[i].encode=true;
                    self.features[i].local=false;
                }
        }
        Ext.apply(this, {
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: function () {

                        var allitems = {

                            'add': {
                                icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                                text: tr('main.add'),
                                width:75,
                                scope: self,
                                handler: self.onAddClick
                            },
                            'remove': {
                                icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                                text:  tr('main.remove'),
                                width:75,
                                disabled: true,
                                itemId: 'delete',
                                scope: self,
                                handler: self.onDeleteClick
                            },
                            'refresh': {
                                icon: 'extjs-4.2.1/examples/page-analyzer/resources/images/refresh.gif',
                                text: tr('main.refresh'),
                                width:75,
                                itemId: 'refresh',
                                scope: self,
                                handler: function () {
                                    self.refresh()
                                }
                            },
                            'fill':{
                                xtype: 'tbfill'
                            },
                            'search': [
                                {
                                    xtype: 'label',
                                    text: tr('main.search')+': '
                                },
                                {
                                    xtype: 'triggerfield',
                                    name: 'searchstring',
                                    checkChangeBuffer:500,
                                    triggerCls:'x-form-clear-trigger',
                                    width:self.searchStringWidth,
                                    listeners: {
                                        change: function () {
                                            this.up().processSearch()
                                        }
                                    },
                                    onTriggerClick: function() {
                                        this.setValue("")
                                    }
                                },
                                {
                                    xtype: 'combobox',
                                    name: 'searchfield',
                                    width:self.searchFieldWidth,
                                    store: function () {
                                        var elems = [];
                                        if(self.sortType=="local")
                                            elems.push(['any', '- '+tr('main.all')+' -']);
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
                                            this.select(this.getStore().data.items[0])
                                            this.on('change', function () {
                                                this.up().processSearch()
                                            })
                                        }
                                    },
                                    editable: false,
                                    allowBlank: false
                                }
                            ]


                        }

                        var items = new Array()
                        for (var i = 0; i < self.dockedToolbar.length; i++) {
                            var obj = self.dockedToolbar[i];

                            var e = allitems[obj]
                            if (!e)
                                throw new Error("not found dockedToolbar:" + obj)

                            if (Ext.isArray(e))
                                items = items.concat(e)
                            else
                                items.push(e)
                        }

                        return items
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
                    //processSearch: function () {
                    //    var searchString = this.down('[name=searchstring]').getValue();
                    //    console.log("val:" + searchString)
                    //    var field = this.down('[name=searchfield]').getValue();
                    //    console.log("fgh:" + field)
                    //
                    //    if (field != null && searchString != null) {
                    //        var store = this.up('grid').store;
                    //        store.getProxy().setExtraParam('searchfield', field)
                    //        store.getProxy().setExtraParam('searchstring', searchString)
                    //        store.load()
                    //    }
                    //}
                }
            ]
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    onSelectChange:function(){}
});