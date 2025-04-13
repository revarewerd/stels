/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
Ext.define('Billing.view.equipment.EquipmentTypesForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.eqtypesform',
    requires: [
        'Ext.form.field.Text',
        'Ext.grid.plugin.CellEditing'        
        ],
    header: false,
    initComponent: function () {
        this.addEvents('create');        
        var self = this;

        Ext.apply(this, {
            activeRecord: null,
            itemId:'eqtypes',
            layout: {
                type: 'accordion',
                multi: true,
                titleCollapse: true,
                hideCollapseTool: true
            },
            items: [{                            
                xtype: 'form',
                title: '<font color="1a4780"><b>Основное</b></font>',
                bodyPadding: '10px',
                autoScroll: true,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                defaultType: 'textfield',
                defaults: {
                    margin: '0 10 5 0' 
                    ///labelAlign: 'right',                    
                    //labelWidth: 135
                },
                 fieldDefaults: {
                        //margin: '0 20 5 20',
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth:135                       
                },
                items: [
                {                   
                    xtype:'combobox',
                    fieldLabel: 'Тип устройства',  
                    forceSelection:false,
                    queryMode: 'local',
                    store: [
                                ['Абонентский терминал', 'Абонентский терминал'],
//                                ['Дополнительный абонентский терминал', 'Дополнительный абонентский терминал'],                                                
                                ['Спящий блок автономного типа GSM', 'Спящий блок автономного типа GSM'],
                                ['Спящий блок на постоянном питании типа Впайка', 'Спящий блок на постоянном питании типа Впайка'],
                                ['Радиозакладка', 'Радиозакладка'],
                                ['Датчик уровня топлива', 'Датчик уровня топлива']
                    ],    
                    name: 'type',
                    allowBlank: false
                },
                 {                    
                    fieldLabel: 'Марка',                    
                    name: 'mark',
                    allowBlank: false
                },
                 {                    
                    fieldLabel: 'Модель',
                    name: 'model'
                    //allowBlank: false
                },
                 {                    
                    fieldLabel: 'Сервер',                    
                    name: 'server'
                    //allowBlank: false
                }, {
                    fieldLabel: 'Порт',                    
                    name: 'port'
                    //allowBlank: false
                },
                {
                    margin: '0 10 10 0',      
                    labelAlign: 'right',
                    labelWidth:135 ,
                    xtype: 'textareafield',
                    rows:3,
                    fieldLabel: 'Примечание',
                    name: 'objnote'
                }]
            },
            {
                xtype: 'grid',
                title: '<font color="1a4780"><b>Параметры датчиков</b></font>',
                collapsed: false,  
                autoScroll: true,
                multiSelect: true,
                plugins: [
                            Ext.create('Ext.grid.plugin.CellEditing', {
                                clicksToEdit: 1
                            })
                        ],
                store: Ext.create('Ext.data.Store', {
                    fields: ['name','paramName','unit','minValue','maxValue','ratio','comment'],
                    //data: {'paramName': 'Lisa',  "paramId":"lisa@simpsons.com"} ,
                    autoSync: true,
                    autoLoad: true
                    //proxy: {
                    //    type: 'memory',
                    //    reader: {
                    //        type: 'json'
                    //    }
                    //}
                }),
//                model:'SensorParams', 
//                data:{'items':[ {'paramName': 'Lisa',  "paramId":"lisa@simpsons.com"} ]},
//                proxy: {
//                    type: 'memory',
//                    reader: {
//                        type: 'json',
//                        root: 'items'
//                    }                            
//                },
                columns: [                
                {
                    header: '№',
                    xtype: 'rownumberer',
                    width:40                    
                },
                {
                    header: 'Параметр',
                    //width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'paramName',
                    editor: {
                                xtype: 'textfield',
                                allowBlank: false
                            }
                },
                {
                    header: 'Название',
                    //width:170,
                    flex: 2,
                    sortable: true,
                    dataIndex: 'name',
                    editor: {
                                xtype: 'textfield',
                                allowBlank: false
                            }
                },
                    {
                        dataIndex: 'unit',
                        flex: 1,
                        header:'Ед. измерения',// tr('settingssensors.grid.unit')
                        editor: {
                            xtype: 'textfield',
                            allowBlank: false
                        }
                    },
                    {
                        dataIndex: 'minValue',
                        flex: 1,
                        header:'Ниж. граница',//tr('settingssensors.grid.lowbound')
                        editor: {
                            xtype: 'textfield',
                            allowBlank: false
                        }
                    },
                    {
                        dataIndex: 'maxValue',
                        flex: 1,
                        header:'Верх. граница',// tr('settingssensors.grid.upbound')
                        editor: {
                            xtype: 'textfield',
                            allowBlank: false
                        }
                    },
                    {
                        dataIndex: 'ratio',
                        header:'Коэффициент', //tr('settingssensors.grid.ratio'),
                        flex: 1,
                        editor: {
                            xtype: 'textfield',
                            allowBlank: false
                        }
                    },
                    {
                        dataIndex: 'comment',
                        header:'Комментарий',//tr('settingssensors.grid.comment')
                        flex: 1,
                        editor: {
                            xtype: 'textfield',
                            allowBlank: false
                        }
                    }
                ],
                dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'right',
                    align: 'left',
                    items: [
                        {
                            xtype: 'button',
                            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                            tooltip: 'Добавить устройство',
                            handler:function(){
                                console.log('add new eqtype')
                                self.down('grid').getStore().add( {'paramName': ' ',  "paramId":" ","ratio":1})
                            }
                            },
                        {
                            xtype: 'button',
                            tooltip: 'Удалить устройство',
                            icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                            handler:function(){
                                console.log('remove selected eqtypes')
                                self.down('grid').getStore().remove( self.down('grid').getSelectionModel( ).getSelection())
                            }
                        }]
                }]
            }
            ],
        listeners: {
                afterrender: function (form, eopts) {
                    console.log("Form rendered ", form);
                    var record=form.getRecord();
                    if(record)
                    {console.log("Form record", record)
                    if(record.get('_id')) form.onLoad();}
                }
            }
        })
        this.callParent();
    },
    onSave: function () {
        console.log("form.onSave")
        var self=this        
         this.getForm().updateRecord();         
         var data=this.getForm().getRecord().getData()
         var gridstore=this.down('grid').getStore()
         console.log('gridstore',gridstore);  
         data["sensorparams"]=Ext.pluck(gridstore.data.items, 'data');
//         data["sensorparams"]=new Array(); 
//         gridstore.each(function(item){
//             console.log('item',item) 
//             data["sensorparams"].push(item.getData( ))
//         })
         //data["sensorparams"]=gridstore.getRange(0,gridstore.getTotalCount())
         console.log("data",data)
         equipmentTypesData.updateData(data, function(result, e){             
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
                    console.log("Результат запроса", result, " e=", e);
                        self.fireEvent('save');
                        self.up('window').close();                    
                }
         })
    },  
    onLoad:function(){
        console.log('form.onLoad');
        var self=this        ;
        equipmentTypesData.loadData(this.getRecord().get("_id"),function(result){
            console.log('loadResult',result);
            var record=self.getRecord();
            self.loadRecord(Ext.create('EqType',result));
            //self.getForm().updateRecord(result)
            console.log('current record',record);
            //record.set('objnote',result['objnote'])
            var gridstore=self.down('grid').getStore().setProxy(Ext.create('Ext.data.proxy.Memory',
                {
                    reader: {
                        type: 'json'
                    },
                    data:  result["sensorparams"]
                })
            );
            self.down('grid').getStore().load();
            for (var i in result["sensorparams"]) {
                console.log('sensor',result["sensorparams"][i]);
            }
        })
    }
});
Ext.define('Billing.view.equipment.EquipmentTypesForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.eqtypeswindow',
    title: 'Объект',
    width: 800,
    height: 650,
    //constrainHeader: true,
    maximizable: true,
    layout: 'fit',
    items: [       
                {xtype: 'eqtypesform'}//,
                //{xtype: 'eqpanel'}
    ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {text: 'История',
                         handler: function () {
                            var self=this; 
                            console.log("self",self)
                            var id=this.up('window').down('form').getRecord().get("_id");
                            var existingWindow = WRExtUtils.createOrFocus('EquipmentTypesEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                                aggregateId:id,
                                aggregateType:"EquipmentTypesAggregate"
                            });
                            existingWindow.show();
                            console.log("existingWindow",existingWindow)}
                        },
                        '->', {
                        icon: 'images/ico16_okcrc.png',
                        itemId: 'save',
                        text: 'Сохранить',
                        //scope:this,
                        handler: function () {
                            this.up('window').down('form').onSave()
                        }
                    }, {
                        icon: 'images/ico16_cancel.png',
                        text: 'Отменить',
                        //scope:this,
                        handler: function () {
                            this.up('window').close()
                            //this.up('form').onLoad()
                        }
                    }]
                }
            ]    
})
Ext.define('EqType', {
        extend: 'Ext.data.Model',
        fields: [
            '_id',
            //Вкладка Параметры объекта
            'type','mark','model','server','port','objnote'
        ],
        idProperty: '_id'
    }
); 
