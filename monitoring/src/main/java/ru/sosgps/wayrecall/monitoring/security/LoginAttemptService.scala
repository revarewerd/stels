package ru.sosgps.wayrecall.monitoring.security

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutionException, TimeUnit}

import com.google.common.cache.{CacheLoader, CacheBuilder, LoadingCache}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.event.{AuthenticationSuccessEvent, AuthenticationFailureBadCredentialsEvent}
import org.springframework.security.core.userdetails.{UserDetails, UserDetailsChecker}
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.{Component, Service}

class LoginAttemptService {

	private val MAX_ATTEMPT: Integer = 5
	private val attemptsCache: LoadingCache[String, AtomicInteger] = CacheBuilder.newBuilder()
		.expireAfterWrite(1, TimeUnit.MINUTES).build(new CacheLoader[String, AtomicInteger] {
		override def load(key: String): AtomicInteger = {
			new AtomicInteger(0)
		}
	})

	def loginSucceeded(key: String): Unit = {
		attemptsCache.invalidate(key)
	}

	def loginFailed(key: String): Unit = {
		attemptsCache.get(key).incrementAndGet()
	}

	def isBlocked(key: String): Boolean = {
		try {
			attemptsCache.get(key).intValue() >= MAX_ATTEMPT
		} catch {
			case e: ExecutionException => false
		}
	}
}

class AuthenticationFailureListener extends ApplicationListener[AuthenticationFailureBadCredentialsEvent] {

	@Autowired
	private val loginAttemptService: LoginAttemptService = null

	override def onApplicationEvent(e: AuthenticationFailureBadCredentialsEvent) {
		e.getAuthentication().getDetails() match {
			case auth: WebAuthenticationDetails =>
				loginAttemptService.loginFailed(auth.getRemoteAddress)
			case _ =>
		}
	}
}

class AuthenticationSuccessEventListener extends ApplicationListener[AuthenticationSuccessEvent] {

	@Autowired
	private val loginAttemptService: LoginAttemptService = null

	override def onApplicationEvent(e: AuthenticationSuccessEvent): Unit = {
		e.getAuthentication().getDetails() match {
			case auth: WebAuthenticationDetails =>
				loginAttemptService.loginSucceeded(auth.getRemoteAddress)
			case _ =>
		}
	}
}

class BruteforceChecker extends UserDetailsChecker with grizzled.slf4j.Logging {

	debug("BruteforceChecker()")

	@Autowired
	private val loginAttemptService: LoginAttemptService = null

	override def check(userDetails: UserDetails): Unit = {

		debug("check() " + loginAttemptService)
		if (loginAttemptService.isBlocked(ru.sosgps.wayrecall.utils.web.springCurrentRequest.getRemoteAddr))
			throw new LockedException("Too many attempts! Wait for 1 minute")
	}
}