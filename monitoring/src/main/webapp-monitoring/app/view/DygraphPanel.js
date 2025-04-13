Ext.define('Seniel.view.DygraphPanel', {
    extend: 'Ext.panel.Panel',
    title: 'DygraphPanel widget',
    alias: 'widget.dygraphpanel',
    dygraph: null,
    layout: 'fit',
    innerPanel: null,
    dygraphData: null,
    bodyBorder: false,
    border: false,

    initComponent: function() {
       // this.addEvents('graphClick');
        var self = this;

        this.items = [
            this.innerPanel = new Ext.panel.Panel({
                flex: 1
            })
        ];

        this.on('resize',function() {
            console.log("resize dygraph");
            if (self.dygraph)
            {
                if(!self.dygraph.rawData_)
                    self.dygraph.rawData_ = [];
                self.dygraph.resize();
            }
        });

        self.initConfig = {
            file: [],
            dateWindow: null,
            valueRange: null,
            labels: [],
            series: {}
        };
        
        if(self.axisLabels)
            self.initConfig.labels = self.axisLabels;

        Seniel.view.DygraphPanel.superclass.initComponent.apply(this);
    },

    afterRender: function() {
        Seniel.view.DygraphPanel.superclass.afterRender.call(this);
        var self = this;
        try {
            var blockRedraw = false;
            console.log("Dygraphpanel afterrender");
            this.dygraph = new Dygraph(
                this.innerPanel.body.dom,
                self.initConfig.file,
                {
                    rollPeriod: 1,
                    height: (self.up('tabpanel').getHeight()-36),
                    width: self.getWidth(),
                    labels: self.initConfig.labels,
                    labelsDivWidth: 270,
                    colors: self.lineColors,
                    connectSeparatedPoints: true,
                    xAxisLabelWidth: 80,
                    axes: {
                        x: {
                            valueFormatter: function(val) {
                                return Ext.Date.format(new Date(val), tr('format.extjs.datetime'));
                            },
                            axisLabelFormatter: function(d, gran) {
                                var res;
                                if (self.dygraph) {
                                    var range = self.dygraph.xAxisRange();
                                    var hours = (range[1] - range[0]) / (60 * 60 * 1000);
                                    if (hours > 48) {
                                        res = Ext.Date.format(new Date(d), 'd.m');
                                    } else if (hours > 1) {
                                        res = Ext.Date.format(new Date(d), tr('format.extjs.time'));
                                    } else {
                                        res = Ext.Date.format(new Date(d), 'H:i:s');
                                    }
                                } else {
                                    res = d;
                                }
                                return res;
                            }
                        },
                        y1: {
                            valueFormatter: function(val) {
                                var unit = '';
                                if (self.units && self.units.y)
                                    unit = ' ' + self.units.y;
                                
                                return val + unit;
                            }
                        },
                        y2: {
                            valueFormatter: function(val) {
                                var unit = '';
                                if (self.units && self.units.y2)
                                    unit = ' ' + self.units.y2;

                                return val + unit;
                            }
                        }
                    },
                    series: self.initConfig.series,
                    

                    clickCallback: function(e, x, pts) {
                        console.log('Click ' + self.dygraph.lastRow_ + ' ' + self.dygraph.getSelection());
                        console.log('x = ' + x);
                        self.fireEvent('graphClick', self, x, self.dygraph.getSelection());
                    },

                    pointClickCallback: function(e, p) {
                        console.log('Point Click' + p.name + ': ' + p.x);
                    },
                    
                    drawCallback: function(d, isInitial) {
                        if (blockRedraw || isInitial) return;
                        blockRedraw = true;
                        if (d.connectedDygraph) {
                            var xrange = d.xAxisRange();
//                            var yrange = d.yAxisRange();
                            d.connectedDygraph.updateOptions({
                                dateWindow: xrange
                            });
                        }
                        blockRedraw = false;
                    },
                            
                    interactionModel: {
                        mousedown: function(event, g, context) {
                            context.initializeMouseDown(event, g, context);
                            if (self.dygraph.interactionMode === 'zoom') {
                                Dygraph.startZoom(event, g, context);
                            } else if (self.dygraph.interactionMode === 'pan') {
                                Dygraph.startPan(event, g, context);
                            }
                        },
                        mousemove: function(event, g, context) {
                            if (context.isPanning) {
                                Dygraph.movePan(event, g, context);
                            } else if (context.isZooming) {
                                Dygraph.moveZoom(event, g, context);
                            }
                        },
                        mouseup: function(event, g, context) {
                            if (context.isPanning) {
                                Dygraph.endPan(event, g, context);
                            } else if (context.isZooming) {
                                Dygraph.endZoom(event, g, context);
                            }
                        },
                        dblclick: function(event, g, context) {
                            Dygraph.defaultInteractionModel.dblclick(event, g, context);
                        }
                    }
                }
            );
            console.log("Dygraph initialized");
            self.dygraph.interactionMode = 'zoom';
        } catch (e) {
            console.log("Cant init Dygraph", e);
        }
    },
    setData: function(data) {
        var self = this;
        self.setLoading(tr('main.loading') + '...');

        var config = {
            file: data,
            dateWindow: null,
            valueRange: null
        };
        if (self.dygraph) {
            self.dygraph.updateOptions(config);

            self.dygraph.ready(function () {
                self.setLoading(false);
            });

            console.log("Dygraph set data complete")
        }
        else {
            self.initConfig.file = config.file;
        }

    },
    setDataAndOptions: function (data, options) {
        var self = this;
        console.log("setDataAndOptions ");
        console.log(options);
        self.setLoading(tr('main.loading') + '...');
        var config = {
            file: data,
            dateWindow: null,
            valueRange: null,
            labels: options.labels,
            underlayCallback: options.underlayCallback,
            series: {} // TODO check without mappings
        };

        Ext.Array.each(config.labels, function (l) {
            var labelAxis = options.axisOf(l);
            if(labelAxis != undefined) {
                config.series[l] = {axis: labelAxis};
            }
        });


        // var visibility = [];
        // for(var i = 1; i < config.labels.length; ++i) {
        //     var l = config.labels[i];
        //     if(options.invisible && options.invisible[l])
        //         visibility.push(false);
        //     else visibility.push(true);
        // }

        self.axisLabels = config.labels;

        console.log("Here");
        if(self.dygraph) {
            //console.log("Here2");
          //  console.log('Update options config = ');
            console.log(config);
            self.dygraph.updateOptions(config);

            self.dygraph.ready(function () {
                self.setLoading(false);
            });
        }
        else
        {
           // console.log("Here3");
            self.initConfig = config;
        }

    }
});

