/**
 * Created by IVAN on 02.06.2014.
 */
Ext.define('Billing.view.user.RolesForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.rolesform',
    header: false,
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        Ext.apply(this, {
            defaultType: 'textfield',
            layout:'border',
            items: [
                {
                    xtype: 'textfield',
                    margin: '5 10 5 10',
                    region:'north',
                    fieldLabel: 'Наименование',
                    labelWidth: 100,
                    name: 'name',
                    itemId: 'name'
                },
                {
                    xtype:'grid',
                    title    : 'Права',
                    region:'center',
                    store    : {
                        fields: [
                            {name: 'name', type: 'string'},
                            {name: 'value', type: 'string', hidden:true},
                            {name: 'active',  type: 'boolean'}
                        ],
                        data: [
                            {name: 'Просмотр учетной записи',value:'AccountView', active: false},
                            {name: 'Создание учетной записи',value:'AccountCreate', active: false},
                            {name: 'Измение учетной записи',value:'AccountDataSet', active: false},
                            {name: 'Удаление учетной записи',value:'AccountDelete', active: false},
                            {name: 'Просмотр объекта',value:'ObjectView', active: false},
                            {name: 'Создание объекта',value:'ObjectCreate', active: false},
                            {name: 'Измение объекта',value:'ObjectDataSet', active: false},
                            {name: 'Удаление объекта',value:'ObjectDelete', active: false},
                            {name: 'Перемещение объекта в корзину', value: 'ObjectRemove', active: false},
                            {name: 'Восстановление объекта', value: 'ObjectRestore', active: false},

                            {name: 'Просмотр оборудования',value:'EquipmentView', active: false},
                            {name: 'Создание оборудования',value:'EquipmentCreate', active: false},
                            {name: 'Измение оборудования',value:'EquipmentDataSet', active: false},
                            {name: 'Удаление оборудования',value:'EquipmentDelete', active: false},
                            {name: 'Перемещение оборудования в корзину', value: 'EquipmentRemove', active: false},
                            {name: 'Восстановление оборудования', value: 'EquipmentRestore', active: false},

                            {name: 'Просмотр пользователя',value:'UserView', active: false},
                            {name: 'Создание пользователя',value:'UserCreate', active: false},
                            {name: 'Измение пользователя',value:'UserDataSet', active: false},
                            {name: 'Удаление пользователя',value:'UserDelete', active: false},
                            {name: 'Изменение прав пользователя',value:'ChangePermissions', active: false},
                            {name: 'Просмотр типа оборудования',value:'EquipmentTypesView', active: false},
                            {name: 'Создание типа оборудования',value:'EquipmentTypesCreate', active: false},
                            {name: 'Измение типа оборудования',value:'EquipmentTypesDataSet', active: false},
                            {name: 'Удаление типа оборудования',value:'EquipmentTypesDelete', active: false},
                            {name: 'Просмотр тарифа',value:'TariffPlanView', active: false},
                            {name: 'Создание тарифа',value:'TariffPlanCreate', active: false},
                            {name: 'Измение тарифа',value:'TariffPlanDataSet', active: false},
                            {name: 'Удаление тарифа',value:'TariffPlanDelete', active: false},
                            {name: 'Изменение ролей пользователя',value: 'ChangeRoles', active: false},
                            {name: 'Изменение баланса учетной записи',value: 'ChangeBalance', active: false},
                            {name: 'Менеджер',value: 'Manager', active: false},
                            {name: 'Вход в аккаунты дилеров',value: 'DealerBackdoor', active: false},
                            {name: 'Монтажник',value: 'Installer', active: false},
                            {name: 'Кладовщик',value: 'Storekeeper', active: false},
                            {name: 'Бухгалтер',value: 'Accountant', active: false}
                            ]
                    },
                    columns  : [
                        { text : 'Действие', dataIndex : 'name',flex: 2 },
                        { xtype : 'checkcolumn', text : 'Разрешить', dataIndex : 'active',flex: 1 }
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
                        scope: this,
                        handler: this.onSave
                    }, {
                        icon: 'images/ico16_cancel.png',
                        text: 'Отменить',
                        scope: this,
                        handler: this.closeAction
                    }]
                }
            ]
        });
        this.callParent();
    },
    onSave: function () {
        var panel=this
        console.log("on save")
        //var active = this.activeRecord
        var id=panel.activeRecord.get("_id")
        var name=panel.down("[itemId=name]").getValue()
        var authGrid=panel.down('grid')
        var records=authGrid.getStore().getRange()
        var authorities = [];
        for (var i in records) {
            var state = records[i].get('active');
            if (state)
                authorities.push(records[i].get('value'));
        }
        console.log("id",id)
        console.log("name",name)
        console.log("authorities",authorities)
        var data={
            "_id":id,
            "name":name,
            "authorities":authorities
        }
        console.log("data",data)
        rolesService.update(data,function(){
            panel.fireEvent('save');
            panel.closeAction();
        })
//        var form = panel.getForm();
//        var record=form.getRecord();
//        if (!active) {
//           return;
//        }
//                if (form.isValid()) {
//                    active.setDirty(); //Иначе агрегаты с пустыми данными не обновляются
//                    form.updateRecord();
                    //this.onReset();
//                    var userGrid=Ext.ComponentQuery.query('[xtype=allusersgrid]')[0];
//                    if (!active.get("_id") || userGrid.getStore().getById(active.get("_id"))==null){
//                        console.log("NEW USER");
//                        this.fireEvent('newitemsave');
//                    }
//                    else this.fireEvent('save');
//                    console.log("name",name)
//                    this.closeAction();
//                }
    },
    setActiveRecord: function (record) {
        this.activeRecord = record;
        this.getForm().loadRecord(record);
        var grid=this.down('grid');
        var store=grid.getStore();
        var authData=record.get("authorities")
        for(i in authData){
            var rec=store.findRecord("value",authData[i])
            if(rec!=null)
            {rec.set("active",true)
                rec.commit()
            }
        }
    }
});
Ext.define('Billing.view.user.RolesWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.roleswnd',
    closable: true,
    maximizable: true,
    width: 600,
    height: 400,
    layout: 'fit',

    initComponent: function () {
        Ext.apply(this, {
            items:[{
                xtype:"rolesform",
                layout: {
                    type: 'vbox',       // Arrange child items vertically
                    align: 'stretch',    // Each takes up full width
                    padding: 20
                },
                closeAction: function () {
                    this.up('window').close();
                }
            }]
        });
        this.callParent();
    }
});
