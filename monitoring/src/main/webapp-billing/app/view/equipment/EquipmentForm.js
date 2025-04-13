Ext.define('Billing.view.equipment.EquipmentForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.eqform',   
    title: 'Устройство ?',     
    layout: {
          type:'vbox',
          align:'stretch'
      },      
    border:false,
    hideRule:true,
    initComponent: function () {
        
        var self = this;
        Ext.apply(this, {
            items:[
           {xtype: 'tabpanel',           
            border:false,
            defaults:
                    {border:false},                          
             items: [
                    {title: 'Основные сведения',                        
                        xtype: 'form',                        
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{
                                margin: '0 20 5 20'//,
                                //vtype:'alphanum',                                
                            }, 
                            items: [
                              {                                    
                                  margin: '20 20 5 20',
                                  fieldLabel: 'Собственник',
                                  name:'eqOwner',
                                  readOnly:this.hideRule
                              },                             
                              {    
                                  fieldLabel: 'Основание использования',                                  
                                  name:'eqRightToUse',
                                  readOnly:this.hideRule
                              },
                              {   
                                  xtype: 'datefield',
                                  fieldLabel: 'Дата продажи',
                                  maxValue:new Date(),
                                  name:'eqSellDate',
                                  format:'d.m.Y',
                                  altFormats:'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j',
                                  readOnly:this.hideRule
                              },
                              {    
                                  fieldLabel: 'Работа',                                  
                                  name:'eqWork',
                                  readOnly:this.hideRule
                              },
                              {   
                                  xtype: 'datefield',
                                  fieldLabel: 'Дата работы',
                                  maxValue:new Date(),
                                  format:'d.m.Y',
                                  altFormats:'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j',
                                  name:'eqWorkDate',
                                  readOnly:this.hideRule
                              },                  
                              {   margin: '0 20 20 20',
                                  xtype: 'textareafield',                                    
                                  fieldLabel: 'Примечание',
                                  rows:2,
                                  name: 'eqNote',
                                  hidden:this.hideRule}
                            ]                    
                   },
                    {title:'Характеристики устройства',
                     xtype: 'form',
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{  
                                margin: '0 20 5 20'//,
                                //vtype:'alphanum',                                
                            },                            
                            items: [
                                {
                                    margin: '20 20 5 20',
                                    xtype: 'combobox',
                                    fieldLabel: 'Тип',
                                    name: 'eqtype',
                                    allowBlank: false,
                                    forceSelection:true,
                                    //queryMode: 'local',
                                    store: Ext.create('EDS.store.EquipmentDeviceTypesService', {
                                        autoLoad: true,
                                        buffered:false,
                                        listeners:{
                                            load:function(store, records, successful, eOpts){
                                             var combo=self.down("[name=eqtype]")
                                                var eqRec=self.up("#eqPanel").getRecord()
                                                var eqtype=""
                                                if(eqRec!=null && eqRec!=undefined){
                                                    eqtype=eqRec.get("eqtype")
                                                }
                                                combo.setValue(eqtype)
                                            }
                                        }
                                    }),
                                    //store: [
                                    //            ['Основной абонентский терминал', 'Основной абонентский терминал'],
                                    //            ['Дополнительный абонентский терминал', 'Дополнительный абонентский терминал'],
                                    //            ['Спящий блок автономного типа GSM', 'Спящий блок автономного типа GSM'],
                                    //            ['Спящий блок на постоянном питании типа Впайка', 'Спящий блок на постоянном питании типа Впайка'],
                                    //            ['Радиозакладка', 'Радиозакладка'],
                                    //            ['Датчик уровня топлива', 'Датчик уровня топлива'],
                                    //            ['Виртуальный терминал', 'Виртуальный терминал']
                                    //],
                                    listeners:{
                                        change:function( cmb, newValue, oldValue, eOpts ){
                                            var panel=cmb.up('[itemId=eqPanel]');                                            
                                            if(!panel)                                                
                                                var rec=cmb.up('eqform').getRecord();
                                            else 
                                                rec=panel.getRecord();
                                            var eqMark=self.down('[name=eqMark]');
                                            var eqMarkStore=eqMark.getStore() ;  
                                            //self.down('[name=eqMark]').setValue("")
                                            var eqtype=cmb.getValue();
                                                if(eqtype.match('абонентский')) eqtype='Абонентский терминал';
                                            equipmentTypesData.loadMarkByType(eqtype,function(result){
                                                console.log('result',result);                                                
                                                   eqMarkStore.loadData(result);                                                  
                                                   eqMark.setValue(rec.get("eqMark"));
                                                   eqMark.fireEvent('change',eqMark);   
                                               })                                            
                                        }
                                    },
                                    valueField: 'name',
                                    displayField: 'name',
                                    typeAhead: true,
                                    readOnly:this.hideRule
                                },
                                {                                       
                                    fieldLabel: 'Марка',
                                    xtype: 'combobox',
                                    name:'eqMark',
                                    valueField: 'eqMark',
                                    displayField: 'eqMark',
                                    typeAhead: true,                                    
                                    //forceSelection:true,
                                    queryMode: 'local',
                                    store:Ext.create('Ext.data.Store', {
                                        fields:['eqMark']
                                    }),
                                    listeners:{
                                        change:function( cmb, newValue, oldValue, eOpts ){                                            
                                            var panel=cmb.up('[itemId=eqPanel]');                                            
                                            if(!panel)                                       
                                                var rec=cmb.up('eqform').getRecord();
                                            else 
                                                rec=panel.getRecord();                                            
                                            var eqModelStore=self.down('[name=eqModel]').getStore();              
                                            //self.down('[name=eqModel]').setValue("")
                                            var eqtype=self.down('[name=eqtype]').getValue();
                                            if(eqtype!= null && eqtype.match('абонентский')) eqtype='Абонентский терминал';
                                            equipmentTypesData.loadModelByMark(eqtype,cmb.getValue(),function(result){
                                                console.log('result',result);                                                
                                                   eqModelStore.loadData(result);
                                                   console.log('rec.get("eqModel")',rec.get("eqModel"));
                                                   self.down('[name=eqModel]').setValue(rec.get("eqModel"));
                                               })
                                        }
                                    },
                                    readOnly:this.hideRule
                                },                                
                                {   
                                    fieldLabel: 'Модель',
                                    xtype: 'combobox',
                                    valueField: 'eqModel',
                                    displayField: 'eqModel',
                                    name:'eqModel',
                                    typeAhead: true,                                    
                                    //forceSelection:true,
                                    queryMode: 'local',
                                    store:Ext.create('Ext.data.Store', {
                                        fields:['eqModel']
                                    }),
                                    readOnly:this.hideRule
                                },
                                {   
                                    fieldLabel: 'Серийный номер',
                                    name:'eqSerNum',
                                    readOnly:this.hideRule},
                                {   
                                    fieldLabel: 'IMEI',
                                    name:'eqIMEI',
                                    vtype:'alphanum',
                                    readOnly:this.hideRule
                                },
                                {   
                                    fieldLabel: 'Прошивка',
                                    name:'eqFirmware',
                                    hidden:this.hideRule
                                },
                                {   
                                    fieldLabel: 'Конфигурация',
                                    name:'eqConfig',
                                    hidden:this.hideRule
                                },
                                {    
                                    fieldLabel: 'Логин',                                  
                                    name:'eqLogin',
                                    hidden:this.hideRule
                                },
                                {   margin: '0 20 20 20', 
                                    fieldLabel: 'Пароль',                                  
                                    name:'eqPass',
                                    hidden:this.hideRule
                                }                                
                            ]                    
                        },                    
                    {title: 'SIM-карта',
                        xtype: 'form',
                        hidden:this.hideRule,
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{  
                                margin: '0 20 5 20'//,
                                //vtype:'alphanum',
                                //labelWidth:150
                            }, 
                            items: [
                                {   margin: '20 20 5 20',                                    
                                    fieldLabel: 'Собственник',
                                    name:'simOwner'},
                                {                                       
                                    fieldLabel: 'Оператор',
                                    name:'simProvider'},                                
//                                {   
//                                    fieldLabel: 'Договор',
//                                    name:'simContract'},
//                                {   
//                                    fieldLabel: 'Тарифный план',
//                                    name:'simTariff'},
//                                {   
//                                    xtype: 'datefield',
//                                    fieldLabel: 'Дата получения',
//                                    value:new Date(),
//                                    name:'simSetDate',
//                                    format:'d.m.Y',
//                                    altFormats:'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
//                                }, 
                                {   
                                    fieldLabel: 'Абонентский номер',
                                    name:'simNumber'},
                                {   
                                    fieldLabel: 'ICCID',
                                    name:'simICCID'},
                                {   
                                  margin: '0 20 20 20',
                                  xtype: 'textareafield',                                    
                                  fieldLabel: 'Примечание',
                                  rows:2,
                                  name: 'simNote'}                               
                            ]                    
                    },
                    {title: 'Место установки',
                        xtype: 'form',                        
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{  
                                margin: '0 20 5 20'//,
                                //vtype:'alphanum',                                
                            }, 
                            items: [
                                { 
                                  margin: '20 20 5 20',
                                  fieldLabel: 'Место установки',
                                  name: 'instPlace',
                                  readOnly:this.hideRule
                                                           }
                            ]
                     },
                     {   
                         title: 'Настройка оборудования',
                         xtype: 'form',
                         hidden:this.hideRule,
                         fileUpload: true,
                         api: {
                                submit: configFileLoader.uploadFile
                         },
                         name: 'configFileForm',
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{  
                                margin: '0 20 5 20'//,
                                //vtype:'alphanum',                                
                            },
                         items: [
                             {
                                 margin: '20 20 5 20',
                                 fieldLabel: 'Загрузить файл настройки',
                                 xtype: 'filefield',
                                 name: 'configFile',
                                 buttonText: 'Выбрать файл'
                             },
                             {
                                 margin: '20 20 5 20',
                                 fieldLabel: 'Загрузить файл прошивки',
                                 xtype: 'filefield',
                                 name: 'fwFile',
                                 buttonText: 'Выбрать файл'
                             },
                             {
                                 xtype: 'checkbox',
                                 name: "forceConnection",
                                 fieldLabel: 'Форсировать подключение',
                                 checked: false
                             },
                             {
                                 xtype: 'hidden',
                                 name: 'IMEI',
                                 value: "123"
                             }
                         ]
                     }
                ]
            }
            ]
        });
        this.callParent();   
    }
});

