Ext.define('Seniel.view.mapobject.ToolTip', {
    extend: 'Ext.tip.ToolTip',
    renderTo: Ext.getBody(),
    style: {
        opacity: '0.8'
    },
    dismissDelay: 0,
    border: false,
    padding: false,
    width: 480,
    items: [
        {
            xtype: 'grid',
            hideHeaders: true,
            disableSelection: true,
            rowLines: false,
            border: false,
            padding: false,
            columns: [
                {
                    text: 'Name',
                    dataIndex: 'name',
                    width: 180,
                    renderer: function(val) {
                        return '<b style="white-space:normal !important;">' + val + '</b>';
                    }
                },
                {
                    text: 'Value',
                    dataIndex: 'value',
                    flex: 1,
                    renderer: function(val) {
                        return '<div style="white-space:normal !important;">' + val + '</div>';
                    }
                }
            ],
            store: {
                fields: ['name', 'value'],
                data: {
                    'items': []
                },
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            }
        }
    ],
    listeners: {
        hide: function(component) {
            component.destroy();
        }
    }
});