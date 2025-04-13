/**
 * Created by IVAN on 09.10.2015.
 */
Ext.define('Seniel.view.support.SupportRequestWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.supportreqwnd',
    //stateId: 'geozGrid',
    //stateful: true,
    title: tr('support.request.new'),
    icon: 'images/ico16_edit_def.png',
    btnConfig: {
        icon: 'images/ico16_edit_def.png',
        text: tr('support.request.new'),
    },
    minWidth: 400,
    minHeight: 320,
    width: 800,
    height: 300,
    layout: 'fit',
    items:[
        {
            //region:'center',
            xtype:'supportreqpanel',
        }
    ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: [
                '->', {
                    icon: 'images/ico16_okcrc.png',
                    disabled:true,
                    itemId: 'save',
                    text: tr("support.ticket.sendRequest"),
                    handler: function(){
                        var self=this
                        var data=this.up('window').down("#supportreqpanel").getValues();

                        var defaultTimezone = Timezone.getDefaultTimezone()
                        var dateTime=new Date();
                            console.log("dateTime",dateTime)
                        if (defaultTimezone) {
                            dateTime=moment.tz(Ext.Date.format(dateTime, "d.m.Y H:i:s:u"), "DD.MM.YYYY HH:mm:ss:SSS", defaultTimezone.name).toDate()
                            console.log("dateTime",dateTime)
                        }
                        data.dateTime=dateTime.getTime();
                        userSupportRequestEDS.createTicket(data,
                           function(res){
                               console.log("Завяка отправлена");
                           })
                        self.up('window').close();
                    }
                }, {
                    icon: 'images/ico16_cancel.png',
                    text: tr('support.ticket.cancel'),
                    handler: function(){
                        this.up('window').close();}
                }]
        }
    ]
})
Ext.define('Seniel.view.support.SupportRequestPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.supportreqpanel',
    //stateId: 'geozGrid',
    //stateful: true,
    //title: "Новое обращение",//tr('editgeozoneswnd.geozones'),
    //minWidth: 400,
    //minHeight: 320,
    //layout: 'border',
    itemId:"supportreqpanel",
    fieldDefaults:{
        margin: '0 20 5 20'//,
        //vtype:'alphanum',
    },
    layout: {
        type:'vbox',
        align:'stretch'
    },
    items:[
        {
            margin: '20 20 5 20',
            xtype: 'combobox',
            itemId:'category',
            name:'category',
            fieldLabel:tr('support.request.category'),
            allowBlank: false,
            forceSelection:true,
            typeAhead: true,
            valueField: 'value',
            displayField:'name',
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'name'],
                data : [
                    {"value":"equipment", "name":tr('support.request.category.equipment')},
                    {"value":"program", "name":tr('support.request.category.program')},
                    {"value":"finance", "name":tr('support.request.category.finance')}
                ]
            }),
            listeners:{
                change:function( cmb, newValue, oldValue, eOpts ){
                    var panel=cmb.up('panel');
                    var question=panel.down('#question');
                    var qstore=question.getStore() ;
                    var qdata=[]
                    console.log(newValue);
                    switch(newValue) {
                        case 'equipment':{
                            qdata=[
                                {"question":tr("support.request.question.equipment.installChange")},
                                {"question":tr("support.request.question.equipment.notWork")},
                                {"question":tr("support.request.question.another")}
                            ]
                            break;}
                        case 'program':{
                            qdata=[
                                {"question":tr("support.request.question.program.about")},
                                {"question":tr("support.request.question.program.errors")},
                                {"question":tr("support.request.question.program.proposals")},
                                {"question":tr("support.request.question.another")}
                            ]
                            break;}
                        case 'finance':{
                            qdata=[
                                {"question":tr("support.request.question.finance.tariffs")},
                                {"question":tr("support.request.question.finance.funds")},
                                {"question":tr("support.request.question.finance.blocking")},
                                {"question":tr("support.request.question.another")}
                            ]
                            break;}

                    }
                    console.log('qdata',qdata);
                    //qstore.store=qdata;
                    qstore.loadData(qdata);
                    question.clearValue();
                    question.enable();
                    //question.setValue(rec.get("eqMark"));
                    //question.fireEvent('change',eqMark);
                }
            }
        },
        {
            xtype: 'combobox',
            itemId:'question',
            name:'question',
            disabled:true,
            fieldLabel:tr('support.request.question'),
            allowBlank: false,
            forceSelection:true,
            valueField: 'question',
            displayField: 'question',
            typeAhead: true,
            //forceSelection:true,
            queryMode: 'local',
            store:Ext.create('Ext.data.Store', {
                fields:['question']
            }),
            listeners: {
                change: function (cmb, newValue, oldValue, eOpts) {
                    var panel = cmb.up('panel');
                    var description = panel.down('#description');
                    description.setValue("");
                    description.enable();
                }
            }
        },
        {
            margin: '0 20 20 20',
            xtype: 'textareafield',
            itemId:'description',
            name:'description',
            disabled:true,
            fieldLabel: tr('support.request.problem'),
            rows:10,
            listeners: {
                change: function (cmb, newValue, oldValue, eOpts) {
                    var saveBtn= cmb.up('window').down("#save");
                    saveBtn.enable();
                }
            }
        }
    ]
})