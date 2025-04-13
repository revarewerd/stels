package ru.sosgps.wayrecall.monitoring.web
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import ru.sosgps.wayrecall.core.UserRolesChecker
import ru.sosgps.wayrecall.monitoring.web.EDSEndpointBase
import ru.sosgps.wayrecall.utils.errors.NotPermitted

@Controller
class CommandsEndpoint( @Qualifier("org.springframework.security.authenticationManager")
                        @Autowired a: AuthenticationManager,
                        @Autowired roleChecker: UserRolesChecker,
                        @Autowired objectsCommander: ObjectsCommander) extends EDSEndpointBase(a) {

    def filter_anonymous(): Unit = {
        if(roleChecker.getUserName() == "anonymousUser")
            throw new NotPermitted("AnonymousUser is not permitted")
    }

    @RequestMapping(Array("/getnotifications"))
    def get_notifications(request: HttpServletRequest, response: HttpServletResponse): Unit = basicAuth(request, response){
        filter_anonymous()
        val userName = roleChecker.getUserName()
        val result = Map("userName" -> userName)
        json_response(request, response, result)
    }

    @RequestMapping(value=Array("/blocktest"), method=Array(RequestMethod.POST))
    def blockTest(request: HttpServletRequest, response: HttpServletResponse,
                  @RequestParam("uid") uid:String,
                 @RequestParam(value="block", required = false, defaultValue = "true") block:Boolean,
                 @RequestParam(value="password", required = false) password:String)
    : Unit = basicAuth(request, response) {
        filter_anonymous()
//        response.setCharacterEncoding("UTF-8")
        objectsCommander.sendBlockCommand(uid, block, password)
    }
}
