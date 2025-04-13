Ext.define('Seniel.view.reports.FuelGraphReport', {
    extend:'Ext.panel.Panel',
    alias: 'widget.fuelgraphreport',
    requires: [
        'Seniel.view.DygraphPanel'
    ],
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
                var fuelGraph = btn.up('panel').down('#dygraphPanelFuel');
                
                if (fuelGraph && fuelGraph.dygraph) {
                    fuelGraph.dygraph.resetZoom();
                }
            }
        },
        ' ',
        {
            icon: 'images/ico16_zoomin.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.xzoom'),
            tooltipType: 'title',
            scale: 'small',
            pressed: true,
            enableToggle: true,
            toggleGroup: 'dygraphInteractionFuel',
            allowDepress: false,
            toggleHandler: function(btn, pressed) {
                var fuelGraph = btn.up('panel').down('#dygraphPanelFuel');
                var speedGraph = btn.up('panel').down('#dygraphPanelSpeed');
                if (pressed) {
                    if (fuelGraph && fuelGraph.dygraph) {
                        fuelGraph.dygraph.interactionMode = 'zoom';
                    }
                    if (speedGraph && speedGraph.dygraph) {
                        speedGraph.dygraph.interactionMode = 'zoom';
                    }
                }
            }
        },
        {
            icon: 'images/ico16_draw_move.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.xmoving'),
            tooltipType: 'title',
            scale: 'small',
            enableToggle: true,
            toggleGroup: 'dygraphInteractionFuel',
            allowDepress: false,
            toggleHandler: function(btn, pressed) {
                var fuelGraph = btn.up('panel').down('#dygraphPanelFuel');
                var speedGraph = btn.up('panel').down('#dygraphPanelSpeed');
                if (pressed) {
                    if (fuelGraph && fuelGraph.dygraph) {
                        fuelGraph.dygraph.interactionMode = 'pan';
                    }
                    if (speedGraph && speedGraph.dygraph) {
                        speedGraph.dygraph.interactionMode = 'pan';
                    }
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
                var url = './EDS/xychart/fuelgraph.png?uid=' + params.selected + '&from=' + encodeURIComponent(new Date(params.from)) + '&to=' + encodeURIComponent(new Date(params.to));
                window.open(url);
            }
        }
    ],
    layout: 'border',
    defaults: {
        border: false,
        header: false,
        bodyBorder: false,
        listeners: {
            boxready: function() {

            }
        }
    },
    initComponent: function () {
        this.addEvents('graphClick');
        var self = this;
        this.callParent();
    },
    axisGroup: {"fuel": "fuelLevel",
        "fuelSmoothed": "fuelLevel",
        "fuelLevelByCalcNorms":"fuelLevel",
        "fuelLevelByNorms":"fuelLevel",
        "fuelWithCutStarts": "fuelLevel"
    },
    items: [
        { // TODO move to init component?
            region: 'west',
            xtype: 'checkboxgroup',
            width: '25%',
            border: false,
            split: true,
            overflowX: 'auto',
            overflowY: 'auto',
            layout: 'vbox',
            items: [
                { boxLabel: tr('fuelgraphreport.axisLabels.fuel'), name: 'graph', inputValue: 'fuel' },
                { boxLabel: tr('fuelgraphreport.axisLabels.fuelsmoothed'), name: 'graph', inputValue: 'fuelSmoothed' },
                { boxLabel: tr('fuelgraphreport.axisLabels.speed'), name: 'graph', inputValue: 'speed', checked: true },
                //{ boxLabel: "Город/загород", name: 'graph', inputValue: 'urban'},
                //{ boxLabel: "Нормы топлива", name: 'graph', inputValue: 'fuelStandards'},
                // { boxLabel: "--", name: 'graph', inputValue: 'fuelWithCutStarts'},
                { boxLabel: tr('fuelgraphreport.axisLabels.levelbycalcnorms'), name: 'graph', inputValue: 'fuelLevelByCalcNorms' },
                { boxLabel: tr('fuelgraphreport.axisLabels.levelbynorms'), name: 'graph', inputValue: 'fuelLevelByNorms'},
                { boxLabel: tr('fuelgraphreport.axisLabels.distancedriven'), name: 'graph', inputValue: 'distance'}
                //{ boxLabel: 'Заправки/сливы', name: 'graph', inputValue: 'states'}
            ],
            getActiveGraphs: function() {
               var self = this;
                var result = [];
                Ext.Array.each(self.items.items, function(item) {
                    if(item.getValue())
                        result.push({label: item.boxLabel, graph: item.inputValue})
                });
                return result;
            },
            listeners: {
               change: function(self, newValue) {
                   var report = self.up('fuelgraphreport');
                   var graphs = newValue.graph;
                   var graphSet = {};
                   if(graphs )
                   {
                       if(!Ext.isArray(graphs))
                           graphs = [ graphs ];
                       Ext.Array.each(graphs, function(g) {
                           graphSet[g] = true;
                       });
                   }

                   var size = Object.getOwnPropertyNames(graphSet).length;
                   var axesLeft = 2; var usedAxisGroups = {};
                   Ext.Array.each(graphs, function (g) {
                       var group = report.axisGroup[g];
                       if(group && !usedAxisGroups[group]) {
                           usedAxisGroups[group] = true;
                           axesLeft -= 1;
                       }
                       else if(!group)
                           axesLeft -= 1;
                   });

                   if(axesLeft == 0) {
                       Ext.Array.each(self.items.items, function(item) {

                           if (!graphSet[item.inputValue]) {
                               var disable = true;
                               if(report.axisGroup[item.inputValue]) {
                                   Ext.Array.each(graphs, function (g) {
                                        if(report.axisGroup[g] == report.axisGroup[item.inputValue])
                                        {
                                            disable = false;
                                            return false;
                                        }
                                   });
                               }
                               if(disable) {
                                   item.disable();
                               }
                               else
                                   item.enable();
                           }
                       });
                   }
                   else {
                       Ext.Array.each(self.items.items, function(item) {
                           if(item.isDisabled())
                               item.enable();
                       });
                   }
                   // if(size < 2 || (size == 2 && graphSet.fuel && graphSet.fuelSmoothed))
                   // {
                   //     Ext.Array.each(self.items.items, function(item) {
                   //         if(item.isDisabled())
                   //          item.enable();
                   //     });
                   // }
                   // else {
                   //     Ext.Array.each(self.items.items, function(item) {
                   //         if(!graphSet[item.inputValue])
                   //             item.disable();
                   //     });
                   // }
                   var gs = self.getActiveGraphs();
                   var reportWnd = self.up('reportwnd')
                   var settings = {
                       dates: reportWnd.getWorkingDates(),
                       selected: reportWnd.getSelected()
                   };
                   self.up('fuelgraphreport').onGraphsChange(gs, settings);
                   console.log(self.items.items);
               }
            }
        },
        {
            region: 'center',
            xtype: 'dygraphpanel',
            itemId: 'dygraphPanelFuel',
//            axisLabels: ['Дата', 'Топливо'],
            axisLabels: [
                tr('fuelgraphreport.axisLabels.date'),
                tr('fuelgraphreport.axisLabels.fuel'),
                tr('fuelgraphreport.axisLabels.fuelsmoothed'),
                tr('fuelgraphreport.axisLabels.speed')
            ],
            units: {
                y: tr('fuelgraphreport.units')
            },
            //lineColors: ['#008000', '#000080'],
            height: 90,
            listeners: {
                boxready: function() {

                },
                graphClick: function(obj, x, row)
                {
                   // this.up('fuelgraphreport').onGraphClick(obj, x, row);
                    console.log("Fuel report graph click");
                    if (!this.up('reportwnd').down('pathgrid').gridViewed) {
                        this.up('reportwnd').down('tabpanel').setActiveTab('movement');
                        this.up('reportwnd').down('tabpanel').setActiveTab('fuelgraph');
                    }
                    this.up('reportwnd').down('pathgrid').selectRowInGrid(row);
                }


            }
        }
        // {
        //     region: 'south',
        //     xtype: 'dygraphpanel',
        //     itemId: 'dygraphPanelSpeed',
        //     axisLabels: [tr('fuelgraphreport.axisLabels.date'), ],
        //     lineColors: ['#000080'],
        //     height: 40,
        //     split: true
        // }
    ],
    onGraphClick: function (obj, x, row) {
        var self = this;
        console.log("graphClickHandler");
        var map = self.up('reportwnd').down('reportmap');
        var grid = self.up('reportwnd').down('movstatsgrid');
        var params = grid.getStore().getProxy().extraParams;

        mapObjects.getApproximateLonLat(params.selected, x, function(rec,resp) {
            console.log("rec = ");
            console.log(rec);
            if(rec)
                map.addPositionMarker(rec.lon,rec.lat);
        });
        // var pathGrid = this.up('reportwnd').down('pathgrid');
        // if (pathGrid) {
        //     if (!pathGrid.gridViewed) {
        //         self.up('reportwnd').down('tabpanel').setActiveTab('movement');
        //         self.up('reportwnd').down('tabpanel').setActiveTab('fuelgraph');
        //     }
        //     pathGrid.selectRowInGrid(row);
        // }
    },

    onGraphsChange: function(boxes, settings)
    {
        if(boxes.length == 0)
            return;
        var self = this;
        var labels = [
            // tr('fuelgraphreport.axisLabels.date'),
            // tr('fuelgraphreport.axisLabels.fuel'),
            // tr('fuelgraphreport.axisLabels.fuelsmoothed'),
            // tr('fuelgraphreport.axisLabels.speed'),
            // tr('fuelgraphreport.axisLabels.fuelConsumption')

            tr('fuelgraphreport.axisLabels.date')
        ];
        var graphs = [];
        var conf = new DygraphDataConfig(labels);
        var axisForGroup = {};
        var nextAxis = "y";

        for(var i = 0; i < boxes.length; ++i)
        {
            labels.push(boxes[i].label);
            graphs.push(boxes[i].graph);
            var axisGroup = self.axisGroup[boxes[i].graph];
            if(axisGroup)
            {
                if(!axisForGroup[axisGroup]) {
                    if(!nextAxis) {
                        alert("Next axis is null!");
                        throw new Error("next axis is null");
                    }
                    axisForGroup[axisGroup] = nextAxis;
                    nextAxis = (nextAxis == "y") ? "y2" : null;
                }
                conf.bindToAxis(boxes[i].label, axisForGroup[axisGroup]);
            }
            else {
                if(!nextAxis) {
                    alert("Next axis is null!");
                    throw new Error("next axis is null");
                }
                conf.bindToAxis(boxes[i].label, nextAxis);
                nextAxis = (nextAxis == "y") ? "y2" : null;
            }
            if(boxes[i].graph == "urban")
            {
                var urbanSeries = i + 1;
                conf.underlayCallback = function(canvas, area, g) {
                    canvas.fillStyle = "rgba(255, 0, 0, 0.5)";
                    function highlight_period(x_start, x_end) {
                        console.log("Highlight " + new Date(x_start).toISOString() + " " + new Date(x_end).toISOString());
                        var canvas_left_x = g.toDomXCoord(x_start);
                        var canvas_right_x = g.toDomXCoord(x_end);
                        var canvas_width = canvas_right_x - canvas_left_x;
                        canvas.fillRect(canvas_left_x, area.y, canvas_width, area.h);
                    }

                    var prevValue = 0;
                    var startRowX = null;
                    for(var j = 0; j < g.numRows() ; ++j)
                    {
                        var cv = g.getValue(j, urbanSeries);
                        if(cv != prevValue)
                        {
                            if(cv == 1)
                                startRowX = g.getValue(j,0);
                            else {
                                var regionEnd = g.getValue(j,0);
                                highlight_period(startRowX, regionEnd);
                                startRowX = null;
                            }
                        }
                        prevValue = cv;
                    }
                    if(prevValue == 1)
                        highlight_period(startRowX, g.getValue(g.numRows() -1, 0));

                }
            }
        }
        console.log(conf);

        var dygraphPanel = this.down("dygraphpanel");
        var url = "pathdata?data=fuelgraph&selected=" +
            settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString());
        for(var j = 0; j < graphs.length; ++j) {
            url += '&graph=' + graphs[j];
        }
        dygraphPanel.setDataAndOptions(url, conf);
    },
    refreshReport: function(settings) {
        var self = this;
        console.log("fuelgraph refreshReport");
        // var self = this;
        var selected = self.down('checkboxgroup');
        var boxes = selected.getActiveGraphs();
        self.onGraphsChange(boxes, settings);
    },
    listeners: {
        boxready: function() {

            // var fuelDygraph = this.down('#dygraphPanelFuel');
            // var speedDygraph = this.down('#dygraphPanelSpeed');
            //
            // if (speedDygraph && speedDygraph.dygraph && fuelDygraph && fuelDygraph.dygraph) {
            //     fuelDygraph.dygraph.connectedDygraph = speedDygraph.dygraph;
            //     speedDygraph.dygraph.connectedDygraph = fuelDygraph.dygraph;
            // }
        }
    }
});