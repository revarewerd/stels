/**
 * Created by IVAN on 27.11.2014.
 */
Ext.define('Workflow.view.installer.InstallerAccountForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.instaccform',
    layout: {type: 'vbox',
        align: 'stretch'},
    fieldDefaults: {
        labelWidth: 150,
        margin: '0 20 5 20'
    },
    defaults:{
        readOnly:true
    },
    defaultType: 'textfield',
    items: [
        {   margin: '10 20 5 20',
            fieldLabel: 'Фамилия',
            vtype: 'rualpha',
            name: 'cffam'
        },
        {
            fieldLabel: 'Имя',
            //vtype:'allalpha',
            name: 'cfname'
        },
        {
            fieldLabel: 'Отчество',
            //vtype:'allalpha',
            name: 'cffathername'
        },
        {
            vtype: 'phone',
            fieldLabel: 'Мобильный телефон 1',
            name: 'cfmobphone1'},
        {
            vtype: 'phone',
            fieldLabel: 'Мобильный телефон 2',
            name: 'cfmobphone2'},
        {
            vtype: 'phone',
            fieldLabel: 'Рабочий телефон',
            name: 'cfworkphone1'},
        {
            fieldLabel: 'e-mail',
            name: 'cfemail',
            vtype: 'email' },
        {
            xtype: 'textareafield',
            fieldLabel: 'Примечание',
            name: 'cfnote'}
    ],
    loadData:function(accountId){
        var form=this;
        accountData.loadData(accountId,function(data,e){
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                console.log('Учетная запись - ',data);
                var rec = Ext.create('Account',data)
                form.loadRecord(rec)
            }
        })
    }
})