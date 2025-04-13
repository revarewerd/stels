/**
 * Created by ivan on 15.08.16.
 */

Ext.define('Seniel.view.reports.MapGeneratorWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.mapgenerator',
    width: 320,
    height: 280,
    initComponent: function () {
        Ext.apply(this, {
            items: [
                {
                    xtype: 'checkboxfield',
                    boxLabel: 'Custom size',
                    id: 'customSize'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Width',
                    id: 'widthField'
                },
                {
                    xtype: 'textfield',
                    fieldLabel: 'Height',
                    id: 'heightField'
                }
            ]
        });
        this.callParent(arguments)
    },
    bbar: [
        {
            xtype: 'button',
            text: 'Generate',
            handler: function (btn) {
                var wnd = btn.up('mapgenerator');
                var customSize = wnd.down("#customSize").getValue();
                var params = '';
                params += Ext.Object.toQueryString(wnd.reportParams);
                if(customSize) {
                    params += '&width=' + wnd.down('#widthField').getValue();
                    params += '&height=' + wnd.down('#heightField').getValue();
                }
                window.open('/EDS/reportMap?' + params);
                wnd.close();
            }
        }

    ]
});