Ext.define('Billing.view.equipment.EquipmentWindow', { 
extend: 'Ext.window.Window',    
alias: 'widget.eqwnd',
width: 800,
    height: 450,
    //constrainHeader: true,
    layout:'fit',
    maximizable:true,
    icon: 'images/ico16_device_def.png',
    initComponent: function () {

        var self = this;
        Ext.apply(this, {
    items: [
        {xtype:'form',
         itemId:'eqPanel',
            layout: 
                {type:'vbox', align:'stretch'},
            items:[
            {xtype:'form',
            layout: {
                        type:'vbox',
                        align:'stretch'
                    },
            defaultType:'textfield',
            items:[
//                {
//                     margin: '20 20 5 20',
//                     labelAlign:'right',
//                     labelWidth:100,
//                     fieldLabel: 'Учетная запись',
//                     name:'accountName',
//                     readOnly:true
//                },
                {
                    margin: '20 20 5 20',
                    xtype: 'combobox',
                    fieldLabel: 'Учетная запись',
                    name: 'accountId',
                    store: Ext.create('EDS.store.AccountsDataShort', {
                        autoLoad: true
                    }),
                    valueField: '_id',
                    displayField: 'name',
//                            typeAhead: true,
                    forceSelection: true,
                    minChars: 0,
                    allowBlank: false,
                    readOnly:this.hideRule
//                    listeners:{
//                        afterrender: function(cmb){
//                            var store=cmb.getStore();
//                            console.log("store",store);
//                            var value=cmb.findRecordByDisplay("Без Аккаунта")
//                            console.log("value",value);
//                        }
//                    }

                },
                {
                     margin: '5 20 20 20',
                     labelAlign:'right',
                     labelWidth:100,
                     fieldLabel: 'Объект',                                  
                     name:'objectName',
                     readOnly:true
             }]                    
            },
            {
                xtype: 'eqform',
                hideRule:self.hideRule,
                fieldDefaults:{
                    labelAlign:'right',
                    labelWidth:120
                    },
                title:false,
                closeAction: function () {
                    this.up('window').close();
                }
            }
        ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: [
                {text: 'История',
                 disabled:this.hideRule,
                         handler: function () {
                            var self=this; 
                            var id=this.up('form').getRecord().get("_id");
                            var existingWindow = WRExtUtils.createOrFocus('EquipmentEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                                aggregateId:id,
                                aggregateType:"EquipmentAggregate"
                            });
                            existingWindow.show();
                            console.log("existingWindow",existingWindow)
                        }
                },
                '->', {
                icon: 'images/ico16_okcrc.png',
                disabled:this.hideRule,
                itemId: 'save',
                text: 'Сохранить',                
                handler: function(){this.up('form[itemId=eqPanel]').onSave();}
            }, {
                icon: 'images/ico16_cancel.png',
                text: 'Отменить',                                     
                handler: function(){                            
                    this.up('window').close();}
            }]
        }
    ],
    listeners: {
        afterrender: function (panel, eopts) {
            console.log("Form rendered ", panel);
            var record=panel.getRecord();
            if(record)
            {console.log("Form record", record);
            if(record.get('_id')) panel.onLoad();
            }
        }
    },
    onSave: function () {
        var self=this;  
        console.log('onSave',self);
        var form = self.getForm();
        console.log('form rec=',form.getRecord());
        if (form.isValid()) { 
        form.updateRecord(); 
        var data=form.getRecord().getData();
         equipmentData.updateData(data, function(result, e){             
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
                    console.log("Результат запроса", result, " e=", e);
//                        self.fireEvent('save');
//                        var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]')
//                        eqaddwnd[0].down('grid').getStore().load()
//                        console.log('.getStore()',eqaddwnd[0].down('grid').getStore().getRange())
//                        var eqaddstore=eqaddwnd[0].down('grid').getStore()
//                        var eqId=result.eqId 
//                        data["_id"]=eqId
//                        console.log('eqId',eqId,data)
//                        if(eqaddstore.find('eqIMEI',data["eqIMEI"])<0)
//                        eqaddstore.add(data)
//                        self.up('window').close();
                        var configFileForm=self.up('window').down('[name=configFileForm]');                                  
                        if(configFileForm.isValid() && configFileForm.isDirty())
                            {
                            console.log("configFIleForm " +configFileForm);
                            configFileForm.down('[name=IMEI]').setValue(form.getRecord().get('eqIMEI'));  
                            var configUploadMsg="";
                            configFileForm.submit({                                
                                waitMsg: 'Загрузка файла конфигурации...',
                                success: function(f, o) {
                                    configUploadMsg+="Файл настроек для устройства с IMEI " +o.result.fileIMEI+" был успешно загружен.  <br/>";
                                    console.log('Файл загружен', 'Файл "' + o.result.fileOldName + '" был успешно загружен. И сохранен под именем "'+o.result.fileIMEI+'"')
                                         Ext.Msg.alert("Загрузка файлов конфигурации",configUploadMsg, function(){
                                         self.fireEvent('save',result);  
                                         self.up('window').close();    
                                         });  
//                                            Ext.Msg.alert('Файл загружен', 'Файл "' + o.result.fileOldName + '" был успешно загружен. И сохранен под именем "'+o.result.fileIMEI+'"');
                                },
                                failure: function (f, o) {
                                    configUploadMsg+=o.result.errmsg+ "<br/>";
                                    console.log('Произошла ошибка',o.result.errmsg );
                                         Ext.Msg.alert("Загрузка файлов конфигурации",configUploadMsg, function(){
                                         self.fireEvent('save',result);  
                                         self.up('window').close();    
                                         });
                                }
                            }); 
                            }
                        else {
                            self.fireEvent('save',result); 
                            self.up('window').close();
                        }
                }
         })
        }
        else {
            var accountId=self.getRecord().get("accountId");
            if(accountId==null || accountId==""){
                var cmb=self.down("[name=accountId]");
                var rec=cmb.findRecordByDisplay("Без Аккаунта");
                cmb.select(rec)
            }
            form.getFields().each(function (fim) {
                if (!fim.isValid()) {
                    try {
                        console.log("fim ", fim.getName(), " is not valid");
                        var tabpanel = fim.up('tabpanel');
                        tabpanel.setActiveTab(fim.up('form'));
                        var eqform = fim.up('eqform');
                        var maintp = eqform.up('tabpanel');
                        maintp.setActiveTab(eqform);
                        return false;
                    }
                    catch (e) {
                        console.log("e:", e);
                    }
                }
            });
            return true;
        }
    },
    onLoad:function(){
        console.log('form.onLoad');        
        var self=this; 
        equipmentData.loadData(this.getRecord().get("_id"),function(result){
            console.log('loadResult',result);
            self.loadRecord(Ext.create('Equipment',result));    
            var record=self.getRecord(); 
            console.log('record',record);
            var accField=self.down('[name="accountId]');
            accField.getStore().load();
            accField.setValue(result.accountId);
            //self.down('[name="accountName"]').setValue(result.accountName);
            self.down('[name="objectName"]').setValue(result.objectName);
            self.fireEvent('onLoad');
        });
     }
}]
        });
        this.callParent();
    }
});

Ext.define('Equipment',{
extend: 'Ext.data.Model',
    fields: [            
            '_id','accountId',//'uid','objectName',
            //Вкладка Основные сведения
            'eqOwner','eqRightToUse','eqSellDate',//'installDate',
            /*'eqStatus',*/'eqWork','eqWorkDate','eqNote',
            //Вкладка Характеристики устройства
            'eqtype','eqMark','eqModel','eqSerNum',
            'eqIMEI','eqFirmware','eqConfig','eqLogin','eqPass',
            //Вкладка SIM-карта
            'simOwner','simProvider',/*'simContract','simTariff',
            'simSetDate',*/'simNumber','simICCID','simNote',
            //Вкладка SIM-карта
            'instPlace'
            ],
    idProperty: '_id'  
}
);
