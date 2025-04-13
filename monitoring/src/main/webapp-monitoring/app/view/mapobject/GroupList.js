/**
 * Created by IVAN on 26.06.2015.
 */
//Временный костыль для работы selection в grid с grouping feature
Ext.view.Table.override
({
    hasActiveGrouping: function ()
    {
        return this.isGrouping && this.store.isGrouped();
    },

    getRecord: function (node)
    {
        var me = this,
            record,
            recordIndex;

        // If store.destroyStore has been called before some delayed event fires on a node, we must ignore the event.
        if (me.store.isDestroyed)
        {
            return;
        }

        node = me.getNode(node);
        if (node)
        {
            // If we're grouping, the indexes may be off
            // because of collapsed groups, so just grab it by id
            if (!me.hasActiveGrouping())
            {
                recordIndex = node.getAttribute('data-recordIndex');
                if (recordIndex)
                {
                    recordIndex = parseInt(recordIndex, 10);
                    if (recordIndex > -1)
                    {
                        // The index is the index in the original Store, not in a GroupStore
                        // The Grouping Feature increments the index to skip over unrendered records in collapsed groups
                        return me.store.data.getAt(recordIndex);
                    }
                }
            }
            //record = me.store.getByInternalId(node.getAttribute('data-recordId')); // comment this line for the getByInternalId error.
            //console.dir(node.getAttribute('data-recordId'));

            if (!record)
            {
                record = this.dataSource.data.get(node.getAttribute('data-recordId'));
            }

            return record;
        }
    },

    indexInStore: function (node)
    {
        //node = node.isCollapsedPlaceholder ? this.getNode(node) : this.getNode(node, false); // comment this line for the isCollapsedPlaceholder error.
        node = this.getNode(node, true);
        if (!node && node !== 0)
        {
            return -1;
        }
        var recordIndex = node.getAttribute('data-recordIndex');
        if (recordIndex)
        {
            return parseInt(recordIndex, 10);
        }
        return this.dataSource.indexOf(this.getRecord(node));
    }
});


Ext.override(Ext.grid.plugin.CellEditing,
    {
        showEditor: function(ed, context, value)
        {
            var me = this,
                record = context.record,
                columnHeader = context.column,
                sm = me.grid.getSelectionModel(),
                selection = sm.getCurrentPosition(),
                otherView = selection && selection.view;


            // Selection is for another view.
            // This can happen in a lockable grid where there are two grids, each with a separate Editing plugin
            if (otherView && otherView !== me.view)
            {
                return me.lockingPartner.showEditor(ed, me.lockingPartner.getEditingContext(selection.record, selection.columnHeader), value);
            }


            me.setEditingContext(context);
            me.setActiveEditor(ed);
            me.setActiveRecord(record);
            me.setActiveColumn(columnHeader);


            // Select cell on edit only if it's not the currently selected cell
            if (sm.selectByPosition && (!selection || selection.column !== context.colIdx || selection.row !== context.rowIdx))
            {
                sm.selectByPosition
                ({
                    row: (context.store.getGroupField && !! context.store.getGroupField())? context.record.index : context.rowIdx,
                    column: context.colIdx,
                    view: me.view
                });
            }


            ed.startEdit(me.getCell(record, columnHeader), value, context);
            me.editing = true;
            me.scroll = me.view.el.getScroll();
        },
    });
