Ext.define('Seniel.view.sleepers.DataPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.sleeperpanel',
    layout: 'fit',
    border: false,
    items: [
        {
            xtype: 'panel',
            border: false,
            items: [
                {
                    xtype: 'form',
                    defaultType: 'displayfield',
                    defaults: {
                        anchor: '100%',
                        labelWidth: 160
                    },
                    layout: 'anchor',
                    border: false,
                    padding: 8,
                    items :[
                        {
                            fieldLabel: tr('datapanel.form.type'),
                            name: 'type'
                        },
                        {
                            fieldLabel: tr('datapanel.form.serial'),
                            name: 'serialNum'
                        },
                        {
                            fieldLabel: tr('datapanel.form.workdate'),
                            name: 'workDate'
                        }
                    ]
                }
            ]
        }
    ]
});