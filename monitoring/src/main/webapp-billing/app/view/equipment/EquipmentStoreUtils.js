/**
 * Created by IVAN on 27.03.2014.
 */
EquipmentStoreUtils = {
    storeRefresh: function (accountId){
        var acceqstorewnd=Ext.ComponentQuery.query('[xtype=acceqstorewnd]');
        var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]');
        var eqpanels=Ext.ComponentQuery.query('[xtype=eqpanel]');
        for(var i in eqpanels)
        {
            if (eqpanels[i].accountId==accountId)
            {
                eqpanels[i].down('grid').getSelectionModel().deselectAll();
                eqpanels[i].down('grid').getStore().load();
            }
        }
        for(var i in eqaddwnd)
        {
            eqaddwnd[i].down('grid').getSelectionModel().deselectAll();
            eqaddwnd[i].down('grid').getStore().load();
        }
        for(var i in acceqstorewnd)
        {
            console.log('acceqstorewnd',acceqstorewnd[i]);
            if (acceqstorewnd[i].id=='accEqStoreWnd'+accountId)
            {
//                acceqstorewnd[i].close();
//                break;
                acceqstorewnd[i].down('grid').getSelectionModel().deselectAll();
                acceqstorewnd[i].down('grid').getStore().load();
            }
        }
    }
}