// -------
Ext.define('Seniel.view.mapobject.GroupList', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.groupedobjectslist',
    initComponent: function () {
        var self = this;
        self.addEvents('check', 'uncheck', 'mapobjlock', 'mapobjunlock');
        objectsCommander.commandPasswordNeeded(function (resp) {
            self.commandPasswordNeeded = resp;
        });
        Ext.apply(this, {
            store: Ext.create("EDS.store.GroupedMapObjects", {
                autoLoad: true,
                groupField: 'group',
                listeners: {
                    load: function (store, records, successful, eOpts) {
                        //var arr = Ext.Array.filter(records, function(r) {
                        //    return r.get('checked');
                        //});
                        //Ext.each(arr, function(o) {
                        //    self.fireEvent('check', self, o);
                        //});
                        //self.loadSelected = arr.length;
                        var tar = Ext.Array.filter(records, function (r) {
                            return r.get('targeted');
                        });
                        self.targetedObjects = new Array();
                        Ext.each(tar, function (o) {
                            self.targetedObjects.push(o.get('uid'));
                        });

                        var showHidden = Ext.util.Cookies.get('showHiddenObjects');
                        if (showHidden === 'true') {
                            self.dockedItems.get(1).down('button').toggle(true);
                        }
                    }
                },
                filters: [
                    {
                        id: 'hidn',
                        filterFn: function (item) {
                            return item.get('hidden') === false;
                        }
                    }
                ]
            }),
            features: [{
                ftype: 'groupingsummary',
                id:"grouping",
                enableGroupingMenu:false,
                startCollapsed:true,
                groupHeaderTpl: [
                    "<table width='100%'> <tr>" +
                    "<td>{columnName}: {name}</td>" +
                    //" <td width='20px' align='right'> <img id='commands-{name}' src='images/ico16_commands.png'></td>" +
                    " <td  width='20px' align='right'><a class='icon-tooltip' ><img id='globeact-{name}'  src='images/ico16_globeact.png'>" +
                    "<span>"+tr('mapobject.showonmap') + "</span></a></td>" +
                    " <td  width='20px' align='right'><a class='icon-tooltip' ><img id='globeunact-{name}'  src='images/ico16_globe.png'>" +
                    "<span>"+tr('mapobject.hideonmap') + "</span></a></td>" +
                    "</tr></table>"
                ]
            }],
            listeners:{
                'groupclick': function (view, node, group, e, eOpts) {
                    var feature=view.getFeature("grouping");
                    var htmlElement=e.getTarget();
                    if(htmlElement.outerHTML.substr(1,3)=='img'){
                        if(feature.isExpanded(group)) feature.collapse(group)
                        else feature.expand(group)
                    }
                    var groupobjectlist = self;
                    var groups = groupobjectlist.getStore().getGroups();
                    for (var i in groups) {
                        if (groups[i].name == group) {
                            if (htmlElement.id.search('globeact') > -1) {
                                Ext.each(groups[i].children, function (o) {
                                    groupobjectlist.fireEvent('check', groupobjectlist, o)
                                })
                            }
                            if (htmlElement.id.search('globeunact') > -1) {
                                Ext.each(groups[i].children, function (o) {
                                    groupobjectlist.fireEvent('uncheck', groupobjectlist, o);
                                });
                            }
                        }
                    }
                }
            }
        });
        this.callParent();
    },
    displayField: 'name',
    rootVisible: false,
    useArrows: true,
    stateId: 'groupedObjList',
    stateful: true,
    //selType: 'checkboxmodel',
    //selModel: {
    //    showHeaderCheckbox: true,
    //    mode: 'SIMPLE',
    //    checkOnly: true
    //},
    viewConfig: {
        stripeRows: false,
        markDirty: false,
        getRowClass: function (record) {
            return record.get('hidden') === true ? 'grayedText' : '';
        }
    },
    //plugins: 'bufferedrenderer',
    commandPasswordNeeded: true,
    //header: false,
    //hideCollapseTool: true,
    //hideHeaders: false,
    //lastTimeUpdated: new Date().getTime(),
    columns: [
        {
            stateId: 'onMap',
            menuText: tr('mapobject.showingonmap'),
            tooltip: tr('mapobject.showingonmap.tooltip'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            width: 20,
            dataIndex: 'checked',
            menuDisabled: true,
            sealed: true,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_globe.png',
                    isDisabled: function (view, rowIndex, colIndex, item, rec) {
                        return !rec.get('latestmsg');
                    },
                    handler: function (view, rowIndex,colIndex, item ,e,    record ,    row) {
                        //var rec = view.getStore().getAt(rowIndex);
                        //rec.set('checked', true);
                        this.up('groupedobjectslist').fireEvent('check', view, record);
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('checked')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.showingonmap.stop') + '"';
                            return 'x-hide-display';
                        } else if (rec.get('latestmsg')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.showingonmap.show') + '"';
                            return 'object-list-button';
                        } else {
                            return 'object-list-button-disabled';
                        }
                    }
                },
                {
                    icon: 'images/ico16_globeact.png',
                    handler: function (view, rowIndex,colIndex, item ,e,    record ) {
                        //var rec = view.getStore().getAt(rowIndex);
                        //rec.set('checked', false);
                        this.up('groupedobjectslist').fireEvent('uncheck', view, record);
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('checked')) {
                            return 'object-list-button';
                        } else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            dataIndex: 'group',
            text:tr('main.groupsofobjects.group'),
            hidden:true
        },
        {
            stateId: 'name',
            text: tr('mapobject.objectname'),
            menuText: tr('mapobject.objectname.menutext'),
            tooltip: tr('mapobject.objectname.tooltip'),
            tooltipType: 'title',
            flex: 1,
            dataIndex: 'name',
            renderer: function (val, metaData, rec) {
                var res;
                if (rec.get('blocked') === 'wait') {
                    res = '<img src="images/ico16_loading.png" alt="" title="' + tr('mapobject.waitingforanswer') + '" /><span class="object-list-name-text">' + val + '</span>';
                } else if (rec.get('blocked') === true) {
                    res = '<img src="images/ico16_lock.png" alt="" title="' + tr('mapobject.blockedobject') + '" /><span class="object-list-name-text">' + val + '</span>';
                } else {
                    res = val;
                }
                return res;
            },
            summaryType: 'count',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>'+tr('main.groupofobjects.totalobjects')+'{0} </b>', value);
            }
        },
        {
            stateId: 'speed',
            menuText: tr('mapobject.speedstate'),
            tooltip: tr('mapobject.speedstate.tooltip'),
            tooltipType: 'title',
            menuDisabled: true,
            resizable: false,
            dataIndex: 'speed',
            width: 24,
            renderer: function (val, metaData, rec) {
                var lastmsg = new Date(rec.get('latestmsg')),
                    current = new Date(),
                    yesterday = Ext.Date.subtract(current, Ext.Date.DAY, 1),
                    imgPostfix = (!Ext.Date.between(lastmsg, yesterday, current)) ? ('_old') : (''),
                    msgPostfix = (!Ext.Date.between(lastmsg, yesterday, current)) ? (' (' + tr('mapobject.laststate.olddata') + ')') : ('');
                var msg = 'title="' + tr('mapobject.laststate') + msgPostfix + ': ' + tr('mapobject.laststate.standing') + '"',
                    res = '<img src="images/ico16_car_stop' + imgPostfix + '.png" alt="" />',
                    ignition = (rec.get('ignition') !== 'unknown' && rec.get('ignition') > 0) ? (true) : (false);

                if (val > 0) {
                    if (ignition) {
                        msg = 'title="' + tr('mapobject.laststate') + msgPostfix + ': ' + tr('mapobject.laststate.running') +
                            ' (' + tr('mapobject.laststate.speed') + ' ' + val + ' ' + tr('mapobject.laststate.kmsperhour') + '), ' + tr('mapobject.laststate.ignitionon') + '"';
                        res = '<img src="images/ico16_car_drve' + imgPostfix + '.png" alt="" />';
                    } else {
                        msg = 'title="' + tr('mapobject.laststate') + msgPostfix + ': ' + tr('mapobject.laststate.running') +
                            ' (' + tr('mapobject.laststate.speed') + ' ' + val + ' ' + tr('mapobject.laststate.kmsperhour') + ')"';
                        res = '<img src="images/ico16_car_move' + imgPostfix + '.png" alt="" />';
                    }
                } else if (ignition) {
                    msg = 'title="' + tr('mapobject.laststate') + msgPostfix + ': ' + tr('mapobject.laststate.standing') + ', ' + tr('mapobject.laststate.ignitionon') + '"';
                    res = '<img src="images/ico16_car_engn' + imgPostfix + '.png" alt="" />';
                }
                metaData.tdAttr = msg;
                return res;
            }
        },
        {
            stateId: 'lastMsg',
            menuText: tr('mapobject.lastmessage.menuText'),
            tooltip: tr('mapobject.lastmessage.tooltip'),
            tooltipType: 'title',
            menuDisabled: true,
            resizable: false,
            sortable: true,
            dataIndex: 'latestmsg',
            width: 24,
            renderer: function (val, metaData, rec) {
                var color = 'red',
                    strsat = (rec.get('satelliteNum') > 5) ? ('h') : ('l'),
                    text = tr('mapobject.lastmessage.empty');

                if (val) {
                    //В отличии от всплывающего окна на карте не обновляется при наведении,
                    // а генерируется при получении новых данных,
                    // поэтому  метод Seniel.view.WRUtils.getLastMsgText() не подходит
                    text = Ext.Date.format(new Date(val), tr('format.extjs.datetime'));
                    color = Seniel.view.WRUtils.getLastMsgColor(new Date(val)).code;
                }
                metaData.tdAttr = 'title="' + tr('mapobject.lastmessage.lastmessage') + ': ' + text + ' \n' + tr('mapobject.lastmessage.satellitecount') + ': ' + rec.get('satelliteNum') + '"';

                return '<img src="images/ico16_sat' + strsat + color + '.png" alt="" />';
            }
        },
        {
            stateId: 'tracing',
            menuText: tr('mapobject.objecttracing'),
            tooltip: tr('mapobject.objecttracing.tooltip'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            width: 24,
            dataIndex: 'targeted',
            menuDisabled: true,
            sealed: true,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_target.png',
                    handler: function (view, rowIndex,colIndex, item ,e,    record) {
                        this.up('groupedobjectslist').fireEvent("traceobject",record,view,true);
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('targeted')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.objecttracing.stopobjecttracing') + '"';
                            return 'x-hide-display';
                        } else {
                            metaData.tdAttr = 'title="' + tr('mapobject.objecttracing.doobjecttracing') + '"';
                            return 'object-list-button';
                        }
                    }
                },
                {
                    icon: 'images/ico16_targeted.png',
                    handler: function (view, rowIndex,colIndex, item ,e,    record) {
                        this.up('groupedobjectslist').fireEvent("traceobject",record,view,false)
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('targeted')) {
                            return 'object-list-button';
                        } else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            stateId: 'sleeper',
            menuText: tr('mapobject.sleepermessages'),
            tooltip: tr('mapobject.sleepermessages.tooltip'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            menuDisabled: true,
            resizable: true,
            sortable: true,
            dataIndex: 'sleeper',
            width: 24,
            items: [
                {
                    handler: function (view, rowIndex, colIndex, item, e, rec) {
                        var viewport = view.up('viewport');
                        var uid = rec.get('uid');

                        if (!viewport.slprWindows) {
                            viewport.slprWindows = new Array();
                        }

                        if (!viewport.slprWindows[rec.get('uid')]) {
                            var wndname = tr('mapobject.sleepermessages.sleepers') + ' &laquo;' + rec.get('name') + '&raquo;';

                            mapObjects.getSleeperInfo(uid, function (sleeperData) {
                                console.log('Sleeper data = ', sleeperData);
                                if (sleeperData && sleeperData.length) {
                                    viewport.slprWindows[uid] = Ext.create('Seniel.view.sleepers.DataWindow', {
                                        title: wndname,
                                        slprData: sleeperData,
                                        uid: uid,
                                        btnConfig: {
                                            icon: 'images/ico24_device_def.png',
                                            text: wndname
                                        }
                                    });

                                    viewport.slprWindows[uid].on('close', function () {
                                        delete viewport.slprWindows[uid];
                                    });
                                    viewport.showNewWindow(viewport.slprWindows[uid]);
                                }
                            });
                        } else {
                            viewport.slprWindows[uid].setActive(true);
                        }
                        console.log('Sleeper windows = ', viewport.slprWindows);

                    },
                    getClass: function (val, metaData, rec) {
                        if (val !== null) {
                            var lmd = new Date(val.time),
                                lms = val.time ? Ext.Date.format(lmd, tr('format.extjs.datetime')) : tr('mapobject.sleepermessages.nomessages');
                            var res = val.sleeperState === "Warning" ? 'background-image: url(images/ico16_device_err.png);'
                                : val.sleeperState === "OK" ? 'background-image: url(images/ico16_device_ok.png);'
                                : val.sleeperState === "Unknown" ? 'background-image: url(images/ico16_device_def.png);' : '';
                            var battery = val.battery ? "\n" + tr('mapobject.sleepermessages.batterypower') + ": " + val.battery : "";
                            var alarm = val.alarm ? "\n" + tr('mapobject.sleepermessages.alarm') + ": " + val.alarm : "";
                            res = (res) ? (res + 'background-repeat: no-repeat; background-position: 4px 3px;') : ('');
                            metaData.tdAttr = 'title="' + tr('mapobject.sleepermessages.lastmessage') + ': ' + lms + battery + alarm + '" style="padding: 2px; ' + res + '"';
                            return 'object-list-button';
                        }
                        else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            stateId: 'radio',
            menuText: tr('mapobject.radioblocks'),
            tooltip: tr('mapobject.radioblocks.tooltip'),
            tooltipType: 'title',
            menuDisabled: true,
            resizable: false,
            sortable: true,
            dataIndex: 'radioUnit',
            width: 24,
            renderer: function (val, metaData, rec) {
                if (val) {
                    if (val.installed) {
                        var workdate = new Date(val.workDate),
                            current = new Date(),
                            yearago = Ext.Date.subtract(current, Ext.Date.YEAR, 1),
                            isValid = Ext.Date.between(workdate, yearago, current);
                        metaData.tdAttr = 'title="' + val.type + ' ' + val.model + '\n' + tr('mapobject.radioblocks.lastdate') + ': ' + Ext.Date.format(workdate, tr('format.extjs.date')) + '"';
                        return (isValid) ? ('<img src="images/ico16_radio_ok.png" alt="" />') : ('<img src="images/ico16_radio_err.png" alt="" />');
                    } else {
                        return '';
                    }
                } else {
                    return '';
                }
            }
        },
        {
            stateId: 'actions',
            menuText: tr('mapobject.objectactions'),
            tooltip: tr('mapobject.objectactions.tooltip'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            width: 24,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_commands.png',
                    isDisabled: function (view, rowIndex, colIndex, item, rec) {
                        return !((rec.get('canBlock') || rec.get('canGetCoords') || rec.get("canRestartTerminal")) && rec.get('latestmsg'));
                    },
                    handler: function (view, rowIndex, colIndex, item, e, rec) {
                        var mapobjectlist = this.up('groupedobjectslist')
                        console.log("record", rec);
                        var commandPasswordNeeded = view.up('grid').commandPasswordNeeded;
                        mapobjectlist.createCommandWindow(rec, commandPasswordNeeded, e.getXY())
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('canBlock') || rec.get('canGetCoords') || rec.get("canRestartTerminal") && rec.get('latestmsg')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.objectactions.blocking.command.sendcommand') + '"';
                            return '';
                        } else {
                            return 'object-list-button-disabled';
                        }
                    }
                }
            ]
        },
        {
            statedId: 'reports',
            xtype: 'actioncolumn',
            width: 24,
            resizable: false,
            sortable: false,
            menuDisabled: false,
            menuText: tr('main.reports'),
            tooltip: tr('mapobject.reports.tooltip'),
            tooltipType: 'title',
            items: [
                {
                    icon: 'images/ico16_report.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var viewPort = this.up('viewport');
                        var uid = rec.get('uid');
                        var menu = Ext.create('Seniel.view.reports.ReportsMenu', {
                            selectedUid:uid,
                            parentViewport: viewPort,
                            reports:  ['general', 'fuel', 'groupReport', 'addresses']
                        });

                        menu.showAt(e.getXY());
                    },
                    getClass: function(val, metaData, rec) {
                        metaData.tdAttr = 'title="' + tr('mapobject.reports.tooltip') + '"';
                        return '';
                    }
                }
            ]
        },
        {
            stateId: 'params',
            menuText: tr('mapobject.objectactions.objectsettings'),
            tooltip: tr('mapobject.objectactions.objectsettings.tooltip'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            width: 24,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_options.png',
                    isDisabled: function (view, rowIndex, colIndex, item, rec) {
                        return !rec.get('settingsViewEnabled');
                    },
                    handler: function (grid, rowIndex, colIndex, item, e, rec) {
                        var viewport = this.up('viewport');
                        var uid = rec.get('uid');

                        if (!viewport.settingsWindows) {
                            viewport.settingsWindows = new Array();
                        }

                        if (!viewport.settingsWindows[uid]) {
                            objectSettings.loadObjectSettings(rec.get('uid'), function (data, response) {
                                if (data) {
                                    viewport.settingsWindows[uid] = Ext.create('Seniel.view.mapobject.SettingsWindow', {
                                        title: tr('mapobject.objectactions.objectsettings.settings') + ' &laquo;' + rec.get('name') + '&raquo;',
                                        objSettings: data,
                                        objRecords: [rec],
                                        btnConfig: {
                                            icon: 'images/ico24_options.png',
                                            text: tr('mapobject.objectactions.objectsettings.settings') + ' &laquo;' + rec.get('name') + '&raquo;'
                                        }
                                    });

                                    viewport.settingsWindows[uid].on('close', function () {
                                        delete viewport.settingsWindows[uid];
                                    });

                                    viewport.showNewWindow(viewport.settingsWindows[uid]);
                                } else if (response.type === 'exception' && response.message) {
                                    Ext.MessageBox.show({
                                        title: tr('mapobject.objectactions.error'),
                                        msg: response.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                            });
                        } else {
                            viewport.settingsWindows[uid].setActive(true);
                        }
                    },
                    getClass: function (val, metaData, rec) {
                        if (rec.get('settingsViewEnabled')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.objectactions.objectsettings.forobject') + '"';
                            return '';
                        } else {
                            return 'object-list-button-disabled';
                        }
                    }
                }
            ]
        }
    ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'top',
            items: [
                {
                    xtype: 'textfield',
                    name: 'filterobjs',
                    allowBlank: true,
                    fieldLabel: tr('mapobject.objectsearch') + ':',
                    flex: 1,
                    listeners: {
                        change: function(field, val, oldval) {
                            var str = val.replace(/\\/,'\\\\').replace(/\[/, '\\[').replace(/\]/, '\\]').replace(/\+/, '\\+').replace(/\|/, '\\|').replace(/\./, '\\.').replace(/\(/, '\\(').replace(/\)/, '\\)').replace(/\?/, '\\?').replace(/\*/, '\\*').replace(/\^/, '\\^').replace(/\$/, '\\$');
                            console.log('val = '+str);
                            var re = new RegExp(str, 'i'),
                                fname = {id: 'name', property: 'name', value: re},
                                curf = field.up('groupedobjectslist').getStore().filters,
                                fhidn = curf.findBy(function(item) {
                                    return item.id === 'hidn';
                                });
                            console.log("fname=", fname);
                            if ((oldval && val.length < oldval.length) || (oldval && val.length === oldval.length && val !== oldval)){
                                field.up('groupedobjectslist').getStore().clearFilter(true);
                            }
                            if (!fhidn){
                                field.up('groupedobjectslist').getStore().filter(fname);
                            } else {
                                if (val === '') {
                                    field.up('groupedobjectslist').getStore().filter(fhidn);
                                } else {
                                    field.up('groupedobjectslist').getStore().filter([fhidn, fname]);
                                }
                            }
                        }
                    }
                },
                '-',
                {
                    xtype: 'button',
                    itemId:'showHiddenObjects',
                    icon: 'images/ico16_show.png',
                    tooltip: tr('mapobject.showallobjects'),
                    tooltipType: 'title',
                    scale: 'small',
                    enableToggle: true,
                    toggleHandler: function(btn, pressed) {
                        this.up('groupedobjectslist').fireEvent('showhiddenobjects',pressed);
                    }
                },
                '-',
                //{
                //    xtype: 'button',
                //    text: '',
                //    icon: 'images/ico16_checkall.png',
                //    tooltip: tr('mapobject.actionsonselected'),
                //    tooltipType: 'title',
                //    scale: 'small',
                //    menu: {
                //        items: [
                //            {
                //                text: tr('mapobject.showonmap'),
                //                icon: 'images/ico16_globeact.png',
                //                tooltip: tr('mapobject.showonmap.tooltip'),
                //                tooltipType: 'title',
                //                handler: function() {
                //                    var list = this.up('mapobjectlist');
                //                    var selnodes = list.getSelectionModel().getSelection();
                //                    if (selnodes.length === 0){
                //                        Ext.MessageBox.show({
                //                            title: tr('mapobject.impossibleoperation'),
                //                            msg: tr('mapobject.impossibleoperation.tooltip'),
                //                            icon: Ext.MessageBox.INFO,
                //                            buttons: Ext.Msg.OK
                //                        });
                //                        return;
                //                    }
                //                    Ext.each(selnodes, function(o) {
                //                        o.set('checked', true);
                //                        list.fireEvent('check', list, o);
                //                    });
                //                }
                //            },
                //            {
                //                text: tr('mapobject.hideonmap'),
                //                icon: 'images/ico16_globe.png',
                //                tooltip: tr('mapobject.hideonmap.tooltip'),
                //                tooltipType: 'title',
                //                handler: function() {
                //                    var list = this.up('mapobjectlist');
                //                    var selnodes = list.getSelectionModel().getSelection();
                //                    if (selnodes.length === 0){
                //                        Ext.MessageBox.show({
                //                            title: tr('mapobject.impossibleoperation'),
                //                            msg: tr('mapobject.impossibleoperation.tooltip'),
                //                            icon: Ext.MessageBox.INFO,
                //                            buttons: Ext.Msg.OK
                //                        });
                //                        return;
                //                    }
                //                    Ext.each(selnodes, function(o) {
                //                        o.set('checked', false);
                //                        list.fireEvent('uncheck', list, o);
                //                    });
                //                }
                //            },
                //            {
                //                text: tr('mapobject.hideinlist'),
                //                icon: 'images/ico16_hide.png',
                //                tooltip: tr('mapobject.hideinlist.tooltip'),
                //                tooltipType: 'title',
                //                handler: function() {
                //                    var selnodes = this.up('mapobjectlist').getSelectionModel().getSelection();
                //                    if (selnodes.length === 0){
                //                        Ext.MessageBox.show({
                //                            title: tr('mapobject.impossibleoperation'),
                //                            msg: tr('mapobject.impossibleoperation.tooltip'),
                //                            icon: Ext.MessageBox.INFO,
                //                            buttons: Ext.Msg.OK
                //                        });
                //                        return;
                //                    }
                //                    Ext.each(selnodes, function(o) {
                //                        o.set('hidden', true);
                //                    });
                //                    this.up('mapobjectlist').getStore().filter();
                //                    this.up('mapobjectlist').getSelectionModel().deselectAll();
                //
                //                    mapObjects.setHiddenUids(Ext.Array.map(selnodes, function (o) {
                //                        return o.get('uid');
                //                    }));
                //
                //                }
                //            },
                //            {
                //                text: tr('mapobject.showinlist'),
                //                icon: 'images/ico16_show.png',
                //                tooltip: tr('mapobject.showinlist.tooltip'),
                //                tooltipType: 'title',
                //                handler: function() {
                //                    var selnodes = this.up('mapobjectlist').getSelectionModel().getSelection();
                //                    if (selnodes.length === 0){
                //                        Ext.MessageBox.show({
                //                            title: tr('mapobject.impossibleoperation'),
                //                            msg: tr('mapobject.impossibleoperation.tooltip'),
                //                            icon: Ext.MessageBox.INFO,
                //                            buttons: Ext.Msg.OK
                //                        });
                //                        return;
                //                    }
                //                    Ext.each(selnodes, function(o) {
                //                        o.set('hidden', false);
                //                    });
                //                    mapObjects.unsetHiddenUids(Ext.Array.map(selnodes, function(o) {
                //                        return o.get('uid');
                //                    }));
                //                }
                //            }
                //        ]
                //    }
                //}
            ]
        }
    ],
    createCommandWindow:function(rec,commandPasswordNeeded,x,y){
        var viewport = this.up('viewport'),
            wndid = ++viewport.wndcount;
        console.log("record",rec);
        console.log("commandPasswordNeeded",commandPasswordNeeded);
        var dlg=viewport.createOnlyOneWnd('commandWnd','Seniel.view.mapobject.CommandWindow',{
                title:tr('mapobject.objectactions.commands')+' "'+rec.get("name")+'"',
                record:rec,
                commandPasswordNeeded:commandPasswordNeeded,
                btnConfig:{
                    icon: 'images/ico24_messages.png',
                    text:tr('mapobject.objectactions.commands')+' "'+rec.get("name")+'"'
                }
            }
        )
        console.log("dlg",dlg)
        viewport.showNewWindowAt(dlg, x,y)
    }
})
