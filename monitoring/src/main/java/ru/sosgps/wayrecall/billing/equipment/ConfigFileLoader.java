/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.billing.equipment;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
import ch.ralscha.extdirectspring.annotation.ExtDirectMethodType;
import ch.ralscha.extdirectspring.bean.ExtDirectFormPostResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.sosgps.wayrecall.billing.TrackerMesService;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.sms.SmsGate;
import ru.sosgps.wayrecall.utils.ExtDirectService;
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException;
import ru.sosgps.wayrecall.utils.errors.NotPermitted;

import javax.annotation.Resource;

/**
 *
 * @author ИВАН
 */
@ExtDirectService
public class ConfigFileLoader {

    private String configPath;

    @Resource(name = "m2mSmsGate")
    private SmsGate smsgate;

    @Autowired
    private UserRolesChecker rolesChecker;

    @Autowired
    private TrackerMesService trackerMesService;

    @Autowired
    private ObjectsRepositoryReader or;

    @Autowired
    private MongoDBManager mdbm;

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigFileLoader.class);

    @ExtDirectMethod(ExtDirectMethodType.FORM_POST)
    public ExtDirectFormPostResult uploadFile(
            @RequestParam("configFile") MultipartFile confFile,
            @RequestParam("fwFile") MultipartFile fwFile,
            @RequestParam(value = "forceConnection", required = false, defaultValue = "false") boolean forceConnection,
            @RequestParam("IMEI") String IMEI) throws IOException {
        try {

            if(!(rolesChecker.checkAdminAuthority() && rolesChecker.hasRequiredStringAuthorities("EquipmentDataSet")))
                throw new NotPermitted("access denied"); // mb EquipmentDataSet


            log.debug("uploadFile " + confFile.getSize() + " " + confFile.isEmpty() + " " + fwFile.getSize() + " " + fwFile.isEmpty());
            if (!confFile.isEmpty() && !fwFile.isEmpty())
                throw new ImpossibleActionException("Нельзя загрузить несколько файлов одновремеено");

            MultipartFile file = confFile.isEmpty() ? fwFile : confFile;
            if (IMEI != null && !IMEI.equals("")) {
                final ExtDirectFormPostResult extDirectFormPostResult = uploadConfFile(IMEI, file, confFile.isEmpty() ? "efwp" : "fp3c");

                if(forceConnection){
                    final DBObject eqObj = mdbm.getDatabase().apply("equipments").underlying().findOne(new BasicDBObject("eqIMEI", IMEI));
                    trackerMesService.sendRuptelaCMD((String) eqObj.get("simNumber"), "connect 91.230.215.12,9089,TCP");
                }

                return extDirectFormPostResult;

            } else {
                throw new ImpossibleActionException("Нельзя загрузить файл конфигурации для оборудования, IMEI которого не указан");
            }

        } catch (IllegalArgumentException e) {
            ExtDirectFormPostResult resp = new ExtDirectFormPostResult(false);
            resp.addResultProperty("errmsg", e.getMessage());
            return resp;
        }
    }

    private ExtDirectFormPostResult uploadConfFile(String IMEI, MultipartFile file, final String extension) throws IOException {
        ExtDirectFormPostResult resp = new ExtDirectFormPostResult(true);
        if (!file.isEmpty()) {
            final File confdir = new File(configPath, IMEI);
            confdir.mkdirs();
            final int lastIndexOfDot = file.getOriginalFilename().lastIndexOf('.');
            final String ext = file.getOriginalFilename().substring(lastIndexOfDot);
            for (File file2 : confdir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(ext);
                }
            })) {
                file2.delete();
            }

            final File file1 = new File(confdir, file.getOriginalFilename()+".temp");
            //file.transferTo(file1);
            try (FileOutputStream fos = new FileOutputStream(file1)) {
                fos.write(file.getBytes());
            }
            //File dest = new File(file1.getAbsolutePath() + "." + extension);
            File dest = new File(confdir, file.getOriginalFilename());
            file1.renameTo(dest);
            log.debug("Saved file:" + dest.getAbsolutePath() + " exsist=" + dest.exists());
            resp.addResultProperty("fileOldName", file.getOriginalFilename());
            resp.addResultProperty("fileIMEI", IMEI);
        } else
            throw new ImpossibleActionException("Файл для загрузки не был выбран");
        return resp;
    }
}
