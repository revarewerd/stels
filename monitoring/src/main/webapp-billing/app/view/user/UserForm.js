Ext.define('Billing.view.UserForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.userform',
    header: false,
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        Ext.apply(this, {
            //activeRecord: null,
            defaultType: 'textfield',
            bodyPadding: 5,
            fieldDefaults: {
                anchor: '100%',
                labelAlign: 'right',
                labelWidth: 150
            },
            items: [
                {
                    fieldLabel: 'Имя',
                    name: 'name',
                    allowBlank: false
                },
                {
                    fieldLabel: 'Комментарий',
                    name: 'comment',
                    hidden:self.hideRule
                    //allowBlank: false
                },
                {   
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Пароль',
                    layout: 'hbox',                    
                    items: [
                    {
                        xtype: 'textfield',
                        //fieldLabel: 'Пароль',
                        name: 'password',
                        inputType: 'password',
                        flex:1,
                        allowBlank: false
                    },
                    {
                        xtype: 'splitter',
                        width:20
                    },
                    {
                        xtype: 'checkbox',
                        name:'canchangepass',
                        inputValue:'canchangepass',
                        boxLabel:'Разрешить изменение'
                    }
                    ]                            
                },               
                {
                    fieldLabel:'e-mail',
                    name:'email',
                    vtype: 'email' 
                },
                {
                    fieldLabel:'Телефон',
                    name:'phone'
                },
                {
                    fieldLabel:'Тип',
                    name:'userType',
                    xtype: 'combobox',
                    queryMode: 'local',
                    valueField:"userType",
                    displayField:"typeName",
                    forceSelection:'true',
                    allowBlank:false
                },
                {
                    fieldLabel:'Основная учетная запись',
                    name:'mainAccId',
                    xtype: 'combobox',
                    valueField:"_id",
                    displayField:"name", 
                    queryMode: 'local',
                    store: Ext.create('EDS.store.AccountsDataShort', {
                                autoLoad: true,
                        listeners: {
                            beforeload: function (store) {
                                var userId=null;
                                var record=self.getRecord();
                                if(record) userId=self.getRecord().get('_id')
                                if(userId==null || userId=="") userId='new'
                                store.getProxy().setExtraParam("userId", userId);
                            },
                            load: function (store, op) {
                                store.insert(0, new EDS.model.AccountsDataShort({"name": "- нет -", "_id": "-1"}))
                                var record=self.getRecord();
                                if(record) self.down("[name=mainAccId]").setValue(record.get("mainAccId"))
                            }
                        }
                    }),
                    listeners: {
                        afterrender: function () {
                            this.forceSelection=true                          
                        }
                    }
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Показывать баланс',
                    layout: 'hbox',
                    items: [
                        {
                            xtype: 'checkbox',
                            name:'showbalance',
                            inputValue:'showbalance',
                            listeners:{
                                'change':function(chbox, newValue, oldValue, eOpts){
                                    var feedetails=this.up('fieldcontainer').down('field[name=showfeedetails]')
                                    console.log("checkchange newValue",newValue)
                                    if(newValue)
                                     {
                                         feedetails.enable()
                                     }
                                    else
                                     {
                                         feedetails.setValue(false)
                                         this.up("form").updateRecord(this.up("form").getRecord())
                                         feedetails.disable()
                                     }
                                }
                            }
                        },
                        {
                            xtype: 'splitter',
                            width:20
                        },
                        {
                            fieldLabel: 'Показывать детализацию',
                            xtype: 'checkbox',
                            name:'showfeedetails',
                            inputValue:'showfeedetails',
                            disabled:true
                        }
                    ]
                },
                {   
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Пароль для команд',
                    layout: 'hbox',                    
                    items: [
                    {
                        xtype: 'checkbox',
                        name:'hascommandpass',
                        inputValue:'hascommandpass',
                        listeners:{
                            'change':function(chbox, newValue, oldValue, eOpts){
                                var commandpass=this.up('fieldcontainer').down('field[name=commandpass]')
                                if(newValue)
                                    commandpass.enable()
                                else
                                    commandpass.disable()
                            }
                        }
                    },
                    {
                        xtype: 'splitter',
                        width:20
                    },

                    {
                        xtype: 'textfield',
                        fieldLabel: 'Пароль',
                        inputType: 'password',
                        disabled:true,
                        labelWidth:50,
                        flex:1,
                        name:'commandpass',
                        itemId:'commandpass'
                    }
                    ]                            
                },
                {   
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Включен',
                    layout: 'hbox',                    
                    items: [
                    {
                        xtype: 'checkbox',
                        name:'enabled',
                        inputValue:'enabled',
                        listeners:{
                            'change':function(chbox, newValue, oldValue, eOpts){
                                if(!newValue)
                                    this.up('fieldcontainer').down('field[name=blockcause]').enable()
                                else
                                    this.up('fieldcontainer').down('field[name=blockcause]').disable()
                            }
                        }
                    },
                    {
                        xtype: 'splitter',
                        width:20
                    },

                    {
                        xtype: 'textfield',
                        fieldLabel: 'Причина блокировки',
                        labelWidth:130,
                        flex:1,
                        name:'blockcause',
                        itemId:'blockcause'
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
                            xtype: 'button',
                            text: 'Права',
                            handler: function () {


                                if (self.getRecord().get("_id")) {
                                    var wnd= WRExtUtils.createOrFocus('userPermWnd' + self.getRecord().get('_id'), 'Billing.view.user.UserPermissionsWindow', {
                                        userId: self.getRecord().get('_id'),
                                        userName: self.getRecord().get('name'),
                                        hideRule:self.hideRule
                                    })
                                        wnd.on("close",function(){self.down("[name=mainAccId]").getStore().load()})
                                        wnd.show();
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
                        },
                        {
                            xtype: 'button',
                            text: 'Роли',
                            disabled:self.hideRule,
                            handler: function () {

                                self.updateRecord();
                                if (self.getRecord().get("_id")) {
                                    WRExtUtils.createOrFocus('userRolesWnd' + self.getRecord().get('_id'), 'Billing.view.user.UserRolesWindow', {
                                        title: 'Роли пользователя "'+self.getRecord().get('name')+'"',
                                        userId: self.getRecord().get('_id'),
                                        userName: self.getRecord().get('name'),
                                        userType: self.getRecord().get('userType')
                                    }).show();
                                }
                                else {
                                    Ext.MessageBox.show({
                                        title: 'Пользователь ещё не создан',
                                        msg: 'Сохраните пользователя перед тем как назначать ему роль',
                                        buttons: Ext.MessageBox.OK,
                                        icon: Ext.MessageBox.WARNING
                                    });
                                }
                            }
                        },
                        {text: 'История',
                         disabled:self.hideRule,
                         handler: function () {
                            var self=this; 
                            var id=this.up('form').getRecord().get("_id");
                            var existingWindow = WRExtUtils.createOrFocus('UserEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                                aggregateId:id,
                                aggregateType:"UserAggregate"
                            });
                            existingWindow.show();
                            console.log("existingWindow",existingWindow)
                        }
                        },
                        '->',
                        {
                            icon: 'images/ico16_checkall.png',
                            itemId: 'save',
                            text: 'Сохранить',
                            handler: function () {
                                self.onSave(false);
                            }
                        },
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'savenclose',
                            text: 'Сохранить и закрыть',
                            handler: function () {
                                self.onSave(/*close*/true);
                            }
                        }, {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: this.closeAction
                        }
                    ]
                }
            ],
            listeners:{
                afterrender:function(form){

                }
            }
        });
        this.callParent();
    },

    setAvailableUserTypes: function (record) {
        var self=this
        //self.activeRecord = record;
        var userType=self.down("[name=userType]")
        var userName=record.get("name")//self.activeRecord.get("name")
        rolesService.getAvailableUserTypes(userName,function(result){
            if(result){
                var userTypeStore=Ext.create("Ext.data.Store",{
                    fields: [
                        {name: 'typeName', type: 'string'},
                        {name: 'userType',  type: 'string'}
                    ],
                    data: result
                })
                userType.bindStore(userTypeStore);
            }
            self.getForm().loadRecord(record)
        })
        //this.onLoad(record.get("_id"))
        //this.getForm().loadRecord(record);
    },

    onSave: function (close) {
        console.log("on Save")
        var self=this;
        //var active = this.activeRecord;
        var form = this.getForm();
//        if (!active) {
//            return;
//        }
        if (form.isValid()) {
            //active.setDirty(); //Иначе агрегаты с пустыми данными не обновляются
            //self.getRecord().updateRecord();
            var record=form.getRecord()
            console.log("record=",record)
            console.log("Form updateRec=", form.updateRecord());
            var resultFun=function(result,e){
                if (e.type === "exception") {
                    console.log("exception=", e);
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                    //self.up('window').close()
                }
                else {
                    console.log("saveResult=",result)
                    console.log("id=",result._id)
                    if(close)
                    {self.closeAction();}
                    else
                    self.onLoad(result._id)
                }
            }
            //form.updateRecord(active);
            //this.onReset();
            //console.log("active.get(_id)",active.get("_id"))
            var userGrid=Ext.ComponentQuery.query('[xtype=allusersgrid]')[0];
            if (!record.get("_id") || userGrid.getStore().getById(record.get("_id"))==null){
                console.log("NEW USER")
                usersService.create(form.getRecord().getData(), resultFun)
               // this.fireEvent('newitemsave');
            }
            else {
                usersService.update(form.getRecord().getData(),resultFun)
//                    function(result,e){
//                    if (e.type === "exception") {
//                        console.log("exception=", e);
//                        Ext.MessageBox.show({
//                            title: 'Произошла ошибка',
//                            msg: e.message,
//                            icon: Ext.MessageBox.ERROR,
//                            buttons: Ext.Msg.OK
//                        });
//                        self.up('window').close()
//                    }
//                    else {
//                        console.log("saveResult=",result)
//                        console.log("id=",result._id)
//                        self.onLoad(result._id)
//                    }
//                }
                //)
            }
            //this.closeAction();
        }       
    },

    onLoad: function (userId) {
        console.log("on Load");
        var self=this;
        console.log("userId=", userId);
        var currentUser=Ext.create('User',{})
        if (userId  != null) {
            usersService.load(userId, function (loadResult,e) {
                if (e.type === "exception") {
                    console.log("exception=", e);
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                    self.up('window').close()
                }
                else {
                    console.log("loadResult=",loadResult);
                    currentUser = Ext.create('User',loadResult);
                    console.log("currentUser=",currentUser);
                    self.getForm().loadRecord(currentUser);
                    self.setAvailableUserTypes(currentUser);
                    self.down("[name=mainAccId]").getStore().load();
                    self.up('window').setTitle("Пользователь \""+loadResult.name+"\"");
                }
            })
        }
        else {self.onReset() ;self.setAvailableUserTypes(currentUser),self.down("[name=mainAccId]").getStore().load();}
    },

    onReset: function () {        
        this.getForm().reset();
    }
});
Ext.define('Billing.view.user.UserForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.userwnd',
    title: 'Пользователь "',
    width: 600,
    height: 350,
    layout: 'fit',
    icon: 'extjs-4.2.1/examples/shared/icons/fam/user_suit.gif',
    maximizable: true,
    initComponent: function () {
        var self = this;        
        Ext.apply(this,{
            items: [
                {
                    xtype: 'userform',
                    hideRule:self.hideRule,
                    closeAction: function () {
                        this.up('window').close();
                    }
                }
            ]         
            }) 
        this.callParent()
    }
});
Ext.define('User', {
        extend: 'Ext.data.Model',
        fields: [
            "_id", "name", "comment", "password", "phone", "email", "lastLoginDate",
           "lastAction", "mainAccId", "mainAccName", "hascommandpass", "commandpass", "enabled",
            "blockcause","canchangepass","showbalance","showfeedetails","userType","creator","hasBlockedMainAccount","hasObjectsOnBlockedAccount"
        ],
        idProperty: '_id'
    }
);