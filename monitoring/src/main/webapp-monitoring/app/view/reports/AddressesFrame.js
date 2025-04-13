Ext.define('Seniel.view.reports.AddressesFrame', {
    //extend:'Ext.grid.Panel',
    extend: 'Ext.Component',
    alias: 'widget.addressesframe',
    autoEl: {
        tag: 'iframe',
        style: 'height: 100%; width: 100%; border: none',
        src: '/EDS/addressReport?uid=o1603556978227868060&from=Fri%20Feb%2001%202013%2000%3A00%3A00%20GMT%2B0400%20(MSK)&to=Fri%20Feb%2001%202013%2023%3A59%3A00%20GMT%2B0400%20(MSK)'
    },
//    height: 600,
//    id: 'data_export_iframe',
//    width: 600,
//    selType: 'checkboxmodel',
//    selModel: {
//        showHeaderCheckbox: true,
//        ignoreRightMouseSelection: true,
//        checkOnly: true,
//        mode: 'MULTI'
//    },
    title: tr('main.reports.visitedaddrs'),
    //html: 'Hello world!',
    refreshReport: function(settings) {
        console.log("addrframe refresh:", settings);
        console.log(this);
        var url = "/EDS/addressReport?" +
            "uid=" + settings.selected +
            "&from=" + encodeURIComponent(settings.dates.from) +
            "&to=" + encodeURIComponent(settings.dates.to)+
            "&controls=" + true;
        if(this.el)
            this.el.dom.src = url;
        else
            this.autoEl.src = url;

//        var reportWnd = this.up('reportwnd');
//        var store = this.getStore();
//        var self = this;
//
//        Ext.apply(store.getProxy().extraParams, {
//            from: settings.dates.from,
//            to: settings.dates.to,
//            selected: settings.selected
//        });
//        store.load({
//            scope: this,
//            callback: function(records, operation, success) {
//                reportWnd.down('seniel-mapwidget').drawParkingPoints(records);
//            }
//        });
    },

    initComponent: function() {
        this.callParent(arguments);

    }
});