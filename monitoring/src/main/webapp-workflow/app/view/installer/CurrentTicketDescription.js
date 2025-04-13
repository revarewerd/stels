/**
 * Created by IVAN on 02.12.2014.
 */
Ext.tip.QuickTipManager.init();
Ext.define('Workflow.view.installer.CurrentTicketDescription', {
    extend: 'Ext.form.Panel',
    itemId:'ticketForm',
    alias: "widget.currentticketdesc",
    title: 'Параметры заявки',
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
            fieldLabel: '№',
            name: '_id',
            labelWidth: 20,
            readOnly:true,
            flex:1,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var panel=textfield.up('panel')
                    panel.addQuickTip(textfield)
                }
            }
        },
        {
            fieldLabel: 'Дата',
            name: 'openDate',
            xtype:'datefield',
            format:'d.m.Y',
            altFormats:'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j',
            labelWidth: 40,
            readOnly:true,
            flex:2,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var panel=textfield.up('panel')
                    panel.addQuickTip(textfield)
                }
            }
        },
        {
            fieldLabel: 'Аккаунт',
            name: 'accountName',
            labelWidth: 50,
            readOnly:true,
            flex:3,
            listeners: {
                change:function( textfield, newValue, oldValue, eOpts ){
                    if(!newValue) return
                    var panel=textfield.up('panel')
                    panel.addQuickTip(textfield)
                }
            }
        },
        {
            fieldLabel: 'Число работ',
            name: 'worksCount',
            labelWidth: 80,
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