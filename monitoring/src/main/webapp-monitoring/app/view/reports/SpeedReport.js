Ext.define('Seniel.view.reports.SpeedReport', {
    extend: 'Seniel.view.DygraphPanel',
    alias: 'widget.speedreport',
    lbar: [
        {
            icon: 'images/ico16_zoomout.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.returnscale'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var speedGraph = btn.up('speedreport');
                if (speedGraph && speedGraph.dygraph) {
                    speedGraph.dygraph.resetZoom();
                }
            }
        },
        {
            icon: 'images/ico16_printer.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('basereport.export.chart'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var grid = btn.up('window').down('movstatsgrid');
                var params = grid.getStore().getProxy().extraParams;
                var url = './EDS/xychart/speed.png?uid=' + params.selected + '&from=' + encodeURIComponent(new Date(params.from)) + '&to=' + encodeURIComponent(new Date(params.to));
                window.open(url);
            }
        }
    ],
    axisLabels: [tr('fuelgraphreport.axisLabels.date'), tr('fuelgraphreport.axisLabels.speed')],
    units: {
        y: tr('mapobject.laststate.kmsperhour')
    },
    lineColors: ['#008080'],
    
    //Рисовалка отчета по скорости
    refreshReport: function (settings) {
        console.log("speed refreshReport");
        var reportWnd = this.up('reportwnd');
        this.setData("pathdata?data=speedgraph&selected=" +
            settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString()));
    },
    initComponent: function() {
        this.addEvents('graphClick');

        this.on('graphClick', function(obj, x, row) {
            console.log("Speed report graph click");
            if (!this.up('reportwnd').down('pathgrid').gridViewed) {
                this.up('reportwnd').down('tabpanel').setActiveTab('movement');
                this.up('reportwnd').down('tabpanel').setActiveTab('speed');
            } 
            this.up('reportwnd').down('pathgrid').selectRowInGrid(row);
        });

        this.callParent();
    }//,
//    listeners: {
//        'graphClick': function (obj, x, row) {
//            this.up('reportwnd').down('pathgrid').selectRowInGrid(row);
//        }
//    }
});