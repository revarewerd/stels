Ext.define('Seniel.view.LeftPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.leftpanel',
    stateId: 'leftPan',
    stateful: true,

    title: tr('leftpanel.title'),
    width: 420,

    layout: 'border',

    items: [
        {
            title:'Объекты',
            region: 'center',
            xtype: 'mapobjectlist',
            title: tr('leftpanel.objects'),
            html: tr('leftpanel.objects.body')
        },
        {
            title:tr('main.groupsofobjects'),
            region: 'center',
            xtype: 'groupedobjectslist',
            //title: tr('leftpanel.objects'),
            //html: tr('leftpanel.objects.body')
        }
    ]
});