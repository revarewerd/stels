Ext.define('Seniel.view.reports.ExportWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.repexportwnd',
    icon: 'images/ico16_download.png',
    title: tr('basereport.export.title'),
    width: 320,
    height: 280,
    minWidth: 240,
    minHeight: 200,
    modal: true,
    layout: 'anchor',
    items: [
        {
            xtype: 'combo',
            itemId: 'exportType',
            fieldLabel: tr('basereport.export.filetype'),
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['type', 'text'],
                data: {
                    'items': [
                        {type: 'pdf', text: 'PDF'},
                        {type: 'xls', text: 'XLS'},
                        {type: 'docx', text: 'DOCX'}
                    ]
                },
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            }),
            allowBlank: false,
            editable: false,
            queryMode: 'local',
            valueField: 'type',
            displayField: 'text',
            value: 'pdf',
            labelWidth: 100,
            padding: '12 12 0 12'
        },
        {
            xtype: 'checkboxgroup',
            itemId: 'exportList',
            fieldLabel: tr('basereport.export.checkreport'),
            items: [
                
            ],
            labelWidth: 96,
            columns: 1,
            vertical: true,
            padding: '12 8 0 12',
            listeners: {
                boxready: function() {
                    var wnd = this.up('window');
                    var name = this.id + '-select';
                    var reports = [];
                    console.log('Reports list = ', wnd.reportsList);
                    if (wnd && wnd.reportsList) {
                        for (var i = 0; i < wnd.reportsList.length; i++) {
                            reports.push(
                                Ext.create('Ext.form.field.Checkbox', {
                                    boxLabel: wnd.reportsList[i].label,
                                    inputValue: wnd.reportsList[i].input,
                                    name: name
                                })
                            );
                        }
                    }
                    
                    this.add(reports);
                }
            }
        }
    ],
    bbar: [
        '->',
        {
            xtype: 'button',
            text: tr('main.ok'),
            itemId: 'btnOk',
            icon: 'images/ico16_okcrc.png',
            handler: function(btn) {
                var wnd = btn.up('repexportwnd');
                if (wnd && wnd.reportsParams) {
                    var urlParams = '';
                    for (var i in wnd.reportsParams) {
                        if (wnd.reportsParams[i]) {
                            var param = wnd.reportsParams[i];
                            if(Ext.isArray(param))
                                for(var s = 0; s < param.length; ++s)
                                    urlParams += i + '=' + encodeURIComponent(param[s]) + '&'
                            else
                                urlParams += i + '=' + encodeURIComponent(param) + '&';
                        }
                    }
                    
                    var repList = wnd.down('#exportList').getValue()[wnd.down('#exportList').id + '-select'];
                    var fileType = wnd.down('#exportType').getValue();
                    if (fileType !== 'csv') {
                        var url = './EDS/export2' + fileType.toUpperCase() + '/report.' + fileType + '?' + urlParams;
                        url += 'repList=';
                        if (typeof repList === 'string' || repList instanceof String)
                            url += repList;
                        else {
                            for (var j = 0; j < repList.length; j++) {
                                url += (j === 0)?(repList[j]):(';' + repList[j]);
                            }
                        }
                        
                        window.open(url);
                    } else {
                        for (var k = 0; k < repList.length; k++) {
                            if (repList[k] !== 'speed' && repList[k] !== 'sensor' && repList[k] !== 'fuelgraph') {
                                window.open('./EDS/generateCSV/' + repList[k] + '.csv?uid=' + wnd.reportsParams.uid + '&from=' + encodeURIComponent(new Date(wnd.reportsParams.from)) + '&to=' + encodeURIComponent(new Date(wnd.reportsParams.to)), repList[k]);
                            }
                        } 
                    }
                }
                
                
                wnd.close();
            }
        },
        {
            xtype: 'button',
            text: tr('main.cancel'),
            itemId: 'btnCancel',
            icon: 'images/ico16_cancel.png',
            handler: function(btn) {
                var wnd = btn.up('repexportwnd');
                wnd.close();
            }
        }
    ]

    //---------
    // Функциии
    //---------
    

});