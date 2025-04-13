Ext.define('Seniel.view.sleepers.DataWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: [
        'Seniel.view.sleepers.DataPanel',
        'Seniel.view.sleepers.MessagesGrid',
        'Seniel.view.WRWindow'
    ],
    alias: 'widget.sleeperswnd',
    stateId: 'sleeperWnd',
    stateful: true,
    icon: 'images/ico16_device_def.png',
    maximizable: false,
    height: 440,
    width: 720,
    minWidth: 720,
    layout: 'border',
    items: [{
        region: 'west',
        width: '100%',
        minWidth: 240,
        minHeight: 120,
        border: false,
        items: [
            {
                xtype: 'tabpanel',
                border: false,
                padding: false,
                listeners: {
                    afterrender: function(tabpanel) {
                        var wnd = tabpanel.up('window'),
                            sdmodel;
                        for (var i = 0; i < wnd.slprData.length; i++) {
                            sdmodel = Ext.create('Seniel.view.sleepers.DataModel', wnd.slprData[i]);
                            tabpanel.add(Ext.create('Seniel.view.sleepers.DataPanel', {
                                title: wnd.slprData[i].model,
                                slprModel: sdmodel,
                                listeners: {
                                    boxready: function(panel) {
                                        panel.down('form').loadRecord(panel.slprModel);
                                    }
                                }
                            }));
                        }
                        tabpanel.setActiveTab(0);
                    },
                    tabchange: function(tabPanel, newCard, oldCard) {
                        var store = tabPanel.up('window').down('grid').getStore(),
                            i = 0;
                        for (; i < tabPanel.items.items.length; i++) {
                            if (tabPanel.items.items[i].id === newCard.id) {
                                if (store.simNumbers && store.simNumbers.length) {
                                    store.clearFilter();
                                    store.filter([{
                                        filterFn: function(rec) {
                                            return (store.simNumbers[i].match(rec.get('senderPhone')))?(true):(false);
                                        }
                                    }]);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        ]
    }, {
        //There will be a map with sleeper events
        region: 'center',
        xtype: 'panel',
        border: false,
        hidden: true,
        autoScroll: true
    }, {
        region: 'south',
        xtype: 'panel',
        title: tr('datapanel.messages'),
        border: false,
        split: true,
        minHeight: 120,
        height: 240,
        layout: 'border',
        items: [
            {
                xtype: 'sleepergrid',
                region: 'center',
                autoScroll: true
            }
        ]
    }]

    //---------
    // Функциии
    //---------
    
});

Ext.define('Seniel.view.sleepers.DataModel', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'model',  type: 'string'},
        {name: 'type', type: 'string'},
        {name: 'state', type: 'string'},
        {name: 'serialNum', type: 'string'},
        {name: 'instDate', type: 'date'},
        {
            name: 'workDate',
            type: 'date',
            convert: function(val, rec) {
                if (!val) {
                    val = rec.get('instDate');
                }
                var wDate = new Date(val);
                return Ext.Date.format(wDate, 'd.m.Y');
            }
        }
    ]
});