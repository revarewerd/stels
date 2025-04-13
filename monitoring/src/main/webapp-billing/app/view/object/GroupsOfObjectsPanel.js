/**
 * Created by IVAN on 28.05.2015.
 */
Ext.define('Billing.view.object.GroupsOfObjectsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objgroupspanel',
    title: 'Группы объектов',
    requires: [
        'Billing.view.object.GroupOfObjectsForm'
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
    loadBeforeActivated: false,
    storeName: 'EDS.store.GroupsOfObjects',
    itemType:'objects',
    dockedToolbar: ['add', 'remove', 'refresh', 'search','fill'/*,'gridDataExport'*/],
    initComponent: function () {
        var self=this;
        var hideRule=!self.viewPermit;
        Ext.apply(this,{
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
                        return Ext.String.format('<b>Всего позиций: {0} </b>', value);
                    }
                },
                {
                    header: 'Пользователь',
                    width: 170,
                    sortable: true,
                    dataIndex: 'userName',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Объекты',
                    flex:2,
                    sortable: true,
                    dataIndex: 'objectsNames',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"'
                        return val
                    },
                    hidden:hideRule
                },
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    flex: 1
                }
            ],
            listeners: {
                celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    var dataIndex=self.columnManager.columns[cellIndex].dataIndex
                    self.showGroupOfObjectsForm(record);
                }
            },
            onSelectChange: function (selModel, selections) {
                if (hideRule)
                    this.down('#delete').setDisabled(true);
                else
                    this.down('#delete').setDisabled(selections.length === 0);
            }
        })
        this.callParent()
    },
    onDeleteClick: function () {
        var self=this;
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' записей?', function (button) {
                if (button === 'yes') {
                    groupsOfObjects.remove(Ext.Array.map(selection, function(m){return m.get("_id");}), function (r, e) {
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
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
        var self = this
        var existingWindow = WRExtUtils.createOrFocus('GroupOfObjectsForm' + record.get('_id'), 'Billing.view.object.GroupOfObjectsForm.Window', {
            title: 'Группа объектов "' + record.get('name') + '"',
            groupId:record.get('_id')
//          hideRule: !self.viewPermit
        });
        if (existingWindow.justCreated) {
//            var newobjpanel = existingWindow.down('[itemId=objpanel]')
            existingWindow.on('onSave', function (args) {
                self.refresh()
            })
//            newobjpanel.loadRecord(record)
            existingWindow.onLoad(record);
        }
        existingWindow.show();
    }
})