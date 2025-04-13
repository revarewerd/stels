ObjectFromExcel = {
    parseExcelString:function(excelString,objPanel){
            console.log('parceExcelString =',excelString.excelString);
            var paramsArray=excelString.excelString.split('\t');
            console.log('paramsArray =',paramsArray);
            
            //var objPanel=this
            objPanel.updateRecord()
            var objPanelRec=objPanel.getRecord();
            //console.log(objPanelRec) 
            objPanelRec.beginEdit();
            var name=''
            if(paramsArray[8]!='' && paramsArray[8]!=undefined && paramsArray[8]!=' ') name+=(paramsArray[8]);
            if(paramsArray[9]!='' && paramsArray[9]!=undefined) {if(name!='') name+=' '+paramsArray[9]; else name=paramsArray[9]};
            if (paramsArray[10]!='' && paramsArray[10]!=undefined) {if(name!='') name+=' '+paramsArray[10]; else name=paramsArray[10]};
            if(!objPanelRec.get("name"))   objPanelRec.set({'name':name});
            if(!objPanelRec.get("type"))   objPanelRec.set({'type':paramsArray[7]});
            if(!objPanelRec.get("marka"))   objPanelRec.set({'marka':paramsArray[8]});
            if(!objPanelRec.get("model"))   objPanelRec.set({'model':paramsArray[9]});
            if(!objPanelRec.get("gosnumber"))   objPanelRec.set({'gosnumber':paramsArray[10]});
            if(!objPanelRec.get("VIN"))   objPanelRec.set({'VIN':paramsArray[11]});
            console.log("objPanelRec.get('objnote')",objPanelRec.get("objnote"));
            var currentNote=objPanelRec.get("objnote");
            console.log("currentNote.search(paramsArray[13])",currentNote.search(paramsArray[13]));
            if(!currentNote)   objPanelRec.set({'objnote':paramsArray[13]});
            else if(paramsArray[13]!='' && paramsArray[13]);
            {
                var noteArray=currentNote.split("; ");
                var exists=false;
                for(var i in noteArray){                    
                    console.log("noteArray "+i,noteArray[i]);
                    if(noteArray[i]==paramsArray[13]){exists=true; break;}                    
                }
                if(!exists) objPanelRec.set({'objnote':objPanelRec.get("objnote")+"; "+paramsArray[13]});
            }
//            objPanelRec.set({
//                'name':name,
//                'type':paramsArray[7],
//                'marka':paramsArray[8],
//                'model':paramsArray[9],
//                'gosnumber':paramsArray[10],
//                'VIN':paramsArray[11],
//                'objnote':paramsArray[13]
//            })           
            objPanelRec.endEdit();
//            console.log(objPanelRec)            
            objPanel.loadRecord(objPanelRec);
            
            //var eqForm=this.down('eqform')            
            //if(!eqForm) {
            var eqPanel=objPanel.down('eqpanel');
            var eqPanelStore=eqPanel.down('grid').getStore();
            console.log('eqPanel store',eqPanelStore);
            console.log('range',eqPanelStore.getRange());
            var eqForm;
            var recn=eqPanelStore.find("eqIMEI",paramsArray[20]);            
            if(recn<0) {recn=eqPanelStore.find("eqSerNum",paramsArray[19]);}
            if(recn<0) {recn=eqPanelStore.find("simNumber",paramsArray[30]);}
            if(recn<0) {eqForm=eqPanel.addEquipmentForm().down('form') ;
//                        eqPanel.up('form').newEqRecord(eqPanel,eqForm,paramsArray)                          
                          this.newEqRecord(eqPanel, eqForm, paramsArray);
            }
            else  {eqForm=eqPanel.showEquipmentForm(eqPanelStore.getAt(recn)).down('form');
                eqForm.on('onLoad',function(){                    
                    //eqPanel.up('form').newEqRecord(eqPanel,eqForm,paramsArray)                    
                    this.newEqRecord(eqPanel, eqForm, paramsArray);
                })
                eqForm.onLoad();
            } 
            },
            newEqRecord:function(eqPanel,eqForm,paramsArray){
                            console.log('eqForm',eqForm);
            var eqFormRec=eqForm.getRecord();
            console.log('eqFormRec',eqFormRec);
            eqFormRec.beginEdit();
            if(!eqFormRec.get('contract'))   eqFormRec.set({'contract':paramsArray[6]});
            if(!eqFormRec.get('eqOwner'))   eqFormRec.set({'eqOwner':paramsArray[14]});
            if(!eqFormRec.get('eqRightToUse'))   eqFormRec.set({'eqRightToUse':paramsArray[15]});
            if(!eqFormRec.get('eqMark'))   eqFormRec.set({'eqMark':paramsArray[17]});
            if(!eqFormRec.get('eqModel'))   eqFormRec.set({'eqModel':paramsArray[18]});
            if(!eqFormRec.get('eqSerNum'))   eqFormRec.set({'eqSerNum':paramsArray[19]});
            if(!eqFormRec.get('eqIMEI'))   eqFormRec.set({'eqIMEI':paramsArray[20]});
            if(!eqFormRec.get('eqFirmware'))   eqFormRec.set({'eqFirmware':paramsArray[21]});
            if(!eqFormRec.get('eqLogin'))   eqFormRec.set({'eqLogin':paramsArray[22]});
            if(!eqFormRec.get('eqPass'))   eqFormRec.set({'eqPass':paramsArray[23]});
            if(!eqFormRec.get('eqWork'))   eqFormRec.set({'eqWork':paramsArray[26]});
            if(!eqFormRec.get('eqNote'))   eqFormRec.set({'eqNote':paramsArray[24]});
            if(!eqFormRec.get('instPlace'))   eqFormRec.set({'instPlace':paramsArray[12]});
            if(!eqFormRec.get('simOwner'))   eqFormRec.set({'simOwner':paramsArray[28]});
            if(!eqFormRec.get('simProvider'))   eqFormRec.set({'simProvider':paramsArray[29]});
            if(!eqFormRec.get('simNumber'))   eqFormRec.set({'simNumber':paramsArray[30]});
            if(!eqFormRec.get('simICCID'))   eqFormRec.set({'simICCID':paramsArray[31]});
            if(!eqFormRec.get('simNote'))   eqFormRec.set({'simNote':paramsArray[32]});
            eqFormRec.set({
//                'contract':paramsArray[6],
//                'eqOwner':paramsArray[14],
//                'eqRightToUse':paramsArray[15],
//                'eqMark':paramsArray[17],
//                'eqModel':paramsArray[18],
//                'eqSerNum':paramsArray[19],
//                'eqIMEI':paramsArray[20],
//                'eqFirmware':paramsArray[21],
//                'eqLogin':paramsArray[22],
//                'eqPass':paramsArray[23],
                'eqWorkDate':paramsArray[27],
                'eqSellDate':paramsArray[25]
//                'eqWork':paramsArray[26],
//                'eqNote':paramsArray[24],
//                'instPlace':paramsArray[12],
//                'simOwner':paramsArray[28],
//                'simProvider':paramsArray[29],
//                'simContract':paramsArray[30],
//                'simTariff':paramsArray[31],
//                'simSetDate':paramsArray[32],
//                'simNumber':paramsArray[30],
//                'simICCID':paramsArray[31],
//                'simNote':paramsArray[32]
            });
            if(!eqFormRec.get('eqtype'));
            switch(paramsArray[16]){
            case 'трекер':
                {eqFormRec.set('eqtype','Основной абонентский терминал');
                break;}
            case 'спящий':
                {eqFormRec.set('eqtype','Спящий блок автономного типа GSM');
                break;}
            case 'радиомаяк':
                {eqFormRec.set('eqtype','Радиозакладка');
                break;}            
            }
            eqFormRec.endEdit()
            console.log("eqFormRec",eqFormRec);
            eqForm.loadRecord(eqFormRec);
            eqForm/*.up('window')*/.on('save',function(result){
                console.log('result',result);
                var eqformrec=eqForm.getRecord();
                var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]');
                if(eqaddwnd.length==0) {                
                eqaddwnd[0]=WRExtUtils.createOrFocus('eqAddWnd', 'Billing.view.equipment.EquipmentStoreWindow', {});
                }
                var eqaddstore=eqaddwnd[0].down('grid').getStore();
                var eqaddrec;
                eqaddstore.load(function(records, operation, success) {
                    console.log('loaded records',records);
                     var recn=eqaddstore.find("_id",result.eqId);
                     console.log('recn',recn);
                     if(recn<0)   
                        {   var searchstring=eqaddwnd[0].down('[name=searchstring]');
                            var searchfield=eqaddwnd[0].down('[name=searchfield]');
                            eqaddwnd[0].show();
                            var IMEI=eqformrec.get("eqIMEI");
                            var phone=eqformrec.get("simNumber");
                            var serial=eqformrec.get("eqSerNum");
                            if(IMEI!=null && IMEI!="")
                                {searchstring.setValue(IMEI);
                                searchfield.setValue("eqIMEI");}
                            else if(phone!=null && phone!="")
                                {searchstring.setValue(phone);
                                 searchfield.setValue("simNumber");}
                            else if(serial!=null && serial!="")
                                {searchstring.setValue(serial);
                                searchfield.setValue("eqSerNum");}
                            searchstring.fireEvent("change");}
                     else 
                        {eqaddrec=eqaddstore.getAt(recn);
                         eqaddstore.removeAt(recn);
                         console.log('eqaddrec',eqaddrec);
                         //eqaddwnd[0].close()
                         eqPanel.down('[itemId=equipGrid]').getStore().add(eqaddrec);
                         
                     }
                })
            })
            }
}


