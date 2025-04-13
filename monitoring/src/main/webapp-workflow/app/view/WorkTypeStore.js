/**
 * Created by IVAN on 05.02.2015.
 */
Ext.define('Workflow.view.WorkTypeStore', {
    extend:'Ext.data.Store',
    fields: ['workType', 'workDesc'],
    data: [
        {   "workType":"install","workDesc":"Установка"},
        {   "workType": "remove","workDesc": "Удаление"},
        {   "workType":"replace","workDesc":"Замена"}

    ]
})