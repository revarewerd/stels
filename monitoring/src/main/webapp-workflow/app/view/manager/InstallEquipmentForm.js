/**
 * Created by IVAN on 25.02.2015.
 */
Ext.define('Workflow.view.manager.InstallEquipmentForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.maninsteqform',
    itemId: "manInstEqForm",
    layout: {
        type: 'vbox',
        align:'stretch'
    },
    defaultType:'textfield',
    defaults:{
        margin: '0 20 5 20'
    },
    items: [
        {
            margin: '20 20 5 20',
            xtype: 'combobox',
            fieldLabel:'Тип устройства',
            allowBlank:false,
            //itemId: 'eqType',
            name:'eqtype',
            //regex:/^\d+$/i,
            validateBlank: true,
            readOnly: false,
            store: [
                ['Основной абонентский терминал', 'Основной абонентский терминал'],
                ['Дополнительный абонентский терминал', 'Дополнительный абонентский терминал'],
                ['Спящий блок автономного типа GSM', 'Спящий блок автономного типа GSM'],
                ['Спящий блок на постоянном питании типа Впайка', 'Спящий блок на постоянном питании типа Впайка'],
                ['Радиозакладка', 'Радиозакладка'],
                ['Датчик уровня топлива', 'Датчик уровня топлива'],
                ['Виртуальный терминал', 'Виртуальный терминал']
            ],
            valueField: 'eqtype',
            displayField: 'eqtype',
            listeners:{
                change:function( cmb, newValue, oldValue, eOpts ){
                    var panel=cmb.up('#manInstEqForm');
                    //var rec=panel.getRecord();
                    var eqMark=panel.down('[name=eqMark]');
                    var eqMarkStore=eqMark.getStore() ;
                    //self.down('[name=eqMark]').setValue("")
                    var eqtype=cmb.getValue();
                    if(eqtype!=null && eqtype.match('абонентский')) eqtype='Абонентский терминал';
                    equipmentTypesData.loadMarkByType(eqtype,function(result){
                        console.log('result',result);
                        eqMarkStore.loadData(result);
                        //eqMark.setValue(rec.get("eqMark"));
                        eqMark.fireEvent('change',eqMark);
                    })
                }
            },
            typeAhead: true,
            //forceSelection:true,
            queryMode: 'local'
        },
        {
            //margin: '0 0 0 40',
            xtype: 'combobox',
            fieldLabel:'Марка',
            //itemId: "eqMark",
            name:'eqMark',
            valueField: 'eqMark',
            displayField: 'eqMark',
            //regex:/^\d+$/i,
            validateBlank: true,
            readOnly: false,
            store:Ext.create('Ext.data.Store', {
                fields:['eqMark']
            }),
            typeAhead: true,
            //forceSelection:true,
            queryMode: 'local',
            listeners:{
                change:function( cmb, newValue, oldValue, eOpts ){
                    var panel=cmb.up('#manInstEqForm');
                    //rec=panel.getRecord();
                    var eqModelStore=panel.down('[name=eqModel]').getStore();
                    //self.down('[name=eqModel]').setValue("")
                    var eqtype=panel.down('[name=eqtype]').getValue();
                    if(eqtype!= null && eqtype.match('абонентский')) eqtype='Абонентский терминал';
                    equipmentTypesData.loadModelByMark(eqtype,cmb.getValue(),function(result){
                        console.log('result',result);
                        eqModelStore.loadData(result);
                        //console.log('rec.get("eqModel")',rec.get("eqModel"));
                        //self.down('#eqMode]').setValue(rec.get("eqModel"));
                    })
                }
            }
        },
        {
            //margin: '0 0 0 40',
            xtype: 'combobox',
            fieldLabel:'Модель',
            //itemId: "eqModel",
            name:'eqModel',
            valueField: 'eqModel',
            displayField: 'eqModel',
            //regex:/^\d+$/i,
            validateBlank: true,
            readOnly: false,
            typeAhead: true,
            //forceSelection:true,
            queryMode: 'local',
            store:Ext.create('Ext.data.Store', {
                fields:['eqModel']
            })
        }
    ],
    getData:function(){
        var form=this;
        if(form.isValid()){
            form.updateRecord();
            var rec=form.getRecord();
            form.reset();
            var data=rec.getData();
            delete data._id
            return data
        }
        else  {
            Ext.MessageBox.show({
                title: 'Произошла ошибка',
                msg: "Заполните все необходимые поля корректными данными",
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
    }
})