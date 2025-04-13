/**
 * Created by IVAN on 17.11.2015.
 */
Ext.define('Seniel.view.reports.OneDayWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: [
        'Seniel.view.reports.PathGrid',
        'Seniel.view.WRWindow'
    ],
    alias: 'widget.onedaywnd',
    //stateId: 'repWnd',
    //stateful: true,
    minWidth: 640,
    minHeight: 480,
    layout: 'border',
    items: [
        {
            region: 'center',
            title: tr('basereport.map'),
            xtype: 'reportmap',
            split: true,
            minHeight: 256
        },
        {

            region: 'south',
            title: tr('basereport.data'),
            header: {height: 0},
            width: 200,
            height: 180,
            minHeight: 120,
            //hidden: true,
            collapsible: true,
            titleCollapse: true,
            floatable: false,
            split: true,
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [

            ]
        }
    ],
    initComponent: function() {
        this.callParent(arguments);

        this.on('boxready', function(wnd) {
            var viewport = wnd.up('viewport'),
                //cb = wnd.down('combobox'),
                rm = wnd.down('reportmap');

            //cb.getStore().add(viewport.down('mapobjectlist').roData);
            //cb.select(cb.getStore().getAt(0));

            var curMap = Ext.util.Cookies.get('currentMapLayer');
            if (!curMap) {
                curMap = 'openstreetmaps';
            }
            rm.map.events.un({'changelayer': rm.onChangeLayer});
            rm.map.setBaseLayer(rm.map.getLayersBy('codeName', curMap)[0]);

            var mainPanel = wnd.down('panel[title="'+tr('basereport.data')+'"]');
            if (wnd.repConfig && wnd.repConfig.dataPanels) {
                var widgets = wnd.repConfig.dataPanels;
                for (var i = 0; i < widgets.length; i++) {
                    widgets[i] = Ext.create(widgets[i].type, {
                        itemId: widgets[i].itemId,
                        title: widgets[i].title,
                        repCodeName: widgets[i].repCodeName
                    });
                }
            } else {
                var widgets = [
                    {
                        type: 'Seniel.view.reports.MovementStats',
                        title: tr('basereport.statistics'),
                        itemId: 'movstats',
                        repCodeName: 'stat'
                    }
                ];
            }

            if (widgets.length > 1) {
                mainPanel.add(Ext.create('Ext.tab.Panel', {
                    flex: 1,
                    floatable: true,
                    title: tr('basereport.reportdata'),
                    itemId: 'repdataPanel',
                    header: false,
                    items: widgets
                }));
            } else {
                widgets[0].region = 'center';
                mainPanel.add(Ext.create('Ext.panel.Panel', {
                    flex: 1,
                    floatable: true,
                    title:  tr('basereport.reportdata'),
                    itemId: 'repdataPanel',
                    header: false,
                    layout: 'border',
                    items: [
                        widgets[0]
                    ]
                }));
            }

            var tr2 = tr('basereport.data');
            var dataPanel = wnd.down('panel[title="'+ tr2+'"]'),
                rdPanel = dataPanel.getComponent('repdataPanel'),
                rdItems = [];
            var confPanels = wnd.repConfig.dataPanels;
            var settings = wnd.repConfig.settings;
            console.log("settings",settings)
            for (var i = 0; i < confPanels.length; i++) {
                rdItems.push(rdPanel.getComponent(confPanels[i].itemId));
            }
            var    reports = [wnd.down('seniel-mapwidget')].concat(rdItems);
            for (var i = 0; i < reports.length; i++) {
                reports[i].refreshReport(settings);
            }
        });
    }
});