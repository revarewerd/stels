/**
 * Created by IVAN on 10.12.2014.
 */
Ext.tip.QuickTipManager.init();
Ext.define('Workflow.view.installer.CurrentWorkDescription', {
    extend: 'Ext.form.Panel',
    itemId:'ticketForm',
    alias: "widget.currentworkdesc",
    title: 'Работа',
    layout:{
        type:'hbox',
        align:'stretchmax'
    },
    defaultType: 'textfield',

    fieldDefaults: {
        margin: '5 5 5 5'
    },
    items: [
        {
            xtype: 'combobox',
            fieldLabel: 'Работа',
            store:Ext.create('Workflow.view.WorkTypeStore'),
            forceSelection:true,
            allowBlank:false,
            valueField: 'workType',
            displayField: 'workDesc',
            name: 'workType',
            labelWidth: 50,
            readOnly:true,
            flex:1,
            //value: '101',
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    var panel=textfield.up('panel')
                    if(newValue!=null) panel.addQuickTip(textfield)
                }
            }
        },
        {
            fieldLabel: 'Объект',
            name: 'objectName',
            labelWidth: 50,
            readOnly:true,
            flex:2,
            //value:'30.10.2014',
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var panel=textfield.up('panel')
                    panel.addQuickTip(textfield)
                }
            }
        },
        {
            fieldLabel: 'Установка',
            name: 'install',
            labelWidth: 70,
            readOnly:true,
            flex:2,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var record=textfield.up("#curWorkDesc").getRecord()
                    var equipment=record.get("install");
                    if(equipment!=null ) {
                        var result = equipment.eqtype;
                        if (equipment.eqMark != null && equipment.eqMark != "") {
                            result = equipment.eqMark
                            if (equipment.eqModel != null && equipment.eqModel != "")
                                result = result + " " + equipment.eqModel
                        }
                        textfield.setValue(result);
                        var panel = textfield.up('panel')
                        panel.addQuickTip(textfield)
                    }
                }
            }
        },
        {
            fieldLabel: 'Удаление',
            name: 'remove',
            labelWidth: 70,
            readOnly:true,
            flex:2,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var record=textfield.up("#curWorkDesc").getRecord()
                    var equipment=record.get("remove");
                    if(equipment!=null ) {
                        var result = equipment.eqtype;
                        if (equipment.eqMark != null) {
                            result = equipment.eqMark
                            if (equipment.eqModel != null)
                                result = result + " " + equipment.eqModel
                        }
                        textfield.setValue(result);
                        var panel = textfield.up('panel')
                        panel.addQuickTip(textfield)
                    }
                }
            }
        },
        {
            xtype: 'combobox',
            fieldLabel: 'Статус',
            store:Ext.create('Workflow.view.WorkStatusStore'),
            forceSelection:true,
            allowBlank:false,
            valueField: 'workStatus',
            displayField: 'workStatusDesc',
            name: 'workStatus',
            labelWidth: 50,
            readOnly:true,
            flex:2,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var panel=textfield.up('panel')
                    panel.addQuickTip(textfield)
                }
            }
        }
    ],
    addQuickTip:function(comp){
        console.log("add tip for comp=",comp)
        Ext.tip.QuickTipManager.register({
            target: comp.getId(), // Target button's ID
            title:  comp.getFieldLabel(),//me.fieldLabel,  // QuickTip Header
            text  : comp.getRawValue()// Tip content
        });

    }
})