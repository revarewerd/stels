/**
 * Created by IVAN on 19.02.2015.
 */
Ext.define('Workflow.view.WorkStatusStore', {
    extend:'Ext.data.Store',
    fields: ['workStatus', 'workStatusDesc'],
    data: [
        {   "workStatus":"complete","workStatusDesc":"Выполнена"},
        {   "workStatus": "notcomplete","workStatusDesc": "Не выполнена"}
    ]
})