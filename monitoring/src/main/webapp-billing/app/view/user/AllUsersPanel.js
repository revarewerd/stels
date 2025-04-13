Ext.define('Billing.view.user.AllUsersPanel', {
    extend: 'WRExtUtils.WRGrid',    
    alias: 'widget.allusersgrid',
    title: 'Пользователи',
    requires:[
    'Billing.view.user.UserForm'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],    
    storeName: 'EDS.store.UsersService',
    invalidateScrollerOnRefresh: false,
    itemType:'users',
    dockedToolbar: ['add', 'remove', 'refresh', 'search','fill','gridDataExport'],
    viewConfig: {
         getRowClass: function(record, rowIndex, rowParams, store){
                            var rowCss=' '
                            if(record.get("enabled") == false) rowCss='redText';
                            else if(record.get("hasBlockedMainAccount") == true)  rowCss='redText';
                            else if(record.get("hasObjectsOnBlockedAccount") == true)  rowCss='yellowText';
                            return rowCss;
                        }
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
            menuDisabled: true,
            sortable: false,
            xtype: 'actioncolumn',
            itemId: 'usr',
            width: 20,
            resizable: false,
            items: [
                {
                    icon: 'extjs-4.2.1/examples/shared/icons/fam/user_suit.gif'
                }
            ]
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
            }//,
//        summaryType: 'count',
//        summaryRenderer: function(value, summaryData, dataIndex) {
//            return Ext.String.format('<b>Всего позиций: {0} </b>', value); 
//        }
        },
        {
            header: 'Тип пользователя',
            flex: 1,
            dataIndex: 'userType',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Комментарий',
            flex: 1,
            sortable: true,
            dataIndex: 'comment',
            filter: {
                type: 'string'
            },
            hidden: !this.viewPermit,
            summaryType: 'count',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
            }
        },
        {
            header: 'Основная учетная запись',
            flex: 1,
            sortable: true,
            dataIndex: 'mainAccName',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Статус',
            dataIndex: 'enabled',
            width: 100,
            renderer: function (value) {
                if (value) return "Включен"
                else return "Заблокирован"
            }
        },
        {
            header: 'Войти',
            width: 150,
            sortable: false,
            dataIndex: "name",
            align: 'left',
            renderer: function (value) {
                return '<a href="EDS/monitoringbackdoor?login=' + encodeURIComponent(value) + '" target="slavemonitoring"> Войти как пользователь </a>';
            }
        },
        {
            xtype: 'actioncolumn',
            header: 'Права',
            width: 60,
            sortable: false,
            align: 'center',
            items: [
                {
                    icon: 'extjs-4.2.1/examples/shared/icons/fam/cog_edit.png',
                    //iconCls: 'mousepointer',
                    tooltip: 'Редактировать права пользователя',
                    handler: function (grid, rowIndex, colIndex) {
                        var grid = this.up("[xtype=allusersgrid]")
                        var rec = grid.getStore().getAt(rowIndex);
                        if (rec.get("_id")) {
                            WRExtUtils.createOrFocus('userPermWnd' + rec.get("_id"), 'Billing.view.user.UserPermissionsWindow', {
                                userId: rec.get("_id"),
                                userName: rec.get('name'),
                                hideRule: !grid.viewPermit
                            }).show();
                        }
                        else {
                            Ext.MessageBox.show({
                                title: 'Пользователь ещё не создан',
                                msg: 'Сохраните пользователя перед тем как назначать ему права',
                                buttons: Ext.MessageBox.OK,
                                icon: Ext.MessageBox.WARNING
                            });
                        }
                    }
                }
            ]
        },
        {
            header: 'Последний вход',
            flex: 1,
            sortable: true,
            dataIndex: 'lastLoginDate',
            xtype: 'datecolumn',
            format: "d.m.Y H:i:s"
        },
        {
            header: 'Последнее действие',
            flex: 1,
            sortable: true,
            dataIndex: 'lastAction',
            xtype: 'datecolumn',
            format: "d.m.Y H:i:s"
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        }
    ],
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var itemId=self.columnManager.columns[cellIndex].itemId;
            if (itemId == "usr") {
                this.showUserForm(record)
            }
        },
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self = this;
            var dataIndex = self.columnManager.columns[cellIndex].dataIndex
            switch (dataIndex) {
                case "name":
                    this.showUserForm(record);
                    break;
            }
        }
    },
    initComponent: function () {
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    showUserForm: function (record) {
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('userWnd' + record.get('_id'), 'Billing.view.user.UserForm.Window', {
            title: 'Пользователь "' + record.get('name') + '"',
            hideRule: !self.viewPermit
        });
        if (existingWindow.justCreated)
            existingWindow.down('userform').onLoad(record.get("_id"));
        existingWindow.show();

        return existingWindow.down('userform');
    },

    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },
    onDeleteClick: function () {
        var self = this;
        var selection = this.getView().getSelectionModel().getSelection();
        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите безвозвратно удалить ' + selection.length + ' пользователей?', function (button) {
                if (button === 'yes') {
                    store.remove(selection);     
                    //self.refresh();
//                    for(var i=0;i<selection.length;i++)
//                    {
//                        usersPermissionsChecker.deleteUserPermissions(selection[i].getId());
//                    }
                }
            });
        }
    },
    onAddClick: function () {
        var rec = Ext.create('User',{})
        this.showUserForm(rec);
    },
    updateData:function(data){
        this.changeData(data,"user")
    }
});