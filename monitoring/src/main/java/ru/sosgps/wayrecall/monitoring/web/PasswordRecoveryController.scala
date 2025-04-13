package ru.sosgps.wayrecall.monitoring.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, RequestParam, RequestMapping}
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.MailSender

@Controller
class PasswordRecoveryController extends grizzled.slf4j.Logging {

	@Autowired
	var mdbm : MongoDBManager = null
	@Autowired
	var mailSender : MailSender = null

	@RequestMapping(value = Array("/recoverypassword"), produces = Array("application/json; charset=utf-8"), params = Array("username"))
	@ResponseBody
	def recoveryPassword(@RequestParam("username") username : String) : String = {

		logger.debug("recoveryPassword")
		val obj = mdbm.getDatabase().apply("users").findOne(Map("name" -> username))
		obj match {
			case None => PasswordRecoveryController.RESULT_ERROR_NO_USER
			case Some(o) =>
				val emailAddress = o.as[String] ("email")
				val password = o.as[String] ("password")

				if (emailAddress.isEmpty) {
					return PasswordRecoveryController.RESULT_ERROR_NO_EMAIL
				}

				mailSender.sendEmail(emailAddress, "Восстановление пароля", "Ваш пароль: " + password)
				PasswordRecoveryController.RESULT_OK
		}
	}
}

object PasswordRecoveryController {

	private val RESULT_OK : String = "{\"message\":\"Пароль отправлен на почту\"}"
	private val RESULT_ERROR_NO_EMAIL : String = "{\"message\":\"Ошибка в процессе восстановления пароля: отсутствует email-адрес в профиле, обратитесь в техническую поддержку\"}"
	private val RESULT_ERROR_NO_USER : String = "{\"message\":\"Ошибка в процессе восстановления пароля: такого пользователя нет\"}"
}
