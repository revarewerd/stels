/**
 * Created by IVAN on 04.12.2014.
 */
Ext.define('Workflow.view.installer.CurrentWorkForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.currentworkform',
    layout: {type: 'vbox',
        align: 'stretch'},
    fieldDefaults: {
        labelWidth: 150,
        margin: '0 20 5 20'
    },
    defaultType: 'textfield',
    items: [
        {
            xtype:'tabpanel',
            items:[
                {
                    //itemId:"curObjectForm",
                    name:"curObjectForm",
                    title:'Объект',
                    xtype: 'form',
                    layout: {
                        type:'vbox',
                        align:'stretch'
                    },
                    defaultType:'textfield',
                    fieldDefaults:{
                        margin: '0 20 5 20'
                    },
                    defaults:{
                        readOnly:true
                    },
                    items: [
                        {
                            margin: '10 20 5 20',
                            fieldLabel: 'Наименование',
                            name: 'name',
                            allowBlank: false,
                            regex: /^[\S]+.*[\S]+.*[\S]+$/i
                        },
                        {
                            fieldLabel: 'Тип',
                            name: 'type'
                        },
                        {
                            fieldLabel: 'Марка',
                            name: 'marka'
                        },
                        {
                            fieldLabel: 'Модель',
                            name: 'model'
                        },
                        {
                            fieldLabel: 'Госномер',
                            name: 'gosnumber'
                        },
                        {
                            fieldLabel: 'VIN',
                            name: 'VIN'
                        },
                        {
                            xtype: 'fieldcontainer',
                            margin: '0 20 0 0',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'checkbox',
                                    labelWidth: 180,
                                    fieldLabel: 'Блокировка бензонасоса',
                                    name: 'fuelPumpLock',
                                    readOnly: false
                                },
                                {
                                    xtype: 'checkbox',
                                    margin: '0 0 0 20',
                                    labelWidth: 150,
                                    fieldLabel: 'Блокировка зажигания',
                                    name: 'ignitionLock',
                                    readOnly: false
                                }
                            ]
                        }
                    ]

                },
                {
                title:'Характеристики устройства',
                itemId:"eqCharForm",
                xtype: 'form',
                layout: {
                    type:'vbox',
                    align:'stretch'
                },
                defaultType:'textfield',
                fieldDefaults:{
                    margin: '0 20 5 20'
                },
                defaults:{
                    readOnly:false
                },
                items: [
                    {
                        margin: '20 20 5 20',
                        xtype: 'combobox',
                        fieldLabel: 'Тип',
                        name: 'eqtype',
                        allowBlank: false,
                        forceSelection:true,
                        queryMode: 'local',
                        store: [
                            ['Основной абонентский терминал', 'Основной абонентский терминал'],
                            ['Дополнительный абонентский терминал', 'Дополнительный абонентский терминал'],
                            ['Спящий блок автономного типа GSM', 'Спящий блок автономного типа GSM'],
                            ['Спящий блок на постоянном питании типа Впайка', 'Спящий блок на постоянном питании типа Впайка'],
                            ['Радиозакладка', 'Радиозакладка'],
                            ['Датчик уровня топлива', 'Датчик уровня топлива'],
                            ['Виртуальный терминал', 'Виртуальный терминал']
                        ],
                        listeners:{
                            change:function( cmb, newValue, oldValue, eOpts ){
                                //var panel=cmb.up('[itemId=eqPanel]');
                                //if(!panel)
                                    var form=cmb.up('currentworkform')
                                    var rec=form.getRecord();
                                //else
                                //    rec=panel.getRecord();
                                var eqMark=form.down('[name=eqMark]');
                                var eqMarkStore=eqMark.getStore() ;
                                //self.down('[name=eqMark]').setValue("")
                                var eqtype=cmb.getValue();
                                if(eqtype!= null) {
                                    if (eqtype.match('абонентский')) eqtype = 'Абонентский терминал';
                                    equipmentTypesData.loadMarkByType(eqtype, function (result) {
                                        console.log('result', result);
                                        eqMarkStore.loadData(result);
                                        eqMark.setValue(rec.get("eqMark"));
                                        eqMark.fireEvent('change', eqMark);
                                    })
                                }
                            }
                        },
                        valueField: 'name',
                        displayField: 'name',
                        typeAhead: true
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
                                //var panel=cmb.up('[itemId=eqPanel]');
                                //if(!panel)
                                    var form=cmb.up('currentworkform')
                                    var rec=form.getRecord();
                                //else
                                //    rec=panel.getRecord();
                                var eqModelStore=form.down('[name=eqModel]').getStore();
                                //self.down('[name=eqModel]').setValue("")
                                var eqtype=form.down('[name=eqtype]').getValue();
                                if(eqtype!= null) {
                                    if (eqtype.match('абонентский')) eqtype = 'Абонентский терминал';
                                    equipmentTypesData.loadModelByMark(eqtype, cmb.getValue(), function (result) {
                                        console.log('result', result);
                                        eqModelStore.loadData(result);
                                        console.log('rec.get("eqModel")', rec.get("eqModel"));
                                        form.down('[name=eqModel]').setValue(rec.get("eqModel"));
                                    })
                                }
                            }
                        }
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
                        })
                    },
                    {
                        fieldLabel: 'Серийный номер',
                        name:'eqSerNum'
                    },
                    {
                        fieldLabel: 'IMEI',
                        name:'eqIMEI',
                        vtype:'alphanum'
                    },
                    {
                        fieldLabel: 'Прошивка',
                        name:'eqFirmware'
                    },
                    {
                        fieldLabel: 'Конфигурация',
                        name:'eqConfig'
                    },
                    {
                        fieldLabel: 'Логин',
                        name:'eqLogin'
                    },
                    {   margin: '0 20 20 20',
                        fieldLabel: 'Пароль',
                        name:'eqPass'
                    }
                ]
            },
                {title: 'SIM-карта',
                    itemId:'simForm',
                    xtype: 'form',
                    hidden:this.hideRule,
                    layout: {
                        type:'vbox',
                        align:'stretch'
                    },
                    defaultType:'textfield',
                    defaults:{
                        readOnly:false
                    },
                    fieldDefaults:{
                        margin: '0 20 5 20'
                    },
                    items: [
                        {   margin: '20 20 5 20',
                            fieldLabel: 'Собственник',
                            name:'simOwner'
                        },
                        {
                            fieldLabel: 'Оператор',
                            name:'simProvider'
                        },
                        {
                            fieldLabel: 'Абонентский номер',
                            name:'simNumber'
                        },
                        {
                            fieldLabel: 'ICCID',
                            name:'simICCID'
                        },
                        {
                            margin: '0 20 20 20',
                            xtype: 'textareafield',
                            fieldLabel: 'Примечание',
                            rows:2,
                            name: 'simNote'
                        }
                    ]
                },
                {title: 'Место установки',
                    itemId:"instPlaceForm",
                    xtype: 'form',
                    layout: {
                        type:'vbox',
                        align:'stretch'
                    },
                    defaults:{
                        readOnly:false
                    },
                    defaultType:'textfield',
                    fieldDefaults:{
                        margin: '0 20 5 20'
                    },
                    items: [
                        {
                            margin: '20 20 5 20',
                            fieldLabel: 'Место установки',
                            name: 'instPlace'
                        }
                    ]
                }]
        }
    ],
    setReadOnly: function(readOnly){
        var self=this
        var curObjectLockForm=self.down("[name=curObjectForm]").down('fieldcontainer')
        var instPlaceForm=self.down("#instPlaceForm")
        var simForm = self.down("#simForm")
        var eqCharForm = self.down("#eqCharForm")
        var forms=[curObjectLockForm,instPlaceForm,simForm,eqCharForm]
        for(i in forms){
            var items=forms[i].items.items
            for(j in items ){
                items[j].setReadOnly(readOnly)
            }
        }
    }
})

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