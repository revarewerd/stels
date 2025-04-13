/**
 * Created by IVAN on 03.06.2014.
 */
Ext.define('Billing.view.user.RolesPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.userrolesgrid',
    title: 'Роли пользователей',
    requires:[
        //'Billing.view.user.UserForm'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    storeName: 'EDS.store.RolesService',
    invalidateScrollerOnRefresh: false,
    columns: [
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex:1
        },
        {
            header: '№',
            xtype: 'rownumberer',
            width:40,
            resizable:true
        },
        {
            header: 'Имя',
            flex: 1,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;"';
                return val;
            }
        },
        {
            header: 'Права',
            flex: 3,
            sortable: true,
            dataIndex: 'authorities',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
            metaData.tdAttr = '" title="'+val+'"';
            return val
            }
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex:1
        }
    ],
    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex
            switch(dataIndex)
            {
                case "name":
                    this.showRolesWindow(record);
                    break;
            }
        }
    },
    showRolesWindow: function (record) {

        var existingWindow = WRExtUtils.createOrFocus('rolesWnd' + record.get('_id'), 'Billing.view.user.RolesWindow', {
            title: 'Пользовательская роль "' + record.get('name') + '"'
        });
        var self = this;
        existingWindow.down('rolesform').on('save', function () {
            self.store.reload();
        });
        if (existingWindow.justCreated)
            existingWindow.down('rolesform').setActiveRecord(record);
        existingWindow.show();

        return existingWindow.down('rolesform');
    },
    onAddClick: function () {
        var rec = new EDS.model.RolesService({
        });
        this.showRolesWindow(rec);
    },
    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },
    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();
        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' пользователей?', function (button) {
                if (button === 'yes') {
                    store.remove(selection);
                }
            });
        }
    }
})