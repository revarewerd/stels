/**
 * Created by IVAN on 09.10.2015.
 */
Ext.define('Seniel.view.support.UserTicketsWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.userticketswnd',
    title: tr('support.request.my'),
    icon: 'images/ico16_eventsmsgs.png',
    btnConfig: {
        icon: 'images/ico16_eventsmsgs.png',
        text: tr('support.request.my')
    },
    minWidth: 400,
    minHeight: 320,
    width: 1000,
    height: 600,
    layout: 'border',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            items: [
                {
                    region: 'center',
                    xtype: 'userticketspanel'
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->',
                        {
                            text: 'OK',
                            width: 100,
                            handler: function () {
                                self.close();
                            }
                        }]
                }
            ]
        });
        this.callParent();
    }
});
Ext.define('Seniel.view.UserTicketsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.userticketspanel',
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    loadBeforeActivated: true,
    storeName: 'EDS.store.UserSupportRequestEDS',
    itemType: 'objects',
    searchStringWidth: 120,
    searchFieldWidth: 100,
    dockedToolbar: ['add', /*'remove',*/ 'refresh', 'fill', 'search'],

    initComponent: function () {
        var self = this;
        var getSearchCriteriaList = function (searchString, criteriaList) {
            var searchCriteriaList = [];
            for (var i in criteriaList) {
                if (criteriaList[i].criteriaDisplay.indexOf(searchString.toString().toLowerCase()) > -1)
                    searchCriteriaList.push(criteriaList[i].criteriaValue)
            }
            return searchCriteriaList
        };
        Ext.apply(this, {
            specialFilters: {
                'category': {
                    'filterFn': function (record, searchString) {
                        var criteriaList = [
                            {
                                criteriaDisplay: tr('support.request.category.equipment').toLowerCase(),
                                criteriaValue: 'equipment'
                            },
                            {
                                criteriaDisplay: tr('support.request.category.program').toLowerCase(),
                                criteriaValue: 'program'
                            },
                            {
                                criteriaDisplay: tr('support.request.category.finance').toLowerCase(),
                                criteriaValue: 'finance'
                            }
                        ];
                        return Ext.Array.contains(getSearchCriteriaList(searchString, criteriaList), record.get("category"))
                    }
                },
                'status': {
                    'filterFn': function (record, searchString) {
                        var criteriaList = [
                            {
                                criteriaDisplay: tr('support.ticket.status.new').toLowerCase(),
                                criteriaValue: 'new'
                            },
                            {
                                criteriaDisplay: tr('support.ticket.status.open').toLowerCase(),
                                criteriaValue: 'open'
                            },
                            {
                                criteriaDisplay: tr('support.ticket.status.close').toLowerCase(),
                                criteriaValue: 'close'
                            }
                        ];
                        return Ext.Array.contains(getSearchCriteriaList(searchString, criteriaList), record.get("status"))
                    }
                },
                'dateTime': {
                    'filterFn': function (record, searchString) {
                        var date = new Date(record.get("dateTime"));
                        var formatedDateTime = Ext.Date.format(date, tr('format.extjs.datetime'));
                        return formatedDateTime.toString().toLowerCase().indexOf(searchString.toString().toLowerCase()) > -1
                    }
                }
            },
            columns: [
                {
                    header: '#',
                    xtype: 'rownumberer',
                    width: 40,
                    resizable: true
                },
                {
                    header: tr('support.ticket.code'),
                    dataIndex: '_id',
                    width: 160,
                    resizable: true
                },
                {
                    header: tr('support.ticket.date'),
                    width: 120,
                    sortable: true,
                    dataIndex: 'dateTime',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val) {
                        var d = new Date(val);
                        return Ext.Date.format(d, tr('format.extjs.datetime'));
                    },
                    summaryType: 'count',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>Всего позиций: {0} </b>', value);
                    }
                },
                {
                    header: tr('support.request.category'),
                    width: 160,
                    sortable: true,
                    dataIndex: 'category',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"'
                        switch (val) {
                            case "equipment": {
                                val = tr('support.request.category.equipment');
                                break;
                            }
                            case "program": {
                                val = tr('support.request.category.program');
                                break;
                            }
                            case "finance": {
                                val = tr('support.request.category.finance');
                                break;
                            }
                        }
                        return val
                    }
                },
                {
                    header: tr('support.request.question'),
                    width: 180,
                    sortable: true,
                    dataIndex: 'question',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: tr('support.ticket.content'),
                    flex: 2,
                    sortable: true,
                    dataIndex: 'description',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"';
                        return val
                    }
                    //hidden:hideRule
                },
                {
                    header: tr('support.ticket.status'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'status',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        var res = "";
                        switch (val) {
                            case "new" : {
                                res = '<span style="color:greenyellow;">' + tr("support.ticket.status.new") + '</span>';
                                break;
                            }
                            case "open" : {
                                res = '<span style="color:green;">' + tr("support.ticket.status.open") + '</span>';
                                break;
                            }
                            case "close" : {
                                res = '<span style="color:red;">' + tr("support.ticket.status.close") + '</span>';
                                break;
                            }
                            default : {
                                res = val;
                                break;
                            }
                        }
                        return res;
                    }
                },
                {
                    text: '<img src="images/ico16_msghist.png"/>',
                    menuText: tr("support.ticket.readStatus"),
                    tooltip: tr("support.ticket.readStatus"),
                    tooltipType: 'title',
                    sortable: true,
                    resizable: false,
                    dataIndex: 'userRead',
                    width: 32,
                    renderer: function (val, metaData, rec) {
                        if (val === true) {
                            metaData.tdAttr = 'style="cursor: pointer !important;" ' + 'title="' + tr("support.ticket.readStatus.read") + '"';
                            return '<img src="images/ico16_msghist.png"  alt=""/>'
                        }
                        else {
                            metaData.tdAttr = 'style="cursor: pointer !important;" ' + 'title="' + tr("support.ticket.readStatus.unread") + '"';
                            return '<img src="images/ico16_msghist_yel.png" alt=""/>'
                        }
                    }
                },
                {
                    xtype: 'actioncolumn',
                    menuText: tr('support.ticket'),
                    //hidden:hideRule,
                    header: tr('support.ticket'),
                    width: 60,
                    dataIndex: 'name',
                    sortable: false,
                    align: 'center',
                    items: [
                        {
                            icon: 'images/ico16_eventsmsgs.png',
                            //disabled:hideRule,
                            tooltip: tr('support.ticket.show'),
                            handler: function (view, rowIndex, colIndex, item, e, rec) {
                                var grid = this.up('window').down('grid');
                                grid.showTicketForm(rec);
                                console.log("Просмотр заявки");
                            }
                        }
                    ]
                }
            ],
            listeners: {
                celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    self.showTicketForm(record);
                },
                cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    var dataIndex = self.columnManager.columns[cellIndex].dataIndex
                    switch (dataIndex) {
                        case  "userRead": {
                            var readStatus = record.get('userRead');
                            var ticketId = record.get("_id");
                            //var text = "Пометить прочитанной";
                            //if (readStatus) text = "Пометить непрочитанной";
                            //Ext.MessageBox.confirm('Изменить статус прочтения', text, function (button) {
                            //    if (button === 'yes') {
                            userSupportRequestEDS.changeTicketReadStatus(ticketId, !readStatus, function (readStatus, e) {
                                if (!e.status) {
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else self.refresh();
                            });
                            //    }
                            //});
                        }
                    }
                }
            },
            onSelectChange: function (selModel, selections) {
                //if (hideRule)
                //    this.down('#delete').setDisabled(true);
                //else
                //this.down('#delete').setDisabled(selections.length === 0);
            }
        });
        this.callParent();
        userInfo.getUserSettings(function (resp, e) {
            self.down('toolbar').insert(2, {
                margin: '0 0 0 10',
                itemId: 'supportNotificationsByEmail',
                name: 'supportNotificationsByEmail',
                boxLabel: tr('support.notifications.byEmail'),
                xtype: 'checkbox',
                checked: !!resp.supportNotificationsByEmail,
                handler: function (chb, checked) {
                    var data = {'supportNotificationsByEmail': checked};
                    userInfo.updateUserSettings(data);
                }
            });
        });

    },
    onAddClick: function (type) {
        var viewport = this.up('viewport');
        if (!this.openedWnd) {
            this.openedWnd = Ext.create("Seniel.view.support.SupportRequestWindow", {});
            var self = this;
            this.openedWnd.on('close', function () {
                self.refresh();
                delete self.openedWnd;
            });
            viewport.showNewWindow(this.openedWnd);
        } else {
            this.openedWnd.setActive(true);
        }
    },
    showTicketForm: function (record) {
        var self = this;
        var viewport = self.up('viewport');
        var wnd = viewport.createOrFocus('TicketWindow' + record.get('_id'), 'Seniel.view.support.TicketWindow', {
            title: tr('support.ticket') + ' № "' + record.get('_id') + '"',
            ticketId: record.get('_id'),
            icon: 'images/ico16_eventsmsgs.png',
            btnConfig: {
                icon: 'images/ico16_eventsmsgs.png',
                text: tr('support.ticket')
            }
        });
        if (wnd.justCreated) {
            viewport.showNewWindow(wnd);
            wnd.onLoad(record.get("_id"));
            wnd.on('close', function (args) {
                self.refresh();
            });
        }
    }
})