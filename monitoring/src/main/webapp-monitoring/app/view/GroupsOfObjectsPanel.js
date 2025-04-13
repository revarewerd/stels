/**
 * Created by IVAN on 28.05.2015.
 */
Ext.define('Seniel.view.GroupsOfObjectsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objgroupspanel',
    //title: 'Группы объектов',
    requires: [
        'Seniel.view.GroupOfObjectsForm'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    loadBeforeActivated: true,
    storeName: 'EDS.store.UserGroupsOfObjects',
    itemType:'objects',
    searchStringWidth:120,
    searchFieldWidth:100,
    dockedToolbar: ['add', 'remove', 'refresh', 'fill','search'],
    initComponent: function () {
        var self=this;
        //var hideRule=!self.viewPermit;
        Ext.apply(this,{
            columns: [
                //{
                //    hideable: false,
                //    menuDisabled: true,
                //    sortable: false,
                //    flex: 1
                //},
                {
                    header: '№',
                    xtype: 'rownumberer',
                    width: 40,
                    resizable: true
                },
                {
                    header: tr('main.groupsofobjects.name'),
                    width: 170,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                        return val
                    },
                    summaryType: 'count',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>'+tr('main.groupofobjects.totalobjects')+'{0} </b>', value);
                    }
                },
                //{
                //    header: 'Пользователь',
                //    width: 170,
                //    sortable: true,
                //    dataIndex: 'userName',
                //    filter: {
                //        type: 'string'
                //    }
                //},
                {
                    header: tr('main.groupsofobjects.objects'),
                    flex:7,
                    sortable: true,
                    dataIndex: 'objectsNames',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"'
                        return val
                    },
                    //hidden:hideRule
                },
                {
                    xtype: 'actioncolumn',
                    width: 20,
                    menuText: tr('main.groupsofobjects.show'),
                    menuDisabled: true,
                    sealed: true,
                    items: [
                        {
                            icon: 'images/ico16_show.png',
                            tooltip: tr('main.groupsofobjects.show'),
                            tooltipType: 'title',
                            handler: function (view, rowIndex, colIndex, item, e, rec) {
                               self.showGroupOfObjectsForm(rec);
                            }
                        }
                    ]
                }
                //{
                //    hideable: false,
                //    menuDisabled: true,
                //    sortable: false,
                //    flex: 1
                //}
            ],
            listeners: {
                celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    var dataIndex=self.columnManager.columns[cellIndex].dataIndex
                    self.showGroupOfObjectsForm(record);
                }
            },
            onSelectChange: function (selModel, selections) {
                //if (hideRule)
                //    this.down('#delete').setDisabled(true);
                //else
                    this.down('#delete').setDisabled(selections.length === 0);
            }
        })
        this.callParent()
    },
    onDeleteClick: function () {
        var self=this
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm(tr('main.itemremoving.removeitems'), tr('main.itemremoving.sure')+' ' + selection.length + ' '+tr('main.itemremoving.records?'), function (button) {
                if (button === 'yes') {
                    userGroupsOfObjects.remove(Ext.Array.map(selection, function(m){return m.get("_id");}), function (r, e) {
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: tr('mapobject.objectactions.error'),
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                        else self.refresh();
                    })
                }
            });
        }
    },
    onAddClick: function (type) {
        var rec = Ext.create('ObjectsGroup');
        var form = this.showGroupOfObjectsForm(rec);
    },
    showGroupOfObjectsForm: function(record) {
        var self = this;
        var viewport = this.up('viewport');
        var wnd=viewport.createOrFocus('GroupOfObjectsForm' + record.get('_id'), 'Seniel.view.GroupOfObjectsForm.Window', {
            title: tr('main.groupofobjects')+' "' + record.get('name') + '"',
            groupId:record.get('_id'),
            btnConfig: {
                icon: 'images/cars/car_001_blu_24.png',
                text: tr('main.groupofobjects')+' "' + record.get('name') + '"'
            },
        })
        if(wnd.justCreated) {
            viewport.showNewWindow(wnd);
            wnd.onLoad(record);
            wnd.on('onSave', function (args) {
                self.refresh();
            });
        }
    }
})

Ext.define('Seniel.view.GroupsOfObjectsPanel.Window', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.objwnd',
    title: tr('main.groupsofobjects'),
    stateId: 'grObjPanelWnd',
    stateful: true,
    minWidth: 600,
    minHeight: 400,
    width: 600,
    height: 400,
    maximizable: true,
    layout: 'fit',
    icon: 'images/cars/car_001_blu_24.png',
    btnConfig: {
        icon: 'images/cars/car_001_blu_24.png',
        text: tr('main.groupsofobjects')
    },
    initComponent: function () {
        var self = this;
        self.on("close",
            function (wnd, e) {
                var groupedobjlist=Ext.ComponentQuery.query('groupedobjectslist');
                groupedobjlist[0].getStore().load()
            }
        )
        Ext.apply(this, {
            items: [
                {
                    xtype: 'objgroupspanel'
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->',
                        {
                            text: 'OK',
                            width:100,
                            handler: function () {
                                self.close();
                            }
                        }]
                }
            ]
        });
        this.callParent();
    }
})