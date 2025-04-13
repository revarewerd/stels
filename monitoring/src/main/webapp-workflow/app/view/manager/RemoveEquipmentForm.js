/**
 * Created by IVAN on 25.02.2015.
 */
Ext.define('Workflow.view.manager.RemoveEquipmentForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.manremeqform',
    itemId: "manRemEqForm",
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    defaultType: 'textfield',
    defaults: {
        margin: '0 20 5 20'
    },
    items: [
        {
            margin: '20 20 5 20',
            xtype: 'combobox',
            fieldLabel: 'Выберите устройство',
            itemId: "existEq",
            allowBlank: false,
            //disabled: true,
            forceSelection: true,
            store: Ext.create('EDS.Ext5.store.ObjectsEquipmentService', {
                //autoLoad: true,
                autoSync: false,
                sorters: [{
                    sorterFn: function (o1, o2) {
                        var getRank = function (o) {
                                var name = o.get('eqtype');
                                if (name === 'Основной абонентский терминал') {
                                    return 1;
                                } else {
                                    return 2;
                                }
                            },
                            rank1 = getRank(o1),
                            rank2 = getRank(o2);
                        if (rank1 === rank2) {
                            return 0;
                        }

                        return rank1 < rank2 ? -1 : 1;
                    }
                }],
                listeners: {
                    load: function (store, records, successful, eOpts) {
                        console.log("store load")
                        for (i in records) {
                            var data = records[i].get("eqtype") + " - " + records[i].get("eqMark") + " - " +
                                records[i].get("eqModel") + " - IMEI:" + records[i].get("eqIMEI");
                            records[i].set("data", data)
                        }
                    }
                }
            }),
            valueField: 'eqIMEI',
            displayField: 'data',
            listeners: {
                select: function (combo, record, eOpts) {
                    var rec = record[0]
                    combo.up("#manRemEqForm").loadRecord(rec);
                }
            }
        },
        {
            fieldLabel: 'Тип устройства',
            name: 'eqtype',
            readOnly: true
        },
        {
            fieldLabel: 'Марка',
            name: 'eqMark',
            readOnly: true
        },
        {
            fieldLabel: 'Модель',
            name: 'eqModel',
            readOnly: true
        }
    ],
    getData:function(){
        var form=this;
        if(form.isValid()){
            form.updateRecord();
            var rec=form.getRecord();
            //var eqIMEI=form.down("#existEq").getValue();
            //rec.set("eqIMEI",eqIMEI);
            form.reset();
            var data=rec.getData();
            //delete data._id
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