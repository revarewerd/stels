/**
 * Created by IVAN on 03.06.2014.
 */
Ext.define('Billing.view.user.UserRolesForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.userrolesform',
    header: false,
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        var rolesList=[];
        console.log("userType",self.userType)
        if(self.userType=="admin")
            rolesList=  [
            {name: 'Просмотр учетной записи', value: 'AccountView', active: false},
            {name: 'Создание учетной записи', value: 'AccountCreate', active: false},
            {name: 'Измение учетной записи', value: 'AccountDataSet', active: false},
            {name: 'Удаление учетной записи', value: 'AccountDelete', active: false},

            {name: 'Просмотр объекта', value: 'ObjectView', active: false},
            {name: 'Создание объекта', value: 'ObjectCreate', active: false},
            {name: 'Измение объекта', value: 'ObjectDataSet', active: false},
            {name: 'Удаление объекта', value: 'ObjectDelete', active: false},
            {name: 'Перемещение объекта в корзину', value: 'ObjectRemove', active: false},
            {name: 'Восстановление объекта', value: 'ObjectRestore', active: false},

            {name: 'Просмотр оборудования', value: 'EquipmentView', active: false},
            {name: 'Создание оборудования', value: 'EquipmentCreate', active: false},
            {name: 'Измение оборудования', value: 'EquipmentDataSet', active: false},
            {name: 'Удаление оборудования', value: 'EquipmentDelete', active: false},
            {name: 'Перемещение оборудования в корзину', value: 'EquipmentRemove', active: false},
            {name: 'Восстановление оборудования', value: 'EquipmentRestore', active: false},

            {name: 'Просмотр пользователя', value: 'UserView', active: false},
            {name: 'Создание пользователя', value: 'UserCreate', active: false},
            {name: 'Измение пользователя', value: 'UserDataSet', active: false},
            {name: 'Удаление пользователя', value: 'UserDelete', active: false},
            {name: 'Изменение прав пользователя', value: 'ChangePermissions', active: false},

            {name: 'Просмотр типа оборудования', value: 'EquipmentTypesView', active: false},
            {name: 'Создание типа оборудования', value: 'EquipmentTypesCreate', active: false},
            {name: 'Измение типа оборудования', value: 'EquipmentTypesDataSet', active: false},
            {name: 'Удаление типа оборудования', value: 'EquipmentTypesDelete', active: false},

            {name: 'Просмотр тарифа', value: 'TariffPlanView', active: false},
            {name: 'Создание тарифа', value: 'TariffPlanCreate', active: false},
            {name: 'Измение тарифа', value: 'TariffPlanDataSet', active: false},
            {name: 'Удаление тарифа', value: 'TariffPlanDelete', active: false},

            {name: 'Изменение ролей пользователя',value: 'ChangeRoles', active: false},
            {name: 'Изменение баланса учетной записи',value: 'ChangeBalance', active: false},
            {name: 'Менеджер',value: 'Manager', active: false},
            {name: 'Монтажник',value: 'Installer', active: false},            
            {name: 'Вход в аккаунты дилеров',value: 'DealerBackdoor', active: false},
            {name: 'Кладовщик',value: 'Storekeeper', active: false},
            {name: 'Бухгалтер',value: 'Accountant', active: false}
            ];
        else if(self.userType=="servicer") rolesList=  [
            {name: 'Менеджер',value: 'Manager', active: false},
            {name: 'Монтажник',value: 'Installer', active: false},
            {name: 'Кладовщик',value: 'Storekeeper', active: false},
            {name: 'Бухгалтер',value: 'Accountant', active: false}
            ];
        else if(self.userType=="superuser")
            rolesList=  [
                {name: 'Просмотр учетной записи', value: 'AccountView', active: false},
                {name: 'Просмотр объекта', value: 'ObjectView', active: false},
                {name: 'Просмотр оборудования', value: 'EquipmentView', active: false},
                {name: 'Просмотр пользователя', value: 'UserView', active: false},
                {name: 'Создание пользователя', value: 'UserCreate', active: false},
                {name: 'Измение пользователя', value: 'UserDataSet', active: false},
                {name: 'Удаление пользователя', value: 'UserDelete', active: false},
                {name: 'Изменение прав пользователя', value: 'ChangePermissions', active: false}
            ]
        Ext.apply(this, {
            defaultType: 'textfield',
            layout: 'border',
            items: [
                {
                    fieldLabel: 'Роль',
                    region: 'north',
                    margin: '5 10 5 10',
                    labelWidth: 50,
                    name: 'role',
                    xtype: 'combobox',
                    queryMode: 'local',
                    forceSelection: 'true',
                    allowBlank: false,
                    valueField: "_id",
                    displayField: "name",
                    disabled: (self.userType=="user"),
                    store: Ext.create('EDS.store.RolesService', {
                        autoLoad: true,
                        listeners: {
                            beforeload: function (store, op) {
                                console.log("userId", self.up('window').userId)
                                store.getProxy().setExtraParam("userId", self.up('window').userId)
                            },
                            load: function (store, op) {
                                store.insert(0, new EDS.model.RolesService({"name": "- Пользовательская -", "_id": "-1", "authorities": []}))
                            }
                        }
                    }),
                    listeners: {
                        'change': function (box, newValue, oldValue, eOpts) {
                            var grid = this.up('form').down('grid')
                            var gridStore = grid.getStore()
                            console.log("checkchange newValue", newValue)
                            if (newValue != null && newValue != "-1") {
                                var rec = box.getStore().getById(newValue)
                                var authData = rec.get("authorities")
                                var gridRecs = gridStore.getRange()
                                for (i in gridRecs) {
                                    gridRecs[i].set("active", false)
                                    gridRecs[i].commit()
                                }
                                for (i in authData) {
                                    var rec = gridStore.findRecord("value", authData[i])
                                    if (rec != null) {
                                        rec.set("active", true)
                                        rec.commit()
                                    }
                                }
                            }
                        }
                    }
                },
                {
                    xtype: 'grid',
                    region: 'center',
                    title: 'Права',
                    store: {
                        fields: [
                            {name: 'name', type: 'string'},
                            {name: 'value', type: 'string', hidden: true},
                            {name: 'active', type: 'boolean'}
                        ],
                        data: rolesList
                    },
                    columns: [
                        { text: 'Действие', dataIndex: 'name', flex: 2, sortable: false },
                        { xtype: 'checkcolumn',
                            text: 'Разрешить',
                            dataIndex: 'active',
                            flex: 1,
                            listeners: {
                                checkchange: function (column, rowIndex, checked, eOpts) {
                                    column.up('window').down('[name=role]').setValue("-1", false)
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
                    items: [
                        '->',
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'save',
                            text: 'Сохранить',
                            disabled: (self.userType=="user"),
                            scope: this,
                            handler: this.onSave
                        }, {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: this.closeAction
                        }]
                }
            ],
            listeners: {
                afterrender: function (form, eopts) {
                    var userId = form.up('window').userId
                    console.log('userId', userId)
                    rolesService.checkChangeRoleAuthority(function (res) {
                        if (!res) {
                            form.down("[itemId=save]").disable(true)
                            form.down("[xtype=checkcolumn]").disable(true)
                            form.down("[name=role]").getPicker().disable(true)
                        }
                        form.onLoad(userId);
                    })

                }
            }
        });
        this.callParent();
    },
    onSave: function () {
        var panel = this
        console.log("on save")
        var templateId = panel.down("[name=role]").getValue()
        var authGrid = panel.down('grid')
        var records = authGrid.getStore().getRange()
        var authorities = new Array();
        console.log("templateId", templateId)
        if (templateId == "-1")
            for (var i in records) {
                var state = records[i].get('active');
                if (state)
                    authorities.push(records[i].get('value'));
            }
        console.log("authorities", authorities)
        var data = {
            "userId": panel.up('window').userId,
            "templateId": templateId,
            "authorities": authorities
        }
        console.log("data", data)
        rolesService.updateUserRole(data, function (res, e) {
            if (e.type === "exception") {
                console.log("exception=", e);
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                panel.fireEvent('save');
                panel.closeAction();
            }
        })
    },
    onLoad: function (userId) {
        var self = this
        var authGrid = self.down('grid')
        var authStore = authGrid.getStore()
        var role = self.down('[name=role]')
        console.log("on load")
        rolesService.getUserRole(userId, function (data) {
            var authorities = data.authorities
            console.log("data", data)
            if (data.templateId == -1 || data.templateId == undefined) {
                role.setValue("-1", true)
                for (i in authorities) {
                    var rec = authStore.findRecord("value", authorities[i])
                    if (rec != null) {
                        rec.set("active", true)
                        rec.commit()
                    }
                }
            }
            else role.setValue(data.templateId, true)
        })
    }
});
Ext.define('Billing.view.user.UserRolesWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.userroleswnd',
    closable: true,
    maximizable: true,
    width: 600,
    height: 400,
    layout: 'fit',

    initComponent: function () {
        var self=this;
        console.log("userType",self.userType)
        Ext.apply(this, {
            items: [
                {
                    xtype: "userrolesform",
                    userType:self.userType,
                    layout: {
                        type: 'vbox',       // Arrange child items vertically
                        align: 'stretch',    // Each takes up full width
                        padding: 20
                    },
                    closeAction: function () {
                        this.up('window').close();
                    }
                }
            ]
        });
        this.callParent();
    }
});
