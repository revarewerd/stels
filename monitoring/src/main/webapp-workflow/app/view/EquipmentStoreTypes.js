/**
 * Created by IVAN on 24.03.2015.
 */
Ext.define('Workflow.view.EquipmentStoreTypes', {
    extend:'Ext.data.Store',
    fields: ['storeType', 'storeDesc'],
    data: [
        {   "storeType":"accStore","storeDesc":"склад клиента"},
        {   "storeType": "instStore","storeDesc": "склад монтажника"},
        {   "storeType":"mainStore","storeDesc":"главный склад"}

    ]
})