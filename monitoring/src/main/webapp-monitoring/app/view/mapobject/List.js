Ext.define('Seniel.view.mapobject.List', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.mapobjectlist',
    stateId: 'objList',
    stateful: true,
    selType: 'checkboxmodel',
    selModel: {
        showHeaderCheckbox: true,
        mode: 'SIMPLE',
        checkOnly: true
    },
    viewConfig: {
        stripeRows: false,
        markDirty: false,
        getRowClass: function(record) {
            return record.get('hidden') === true ? 'grayedText' : '';
        }
    },
    plugins: 'bufferedrenderer',
    commandPasswordNeeded: true,
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
                            curf = field.up('mapobjectlist').getStore().filters,
                            fhidn = curf.findBy(function(item) {
                                return item.id === 'hidn';
                            });
                            console.log("fname=", fname);
                            if ((oldval && val.length < oldval.length) || (oldval && val.length === oldval.length && val !== oldval)){
                                field.up('mapobjectlist').getStore().clearFilter(true);
                            }
                            if (!fhidn){
                                field.up('mapobjectlist').getStore().filter(fname);
                            } else {
                                if (val === '') {
                                    field.up('mapobjectlist').getStore().filter(fhidn);
                                } else {
                                    field.up('mapobjectlist').getStore().filter([fhidn, fname]);
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
                        this.up('mapobjectlist').fireEvent('showhiddenobjects',pressed);
                    }
                },
                '-',
                {
                    xtype: 'button',
                    text: '',
                    icon: 'images/ico16_checkall.png',
                    tooltip: tr('mapobject.actionsonselected'),
                    tooltipType: 'title',
                    scale: 'small',
                    menu: {
                        items: [
                            {
                                text: tr('mapobject.showonmap'),
                                icon: 'images/ico16_globeact.png',
                                tooltip: tr('mapobject.showonmap.tooltip'),
                                tooltipType: 'title',
                                handler: function() {
                                    var list = this.up('mapobjectlist');
                                    var selnodes = list.getSelectionModel().getSelection();
                                    if (selnodes.length === 0){
                                        Ext.MessageBox.show({
                                            title: tr('mapobject.impossibleoperation'),
                                            msg: tr('mapobject.impossibleoperation.tooltip'),
                                            icon: Ext.MessageBox.INFO,
                                            buttons: Ext.Msg.OK
                                        });
                                        return;
                                    }
                                    Ext.each(selnodes, function(o) {
                                        //o.set('checked', true);
                                        list.fireEvent('check', list, o);
                                    });
                                }
                            },
                            {
                                text: tr('mapobject.hideonmap'),
                                icon: 'images/ico16_globe.png',
                                tooltip: tr('mapobject.hideonmap.tooltip'),
                                tooltipType: 'title',
                                handler: function() {
                                    var list = this.up('mapobjectlist');
                                    var selnodes = list.getSelectionModel().getSelection();
                                    if (selnodes.length === 0){
                                        Ext.MessageBox.show({
                                            title: tr('mapobject.impossibleoperation'),
                                            msg: tr('mapobject.impossibleoperation.tooltip'),
                                            icon: Ext.MessageBox.INFO,
                                            buttons: Ext.Msg.OK
                                        });
                                        return;
                                    }
                                    Ext.each(selnodes, function(o) {
                                        //o.set('checked', false);
                                        list.fireEvent('uncheck', list, o);
                                    });
                                }
                            },
                            {
                                text: tr('mapobject.hideinlist'),
                                icon: 'images/ico16_hide.png',
                                tooltip: tr('mapobject.hideinlist.tooltip'),
                                tooltipType: 'title',
                                handler: function() {
                                    var selnodes = this.up('mapobjectlist').getSelectionModel().getSelection();
                                    if (selnodes.length === 0){
                                        Ext.MessageBox.show({
                                            title: tr('mapobject.impossibleoperation'),
                                            msg: tr('mapobject.impossibleoperation.tooltip'),
                                            icon: Ext.MessageBox.INFO,
                                            buttons: Ext.Msg.OK
                                        });
                                        return;
                                    }
                                    Ext.each(selnodes, function(o) {
                                        o.set('hidden', true);
                                    });
                                    this.up('mapobjectlist').getStore().filter();
                                    this.up('mapobjectlist').getSelectionModel().deselectAll();

                                    mapObjects.setHiddenUids(Ext.Array.map(selnodes, function (o) {
                                        return o.get('uid');
                                    }));

                                }
                            },
                            {
                                text: tr('mapobject.showinlist'),
                                icon: 'images/ico16_show.png',
                                tooltip: tr('mapobject.showinlist.tooltip'),
                                tooltipType: 'title',
                                handler: function() {
                                    var selnodes = this.up('mapobjectlist').getSelectionModel().getSelection();
                                    if (selnodes.length === 0){
                                        Ext.MessageBox.show({
                                            title: tr('mapobject.impossibleoperation'),
                                            msg: tr('mapobject.impossibleoperation.tooltip'),
                                            icon: Ext.MessageBox.INFO,
                                            buttons: Ext.Msg.OK
                                        });
                                        return;
                                    }
                                    Ext.each(selnodes, function(o) {
                                        o.set('hidden', false);
                                    });
                                    mapObjects.unsetHiddenUids(Ext.Array.map(selnodes, function(o) {
                                        return o.get('uid');
                                    }));
                                }
//                            },
//                            {
//                                text: 'Настройки объектов',
//                                icon: 'images/ico16_options.png',
//                                tooltip: 'Настроить все отмеченные объекты',
//                                tooltipType: 'title',
//                                handler: function(){
//                                    var viewport = this.up('viewport'),
//                                        self = this,
//                                        selnodes = this.up('mapobjectlist').getSelectionModel().getSelection(),
//                                        wh = ((viewport.getHeight()-75)>420)?(420):(viewport.getHeight()-75),
//                                        ww = (viewport.getWidth()>480)?(480):(viewport.getWidth());
//                                    if (selnodes.length === 0) {
//                                        Ext.MessageBox.show({
//                                            title: 'Операция невозможна',
//                                            msg: 'Не выбран ни один объект из списка',
//                                            icon: Ext.MessageBox.INFO,
//                                            buttons: Ext.Msg.OK
//                                        });
//                                        return;
//                                    }
//                                    this.disable();
//                                    var wnd = Ext.create('Seniel.view.CarSettingsWindow', {
//                                        x: (viewport.getWidth()-ww)/2 ,
//                                        y: 0,
//                                        width: ww,
//                                        height: wh,
//                                        icon: 'images/ico16_options.png',
//                                        title: 'Настройки группы объектов',
//                                        btnConfig: {
//                                            icon: 'images/ico24_options.png',
//                                            text: 'Настройки объектов'
//                                        }
//                                    });
//                                    viewport.addMask(wnd);
//                                    var maincontainer = viewport.down('container[itemId="maincontainer"]');
//                                    maincontainer.add([wnd]);
//                                    wnd.on('close', function() {
//                                        self.enable();
//                                    });
//                                    wnd.on('boxready', function() {
//                                        var rg = this.down('panel[itemId="objimage"]').down('radiogroup'),
//                                            ra = new Array();
//                                        this.objectRecArray = selnodes;
//                                        rg.rgName = 'imgSizeObjGroup';
//                                        for (var i = 0; i < 3; i++) {
//                                            ra[i] = new Ext.form.field.Radio({
//                                                boxLabel: (i*8+24)+'x'+(i*8+24),
//                                                name: rg.rgName,
//                                                inputValue: (i*8+24),
//                                                padding: '0 12 0 0'
//                                            });
//                                            rg.insert(i, ra[i]);
//                                        }
//                                    });
//                                    wnd.show();
//                                }
                            }
                        ]
                    }
                }
            ]
        }
    ],
    header: false,
    hideCollapseTool: true,
    hideHeaders: false,
    lastTimeUpdated: new Date().getTime(),
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
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        return !rec.get('latestmsg');
                    },
                    handler: function(view, rowIndex,colIndex, item ,e,    record) {
                        //var rec = view.getStore().getAt(rowIndex);
                        //rec.set('checked', true);
                        this.up('mapobjectlist').fireEvent('check', view, record);
                    },
                    getClass: function(val, metaData, rec){
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
                    handler: function(view, rowIndex,colIndex, item ,e,    record) {
                        //var rec = view.getStore().getAt(rowIndex);
                        //rec.set('checked', false);
                        this.up('mapobjectlist').fireEvent('uncheck', view, record);
                    },
                    getClass: function(val, metaData, rec) {
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
            stateId: 'name',
            text: tr('mapobject.objectname'),
            menuText: tr('mapobject.objectname.menutext'),
            tooltip: tr('mapobject.objectname.tooltip'),
            tooltipType: 'title',
            flex: 1,
            dataIndex: 'name',
            renderer: function(val, metaData, rec) {
                var res;
                if (rec.get('blocked') === 'wait') {
                    res = '<img src="images/ico16_loading.png" alt="" title="'+tr('mapobject.waitingforanswer')+'" /><span class="object-list-name-text">'+val+'</span>';
                } else if (rec.get('blocked') === true) {
                    res = '<img src="images/ico16_lock.png" alt="" title="'+tr('mapobject.blockedobject')+'" /><span class="object-list-name-text">'+val+'</span>';
                } else {
                    res = val;
                }
                return res;
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
            renderer: function(val, metaData, rec) {
                var lastmsg = new Date(rec.get('latestmsg')),
                    current = new Date(),
                    yesterday = Ext.Date.subtract(current, Ext.Date.DAY, 1),
                    imgPostfix = (!Ext.Date.between(lastmsg, yesterday, current))?('_old'):(''),
                    msgPostfix = (!Ext.Date.between(lastmsg, yesterday, current))?(' ('+tr('mapobject.laststate.olddata')+')'):('');
                var msg = 'title="' + tr('mapobject.laststate') + msgPostfix + ': ' + tr('mapobject.laststate.standing') + '"',
                    res = '<img src="images/ico16_car_stop' + imgPostfix + '.png" alt="" />',
                    ignition = (rec.get('ignition') !== 'unknown' && rec.get('ignition') > 0)?(true):(false);

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
            renderer: function(val, metaData, rec) {
                var color = 'red',
                    strsat = (rec.get('satelliteNum') > 5)?('h'):('l'),
                    text = tr('mapobject.lastmessage.empty');

                if (val) {
                    text = Seniel.view.WRUtils.getLastMsgText(new Date(val));
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
                    handler: function(view, rowIndex,colIndex, item ,e,    record) {
                        this.up('mapobjectlist').fireEvent("traceobject",record,view,true);
                    },
                    getClass: function(val, metaData, rec) {
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
                    handler: function(view, rowIndex,colIndex, item ,e,    record) {
                        this.up('mapobjectlist').fireEvent("traceobject",record,view,false)
                    },
                    getClass: function(val, metaData, rec) {
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
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var viewport = view.up('viewport');
                        var uid = rec.get('uid');

                        if (!viewport.slprWindows) {
                            viewport.slprWindows = new Array();
                        }

                        if (!viewport.slprWindows[rec.get('uid')]) {
                            var wndname = tr('mapobject.sleepermessages.sleepers') + ' &laquo;' + rec.get('name') + '&raquo;';

                            mapObjects.getSleeperInfo(uid, function(sleeperData) {
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

                                    viewport.slprWindows[uid].on('close', function() {
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
                    getClass: function(val, metaData, rec) {
                        if (val !== null) {
                            var lmd = new Date(val.time),
                                lms = val.time ? Ext.Date.format(lmd, tr('format.extjs.datetime')) : tr('mapobject.sleepermessages.nomessages');
                            var res = val.sleeperState === "Warning" ? 'background-image: url(images/ico16_device_err.png);'
                                : val.sleeperState === "OK" ? 'background-image: url(images/ico16_device_ok.png);'
                                : val.sleeperState === "Unknown" ? 'background-image: url(images/ico16_device_def.png);' : '';
                            var battery = val.battery ? "\n" + tr('mapobject.sleepermessages.batterypower') + ": " + val.battery : "";
                            var alarm = val.alarm ? "\n" + tr('mapobject.sleepermessages.alarm') + ": " + val.alarm : "";
                            res = (res)?(res + 'background-repeat: no-repeat; background-position: 4px 3px;'):('');
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
            renderer: function(val, metaData, rec) {
                if (val) {
                    if (val.installed) {
                        var workdate = new Date(val.workDate),
                            current = new Date(),
                            yearago = Ext.Date.subtract(current, Ext.Date.YEAR, 1),
                            isValid = Ext.Date.between(workdate, yearago, current);
                        metaData.tdAttr = 'title="' + val.type + ' ' + val.model + '\n' + tr('mapobject.radioblocks.lastdate') + ': ' + Ext.Date.format(workdate, tr('format.extjs.date')) + '"';
                        return (isValid)?('<img src="images/ico16_radio_ok.png" alt="" />'):('<img src="images/ico16_radio_err.png" alt="" />');
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
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        return !((rec.get('canBlock') || rec.get('canGetCoords') ||  rec.get("canRestartTerminal")) && rec.get('latestmsg'));
                    },
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var mapobjectlist = this.up('mapobjectlist')
                        console.log("record",rec);
                        var commandPasswordNeeded=view.up('grid').commandPasswordNeeded;
                        mapobjectlist.createCommandWindow(rec,commandPasswordNeeded, e.getXY())
                    },
                    getClass: function(val, metaData, rec) {
                        if (rec.get('canBlock') || rec.get('canGetCoords') ||  rec.get("canRestartTerminal") && rec.get('latestmsg')) {
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
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        return !rec.get('settingsViewEnabled');
                    },
                    handler: function(grid, rowIndex, colIndex, item, e, rec) {
                        var viewport = this.up('viewport');
                        var uid = rec.get('uid');
                        
                        if (!viewport.settingsWindows) {
                            viewport.settingsWindows = new Array();
                        }

                        if (!viewport.settingsWindows[uid]) {
                            objectSettings.loadObjectSettings(rec.get('uid'), function(data, response) {
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

                                    viewport.settingsWindows[uid].on('close', function() {
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
                    getClass: function(val, metaData, rec) {
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
    title: tr('mapobject.allobjects'),//'All MapObjects',
    loadSelected: null,
    initComponent: function() {
        var self = this;
        self.addEvents('check', 'uncheck', 'mapobjlock', 'mapobjunlock','mapobjevent','mapobjremoveevent');
        objectsCommander.commandPasswordNeeded(function (resp) {
            self.commandPasswordNeeded = resp;
        });

        var thestore = Ext.create('EDS.store.MapObjects', {
            autoLoad: true,
            listeners: {
                load: function(store, records, successful, eOpts) {
                    var arr = Ext.Array.filter(records, function(r) {
                        return r.get('checked');
                    });
                    Ext.each(arr, function(o) {
                        self.fireEvent('check', self, o);
                    });
                    self.loadSelected = arr.length;

                    var tar = Ext.Array.filter(records, function(r) {
                        return r.get('targeted');
                    });
                    self.targetedObjects = new Array();
                    Ext.each(tar, function(o) {
                        self.targetedObjects.push(o.get('uid'));
                    });
                    
                    var showHidden = Ext.util.Cookies.get('showHiddenObjects');
                    if (showHidden === 'true') {
                        self.dockedItems.get(1).down('button').toggle(true);
                    }
                },
                update: function(store, record, operation, modifiedFieldNames, eOpts ) {
                    if (modifiedFieldNames.indexOf('blocked', 0) !== -1) {
                        if (record.get('blocked') === true) {
                            self.fireEvent('mapobjlock', record);
                        } else if (record.get('blocked') === false) {
                            self.fireEvent('mapobjunlock', record);
                        }
                    }
                }
            },
            filters: [
                {
                    id: 'hidn',
                    filterFn: function(item) {
                        return item.get('hidden') === false;
                    }
                }
            ],
            sort: function (params) {
                var dir = params ? params.direction : 'ASC';
                var prop = params ? params.property : 'name';

                params = params || { property: "name", direction: "ASC" };
                var mod = params.direction.toUpperCase() === "DESC" ? -1 : 1;

//                this.sorters.clear();        // these lines are needed
//                this.sorters.add(params);   // to update the column state

                thestore.superclass.sort.call(this,params);

                if (prop === 'sleeper') {
                    this.doSort(function (o1, o2) {
                        var sleeper1 = o1.get('sleeper');
                        var sleeper2 = o2.get('sleeper');

                        if (sleeper1 === sleeper2)
                            return 0;
                        if (!sleeper1)
                            return -1 * mod;
                        if (!sleeper2)
                            return 1 * mod;
                        return mod * (sleeper1.time > sleeper2.time ? 1 : sleeper1.time === sleeper2.time ? 0 : -1);
                    });
                }

                if (prop === 'radioUnit') {
                    this.doSort(function (o1, o2) {
                        var radio1 = o1.get('radioUnit');
                        var radio2 = o2.get('radioUnit');

//                        if (sleeper1 == sleeper2)
//                            return 0;
                        if (!radio1 || !radio1.installed)
                            return -1 * mod;
                        if (!radio2 || !radio2.installed)
                            return 1 * mod;
                        return mod * (radio1.workDate > radio2.workDate ? 1 : radio1.workDate === radio2.workDate ? 0 : -1);
                    });
                }
            }
        });

        Ext.apply(this, {
            store: thestore
        });
        this.callParent();
    },
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
    },
    displayField: 'name',
    rootVisible: false,
    useArrows: true
});